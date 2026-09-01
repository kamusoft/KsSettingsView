package jp.kamusoft.kssettingsview.ui

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell

/**
 * 読み取り専用の表示用 Cell。
 *
 * `title` を必須、`description` / `valueText` / `icon` / `hintText` は任意。
 * `data class` の自動 `equals` / `hashCode` を採用する。
 *
 * `DSLReidentifiableCell` / [DSLStyleModifiableCell] / [DSLIconModifiableCell] 規約に
 * 準拠することで、Compose DSL 経路で `id` / `style` / `icon` の rebind が可能になる。
 *
 * `style: CellStyle` は UI 層 [CellStyle] として各 Cell が個別に保持する（Core の `Cell` 抽象は
 * `style` を要求しない）。`icon` の型も UI 層 [KsImage] を参照する（core/ADR-0009）。
 *
 * @property id 一意 ID（既定で `label-<random UUID>` を自動採番）
 * @property style Cell 個別スタイル（UI 層 [CellStyle]、既定 `CellStyle()`）
 * @property title タイトル（必須）
 * @property description 説明文（任意）
 * @property valueText 右側に表示する値文字列（任意）
 * @property icon アイコン（任意、UI 層 [KsImage]）
 * @property hintText ヒントテキスト（任意、右上）
 * @property isEnabled 有効／無効（既定 `true`）。`false` のときはテキスト色を `Theme.disabledTextColor` に置換
 */
data class LabelCell(
    override val id: String = "label-${java.util.UUID.randomUUID()}",
    override val style: CellStyle = CellStyle(),
    val title: String,
    val description: String? = null,
    val valueText: String? = null,
    val icon: KsImage? = null,
    val hintText: String? = null,
    val isEnabled: Boolean = true,
    override val isVisible: Boolean = true,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell, DSLIconModifiableCell, VisibilityAware {
    override fun withDSLId(newId: String): Cell = copy(id = newId)
    override fun withDSLStyle(newStyle: CellStyle): Cell = copy(style = newStyle)
    override fun withDSLIcon(newIcon: KsImage?): Cell = copy(icon = newIcon)
}
