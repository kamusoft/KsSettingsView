// SectionVisibilityTests.swift
// KsSettingsViewCoreTests
//
// `Section.isVisible` の既定値（true）と、等価性判定に含まれることを検証する。

import XCTest
@testable import KsSettingsViewCore

final class SectionVisibilityTests: XCTestCase {

    func test_isVisible_既定値_true() {
        let section = Section(id: UUID(), header: nil, footer: nil, cells: [])
        XCTAssertTrue(section.isVisible, "Section の isVisible は既定で true でなければならない")
    }

    func test_isVisible_明示指定_false() {
        let section = Section(id: UUID(), header: nil, footer: nil, cells: [], isVisible: false)
        XCTAssertFalse(section.isVisible)
    }

    func test_等価性_isVisible_のみ異なるインスタンスは等価とみなされない() {
        let id = UUID()
        let a = Section(id: id, header: .text("h"), footer: nil, cells: [], isVisible: true)
        let b = Section(id: id, header: .text("h"), footer: nil, cells: [], isVisible: false)
        XCTAssertNotEqual(a, b, "isVisible のみ異なる Section は等価とみなされてはならない")
        XCTAssertNotEqual(a.hashValue, b.hashValue, "isVisible が異なる場合 hash 値が一致しないことが期待される（保証は等価性のみ）")
    }

    func test_等価性_全フィールド一致は等価() {
        let id = UUID()
        let a = Section(id: id, header: .text("h"), footer: .text("f"), cells: [], headerHeight: 10, isVisible: false)
        let b = Section(id: id, header: .text("h"), footer: .text("f"), cells: [], headerHeight: 10, isVisible: false)
        XCTAssertEqual(a, b)
    }

    func test_isVisible_未指定の既存呼び出し互換() {
        // 既存呼び出し（isVisible を渡さない）が引き続きビルドできる + 既定 true が適用される
        let section = Section(id: UUID(), header: .text("一般"), footer: nil, cells: [])
        XCTAssertTrue(section.isVisible)
    }
}
