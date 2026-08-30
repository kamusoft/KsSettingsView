// KsSettingsViewBuilder.swift
// KsSettingsViewSwiftUI
//
// `KsSettingsView { Section { Cell... } ... }` の DSL ルート用 `@resultBuilder`。
//
// 本ビルダーの Component 型は `[DSLSectionNode]` であり、DSL ビルド中に Section 単位の
// 安定 ID ヒント（ForEach の `item.id` / `.sectionID(_:)` 等）を Node に保持しながら
// 集約する。後段 `DSLRootTree.resolvedSections()` で安定 ID を解決した
// `[KsSettingsViewCore.Section]` に変換される。
//
// 既存 `extension Section.init(_:cells:)` / `extension Section.sectionID(_:)` 等は
// `KsSettingsViewCore.Section` を返すため、`buildExpression(_ section: Section)` で
// `Section` 型から `DSLSectionNode` に **自動昇格** する。昇格時に DSL ヒントレジストリから
// `section.id` / `cell.id` をキーにヒント（ForEach の item.id / 明示 .sectionID / .cellID）を
// 引き出し、Node に転写する。これにより `Section("...") { Cell() }` という直感的な記法を
// 維持しつつ、安定 ID パイプライン（Node 経由）に乗せることができる（core/ADR-0008）。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore

/// `KsSettingsView { ... }` DSL のルート用 result builder。
///
/// Component 型は `[DSLSectionNode]` で、安定 ID 解決前の中間表現を集約する。
@resultBuilder
public struct KsSettingsViewBuilder {

    /// 1 個の `DSLSectionNode` を `Component`（`[DSLSectionNode]`）に変換する。
    public static func buildExpression(_ expression: DSLSectionNode) -> [DSLSectionNode] {
        return [expression]
    }

    /// 独自 `ForEach` 戻り値（`[DSLSectionNode]`）をそのまま `Component` として扱う。
    public static func buildExpression(_ expression: [DSLSectionNode]) -> [DSLSectionNode] {
        return expression
    }

    /// 既存 `KsSettingsViewCore.Section` 値を DSL ノードへ自動昇格する。
    /// `Section("見出し") { Cell() }` という既存 DSL 記法を維持しつつ、Node 経路に乗せる。
    public static func buildExpression(
        _ expression: KsSettingsViewCore.Section
    ) -> [DSLSectionNode] {
        return [promoteToNode(expression)]
    }

    /// 旧 builder（`SettingsRootBuilder` / `ForEach<...> -> [Section]`）戻り値の `[Section]`
    /// 配列を Node 列に自動昇格する。
    public static func buildExpression(
        _ expression: [KsSettingsViewCore.Section]
    ) -> [DSLSectionNode] {
        return expression.map { promoteToNode($0) }
    }

    /// 各子要素を flat に連結する。
    public static func buildBlock(_ components: [DSLSectionNode]...) -> [DSLSectionNode] {
        return components.flatMap { $0 }
    }

    /// `for` ループ展開
    public static func buildArray(_ components: [[DSLSectionNode]]) -> [DSLSectionNode] {
        return components.flatMap { $0 }
    }

    /// `if` 単体（else なし）
    public static func buildOptional(_ component: [DSLSectionNode]?) -> [DSLSectionNode] {
        return component ?? []
    }

    /// `if/else` 両方
    public static func buildEither(first component: [DSLSectionNode]) -> [DSLSectionNode] {
        return component
    }

    public static func buildEither(second component: [DSLSectionNode]) -> [DSLSectionNode] {
        return component
    }

    // MARK: - Section → DSLSectionNode 昇格

    /// `Section` 値型と DSL ヒントレジストリの内容から `DSLSectionNode` を構築する。
    ///
    /// 1. `section.id` をキーにレジストリから Section ヒント（`.forEach(itemID)` / `.explicit(...)`）を取り出す
    /// 2. 各 `section.cells[i].id` をキーにレジストリから Cell ヒントを取り出して `DSLCellNode` を構築
    /// 3. これらを束ねた `DSLSectionNode` を返す
    internal static func promoteToNode(
        _ section: KsSettingsViewCore.Section
    ) -> DSLSectionNode {
        let sectionHint = DSLHintRegistry.shared.sectionHint(for: section.id)
        let cellNodes: [DSLCellNode] = section.cells.map { cell in
            let cellHint = DSLHintRegistry.shared.cellHint(for: cell.id)
            return DSLCellNode(cell: cell, identityHint: cellHint)
        }
        return DSLSectionNode(
            section: section,
            identityHint: sectionHint,
            cellNodes: cellNodes
        )
    }
}

/// `Section { Cell... }` の DSL 内用 result builder。
///
/// 旧 `@SectionBuilder` と同じ Component 型 `[any KsCell]` を維持する。
/// Cell ヒント（ForEach の item.id / `.cellID(_:)`）は DSL ヒントレジストリに記録され、
/// 親 `KsSettingsViewBuilder.promoteToNode(_:)` で `Section` → `DSLSectionNode` 昇格時に
/// 各 Cell の `DSLCellNode` に転写される。
///
/// 新規 `KsSectionBuilder` として導入するのは、将来 Cell ノード経路を直接受け付ける
/// DSL（`.modifier` チェーンを Node ベースで持つ等）の拡張余地を残すため。
@resultBuilder
public struct KsSectionBuilder {

    /// 1 個の `any KsCell` を `Component`（`[any KsCell]`）に変換する。
    public static func buildExpression(_ expression: any KsCell) -> [any KsCell] {
        return [expression]
    }

    /// 独自 `ForEach` 戻り値（`[any KsCell]`）をそのまま `Component` として扱う。
    public static func buildExpression(_ expression: [any KsCell]) -> [any KsCell] {
        return expression
    }

    /// 各子要素を flat に連結する。
    public static func buildBlock(_ components: [any KsCell]...) -> [any KsCell] {
        return components.flatMap { $0 }
    }

    /// `for` ループ展開
    public static func buildArray(_ components: [[any KsCell]]) -> [any KsCell] {
        return components.flatMap { $0 }
    }

    /// `if` 単体（else なし）
    public static func buildOptional(_ component: [any KsCell]?) -> [any KsCell] {
        return component ?? []
    }

    /// `if/else` 両方
    public static func buildEither(first component: [any KsCell]) -> [any KsCell] {
        return component
    }

    public static func buildEither(second component: [any KsCell]) -> [any KsCell] {
        return component
    }
}
#endif
