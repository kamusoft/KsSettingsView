// KsBridgePickerCell.swift
// KsSettingsViewBridge
//
// interop 境界で `PickerCell` を輸送する `@objc` 互換 DTO。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore
import KsSettingsViewUI

/// 一覧から項目を選ぶ Cell (`PickerCell`) を輸送する DTO。
///
/// 選択値は Native の実体である index で運ぶ (maui/ADR-0012)。項目の表示整形は上位層が適用済みの
/// 主表示・副表示のペア (`KsBridgePickerItem`) として `items` に載せる。
///
/// 選択変更は `selectionMode` に応じて
/// `KsBridgeInteractionDelegate.pickerCellSelectionChanged(cellID:index:)` または
/// `pickerCellMultiSelectionChanged(cellID:indices:)` で通知される。
@objc(KsBridgePickerCell)
public final class KsBridgePickerCell: KsBridgeCell {

    /// 選択候補の項目 (表示整形済み)
    @objc public var items: [KsBridgePickerItem] = []

    /// 選択モードの序数 (`0 = Single / 1 = Multiple`)
    @objc public var selectionMode: Int = 0

    /// 単一選択モードの選択 index (未選択は `nil`)
    @objc public var selectedIndex: NSNumber?

    /// 複数選択モードの選択 index 群
    @objc public var selectedIndices: [Int] = []

    /// 複数選択モードでの選択上限 (`0` で無制限)
    @objc public var maxSelectedNumber: Int = 0

    /// 選択面のタイトル (未指定は `nil`)
    @objc public var pageTitle: String?

    /// 選択強調色 (ARGB、未指定は `nil`)
    @objc public var accentColor: NSNumber?

    override func makeCell(id: UUID, relay: KsBridgeInteractionRelay) -> any KsCell {
        let notifiedCellID = KsBridgeIdentifier.string(from: id)
        let pickerItems = KsBridgeValueTransport.pickerItems(from: items)
        switch KsBridgeValueTransport.selectionMode(from: selectionMode) {
        case .single:
            return PickerCell(
                id: id,
                style: resolvedStyle,
                title: title,
                description: descriptionText,
                valueText: valueText,
                icon: resolvedIcon,
                hintText: hintText,
                items: pickerItems,
                selectedIndex: selectedIndex?.intValue,
                pageTitle: pageTitle,
                accentColor: KsBridgeColor.uiColor(accentColor),
                onSelectionChanged: { relay.pickerCellSelectionChanged(cellID: notifiedCellID, index: $0) },
                isEnabled: isEnabled,
                isVisible: isVisible
            )
        case .multiple:
            return PickerCell(
                id: id,
                style: resolvedStyle,
                title: title,
                description: descriptionText,
                valueText: valueText,
                icon: resolvedIcon,
                hintText: hintText,
                items: pickerItems,
                selectedIndices: KsBridgeValueTransport.indexSet(from: selectedIndices),
                maxSelectedNumber: maxSelectedNumber,
                pageTitle: pageTitle,
                accentColor: KsBridgeColor.uiColor(accentColor),
                onMultiSelectionChanged: {
                    relay.pickerCellMultiSelectionChanged(cellID: notifiedCellID, indices: $0)
                },
                isEnabled: isEnabled,
                isVisible: isVisible
            )
        }
    }
}
#endif
