// KsBridgeCheckboxCell.swift
// KsSettingsViewBridge
//
// interop 境界で `CheckboxCell` を輸送する `@objc` 互換 DTO。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore
import KsSettingsViewUI

/// チェックボックス Cell (`CheckboxCell`) を輸送する DTO。
///
/// 値変更は `KsBridgeInteractionDelegate.checkboxCellChanged(cellID:isChecked:)` で通知される。
@objc(KsBridgeCheckboxCell)
public final class KsBridgeCheckboxCell: KsBridgeCell {

    /// チェック状態
    @objc public var isChecked: Bool = false

    /// チェックマーク色 (ARGB、未指定は `nil`)
    @objc public var accentColor: NSNumber?

    override func makeCell(id: UUID, relay: KsBridgeInteractionRelay) -> any KsCell {
        let notifiedCellID = KsBridgeIdentifier.string(from: id)
        return CheckboxCell(
            id: id,
            style: resolvedStyle,
            title: title,
            description: descriptionText,
            valueText: valueText,
            icon: resolvedIcon,
            hintText: hintText,
            isChecked: isChecked,
            accentColor: KsBridgeColor.uiColor(accentColor),
            onValueChanged: { relay.checkboxCellChanged(cellID: notifiedCellID, isChecked: $0) },
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif
