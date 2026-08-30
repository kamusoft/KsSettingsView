// PickerCellItemsTests.swift
// KsSettingsViewUITests
//
// `PickerCell` の候補モデル（`PickerItem`）と、任意の要素型を受けるジェネリック縁の検証。
// 射影・副表示の正規化・元要素の書き戻し・逆引き・value 自動表示・公開シグネチャの呼び出し形を扱う。

#if canImport(UIKit)
import XCTest
import UIKit
import SwiftUI
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

/// 射影対象の要素型（主表示と副表示の材料を別プロパティに持つ）。
private struct Plan: Equatable, Sendable {
    let name: String
    let detail: String
}

@MainActor
final class PickerCellItemsTests: XCTestCase {

    private let plans = [
        Plan(name: "無料", detail: "広告あり"),
        Plan(name: "標準", detail: "広告なし"),
        Plan(name: "上位", detail: "全機能")
    ]

    // MARK: - 選択面を通した確定操作のヘルパ

    /// Renderer の配線と同一経路で選択面を組み立てる（`render` からの配線漏れも検出する）。
    private func makeSurface(for cell: PickerCell) throws -> PickerListViewController {
        let view = PickerCellView()
        view.render(cell: cell, theme: Theme())
        let vc = try XCTUnwrap(view._makeListViewControllerForTesting())
        vc.loadViewIfNeeded()
        return vc
    }

    /// 単一選択の選択面で1行を確定する。
    private func confirmSingle(_ cell: PickerCell, row: Int) throws {
        try makeSurface(for: cell)._simulateSelect(row)
    }

    /// 複数選択の選択面で任意の行をトグルしてから「完了」で確定する。
    private func confirmMultiple(_ cell: PickerCell, toggling rows: [Int]) throws {
        let vc = try makeSurface(for: cell)
        for row in rows {
            vc._simulateSelect(row)
        }
        vc._simulateDone()
    }

    // MARK: - 候補モデル

    func test_生のPickerItem列がそのまま候補になる() {
        let items = [
            PickerItem(text: "無料", subText: "広告あり"),
            PickerItem(text: "標準")
        ]
        let cell = PickerCell(title: "プラン", items: items, selectedIndex: 0)
        XCTAssertEqual(cell.items, items)
        XCTAssertEqual(cell.items[0].subText, "広告あり")
        XCTAssertNil(cell.items[1].subText)
    }

    func test_ジェネリック縁の射影が各要素に適用される() {
        let cell = PickerCell(
            title: "プラン",
            items: plans,
            displayText: { $0.name },
            subText: { $0.detail },
            selectedIndex: 0
        )
        XCTAssertEqual(cell.items, [
            PickerItem(text: "無料", subText: "広告あり"),
            PickerItem(text: "標準", subText: "広告なし"),
            PickerItem(text: "上位", subText: "全機能")
        ])
    }

    func test_String特殊化は射影なしで主表示だけの候補になる() {
        let cell = PickerCell(title: "テーマ", items: ["ライト", "ダーク"], selectedIndex: 1)
        XCTAssertEqual(cell.items, [PickerItem(text: "ライト"), PickerItem(text: "ダーク")])
        XCTAssertNil(cell.items[0].subText)
        XCTAssertNil(cell.items[1].subText)
    }

    func test_空文字列のsubTextは副表示なしへ正規化される() {
        let projected = PickerCell(
            title: "プラン",
            items: plans,
            displayText: { $0.name },
            subText: { _ in "" },
            selectedIndex: nil
        )
        XCTAssertEqual(projected.items.compactMap(\.subText), [], "射影が空文字列を返した候補は副表示を持たない")

        let raw = PickerCell(title: "プラン", items: [PickerItem(text: "無料", subText: "")], selectedIndex: nil)
        XCTAssertNil(raw.items[0].subText, "生の経路でも空文字列は副表示なしへ揃う")
    }

