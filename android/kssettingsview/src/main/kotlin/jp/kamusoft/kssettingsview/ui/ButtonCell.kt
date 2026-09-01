package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.CellTitleAlignment
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell

/**
 * ボタン用途の Cell。
 *
 * `title` をボタンスタイル表示する。Disclosure Indicator は表示しない。
 * `titleColor` を指定するとボタンテキストの色を上書きする。
 *
 * `titleAlignment` でタイトルの水平方向の揃え位置を指定する（既定 `CellTitleAlignment.CENTER`）。
 *
 * 共通フィールドのうち `valueText` / `icon` / `hintText` を持つ。
 * **`description` は意図的に持たない**（`AiForms.Maui.SettingsView` の `ButtonCell` が
 * `Description` を `private new` で隠蔽している挙動と揃える）。`icon` / `valueText` / `hintText` が
 * すべて `null` のときはボタンスタイル（`titleAlignment` を Cell 全体に反映）、いずれか指定時は
 * 通常レイアウト（title 列内 `titleAlignment` 反映）に切り替える。
 *
 * 等価性判定からは `onTap` を除く（関数値は再構築のたびに別インスタンスになり、内容変化の
 * 誤検出を招くため）。`titleAlignment` / `isEnabled` は判定に含める。
 *
 * `titleColor` は Compose の `Color?` を直接受け取る（core/ADR-0009）。
 */
public data class ButtonCell(
    override val id: String = "button-${java.util.UUID.randomUUID()}",
    override val style: CellStyle = CellStyle(),
    val title: String,
    val valueText: String? = null,
    val icon: KsImage? = null,
    val hintText: String? = null,
    val titleColor: Color? = null,
    val onTap: (() -> Unit)? = null,
    val titleAlignment: CellTitleAlignment = CellTitleAlignment.CENTER,
    val isEnabled: Boolean = true,
    override val isVisible: Boolean = true,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell, DSLIconModifiableCell, VisibilityAware {
    override fun withDSLId(newId: String): Cell = copy(id = newId)
    override fun withDSLStyle(newStyle: CellStyle): Cell = copy(style = newStyle)
    override fun withDSLIcon(newIcon: KsImage?): Cell = copy(icon = newIcon)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ButtonCell) return false
        return id == other.id &&
            style == other.style &&
            title == other.title &&
            valueText == other.valueText &&
            icon == other.icon &&
            hintText == other.hintText &&
            titleColor == other.titleColor &&
            titleAlignment == other.titleAlignment &&
            isEnabled == other.isEnabled &&
            isVisible == other.isVisible
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (valueText?.hashCode() ?: 0)
        result = 31 * result + (icon?.hashCode() ?: 0)
        result = 31 * result + (hintText?.hashCode() ?: 0)
        result = 31 * result + (titleColor?.hashCode() ?: 0)
        result = 31 * result + titleAlignment.hashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + isVisible.hashCode()
        return result
    }
}
