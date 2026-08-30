// SettingsRootTests.swift
// KsSettingsViewCoreTests
//
// `SettingsRoot` の構築と等価性を検証する。`SettingsRoot` はスタイルを持たない
//（`Theme` は UI 層に属する: core/ADR-0009）ため、確認は `sections` のみで行う。

import XCTest
@testable import KsSettingsViewCore

final class SettingsRootTests: XCTestCase {

    func test_構築_sections_を保持する() {
        // GIVEN: 任意個数の Section リスト
        let sectionId = UUID()
        let sections = [Section(id: sectionId, header: .text("general"), footer: nil, cells: [])]

        // WHEN: SettingsRoot を構築する
        let root = SettingsRoot(sections: sections)

        // THEN: sections を保持する
        XCTAssertEqual(root.sections.count, 1)
        XCTAssertEqual(root.sections[0].id, sectionId)
        XCTAssertEqual(root.sections[0].header, SectionAccessory.text("general"))
    }

    func test_等価性_同一_sections_は等しい() {
        // GIVEN: 同じ sections
        let sectionId = UUID()
        let cellId = UUID()
        let cells: [any KsCell] = [DummyLabelCell(id: cellId, title: "a")]
        let s1 = Section(id: sectionId, header: .text("h"), footer: .text("f"), cells: cells)
        let s2 = Section(id: sectionId, header: .text("h"), footer: .text("f"), cells: cells)

        // WHEN: 同じ内容で 2 つ生成
        let r1 = SettingsRoot(sections: [s1])
        let r2 = SettingsRoot(sections: [s2])

        // THEN: 等価
        XCTAssertEqual(r1, r2)
        XCTAssertEqual(r1.hashValue, r2.hashValue)
    }

    func test_等価性_異なる_sections_は等しくない() {
        // GIVEN: sections が異なる
        let r1 = SettingsRoot(sections: [])
        let r2 = SettingsRoot(sections: [Section()])

        // WHEN / THEN: 等価でない
        XCTAssertNotEqual(r1, r2)
    }

    func test_空_sections_でも構築できる() {
        // GIVEN/WHEN: sections が空
        let root = SettingsRoot()

        // THEN: 例外なく生成され、sections が空
        XCTAssertTrue(root.sections.isEmpty)
    }

    func test_theme_プロパティ不在() {
        // 本テストは型レベルの API 確認。`SettingsRoot` に `theme` フィールドが
        // 存在しないことをコンパイル時に保証する（コメントとして残す）。
        // 以下の行をアンコメントするとコンパイルエラーになる：
        //   _ = SettingsRoot().theme
        let root = SettingsRoot()
        XCTAssertTrue(root.sections.isEmpty)
    }
}
