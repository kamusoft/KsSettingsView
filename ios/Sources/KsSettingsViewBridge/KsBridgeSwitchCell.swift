// KsBridgeSwitchCell.swift
// KsSettingsViewBridge
//
// interop 境界で `SwitchCell` を輸送する `@objc` 互換 DTO。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore
import KsSettingsViewUI

/// ON/OFF スイッチを持つ Cell (`SwitchCell`) を輸送する DTO。
///
/// 値変更は `KsBridgeInteractionDelegate.switchCellChanged(cellID:isOn:)` で通知される。
@objc(KsBridgeSwitchCell)
public final class KsBridgeSwitchCell: KsBridgeCell {

    /// 現在の ON/OFF 値
    @objc public var isOn: Bool = false

    /// スイッチ ON 時の色 (ARGB、未指定は `nil`)
    @objc public var accentColor: NSNumber?

    override func makeCell(id: UUID, relay: KsBridgeInteractionRelay) -> any KsCell {
        let notifiedCellID = KsBridgeIdentifier.string(from: id)
        return SwitchCell(
            id: id,
            style: resolvedStyle,
            title: title,
            description: descriptionText,
            valueText: valueText,
            icon: resolvedIcon,
            hintText: hintText,
            isOn: isOn,
            accentColor: KsBridgeColor.uiColor(accentColor),
            onValueChanged: { relay.switchCellChanged(cellID: notifiedCellID, isOn: $0) },
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif
