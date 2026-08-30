// ReplaceCellTypeChangeTests.swift
// KsSettingsViewUITests
//
// 部分更新経路 (`replaceCell` / `replaceCells`) で同一 ID のまま Cell の具象型が変わる
// 差し替えが、Native cell を交換して反映されることを検証する。
//
// 検証は内部状態ではなく、window に載せた実物の行の型と描画文字列で行う。具象型の変化を
// 検出せず同一 Native cell を再構成する実装では、`cellProvider` が別の reuse identifier を
// dequeue して UIKit が例外を投げるため、本テスト群は完走できない。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class ReplaceCellTypeChangeTests: XCTestCase {

    /// Store に接続した Controller を window に載せ、行の実描画を確定させる。
    ///
    /// Store 購読は `[weak self]` で張られるため、window に controller を強参照させて
    /// 更新が届く所有関係を作る。
    private func hostController(
        store: SettingsRootStore
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let controller = KsSettingsViewController(store: store)
        let size = CGSize(width: 375, height: 600)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        let root = controller.view!
        root.frame = CGRect(origin: .zero, size: size)
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

    // MARK: - replaceCell (単発)

    /// `replaceCell` で具象型が変わる差し替えを行うと、Native cell が交換されて新しい行が表示される。
    func test_replaceCell_具象型が変わる差し替えでNativeCellが交換される() {
        let sharedID = UUID()
        let other = LabelCell(title: "そのまま")
        let sec = Section(
            header: .text("S"),
            cells: [LabelCell(id: sharedID, title: "ラベル"), other]
        )
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        let (controller, cv, window) = hostController(store: store)
        defer { window.isHidden = true }

        XCTAssertTrue(
            cv.cellForItem(at: IndexPath(item: 0, section: 0)) is LabelCellView,
            "前提: 初期表示の行が LabelCellView ではない"
        )
        let beforeItems = controller.internalDataSource?.snapshot().itemIdentifiers ?? []

        store.replaceCell(
            cellID: KsCellID(id: sharedID),
            new: SwitchCell(id: sharedID, title: "スイッチ")
        )
        pump(cv)

        XCTAssertTrue(
            cv.cellForItem(at: IndexPath(item: 0, section: 0)) is SwitchCellView,
            "具象型の変更後も旧 Renderer の Native cell が残っている"
        )
        XCTAssertEqual(
            renderedTitles(cv, count: 2), ["スイッチ", "そのまま"],
            "具象型が変わった Cell の内容が表示へ反映されていない"
        )
        XCTAssertEqual(
            controller.internalDataSource?.snapshot().itemIdentifiers ?? [], beforeItems,
            "行の集合・順序は変化しない（追加・削除・移動なし）"
        )
    }

    /// 型が変わらない内容更新は、行を破棄・再生成せず同一 Native cell を再構成する。
    func test_replaceCell_型が変わらない内容更新は同一行が再構成される() {
        let cellA = LabelCell(title: "A")
        let sec = Section(header: .text("S"), cells: [cellA])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        let (_, cv, window) = hostController(store: store)
        defer { window.isHidden = true }

        let rowBefore = cv.cellForItem(at: IndexPath(item: 0, section: 0))

        store.replaceCell(
            cellID: KsCellID(cell: cellA),
            new: LabelCell(id: cellA.id, title: "A2")
        )
        pump(cv)

        XCTAssertEqual(renderedTitles(cv, count: 1), ["A2"], "内容更新が表示へ反映されていない")
        XCTAssertTrue(
            rowBefore === cv.cellForItem(at: IndexPath(item: 0, section: 0)),
            "同一の行が再構成される（破棄・再生成による差し替えではない）"
        )
    }

    // MARK: - replaceCells (バッチ)

    /// `replaceCells` のバッチに具象型変化が含まれても、その行だけ Native cell を交換して反映する。
    func test_replaceCells_具象型が変わる差し替えを含むバッチが反映される() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let cellC = LabelCell(title: "C")
        let sec = Section(header: .text("S"), cells: [cellA, cellB, cellC])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        let (controller, cv, window) = hostController(store: store)
        defer { window.isHidden = true }

        XCTAssertEqual(renderedTitles(cv, count: 3), ["A", "B", "C"], "初期表示")
        let beforeItems = controller.internalDataSource?.snapshot().itemIdentifiers ?? []
        // 型が変わらない行が再構成で済むことを確かめるため、実体を控えておく。
        let thirdRowBefore = cv.cellForItem(at: IndexPath(item: 2, section: 0))

        store.replaceCells([
            (cellID: KsCellID(cell: cellA), new: SwitchCell(id: cellA.id, title: "スイッチ")),
            (cellID: KsCellID(cell: cellC), new: LabelCell(id: cellC.id, title: "C2")),
        ])
        pump(cv)

        XCTAssertTrue(
            cv.cellForItem(at: IndexPath(item: 0, section: 0)) is SwitchCellView,
            "具象型の変更後も旧 Renderer の Native cell が残っている"
        )
        XCTAssertEqual(
            renderedTitles(cv, count: 3), ["スイッチ", "B", "C2"],
            "型が変わった行と内容だけ変わった行の双方が表示へ反映されていない"
        )
        XCTAssertTrue(
            thirdRowBefore === cv.cellForItem(at: IndexPath(item: 2, section: 0)),
            "型が変わらない行は同一の行が再構成される（破棄・再生成による差し替えではない）"
        )
        XCTAssertEqual(
            controller.internalDataSource?.snapshot().itemIdentifiers ?? [], beforeItems,
            "行の集合・順序は変化しない（追加・削除・移動なし）"
        )
    }

    /// 具象型が変わる行だけを含むバッチでも、対象行が交換されて反映される。
    func test_replaceCells_具象型変化のみのバッチが反映される() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let sec = Section(header: .text("S"), cells: [cellA, cellB])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        let (_, cv, window) = hostController(store: store)
        defer { window.isHidden = true }

        store.replaceCells([
            (cellID: KsCellID(cell: cellA), new: SwitchCell(id: cellA.id, title: "スイッチ")),
            (cellID: KsCellID(cell: cellB), new: CommandCell(id: cellB.id, title: "コマンド")),
        ])
        pump(cv)

        XCTAssertTrue(
            cv.cellForItem(at: IndexPath(item: 0, section: 0)) is SwitchCellView,
            "1 行目の Native cell が交換されていない"
        )
        XCTAssertTrue(
            cv.cellForItem(at: IndexPath(item: 1, section: 0)) is CommandCellView,
            "2 行目の Native cell が交換されていない"
        )
        XCTAssertEqual(renderedTitles(cv, count: 2), ["スイッチ", "コマンド"])
    }
}
#endif
