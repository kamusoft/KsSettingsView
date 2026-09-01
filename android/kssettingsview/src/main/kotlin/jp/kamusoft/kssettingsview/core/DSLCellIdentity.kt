package jp.kamusoft.kssettingsview.core

/**
 * DSL から `id` 書き換えを許容する Cell が満たすべき規約。
 *
 * 具象 Cell（UI 層の `LabelCell` / `SwitchCell` 等）や Sample アプリ内の具象 Cell は
 * 本インターフェースを実装する規約とする。DSL は再評価のたびに宣言ツリーから
 * 安定 ID を解決し直すため、その ID を Cell 側へ反映する経路が要る
 * （core/ADR-0008）。
 *
 * # Core 層配置の理由
 *
 * 具象 Cell（UI 層）が DSL（Compose 層）を **直接参照** すると層の向きが逆流するため、
 * 両者の共通祖先である Core 層（`jp.kamusoft.kssettingsview.core` パッケージ）に配置する。
 * これにより `ui → core ← compose` の参照方向を保ったまま、DSL 経路の rebind 規約を
 * 具象 Cell が満たせるようになる。
 *
 * # スタイル関連規約との分担
 *
 * Core に置くのは ID 書き換え規約のみとする。スタイル合成経路の規約
 * （`DSLStyleModifiableCell`）は `CellStyle` とともに UI 層（`jp.kamusoft.kssettingsview.ui`
 * パッケージ）が持つ
 * （core/ADR-0009）。
 */
interface DSLReidentifiableCell : Cell {
    /**
     * 自身を copy し、新しい `id` を持つ Cell を返す。
     * 通常は `data class` の `copy(id = newId)` で実装する。
     */
    fun withDSLId(newId: String): Cell
}
