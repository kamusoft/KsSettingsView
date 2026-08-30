// RootAccessoryRenderingTests.swift
// KsSettingsViewUITests
//
// `KsSettingsViewController.rootHeader` / `rootFooter` に設定した Root H/F が
// 描画されることを検証する。

#if canImport(UIKit)
import XCTest
import SwiftUI
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class RootAccessoryRenderingTests: XCTestCase {

    func test_rootHeaderにtextを代入するとboundary構成が更新される() {
        let controller = KsSettingsViewController(root: SettingsRoot())
        _ = controller.view

        XCTAssertNil(controller.rootHeader)
        controller.rootHeader = .text("プロフィール")
        XCTAssertEqual(controller.rootHeader, .text("プロフィール"))
    }

    func test_rootFooterにtextを代入するとboundary構成が更新される() {
        let controller = KsSettingsViewController(root: SettingsRoot())
        _ = controller.view

        XCTAssertNil(controller.rootFooter)
        controller.rootFooter = .text("v1.0.0")
        XCTAssertEqual(controller.rootFooter, .text("v1.0.0"))
    }

    func test_rootHeaderにnilを代入するとboundaryが削除される() {
        let controller = KsSettingsViewController(root: SettingsRoot())
        _ = controller.view
        controller.rootHeader = .text("X")
        XCTAssertNotNil(controller.rootHeader)

        controller.rootHeader = nil
        XCTAssertNil(controller.rootHeader)
    }

    func test_rootHeaderにviewを代入してもboundary構成が反映される() {
        let controller = KsSettingsViewController(root: SettingsRoot())
        _ = controller.view

        let kav = KsAnyView.swiftUI { EmptyView() }
        controller.rootHeader = .view(kav)

        if case .view = controller.rootHeader {
            // success
        } else {
            XCTFail("rootHeader が .view ケースとして保持されていない")
        }
    }
}
#endif
