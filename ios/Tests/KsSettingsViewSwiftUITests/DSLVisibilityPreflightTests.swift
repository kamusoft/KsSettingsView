// DSLVisibilityPreflightTests.swift
// KsSettingsViewSwiftUITests
//
// 可視性変化の preflight 検出テスト。`DSLDiffCalculator.compute(from:to:)` が同一 ID で
// `Section.isVisible` または `Cell.isVisible` の変化を検出した場合、`.full(newRoot)` のみを
// 発行することを検証する。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewCore
@testable import KsSettingsViewUI

final class DSLVisibilityPreflightTests: XCTestCase {

    private func makeTree(_ sections: [KsSettingsViewCore.Section]) -> DSLDiffCalculator.ResolvedTree {
        return DSLDiffCalculator.ResolvedTree(sections: sections)
    }

    // MARK: - Cell.isVisible の preflight

    func test_Cell_isVisible_変化のみで_full_発行() {
        let sectionID = UUID()
        let cellID = UUID()
        let old = makeTree([
            KsSettingsViewCore.Section(id: sectionID, cells: [
                LabelCell(id: cellID, title: "A", isVisible: true)
            ])
        ])
        let new = makeTree([
            KsSettingsViewCore.Section(id: sectionID, cells: [
                LabelCell(id: cellID, title: "A", isVisible: false)
            ])
        ])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        switch diffs.first {
        case .full:
            break
        default:
            XCTFail("expected .full diff, got \(String(describing: diffs.first))")
        }
    }

    func test_Cell_isVisible_変化_と_内容変化_で_full_発行() {
        let sectionID = UUID()
        let cellID = UUID()
        let old = makeTree([
            KsSettingsViewCore.Section(id: sectionID, cells: [
                LabelCell(id: cellID, title: "旧", isVisible: true)
            ])
        ])
        let new = makeTree([
            KsSettingsViewCore.Section(id: sectionID, cells: [
                LabelCell(id: cellID, title: "新", isVisible: false)
            ])
        ])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        if case .full = diffs.first {} else {
            XCTFail("expected .full only; contents change must be subsumed into .full")
        }
    }

    // MARK: - Section.isVisible の preflight

    func test_Section_isVisible_変化のみで_full_発行() {
        let sectionID = UUID()
        let cellID = UUID()
        let old = makeTree([
            KsSettingsViewCore.Section(
                id: sectionID,
                header: .text("一般"),
                cells: [LabelCell(id: cellID, title: "A")],
                isVisible: true
            )
        ])
        let new = makeTree([
            KsSettingsViewCore.Section(
                id: sectionID,
                header: .text("一般"),
                cells: [LabelCell(id: cellID, title: "A")],
                isVisible: false
            )
        ])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        if case .full = diffs.first {} else {
            XCTFail("expected .full only")
        }
    }

    // MARK: - 可視性無変化なら通常 Diff

    func test_可視性無変化なら通常の_Diff() {
        let sectionID = UUID()
        let cellID = UUID()
        let old = makeTree([
            KsSettingsViewCore.Section(id: sectionID, cells: [
                LabelCell(id: cellID, title: "旧", isVisible: true)
            ])
        ])
        let new = makeTree([
            KsSettingsViewCore.Section(id: sectionID, cells: [
                LabelCell(id: cellID, title: "新", isVisible: true)
            ])
        ])
        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        // 内容変化のみ → replaceCell 一件
        XCTAssertEqual(diffs.count, 1)
        if case .replaceCell = diffs.first {} else {
            XCTFail("expected .replaceCell")
        }
    }

    // MARK: - containsVisibilityChange helper 単体

    func test_containsVisibilityChange_Section_変化_検出() {
        let sectionID = UUID()
        let old = [KsSettingsViewCore.Section(id: sectionID, isVisible: true)]
        let new = [KsSettingsViewCore.Section(id: sectionID, isVisible: false)]
        XCTAssertTrue(DSLDiffCalculator.containsVisibilityChange(from: old, to: new))
    }

    func test_containsVisibilityChange_変化なしなら_false() {
        let sectionID = UUID()
        let old = [KsSettingsViewCore.Section(id: sectionID, isVisible: true)]
        let new = [KsSettingsViewCore.Section(id: sectionID, isVisible: true)]
        XCTAssertFalse(DSLDiffCalculator.containsVisibilityChange(from: old, to: new))
    }
}
#endif