    /// Swift では配列が値型のため、`source` への代入が縁の捕捉した列に届くことは構造的に無く、
    /// この観点は言語のセマンティクスが保証している (実装を変えても本ケースは落ちない)。
    /// ここで実際に確かめているのは、確定した位置の要素が `onItemSelected` へ渡ることである。
    /// 参照型のコレクションを共有し得る Android 側と対称な位置に置くため、ケース名は揃えてある。
    func test_構築後の元コレクション変更はobject_callbackに現れない() throws {
        var source = plans
        var received: Plan?
        let cell = PickerCell(
            title: "プラン",
            items: source,
            displayText: { $0.name },
            selectedIndex: nil,
            onItemSelected: { received = $0 }
        )
        source[1] = Plan(name: "差し替え", detail: "後から変更")

        try confirmSingle(cell, row: 1)
        XCTAssertEqual(received, plans[1], "構築時点の列の要素が届く")
    }

    // MARK: - value 自動表示

    func test_複数選択の自動表示は主表示のみをカンマ連結する() {
        let cell = PickerCell(
            title: "プラン",
            items: plans,
            displayText: { $0.name },
            subText: { $0.detail },
            selectedIndices: [2, 0]
        )
        XCTAssertEqual(cell.effectiveValueText(), "無料, 上位")
    }

    func test_単一選択の自動表示は主表示のみを使う() {
        let cell = PickerCell(
            title: "プラン",
            items: plans,
            displayText: { $0.name },
            subText: { $0.detail },
            selectedIndex: 1
        )
        XCTAssertEqual(cell.effectiveValueText(), "標準")
    }

    func test_範囲外indexは自動表示から除外される() {
        let multi = PickerCell(
            title: "プラン",
            items: plans,
            displayText: { $0.name },
            selectedIndices: [0, 99]
        )
        XCTAssertEqual(multi.effectiveValueText(), "無料")

        let single = PickerCell(
            title: "プラン",
            items: plans,
            displayText: { $0.name },
            selectedIndex: 99
        )
        XCTAssertNil(single.effectiveValueText())
    }

    // MARK: - 単一選択の object 書き戻し

    func test_確定でonItemSelectedに元要素が1回届く() throws {
        var received: [Plan] = []
        var indices: [Int] = []
        let cell = PickerCell(
            title: "プラン",
            items: plans,
            displayText: { $0.name },
            selectedIndex: nil,
            onSelectionChanged: { indices.append($0) },
            onItemSelected: { received.append($0) }
        )
        try confirmSingle(cell, row: 2)
        XCTAssertEqual(indices, [2], "index の通知は1回")
        XCTAssertEqual(received, [plans[2]], "元要素の通知は1回")
    }

    func test_selectedItemは構築時に候補列から逆引きされる() {
        var selected: Plan? = plans[1]
        let cell = PickerCell(
            title: "プラン",
            items: plans,
            displayText: { $0.name },
            selectedItem: Binding(get: { selected }, set: { selected = $0 })
        )
        XCTAssertEqual(cell.selectedIndex, 1)
    }

    func test_同値の重複候補は最初のindexへ解決される() {
        let duplicated = [plans[0], plans[1], plans[1]]
        var selected: Plan? = plans[1]
        let cell = PickerCell(
            title: "プラン",
            items: duplicated,
            displayText: { $0.name },
            selectedItem: Binding(get: { selected }, set: { selected = $0 })
        )
        XCTAssertEqual(cell.selectedIndex, 1)
    }

    func test_候補列に無いselectedItemは未選択になる() {
        var selected: Plan? = Plan(name: "存在しない", detail: "")
        let cell = PickerCell(
            title: "プラン",
            items: plans,
            displayText: { $0.name },
            selectedItem: Binding(get: { selected }, set: { selected = $0 })
        )
        XCTAssertNil(cell.selectedIndex)
    }

    func test_確定でselectedItemが対応する元要素へ書き戻される() throws {
        var selected: Plan? = plans[0]
        let cell = PickerCell(
            title: "プラン",
            items: plans,
            displayText: { $0.name },
            selectedItem: Binding(get: { selected }, set: { selected = $0 })
        )
        try confirmSingle(cell, row: 2)
        XCTAssertEqual(selected, plans[2])
    }

    // MARK: - 複数選択の object 受け取り

    func test_確定でonItemsSelectedにindex昇順の元要素列が届く() throws {
        var received: [[Plan]] = []
        let cell = PickerCell(
            title: "プラン",
            items: plans,
            displayText: { $0.name },
            selectedIndices: [],
            onItemsSelected: { received.append($0) }
        )
        try confirmMultiple(cell, toggling: [2, 0])
        XCTAssertEqual(received, [[plans[0], plans[2]]], "index 昇順の列が1回届く")
    }

