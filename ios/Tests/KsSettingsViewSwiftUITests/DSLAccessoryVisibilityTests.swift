// DSLAccessoryVisibilityTests.swift
// KsSettingsViewSwiftUITests
//
// 宣言 DSL における Section Header / Footer 表示トグルの指定・転写・再評価反映と、
// Store 経路との観測結果の対称性を検証する (core/ADR-0023 / core/ADR-0018)。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewCore
@testable import KsSettingsViewUI

@MainActor
final class DSLAccessoryVisibilityTests: XCTestCase {

    // MARK: - ヘルパ

    /// DSL を評価し、安定 ID 解決済みの `[Section]` を返す。
    private func evaluate(
        @KsSettingsViewBuilder _ builder: () -> [DSLSectionNode]
    ) -> [KsSettingsViewCore.Section] {
        DSLHintRegistry.shared.reset()
        let nodes = builder()
        return DSLRootTree(sectionNodes: nodes).resolvedSections()
    }

    private func makeTree(_ sections: [KsSettingsViewCore.Section]) -> DSLDiffCalculator.ResolvedTree {
        return DSLDiffCalculator.ResolvedTree(sections: sections)
    }

    /// Store 接続済み controller を window に載せ、レイアウトを確定させる。
    private func hostInWindow(
        store: SettingsRootStore
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let controller = KsSettingsViewController(store: store)
        let size = CGSize(width: 375, height: 600)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        let rootView = controller.view!
        rootView.frame = CGRect(origin: .zero, size: size)
        rootView.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        pump(cv)
        return (controller, cv, window)
    }

    private func pump(_ view: UIView, seconds: TimeInterval = 0.05) {
        view.setNeedsLayout()
        view.layoutIfNeeded()
        RunLoop.current.run(until: Date().addingTimeInterval(seconds))
        view.setNeedsLayout()
        view.layoutIfNeeded()
    }

