// EffectiveStyleResolutionTests.swift
// KsSettingsViewUITests
//
// `EffectiveStyle` の新規アクセサ（`effectiveValueTextColor` 等）と、`cellTitleFontSize`
// による pointSize 上書き、ButtonCell 4 段優先の解決順序を検証する。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI

final class EffectiveStyleResolutionTests: XCTestCase {

    // MARK: - effectiveValueTextColor / effectiveValueTextFont

    func test_effectiveValueTextColor_CellStyle優先() {
        let cellColor = UIColor(red: 1.0, green: 0.0, blue: 0.0, alpha: 1.0)
        let themeColor = UIColor(red: 0.0, green: 1.0, blue: 0.0, alpha: 1.0)
        let result = EffectiveStyle.effectiveValueTextColor(
            cellStyle: CellStyle(valueTextColor: cellColor),
            theme: Theme(cellValueTextColor: themeColor)
        )
        XCTAssertTrue(result.isEqual(cellColor))
    }

    func test_effectiveValueTextColor_Themeフォールバック() {
        let themeColor = UIColor(red: 0.0, green: 1.0, blue: 0.0, alpha: 1.0)
        let result = EffectiveStyle.effectiveValueTextColor(
            cellStyle: CellStyle(),
            theme: Theme(cellValueTextColor: themeColor)
        )
        XCTAssertTrue(result.isEqual(themeColor))
    }

    func test_effectiveValueTextColor_既定フォールバック_cellTitleColor() {
        // `Theme.cellValueTextColor == nil` のとき `Theme.cellTitleColor` にフォールバック
        let titleColor = UIColor(red: 0.5, green: 0.5, blue: 0.0, alpha: 1.0)
        let result = EffectiveStyle.effectiveValueTextColor(
            cellStyle: CellStyle(),
            theme: Theme(cellTitleColor: titleColor)
        )
        XCTAssertTrue(result.isEqual(titleColor))
    }

    func test_effectiveValueTextColor_全てnil時は_label() {
        let result = EffectiveStyle.effectiveValueTextColor(
            cellStyle: CellStyle(),
            theme: Theme()
        )
        XCTAssertEqual(result, UIColor.label)
    }

    func test_effectiveValueTextFont_Themeフォールバック() {
        let themeFont = UIFont.systemFont(ofSize: 13.0)
        let result = EffectiveStyle.effectiveValueTextFont(
            cellStyle: CellStyle(),
            theme: Theme(cellValueTextFont: themeFont)
        )
        XCTAssertTrue(result.isEqual(themeFont))
    }

    // MARK: - effectiveDescriptionColor / effectiveDescriptionFont

    func test_effectiveDescriptionColor_CellStyle優先() {
        let cellColor = UIColor(red: 0.3, green: 0.3, blue: 0.3, alpha: 1.0)
        let result = EffectiveStyle.effectiveDescriptionColor(
            cellStyle: CellStyle(descriptionColor: cellColor),
            theme: Theme(cellDescriptionColor: .blue)
        )
        XCTAssertTrue(result.isEqual(cellColor))
    }

    func test_effectiveDescriptionColor_Themeフォールバック() {
        let themeColor = UIColor(red: 0.1, green: 0.2, blue: 0.3, alpha: 1.0)
        let result = EffectiveStyle.effectiveDescriptionColor(
            cellStyle: CellStyle(),
            theme: Theme(cellDescriptionColor: themeColor)
        )
        XCTAssertTrue(result.isEqual(themeColor))
    }

    func test_effectiveDescriptionColor_既定フォールバック_secondaryLabel() {
        let result = EffectiveStyle.effectiveDescriptionColor(
            cellStyle: CellStyle(),
            theme: Theme()
        )
        XCTAssertEqual(result, UIColor.secondaryLabel)
    }

    // MARK: - effectiveHintTextColor / effectiveHintFont

    func test_effectiveHintTextColor_CellStyle優先() {
        let cellColor = UIColor.red
        let result = EffectiveStyle.effectiveHintTextColor(
            cellStyle: CellStyle(hintTextColor: cellColor),
            theme: Theme(cellHintTextColor: .green)
        )
        XCTAssertTrue(result.isEqual(cellColor))
    }

