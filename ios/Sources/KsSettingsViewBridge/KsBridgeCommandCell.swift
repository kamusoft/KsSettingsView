// KsBridgeCommandCell.swift
// KsSettingsViewBridge
//
// interop 境界で `CommandCell` を輸送する `@objc` 互換 DTO。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore
import KsSettingsViewUI

/// タップで処理を実行する Cell (`CommandCell`) を輸送する DTO。
///
/// タップは `KsBridgeInteractionDelegate.commandCellTapped(cellID:)` で通知される。
@objc(KsBridgeCommandCell)
public final class KsBridgeCommandCell: KsBridgeCell {

    /// Disclosure Indicator を非表示にするフラグ
    @objc public var hideArrow: Bool = false

    override func makeCell(id: UUID, relay: KsBridgeInteractionRelay) -> any KsCell {
        let notifiedCellID = KsBridgeIdentifier.string(from: id)
        return CommandCell(
            id: id,
            style: resolvedStyle,
            title: title,
            description: descriptionText,
            valueText: valueText,
            icon: resolvedIcon,
            hintText: hintText,
            hideArrow: hideArrow,
            onTap: { relay.commandCellTapped(cellID: notifiedCellID) },
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif
