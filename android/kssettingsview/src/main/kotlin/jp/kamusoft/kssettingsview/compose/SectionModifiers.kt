package jp.kamusoft.kssettingsview.compose

/**
 * `DSLSettingsRootScope` の中で **直前に追加された Section** に明示 ID を指定する DSL 拡張関数。
 *
 * 利用例（スコープ関数形式）:
 * ```kotlin
 * KsSettingsView {
 *     Section { LabelCell("動的Section") }
 *     sectionID("dynamic-section-1") // 直前 Section に対する明示 ID
 * }
 * ```
 *
 * # 推奨：chain 形式
 *
 * `Section(...)` は [SectionHandle] を返すため、iOS の `.sectionID(_:)` と並列な chain 形式が
 * **正規 API** である：
 *
 * ```kotlin
 * KsSettingsView {
 *     Section { LabelCell("動的Section") }.sectionID("dynamic-section-1")
 * }
 * ```
 *
 * 新規記述では [SectionHandle.sectionID] の chain 形式を推奨する。本スコープ関数形式は
 * 動作が等価な別記法として併存する。
 */
public fun DSLSettingsRootScope.sectionID(id: Any) {
    overrideLastSectionId(DSLIdentityHint.Explicit(id))
}

/**
 * `DSLSectionScope` の中で **直前に追加された Cell** に明示 ID を指定する DSL 拡張関数。
 *
 * # 推奨：chain 形式
 *
 * `cell(...)` は [CellHandle] を返すため、iOS の `.cellID(_:)` と並列な chain 形式が
 * **正規 API** である：
 *
 * ```kotlin
 * Section {
 *     cell(MyCell(...)).cellID("my-cell-1")
 * }
 * ```
 *
 * 新規記述では [CellHandle.cellID] の chain 形式を推奨する。本スコープ関数形式は
 * 動作が等価な別記法として併存する。
 */
public fun DSLSectionScope.cellID(id: Any) {
    overrideLastCellId(DSLIdentityHint.Explicit(id))
}
