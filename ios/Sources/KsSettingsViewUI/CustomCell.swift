// CustomCell.swift
// KsSettingsViewUI
//
// 事前登録なしで DSL に直書きできる、任意 SwiftUI View を行全体に描画する Cell。
//
// 決定: core/ADR-0014（content 値 + builder による構成）
//       core/ADR-0022（共通行レイアウトの適用除外）

#if canImport(UIKit)
import Foundation
import SwiftUI
import UIKit
import KsSettingsViewCore

/// 利用者定義の content 値と、content から SwiftUI View を生成する builder を保持する Cell。
///
/// ```swift
/// // ① インライン利用（データ駆動）
/// CustomCell(content: volume) { value in
///     Slider(value: .constant(Double(value)), in: 0...100)
/// }
///
/// // ② content を持たない静的コンテンツ
/// CustomCell {
///     HStack { Image(systemName: "star"); Text("おすすめ") }
/// }
/// ```
///
/// # 型消去の方針（core/ADR-0016）
///
/// `KsCellRegistry` は `ObjectIdentifier(type(of: cell))` を解決キーにするため、
/// ジェネリック struct を Cell 型にすると実体型ごとの事前登録が必要になり
/// core/ADR-0014 の「事前登録なし」と矛盾する。そこで本型は**非ジェネリック**とし、
/// content を `AnyHashable`、builder を `(AnyHashable) -> AnyView` へ init 時点で
/// 型消去して保持する。利用者から見た型安全はジェネリック `init` が担保する。
///
/// # 等価性（core/ADR-0014）
///
/// `id` / `style` / `content`（値と実体型）/ `showArrow` / `isEnabled` / `isVisible` のみが
/// 等価判定に参加し、関数値（`builder` / `onTap`）は除外される。
/// DSL の再評価ごとに新しいクロージャが生成されても差分検出が暴発しない。
///
/// # 利用者側の契約
///
/// - `content` は値等価（`Hashable`）を持つ **non-Optional** の型であること。
///   Swift の型システム上 `Optional` も `Hashable` に適合するため受け付けられてしまうが、
///   契約としては non-null（`Optional` を渡さない）とする。
/// - `builder` / `onTap` は同一 id・同一 content の間で意味的に安定であること。
///   見た目や動作を変える値はクロージャのキャプチャではなく `content` に含めること
///   （関数値だけを差し替えても再バインドは発生しない）。
///
/// # `@unchecked Sendable` の根拠
///
/// `KsCell` は `Sendable` を要求するが、`AnyHashable` および `AnyView` を返す builder は
/// Sendable ではない。`KsAnyView` と同型の問題であり、同じ手当（`@unchecked Sendable` +
/// 描画側を `@MainActor` に限定）を踏襲する。全フィールドが `let` で、生成後は不変。
public struct CustomCell: KsCell, DSLReidentifiable, DSLStyleModifiable, VisibilityAware, @unchecked Sendable {
    /// content を持たない省略形で使う内部空値。全インスタンスが相等になる。
    ///
    /// これにより省略形の等価性は「content を除く参加要素（id / style / showArrow /
    /// isEnabled / isVisible）」で決まる。
    internal struct EmptyContent: Hashable, Sendable {
        internal init() {}
    }

    public let id: UUID
    public let style: CellStyle

    /// 型消去済み content。等価性の主対象。
    ///
    /// content を持たない省略形では `EmptyContent()` が入る。
    public let content: AnyHashable

    /// 型消去前の content の実体型トークン。`content` と対で等価性に参加する。
    ///
    /// `AnyHashable` は Foundation ブリッジ経由で異なる実体型の値を等価と判定することがある
    /// （`AnyHashable(Int(1)) == AnyHashable(Double(1.0))` は `true`）。builder の引数型は
    /// 実体型そのものなので、値だけを比べると「content の型が変わったのに等価」と判定され、
    /// 差分検出が再バインドを省略して古い builder の出力が残ってしまう。
    /// 実体型トークンを等価性・hash に含めることでこの取りこぼしを防ぐ。
    internal let contentType: ObjectIdentifier

    /// 型消去済み builder。等価性からは除外される。
    ///
    /// 引数には常に自身の `content` が渡される（`CustomCellView` が呼び出す）。
    internal let builder: (AnyHashable) -> AnyView

    /// Disclosure Indicator を表示するフラグ（既定 `false`）。
    /// `true` のとき標準 Cell（CommandCell）と同一の chevron を trailing に合成する。
    public let showArrow: Bool

    /// 行タップ時に発火するクロージャ（既定 `nil` = 行タップ非対応）。
    ///
    /// content 内の操作可能要素がタップを消費した場合は発火しない。
    public let onTap: (@Sendable () -> Void)?

    /// 有効／無効フラグ（既定 `true`）。
    ///
    /// `false` のとき行タップは発火せず、content 内部の操作も抑止される。
    /// あわせて content を淡色化するが、これは既定の振る舞いであり、
    /// 無効時の描き分けを content 側で追加するのは利用者の自由。
    public let isEnabled: Bool

    /// 可視性フラグ（既定 `true`）。`false` のとき visible projection から除外される。
    public let isVisible: Bool

    // MARK: - 公開イニシャライザ

    /// content 値を伴う CustomCell を生成する（データ駆動）。
    ///
    /// - Parameters:
    ///   - id: Cell ID（省略時は自動採番）
    ///   - style: 個別スタイル。行レベル項目（背景色 / cellHeight）のみが効く
    ///   - content: 表示の元になる値。値等価（`Hashable`）を持つ non-Optional 型であること
    ///   - showArrow: Disclosure Indicator を表示するか（既定 `false`）
    ///   - onTap: 行タップ時のクロージャ（既定 `nil` = 行タップ非対応）
    ///   - isEnabled: 有効フラグ（既定 `true`）
    ///   - isVisible: 可視性フラグ（既定 `true`）
    ///   - builder: content から行全体の SwiftUI View を生成するクロージャ
    public init<C: Hashable, V: View>(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        content: C,
        showArrow: Bool = false,
        onTap: (@Sendable () -> Void)? = nil,
        isEnabled: Bool = true,
        isVisible: Bool = true,
        @ViewBuilder builder: @escaping (C) -> V
    ) {
        self.init(
            id: id,
            style: style,
            erasedContent: AnyHashable(content),
            contentType: ObjectIdentifier(C.self),
            erasedBuilder: { erased in
                guard let typed = erased.base as? C else {
                    // `builder` には常に自身の `content` が渡されるため到達しない。
                    assertionFailure("CustomCell: content の型が builder の期待型と一致しない")
                    return AnyView(EmptyView())
                }
                return AnyView(builder(typed))
            },
            showArrow: showArrow,
            onTap: onTap,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    /// content を持たない CustomCell を生成する（静的コンテンツの省略形）。
    ///
    /// 等価性は content を除く参加要素（id / style / showArrow / isEnabled / isVisible）で決まる。
    public init<V: View>(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        showArrow: Bool = false,
        onTap: (@Sendable () -> Void)? = nil,
        isEnabled: Bool = true,
        isVisible: Bool = true,
        @ViewBuilder builder: @escaping () -> V
    ) {
        self.init(
            id: id,
            style: style,
            erasedContent: AnyHashable(EmptyContent()),
            contentType: ObjectIdentifier(EmptyContent.self),
            erasedBuilder: { _ in AnyView(builder()) },
            showArrow: showArrow,
            onTap: onTap,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    // MARK: - 内部イニシャライザ（型消去済みの値をそのまま受け取る）

    /// 型消去済みの content / builder を受け取る内部イニシャライザ。
    ///
    /// `withDSLID(_:)` / `withStyle(_:)` の copy 生成に使う（builder を再度型消去し直さない）。
    internal init(
        id: UUID,
        style: CellStyle,
        erasedContent: AnyHashable,
        contentType: ObjectIdentifier,
        erasedBuilder: @escaping (AnyHashable) -> AnyView,
        showArrow: Bool,
        onTap: (@Sendable () -> Void)?,
        isEnabled: Bool,
        isVisible: Bool
    ) {
        self.id = id
        self.style = style
        self.content = erasedContent
        self.contentType = contentType
        self.builder = erasedBuilder
        self.showArrow = showArrow
        self.onTap = onTap
        self.isEnabled = isEnabled
        self.isVisible = isVisible
    }

    // MARK: - Hashable / Equatable 手動実装（関数値を判定対象から除外）

    public static func == (lhs: CustomCell, rhs: CustomCell) -> Bool {
        return lhs.id == rhs.id
            && lhs.style == rhs.style
            && lhs.contentType == rhs.contentType
            && lhs.content == rhs.content
            && lhs.showArrow == rhs.showArrow
            && lhs.isEnabled == rhs.isEnabled
            && lhs.isVisible == rhs.isVisible
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hashCellStyle(style, into: &hasher)
        hasher.combine(contentType)
        hasher.combine(content)
        hasher.combine(showArrow)
        hasher.combine(isEnabled)
        hasher.combine(isVisible)
    }

    // MARK: - DSL 経路の copy 生成

    public func withDSLID(_ id: UUID) -> CustomCell {
        return CustomCell(
            id: id,
            style: style,
            erasedContent: content,
            contentType: contentType,
            erasedBuilder: builder,
            showArrow: showArrow,
            onTap: onTap,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    public func withStyle(_ style: CellStyle) -> CustomCell {
        return CustomCell(
            id: id,
            style: style,
            erasedContent: content,
            contentType: contentType,
            erasedBuilder: builder,
            showArrow: showArrow,
            onTap: onTap,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif
