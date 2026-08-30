// ContentUpdateBatchTests.swift
// KsSettingsViewUITests
//
// `SettingsRootStore.replaceCells` による内容更新バッチが `KsSettingsViewController` の
// 表示へ反映され、構造（行の追加・削除・移動）を伴わないことを検証する。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class ContentUpdateBatchTests: XCTestCase {

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

    /// 先頭 Section の各行に実際に表示されているタイトル文字列を返す。
    private func renderedTitles(_ cv: UICollectionView, count: Int) -> [String?] {
        return (0..<count).map { item in
            let cell = cv.cellForItem(at: IndexPath(item: item, section: 0))
            return (cell as? KsListCellBase)?.titleLabel.text
        }
    }

    /// バッチ内容更新が表示へ反映され、構造変更を伴わないことを検証する。
    ///
    /// 表示の検証は内部状態ではなく実描画された行のタイトルで行う。model だけを更新して
    /// 部分更新を発行しない実装では、行の表示が古いままとなり本テストが落ちる。
    func test_replaceCells_バッチ更新が表示へ反映され構造変更は発生しない() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let cellC = LabelCell(title: "C")
        let sec = Section(header: .text("S"), cells: [cellA, cellB, cellC])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        let (controller, cv, window) = hostController(store: store)
        defer { window.isHidden = true }

        XCTAssertEqual(renderedTitles(cv, count: 3), ["A", "B", "C"], "初期表示")

        let beforeSections = controller.internalDataSource?.snapshot().sectionIdentifiers ?? []
        let beforeItems = controller.internalDataSource?.snapshot().itemIdentifiers ?? []
        // 更新対象の行そのものが破棄・再生成されていないことを確かめるため、実体を控えておく。
        let firstRowBefore = cv.cellForItem(at: IndexPath(item: 0, section: 0))

        store.replaceCells([
            (cellID: KsCellID(cell: cellA), new: LabelCell(id: cellA.id, title: "A2")),
            (cellID: KsCellID(cell: cellC), new: LabelCell(id: cellC.id, title: "C2")),
        ])
        pump(cv)

        XCTAssertEqual(
            renderedTitles(cv, count: 3),
            ["A2", "B", "C2"],
            "対象行の表示内容が更新され、対象外の行は変わらない"
        )

        let afterSections = controller.internalDataSource?.snapshot().sectionIdentifiers ?? []
        let afterItems = controller.internalDataSource?.snapshot().itemIdentifiers ?? []
        XCTAssertEqual(beforeSections, afterSections, "Section の集合・順序は変化しない")
        XCTAssertEqual(beforeItems, afterItems, "行の集合・順序は変化しない（追加・削除・移動なし）")
        XCTAssertEqual(
            afterItems.count, 3,
            "行数は変化しない"
        )
        XCTAssertTrue(
            firstRowBefore === cv.cellForItem(at: IndexPath(item: 0, section: 0)),
            "同一の行が再構成される（破棄・再生成による差し替えではない）"
        )
    }

    /// 適用 0 件のバッチは配信されないため、表示も構造も変化しない。
    func test_replaceCells_存在しないIDのみでは表示が変化しない() {
        let cellA = LabelCell(title: "A")
        let sec = Section(header: .text("S"), cells: [cellA])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        let (controller, cv, window) = hostController(store: store)
        defer { window.isHidden = true }

        let beforeItems = controller.internalDataSource?.snapshot().itemIdentifiers ?? []

        store.replaceCells([(cellID: KsCellID(id: UUID()), new: LabelCell(title: "X"))])
        pump(cv)

        XCTAssertEqual(renderedTitles(cv, count: 1), ["A"])
        XCTAssertEqual(controller.internalDataSource?.snapshot().itemIdentifiers ?? [], beforeItems)
    }
}
#endif
