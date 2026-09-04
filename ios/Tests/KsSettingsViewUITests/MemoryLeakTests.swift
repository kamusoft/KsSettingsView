// MemoryLeakTests.swift
// KsSettingsViewUITests
//
// `KsSettingsViewController` が所定のタイミングで `deinit` されることを検証する。
// 直接設定した場合と `SettingsRootStore` 経由で設定した場合の両方を対象とする。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
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

        awaitCondition(
            "Controller がスコープを抜けた後に解放される",
            actual: { "weakController=\(weakController == nil ? "nil" : "non-nil")" },
            until: { weakController == nil }
        )

        XCTAssertNil(weakController, "Controller がスコープを抜けても解放されていない（メモリリーク）")
    }

    func test_Store経由でもControllerがdeinitされ解放後もStoreを操作できる() {
        weak var weakController: KsSettingsViewController?
        // Store は長命想定でテスト用に外で保持する
        let initialCell = LabelCell(title: "A")
        let insertedCell = LabelCell(title: "B")
        let section = Section(header: .text("S"), cells: [initialCell])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section]))

        autoreleasepool {
            let controller = KsSettingsViewController(store: store)
            _ = controller.view
            // Store のメソッドを呼んで Diff 配信経路を確実に動かす
            store.insertCell(insertedCell, in: section.id, at: 1)
            XCTAssertEqual(store.root.sections.first?.cells.count, 2)
            weakController = controller
            XCTAssertNotNil(weakController)
        }

        awaitCondition(
            "Store 経由の Controller がスコープを抜けた後に解放される",
            actual: { "weakController=\(weakController == nil ? "nil" : "non-nil")" },
            until: { weakController == nil }
        )

        XCTAssertNil(weakController, "Store 経路でも Controller が解放されていない（メモリリーク）")

        // Store は長命なまま使い続けられ、Controller 解放後も操作結果が状態へ反映される。
        store.removeCell(cellID: KsCellID(cell: insertedCell))
        XCTAssertEqual(store.root.sections.first?.cells.count, 1)
        XCTAssertEqual(store.root.sections.first?.cells.first?.id, initialCell.id)
    }
}
#endif
