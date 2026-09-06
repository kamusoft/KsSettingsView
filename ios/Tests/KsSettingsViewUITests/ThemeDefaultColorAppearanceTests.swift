// ThemeDefaultColorAppearanceTests.swift
// KsSettingsViewUITests
//
// `Theme` の既定色が外観（ライト / ダーク）に追随することを、生値の解決結果で検証する。
//
// 観測点は `UIColor.resolvedColor(with:)` が返す実際の RGBA 値に取る。ライトはライブラリの
// ライト外観の固定値、ダークはライブラリが所有する dark セットの値であることを、期待値を
// テスト側に literal で持って比較する（実装側の定数と突き合わせると恒真になるため）。
//
// dark セットの生値は Android 側の既定色と同じ値であり、3 platform 同値の契約をここで固定する。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI

final class ThemeDefaultColorAppearanceTests: XCTestCase {

    // MARK: - ハーネス

    /// 指定した外観で色を解決し、RGBA 成分を返す。
    private func rgba(
        _ color: UIColor,
        style: UIUserInterfaceStyle
    ) -> (r: CGFloat, g: CGFloat, b: CGFloat, a: CGFloat) {
        let resolved = color.resolvedColor(with: UITraitCollection(userInterfaceStyle: style))
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        resolved.getRed(&r, green: &g, blue: &b, alpha: &a)
        return (r, g, b, a)
    }

    /// `0xRRGGBB` を不透明色の RGBA 成分に展開する。
    private func components(hex: UInt32) -> (r: CGFloat, g: CGFloat, b: CGFloat, a: CGFloat) {
        return (
            CGFloat((hex >> 16) & 0xFF) / 255.0,
            CGFloat((hex >> 8) & 0xFF) / 255.0,
            CGFloat(hex & 0xFF) / 255.0,
            1.0
        )
    }

    /// 指定外観で解決した色が期待の RGBA 成分と一致することを検査する。
    private func assertResolved(
        _ color: UIColor,
        style: UIUserInterfaceStyle,
        equals expected: (r: CGFloat, g: CGFloat, b: CGFloat, a: CGFloat),
        _ label: String,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        let actual = rgba(color, style: style)
        let distance = abs(actual.r - expected.r) + abs(actual.g - expected.g)
            + abs(actual.b - expected.b) + abs(actual.a - expected.a)
        XCTAssertLessThan(
            distance, 1.0 / 512.0,
            "\(label): 期待 \(expected) に対し実測 \(actual)",
            file: file, line: line
        )
    }

    // MARK: - ライト外観の既定色は固定値

    func test_既定色のライト解決値が固定値と一致する() {
        assertResolved(
            Theme.defaultSeparatorColor, style: .light,
            equals: (0.78, 0.78, 0.80, 1.0), "separator"
        )
        assertResolved(
            Theme.defaultSelectedColor, style: .light,
            equals: (0.85, 0.85, 0.85, 1.0), "選択中の背景"
        )
        assertResolved(
            Theme.defaultAccentColor, style: .light,
            equals: (0.0, 0.478, 1.0, 1.0), "accent"
        )
        assertResolved(
            Theme.defaultHeaderBackgroundColor, style: .light,
            equals: (0.95, 0.95, 0.97, 1.0), "Header 背景"
        )
        assertResolved(
            Theme.defaultFooterBackgroundColor, style: .light,
            equals: (0.95, 0.95, 0.97, 1.0), "Footer 背景"
        )
        assertResolved(
            Theme.defaultHeaderTextColor, style: .light,
            equals: (0.43, 0.43, 0.45, 1.0), "Header 文字"
        )
        assertResolved(
            Theme.defaultFooterTextColor, style: .light,
            equals: (0.43, 0.43, 0.45, 1.0), "Footer 文字"
        )
        assertResolved(
            Theme.defaultBackgroundColor, style: .light,
            equals: (1.0, 1.0, 1.0, 1.0), "list 下地"
        )
        assertResolved(
            Theme.defaultCellBackgroundColor, style: .light,
            equals: (1.0, 1.0, 1.0, 1.0), "Cell 背景"
        )
        assertResolved(
            Theme.defaultDisabledTextColor, style: .light,
            equals: (0.6, 0.6, 0.6, 1.0), "disabled 文字"
        )
    }

    // MARK: - ダーク外観の既定色は dark セットの値

    func test_既定色のダーク解決値がdarkセットの値と一致する() {
        assertResolved(
            Theme.defaultBackgroundColor, style: .dark,
            equals: components(hex: 0x000000), "list 下地"
        )
        assertResolved(
            Theme.defaultCellBackgroundColor, style: .dark,
            equals: components(hex: 0x1C1C1E), "Cell 背景"
        )
        assertResolved(
            Theme.defaultSeparatorColor, style: .dark,
            equals: components(hex: 0x38383A), "separator"
        )
        assertResolved(
            Theme.defaultSelectedColor, style: .dark,
            equals: components(hex: 0x2C2C2E), "選択中の背景"
        )
        assertResolved(
            Theme.defaultAccentColor, style: .dark,
            equals: components(hex: 0x0A84FF), "accent"
        )
        assertResolved(
            Theme.defaultDisabledTextColor, style: .dark,
            equals: components(hex: 0x636366), "disabled 文字"
        )
        assertResolved(
            Theme.defaultHeaderBackgroundColor, style: .dark,
            equals: components(hex: 0x000000), "Header 背景"
        )
        assertResolved(
            Theme.defaultFooterBackgroundColor, style: .dark,
            equals: components(hex: 0x000000), "Footer 背景"
        )
        assertResolved(
            Theme.defaultHeaderTextColor, style: .dark,
            equals: components(hex: 0x8E8E93), "Header 文字"
        )
        assertResolved(
            Theme.defaultFooterTextColor, style: .dark,
            equals: components(hex: 0x8E8E93), "Footer 文字"
        )
    }

