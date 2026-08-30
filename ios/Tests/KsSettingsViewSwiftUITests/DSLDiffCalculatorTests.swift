// DSLDiffCalculatorTests.swift
// KsSettingsViewSwiftUITests
//
// `DSLDiffCalculator.compute(from:to:)` の各 Diff 種別を検証する。
//
// Theme 変化は Diff 列には載らず、呼び出し側が `Store.applyTheme(_:)` を別途呼ぶ責務のため、
// Theme に関する検証は「Diff 列に含まれないこと」の確認にとどめる（core/ADR-0009）。

import XCTest
import SwiftUI
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewCore

#if canImport(UIKit)
import UIKit
@testable import KsSettingsViewUI

final class DSLDiffCalculatorTests: XCTestCase {

    // MARK: - ヘルパ

    /// DSL 評価結果を模した resolved tree を作るヘルパ。
    private func makeTree(
        sections: [KsSettingsViewCore.Section],
        rootHeader: RootAccessory? = nil,
        rootFooter: RootAccessory? = nil,
        theme: Theme = Theme()
    ) -> DSLDiffCalculator.ResolvedTree {
        return DSLDiffCalculator.ResolvedTree(
            sections: sections,
            rootHeader: rootHeader,
            rootFooter: rootFooter,
            theme: theme
        )
    }

    private func sec(
        id: UUID = UUID(),
        header: SectionAccessory? = nil,
        cells: [any KsCell] = [],
        headerHeight: Double = -1
    ) -> KsSettingsViewCore.Section {
        return KsSettingsViewCore.Section(id: id, header: header, cells: cells, headerHeight: headerHeight)
    }

    // MARK: - Tests

    func test_完全一致なら空のDiffが返る() {
        let s1 = sec(header: .text("A"), cells: [DummyTestCell(title: "X")])
        let old = makeTree(sections: [s1])
        let new = makeTree(sections: [s1])
        XCTAssertEqual(DSLDiffCalculator.compute(from: old, to: new), [])
    }

    func test_Cell追加でinsertCellのみが発行される() {
        let sectionID = UUID()
        let cellAID = UUID()
        let cellA = DummyTestCell(id: cellAID, title: "A")
        let cellB = DummyTestCell(title: "B")

        let old = makeTree(sections: [sec(id: sectionID, cells: [cellA])])
        let new = makeTree(sections: [sec(id: sectionID, cells: [cellA, cellB])])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        if case let .insertCell(sid, index, _) = diffs[0] {
            XCTAssertEqual(sid, sectionID)
            XCTAssertEqual(index, 1)
        } else {
            XCTFail("Expected .insertCell, got \(diffs[0])")
        }
    }

    func test_Cell削除でremoveCellのみが発行される() {
        let sectionID = UUID()
        let cellA = DummyTestCell(title: "A")
        let cellB = DummyTestCell(title: "B")

        let old = makeTree(sections: [sec(id: sectionID, cells: [cellA, cellB])])
        let new = makeTree(sections: [sec(id: sectionID, cells: [cellA])])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        if case let .removeCell(cid) = diffs[0] {
            XCTAssertEqual(cid.id, cellB.id)
        } else {
            XCTFail("Expected .removeCell, got \(diffs[0])")
        }
    }

    func test_Cell内容変更でreplaceCellが発行される() {
        let sectionID = UUID()
        let cellID = UUID()

        let old = makeTree(sections: [sec(id: sectionID, cells: [DummyTestCell(id: cellID, title: "Taro")])])
        let new = makeTree(sections: [sec(id: sectionID, cells: [DummyTestCell(id: cellID, title: "Hanako")])])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        if case let .replaceCell(cid, _) = diffs[0] {
            XCTAssertEqual(cid.id, cellID)
        } else {
            XCTFail("Expected .replaceCell, got \(diffs[0])")
        }
    }

