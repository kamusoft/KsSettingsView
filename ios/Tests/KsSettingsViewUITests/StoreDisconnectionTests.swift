// StoreDisconnectionTests.swift
// KsSettingsViewUITests
//
// `KsSettingsViewController.disconnectStore()` が Store 購読を解除し、以後の Store 更新が
// 表示へ反映されなくなることを検証する。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class StoreDisconnectionTests: XCTestCase {

    /// Store に接続した Controller を window に載せ、行の実描画を確定させる。
    private func hostController(
        store: SettingsRootStore
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let controller = KsSettingsViewController(store: store)
        let size = CGSize(width: 375, height: 600)
        let root = controller.view!
        root.frame = CGRect(origin: .zero, size: size)
        let window = UIWindow(frame: root.frame)
        window.addSubview(root)
        window.makeKeyAndVisible()
        root.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        pump(cv)
        return (controller, cv, window)
    }

    /// レイアウトと再構成を確定させる。
    private func pump(_ view: UIView, seconds: TimeInterval = 0.05) {
        view.setNeedsLayout()
        view.layoutIfNeeded()
        RunLoop.current.run(until: Date().addingTimeInterval(seconds))
        view.setNeedsLayout()
        view.layoutIfNeeded()
    }

    /// 先頭 Section に実際に表示されている行タイトルを返す。
    private func renderedTitles(_ cv: UICollectionView) -> [String?] {
        return (0..<cv.numberOfItems(inSection: 0)).map { item in
            let cell = cv.cellForItem(at: IndexPath(item: item, section: 0))
            return (cell as? KsListCellBase)?.titleLabel.text
        }
    }

    /// 標準構成: Section 1 個に Cell A / B。
    private func makeStore() -> SettingsRootStore {
        return SettingsRootStore(initialRoot: SettingsRoot(sections: [
            Section(header: .text("S"), cells: [LabelCell(title: "A"), LabelCell(title: "B")])
        ]))
    }

    /// 解除後は構造 Diff・内容更新バッチ・Theme のいずれも表示へ反映されない。
    func test_disconnectStore後のStore更新は表示へ反映されない() {
        let store = makeStore()
        let (controller, cv, window) = hostController(store: store)
        defer { window.isHidden = true }
        XCTAssertEqual(renderedTitles(cv), ["A", "B"])
        let themeBefore = controller.currentTheme

        controller.disconnectStore()

        let sectionID = store.root.sections[0].id
        store.insertCell(LabelCell(title: "C"), in: sectionID, at: 2)
        let cellA = store.root.sections[0].cells[0]
        store.replaceCells([(cellID: KsCellID(cell: cellA), new: LabelCell(id: cellA.id, title: "A-updated"))])
        store.applyTheme(Theme(cellTitleColor: .green))
        pump(cv)

        XCTAssertEqual(renderedTitles(cv), ["A", "B"], "解除後の構造・内容更新は表示へ届かない")
        XCTAssertEqual(controller.currentTheme, themeBefore, "解除後の Theme 更新は届かない")
    }

    /// 解除は冪等で、Store 未接続の Controller に対して呼んでも何も起きない。
    func test_disconnectStoreは冪等() {
        let store = makeStore()
        let (controller, cv, window) = hostController(store: store)
        defer { window.isHidden = true }

        controller.disconnectStore()
        controller.disconnectStore()

        XCTAssertEqual(renderedTitles(cv), ["A", "B"], "解除の繰り返しで表示は失われない")

        let standalone = KsSettingsViewController(root: SettingsRoot())
        standalone.disconnectStore()
    }
}
#endif