    /// ButtonCell の title 既定はシステムの青（`.systemBlue`）のままで、外観ごとに解決される。
    ///
    /// システム色は独自の色空間で定義されており、他の既定色のような sRGB の生値では持たない。
    /// 検査は「システム色そのものであること」と「ライトとダークで異なる値に解決されること」に取る。
    func test_ButtonCellのtitle既定はsystemBlueのまま外観に追随する() {
        XCTAssertEqual(Theme.defaultButtonTitleColor, UIColor.systemBlue)
        let light = rgba(Theme.defaultButtonTitleColor, style: .light)
        let dark = rgba(Theme.defaultButtonTitleColor, style: .dark)
        XCTAssertNotEqual(
            light.g, dark.g,
            "ButtonCell title の既定はライトとダークで異なる値に解決される"
        )
    }

    /// Cell title / description の既定はシステム色に委ねる。
    func test_Cellのtitleとdescriptionの既定はシステム色のまま() {
        XCTAssertEqual(Theme.defaultCellTitleColor, UIColor.label)
        XCTAssertEqual(Theme.defaultCellDescriptionColor, UIColor.secondaryLabel)
    }

    /// dark セットの生値は Android の既定色と同じ値であり、3 platform で共通の生値を持つ。
    func test_darkセットの生値が3platform共通の値である() {
        assertResolved(Theme.darkBackgroundColorValue, style: .dark,
                       equals: components(hex: 0x000000), "list 下地")
        assertResolved(Theme.darkCellBackgroundColorValue, style: .dark,
                       equals: components(hex: 0x1C1C1E), "Cell 背景")
        assertResolved(Theme.darkSeparatorColorValue, style: .dark,
                       equals: components(hex: 0x38383A), "separator")
        assertResolved(Theme.darkSelectedColorValue, style: .dark,
                       equals: components(hex: 0x2C2C2E), "選択中の背景")
        assertResolved(Theme.darkAccentColorValue, style: .dark,
                       equals: components(hex: 0x0A84FF), "accent")
        assertResolved(Theme.darkDisabledTextColorValue, style: .dark,
                       equals: components(hex: 0x636366), "disabled 文字")
        assertResolved(Theme.darkHeaderBackgroundColorValue, style: .dark,
                       equals: components(hex: 0x000000), "Header 背景")
        assertResolved(Theme.darkFooterBackgroundColorValue, style: .dark,
                       equals: components(hex: 0x000000), "Footer 背景")
        assertResolved(Theme.darkHeaderTextColorValue, style: .dark,
                       equals: components(hex: 0x8E8E93), "Header 文字")
        assertResolved(Theme.darkFooterTextColorValue, style: .dark,
                       equals: components(hex: 0x8E8E93), "Footer 文字")
    }

    // MARK: - 等価性

    func test_既定Theme同士と既定定数を明示したThemeは等価() {
        XCTAssertEqual(Theme(), Theme())
        let explicit = Theme(
            separatorColor: Theme.defaultSeparatorColor,
            backgroundColor: Theme.defaultBackgroundColor,
            cellBackgroundColor: Theme.defaultCellBackgroundColor,
            selectedColor: Theme.defaultSelectedColor,
            cellAccentColor: Theme.defaultAccentColor,
            disabledTextColor: Theme.defaultDisabledTextColor,
            headerTextColor: Theme.defaultHeaderTextColor,
            headerBackgroundColor: Theme.defaultHeaderBackgroundColor,
            footerTextColor: Theme.defaultFooterTextColor,
            footerBackgroundColor: Theme.defaultFooterBackgroundColor
        )
        XCTAssertEqual(explicit, Theme())
    }

    /// 既定と同じ生値を固定色で明示した Theme は既定 Theme と等価ではなく、ダークでも変わらない。
    func test_固定色で明示したThemeは既定と等価でなくダークでも変わらない() {
        let fixedWhite = UIColor(red: 1.0, green: 1.0, blue: 1.0, alpha: 1.0)
        let theme = Theme(backgroundColor: fixedWhite)
        XCTAssertNotEqual(theme, Theme())
        assertResolved(
            theme.backgroundColor, style: .dark,
            equals: (1.0, 1.0, 1.0, 1.0), "固定色で明示した list 下地"
        )
    }

    /// 利用者が両外観の値を持つ色を渡した場合、その色自身の dark 値へ解決される。
    func test_利用者のdynamicな色はその色自身のdark値になる() {
        let userColor = UIColor { trait in
            trait.userInterfaceStyle == .dark
                ? UIColor(red: 0.2, green: 0.4, blue: 0.6, alpha: 1.0)
                : UIColor(red: 0.9, green: 0.8, blue: 0.7, alpha: 1.0)
        }
        let theme = Theme(backgroundColor: userColor)
        assertResolved(
            theme.backgroundColor, style: .light,
            equals: (0.9, 0.8, 0.7, 1.0), "利用者の色（ライト）"
        )
        assertResolved(
            theme.backgroundColor, style: .dark,
            equals: (0.2, 0.4, 0.6, 1.0), "利用者の色（ダーク）"
        )
    }
}
#endif
