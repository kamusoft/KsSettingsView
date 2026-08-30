// CellVisibilityTests.swift
// KsSettingsViewUITests
//
// 7 Cell の `isVisible` 既定値・等価性・DSL 経路（withDSLID / withStyle / withIcon）での保持テスト。
// `VisibilityAware` 準拠の確認。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewCore
@testable import KsSettingsViewUI

final class CellVisibilityTests: XCTestCase {

    // MARK: - 既定値

    func test_LabelCell_isVisible_既定値_true() {
        let cell = LabelCell(title: "a")
        XCTAssertTrue(cell.isVisible)
    }

    func test_CommandCell_isVisible_既定値_true() {
        let cell = CommandCell(title: "a")
        XCTAssertTrue(cell.isVisible)
    }

    func test_ButtonCell_isVisible_既定値_true() {
        let cell = ButtonCell(title: "a")
        XCTAssertTrue(cell.isVisible)
    }

    func test_SwitchCell_isVisible_既定値_true() {
        let cell = SwitchCell(title: "a")
        XCTAssertTrue(cell.isVisible)
    }

    func test_CheckboxCell_isVisible_既定値_true() {
        let cell = CheckboxCell(title: "a")
        XCTAssertTrue(cell.isVisible)
    }

    func test_RadioCell_isVisible_既定値_true() {
        let cell = RadioCell(title: "a", groupId: "g", value: "v", selectedValue: "v")
        XCTAssertTrue(cell.isVisible)
    }

    func test_SimpleCheckCell_isVisible_既定値_true() {
        let cell = SimpleCheckCell(title: "a")
        XCTAssertTrue(cell.isVisible)
    }

    // MARK: - VisibilityAware 準拠

    func test_7Cell_全て_VisibilityAware_に準拠する() {
        XCTAssertNotNil(LabelCell(title: "a") as VisibilityAware)
        XCTAssertNotNil(CommandCell(title: "a") as VisibilityAware)
        XCTAssertNotNil(ButtonCell(title: "a") as VisibilityAware)
        XCTAssertNotNil(SwitchCell(title: "a") as VisibilityAware)
        XCTAssertNotNil(CheckboxCell(title: "a") as VisibilityAware)
        XCTAssertNotNil(RadioCell(title: "a", groupId: "g", value: "v", selectedValue: "v") as VisibilityAware)
        XCTAssertNotNil(SimpleCheckCell(title: "a") as VisibilityAware)
    }

    // MARK: - 等価性に isVisible を含める

    func test_LabelCell_isVisible_のみ異なる_等価ではない() {
        let id = UUID()
        let a = LabelCell(id: id, title: "x", isVisible: true)
        let b = LabelCell(id: id, title: "x", isVisible: false)
        XCTAssertNotEqual(a, b)
    }

    func test_SwitchCell_isVisible_のみ異なる_等価ではない() {
        let id = UUID()
        let a = SwitchCell(id: id, title: "x", isOn: true, isVisible: true)
        let b = SwitchCell(id: id, title: "x", isOn: true, isVisible: false)
        XCTAssertNotEqual(a, b)
    }

    // MARK: - DSL 経路（withDSLID / withStyle / withIcon）での保持

    func test_LabelCell_withDSLID_isVisible_を保持() {
        let cell = LabelCell(title: "x", isVisible: false)
        let rebound = cell.withDSLID(UUID())
        XCTAssertFalse(rebound.isVisible)
    }

    func test_LabelCell_withStyle_isVisible_を保持() {
        let cell = LabelCell(title: "x", isVisible: false)
        let restyled = cell.withStyle(CellStyle())
        XCTAssertFalse(restyled.isVisible)
    }

    func test_LabelCell_withIcon_isVisible_を保持() {
        let cell = LabelCell(title: "x", isVisible: false)
        let rebound = cell.withIcon(nil)
        XCTAssertFalse(rebound.isVisible)
    }

    func test_CommandCell_withDSLID_withStyle_withIcon_isVisible_を保持() {
        let cell = CommandCell(title: "x", isVisible: false)
        XCTAssertFalse(cell.withDSLID(UUID()).isVisible)
        XCTAssertFalse(cell.withStyle(CellStyle()).isVisible)
        XCTAssertFalse(cell.withIcon(nil).isVisible)
    }

    func test_ButtonCell_withDSLID_withStyle_withIcon_isVisible_を保持() {
        let cell = ButtonCell(title: "x", isVisible: false)
        XCTAssertFalse(cell.withDSLID(UUID()).isVisible)
        XCTAssertFalse(cell.withStyle(CellStyle()).isVisible)
        XCTAssertFalse(cell.withIcon(nil).isVisible)
    }

    func test_SwitchCell_withDSLID_withStyle_withIcon_isVisible_を保持() {
        let cell = SwitchCell(title: "x", isVisible: false)
        XCTAssertFalse(cell.withDSLID(UUID()).isVisible)
        XCTAssertFalse(cell.withStyle(CellStyle()).isVisible)
        XCTAssertFalse(cell.withIcon(nil).isVisible)
    }

    func test_CheckboxCell_withDSLID_withStyle_withIcon_isVisible_を保持() {
        let cell = CheckboxCell(title: "x", isVisible: false)
        XCTAssertFalse(cell.withDSLID(UUID()).isVisible)
        XCTAssertFalse(cell.withStyle(CellStyle()).isVisible)
        XCTAssertFalse(cell.withIcon(nil).isVisible)
    }

    func test_RadioCell_withDSLID_withStyle_withIcon_isVisible_を保持() {
        let cell = RadioCell(title: "x", groupId: "g", value: "v", selectedValue: "v", isVisible: false)
        XCTAssertFalse(cell.withDSLID(UUID()).isVisible)
        XCTAssertFalse(cell.withStyle(CellStyle()).isVisible)
        XCTAssertFalse(cell.withIcon(nil).isVisible)
    }

    func test_SimpleCheckCell_withDSLID_withStyle_withIcon_isVisible_を保持() {
        let cell = SimpleCheckCell(title: "x", isVisible: false)
        XCTAssertFalse(cell.withDSLID(UUID()).isVisible)
        XCTAssertFalse(cell.withStyle(CellStyle()).isVisible)
        XCTAssertFalse(cell.withIcon(nil).isVisible)
    }

    // MARK: - VisibilityAware 非準拠の Cell（dummy）

    func test_VisibilityAware非準拠Cell_は_keyPath経由_でnil() {
        // TestDummyCell は VisibilityAware に準拠していないため、`as? VisibilityAware` は nil
        let cell = TestDummyCell(label: "x")
        XCTAssertNil(cell as? VisibilityAware)
    }
}
#endif