    func test_effectiveHintTextColor_Themeフォールバック() {
        let themeColor = UIColor.red
        let result = EffectiveStyle.effectiveHintTextColor(
            cellStyle: CellStyle(),
            theme: Theme(cellHintTextColor: themeColor)
        )
        XCTAssertTrue(result.isEqual(themeColor))
    }

    func test_effectiveHintTextColor_既定フォールバックは_cellAccentColor() {
        let accent = UIColor.purple
        let result = EffectiveStyle.effectiveHintTextColor(
            cellStyle: CellStyle(),
            theme: Theme(cellAccentColor: accent)
        )
        XCTAssertTrue(result.isEqual(accent))
    }

    // MARK: - effectiveIconSize / effectiveIconRadius

    func test_effectiveIconSize_CellStyle優先() {
        let result = EffectiveStyle.effectiveIconSize(
            cellStyle: CellStyle(iconSize: 40.0),
            theme: Theme(cellIconSize: 32.0)
        )
        XCTAssertEqual(result, 40.0)
    }

    func test_effectiveIconSize_Themeフォールバック() {
        let result = EffectiveStyle.effectiveIconSize(
            cellStyle: CellStyle(),
            theme: Theme(cellIconSize: 32.0)
        )
        XCTAssertEqual(result, 32.0)
    }

    func test_effectiveIconSize_既定は24pt() {
        let result = EffectiveStyle.effectiveIconSize(
            cellStyle: CellStyle(),
            theme: Theme()
        )
        XCTAssertEqual(result, 24.0)
    }

    func test_effectiveIconRadius_CellStyle優先() {
        let result = EffectiveStyle.effectiveIconRadius(
            cellStyle: CellStyle(iconRadius: 8.0),
            theme: Theme(cellIconRadius: 4.0)
        )
        XCTAssertEqual(result, 8.0)
    }

    func test_effectiveIconRadius_Themeフォールバック() {
        let result = EffectiveStyle.effectiveIconRadius(
            cellStyle: CellStyle(),
            theme: Theme(cellIconRadius: 4.0)
        )
        XCTAssertEqual(result, 4.0)
    }

    func test_effectiveIconRadius_既定は0pt() {
        let result = EffectiveStyle.effectiveIconRadius(
            cellStyle: CellStyle(),
            theme: Theme()
        )
        XCTAssertEqual(result, 0.0)
    }

    /// icon size の有効値は正の有限値のみ。それ以外は未指定として次の段へ解決する。
    func test_effectiveIconSize_無効値は未指定として次の段へ解決する() {
        for invalid in [CGFloat(0), -12, .nan, .infinity, -.infinity] {
            XCTAssertEqual(
                EffectiveStyle.effectiveIconSize(
                    cellStyle: CellStyle(iconSize: invalid),
                    theme: Theme(cellIconSize: 30)
                ),
                30.0,
                "CellStyle の無効な iconSize (\(invalid)) は Theme へ送られる"
            )
            XCTAssertEqual(
                EffectiveStyle.effectiveIconSize(
                    cellStyle: CellStyle(),
                    theme: Theme(cellIconSize: invalid)
                ),
                Theme.defaultCellIconSize,
                "Theme の無効な cellIconSize (\(invalid)) は既定値へ送られる"
            )
        }
    }

    /// icon radius の有効値は 0 以上の有限値のみ。0 は「角丸なし」という有効な指定として扱う。
    func test_effectiveIconRadius_無効値は未指定として次の段へ解決する() {
        for invalid in [CGFloat(-3), .nan, .infinity, -.infinity] {
            XCTAssertEqual(
                EffectiveStyle.effectiveIconRadius(
                    cellStyle: CellStyle(iconRadius: invalid),
                    theme: Theme(cellIconRadius: 6)
                ),
                6.0,
                "CellStyle の無効な iconRadius (\(invalid)) は Theme へ送られる"
            )
            XCTAssertEqual(
                EffectiveStyle.effectiveIconRadius(
                    cellStyle: CellStyle(),
                    theme: Theme(cellIconRadius: invalid)
                ),
                Theme.defaultCellIconRadius,
                "Theme の無効な cellIconRadius (\(invalid)) は既定値へ送られる"
            )
        }
    }

