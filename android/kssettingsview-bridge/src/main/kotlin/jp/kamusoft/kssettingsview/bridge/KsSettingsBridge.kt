package jp.kamusoft.kssettingsview.bridge

import android.content.Context
import android.view.View
import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.ui.KsScrollAnchor
import jp.kamusoft.kssettingsview.ui.KsScrollController
import jp.kamusoft.kssettingsview.ui.KsSettingsView
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle
import jp.kamusoft.kssettingsview.ui.SettingsRootStore

/**
 * interop 境界から設定画面を操作する Bridge。
 *
 * Bridge は `SettingsRootStore` を内部に所有し、公開 API を Store の公開操作へ変換する
 * （maui/ADR-0001）。Native Host は Bridge が生成し、内部 Store を bind 済みの状態で公開する
 * （maui/ADR-0005）。Bridge は同時に 1 つの Host を持ち、生きている Host がある間は
 * [makeHostView] を繰り返し呼んでも同じ Host を返す。
 *
 * [releaseHost] は Host だけを解放して Store を維持するため、解放後の [makeHostView] は Store
 * 現在状態から表示を復元した新しい Host を返す（maui/ADR-0007）。
 *
 * `Context` は [makeHostView] の引数で受け取り、Bridge のフィールドとしては保持しない。生成した
 * Host が `Context` を保持するため、Host を持っている間の Bridge の寿命は Host（= Activity）の
 * 寿命を超えてはならない。[releaseHost] で解放すれば Bridge は `Context` への参照を持たなくなり、
 * 別の `Context` で Host を作り直せる。
 *
 * 破棄（[dispose]）は冪等で、破棄後の操作 API と Host 生成はすべて no-op になる。破棄後は
 * Store を操作しないため、呼び出し側が保持し続けている Host の表示も変化しない。
 *
 * スクロール命令（[scrollToCell] など）は Store を経由せず、Bridge が持つ 1 つの命令ハンドルを
 * 通じて生きている Host へ届く。Host を手放すときはその表示位置を控え、次に作る Host で戻す。
 *
 * ユーザー操作は [interactionListener] へ通知する（maui/ADR-0003）。listener は強参照で保持する
 * ため、不要になったら `null` を設定して解除する。未設定・解除後の通知は破棄される。
 *
 * スレッド契約: 全 API を UI スレッドから呼ぶ。Bridge 自身は marshal しない（maui/ADR-0005）。
 */
class KsSettingsBridge {

    /**
     * 内部所有 Store。公開 API はすべてこの Store の公開操作へ変換される。
     *
     * interop 境界へ出さないため、JVM から見えない合成メンバとして公開する。
     */
    @get:JvmSynthetic
    internal val store: SettingsRootStore = SettingsRootStore(initialRoot = SettingsRoot())

    /** 生成済みの Native Host。未生成のときは `null`。 */
    private var hostView: KsSettingsView? = null

    /**
     * 生成済みの Host の表示位置を、window に取り付けられている間控え続けるもの。Host が無いときは `null`。
     *
     * Host は window から外れた後に解放されることがあり（MAUI の Handler の切断はページが画面から外れた
     * 後に届く）、その時点の Host からは位置を控えられないため。
     */
    @get:JvmSynthetic
    internal var hostAnchorTracker: KsBridgeHostAnchorTracker? = null
        private set

    /** 破棄済みかどうか。 */
    @get:JvmSynthetic
    internal var isDisposed: Boolean = false
        private set

    /**
     * 現在の見た目スタイル。
     *
     * スタイルは Store ではなく Host のプロパティのため、Host を作り直すと失われる。Store が
     * 設定ツリーと Theme を保つのと同じ生存性を与えるため、Bridge が Host の外で保持し、
     * Host の生成のたびに適用する。
     */
    @get:JvmSynthetic
    internal var style: KsSettingsViewStyle = KsSettingsViewStyle.Classic
        private set

    /**
     * スクロール命令を Host へ届けるハンドル。
     *
     * Host を作るたびにその Host へつなぎ直す。Host が無い間の命令はハンドルが未接続のため何も
     * 起こさない（Host 不在の判定を Bridge 側で持たず、Native の契約にそのまま委ねる）。
     */
    @get:JvmSynthetic
    internal val scrollController: KsScrollController = KsScrollController()

