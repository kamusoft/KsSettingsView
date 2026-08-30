// FullSnapshotContentTargetsTests.swift
// KsSettingsViewUITests
//
// full snapshot 適用時の内容再適用の対象選定 (`FullSnapshotContentTargets.compute`) を、
// 返却される ID 集合そのもので検証する。
//
// 対象選定の契約は「旧・新 visible projection の双方に存在し、値が変わった Cell に限る」
// であり、全件を無条件に対象へ含める実装や、新規挿入・削除・hidden の Cell を巻き込む実装は
// 本テスト群で落ちる。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class FullSnapshotContentTargetsTests: XCTestCase {

    /// model の Section 群から visible projection を作る（Host が snapshot 構築に使うものと同じ）。
    private func projection(
        _ sections: [KsSettingsViewCore.Section]
    ) -> [KsSettingsViewCore.Section] {
        return KsSettingsViewController.computeVisibleSections(from: sections)
    }

    /// 対象 ID 群を UUID 集合として取り出す（順序に依存しない比較のため）。
    private func ids(_ cellIDs: [KsCellID]) -> Set<UUID> {
        return Set(cellIDs.map { $0.id })
    }

    // MARK: - 境界: 初回適用・完全同値

    func test_旧projectionが空なら対象は空になる() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let new = projection([Section(header: .text("S"), cells: [cellA, cellB])])

        let targets = FullSnapshotContentTargets.compute(
            oldVisible: [],
            newVisible: new,
            reloadSectionIDs: []
        )

        XCTAssertTrue(targets.isEmpty, "初回適用は旧側に照合先が無く、内容再適用の対象を持たない")
    }

    func test_完全同値なら対象は空になる() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let sections = [Section(header: .text("S"), cells: [cellA, cellB])]

        let targets = FullSnapshotContentTargets.compute(
            oldVisible: projection(sections),
            newVisible: projection(sections),
            reloadSectionIDs: []
        )

        XCTAssertTrue(targets.isEmpty, "値が変わっていない Cell を対象へ含めてはならない")
    }

    func test_内容が変わったCellだけが対象になる() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let cellC = LabelCell(title: "C")
        let sectionID = UUID()
        let old = projection([
            Section(id: sectionID, header: .text("S"), cells: [cellA, cellB, cellC]),
        ])
        let new = projection([
            Section(id: sectionID, header: .text("S"), cells: [
                LabelCell(id: cellA.id, title: "A2"),
                cellB,
                LabelCell(id: cellC.id, title: "C2"),
            ]),
        ])

        let targets = FullSnapshotContentTargets.compute(
            oldVisible: old,
            newVisible: new,
            reloadSectionIDs: []
        )

        XCTAssertEqual(ids(targets.reconfigure), [cellA.id, cellC.id],
                       "内容が変わった Cell だけが reconfigure の対象になる")
        XCTAssertEqual(targets.reconfigure.count, 2,
                       "同じ Cell が複数回積まれている (内容再適用は 1 Cell につき 1 度)")
        XCTAssertTrue(targets.reload.isEmpty)
    }

    // MARK: - 境界: 新規挿入・削除

    func test_新規挿入されたCellは対象にならない() {
        let cellA = LabelCell(title: "A")
        let inserted = LabelCell(title: "NEW")
        let sectionID = UUID()
        let old = projection([Section(id: sectionID, cells: [cellA])])
        let new = projection([
            Section(id: sectionID, cells: [LabelCell(id: cellA.id, title: "A2"), inserted]),
        ])

        let targets = FullSnapshotContentTargets.compute(
            oldVisible: old,
            newVisible: new,
            reloadSectionIDs: []
        )

        XCTAssertEqual(ids(targets.reconfigure), [cellA.id],
                       "新規挿入 Cell は構造反映の通常 bind で最新内容になるため対象へ含めない")
        XCTAssertFalse(ids(targets.reconfigure).contains(inserted.id))
    }

    func test_削除されたCellは対象にならない() {
        let cellA = LabelCell(title: "A")
        let removed = LabelCell(title: "GONE")
        let sectionID = UUID()
        let old = projection([Section(id: sectionID, cells: [cellA, removed])])
        let new = projection([Section(id: sectionID, cells: [LabelCell(id: cellA.id, title: "A2")])])

        let targets = FullSnapshotContentTargets.compute(
            oldVisible: old,
            newVisible: new,
            reloadSectionIDs: []
        )

        XCTAssertEqual(ids(targets.reconfigure), [cellA.id])
        XCTAssertFalse(ids(targets.reconfigure).contains(removed.id),
                       "新 projection に存在しない Cell を対象へ含めてはならない")
    }

    // MARK: - 境界: 可視性切替

    func test_hiddenから可視へ戻ったCellは対象にならない() {
        let visibleCell = LabelCell(title: "見えている")
        let toggledCell = LabelCell(title: "隠れていた", isVisible: false)
        let sectionID = UUID()
        let old = projection([
            Section(id: sectionID, cells: [visibleCell, toggledCell]),
        ])
        let new = projection([
            Section(id: sectionID, cells: [
                LabelCell(id: visibleCell.id, title: "見えている2"),
                LabelCell(id: toggledCell.id, title: "隠れていた2", isVisible: true),
            ]),
        ])

        let targets = FullSnapshotContentTargets.compute(
            oldVisible: old,
            newVisible: new,
            reloadSectionIDs: []
        )

        XCTAssertEqual(ids(targets.reconfigure), [visibleCell.id],
                       "hidden から現れた Cell は構造反映の通常 bind で最新内容になる")
    }

    func test_可視からhiddenへ変わったCellは対象にならない() {
        let stayingCell = LabelCell(title: "残る")
        let hidingCell = LabelCell(title: "隠れる")
        let sectionID = UUID()
        let old = projection([Section(id: sectionID, cells: [stayingCell, hidingCell])])
        let new = projection([
            Section(id: sectionID, cells: [
                LabelCell(id: stayingCell.id, title: "残る2"),
                LabelCell(id: hidingCell.id, title: "隠れる2", isVisible: false),
            ]),
        ])

        let targets = FullSnapshotContentTargets.compute(
            oldVisible: old,
            newVisible: new,
            reloadSectionIDs: []
        )

        XCTAssertEqual(ids(targets.reconfigure), [stayingCell.id])
        XCTAssertFalse(ids(targets.reconfigure).contains(hidingCell.id),
                       "新 projection から外れた Cell を対象へ含めてはならない")
    }

    func test_hiddenだったSectionの再表示ではその中のCellは対象にならない() {
        let visibleCell = LabelCell(title: "常時表示")
        let cellInSection = LabelCell(title: "旧")
        let visibleSectionID = UUID()
        let toggledSectionID = UUID()
        let old = projection([
            Section(id: visibleSectionID, cells: [visibleCell]),
            Section(id: toggledSectionID, cells: [cellInSection], isVisible: false),
        ])
        let new = projection([
            Section(id: visibleSectionID, cells: [LabelCell(id: visibleCell.id, title: "常時表示2")]),
            Section(id: toggledSectionID,
                    cells: [LabelCell(id: cellInSection.id, title: "新")],
                    isVisible: true),
        ])

        let targets = FullSnapshotContentTargets.compute(
            oldVisible: old,
            newVisible: new,
            reloadSectionIDs: []
        )

        XCTAssertEqual(ids(targets.reconfigure), [visibleCell.id],
                       "hidden Section 内の Cell は旧 projection に載っていないため対象外")
    }

    // MARK: - 境界: 移動と内容変更の複合

    func test_移動しつつ内容が変わったCellは対象になる() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let cellC = LabelCell(title: "C")
        let sectionID = UUID()
        let old = projection([Section(id: sectionID, cells: [cellA, cellB, cellC])])
        // C を先頭へ移動しつつ内容も変更。B は移動のみ、A は移動のみで内容不変。
        let new = projection([
            Section(id: sectionID, cells: [
                LabelCell(id: cellC.id, title: "C2"),
                cellA,
                cellB,
            ]),
        ])

        let targets = FullSnapshotContentTargets.compute(
            oldVisible: old,
            newVisible: new,
            reloadSectionIDs: []
        )

        XCTAssertEqual(ids(targets.reconfigure), [cellC.id],
                       "位置の変化は構造同期の担当であり、内容が変わった Cell だけが対象になる")
    }

    func test_別Sectionへ移った同一IDのCellも内容変化があれば対象になる() {
        let moving = LabelCell(title: "移動する")
        let sectionAID = UUID()
        let sectionBID = UUID()
        let old = projection([
            Section(id: sectionAID, cells: [moving]),
            Section(id: sectionBID, cells: []),
        ])
        let new = projection([
            Section(id: sectionAID, cells: []),
            Section(id: sectionBID, cells: [LabelCell(id: moving.id, title: "移動した")]),
        ])

        let targets = FullSnapshotContentTargets.compute(
            oldVisible: old,
            newVisible: new,
            reloadSectionIDs: []
        )

        XCTAssertEqual(ids(targets.reconfigure), [moving.id])
    }

    // MARK: - 境界: reload 対象 Section の除外

    func test_reload対象Sectionの内容変化Cellはreconfigureの対象から外れる() {
        let cellInReloaded = LabelCell(title: "H 変更 Section の Cell")
        let cellInPlain = LabelCell(title: "通常 Section の Cell")
        let reloadedSectionID = UUID()
        let plainSectionID = UUID()
        let old = projection([
            Section(id: reloadedSectionID, header: .text("旧"), cells: [cellInReloaded]),
            Section(id: plainSectionID, header: .text("固定"), cells: [cellInPlain]),
        ])
        let new = projection([
            Section(id: reloadedSectionID, header: .text("新"),
                    cells: [LabelCell(id: cellInReloaded.id, title: "H 変更 Section の Cell2")]),
            Section(id: plainSectionID, header: .text("固定"),
                    cells: [LabelCell(id: cellInPlain.id, title: "通常 Section の Cell2")]),
        ])

        let targets = FullSnapshotContentTargets.compute(
            oldVisible: old,
            newVisible: new,
            reloadSectionIDs: [reloadedSectionID]
        )

        XCTAssertEqual(ids(targets.reconfigure), [cellInPlain.id],
                       "Section 全体が再構成される側の Cell へ内容再適用を重ねない")
    }

    // MARK: - 境界: 具象型の変更

    func test_同一IDで具象型が変わったCellはreloadへ分離される() {
        let sharedID = UUID()
        let stayingCell = LabelCell(title: "そのまま")
        let sectionID = UUID()
        let old = projection([
            Section(id: sectionID, cells: [LabelCell(id: sharedID, title: "ラベル"), stayingCell]),
        ])
        let new = projection([
            Section(id: sectionID, cells: [
                SwitchCell(id: sharedID, title: "スイッチ"),
                LabelCell(id: stayingCell.id, title: "そのまま2"),
            ]),
        ])

        let targets = FullSnapshotContentTargets.compute(
            oldVisible: old,
            newVisible: new,
            reloadSectionIDs: []
        )

        XCTAssertEqual(ids(targets.reload), [sharedID],
                       "具象型が変われば Native cell を維持できないため reload 側へ分ける")
        XCTAssertEqual(ids(targets.reconfigure), [stayingCell.id],
                       "型が同じで内容だけ変わった Cell は reconfigure のまま")
    }

    func test_具象型が同じなら値が変わってもreloadは空のままになる() {
        let cell = SwitchCell(title: "スイッチ", isOn: false)
        let sectionID = UUID()
        let old = projection([Section(id: sectionID, cells: [cell])])
        let new = projection([
            Section(id: sectionID, cells: [SwitchCell(id: cell.id, title: "スイッチ", isOn: true)]),
        ])

        let targets = FullSnapshotContentTargets.compute(
            oldVisible: old,
            newVisible: new,
            reloadSectionIDs: []
        )

        XCTAssertEqual(ids(targets.reconfigure), [cell.id])
        XCTAssertTrue(targets.reload.isEmpty, "同一具象型の値変化で Native cell を交換しない")
    }
}
#endif
