// Theme.swift
// KsSettingsViewUI
//
// SettingsView 全体に適用されるスタイル値型（UI 層所属）。
// フィールド型は UIKit Native 型 (`UIColor` / `UIFont` / `CGFloat`) を直接保持する。
//
// スタイルは Core ではなく UI 層に置き、Native 型で表現する（core/ADR-0009）。
//
// 主なフィールドは以下：
// - 画面全体の背景色（`backgroundColor`）
// - Cell 全体既定（`cellTitle*` / `cellValueText*` / `cellDescription*` / `cellHint*` / `cellIcon*`）
// - Header / Footer Font（`headerFont` / `footerFont`）および `headerHeight`
// - `cellTitleFontSize`（`cellTitleFont` の pointSize を上書きする独立 `Double` フィールド）
// - Section 装飾（`sectionMargin` / `sectionCornerRadius` / `sectionBorderWidth` / `sectionBorderColor`）

#if canImport(UIKit)
import UIKit

/// SettingsView 全体に適用されるスタイル値型。
///
/// すべての色フィールドは `UIColor`、フォントフィールドは `UIFont` を直接保持する。
/// 中間の論理色・論理フォント表現は経由しない（core/ADR-0009）。
///
/// `UIColor` / `UIFont` は Swift の `Equatable` に準拠していないため、本型の `==` は
/// 各フィールドについて `isEqual(_:)` ベースの手動実装で判定する。
///
/// 「Cell 全体既定」フィールド群（`cellTitleColor` / `cellDescriptionColor` 等）は
/// 個別 Cell の `CellStyle.X` が `nil` のときの **フォールバック値** として参照される
/// （解決順序: `CellStyle.X` → `Theme.cellX` → プラットフォーム既定）。
///
/// `cellTitleFontSize` は `cellTitleFont` と並立する独立 `Double` フィールドで、
/// `> 0` のとき `cellTitleFont.pointSize` を **上書き** する（オリジナル
/// `AiForms.Maui.SettingsView.SettingsView.CellTitleFontSize` との運用互換のため）。
///
/// `@unchecked Sendable` の根拠：
/// - 全フィールドが `let`（immutable）。
/// - 構成要素の `UIColor` / `UIFont` は Apple 実装上、内部状態が事実上 immutable で thread-safe。
/// - Swift 6.2 の strict concurrency では `UIColor` / `UIFont` が Sendable 適合していないため、
///   現実的な thread 安全性を確保した上で `@unchecked` を明示する。
public struct Theme: Equatable, @unchecked Sendable {
    // MARK: - 全体背景・装飾

    /// セパレータ色
    public let separatorColor: UIColor
    /// SettingsView (UICollectionView) 自身の背景色。`cellBackgroundColor` とは独立。
    public let backgroundColor: UIColor
    /// Cell 既定背景色
    public let cellBackgroundColor: UIColor
    /// Cell 選択時の背景色
    public let selectedColor: UIColor
    /// アクセント色（選択系 Cell の着色既定値）
    public let cellAccentColor: UIColor
    /// `isEnabled = false` 時のテキスト置換色
    public let disabledTextColor: UIColor
    /// スクロールインジケータ表示
    public let scrollIndicatorVisible: Bool

    // MARK: - 行高さ

    /// 行高さ基準値（論理 pt、整数）。`-1` は未指定。
    public let rowHeight: Int
    /// 可変高さフラグ（`true` で個別 Cell ごとに可変 + 最低高さ保証、`false` で全 Cell 一律固定）。
    ///
    /// **既定値は `true`**（Auto 高さ + 下限保証）。これはオリジナル AiForms iOS
    /// （`AiTableView.RowHeight = UITableView.AutomaticDimension` + `MinRowHeight = 48`）の
    /// 既定挙動を踏襲したもので、`Theme()` を引数なしで構築した状態では各 Cell が内容に応じて
    /// 自然に伸縮し、`description` / `hintText` 等が見切れない。
    /// 「全 Cell を一律固定高さで揃えたい」場合のみ `Theme(hasUnevenRows: false)` を明示指定する。
    public let hasUnevenRows: Bool

    // MARK: - Header / Footer

