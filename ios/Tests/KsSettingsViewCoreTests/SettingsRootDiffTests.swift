// SettingsRootDiffTests.swift
// KsSettingsViewCoreTests
//
// `SettingsRootDiff` の全ケースについて、生成と payload の取り出しが対称であること、
// および等価性がケースと payload の両方で決まることを検証する。

import XCTest
@testable import KsSettingsViewCore

final class SettingsRootDiffTests: XCTestCase {

    // MARK: - 生成・payload 取り出しテスト（全 11 ケース）

    func test_full_生成と_payload_取り出し() {
        // GIVEN
        let root = SettingsRoot(sections: [])
        // WHEN
        let diff: SettingsRootDiff = .full(root)
        // THEN
        guard case .full(let extracted) = diff else {
            XCTFail("Expected .full case")
            return
        }
        XCTAssertEqual(extracted, root)
    }

    func test_insertSection_生成と_payload_取り出し() {
        // GIVEN
        let section = Section(header: .text("S"), cells: [])
        // WHEN
        let diff: SettingsRootDiff = .insertSection(at: 2, section: section)
        // THEN
        guard case let .insertSection(index, extractedSection) = diff else {
            XCTFail("Expected .insertSection case")
            return
        }
        XCTAssertEqual(index, 2)
        XCTAssertEqual(extractedSection, section)
    }

    func test_removeSection_生成と_payload_取り出し() {
        let sectionID = UUID()
        let diff: SettingsRootDiff = .removeSection(sectionID: sectionID)
        guard case .removeSection(let id) = diff else {
            XCTFail("Expected .removeSection case")
            return
        }
        XCTAssertEqual(id, sectionID)
    }

    func test_moveSection_生成と_payload_取り出し() {
        let diff: SettingsRootDiff = .moveSection(from: 1, to: 4)
        guard case let .moveSection(from, to) = diff else {
            XCTFail("Expected .moveSection case")
            return
        }
        XCTAssertEqual(from, 1)
        XCTAssertEqual(to, 4)
    }

    func test_replaceSection_生成と_payload_取り出し() {
        let sectionID = UUID()
        let newSection = Section(header: .text("new"), cells: [])
        let diff: SettingsRootDiff = .replaceSection(sectionID: sectionID, new: newSection)
        guard case let .replaceSection(id, new) = diff else {
            XCTFail("Expected .replaceSection case")
            return
        }
        XCTAssertEqual(id, sectionID)
        XCTAssertEqual(new, newSection)
    }

    func test_insertCell_生成と_payload_取り出し() {
        let sectionID = UUID()
        let cell = DummyLabelCell(title: "A")
        let diff: SettingsRootDiff = .insertCell(sectionID: sectionID, at: 3, cell: cell)
        guard case let .insertCell(id, index, extractedCell) = diff else {
            XCTFail("Expected .insertCell case")
            return
        }
        XCTAssertEqual(id, sectionID)
        XCTAssertEqual(index, 3)
        XCTAssertEqual(AnyHashable(extractedCell), AnyHashable(cell))
    }

    func test_removeCell_生成と_payload_取り出し() {
        let cell = DummyLabelCell(title: "A")
        let cellID = KsCellID(cell: cell)
        let diff: SettingsRootDiff = .removeCell(cellID: cellID)
        guard case .removeCell(let id) = diff else {
            XCTFail("Expected .removeCell case")
            return
        }
        XCTAssertEqual(id, cellID)
    }

    func test_replaceCell_生成と_payload_取り出し() {
        let oldCell = DummyLabelCell(title: "old")
        let cellID = KsCellID(cell: oldCell)
        let newCell = DummyLabelCell(id: oldCell.id, title: "new")
        let diff: SettingsRootDiff = .replaceCell(cellID: cellID, new: newCell)
        guard case let .replaceCell(id, extractedNew) = diff else {
            XCTFail("Expected .replaceCell case")
            return
        }
        XCTAssertEqual(id, cellID)
        XCTAssertEqual(AnyHashable(extractedNew), AnyHashable(newCell))
    }

    func test_moveCell_生成と_payload_取り出し() {
        let cell = DummyLabelCell(title: "A")
        let cellID = KsCellID(cell: cell)
        let diff: SettingsRootDiff = .moveCell(cellID: cellID, to: 5)
        guard case let .moveCell(id, to) = diff else {
            XCTFail("Expected .moveCell case")
            return
        }
        XCTAssertEqual(id, cellID)
        XCTAssertEqual(to, 5)
    }

