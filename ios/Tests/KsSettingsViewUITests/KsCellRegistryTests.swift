// KsCellRegistryTests.swift
// KsSettingsViewUITests
//
// `KsCellRegistry` の登録・解決・未登録時の挙動を検証する。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

final class KsCellRegistryTests: XCTestCase {
    func test_登録した型が解決できる() {
        let registry = KsCellRegistry()
        registry.register(cellType: TestDummyCell.self, rendererType: TestDummyCellView.self)

        let cell: any KsCell = TestDummyCell(label: "abc")
        let resolved = registry.resolveRendererType(for: cell)
        XCTAssertNotNil(resolved)
        XCTAssertTrue(resolved == TestDummyCellView.self)
    }

    func test_未登録の型はnilが返る() {
        let registry = KsCellRegistry()
        // 登録せず解決のみ試みる
        let cell: any KsCell = TestDummyCell(label: "abc")
        XCTAssertNil(registry.resolveRendererType(for: cell))
    }

    func test_removeAllで登録が消える() {
        let registry = KsCellRegistry()
        registry.register(cellType: TestDummyCell.self, rendererType: TestDummyCellView.self)
        registry.removeAll()
        let cell: any KsCell = TestDummyCell(label: "abc")
        XCTAssertNil(registry.resolveRendererType(for: cell))
    }
}
#endif
