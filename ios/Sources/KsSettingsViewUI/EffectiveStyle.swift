// EffectiveStyle.swift
// KsSettingsViewUI
//
// `Theme` と `CellStyle` を合成し UIKit 描画で使う `UIColor` / `UIFont` を返すユーティリティ。
//
// スタイルは UI 層に属し Native 型（`UIColor` / `UIFont`）で表現する（core/ADR-0009）。
//
// 解決順序：
//   最終値 = cellStyle.X            if X != nil
//          else theme.cellX         if cellX != nil
//          else プラットフォーム既定（`Theme` の default 定数、または UI 層内の既定値）
//
// `cellTitleFontSize` のみ特殊で、`> 0` のとき `cellTitleFont.pointSize` を上書きする。
// `ButtonCell.titleColor` は 4 段（Cell 個別 → CellStyle → Theme → 既定）の特殊解決。

#if canImport(UIKit)
import UIKit

/// Cell の描画に使う「実効スタイル」をまとめた構造体。
///
/// `CellStyle.titleColor` のように `nil` 可のフィールドは `Theme` から補完される。
/// 本構造体は UI 描画コード内で安全に nil チェック不要に各値を読めるようにするためのもの。
public struct EffectiveStyle {
    /// iOS の最低行高さ（pt）。`Theme.rowHeight` / `CellStyle.cellHeight` の下限として用いる。
    public static let minRowHeight: CGFloat = 48

    /// タイトル文字色（Theme.headerTextColor 等とは独立。Cell 本体タイトル用）
    public let titleColor: UIColor
    /// タイトルフォント（`cellTitleFontSize > 0` のとき pointSize を上書き済み）
    public let titleFont: UIFont
    /// 説明文色
    public let descriptionColor: UIColor
    /// 説明文フォント
    public let descriptionFont: UIFont
    /// ヒントテキスト色
    public let hintTextColor: UIColor
    /// ヒントテキストフォント
    public let hintTextFont: UIFont
    /// Cell 背景色
    public let cellBackgroundColor: UIColor
    /// セパレータ色
    public let separatorColor: UIColor
    /// 選択背景色
    public let selectedBackgroundColor: UIColor
    /// アイコン辺長（pt）
    public let iconSize: CGFloat
    /// アイコン角丸（pt）
    public let iconRadius: CGFloat
    /// Cell 高さ（pt、指定がなければ `nil`）。Auto Layout 計算ベースのため通常 nil で十分。
    public let cellHeight: CGFloat?

    // MARK: - refine-basic-cells-style で追加された合成プロパティ

    /// accent 色（`CellStyle.accentColor ?? Theme.cellAccentColor`）。
    public let accentColor: UIColor
    /// 値テキスト色（`CellStyle.valueTextColor ?? Theme.cellValueTextColor ?? Theme.cellTitleColor ?? .label`）。
    public let valueTextColor: UIColor
    /// 値テキストフォント（`CellStyle.valueTextFont ?? Theme.cellValueTextFont ?? Theme.cellTitleFont ?? body`）。
    public let valueTextFont: UIFont
    /// 無効時テキスト色（`Theme.disabledTextColor`）。
    public let disabledTextColor: UIColor
    /// placeholder 文字色（`CellStyle.placeholderColor ?? Theme.cellPlaceholderColor`）。
    /// `nil` はどの段にも指定がないことを表し、プラットフォーム既定で描画する。
    public let placeholderColor: UIColor?
    /// 実効行高さ（pt）。`CellStyle.cellHeight ?? Theme.rowHeight` を MinRowHeight で下限ガード。
    public let effectiveCellHeight: CGFloat
    /// 固定高さモードか（`!Theme.hasUnevenRows`）。
    public let isFixedHeight: Bool
    /// タイトル色が CellStyle / Theme いずれかで「明示指定」されたかを示すフラグ。
    public let titleColorIsExplicit: Bool

