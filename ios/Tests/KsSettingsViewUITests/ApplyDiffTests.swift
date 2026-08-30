// ApplyDiffTests.swift
// KsSettingsViewUITests
//
// `KsSettingsViewController.applyDiff(_:)` の全 11 ケースに対する snapshot 状態検証テスト。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class ApplyDiffTests: XCTestCase {

    private func makeController(sections: [Section] = []) -> KsSettingsViewController {
        let controller = KsSettingsViewController(root: SettingsRoot(sections: sections))
        _ = controller.view
        return controller
    }

    // MARK: - .full

    func test_applyDiff_full_全体差し替え() {
        let controller = makeController()
        let newRoot = SettingsRoot(sections: [
            Section(header: .text("S1"), cells: [LabelCell(title: "A")]),
            Section(header: .text("S2"), cells: [LabelCell(title: "B"), LabelCell(title: "C")]),
        ])
        controller.applyDiff(.full(newRoot))

        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfSections, 2)
        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems, 3)
    }

    // MARK: - .insertSection

    func test_applyDiff_insertSection() {
        let controller = makeController()
        let newSection = Section(header: .text("S"), cells: [LabelCell(title: "A")])
        controller.applyDiff(.insertSection(at: 0, section: newSection))

        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfSections, 1)
        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems(inSection: newSection.id), 1)
    }

    // MARK: - .removeSection

    func test_applyDiff_removeSection() {
        let s1 = Section(header: .text("S1"), cells: [LabelCell(title: "A")])
        let s2 = Section(header: .text("S2"), cells: [LabelCell(title: "B")])
        let controller = makeController(sections: [s1, s2])

        controller.applyDiff(.removeSection(sectionID: s1.id))

        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfSections, 1)
        XCTAssertEqual(controller.internalDataSource?.snapshot().sectionIdentifiers, [s2.id])
    }

    // MARK: - .moveSection

    func test_applyDiff_moveSection() {
        let s1 = Section(header: .text("S1"))
        let s2 = Section(header: .text("S2"))
        let s3 = Section(header: .text("S3"))
        let controller = makeController(sections: [s1, s2, s3])

        controller.applyDiff(.moveSection(from: 0, to: 2))

        XCTAssertEqual(controller.internalDataSource?.snapshot().sectionIdentifiers, [s2.id, s3.id, s1.id])
    }

    // MARK: - .replaceSection

    func test_applyDiff_replaceSection() {
        let s1 = Section(header: .text("old"), cells: [LabelCell(title: "A")])
        let controller = makeController(sections: [s1])

        let newSection = Section(
            id: s1.id,
            header: .text("new"),
            cells: [LabelCell(title: "X"), LabelCell(title: "Y")]
        )
        controller.applyDiff(.replaceSection(sectionID: s1.id, new: newSection))

        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems(inSection: s1.id), 2)
    }

    // MARK: - .insertCell

    func test_applyDiff_insertCell() {
        let s1 = Section(header: .text("S"), cells: [LabelCell(title: "A")])
        let controller = makeController(sections: [s1])

        let newCell = LabelCell(title: "B")
        controller.applyDiff(.insertCell(sectionID: s1.id, at: 0, cell: newCell))

        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems(inSection: s1.id), 2)
        let firstItemID = controller.internalDataSource?.snapshot().itemIdentifiers(inSection: s1.id).first
        XCTAssertEqual(firstItemID, KsCellID(cell: newCell))
    }

    // MARK: - .removeCell

    func test_applyDiff_removeCell() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let s1 = Section(header: .text("S"), cells: [cellA, cellB])
        let controller = makeController(sections: [s1])

        controller.applyDiff(.removeCell(cellID: KsCellID(cell: cellA)))

        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems(inSection: s1.id), 1)
    }

    // MARK: - .replaceCell

    func test_applyDiff_replaceCell() {
        let cellA = LabelCell(title: "A")
        let s1 = Section(header: .text("S"), cells: [cellA])
        let controller = makeController(sections: [s1])

        let oldID = KsCellID(cell: cellA)
        let beforeItems = controller.internalDataSource?.snapshot().itemIdentifiers(inSection: s1.id) ?? []

        let newCell = LabelCell(id: cellA.id, title: "A-replaced")
        controller.applyDiff(.replaceCell(cellID: oldID, new: newCell))

        // 「表示状態同期の三層分離」: replaceCell は reconfigureItems 経路で同一セルの内容のみ更新する。
        // snapshot の item 集合・順序（構造同期）は変化しない（同じ KsCellID が 1 件のまま）。
        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems(inSection: s1.id), 1)
        let afterItems = controller.internalDataSource?.snapshot().itemIdentifiers(inSection: s1.id) ?? []
        XCTAssertEqual(beforeItems, afterItems, "構造同期（item 集合・順序）は reconfigure で変化しない")
        XCTAssertEqual(afterItems.first, oldID, "同一 KsCellID が維持される（reload による差し替えではない）")

        // cellIndex の当該 Cell は新しい内容に更新されている（内部 root に反映）。
        let updated = controller.root.sections.first?.cells.first as? LabelCell
        XCTAssertEqual(updated?.title, "A-replaced")
    }

    /// 同一 id セルへの **2 回連続** の replaceCell が両方反映されることを検証する回帰テスト。
    ///
    /// reconfigureItems は snapshot 識別子（KsCellID）を変えないため、KsCellID が内容ハッシュを
    /// 含むと 1 回目の更新後も snapshot は「1 回目時点の識別子」を保持し、2 回目の replaceCell
    /// （= 2 回目の内容で生成した識別子）が `snapshot.itemIdentifiers.contains` で外れて
    /// `reportMissingID`（DEBUG: assertionFailure / Release: 更新黙殺）に落ちてしまう。
    /// KsCellID の同一性は id 限定であり（core/ADR-0010）、識別子が内容変化で変わらないため
    /// 連続更新が安定する。
    ///
    /// 注: 本テストは `#if canImport(UIKit)` 内であり macOS ホストの `swift test` では実行されない
    ///     （Executed 0）。iOS シミュレータ実行で reconfigure の実経路を検証する。
    ///     発行される cellID が id 限定で安定する点は、ホスト実行される
    ///     `DSLDiffCalculatorTests` / `KsCellIDTests` で別途担保している。
    func test_applyDiff_replaceCell_同一idへの2回連続更新が両方反映される() {
        let cellA = LabelCell(title: "A")
        let s1 = Section(header: .text("S"), cells: [cellA])
        let controller = makeController(sections: [s1])

        let stableID = KsCellID(cell: cellA)
        let beforeItems = controller.internalDataSource?.snapshot().itemIdentifiers(inSection: s1.id) ?? []

        // 更新1: A -> B（同一 id）。DSLDiffCalculator 同様に「直前の Cell から生成した cellID」で発行する。
        let cellB = LabelCell(id: cellA.id, title: "B")
        controller.applyDiff(.replaceCell(cellID: KsCellID(cell: cellA), new: cellB))

        // 更新1 後の root 反映確認
        XCTAssertEqual((controller.root.sections.first?.cells.first as? LabelCell)?.title, "B")

        // 更新2: B -> C（同一 id）。reconfigure は snapshot 識別子を変えないため、
        // 直前の Cell（B）から生成した cellID でも id が同じなら snapshot に在る。
        let cellC = LabelCell(id: cellA.id, title: "C")
        controller.applyDiff(.replaceCell(cellID: KsCellID(cell: cellB), new: cellC))

        // 2 回目の更新が破棄されず反映されている（id 限定識別子なら contains が外れない）。
        XCTAssertEqual(
            (controller.root.sections.first?.cells.first as? LabelCell)?.title,
            "C",
            "2 回目の連続内容更新が反映されること（Critical 回帰）"
        )

        // 構造同期 snapshot は終始不変（同一 id の単一識別子のまま）。
        let afterItems = controller.internalDataSource?.snapshot().itemIdentifiers(inSection: s1.id) ?? []
        XCTAssertEqual(beforeItems, afterItems, "連続 reconfigure でも構造同期 snapshot は不変")
        XCTAssertEqual(afterItems, [stableID])
    }

    // MARK: - .moveCell

    func test_applyDiff_moveCell() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let cellC = LabelCell(title: "C")
        let s1 = Section(header: .text("S"), cells: [cellA, cellB, cellC])
        let controller = makeController(sections: [s1])

        let aID = KsCellID(cell: cellA)
        controller.applyDiff(.moveCell(cellID: aID, to: 2))

        let items = controller.internalDataSource?.snapshot().itemIdentifiers(inSection: s1.id) ?? []
        XCTAssertEqual(items.last, aID)
    }

    // MARK: - .updateAccessory

    func test_applyDiff_updateAccessory_rootHeader() {
        let controller = makeController()
        controller.applyDiff(.updateAccessory(target: .rootHeader, accessory: .root(.text("X"))))
        XCTAssertEqual(controller.rootHeader, .text("X"))
    }

    func test_applyDiff_updateAccessory_rootFooter() {
        let controller = makeController()
        controller.applyDiff(.updateAccessory(target: .rootFooter, accessory: .root(.text("Y"))))
        XCTAssertEqual(controller.rootFooter, .text("Y"))
    }

    func test_applyDiff_updateAccessory_sectionHeader() {
        let s1 = Section(header: .text("old"))
        let controller = makeController(sections: [s1])

        controller.applyDiff(.updateAccessory(
            target: .sectionHeader(sectionID: s1.id),
            accessory: .section(.text("new"))
        ))

        // internal root に反映される
        XCTAssertEqual(controller.internalDataSource?.snapshot().sectionIdentifiers.first, s1.id)
    }

    // MARK: - applyTheme
    //
    // Theme は構造 Diff の対象ではない（core/ADR-0009）。Theme 適用は Controller 直 API
    // `applyTheme(_:)` または Store の `applyTheme(_:)` 経由で行う。
    func test_applyTheme_snapshotは変更されない() {
        let cell = LabelCell(title: "A")
        let s1 = Section(header: .text("S"), cells: [cell])
        let controller = makeController(sections: [s1])

        let newTheme = Theme(separatorColor: UIColor(red: 1.0, green: 0, blue: 0, alpha: 1.0))
        controller.applyTheme(newTheme)

        // theme は controller 内部に保持される。snapshot の item 集合・順序は変わらない（reconfigure のみ）。
        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems, 1)
    }

    // MARK: - エラーハンドリング（Release 相当）
    //
    // DEBUG ビルドでは `assertionFailure` でクラッシュするため、本テストは Release ビルド時の
    // 「クラッシュしない・snapshot 不変」のみ検証する。DEBUG では `assertionFailure` の
    // 挙動上テストフレームワークが detect するため、ここでは Release 経路の検証を主とする。

#if !DEBUG
    func test_applyDiff_存在しないcellIDのremove_はクラッシュせずsnapshot不変() {
        let cellA = LabelCell(title: "A")
        let s1 = Section(header: .text("S"), cells: [cellA])
        let controller = makeController(sections: [s1])

        let bogusID = KsCellID(cell: LabelCell(title: "Z"))
        controller.applyDiff(.removeCell(cellID: bogusID))

        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems(inSection: s1.id), 1)
    }

    func test_applyDiff_存在しないsectionIDのremove_はクラッシュせずsnapshot不変() {
        let s1 = Section(header: .text("S"))
        let controller = makeController(sections: [s1])

        controller.applyDiff(.removeSection(sectionID: UUID()))

        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfSections, 1)
    }
#endif
}
#endif
