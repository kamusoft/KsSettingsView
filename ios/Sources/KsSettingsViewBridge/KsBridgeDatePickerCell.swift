// KsBridgeDatePickerCell.swift
// KsSettingsViewBridge
//
// interop 境界で `DatePickerCell` を輸送する `@objc` 互換 DTO。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore
import KsSettingsViewUI

/// 日付を選ぶ Cell (`DatePickerCell`) を輸送する DTO。
///
/// 日付は壁時計値として "yyyy-MM-dd" の文字列で運ぶ (maui/ADR-0012)。値変更は
/// `KsBridgeInteractionDelegate.datePickerCellChanged(cellID:date:)` で同じ書式で通知される。
/// 選択面の形式は統一 enum の序数で運び、未指定のときは Native 既定を使う (maui/ADR-0013)。
@objc(KsBridgeDatePickerCell)
public final class KsBridgeDatePickerCell: KsBridgeCell {

    /// 現在の日付 ("yyyy-MM-dd")
    @objc public var date: String = "1970-01-01"

    /// 表示フォーマット (`DateFormatter` の書式、未指定は `nil` で Native 既定)
    @objc public var format: String?

    /// 選択できる最小日付 ("yyyy-MM-dd"、未指定は `nil`)
    @objc public var minDate: String?

    /// 選択できる最大日付 ("yyyy-MM-dd"、未指定は `nil`)
    @objc public var maxDate: String?

    /// 選択面のタイトル (未指定は `nil`)
    @objc public var pickerTitle: String?

    /// 選択強調色 (ARGB、未指定は `nil`)
    @objc public var accentColor: NSNumber?

    /// 選択面の形式の序数 (`0 = Calendar / 1 = Wheels`、未指定は `nil` で Native 既定)
    @objc public var uiStyle: NSNumber?

    /// Today ボタンの表示文字列 (`nil` または空で非表示)
    @objc public var todayText: String?

    /// 未指定の項目に使う `DatePickerCell` 側の既定値の取得元。
    ///
    /// 値を写し取らず Native の既定から引く (Native 側を変えたときに輸送側が古い既定を
    /// 渡し続けることを防ぐ)。
    private static func nativeDefaults() -> DatePickerCell {
        DatePickerCell(title: "", date: Date(timeIntervalSince1970: 0))
    }

    /// `format` 未指定のときに使う `DatePickerCell` 側の既定表示フォーマット。
    private static let defaultFormat: String = nativeDefaults().format

    /// `uiStyle` 未指定のときに使う `DatePickerCell` 側の既定形式。
    private static let defaultUIStyle: DatePickerUIStyle = nativeDefaults().uiStyle

    override func makeCell(id: UUID, relay: KsBridgeInteractionRelay) -> any KsCell {
        let notifiedCellID = KsBridgeIdentifier.string(from: id)
        return DatePickerCell(
            id: id,
            style: resolvedStyle,
            title: title,
            description: descriptionText,
            valueText: valueText,
            icon: resolvedIcon,
            hintText: hintText,
            date: KsBridgeValueTransport.date(from: date),
            format: format ?? Self.defaultFormat,
            minDate: KsBridgeValueTransport.optionalDate(from: minDate),
            maxDate: KsBridgeValueTransport.optionalDate(from: maxDate),
            pickerTitle: pickerTitle,
            accentColor: KsBridgeColor.uiColor(accentColor),
            uiStyle: KsBridgeValueTransport.datePickerUIStyle(from: uiStyle) ?? Self.defaultUIStyle,
            todayText: todayText,
            onValueChanged: { relay.datePickerCellChanged(cellID: notifiedCellID, date: $0) },
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif
