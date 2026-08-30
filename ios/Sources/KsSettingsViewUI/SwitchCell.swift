// SwitchCell.swift
// KsSettingsViewUI
//
// ON/OFF を切り替えるトグルスイッチを持つ Cell。値変更時に `onValueChanged` を発火する。
//
// 全 Cell 共通の `isEnabled` を持ち、`accentColor` は Native 型（`UIColor?`）を直接保持する
// （core/ADR-0009）。`description` / `valueText` / `icon` / `hintText` は全 Cell 共通の
// 行レイアウトフィールドとして持つ（core/ADR-0011）。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore

/// ON/OFF スイッチを持つ Cell。
///
/// 右側に `UISwitch` を表示し、ユーザー操作で `onValueChanged(Bool)` を発火する。
/// `accentColor` を指定すると ON 時のスイッチの色を変更できる。
///
/// `onValueChanged` クロージャは `Hashable` / `Equatable` の判定対象から除外する。
public struct SwitchCell: KsCell, DSLReidentifiable, DSLStyleModifiable, DSLIconModifiable, VisibilityAware {
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
    /// 現在の ON/OFF 値
    public let isOn: Bool
    /// スイッチ ON 時の色（任意）
    public let accentColor: UIColor?
    /// 値変更時に呼ばれるクロージャ
    public let onValueChanged: (@Sendable (Bool) -> Void)?
    /// 有効／無効フラグ（既定 `true`）。`false` のときはスイッチ操作を受け付けず、
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
        isOn: Bool = false,
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
        self.isOn = isOn
        self.accentColor = accentColor
        self.onValueChanged = onValueChanged
        self.isEnabled = isEnabled
        self.isVisible = isVisible
    }

    public static func == (lhs: SwitchCell, rhs: SwitchCell) -> Bool {
        return lhs.id == rhs.id
            && lhs.style == rhs.style
            && lhs.title == rhs.title
            && lhs.description == rhs.description
            && lhs.valueText == rhs.valueText
            && lhs.icon == rhs.icon
            && lhs.hintText == rhs.hintText
            && lhs.isOn == rhs.isOn
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
        hasher.combine(isOn)
        if let c = accentColor {
            hasher.combine(c.hashValue)
        } else {
            hasher.combine(Int(0))
        }
        hasher.combine(isEnabled)
        hasher.combine(isVisible)
    }

    public func withDSLID(_ id: UUID) -> SwitchCell {
        return SwitchCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            isOn: isOn,
            accentColor: accentColor,
            onValueChanged: onValueChanged,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    public func withStyle(_ style: CellStyle) -> SwitchCell {
        return SwitchCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            isOn: isOn,
            accentColor: accentColor,
            onValueChanged: onValueChanged,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    /// DSL の `.icon(_:)` modifier 経由で呼ばれる、`icon` のみを書き換えた copy を返す。
    public func withIcon(_ icon: KsImage?) -> SwitchCell {
        return SwitchCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            isOn: isOn,
            accentColor: accentColor,
            onValueChanged: onValueChanged,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif
