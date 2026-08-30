// KsBridgeUpdateTests.swift
// KsSettingsViewBridgeTests
//
// Store 公開操作と 1:1 対応する更新 API が表示へ反映されることを検証する。

#if canImport(UIKit)
import XCTest
import UIKit
import Combine
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class KsBridgeUpdateTests: XCTestCase {

    /// Cell の挿入と削除が表示へ反映される。
    func test_insertCellとremoveCellが表示へ反映される() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        let inserted = KsBridgeLabelCell(title: "A2")
        let insertedID = fixture.bridge.insertCell(inserted, sectionID: fixture.section1.sectionID, at: 1)
        fixture.bridge.removeCell(cellID: fixture.cellC.cellID)
        KsBridgeTestHost.pump(attachment)

        XCTAssertEqual(insertedID, inserted.cellID)
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A", "A2", "B"], []])
    }

    /// Section の挿入・並べ替え・削除が表示へ反映される。
    func test_insertSectionとmoveSectionとremoveSectionが表示へ反映される() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        let newSection = KsBridgeSection(headerText: "S3", footerText: nil)
        newSection.addCell(KsBridgeLabelCell(title: "D"))
        let insertedID = fixture.bridge.insertSection(newSection, at: 0)
        XCTAssertEqual(insertedID, newSection.sectionID)
        KsBridgeTestHost.pump(attachment)
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["D"], ["A", "B"], ["C"]])

        fixture.bridge.moveSection(from: 0, to: 2)
        KsBridgeTestHost.pump(attachment)
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A", "B"], ["C"], ["D"]])

        fixture.bridge.removeSection(sectionID: fixture.section2.sectionID)
        KsBridgeTestHost.pump(attachment)
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A", "B"], ["D"]])
    }

    /// Section の内容置換は sectionID の identity を保ったまま反映される。
    func test_replaceSection_はsectionIDのidentityを保つ() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        let replacement = KsBridgeSection(headerText: "S1-new", footerText: nil)
        replacement.addCell(KsBridgeLabelCell(title: "Z"))
        fixture.bridge.replaceSection(sectionID: fixture.section1.sectionID, newSection: replacement)
        KsBridgeTestHost.pump(attachment)

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["Z"], ["C"]])
        XCTAssertEqual(
            attachment.controller.internalDataSource?.snapshot().sectionIdentifiers.first,
            fixture.section1.identifier,
            "置換後も Section の identity は sectionID のまま保たれる"
        )
        XCTAssertEqual(
            fixture.bridge.store.root.sections.first?.header,
            .text("S1-new"),
            "置換後の header が Store の状態へ写る"
        )
    }

    /// replace 系が返す ID で後続操作が通り、渡した DTO 自身の ID は破棄される。
    func test_replace系が返すIDで後続操作ができる() throws {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        let replacement = KsBridgeSection(headerText: "S1", footerText: nil)
        replacement.addCell(KsBridgeLabelCell(title: "Z"))
        let replacedSectionID = try XCTUnwrap(
            fixture.bridge.replaceSection(
                sectionID: fixture.section1.sectionID,
                newSection: replacement
            )
        )
        XCTAssertEqual(replacedSectionID, fixture.section1.sectionID, "戻り値は対象の sectionID")
        XCTAssertNotEqual(
            replacedSectionID,
            replacement.sectionID,
            "渡した DTO 自身の sectionID は破棄される"
        )

        let appended = KsBridgeLabelCell(title: "Z2")
        let appendedID = try XCTUnwrap(
            fixture.bridge.insertCell(appended, sectionID: replacedSectionID, at: 99)
        )
        KsBridgeTestHost.pump(attachment)
        XCTAssertEqual(
            KsBridgeTestHost.renderedTitles(attachment),
            [["Z", "Z2"], ["C"]],
            "replaceSection の戻り値 ID を挿入先に指定できる"
        )

        let replacedCellID = try XCTUnwrap(
            fixture.bridge.replaceCell(cellID: appendedID, newCell: KsBridgeLabelCell(title: "Z3"))
        )
        XCTAssertEqual(replacedCellID, appendedID, "戻り値は対象の cellID")
        KsBridgeTestHost.pump(attachment)
        XCTAssertEqual(
            KsBridgeTestHost.renderedTitles(attachment),
            [["Z", "Z3"], ["C"]],
            "replaceCell の戻り値 ID でさらに置換できる"
        )

        let replacedAgain = fixture.bridge.replaceCell(
            cellID: replacedCellID,
            newCell: KsBridgeLabelCell(title: "Z4")
        )
        XCTAssertEqual(replacedAgain, replacedCellID, "戻り値の ID は何度でも使える")
    }

    /// 対象が存在しない replace 系は `nil` を返し、状態も表示も変えない。
    func test_replace系は対象が存在しないときnilを返す() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        XCTAssertNil(
            fixture.bridge.replaceSection(
                sectionID: KsBridgeFixture.unusedIdentifier(),
                newSection: KsBridgeSection(headerText: "X", footerText: nil)
            )
        )
        XCTAssertNil(
            fixture.bridge.replaceSection(
                sectionID: KsBridgeFixture.unknownIdentifier,
                newSection: KsBridgeSection(headerText: "X", footerText: nil)
            )
        )
        XCTAssertNil(
            fixture.bridge.replaceCell(
                cellID: KsBridgeFixture.unusedIdentifier(),
                newCell: KsBridgeLabelCell(title: "X")
            )
        )
        XCTAssertNil(
            fixture.bridge.replaceCell(
                cellID: KsBridgeFixture.unknownIdentifier,
                newCell: KsBridgeLabelCell(title: "X")
            )
        )
        KsBridgeTestHost.pump(attachment)

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A", "B"], ["C"]])
    }

    /// 同じ cellID への内容置換は行の identity を維持し、削除+挿入として扱われない。
    func test_replaceCell_は行のidentityを維持する() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        let beforeItems = attachment.controller.internalDataSource?.snapshot().itemIdentifiers ?? []
        let firstRowBefore = attachment.collectionView.cellForItem(at: IndexPath(item: 0, section: 0))

        fixture.bridge.replaceCell(
            cellID: fixture.cellA.cellID,
            newCell: KsBridgeLabelCell(title: "A2")
        )
        KsBridgeTestHost.pump(attachment)

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A2", "B"], ["C"]])
        XCTAssertEqual(
            attachment.controller.internalDataSource?.snapshot().itemIdentifiers ?? [],
            beforeItems,
            "行の集合・順序は変化しない"
        )
        XCTAssertTrue(
            firstRowBefore === attachment.collectionView.cellForItem(at: IndexPath(item: 0, section: 0)),
            "同一の行が再構成される"
        )
    }

    /// 複数 Cell の内容更新が 1 回のバッチ内容更新として反映される。
    func test_replaceCells_が1バッチで反映される() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        var batches: [[KsCellID]] = []
        let subscription = fixture.bridge.store.contentUpdateBatchPublisher.sink { batches.append($0) }
        defer { subscription.cancel() }

        let beforeItems = attachment.controller.internalDataSource?.snapshot().itemIdentifiers ?? []
        fixture.bridge.replaceCells([
            KsBridgeCellUpdate(cellID: fixture.cellA.cellID, cell: KsBridgeLabelCell(title: "A2")),
            KsBridgeCellUpdate(cellID: fixture.cellC.cellID, cell: KsBridgeLabelCell(title: "C2"))
        ])
        KsBridgeTestHost.pump(attachment)

        XCTAssertEqual(batches.count, 1, "1 回のバッチとして配信される")
        XCTAssertEqual(batches.first?.count, 2)
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A2", "B"], ["C2"]])
        XCTAssertEqual(
            attachment.controller.internalDataSource?.snapshot().itemIdentifiers ?? [],
            beforeItems,
            "構造変更は発生しない"
        )
    }

    /// 未知 ID だけの `replaceCells` は適用 0 件となり配信されない。
    func test_replaceCells_未知IDのみでは配信されず表示も変わらない() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        var batches: [[KsCellID]] = []
        let subscription = fixture.bridge.store.contentUpdateBatchPublisher.sink { batches.append($0) }
        defer { subscription.cancel() }

        fixture.bridge.replaceCells([
            KsBridgeCellUpdate(
                cellID: KsBridgeFixture.unusedIdentifier(),
                cell: KsBridgeLabelCell(title: "X")
            ),
            KsBridgeCellUpdate(
                cellID: KsBridgeFixture.unknownIdentifier,
                cell: KsBridgeLabelCell(title: "Y")
            )
        ])
        KsBridgeTestHost.pump(attachment)

        XCTAssertEqual(batches.count, 0)
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A", "B"], ["C"]])
    }

    /// Cell の移動が表示へ反映される。
    func test_moveCell_が表示へ反映される() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        fixture.bridge.moveCell(cellID: fixture.cellA.cellID, to: 1)
        KsBridgeTestHost.pump(attachment)

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["B", "A"], ["C"]])
    }

    /// Section header / footer の text 更新と解除が表示へ反映される。
    func test_updateAccessory_のtext更新と解除が表示へ反映される() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        fixture.bridge.updateAccessory(
            target: .sectionHeader,
            sectionID: fixture.section1.sectionID,
            text: "S1-renamed"
        )
        fixture.bridge.updateAccessory(
            target: .sectionFooter,
            sectionID: fixture.section1.sectionID,
            text: "footer"
        )
        KsBridgeTestHost.pump(attachment)

        XCTAssertEqual(KsBridgeTestHost.headerText(attachment, section: 0), "S1-renamed")
        XCTAssertEqual(KsBridgeTestHost.footerText(attachment, section: 0), "footer")

        fixture.bridge.updateAccessory(
            target: .sectionHeader,
            sectionID: fixture.section1.sectionID,
            text: nil
        )
        KsBridgeTestHost.pump(attachment)

        XCTAssertNil(KsBridgeTestHost.headerText(attachment, section: 0),
                     "clear 後は accessory が指定されていない場合と同じ表示になる")
    }

    /// Bridge が採番していない canonical UUID の `updateAccessory` は、header / footer とも
    /// 状態も表示も変えない。
    ///
    /// Bridge は `updateAccessory` を Store へ素通しするため、この no-op は Store 側の契約
    /// (core/ADR-0020) がそのまま interop 表面に現れたものである。表示が変化しないことに加えて
    /// 後続操作が表示へ届くことまで確認し、Host の Diff 購読が生きていることも観察する。
    func test_updateAccessory_の未使用sectionIDはno_opになる() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        let unusedID = KsBridgeFixture.unusedIdentifier()
        fixture.bridge.updateAccessory(target: .sectionHeader, sectionID: unusedID, text: "X")
        fixture.bridge.updateAccessory(target: .sectionFooter, sectionID: unusedID, text: "Y")
        KsBridgeTestHost.pump(attachment)

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A", "B"], ["C"]])
        XCTAssertEqual(KsBridgeTestHost.headerText(attachment, section: 0), "S1")
        XCTAssertEqual(KsBridgeTestHost.headerText(attachment, section: 1), "S2")
        XCTAssertNil(KsBridgeTestHost.footerText(attachment, section: 0))
        XCTAssertEqual(
            fixture.bridge.store.root.sections.map { $0.header },
            [.text("S1"), .text("S2")],
            "Store の現在状態も変化しない"
        )

        fixture.bridge.replaceCell(cellID: fixture.cellA.cellID, newCell: KsBridgeLabelCell(title: "A2"))
        KsBridgeTestHost.pump(attachment)

        XCTAssertEqual(
            KsBridgeTestHost.renderedTitles(attachment),
            [["A2", "B"], ["C"]],
            "後続操作が表示へ届く (Host の Diff 購読が生きている)"
        )
    }

    /// Root header / footer の text 更新が表示へ反映される。
    func test_updateAccessory_のroot対象が表示へ反映される() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        fixture.bridge.updateAccessory(target: .rootHeader, sectionID: nil, text: "ROOT-H")
        fixture.bridge.updateAccessory(target: .rootFooter, sectionID: nil, text: "ROOT-F")
        KsBridgeTestHost.pump(attachment)

        XCTAssertEqual(attachment.controller.rootHeader, .text("ROOT-H"))
        XCTAssertEqual(attachment.controller.rootFooter, .text("ROOT-F"))
    }
}
#endif
