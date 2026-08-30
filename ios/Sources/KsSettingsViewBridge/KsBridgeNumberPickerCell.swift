// KsBridgeNumberPickerCell.swift
// KsSettingsViewBridge
//
// interop 境界で `NumberPickerCell` を輸送する `@objc` 互換 DTO。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore
import KsSettingsViewUI

/// 数値を選ぶ Cell (`NumberPickerCell`) を輸送する DTO。
///
/// 値変更は `KsBridgeInteractionDelegate.numberPickerCellChanged(cellID:value:)` で通知される。
@objc(KsBridgeNumberPickerCell)
public final class KsBridgeNumberPickerCell: KsBridgeCell {

    /// 選択できる最小値
    @objc public var min: Int = 0

    /// 選択できる最大値
    @objc public var max: Int = 100

    /// 選択の刻み幅
    @objc public var step: Int = 1

    /// 現在の値
    @objc public var value: Int = 0

    /// 値に付ける単位文字列 (空文字列で単位なし)
    @objc public var unit: String = ""

    /// 選択面のタイトル (未指定は `nil`)
    @objc public var pickerTitle: String?

    /// 選択強調色 (ARGB、未指定は `nil`)
    @objc public var accentColor: NSNumber?

    override func makeCell(id: UUID, relay: KsBridgeInteractionRelay) -> any KsCell {
        let notifiedCellID = KsBridgeIdentifier.string(from: id)
        return NumberPickerCell(
            id: id,
            style: resolvedStyle,
            title: title,
            description: descriptionText,
            valueText: valueText,
            icon: resolvedIcon,
            hintText: hintText,
            min: min,
            max: max,
            step: step,
            value: value,
            unit: unit,
            pickerTitle: pickerTitle,
            accentColor: KsBridgeColor.uiColor(accentColor),
            onValueChanged: { relay.numberPickerCellChanged(cellID: notifiedCellID, value: $0) },
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif
