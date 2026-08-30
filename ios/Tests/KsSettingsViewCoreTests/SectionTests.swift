// SectionTests.swift
// KsSettingsViewCoreTests
//
// `Section` の構築（文字列ヘッダ / 任意 View ヘッダ / 空セクション）と、
// `cells` や `headerHeight` を含む等価性判定を検証する。

import SwiftUI
import XCTest
@testable import KsSettingsViewCore

final class SectionTests: XCTestCase {

    func test_構築_文字列ヘッダで全フィールドを保持する() {
        // GIVEN
        let id = UUID()
        let cells: [any KsCell] = [DummyLabelCell(title: "a")]

        // WHEN
        let section = Section(
            id: id,
            header: .text("一般"),
            footer: .text("footer"),
            cells: cells
        )

        // THEN
        XCTAssertEqual(section.id, id)
        XCTAssertEqual(section.header, .text("一般"))
        XCTAssertEqual(section.footer, .text("footer"))
        XCTAssertEqual(section.cells.count, 1)

        // header から元の文字列を取り出せる（ケース別取り出し）
        if case let .text(value) = section.header {
            XCTAssertEqual(value, "一般")
        } else {
            XCTFail("header should be .text case")
        }
    }

    func test_構築_view_ヘッダで_KsAnyView_を持てる() {
        // GIVEN: SwiftUI View をラップした KsAnyView
        let anyView = KsAnyView.swiftUI { Text("header view") }

        // WHEN
        let section = Section(
            id: UUID(),
            header: .view(anyView),
            footer: nil,
            cells: []
        )

        // THEN: header から view ケースを取り出せる
        guard case .view = section.header else {
            XCTFail("header should be .view case")
            return
        }
    }

    func test_空_cells_で構築でき_isEmpty_が真() {
        // GIVEN/WHEN
        let section = Section(cells: [])

        // THEN: 例外なく構築でき、cells.isEmpty が真
        XCTAssertTrue(section.cells.isEmpty)
        XCTAssertNil(section.header)
        XCTAssertNil(section.footer)
    }

    func test_等価性_同フィールドは等しい() {
        let id = UUID()
        let cellId = UUID()
        let cells: [any KsCell] = [DummyLabelCell(id: cellId, title: "a")]
        let s1 = Section(id: id, header: .text("h"), footer: .text("f"), cells: cells)
        let s2 = Section(id: id, header: .text("h"), footer: .text("f"), cells: cells)
        XCTAssertEqual(s1, s2)
        XCTAssertEqual(s1.hashValue, s2.hashValue)
    }

    func test_等価性_id_が異なれば等しくない() {
        let s1 = Section(id: UUID(), header: nil, footer: nil, cells: [])
        let s2 = Section(id: UUID(), header: nil, footer: nil, cells: [])
        XCTAssertNotEqual(s1, s2)
    }

    func test_等価性_view_ヘッダ同士は中身無視で等しい() {
        let id = UUID()
        let s1 = Section(id: id, header: .view(KsAnyView.swiftUI { Text("a") }))
        let s2 = Section(id: id, header: .view(KsAnyView.swiftUI { Text("b") }))
        XCTAssertEqual(s1, s2)
        XCTAssertEqual(s1.hashValue, s2.hashValue)
    }

    func test_等価性_異なる_cells_は等しくない() {
        let id = UUID()
        let cell1: any KsCell = DummyLabelCell(id: UUID(), title: "a")
        let cell2: any KsCell = DummyLabelCell(id: UUID(), title: "b")
        let s1 = Section(id: id, cells: [cell1])
        let s2 = Section(id: id, cells: [cell2])
        XCTAssertNotEqual(s1, s2)
    }

    func test_異種_Cell_を_cells_に格納できる() {
        // GIVEN: 異なる具象 Cell A / B
        let label = DummyLabelCell(title: "label")
        let switchCell = DummySwitchCell(isOn: true)

        // WHEN: 同じ [any KsCell] に格納
        let cells: [any KsCell] = [label, switchCell]

        // THEN: コンパイル可能、要素数 2
        let section = Section(cells: cells)
        XCTAssertEqual(section.cells.count, 2)
    }

