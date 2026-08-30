// DiffableDataSourceTests.swift
// KsSettingsViewUITests
//
// `applyDiff(.full(...))` で全体 snapshot が反映され、`applyDiff(.insertCell(...))` 等で
// 部分更新が正しく行われることを検証する。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class DiffableDataSourceTests: XCTestCase {
    func test_full_diffを2回適用してもsnapshotのitem数は変わらない() {
        let controller = KsSettingsViewController(root: SettingsRoot())
        _ = controller.view

        let cellId = UUID()
        let sectionId = UUID()
        let section = Section(
            id: sectionId,
            header: .text("S1"),
            cells: [LabelCell(id: cellId, title: "A")]
        )
        let root = SettingsRoot(sections: [section])
        controller.applyDiff(.full(root))
        controller.applyDiff(.full(root))

        let snapshot = controller.internalDataSource?.snapshot()
        XCTAssertEqual(snapshot?.numberOfSections, 1)
        XCTAssertEqual(snapshot?.numberOfItems(inSection: sectionId), 1)
    }

    func test_insertCell_diffで1件Cellが追加される() {
        let sectionId = UUID()
        let cellA = LabelCell(title: "A")

        let controller = KsSettingsViewController(root: SettingsRoot(sections: [
            Section(id: sectionId, header: .text("S1"), cells: [cellA])
        ]))
        _ = controller.view

        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems(inSection: sectionId), 1)

        let cellB = LabelCell(title: "B")
        controller.applyDiff(.insertCell(sectionID: sectionId, at: 1, cell: cellB))

        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems(inSection: sectionId), 2)
    }
}
#endif