    /// 0 の radius は「角丸なし」という指定として受け付け、Theme へ送らない。
    func test_effectiveIconRadius_0は有効な指定として扱う() {
        XCTAssertEqual(
            EffectiveStyle.effectiveIconRadius(
                cellStyle: CellStyle(iconRadius: 0),
                theme: Theme(cellIconRadius: 6)
            ),
            0.0,
            "CellStyle の iconRadius = 0 は角丸なしの指定として優先される"
        )
    }

    /// icon の既定の生値は Android の `DEFAULT_CELL_ICON_SIZE_DP_VALUE` /
    /// `DEFAULT_CELL_ICON_RADIUS_DP_VALUE` と同じ 24 / 0 である。
    func test_icon既定の生値は両platform共通() {
        XCTAssertEqual(Theme.defaultCellIconSize, 24.0, "icon size の既定の生値は 24")
        XCTAssertEqual(Theme.defaultCellIconRadius, 0.0, "icon radius の既定の生値は 0（角丸なし）")
    }

    // MARK: - effectiveBackgroundColor / effectiveAccentColor

    func test_effectiveBackgroundColor_CellStyle優先() {
        let result = EffectiveStyle.effectiveBackgroundColor(
            cellStyle: CellStyle(backgroundColor: .red),
            theme: Theme(cellBackgroundColor: .white)
        )
        XCTAssertTrue(result.isEqual(UIColor.red))
    }

    func test_effectiveAccentColor_Themeフォールバック() {
        let result = EffectiveStyle.effectiveAccentColor(
            cellStyle: CellStyle(),
            theme: Theme(cellAccentColor: .orange)
        )
        XCTAssertTrue(result.isEqual(UIColor.orange))
    }

    // MARK: - cellTitleFontSize による pointSize 上書き

    func test_cellTitleFontSize_pointSizeを上書き() {
        let theme = Theme(
            cellTitleFont: UIFont.systemFont(ofSize: 14),
            cellTitleFontSize: 20.0
        )
        let result = EffectiveStyle.effectiveTitleFont(cellStyle: CellStyle(), theme: theme)
        XCTAssertEqual(result.pointSize, 20.0, accuracy: 0.5)
    }

    func test_cellTitleFontSize_未指定_minus1は上書きしない() {
        let baseFont = UIFont.systemFont(ofSize: 14)
        let theme = Theme(cellTitleFont: baseFont, cellTitleFontSize: -1.0)
        let result = EffectiveStyle.effectiveTitleFont(cellStyle: CellStyle(), theme: theme)
        XCTAssertEqual(result.pointSize, 14.0, accuracy: 0.5)
    }

    func test_cellTitleFontSize_ゼロは上書きしない() {
        let baseFont = UIFont.systemFont(ofSize: 14)
        let theme = Theme(cellTitleFont: baseFont, cellTitleFontSize: 0.0)
        let result = EffectiveStyle.effectiveTitleFont(cellStyle: CellStyle(), theme: theme)
        XCTAssertEqual(result.pointSize, 14.0, accuracy: 0.5)
    }

    // MARK: - ButtonCell 4 段優先解決

    func test_effectiveButtonTitleColor_ButtonCell個別が最優先() {
        let buttonColor = UIColor.red
        let cellStyleColor = UIColor.green
        let themeColor = UIColor.blue
        let result = EffectiveStyle.effectiveButtonTitleColor(
            buttonCellTitleColor: buttonColor,
            cellStyle: CellStyle(titleColor: cellStyleColor),
            theme: Theme(cellTitleColor: themeColor)
        )
        XCTAssertTrue(result.isEqual(buttonColor))
    }