    /// Section ヘッダのテキスト色
    public let headerTextColor: UIColor
    /// Section ヘッダの背景色
    public let headerBackgroundColor: UIColor
    /// Section ヘッダ既定フォントサイズ（論理単位 pt、`-1` は未指定）
    public let headerFontSize: Double
    /// Section ヘッダ既定フォント（family / weight / 装飾を含む。`nil` は未指定）
    ///
    /// `headerFontSize > 0` かつ `headerFont != nil` のとき、size は `headerFontSize` 優先。
    public let headerFont: UIFont?
    /// SettingsView 全体に適用される Section Header の既定高さ（論理 pt）。
    /// `-1.0` は未指定（自動）を表す。
    public let headerHeight: Double
    /// Section フッタのテキスト色
    public let footerTextColor: UIColor
    /// Section フッタの背景色
    public let footerBackgroundColor: UIColor
    /// Section フッタ既定フォントサイズ（論理単位 pt、`-1` は未指定）
    public let footerFontSize: Double
    /// Section フッタ既定フォント（family / weight / 装飾を含む。`nil` は未指定）
    ///
    /// `footerFontSize > 0` かつ `footerFont != nil` のとき、size は `footerFontSize` 優先。
    public let footerFont: UIFont?

    // MARK: - Cell 全体既定（オリジナル `CellXxx` 系の Theme 昇格）

    /// Cell タイトル既定色（`nil` は未指定 → `UIColor.label`）
    public let cellTitleColor: UIColor?
    /// Cell タイトル既定フォント（`nil` は未指定 → `UIFont.preferredFont(forTextStyle: .body)`）
    public let cellTitleFont: UIFont?
    /// Cell タイトル既定フォントサイズ（独立 `Double`、`-1.0` は未指定）。
    ///
    /// `> 0` のとき、`cellTitleFont.pointSize` を上書きする。family / weight 等は `cellTitleFont` 由来のまま維持。
    public let cellTitleFontSize: Double
    /// LabelCell / CommandCell の valueText 既定色。`nil` は未指定 → `cellTitleColor` 等にフォールバック。
    public let cellValueTextColor: UIColor?
    /// LabelCell / CommandCell の valueText 既定フォント。`nil` は未指定 → `cellTitleFont` 等にフォールバック。
    public let cellValueTextFont: UIFont?
    /// description の既定色。`nil` は未指定 → `UIColor.secondaryLabel` 相当にフォールバック。
    public let cellDescriptionColor: UIColor?
    /// description の既定フォント。`nil` は未指定 → footnote 相当フォントにフォールバック。
    public let cellDescriptionFont: UIFont?
    /// hintText の既定色。`nil` は未指定 → `cellAccentColor` にフォールバック。
    public let cellHintTextColor: UIColor?
    /// hintText の既定フォント。`nil` は未指定 → footnote 相当フォントにフォールバック。
    public let cellHintFont: UIFont?
    /// EntryCell の placeholder 既定色。`nil` は未指定 → プラットフォーム既定 (システムの placeholder 色)。
    public let cellPlaceholderColor: UIColor?
    /// アイコンの既定サイズ（正方形の一辺 pt）。`nil` は未指定 → 24pt にフォールバック。
    public let cellIconSize: CGFloat?
    /// アイコンの既定角丸半径（pt）。`nil` は未指定 → 0pt（角丸なし）にフォールバック。
    public let cellIconRadius: CGFloat?

    // MARK: - Section 装飾

    /// Section 単位（Header・Cell の箱・Footer を一体とした表示単位）の**外側**余白。
    ///
    /// `nil` は未指定を表し、style ごとの既定へ解決する（`.modern` はライブラリ既定の箱の余白、
    /// `.classic` は上下 0）。水平成分は leading / trailing 基準で解釈し、`.classic` では
    /// 「Section 境界は全幅」の契約を保つため無視する。負の成分は 0 として扱う。
    ///
    /// 隣接 Section 間の間隔は前 Section の bottom と次 Section の top の加算になる。
    public let sectionMargin: NSDirectionalEdgeInsets?
    /// `.modern` の Section の箱の角丸半径（pt）。`nil` は未指定 → ライブラリ既定へ解決する。
    ///
    /// 箱の寸法に対して大きすぎる値は描画時に幾何的に許される値へ clamp する（構築時には拒否しない）。
    public let sectionCornerRadius: CGFloat?
    /// `.modern` の Section の箱のボーダー幅（pt）。`nil` は未指定 → 実効 0（ボーダーなし）。
    public let sectionBorderWidth: CGFloat?
    /// `.modern` の Section の箱のボーダー色。`nil` は未指定 → 実効透明。
    public let sectionBorderColor: UIColor?

