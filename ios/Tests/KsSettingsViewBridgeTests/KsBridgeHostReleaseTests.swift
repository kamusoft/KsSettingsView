// KsBridgeHostReleaseTests.swift
// KsSettingsViewBridgeTests
//
// Host だけを解放して Store を維持する `releaseHost()` の契約を検証する。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class KsBridgeHostReleaseTests: XCTestCase {

    /// 不透明な緑 (ARGB) を表す輸送値。
    private static let opaqueGreen = NSNumber(value: Int32(bitPattern: 0xFF00FF00))

    /// 指定 Section の先頭行に実描画された title の文字色を返す。
    private func firstRowTitleColor(_ attachment: KsBridgeTestHost.Attachment) -> UIColor? {
        let cell = attachment.collectionView.cellForItem(at: IndexPath(item: 0, section: 0))
        return (cell as? KsListCellBase)?.titleLabel.textColor
    }

    /// 解放後に生成した Host は解放前と別インスタンスで、Store 現在状態から表示を復元する。
    func test_解放後の再生成はStore現在状態を復元する() {
        let fixture = KsBridgeFixture.standard()
        let first = KsBridgeTestHost.attach(fixture.bridge)
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(first), [["A", "B"], ["C"]])

        fixture.bridge.releaseHost()
        let second = KsBridgeTestHost.attach(fixture.bridge)

        XCTAssertFalse(first.controller === second.controller, "解放後は新しい Host が返る")
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(second), [["A", "B"], ["C"]])
    }

    /// Host 不在の間に適用した更新は、再生成した Host の表示に反映される。
    func test_解放中の更新は再生成時に反映される() {
        let fixture = KsBridgeFixture.standard()
        let first = KsBridgeTestHost.attach(fixture.bridge)
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(first), [["A", "B"], ["C"]])

        fixture.bridge.releaseHost()
        fixture.bridge.replaceCell(
            cellID: fixture.cellA.cellID,
            newCell: KsBridgeLabelCell(title: "A-updated")
        )
        fixture.bridge.updateAccessory(
            target: .sectionHeader,
            sectionID: fixture.section1.sectionID,
            text: "S1-updated"
        )
        let theme = KsBridgeTheme()
        theme.cellTitleColor = Self.opaqueGreen
        fixture.bridge.setTheme(theme)

        let second = KsBridgeTestHost.attach(fixture.bridge)

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(second), [["A-updated", "B"], ["C"]])
        XCTAssertEqual(KsBridgeTestHost.headerText(second, section: 0), "S1-updated")
        XCTAssertEqual(firstRowTitleColor(second), UIColor(red: 0, green: 1, blue: 0, alpha: 1))
    }

    /// 解放後に Store を更新しても、view 階層に残置した旧 Host の表示は変化しない。
    func test_解放後のStore更新は旧handleに反映されない() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)
        let titlesBefore = KsBridgeTestHost.renderedTitles(attachment)
        let colorBefore = firstRowTitleColor(attachment)

        fixture.bridge.releaseHost()
        fixture.bridge.replaceCell(
            cellID: fixture.cellA.cellID,
            newCell: KsBridgeLabelCell(title: "A-updated")
        )
        let theme = KsBridgeTheme()
        theme.cellTitleColor = Self.opaqueGreen
        fixture.bridge.setTheme(theme)
        waitForNegativeVerification(in: attachment.collectionView)

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), titlesBefore)
        XCTAssertEqual(firstRowTitleColor(attachment), colorBefore)
        XCTAssertEqual(
            fixture.bridge.store.theme.cellTitleColor,
            UIColor(red: 0, green: 1, blue: 0, alpha: 1),
            "Store 自体は更新されている"
        )
    }

    /// 解放 API を繰り返し呼んでもエラーにならず、Store の設定ツリーと Theme は維持される。
    func test_releaseHostは冪等でStoreを維持する() {
        let fixture = KsBridgeFixture.standard()
        let theme = KsBridgeTheme()
        theme.cellTitleColor = Self.opaqueGreen
        fixture.bridge.setTheme(theme)
        _ = KsBridgeTestHost.attach(fixture.bridge)

        fixture.bridge.releaseHost()
        fixture.bridge.releaseHost()
        fixture.bridge.releaseHost()

        let restored = KsBridgeTestHost.attach(fixture.bridge)
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(restored), [["A", "B"], ["C"]])
        XCTAssertEqual(KsBridgeTestHost.headerText(restored, section: 0), "S1")
        XCTAssertEqual(firstRowTitleColor(restored), UIColor(red: 0, green: 1, blue: 0, alpha: 1))
    }

    /// Host を生成していない Bridge への解放呼び出しは no-op で、その後の生成に影響しない。
    func test_Host未生成でのreleaseHostはno_op() {
        let fixture = KsBridgeFixture.standard()

        fixture.bridge.releaseHost()

        let attachment = KsBridgeTestHost.attach(fixture.bridge)
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A", "B"], ["C"]])
    }

    /// 破棄済みの Bridge への解放呼び出しは no-op で、Host 生成は引き続き `nil` を返す。
    func test_dispose後のreleaseHostはno_op() {
        let fixture = KsBridgeFixture.standard()
        _ = KsBridgeTestHost.attach(fixture.bridge)
        fixture.bridge.dispose()

        fixture.bridge.releaseHost()

        XCTAssertNil(fixture.bridge.makeHostViewController())
    }

    /// 解放後、Bridge は旧 Host を参照せず、外部参照を破棄した旧 Host は回収される。
    func test_解放後に旧Hostへの参照を保持しない() {
        let fixture = KsBridgeFixture.standard()
        weak var weakHost: KsSettingsViewController?

        autoreleasepool {
            guard let host = fixture.bridge.makeHostViewController() as? KsSettingsViewController else {
                return XCTFail("Bridge が Native Host を返さなかった")
            }
            _ = host.view
            weakHost = host
            XCTAssertNotNil(weakHost)
            fixture.bridge.releaseHost()
        }

        let exp = expectation(description: "wait runloop")
        DispatchQueue.main.async { exp.fulfill() }
        wait(for: [exp], timeout: 1.0)

        XCTAssertNil(weakHost, "解放後も旧 Host が参照され続けている")
        XCTAssertNil(fixture.bridge.hostController)
    }
}
#endif
