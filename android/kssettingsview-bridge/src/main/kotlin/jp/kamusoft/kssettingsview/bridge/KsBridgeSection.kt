package jp.kamusoft.kssettingsview.bridge

import android.view.View
import jp.kamusoft.kssettingsview.core.Section

/**
 * Section を interop 境界で輸送する DTO。
 *
 * インスタンス生成時に Bridge が canonical UUID 文字列の [sectionID] を採番する
 * （maui/ADR-0005）。header / footer は text と View の両方を輸送でき、同じ位置に両方を
 * 指定した場合は View を表示する。
 *
 * [cells] は共通基底型 [KsBridgeCell] で保持するため、Cell 種の異なる DTO を混載できる。
 *
 * DTO は 1 インスタンスが 1 つの Section identity を表す。同じインスタンスを複数箇所へ追加すると
 * 同じ [sectionID] の Section が重複するため、Section ごとに新しいインスタンスを生成する。
 *
 * @property headerText ヘッダテキスト（`null` でヘッダなし）
 * @property footerText フッタテキスト（`null` でフッタなし）
 */
class KsBridgeSection @JvmOverloads constructor(
    var headerText: String? = null,
    var footerText: String? = null,
    cells: List<KsBridgeCell> = emptyList(),
) {

    /** Bridge が採番した canonical UUID 文字列の Section ID。 */
    val sectionID: String = KsBridgeIdentifier.make()

    /**
     * ヘッダに表示する View（`null` で View 指定なし）。
     *
     * 非 `null` のときは [headerText] より優先され、ヘッダにはこの View が表示される。
     */
    var headerView: View? = null

    /**
     * フッタに表示する View（`null` で View 指定なし）。
     *
     * 非 `null` のときは [footerText] より優先され、フッタにはこの View が表示される。
     */
    var footerView: View? = null

    /** 可視性フラグ。`false` の Section は header / footer / 配下 Cell ごと表示から除外される。 */
    var isVisible: Boolean = true

    /**
     * Header の表示トグル（core/ADR-0023）。`false` のとき内容があっても Header を表示しない。
     *
     * 内容が無い（`null` または空文字列）Header をトグルで表示させることはできない。
     */
    var isHeaderVisible: Boolean = true

    /** Footer の表示トグル（core/ADR-0023）。意味論は [isHeaderVisible] と対称。 */
    var isFooterVisible: Boolean = true

    /** ヘッダの固定高さ（論理単位、`null` で Native 既定の自動高さ）。 */
    var headerHeight: Double? = null

    private val mutableCells: MutableList<KsBridgeCell> = cells.toMutableList()

    /** Section 内の Cell 群（追加順のスナップショット）。 */
    val cells: List<KsBridgeCell>
        get() = mutableCells.toList()

    /**
     * Cell を末尾に追加し、Bridge が採番した cellID を返す。
     *
     * @param cell 追加する Cell DTO（Cell 種を問わない）
     * @return 追加した Cell の cellID
     */
    fun addCell(cell: KsBridgeCell): String {
        mutableCells.add(cell)
        return cell.cellID
    }

    /**
     * DTO の現在の内容から Native の `Section` を組み立てる。
     *
     * @param id 生成する Section の ID（既定は DTO 自身が採番した [sectionID]）
     * @param relay 配下 Cell のユーザー操作を転送する中継
     */
    @JvmSynthetic
    internal fun makeSection(relay: KsBridgeInteractionRelay, id: String = sectionID): Section =
        Section(
            id = id,
            header = KsBridgeAccessoryView.sectionAccessory(view = headerView, text = headerText),
            footer = KsBridgeAccessoryView.sectionAccessory(view = footerView, text = footerText),
            cells = mutableCells.map { it.makeCell(relay) },
            // 未指定の headerHeight は Native の Section 既定（自動高さ）をそのまま使う。
            headerHeight = headerHeight ?: Section(id = id).headerHeight,
            isVisible = isVisible,
            isHeaderVisible = isHeaderVisible,
            isFooterVisible = isFooterVisible,
        )
}