    /// 任意フィールドを指定して `Theme` を生成する。
    public init(
        separatorColor: UIColor = Theme.defaultSeparatorColor,
        backgroundColor: UIColor = Theme.defaultBackgroundColor,
        cellBackgroundColor: UIColor = .white,
        selectedColor: UIColor = Theme.defaultSelectedColor,
        cellAccentColor: UIColor = Theme.defaultAccentColor,
        disabledTextColor: UIColor = Theme.defaultDisabledTextColor,
        scrollIndicatorVisible: Bool = true,
        rowHeight: Int = -1,
        hasUnevenRows: Bool = true,
        headerTextColor: UIColor = Theme.defaultHeaderTextColor,
        headerBackgroundColor: UIColor = Theme.defaultHeaderBackgroundColor,
        headerFontSize: Double = -1,
        headerFont: UIFont? = nil,
        headerHeight: Double = -1.0,
        footerTextColor: UIColor = Theme.defaultFooterTextColor,
        footerBackgroundColor: UIColor = Theme.defaultFooterBackgroundColor,
        footerFontSize: Double = -1,
        footerFont: UIFont? = nil,
        cellTitleColor: UIColor? = nil,
        cellTitleFont: UIFont? = nil,
        cellTitleFontSize: Double = -1.0,
        cellValueTextColor: UIColor? = nil,
        cellValueTextFont: UIFont? = nil,
        cellDescriptionColor: UIColor? = nil,
        cellDescriptionFont: UIFont? = nil,
        cellHintTextColor: UIColor? = nil,
        cellHintFont: UIFont? = nil,
        cellPlaceholderColor: UIColor? = nil,
        cellIconSize: CGFloat? = nil,
        cellIconRadius: CGFloat? = nil,
        sectionMargin: NSDirectionalEdgeInsets? = nil,
        sectionCornerRadius: CGFloat? = nil,
        sectionBorderWidth: CGFloat? = nil,
        sectionBorderColor: UIColor? = nil
    ) {
        self.separatorColor = separatorColor
        self.backgroundColor = backgroundColor
        self.cellBackgroundColor = cellBackgroundColor
        self.selectedColor = selectedColor
        self.cellAccentColor = cellAccentColor
        self.disabledTextColor = disabledTextColor
        self.scrollIndicatorVisible = scrollIndicatorVisible
        self.rowHeight = rowHeight
        self.hasUnevenRows = hasUnevenRows
        self.headerTextColor = headerTextColor
        self.headerBackgroundColor = headerBackgroundColor
        self.headerFontSize = headerFontSize
        self.headerFont = headerFont
        self.headerHeight = headerHeight
        self.footerTextColor = footerTextColor
        self.footerBackgroundColor = footerBackgroundColor
        self.footerFontSize = footerFontSize
        self.footerFont = footerFont
        self.cellTitleColor = cellTitleColor
        self.cellTitleFont = cellTitleFont
        self.cellTitleFontSize = cellTitleFontSize
        self.cellValueTextColor = cellValueTextColor
        self.cellValueTextFont = cellValueTextFont
        self.cellDescriptionColor = cellDescriptionColor
        self.cellDescriptionFont = cellDescriptionFont
        self.cellHintTextColor = cellHintTextColor
        self.cellHintFont = cellHintFont
        self.cellPlaceholderColor = cellPlaceholderColor
        self.cellIconSize = cellIconSize
        self.cellIconRadius = cellIconRadius
        self.sectionMargin = sectionMargin
        self.sectionCornerRadius = sectionCornerRadius
        self.sectionBorderWidth = sectionBorderWidth
        self.sectionBorderColor = sectionBorderColor
    }

    // MARK: - Equatable 手動実装