    /// 「同一 id セルへの 2 回連続の内容更新」シナリオ。
    ///
    /// reconfigure は snapshot 識別子（KsCellID）を変えないため、内容変化のたびに発行される
    /// `.replaceCell` の `cellID` は **常に同一 id** である必要がある。これが内容に依存して
    /// 変わると、Controller の snapshot 識別子（id 限定）と照合できず 2 回目の更新が破綻する。
    ///
    /// 本テストは UIKit の実挙動ではなく Diff レイヤのロジックを対象とする。Controller 側の
    /// `reconfigureItems` の実行はシミュレータ実行のテストが受け持ち、その前提となる
    /// 「発行される cellID が id 限定で安定」を本テストが担保する。
    func test_同一idへの2回連続内容更新で常に同一cellIDのreplaceCellが発行される() {
        let sectionID = UUID()
        let cellID = UUID()

        let v0 = makeTree(sections: [sec(id: sectionID, cells: [DummyTestCell(id: cellID, title: "A")])])
        let v1 = makeTree(sections: [sec(id: sectionID, cells: [DummyTestCell(id: cellID, title: "B")])])
        let v2 = makeTree(sections: [sec(id: sectionID, cells: [DummyTestCell(id: cellID, title: "C")])])

        // 更新1: A -> B
        let diffs1 = DSLDiffCalculator.compute(from: v0, to: v1)
        XCTAssertEqual(diffs1.count, 1)
        guard case let .replaceCell(cid1, new1) = diffs1[0] else {
            return XCTFail("更新1: Expected .replaceCell, got \(diffs1[0])")
        }

        // 更新2: B -> C
        let diffs2 = DSLDiffCalculator.compute(from: v1, to: v2)
        XCTAssertEqual(diffs2.count, 1)
        guard case let .replaceCell(cid2, new2) = diffs2[0] else {
            return XCTFail("更新2: Expected .replaceCell, got \(diffs2[0])")
        }

        // 1 回目と 2 回目の cellID が **同一**（id 限定のため内容差で変わらない）。
        // これが従来 contentHash 込みだと cid1 != cid2 となり、reconfigure の照合（snapshot は
        // 1 回目時点の識別子を保持）が 2 回目で外れて破綻していた。
        XCTAssertEqual(cid1, cid2, "連続内容更新で発行される cellID は id 限定で常に同一でなければならない")
        XCTAssertEqual(cid1.id, cellID)
        XCTAssertEqual(cid2.id, cellID)

        // 内容（new ペイロード）はそれぞれ正しく更新後の Cell を運ぶ
        XCTAssertEqual((new1 as? DummyTestCell)?.title, "B")
        XCTAssertEqual((new2 as? DummyTestCell)?.title, "C")
    }

    func test_Section追加でinsertSectionが発行される() {
        let s1 = sec(cells: [DummyTestCell(title: "A")])
        let s2 = sec(cells: [DummyTestCell(title: "B")])

        let old = makeTree(sections: [s1])
        let new = makeTree(sections: [s1, s2])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        if case let .insertSection(at, section) = diffs[0] {
            XCTAssertEqual(at, 1)
            XCTAssertEqual(section.id, s2.id)
        } else {
            XCTFail("Expected .insertSection, got \(diffs[0])")
        }
    }

    func test_Section削除でremoveSectionが発行される() {
        let s1 = sec(cells: [DummyTestCell(title: "A")])
        let s2 = sec(cells: [DummyTestCell(title: "B")])

        let old = makeTree(sections: [s1, s2])
        let new = makeTree(sections: [s1])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        if case let .removeSection(sid) = diffs[0] {
            XCTAssertEqual(sid, s2.id)
        } else {
            XCTFail("Expected .removeSection, got \(diffs[0])")
        }
    }

    func test_Section_H_F_変更でupdateAccessoryが発行される() {
        let sectionID = UUID()
        let cell = DummyTestCell(title: "A")
        let old = makeTree(sections: [sec(id: sectionID, header: .text("旧"), cells: [cell])])
        let new = makeTree(sections: [sec(id: sectionID, header: .text("新"), cells: [cell])])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        if case let .updateAccessory(target, accessory) = diffs[0] {
            XCTAssertEqual(target, .sectionHeader(sectionID: sectionID))
            XCTAssertEqual(accessory, .section(.text("新")))
        } else {
            XCTFail("Expected .updateAccessory, got \(diffs[0])")
        }
    }

    func test_Root_Header_変更でupdateAccessoryが発行される() {
        let old = makeTree(sections: [], rootHeader: .text("旧"))
        let new = makeTree(sections: [], rootHeader: .text("新"))

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        if case let .updateAccessory(target, accessory) = diffs[0] {
            XCTAssertEqual(target, .rootHeader)
            XCTAssertEqual(accessory, .root(.text("新")))
        } else {
            XCTFail("Expected .updateAccessory, got \(diffs[0])")
        }
    }

