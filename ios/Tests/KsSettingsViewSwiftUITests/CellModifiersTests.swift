// CellModifiersTests.swift
// KsSettingsViewSwiftUITests
//
// Cell modifier（`.font(...)` / `.cellHeight(...)` / `.cellID(...)` 等）の動作を検証する。
//
// スタイル系 modifier の引数は Native 型（`UIFont` / `CGFloat`）を直接受け取る
//（core/ADR-0009）。

import XCTest
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewCore

#if canImport(UIKit)
import UIKit
@testable import KsSettingsViewUI

final class CellModifiersTests: XCTestCase {

    func test_fontModifier_でstyle_titleFont_が上書きされる() {
        let cell = DummyTestCell(title: "A")
        let newFont = UIFont.systemFont(ofSize: 24)
        let modified = cell.font(newFont)
        XCTAssertEqual(modified.style.titleFont, newFont)
    }

    func test_cellHeightModifier_でstyle_cellHeight_が上書きされる() {
        let cell = DummyTestCell(title: "A")
        let modified = cell.cellHeight(80)
        XCTAssertEqual(modified.style.cellHeight, 80)
    }

    func test_モディファイア連鎖でも値型は不変() {
        let cell = DummyTestCell(title: "A")
        let newFont = UIFont.systemFont(ofSize: 24)
        let chained = cell.font(newFont).cellHeight(80)
        // 元 cell は何も変更されていない
        XCTAssertEqual(cell.style.cellHeight, nil)
        XCTAssertNil(cell.style.titleFont)
        XCTAssertEqual(chained.style.titleFont, newFont)
        XCTAssertEqual(chained.style.cellHeight, 80)
    }

    func test_cellID_明示指定が_HintRegistry_に記録される() {
        DSLHintRegistry.shared.reset()
        let cell = DummyTestCell(title: "A")
        _ = cell.cellID("dynamic-cell-1")
        let hint = DSLHintRegistry.shared.cellHint(for: cell.id)
        if case .explicit(let id) = hint {
            XCTAssertEqual(id, AnyHashable("dynamic-cell-1"))
        } else {
            XCTFail("Expected .explicit hint, got \(String(describing: hint))")
        }
    }
}
#endif
