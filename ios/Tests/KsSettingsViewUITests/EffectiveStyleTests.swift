// EffectiveStyleTests.swift
// KsSettingsViewUITests
//
// `EffectiveStyle` の合成挙動を検証する。
// - CellStyle が未指定なら Theme（または既定値）から補完
// - CellStyle 指定値があれば優先される
//
// `Theme` / `CellStyle` は `UIColor` / `UIFont` を直接保持する（core/ADR-0009）ため、
// テストも UIColor / UIFont の直接構築で組み立てる。中間表現は介在しない。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

final class EffectiveStyleTests: XCTestCase {

    func test_CellStyle未指定時はTheme補完が行われる_背景色() {
        let bg = UIColor(red: 0.1, green: 0.2, blue: 0.3, alpha: 1.0)
        let theme = Theme(cellBackgroundColor: bg)
        let cellStyle = CellStyle()
        let effective = EffectiveStyle(theme: theme, cellStyle: cellStyle)

        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        effective.cellBackgroundColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 0.1, accuracy: 0.001)
        XCTAssertEqual(g, 0.2, accuracy: 0.001)
        XCTAssertEqual(b, 0.3, accuracy: 0.001)
    }

    func test_CellStyle指定値が優先される_titleColor() {
        let theme = Theme()
        let titleColor = UIColor(red: 0.9, green: 0.0, blue: 0.0, alpha: 1.0)
        let cellStyle = CellStyle(titleColor: titleColor)
        let effective = EffectiveStyle(theme: theme, cellStyle: cellStyle)

        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        effective.titleColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 0.9, accuracy: 0.001)
    }

    func test_iconSize既定は24pt() {
        let theme = Theme()
        let cellStyle = CellStyle()
        let effective = EffectiveStyle(theme: theme, cellStyle: cellStyle)
        XCTAssertEqual(effective.iconSize, 24.0)
    }

    func test_iconSize_CellStyle指定値が優先される() {
        let theme = Theme()
        let cellStyle = CellStyle(iconSize: 32.0)
        let effective = EffectiveStyle(theme: theme, cellStyle: cellStyle)
        XCTAssertEqual(effective.iconSize, 32.0)
    }

    // MARK: - 合成プロパティ

    func test_CellStyle_backgroundColor指定時はTheme_cellBackgroundColorよりも優先される() {
        let yellow = UIColor(red: 1.0, green: 1.0, blue: 0.0, alpha: 1.0)
        let effective = EffectiveStyle(
            theme: Theme(cellBackgroundColor: .white),
            cellStyle: CellStyle(backgroundColor: yellow)
        )
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        effective.cellBackgroundColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 1.0, accuracy: 0.001)
        XCTAssertEqual(g, 1.0, accuracy: 0.001)
        XCTAssertEqual(b, 0.0, accuracy: 0.001)
    }

    func test_CellStyle_accentColor指定時はTheme_cellAccentColorよりも優先される() {
        let green = UIColor(red: 0.0, green: 1.0, blue: 0.0, alpha: 1.0)
        let effective = EffectiveStyle(
            theme: Theme(cellAccentColor: UIColor(red: 0.0, green: 0.0, blue: 1.0, alpha: 1.0)),
            cellStyle: CellStyle(accentColor: green)
        )
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        effective.accentColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(g, 1.0, accuracy: 0.001)
    }

    func test_CellStyle_valueTextColor指定時はdescriptionColorよりも優先される() {
        let darkGray = UIColor(red: 0.2, green: 0.2, blue: 0.2, alpha: 1.0)
        let effective = EffectiveStyle(
            theme: Theme(),
            cellStyle: CellStyle(valueTextColor: darkGray)
        )
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        effective.valueTextColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 0.2, accuracy: 0.001)
    }

    func test_disabledTextColor_はThemeから取得される() {
        let lightGray = UIColor(red: 0.7, green: 0.7, blue: 0.7, alpha: 1.0)
        let effective = EffectiveStyle(
            theme: Theme(disabledTextColor: lightGray),
            cellStyle: CellStyle()
        )
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        effective.disabledTextColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 0.7, accuracy: 0.001)
    }

    func test_effectiveCellHeight_CellStyle_cellHeight指定があればそれを採用() {
        let effective = EffectiveStyle(
            theme: Theme(rowHeight: 44),
            cellStyle: CellStyle(cellHeight: 80.0)
        )
        XCTAssertEqual(effective.effectiveCellHeight, 80.0)
    }

    func test_effectiveCellHeight_Theme_rowHeight指定があればそれを採用() {
        let effective = EffectiveStyle(
            theme: Theme(rowHeight: 60),
            cellStyle: CellStyle()
        )
        XCTAssertEqual(effective.effectiveCellHeight, 60.0)
    }

    func test_effectiveCellHeight_最低48ptで下限ガード() {
        let effective = EffectiveStyle(
            theme: Theme(rowHeight: 20),
            cellStyle: CellStyle()
        )
        XCTAssertEqual(effective.effectiveCellHeight, 48.0)
    }

    func test_isFixedHeight_はTheme_hasUnevenRowsの否定で決まる() {
        let fixed = EffectiveStyle(
            theme: Theme(hasUnevenRows: false),
            cellStyle: CellStyle()
        )
        let uneven = EffectiveStyle(
            theme: Theme(hasUnevenRows: true),
            cellStyle: CellStyle()
        )
        XCTAssertTrue(fixed.isFixedHeight)
        XCTAssertFalse(uneven.isFixedHeight)
    }

    // MARK: - iOS 下限保証の回帰テスト

    /// `Theme()` 引数なし（`rowHeight = -1` / `hasUnevenRows = true` 新デフォルト）かつ `CellStyle()` も
    /// 未指定の場合、`minRowHeight = 48pt` が base として採用されて `effectiveCellHeight = 48.0` を返す。
    /// iOS は `UITableView.AutomaticDimension` 採用のため、Android のように 60pt を base にする必要はない。
    func test_effectiveCellHeight_Theme未指定時に48ptを採用する() {
        let effective = EffectiveStyle(
            theme: Theme(),
            cellStyle: CellStyle()
        )
        XCTAssertEqual(effective.effectiveCellHeight, 48.0)
        XCTAssertEqual(effective.effectiveCellHeight, EffectiveStyle.minRowHeight)
    }

    /// `Theme()`（新デフォルト `hasUnevenRows = true`）のとき `isFixedHeight = false` であり、
    /// `KsCellViewSupport.adjustedLayoutAttributes` の「下限保証 + intrinsic > 下限なら intrinsic」モードが選択される。
    func test_isFixedHeight_Theme未指定時はfalseで可変高さモードになる() {
        let effective = EffectiveStyle(
            theme: Theme(),
            cellStyle: CellStyle()
        )
        XCTAssertFalse(effective.isFixedHeight)
    }

    // MARK: - Theme.cellTitleColor / Theme.cellTitleFont の 3 段階優先順位

    func test_titleColor_Themeのみ指定_合成値はThemeを採用しisExplicitはtrue() {
        let themeColor = UIColor(red: 0.2, green: 0.4, blue: 0.6, alpha: 1.0)
        let effective = EffectiveStyle(
            theme: Theme(cellTitleColor: themeColor),
            cellStyle: CellStyle()
        )
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        effective.titleColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 0.2, accuracy: 0.005)
        XCTAssertEqual(g, 0.4, accuracy: 0.005)
        XCTAssertEqual(b, 0.6, accuracy: 0.005)
        XCTAssertTrue(effective.titleColorIsExplicit)
    }

    func test_titleColor_CellStyleのみ指定_合成値はCellStyleを採用しisExplicitはtrue() {
        let cellColor = UIColor(red: 0.9, green: 0.0, blue: 0.0, alpha: 1.0)
        let effective = EffectiveStyle(
            theme: Theme(),
            cellStyle: CellStyle(titleColor: cellColor)
        )
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        effective.titleColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 0.9, accuracy: 0.005)
        XCTAssertTrue(effective.titleColorIsExplicit)
    }

    func test_titleColor_両方指定_CellStyleが優先される() {
        let cellColor = UIColor(red: 1.0, green: 0.0, blue: 0.0, alpha: 1.0)
        let themeColor = UIColor(red: 0.0, green: 0.0, blue: 1.0, alpha: 1.0)
        let effective = EffectiveStyle(
            theme: Theme(cellTitleColor: themeColor),
            cellStyle: CellStyle(titleColor: cellColor)
        )
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        effective.titleColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 1.0, accuracy: 0.005)
        XCTAssertEqual(b, 0.0, accuracy: 0.005)
        XCTAssertTrue(effective.titleColorIsExplicit)
    }

    func test_titleColor_両方未指定_label_isExplicitはfalse() {
        let effective = EffectiveStyle(
            theme: Theme(),
            cellStyle: CellStyle()
        )
        XCTAssertEqual(effective.titleColor, UIColor.label)
        XCTAssertFalse(effective.titleColorIsExplicit)
    }

    func test_titleFont_Themeのみ指定_合成値はThemeを採用() {
        let themeFont = UIFont.systemFont(ofSize: 22.0, weight: .bold)
        let effective = EffectiveStyle(
            theme: Theme(cellTitleFont: themeFont),
            cellStyle: CellStyle()
        )
        XCTAssertEqual(effective.titleFont.pointSize, 22.0, accuracy: 0.5)
    }

    func test_titleFont_両方指定_CellStyleが優先される() {
        let cellFont = UIFont.systemFont(ofSize: 19.0, weight: .regular)
        let themeFont = UIFont.systemFont(ofSize: 22.0, weight: .bold)
        let effective = EffectiveStyle(
            theme: Theme(cellTitleFont: themeFont),
            cellStyle: CellStyle(titleFont: cellFont)
        )
        XCTAssertEqual(effective.titleFont.pointSize, 19.0, accuracy: 0.5)
    }

    func test_titleFont_両方未指定_preferredBodyFontが採用される() {
        let effective = EffectiveStyle(
            theme: Theme(),
            cellStyle: CellStyle()
        )
        // 既定: UIFont.preferredFont(forTextStyle: .body)
        XCTAssertEqual(
            effective.titleFont.fontDescriptor.object(forKey: .textStyle) as? UIFont.TextStyle,
            .body
        )
    }
}
#endif