    func test_範囲外indexはonItemsSelectedの列から除外されindex集合には残る() throws {
        var receivedIndices: Set<Int>?
        var receivedItems: [Plan]?
        let cell = PickerCell(
            title: "プラン",
            items: plans,
            displayText: { $0.name },
            selectedIndices: [0, 99],
            onMultiSelectionChanged: { receivedIndices = $0 },
            onItemsSelected: { receivedItems = $0 }
        )
        try confirmMultiple(cell, toggling: [])
        XCTAssertEqual(receivedIndices, [0, 99], "index 集合は範囲外 index を保持する")
        XCTAssertEqual(receivedItems, [plans[0]], "元要素列は有効な index の要素だけを含む")
    }

    // MARK: - 公開シグネチャの呼び出し形

    /// 公開している構築経路がすべて意図した overload へ解決されることを固定する。
    /// （型推論の衝突で別 overload に流れると、ここで解決失敗またはモード不一致になる）
    func test_公開シグネチャの全呼び出し形が意図したoverloadへ解決される() {
        var index: Int? = 0
        var indices: Set<Int> = [0]
        var plan: Plan? = plans[0]
        var name: String? = "ライト"
        let indexBinding = Binding<Int?>(get: { index }, set: { index = $0 })
        let indicesBinding = Binding<Set<Int>>(get: { indices }, set: { indices = $0 })
        let planBinding = Binding<Plan?>(get: { plan }, set: { plan = $0 })
        let nameBinding = Binding<String?>(get: { name }, set: { name = $0 })
        let rawItems = [PickerItem(text: "ライト"), PickerItem(text: "ダーク", subText: "省電力")]
        let strings = ["ライト", "ダーク"]
        // 候補件数を全経路で揃え、射影後の件数を1つの assert で確かめられるようにする
        let planPair = Array(plans.prefix(2))

        let cells: [PickerCell] = [
            // 生の経路
            PickerCell(title: "t", items: rawItems, selectedIndex: 0, onSelectionChanged: { _ in }),
            PickerCell(title: "t", items: rawItems, selectedIndex: indexBinding),
            PickerCell(title: "t", items: rawItems, selectedIndices: [0], onMultiSelectionChanged: { _ in }),
            PickerCell(title: "t", items: rawItems, selectedIndices: indicesBinding),
            // ジェネリック縁（Store 経路）
            PickerCell(
                title: "t",
                items: planPair,
                displayText: { $0.name },
                subText: { $0.detail },
                selectedIndex: 0,
                onSelectionChanged: { _ in },
                onItemSelected: { _ in }
            ),
            PickerCell(
                title: "t",
                items: planPair,
                displayText: { $0.name },
                selectedIndices: [0],
                maxSelectedNumber: 2,
                onMultiSelectionChanged: { _ in },
                onItemsSelected: { _ in }
            ),
            // ジェネリック縁（DSL 経路）
            PickerCell(
                title: "t",
                items: planPair,
                displayText: { $0.name },
                selectedIndex: indexBinding,
                onItemSelected: { _ in }
            ),
            PickerCell(title: "t", items: planPair, displayText: { $0.name }, selectedItem: planBinding),
            PickerCell(
                title: "t",
                items: planPair,
                displayText: { $0.name },
                selectedIndices: indicesBinding,
                onItemsSelected: { _ in }
            ),
            // String 特殊化（`displayText` 省略）
            PickerCell(title: "t", items: strings, selectedIndex: 0, onItemSelected: { _ in }),
            PickerCell(title: "t", items: strings, selectedIndex: indexBinding),
            PickerCell(title: "t", items: strings, selectedItem: nameBinding),
            PickerCell(title: "t", items: strings, selectedIndices: [0], onItemsSelected: { _ in }),
            PickerCell(title: "t", items: strings, selectedIndices: indicesBinding)
        ]

        let modes = cells.map(\.selectionMode)
        XCTAssertEqual(
            modes,
            [.single, .single, .multiple, .multiple, .single, .multiple, .single, .single, .multiple,
             .single, .single, .single, .multiple, .multiple]
        )
        XCTAssertTrue(cells.allSatisfy { $0.items.count == 2 }, "どの経路も2件の候補へ射影される")
        XCTAssertEqual(cells[4].items[1].subText, "広告なし", "ジェネリック縁の副表示射影が効く")
    }
}
#endif
