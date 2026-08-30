package jp.kamusoft.kssettingsview.ui

/**
 * Cell が `isVisible: Boolean` プロパティを公開する opt-in 抽象（UI 層）。
 *
 * UI 層のフィルタ層（`KsSettingsView.flatten` の visible projection 構築）は
 * `(cell as? VisibilityAware)?.isVisible ?: true` の形で問い合わせる。
 *
 * - Core 抽象 [jp.kamusoft.kssettingsview.core.Cell] にはこの要求を追加しない（薄い Cell 契約を
 *   保つため。core/ADR-0013）。
 * - UI 層が提供する Cell は [CustomCell] を含めてすべて opt-in 準拠する。
 * - 非準拠の Cell（ライブラリ利用者が独自定義した [jp.kamusoft.kssettingsview.core.Cell]
 *   実装型など）はフィルタの判定で常に visible（`true`）として扱われ、描画される。
 *
 * 可視性の切替は構造 Diff とは別経路の visible projection 再構築として扱う（core/ADR-0010）。
 */
interface VisibilityAware {
    /** Cell の可視性。`true` で UI 層の visible projection に含める、`false` で除外する。 */
    val isVisible: Boolean
}
