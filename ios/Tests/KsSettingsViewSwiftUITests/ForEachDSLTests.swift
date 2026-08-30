// ForEachDSLTests.swift
// KsSettingsViewSwiftUITests
//
// 独自 `ForEach` 関数 4 オーバーロードの動作を検証する。

import XCTest
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewCore

#if canImport(UIKit)
@testable import KsSettingsViewUI

private struct TodoItem: Identifiable, Equatable {
    let id: Int
    let name: String
}

private struct LegacyModel: Equatable {
    let myKey: String
    let name: String
}

final class ForEachDSLTests: XCTestCase {

    func test_セクション内_Identifiable版_ForEach_でCellが展開される() {
        DSLHintRegistry.shared.reset()
        let items = [
            TodoItem(id: 1, name: "A"),
            TodoItem(id: 2, name: "B"),
        ]
        let cells: [any KsCell] = ForEach(items) { item in
            DummyTestCell(title: item.name)
        }
        XCTAssertEqual(cells.count, 2)
        // 各 Cell に forEach ヒントが付与されている
        for cell in cells {
            let hint = DSLHintRegistry.shared.cellHint(for: cell.id)
            if case .forEach(let id) = hint {
                XCTAssertTrue([AnyHashable(1), AnyHashable(2)].contains(id))
            } else {
                XCTFail("Expected .forEach hint, got \(String(describing: hint))")
            }
        }
    }

    func test_セクション内_id_KeyPath版_ForEach_でCellが展開される() {
        DSLHintRegistry.shared.reset()
        let items = [
            LegacyModel(myKey: "K1", name: "A"),
            LegacyModel(myKey: "K2", name: "B"),
        ]
        let cells: [any KsCell] = ForEach(items, id: \.myKey) { item in
            DummyTestCell(title: item.name)
        }
        XCTAssertEqual(cells.count, 2)
        for cell in cells {
            let hint = DSLHintRegistry.shared.cellHint(for: cell.id)
            if case .forEach(let id) = hint {
                XCTAssertTrue([AnyHashable("K1"), AnyHashable("K2")].contains(id))
            } else {
                XCTFail("Expected .forEach hint, got \(String(describing: hint))")
            }
        }
    }

    func test_ルート_Identifiable版_ForEach_でSection群が展開される() {
        DSLHintRegistry.shared.reset()
        let items = [TodoItem(id: 1, name: "A"), TodoItem(id: 2, name: "B")]
        let sections: [KsSettingsViewCore.Section] = ForEach(items) { item in
            KsSettingsViewCore.Section(cells: [DummyTestCell(title: item.name)])
        }
        XCTAssertEqual(sections.count, 2)
        for section in sections {
            let hint = DSLHintRegistry.shared.sectionHint(for: section.id)
            if case .forEach(let id) = hint {
                XCTAssertTrue([AnyHashable(1), AnyHashable(2)].contains(id))
            } else {
                XCTFail("Expected .forEach hint, got \(String(describing: hint))")
            }
        }
    }

    func test_空コレクションでも例外なく展開される() {
        DSLHintRegistry.shared.reset()
        let cells: [any KsCell] = ForEach([TodoItem]()) { item in
            DummyTestCell(title: item.name)
        }
        XCTAssertEqual(cells.count, 0)
    }
}
#endif