    public static func == (lhs: Theme, rhs: Theme) -> Bool {
        return uiColorEqual(lhs.separatorColor, rhs.separatorColor)
            && uiColorEqual(lhs.backgroundColor, rhs.backgroundColor)
            && uiColorEqual(lhs.cellBackgroundColor, rhs.cellBackgroundColor)
            && uiColorEqual(lhs.selectedColor, rhs.selectedColor)
            && uiColorEqual(lhs.cellAccentColor, rhs.cellAccentColor)
            && uiColorEqual(lhs.disabledTextColor, rhs.disabledTextColor)
            && lhs.scrollIndicatorVisible == rhs.scrollIndicatorVisible
            && lhs.rowHeight == rhs.rowHeight
            && lhs.hasUnevenRows == rhs.hasUnevenRows
            && uiColorEqual(lhs.headerTextColor, rhs.headerTextColor)
            && uiColorEqual(lhs.headerBackgroundColor, rhs.headerBackgroundColor)
            && lhs.headerFontSize == rhs.headerFontSize
            && uiFontEqualOptional(lhs.headerFont, rhs.headerFont)
            && lhs.headerHeight == rhs.headerHeight
            && uiColorEqual(lhs.footerTextColor, rhs.footerTextColor)
            && uiColorEqual(lhs.footerBackgroundColor, rhs.footerBackgroundColor)
            && lhs.footerFontSize == rhs.footerFontSize
            && uiFontEqualOptional(lhs.footerFont, rhs.footerFont)
            && uiColorEqualOptional(lhs.cellTitleColor, rhs.cellTitleColor)
            && uiFontEqualOptional(lhs.cellTitleFont, rhs.cellTitleFont)
            && lhs.cellTitleFontSize == rhs.cellTitleFontSize
            && uiColorEqualOptional(lhs.cellValueTextColor, rhs.cellValueTextColor)
            && uiFontEqualOptional(lhs.cellValueTextFont, rhs.cellValueTextFont)
            && uiColorEqualOptional(lhs.cellDescriptionColor, rhs.cellDescriptionColor)
            && uiFontEqualOptional(lhs.cellDescriptionFont, rhs.cellDescriptionFont)
            && uiColorEqualOptional(lhs.cellHintTextColor, rhs.cellHintTextColor)
            && uiFontEqualOptional(lhs.cellHintFont, rhs.cellHintFont)
            && uiColorEqualOptional(lhs.cellPlaceholderColor, rhs.cellPlaceholderColor)
            && lhs.cellIconSize == rhs.cellIconSize
            && lhs.cellIconRadius == rhs.cellIconRadius
            && lhs.sectionMargin == rhs.sectionMargin
            && lhs.sectionCornerRadius == rhs.sectionCornerRadius
            && lhs.sectionBorderWidth == rhs.sectionBorderWidth
            && uiColorEqualOptional(lhs.sectionBorderColor, rhs.sectionBorderColor)
    }
}

// MARK: - 既定色プリセット

extension Theme {
    /// システム標準の灰色 separator（おおよそ #C8C7CC）
    public static let defaultSeparatorColor = UIColor(red: 0.78, green: 0.78, blue: 0.80, alpha: 1.0)
    /// 選択時のグレー（おおよそ #D9D9D9）
    public static let defaultSelectedColor = UIColor(red: 0.85, green: 0.85, blue: 0.85, alpha: 1.0)
    /// アクセント既定色（システム強調色相当の青、おおよそ #007AFF）
    public static let defaultAccentColor = UIColor(red: 0.0, green: 0.478, blue: 1.0, alpha: 1.0)
    /// ヘッダ既定背景色（システムグループ化背景に近い #F2F2F7）
    public static let defaultHeaderBackgroundColor = UIColor(red: 0.95, green: 0.95, blue: 0.97, alpha: 1.0)
    /// フッタ既定背景色
    public static let defaultFooterBackgroundColor: UIColor = defaultHeaderBackgroundColor
    /// ヘッダ既定テキスト色（おおよそ #6D6D72）
    public static let defaultHeaderTextColor = UIColor(red: 0.43, green: 0.43, blue: 0.45, alpha: 1.0)
    /// フッタ既定テキスト色
    public static let defaultFooterTextColor = UIColor(red: 0.43, green: 0.43, blue: 0.45, alpha: 1.0)
    /// SettingsView 全体の既定背景色（白系）
    public static let defaultBackgroundColor = UIColor(red: 1.0, green: 1.0, blue: 1.0, alpha: 1.0)
    /// `isEnabled = false` 時のテキスト色（やや薄い灰色、おおよそ #999999）
    public static let defaultDisabledTextColor = UIColor(red: 0.6, green: 0.6, blue: 0.6, alpha: 1.0)

