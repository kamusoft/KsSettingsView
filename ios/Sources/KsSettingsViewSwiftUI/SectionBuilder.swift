// SectionBuilder.swift
// KsSettingsViewSwiftUI
//
// `Section { Cell... }` という宣言的構文を可能にする `@resultBuilder`。
// `SettingsRootBuilder` と分離している（Section 内の cells は `[any KsCell]` を集めるため）。
//
// 注意:
//   - 利用側で `import SwiftUI` していた場合 `Section` が `SwiftUI.Section` と曖昧になる。
//     `KsSection` type alias、または `ksSection` トップレベル関数で曖昧解消する。
//   - result builder の `Component` 型は `[any KsCell]` で統一する。
//     - `buildExpression(_ expression: any KsCell) -> [any KsCell]` で 1 セルを 1 要素配列に包む。
//     - `buildBlock(_ components: [any KsCell]...) -> [any KsCell]` で配列を flat に集約する。
//     - これにより `if` / 配列展開 / オプショナル等にも一貫して対応できる。

import Foundation
import KsSettingsViewCore

/// SwiftUI 利用者が `import SwiftUI` した上で `Section` を使うと `SwiftUI.Section` と曖昧になる。
/// 本モジュールが提供する DSL では bare `Section` は `KsSettingsViewCore.Section` を指す。
/// 利用者には `KsSection` という曖昧解消用の型エイリアスも合わせて公開する。
public typealias KsSection = KsSettingsViewCore.Section

/// Section 内に Cell を並べる宣言的構文を可能にする result builder。
///
/// 例:
/// ```swift
/// Section("一般") {
///     LabelCell(...)
///     SwitchCell(...)
/// }
/// ```
@resultBuilder
public struct SectionBuilder {
    /// 1 個の `KsCell` 値を `Component`（`[any KsCell]`）に変換する。
    public static func buildExpression(_ expression: any KsCell) -> [any KsCell] {
        return [expression]
    }

    /// `[any KsCell]` 戻り値（例えば動的に生成した Cell 配列）をそのまま `Component` として扱う。
    public static func buildExpression(_ expression: [any KsCell]) -> [any KsCell] {
        return expression
    }

    /// 各子要素を flat に連結する（`Component` = `[any KsCell]`）。
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

// MARK: - DSL ファクトリ関数（曖昧性回避）
//
// `extension Section { init(...) }` の形は Swift のモジュール跨ぎの import / `@testable`
// 解決順によっては DSL init が見えなくなる場合がある（特に iOS シミュレータでの Xcode ビルド）。
// 同等の機能を持つトップレベル関数 `ksSection(_:footer:cells:)` を提供して回避する。
// 旧来の `Section("一般") { ... }` も `extension Section` の DSL init で引き続き利用可能。

/// 文字列ヘッダ（任意）+ DSL ベースの cells で `Section` を生成するトップレベル関数版。
/// 利用者が `import SwiftUI` していても `SwiftUI.Section` と曖昧にならない。
/// - Parameters:
///   - header: 文字列ヘッダ（`nil` でヘッダ非表示）
///   - footer: 文字列フッタ（`nil` でフッタ非表示）
///   - isHeaderVisible: Header 表示トグル（`false` で内容があっても Header を隠す）
///   - isFooterVisible: Footer 表示トグル（`false` で内容があっても Footer を隠す）
///   - cells: `@SectionBuilder` でビルドする Cell 群
public func ksSection(
    _ header: String? = nil,
    footer: String? = nil,
    headerHeight: Double = -1,
    isVisible: Bool = true,
    isHeaderVisible: Bool = true,
    isFooterVisible: Bool = true,
    @SectionBuilder cells: () -> [any KsCell]
) -> KsSettingsViewCore.Section {
    let headerAcc: SectionAccessory? = header.map { .text($0) }
    let footerAcc: SectionAccessory? = footer.map { .text($0) }
    return KsSettingsViewCore.Section(
        header: headerAcc,
        footer: footerAcc,
        cells: cells(),
        headerHeight: headerHeight,
        isVisible: isVisible,
        isHeaderVisible: isHeaderVisible,
        isFooterVisible: isFooterVisible
    )
}

/// `SectionAccessory` 直指定 + DSL ベースの cells で `Section` を生成するトップレベル関数版。
public func ksSection(
    header: SectionAccessory?,
    footer: SectionAccessory? = nil,
    headerHeight: Double = -1,
    isVisible: Bool = true,
    isHeaderVisible: Bool = true,
    isFooterVisible: Bool = true,
    @SectionBuilder cells: () -> [any KsCell]
) -> KsSettingsViewCore.Section {
    return KsSettingsViewCore.Section(
        header: header,
        footer: footer,
        cells: cells(),
        headerHeight: headerHeight,
        isVisible: isVisible,
        isHeaderVisible: isHeaderVisible,
        isFooterVisible: isFooterVisible
    )
}

/// `Section` を DSL で構築するための便利イニシャライザ群。
extension Section {
    /// 文字列ヘッダ（任意）+ DSL ベースの cells で `Section` を生成する。
    /// - Parameters:
    ///   - header: 文字列ヘッダ（`nil` でヘッダ非表示）
    ///   - footer: 文字列フッタ（`nil` でフッタ非表示）
    ///   - isHeaderVisible: Header 表示トグル（`false` で内容があっても Header を隠す）
    ///   - isFooterVisible: Footer 表示トグル（`false` で内容があっても Footer を隠す）
    ///   - cells: `@SectionBuilder` でビルドする Cell 群
    public init(
        _ header: String? = nil,
        footer: String? = nil,
        headerHeight: Double = -1,
        isVisible: Bool = true,
        isHeaderVisible: Bool = true,
        isFooterVisible: Bool = true,
        @SectionBuilder cells: () -> [any KsCell]
    ) {
        let headerAcc: SectionAccessory? = header.map { .text($0) }
        let footerAcc: SectionAccessory? = footer.map { .text($0) }
        self.init(
            header: headerAcc,
            footer: footerAcc,
            cells: cells(),
            headerHeight: headerHeight,
            isVisible: isVisible,
            isHeaderVisible: isHeaderVisible,
            isFooterVisible: isFooterVisible
        )
    }

    /// `SectionAccessory` 直指定 + DSL ベースの cells で `Section` を生成する。
    /// - Parameters:
    ///   - header: ヘッダ（任意の `.text` / `.view` を渡せる。`nil` で非表示）
    ///   - footer: フッタ（同上）
    ///   - isHeaderVisible: Header 表示トグル（`false` で内容があっても Header を隠す）
    ///   - isFooterVisible: Footer 表示トグル（`false` で内容があっても Footer を隠す）
    ///   - cells: `@SectionBuilder` でビルドする Cell 群
    public init(
        header: SectionAccessory?,
        footer: SectionAccessory? = nil,
        headerHeight: Double = -1,
        isVisible: Bool = true,
        isHeaderVisible: Bool = true,
        isFooterVisible: Bool = true,
        @SectionBuilder cells: () -> [any KsCell]
    ) {
        self.init(
            header: header,
            footer: footer,
            cells: cells(),
            headerHeight: headerHeight,
            isVisible: isVisible,
            isHeaderVisible: isHeaderVisible,
            isFooterVisible: isFooterVisible
        )
    }
}
