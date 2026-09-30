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

    /// 外観に応じて `CellStyle` の色だけを差し替えた再評価は、行の追加・削除ではなく
    /// 内容更新（`.replaceCell`）として発行され、新しい色を payload で運ぶ。
    ///
    /// SwiftUI DSL で `colorScheme` に応じて色を選ぶ利用者は、外観が変わると同じ id・同じ title の
    /// まま style だけが違う Cell を返す。その差が内容更新にならないと、表示中の行に届かない。
    func test_CellStyleの色だけの変更でreplaceCellが発行される() {
        let sectionID = UUID()
        let cellID = UUID()
        let lightColor = UIColor(red: 0.9, green: 0.8, blue: 0.7, alpha: 1.0)
        let darkColor = UIColor(red: 0.2, green: 0.4, blue: 0.6, alpha: 1.0)

        let old = makeTree(sections: [sec(
            id: sectionID,
            cells: [LabelCell(id: cellID, style: CellStyle(titleColor: lightColor), title: "A")]
        )])
        let new = makeTree(sections: [sec(
            id: sectionID,
            cells: [LabelCell(id: cellID, style: CellStyle(titleColor: darkColor), title: "A")]
        )])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        guard case let .replaceCell(cid, payload) = diffs[0] else {
            return XCTFail("Expected .replaceCell, got \(diffs[0])")
        }
        XCTAssertEqual(cid.id, cellID, "内容更新の対象は同じ id の行である")
        XCTAssertEqual(
            (payload as? LabelCell)?.style.titleColor, darkColor,
            "payload は切替後の title 色を運ぶ"
        )
    }

    /// 色が同じままの再評価は差分を生まない（内容更新が過剰に発行されない）。
    func test_CellStyleの色が同じなら差分は発行されない() {
        let sectionID = UUID()
        let cellID = UUID()
        let color = UIColor(red: 0.9, green: 0.8, blue: 0.7, alpha: 1.0)

        let old = makeTree(sections: [sec(
            id: sectionID,
            cells: [LabelCell(id: cellID, style: CellStyle(titleColor: color), title: "A")]
        )])
        let new = makeTree(sections: [sec(
            id: sectionID,
            cells: [LabelCell(id: cellID, style: CellStyle(titleColor: color), title: "A")]
        )])

        XCTAssertEqual(DSLDiffCalculator.compute(from: old, to: new), [])
    }

    // MARK: - 移動の最小化（追加・削除でずれただけの項目には移動を出さない）

    /// 名前ごとに固定の UUID を割り当てる（同じ名前は同じ ID になる）。
    private final class IDBook {
        private var ids: [String: UUID] = [:]
        private var names: [UUID: String] = [:]
        func id(_ name: String) -> UUID {
            if let id = ids[name] { return id }
            let id = UUID()
            ids[name] = id
            names[id] = name
            return id
        }
        func name(_ id: UUID) -> String { names[id] ?? "?" }
    }

    private func singleSection(_ names: [String], book: IDBook, sectionName: String = "s1")
        -> DSLDiffCalculator.ResolvedTree {
        makeTree(sections: [sec(
            id: book.id(sectionName),
            cells: names.map { DummyTestCell(id: book.id($0), title: $0) }
        )])
    }

    /// 差分列を実際の Store に順に適用し、適用後の Section / Cell の並びを名前で返す。
    ///
    /// Store の各操作が差分をどう解釈するか（移動は取り除いた後の位置へ挿入）まで含めて、
    /// 宣言どおりの並びに着地するかを確かめるために使う。
    @MainActor
    private func applyToStore(
        from tree: DSLDiffCalculator.ResolvedTree,
        diffs: [SettingsRootDiff],
        book: IDBook
    ) -> [String] {
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: tree.sections))
        for diff in diffs {
            switch diff {
            case let .full(root): store.replaceAll(root)
            case let .insertSection(at: index, section: section): store.insertSection(section, at: index)
            case let .removeSection(sectionID: id): store.removeSection(sectionID: id)
            case let .moveSection(from: from, to: to): store.moveSection(from: from, to: to)
            case let .replaceSection(sectionID: id, new: section): store.replaceSection(sectionID: id, new: section)
            case let .insertCell(sectionID: sid, at: index, cell: cell): store.insertCell(cell, in: sid, at: index)
            case let .removeCell(cellID: cid): store.removeCell(cellID: cid)
            case let .replaceCell(cellID: cid, new: cell): store.replaceCell(cellID: cid, new: cell)
            case let .moveCell(cellID: cid, to: index): store.moveCell(cellID: cid, to: index)
            case let .updateAccessory(target: target, accessory: accessory):
                store.updateAccessory(target: target, accessory: accessory)
            }
        }
        return Self.layout(of: store.root.sections, book: book)
    }

    private static func layout(of sections: [KsSettingsViewCore.Section], book: IDBook) -> [String] {
        sections.map { section in
            book.name(section.id) + ":" + section.cells.map { book.name($0.id) }.joined(separator: ",")
        }
    }

    private func moveCells(_ diffs: [SettingsRootDiff]) -> [UUID] {
        diffs.compactMap { if case let .moveCell(cellID: cid, to: _) = $0 { return cid.id } else { return nil } }
    }

    private func moveSectionCount(_ diffs: [SettingsRootDiff]) -> Int {
        diffs.filter { if case .moveSection = $0 { return true } else { return false } }.count
    }

    @MainActor
    func test_途中への1項目の挿入ではinsertCellだけを出し後ろの項目へ移動を出さない() {
        let book = IDBook()
        let before = (0..<30).map { "item-\($0)" }
        var after = before
        after.insert("inserted", at: 12)
        let old = singleSection(before, book: book)
        let new = singleSection(after, book: book)

        let diffs = DSLDiffCalculator.compute(from: old, to: new)

        XCTAssertEqual(diffs.count, 1)
        guard case let .insertCell(_, index, cell) = diffs[0] else {
            return XCTFail("Expected .insertCell, got \(diffs[0])")
        }
        XCTAssertEqual(index, 12)
        XCTAssertEqual(cell.id, book.id("inserted"))
        XCTAssertEqual(applyToStore(from: old, diffs: diffs, book: book), Self.layout(of: new.sections, book: book))
    }

    @MainActor
    func test_途中の1項目の削除ではremoveCellだけを出し後ろの項目へ移動を出さない() {
        let book = IDBook()
        let before = (0..<30).map { "item-\($0)" }
        let after = before.filter { $0 != "item-5" }
        let old = singleSection(before, book: book)
        let new = singleSection(after, book: book)

        let diffs = DSLDiffCalculator.compute(from: old, to: new)

        XCTAssertEqual(diffs, [.removeCell(cellID: KsCellID(id: book.id("item-5")))])
        XCTAssertEqual(applyToStore(from: old, diffs: diffs, book: book), Self.layout(of: new.sections, book: book))
    }

    @MainActor
    func test_先頭の項目を末尾へ送る並べ替えでは移動を1件だけ出す() {
        let book = IDBook()
        let old = singleSection(["a", "b", "c", "d", "e"], book: book)
        let new = singleSection(["b", "c", "d", "e", "a"], book: book)

        let diffs = DSLDiffCalculator.compute(from: old, to: new)

        XCTAssertEqual(diffs, [.moveCell(cellID: KsCellID(id: book.id("a")), to: 4)])
        XCTAssertEqual(applyToStore(from: old, diffs: diffs, book: book), Self.layout(of: new.sections, book: book))
    }

    @MainActor
    func test_逆順への並べ替えでは必要な移動を出し宣言の並びに着地する() {
        let book = IDBook()
        let old = singleSection(["a", "b", "c", "d"], book: book)
        let new = singleSection(["d", "c", "b", "a"], book: book)

        let diffs = DSLDiffCalculator.compute(from: old, to: new)

        // 相対順序を保てるのは 1 項目だけなので、残りの 3 項目に移動が要る
        XCTAssertEqual(moveCells(diffs).count, 3)
        XCTAssertEqual(applyToStore(from: old, diffs: diffs, book: book), Self.layout(of: new.sections, book: book))
    }

    @MainActor
    func test_挿入と削除と並べ替えの組み合わせでも宣言の並びに着地する() {
        let book = IDBook()
        let old = singleSection(["a", "b", "c", "d", "e", "f", "g"], book: book)
        // b を削除、x と y を挿入、f を先頭側へ、a を後ろへ
        let new = singleSection(["x", "f", "c", "d", "a", "y", "e", "g"], book: book)

        let diffs = DSLDiffCalculator.compute(from: old, to: new)

        // 相対順序が変わったのは f と a の 2 項目だけ（c・d・e・g は並びを保つ）
        XCTAssertEqual(Set(moveCells(diffs).map { book.name($0) }), ["f", "a"])
        XCTAssertEqual(applyToStore(from: old, diffs: diffs, book: book), Self.layout(of: new.sections, book: book))
    }

    @MainActor
    func test_並べ替えと内容変更の組み合わせでは移動の後に内容更新が届き宣言に着地する() {
        let book = IDBook()
        let sid = book.id("s1")
        let old = makeTree(sections: [sec(id: sid, cells: ["a", "b", "c"].map {
            DummyTestCell(id: book.id($0), title: $0)
        })])
        let new = makeTree(sections: [sec(id: sid, cells: [
            DummyTestCell(id: book.id("c"), title: "c2"),
            DummyTestCell(id: book.id("a"), title: "a"),
            DummyTestCell(id: book.id("b"), title: "b"),
        ])])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)

        XCTAssertEqual(diffs.count, 2)
        XCTAssertEqual(moveCells(diffs), [book.id("c")])
        guard case let .replaceCell(cid, payload) = diffs[1] else {
            return XCTFail("Expected .replaceCell after move, got \(diffs[1])")
        }
        XCTAssertEqual(cid.id, book.id("c"))
        XCTAssertEqual((payload as? DummyTestCell)?.title, "c2")
        XCTAssertEqual(applyToStore(from: old, diffs: diffs, book: book), Self.layout(of: new.sections, book: book))
    }

    @MainActor
    func test_並べ替えの組み合わせを網羅しても常に宣言の並びに着地し移動は最小になる() {
        // 5 項目の全順列 × 追加・削除の有無で、Store に適用した結果が宣言と一致し、
        // 移動の件数が「項目数 - 相対順序を保てる最大の項目数」に等しいことを確かめる。
        let base = ["a", "b", "c", "d", "e"]
        for perm in Self.permutations(base) {
            for variant in 0..<3 {
                let book = IDBook()
                var target = perm
                switch variant {
                case 1: target.insert("new", at: 2)
                case 2: target.removeAll { $0 == "c" }
                default: break
                }
                let old = singleSection(base, book: book)
                let new = singleSection(target, book: book)
                let diffs = DSLDiffCalculator.compute(from: old, to: new)
                XCTAssertEqual(
                    applyToStore(from: old, diffs: diffs, book: book),
                    Self.layout(of: new.sections, book: book),
                    "perm=\(perm) variant=\(variant)"
                )
                let kept = target.filter { base.contains($0) }
                let keptOld = base.filter { kept.contains($0) }
                let expectedMoves = kept.count - Self.lisLength(kept.map { keptOld.firstIndex(of: $0) ?? 0 })
                XCTAssertEqual(moveCells(diffs).count, expectedMoves, "perm=\(perm) variant=\(variant)")
            }
        }
    }

    @MainActor
    func test_Sectionの途中への挿入では後ろのSectionへ移動を出さず宣言の並びに着地する() {
        let book = IDBook()
        let sections = (0..<5).map { sec(id: book.id("s\($0)"), cells: [DummyTestCell(id: book.id("c\($0)"), title: "c")]) }
        let inserted = sec(id: book.id("new"), cells: [DummyTestCell(id: book.id("cn"), title: "n")])
        let old = makeTree(sections: sections)
        let new = makeTree(sections: [inserted] + sections)

        let diffs = DSLDiffCalculator.compute(from: old, to: new)

        XCTAssertEqual(diffs.count, 1)
        guard case let .insertSection(at: index, section: _) = diffs[0] else {
            return XCTFail("Expected .insertSection, got \(diffs[0])")
        }
        XCTAssertEqual(index, 0)
        XCTAssertEqual(applyToStore(from: old, diffs: diffs, book: book), Self.layout(of: new.sections, book: book))
    }

    @MainActor
    func test_Sectionの挿入と並べ替えの組み合わせでも宣言の並びに着地する() {
        let book = IDBook()
        func s(_ name: String) -> KsSettingsViewCore.Section {
            sec(id: book.id(name), cells: [DummyTestCell(id: book.id("c-" + name), title: name)])
        }
        let old = makeTree(sections: ["s0", "s1", "s2", "s3", "s4"].map(s))
        let new = makeTree(sections: ["s4", "s0", "new", "s2", "s1"].map(s))

        let diffs = DSLDiffCalculator.compute(from: old, to: new)

        // s3 の削除、new の挿入に加え、相対順序が変わった 2 つの Section だけを移す
        XCTAssertEqual(moveSectionCount(diffs), 2)
        XCTAssertEqual(applyToStore(from: old, diffs: diffs, book: book), Self.layout(of: new.sections, book: book))
    }

    private static func permutations(_ items: [String]) -> [[String]] {
        guard items.count > 1 else { return [items] }
        return items.flatMap { head in
            permutations(items.filter { $0 != head }).map { [head] + $0 }
        }
    }

    /// 狭義の最長増加部分列の長さ（期待値の算出用。O(n^2) の素朴な実装）。
    private static func lisLength(_ values: [Int]) -> Int {
        guard !values.isEmpty else { return 0 }
        var best = [Int](repeating: 1, count: values.count)
        for i in values.indices {
            for j in 0..<i where values[j] < values[i] {
                best[i] = max(best[i], best[j] + 1)
            }
        }
        return best.max() ?? 0
    }
}

struct EmptyTestView: SwiftUI.View {
    var body: some SwiftUI.View { SwiftUI.Text("") }
}
#endif
