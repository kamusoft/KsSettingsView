// KsBridgeRadioCell.swift
// KsSettingsViewBridge
//
// interop 境界で `RadioCell` を輸送する `@objc` 互換 DTO。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore
import KsSettingsViewUI

/// 同一グループ内で 1 つだけ選択される Cell (`RadioCell`) を輸送する DTO。
///
/// 選択は `KsBridgeInteractionDelegate.radioCellSelected(cellID:value:)` で通知される。
/// グループ内の他 Cell の `selectedValue` を追随させるのは上位層の責務であり、Bridge は
/// 選択された Cell 自身の cellID と値だけを通知する。
@objc(KsBridgeRadioCell)
public final class KsBridgeRadioCell: KsBridgeCell {

    /// 同一選択グループの識別子
    @objc public var groupID: String = ""

    /// この Cell の値
    @objc public var value: String = ""

    /// グループ内の現在選択値 (`value` と一致するときチェック表示)
    @objc public var selectedValue: String = ""

    /// チェックマーク色 (ARGB、未指定は `nil`)
    @objc public var accentColor: NSNumber?

    override func makeCell(id: UUID, relay: KsBridgeInteractionRelay) -> any KsCell {
        let notifiedCellID = KsBridgeIdentifier.string(from: id)
        return RadioCell(
            id: id,
            style: resolvedStyle,
            title: title,
            description: descriptionText,
            valueText: valueText,
            icon: resolvedIcon,
            hintText: hintText,
            groupId: groupID,
            value: value,
            selectedValue: selectedValue,
            accentColor: KsBridgeColor.uiColor(accentColor),
            onSelected: { relay.radioCellSelected(cellID: notifiedCellID, value: $0) },
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif
