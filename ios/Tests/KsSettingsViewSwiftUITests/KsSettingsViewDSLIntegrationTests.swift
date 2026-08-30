// KsSettingsViewDSLIntegrationTests.swift
// KsSettingsViewSwiftUITests
//
// `KsSettingsView { ... }` DSL を **2 回評価** したときに、
// Section / Cell の安定 ID が body 再評価をまたいで一致することを検証する Integration テスト。
//
// DSL から解決 `[Section]` までの安定 ID パイプラインは、個々の部品のユニットテストが
// 緑でも経路として繋がっていないことがあり得るため、DSL 評価から Diff 算出までを
// 通しで検証する（core/ADR-0008）。
//
// 検証範囲:
//   - 静的 DSL の 2 回評価で空 Diff（同一ツリー）
//   - Cell 内容変更で対象 Cell の `replaceCell` のみ発行
//   - `ForEach(items)` で items に append → 既存 Cell ID 不変・新規のみ insertCell
//   - `.sectionID(_:)` で動的追加の Section ID 安定化
//   - `.cellID(_:)` で動的追加の Cell ID 安定化
//   - `.rootHeader(_:)` 変更 → Root Header の `updateAccessory`
//
// アプローチ:
//   `DSLBackedRepresentable` 内部を直接呼ぶのは難しいため、
//   `KsSettingsViewBuilder` ベースの DSL を 2 回評価して `DSLRootTree.resolvedSections()`
//   を経由した resolved `[Section]` の ID 安定性を検証する。
//   さらに `DSLDiffCalculator.compute(from:to:)` に渡して Diff が期待通りであることも確認する。

import XCTest
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewCore

#if canImport(UIKit)
@testable import KsSettingsViewUI

final class KsSettingsViewDSLIntegrationTests: XCTestCase {

    // MARK: - Helpers

    /// 与えられた DSL ビルダーを評価し、`DSLRootTree.resolvedSections()` で安定 ID 解決済みの
    /// `[KsSettingsViewCore.Section]` を返す。
    private func evaluate(
        @KsSettingsViewBuilder _ builder: () -> [DSLSectionNode]
    ) -> [KsSettingsViewCore.Section] {
        DSLHintRegistry.shared.reset()
        let nodes = builder()
        let tree = DSLRootTree(sectionNodes: nodes)
        return tree.resolvedSections()
    }

    /// resolved tree を構築するヘルパ。
    private func makeResolvedTree(
        sections: [KsSettingsViewCore.Section],
        rootHeader: RootAccessory? = nil
    ) -> DSLDiffCalculator.ResolvedTree {
        return DSLDiffCalculator.ResolvedTree(
            sections: sections,
            rootHeader: rootHeader,
            rootFooter: nil,
            theme: Theme()
        )
    }

    // MARK: - 静的構造の body 再評価耐性

    func test_静的DSL_2回評価で同じSectionIDとCellIDが返る() {
        let first = evaluate {
            Section("一般") {
                DummyTestCell(title: "A")
                DummyTestCell(title: "B")
            }
            Section("高度") {
                DummyTestCell(title: "C")
            }
        }
        let second = evaluate {
            Section("一般") {
                DummyTestCell(title: "A")
                DummyTestCell(title: "B")
            }
            Section("高度") {
                DummyTestCell(title: "C")
            }
        }

        XCTAssertEqual(first.count, 2)
        XCTAssertEqual(second.count, 2)
        XCTAssertEqual(first.map { $0.id }, second.map { $0.id },
                       "ヘッダ文字列ベースで Section ID が安定すること")

        for (s1, s2) in zip(first, second) {
            XCTAssertEqual(s1.cells.map { $0.id }, s2.cells.map { $0.id },
                           "(SectionID, 位置, Cell 型) ベースで Cell ID が安定すること")
        }
    }

    func test_静的DSL_2回評価でDiffが空になる() {
        let first = evaluate {
            Section("一般") {
                DummyTestCell(title: "A")
                DummyTestCell(title: "B")
            }
        }
        let second = evaluate {
            Section("一般") {
                DummyTestCell(title: "A")
                DummyTestCell(title: "B")
            }
        }
        let diffs = DSLDiffCalculator.compute(
            from: makeResolvedTree(sections: first),
            to: makeResolvedTree(sections: second)
        )
        XCTAssertEqual(diffs, [], "同一 DSL の再評価では Diff が空になるべき")
    }

    // MARK: - Cell 内容変更で replaceCell のみ

    func test_Cell内容変更で該当CellのreplaceCellのみ発行される() {
        // 1 回目：title=旧
        let first = evaluate {
            Section("一般") {
                DummyTestCell(title: "旧")
                DummyTestCell(title: "B")
            }
        }
        // 2 回目：1 番目の Cell のみ title=新 に変更
        let second = evaluate {
            Section("一般") {
                DummyTestCell(title: "新")
                DummyTestCell(title: "B")
            }
        }
        // Cell ID は (SectionID, 位置, 型) ベースで採番されるため、位置と型が同じなら ID 一致
        XCTAssertEqual(first[0].cells[0].id, second[0].cells[0].id)
        XCTAssertEqual(first[0].cells[1].id, second[0].cells[1].id)

        let diffs = DSLDiffCalculator.compute(
            from: makeResolvedTree(sections: first),
            to: makeResolvedTree(sections: second)
        )
        XCTAssertEqual(diffs.count, 1, "差分は 1 件のみ（Cell[0] の replaceCell）")
        if case let .replaceCell(cellID, new) = diffs[0] {
            XCTAssertEqual(cellID.id, first[0].cells[0].id)
            // 内容が新しい Cell に更新されている
            if let dummy = new as? DummyTestCell {
                XCTAssertEqual(dummy.title, "新")
            } else {
                XCTFail("new cell should be DummyTestCell, got \(type(of: new))")
            }
        } else {
            XCTFail("Expected .replaceCell, got \(diffs[0])")
        }
    }

