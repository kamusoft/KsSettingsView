// KsBridgeOperationContractTests.swift
// KsSettingsViewBridgeTests
//
// 全 12 操作を 1 つの表で駆動し、操作後の観察可能な結果 (Host の表示内容と通知) が
// 対応する Store 操作の契約と一致することを検証する。

#if canImport(UIKit)
import XCTest
import UIKit
import Combine
import KsSettingsViewTestSupport
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class KsBridgeOperationContractTests: XCTestCase {

    /// 1 操作分の検証仕様。
    ///
    /// 起点はいずれも標準構成 (Section "S1" に Cell A / B、Section "S2" に Cell C)。
    private struct OperationCase {
        /// 失敗時に操作を特定するためのラベル
        let label: String
        /// 検証対象の操作
        let act: (KsBridgeFixture.Built) -> Void
        /// 操作後に実描画される行タイトル (Section ごと)
        let titles: [[String]]
        /// 操作後に実描画される Section header のテキスト
        let headers: [String?]
        /// 操作によって配信される構造 Diff の件数
        let diffCount: Int
        /// 操作によって配信されるバッチ内容更新の件数
        let batchCount: Int
    }

    /// 標準構成に対する全 12 操作の契約表。代表的な引数に加えて、未知 ID と index の
    /// 丸め込みの境界も操作ごとに含める。
    private static func cases() -> [OperationCase] {
        return [
            // 1. setRoot
            OperationCase(
                label: "setRoot: 別の root で全置換",
                act: { fixture in
                    let builder = KsBridgeRootBuilder()
                    let section = builder.addSection(headerText: "N1", footerText: nil)
                    builder.addLabelCell(KsBridgeLabelCell(title: "X"), sectionID: section.sectionID)
                    fixture.bridge.setRoot(builder)
                },
                titles: [["X"]],
                headers: ["N1"],
                diffCount: 1,
                batchCount: 0
            ),

            // 2. insertSection
            OperationCase(
                label: "insertSection: 先頭へ挿入",
                act: { fixture in
                    let section = KsBridgeSection(headerText: "S0", footerText: nil)
                    section.addCell(KsBridgeLabelCell(title: "D"))
                    fixture.bridge.insertSection(section, at: 0)
                },
                titles: [["D"], ["A", "B"], ["C"]],
                headers: ["S0", "S1", "S2"],
                diffCount: 1,
                batchCount: 0
            ),
            OperationCase(
                label: "insertSection: 範囲外 index は末尾へ丸められる",
                act: { fixture in
                    let section = KsBridgeSection(headerText: "S3", footerText: nil)
                    section.addCell(KsBridgeLabelCell(title: "D"))
                    fixture.bridge.insertSection(section, at: 99)
                },
                titles: [["A", "B"], ["C"], ["D"]],
                headers: ["S1", "S2", "S3"],
                diffCount: 1,
                batchCount: 0
            ),

            // 3. removeSection
            OperationCase(
                label: "removeSection: 既知 ID",
                act: { fixture in
                    fixture.bridge.removeSection(sectionID: fixture.section1.sectionID)
                },
                titles: [["C"]],
                headers: ["S2"],
                diffCount: 1,
                batchCount: 0
            ),
            OperationCase(
                label: "removeSection: 未知 ID は no-op",
                act: { fixture in
                    fixture.bridge.removeSection(sectionID: KsBridgeFixture.unusedIdentifier())
                    fixture.bridge.removeSection(sectionID: KsBridgeFixture.unknownIdentifier)
                },
                titles: [["A", "B"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 0,
                batchCount: 0
            ),

            // 4. moveSection
            OperationCase(
                label: "moveSection: 順序入れ替え",
                act: { fixture in
                    fixture.bridge.moveSection(from: 0, to: 1)
                },
                titles: [["C"], ["A", "B"]],
                headers: ["S2", "S1"],
                diffCount: 1,
                batchCount: 0
            ),
            OperationCase(
                label: "moveSection: 範囲外の移動先は末尾へ丸められる",
                act: { fixture in
                    fixture.bridge.moveSection(from: 0, to: 99)
                },
                titles: [["C"], ["A", "B"]],
                headers: ["S2", "S1"],
                diffCount: 1,
                batchCount: 0
            ),
            OperationCase(
                label: "moveSection: 範囲外の移動元は no-op",
                act: { fixture in
                    fixture.bridge.moveSection(from: 99, to: 0)
                },
                titles: [["A", "B"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 0,
                batchCount: 0
            ),

            // 5. replaceSection
            // 置換後の header text は起点と同じ "S1" に固定する。header text を変える経路には
            // 未修正の再描画不具合があり、この表では Cell 側の置換だけを見る。
            OperationCase(
                label: "replaceSection: 既知 ID (header text は不変)",
                act: { fixture in
                    let replacement = KsBridgeSection(headerText: "S1", footerText: nil)
                    replacement.addCell(KsBridgeLabelCell(title: "Z"))
                    fixture.bridge.replaceSection(sectionID: fixture.section1.sectionID, newSection: replacement)
                },
                titles: [["Z"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 1,
                batchCount: 0
            ),
            OperationCase(
                label: "replaceSection: 未知 ID は no-op",
                act: { fixture in
                    fixture.bridge.replaceSection(
                        sectionID: KsBridgeFixture.unusedIdentifier(),
                        newSection: KsBridgeSection(headerText: "X", footerText: nil)
                    )
                    fixture.bridge.replaceSection(
                        sectionID: KsBridgeFixture.unknownIdentifier,
                        newSection: KsBridgeSection(headerText: "X", footerText: nil)
                    )
                },
                titles: [["A", "B"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 0,
                batchCount: 0
            ),

            // 6. insertCell
            OperationCase(
                label: "insertCell: Section 先頭へ挿入",
                act: { fixture in
                    fixture.bridge.insertCell(
                        KsBridgeLabelCell(title: "A0"),
                        sectionID: fixture.section1.sectionID,
                        at: 0
                    )
                },
                titles: [["A0", "A", "B"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 1,
                batchCount: 0
            ),
            OperationCase(
                label: "insertCell: 範囲外 index は末尾へ丸められる",
                act: { fixture in
                    fixture.bridge.insertCell(
                        KsBridgeLabelCell(title: "A9"),
                        sectionID: fixture.section1.sectionID,
                        at: 99
                    )
                },
                titles: [["A", "B", "A9"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 1,
                batchCount: 0
            ),
            OperationCase(
                label: "insertCell: 未知 sectionID は no-op",
                act: { fixture in
                    fixture.bridge.insertCell(
                        KsBridgeLabelCell(title: "X"),
                        sectionID: KsBridgeFixture.unusedIdentifier(),
                        at: 0
                    )
                    fixture.bridge.insertCell(
                        KsBridgeLabelCell(title: "X"),
                        sectionID: KsBridgeFixture.unknownIdentifier,
                        at: 0
                    )
                },
                titles: [["A", "B"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 0,
                batchCount: 0
            ),

            // 7. removeCell
            OperationCase(
                label: "removeCell: 既知 ID",
                act: { fixture in
                    fixture.bridge.removeCell(cellID: fixture.cellA.cellID)
                },
                titles: [["B"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 1,
                batchCount: 0
            ),
            OperationCase(
                label: "removeCell: 未知 ID は no-op",
                act: { fixture in
                    fixture.bridge.removeCell(cellID: KsBridgeFixture.unusedIdentifier())
                    fixture.bridge.removeCell(cellID: KsBridgeFixture.unknownIdentifier)
                },
                titles: [["A", "B"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 0,
                batchCount: 0
            ),

            // 8. moveCell
            OperationCase(
                label: "moveCell: Section 内で移動",
                act: { fixture in
                    fixture.bridge.moveCell(cellID: fixture.cellA.cellID, to: 1)
                },
                titles: [["B", "A"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 1,
                batchCount: 0
            ),
            OperationCase(
                label: "moveCell: 範囲外 index は末尾へ丸められる",
                act: { fixture in
                    fixture.bridge.moveCell(cellID: fixture.cellA.cellID, to: 99)
                },
                titles: [["B", "A"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 1,
                batchCount: 0
            ),
            OperationCase(
                label: "moveCell: 未知 ID は no-op",
                act: { fixture in
                    fixture.bridge.moveCell(cellID: KsBridgeFixture.unusedIdentifier(), to: 0)
                    fixture.bridge.moveCell(cellID: KsBridgeFixture.unknownIdentifier, to: 0)
                },
                titles: [["A", "B"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 0,
                batchCount: 0
            ),

            // 9. replaceCell
            OperationCase(
                label: "replaceCell: 既知 ID",
                act: { fixture in
                    fixture.bridge.replaceCell(
                        cellID: fixture.cellB.cellID,
                        newCell: KsBridgeLabelCell(title: "B2")
                    )
                },
                titles: [["A", "B2"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 1,
                batchCount: 0
            ),
            OperationCase(
                label: "replaceCell: 未知 ID は no-op",
                act: { fixture in
                    fixture.bridge.replaceCell(
                        cellID: KsBridgeFixture.unusedIdentifier(),
                        newCell: KsBridgeLabelCell(title: "X")
                    )
                    fixture.bridge.replaceCell(
                        cellID: KsBridgeFixture.unknownIdentifier,
                        newCell: KsBridgeLabelCell(title: "X")
                    )
                },
                titles: [["A", "B"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 0,
                batchCount: 0
            ),

            // 10. updateAccessory
            OperationCase(
                label: "updateAccessory: Section header の text 更新",
                act: { fixture in
                    fixture.bridge.updateAccessory(
                        target: .sectionHeader,
                        sectionID: fixture.section1.sectionID,
                        text: "S1-renamed"
                    )
                },
                titles: [["A", "B"], ["C"]],
                headers: ["S1-renamed", "S2"],
                diffCount: 1,
                batchCount: 0
            ),
            OperationCase(
                label: "updateAccessory: canonical UUID でない sectionID は no-op",
                act: { fixture in
                    fixture.bridge.updateAccessory(
                        target: .sectionHeader,
                        sectionID: KsBridgeFixture.unknownIdentifier,
                        text: "X"
                    )
                },
                titles: [["A", "B"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 0,
                batchCount: 0
            ),
            OperationCase(
                label: "updateAccessory: 未使用 sectionID の Section header は no-op",
                act: { fixture in
                    fixture.bridge.updateAccessory(
                        target: .sectionHeader,
                        sectionID: KsBridgeFixture.unusedIdentifier(),
                        text: "X"
                    )
                },
                titles: [["A", "B"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 0,
                batchCount: 0
            ),
            OperationCase(
                label: "updateAccessory: 未使用 sectionID の Section footer は no-op",
                act: { fixture in
                    fixture.bridge.updateAccessory(
                        target: .sectionFooter,
                        sectionID: KsBridgeFixture.unusedIdentifier(),
                        text: "Y"
                    )
                },
                titles: [["A", "B"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 0,
                batchCount: 0
            ),

            // 11. replaceCells
            OperationCase(
                label: "replaceCells: 複数 Cell を 1 バッチで更新",
                act: { fixture in
                    fixture.bridge.replaceCells([
                        KsBridgeCellUpdate(cellID: fixture.cellA.cellID, cell: KsBridgeLabelCell(title: "A2")),
                        KsBridgeCellUpdate(cellID: fixture.cellC.cellID, cell: KsBridgeLabelCell(title: "C2"))
                    ])
                },
                titles: [["A2", "B"], ["C2"]],
                headers: ["S1", "S2"],
                diffCount: 0,
                batchCount: 1
            ),
            OperationCase(
                label: "replaceCells: 未知 ID を含んでも既知分だけが 1 バッチで適用される",
                act: { fixture in
                    fixture.bridge.replaceCells([
                        KsBridgeCellUpdate(
                            cellID: KsBridgeFixture.unusedIdentifier(),
                            cell: KsBridgeLabelCell(title: "X")
                        ),
                        KsBridgeCellUpdate(cellID: fixture.cellB.cellID, cell: KsBridgeLabelCell(title: "B2")),
                        KsBridgeCellUpdate(
                            cellID: KsBridgeFixture.unknownIdentifier,
                            cell: KsBridgeLabelCell(title: "Y")
                        )
                    ])
                },
                titles: [["A", "B2"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 0,
                batchCount: 1
            ),
            OperationCase(
                label: "replaceCells: 未知 ID のみは適用 0 件で配信されない",
                act: { fixture in
                    fixture.bridge.replaceCells([
                        KsBridgeCellUpdate(
                            cellID: KsBridgeFixture.unusedIdentifier(),
                            cell: KsBridgeLabelCell(title: "X")
                        )
                    ])
                },
                titles: [["A", "B"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 0,
                batchCount: 0
            ),
            OperationCase(
                label: "replaceCells: 空リストは no-op",
                act: { fixture in
                    fixture.bridge.replaceCells([])
                },
                titles: [["A", "B"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 0,
                batchCount: 0
            ),

            // 12. setTheme
            OperationCase(
                label: "setTheme: 構造 Diff を発行せず構造も変えない",
                act: { fixture in
                    let theme = KsBridgeTheme()
                    theme.cellTitleColor = NSNumber(value: Int32(bitPattern: 0xFF00FF00))
                    fixture.bridge.setTheme(theme)
                },
                titles: [["A", "B"], ["C"]],
                headers: ["S1", "S2"],
                diffCount: 0,
                batchCount: 0
            )
        ]
    }

    /// 標準構成の起点で実描画される行タイトル。
    private static let baselineTitles = [["A", "B"], ["C"]]

    /// 標準構成の起点で実描画される Section header のテキスト。
    private static let baselineHeaders: [String?] = ["S1", "S2"]

    /// 全 12 操作が契約どおりに反映されることを、観察可能な結果 (表示内容と通知) で検証する。
    func test_全12操作が契約どおりに反映される() {
        for testCase in Self.cases() {
            let fixture = KsBridgeFixture.standard()
            let attachment = KsBridgeTestHost.attach(fixture.bridge)
            XCTAssertEqual(
                KsBridgeTestHost.renderedTitles(attachment),
                Self.baselineTitles,
                "起点の表示が標準構成である: \(testCase.label)"
            )

            var diffs: [SettingsRootDiff] = []
            var batches: [[KsCellID]] = []
            let diffSubscription = fixture.bridge.store.diffPublisher.sink { diffs.append($0) }
            let batchSubscription = fixture.bridge.store.contentUpdateBatchPublisher.sink { batches.append($0) }

            testCase.act(fixture)
            // 表 (契約) の期待値が起点と同じケースは「表示が変わらないこと」の検証であり、
            // 待つべき正の完了条件を持たない (cross/ADR-0027)。期待値が起点と異なるケースだけ
            // 期待の表示へ到達したことを完了条件として待つ。
            let expectsNoVisibleChange = testCase.titles == Self.baselineTitles
                && testCase.headers == Self.baselineHeaders
            if expectsNoVisibleChange {
                waitForNegativeVerification(in: attachment.collectionView)
            } else {
                awaitCondition(
                    "操作後の表示への反映: \(testCase.label)",
                    in: attachment.collectionView,
                    actual: {
                        "行タイトル \(KsBridgeTestHost.renderedTitles(attachment)) / "
                            + "header \(Self.renderedHeaders(attachment))"
                    },
                    until: {
                        KsBridgeTestHost.renderedTitles(attachment) == testCase.titles
                            && Self.renderedHeaders(attachment) == testCase.headers
                    }
                )
            }

            XCTAssertEqual(
                KsBridgeTestHost.renderedTitles(attachment),
                testCase.titles,
                "表示される行タイトル: \(testCase.label)"
            )
            XCTAssertEqual(
                Self.renderedHeaders(attachment),
                testCase.headers,
                "表示される Section header: \(testCase.label)"
            )
            XCTAssertEqual(diffs.count, testCase.diffCount, "構造 Diff の件数: \(testCase.label)")
            XCTAssertEqual(batches.count, testCase.batchCount, "バッチ内容更新の件数: \(testCase.label)")

            diffSubscription.cancel()
            batchSubscription.cancel()
        }
    }

    /// 実描画されている Section header のテキストを Section 順に返す。
    private static func renderedHeaders(_ attachment: KsBridgeTestHost.Attachment) -> [String?] {
        return (0..<attachment.collectionView.numberOfSections).map {
            KsBridgeTestHost.headerText(attachment, section: $0)
        }
    }
}
#endif
