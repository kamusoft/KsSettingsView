// CheckboxCell.swift
// KsSettingsViewUI
//
// ON/OFF をチェックマークで表す Cell。タップで toggle し `onValueChanged` を発火する。
//
// 全 Cell 共通の `isEnabled` を持ち、`accentColor` は Native 型（`UIColor?`）を直接保持する
// （core/ADR-0009）。`description` / `valueText` / `icon` / `hintText` は全 Cell 共通の
// 行レイアウトフィールドとして持つ（core/ADR-0011）。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore

/// チェックボックス Cell。
///
/// 右端にチェックマーク（accent カラー）を表示し、Cell 全体のタップで toggle する。
/// AiForms.Maui.SettingsView と同じく `Checked` は TwoWay バインディング相当。
public struct CheckboxCell: KsCell, DSLReidentifiable, DSLStyleModifiable, DSLIconModifiable, VisibilityAware {
    public let id: UUID
    public let style: CellStyle
    public let title: String
    public let description: String?
    /// 右側に表示する値文字列（任意）
    public let valueText: String?
    /// アイコン（任意）
    public let icon: KsImage?
    /// ヒントテキスト（任意、右上）
    public let hintText: String?
    /// チェック状態
    public let isChecked: Bool
    /// チェックマーク色（任意、Theme.cellAccentColor の代替）
    public let accentColor: UIColor?
    /// 値変更時に呼ばれるクロージャ
    public let onValueChanged: (@Sendable (Bool) -> Void)?
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
        isChecked: Bool = false,
        accentColor: UIColor? = nil,
        onValueChanged: (@Sendable (Bool) -> Void)? = nil,
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
        self.isChecked = isChecked
        self.accentColor = accentColor
        self.onValueChanged = onValueChanged
        self.isEnabled = isEnabled
        self.isVisible = isVisible
    }

    public static func == (lhs: CheckboxCell, rhs: CheckboxCell) -> Bool {
        return lhs.id == rhs.id
            && lhs.style == rhs.style
            && lhs.title == rhs.title
            && lhs.description == rhs.description
            && lhs.valueText == rhs.valueText
            && lhs.icon == rhs.icon
            && lhs.hintText == rhs.hintText
            && lhs.isChecked == rhs.isChecked
            && uiColorEqualOptional(lhs.accentColor, rhs.accentColor)
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
        hasher.combine(isChecked)
        if let c = accentColor {
            hasher.combine(c.hashValue)
        } else {
            hasher.combine(Int(0))
        }
        hasher.combine(isEnabled)
        hasher.combine(isVisible)
    }

    public func withDSLID(_ id: UUID) -> CheckboxCell {
        return CheckboxCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            isChecked: isChecked,
            accentColor: accentColor,
            onValueChanged: onValueChanged,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    public func withStyle(_ style: CellStyle) -> CheckboxCell {
        return CheckboxCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            isChecked: isChecked,
            accentColor: accentColor,
            onValueChanged: onValueChanged,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    /// DSL の `.icon(_:)` modifier 経由で呼ばれる、`icon` のみを書き換えた copy を返す。
    public func withIcon(_ icon: KsImage?) -> CheckboxCell {
        return CheckboxCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            isChecked: isChecked,
            accentColor: accentColor,
            onValueChanged: onValueChanged,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif
