// CommandCell.swift
// KsSettingsViewUI
//
// タップで処理を実行する Cell。Disclosure Indicator を表示し、`onTap` クロージャを発火する。
//
// 全 Cell 共通の `isEnabled` を持つ。`style`（UI 層 `CellStyle`）と `icon`（UI 層 `KsImage?`）は
// Core の Cell 抽象ではなく本 Cell が個別に保持する（core/ADR-0009）。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore

/// タップで処理を実行する Cell。
///
/// `LabelCell` のフィールドに加えて、`onTap` クロージャと `hideArrow` フラグを持つ。
/// `hideArrow` が `false`（既定）の場合は右端に Disclosure Indicator（chevron）を表示する。
///
/// クロージャ（`onTap`）は `Hashable` / `Equatable` の判定対象から除外する
/// （毎回新規クロージャが生成されると差分検出が暴発するため）。
public struct CommandCell: KsCell, DSLReidentifiable, DSLStyleModifiable, DSLIconModifiable, VisibilityAware {
    public let id: UUID
    public let style: CellStyle
    public let title: String
    public let description: String?
    public let valueText: String?
    public let icon: KsImage?
    public let hintText: String?
    /// Disclosure Indicator を非表示にするフラグ（既定 `false`、つまり表示）
    public let hideArrow: Bool
    /// タップ時に発火するクロージャ
    public let onTap: (@Sendable () -> Void)?
    /// 有効／無効フラグ（既定 `true`）。`false` のときはタップを受け付けず、
    /// テキスト色を `Theme.disabledTextColor` に置換する。
    public let isEnabled: Bool
    /// 可視性フラグ（既定 `true`）。`false` のときは UI 層の visible projection から除外される。
    public let isVisible: Bool

    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        hideArrow: Bool = false,
        onTap: (@Sendable () -> Void)? = nil,
        isEnabled: Bool = true,
        isVisible: Bool = true
    ) {
        self.id = id
        self.style = style
        self.title = title
        self.description = description
        self.valueText = valueText
        self.icon = icon
        self.hintText = hintText
        self.hideArrow = hideArrow
        self.onTap = onTap
        self.isEnabled = isEnabled
        self.isVisible = isVisible
    }

    // MARK: - Hashable / Equatable 手動実装（クロージャを判定対象から除外）

    public static func == (lhs: CommandCell, rhs: CommandCell) -> Bool {
        return lhs.id == rhs.id
            && lhs.style == rhs.style
            && lhs.title == rhs.title
            && lhs.description == rhs.description
            && lhs.valueText == rhs.valueText
            && lhs.icon == rhs.icon
            && lhs.hintText == rhs.hintText
            && lhs.hideArrow == rhs.hideArrow
            && lhs.isEnabled == rhs.isEnabled
            && lhs.isVisible == rhs.isVisible
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hashCellStyle(style, into: &hasher)
        hasher.combine(title)
        hasher.combine(description)
        hasher.combine(valueText)
        hasher.combine(icon)
        hasher.combine(hintText)
        hasher.combine(hideArrow)
        hasher.combine(isEnabled)
        hasher.combine(isVisible)
    }

    public func withDSLID(_ id: UUID) -> CommandCell {
        return CommandCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            hideArrow: hideArrow,
            onTap: onTap,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    public func withStyle(_ style: CellStyle) -> CommandCell {
        return CommandCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            hideArrow: hideArrow,
            onTap: onTap,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    /// DSL の `.icon(_:)` modifier 経由で呼ばれる、`icon` のみを書き換えた copy を返す。
    public func withIcon(_ icon: KsImage?) -> CommandCell {
        return CommandCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            hideArrow: hideArrow,
            onTap: onTap,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif
