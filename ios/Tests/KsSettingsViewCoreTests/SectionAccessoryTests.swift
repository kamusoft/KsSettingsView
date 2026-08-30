// SectionAccessoryTests.swift
// KsSettingsViewCoreTests
//
// `SectionAccessory` の text / view 両ケースの構築とケース別取り出し、text ケースの
// 等価性（文字列で判定）と view ケースの等価性（中身は無視）、text と view がケースとして
// 区別されること、および Hashable として Set に格納できることを検証する。

import SwiftUI
import XCTest
@testable import KsSettingsViewCore

final class SectionAccessoryTests: XCTestCase {

    // MARK: - text ケース

    func test_text_ケースの構築とケース別取り出し() {
        // GIVEN/WHEN
        let accessory: SectionAccessory = .text("一般")

        // THEN: ケース別取り出し
        guard case let .text(value) = accessory else {
            XCTFail("expected .text case")
            return
        }
        XCTAssertEqual(value, "一般")
    }

    func test_text_等価性_同じ文字列は等しい() {
        let a: SectionAccessory = .text("h")
        let b: SectionAccessory = .text("h")
        XCTAssertEqual(a, b)
        XCTAssertEqual(a.hashValue, b.hashValue)
    }

    func test_text_等価性_異なる文字列は等しくない() {
        let a: SectionAccessory = .text("h1")
        let b: SectionAccessory = .text("h2")
        XCTAssertNotEqual(a, b)
    }

    // MARK: - view ケース

    func test_view_ケースの構築() {
        // GIVEN: SwiftUI View をラップした KsAnyView
        let anyView = KsAnyView.swiftUI { Text("header view") }

        // WHEN
        let accessory: SectionAccessory = .view(anyView)

        // THEN: ケース判別が出来る
        guard case .view = accessory else {
            XCTFail("expected .view case")
            return
        }
    }

    func test_view_等価性_中身が違っても等しい() {
        // GIVEN: 中身の違う KsAnyView を持つ 2 つの .view ケース
        let a: SectionAccessory = .view(KsAnyView.swiftUI { Text("a") })
        let b: SectionAccessory = .view(KsAnyView.swiftUI { Text("b") })
        // THEN: KsAnyView は等価性に参加しないため、ケース一致のみで等価
        XCTAssertEqual(a, b)
        XCTAssertEqual(a.hashValue, b.hashValue)
    }

    // MARK: - text と view の区別

    func test_text_と_view_は別ケースとして区別される() {
        // GIVEN: 表示上は同じ "h" でも、片方は文字列ヘッダ、片方は任意 View ヘッダ
        let textAccessory: SectionAccessory = .text("h")
        let viewAccessory: SectionAccessory = .view(KsAnyView.swiftUI { Text("h") })

        // THEN: 別ケースとして等しくない
        XCTAssertNotEqual(textAccessory, viewAccessory)
    }

    // MARK: - Hashable

    func test_Hashable_Set_に格納できケース別に区別される() {
        let set: Set<SectionAccessory> = [
            .text("a"),
            .text("a"), // 重複
            .text("b"),
            .view(KsAnyView.swiftUI { Text("x") }),
            .view(KsAnyView.swiftUI { Text("y") }), // 中身違いだが view 同士は等価
        ]
        // 期待: .text("a") / .text("b") / .view(...) の 3 要素
        XCTAssertEqual(set.count, 3)
    }
}