    /**
     * 手放した Host から控えた表示位置。次に作る Host へ渡した時点で使い切る。
     *
     * 位置は Host の世代をまたいで生きる Bridge が持つ（maui/ADR-0030）。
     */
    @get:JvmSynthetic
    internal var scrollAnchor: KsScrollAnchor? = null
        private set

    /**
     * Cell のコールバックと [interactionListener] の間に立つ中継。
     *
     * Cell へ注入する閉包はこの中継だけを掴むため、listener の差し替え・解除は生成済みの Cell に
     * そのまま反映され、閉包が listener 実装を直接掴むこともない。
     */
    @get:JvmSynthetic
    internal val interactionRelay: KsBridgeInteractionRelay = KsBridgeInteractionRelay()

    // MARK: - ユーザー操作の通知

    /**
     * ユーザー操作の通知先。
     *
     * `null` を設定すると解除でき、以後の操作は通知されない。設定・解除は表示中でも行える。
     */
    var interactionListener: KsBridgeInteractionListener?
        get() = interactionRelay.listener
        set(value) {
            interactionRelay.listener = value
        }

    // MARK: - Native Host

    /**
     * 内部 Store を bind 済みの Native Host を返す。
     *
     * 生きている Host があればそれを返し、未生成または [releaseHost] で解放済みなら新しい Host を
     * 生成して返す。破棄済みの Bridge では `null` を返す。
     * Host は接続時点の Store の現在状態から表示を復元するため、[setRoot] は Host 生成の前後
     * どちらで呼んでもよく、解放中に適用した更新も再生成した Host の表示に反映される。
     * ただし root の header / footer は Store ではなく Host が持つプロパティのため復元されない —
     * 再生成後も引き継ぐ場合は、呼び出し側が値を保持して [updateAccessory] で再適用する。再適用は
     * Host を view 階層へ取り付ける前に行ってよく、渡した値は最初の表示に含まれる。
     * 解放後は解放前と別の `Context` を渡してもよい。
     *
     * 新しい Host を作ったときは、スクロール命令をその Host へ届くようにする。前の Host を
     * [releaseHost] で手放したときに控えた表示位置があれば、その位置へ戻す要求を済ませてから返す。
     * 戻すのは初回の表示の後で、この後に出したスクロール命令は戻した後に実行される。
     *
     * @param context Host の生成に使う `Context`（Bridge のフィールドとしては保持しないが、
     *   生成された Host が保持する）
     * @return view 階層へ取り付ける Native Host
     */
    fun makeHostView(context: Context): View? {
        if (isDisposed) return null
        hostView?.let { return it }
        val view = KsSettingsView(context)
        view.style = style
        view.bind(store)
        view.scrollController = scrollController
        hostAnchorTracker = KsBridgeHostAnchorTracker(view)
        // 戻しは命令と同じ待ち行列に積まれるため、ここで積めば Host 生成後に届く命令より先に処理される。
        scrollAnchor?.let {
            view.restoreScrollAnchor(it)
            scrollAnchor = null
        }
        hostView = view
        return view
    }

    /**
     * Native Host だけを解放し、Store（設定ツリーと Theme）は維持する（maui/ADR-0007）。
     *
     * 解放時に旧 Host の Store 購読を解除して無効化するため、解放後に Store へ適用した更新は
     * 旧 Host の表示に反映されない。旧 Host の view 階層からの取り外しと参照の破棄は呼び出し側の
     * 責務であり、解放後の Bridge は旧 Host とその `Context` への参照を持たない。
     *
     * root の header / footer は Store ではなく Host が持つプロパティのため、解放とともに失われる。
     * 再生成した Host へ引き継ぐ場合は、呼び出し側が値を保持して [updateAccessory] で再適用する。
     *
     * 解放の前に Host の表示位置（表示範囲の上端にかかる要素と、そこからのずれ）を控え、次に生成する
     * Host で戻す。Host が既に window から外れていれば、外れる直前に表示していた位置を控える。
     * 控えられる内容が無い Host（一度も行を配置していないなど）を解放したときは控えを持たず、
     * 次の Host は内容の先頭から表示する。解放した Host へはスクロール命令が届かなくなる。
     *
     * 冪等であり、Host 不在時（未生成・解放済み）および破棄済みの Bridge では no-op になる。
     */
    fun releaseHost() {
        if (isDisposed) return
        val view = hostView ?: return
        val tracker = hostAnchorTracker
        scrollAnchor = tracker?.anchorForRelease() ?: view.captureScrollAnchor()
        tracker?.stop()
        hostAnchorTracker = null
        // Store からの切断はスクロール命令ハンドルの接続も外す。
        view.unbind()
        hostView = null
    }

