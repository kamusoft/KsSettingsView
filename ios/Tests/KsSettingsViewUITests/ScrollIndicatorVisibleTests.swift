// ScrollIndicatorVisibleTests.swift
// KsSettingsViewUITests
//
// `Theme.scrollIndicatorVisible` が `UICollectionView.showsVerticalScrollIndicator` へ届くことを
// 検証する。初期 Theme の反映と、`applyTheme(_:)` での差し替えへの追従の両方を見る。
//
// 設定リストは縦にしかスクロールしないため、検証対象は縦インジケータのみとする。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class ScrollIndicatorVisibleTests: XCTestCase {

    private var window: UIWindow?

    override func tearDown() {
        window?.isHidden = true
        window = nil
        super.tearDown()
    }

    /// 指定 Theme の controller を window に載せ、実描画を確定させる。
    private func hostController(theme: Theme) -> (KsSettingsViewController, UICollectionView) {
        let controller = KsSettingsViewController(
            root: SettingsRoot(sections: [
                KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])
            ]),
            theme: theme
        )
        let size = CGSize(width: 375, height: 600)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        self.window = window

        let rootView = controller.view!
        rootView.frame = CGRect(origin: .zero, size: size)
        rootView.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        awaitInitialRender(controller)
        return (controller, cv)
    }

    func test_既定Themeでは縦スクロールインジケータが表示される() {
        let (_, cv) = hostController(theme: Theme())

        XCTAssertTrue(
            cv.showsVerticalScrollIndicator,
            "Theme.scrollIndicatorVisible の既定は true なので縦インジケータは表示される"
        )
    }

    func test_scrollIndicatorVisibleがfalseの初期Themeで縦インジケータが消える() {
        let (_, cv) = hostController(theme: Theme(scrollIndicatorVisible: false))

        XCTAssertFalse(
            cv.showsVerticalScrollIndicator,
            "初期 Theme の scrollIndicatorVisible = false が view load 時点で反映されていない"
        )
    }

    func test_applyThemeで縦インジケータの表示が追従する() {
        let (controller, cv) = hostController(theme: Theme())
        XCTAssertTrue(cv.showsVerticalScrollIndicator, "前提: 既定は表示")

        controller.applyTheme(Theme(scrollIndicatorVisible: false))
        XCTAssertFalse(
            cv.showsVerticalScrollIndicator,
            "applyTheme で false へ追従していない"
        )

        controller.applyTheme(Theme(scrollIndicatorVisible: true))
        XCTAssertTrue(
            cv.showsVerticalScrollIndicator,
            "applyTheme で true へ戻っていない"
        )
    }

    func test_Store経由のTheme更新で縦インジケータの表示が追従する() {
        let store = SettingsRootStore(
            initialRoot: SettingsRoot(sections: [
                KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])
            ]),
            initialTheme: Theme()
        )
        let controller = KsSettingsViewController(store: store)
        let size = CGSize(width: 375, height: 600)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        self.window = window
        controller.view!.frame = CGRect(origin: .zero, size: size)
        controller.view!.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        awaitInitialRender(controller)

        XCTAssertTrue(cv.showsVerticalScrollIndicator, "前提: 既定は表示")

        store.applyTheme(Theme(scrollIndicatorVisible: false))
        awaitCondition(
            "Store 経由の Theme 更新で縦インジケータが消える",
            in: cv,
            actual: { "showsVerticalScrollIndicator = \(cv.showsVerticalScrollIndicator)" },
            until: { !cv.showsVerticalScrollIndicator }
        )
    }

    // MARK: - 行とスクロール位置の維持

    /// スクロールバーの表示だけを変えても、表示中の行も content offset も作り直されない。
    func test_スクロールバーの表示だけを変えても行とスクロール位置は維持される() throws {
        let cells = (1...40).map { LabelCell(title: "行 \($0)") }
        let section = KsSettingsViewCore.Section(header: .text("一般"), cells: cells)
        let controller = KsSettingsViewController(
            root: SettingsRoot(sections: [section]),
            theme: Theme()
        )
        let size = CGSize(width: 375, height: 600)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        self.window = window
        controller.view!.frame = CGRect(origin: .zero, size: size)
        controller.view!.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        awaitInitialRender(controller)

        // 途中までスクロールした状態を作る。行の高さは自己サイズで、画面外の行は推定値のまま
        // 置かれるため、レイアウトの作り直しで推定値が実寸へ入れ替わると offset が補正される。
        // 補正しきる前に控えると、Theme とは関係のないずれを掴んでしまうので、作り直しても
        // offset が動かなくなるまで進めてから控える。
        cv.scrollToItem(at: IndexPath(item: 12, section: 0), at: .top, animated: false)
        awaitCondition(
            "自己サイズ行の高さ確定に伴う content offset の補正が落ち着く",
            in: cv,
            actual: { "contentOffset = \(cv.contentOffset)" },
            until: {
                let observed = cv.contentOffset
                cv.collectionViewLayout.invalidateLayout()
                layoutNow(cv)
                return cv.contentOffset == observed
            }
        )
        let beforeOffset = cv.contentOffset
        let beforeCells = cv.indexPathsForVisibleItems.sorted().map { ($0, cv.cellForItem(at: $0)) }
        let beforeIDs = visibleItemIDs(controller, cv)
        XCTAssertFalse(beforeCells.isEmpty, "前提: 行が表示されていない")
        XCTAssertGreaterThan(beforeOffset.y, 0, "前提: スクロールしていない")

        controller.applyTheme(Theme(scrollIndicatorVisible: false))
        cv.layoutIfNeeded()

        XCTAssertFalse(cv.showsVerticalScrollIndicator, "前提: 表示設定が変わっていない")
        XCTAssertEqual(cv.contentOffset, beforeOffset, "content offset が変わっている")
        let afterIDs = visibleItemIDs(controller, cv)
        XCTAssertEqual(beforeIDs, afterIDs, "表示中の Section / Cell の ID が変わっている")
        for (indexPath, beforeCell) in beforeCells {
            XCTAssertTrue(
                cv.cellForItem(at: indexPath) === beforeCell,
                "\(indexPath) の行の View インスタンスが作り直されている"
            )
        }
    }

    /// 表示中の行に対応する Section / Cell の ID を indexPath 順に返す。
    private func visibleItemIDs(
        _ controller: KsSettingsViewController,
        _ cv: UICollectionView
    ) -> [String] {
        guard let snapshot = controller.internalDataSource?.snapshot() else { return [] }
        return cv.indexPathsForVisibleItems.sorted().map { indexPath in
            let sectionID = snapshot.sectionIdentifiers[indexPath.section]
            let itemID = snapshot.itemIdentifiers(inSection: sectionID)[indexPath.item]
            return "\(sectionID)/\(itemID.id)"
        }
    }

    // MARK: - PickerCell の選択面

    /// 選択面の候補リストも、開いた時点の `scrollIndicatorVisible` に従う。
    func test_PickerCellの候補リストも表示設定に従う() throws {
        let cell = PickerCell(
            title: "テーマ",
            items: ["ライト", "ダーク", "システム"].map { PickerItem(text: $0) },
            selectedIndex: 0
        )

        let visibleList = try XCTUnwrap(makePickerList(cell: cell, theme: Theme()))
        XCTAssertTrue(
            visibleList.tableView.showsVerticalScrollIndicator,
            "既定の Theme では候補リストの縦スクロールインジケータが表示される"
        )

        let hiddenList = try XCTUnwrap(
            makePickerList(cell: cell, theme: Theme(scrollIndicatorVisible: false))
        )
        XCTAssertFalse(
            hiddenList.tableView.showsVerticalScrollIndicator,
            "scrollIndicatorVisible = false は候補リストにも反映される"
        )
    }

    /// 表示中の選択面は Theme 差し替えに追従しなくてよいが、開き直した選択面は新しい値に従う。
    ///
    /// 候補リストは開いた時点の値を控えるため、追従しないことは構造上そうなる。一方「次に開いたとき
    /// から新しい値に従う」は、Theme を差し替えてから開き直す経路を通らないと確かめられない。
    func test_Themeを差し替えて開き直した候補リストは新しい値に従う() throws {
        let cell = PickerCell(
            title: "テーマ",
            items: ["ライト", "ダーク", "システム"].map { PickerItem(text: $0) },
            selectedIndex: 0
        )
        let cellView = PickerCellView()

        cellView.render(cell: cell, theme: Theme())
        let first = try XCTUnwrap(cellView._makeListViewControllerForTesting())
        first.loadViewIfNeeded()
        XCTAssertTrue(first.tableView.showsVerticalScrollIndicator, "前提: 既定では表示される")

        // 行へ新しい Theme を描き直してから開き直す。
        cellView.render(cell: cell, theme: Theme(scrollIndicatorVisible: false))
        let reopened = try XCTUnwrap(cellView._makeListViewControllerForTesting())
        reopened.loadViewIfNeeded()

        XCTAssertFalse(
            reopened.tableView.showsVerticalScrollIndicator,
            "開き直した候補リストは差し替え後の Theme に従う"
        )
    }

    /// PickerCell の行を指定 Theme で描画し、そこから選択面の候補リストを組み立てる。
    ///
    /// 提示経路と同じ組み立て seam を通すので、Theme の受け渡しは実際に開いたときと同じになる。
    private func makePickerList(cell: PickerCell, theme: Theme) -> PickerListViewController? {
        let cellView = PickerCellView()
        cellView.render(cell: cell, theme: theme)
        let listVC = cellView._makeListViewControllerForTesting()
        listVC?.loadViewIfNeeded()
        return listVC
    }
}
#endif
