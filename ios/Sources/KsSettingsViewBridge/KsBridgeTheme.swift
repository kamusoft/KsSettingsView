// KsBridgeTheme.swift
// KsSettingsViewBridge
//
// interop 境界で `Theme` を輸送するプリミティブ表現の DTO。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewUI

/// Theme を interop 境界で輸送する DTO。
///
/// 項目は Native の `Theme` の公開項目と 1 対 1 で対応する。色は ARGB を詰めた 32bit 整数、
/// フォントは `KsBridgeFont` の記述子、寸法とフラグは数値で表し、`nil` は「未指定」を意味する
/// (maui/ADR-0004)。未指定の項目は `Theme` 側の未指定 (既定値) として扱われる。
///
/// この DTO は輸送専用であり、利用者向けの Theme 公開契約ではない。
@objc(KsBridgeTheme)
public final class KsBridgeTheme: NSObject {

    // MARK: - 全体背景・装飾

    /// セパレータ色 (ARGB)
    @objc public var separatorColor: NSNumber?
    /// SettingsView 自身の背景色 (ARGB)
    @objc public var backgroundColor: NSNumber?
    /// Cell 既定背景色 (ARGB)
    @objc public var cellBackgroundColor: NSNumber?
    /// Cell 選択時の背景色 (ARGB)
    @objc public var selectedColor: NSNumber?
    /// アクセント色 (ARGB)
    @objc public var cellAccentColor: NSNumber?
    /// 無効時のテキスト置換色 (ARGB)
    @objc public var disabledTextColor: NSNumber?
    /// スクロールインジケータ表示 (真偽値)
    @objc public var scrollIndicatorVisible: NSNumber?

    // MARK: - 行高さ

    /// 行高さ基準値 (整数 pt)
    @objc public var rowHeight: NSNumber?
    /// 可変高さフラグ (真偽値)
    @objc public var hasUnevenRows: NSNumber?

    // MARK: - Header / Footer

    /// Section ヘッダのテキスト色 (ARGB)
    @objc public var headerTextColor: NSNumber?
    /// Section ヘッダの背景色 (ARGB)
    @objc public var headerBackgroundColor: NSNumber?
    /// Section ヘッダ既定フォントサイズ (pt)
    @objc public var headerFontSize: NSNumber?
    /// Section ヘッダ既定フォント
    @objc public var headerFont: KsBridgeFont?
    /// Section ヘッダの既定高さ (pt)
    @objc public var headerHeight: NSNumber?
    /// Section フッタのテキスト色 (ARGB)
    @objc public var footerTextColor: NSNumber?
    /// Section フッタの背景色 (ARGB)
    @objc public var footerBackgroundColor: NSNumber?
    /// Section フッタ既定フォントサイズ (pt)
    @objc public var footerFontSize: NSNumber?
    /// Section フッタ既定フォント
    @objc public var footerFont: KsBridgeFont?

    // MARK: - Cell 全体既定

    /// Cell タイトル既定色 (ARGB)
    @objc public var cellTitleColor: NSNumber?
    /// Cell タイトル既定フォント
    @objc public var cellTitleFont: KsBridgeFont?
    /// Cell タイトル既定フォントサイズ (pt)
    @objc public var cellTitleFontSize: NSNumber?
    /// valueText 既定色 (ARGB)
    @objc public var cellValueTextColor: NSNumber?
    /// valueText 既定フォント
    @objc public var cellValueTextFont: KsBridgeFont?
    /// description 既定色 (ARGB)
    @objc public var cellDescriptionColor: NSNumber?
    /// description 既定フォント
    @objc public var cellDescriptionFont: KsBridgeFont?
    /// hintText 既定色 (ARGB)
    @objc public var cellHintTextColor: NSNumber?
    /// hintText 既定フォント
    @objc public var cellHintFont: KsBridgeFont?
    /// EntryCell の placeholder 既定色 (ARGB)
    @objc public var cellPlaceholderColor: NSNumber?
    /// アイコン既定サイズ (pt)
    @objc public var cellIconSize: NSNumber?
    /// アイコン既定角丸半径 (pt)
    @objc public var cellIconRadius: NSNumber?

    // MARK: - Section 装飾

    /// Section 単位の外側余白の上成分 (pt)
    ///
    /// 余白の 4 成分は全体で 1 つの指定として扱う。1 つでも `nil` なら余白全体が未指定になる。
    @objc public var sectionMarginTop: NSNumber?
    /// Section 単位の外側余白の leading 成分 (pt)
    @objc public var sectionMarginLeading: NSNumber?
    /// Section 単位の外側余白の下成分 (pt)
    @objc public var sectionMarginBottom: NSNumber?
    /// Section 単位の外側余白の trailing 成分 (pt)
    @objc public var sectionMarginTrailing: NSNumber?
    /// Section の箱の角丸半径 (pt)
    @objc public var sectionCornerRadius: NSNumber?
    /// Section の箱のボーダー幅 (pt)
    @objc public var sectionBorderWidth: NSNumber?
    /// Section の箱のボーダー色 (ARGB)
    @objc public var sectionBorderColor: NSNumber?

    /// 全項目が未指定の DTO を生成する。
    @objc public override init() {
        super.init()
    }

    /// DTO から Native の `Theme` を解決する。未指定の項目は `Theme` の既定値を用いる。
    internal func resolve() -> Theme {
        let base = Theme()
        return Theme(
            separatorColor: KsBridgeColor.uiColor(separatorColor) ?? base.separatorColor,
            backgroundColor: KsBridgeColor.uiColor(backgroundColor) ?? base.backgroundColor,
            cellBackgroundColor: KsBridgeColor.uiColor(cellBackgroundColor) ?? base.cellBackgroundColor,
            selectedColor: KsBridgeColor.uiColor(selectedColor) ?? base.selectedColor,
            cellAccentColor: KsBridgeColor.uiColor(cellAccentColor) ?? base.cellAccentColor,
            disabledTextColor: KsBridgeColor.uiColor(disabledTextColor) ?? base.disabledTextColor,
            scrollIndicatorVisible: scrollIndicatorVisible?.boolValue ?? base.scrollIndicatorVisible,
            rowHeight: rowHeight?.intValue ?? base.rowHeight,
            hasUnevenRows: hasUnevenRows?.boolValue ?? base.hasUnevenRows,
            headerTextColor: KsBridgeColor.uiColor(headerTextColor) ?? base.headerTextColor,
            headerBackgroundColor: KsBridgeColor.uiColor(headerBackgroundColor) ?? base.headerBackgroundColor,
            headerFontSize: headerFontSize?.doubleValue ?? base.headerFontSize,
            headerFont: headerFont?.resolve(),
            headerHeight: headerHeight?.doubleValue ?? base.headerHeight,
            footerTextColor: KsBridgeColor.uiColor(footerTextColor) ?? base.footerTextColor,
            footerBackgroundColor: KsBridgeColor.uiColor(footerBackgroundColor) ?? base.footerBackgroundColor,
            footerFontSize: footerFontSize?.doubleValue ?? base.footerFontSize,
            footerFont: footerFont?.resolve(),
            cellTitleColor: KsBridgeColor.uiColor(cellTitleColor),
            cellTitleFont: cellTitleFont?.resolve(),
            cellTitleFontSize: cellTitleFontSize?.doubleValue ?? base.cellTitleFontSize,
            cellValueTextColor: KsBridgeColor.uiColor(cellValueTextColor),
            cellValueTextFont: cellValueTextFont?.resolve(),
            cellDescriptionColor: KsBridgeColor.uiColor(cellDescriptionColor),
            cellDescriptionFont: cellDescriptionFont?.resolve(),
            cellHintTextColor: KsBridgeColor.uiColor(cellHintTextColor),
            cellHintFont: cellHintFont?.resolve(),
            cellPlaceholderColor: KsBridgeColor.uiColor(cellPlaceholderColor),
            cellIconSize: cellIconSize.map { CGFloat($0.doubleValue) },
            cellIconRadius: cellIconRadius.map { CGFloat($0.doubleValue) },
            sectionMargin: resolvedSectionMargin(),
            sectionCornerRadius: sectionCornerRadius.map { CGFloat($0.doubleValue) },
            sectionBorderWidth: sectionBorderWidth.map { CGFloat($0.doubleValue) },
            sectionBorderColor: KsBridgeColor.uiColor(sectionBorderColor)
        )
    }

    /// margin の論理 4 成分から方向対応型を組み立てる。
    ///
    /// 4 成分は余白全体で 1 つの指定であり、1 つでも未指定なら余白全体を未指定として解決する。
    private func resolvedSectionMargin() -> NSDirectionalEdgeInsets? {
        guard let top = sectionMarginTop,
              let leading = sectionMarginLeading,
              let bottom = sectionMarginBottom,
              let trailing = sectionMarginTrailing else {
            return nil
        }
        return NSDirectionalEdgeInsets(
            top: CGFloat(top.doubleValue),
            leading: CGFloat(leading.doubleValue),
            bottom: CGFloat(bottom.doubleValue),
            trailing: CGFloat(trailing.doubleValue)
        )
    }
}
#endif
