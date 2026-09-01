package jp.kamusoft.kssettingsview.ui

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.SectionAccessory

/**
 * 平坦リスト用の項目型。
 *
 * `Section H/F + Cell` を `mainListAdapter`（[KsSettingsListAdapter]）へ単一の `List` で渡すために、
 * `sealed interface` で 3 つのサブタイプに分岐させる。Section を入れ子リストにせず平坦化することで、
 * 単一 `RecyclerView` + `ListAdapter` の差分計算に載せられる。
 *
 * 各サブタイプは所属 Section の [sectionId] を保持し、`ItemDecoration` の境界判定（前後参照）を
 * `O(1)` で行えるようにする。
 */
internal sealed interface CellListItem {

    /** 所属 Section の ID。`ItemDecoration` 等で前後の項目との境界判定に使用する。 */
    val sectionId: String

    /**
     * Section ヘッダ項目。
     *
     * @property sectionId 所属 Section の ID
     * @property accessory 表示する [SectionAccessory]（[SectionAccessory.Text] / [SectionAccessory.View]）
     * @property headerHeight `Section.headerHeight` に対応する固定高さ（既定 `-1.0` = 自動高さ）。
     *   accessory が Text か View かに依らず、bind 時に `applySectionHeaderHeight` が
     *   `itemView.layoutParams.height` へ適用する。
     */
    data class SectionHeader(
        override val sectionId: String,
        val accessory: SectionAccessory,
        val headerHeight: Double = -1.0,
    ) : CellListItem

    /**
     * Cell 項目。
     *
     * @property sectionId 所属 Section の ID
     * @property cell 描画対象の [Cell]
     */
    data class CellRow(
        override val sectionId: String,
        val cell: Cell,
    ) : CellListItem

    /**
     * Section フッタ項目。
     *
     * @property sectionId 所属 Section の ID
     * @property accessory 表示する [SectionAccessory]（[SectionAccessory.Text] / [SectionAccessory.View]）
     */
    data class SectionFooter(
        override val sectionId: String,
        val accessory: SectionAccessory,
    ) : CellListItem
}