    private func hasHeaderArea(_ cv: UICollectionView, section: Int) -> Bool {
        return cv.collectionViewLayout.layoutAttributesForSupplementaryView(
            ofKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: section)
        ) != nil
    }

    private func hasFooterArea(_ cv: UICollectionView, section: Int) -> Bool {
        return cv.collectionViewLayout.layoutAttributesForSupplementaryView(
            ofKind: UICollectionView.elementKindSectionFooter,
            at: IndexPath(item: 0, section: section)
        ) != nil
    }

    /// `SettingsRootDiff` 列を Store の対応操作へ流し込む (DSL 適用経路と同じ変換)。
    private func applyDiffs(_ diffs: [SettingsRootDiff], to store: SettingsRootStore) {
        for diff in diffs {
            switch diff {
            case .full(let root):
                store.replaceAll(root)
            case let .insertSection(index, section):
                store.insertSection(section, at: index)
            case .removeSection(let sectionID):
                store.removeSection(sectionID: sectionID)
            case let .moveSection(from, to):
                store.moveSection(from: from, to: to)
            case let .replaceSection(sectionID, new):
                store.replaceSection(sectionID: sectionID, new: new)
            case let .insertCell(sectionID, index, cell):
                store.insertCell(cell, in: sectionID, at: index)
            case .removeCell(let cellID):
                store.removeCell(cellID: cellID)
            case let .replaceCell(cellID, new):
                store.replaceCell(cellID: cellID, new: new)
            case let .moveCell(cellID, to):
                store.moveCell(cellID: cellID, to: to)
            case let .updateAccessory(target, accessory):
                store.updateAccessory(target: target, accessory: accessory)
            }
        }
    }

    // MARK: - DSL 構築 API からの転写

    func test_ksSectionのトグル引数がresolvedSectionへ転写される() {
        let sections = evaluate {
            ksSection("一般", footer: "補足", isHeaderVisible: false, isFooterVisible: false) {
                LabelCell(title: "A")
            }
        }
        XCTAssertEqual(sections.count, 1)
        XCTAssertFalse(sections[0].isHeaderVisible, "ksSection の isHeaderVisible が転写されていない")
        XCTAssertFalse(sections[0].isFooterVisible, "ksSection の isFooterVisible が転写されていない")
    }

    func test_SectionのDSLイニシャライザのトグル引数がresolvedSectionへ転写される() {
        let sections = evaluate {
            Section("一般", footer: "補足", isHeaderVisible: false) {
                LabelCell(title: "A")
            }
        }
        XCTAssertEqual(sections.count, 1)
        XCTAssertFalse(sections[0].isHeaderVisible)
        XCTAssertTrue(sections[0].isFooterVisible, "指定しない側は既定 true のままでなければならない")
    }

    func test_トグル指定なしのDSL構築では既定値trueになる() {
        let sections = evaluate {
            ksSection("一般", footer: "補足") { LabelCell(title: "A") }
        }
        XCTAssertTrue(sections[0].isHeaderVisible)
        XCTAssertTrue(sections[0].isFooterVisible)
    }

    /// `SectionAccessory` 直指定版の `ksSection` でも転写される。
    func test_accessory直指定のksSectionでもトグルが転写される() {
        let sections = evaluate {
            ksSection(
                header: .text("一般"),
                footer: .text("補足"),
                isHeaderVisible: false,
                isFooterVisible: false
            ) {
                LabelCell(title: "A")
            }
        }
        XCTAssertFalse(sections[0].isHeaderVisible)
        XCTAssertFalse(sections[0].isFooterVisible)
    }

    /// Section modifier (`sectionHeader` / `sectionFooter`) は Section を再構築するため、
    /// トグル値が既定 true へ戻らないことを確認する。
    func test_Section_modifierをまたいでトグルが保持される() {
        let sections = evaluate {
            ksSection(isHeaderVisible: false, isFooterVisible: false) { LabelCell(title: "A") }
                .sectionHeader("一般")
                .sectionFooter("補足")
        }
        XCTAssertFalse(sections[0].isHeaderVisible, "sectionHeader modifier でトグルが失われている")
        XCTAssertFalse(sections[0].isFooterVisible, "sectionFooter modifier でトグルが失われている")
        XCTAssertEqual(sections[0].header, .text("一般"))
        XCTAssertEqual(sections[0].footer, .text("補足"))
    }

    // MARK: - 再評価による差分検出

    func test_Headerトグルのみの変化でfullが発行される() {
        let sectionID = UUID()
        let cell = LabelCell(title: "A")
        let old = makeTree([KsSettingsViewCore.Section(
            id: sectionID, header: .text("一般"), cells: [cell]
        )])
        let new = makeTree([KsSettingsViewCore.Section(
            id: sectionID, header: .text("一般"), cells: [cell], isHeaderVisible: false
        )])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1, "トグルのみの変更では .full 1 件だけが発行される")
        guard case .full(let root) = diffs[0] else {
            XCTFail("expected .full, got \(diffs[0])")
            return
        }
        XCTAssertEqual(root.sections.first?.isHeaderVisible, false,
                       "新しいトグル値を載せた .full でなければならない")
    }

    func test_Footerトグルのみの変化でfullが発行される() {
        let sectionID = UUID()
        let cell = LabelCell(title: "A")
        let old = makeTree([KsSettingsViewCore.Section(
            id: sectionID, footer: .text("補足"), cells: [cell]
        )])
        let new = makeTree([KsSettingsViewCore.Section(
            id: sectionID, footer: .text("補足"), cells: [cell], isFooterVisible: false
        )])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        guard case .full(let root) = diffs[0] else {
            XCTFail("expected .full, got \(diffs[0])")
            return
        }
        XCTAssertEqual(root.sections.first?.isFooterVisible, false)
    }

    /// トグル変化と Cell 内容変化が同じ再評価で重なっても、発行は `.full` 1 件だけになる。
    func test_トグル変化とCell内容変化の併発でもfullのみが発行される() {
        let sectionID = UUID()
        let cellID = UUID()
        let old = makeTree([KsSettingsViewCore.Section(
            id: sectionID, header: .text("一般"), cells: [LabelCell(id: cellID, title: "旧")]
        )])
        let new = makeTree([KsSettingsViewCore.Section(
            id: sectionID,
            header: .text("一般"),
            cells: [LabelCell(id: cellID, title: "新")],
            isHeaderVisible: false
        )])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        if case .full = diffs[0] {} else {
            XCTFail("expected .full only, got \(diffs[0])")
        }
    }

    /// トグル不変なら preflight は発火せず、通常の Diff 経路のままである (退行防止)。
    func test_トグル不変なら通常のDiffが発行される() {
        let sectionID = UUID()
        let cellID = UUID()
        let old = makeTree([KsSettingsViewCore.Section(
            id: sectionID, header: .text("一般"), cells: [LabelCell(id: cellID, title: "旧")],
            isHeaderVisible: false
        )])
        let new = makeTree([KsSettingsViewCore.Section(
            id: sectionID, header: .text("一般"), cells: [LabelCell(id: cellID, title: "新")],
            isHeaderVisible: false
        )])

        let diffs = DSLDiffCalculator.compute(from: old, to: new)
        XCTAssertEqual(diffs.count, 1)
        if case .replaceCell = diffs[0] {} else {
            XCTFail("expected .replaceCell, got \(diffs[0])")
        }
    }

    func test_containsAccessoryVisibilityChange_はトグル変化のみを検出する() {
        let sectionID = UUID()
        let base = KsSettingsViewCore.Section(
            id: sectionID, header: .text("一般"), footer: .text("補足"), cells: []
        )
        let headerHidden = KsSettingsViewCore.Section(
            id: sectionID, header: .text("一般"), footer: .text("補足"), cells: [],
            isHeaderVisible: false
        )
        let footerHidden = KsSettingsViewCore.Section(
            id: sectionID, header: .text("一般"), footer: .text("補足"), cells: [],
            isFooterVisible: false
        )
        XCTAssertTrue(DSLDiffCalculator.containsAccessoryVisibilityChange(from: [base], to: [headerHidden]))
        XCTAssertTrue(DSLDiffCalculator.containsAccessoryVisibilityChange(from: [base], to: [footerHidden]))
        XCTAssertFalse(DSLDiffCalculator.containsAccessoryVisibilityChange(from: [base], to: [base]))
        // 別 ID の Section は比較対象にならない
        let otherID = KsSettingsViewCore.Section(
            id: UUID(), header: .text("一般"), footer: .text("補足"), cells: [],
            isHeaderVisible: false
        )
        XCTAssertFalse(DSLDiffCalculator.containsAccessoryVisibilityChange(from: [base], to: [otherID]))
    }

    // MARK: - DSL 経路の表示反映

    func test_DSLでトグルを指定して構築するとHeaderが表示されない() {
        let sections = evaluate {
            ksSection("一般", footer: "補足", isHeaderVisible: false) { LabelCell(title: "A") }
        }
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: sections))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        XCTAssertFalse(hasHeaderArea(cv, section: 0), "DSL 指定のトグルが表示へ届いていない")
        XCTAssertTrue(hasFooterArea(cv, section: 0), "Footer まで隠れている")
    }

    func test_DSL再評価でトグル変更が両方向に反映される() {
        let first = evaluate {
            ksSection("一般", footer: "補足") { LabelCell(title: "A") }
        }
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: first))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }
        XCTAssertTrue(hasHeaderArea(cv, section: 0), "前提: 初期表示で Header 領域が無い")

        // WHEN: 再評価でトグルが false に変わる
        let second = evaluate {
            ksSection("一般", footer: "補足", isHeaderVisible: false) { LabelCell(title: "A") }
        }
        applyDiffs(
            DSLDiffCalculator.compute(from: makeTree(first), to: makeTree(second)),
            to: store
        )
        pump(cv)
        XCTAssertFalse(hasHeaderArea(cv, section: 0), "DSL 再評価のトグル false が表示へ届いていない")

        // WHEN: 再評価でトグルが true に戻る
        let third = evaluate {
            ksSection("一般", footer: "補足") { LabelCell(title: "A") }
        }
        applyDiffs(
            DSLDiffCalculator.compute(from: makeTree(second), to: makeTree(third)),
            to: store
        )
        pump(cv)
        XCTAssertTrue(hasHeaderArea(cv, section: 0), "DSL 再評価のトグル true が表示へ届いていない")
    }

    // MARK: - Store 経路と DSL 経路の対称性 (core/ADR-0018)

    /// 同一内容の Section に同じトグル操作を与えたとき、Store 経路と DSL 経路の表示結果が一致する。
    func test_Store経路とDSL経路でHeaderトグルの表示結果が一致する() {
        // DSL 経路: 再評価で isHeaderVisible を false へ
        let dslBefore = evaluate {
            ksSection("一般", footer: "補足") { LabelCell(title: "A") }
        }
        let dslStore = SettingsRootStore(initialRoot: SettingsRoot(sections: dslBefore))
        let (dslController, dslCV, dslWindow) = hostInWindow(store: dslStore)
        defer {
            dslWindow.isHidden = true
            withExtendedLifetime(dslController) {}
        }
        let dslAfter = evaluate {
            ksSection("一般", footer: "補足", isHeaderVisible: false) { LabelCell(title: "A") }
        }
        applyDiffs(
            DSLDiffCalculator.compute(from: makeTree(dslBefore), to: makeTree(dslAfter)),
            to: dslStore
        )
        pump(dslCV)

        // Store 経路: 同じ Section を replaceSection で isHeaderVisible = false へ
        let storeSection = dslBefore[0]
        let storeStore = SettingsRootStore(initialRoot: SettingsRoot(sections: [storeSection]))
        let (storeController, storeCV, storeWindow) = hostInWindow(store: storeStore)
        defer {
            storeWindow.isHidden = true
            withExtendedLifetime(storeController) {}
        }
        storeStore.replaceSection(sectionID: storeSection.id, new: KsSettingsViewCore.Section(
            id: storeSection.id,
            header: storeSection.header,
            footer: storeSection.footer,
            cells: storeSection.cells,
            isHeaderVisible: false
        ))
        pump(storeCV)

        // THEN: 両経路の表示結果が一致する
        XCTAssertEqual(hasHeaderArea(dslCV, section: 0), hasHeaderArea(storeCV, section: 0),
                       "Header 領域の有無が Store 経路と DSL 経路で一致しない")
        XCTAssertFalse(hasHeaderArea(dslCV, section: 0), "両経路とも Header は非表示でなければならない")
        XCTAssertEqual(hasFooterArea(dslCV, section: 0), hasFooterArea(storeCV, section: 0),
                       "Footer 領域の有無が Store 経路と DSL 経路で一致しない")
        XCTAssertTrue(hasFooterArea(storeCV, section: 0), "両経路とも Footer は表示のままでなければならない")
    }

    /// Footer 側も同じく両経路の表示結果が一致する。
    func test_Store経路とDSL経路でFooterトグルの表示結果が一致する() {
        let dslBefore = evaluate {
            ksSection("一般", footer: "補足") { LabelCell(title: "A") }
        }
        let dslStore = SettingsRootStore(initialRoot: SettingsRoot(sections: dslBefore))
        let (dslController, dslCV, dslWindow) = hostInWindow(store: dslStore)
        defer {
            dslWindow.isHidden = true
            withExtendedLifetime(dslController) {}
        }
        let dslAfter = evaluate {
            ksSection("一般", footer: "補足", isFooterVisible: false) { LabelCell(title: "A") }
        }
        applyDiffs(
            DSLDiffCalculator.compute(from: makeTree(dslBefore), to: makeTree(dslAfter)),
            to: dslStore
        )
        pump(dslCV)

        let storeSection = dslBefore[0]
        let storeStore = SettingsRootStore(initialRoot: SettingsRoot(sections: [storeSection]))
        let (storeController, storeCV, storeWindow) = hostInWindow(store: storeStore)
        defer {
            storeWindow.isHidden = true
            withExtendedLifetime(storeController) {}
        }
        storeStore.replaceSection(sectionID: storeSection.id, new: KsSettingsViewCore.Section(
            id: storeSection.id,
            header: storeSection.header,
            footer: storeSection.footer,
            cells: storeSection.cells,
            isFooterVisible: false
        ))
        pump(storeCV)

        XCTAssertEqual(hasFooterArea(dslCV, section: 0), hasFooterArea(storeCV, section: 0),
                       "Footer 領域の有無が Store 経路と DSL 経路で一致しない")
        XCTAssertFalse(hasFooterArea(dslCV, section: 0), "両経路とも Footer は非表示でなければならない")
        XCTAssertTrue(hasHeaderArea(dslCV, section: 0), "Header まで隠れている")
    }
}
#endif
