// TimePickerCell.swift
// KsSettingsViewUI
//
// 時刻選択 Cell。タップで `UIDatePicker(.time)` 内蔵モーダルを開く。
// `time` は **Native `Foundation.Date` を直接公開**（独自値型でラップしない）。
// `Date` の hour / minute 成分のみを参照する（year/month/day は無視）。
// Native 型を独自値型でラップしないのは core/ADR-0009 による。

#if canImport(UIKit)
import Foundation
import UIKit
import SwiftUI
import KsSettingsViewCore

/// 時刻選択 Cell。`Date` の hour / minute 成分のみを使用する。
public struct TimePickerCell: KsCell, DSLReidentifiable, DSLStyleModifiable, DSLIconModifiable, VisibilityAware {
    public let id: UUID
    public let style: CellStyle
    public let title: String
    public let description: String?
    public let valueText: String?
    public let icon: KsImage?
    public let hintText: String?
    /// 時刻値（`Date` の hour / minute 成分のみ使用）
    public let time: Date
    /// 表示フォーマット（`DateFormatter.dateFormat`、既定 "HH:mm"）。
    /// 行の valueText の文字列化にだけ効き、選択面の時制には関与しない（core/ADR-0028）。
    public let format: String
    /// 選択面の時制（既定 `true` = 24時間制、`false` で12時間制）。
    /// 選択面の時制はこの値だけで決まる（core/ADR-0028）。
    public let is24Hour: Bool
    public let pickerTitle: String?
    public let accentColor: UIColor?
    public let onValueChanged: (@Sendable (Date) -> Void)?
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
        time: Date,
        format: String = "HH:mm",
        is24Hour: Bool = true,
        pickerTitle: String? = nil,
        accentColor: UIColor? = nil,
        onValueChanged: (@Sendable (Date) -> Void)? = nil,
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
        self.time = time
        self.format = format
        self.is24Hour = is24Hour
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
        time: Binding<Date>,
        format: String = "HH:mm",
        is24Hour: Bool = true,
        pickerTitle: String? = nil,
        accentColor: UIColor? = nil,
        isEnabled: Bool = true,
        isVisible: Bool = true
    ) {
        let setter: @Sendable (Date) -> Void = { newDate in
            MainActor.assumeIsolated { time.wrappedValue = newDate }
        }
        self.init(
            id: id, style: style, title: title, description: description, valueText: valueText,
            icon: icon, hintText: hintText, time: time.wrappedValue, format: format,
            is24Hour: is24Hour, pickerTitle: pickerTitle, accentColor: accentColor, onValueChanged: setter,
            isEnabled: isEnabled, isVisible: isVisible
        )
    }

    /// 自動 valueText: 明示があればそれを、`nil` のときは `format` で文字列化（hour / minute のみ）。
    /// `DateFormatter` はフォーマット文字列ごとに `CachedDateFormatter` でキャッシュし、
    /// `effectiveValueText()` 呼び出し毎の新規生成コスト（ICU バインディング初期化を含む）を回避する。
    internal func effectiveValueText() -> String? {
        if let v = valueText { return v }
        return CachedDateFormatter.string(from: time, format: format)
    }

    public static func == (lhs: TimePickerCell, rhs: TimePickerCell) -> Bool {
        return lhs.id == rhs.id
            && lhs.style == rhs.style
            && lhs.title == rhs.title
            && lhs.description == rhs.description
            && lhs.valueText == rhs.valueText
            && lhs.icon == rhs.icon
            && lhs.hintText == rhs.hintText
            && lhs.time == rhs.time
            && lhs.format == rhs.format
            && lhs.is24Hour == rhs.is24Hour
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
        hasher.combine(time)
        hasher.combine(format)
        hasher.combine(is24Hour)
        hasher.combine(pickerTitle)
        if let c = accentColor { hasher.combine(c.hashValue) } else { hasher.combine(Int(0)) }
        hasher.combine(isEnabled)
        hasher.combine(isVisible)
    }

    public func withDSLID(_ id: UUID) -> TimePickerCell {
        return TimePickerCell(
            id: id, style: style, title: title, description: description, valueText: valueText,
            icon: icon, hintText: hintText, time: time, format: format,
            is24Hour: is24Hour, pickerTitle: pickerTitle, accentColor: accentColor,
            onValueChanged: onValueChanged,
            isEnabled: isEnabled, isVisible: isVisible
        )
    }

    public func withStyle(_ style: CellStyle) -> TimePickerCell {
        return TimePickerCell(
            id: id, style: style, title: title, description: description, valueText: valueText,
            icon: icon, hintText: hintText, time: time, format: format,
            is24Hour: is24Hour, pickerTitle: pickerTitle, accentColor: accentColor,
            onValueChanged: onValueChanged,
            isEnabled: isEnabled, isVisible: isVisible
        )
    }

    public func withIcon(_ icon: KsImage?) -> TimePickerCell {
        return TimePickerCell(
            id: id, style: style, title: title, description: description, valueText: valueText,
            icon: icon, hintText: hintText, time: time, format: format,
            is24Hour: is24Hour, pickerTitle: pickerTitle, accentColor: accentColor,
            onValueChanged: onValueChanged,
            isEnabled: isEnabled, isVisible: isVisible
        )
    }
}
#endif
