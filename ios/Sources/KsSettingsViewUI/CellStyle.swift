// CellStyle.swift
// KsSettingsViewUI
//
// 単一 Cell に適用されるスタイル値型（UI 層所属）。
// 各フィールドは Optional（`nil` は「未指定 → Theme から継承」を意味する）。
// スタイルは Core ではなく UI 層に置き、Native 型（`UIColor` / `UIFont`）で表現する
// （core/ADR-0009）。

#if canImport(UIKit)
import UIKit

/// 単一 Cell に適用される論理スタイル。
///
/// 各フィールドは Optional で、`nil` は「未指定 → Theme から継承」を意味する。
/// 色は `UIColor?`、フォントは `UIFont?`、サイズは `CGFloat?` を直接保持する。
///
/// `@unchecked Sendable` の根拠：
/// - 全フィールドが `let`（immutable）。
/// - 構成要素の `UIColor` / `UIFont` は Apple 実装上、内部状態が事実上 immutable で thread-safe。
/// - Swift 6.2 の strict concurrency では `UIColor` / `UIFont` が Sendable 適合していないため、
///   現実的な thread 安全性を確保した上で `@unchecked` を明示する。
public struct CellStyle: Equatable, @unchecked Sendable {
    /// タイトル文字色
    public let titleColor: UIColor?
    /// タイトルフォント
    public let titleFont: UIFont?
    /// 説明文色
    public let descriptionColor: UIColor?
    /// 説明文フォント
    public let descriptionFont: UIFont?
    /// 値テキスト色（LabelCell / CommandCell の右寄せ値）
    public let valueTextColor: UIColor?
    /// 値テキストフォント
    public let valueTextFont: UIFont?
    /// アイコンサイズ（pt）
    public let iconSize: CGFloat?
    /// アイコン角丸半径（pt）
    public let iconRadius: CGFloat?
    /// Cell 高さ（pt）
    public let cellHeight: CGFloat?
    /// ヒントテキスト色
    public let hintTextColor: UIColor?
    /// ヒントテキストフォント
    public let hintTextFont: UIFont?
    /// Cell 個別背景色（`nil` のとき Theme.cellBackgroundColor）
    public let backgroundColor: UIColor?
    /// Cell 個別 accent 色（`nil` のとき Theme.cellAccentColor）
    public let accentColor: UIColor?
    /// EntryCell の placeholder 文字色（`nil` のとき Theme.cellPlaceholderColor）
    public let placeholderColor: UIColor?

    /// 任意フィールドを指定して `CellStyle` を生成する（既定はすべて `nil`）。
    public init(
        titleColor: UIColor? = nil,
        titleFont: UIFont? = nil,
        descriptionColor: UIColor? = nil,
        descriptionFont: UIFont? = nil,
        valueTextColor: UIColor? = nil,
        valueTextFont: UIFont? = nil,
        iconSize: CGFloat? = nil,
        iconRadius: CGFloat? = nil,
        cellHeight: CGFloat? = nil,
        hintTextColor: UIColor? = nil,
        hintTextFont: UIFont? = nil,
        backgroundColor: UIColor? = nil,
        accentColor: UIColor? = nil,
        placeholderColor: UIColor? = nil
    ) {
        self.titleColor = titleColor
        self.titleFont = titleFont
        self.descriptionColor = descriptionColor
        self.descriptionFont = descriptionFont
        self.valueTextColor = valueTextColor
        self.valueTextFont = valueTextFont
        self.iconSize = iconSize
        self.iconRadius = iconRadius
        self.cellHeight = cellHeight
        self.hintTextColor = hintTextColor
        self.hintTextFont = hintTextFont
        self.backgroundColor = backgroundColor
        self.accentColor = accentColor
        self.placeholderColor = placeholderColor
    }

    // MARK: - Equatable 手動実装

    public static func == (lhs: CellStyle, rhs: CellStyle) -> Bool {
        return uiColorEqualOptional(lhs.titleColor, rhs.titleColor)
            && uiFontEqualOptional(lhs.titleFont, rhs.titleFont)
            && uiColorEqualOptional(lhs.descriptionColor, rhs.descriptionColor)
            && uiFontEqualOptional(lhs.descriptionFont, rhs.descriptionFont)
            && uiColorEqualOptional(lhs.valueTextColor, rhs.valueTextColor)
            && uiFontEqualOptional(lhs.valueTextFont, rhs.valueTextFont)
            && lhs.iconSize == rhs.iconSize
            && lhs.iconRadius == rhs.iconRadius
            && lhs.cellHeight == rhs.cellHeight
            && uiColorEqualOptional(lhs.hintTextColor, rhs.hintTextColor)
            && uiFontEqualOptional(lhs.hintTextFont, rhs.hintTextFont)
            && uiColorEqualOptional(lhs.backgroundColor, rhs.backgroundColor)
            && uiColorEqualOptional(lhs.accentColor, rhs.accentColor)
            && uiColorEqualOptional(lhs.placeholderColor, rhs.placeholderColor)
    }
}

// MARK: - Cell の Hashable 補助

/// 各 Cell の `hash(into:)` から `style: CellStyle` を `Hasher` に取り込むためのフリー関数。
///
/// `CellStyle` は `Hashable` には準拠していない（`UIColor` / `UIFont` フィールドが Swift
/// `Hashable` 自動合成の対象外）。本ヘルパは UIKit の `UIColor.hashValue` / `UIFont.hashValue` を
/// 取り込むことで「`==` と整合する hash」を生成する。
///
/// `Hashable` 契約（`a == b ⇒ a.hashValue == b.hashValue`）は `UIColor.isEqual` と `hashValue`
/// が一貫している限り保たれる（Apple 公開実装で一致）。Cell の差分検出は `KsCellID`（id 単独）で
/// 行うため、style の hash 衝突が起きても構造同期に影響しない。
@inline(__always)
internal func hashCellStyle(_ style: CellStyle, into hasher: inout Hasher) {
    func combineUIColor(_ c: UIColor?) {
        if let c = c { hasher.combine(c.hashValue) } else { hasher.combine(Int(0)) }
    }
    func combineUIFont(_ f: UIFont?) {
        if let f = f { hasher.combine(f.hashValue) } else { hasher.combine(Int(0)) }
    }
    combineUIColor(style.titleColor)
    combineUIFont(style.titleFont)
    combineUIColor(style.descriptionColor)
    combineUIFont(style.descriptionFont)
    combineUIColor(style.valueTextColor)
    combineUIFont(style.valueTextFont)
    hasher.combine(style.iconSize)
    hasher.combine(style.iconRadius)
    hasher.combine(style.cellHeight)
    combineUIColor(style.hintTextColor)
    combineUIFont(style.hintTextFont)
    combineUIColor(style.backgroundColor)
    combineUIColor(style.accentColor)
    combineUIColor(style.placeholderColor)
}
#endif
