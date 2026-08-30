// MemoryLeakTests.swift
// KsSettingsViewUITests
//
// `KsSettingsViewController` が所定のタイミングで `deinit` されることを検証する。
// 直接設定した場合と `SettingsRootStore` 経由で設定した場合の両方を対象とする。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class MemoryLeakTests: XCTestCase {
    func test_KsSettingsViewControllerはスコープを抜けるとdeinitされる() {
        weak var weakController: KsSettingsViewController?

        autoreleasepool {
            let controller = KsSettingsViewController(root: SettingsRoot(sections: [
                Section(header: .text("S"), cells: [LabelCell(title: "A")])
            ]))
            _ = controller.view
            weakController = controller
            XCTAssertNotNil(weakController)
        }

        let exp = expectation(description: "wait runloop")
        DispatchQueue.main.async { exp.fulfill() }
        wait(for: [exp], timeout: 1.0)

        XCTAssertNil(weakController, "Controller がスコープを抜けても解放されていない（メモリリーク）")
    }

    func test_Store経由でもControllerがdeinitされStore購読が解除される() {
        weak var weakController: KsSettingsViewController?
        // Store は長命想定でテスト用に外で保持する
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [
            Section(header: .text("S"), cells: [LabelCell(title: "A")])
        ]))

        autoreleasepool {
            let controller = KsSettingsViewController(store: store)
            _ = controller.view
            // Store のメソッドを呼んで Diff 配信経路を確実に動かす
            store.insertCell(LabelCell(title: "B"), in: store.root.sections[0].id, at: 1)
            weakController = controller
            XCTAssertNotNil(weakController)
        }

        let exp = expectation(description: "wait runloop")
        DispatchQueue.main.async { exp.fulfill() }
        wait(for: [exp], timeout: 1.0)

        XCTAssertNil(weakController, "Store 経路でも Controller が解放されていない（メモリリーク）")

        // Store は長命なまま使い続けられること（追加操作してもクラッシュしない）
        store.removeCell(cellID: KsCellID(cell: store.root.sections[0].cells.last!))
    }
}
#endif
