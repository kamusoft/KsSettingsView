package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import jp.kamusoft.kssettingsview.R
import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * この View を載せている [KsSettingsView] を親方向へ辿って返す（見つからなければ `null`）。
 *
 * 行の ViewHolder から、表示中の選択面を預ける先を解決するために使う。`KsSettingsView` の外で
 * 単体の行を組み立てた場合は `null` になり、そのときは表示継続の対象にならない。
 */
internal fun View.findKsSettingsViewHost(): KsSettingsView? {
    var current: View? = this
    while (current != null) {
        if (current is KsSettingsView) return current
        current = current.parent as? View
    }
    return null
}

/**
 * 設定画面 UI のエントリポイント `View`。
 *
 * `FrameLayout` を継承し、内部に `RecyclerView` を 1 つ持つ。
 * `RecyclerView.adapter` は `ConcatAdapter(headerAdapter, mainListAdapter, footerAdapter)` で
 * Root H/F + 平坦リストをまとめる構成。
 *
 * # Theme の扱い
 *
 * Theme は `SettingsRoot` には含まれず、本 View の `var theme: Theme` プロパティと
 * Store の `theme: StateFlow<Theme>` 経路で扱う。`applyDiff` は Theme 更新を
 * 含まない（構造差分のみ）。
 *
 * # 使い方
 *
 * ```kotlin
 * val store = SettingsRootStore(initialRoot = settingsRoot { ... }, initialTheme = Theme())
 * val view = KsSettingsView(context).apply {
 *     style = KsSettingsViewStyle.Modern
 *     bind(store)
 * }
 * ```
 */
