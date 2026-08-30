// CustomCellDSLTests.swift
// KsSettingsViewSwiftUITests
//
// `CustomCell` の iOS DSL 経路（SectionBuilder への直書き・modifier チェーン・
// 安定 ID 採番・同値 content の no-rebind）を検証する。

import XCTest
import SwiftUI
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewCore

#if canImport(UIKit)
import UIKit
@testable import KsSettingsViewUI

final class CustomCellDSLTests: XCTestCase {

    // MARK: - ヘルパ

    private func evaluate(
        @KsSettingsViewBuilder _ builder: () -> [DSLSectionNode]
    ) -> [KsSettingsViewCore.Section] {
        DSLHintRegistry.shared.reset()
        let nodes = builder()
        let tree = DSLRootTree(sectionNodes: nodes)
        return tree.resolvedSections()
    }

    private func makeResolvedTree(
        sections: [KsSettingsViewCore.Section]
    ) -> DSLDiffCalculator.ResolvedTree {
        return DSLDiffCalculator.ResolvedTree(
            sections: sections,
            rootHeader: nil,
            rootFooter: nil,
            theme: Theme()
        )
    }

    // MARK: - DSL による配置

    /// CustomCell を iOS DSL のセクションへイニシャライザ直書きで配置できる。
    func test_SectionBuilderにCustomCellをイニシャライザ直書きで配置できる() {
        let section = ksSection("カスタム") {
            LabelCell(title: "前")
            CustomCell(content: 42) { value in
                Text("値: \(value)")
            }
            CustomCell {
                Image(systemName: "star")
            }
            LabelCell(title: "後")
        }

        XCTAssertEqual(section.cells.count, 4)
        XCTAssertTrue(section.cells[1] is CustomCell, "content あり形が直書きで配置される")
        XCTAssertTrue(section.cells[2] is CustomCell, "content なし形も直書きで配置される")
        XCTAssertEqual((section.cells[1] as? CustomCell)?.content, AnyHashable(42))
    }

    /// modifier チェーン（`cellHeight` 等）が CustomCell でも機能する。
    func test_CustomCellにstyle_modifierチェーンが効く() {
        let cell = CustomCell(content: "x") { v in Text(v) }
            .cellHeight(120)
            .backgroundColor(.systemTeal)

        XCTAssertEqual(cell.style.cellHeight, 120)
        XCTAssertEqual(cell.style.backgroundColor, UIColor.systemTeal)
        XCTAssertEqual(cell.content, AnyHashable("x"), "modifier 適用後も content は保たれる")
    }

    /// icon modifier は CustomCell に適用できない（型として非対応）。
    func test_CustomCellはDSLIconModifiableに準拠しない() {
        let cell = CustomCell(content: "x") { v in Text(v) }
        XCTAssertFalse(
            cell is any DSLIconModifiable,
            "アイコン領域が存在しないため icon modifier は型として受け付けない"
        )
    }

    /// id 省略時の同一性は既存 DSL 規約（安定 ID 付与）に従う。
    func test_id省略時のCustomCellは2回評価で同じIDになる() {
        let first = evaluate {
            Section("カスタム") {
                CustomCell(content: "A") { v in Text(v) }
                CustomCell(content: "B") { v in Text(v) }
            }
        }
        let second = evaluate {
            Section("カスタム") {
                CustomCell(content: "A") { v in Text(v) }
                CustomCell(content: "B") { v in Text(v) }
            }
        }

        XCTAssertEqual(first.count, 1)
        XCTAssertEqual(
            first[0].cells.map { $0.id },
            second[0].cells.map { $0.id },
            "(SectionID, 位置, Cell 型) ベースで CustomCell の ID も安定する"
        )
        XCTAssertTrue(first[0].cells.allSatisfy { $0 is CustomCell })
    }

    // MARK: - 同値 content の no-rebind

    /// content が等価のままの再構成では `replaceCell` が発行されない
    /// （builder / onTap は毎回新しいクロージャでも差分検出が暴発しない）。
    func test_同値contentの再評価ではreplaceCellが発行されない() {
        let first = evaluate {
            Section("カスタム") {
                CustomCell(content: "同じ", onTap: {}) { v in Text(v) }
            }
        }
        let second = evaluate {
            Section("カスタム") {
                CustomCell(content: "同じ", onTap: {}) { v in Color.red }
            }
        }

        let diffs = DSLDiffCalculator.compute(
            from: makeResolvedTree(sections: first),
            to: makeResolvedTree(sections: second)
        )
        XCTAssertEqual(diffs, [], "content が等価なら再バインドは要求されない")
    }

    /// content が変われば `replaceCell` が 1 件だけ発行される（差分検出に検出力があることの確認）。
    func test_contentの変化でreplaceCellが発行される() {
        let first = evaluate {
            Section("カスタム") {
                CustomCell(content: "A") { v in Text(v) }
            }
        }
        let second = evaluate {
            Section("カスタム") {
                CustomCell(content: "B") { v in Text(v) }
            }
        }

        let diffs = DSLDiffCalculator.compute(
            from: makeResolvedTree(sections: first),
            to: makeResolvedTree(sections: second)
        )
        XCTAssertEqual(diffs.count, 1)
        guard case let .replaceCell(cellID, new) = diffs[0] else {
            XCTFail("Expected .replaceCell, got \(diffs[0])")
            return
        }
        XCTAssertEqual(cellID, KsCellID(cell: first[0].cells[0]))
        XCTAssertEqual((new as? CustomCell)?.content, AnyHashable("B"))
    }

    /// content の値が `AnyHashable` 比較で等価に見えても、実体型が変われば `replaceCell` が
    /// 発行される（builder の引数型が変わるため、再バインドしないと古い出力が残る）。
    func test_content実体型の変化でreplaceCellが発行される() {
        let first = evaluate {
            Section("カスタム") {
                CustomCell(content: Int(1)) { v in Text("Int: \(v)") }
            }
        }
        let second = evaluate {
            Section("カスタム") {
                CustomCell(content: Double(1.0)) { v in Text("Double: \(v)") }
            }
        }

        // 前提の確認: 値そのものは `AnyHashable` 比較では等価になってしまう。
        XCTAssertEqual(
            (first[0].cells[0] as? CustomCell)?.content,
            (second[0].cells[0] as? CustomCell)?.content,
            "AnyHashable の値比較だけでは Int(1) と Double(1.0) を区別できない（前提）"
        )

        let diffs = DSLDiffCalculator.compute(
            from: makeResolvedTree(sections: first),
            to: makeResolvedTree(sections: second)
        )
        XCTAssertEqual(diffs.count, 1, "content の型変化は差分検出をすり抜けない")
        guard case let .replaceCell(cellID, new) = diffs[0] else {
            XCTFail("Expected .replaceCell, got \(diffs[0])")
            return
        }
        XCTAssertEqual(cellID, KsCellID(cell: first[0].cells[0]))
        XCTAssertEqual((new as? CustomCell)?.content, AnyHashable(Double(1.0)))
    }

    /// 表示に効くスカラー（showArrow）の変化でも `replaceCell` が発行される。
    func test_showArrowの変化でreplaceCellが発行される() {
        let first = evaluate {
            Section("カスタム") {
                CustomCell(content: "x", showArrow: false) { v in Text(v) }
            }
        }
        let second = evaluate {
            Section("カスタム") {
                CustomCell(content: "x", showArrow: true) { v in Text(v) }
            }
        }

        let diffs = DSLDiffCalculator.compute(
            from: makeResolvedTree(sections: first),
            to: makeResolvedTree(sections: second)
        )
        XCTAssertEqual(diffs.count, 1)
        if case .replaceCell = diffs[0] {} else {
            XCTFail("Expected .replaceCell, got \(diffs[0])")
        }
    }
}
#endif
