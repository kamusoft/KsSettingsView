// ThemeRenameTests.swift
// KsSettingsViewUITests
//
// `Theme.backgroundColor` / `Theme.cellTitleColor` / `Theme.cellTitleFont` が
// 参照可能であることを確認する。
//
// `Theme` は `viewBackgroundColor` / `titleColor` / `titleFont` を持たない。
// 存在しない名前を参照するコードがあれば本ターゲットのコンパイル自体が失敗する
// ため、本テスト群は「期待どおりの名前が使用できる」コンパイル成立性をテストランタイムでも
// 担保する位置づけ。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI

final class ThemeRenameTests: XCTestCase {

    // MARK: - backgroundColor（旧 viewBackgroundColor）

    func test_backgroundColor_新名で参照可能() {
        let pink = UIColor(red: 1.0, green: 0.5, blue: 0.6, alpha: 1.0)
        let theme = Theme(backgroundColor: pink)
        XCTAssertTrue(theme.backgroundColor.isEqual(pink))
    }

    func test_backgroundColor_既定値はdefaultBackgroundColor() {
        let theme = Theme()
        XCTAssertTrue(theme.backgroundColor.isEqual(Theme.defaultBackgroundColor))
    }

    // MARK: - cellTitleColor（旧 titleColor）

    func test_cellTitleColor_新名で参照可能() {
        let blue = UIColor(red: 0.0, green: 0.0, blue: 1.0, alpha: 1.0)
        let theme = Theme(cellTitleColor: blue)
        XCTAssertNotNil(theme.cellTitleColor)
        XCTAssertTrue(theme.cellTitleColor!.isEqual(blue))
    }

    func test_cellTitleColor_既定値はnil() {
        let theme = Theme()
        XCTAssertNil(theme.cellTitleColor)
    }

    // MARK: - cellTitleFont（旧 titleFont）

    func test_cellTitleFont_新名で参照可能() {
        let font = UIFont.systemFont(ofSize: 18.0, weight: .semibold)
        let theme = Theme(cellTitleFont: font)
        XCTAssertNotNil(theme.cellTitleFont)
        XCTAssertTrue(theme.cellTitleFont!.isEqual(font))
    }

    func test_cellTitleFont_既定値はnil() {
        let theme = Theme()
        XCTAssertNil(theme.cellTitleFont)
    }

    // MARK: - backgroundColor / cellBackgroundColor が独立に保持される

    func test_backgroundColor_と_cellBackgroundColor_は独立() {
        let viewBg = UIColor(red: 0.95, green: 0.93, blue: 0.90, alpha: 1.0)
        let cellBg = UIColor.white
        let theme = Theme(backgroundColor: viewBg, cellBackgroundColor: cellBg)
        XCTAssertTrue(theme.backgroundColor.isEqual(viewBg))
        XCTAssertTrue(theme.cellBackgroundColor.isEqual(cellBg))
        XCTAssertFalse(theme.backgroundColor.isEqual(theme.cellBackgroundColor))
    }
}
#endif