    /// `Theme` と `CellStyle` を合成して `EffectiveStyle` を生成する。
    /// - Parameters:
    ///   - theme: 全体テーマ
    ///   - cellStyle: 個別 Cell スタイル
    public init(theme: Theme, cellStyle: CellStyle) {
        self.titleColor = Self.effectiveTitleColor(cellStyle: cellStyle, theme: theme)
        self.titleFont = Self.effectiveTitleFont(cellStyle: cellStyle, theme: theme)
        self.titleColorIsExplicit = (cellStyle.titleColor != nil) || (theme.cellTitleColor != nil)

        self.descriptionColor = Self.effectiveDescriptionColor(cellStyle: cellStyle, theme: theme)
        self.descriptionFont = Self.effectiveDescriptionFont(cellStyle: cellStyle, theme: theme)

        self.hintTextColor = Self.effectiveHintTextColor(cellStyle: cellStyle, theme: theme)
        self.hintTextFont = Self.effectiveHintFont(cellStyle: cellStyle, theme: theme)

        self.cellBackgroundColor = Self.effectiveBackgroundColor(cellStyle: cellStyle, theme: theme)
        self.separatorColor = theme.separatorColor
        self.selectedBackgroundColor = theme.selectedColor

        self.iconSize = Self.effectiveIconSize(cellStyle: cellStyle, theme: theme)
        self.iconRadius = Self.effectiveIconRadius(cellStyle: cellStyle, theme: theme)
        self.cellHeight = cellStyle.cellHeight

        self.accentColor = Self.effectiveAccentColor(cellStyle: cellStyle, theme: theme)

        self.valueTextColor = Self.effectiveValueTextColor(cellStyle: cellStyle, theme: theme)
        self.valueTextFont = Self.effectiveValueTextFont(cellStyle: cellStyle, theme: theme)

        self.disabledTextColor = theme.disabledTextColor

        self.placeholderColor = Self.effectivePlaceholderColor(cellStyle: cellStyle, theme: theme)

        self.effectiveCellHeight = Self.effectiveCellHeight(cellStyle: cellStyle, theme: theme)
        self.isFixedHeight = !theme.hasUnevenRows
    }
}

// MARK: - EffectiveStyle アクセサ群（解決順序 `CellStyle → Theme → 既定`）

extension EffectiveStyle {
    /// タイトル文字色を解決する。
    /// 解決順序: `cellStyle.titleColor` → `theme.cellTitleColor` → `UIColor.label`
    public static func effectiveTitleColor(cellStyle: CellStyle, theme: Theme) -> UIColor {
        if let c = cellStyle.titleColor { return c }
        if let c = theme.cellTitleColor { return c }
        return Theme.defaultCellTitleColor
    }

    /// タイトルフォントを解決する。
    /// 解決順序: `cellStyle.titleFont` → `theme.cellTitleFont` → body フォント。
    /// `theme.cellTitleFontSize > 0` のとき、最終フォントの pointSize を上書きする。
    public static func effectiveTitleFont(cellStyle: CellStyle, theme: Theme) -> UIFont {
        let baseFont: UIFont
        if let f = cellStyle.titleFont {
            baseFont = f
        } else if let f = theme.cellTitleFont {
            baseFont = f
        } else {
            baseFont = Theme.defaultCellTitleFont
        }
        // `cellTitleFontSize > 0` のとき、pointSize を上書きする
        if theme.cellTitleFontSize > 0 {
            let overriddenSize = CGFloat(theme.cellTitleFontSize)
            // family / weight / 装飾は維持し、size だけ差し替える
            return baseFont.withSize(overriddenSize)
        }
        return baseFont
    }

    /// description 色を解決する。
    /// 解決順序: `cellStyle.descriptionColor` → `theme.cellDescriptionColor` → `UIColor.secondaryLabel`
    public static func effectiveDescriptionColor(cellStyle: CellStyle, theme: Theme) -> UIColor {
        if let c = cellStyle.descriptionColor { return c }
        if let c = theme.cellDescriptionColor { return c }
        return Theme.defaultCellDescriptionColor
    }

    /// description フォントを解決する。
    /// 解決順序: `cellStyle.descriptionFont` → `theme.cellDescriptionFont` → footnote フォント
    public static func effectiveDescriptionFont(cellStyle: CellStyle, theme: Theme) -> UIFont {
        if let f = cellStyle.descriptionFont { return f }
        if let f = theme.cellDescriptionFont { return f }
        return Theme.defaultCellDescriptionFont
    }

    /// valueText 色を解決する。
    /// 解決順序: `cellStyle.valueTextColor` → `theme.cellValueTextColor` → `theme.cellTitleColor` → `UIColor.label`
    public static func effectiveValueTextColor(cellStyle: CellStyle, theme: Theme) -> UIColor {
        if let c = cellStyle.valueTextColor { return c }
        if let c = theme.cellValueTextColor { return c }
        if let c = theme.cellTitleColor { return c }
        return Theme.defaultCellTitleColor
    }

