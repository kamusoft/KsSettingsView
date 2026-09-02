package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell

/**
 * ON/OFF スイッチを持つ Cell。
 *
 * 右側に `MaterialSwitch` を表示し、ユーザー操作で `onValueChanged(Boolean)` を発火する。
 * `accentColor` を指定すると ON 時のスイッチの色を変更できる。
 *
 * `accentColor` は Compose の [Color]? を直接受け取るため、利用者は `Color.Green` などの
 * 慣れた API で指定できる（core/ADR-0009）。
 *
 * 共通フィールドとして `description` / `valueText` / `icon` / `hintText` を持ち、
 * 全 Cell 共通レイアウト規約 `[icon][title / description][valueText][hintText][accessory]` に従う。
 */
public data class SwitchCell(
    override val id: String = "switch-${java.util.UUID.randomUUID()}",
    override val style: CellStyle = CellStyle(),
    val title: String,
    val description: String? = null,
    val valueText: String? = null,
    val icon: KsImage? = null,
    val hintText: String? = null,
    val isOn: Boolean = false,
    val accentColor: Color? = null,
    val onValueChanged: ((Boolean) -> Unit)? = null,
    val isEnabled: Boolean = true,
    override val isVisible: Boolean = true,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell, DSLIconModifiableCell, VisibilityAware {
    override fun withDSLId(newId: String): Cell = copy(id = newId)
    override fun withDSLStyle(newStyle: CellStyle): Cell = copy(style = newStyle)
    override fun withDSLIcon(newIcon: KsImage?): Cell = copy(icon = newIcon)

    /**
     * 等価性（値型としての性質）。クロージャ（[onValueChanged]）のみ除外し、内部状態 [isOn] / [isEnabled] /
     * [isVisible] を含むすべての保持フィールドを比較する。関数値は再構築のたびに別インスタンスに
     * なるため、含めると内容変化を誤検出する（core/ADR-0010）。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SwitchCell) return false
        return id == other.id &&
            style == other.style &&
            title == other.title &&
            description == other.description &&
            valueText == other.valueText &&
            icon == other.icon &&
            hintText == other.hintText &&
            isOn == other.isOn &&
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
        result = 31 * result + isOn.hashCode()
        result = 31 * result + (accentColor?.hashCode() ?: 0)
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + isVisible.hashCode()
        return result
    }
}
