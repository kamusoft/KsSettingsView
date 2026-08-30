// KsBridgeEntryCell.swift
// KsSettingsViewBridge
//
// interop 境界で `EntryCell` を輸送する `@objc` 互換 DTO。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore
import KsSettingsViewUI

/// テキスト入力欄を持つ Cell (`EntryCell`) を輸送する DTO。
///
/// `EntryCell` は値文字列を持たないため、基底の `valueText` は Native へ写されない。
/// テキスト変更は `KsBridgeInteractionDelegate.entryCellTextChanged(cellID:text:)` で通知される。
@objc(KsBridgeEntryCell)
public final class KsBridgeEntryCell: KsBridgeCell {

    /// 現在のテキスト値
    @objc public var text: String = ""

    /// プレースホルダ (未指定は `nil`)
    @objc public var placeholder: String?

    /// プレースホルダ文字色 (ARGB、未指定は `nil`)
    @objc public var placeholderColor: NSNumber?

    /// キーボード種別の序数 (`0 = Default / 1 = Plain / 2 = Text / 3 = Chat / 4 = Url /
    /// 5 = Email / 6 = Numeric / 7 = Telephone`)
    @objc public var keyboard: Int = 0

    /// パスワードマスクフラグ
    @objc public var isPassword: Bool = false

    /// テキスト配置の序数 (`0 = Start / 1 = Center / 2 = End`、未指定は `nil`)
    @objc public var textAlignment: NSNumber?

    /// caret 色および選択ハイライト色 (ARGB、未指定は `nil`)
    @objc public var accentColor: NSNumber?

    /// 最大文字数 (未指定は `nil` で無制限)
    @objc public var maxLength: NSNumber?

    /// `textAlignment` 未指定のときに使う `EntryCell` 側の既定配置。
    ///
    /// 値を写し取らず Native の既定から引く (Native 側を変えたときに輸送側が古い既定を
    /// 渡し続けることを防ぐ)。
    private static let defaultTextAlignment: CellTitleAlignment =
        EntryCell(title: "", text: "").textAlignment

    override func makeCell(id: UUID, relay: KsBridgeInteractionRelay) -> any KsCell {
        let notifiedCellID = KsBridgeIdentifier.string(from: id)
        return EntryCell(
            id: id,
            style: resolvedStyle,
            title: title,
            description: descriptionText,
            icon: resolvedIcon,
            hintText: hintText,
            text: text,
            placeholder: placeholder,
            placeholderColor: KsBridgeColor.uiColor(placeholderColor),
            keyboardType: KsBridgeValueTransport.keyboardType(from: keyboard),
            isPassword: isPassword,
            textAlignment: KsBridgeValueTransport.titleAlignment(
                from: textAlignment,
                fallback: Self.defaultTextAlignment
            ),
            accentColor: KsBridgeColor.uiColor(accentColor),
            maxLength: maxLength?.intValue,
            onTextChanged: { relay.entryCellTextChanged(cellID: notifiedCellID, text: $0) },
            isEnabled: isEnabled,
            isVisible: isVisible
        )
    }
}
#endif