    /// valueText フォントを解決する。
    /// 解決順序: `cellStyle.valueTextFont` → `theme.cellValueTextFont` → `theme.cellTitleFont` → body フォント
    public static func effectiveValueTextFont(cellStyle: CellStyle, theme: Theme) -> UIFont {
        if let f = cellStyle.valueTextFont { return f }
        if let f = theme.cellValueTextFont { return f }
        if let f = theme.cellTitleFont { return f }
        return Theme.defaultCellTitleFont
    }

    /// hintText 色を解決する。
    /// 解決順序: `cellStyle.hintTextColor` → `theme.cellHintTextColor` → `theme.cellAccentColor`
    public static func effectiveHintTextColor(cellStyle: CellStyle, theme: Theme) -> UIColor {
        if let c = cellStyle.hintTextColor { return c }
        if let c = theme.cellHintTextColor { return c }
        return theme.cellAccentColor
    }

    /// hintText フォントを解決する。
    /// 解決順序: `cellStyle.hintTextFont` → `theme.cellHintFont` → footnote フォント
    public static func effectiveHintFont(cellStyle: CellStyle, theme: Theme) -> UIFont {
        if let f = cellStyle.hintTextFont { return f }
        if let f = theme.cellHintFont { return f }
        return Theme.defaultCellHintFont
    }

    /// アイコンサイズ（正方形の一辺 pt）を解決する。
    /// 解決順序: `cellStyle.iconSize` → `theme.cellIconSize` → 24pt
    ///
    /// 有効な指定は正の有限値のみで、それ以外（0 以下 / 非有限値）は未指定として次の段へ送る。
    public static func effectiveIconSize(cellStyle: CellStyle, theme: Theme) -> CGFloat {
        if let v = cellStyle.iconSize, isValidIconSize(v) { return v }
        if let v = theme.cellIconSize, isValidIconSize(v) { return v }
        return Theme.defaultCellIconSize
    }

    /// アイコン角丸半径（pt）を解決する。
    /// 解決順序: `cellStyle.iconRadius` → `theme.cellIconRadius` → 0pt（角丸なし）
    ///
    /// 有効な指定は 0 以上の有限値のみで、それ以外（負値 / 非有限値）は未指定として次の段へ送る。
    public static func effectiveIconRadius(cellStyle: CellStyle, theme: Theme) -> CGFloat {
        if let v = cellStyle.iconRadius, isValidIconRadius(v) { return v }
        if let v = theme.cellIconRadius, isValidIconRadius(v) { return v }
        return Theme.defaultCellIconRadius
    }

    /// icon 枠の一辺として有効な値か。
    ///
    /// 正の有限値だけを指定として受け付ける。0 以下では icon が描画されず、非有限値は
    /// レイアウト制約の定数として使えないため、指定ではなく未指定として扱う。
    private static func isValidIconSize(_ value: CGFloat) -> Bool {
        return value.isFinite && value > 0
    }

    /// icon の角丸半径として有効な値か。
    ///
    /// 0 以上の有限値だけを指定として受け付ける（0 は「角丸なし」という意味のある指定）。
    /// 負値・非有限値は描画できないため未指定として扱う。
    private static func isValidIconRadius(_ value: CGFloat) -> Bool {
        return value.isFinite && value >= 0
    }

    /// placeholder 文字色を解決する（Cell 固有値を伴わない `CellStyle` 以降の段）。
    /// 解決順序: `cellStyle.placeholderColor` → `theme.cellPlaceholderColor` → プラットフォーム既定（`nil`）
    ///
    /// 戻り値の `nil` は「どの段にも指定がない」ことを表し、描画側はプラットフォーム既定の
    /// placeholder 表示をそのまま使う（ライブラリ独自の既定色を持ち込まない）。
    public static func effectivePlaceholderColor(cellStyle: CellStyle, theme: Theme) -> UIColor? {
        if let c = cellStyle.placeholderColor { return c }
        return theme.cellPlaceholderColor
    }

