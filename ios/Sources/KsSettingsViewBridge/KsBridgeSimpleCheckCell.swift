// KsBridgeSimpleCheckCell.swift
// KsSettingsViewBridge
//
// interop 境界で `SimpleCheckCell` を輸送する `@objc` 互換 DTO。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore
import KsSettingsViewUI

/// 行全体のタップでチェックを切り替える Cell (`SimpleCheckCell`) を輸送する DTO。
///
/// 値変更は `KsBridgeInteractionDelegate.simpleCheckCellChanged(cellID:isChecked:)` で通知される。
@objc(KsBridgeSimpleCheckCell)
public final class KsBridgeSimpleCheckCell: KsBridgeCell {

    /// チェック状態
    @objc public var isChecked: Bool = false

    /// チェックマーク色 (ARGB、未指定は `nil`)
    @objc public var accentColor: NSNumber?

    override func makeCell(id: UUID, relay: KsBridgeInteractionRelay) -> any KsCell {
        let notifiedCellID = KsBridgeIdentifier.string(from: id)
        return SimpleCheckCell(
            id: id,
            style: resolvedStyle,
            title: title,
            description: descriptionText,
            valueText: valueText,
            icon: resolvedIcon,
            hintText: hintText,
            isChecked: isChecked,
            accentColor: KsBridgeColor.uiColor(accentColor),
            onValueChanged: { relay.simpleCheckCellChanged(cellID: notifiedCellID, isChecked: $0) },
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif
