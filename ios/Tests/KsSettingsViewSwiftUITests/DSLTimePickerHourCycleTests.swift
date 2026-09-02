// DSLTimePickerHourCycleTests.swift
// KsSettingsViewSwiftUITests
//
// 宣言 DSL で `TimePickerCell` の `is24Hour` を変えたとき、DSL 再評価 → 差分検出
// (`DSLDiffCalculator`) → Store → Host → 行の再バインドを通って picker の時制へ届くことと、
// 同じ変化を Store 経路で与えた結果と一致することを検証する (core/ADR-0018)。
//
// 差分検出層が `is24Hour` の差を翻訳できないと diff 0 件のまま表示に届かない無音の失敗になるため、
// 検証は生成された diff の件数ではなく window に載せた実物の行の picker で行う。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewCore
@testable import KsSettingsViewUI

@MainActor
final class DSLTimePickerHourCycleTests: XCTestCase {

    private static let time = Calendar.current.date(bySettingHour: 22, minute: 15, second: 0, of: Date())!

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

    /// Store 接続済み controller を window に載せ、行の実描画を確定させる。
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
        // window へ載せた直後は行が未生成のため、先頭行が実体化することを初期反映の完了条件とする。
        awaitNonNil("初期表示の先頭行の実描画", in: cv) {
            cv.cellForItem(at: IndexPath(item: 0, section: 0))
        }
        return (controller, cv, window)
    }

    /// 先頭行の picker の時制が期待どおりになるまで待つ。
    private func awaitRenderedIs24Hour(
        _ cv: UICollectionView,
        equals expected: Bool,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        awaitEqual(
            "先頭行の picker の時制 (24 時間制か)",
            expected: expected,
            in: cv,
            file: file,
            line: line,
            actual: { self.renderedIs24Hour(cv) }
        )
    }

    /// 先頭 Section の先頭行が実際に提示する picker の時制を返す。
    ///
    /// `j` は「その Locale の時制に従う時」を表すテンプレートで、24時間制なら `H` / `k`、
    /// 12時間制なら `h` / `K` に解決される。
    private func renderedIs24Hour(_ cv: UICollectionView) -> Bool? {
        let row = cv.cellForItem(at: IndexPath(item: 0, section: 0))
        guard let view = row as? TimePickerCellView, let locale = view._pickerLocale else {
            return nil
        }
        guard let pattern = DateFormatter.dateFormat(fromTemplate: "j", options: 0, locale: locale) else {
            return nil
        }
        if pattern.contains("H") || pattern.contains("k") { return true }
        if pattern.contains("h") || pattern.contains("K") { return false }
        return nil
    }

    // MARK: - DSL 再評価での反映

    /// DSL 再評価で `is24Hour` を false へ変えると、差分が生成されて picker の時制へ届く。
    func test_DSL再評価のis24Hour変更がpickerの時制へ届く() {
        let before = evaluate {
            ksSection("TimePickerCell") {
                TimePickerCell(title: "就寝", time: Self.time)
            }
        }
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: before))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }
        XCTAssertEqual(renderedIs24Hour(cv), true, "前提: 初期表示の picker が 24 時間制になっていない")

        let after = evaluate {
            ksSection("TimePickerCell") {
                TimePickerCell(title: "就寝", time: Self.time, is24Hour: false)
            }
        }
        applyDiffs(DSLDiffCalculator.compute(from: makeTree(before), to: makeTree(after)), to: store)
        awaitRenderedIs24Hour(cv, equals: false)

        XCTAssertEqual(renderedIs24Hour(cv), false, "DSL 再評価の is24Hour 変更が picker へ届いていない")
    }

    /// 24時間制へ戻す方向の再評価も同じ経路で届く。
    func test_DSL再評価のis24Hour変更は24時間制へ戻す方向でも届く() {
        let before = evaluate {
            ksSection("TimePickerCell") {
                TimePickerCell(title: "就寝", time: Self.time, is24Hour: false)
            }
        }
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: before))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }
        XCTAssertEqual(renderedIs24Hour(cv), false, "前提: 初期表示の picker が 12 時間制になっていない")

        let after = evaluate {
            ksSection("TimePickerCell") {
                TimePickerCell(title: "就寝", time: Self.time, is24Hour: true)
            }
        }
        applyDiffs(DSLDiffCalculator.compute(from: makeTree(before), to: makeTree(after)), to: store)
        awaitRenderedIs24Hour(cv, equals: true)

        XCTAssertEqual(renderedIs24Hour(cv), true, "DSL 再評価の is24Hour 変更が picker へ届いていない")
    }

    // MARK: - Store 経路と DSL 経路の対称性 (core/ADR-0018)

    /// 同一内容の Cell に同じ `is24Hour` 変更を与えたとき、Store 経路と DSL 経路の表示結果が一致する。
    func test_Store経路とDSL経路でis24Hour変更の表示結果が一致する() {
        // DSL 経路: 再評価で is24Hour を false へ
        let dslBefore = evaluate {
            ksSection("TimePickerCell") {
                TimePickerCell(title: "就寝", time: Self.time)
            }
        }
        let dslStore = SettingsRootStore(initialRoot: SettingsRoot(sections: dslBefore))
        let (dslController, dslCV, dslWindow) = hostInWindow(store: dslStore)
        defer {
            dslWindow.isHidden = true
            withExtendedLifetime(dslController) {}
        }
        let dslAfter = evaluate {
            ksSection("TimePickerCell") {
                TimePickerCell(title: "就寝", time: Self.time, is24Hour: false)
            }
        }
        applyDiffs(
            DSLDiffCalculator.compute(from: makeTree(dslBefore), to: makeTree(dslAfter)),
            to: dslStore
        )
        awaitRenderedIs24Hour(dslCV, equals: false)

        // Store 経路: 同じ Cell を replaceCell で is24Hour = false へ
        let storeSection = dslBefore[0]
        let storeStore = SettingsRootStore(initialRoot: SettingsRoot(sections: [storeSection]))
        let (storeController, storeCV, storeWindow) = hostInWindow(store: storeStore)
        defer {
            storeWindow.isHidden = true
            withExtendedLifetime(storeController) {}
        }
        guard let original = storeSection.cells.first as? TimePickerCell else {
            return XCTFail("前提: 先頭 Cell が TimePickerCell ではない")
        }
        storeStore.replaceCell(
            cellID: KsCellID(id: original.id),
            new: TimePickerCell(
                id: original.id,
                title: original.title,
                time: original.time,
                is24Hour: false
            )
        )
        awaitRenderedIs24Hour(storeCV, equals: false)

        XCTAssertEqual(renderedIs24Hour(dslCV), renderedIs24Hour(storeCV),
                       "picker の時制が Store 経路と DSL 経路で一致しない")
        XCTAssertEqual(renderedIs24Hour(storeCV), false, "両経路とも 12 時間制でなければならない")
    }
}
#endif
