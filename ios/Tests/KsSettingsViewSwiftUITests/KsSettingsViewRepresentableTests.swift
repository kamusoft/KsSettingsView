// KsSettingsViewRepresentableTests.swift
// KsSettingsViewSwiftUITests
//
// `KsSettingsView`（UIViewControllerRepresentable）の make / update を検証する。
// SwiftUI の `Context` は直接生成できないため、`KsSettingsView.makeController()` /
// `applyUpdate(to:coordinator:)` のテスト容易化フック経由で検証する。
//
// 検証対象は `SettingsRootStore` 経由の更新、`KsSettingsViewStyle` の反映と切り替え、
// および Root H/F modifier。

#if canImport(UIKit)
import XCTest
import UIKit
import SwiftUI
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class KsSettingsViewRepresentableTests: XCTestCase {

    func test_makeControllerでstoreのrootとstyleが反映される() {
        let initialRoot = SettingsRoot(sections: [
            KsSettingsViewCore.Section(header: .text("S"), cells: [LabelCell(title: "A")])
        ])
        let store = SettingsRootStore(initialRoot: initialRoot)
        let view = KsSettingsView(store: store, style: .modern)
        let controller = view.makeController()
        _ = controller.view

        XCTAssertEqual(controller.style, .modern)
        // Store の初期 root が controller 内部の root に反映されること
        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfSections, 1)
        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems, 1)
    }

    func test_storeのinsertCellでcontrollerのsnapshotが更新される() {
        let sectionID = UUID()
        let initialRoot = SettingsRoot(sections: [
            KsSettingsViewCore.Section(id: sectionID, header: .text("S"), cells: [LabelCell(title: "A")])
        ])
        let store = SettingsRootStore(initialRoot: initialRoot)
        let view = KsSettingsView(store: store, style: .classic)
        let controller = view.makeController()
        _ = controller.view

        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems, 1)

        // Store の insertCell が controller の applyDiff 経由で snapshot に反映される
        store.insertCell(LabelCell(title: "B"), in: sectionID, at: 1)
        XCTAssertEqual(controller.internalDataSource?.snapshot().numberOfItems, 2)
    }

    func test_applyUpdateでstyleが切り替わる() {
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [
            KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])
        ]))

        let viewClassic = KsSettingsView(store: store, style: .classic)
        let coordinator = viewClassic.makeCoordinator()
        let controller = viewClassic.makeController()
        _ = controller.view
        XCTAssertEqual(controller.style, .classic)

        let viewModern = KsSettingsView(store: store, style: .modern)
        viewModern.applyUpdate(to: controller, coordinator: coordinator)

        XCTAssertEqual(controller.style, .modern)
    }

    func test_modernで初期化したcontrollerは即時にmodernになる() {
        let store = SettingsRootStore(initialRoot: SettingsRoot())
        let view = KsSettingsView(store: store, style: .modern)
        let controller = view.makeController()
        _ = controller.view

        XCTAssertEqual(controller.style, .modern)
    }

    func test_rootHeader_modifierでcontrollerのrootHeaderが設定される() {
        let store = SettingsRootStore(initialRoot: SettingsRoot())
        let view = KsSettingsView(store: store)
            .rootHeader("プロフィール")
        let controller = view.makeController()
        _ = controller.view

        XCTAssertEqual(controller.rootHeader, .text("プロフィール"))
    }

    func test_rootFooter_modifierでcontrollerのrootFooterが設定される() {
        let store = SettingsRootStore(initialRoot: SettingsRoot())
        let view = KsSettingsView(store: store)
            .rootFooter("v1.0.0")
        let controller = view.makeController()
        _ = controller.view

        XCTAssertEqual(controller.rootFooter, .text("v1.0.0"))
    }

    func test_applyUpdateでrootHeaderが更新される() {
        let store = SettingsRootStore(initialRoot: SettingsRoot())
        let view1 = KsSettingsView(store: store).rootHeader("旧")
        let coordinator = view1.makeCoordinator()
        let controller = view1.makeController()
        _ = controller.view
        XCTAssertEqual(controller.rootHeader, .text("旧"))

        let view2 = KsSettingsView(store: store).rootHeader("新")
        view2.applyUpdate(to: controller, coordinator: coordinator)

        XCTAssertEqual(controller.rootHeader, .text("新"))
    }
}
#endif