    func test_effectiveButtonTitleColor_ButtonCellがnilならCellStyle() {
        let cellStyleColor = UIColor.green
        let themeColor = UIColor.blue
        let result = EffectiveStyle.effectiveButtonTitleColor(
            buttonCellTitleColor: nil,
            cellStyle: CellStyle(titleColor: cellStyleColor),
            theme: Theme(cellTitleColor: themeColor)
        )
        XCTAssertTrue(result.isEqual(cellStyleColor))
    }

    func test_effectiveButtonTitleColor_ButtonCellとCellStyleがnilならTheme() {
        let themeColor = UIColor.blue
        let result = EffectiveStyle.effectiveButtonTitleColor(
            buttonCellTitleColor: nil,
            cellStyle: CellStyle(),
            theme: Theme(cellTitleColor: themeColor)
        )
        XCTAssertTrue(result.isEqual(themeColor))
    }

    func test_effectiveButtonTitleColor_全てnilならsystemBlue() {
        // `ButtonCell.titleColor` の解決は 4 段優先で、その 4 段目（全て未指定のとき）は
        // プラットフォーム既定として Button の慣習的なアクセント色 `.systemBlue` を採る。
        let result = EffectiveStyle.effectiveButtonTitleColor(
            buttonCellTitleColor: nil,
            cellStyle: CellStyle(),
            theme: Theme()
        )
        XCTAssertTrue(result.isEqual(UIColor.systemBlue))
    }

    // MARK: - effectivePlaceholderColor（4 段優先）

    func test_effectivePlaceholderColor_EntryCell個別が最優先() {
        let cellColor = UIColor(red: 1.0, green: 0.0, blue: 0.0, alpha: 1.0)
        let styleColor = UIColor(red: 0.0, green: 1.0, blue: 0.0, alpha: 1.0)
        let themeColor = UIColor(red: 0.0, green: 0.0, blue: 1.0, alpha: 1.0)
        let result = EffectiveStyle.effectivePlaceholderColor(
            entryPlaceholderColor: cellColor,
            cellStyle: CellStyle(placeholderColor: styleColor),
            theme: Theme(cellPlaceholderColor: themeColor)
        )
        XCTAssertEqual(result, cellColor)
    }

    func test_effectivePlaceholderColor_EntryCellがnilならCellStyle() {
        let styleColor = UIColor(red: 0.0, green: 1.0, blue: 0.0, alpha: 1.0)
        let themeColor = UIColor(red: 0.0, green: 0.0, blue: 1.0, alpha: 1.0)
        let result = EffectiveStyle.effectivePlaceholderColor(
            entryPlaceholderColor: nil,
            cellStyle: CellStyle(placeholderColor: styleColor),
            theme: Theme(cellPlaceholderColor: themeColor)
        )
        XCTAssertEqual(result, styleColor)
    }

    func test_effectivePlaceholderColor_EntryCellとCellStyleがnilならTheme() {
        let themeColor = UIColor(red: 0.0, green: 0.0, blue: 1.0, alpha: 1.0)
        let result = EffectiveStyle.effectivePlaceholderColor(
            entryPlaceholderColor: nil,
            cellStyle: CellStyle(),
            theme: Theme(cellPlaceholderColor: themeColor)
        )
        XCTAssertEqual(result, themeColor)
    }

    func test_effectivePlaceholderColor_全てnilならプラットフォーム既定() {
        // 4 段目はライブラリ独自の既定色ではなく「未解決 (nil)」で、描画側が
        // システム既定の placeholder 表示をそのまま使う。
        let result = EffectiveStyle.effectivePlaceholderColor(
            entryPlaceholderColor: nil,
            cellStyle: CellStyle(),
            theme: Theme()
        )
        XCTAssertNil(result)
    }