    // MARK: - lifecycle

    /**
     * Bridge を破棄する。冪等であり、破棄後の操作 API と Host 生成は no-op になる。
     *
     * 破棄と同時に [interactionListener] を解除するため、破棄後のユーザー操作は通知されない。
     * 控えていた表示位置も捨て、保持し続けている Host へもスクロール命令は届かなくなる。
     */
    fun dispose() {
        if (isDisposed) return
        isDisposed = true
        hostView?.scrollController = null
        hostAnchorTracker?.stop()
        hostAnchorTracker = null
        hostView = null
        scrollAnchor = null
        interactionRelay.listener = null
    }

    // MARK: - Root 全体操作

    /**
     * Builder が組み立てた設定ツリーで root を全置換する。
     *
     * @param builder 設定ツリーの Builder
     */
    fun setRoot(builder: KsBridgeRootBuilder) {
        if (isDisposed) return
        store.replaceAll(builder.makeRoot(interactionRelay))
    }

    // MARK: - Section 操作

    /**
     * Section を指定 index へ挿入する。index は model 配列上の位置で、範囲外は端へ丸められる。
     *
     * @param section 挿入する Section DTO
     * @param index 挿入位置
     * @return 挿入した Section の sectionID。破棄済みの Bridge では `null`
     */
    fun insertSection(section: KsBridgeSection, index: Int): String? {
        if (isDisposed) return null
        store.insertSection(section.makeSection(interactionRelay), at = index)
        return section.sectionID
    }

    /**
     * 指定 ID の Section を削除する。未知の ID は no-op。
     *
     * @param sectionID 対象 Section の sectionID
     */
    fun removeSection(sectionID: String) {
        if (isDisposed) return
        val id = KsBridgeIdentifier.canonical(sectionID) ?: return
        store.removeSection(sectionId = id)
    }

    /**
     * Section の順序を変更する。index は model 配列上の位置で、範囲外の移動先は端へ丸められる。
     *
     * @param from 移動元 index
     * @param to 移動先 index
     */
    fun moveSection(from: Int, to: Int) {
        if (isDisposed) return
        store.moveSection(from = from, to = to)
    }

    /**
     * 指定 ID の Section の内容を置換し、置換後も有効な sectionID を返す。未知の ID は no-op。
     *
     * 置換後も Section の identity は [sectionID] のまま保たれる。[newSection] 自身が採番した
     * `sectionID` は破棄されるため、以後の操作には戻り値の ID を使う。Section 内の Cell は
     * DTO が持つ ID で作り直されるため、既存 Cell の ID を温存したい場合は Cell DTO に
     * [KsBridgeCell.adoptCellID] で採番済みの ID を引き継がせてから渡す。
     *
     * @param sectionID 対象 Section の sectionID
     * @param newSection 置換後の内容
     * @return 置換後も有効な sectionID（対象と同じ ID）。破棄済み、または対象 Section が
     *   存在しない場合は `null`（no-op）
     */
    fun replaceSection(sectionID: String, newSection: KsBridgeSection): String? {
        if (isDisposed) return null
        val id = KsBridgeIdentifier.canonical(sectionID) ?: return null
        if (store.state.value.sections.none { it.id == id }) return null
        store.replaceSection(sectionId = id, new = newSection.makeSection(interactionRelay, id = id))
        return id
    }

    // MARK: - Cell 操作

    /**
     * 指定 Section の指定 index へ Cell を挿入する。index は model 配列上の位置で、
     * 範囲外は端へ丸められる。
     *
     * @param cell 挿入する Cell DTO
     * @param sectionID 挿入先 Section の sectionID
     * @param index 挿入位置
     * @return 挿入した Cell の cellID。破棄済み、または Section が存在しない場合は `null`（no-op）
     */
    fun insertCell(cell: KsBridgeCell, sectionID: String, index: Int): String? {
        if (isDisposed) return null
        val id = KsBridgeIdentifier.canonical(sectionID) ?: return null
        if (store.state.value.sections.none { it.id == id }) return null
        store.insertCell(cell.makeCell(interactionRelay), sectionId = id, at = index)
        return cell.cellID
    }

