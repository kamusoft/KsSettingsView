// NumberPickerCell.swift
// KsSettingsViewUI
//
// 範囲指定の数値選択 Cell。タップで埋め込み `UIPickerView` を `inputView` 経由で
// キーボード位置にスライドアップ表示する（AiForms 互換）。
//
// `unit` で任意の単位文字列（例: "px"）を設定できる。指定時は valueText と Picker UI
// の各候補表示に suffix として付加される（例: "15 px"）。

#if canImport(UIKit)
import Foundation
import UIKit
import SwiftUI
import KsSettingsViewCore

/// 範囲指定の数値選択 Cell。
public struct NumberPickerCell: KsCell, DSLReidentifiable, DSLStyleModifiable, DSLIconModifiable, VisibilityAware {
    public let id: UUID
    public let style: CellStyle
    public let title: String
    public let description: String?
    public let valueText: String?
    public let icon: KsImage?
    public let hintText: String?
    public let min: Int
    public let max: Int
    public let step: Int
    public let value: Int
    /// 単位文字列（例: "px"）。空文字列または `nil` 相当時は suffix 付加なし。
    /// AiForms オリジナル `NumberPickerCell.Unit` 互換（既定 `""`）。
    public let unit: String
    public let pickerTitle: String?
    public let accentColor: UIColor?
    public let onValueChanged: (@Sendable (Int) -> Void)?
    public let isEnabled: Bool
    public let isVisible: Bool

    // MARK: - Store 経路 init

    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        min: Int = 0,
        max: Int = 100,
        step: Int = 1,
        value: Int,
        unit: String = "",
        pickerTitle: String? = nil,
        accentColor: UIColor? = nil,
        onValueChanged: (@Sendable (Int) -> Void)? = nil,
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
        self.min = min
        self.max = max
        self.step = step
        self.value = value
        self.unit = unit
        self.pickerTitle = pickerTitle
        self.accentColor = accentColor
        self.onValueChanged = onValueChanged
        self.isEnabled = isEnabled
        self.isVisible = isVisible
    }

    // MARK: - DSL 経路 init

    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        min: Int = 0,
        max: Int = 100,
        step: Int = 1,
        value: Binding<Int>,
        unit: String = "",
        pickerTitle: String? = nil,
        accentColor: UIColor? = nil,
        isEnabled: Bool = true,
        isVisible: Bool = true
    ) {
        let setter: @Sendable (Int) -> Void = { newValue in
            MainActor.assumeIsolated { value.wrappedValue = newValue }
        }
        self.init(
            id: id,
            style: style,
            title: title,
            description: description,
            valueText: valueText,
            icon: icon,
            hintText: hintText,
            min: min,
            max: max,
            step: step,
            value: value.wrappedValue,
            unit: unit,
            pickerTitle: pickerTitle,
            accentColor: accentColor,
            onValueChanged: setter,
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }

    /// 自動 valueText: `valueText` 明示があればそれを、`nil` のときは
    /// `"<value> <unit>"` (unit 空なら数字のみ) を返す。
    /// AiForms オリジナル `NumberPickerCellView.FormatNumber` 相当。
    internal func effectiveValueText() -> String? {
        if let v = valueText { return v }
        return NumberPickerCell.format(value: value, unit: unit)
    }

    /// 内部共通フォーマッタ。Picker UI 各行表示でも同じロジックを使う。
    internal static func format(value: Int, unit: String) -> String {
        if unit.isEmpty {
            return String(value)
        }
        return "\(value) \(unit)"
    }

    public static func == (lhs: NumberPickerCell, rhs: NumberPickerCell) -> Bool {
        return lhs.id == rhs.id
            && lhs.style == rhs.style
            && lhs.title == rhs.title
            && lhs.description == rhs.description
            && lhs.valueText == rhs.valueText
            && lhs.icon == rhs.icon
            && lhs.hintText == rhs.hintText
            && lhs.min == rhs.min
            && lhs.max == rhs.max
            && lhs.step == rhs.step
            && lhs.value == rhs.value
            && lhs.unit == rhs.unit
            && lhs.pickerTitle == rhs.pickerTitle
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
        hasher.combine(min)
        hasher.combine(max)
        hasher.combine(step)
        hasher.combine(value)
        hasher.combine(unit)
        hasher.combine(pickerTitle)
        if let c = accentColor {
            hasher.combine(c.hashValue)
        } else {
            hasher.combine(Int(0))
        }
        hasher.combine(isEnabled)
        hasher.combine(isVisible)
    }

    public func withDSLID(_ id: UUID) -> NumberPickerCell {
        return NumberPickerCell(
            id: id, style: style, title: title, description: description, valueText: valueText,
            icon: icon, hintText: hintText, min: min, max: max, step: step, value: value,
            unit: unit, pickerTitle: pickerTitle, accentColor: accentColor,
            onValueChanged: onValueChanged, isEnabled: isEnabled, isVisible: isVisible
        )
    }

    public func withStyle(_ style: CellStyle) -> NumberPickerCell {
        return NumberPickerCell(
            id: id, style: style, title: title, description: description, valueText: valueText,
            icon: icon, hintText: hintText, min: min, max: max, step: step, value: value,
            unit: unit, pickerTitle: pickerTitle, accentColor: accentColor,
            onValueChanged: onValueChanged, isEnabled: isEnabled, isVisible: isVisible
        )
    }

    public func withIcon(_ icon: KsImage?) -> NumberPickerCell {
        return NumberPickerCell(
            id: id, style: style, title: title, description: description, valueText: valueText,
            icon: icon, hintText: hintText, min: min, max: max, step: step, value: value,
            unit: unit, pickerTitle: pickerTitle, accentColor: accentColor,
            onValueChanged: onValueChanged, isEnabled: isEnabled, isVisible: isVisible
        )
    }
}
#endif
