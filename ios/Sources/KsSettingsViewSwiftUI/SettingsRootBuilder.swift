// SettingsRootBuilder.swift
// KsSettingsViewSwiftUI
//
// `SettingsRoot { Section { ... } ... }` という宣言的構文を可能にする `@resultBuilder`。
//
// 注意:
//   利用側で `import SwiftUI` していた場合 `Section` が `SwiftUI.Section` と曖昧になる。
//   builder の引数 / 戻り型は `KsSettingsViewCore.Section` で完全修飾する。
//
//   result builder の `Component` 型は `[KsSettingsViewCore.Section]` で統一する。
//   - `buildExpression(_ expression: Section) -> [Section]` で 1 セクションを 1 要素配列に包む。
//   - `buildBlock(_ components: [Section]...) -> [Section]` で配列を flat に集約する。

import Foundation
import KsSettingsViewCore

/// SettingsRoot 内に `Section` を並べる宣言的構文を可能にする result builder。
///
/// 例:
/// ```swift
/// let root = SettingsRoot {
///     Section("一般") { /* Cells */ }
///     Section("高度") { /* Cells */ }
/// }
/// ```
@resultBuilder
public struct SettingsRootBuilder {
    /// 1 個の `Section` を `Component`（`[Section]`）に変換する。
    public static func buildExpression(
        _ expression: KsSettingsViewCore.Section
    ) -> [KsSettingsViewCore.Section] {
        return [expression]
    }

    /// `[Section]` 戻り値（動的生成済み配列）をそのまま `Component` として扱う。
    public static func buildExpression(
        _ expression: [KsSettingsViewCore.Section]
    ) -> [KsSettingsViewCore.Section] {
        return expression
    }

    /// 各子要素を flat に連結する。
    public static func buildBlock(
        _ components: [KsSettingsViewCore.Section]...
    ) -> [KsSettingsViewCore.Section] {
        return components.flatMap { $0 }
    }

    /// `for` ループ展開
    public static func buildArray(
        _ components: [[KsSettingsViewCore.Section]]
    ) -> [KsSettingsViewCore.Section] {
        return components.flatMap { $0 }
    }

    /// `if` 単体（else なし）
    public static func buildOptional(
        _ component: [KsSettingsViewCore.Section]?
    ) -> [KsSettingsViewCore.Section] {
        return component ?? []
    }

    /// `if/else` 両方
    public static func buildEither(
        first component: [KsSettingsViewCore.Section]
    ) -> [KsSettingsViewCore.Section] {
        return component
    }

    public static func buildEither(
        second component: [KsSettingsViewCore.Section]
    ) -> [KsSettingsViewCore.Section] {
        return component
    }
}

/// `SettingsRoot` を DSL で構築するための便利イニシャライザ群。
extension SettingsRoot {
    /// DSL から `SettingsRoot` を生成する（`sections` のみ）。
    /// - Parameter sections: `@SettingsRootBuilder` でビルドする `Section` 群
    ///
    /// 注: `SettingsRoot.theme` は `purify-core-extract-style-to-ui-layer` で削除されたため
    ///     Theme は View 側（`.theme(_:)` modifier）で受け取る。
    public init(
        @SettingsRootBuilder sections: () -> [KsSettingsViewCore.Section]
    ) {
        self.init(sections: sections())
    }
}
