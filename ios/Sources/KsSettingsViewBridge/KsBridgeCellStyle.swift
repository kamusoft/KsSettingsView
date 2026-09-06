// KsBridgeCellStyle.swift
// KsSettingsViewBridge
//
// interop 境界で `CellStyle` を輸送するプリミティブ表現の DTO。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewUI

/// Cell 個別スタイルを interop 境界で輸送する DTO。
///
/// 輸送するのは MAUI の Cell が CellStyle 段として公開する項目 — title / description / valueText /
/// hint の色とフォント、icon の寸法、行の高さ、行の背景色。色は ARGB を詰めた 32bit 整数、
/// フォントは `KsBridgeFont` の記述子、寸法は数値で表し、`nil` は「未指定 → Theme から継承」を
/// 意味する (maui/ADR-0004)。
///
/// Native の `CellStyle` が持つ accent と placeholder の CellStyle 段は MAUI から設定する手段が
/// なく、MAUI の `AccentColor` / `PlaceholderColor` は Cell 固有段として Cell 種別ごとの DTO
/// (`KsBridgeSwitchCell` / `KsBridgeEntryCell` 等) で運ぶ。`accentColor` の枠は wire 形式として
/// 残るが MAUI からは設定されず、placeholder の枠はこの DTO に無い。
///
/// この DTO は輸送専用であり、利用者向けのスタイル公開契約ではない。
@objc(KsBridgeCellStyle)
public final class KsBridgeCellStyle: NSObject {

    /// タイトル文字色 (ARGB)
    @objc public var titleColor: NSNumber?
    /// タイトルフォント
    @objc public var titleFont: KsBridgeFont?
    /// 説明文色 (ARGB)
    @objc public var descriptionColor: NSNumber?
    /// 説明文フォント
    @objc public var descriptionFont: KsBridgeFont?
    /// 値テキスト色 (ARGB)
    @objc public var valueTextColor: NSNumber?
    /// 値テキストフォント
    @objc public var valueTextFont: KsBridgeFont?
    /// アイコンサイズ (pt)
    @objc public var iconSize: NSNumber?
    /// アイコン角丸半径 (pt)
    @objc public var iconRadius: NSNumber?
    /// Cell 高さ (pt)
    @objc public var cellHeight: NSNumber?
    /// ヒントテキスト色 (ARGB)
    @objc public var hintTextColor: NSNumber?
    /// ヒントテキストフォント
    @objc public var hintTextFont: KsBridgeFont?
    /// Cell 個別背景色 (ARGB)
    @objc public var backgroundColor: NSNumber?
    /// Cell 個別 accent 色 (ARGB)
    @objc public var accentColor: NSNumber?

    /// 全項目が未指定の DTO を生成する。
    @objc public override init() {
        super.init()
    }

    /// DTO から Native の `CellStyle` を解決する。未指定の項目は `nil` (Theme 継承) のままにする。
    internal func resolve() -> CellStyle {
        return CellStyle(
            titleColor: KsBridgeColor.uiColor(titleColor),
            titleFont: titleFont?.resolve(),
            descriptionColor: KsBridgeColor.uiColor(descriptionColor),
            descriptionFont: descriptionFont?.resolve(),
            valueTextColor: KsBridgeColor.uiColor(valueTextColor),
            valueTextFont: valueTextFont?.resolve(),
            iconSize: iconSize.map { CGFloat($0.doubleValue) },
            iconRadius: iconRadius.map { CGFloat($0.doubleValue) },
            cellHeight: cellHeight.map { CGFloat($0.doubleValue) },
            hintTextColor: KsBridgeColor.uiColor(hintTextColor),
            hintTextFont: hintTextFont?.resolve(),
            backgroundColor: KsBridgeColor.uiColor(backgroundColor),
            accentColor: KsBridgeColor.uiColor(accentColor)
        )
    }
}
#endif