public class KsSettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    /** 内部 RecyclerView。 */
    private val recyclerView: RecyclerView = RecyclerView(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        )
        layoutManager = LinearLayoutManager(context)
        // change アニメーション（項目更新時のクロスフェード）を無効化する。
        // 有効なままだと `DefaultItemAnimator.canReuseUpdatedViewHolder` が false を返し、
        // 内容更新のたびに RecyclerView が ViewHolder を新規生成して旧行と重ねてフェードさせる。
        // EntryCell では EditText インスタンスごと差し替わって InputConnection が張り直され、
        // 日本語 IME の未確定文字列が 1 打鍵ごとに確定してしまう。
        // 無効化することで同一 ViewHolder への再 bind となり、行のちらつきも解消する。
        // 設計判断: android/ADR-0001（payload 付き通知との二重担保）。
        (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    }

    /** Root Header 用 Adapter。 */
    private val headerAdapter: RootHeaderFooterAdapter =
        RootHeaderFooterAdapter(role = RootHeaderFooterAdapter.Role.HEADER)

    /** Root Footer 用 Adapter。 */
    private val footerAdapter: RootHeaderFooterAdapter =
        RootHeaderFooterAdapter(role = RootHeaderFooterAdapter.Role.FOOTER)

    /** Section H/F + Cell の平坦リスト用 Adapter。 */
    private val mainListAdapter: KsSettingsListAdapter = KsSettingsListAdapter()

    /** ConcatAdapter（headerAdapter, mainListAdapter, footerAdapter の 3 段構成）。 */
    private val concatAdapter: ConcatAdapter = run {
        val config = ConcatAdapter.Config.Builder()
            .setStableIdMode(ConcatAdapter.Config.StableIdMode.SHARED_STABLE_IDS)
            .build()
        ConcatAdapter(config, headerAdapter, mainListAdapter, footerAdapter)
    }

    /** 現在登録されている ItemDecoration（style 変更時に removeItemDecoration するため保持）。 */
    private var currentDecoration: RecyclerView.ItemDecoration? = null

    /** 内部の `SettingsRoot`（applyDiff で部分更新される）。 */
    private var internalRoot: SettingsRoot = SettingsRoot()

    /** 内部の `Theme`（Store 購読 / `applyTheme` で更新される）。 */
    private var internalTheme: Theme = Theme()

    /** Store 購読の Job。`onDetachedFromWindow` で cancel する。 */
    private var storeCollectJob: Job? = null

    /**
     * バインド対象の Store 参照。
     *
     * `bind(store)` 呼び出し時点で `findViewTreeLifecycleOwner()` が `null` を返した場合
     * （View が attach 前など）に「pending Store」として保持し、`onAttachedToWindow` 時に
     * 再度購読確立を試みる。同一 Store の重複 bind 時は購読を作り直さない判定にも使用する。
     */
    private var pendingStore: SettingsRootStore? = null

    /** Window に attach されているか（復元走査の駆動条件のひとつ）。 */
    private var isAttachedToHostWindow: Boolean = false

    /** `SettingsRoot` が一度でも反映されたか（復元走査の駆動条件のひとつ）。 */
    private var isRootApplied: Boolean = false

    /**
     * 復元走査を予約済みか（次のメッセージで走る分が queue に載っているか）。
     *
     * 予約の重複を防ぐだけのフラグで、予約分を消化した時点で下ろす。
     */
    private var isRestoreScanScheduled: Boolean = false

    /**
     * 復元したカレンダーダイアログの「今日」ボタンが参照する今日の取得元。
     *
     * 端末の既定タイムゾーンにおける今日を返す。テストから固定日付を注入して、
     * 実行時刻に依存しない検証を行えるようにするための差し替え点。
     */
    internal var restoreTodayProvider: () -> LocalDate = { LocalDate.now() }

    /**
     * 表示中のカレンダー選択面（android/ADR-0019）。閉じられた時点で参照を手放す。
     *
     * 構成変更で Activity が作り直されるとき、この選択面を畳んでから表示状態を保存する。
     */
    private var activeCalendarDialog: DateCalendarDialog? = null

    /** [activeCalendarDialog] を開いた `DatePickerCell` の id。 */
    private var activeCalendarCellId: String? = null

    /**
     * 構成変更をまたいで引き継いだ、カレンダー選択面の表示状態と対象 Cell の id。
     *
     * 復元は「attach 済み」かつ「root 反映済み」が揃ってからでないと対象 Cell を探せないため、
     * ここへ預けておき、条件が揃った時点で1回だけ消化する。
     */
    private var pendingCalendarRestore: Pair<String, DateCalendarDisplayState>? = null

    /**
     * 見た目スタイル。
     *
     * setter で内部 `RecyclerView` の `ItemDecoration` を入れ替え、`invalidateItemDecorations` を呼ぶ。
     */
    public var style: KsSettingsViewStyle = KsSettingsViewStyle.Classic
        set(value) {
            field = value
            applyDecoration(value)
        }

    /**
     * Root Header。`null` で非表示。
     *
     * setter で内部 `headerAdapter.view` を更新する（`RootHeaderFooterAdapter` 側で
     * `notifyItemInserted` / `notifyItemRemoved` / `notifyItemChanged` を発行）。
     */
    public var rootHeader: RootAccessory? = null
        set(value) {
            field = value
            headerAdapter.view = value
        }

    /**
     * Root Footer。`null` で非表示。
     */
    public var rootFooter: RootAccessory? = null
        set(value) {
            field = value
            footerAdapter.view = value
        }

    /**
     * `theme` プロパティのバッキングフィールド。
     *
     * `setRootDirect` から内部のみで同期するために `internalTheme` と並列に管理する。
     * setter 経由（`view.theme = ...`）の場合のみ `applyThemeInternal` を発火させる。
     */
    private var themeBacking: Theme = Theme()

    /**
     * SettingsView 全体に適用される Theme。
     *
     * setter で `RecyclerView.backgroundColor` と各 ViewHolder の実効スタイルを再評価する。
     * Theme 更新は `SettingsRootDiff` 経路には流れない（`applyDiff` で扱わない）独立した API。
     *
     * 同値代入時の挙動について：
     * - Store 経路（`store.theme.collect` → setter）では `MutableStateFlow.value` setter が
     *   `equals` で同値を弾くため、本 setter には同値が届かない設計になっている。
     * - 直接呼び出し経路（外部から `view.theme = newTheme`）にも二重防御として同値スキップを
     *   入れ、Compose recomposition 頻発時の不要な `notifyDataSetChanged` 3 連発を抑止する。
     */
    public var theme: Theme
        get() = themeBacking
        set(value) {
            // 同値スキップ：`applyThemeInternal` は ConcatAdapter 配下 3 つの Adapter に対して
            // `notifyDataSetChanged()` を発火させるため、不要な reload を確実に避ける。
            if (themeBacking == value) return
            themeBacking = value
            applyThemeInternal(value)
        }

    init {
        // ID を持たない View には View 階層のインスタンス状態が保存されない。ホストが ID を
        // 与えていないときだけライブラリ既定の ID を自分へ付け、ホストが明示した ID は尊重する
        // （android/ADR-0021）。
        if (id == NO_ID) id = R.id.ks_settings_view

        addView(recyclerView)
        recyclerView.adapter = concatAdapter

        // 基本 Cell 7 種を自動登録する。
        if (!KsCellRegistry.isRegistered(LabelCell::class)) {
            KsCellRegistry.registerBasicCells(context)
        }
        // 入力系 Cell 5 種を自動登録する。自動登録はオプトアウト可能とし、
        // `KsCellRegistry.isRegistered(EntryCell::class)` を見て既登録ならスキップする
        // （テストや利用者が事前に異なる factory を登録するケースに対応）。
        if (!KsCellRegistry.isRegistered(EntryCell::class)) {
            KsCellRegistry.registerInputCells(context)
        }
        // CustomCell を自動登録する。基本 / 入力 Cell と同じく既登録ならスキップする。
        if (!KsCellRegistry.isRegistered(CustomCell::class)) {
            KsCellRegistry.registerCustomCell(context)
        }

        applyDecoration(style)
        // 初期 Theme（既定）を Adapter 群と RecyclerView 背景に反映する。
        // `applyThemeInternal` 経由ではなく直接設定し、構築時の `notifyDataSetChanged` を抑制する
        // （まだ何も bind されていないため全件 reload は不要）。
        mainListAdapter.theme = internalTheme
        headerAdapter.theme = internalTheme
        footerAdapter.theme = internalTheme
        recyclerView.setBackgroundColor(internalTheme.backgroundColor.toArgb())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // detach で切った adapter 参照を戻す。adapter がセットされるのは構築時の 1 回だけなので、
        // ここで戻さないと ViewPager2 のオフスクリーンページや Compose `AndroidView` の付け外しの
        // ように View を作り直さず detach / attach するホストで、内部状態を保ったままリストが空で
        // 復帰する。`concatAdapter` と配下 3 つの Adapter は状態ごと保持されているため、戻すだけで
        // detach 前の内容がそのまま出る（スクロール位置は復元対象に含まない）。
        // 既に入っているときに代入し直さないのは、`RecyclerView.setAdapter` が同一インスタンスでも
        // `removeAndRecycleViews` を伴う作り直しになり、初回 attach のたびに全 ViewHolder が
        // 無駄に再生成されるためである。
        if (recyclerView.adapter == null) {
            recyclerView.adapter = concatAdapter
        }

        // attach 時点で pending Store があり、かつ購読が張られていなければ、
        // ここで購読確立を試みる。Compose `AndroidView.factory` 内で bind(store) を
        // 呼んだ場合、その時点ではまだ Window に attach されておらず
        // `findViewTreeLifecycleOwner()` が null を返すケースがあるため、本フックで
        // リトライすることで Compose 利用時も確実に Diff 購読を開始できる。
        val store = pendingStore
        if (store != null && storeCollectJob == null) {
            resyncFromStore(store)
            attachStoreCollection(store)
        }

        isAttachedToHostWindow = true
        scheduleRestoreScanIfReady()
    }

    override fun onDetachedFromWindow() {
        isAttachedToHostWindow = false
        // 表示中の選択面は window に紐づく。行が window から外れた後も開いたままにすると、
        // 閉じる主体を失った window だけが残る。
        dismissActiveCalendarDialog()
        // Store 購読 Job を cancel する（メモリリーク防止）。
        storeCollectJob?.cancel()
        storeCollectJob = null
        // RecyclerView の adapter 参照を切る。切った参照は `onAttachedToWindow` で戻す。
        recyclerView.adapter = null
        super.onDetachedFromWindow()
    }

    // MARK: - 公開 API

    /**
     * `SettingsRootStore` をバインドし、Store の Diff Flow / Theme StateFlow を購読する。
     *
     * 初期状態として `store.state.value` で root、`store.theme.value` で Theme を反映後、
     * `store.diffs` / `store.theme` を `lifecycleScope` で collect 開始する。
     *
     * `findViewTreeLifecycleOwner()` が `null`（View がまだ Window に attach されていない、
     * Compose の `AndroidView.factory` 内など）の場合は、Store を `pendingStore` として
     * 保持し、`onAttachedToWindow` で改めて購読確立を試みる。
     *
     * 同一 Store を再 bind した場合は、既に購読が張られていれば購読は維持し（再構築しない）、
     * 初期 state / theme の反映のみ行う。別 Store を bind した場合は古い Job を cancel して
     * 新しい Store の購読を開始する。
     *
     * @param store バインドする Store
     */
    public fun bind(store: SettingsRootStore) {
        // 同一 Store の再 bind: 購読は維持し、初期 state / theme の再適用のみ行う。
        if (pendingStore === store && storeCollectJob?.isActive == true) {
            setRootDirect(store.state.value, store.theme.value)
            return
        }

        // 別 Store への bind は既存購読を解除し、Store 参照を差し替える。
        storeCollectJob?.cancel()
        storeCollectJob = null
        pendingStore = store

        // 初期 state / theme を即時反映
        setRootDirect(store.state.value, store.theme.value)

        // ライフサイクル経由で diffs / theme を collect。LifecycleOwner が取得できない場合は
        // pendingStore として保持し、onAttachedToWindow でリトライする。
        attachStoreCollection(store)
    }

    /**
     * バインド中の Store から切り離し、購読を解除する。
     *
     * 解除後は Store への更新（構造 Diff・内容更新バッチ・Theme）が表示へ反映されなくなる。
     * 表示中の内容はそのまま残るため、view 階層からの取り外しと参照の破棄は呼び出し側の責務。
     *
     * `onDetachedFromWindow` による購読の停止と違い、Store 参照そのものを手放すため、
     * 再 attach しても購読は復活しない。再び追従させたい場合は [bind] を呼び直す。
     *
     * 冪等であり、Store 未バインドおよび解除済みの状態で呼んでも何も起きない。
     */
    public fun unbind() {
        storeCollectJob?.cancel()
        storeCollectJob = null
        pendingStore = null
    }

    /**
     * 購読を張り直す直前に、Store の現在の `SettingsRoot` を取り込み直す。
     *
     * `diffs` / `contentUpdateBatches` は replay を持たない `SharedFlow` で、購読者がいない間に
     * 発行された Diff は誰にも届かないまま捨てられる。`onDetachedFromWindow` は購読 Job を cancel
     * するため、detach 中の Store 更新は取りこぼす。ここで現在値を反映し直さないと、再 attach 後に
     * 「detach 時点の内容」で復帰し、利用者からは正常に見えるまま古い内容が残る。
     *
     * Theme をここで反映しないのは、`theme` が `StateFlow` であり、[attachStoreCollection] が
     * collect を張り直した時点で現在値が改めて流れてくるためである。取り込むのは root だけにして、
     * Theme は現在値をそのまま持ち越す。なお [setRootDirect] は渡された Theme が現在値と異なる
     * ときに自ら再 bind 通知を発行するため、[bind] のように Theme を引数で渡す経路でも、続く
     * collect の同値スキップとは無関係に表示へ反映される。
     */
    private fun resyncFromStore(store: SettingsRootStore) {
        setRootDirect(store.state.value, internalTheme)
    }

    /**
     * Store の `diffs` / `theme` を `findViewTreeLifecycleOwner()` 配下の `lifecycleScope` で
     * collect 開始する。
     *
     * LifecycleOwner が解決できない場合（attach 前など）は何もしない。`onAttachedToWindow` で
     * 再度呼び出されることを想定する。
     */
    private fun attachStoreCollection(store: SettingsRootStore) {
        val owner = findViewTreeLifecycleOwner()
        if (owner == null) {
            Log.d(
                LOG_TAG,
                "bind(): findViewTreeLifecycleOwner() returned null. " +
                    "Diff collection will be retried on onAttachedToWindow().",
            )
            return
        }
        storeCollectJob = owner.lifecycleScope.launch {
            launch {
                store.diffs.collect { diff ->
                    applyDiff(diff)
                }
            }
            // 複数 Cell の内容更新バッチ（RadioCell グループ連動等）を collect し、
            // 単一 submitList + 複数 notifyItemChanged で一括反映する。
            launch {
                store.contentUpdateBatches.collect { cellIds ->
                    applyContentUpdateBatch(cellIds)
                }
            }
            // accessory の再計測要求を collect し、対象行だけを測り直す。
            launch {
                store.accessoryMeasureInvalidations.collect { target ->
                    invalidateAccessoryMeasurement(target)
                }
            }
            // Theme StateFlow を購読し、変更を View に反映する。
            // 初期値は bind() の setRootDirect で反映済みのため、ここでは distinctUntilChanged 相当の
            // 動作で重複反映を避けたいが、StateFlow 自体が conflate 性質を持つので素朴に collect する。
            launch {
                store.theme.collect { newTheme ->
                    if (theme != newTheme) {
                        theme = newTheme
                    }
                }
            }
        }
    }

    /**
     * 複数 Cell の内容更新を 1 回の部分更新でまとめて反映する。
     */
    private fun applyContentUpdateBatch(cellIds: List<String>) {
        val store = pendingStore ?: return
        internalRoot = store.state.value
        mainListAdapter.submitContentUpdate(
            newList = flatten(internalRoot.sections),
            cellIds = cellIds,
        )
    }

    /**
     * 指定した accessory 領域の高さを測り直す。
     *
     * 行の高さは中身の計測結果から決まるため、レイアウト要求は行そのものではなく中身（container
     * 配下の hosted view）へ出す。中身が自分の `onMeasure` をやり直し、その要求が親へ伝播して
     * 行の高さも測り直される。text accessory のように container を持たない行では行自身へ出す。
     *
     * 対象が表示対象に存在しない（未設定の Root accessory・hidden または未知の Section・画面外で
     * 行が生成されていない）ときは no-op。固定高さの Section header は `layoutParams.height` が
     * 優先されるため表示は変わらない。
     *
     * @param target 再計測する accessory
     */
    public fun invalidateAccessoryMeasurement(target: AccessoryTarget) {
        val position = accessoryAdapterPosition(target) ?: return
        val itemView = recyclerView.findViewHolderForAdapterPosition(position)?.itemView ?: return
        val hosted = (itemView as? ViewGroup)?.getChildAt(0)
        (hosted ?: itemView).requestLayout()
    }

    /**
     * 再計測対象を `ConcatAdapter` 全体での行位置へ解決する。
     *
     * Root H/F は専用 Adapter の 1 行であり、先頭 / 末尾に位置する。Section H/F は
     * `mainListAdapter` の平坦リスト上の位置に、先頭の Root Header 分をずらして解決する。
     *
     * @return 表示対象に存在しない accessory では `null`
     */
    private fun accessoryAdapterPosition(target: AccessoryTarget): Int? {
        val headerOffset = headerAdapter.itemCount
        return when (target) {
            AccessoryTarget.RootHeader -> if (headerOffset == 0) null else 0

            AccessoryTarget.RootFooter ->
                if (footerAdapter.itemCount == 0) null else headerOffset + mainListAdapter.itemCount

            is AccessoryTarget.SectionHeader -> {
                val index = mainListAdapter.currentList.indexOfFirst {
                    it is CellListItem.SectionHeader && it.sectionId == target.sectionId
                }
                if (index < 0) null else headerOffset + index
            }

            is AccessoryTarget.SectionFooter -> {
                val index = mainListAdapter.currentList.indexOfFirst {
                    it is CellListItem.SectionFooter && it.sectionId == target.sectionId
                }
                if (index < 0) null else headerOffset + index
            }
        }
    }

    /**
     * Test / Preview 用に直接 `SettingsRoot` と `Theme` を反映する。
     *
     * Store を介さず、内部状態と各 Adapter を一気に新 root / theme の内容で更新する。
     * 本パスでは `notifyDataSetChanged` は呼び出さず、Adapter の `theme` プロパティと
     * RecyclerView 背景色を直接更新する。
     *
     * # Theme の反映
     *
     * 渡された Theme が現在値と異なるときは、表示中の行へ [PAYLOAD_THEME] 付きの部分更新を
     * 発行して再 bind させる。Theme は `CellListItem` に含まれず `submitList` の差分に現れないため、
     * 通知しないと表示済みの行が古い配色のまま残る。
     *
     * # 構造と内容の分離
     *
     * 本メソッドは `setRoot` / `SettingsRootDiff.Full` / `replaceSection` / 可視性フォールバックの
     * 共通出口であり、構造（行の挿入・削除・移動）と内容（同一 id の行の中身）の両方が同時に
     * 変わり得る。構造は `submitList` の DiffUtil に委ね、DiffUtil には現れない
     * 「同一 id で残る Cell の内容差」だけを内容通知で補う（android/ADR-0012）。
     *
     * 内容通知の対象は [contentChangedCellIds] が返す「更新前後の表示リスト双方に存在し、値が変化した
     * Cell の id」に限る。新規に現れる行は挿入の構造通知で bind されるため通知を重ねず、消える行には
     * 通知先の行がない。初回の root 反映では旧リストが空になるので、対象は自然に 0 件になる。
     *
     * `ItemDecoration` だけは Theme を構築時に受け取って保持するため、直接更新では追従できない。
     * ここで [applyDecoration] を呼んで作り直す。`ItemDecoration` の入れ替えは Adapter への通知を
     * 一切発行しないので、本パスが避けている `notifyDataSetChanged` 多重呼び出しには当たらない。
     */
    internal fun setRootDirect(root: SettingsRoot, theme: Theme = Theme()) {
        internalRoot = root
        // theme プロパティ／フィールドを内部値として同期させる。`applyThemeInternal` 経由ではなく
        // 直接代入することで、AsyncListDiffer 在中の `submitList` と競合する `notifyDataSetChanged`
        // 多重呼び出しを避ける。
        val themeChanged = internalTheme != theme
        internalTheme = theme
        themeBacking = theme
        mainListAdapter.theme = theme
        headerAdapter.theme = theme
        footerAdapter.theme = theme
        recyclerView.setBackgroundColor(theme.backgroundColor.toArgb())
        if (themeChanged) {
            // 表示中の行へ再 bind を促す。`themeBacking` をここで直接書き換えるため、この後に
            // Store の `theme` StateFlow が同じ値を流しても `theme` setter の同値スキップに阻まれ、
            // `applyThemeInternal` は走らない。ここで通知しないと、表示済みの Cell・Section H/F・
            // Root H/F が古い Theme の配色とフォントのまま残る。
            // 構造差分（`submitList`）は itemCount を変えるが、本通知は itemCount を変えないため
            // AsyncListDiffer の差分計算とは競合しない。
            notifyThemeChangedToAdapters()
        }
        val newList = flatten(root.sections)
        // 内容通知の対象は submitList 提出前の表示リストから求める必要があるため、先に算出する。
        val contentCellIds = contentChangedCellIds(mainListAdapter.currentList, newList)
        mainListAdapter.submitFullUpdate(newList = newList, contentCellIds = contentCellIds)
        isRootApplied = true
        scheduleRestoreScanIfReady()
        // ItemDecoration を現 Theme で作り直す（separator 色等の反映）。
        applyDecoration(style)
    }

    /**
     * `SettingsRootDiff` を受け取り、内部状態と Adapter を部分更新する。
     */
    public fun applyDiff(diff: SettingsRootDiff) {
        when (diff) {
            is SettingsRootDiff.Full -> {
                setRootDirect(diff.root, internalTheme)
            }
            is SettingsRootDiff.InsertSection -> {
                val sections = internalRoot.sections.toMutableList()
                val clamped = diff.index.coerceIn(0, sections.size)
                sections.add(clamped, diff.section)
                internalRoot = internalRoot.copy(sections = sections.toList())
                mainListAdapter.submitList(flatten(internalRoot.sections))
            }
            is SettingsRootDiff.RemoveSection -> {
                val sections = internalRoot.sections.toMutableList()
                val index = sections.indexOfFirst { it.id == diff.sectionId }
                if (index < 0) {
                    reportMissingId("removeSection: sectionId ${diff.sectionId} not found")
                    return
                }
                sections.removeAt(index)
                internalRoot = internalRoot.copy(sections = sections.toList())
                mainListAdapter.submitList(flatten(internalRoot.sections))
            }
            is SettingsRootDiff.MoveSection -> {
                val sections = internalRoot.sections.toMutableList()
                if (diff.from !in sections.indices) {
                    reportMissingId("moveSection: from index ${diff.from} out of bounds (size: ${sections.size})")
                    return
                }
                val moved = sections.removeAt(diff.from)
                val clamped = diff.to.coerceIn(0, sections.size)
                sections.add(clamped, moved)
                internalRoot = internalRoot.copy(sections = sections.toList())
                mainListAdapter.submitList(flatten(internalRoot.sections))
            }
            is SettingsRootDiff.ReplaceSection -> {
                // 「ReplaceCell / ReplaceSection の可視性切替防御（Android）」: ReplaceSection は型上
                // Section 全体置換であり、`header` / `footer` / `headerHeight` / `isVisible` / `cells`
                // の任意変化を内包し得る。内部 cell の細粒度差分抽出は試みず、常に Full 経路
                // （`setRootDirect` 相当）で処理する。
                val sections = internalRoot.sections.toMutableList()
                val index = sections.indexOfFirst { it.id == diff.sectionId }
                if (index < 0) {
                    reportMissingId("replaceSection: sectionId ${diff.sectionId} not found")
                    return
                }
                sections[index] = diff.newSection
                internalRoot = internalRoot.copy(sections = sections.toList())
                // Full 経路に倒すことで cells 集合・accessory・visibility いずれの変化も
                // visible projection を再構築して反映する。
                setRootDirect(internalRoot, internalTheme)
            }
            is SettingsRootDiff.InsertCell -> {
                val sections = internalRoot.sections.toMutableList()
                val sectionIndex = sections.indexOfFirst { it.id == diff.sectionId }
                if (sectionIndex < 0) {
                    reportMissingId("insertCell: sectionId ${diff.sectionId} not found")
                    return
                }
                val target = sections[sectionIndex]
                val cells = target.cells.toMutableList()
                val clamped = diff.index.coerceIn(0, cells.size)
                cells.add(clamped, diff.cell)
                sections[sectionIndex] = target.copy(cells = cells.toList())
                internalRoot = internalRoot.copy(sections = sections.toList())
                mainListAdapter.submitList(flatten(internalRoot.sections))
            }
            is SettingsRootDiff.RemoveCell -> {
                val sections = internalRoot.sections.toMutableList()
                var found = false
                for (i in sections.indices) {
                    val target = sections[i]
                    val cellIndex = target.cells.indexOfFirst { it.id == diff.cellId }
                    if (cellIndex >= 0) {
                        val cells = target.cells.toMutableList()
                        cells.removeAt(cellIndex)
                        sections[i] = target.copy(cells = cells.toList())
                        found = true
                        break
                    }
                }
                if (!found) {
                    reportMissingId("removeCell: cellId ${diff.cellId} not found")
                    return
                }
                internalRoot = internalRoot.copy(sections = sections.toList())
                mainListAdapter.submitList(flatten(internalRoot.sections))
            }
            is SettingsRootDiff.ReplaceCell -> {
                // 「表示状態同期の三層分離」: ReplaceCell は同一 id の内容更新（reconfigure）であり、
                // セルの破棄・再生成（フルリバインド）を意味しない。内部 root を更新したうえで、
                // submitList ではなく notifyItemChanged による部分更新（submitContentUpdate）で反映する。
                //
                // ReplaceCell / ReplaceSection の可視性切替防御:
                // 旧 Cell と新 Cell の `isVisible` が異なる場合、可視性変化は構造同期上の挿入・削除と
                // して扱われるべき第三カテゴリで、reconfigure 経路には乗せられない。本実装では
                // **`internalRoot` から取得した旧 Cell の `isVisible`** で先に検出し、Full 経路
                // （`setRootDirect` 相当）にフォールバックする。検出は visible projection 上の存在
                // チェックよりも先に行う必要があり、旧 Cell が hidden でも model 上から取得した旧値で
                // 判定する必要がある。
                val sections = internalRoot.sections.toMutableList()
                var found = false
                var visibilityToggled = false
                for (i in sections.indices) {
                    val target = sections[i]
                    val cellIndex = target.cells.indexOfFirst { it.id == diff.cellId }
                    if (cellIndex >= 0) {
                        val oldCell = target.cells[cellIndex]
                        val oldVisible = (oldCell as? VisibilityAware)?.isVisible ?: true
                        val newVisible = (diff.newCell as? VisibilityAware)?.isVisible ?: true
                        visibilityToggled = oldVisible != newVisible
                        val cells = target.cells.toMutableList()
                        cells[cellIndex] = diff.newCell
                        sections[i] = target.copy(cells = cells.toList())
                        found = true
                        break
                    }
                }
                if (!found) {
                    reportMissingId("replaceCell: cellId ${diff.cellId} not found")
                    return
                }
                internalRoot = internalRoot.copy(sections = sections.toList())
                if (visibilityToggled) {
                    // 可視性切替: Full 経路で snapshot 再構築。submitContentUpdate（notifyItemChanged
                    // 系）は使わない（hidden 状態の Cell に対する ViewHolder が存在しないため、内容
                    // 反映が走らないことに加え、構造同期側の挿入・削除アニメーションも逸する）。
                    setRootDirect(internalRoot, internalTheme)
                } else {
                    mainListAdapter.submitContentUpdate(
                        newList = flatten(internalRoot.sections),
                        cellId = diff.cellId,
                    )
                }
            }
            is SettingsRootDiff.MoveCell -> {
                val sections = internalRoot.sections.toMutableList()
                var found = false
                for (i in sections.indices) {
                    val target = sections[i]
                    val cellIndex = target.cells.indexOfFirst { it.id == diff.cellId }
                    if (cellIndex >= 0) {
                        val cells = target.cells.toMutableList()
                        val moved = cells.removeAt(cellIndex)
                        val clamped = diff.toIndex.coerceIn(0, cells.size)
                        cells.add(clamped, moved)
                        sections[i] = target.copy(cells = cells.toList())
                        found = true
                        break
                    }
                }
                if (!found) {
                    reportMissingId("moveCell: cellId ${diff.cellId} not found")
                    return
                }
                internalRoot = internalRoot.copy(sections = sections.toList())
                mainListAdapter.submitList(flatten(internalRoot.sections))
            }
            is SettingsRootDiff.UpdateAccessory -> {
                applyUpdateAccessory(diff.target, diff.accessory)
            }
        }
    }

    /**
     * 新 Theme を View / Adapter / Decoration に反映する。
     *
     * Theme は `CellListItem` に含まれないため `submitList` を呼んでも DiffUtil は no-op になる。
     * 全 Cell の bind を新 Theme で強制的に再実行する必要があるが、`notifyDataSetChanged()` は
     * ViewHolder を全件再生成するため、payload 付き `notifyItemRangeChanged` で「同一 ViewHolder への
     * 再 bind」に留めて穏やかに反映する（アニメーションのちらつき低減・全件再生成回避）。
     *
     * 子 Adapter ごとに `itemCount > 0` のときだけ通知を発行し、空 Adapter には不要な通知を送らない。
     * ConcatAdapter 配下の通知は子 Adapter で発行する必要があるため、対象 Adapter ごとに 1 回ずつ
     * `notifyItemRangeChanged(0, itemCount, PAYLOAD_THEME)` を呼ぶ。
     */
    private fun applyThemeInternal(theme: Theme) {
        internalTheme = theme
        mainListAdapter.theme = theme
        headerAdapter.theme = theme
        footerAdapter.theme = theme
        // Theme.backgroundColor を RecyclerView に反映する
        recyclerView.setBackgroundColor(theme.backgroundColor.toArgb())
        notifyThemeChangedToAdapters()
        // ItemDecoration を新 Theme で再構築（separator 色等の反映）
        applyDecoration(style)
    }

    /**
     * 表示中の全 ViewHolder へ「Theme が変わったので描き直せ」を伝える。
     *
     * payload 付きで部分更新通知する。Adapter ごとに `itemCount > 0` のときのみ発行することで
     * 空 Adapter への不要な通知を避ける。[PAYLOAD_THEME] を受けた行のうち、Cell と text 形式の
     * Header / Footer は 2 引数版のフル bind へ落ちて確実に描き直され、View 形式の Header / Footer
     * だけが中身の作り直しを免れる。
     *
     * 呼び出し側は事前に各 Adapter の `theme` プロパティを新しい値へ更新しておく必要がある
     * （通知はあくまで再 bind の引き金で、値そのものは Adapter が保持しているものを読む）。
     */
    private fun notifyThemeChangedToAdapters() {
        val mainCount = mainListAdapter.itemCount
        if (mainCount > 0) {
            mainListAdapter.notifyItemRangeChanged(0, mainCount, PAYLOAD_THEME)
        }
        val headerCount = headerAdapter.itemCount
        if (headerCount > 0) {
            headerAdapter.notifyItemRangeChanged(0, headerCount, PAYLOAD_THEME)
        }
        val footerCount = footerAdapter.itemCount
        if (footerCount > 0) {
            footerAdapter.notifyItemRangeChanged(0, footerCount, PAYLOAD_THEME)
        }
    }

    private fun applyUpdateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?) {
        when (target) {
            AccessoryTarget.RootHeader -> {
                rootHeader = extractRootAccessory(accessory)
            }
            AccessoryTarget.RootFooter -> {
                rootFooter = extractRootAccessory(accessory)
            }
            is AccessoryTarget.SectionHeader -> {
                updateSectionAccessoryAndSubmit(
                    sectionId = target.sectionId,
                    accessory = accessory,
                    isHeader = true,
                )
            }
            is AccessoryTarget.SectionFooter -> {
                updateSectionAccessoryAndSubmit(
                    sectionId = target.sectionId,
                    accessory = accessory,
                    isHeader = false,
                )
            }
        }
    }

    private fun extractRootAccessory(accessory: SettingsAccessory?): RootAccessory? {
        return when (accessory) {
            is SettingsAccessory.Root -> accessory.accessory
            is SettingsAccessory.Section, null -> null
        }
    }

    private fun updateSectionAccessoryAndSubmit(
        sectionId: String,
        accessory: SettingsAccessory?,
        isHeader: Boolean,
    ) {
        val sections = internalRoot.sections.toMutableList()
        val index = sections.indexOfFirst { it.id == sectionId }
        if (index < 0) {
            reportMissingId("updateAccessory(section): sectionId $sectionId not found")
            return
        }

        val newAccessory: SectionAccessory? = when (accessory) {
            is SettingsAccessory.Section -> accessory.accessory
            is SettingsAccessory.Root, null -> null
        }

        val target = sections[index]
        sections[index] = target.copy(
            header = if (isHeader) newAccessory else target.header,
            footer = if (isHeader) target.footer else newAccessory,
        )
        internalRoot = internalRoot.copy(sections = sections.toList())
        mainListAdapter.submitList(flatten(internalRoot.sections))
    }

    // MARK: - カレンダー選択面の表示継続（android/ADR-0019）

    /**
     * 提示したカレンダー選択面を、表示継続の対象として覚える。
     *
     * 覚えるのは1面だけでよい。選択面はモーダルであり、同時に2つ開くことはない。
     *
     * @param cellId 選択面を開いた `DatePickerCell` の id
     * @param dialog 提示した選択面
     * @return 選択面が閉じたときに呼び、覚えている参照を手放すための処理
     */
    internal fun trackCalendarDialog(cellId: String, dialog: DateCalendarDialog): () -> Unit {
        activeCalendarDialog = dialog
        activeCalendarCellId = cellId
        return {
            if (activeCalendarDialog === dialog) {
                activeCalendarDialog = null
                activeCalendarCellId = null
            }
        }
    }

    /** 表示中のカレンダー選択面を閉じる（開いていなければ何もしない）。 */
    private fun dismissActiveCalendarDialog() {
        activeCalendarDialog?.takeIf { it.isShowing }?.dismiss()
        activeCalendarDialog = null
        activeCalendarCellId = null
    }

    /**
     * View 階層のインスタンス状態として、表示中のカレンダー選択面の状態を保存する
     * （android/ADR-0021）。
     *
     * ここでは状態を控えるだけで選択面は畳まない。状態保存はホストが実際に破棄されるときだけで
     * なく、ホーム画面や他アプリへ移るたびに起こる。畳んでしまうと、そのまま戻ってきただけの
     * 利用者から、開いていた選択面と途中まで進めた選択が失われる。破棄をまたぐ場合に選択面を
     * 閉じるのは、選択面自身がホストの破棄を購読して行う（[Dialog.showAnchoredTo]）。
     *
     * ライブラリ既定の ID を持つインスタンスが同一階層に複数あるときは、保存先が互いに衝突して
     * 状態が混ざるため保存しない。ホストが個別の ID を与えていれば衝突しない。
     */
    override fun onSaveInstanceState(): Parcelable {
        val saved = SavedState(super.onSaveInstanceState())
        val dialog = activeCalendarDialog
        val cellId = activeCalendarCellId
        if (dialog != null && cellId != null && dialog.isShowing && !hasAmbiguousLibraryDefaultId()) {
            saved.calendarCellId = cellId
            saved.calendarDisplayState = dialog.displayState()
        }
        return saved
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        val cellId = state.calendarCellId
        val display = state.calendarDisplayState
        if (cellId != null && display != null && !hasAmbiguousLibraryDefaultId()) {
            pendingCalendarRestore = cellId to display
            scheduleRestoreScanIfReady()
        }
    }

    /**
     * ライブラリ既定の ID を持つ [KsSettingsView] が、同一の View 階層に複数あるか。
     *
     * インスタンス状態は ID をキーに保存されるため、既定 ID のインスタンスが複数あると
     * 互いの状態を上書きし合う。所有者を一意に決められない構成では復元へ進まない。
     */
    private fun hasAmbiguousLibraryDefaultId(): Boolean {
        if (id != R.id.ks_settings_view) return false
        return countLibraryDefaultIdViews(rootView) > 1
    }

    /** [view] 以下にある、ライブラリ既定の ID を持つ [KsSettingsView] の数を数える。 */
    private fun countLibraryDefaultIdViews(view: View): Int {
        if (view is KsSettingsView) return if (view.id == R.id.ks_settings_view) 1 else 0
        if (view !is ViewGroup) return 0
        var count = 0
        for (index in 0 until view.childCount) {
            count += countLibraryDefaultIdViews(view.getChildAt(index))
        }
        return count
    }

    /**
     * 引き継いだ表示状態から、カレンダー選択面を提示し直す。
     *
     * 提示するのは、保存時と同一 id の `DatePickerCell`（[DatePickerUIStyle.Material]）が現 root に
     * ちょうど1つあるときに限る。0 個でも 2 個以上でも所有者を確定できないため提示せず、
     * どの Cell へも値を書き込まない。
     *
     * 引き継いだ状態は消化した時点で手放す（成立しなかった場合も再試行しない）。
     */
    private fun restoreCalendarDialogIfPending() {
        val (cellId, display) = pendingCalendarRestore ?: return
        pendingCalendarRestore = null
        val cell = findCalendarRestoreTarget(cellId) ?: return
        val range = DateCalendarRange.of(cell) ?: return
        // 行の描画と同じ Context（同梱テーマ適用済み）で解決し、提示時と復元時で色を揃える。
        val effective = EffectiveStyle.from(context.ksThemedContext(), internalTheme, cell.style)
        val dialog = DateCalendarDialog(
            hostContext = context,
            dialogTitle = cell.pickerTitle ?: cell.title,
            range = range,
            initialDate = range.clamp(cell.date),
            todayText = cell.todayText?.takeIf { it.isNotEmpty() },
            today = restoreTodayProvider,
            colors = resolveDatePickerDialogColors(cell, internalTheme, effective),
            restoredState = display.clampedTo(range),
            onConfirmed = { newDate -> cell.onValueChanged?.invoke(newDate) },
        )
        val forgetDialog = trackCalendarDialog(cell.id, dialog)
        dialog.showAnchoredTo(this, forgetDialog)
    }

    /** 引き継いだ表示状態に対応する適格な Cell を現 root から探す（一意でなければ `null`）。 */
    private fun findCalendarRestoreTarget(cellId: String): DatePickerCell? {
        val candidates = internalRoot.sections
            .asSequence()
            .flatMap { it.cells.asSequence() }
            .filterIsInstance<DatePickerCell>()
            .filter { it.id == cellId && it.uiStyle == DatePickerUIStyle.Material }
            .take(2)
            .toList()
        return candidates.singleOrNull()
    }

    // MARK: - 選択面の復元

    /**
     * 「Window に attach 済み」かつ「root 反映済み」の両条件が揃った時点で、引き継いだ表示状態の
     * 消化を予約する。
     *
     * 消化自体は次のメッセージへ回す。root 反映と attach のどちらが先でも、両方が揃ってから
     * 一度だけ走らせるための予約であり、予約分の重複は [isRestoreScanScheduled] が防ぐ。
     *
     * 予約分が detach 中に消化されて空振りした場合は、次の attach でここから予約し直す。
     */
    private fun scheduleRestoreScanIfReady() {
        if (isRestoreScanScheduled) return
        if (pendingCalendarRestore == null) return
        if (!isAttachedToHostWindow || !isRootApplied) return
        isRestoreScanScheduled = true
        post { runRestoreScan() }
    }

    /**
     * 予約分の復元処理を実行する。
     *
     * 引き継いだ表示状態からのカレンダー選択面の提示（[restoreCalendarDialogIfPending]）を担う。
     */
    private fun runRestoreScan() {
        // 予約分はここで消化済み。以降の再予約可否は引き継ぎ状態の有無が決める。
        isRestoreScanScheduled = false
        if (!isAttachedToHostWindow) return
        restoreCalendarDialogIfPending()
    }

    // MARK: - 内部処理

    /**
     * `style` プロパティに対応する [RecyclerView.ItemDecoration] を適用する。
     */
    private fun applyDecoration(newStyle: KsSettingsViewStyle) {
        currentDecoration?.let { recyclerView.removeItemDecoration(it) }
        val decoration: RecyclerView.ItemDecoration = when (newStyle) {
            KsSettingsViewStyle.Classic -> ClassicSectionDecoration(theme = internalTheme)
            KsSettingsViewStyle.Modern -> ModernSectionDecoration(theme = internalTheme)
        }
        recyclerView.addItemDecoration(decoration)
        currentDecoration = decoration
        recyclerView.invalidateItemDecorations()
    }

    /**
     * 存在しない ID への操作のエラーハンドリング。
     * DEBUG ビルドでは `error()` で即座にクラッシュし、Release では `Log.w` でログ出力のみ。
     */
    private fun reportMissingId(message: String) {
        if (KsCellRegistry.strictMode) {
            error(message)
        } else {
            Log.w(LOG_TAG, message)
        }
    }

    // MARK: - Internal アクセサ（テスト・診断用）

    internal fun internalRecyclerView(): RecyclerView = recyclerView

    internal fun internalCurrentDecoration(): RecyclerView.ItemDecoration? = currentDecoration

    internal fun internalMainListAdapter(): KsSettingsListAdapter = mainListAdapter

    internal fun internalHeaderAdapter(): RootHeaderFooterAdapter = headerAdapter

    internal fun internalFooterAdapter(): RootHeaderFooterAdapter = footerAdapter

    internal fun internalDetachForTest() {
        onDetachedFromWindow()
    }

    /** Test / 診断用に現在の内部 root を返す。 */
    internal fun internalRoot(): SettingsRoot = internalRoot

    /** Test / 診断用に現在の内部 theme を返す。 */
    internal fun internalTheme(): Theme = internalTheme

    /**
     * [KsSettingsView] が View 階層のインスタンス状態として保存する内容。
     *
     * 表示中だったカレンダー選択面の対象 Cell と表示状態だけを持つ。ボトムシート系の選択面は
     * 保存対象に含めない（構成変更で閉じ、値も書き込まない）。日付は端末タイムゾーンに依存しない
     * epoch day で持つ。
     */
    internal class SavedState : BaseSavedState {

        /** 表示中だったカレンダー選択面の対象 `DatePickerCell` の id（保存対象が無ければ `null`）。 */
        var calendarCellId: String? = null

        /** 表示中だったカレンダー選択面の表示状態（保存対象が無ければ `null`）。 */
        var calendarDisplayState: DateCalendarDisplayState? = null

        constructor(superState: Parcelable?) : super(superState)

        private constructor(source: Parcel) : super(source) {
            calendarCellId = source.readString()
            if (calendarCellId == null) return
            val selectedEpochDay = source.readLong()
            calendarDisplayState = DateCalendarDisplayState(
                selectedDate = if (selectedEpochDay == NO_SELECTED_DATE) {
                    null
                } else {
                    LocalDate.ofEpochDay(selectedEpochDay)
                },
                displayedMonth = LocalDate.ofEpochDay(source.readLong()),
                isTextInput = source.readInt() != 0,
            )
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            val display = calendarDisplayState
            if (display == null) {
                out.writeString(null)
                return
            }
            out.writeString(calendarCellId)
            out.writeLong(display.selectedDate?.toEpochDay() ?: NO_SELECTED_DATE)
            out.writeLong(display.displayedMonth.toEpochDay())
            out.writeInt(if (display.isTextInput) 1 else 0)
        }

        companion object {
            /** 選択日が定まっていないことを表す番兵（実在する epoch day と重ならない値）。 */
            private const val NO_SELECTED_DATE: Long = Long.MIN_VALUE

            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState = SavedState(source)

                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }

    public companion object {
        private const val LOG_TAG = "KsSettingsView"

        /**
         * Theme 更新時の `notifyItemRangeChanged` payload キー。
         *
         * Theme を反映する部分更新通知に付与する。Cell と text 形式の Header / Footer は
         * 3 引数版 `onBindViewHolder` から `super` へ委譲されてフル bind で再描画される。
         *
         * View 形式（`KsAnyView` backing）の Header / Footer だけは本 payload の振り分け対象で、
         * 中身を作り直さない。`KsAnyView.AndroidView` の View は factory から再生成すると
         * 内部状態（入力中のテキスト・スクロール位置・フォーカス）を失うのに対し、View 形式は
         * Theme が決める文字を持たないため、作り直しても得るものがない。
         */
        public const val PAYLOAD_THEME: String = "ks-theme"

        /**
         * 内容更新時の `notifyItemChanged` payload キー。
         *
         * Cell の内容更新 (`KsSettingsListAdapter.submitContentUpdate`)・Section H/F の内容差
         * (`CellListItemDiffCallback.getChangePayload`)・Root H/F の差し替え
         * (`RootHeaderFooterAdapter.view`) が共通で付与する。値そのものは参照されない。
         * payload が**非空であること**に意味があり、それによって
         * `SimpleItemAnimator.canReuseUpdatedViewHolder` が true を返して同一 ViewHolder への
         * 再 bind が保証される。各 Adapter の 3 引数版 `onBindViewHolder` は本 payload を
         * 振り分け対象外として `super` へ委譲し、2 引数版のフル bind に落ちるため内容は完全に反映される。
         *
         * 設計判断: android/ADR-0001（change アニメーション無効化との二重担保）。
         */
        internal const val PAYLOAD_CONTENT: String = "ks-content"

        /**
         * View accessory の Section Header で **固定高さだけが変わった**ときの
         * `notifyItemChanged` payload キー。
         *
         * この payload だけが届いた行は、`KsSettingsListAdapter` の 3 引数版 `onBindViewHolder` が
         * 高さの反映だけを行い、`KsAnyView` の中身を作り直さない。`KsAnyView.AndroidView` の View は
         * factory から再生成すると内部状態を失うため、高さのみの変更を内容の再バインドと区別する。
         */
        internal const val PAYLOAD_HEADER_HEIGHT: String = "ks-header-height"

        /**
         * `SettingsRoot.sections` を `CellListItem` の平坦リストに展開する。
         *
         * # 可視性フィルタ（visible projection の構築）
         *
         * 「表示状態同期の三層分離」に従い、以下を平坦リストから除外する：
         *
         * - `Section.isVisible = false` の Section は header / footer / 全 cells を完全除外
         *   （ヘッダ・フッタ・Cell すべて非生成）
         * - Section の Header / Footer は [shouldShowHeader] / [shouldShowFooter] の
         *   「表示トグル && 内容あり」の AND 判定を満たす場合にのみ行を生成する
         * - visible な Section 内の Cell は `(cell as? VisibilityAware)?.isVisible == false` を除外
         * - `VisibilityAware` に準拠していない Cell は常に visible として扱う（safe-by-default）
         *
         * 部分 Diff（InsertCell / RemoveCell / MoveCell / UpdateAccessory 等）に対する hidden 対象の
         * no-op 規約は本フィルタによって自然に成立する：`internalRoot` 側に値が書き込まれても、
         * 当該対象が hidden であれば前回 `flatten` 結果に含まれず、`submitList` 後の DiffUtil 上でも
         * 差分が発生しないため、`notifyItemChanged` 系の部分更新通知は対応 ViewHolder が存在せず
         * 自然に no-op となる。
         */
        internal fun flatten(sections: List<Section>): List<CellListItem> {
            val out = ArrayList<CellListItem>(sections.sumOf { 2 + it.cells.size })
            for (section in sections) {
                // Section.isVisible = false の場合、Section 全体（header / footer / cells）を除外する。
                if (!section.isVisible) continue
                val header = section.header
                if (header != null && shouldShowHeader(section)) {
                    out.add(
                        CellListItem.SectionHeader(
                            sectionId = section.id,
                            accessory = header,
                            headerHeight = section.headerHeight,
                        ),
                    )
                }
                for (cell in section.cells) {
                    // VisibilityAware に opt-in した Cell のみフィルタを適用する。
                    // 非準拠 Cell は常に visible として扱う（safe-by-default）。
                    val cellVisible = (cell as? VisibilityAware)?.isVisible ?: true
                    if (!cellVisible) continue
                    out.add(CellListItem.CellRow(sectionId = section.id, cell = cell))
                }
                val footer = section.footer
                if (footer != null && shouldShowFooter(section)) {
                    out.add(CellListItem.SectionFooter(sectionId = section.id, accessory = footer))
                }
            }
            return out
        }

        /**
         * accessory に内容があるかを判定する。
         *
         * 「内容の不在」は **null または空 text** とし、Header / Footer で共通の判定とする
         * （core/ADR-0023）。View accessory は中身が空でも常に内容ありとして扱う
         * （領域だけを確保したい用途は View accessory と高さ指定で表現する）。
         */
        internal fun hasAccessoryContent(accessory: SectionAccessory?): Boolean {
            return when (accessory) {
                null -> false
                is SectionAccessory.Text -> accessory.value.isNotEmpty()
                is SectionAccessory.View -> true
            }
        }

        /**
         * Section Header の行を生成するかを判定する。
         *
         * 判定は「表示トグル && 内容あり」の AND 合成（core/ADR-0023）。内容の無い Header で
         * 行を生成すると、内容がないまま高さだけが残るため生成しない。高さ（`Section.headerHeight` /
         * `Theme.headerHeight`）の解決は本判定の後に適用され、Header の存在を作らない。
         */
        internal fun shouldShowHeader(section: Section): Boolean {
            return section.isHeaderVisible && hasAccessoryContent(section.header)
        }

        /** Section Footer の行を生成するかを判定する。判定規則は [shouldShowHeader] と対称。 */
        internal fun shouldShowFooter(section: Section): Boolean {
            return section.isFooterVisible && hasAccessoryContent(section.footer)
        }

        /**
         * full 更新で内容変更通知を発行すべき Cell の id を、新リストの並び順で返す。
         *
         * 対象は「更新前後の平坦リスト双方に存在し、かつ Cell の値が変化したもの」に限る。
         *
         * - 新規に現れる行（新規 Cell・hidden からの復帰）は挿入の構造通知で bind されるため、
         *   内容通知を重ねない
         * - 消える行（削除・hidden 化）はそもそも通知先の行が存在しない
         * - 残る行のうち値が等しいものは表示すべき内容が変わっていないため、通知しない
         *   （Section H/F の accessory 内容が同一なら通知しないのと同じ扱い）
         *
         * 各 Cell の `equals` はクロージャ（タップ・値変更のリスナー）を除いた表示上の全フィールドを
         * 比較する値等価である。関数値（`CustomCell` の `builder` / `onTap` 等）は等価性に参加しない
         * （core/ADR-0014）ため、表示に効く値は `content` 側に含めるのが利用者契約 — 関数値だけを
         * 差し替えた full 更新はここでは変化として検出されない。
         *
         * @param oldList 更新前の平坦リスト（現在表示中のもの）
         * @param newList 更新後の平坦リスト
         */
        internal fun contentChangedCellIds(
            oldList: List<CellListItem>,
            newList: List<CellListItem>,
        ): List<String> {
            if (oldList.isEmpty() || newList.isEmpty()) return emptyList()
            val oldCells = HashMap<String, Cell>()
            for (item in oldList) {
                if (item is CellListItem.CellRow) oldCells[item.cell.id] = item.cell
            }
            if (oldCells.isEmpty()) return emptyList()
            val changed = ArrayList<String>()
            for (item in newList) {
                if (item !is CellListItem.CellRow) continue
                val oldCell = oldCells[item.cell.id] ?: continue
                if (oldCell != item.cell) changed.add(item.cell.id)
            }
            return changed
        }
    }
}