    // MARK: - headerHeight

    func test_headerHeight_既定値は_minus1() {
        // GIVEN: headerHeight を指定せず Section を構築
        let section = Section(id: UUID(), header: nil, footer: nil, cells: [])
        // THEN: 既定値 -1（自動）が適用される
        XCTAssertEqual(section.headerHeight, -1)
    }

    func test_headerHeight_明示指定で値を保持する() {
        // GIVEN: headerHeight = 40 を明示指定
        let section = Section(
            id: UUID(),
            header: .text("一般"),
            footer: nil,
            cells: [],
            headerHeight: 40
        )
        // THEN: 指定値を保持
        XCTAssertEqual(section.headerHeight, 40)
    }

    func test_headerHeight_は等価性判定に含まれる() {
        let id = UUID()
        let s1 = Section(id: id, headerHeight: -1)
        let s2 = Section(id: id, headerHeight: 40)
        XCTAssertNotEqual(s1, s2)

        let s3 = Section(id: id, headerHeight: 40)
        XCTAssertEqual(s2, s3)
        XCTAssertEqual(s2.hashValue, s3.hashValue)
    }

    // MARK: - Header / Footer 表示トグル

    func test_表示トグルの既定値はいずれもtrue() {
        // GIVEN: トグルを指定せず Section を構築
        let section = Section(id: UUID(), header: .text("一般"), footer: .text("補足"), cells: [])
        // THEN: 既定値 true が適用される
        XCTAssertTrue(section.isHeaderVisible)
        XCTAssertTrue(section.isFooterVisible)
    }

    func test_表示トグルは明示指定した値を保持する() {
        // GIVEN: 両トグルを false で明示指定
        let section = Section(
            id: UUID(),
            header: .text("一般"),
            footer: .text("補足"),
            cells: [],
            isHeaderVisible: false,
            isFooterVisible: false
        )
        // THEN: 指定値を保持し、accessory の内容は失われない
        XCTAssertFalse(section.isHeaderVisible)
        XCTAssertFalse(section.isFooterVisible)
        XCTAssertEqual(section.header, .text("一般"))
        XCTAssertEqual(section.footer, .text("補足"))
    }

    func test_isHeaderVisible_は等価性判定に含まれる() {
        // GIVEN: トグル以外が同一内容の 2 つの Section
        let id = UUID()
        let s1 = Section(id: id, header: .text("一般"), cells: [])
        // WHEN: 一方だけ isHeaderVisible = false にする
        let s2 = Section(id: id, header: .text("一般"), cells: [], isHeaderVisible: false)
        // THEN: 等価にならない
        XCTAssertNotEqual(s1, s2)

        let s3 = Section(id: id, header: .text("一般"), cells: [], isHeaderVisible: false)
        XCTAssertEqual(s2, s3)
        XCTAssertEqual(s2.hashValue, s3.hashValue)
    }

    func test_isFooterVisible_は等価性判定に含まれる() {
        let id = UUID()
        let s1 = Section(id: id, footer: .text("補足"), cells: [])
        let s2 = Section(id: id, footer: .text("補足"), cells: [], isFooterVisible: false)
        XCTAssertNotEqual(s1, s2)

        let s3 = Section(id: id, footer: .text("補足"), cells: [], isFooterVisible: false)
        XCTAssertEqual(s2, s3)
        XCTAssertEqual(s2.hashValue, s3.hashValue)
    }

    func test_HeaderトグルとFooterトグルは独立して等価性へ参加する() {
        // GIVEN: Header だけ隠した Section と Footer だけ隠した Section
        let id = UUID()
        let headerHidden = Section(
            id: id, header: .text("一般"), footer: .text("補足"), cells: [], isHeaderVisible: false
        )
        let footerHidden = Section(
            id: id, header: .text("一般"), footer: .text("補足"), cells: [], isFooterVisible: false
        )
        // THEN: 片方だけの違いが等価性に現れる
        XCTAssertNotEqual(headerHidden, footerHidden)
    }
}
