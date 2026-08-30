// VisibilityProjectionTests.swift
// KsSettingsViewUITests
//
// `KsSettingsViewController` の visible projection 化テスト。Section.isVisible / Cell.isVisible が
// snapshot から除外されること、hidden Cell / Section に対する部分 Diff が no-op になること、
// ReplaceCell / ReplaceSection の Full 経路フォールバックが発火することを検証する。
//
// 検証範囲:
//   - visible projection と全体ツリーの二重管理
//   - 部分 Diff の index 規約と hidden 対象の no-op
//   - ReplaceCell / ReplaceSection の可視性切替防御

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class VisibilityProjectionTests: XCTestCase {

    private func makeController(sections: [Section] = []) -> KsSettingsViewController {
        let controller = KsSettingsViewController(root: SettingsRoot(sections: sections))
        _ = controller.view
        return controller
    }

    // MARK: - visible projection の基本

    func test_Section_isVisible_false_の_Section_は_snapshot_から除外される() {
        let s1 = Section(header: .text("S1"), cells: [LabelCell(title: "A")], isVisible: true)
        let s2 = Section(header: .text("S2"), cells: [LabelCell(title: "B")], isVisible: false)
        let controller = makeController(sections: [s1, s2])

        let snapshot = controller.internalDataSource?.snapshot()
        XCTAssertEqual(snapshot?.numberOfSections, 1)
        XCTAssertEqual(snapshot?.sectionIdentifiers, [s1.id])
    }

    func test_Cell_isVisible_false_の_Cell_は_snapshot_から除外される() {
        let visible = LabelCell(title: "A", isVisible: true)
        let hidden = LabelCell(title: "B", isVisible: false)
        let s1 = Section(header: .text("S1"), cells: [visible, hidden], isVisible: true)
        let controller = makeController(sections: [s1])

        let snapshot = controller.internalDataSource?.snapshot()
        XCTAssertEqual(snapshot?.numberOfItems(inSection: s1.id), 1)
    }

    func test_computeVisibleSections_先頭_hidden_Section_除外() {
        let s1 = Section(header: .text("hidden"), cells: [LabelCell(title: "A")], isVisible: false)
        let s2 = Section(header: .text("visible"), cells: [LabelCell(title: "B")], isVisible: true)
        let visible = KsSettingsViewController.computeVisibleSections(from: [s1, s2])
        XCTAssertEqual(visible.count, 1)
        XCTAssertEqual(visible.first?.id, s2.id)
    }

    // MARK: - isVisible toggle で構造変化

    func test_isVisible_true_to_false_は構造同期上の削除() {
        let visible = LabelCell(title: "A", isVisible: true)
        let s1 = Section(header: .text("S1"), cells: [visible])
        let controller = makeController(sections: [s1])

        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems, 1)

        // 同一 id だが isVisible のみ false に変えた新 Cell に置換 → Full 経路
        let hidden = LabelCell(id: visible.id, title: "A", isVisible: false)
        controller.applyDiff(.replaceCell(cellID: KsCellID(cell: visible), new: hidden))

        XCTAssertEqual(
            controller.internalDataSource?.snapshot().numberOfItems,
            0,
            "isVisible false への切替で snapshot から消える（Full フォールバック）"
        )
    }

    func test_isVisible_false_to_true_は構造同期上の挿入() {
        let hidden = LabelCell(title: "A", isVisible: false)
        let s1 = Section(header: .text("S1"), cells: [hidden])
        let controller = makeController(sections: [s1])

        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems, 0)

        let visible = LabelCell(id: hidden.id, title: "A", isVisible: true)
        controller.applyDiff(.replaceCell(cellID: KsCellID(cell: hidden), new: visible))

        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems, 1)
    }

    // MARK: - 全 hidden でもクラッシュしない

    func test_全Section全Cell_hidden_でも_空表示_クラッシュなし() {
        let s1 = Section(
            header: .text("S1"),
            cells: [LabelCell(title: "A", isVisible: false)],
            isVisible: false
        )
        let controller = makeController(sections: [s1])
        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfSections, 0)
    }

    // MARK: - isEnabled と独立性

    func test_isVisible_false_isEnabled_false_の_Cell_は_描画されない() {
        let cell = LabelCell(title: "A", isEnabled: false, isVisible: false)
        let s1 = Section(header: .text("S1"), cells: [cell])
        let controller = makeController(sections: [s1])
        // 描画されない（snapshot 上 0 アイテム）
        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems, 0)
    }

    // MARK: - hidden 対象の部分 Diff no-op

    func test_hidden_Cell_への_removeCell_は_snapshot_変化なし_model_は更新() {
        let hidden = LabelCell(title: "hidden", isVisible: false)
        let visible = LabelCell(title: "visible", isVisible: true)
        let s1 = Section(header: .text("S1"), cells: [hidden, visible])
        let controller = makeController(sections: [s1])

        let snapshotBefore = controller.internalDataSource?.snapshot().numberOfItems ?? 0
        XCTAssertEqual(snapshotBefore, 1)

        controller.applyDiff(.removeCell(cellID: KsCellID(cell: hidden)))

        // snapshot は変化なし
        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems, 1)
        // model は更新されている（root.sections.cells から hidden が消えている）
        XCTAssertEqual(controller.root.sections.first?.cells.count, 1)
        XCTAssertEqual(controller.root.sections.first?.cells.first?.id, visible.id)
    }

    func test_hidden_Section_への_updateAccessory_は_model_のみ更新() {
        let s1 = Section(
            header: .text("旧"),
            cells: [LabelCell(title: "A")],
            isVisible: false
        )
        let controller = makeController(sections: [s1])

        controller.applyDiff(.updateAccessory(
            target: .sectionHeader(sectionID: s1.id),
            accessory: .section(.text("新"))
        ))

        // model 側は更新されている
        XCTAssertEqual(controller.root.sections.first?.header, .text("新"))

        // 後で isVisible = true に戻すと、更新された accessory が反映される
        let visibleNow = Section(
            id: s1.id,
            header: .text("新"),
            footer: nil,
            cells: s1.cells,
            isVisible: true
        )
        controller.applyDiff(.replaceSection(sectionID: s1.id, new: visibleNow))
        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfSections, 1)
    }

    // MARK: - insertCell の index は model 配列基準

    func test_insertCell_index_は_model_配列基準_hidden_を跨いで挿入() {
        // 初期: [A(visible), B(hidden), C(visible)]
        let a = LabelCell(title: "A", isVisible: true)
        let b = LabelCell(title: "B", isVisible: false)
        let c = LabelCell(title: "C", isVisible: true)
        let s1 = Section(header: .text("S1"), cells: [a, b, c])
        let controller = makeController(sections: [s1])

        // model 基準 index = 2（C の前）に D を挿入
        let d = LabelCell(title: "D", isVisible: true)
        controller.applyDiff(.insertCell(sectionID: s1.id, at: 2, cell: d))

        // model 配列は [A, B, D, C]
        let cells = controller.root.sections.first?.cells ?? []
        XCTAssertEqual(cells.count, 4)
        XCTAssertEqual(cells.map { $0.id }, [a.id, b.id, d.id, c.id])

        // visible projection 上は [A, D, C]（順序）
        let snapshot = controller.internalDataSource?.snapshot()
        let itemIDs = snapshot?.itemIdentifiers(inSection: s1.id) ?? []
        XCTAssertEqual(
            itemIDs.map { $0.id },
            [a.id, d.id, c.id]
        )
    }

    // MARK: - ReplaceSection は常に Full 経路

    func test_replaceSection_は_常に_Full_経路で処理される() {
        let s1 = Section(header: .text("旧"), cells: [LabelCell(title: "A")])
        let controller = makeController(sections: [s1])

        let newSection = Section(
            id: s1.id,
            header: .text("新"),
            footer: nil,
            cells: [LabelCell(title: "X"), LabelCell(title: "Y")],
            isVisible: true
        )
        controller.applyDiff(.replaceSection(sectionID: s1.id, new: newSection))

        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems(inSection: s1.id), 2)
        XCTAssertEqual(controller.root.sections.first?.header, .text("新"))
    }
}
#endif