    /**
     * 指定 ID の Cell を削除する。未知の ID は no-op。
     *
     * @param cellID 対象 Cell の cellID
     */
    fun removeCell(cellID: String) {
        if (isDisposed) return
        val id = KsBridgeIdentifier.canonical(cellID) ?: return
        store.removeCell(cellId = id)
    }

    /**
     * 指定 ID の Cell を同一 Section 内で移動する。未知の ID は no-op。
     * index は model 配列上の位置で、範囲外は端へ丸められる。
     *
     * @param cellID 対象 Cell の cellID
     * @param index 移動先 index
     */
    fun moveCell(cellID: String, index: Int) {
        if (isDisposed) return
        val id = KsBridgeIdentifier.canonical(cellID) ?: return
        store.moveCell(cellId = id, to = index)
    }

    /**
     * 指定 ID の Cell の内容を置換し、置換後も有効な cellID を返す。未知の ID は no-op。
     *
     * 置換後も行の identity は [cellID] のまま保たれ、行の削除と挿入としては扱われない。
     * [newCell] 自身が採番した `cellID` は破棄されるため、以後の操作には戻り値の ID を使う。
     *
     * @param cellID 対象 Cell の cellID
     * @param newCell 置換後の内容
     * @return 置換後も有効な cellID（対象と同じ ID）。破棄済み、または対象 Cell が存在しない
     *   場合は `null`（no-op）
     */
    fun replaceCell(cellID: String, newCell: KsBridgeCell): String? {
        if (isDisposed) return null
        val id = KsBridgeIdentifier.canonical(cellID) ?: return null
        if (store.state.value.sections.none { section -> section.cells.any { it.id == id } }) {
            return null
        }
        store.replaceCell(cellId = id, new = newCell.makeCell(id, interactionRelay))
        return id
    }

    /**
     * 複数 Cell の内容をまとめて置換し、1 回のバッチ内容更新として反映する。
     *
     * 更新は入力順に適用され、未知の ID は無視される。適用が 0 件のときは状態も表示も変化しない。
     * 各更新は同じ ID の内容更新であり、行の identity を変えない。可視性を変える更新は
     * バッチではなく [replaceCell] で行う。
     *
     * @param updates （対象 cellID, 置換後の内容）の並び
     */
    fun replaceCells(updates: List<KsBridgeCellUpdate>) {
        if (isDisposed) return
        val resolved: List<Pair<String, Cell>> = updates.mapNotNull { update ->
            val id = KsBridgeIdentifier.canonical(update.cellID) ?: return@mapNotNull null
            id to update.cell.makeCell(id, interactionRelay)
        }
        store.replaceCells(resolved)
    }

    // MARK: - Accessory / Theme 操作

    /**
     * Root / Section の header・footer に表示する text を更新する。
     *
     * [text] が `null` のときは accessory を解除し、accessory が指定されていない場合と同じ表示に戻す。
     * [KsBridgeAccessoryTarget.SectionHeader] / [KsBridgeAccessoryTarget.SectionFooter] を
     * 指定するときは [sectionID] が必須で、canonical UUID 文字列として解釈できない場合は no-op になる。
     * canonical UUID でも Store の現在状態に存在しない sectionID は Store 側で no-op になり、
     * 状態・表示・通知は変化しない（core/ADR-0020）。
     *
     * Section 対象の text は Store の状態に保存され Host 再生成後も復元されるが、root 対象の
     * text は Store ではなく Host が持つため、[releaseHost] 後の再生成には引き継がれない —
     * 引き継ぐ場合は呼び出し側が値を保持して再適用する。生成済みの Host に対しては、view 階層への
     * 取り付け前に渡した root 対象の値も失われず、最初の表示に含まれる。
     *
     * @param target 更新対象
     * @param sectionID Section を対象にするときの sectionID（root 対象では参照しない）
     * @param text 表示する text（`null` で解除）
     */
    fun updateAccessory(target: KsBridgeAccessoryTarget, sectionID: String?, text: String?) {
        if (isDisposed) return
        when (target) {
            KsBridgeAccessoryTarget.RootHeader -> store.updateAccessory(
                target = AccessoryTarget.RootHeader,
                accessory = rootAccessory(text),
            )

            KsBridgeAccessoryTarget.RootFooter -> store.updateAccessory(
                target = AccessoryTarget.RootFooter,
                accessory = rootAccessory(text),
            )

            KsBridgeAccessoryTarget.SectionHeader -> {
                val id = KsBridgeIdentifier.canonical(sectionID) ?: return
                store.updateAccessory(
                    target = AccessoryTarget.SectionHeader(sectionId = id),
                    accessory = sectionAccessory(text),
                )
            }

            KsBridgeAccessoryTarget.SectionFooter -> {
                val id = KsBridgeIdentifier.canonical(sectionID) ?: return
                store.updateAccessory(
                    target = AccessoryTarget.SectionFooter(sectionId = id),
                    accessory = sectionAccessory(text),
                )
            }
        }
    }

