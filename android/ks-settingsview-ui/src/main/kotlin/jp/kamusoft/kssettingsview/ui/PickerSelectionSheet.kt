package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.toArgb
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * 選択面（[PickerCell] の [PickerSelectionSheet] / [NumberPickerCell] の [NumberSelectionSheet]）に
 * 適用する解決済みスタイル値。
 *
 * 色は Theme / CellStyle / Cell 固有値から解決済みの ARGB Int として保持し、
 * 選択面の描画側では解決順序を意識しない。
 *
 * @property accentColor 選択印と操作ラベルの強調色
 * @property sheetBackgroundColor シート面の背景色
 * @property separatorColor 候補行の区切り線色
 * @property rippleColor 候補行タップ時の Ripple 色
 * @property onAccentTextColor 強調色の上に載せる文字色（確定ボタンのラベル）
 * @property itemTextColor 候補行およびタイトルの文字色
 * @property itemTypeface 候補行およびタイトルの Typeface
 * @property itemTextSizeSp 候補行の文字サイズ（sp）
 * @property itemSubTextColor 候補行の副表示の文字色
 * @property itemSubTextTypeface 候補行の副表示の Typeface
 * @property itemSubTextSizeSp 候補行の副表示の文字サイズ（sp）
 */
internal data class PickerSheetStyle(
    @ColorInt val accentColor: Int,
    @ColorInt val sheetBackgroundColor: Int,
    @ColorInt val separatorColor: Int,
    @ColorInt val rippleColor: Int,
    @ColorInt val onAccentTextColor: Int,
    @ColorInt val itemTextColor: Int,
    val itemTypeface: Typeface,
    val itemTextSizeSp: Float,
    @ColorInt val itemSubTextColor: Int,
    val itemSubTextTypeface: Typeface,
    val itemSubTextSizeSp: Float,
) {
    /** ヘッダータイトルの文字サイズ（sp）。候補行より 1sp 大きい。 */
    val headerTitleTextSizeSp: Float
        get() = itemTextSizeSp + HEADER_TITLE_SIZE_DELTA_SP

    /** ヘッダーの操作ラベル（取消 / 確定）の文字サイズ（sp）。候補行より 1sp 小さい。 */
    val headerActionTextSizeSp: Float
        get() = itemTextSizeSp - HEADER_ACTION_SIZE_DELTA_SP

    companion object {
        /** ヘッダータイトルの文字サイズを候補行から導く差分（sp）。 */
        private const val HEADER_TITLE_SIZE_DELTA_SP: Float = 1f

        /** ヘッダーの操作ラベルの文字サイズを候補行から導く差分（sp）。 */
        private const val HEADER_ACTION_SIZE_DELTA_SP: Float = 1f

        /**
         * [PickerCell] と [Theme] / [EffectiveStyle] から選択面のスタイルを解決する。
         *
         * 強調色は「Cell 固有値 → CellStyle → Theme」の順で解決する
         * （`cell.accentColor` → `EffectiveStyle.accentColor` = `CellStyle.accentColor` → `Theme.cellAccentColor`）。
         * シート面と区切り線は Cell 個別の背景指定ではなく Theme の値を使う（選択面は Cell 行とは別の面であるため）。
         * 強調色の上に載せる文字色は list 全体の下地色 `Theme.backgroundColor` を使う（強調色で塗った面を
         * 下地色で抜く配色）。
         * 候補行のタイポグラフィ（色・Typeface・文字サイズ）は Cell 行のタイトルと同じ実効値を使い、
         * 利用者のフォント指定が選択面にも一貫して反映されるようにする。ヘッダーの文字サイズも
         * 候補行のサイズを基準に導出する（[headerTitleTextSizeSp] / [headerActionTextSizeSp]）。
         * 候補行の副表示は「主文の補足」という意味論が一致する description 系統の実効値
         * （`CellStyle.descriptionColor / descriptionFont` → `Theme.cellDescriptionColor /
         * cellDescriptionFont`）を継承する。
         */
        fun from(cell: PickerCell, theme: Theme, effective: EffectiveStyle): PickerSheetStyle =
            from(cell.accentColor, theme, effective)

        /**
         * [NumberPickerCell] と [Theme] / [EffectiveStyle] から選択面のスタイルを解決する。
         *
         * 解決規則は [PickerCell] 版と同一（強調色は「Cell 固有値 → CellStyle → Theme」の順）。
         * NumberPickerCell の選択面（[NumberSelectionSheet]）は PickerCell の選択面と同じ器・
         * 同じヘッダー意匠を持つため、スタイル値の型と解決規則も共有する。
         */
        fun from(cell: NumberPickerCell, theme: Theme, effective: EffectiveStyle): PickerSheetStyle =
            from(cell.accentColor, theme, effective)

        /**
         * [DatePickerCell] と [Theme] / [EffectiveStyle] から選択面のスタイルを解決する。
         *
         * 解決規則は [PickerCell] 版と同一（強調色は「Cell 固有値 → CellStyle → Theme」の順）。
         * DatePickerCell の Spinner モードの選択面（[DateSelectionSheet]、android/ADR-0009）も
         * 同じ器・同じヘッダー意匠を持つため、スタイル値の型と解決規則を共有する。
         * ヘッダーの確定 / 取消操作だけは `androidButtonColor` を最優先する契約があるため、
         * その差し替えは選択面側で行う。
         */
        fun from(cell: DatePickerCell, theme: Theme, effective: EffectiveStyle): PickerSheetStyle =
            from(cell.accentColor, theme, effective)

        /**
         * [TimePickerCell] と [Theme] / [EffectiveStyle] から選択面のスタイルを解決する。
         *
         * 解決規則は [PickerCell] 版と同一（強調色は「Cell 固有値 → CellStyle → Theme」の順）。
         * TimePickerCell の選択面（[TimeSelectionSheet]、android/ADR-0018）も同じ器・同じヘッダー
         * 意匠を持つため、スタイル値の型と解決規則を共有する。TimePickerCell には
         * `androidButtonColor` に相当する操作色の指定が無いため、ヘッダー操作色も強調色に従う。
         */
        fun from(cell: TimePickerCell, theme: Theme, effective: EffectiveStyle): PickerSheetStyle =
            from(cell.accentColor, theme, effective)

        /** Cell 固有の強調色 [cellAccentColor]（未指定なら `null`）を起点にスタイル値を解決する。 */
        private fun from(
            cellAccentColor: androidx.compose.ui.graphics.Color?,
            theme: Theme,
            effective: EffectiveStyle,
        ): PickerSheetStyle = PickerSheetStyle(
            accentColor = cellAccentColor?.toArgb() ?: effective.accentColor,
            sheetBackgroundColor = theme.cellBackgroundColor.toArgb(),
            separatorColor = theme.separatorColor.toArgb(),
            rippleColor = theme.selectedColor.toArgb(),
            onAccentTextColor = theme.backgroundColor.toArgb(),
            itemTextColor = effective.titleColor,
            itemTypeface = effective.titleTypeface,
            itemTextSizeSp = effective.titleSizeSp,
            itemSubTextColor = effective.descriptionColor,
            itemSubTextTypeface = effective.descriptionTypeface,
            itemSubTextSizeSp = effective.descriptionSizeSp,
        )
    }
}

