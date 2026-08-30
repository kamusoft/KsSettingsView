// RootAccessoryTests.swift
// KsSettingsViewCoreTests
//
// `RootAccessory` の text / view 両ケースの構築とケース別取り出し、各ケースの等価性、
// および `SectionAccessory` とはコンパイル時に別型であることを検証する。

import SwiftUI
import XCTest
@testable import KsSettingsViewCore

final class RootAccessoryTests: XCTestCase {

    // MARK: - text ケース

    func test_text_ケースの構築とケース別取り出し() {
        let accessory: RootAccessory = .text("プロフィール")
        guard case let .text(value) = accessory else {
            XCTFail("expected .text case")
            return
        }
        XCTAssertEqual(value, "プロフィール")
    }

    func test_text_等価性_同じ文字列は等しい() {
        let a: RootAccessory = .text("h")
        let b: RootAccessory = .text("h")
        XCTAssertEqual(a, b)
        XCTAssertEqual(a.hashValue, b.hashValue)
    }

    func test_text_等価性_異なる文字列は等しくない() {
        let a: RootAccessory = .text("h1")
        let b: RootAccessory = .text("h2")
        XCTAssertNotEqual(a, b)
    }

    // MARK: - view ケース

    func test_view_ケースの構築() {
        let anyView = KsAnyView.swiftUI { Text("header") }
        let accessory: RootAccessory = .view(anyView)
        guard case .view = accessory else {
            XCTFail("expected .view case")
            return
        }
    }

    func test_view_等価性_中身が違っても等しい() {
        // GIVEN: 中身の違う KsAnyView を持つ 2 つの .view ケース
        let a: RootAccessory = .view(KsAnyView.swiftUI { Text("a") })
        let b: RootAccessory = .view(KsAnyView.swiftUI { Text("b") })
        // THEN: KsAnyView は等価性に参加しないため、ケース一致のみで等価
        XCTAssertEqual(a, b)
        XCTAssertEqual(a.hashValue, b.hashValue)
    }

    // MARK: - text と view の区別

    func test_text_と_view_は別ケースとして区別される() {
        let textAccessory: RootAccessory = .text("h")
        let viewAccessory: RootAccessory = .view(KsAnyView.swiftUI { Text("h") })
        XCTAssertNotEqual(textAccessory, viewAccessory)
    }

    // MARK: - SectionAccessory との別型保証

    func test_SectionAccessory_との別型保証() {
        // 互いに代入互換性を持たない別型であることを型レベルで確認する。
        // 以下のコードがコンパイルできることが、別型保証の根拠。
        // （直接代入できないため、明示的な書き換えが必要となる）
        let root: RootAccessory = .text("h")
        let section: SectionAccessory = .text("h")

        // Mirror で型名を比較し、別型であることを実行時にも確認
        XCTAssertNotEqual(
            String(describing: type(of: root)),
            String(describing: type(of: section))
        )
        XCTAssertEqual(String(describing: type(of: root)), "RootAccessory")
        XCTAssertEqual(String(describing: type(of: section)), "SectionAccessory")
    }

    // MARK: - Hashable

    func test_Hashable_Set_に格納できケース別に区別される() {
        let set: Set<RootAccessory> = [
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