    // MARK: - ForEach 配下の動的追加

    func test_ForEach_items追加で既存Cell_ID不変_新規のみinsertCellが発行される() {
        struct Item: Identifiable, Equatable {
            let id: Int
            let name: String
        }
        let items1 = [Item(id: 1, name: "A"), Item(id: 2, name: "B")]
        let items2 = items1 + [Item(id: 3, name: "C")]

        let first = evaluate {
            Section("Items") {
                ForEach(items1) { item in
                    DummyTestCell(title: item.name)
                }
            }
        }
        let second = evaluate {
            Section("Items") {
                ForEach(items2) { item in
                    DummyTestCell(title: item.name)
                }
            }
        }

        // 既存 Cell (id=1, id=2) の Cell.id が両者で一致すること
        XCTAssertEqual(first[0].cells.count, 2)
        XCTAssertEqual(second[0].cells.count, 3)
        XCTAssertEqual(first[0].cells[0].id, second[0].cells[0].id,
                       "ForEach item.id=1 の Cell ID が再評価をまたいで一致すること")
        XCTAssertEqual(first[0].cells[1].id, second[0].cells[1].id,
                       "ForEach item.id=2 の Cell ID が再評価をまたいで一致すること")

        let diffs = DSLDiffCalculator.compute(
            from: makeResolvedTree(sections: first),
            to: makeResolvedTree(sections: second)
        )
        XCTAssertEqual(diffs.count, 1, "差分は新規追加 1 件のみ")
        if case let .insertCell(sectionID, index, _) = diffs[0] {
            XCTAssertEqual(sectionID, first[0].id)
            XCTAssertEqual(index, 2)
        } else {
            XCTFail("Expected .insertCell, got \(diffs[0])")
        }
    }

    // MARK: - .sectionID(_:) で動的追加の Section ID 安定化

    func test_sectionID_modifier指定で動的追加時にSection_IDが安定する() {
        // 1 回目：Section A のみ
        let first = evaluate {
            Section { DummyTestCell(title: "X") }
                .sectionID("section-a")
        }
        // 2 回目：Section A + Section B（先頭に新規追加）
        let second = evaluate {
            Section { DummyTestCell(title: "Y") }
                .sectionID("section-b")
            Section { DummyTestCell(title: "X") }
                .sectionID("section-a")
        }

        XCTAssertEqual(first.count, 1)
        XCTAssertEqual(second.count, 2)
        // first の section-a と second の section-a が同じ Section.id を持つ
        XCTAssertEqual(first[0].id, second[1].id,
                       ".sectionID(\"section-a\") は ForEach なしでも安定 ID を提供する")

        let diffs = DSLDiffCalculator.compute(
            from: makeResolvedTree(sections: first),
            to: makeResolvedTree(sections: second)
        )
        // section-a は維持、section-b が insert、section-a が位置 0→1 に move
        XCTAssertTrue(diffs.contains(where: {
            if case let .insertSection(_, section) = $0 { return section.id == second[0].id }
            return false
        }), "section-b の insertSection が発行されるべき")
    }

    // MARK: - .cellID(_:) で動的追加の Cell ID 安定化

    func test_cellID_modifier指定で動的追加時にCell_IDが安定する() {
        let first = evaluate {
            Section("S") {
                DummyTestCell(title: "X").cellID("cell-x")
            }
        }
        let second = evaluate {
            Section("S") {
                DummyTestCell(title: "Y").cellID("cell-y")
                DummyTestCell(title: "X").cellID("cell-x")
            }
        }
        XCTAssertEqual(first[0].cells[0].id, second[0].cells[1].id,
                       ".cellID(\"cell-x\") は位置移動後も安定 Cell ID を提供する")
    }

    // MARK: - Root Header 変更で updateAccessory

    func test_rootHeader_変更でupdateAccessoryが発行される() {
        let first = evaluate {
            Section { DummyTestCell(title: "X") }
        }
        let second = evaluate {
            Section { DummyTestCell(title: "X") }
        }
        let diffs = DSLDiffCalculator.compute(
            from: makeResolvedTree(sections: first, rootHeader: .text("旧")),
            to: makeResolvedTree(sections: second, rootHeader: .text("新"))
        )
        XCTAssertEqual(diffs.count, 1)
        if case let .updateAccessory(target, accessory) = diffs[0] {
            XCTAssertEqual(target, .rootHeader)
            XCTAssertEqual(accessory, .root(.text("新")))
        } else {
            XCTFail("Expected .updateAccessory(rootHeader), got \(diffs[0])")
        }
    }

    // MARK: - Cell modifier 適用でも Cell ID が維持される

    func test_Cell_modifier適用でもCell_IDが維持される() {
        // 1 回目：modifier なし
        let first = evaluate {
            Section("S") {
                DummyTestCell(title: "A")
            }
        }
        // 2 回目：.cellHeight(80) を追加適用（内容が変わるので replaceCell されるが、ID は不変）
        let second = evaluate {
            Section("S") {
                DummyTestCell(title: "A").cellHeight(80)
            }
        }
        XCTAssertEqual(first[0].cells[0].id, second[0].cells[0].id,
                       "Cell modifier 適用後も Cell ID は維持される")

        let diffs = DSLDiffCalculator.compute(
            from: makeResolvedTree(sections: first),
            to: makeResolvedTree(sections: second)
        )
        // style 変更が AnyHashable 比較で検出され、replaceCell が発行される
        XCTAssertTrue(diffs.contains(where: {
            if case .replaceCell = $0 { return true }
            return false
        }), "style 変更で replaceCell が発行されること")
    }
}
#endif
