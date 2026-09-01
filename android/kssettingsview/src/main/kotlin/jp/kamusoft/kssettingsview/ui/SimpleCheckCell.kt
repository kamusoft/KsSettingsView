package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell

/**
 * 単純チェック Cell。
 *
 * Android では accessory（右側）にオリジナル `SimpleCheckCellView.cs` 準拠の小さなチェックマーク
 * （[KsSimpleCheckView]、30x30dp 相当）を表示する。`CheckboxCell` との違いは
 * チェック表現で、`SimpleCheckCell` は手描きの軽量チェックマーク、`CheckboxCell` は
 * Material 風チェックボックスを用いる。
 * `AiForms.Maui.SettingsView` と同じく `isChecked` は OneWay 相当のため、タップ時に
 * [onValueChanged] を呼ぶのみ（実際の `isChecked` 更新は利用者責務）。
 *
 * 共通フィールドとして `description` / `valueText` / `icon` / `hintText` / `accentColor` を持つ。
 * `style` は UI 層所属の [CellStyle] を参照する（core/ADR-0009）。
 */
data class SimpleCheckCell(
    override val id: String = "simple-check-${java.util.UUID.randomUUID()}",
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
        if (other !is SimpleCheckCell) return false
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
