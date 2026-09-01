package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell

/**
 * 候補リストから単一または複数の項目を選択する Cell。
 *
 * `selectionMode` で単一 / 複数を切替え、対応する binding（`selectedIndex` / `selectedIndices`）を
 * 使う。callback 経路は `onSelectionChanged`（単一）/ `onMultiSelectionChanged`（複数）を併設。
 *
 * 選択 UI はボトムシートで表示する（android/ADR-0005）。
 *
 * @property selectionMode 単一 / 複数 のモード切替（既定 [PickerSelectionMode.Single]）
 * @property items 選択候補のリスト（主表示 + 任意の副表示）
 * @property selectedIndex 単一選択モード時の選択 index（`null` で未選択）。
 *   `selectionMode == Multiple` のときは無視される。
 * @property selectedIndices 複数選択モード時の選択 index 集合。
 *   `selectionMode == Single` のときは無視される。
 * @property maxSelectedNumber 複数選択モードでの上限（既定 `0` = 無制限）
 * @property pageTitle モーダル画面のタイトル（任意）
 * @property accentColor 選択強調色（任意）
 * @property valueText 明示指定の valueText（`null` のとき現在の選択値から自動生成）
 * @property onSelectionChanged 単一選択モードでの選択変更 callback
 * @property onMultiSelectionChanged 複数選択モードでの選択変更 callback
 */
data class PickerCell(
    override val id: String = "picker-cell-${java.util.UUID.randomUUID()}",
    override val style: CellStyle = CellStyle(),
    val title: String,
    val description: String? = null,
    val valueText: String? = null,
    val icon: KsImage? = null,
    val hintText: String? = null,
    val items: List<PickerItem> = emptyList(),
    val selectionMode: PickerSelectionMode = PickerSelectionMode.Single,
    val selectedIndex: Int? = null,
    val selectedIndices: Set<Int> = emptySet(),
    val maxSelectedNumber: Int = 0,
    val pageTitle: String? = null,
    val accentColor: Color? = null,
    val onSelectionChanged: ((Int) -> Unit)? = null,
    val onMultiSelectionChanged: ((Set<Int>) -> Unit)? = null,
    val isEnabled: Boolean = true,
    override val isVisible: Boolean = true,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell, DSLIconModifiableCell, VisibilityAware {
    override fun withDSLId(newId: String): Cell = copy(id = newId)
    override fun withDSLStyle(newStyle: CellStyle): Cell = copy(style = newStyle)
    override fun withDSLIcon(newIcon: KsImage?): Cell = copy(icon = newIcon)

    /**
     * `valueText` が `null` のとき、現在の選択値から自動表示する文字列を生成する。
     *
     * 副表示（[PickerItem.subText]）は含めず、主表示だけで組み立てる。
     *
     * - `Single`: `items[selectedIndex].text`（範囲外・未選択なら空文字）
     * - `Multiple`: 選択中の項目の主表示を index 昇順に `, ` 連結（範囲外 index は除外）
     */
    internal fun autoValueText(): String =
        when (selectionMode) {
            PickerSelectionMode.Single -> {
                val idx = selectedIndex ?: return ""
                items.getOrNull(idx)?.text ?: ""
            }
            PickerSelectionMode.Multiple -> {
                selectedIndices.sorted()
                    .mapNotNull { items.getOrNull(it)?.text }
                    .joinToString(", ")
            }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickerCell) return false
        return id == other.id &&
            style == other.style &&
            title == other.title &&
            description == other.description &&
            valueText == other.valueText &&
            icon == other.icon &&
            hintText == other.hintText &&
            items == other.items &&
            selectionMode == other.selectionMode &&
            selectedIndex == other.selectedIndex &&
            selectedIndices == other.selectedIndices &&
            maxSelectedNumber == other.maxSelectedNumber &&
            pageTitle == other.pageTitle &&
            accentColor == other.accentColor &&
            isEnabled == other.isEnabled &&
            isVisible == other.isVisible
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (valueText?.hashCode() ?: 0)
        result = 31 * result + (icon?.hashCode() ?: 0)
        result = 31 * result + (hintText?.hashCode() ?: 0)
        result = 31 * result + items.hashCode()
        result = 31 * result + selectionMode.hashCode()
        result = 31 * result + (selectedIndex ?: -1)
        result = 31 * result + selectedIndices.hashCode()
        result = 31 * result + maxSelectedNumber
        result = 31 * result + (pageTitle?.hashCode() ?: 0)
        result = 31 * result + (accentColor?.hashCode() ?: 0)
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + isVisible.hashCode()
        return result
    }
}
