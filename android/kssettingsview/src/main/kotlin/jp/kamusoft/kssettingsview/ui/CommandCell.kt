package jp.kamusoft.kssettingsview.ui

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell

/**
 * タップで処理を実行する Cell。
 *
 * `LabelCell` のフィールドに加えて、`onTap` クロージャと `hideArrow` フラグを持つ。
 * `hideArrow` が `false`（既定）の場合は右端に Disclosure Indicator（右矢印）を表示する。
 *
 * # 等価性
 *
 * `data class` の自動 `equals` / `hashCode` を採用するが、`onTap` 関数型は等価性判定対象から
 * 除外したい（毎回新規ラムダが生成されると差分検出が暴発する）。Kotlin の `data class` では
 * 等価性判定対象から特定フィールドを除外できないため、`equals` / `hashCode` を **手動で**
 * 上書きする方針を取る。
 *
 * `style` / `icon` は UI 層所属の [CellStyle] / [KsImage] を参照する（core/ADR-0009）。
 */
public data class CommandCell(
    override val id: String = "command-${java.util.UUID.randomUUID()}",
    override val style: CellStyle = CellStyle(),
    val title: String,
    val description: String? = null,
    val valueText: String? = null,
    val icon: KsImage? = null,
    val hintText: String? = null,
    val hideArrow: Boolean = false,
    val onTap: (() -> Unit)? = null,
    val isEnabled: Boolean = true,
    override val isVisible: Boolean = true,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell, DSLIconModifiableCell, VisibilityAware {
    override fun withDSLId(newId: String): Cell = copy(id = newId)
    override fun withDSLStyle(newStyle: CellStyle): Cell = copy(style = newStyle)
    override fun withDSLIcon(newIcon: KsImage?): Cell = copy(icon = newIcon)

    // onTap を除外した equals / hashCode（isEnabled / isVisible は等価性判定に含める）
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommandCell) return false
        return id == other.id &&
            style == other.style &&
            title == other.title &&
            description == other.description &&
            valueText == other.valueText &&
            icon == other.icon &&
            hintText == other.hintText &&
            hideArrow == other.hideArrow &&
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
        result = 31 * result + hideArrow.hashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + isVisible.hashCode()
        return result
    }
}
