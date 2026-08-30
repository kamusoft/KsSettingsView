// KsCellIDTests.swift
// KsSettingsViewCoreTests
//
// `KsCellID` の同一性（`==` / `hashValue`）が **id（UUID）のみ** で決まり、Cell の内容
// （title / isOn 等）には依存しないことを検証する。
//
// 構造同期の同一性判定は id の同一性のみを用い、内容は用いない（core/ADR-0010）。
//
// 注: 本テストは UIKit に依存しない Core 層のロジックテストであり、macOS ホストの
//     `swift test` で実際に実行される（`#if canImport(UIKit)` ガードを持たない）。
//     これにより「構造同期 identity が id 限定であること」をホストで担保する。

import XCTest
@testable import KsSettingsViewCore

final class KsCellIDTests: XCTestCase {

    // MARK: - id 同一性のみで等価判定される

    func test_同一idで内容が異なる_Cell_の_KsCellID_は等価() {
        // GIVEN: 同じ id だが title が異なる 2 つの Cell
        let id = UUID()
        let cellA = DummyLabelCell(id: id, title: "A")
        let cellB = DummyLabelCell(id: id, title: "B-changed")

        // WHEN
        let idA = KsCellID(cell: cellA)
        let idB = KsCellID(cell: cellB)

        // THEN: 内容が違っても id が同じなら KsCellID は等価
        XCTAssertEqual(idA, idB, "内容差は KsCellID の同一性に影響してはならない（構造同期は id 限定）")
        XCTAssertEqual(idA.hashValue, idB.hashValue, "hashValue も id のみで決まる")
    }

    func test_同一idで型が異なる_Cell_の_KsCellID_も等価() {
        // GIVEN: 同じ id だが具象型が異なる Cell（内容比較では別物）
        let id = UUID()
        let labelCell = DummyLabelCell(id: id, title: "A")
        let switchCell = DummySwitchCell(id: id, isOn: true)

        // WHEN / THEN: 構造同期 identity は id のみのため等価
        XCTAssertEqual(KsCellID(cell: labelCell), KsCellID(cell: switchCell))
    }

    func test_id_が異なれば_KsCellID_は非等価() {
        // GIVEN: 内容が完全に同じだが id が異なる Cell
        let cellA = DummyLabelCell(id: UUID(), title: "Same")
        let cellB = DummyLabelCell(id: UUID(), title: "Same")

        // WHEN / THEN
        XCTAssertNotEqual(KsCellID(cell: cellA), KsCellID(cell: cellB))
    }

    // MARK: - init(id:) と init(cell:) の整合

    func test_init_id_と_init_cell_は同一_id_で等価() {
        let id = UUID()
        let cell = DummyLabelCell(id: id, title: "A")

        XCTAssertEqual(KsCellID(id: id), KsCellID(cell: cell))
    }

    // MARK: - 連続内容更新でも KsCellID が安定する（reconfigure 経路の前提）

    func test_同一idへの連続内容更新で_KsCellID_が常に同一() {
        // 「同一 id セルへの 2 回以上連続の内容更新」シナリオ。
        // reconfigure は snapshot 識別子を変えないため、内容が変わるたびに KsCellID が
        // 変わってしまうと 2 回目以降の照合が破綻する。id 限定なら常に同一であることを保証。
        let id = UUID()
        let c0 = DummyLabelCell(id: id, title: "A")
        let c1 = DummyLabelCell(id: id, title: "B")
        let c2 = DummyLabelCell(id: id, title: "C")

        let id0 = KsCellID(cell: c0)
        let id1 = KsCellID(cell: c1)
        let id2 = KsCellID(cell: c2)

        XCTAssertEqual(id0, id1)
        XCTAssertEqual(id1, id2)
        XCTAssertEqual(id0, id2)

        // Set（snapshot の itemIdentifiers 相当）でも 1 要素に畳まれる
        let set: Set<KsCellID> = [id0, id1, id2]
        XCTAssertEqual(set.count, 1, "内容違いでも同一 id は 1 つの識別子に畳まれる")
    }

    // MARK: - 値型の内容比較（Cell 自身の ==）は従来どおり内容差を検出する

    func test_Cell自身の_equals_は内容差を検出する_KsCellID_とは別レイヤ() {
        // KsCellID は id 限定だが、Cell 値自身の `==`（内容比較 = .replaceCell 発行判定に使う）
        // は内容差を検出し続ける。三層分離（構造同期 / 内容更新）が別レイヤであることの確認。
        let id = UUID()
        let cellA = DummyLabelCell(id: id, title: "A")
        let cellB = DummyLabelCell(id: id, title: "B")

        XCTAssertNotEqual(AnyHashable(cellA), AnyHashable(cellB), "内容比較は内容差を検出する")
        XCTAssertEqual(KsCellID(cell: cellA), KsCellID(cell: cellB), "構造同期 identity は id のみ")
    }
}
