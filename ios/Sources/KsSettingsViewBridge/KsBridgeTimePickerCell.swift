// KsBridgeTimePickerCell.swift
// KsSettingsViewBridge
//
// interop 境界で `TimePickerCell` を輸送する `@objc` 互換 DTO。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore
import KsSettingsViewUI

/// 時刻を選ぶ Cell (`TimePickerCell`) を輸送する DTO。
///
/// 時刻は壁時計値として "HH:mm" の文字列で運ぶ (maui/ADR-0012)。値変更は
/// `KsBridgeInteractionDelegate.timePickerCellChanged(cellID:time:)` で同じ書式で通知される。
@objc(KsBridgeTimePickerCell)
public final class KsBridgeTimePickerCell: KsBridgeCell {

    /// 現在の時刻 ("HH:mm")
    @objc public var time: String = "00:00"

    /// 表示フォーマット (`DateFormatter` の書式、未指定は `nil` で Native 既定)
    @objc public var format: String?

    /// 選択面の時制 (`true` = 24時間制 / `false` = 12時間制、未指定は `nil` で Native 既定)
    @objc public var is24Hour: NSNumber?

    /// 選択面のタイトル (未指定は `nil`)
    @objc public var pickerTitle: String?

    /// 選択強調色 (ARGB、未指定は `nil`)
    @objc public var accentColor: NSNumber?

    /// 未指定の項目に使う `TimePickerCell` 側の既定値の取得元。
    ///
    /// 値を写し取らず Native の既定から引く (Native 側を変えたときに輸送側が古い既定を
    /// 渡し続けることを防ぐ)。
    private static func nativeDefaults() -> TimePickerCell {
        TimePickerCell(title: "", time: Date(timeIntervalSince1970: 0))
    }

    /// `format` 未指定のときに使う `TimePickerCell` 側の既定表示フォーマット。
    private static let defaultFormat: String = nativeDefaults().format

    /// `is24Hour` 未指定のときに使う `TimePickerCell` 側の既定時制。
    private static let defaultIs24Hour: Bool = nativeDefaults().is24Hour

    override func makeCell(id: UUID, relay: KsBridgeInteractionRelay) -> any KsCell {
        let notifiedCellID = KsBridgeIdentifier.string(from: id)
        return TimePickerCell(
            id: id,
            style: resolvedStyle,
            title: title,
            description: descriptionText,
            valueText: valueText,
            icon: resolvedIcon,
            hintText: hintText,
            time: KsBridgeValueTransport.time(from: time),
            format: format ?? Self.defaultFormat,
            is24Hour: is24Hour?.boolValue ?? Self.defaultIs24Hour,
            pickerTitle: pickerTitle,
            accentColor: KsBridgeColor.uiColor(accentColor),
            onValueChanged: { relay.timePickerCellChanged(cellID: notifiedCellID, time: $0) },
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif
