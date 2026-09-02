package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell

/**
 * ラジオボタン Cell。
 *
 * 同一 [groupId] の RadioCell 群で単一選択を表現する。利用者は同グループ内の全 RadioCell に
 * 同じ [selectedValue] を設定する。タップで `onSelected(value)` を発火し、[selectedValue] の
 * 更新は SettingsRoot 側の責務であり、本 Cell は選択状態を自前で書き換えない。
 *
 * 共通フィールドとして `description` / `valueText` / `icon` / `hintText` / `accentColor` を持つ。
 * `accentColor` の解決順序は
 * `RadioCell.accentColor → CellStyle.accentColor → Theme.cellAccentColor → プラットフォーム既定`。
 * `style` は UI 層所属の [CellStyle] を参照する（core/ADR-0009）。
 */
public data class RadioCell(
    override val id: String = "radio-${java.util.UUID.randomUUID()}",
    override val style: CellStyle = CellStyle(),
    val title: String,
    val description: String? = null,
    val valueText: String? = null,
    val icon: KsImage? = null,
    val hintText: String? = null,
    val groupId: String,
    val value: String,
    val selectedValue: String,
    val accentColor: Color? = null,
    val onSelected: ((String) -> Unit)? = null,
    val isEnabled: Boolean = true,
    override val isVisible: Boolean = true,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell, DSLIconModifiableCell, VisibilityAware {
    override fun withDSLId(newId: String): Cell = copy(id = newId)
    override fun withDSLStyle(newStyle: CellStyle): Cell = copy(style = newStyle)
    override fun withDSLIcon(newIcon: KsImage?): Cell = copy(icon = newIcon)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RadioCell) return false
        return id == other.id &&
            style == other.style &&
            title == other.title &&
            description == other.description &&
            valueText == other.valueText &&
            icon == other.icon &&
            hintText == other.hintText &&
            groupId == other.groupId &&
            value == other.value &&
            selectedValue == other.selectedValue &&
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
        result = 31 * result + groupId.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + selectedValue.hashCode()
        result = 31 * result + (accentColor?.hashCode() ?: 0)
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + isVisible.hashCode()
        return result
    }
}