/**
 * 選択面の候補1行を構成する View 参照。
 *
 * @property root 行コンテナ（タップ対象・アクセシビリティノード）
 * @property titleView 候補の表示名
 * @property subTitleView 候補の副表示（副表示を持たない候補では `GONE`）
 * @property checkView 選択印（[KsSimpleCheckView] の独自チェックマーク描画）
 */
internal class PickerSheetRowViews(
    val root: LinearLayout,
    val titleView: TextView,
    val subTitleView: TextView,
    val checkView: KsSimpleCheckView,
)

/**
 * [PickerCell] の選択面（ボトムシート）。
 *
 * 器は Material の `BottomSheetDialog`、中身は「ドラッグハンドル + ヘッダー + 候補リスト」で構成する
 * （android/ADR-0005）。ヘッダーは左に取消、中央に `pageTitle ?: title`、複数選択時のみ右に確定を置き、
 * 操作ラベルは OS の公開文字列リソース（`android.R.string.ok` / `android.R.string.cancel`）から解決する。
 *
 * 選択の確定経路は次の2つだけで、それ以外の閉じ方（取消ボタン・シート外側タップ・Back 操作・
 * 下方向スワイプ）では callback を発火しない。
 *
 * - 単一選択: 候補行タップで [onSingleSelected] を発火して閉じる
 * - 複数選択: 候補行タップは作業状態のトグルのみ、確定ボタンで [onMultiConfirmed] を発火して閉じる
 *
 * 候補行は主表示のみの1行構成で、副表示（[PickerItem.subText]）を持つ候補だけが2行構成になる。
 * 副表示は description 系統の実効値を継承し、長さによらず1行に収めて末尾を省略する。
 *
 * 高さはコンテンツ高で表示し、画面高の約半分を上限とする。上限を超える候補数では折り畳み表示のあいだ
 * リストの表示領域を可視領域へ制約し、内部スクロールで全候補へ到達できるようにする。初期表示では
 * 選択中の項目（複数選択なら最小 index）を可視領域の先頭へ送る。制約の解除と再適用はシートの
 * 直接ドラッグに追従する — ドラッグ開始で解除されて自然高まで展開でき、折り目へ戻ると再び制約される。
 *
 * モデル値は正規化せずに扱う。範囲外の index は作業状態に保持され、確定時の集合にも上限判定にも残る。
 *
 * @param hostContext シートを表示する Context
 * @param sheetTitle ヘッダー中央に表示するタイトル
 * @param items 候補一覧（主表示 + 任意の副表示）
 * @param selectionMode 単一 / 複数の選択モード
 * @param selectedIndex 単一選択モードの選択 index（範囲外・null なら選択印なし）
 * @param initialSelectedIndices 複数選択モードの初期選択集合
 * @param maxSelectedNumber 複数選択の上限（`0` 以下で無制限）
 * @param sheetStyle 解決済みのスタイル値
 * @param onSingleSelected 単一選択の確定 callback
 * @param onMultiConfirmed 複数選択の確定 callback
 */
