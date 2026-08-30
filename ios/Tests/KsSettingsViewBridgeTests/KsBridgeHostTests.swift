// KsBridgeHostTests.swift
// KsSettingsViewBridgeTests
//
// Native Host の生成と内部 Store への接続を検証する。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class KsBridgeHostTests: XCTestCase {

    /// Host を先に取り付けてから `setRoot` を呼んでも表示へ反映される。
    func test_Host生成後のsetRootが表示へ反映される() {
        let bridge = KsSettingsBridge()
        let attachment = KsBridgeTestHost.attach(bridge)
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [])

        let builder = KsBridgeRootBuilder()
        let section = builder.addSection(headerText: "S", footerText: nil)
        builder.addLabelCell(KsBridgeLabelCell(title: "A"), sectionID: section.sectionID)
        bridge.setRoot(builder)
        KsBridgeTestHost.pump(attachment)

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A"]])
    }

    /// `setRoot` の後に生成した Host は購読開始前の現在状態から表示を復元する。
    func test_setRoot後に生成したHostが現在状態を復元する() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A", "B"], ["C"]])
    }

    /// Bridge は同時に 1 つの Host を持ち、生成 API を繰り返し呼んでも同じ Host を返す。
    func test_makeHostViewController_は同じHostを返す() {
        let bridge = KsSettingsBridge()

        let first = bridge.makeHostViewController()
        let second = bridge.makeHostViewController()

        XCTAssertNotNil(first)
        XCTAssertTrue(first === second)
    }

    /// 破棄済みの Bridge は Host を生成しない。
    func test_破棄後のmakeHostViewControllerはnilを返す() {
        let bridge = KsSettingsBridge()
        bridge.dispose()

        XCTAssertNil(bridge.makeHostViewController())
    }
}
#endif
