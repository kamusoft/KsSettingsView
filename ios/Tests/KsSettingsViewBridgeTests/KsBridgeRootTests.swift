// KsBridgeRootTests.swift
// KsSettingsViewBridgeTests
//
// Builder による root 構築と `setRoot`、Bridge 採番 ID による後続操作を検証する。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class KsBridgeRootTests: XCTestCase {

    /// Builder で構築した Section と LabelCell が Native の設定 list に表示される。
    func test_setRoot_構築どおりのSectionとLabelCellが表示される() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A", "B"], ["C"]])
        XCTAssertEqual(KsBridgeTestHost.headerText(attachment, section: 0), "S1")
        XCTAssertEqual(KsBridgeTestHost.headerText(attachment, section: 1), "S2")
    }

    /// `setRoot` の再呼び出しで表示が新しい root へ全置換される。
    func test_setRoot_再呼び出しで表示が全置換される() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A", "B"], ["C"]])

        let builder = KsBridgeRootBuilder()
        let section = builder.addSection(headerText: "NEW", footerText: nil)
        builder.addLabelCell(KsBridgeLabelCell(title: "X"), sectionID: section.sectionID)
        fixture.bridge.setRoot(builder)
        KsBridgeTestHost.awaitRenderedTitles(attachment, equals: [["X"]])

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["X"]])
        XCTAssertEqual(KsBridgeTestHost.headerText(attachment, section: 0), "NEW")
    }

    /// Builder が返した cellID をそのまま更新 API へ渡すと対象 Cell の内容が更新される。
    func test_採番されたcellIDでreplaceCellが反映される() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        fixture.bridge.replaceCell(
            cellID: fixture.cellB.cellID,
            newCell: KsBridgeLabelCell(title: "B-updated")
        )
        KsBridgeTestHost.awaitRenderedTitles(attachment, equals: [["A", "B-updated"], ["C"]])

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A", "B-updated"], ["C"]])
    }

    /// Builder は自分が保持していない sectionID への Cell 追加を no-op として扱う。
    func test_Builder_未知のsectionIDへのaddLabelCellはnilを返し追加されない() {
        let builder = KsBridgeRootBuilder()
        let section = builder.addSection(headerText: "S", footerText: nil)

        let result = builder.addLabelCell(
            KsBridgeLabelCell(title: "A"),
            sectionID: KsBridgeFixture.unusedIdentifier()
        )

        XCTAssertNil(result)
        XCTAssertEqual(section.cells.count, 0)
    }

    /// Bridge が採番していない文字列を ID に渡した Cell 操作は状態も表示も変えない。
    func test_不正なIDのremoveCellはno_op() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)
        let before = KsBridgeTestHost.renderedTitles(attachment)

        fixture.bridge.removeCell(cellID: KsBridgeFixture.unknownIdentifier)
        fixture.bridge.removeCell(cellID: KsBridgeFixture.unusedIdentifier())
        waitForNegativeVerification(in: attachment.collectionView)

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), before)
        XCTAssertEqual(fixture.bridge.store.root.sections.flatMap { $0.cells }.count, 3)
    }
}
#endif