    /**
     * Root / Section の header・footer に表示する View を更新する。
     *
     * [view] が `null` のときは accessory を解除し、accessory が指定されていない場合と同じ表示に
     * 戻す。渡した View は取り付け直前に既存の親から切り離されるため、同じインスタンスが
     * リサイクル等で再び取り付けられても失敗しない。
     *
     * 対象の指定と未知 sectionID の扱いは [updateAccessory] と同一で、Section 対象の View は
     * Store の状態に保存され Host 再生成後も復元されるが、root 対象の View は Host が持つため
     * 引き継がれない — 引き継ぐ場合は呼び出し側が値を保持して再適用する。生成済みの Host に
     * 対しては、view 階層への取り付け前に渡した root 対象の View も失われず、最初の表示に含まれる。
     *
     * @param target 更新対象
     * @param sectionID Section を対象にするときの sectionID（root 対象では参照しない）
     * @param view 表示する View（`null` で解除）
     */
    fun updateAccessoryView(target: KsBridgeAccessoryTarget, sectionID: String?, view: View?) {
        if (isDisposed) return
        val anyView = view?.let { KsBridgeAccessoryView.anyView(it) }
        when (target) {
            KsBridgeAccessoryTarget.RootHeader -> store.updateAccessory(
                target = AccessoryTarget.RootHeader,
                accessory = anyView?.let { SettingsAccessory.Root(RootAccessory.View(it)) },
            )

            KsBridgeAccessoryTarget.RootFooter -> store.updateAccessory(
                target = AccessoryTarget.RootFooter,
                accessory = anyView?.let { SettingsAccessory.Root(RootAccessory.View(it)) },
            )

            KsBridgeAccessoryTarget.SectionHeader -> {
                val id = KsBridgeIdentifier.canonical(sectionID) ?: return
                store.updateAccessory(
                    target = AccessoryTarget.SectionHeader(sectionId = id),
                    accessory = anyView?.let { SettingsAccessory.Section(SectionAccessory.View(it)) },
                )
            }

            KsBridgeAccessoryTarget.SectionFooter -> {
                val id = KsBridgeIdentifier.canonical(sectionID) ?: return
                store.updateAccessory(
                    target = AccessoryTarget.SectionFooter(sectionId = id),
                    accessory = anyView?.let { SettingsAccessory.Section(SectionAccessory.View(it)) },
                )
            }
        }
    }

    /**
     * 表示中の accessory 領域の高さを測り直すよう要求する。
     *
     * view accessory の中身が自分の計測結果を変えたときに呼ぶ。一過性の要求であり Store の状態は
     * 変化しない。対象が表示されていないとき、および固定高さの Section header では表示が変わらない。
     *
     * @param target 再計測する accessory
     * @param sectionID Section を対象にするときの sectionID（root 対象では参照しない）
     */
    fun invalidateAccessoryMeasurement(target: KsBridgeAccessoryTarget, sectionID: String?) {
        if (isDisposed) return
        when (target) {
            KsBridgeAccessoryTarget.RootHeader ->
                store.invalidateAccessoryMeasurement(AccessoryTarget.RootHeader)

            KsBridgeAccessoryTarget.RootFooter ->
                store.invalidateAccessoryMeasurement(AccessoryTarget.RootFooter)

            KsBridgeAccessoryTarget.SectionHeader -> {
                val id = KsBridgeIdentifier.canonical(sectionID) ?: return
                store.invalidateAccessoryMeasurement(AccessoryTarget.SectionHeader(sectionId = id))
            }

            KsBridgeAccessoryTarget.SectionFooter -> {
                val id = KsBridgeIdentifier.canonical(sectionID) ?: return
                store.invalidateAccessoryMeasurement(AccessoryTarget.SectionFooter(sectionId = id))
            }
        }
    }

