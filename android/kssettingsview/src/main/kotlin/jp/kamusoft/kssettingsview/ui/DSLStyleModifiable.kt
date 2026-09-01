package jp.kamusoft.kssettingsview.ui

import jp.kamusoft.kssettingsview.core.Cell

/**
 * DSL から `CellStyle` 書き換えを許容する Cell が満たすべき規約（UI 層配置）。
 *
 * `CellStyle` が UI 層に属する（core/ADR-0009）ため、本インターフェースも Core ではなく
 * UI 層（`jp.kamusoft.kssettingsview.ui` パッケージ）に置く。Compose 層
 * （`jp.kamusoft.kssettingsview.compose` パッケージ）の DSL Modifier が本インターフェースを参照する。
 *
 * `Cell` の各具象 Cell は読み取り専用の `val style: CellStyle` を持つため、modifier から
 * `CellStyle` の各フィールドを書き換えるには「`style` のみ書き換えた自身の copy」を
 * 生成する API が必要。本インターフェースはその API を規定する。
 *
 * UI 層の基本 Cell（`LabelCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` /
 * `SimpleCheckCell` / `ButtonCell` / `CommandCell`）はすべて本インターフェースへ準拠する。
 *
 * Core の `Cell` は `style` を要求しないため、`style` 取得 API も本インターフェースが束ねて要求する。
 * これにより modifier 経路は `cell as? DSLStyleModifiableCell` 経由で `style` を取得できる。
 */
interface DSLStyleModifiableCell : Cell {
    /** Cell 個別の [CellStyle] を返す（読み取り専用）。 */
    val style: CellStyle

    /**
     * 自身を copy し、新しい `style` を持つ Cell を返す。
     * 通常は `data class` の `copy(style = newStyle)` で実装する。
     */
    fun withDSLStyle(newStyle: CellStyle): Cell
}
