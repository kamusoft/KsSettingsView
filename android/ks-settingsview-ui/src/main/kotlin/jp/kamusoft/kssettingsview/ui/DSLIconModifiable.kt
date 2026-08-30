package jp.kamusoft.kssettingsview.ui

import jp.kamusoft.kssettingsview.core.Cell

/**
 * DSL 経路から `icon` 書き換えを許容する Cell が満たすべき規約（UI 層配置）。
 *
 * Compose DSL の `CellHandle.icon(_: KsImage)` modifier 経路を満たすために、UI 層で `icon` を
 * 持つ Cell が準拠するインターフェースとして定義する。
 *
 * `KsImage` は `ks-settingsview-ui` 所属の sealed interface であり、Core からは参照できないため
 * 本インターフェースも UI 層配置となる。`DSLStyleModifiableCell` と同様、`ks-settingsview-compose`
 * 層の DSL Modifier は `ks-settingsview-compose → ks-settingsview-ui` の依存を辿って参照する。
 *
 * `icon: KsImage?` を持つ Cell が準拠する。アイコン領域を持たない `CustomCell` は準拠せず、
 * その場合 `CellHandle.icon(_:)` modifier は no-op として扱う。
 */
interface DSLIconModifiableCell : Cell {
    /**
     * 自身を copy し、新しい `icon` を持つ Cell を返す。
     * @param newIcon 新しい `KsImage`（`null` でアイコンクリア）
     */
    fun withDSLIcon(newIcon: KsImage?): Cell
}