    /**
     * Theme を適用する。同値の Theme を再指定した場合は更新が通知されない。
     *
     * @param theme 輸送 DTO の Theme
     */
    fun setTheme(theme: KsBridgeTheme) {
        if (isDisposed) return
        store.applyTheme(theme.resolve())
    }

    // MARK: - 見た目スタイル

    /**
     * 見た目スタイルを適用する。
     *
     * スタイルは Store を経由せず Host のプロパティへ直接適用する — Native 側でもスタイルは
     * Store の管理外にあり、この操作はスクロール命令と並んで Store 公開操作との 1 対 1
     * （maui/ADR-0002）の枠外になる（maui/ADR-0023）。Host 未生成のときは値を控え、次の Host 生成時に適用する。
     *
     * @param style 見た目スタイルの序数（Classic = 0 / Modern = 1）。定義域外は Classic
     */
    fun setStyle(style: Int) {
        if (isDisposed) return
        val resolved = KsBridgeStyle.style(style)
        this.style = resolved
        hostView?.style = resolved
    }

    // MARK: - スクロール命令

    /**
     * 指定 ID の Cell の行へスクロールする。
     *
     * Store を経由しない更新 API で、[setStyle] と同じく Store 公開操作との 1 対 1 の枠外にある。
     * 命令は、同じ処理の中で行った更新が表示に反映された後に実行される。非表示の Cell・未知の ID・
     * canonical UUID として解釈できない ID、Host が無いとき、および破棄済みの Bridge では何もしない。
     *
     * @param cellID 対象 Cell の cellID
     * @param position 行を表示範囲のどこへ合わせるか（Start = 0 / Center = 1 / End = 2）。定義域外は Start
     * @param animated アニメーションするか
     */
    fun scrollToCell(cellID: String, position: Int, animated: Boolean) {
        if (isDisposed) return
        val id = KsBridgeIdentifier.canonical(cellID) ?: return
        scrollController.scrollTo(id, KsBridgeScrollPosition.position(position), animated)
    }

    /**
     * 指定 ID の Section へ、見出しごと見えるようにスクロールする。
     *
     * 何もしない条件は [scrollToCell] と同じ。
     *
     * @param sectionID 対象 Section の sectionID
     * @param position Section の範囲を表示範囲のどこへ合わせるか（Start = 0 / Center = 1 / End = 2）。
     *   定義域外は Start
     * @param animated アニメーションするか
     */
    fun scrollToSection(sectionID: String, position: Int, animated: Boolean) {
        if (isDisposed) return
        val id = KsBridgeIdentifier.canonical(sectionID) ?: return
        scrollController.scrollToSection(id, KsBridgeScrollPosition.position(position), animated)
    }

    /**
     * 内容の最上端（Root Header を含む）へスクロールする。Host が無いときと破棄済みの Bridge では
     * 何もしない。
     *
     * @param animated アニメーションするか
     */
    fun scrollToStart(animated: Boolean) {
        if (isDisposed) return
        scrollController.scrollToStart(animated)
    }

    /**
     * 内容の最下端（Root Footer を含む）へスクロールする。Host が無いときと破棄済みの Bridge では
     * 何もしない。
     *
     * @param animated アニメーションするか
     */
    fun scrollToEnd(animated: Boolean) {
        if (isDisposed) return
        scrollController.scrollToEnd(animated)
    }

    // MARK: - 内部ヘルパ

    /** Root 対象の accessory を text から組み立てる。`null` は解除。 */
    private fun rootAccessory(text: String?): SettingsAccessory? =
        text?.let { SettingsAccessory.Root(RootAccessory.Text(it)) }

    /** Section 対象の accessory を text から組み立てる。`null` は解除。 */
    private fun sectionAccessory(text: String?): SettingsAccessory? =
        text?.let { SettingsAccessory.Section(SectionAccessory.Text(it)) }
}
