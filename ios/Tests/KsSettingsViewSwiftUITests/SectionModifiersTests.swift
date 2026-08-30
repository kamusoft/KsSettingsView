// SectionModifiersTests.swift
// KsSettingsViewSwiftUITests
//
// Section modifier（`.sectionHeader(...)` / `.sectionFooter(...)` / `.sectionID(...)`）の動作を検証する。

import XCTest
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewCore

#if canImport(UIKit)
@testable import KsSettingsViewUI

final class SectionModifiersTests: XCTestCase {

    func test_sectionHeader_String_でheaderが上書きされる() {
        let section = KsSettingsViewCore.Section(cells: [DummyTestCell(title: "A")])
        let modified = section.sectionHeader("見出し")
        XCTAssertEqual(modified.header, .text("見出し"))
    }

    func test_sectionFooter_String_でfooterが上書きされる() {
        let section = KsSettingsViewCore.Section(cells: [DummyTestCell(title: "A")])
        let modified = section.sectionFooter("注釈")
        XCTAssertEqual(modified.footer, .text("注釈"))
    }

    func test_sectionID_明示指定が_HintRegistry_に記録される() {
        DSLHintRegistry.shared.reset()
        let section = KsSettingsViewCore.Section(cells: [DummyTestCell(title: "A")])
        _ = section.sectionID("dynamic-section-1")
        let hint = DSLHintRegistry.shared.sectionHint(for: section.id)
        if case .explicit(let id) = hint {
            XCTAssertEqual(id, AnyHashable("dynamic-section-1"))
        } else {
            XCTFail("Expected .explicit hint, got \(String(describing: hint))")
        }
    }

    /// modifier は Section を再構築するため、accessory 以外のフィールドを既定値へ落とさない。
    func test_モディファイアはid以外の状態フィールドを保持する() {
        let section = KsSettingsViewCore.Section(
            cells: [DummyTestCell(title: "A")],
            headerHeight: 40,
            isVisible: false,
            isHeaderVisible: false,
            isFooterVisible: false
        )
        let modified = section.sectionHeader("見出し").sectionFooter("注釈")
        XCTAssertEqual(modified.id, section.id)
        XCTAssertEqual(modified.headerHeight, 40)
        XCTAssertFalse(modified.isVisible, "isVisible が既定値へ戻っている")
        XCTAssertFalse(modified.isHeaderVisible, "isHeaderVisible が既定値へ戻っている")
        XCTAssertFalse(modified.isFooterVisible, "isFooterVisible が既定値へ戻っている")
    }

    func test_モディファイア連鎖でも元Sectionは不変() {
        let section = KsSettingsViewCore.Section(cells: [DummyTestCell(title: "A")])
        let modified = section.sectionHeader("H").sectionFooter("F")
        XCTAssertNil(section.header)
        XCTAssertNil(section.footer)
        XCTAssertEqual(modified.header, .text("H"))
        XCTAssertEqual(modified.footer, .text("F"))
    }
}
#endif