    // MARK: - Cell 全体既定 / フォールバック先既定値（内部の実効値解決と共有）

    /// `cellTitleColor` 未指定時のフォールバック色（`UIColor.label`）。
    public static let defaultCellTitleColor: UIColor = .label
    /// `cellTitleFont` 未指定時のフォールバックフォント（body スタイル）。
    public static let defaultCellTitleFont: UIFont = UIFont.preferredFont(forTextStyle: .body)
    /// `cellDescriptionColor` 未指定時のフォールバック色（`UIColor.secondaryLabel`）。
    public static let defaultCellDescriptionColor: UIColor = .secondaryLabel
    /// `cellDescriptionFont` 未指定時のフォールバックフォント（footnote スタイル）。
    public static let defaultCellDescriptionFont: UIFont = UIFont.preferredFont(forTextStyle: .footnote)
    /// `cellHintFont` 未指定時のフォールバックフォント（footnote スタイル）。
    public static let defaultCellHintFont: UIFont = UIFont.preferredFont(forTextStyle: .footnote)
    /// `cellIconSize` 未指定時のフォールバック値（24pt）。
    public static let defaultCellIconSize: CGFloat = 24.0
    /// `cellIconRadius` 未指定時のフォールバック値（0pt = 角丸なし）。
    public static let defaultCellIconRadius: CGFloat = 0.0
    /// ButtonCell の `titleColor` 4 段解決で「いずれも未指定」のときに使う既定色（`.systemBlue`）。
    ///
    /// 通常 Cell の `cellTitleColor` 既定 (`UIColor.label`) と異なり、ButtonCell は
    /// 「tappable に見えるよう慣習的に青色を使う」運用のためここで分離する。
    /// ButtonCell の title 色は `ButtonCell.titleColor` → `CellStyle.titleColor` → `Theme.cellTitleColor` → 本値
    /// の順で解決される。
    public static let defaultButtonTitleColor: UIColor = .systemBlue
    /// Section / Root Header / Footer のテキストフォントの既定（`UIListContentConfiguration.cell()` の
    /// text 既定相当 = footnote スタイル）。`Theme.headerFont` / `Theme.footerFont` が `nil` のときの
    /// フォールバック先。`headerFontSize` / `footerFontSize` が `> 0` のときは pointSize が上書きされる。
    public static let defaultHeaderFooterFont: UIFont = UIFont.preferredFont(forTextStyle: .footnote)
}

// MARK: - UIColor / UIFont 比較ヘルパ

/// `UIColor.isEqual(_:)` ベースの等価判定。
@inline(__always)
internal func uiColorEqual(_ lhs: UIColor, _ rhs: UIColor) -> Bool {
    return lhs.isEqual(rhs)
}

/// Optional 版の `UIColor` 等価判定。
@inline(__always)
internal func uiColorEqualOptional(_ lhs: UIColor?, _ rhs: UIColor?) -> Bool {
    switch (lhs, rhs) {
    case (nil, nil): return true
    case (let l?, let r?): return l.isEqual(r)
    default: return false
    }
}

/// `UIFont.isEqual(_:)` ベースの等価判定。
@inline(__always)
internal func uiFontEqual(_ lhs: UIFont, _ rhs: UIFont) -> Bool {
    return lhs.isEqual(rhs)
}

/// Optional 版の `UIFont` 等価判定。
@inline(__always)
internal func uiFontEqualOptional(_ lhs: UIFont?, _ rhs: UIFont?) -> Bool {
    switch (lhs, rhs) {
    case (nil, nil): return true
    case (let l?, let r?): return l.isEqual(r)
    default: return false
    }
}
#endif