    func test_EffectiveStyle_placeholderColorはCellStyleからThemeへ解決する() {
        let styleColor = UIColor(red: 0.0, green: 1.0, blue: 0.0, alpha: 1.0)
        let themeColor = UIColor(red: 0.0, green: 0.0, blue: 1.0, alpha: 1.0)
        let fromStyle = EffectiveStyle(
            theme: Theme(cellPlaceholderColor: themeColor),
            cellStyle: CellStyle(placeholderColor: styleColor)
        )
        XCTAssertEqual(fromStyle.placeholderColor, styleColor)

        let fromTheme = EffectiveStyle(
            theme: Theme(cellPlaceholderColor: themeColor),
            cellStyle: CellStyle()
        )
        XCTAssertEqual(fromTheme.placeholderColor, themeColor)

        XCTAssertNil(EffectiveStyle(theme: Theme(), cellStyle: CellStyle()).placeholderColor)
    }

    // MARK: - UIFont equals 安定性

    func test_UIFontEquals_同一インスタンスを使った2つのThemeは等価() {
        let font = UIFont.systemFont(ofSize: 16, weight: .regular)
        let theme1 = Theme(cellTitleFont: font)
        let theme2 = Theme(cellTitleFont: font)
        XCTAssertEqual(theme1, theme2)
    }

    func test_UIFontEquals_別生成でも同一systemFontなら等価() {
        let font1 = UIFont.systemFont(ofSize: 16, weight: .regular)
        let font2 = UIFont.systemFont(ofSize: 16, weight: .regular)
        let theme1 = Theme(cellTitleFont: font1)
        let theme2 = Theme(cellTitleFont: font2)
        XCTAssertEqual(theme1, theme2)
    }

    // MARK: - effectiveHeaderFont / effectiveFooterFont の解決

    /// `Theme.headerFont` 未指定 / `headerFontSize` 未指定 → footnote 既定にフォールバック。
    func test_effectiveHeaderFont_全て未指定なら既定footnote() {
        let theme = Theme()
        let result = EffectiveStyle.effectiveHeaderFont(theme: theme)
        XCTAssertTrue(result.isEqual(UIFont.preferredFont(forTextStyle: .footnote)))
    }

    /// `Theme.headerFont` 指定 / `headerFontSize` 未指定 → headerFont そのまま。
    func test_effectiveHeaderFont_headerFontだけ指定_そのまま使う() {
        let custom = UIFont(name: "Avenir-Heavy", size: 18)!
        let theme = Theme(headerFont: custom)
        let result = EffectiveStyle.effectiveHeaderFont(theme: theme)
        XCTAssertTrue(result.isEqual(custom))
    }

    /// `Theme.headerFont` 指定 / `headerFontSize > 0` → headerFont をベースに size 上書き。
    func test_effectiveHeaderFont_headerFontSize優先_size上書き() {
        let base = UIFont.systemFont(ofSize: 14, weight: .regular)
        let theme = Theme(headerFontSize: 24, headerFont: base)
        let result = EffectiveStyle.effectiveHeaderFont(theme: theme)
        XCTAssertEqual(result.pointSize, 24)
    }

    /// `Theme.headerFont` 未指定 / `headerFontSize > 0` → footnote 既定の size 上書き。
    func test_effectiveHeaderFont_headerFontなしheaderFontSizeのみ_size上書き() {
        let theme = Theme(headerFontSize: 22)
        let result = EffectiveStyle.effectiveHeaderFont(theme: theme)
        XCTAssertEqual(result.pointSize, 22)
    }

    /// `Theme.footerFont` 未指定 / `footerFontSize` 未指定 → footnote 既定にフォールバック。
    func test_effectiveFooterFont_全て未指定なら既定footnote() {
        let theme = Theme()
        let result = EffectiveStyle.effectiveFooterFont(theme: theme)
        XCTAssertTrue(result.isEqual(UIFont.preferredFont(forTextStyle: .footnote)))
    }

    /// `Theme.footerFont` 指定 / `footerFontSize > 0` → footerFont をベースに size 上書き。
    func test_effectiveFooterFont_footerFontSize優先_size上書き() {
        let base = UIFont.systemFont(ofSize: 14, weight: .regular)
        let theme = Theme(footerFontSize: 28, footerFont: base)
        let result = EffectiveStyle.effectiveFooterFont(theme: theme)
        XCTAssertEqual(result.pointSize, 28)
    }
}
#endif
