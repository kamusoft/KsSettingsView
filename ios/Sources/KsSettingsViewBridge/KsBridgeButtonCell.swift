// KsBridgeButtonCell.swift
// KsSettingsViewBridge
//
// interop 境界で `ButtonCell` を輸送する `@objc` 互換 DTO。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore
import KsSettingsViewUI

/// ボタン用途の Cell (`ButtonCell`) を輸送する DTO。
///
/// `ButtonCell` は説明文を持たないため、基底の `descriptionText` は Native へ写されない。
/// タップは `KsBridgeInteractionDelegate.buttonCellTapped(cellID:)` で通知される。
@objc(KsBridgeButtonCell)
public final class KsBridgeButtonCell: KsBridgeCell {

    /// ボタンテキストの色 (ARGB、未指定は `nil`)
    @objc public var titleColor: NSNumber?

    /// タイトルの水平方向の揃え位置の序数 (`0 = Start / 1 = Center / 2 = End`、未指定は `nil`)
    @objc public var titleAlignment: NSNumber?

    /// `titleAlignment` 未指定のときに使う `ButtonCell` 側の既定配置。
    ///
    /// 値を写し取らず Native の既定から引く (Native 側を変えたときに輸送側が古い既定を
    /// 渡し続けることを防ぐ)。
    private static let defaultTitleAlignment: CellTitleAlignment = ButtonCell(title: "").titleAlignment

    override func makeCell(id: UUID, relay: KsBridgeInteractionRelay) -> any KsCell {
        let notifiedCellID = KsBridgeIdentifier.string(from: id)
        return ButtonCell(
            id: id,
            style: resolvedStyle,
            title: title,
            valueText: valueText,
            icon: resolvedIcon,
            hintText: hintText,
            titleColor: KsBridgeColor.uiColor(titleColor),
            onTap: { relay.buttonCellTapped(cellID: notifiedCellID) },
            titleAlignment: KsBridgeValueTransport.titleAlignment(
                from: titleAlignment,
                fallback: Self.defaultTitleAlignment
            ),
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif
