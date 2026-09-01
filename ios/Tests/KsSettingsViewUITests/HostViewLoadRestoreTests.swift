// HostViewLoadRestoreTests.swift
// KsSettingsViewUITests
//
// Store 接続済みの `KsSettingsViewController` が、view load の時点で Store の現在状態から
// 表示を構築することを検証する（core/ADR-0019）。
//
// 検証する順序は「Host 生成 → Store 操作 → view load」であり、view load は
// `loadViewIfNeeded()` で誘発する。表示の確認は内部状態ではなく、window に載せた実物の
// Cell / supplementary / CollectionView から行う。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class HostViewLoadRestoreTests: XCTestCase {

    // MARK: - ヘルパ

    /// view load 済みの Controller を window に載せ、実描画を確定させる。
    ///
    /// Store 購読は `[weak self]` で張られ Cancellable は Controller 自身が所有するため、
    /// `window.rootViewController` へ設定して window が Controller を強参照する所有関係を作る。
    private func present(_ controller: KsSettingsViewController) -> (UICollectionView, UIWindow) {
        let size = CGSize(width: 375, height: 600)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        let rootView = controller.view!
        rootView.frame = CGRect(origin: .zero, size: size)
        rootView.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        awaitInitialRender(controller)
        return (cv, window)
    }

    /// 指定 Section の各行に実際に表示されているタイトル文字列を返す。
    private func renderedTitles(_ cv: UICollectionView, section: Int, count: Int) -> [String?] {
        return (0..<count).map { item in
            let cell = cv.cellForItem(at: IndexPath(item: item, section: section))
            return (cell as? KsListCellBase)?.titleLabel.text
        }
    }

    /// 表示中の Section Header supplementary から UILabel を取得する。
    private func visibleHeaderLabel(_ cv: UICollectionView, section: Int) -> UILabel? {
        let view = cv.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: section)
        )
        guard let listCell = view as? UICollectionViewListCell else { return nil }
        return listCell.contentView.subviews.compactMap { $0 as? UILabel }.first
    }

    /// 表示中の Root Header supplementary のテキストを返す。
    ///
    /// Root H/F は layout 全体の boundary supplementary であり indexPath を持たないため、
    /// 表示中の supplementary を kind で列挙して取得する。
    private func visibleRootHeaderText(_ cv: UICollectionView) -> String? {
        let views = cv.visibleSupplementaryViews(
            ofKind: KsSettingsViewController.rootHeaderElementKind
        )
        for view in views {
            guard let listCell = view as? UICollectionViewListCell else { continue }
            if let label = listCell.contentView.subviews.compactMap({ $0 as? UILabel }).first {
                return label.text
            }
        }
        return nil
    }

    /// snapshot 上の Section ごとの行数を返す。
    private func snapshotItemCounts(_ controller: KsSettingsViewController) -> [UUID: Int] {
        guard let snapshot = controller.internalDataSource?.snapshot() else { return [:] }
        var result: [UUID: Int] = [:]
        for sectionID in snapshot.sectionIdentifiers {
            result[sectionID] = snapshot.numberOfItems(inSection: sectionID)
        }
        return result
    }

    // MARK: - 構造操作

    func test_viewLoad前の構造操作がload時の表示に反映される() {
        let cellA1 = LabelCell(title: "A1")
        let cellA2 = LabelCell(title: "A2")
        let cellB1 = LabelCell(title: "B1")
        let sectionA = Section(header: .text("A"), cells: [cellA1, cellA2])
        let sectionB = Section(header: .text("B"), cells: [cellB1])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sectionA, sectionB]))

        // Host 生成のみ。view はまだ load しない。
        let controller = KsSettingsViewController(store: store)
        XCTAssertFalse(controller.isViewLoaded, "前提: view は未 load")

        // 未 load のまま構造を操作する。
        store.insertCell(LabelCell(title: "A3"), in: sectionA.id, at: 2)
        store.removeCell(cellID: KsCellID(cell: cellA1))
        store.replaceSection(
            sectionID: sectionB.id,
            new: Section(
                id: sectionB.id,
                header: .text("B"),
                cells: [LabelCell(title: "B1改"), LabelCell(title: "B2")]
            )
        )

        controller.loadViewIfNeeded()

        // viewDidLoad 完了時点の構造が Store の現在状態と一致する。
        XCTAssertEqual(
            snapshotItemCounts(controller),
            [sectionA.id: 2, sectionB.id: 2],
            "viewDidLoad 完了時点の snapshot が操作適用後の Store 現在状態と一致しない"
        )

        let (cv, window) = present(controller)
        defer { window.isHidden = true }

        XCTAssertEqual(renderedTitles(cv, section: 0, count: 2), ["A2", "A3"])
        XCTAssertEqual(renderedTitles(cv, section: 1, count: 2), ["B1改", "B2"])
    }

    // MARK: - Cell 内容更新

    func test_viewLoad前のreplaceCellがload時の表示に反映される() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let section = Section(header: .text("S"), cells: [cellA, cellB])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section]))

        let controller = KsSettingsViewController(store: store)
        XCTAssertFalse(controller.isViewLoaded, "前提: view は未 load")

        store.replaceCell(cellID: KsCellID(cell: cellA), new: LabelCell(id: cellA.id, title: "A改"))

        controller.loadViewIfNeeded()
        let (cv, window) = present(controller)
        defer { window.isHidden = true }

        XCTAssertEqual(
            renderedTitles(cv, section: 0, count: 2),
            ["A改", "B"],
            "単発の内容更新が view load 時の表示に反映されていない"
        )
    }

    func test_viewLoad前のreplaceCellsバッチがload時の表示に反映される() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let cellC = LabelCell(title: "C")
        let section = Section(header: .text("S"), cells: [cellA, cellB, cellC])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section]))

        let controller = KsSettingsViewController(store: store)
        XCTAssertFalse(controller.isViewLoaded, "前提: view は未 load")

        store.replaceCells([
            (cellID: KsCellID(cell: cellA), new: LabelCell(id: cellA.id, title: "A改")),
            (cellID: KsCellID(cell: cellC), new: LabelCell(id: cellC.id, title: "C改")),
        ])

        controller.loadViewIfNeeded()
        let (cv, window) = present(controller)
        defer { window.isHidden = true }

        XCTAssertEqual(
            renderedTitles(cv, section: 0, count: 3),
            ["A改", "B", "C改"],
            "バッチ内容更新が view load 時の表示に反映されていない"
        )
    }

    // MARK: - Section accessory / Theme

    func test_viewLoad前のSectionAccessoryとTheme変更がload時の表示に反映される() {
        let cell = LabelCell(title: "A")
        let section = Section(header: .text("旧ヘッダ"), cells: [cell])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section]))

        let controller = KsSettingsViewController(store: store)
        XCTAssertFalse(controller.isViewLoaded, "前提: view は未 load")

        store.updateAccessory(
            target: .sectionHeader(sectionID: section.id),
            accessory: .section(.text("新ヘッダ"))
        )
        let storeTheme = Theme(backgroundColor: .systemPink, cellTitleColor: .systemGreen)
        store.applyTheme(storeTheme)

        controller.loadViewIfNeeded()
        let (cv, window) = present(controller)
        defer { window.isHidden = true }

        XCTAssertEqual(
            visibleHeaderLabel(cv, section: 0)?.text,
            "新ヘッダ",
            "view load 前の Section accessory 更新が表示に反映されていない"
        )
        XCTAssertEqual(
            cv.backgroundColor,
            UIColor.systemPink,
            "view load 前の Theme 変更が背景色に反映されていない"
        )
        let renderedCell = cv.cellForItem(at: IndexPath(item: 0, section: 0)) as? KsListCellBase
        XCTAssertEqual(
            renderedCell?.titleLabel.textColor,
            UIColor.systemGreen,
            "view load 前の Theme 変更が Cell の描画に反映されていない"
        )
    }

    // MARK: - Theme の優先順位

    func test_Store接続中の直接applyThemeはviewLoad時にStoreThemeで上書きされる() {
        let cell = LabelCell(title: "A")
        let section = Section(header: .text("S"), cells: [cell])
        let storeTheme = Theme(backgroundColor: .systemPink, cellTitleColor: .systemGreen)
        let store = SettingsRootStore(
            initialRoot: SettingsRoot(sections: [section]),
            initialTheme: storeTheme
        )

        let controller = KsSettingsViewController(store: store)
        XCTAssertFalse(controller.isViewLoaded, "前提: view は未 load")

        // Store と異なる Theme を公開 API で直接適用する。
        controller.applyTheme(Theme(backgroundColor: .systemTeal, cellTitleColor: .systemOrange))

        controller.loadViewIfNeeded()
        let (cv, window) = present(controller)
        defer { window.isHidden = true }

        XCTAssertEqual(
            cv.backgroundColor,
            UIColor.systemPink,
            "Store 接続中の直接適用 Theme が view load 後も残っている"
        )
        let renderedCell = cv.cellForItem(at: IndexPath(item: 0, section: 0)) as? KsListCellBase
        XCTAssertEqual(
            renderedCell?.titleLabel.textColor,
            UIColor.systemGreen,
            "Cell の描画が Store の Theme を反映していない"
        )
    }

    // MARK: - Root accessory（復元対象外）

    func test_RootAccessoryは復元対象外で所有者の再適用により表示される() {
        let cell = LabelCell(title: "A")
        let section = Section(header: .text("S"), cells: [cell])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section]))

        let controller = KsSettingsViewController(store: store)
        XCTAssertFalse(controller.isViewLoaded, "前提: view は未 load")

        store.updateAccessory(target: .rootHeader, accessory: .root(.text("Root H")))

        controller.loadViewIfNeeded()
        let (cv, window) = present(controller)
        defer { window.isHidden = true }

        // Root H/F は Store の現在状態に含まれないため、view load 時の復元対象ではない。
        XCTAssertNil(
            controller.rootHeader,
            "Root header は復元対象外だが Controller に復元されている"
        )
        XCTAssertNil(
            visibleRootHeaderText(cv),
            "Root header は復元対象外だが表示されている"
        )

        // 所有者が view load 後に再適用すると表示される。
        store.updateAccessory(target: .rootHeader, accessory: .root(.text("Root H")))
        awaitEqual(
            "再適用した Root header の実描画",
            expected: "Root H" as String?,
            in: cv,
            actual: { visibleRootHeaderText(cv) }
        )

        XCTAssertEqual(controller.rootHeader, .text("Root H"))
        XCTAssertEqual(
            visibleRootHeaderText(cv),
            "Root H",
            "再適用した Root header が表示されていない"
        )
    }

    // MARK: - Store 非接続

    func test_Store非接続initはinit時のrootで表示する() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let section = Section(header: .text("S"), cells: [cellA, cellB])
        let initTheme = Theme(backgroundColor: .systemPink, cellTitleColor: .systemGreen)

        let controller = KsSettingsViewController(
            root: SettingsRoot(sections: [section]),
            theme: initTheme
        )
        XCTAssertFalse(controller.isViewLoaded, "前提: view は未 load")

        controller.loadViewIfNeeded()

        XCTAssertEqual(
            snapshotItemCounts(controller),
            [section.id: 2],
            "Store 非接続 init の構造が init 時の root と一致しない"
        )

        let (cv, window) = present(controller)
        defer { window.isHidden = true }

        XCTAssertEqual(renderedTitles(cv, section: 0, count: 2), ["A", "B"])
        XCTAssertEqual(visibleHeaderLabel(cv, section: 0)?.text, "S")
        XCTAssertEqual(cv.backgroundColor, UIColor.systemPink, "init 時の Theme が反映されていない")
    }
}
#endif