    /// `EntryCell.placeholderColor` 用の 4 段優先 placeholder 色解決。
    ///
    /// 解決順序:
    ///   1. `entryPlaceholderColor`（`EntryCell` 個別フィールド、Cell 固有値が最優先）
    ///   2. `cellStyle.placeholderColor`
    ///   3. `theme.cellPlaceholderColor`
    ///   4. プラットフォーム既定（`nil`）
    public static func effectivePlaceholderColor(
        entryPlaceholderColor: UIColor?,
        cellStyle: CellStyle,
        theme: Theme
    ) -> UIColor? {
        if let c = entryPlaceholderColor { return c }
        return effectivePlaceholderColor(cellStyle: cellStyle, theme: theme)
    }

    /// Cell 背景色を解決する。
    /// 解決順序: `cellStyle.backgroundColor` → `theme.cellBackgroundColor`
    public static func effectiveBackgroundColor(cellStyle: CellStyle, theme: Theme) -> UIColor {
        if let c = cellStyle.backgroundColor { return c }
        return theme.cellBackgroundColor
    }

    /// accent 色を解決する。
    /// 解決順序: `cellStyle.accentColor` → `theme.cellAccentColor`
    public static func effectiveAccentColor(cellStyle: CellStyle, theme: Theme) -> UIColor {
        if let c = cellStyle.accentColor { return c }
        return theme.cellAccentColor
    }

    /// 実効行高さを解決する。
    /// 解決順序: `cellStyle.cellHeight` → `theme.rowHeight`（> 0）→ `minRowHeight`。
    /// 最終値は `minRowHeight` で下限ガードする。
    public static func effectiveCellHeight(cellStyle: CellStyle, theme: Theme) -> CGFloat {
        let baseHeight: CGFloat
        if let h = cellStyle.cellHeight {
            baseHeight = h
        } else if theme.rowHeight > 0 {
            baseHeight = CGFloat(theme.rowHeight)
        } else {
            baseHeight = Self.minRowHeight
        }
        return max(baseHeight, Self.minRowHeight)
    }

    /// `ButtonCell.titleColor` 用の 4 段優先タイトル色解決。
    ///
    /// 解決順序:
    ///   1. `buttonCellTitleColor`（ButtonCell 個別フィールド、Cell 個別最優先）
    ///   2. `cellStyle.titleColor`
    ///   3. `theme.cellTitleColor`
    ///   4. `.systemBlue`（ButtonCell の慣習的なアクセント色、プラットフォーム既定）
    ///
    /// Note: 通常 Cell のタイトル色既定は `UIColor.label` だが、ButtonCell は
    /// 「ボタンとして tappable に見える慣習色」を採用するため 4 段目だけ `.systemBlue` とする。
    /// 本ヘルパは `ButtonCellView` の本番描画から直接呼ばれ、Source of Truth として一本化する。
    public static func effectiveButtonTitleColor(
        buttonCellTitleColor: UIColor?,
        cellStyle: CellStyle,
        theme: Theme
    ) -> UIColor {
        if let c = buttonCellTitleColor { return c }
        if let c = cellStyle.titleColor { return c }
        if let c = theme.cellTitleColor { return c }
        return Theme.defaultButtonTitleColor
    }

    /// Section / Root Header のテキストフォントを解決する。
    ///
    /// `Theme.headerFont` / `Theme.headerFontSize` の解決順序：
    /// 1. `theme.headerFont != nil` のとき、ベースフォントを `headerFont` とする
    /// 2. `nil` のとき、ベースフォントを footnote 既定 (`UIFont.preferredFont(forTextStyle: .footnote)`) とする
    /// 3. `theme.headerFontSize > 0` のとき、ベースフォントの size を `headerFontSize` で上書きする
    ///
    /// 本ヘルパは `KsSettingsViewController` の Section / Root Header 描画経路から呼ばれ、
    /// 「size 上書き」「フォントフォールバック」の責務を 1 箇所に集約する。
    public static func effectiveHeaderFont(theme: Theme) -> UIFont {
        let base = theme.headerFont ?? Theme.defaultHeaderFooterFont
        if theme.headerFontSize > 0 {
            return base.withSize(CGFloat(theme.headerFontSize))
        }
        return base
    }

    /// Section / Root Footer のテキストフォントを解決する。
    ///
    /// 解決順序は `effectiveHeaderFont` と同形で、`footerFont` / `footerFontSize` を参照する。
    public static func effectiveFooterFont(theme: Theme) -> UIFont {
        let base = theme.footerFont ?? Theme.defaultHeaderFooterFont
        if theme.footerFontSize > 0 {
            return base.withSize(CGFloat(theme.footerFontSize))
        }
        return base
    }
}
#endif