    func test_updateAccessory_RootHeader_text_生成と_payload_取り出し() {
        let target: AccessoryTarget = .rootHeader
        let accessory: SettingsAccessory = .root(.text("プロフィール"))
        let diff: SettingsRootDiff = .updateAccessory(target: target, accessory: accessory)
        guard case let .updateAccessory(extractedTarget, extractedAccessory) = diff else {
            XCTFail("Expected .updateAccessory case")
            return
        }
        XCTAssertEqual(extractedTarget, target)
        XCTAssertEqual(extractedAccessory, accessory)
    }

    func test_updateAccessory_SectionHeader_nil_削除を表現できる() {
        // GIVEN: target = sectionHeader、accessory = nil（削除）
        let sectionID = UUID()
        let target: AccessoryTarget = .sectionHeader(sectionID: sectionID)
        let diff: SettingsRootDiff = .updateAccessory(target: target, accessory: nil)
        // WHEN
        guard case let .updateAccessory(extractedTarget, extractedAccessory) = diff else {
            XCTFail("Expected .updateAccessory case")
            return
        }
        // THEN
        XCTAssertEqual(extractedTarget, target)
        XCTAssertNil(extractedAccessory)
    }

    // MARK: - 等価性テスト

    func test_等価性_同一ケース同一payload_は等価() {
        let a: SettingsRootDiff = .removeSection(sectionID: UUID(uuidString: "00000000-0000-0000-0000-000000000001")!)
        let b: SettingsRootDiff = .removeSection(sectionID: UUID(uuidString: "00000000-0000-0000-0000-000000000001")!)
        XCTAssertEqual(a, b)
        XCTAssertEqual(a.hashValue, b.hashValue)
    }

    func test_等価性_異なるケース_は不等() {
        let a: SettingsRootDiff = .removeSection(sectionID: UUID())
        let b: SettingsRootDiff = .moveSection(from: 0, to: 1)
        XCTAssertNotEqual(a, b)
    }

    func test_等価性_同一ケース異なるpayload_は不等() {
        let a: SettingsRootDiff = .moveSection(from: 0, to: 1)
        let b: SettingsRootDiff = .moveSection(from: 0, to: 2)
        XCTAssertNotEqual(a, b)
    }

    func test_等価性_insertCell_同一Cell_は等価() {
        let sectionID = UUID()
        let cell = DummyLabelCell(title: "A")
        let a: SettingsRootDiff = .insertCell(sectionID: sectionID, at: 0, cell: cell)
        let b: SettingsRootDiff = .insertCell(sectionID: sectionID, at: 0, cell: cell)
        XCTAssertEqual(a, b)
        XCTAssertEqual(a.hashValue, b.hashValue)
    }

    func test_等価性_insertCell_異なるCellフィールド_は不等() {
        let sectionID = UUID()
        let a: SettingsRootDiff = .insertCell(sectionID: sectionID, at: 0, cell: DummyLabelCell(title: "A"))
        let b: SettingsRootDiff = .insertCell(sectionID: sectionID, at: 0, cell: DummyLabelCell(title: "B"))
        XCTAssertNotEqual(a, b)
    }

    func test_等価性_replaceCell_同一は等価() {
        let cell = DummyLabelCell(title: "A")
        let cellID = KsCellID(cell: cell)
        let newCell = DummyLabelCell(id: cell.id, title: "NEW")
        let a: SettingsRootDiff = .replaceCell(cellID: cellID, new: newCell)
        let b: SettingsRootDiff = .replaceCell(cellID: cellID, new: newCell)
        XCTAssertEqual(a, b)
        XCTAssertEqual(a.hashValue, b.hashValue)
    }

    func test_等価性_updateAccessory_同一は等価() {
        let target: AccessoryTarget = .rootHeader
        let accessory: SettingsAccessory = .root(.text("X"))
        let a: SettingsRootDiff = .updateAccessory(target: target, accessory: accessory)
        let b: SettingsRootDiff = .updateAccessory(target: target, accessory: accessory)
        XCTAssertEqual(a, b)
        XCTAssertEqual(a.hashValue, b.hashValue)
    }

    func test_等価性_updateAccessory_nilと非nil_は不等() {
        let target: AccessoryTarget = .rootHeader
        let a: SettingsRootDiff = .updateAccessory(target: target, accessory: .root(.text("X")))
        let b: SettingsRootDiff = .updateAccessory(target: target, accessory: nil)
        XCTAssertNotEqual(a, b)
    }
}
