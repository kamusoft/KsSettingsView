// DatePickerCell.swift
// KsSettingsViewUI
//
// 日付選択 Cell。`uiStyle` で UI を切替える：
//   - `.wheels`   AiForms 互換の埋め込み InputView 方式（`UIDatePicker(.date)` + `.wheels`）
//   - `.calendar` `.pageSheet` + `.custom` detent でカレンダー grid を表示する方式
//                 (`UIDatePicker(.date)` + `.inline`)
//
// `date` は **Native `Foundation.Date` を直接公開**。`Date` の year / month / day 成分のみを
// 参照する（hour/minute/second は無視）。
//
// `todayText` を指定すると、Wheels モードでは Toolbar 中央寄りに、Calendar モードでは
// ボタンバーに「Today」相当のボタンが表示される。AiForms オリジナル `DatePickerCell.TodayText` 互換。
//
// 備考: iOS には `androidUiStyle` / `androidButtonColor` 引数は **持たない**（Android 限定）。

#if canImport(UIKit)
import Foundation
import UIKit
import SwiftUI
import KsSettingsViewCore

/// 日付選択 Cell。`Date` の year / month / day 成分のみを使用する。
public struct DatePickerCell: KsCell, DSLReidentifiable, DSLStyleModifiable, DSLIconModifiable, VisibilityAware {
    public let id: UUID
    public let style: CellStyle
    public let title: String
    public let description: String?
    public let valueText: String?
    public let icon: KsImage?
    public let hintText: String?
    public let date: Date
    public let format: String
    public let minDate: Date?
    public let maxDate: Date?
    public let pickerTitle: String?
    public let accentColor: UIColor?
    /// UI スタイル切替（既定 `.wheels` = AiForms 互換）。
    public let uiStyle: DatePickerUIStyle
    /// Today ボタンの表示文字列（`nil` / 空で非表示）。AiForms `TodayText` 互換。
    public let todayText: String?
    public let onValueChanged: (@Sendable (Date) -> Void)?
    public let isEnabled: Bool
    public let isVisible: Bool

    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        date: Date,
        format: String = "yyyy/MM/dd",
        minDate: Date? = nil,
        maxDate: Date? = nil,
        pickerTitle: String? = nil,
        accentColor: UIColor? = nil,
        uiStyle: DatePickerUIStyle = .wheels,
        todayText: String? = nil,
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
        self.date = date
        self.format = format
        self.minDate = minDate
        self.maxDate = maxDate
        self.pickerTitle = pickerTitle
        self.accentColor = accentColor
        self.uiStyle = uiStyle
        self.todayText = todayText
        self.onValueChanged = onValueChanged
        self.isEnabled = isEnabled
        self.isVisible = isVisible
    }

    public init(
        id: UUID = UUID(),
        style: CellStyle = CellStyle(),
        title: String,
        description: String? = nil,
        valueText: String? = nil,
        icon: KsImage? = nil,
        hintText: String? = nil,
        date: Binding<Date>,
        format: String = "yyyy/MM/dd",
        minDate: Date? = nil,
        maxDate: Date? = nil,
        pickerTitle: String? = nil,
        accentColor: UIColor? = nil,
        uiStyle: DatePickerUIStyle = .wheels,
        todayText: String? = nil,
        isEnabled: Bool = true,
        isVisible: Bool = true
    ) {
        let setter: @Sendable (Date) -> Void = { newDate in
            MainActor.assumeIsolated { date.wrappedValue = newDate }
        }
        self.init(
            id: id, style: style, title: title, description: description, valueText: valueText,
            icon: icon, hintText: hintText, date: date.wrappedValue, format: format,
            minDate: minDate, maxDate: maxDate, pickerTitle: pickerTitle, accentColor: accentColor,
            uiStyle: uiStyle, todayText: todayText,
            onValueChanged: setter, isEnabled: isEnabled, isVisible: isVisible
        )
    }

    /// 自動 valueText: 明示があればそれを、`nil` のときは `format` で文字列化（year / month / day のみ）。
    /// `DateFormatter` はフォーマット文字列ごとに `CachedDateFormatter` でキャッシュし、
    /// `effectiveValueText()` 呼び出し毎の新規生成コスト（ICU バインディング初期化を含む）を回避する。
    internal func effectiveValueText() -> String? {
        if let v = valueText { return v }
        return CachedDateFormatter.string(from: date, format: format)
    }

    public static func == (lhs: DatePickerCell, rhs: DatePickerCell) -> Bool {
        return lhs.id == rhs.id
            && lhs.style == rhs.style
            && lhs.title == rhs.title
            && lhs.description == rhs.description
            && lhs.valueText == rhs.valueText
            && lhs.icon == rhs.icon
            && lhs.hintText == rhs.hintText
            && lhs.date == rhs.date
            && lhs.format == rhs.format
            && lhs.minDate == rhs.minDate
            && lhs.maxDate == rhs.maxDate
            && lhs.pickerTitle == rhs.pickerTitle
            && uiColorEqualOptional(lhs.accentColor, rhs.accentColor)
            && lhs.uiStyle == rhs.uiStyle
            && lhs.todayText == rhs.todayText
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
        hasher.combine(date)
        hasher.combine(format)
        hasher.combine(minDate)
        hasher.combine(maxDate)
        hasher.combine(pickerTitle)
        if let c = accentColor { hasher.combine(c.hashValue) } else { hasher.combine(Int(0)) }
        hasher.combine(uiStyle)
        hasher.combine(todayText)
        hasher.combine(isEnabled)
        hasher.combine(isVisible)
    }

    public func withDSLID(_ id: UUID) -> DatePickerCell {
        return DatePickerCell(
            id: id, style: style, title: title, description: description, valueText: valueText,
            icon: icon, hintText: hintText, date: date, format: format,
            minDate: minDate, maxDate: maxDate, pickerTitle: pickerTitle, accentColor: accentColor,
            uiStyle: uiStyle, todayText: todayText,
            onValueChanged: onValueChanged, isEnabled: isEnabled, isVisible: isVisible
        )
    }

    public func withStyle(_ style: CellStyle) -> DatePickerCell {
        return DatePickerCell(
            id: id, style: style, title: title, description: description, valueText: valueText,
            icon: icon, hintText: hintText, date: date, format: format,
            minDate: minDate, maxDate: maxDate, pickerTitle: pickerTitle, accentColor: accentColor,
            uiStyle: uiStyle, todayText: todayText,
            onValueChanged: onValueChanged, isEnabled: isEnabled, isVisible: isVisible
        )
    }

    public func withIcon(_ icon: KsImage?) -> DatePickerCell {
        return DatePickerCell(
            id: id, style: style, title: title, description: description, valueText: valueText,
            icon: icon, hintText: hintText, date: date, format: format,
            minDate: minDate, maxDate: maxDate, pickerTitle: pickerTitle, accentColor: accentColor,
            uiStyle: uiStyle, todayText: todayText,
            onValueChanged: onValueChanged, isEnabled: isEnabled, isVisible: isVisible
        )
    }
}
#endif
