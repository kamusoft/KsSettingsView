package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell

/**
 * チェックボックス Cell。
 *
 * 右端にチェックマーク（accent カラー）を表示し、Cell 全体のタップで toggle する。
 * `AiForms.Maui.SettingsView` と同じく `isChecked` は TwoWay バインディング相当。
 *
 * 共通フィールドとして `description` / `valueText` / `icon` / `hintText` を持つ。
 * `accentColor` は Compose の `Color?` を直接受け取る（core/ADR-0009）。
 */
public data class CheckboxCell(
    override val id: String = "checkbox-${java.util.UUID.randomUUID()}",
    override val style: CellStyle = CellStyle(),
    val title: String,
    val description: String? = null,
    val valueText: String? = null,
    val icon: KsImage? = null,
    val hintText: String? = null,
    val isChecked: Boolean = false,
    val accentColor: Color? = null,
    val onValueChanged: ((Boolean) -> Unit)? = null,
    val isEnabled: Boolean = true,
    override val isVisible: Boolean = true,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell, DSLIconModifiableCell, VisibilityAware {
    override fun withDSLId(newId: String): Cell = copy(id = newId)
    override fun withDSLStyle(newStyle: CellStyle): Cell = copy(style = newStyle)
    override fun withDSLIcon(newIcon: KsImage?): Cell = copy(icon = newIcon)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CheckboxCell) return false
        return id == other.id &&
            style == other.style &&
            title == other.title &&
            description == other.description &&
            valueText == other.valueText &&
            icon == other.icon &&
            hintText == other.hintText &&
            isChecked == other.isChecked &&
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
        result = 31 * result + isChecked.hashCode()
        result = 31 * result + (accentColor?.hashCode() ?: 0)
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + isVisible.hashCode()
        return result
    }
}
