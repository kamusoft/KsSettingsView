// RadioCell.swift
// KsSettingsViewUI
//
// 単一選択のラジオボタン Cell。`groupId` 内で `value == selectedValue` の Cell がチェック表示される。
//
// 全 Cell 共通の `isEnabled` を持ち、`style` は UI 層 `CellStyle` で保持する（core/ADR-0009）。
// `description` / `valueText` / `icon` / `hintText` / `accentColor` は全 Cell 共通の
// 行レイアウトフィールドとして持つ（core/ADR-0011）。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore

/// ラジオボタン Cell。
///
/// 同一 `groupId` の RadioCell 群で単一選択を表現する。利用者は同グループ内の全 RadioCell に
/// 同じ `selectedValue` を設定する。タップで `onSelected(value)` を発火し、`selectedValue` の
/// 更新は SettingsRoot 側の責務とする。
public struct RadioCell: KsCell, DSLReidentifiable, DSLStyleModifiable, DSLIconModifiable, VisibilityAware {
    public let id: UUID
    public let style: CellStyle
    public let title: String
    /// 説明文（任意）
    public let description: String?
    /// 右側に表示する値文字列（任意）
    public let valueText: String?
    /// アイコン（任意）
    public let icon: KsImage?
    /// ヒントテキスト（任意、右上）
    public let hintText: String?
    /// 同一選択グループの識別子
    public let groupId: String
    /// この Cell の値
    public let value: String
    /// グループ内の現在選択値（`value == selectedValue` でチェック表示）
    public let selectedValue: String
    /// チェックマーク色（任意、`Theme.cellAccentColor` の代替）
    public let accentColor: UIColor?
    /// 選択時に呼ばれるクロージャ。引数はこの Cell の `value`
    public let onSelected: (@Sendable (String) -> Void)?
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
        groupId: String,
        value: String,
        selectedValue: String,
        accentColor: UIColor? = nil,
        onSelected: (@Sendable (String) -> Void)? = nil,
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
        self.groupId = groupId
        self.value = value
        self.selectedValue = selectedValue
        self.accentColor = accentColor
        self.onSelected = onSelected
        self.isEnabled = isEnabled
        self.isVisible = isVisible
    }

    public static func == (lhs: RadioCell, rhs: RadioCell) -> Bool {
        return lhs.id == rhs.id
            && lhs.style == rhs.style
            && lhs.title == rhs.title
            && lhs.description == rhs.description
            && lhs.valueText == rhs.valueText
            && lhs.icon == rhs.icon
            && lhs.hintText == rhs.hintText
            && lhs.groupId == rhs.groupId
            && lhs.value == rhs.value
            && lhs.selectedValue == rhs.selectedValue
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
        hasher.combine(groupId)
        hasher.combine(value)
        hasher.combine(selectedValue)
        if let c = accentColor {
            hasher.combine(c.hashValue)
        } else {
            hasher.combine(Int(0))
        }
        hasher.combine(isEnabled)
        hasher.combine(isVisible)
    }

    public func withDSLID(_ id: UUID) -> RadioCell {
        return RadioCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            groupId: groupId,
            value: value,
            selectedValue: selectedValue,
            accentColor: accentColor,
            onSelected: onSelected,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    public func withStyle(_ style: CellStyle) -> RadioCell {
        return RadioCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            groupId: groupId,
            value: value,
            selectedValue: selectedValue,
            accentColor: accentColor,
            onSelected: onSelected,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    /// DSL の `.icon(_:)` modifier 経由で呼ばれる、`icon` のみを書き換えた copy を返す。
    public func withIcon(_ icon: KsImage?) -> RadioCell {
        return RadioCell(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            groupId: groupId,
            value: value,
            selectedValue: selectedValue,
            accentColor: accentColor,
            onSelected: onSelected,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif
