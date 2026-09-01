package jp.kamusoft.kssettingsview.compose

/**
 * DSL の `forEach` で **key を省略** したいデータ型が満たす marker interface。
 *
 * iOS 側 SwiftUI の `Identifiable` プロトコル + `ForEach(items)` シンタックスに対応する
 * Compose 側の最小マーカー。本マーカーを実装したデータ型を
 * [DSLSettingsRootScope.forEach] / [DSLSectionScope.forEach] に渡すと、`key` lambda を
 * 省略でき、内部で `it.id` を `key` として `DSLIdentityHint.ForEach(id)` に変換する。
 *
 * 利用例:
 * ```kotlin
 * data class DemoItem(override val id: Int, val name: String) : KsIdentifiable
 *
 * KsSettingsView {
 *     Section("動的 Section") {
 *         forEach(items) { item ->  // key 省略
 *             LabelCell(title = item.name)
 *         }
 *     }
 * }
 * ```
 */
public interface KsIdentifiable {
    /** ID 採番に用いる安定キー値。`Int` / `String` / `UUID` 等の `Any` を許容する。 */
    public val id: Any
}