    func test_Theme_変更はDiff列に含まれない() {
        // Theme は構造 Diff の対象外であり、Diff 列に載らない。反映は呼び出し側が
        // `Store.applyTheme(_:)` を別途呼ぶ責務とする（core/ADR-0009）。
        let oldTheme = Theme()
        let newTheme = Theme(scrollIndicatorVisible: false)

        let old = makeTree(sections: [], theme: oldTheme)
        let new = makeTree(sections: [], theme: newTheme)

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs, [], "Theme 変化は Diff 列に乗らない（Store.applyTheme 経由）")
    }

    func test_任意View同士のSection_H_F_は等価扱い_updateAccessory非発行() {
        let sectionID = UUID()
        let cell = DummyTestCell(title: "A")
        let header1 = SectionAccessory.view(KsAnyView.swiftUI { EmptyTestView() })
        let header2 = SectionAccessory.view(KsAnyView.swiftUI { EmptyTestView() })

        let old = makeTree(sections: [sec(id: sectionID, header: header1, cells: [cell])])
        let new = makeTree(sections: [sec(id: sectionID, header: header2, cells: [cell])])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs, []) // KsAnyView は差分検出に参加しない
    }

    func test_text_ケースから_view_ケースへの遷移はupdateAccessory発行() {
        let sectionID = UUID()
        let cell = DummyTestCell(title: "A")
        let header1 = SectionAccessory.text("旧")
        let header2 = SectionAccessory.view(KsAnyView.swiftUI { EmptyTestView() })

        let old = makeTree(sections: [sec(id: sectionID, header: header1, cells: [cell])])
        let new = makeTree(sections: [sec(id: sectionID, header: header2, cells: [cell])])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        if case let .updateAccessory(target, _) = diffs[0] {
            XCTAssertEqual(target, .sectionHeader(sectionID: sectionID))
        } else {
            XCTFail("Expected .updateAccessory, got \(diffs[0])")
        }
    }

    // MARK: - headerHeight 変化の preflight 検出

    /// `.full` に載る新ツリーの当該 Section の headerHeight を取り出す。
    private func fullSectionHeaderHeight(
        _ diff: SettingsRootDiff,
        sectionID: UUID
    ) -> Double? {
        guard case let .full(root) = diff else { return nil }
        return root.sections.first(where: { $0.id == sectionID })?.headerHeight
    }

    func test_headerHeightが正値間で変わるとfullが発行される() {
        let sectionID = UUID()
        let cell = DummyTestCell(title: "A")

        let old = makeTree(sections: [sec(id: sectionID, header: .text("H"), cells: [cell], headerHeight: 40)])
        let new = makeTree(sections: [sec(id: sectionID, header: .text("H"), cells: [cell], headerHeight: 80)])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1, "headerHeight のみの変更では .full 1 件だけが発行される")
        XCTAssertEqual(fullSectionHeaderHeight(diffs[0], sectionID: sectionID), 80,
                       "Expected .full with headerHeight 80, got \(diffs[0])")
    }

    func test_headerHeightが自動から固定へ変わるとfullが発行される() {
        let sectionID = UUID()
        let cell = DummyTestCell(title: "A")

        let old = makeTree(sections: [sec(id: sectionID, header: .text("H"), cells: [cell], headerHeight: -1)])
        let new = makeTree(sections: [sec(id: sectionID, header: .text("H"), cells: [cell], headerHeight: 64)])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        XCTAssertEqual(fullSectionHeaderHeight(diffs[0], sectionID: sectionID), 64,
                       "Expected .full with headerHeight 64, got \(diffs[0])")
    }

    func test_headerHeightが固定から自動へ変わるとfullが発行される() {
        let sectionID = UUID()
        let cell = DummyTestCell(title: "A")

        let old = makeTree(sections: [sec(id: sectionID, header: .text("H"), cells: [cell], headerHeight: 64)])
        let new = makeTree(sections: [sec(id: sectionID, header: .text("H"), cells: [cell], headerHeight: -1)])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        XCTAssertEqual(fullSectionHeaderHeight(diffs[0], sectionID: sectionID), -1,
                       "Expected .full with headerHeight -1, got \(diffs[0])")
    }

    /// headerHeight と Cell 内容が同時に変わっても発行は `.full` 1 件だけになる。
    /// Cell の内容変化は `.full` の適用が内包する内容再適用で表示へ届くため、`.replaceCell` を
    /// 続けて発行すると同一 Cell への内容再適用が二重に走る。
    func test_headerHeightとCell内容の同時変更でもfullのみが発行される() {
        let sectionID = UUID()
        let cellID = UUID()

        let old = makeTree(sections: [sec(
            id: sectionID,
            header: .text("H"),
            cells: [DummyTestCell(id: cellID, title: "Taro")],
            headerHeight: 40
        )])
        let new = makeTree(sections: [sec(
            id: sectionID,
            header: .text("H"),
            cells: [DummyTestCell(id: cellID, title: "Hanako")],
            headerHeight: 80
        )])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1, "期待は .full 1 件のみ、got \(diffs)")
        XCTAssertEqual(fullSectionHeaderHeight(diffs[0], sectionID: sectionID), 80,
                       "新しい headerHeight を載せた .full、got \(diffs[0])")
        // `.full` が運ぶ新ツリーに Cell の新しい内容が載っていることを確かめる。
        guard case let .full(root) = diffs[0] else {
            return XCTFail("Expected .full, got \(diffs[0])")
        }
        let carriedCell = root.sections
            .first(where: { $0.id == sectionID })?
            .cells.first(where: { $0.id == cellID })
        XCTAssertEqual((carriedCell as? DummyTestCell)?.title, "Hanako",
                       ".full が運ぶツリーに Cell の新しい内容が載っていない")
    }

    func test_headerHeight不変で内容のみ変わるとfullは発行されずreplaceCellが発行される() {
        let sectionID = UUID()
        let cellID = UUID()

        let old = makeTree(sections: [sec(
            id: sectionID,
            header: .text("H"),
            cells: [DummyTestCell(id: cellID, title: "Taro")],
            headerHeight: 40
        )])
        let new = makeTree(sections: [sec(
            id: sectionID,
            header: .text("H"),
            cells: [DummyTestCell(id: cellID, title: "Hanako")],
            headerHeight: 40
        )])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertFalse(diffs.contains(where: {
            if case .full = $0 { return true } else { return false }
        }), "headerHeight 不変の内容更新で .full が発行されている")
        XCTAssertEqual(diffs.count, 1)
        guard case let .replaceCell(cid, newCell) = diffs[0] else {
            return XCTFail("Expected .replaceCell, got \(diffs[0])")
        }
        XCTAssertEqual(cid.id, cellID)
        XCTAssertEqual((newCell as? DummyTestCell)?.title, "Hanako")
    }

    // MARK: - 可視性変化と headerHeight 変化の併発

    /// 可視性変化と headerHeight 変化が同じ再評価で重なっても、発行は `.full` 1 件だけになる。
    /// 高さ・可視性・Cell 内容のいずれも `.full` の適用で表示へ届く。
    func test_別Sectionの可視性変更とheaderHeight変更の併発でもfullのみが発行される() {
        let sectionAID = UUID()
        let sectionBID = UUID()
        let cellID = UUID()

        let old = makeTree(sections: [
            sec(id: sectionAID, header: .text("A"), cells: [DummyTestCell(id: cellID, title: "Taro")], headerHeight: 40),
            KsSettingsViewCore.Section(id: sectionBID, header: .text("B"), cells: [], isVisible: true),
        ])
        let new = makeTree(sections: [
            sec(id: sectionAID, header: .text("A"), cells: [DummyTestCell(id: cellID, title: "Hanako")], headerHeight: 80),
            KsSettingsViewCore.Section(id: sectionBID, header: .text("B"), cells: [], isVisible: false),
        ])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1, "期待は .full 1 件のみ、got \(diffs)")
        XCTAssertEqual(fullSectionHeaderHeight(diffs[0], sectionID: sectionAID), 80,
                       "新しい headerHeight を載せた .full、got \(diffs[0])")
        guard case let .full(root) = diffs[0] else {
            return XCTFail("Expected .full, got \(diffs[0])")
        }
        let carriedCell = root.sections
            .first(where: { $0.id == sectionAID })?
            .cells.first(where: { $0.id == cellID })
        XCTAssertEqual((carriedCell as? DummyTestCell)?.title, "Hanako",
                       ".full が運ぶツリーに Cell の新しい内容が載っていない")
        XCTAssertEqual(root.sections.first(where: { $0.id == sectionBID })?.isVisible, false,
                       ".full が運ぶツリーに可視性の変化が載っていない")
    }

    /// headerHeight が不変なら、可視性変化は従来どおり `.full` のみで反映する (退行防止)。
    func test_可視性のみの変更ではfullのみが発行される() {
        let sectionAID = UUID()
        let sectionBID = UUID()
        let cellID = UUID()

        let old = makeTree(sections: [
            sec(id: sectionAID, header: .text("A"), cells: [DummyTestCell(id: cellID, title: "Taro")], headerHeight: 40),
            KsSettingsViewCore.Section(id: sectionBID, header: .text("B"), cells: [], isVisible: true),
        ])
        let new = makeTree(sections: [
            sec(id: sectionAID, header: .text("A"), cells: [DummyTestCell(id: cellID, title: "Hanako")], headerHeight: 40),
            KsSettingsViewCore.Section(id: sectionBID, header: .text("B"), cells: [], isVisible: false),
        ])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1, "可視性のみの変化では .full 1 件だけが発行される")
        guard case .full = diffs[0] else {
            return XCTFail("Expected .full, got \(diffs[0])")
        }
    }

    /// headerHeight 変化の preflight は、非表示 Cell / 非表示 Section が混在していても
    /// `.full` 1 件だけを発行する。内容の反映は `.full` の適用側が担うため、Cell 単位の
    /// `.replaceCell` を重ねて発行しない。
    func test_headerHeight変更時は非表示Cellが混在してもfullのみが発行される() {
        let sectionAID = UUID()
        let hiddenSectionID = UUID()
        let visibleCellID = UUID()
        let hiddenCellID = UUID()
        let cellInHiddenSectionID = UUID()

        let old = makeTree(sections: [
            KsSettingsViewCore.Section(
                id: sectionAID,
                header: .text("A"),
                cells: [
                    LabelCell(id: visibleCellID, title: "見える旧"),
                    LabelCell(id: hiddenCellID, title: "隠れる旧", isVisible: false),
                ],
                headerHeight: 40
            ),
            KsSettingsViewCore.Section(
                id: hiddenSectionID,
                header: .text("H"),
                cells: [LabelCell(id: cellInHiddenSectionID, title: "非表示 Section 内旧")],
                isVisible: false
            ),
        ])
        let new = makeTree(sections: [
            KsSettingsViewCore.Section(
                id: sectionAID,
                header: .text("A"),
                cells: [
                    LabelCell(id: visibleCellID, title: "見える新"),
                    LabelCell(id: hiddenCellID, title: "隠れる新", isVisible: false),
                ],
                headerHeight: 80
            ),
            KsSettingsViewCore.Section(
                id: hiddenSectionID,
                header: .text("H"),
                cells: [LabelCell(id: cellInHiddenSectionID, title: "非表示 Section 内新")],
                isVisible: false
            ),
        ])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1, "期待は .full 1 件のみ、got \(diffs)")
        guard case let .full(root) = diffs[0] else {
            return XCTFail("Expected .full, got \(diffs[0])")
        }

        // 可視 Cell も非表示 Cell も、新しい内容を載せたまま `.full` のツリーで運ばれる。
        let carriedTitles: [UUID: String] = Dictionary(
            uniqueKeysWithValues: root.sections.flatMap { section in
                section.cells.compactMap { cell -> (UUID, String)? in
                    guard let label = cell as? LabelCell else { return nil }
                    return (label.id, label.title)
                }
            }
        )
        XCTAssertEqual(carriedTitles[visibleCellID], "見える新")
        XCTAssertEqual(carriedTitles[hiddenCellID], "隠れる新")
        XCTAssertEqual(carriedTitles[cellInHiddenSectionID], "非表示 Section 内新")
    }

    func test_Cell移動でmoveCellが発行される() {
        let sectionID = UUID()
        let a = DummyTestCell(title: "A")
        let b = DummyTestCell(title: "B")
        let c = DummyTestCell(title: "C")

        let old = makeTree(sections: [sec(id: sectionID, cells: [a, b, c])])
        let new = makeTree(sections: [sec(id: sectionID, cells: [a, c, b])])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        // B が位置 1 → 2、または C が位置 2 → 1 の move のいずれかが発行される
        XCTAssertTrue(diffs.contains(where: {
            if case .moveCell = $0 { return true } else { return false }
        }))
    }
}

struct EmptyTestView: SwiftUI.View {
    var body: some SwiftUI.View { SwiftUI.Text("") }
}
#endif
