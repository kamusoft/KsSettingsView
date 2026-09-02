// KsBridgeLifecycleTests.swift
// KsSettingsViewBridgeTests
//
// Bridge の破棄が冪等であること、破棄後の操作が no-op であることを検証する。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class KsBridgeLifecycleTests: XCTestCase {

    /// 破棄 API を繰り返し呼んでもエラーやクラッシュにならない。
    func test_dispose_は冪等() {
        let fixture = KsBridgeFixture.standard()

        fixture.bridge.dispose()
        fixture.bridge.dispose()
        fixture.bridge.dispose()

        XCTAssertTrue(fixture.bridge.isDisposed)
    }

    /// 破棄後の内容更新と Theme 適用は no-op で、表示中の Host も変化しない。
    func test_破棄後のreplaceCellとsetThemeは表示を変えない() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)
        let before = KsBridgeTestHost.renderedTitles(attachment)
        let themeBefore = fixture.bridge.store.theme

        fixture.bridge.dispose()
        fixture.bridge.replaceCell(
            cellID: fixture.cellA.cellID,
            newCell: KsBridgeLabelCell(title: "A-updated")
        )
        let theme = KsBridgeTheme()
        theme.backgroundColor = NSNumber(value: Int32(bitPattern: 0xFF00FF00))
        fixture.bridge.setTheme(theme)
        waitForNegativeVerification(in: attachment.collectionView)

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), before)
        XCTAssertEqual(fixture.bridge.store.theme, themeBefore)
    }

    /// 破棄後の構造操作と root 全置換も状態を変えない。
    func test_破棄後のsetRootと構造操作は状態を変えない() {
        let fixture = KsBridgeFixture.standard()
        let before = fixture.bridge.store.root

        fixture.bridge.dispose()
        fixture.bridge.setRoot(KsBridgeRootBuilder())
        XCTAssertNil(fixture.bridge.insertSection(KsBridgeSection(headerText: "N", footerText: nil), at: 0))
        fixture.bridge.removeSection(sectionID: fixture.section1.sectionID)
        fixture.bridge.moveSection(from: 0, to: 1)
        fixture.bridge.replaceSection(
            sectionID: fixture.section1.sectionID,
            newSection: KsBridgeSection(headerText: "N", footerText: nil)
        )
        XCTAssertNil(fixture.bridge.insertCell(
            KsBridgeLabelCell(title: "N"),
            sectionID: fixture.section1.sectionID,
            at: 0
        ))
        fixture.bridge.removeCell(cellID: fixture.cellA.cellID)
        fixture.bridge.moveCell(cellID: fixture.cellA.cellID, to: 0)
        fixture.bridge.replaceCells([
            KsBridgeCellUpdate(cellID: fixture.cellA.cellID, cell: KsBridgeLabelCell(title: "N"))
        ])
        fixture.bridge.updateAccessory(
            target: .sectionHeader,
            sectionID: fixture.section1.sectionID,
            text: "N"
        )

        XCTAssertEqual(fixture.bridge.store.root, before)
    }
}
#endif