internal class PickerSelectionSheet(
    hostContext: Context,
    sheetTitle: String,
    private val items: List<PickerItem>,
    private val selectionMode: PickerSelectionMode,
    private val selectedIndex: Int?,
    initialSelectedIndices: Set<Int>,
    private val maxSelectedNumber: Int,
    private val sheetStyle: PickerSheetStyle,
    private val onSingleSelected: (Int) -> Unit,
    private val onMultiConfirmed: (Set<Int>) -> Unit,
) : BottomSheetDialog(hostContext.ksThemedContext()) {

    /** 複数選択モードの作業状態。確定操作を経ない限りモデルへは反映しない。 */
    private val workingSelection: MutableSet<Int> = initialSelectedIndices.toMutableSet()

    private val density: Float = context.resources.displayMetrics.density

    private val itemsAdapter = ItemsAdapter()

    /** シート内容のルート（ドラッグハンドル + ヘッダー + 候補リスト）。 */
    internal val contentRoot: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    /** ヘッダー（取消 / タイトル / 確定）。 */
    internal val headerView: SheetHeaderView = SheetHeaderView(
        context = context,
        style = sheetStyle,
        title = sheetTitle,
        showConfirm = selectionMode == PickerSelectionMode.Multiple,
        onCancel = { cancel() },
        onConfirm = {
            onMultiConfirmed(workingSelection.toSet())
            dismiss()
        },
    )

    /** ヘッダー左の取消ラベル。 */
    internal val cancelView: TextView get() = headerView.cancelView

    /** ヘッダー中央のタイトル。 */
    internal val titleView: TextView get() = headerView.titleView

    /** ヘッダー右の確定ラベル（複数選択モードでのみ表示）。 */
    internal val confirmView: TextView get() = headerView.confirmView

    /** ヘッダー左のスロット（取消ラベルの当たり判定を担う）。 */
    internal val cancelSlot: FrameLayout get() = headerView.cancelSlot

    /** ヘッダー右のスロット（確定ラベルの当たり判定を担う）。 */
    internal val confirmSlot: FrameLayout get() = headerView.confirmSlot

    /** 候補リスト。 */
    internal val listView: RecyclerView = SelfContainedRecyclerView(context)

    /**
     * 触覚フィードバックの実行経路。要求が受け付けられたかを返す。
     *
     * 端末が特定の触覚フィードバックを提供しない状況（`performHapticFeedback` が `false` を返す）
     * を再現して代替経路を検証できるよう、差し替え可能にしている。
     */
    internal var hapticRequest: (View, Int) -> Boolean = { view, constant ->
        try {
            view.performHapticFeedback(constant)
        } catch (_: Throwable) {
            false
        }
    }

    /** シートの初期状態（折り畳み位置）を適用済みか。再開時に展開状態を巻き戻さないためのフラグ。 */
    private var initialStateApplied: Boolean = false

    /**
     * 折り目表示のときに候補リストへ適用する高さ。
     * [ViewGroup.LayoutParams.WRAP_CONTENT] のときは制約しない（候補が可視領域に収まる場合）。
     */
    private var collapsedListHeight: Int = ViewGroup.LayoutParams.WRAP_CONTENT

    /**
     * シート面の直接ドラッグに合わせて候補リストの高さ制約を切り替える監視。
     *
     * 折り目表示では候補リストを可視領域へ制約する。この状態ではシート内容の高さが折り目と一致し、
     * すべての候補が画面内にあって内部スクロールで到達できる代わりに、上方向へ展開する余地がない。
     * そこで利用者がシート面（ドラッグハンドルやヘッダー）を掴んだ時点で制約を解除し、コンテンツの
     * 自然高まで展開できるようにする。折り目へ戻ったら再び制約して、折り目表示で候補が画面外に
     * 残らないようにする。伸縮するのは折り目より下の不可視領域なので、見た目の跳ねは起きない。
     * 候補リストのスクロールは常に内部スクロールに閉じており、この監視を起こさない。
     */
    private val listHeightConstraintCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_DRAGGING -> releaseListHeightConstraint()
                BottomSheetBehavior.STATE_COLLAPSED -> applyCollapsedListHeight()
                else -> Unit
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
    }

    init {
        contentRoot.addView(buildSheetDragHandle(context, sheetStyle.separatorColor))
        contentRoot.addView(headerView)
        buildList()
        setContentView(contentRoot)
        // シート外側のタップで閉じられる（確定 callback は発火しない）。
        setCanceledOnTouchOutside(true)
    }

    override fun onStart() {
        super.onStart()
        val container = findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        applySheetSurfaceColor(container, sheetStyle.sheetBackgroundColor)
        applySheetHeight(container)
    }

    // MARK: - View 構築

    private fun dp(value: Float): Int = (value * density).toInt()

    /**
     * 初期表示で可視領域の先頭に見せる候補の位置。選択中の項目がない場合は `null`（先頭のまま）。
     *
     * 単一選択は `selectedIndex`、複数選択は選択中で最も小さい index を対象とする。範囲外の index は
     * 対象にしない。
     */
    private fun initialScrollPosition(): Int? = when (selectionMode) {
        PickerSelectionMode.Single -> selectedIndex?.takeIf { it in items.indices }
        PickerSelectionMode.Multiple ->
            workingSelection.filter { it in items.indices }.minOrNull()
    }

    private fun buildList() {
        listView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = itemsAdapter
            // 最後の行までスクロールしきったときに下端の余白を確保する。
            clipToPadding = false
            setPadding(0, 0, 0, dp(LIST_PADDING_BOTTOM_DP))
            addItemDecoration(SeparatorDecoration(sheetStyle.separatorColor))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }
        contentRoot.addView(listView)
    }

    /** 候補リストの高さを設定する（[ViewGroup.LayoutParams.WRAP_CONTENT] で制約なし）。 */
    private fun setListHeight(height: Int) {
        val params = listView.layoutParams as LinearLayout.LayoutParams
        if (params.height != height) {
            params.height = height
            listView.layoutParams = params
        }
    }

    /**
     * 候補リストの高さ制約を解除し、シート内容が自然高まで伸びられるようにする。
     *
     * リストのスクロール位置は、表示中の行を基準に再レイアウトされるため保たれる。
     */
    private fun releaseListHeightConstraint() {
        setListHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    /**
     * 折り目表示用の高さ制約を候補リストへ適用する。
     *
     * 制約が不要な（候補が可視領域に収まる）場合は [collapsedListHeight] が
     * [ViewGroup.LayoutParams.WRAP_CONTENT] であり、実質的に何もしない。
     */
    private fun applyCollapsedListHeight() {
        setListHeight(collapsedListHeight)
    }

    /**
     * 候補行の View 一式を構築する。
     */
    private fun createRowViews(): PickerSheetRowViews {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT,
            )
            minimumHeight = dp(ROW_MIN_HEIGHT_DP)
            setPadding(
                dp(SheetMetrics.PADDING_HORIZONTAL_DP),
                dp(ROW_PADDING_VERTICAL_DP),
                dp(SheetMetrics.PADDING_HORIZONTAL_DP),
                dp(ROW_PADDING_VERTICAL_DP),
            )
            isClickable = true
            isFocusable = true
            background = RippleDrawable(
                ColorStateList.valueOf(sheetStyle.rippleColor),
                null,
                ColorDrawable(Color.WHITE),
            )
            // 選択状態をアクセシビリティサービスへ「チェック可能 / チェック済み」として公開する。
            // 表示名は行コンテナの contentDescription が担うため、状態と名前が 1 ノードに揃う。
            accessibilityDelegate = object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.isCheckable = true
                    info.isChecked = host.isSelected
                }
            }
        }

        // 主表示と副表示を縦に積むテキスト列。行の残り幅をすべて占め、選択印の分だけ空ける。
        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val title = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            setTextColor(sheetStyle.itemTextColor)
            typeface = sheetStyle.itemTypeface
            textSize = sheetStyle.itemTextSizeSp
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            // 行コンテナが名前と状態をまとめて公開するため、子 TextView は読み上げ対象から外す。
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        // 副表示は長さによらず 1 行に収め、収まらない分は末尾を省略する
        // （副表示を持つ行の行高が内容の長さに依存しない）。
        val subTitle = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(ROW_SUBTEXT_TOP_MARGIN_DP)
            }
            setTextColor(sheetStyle.itemSubTextColor)
            typeface = sheetStyle.itemSubTextTypeface
            textSize = sheetStyle.itemSubTextSizeSp
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            visibility = View.GONE
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val check = KsSimpleCheckView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(CHECK_SIZE_DP), dp(CHECK_SIZE_DP))
            color = sheetStyle.accentColor
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        texts.addView(title)
        texts.addView(subTitle)
        root.addView(texts)
        root.addView(check)
        return PickerSheetRowViews(
            root = root,
            titleView = title,
            subTitleView = subTitle,
            checkView = check,
        )
    }

    // MARK: - 選択の状態遷移

    /** [index] の候補が選択印を持つか。 */
    private fun isCheckedAt(index: Int): Boolean = when (selectionMode) {
        PickerSelectionMode.Single -> selectedIndex == index
        PickerSelectionMode.Multiple -> workingSelection.contains(index)
    }

    private fun applyChecked(row: PickerSheetRowViews, checked: Boolean) {
        row.checkView.isChecked = checked
        // isSelected は AccessibilityNodeInfo の選択状態としてそのまま公開される。
        row.root.isSelected = checked
    }

    /**
     * 候補行タップの処理。
     *
     * 単一選択は即時確定して閉じる。複数選択は作業状態のトグルのみを行い、上限到達後の
     * 新規チェックは無視して拒否を示す触覚フィードバックを要求する（既存チェックの解除は常に可能）。
     */
    private fun handleItemTap(index: Int, row: PickerSheetRowViews) {
        when (selectionMode) {
            PickerSelectionMode.Single -> {
                onSingleSelected(index)
                dismiss()
            }
            PickerSelectionMode.Multiple -> {
                when {
                    workingSelection.contains(index) -> {
                        workingSelection.remove(index)
                        applyChecked(row, false)
                    }
                    maxSelectedNumber > 0 && workingSelection.size >= maxSelectedNumber -> {
                        requestRejectFeedback(row.root)
                    }
                    else -> {
                        workingSelection.add(index)
                        applyChecked(row, true)
                    }
                }
            }
        }
    }

    /**
     * 拒否を示す触覚フィードバックをシステムへ要求する。
     *
     * `REJECT` を提供しない端末（要求が受け付けられず `false` が返る、または例外になる）では
     * `KEYBOARD_TAP` を代替として試し、いずれも通らない場合は触覚フィードバックなしで続行する
     * （選択の可否には影響しない）。
     */
    private fun requestRejectFeedback(view: View) {
        if (hapticRequest(view, HapticFeedbackConstants.REJECT)) return
        hapticRequest(view, HapticFeedbackConstants.KEYBOARD_TAP)
    }

    // MARK: - シートの面と高さ

    /**
     * シートの高さをコンテンツ高（画面高の約半分を上限）へ合わせ、候補リストの表示領域を確定する。
     *
     * 候補の総高が上限を超える場合は、折り目表示のあいだリストの高さを可視領域（上限高からヘッダー等を
     * 差し引いた分）へ制約する。これによりシート内容の高さは折り目と一致し、候補は必ず可視領域内に
     * 収まって、到達はリストの内部スクロールが担う。制約後に選択中の項目を可視領域の先頭へ送る。
     * 制約はシート面のドラッグで解除され、折り目へ戻ると再び適用される。
     *
     * 高さの見積もりは行数ではなく実測（シート内容と候補リストの自然高）だけに依るため、副表示の有無で
     * 行高が変わるリストでもそのまま成立する。初期スクロールも位置指定であり行高に依存しない。
     *
     * 初期スクロールは初回表示のときだけ行い、再開時に利用者のスクロール位置を巻き戻さない。
     */
    private fun applySheetHeight(container: View) {
        val behavior = BottomSheetBehavior.from(container)
        val metrics = context.resources.displayMetrics
        val width = effectiveSheetWidth(container, behavior)
        val screenHeight = metrics.heightPixels

        // 制約のない状態でシート内容の自然高と候補リストの自然高を測る。
        setListHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        contentRoot.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST),
        )
        val naturalHeight = contentRoot.measuredHeight
        val naturalListHeight = listView.measuredHeight
        val cap = (screenHeight * SHEET_INITIAL_HEIGHT_RATIO).toInt()
        val peek = minOf(naturalHeight, cap).coerceAtLeast(1)

        // 折り目より下へはみ出す分だけリストの表示領域を狭め、内部スクロールで到達可能にする。
        val visibleListHeight = peek - (naturalHeight - naturalListHeight)
        collapsedListHeight = if (visibleListHeight > 0 && naturalListHeight > visibleListHeight) {
            visibleListHeight
        } else {
            ViewGroup.LayoutParams.WRAP_CONTENT
        }

        behavior.isFitToContents = true
        behavior.skipCollapsed = false
        behavior.peekHeight = peek

        if (!initialStateApplied) {
            behavior.addBottomSheetCallback(listHeightConstraintCallback)
            applyCollapsedListHeight()
            // 選択中の項目を可視領域の先頭へ送る（次のレイアウトで反映される）。
            initialScrollPosition()?.let { position ->
                (listView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
            }
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            initialStateApplied = true
        } else if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            // 再開時は折り目表示のときだけ制約を戻す（展開中の状態は保つ）。
            applyCollapsedListHeight()
        }
    }

    /**
     * シートの実効幅を求める。
     *
     * レイアウト済みなら実測幅、そうでなければ画面幅を Material の最大幅制約で丸めた値を使う
     * （横幅の広い画面ではシートが画面幅いっぱいにならないため）。
     */
    private fun effectiveSheetWidth(container: View, behavior: BottomSheetBehavior<*>): Int {
        if (container.width > 0) return container.width
        val screenWidth = context.resources.displayMetrics.widthPixels
        val maxWidth = behavior.maxWidth
        return if (maxWidth > 0) minOf(screenWidth, maxWidth) else screenWidth
    }

    // MARK: - 検証用フック

    /**
     * 候補行を [RecyclerView] のレイアウトを経由せずに生成して bind する。
     *
     * 候補行の表示・選択印・アクセシビリティ状態・タップ挙動を、シートの実表示を伴わずに
     * 検証するための経路。返る View は本番と同じ bind 経路を通っている。
     */
    internal fun bindRow(index: Int): PickerSheetRowViews {
        val row = createRowViews()
        bindRow(row, index)
        return row
    }

    /** 複数選択モードの現在の作業状態。 */
    internal fun currentWorkingSelection(): Set<Int> = workingSelection.toSet()

    // MARK: - Adapter

    /**
     * 既存の候補行 View を [position] の候補へ bind し直す本番の bind 実装。
     *
     * Adapter の `onBindViewHolder`（新規 bind と [RecyclerView] の recycle 経路の両方）から
     * 呼ばれる。internal なのはテストがシートの実表示を伴わずに同じ状態遷移を検証するため。
     */
    internal fun bindRow(row: PickerSheetRowViews, position: Int) {
        val item = items[position]
        row.titleView.text = item.text
        val subText = item.subText
        row.subTitleView.text = subText
        // 副表示を持たない候補は主表示のみの1行構成に戻す。
        row.subTitleView.visibility = if (subText == null) View.GONE else View.VISIBLE
        // 副表示を持つ行は主表示に続けて読み上げられるよう連結する。
        row.root.contentDescription =
            if (subText == null) item.text else "${item.text}, $subText"
        applyChecked(row, isCheckedAt(position))
        row.root.setOnClickListener { handleItemTap(position, row) }
    }

    private inner class ItemViewHolder(
        val row: PickerSheetRowViews,
    ) : RecyclerView.ViewHolder(row.root)

    private inner class ItemsAdapter : RecyclerView.Adapter<ItemViewHolder>() {
        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder =
            ItemViewHolder(createRowViews())

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            bindRow(holder.row, position)
        }
    }

    /**
     * 候補行の上端へ 1 物理 pixel の区切り線を描く。
     *
     * 行背景が不透明に塗られても線が消えないよう、children 描画後に呼ばれる `onDrawOver` で描画する。
     */
    private class SeparatorDecoration(
        @ColorInt private val separatorColor: Int,
    ) : RecyclerView.ItemDecoration() {

        private val paint = Paint().apply {
            isAntiAlias = false
            style = Paint.Style.FILL
        }

        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            paint.color = separatorColor
            val right = parent.width.toFloat()
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val top = child.top.toFloat() + child.translationY
                c.drawRect(0f, top, right, top + SEPARATOR_THICKNESS_PX, paint)
            }
        }
    }

    companion object {
        /** 初期表示高さの画面高に対する上限比率。 */
        private const val SHEET_INITIAL_HEIGHT_RATIO: Float = 0.5f

        private const val LIST_PADDING_BOTTOM_DP: Float = 18f
        private const val ROW_PADDING_VERTICAL_DP: Float = 14f
        private const val ROW_MIN_HEIGHT_DP: Float = 48f

        /** 主表示と副表示のあいだの余白（dp）。 */
        private const val ROW_SUBTEXT_TOP_MARGIN_DP: Float = 2f
        private const val CHECK_SIZE_DP: Float = 30f

        /** 区切り線の太さ（1 物理 pixel 固定。dp 換算しない）。 */
        private const val SEPARATOR_THICKNESS_PX: Float = 1f
    }
}
