// FullSnapshotContentRefreshTests.swift
// KsSettingsViewUITests
//
// full 更新 (`replaceAll` / `replaceSection`) で「同一 ID のまま内容が変わった表示中セル」が
// 表示へ反映されることを検証する。
//
// 検証は内部状態ではなく、window に載せた実物の行へ描画された文字列で行う。model だけを
// 更新して内容再適用の出口を持たない実装では、行の表示が古いまま残り本テスト群が落ちる。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class FullSnapshotContentRefreshTests: XCTestCase {

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
        awaitInitialRender(controller)
        return (controller, cv, window)
    }

    /// 実描画された行タイトルが期待どおりになるまで待つ。
    private func awaitRenderedTitles(
        _ cv: UICollectionView,
        count: Int,
        section: Int = 0,
        equals expected: [String?],
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        awaitEqual(
            "実描画された行タイトル",
            expected: expected,
            in: cv,
            file: file,
            line: line,
            actual: { renderedTitles(cv, count: count, section: section) }
        )
    }

    /// 指定 Section の各行に実際に表示されているタイトル文字列を返す。
    private func renderedTitles(
        _ cv: UICollectionView,
        count: Int,
        section: Int = 0
    ) -> [String?] {
        return (0..<count).map { item in
            let cell = cv.cellForItem(at: IndexPath(item: item, section: section))
            return (cell as? KsListCellBase)?.titleLabel.text
        }
    }

    /// 画面に表示されている実物の Header supplementary から UILabel を取得する。
    private func visibleHeaderLabel(_ cv: UICollectionView, section: Int) -> UILabel? {
        let view = cv.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: section)
        )
        guard let listCell = view as? UICollectionViewListCell else { return nil }
        return listCell.contentView.subviews.compactMap { $0 as? UILabel }.first
    }

    // MARK: - 内容変化の反映

    /// `replaceAll` (`.full`) で同一 ID の表示中セルの内容変更が表示へ反映される。
    func test_full更新で表示中セルの内容変化が反映される() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let cellC = LabelCell(title: "C")
        let sec = Section(header: .text("S"), cells: [cellA, cellB, cellC])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        let (controller, cv, window) = hostController(store: store)
        defer { window.isHidden = true }
        _ = controller

        XCTAssertEqual(renderedTitles(cv, count: 3), ["A", "B", "C"], "初期表示")
        let beforeItems = controller.internalDataSource?.snapshot().itemIdentifiers ?? []

        // 同一 Section ID・同一 Cell ID のまま、A / C のタイトルだけ変えた root を適用する。
        let newSec = Section(
            id: sec.id,
            header: sec.header,
            cells: [
                LabelCell(id: cellA.id, title: "A2"),
                cellB,
                LabelCell(id: cellC.id, title: "C2"),
            ]
        )
        store.replaceAll(SettingsRoot(sections: [newSec]))
        awaitRenderedTitles(cv, count: 3, equals: ["A2", "B", "C2"])

        XCTAssertEqual(
            renderedTitles(cv, count: 3),
            ["A2", "B", "C2"],
            "full 更新で同一 ID の表示中セルの内容変化が表示へ反映されていない"
        )
        XCTAssertEqual(
            controller.internalDataSource?.snapshot().itemIdentifiers ?? [],
            beforeItems,
            "内容変化だけの full 更新で行の挿入・削除が発生している"
        )
    }

    /// 内容変化した行の Native cell は破棄されず、同一インスタンスのまま新しい内容を表示する。
    func test_内容変化した表示中セルの行identityが維持される() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let sec = Section(header: .text("S"), cells: [cellA, cellB])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        let (controller, cv, window) = hostController(store: store)
        defer { window.isHidden = true }
        _ = controller

        let firstRowBefore = cv.cellForItem(at: IndexPath(item: 0, section: 0))
        XCTAssertNotNil(firstRowBefore, "前提: 初期表示の行が取得できない")

        let newSec = Section(
            id: sec.id,
            header: sec.header,
            cells: [LabelCell(id: cellA.id, title: "A2"), cellB]
        )
        store.replaceAll(SettingsRoot(sections: [newSec]))
        awaitRenderedTitles(cv, count: 2, equals: ["A2", "B"])

        let firstRowAfter = cv.cellForItem(at: IndexPath(item: 0, section: 0))
        XCTAssertTrue(
            firstRowBefore === firstRowAfter,
            "内容更新で行の Native cell が破棄・再生成されている"
        )
        XCTAssertEqual(renderedTitles(cv, count: 2), ["A2", "B"])
    }

    /// 挿入・削除・内容変更が同時に起きる full 更新でも、すべてが最新の表示になる。
    func test_構造変更と内容変更が混在するfull更新() {
        let keep = LabelCell(title: "残る")
        let changing = LabelCell(title: "変わる旧")
        let removing = LabelCell(title: "消える")
        let sec = Section(header: .text("S"), cells: [keep, changing, removing])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        let (controller, cv, window) = hostController(store: store)
        defer { window.isHidden = true }
        _ = controller

        XCTAssertEqual(renderedTitles(cv, count: 3), ["残る", "変わる旧", "消える"], "初期表示")

        let inserted = LabelCell(title: "増える")
        let newSec = Section(
            id: sec.id,
            header: sec.header,
            cells: [
                keep,
                LabelCell(id: changing.id, title: "変わる新"),
                inserted,
            ]
        )
        store.replaceAll(SettingsRoot(sections: [newSec]))
        awaitRenderedTitles(cv, count: 3, equals: ["残る", "変わる新", "増える"])

        XCTAssertEqual(
            renderedTitles(cv, count: 3),
            ["残る", "変わる新", "増える"],
            "挿入・削除・内容変更の混在で表示が最新になっていない"
        )
    }

    /// 可視性の変化と内容の変化が同じ full 更新に含まれても、内容側が取りこぼされない。
    func test_可視性と内容の同時変更で内容が取りこぼされない() {
        let visibleCell = LabelCell(title: "表示中旧")
        let hiddenCell = LabelCell(title: "隠れている旧", isVisible: false)
        let sec = Section(header: .text("S"), cells: [visibleCell, hiddenCell])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        let (controller, cv, window) = hostController(store: store)
        defer { window.isHidden = true }
        _ = controller

        XCTAssertEqual(renderedTitles(cv, count: 1), ["表示中旧"], "初期表示は可視 Cell だけ")

        let newSec = Section(
            id: sec.id,
            header: sec.header,
            cells: [
                LabelCell(id: visibleCell.id, title: "表示中新"),
                LabelCell(id: hiddenCell.id, title: "隠れていた新", isVisible: true),
            ]
        )
        store.replaceAll(SettingsRoot(sections: [newSec]))
        awaitRenderedTitles(cv, count: 2, equals: ["表示中新", "隠れていた新"])

        XCTAssertEqual(
            renderedTitles(cv, count: 2),
            ["表示中新", "隠れていた新"],
            "可視化された Cell と、表示中だった Cell の内容の双方が最新になっていない"
        )
    }

    /// `replaceSection` (full 経路へ合流) でも同一 ID Cell の内容変化が表示へ反映される。
    func test_replaceSectionで同一IDCellの内容変化が反映される() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let sec = Section(header: .text("S"), cells: [cellA, cellB])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        let (controller, cv, window) = hostController(store: store)
        defer { window.isHidden = true }
        _ = controller

        XCTAssertEqual(renderedTitles(cv, count: 2), ["A", "B"], "初期表示")

        let newSec = Section(
            id: sec.id,
            header: sec.header,
            cells: [LabelCell(id: cellA.id, title: "A2"), cellB]
        )
        store.replaceSection(sectionID: sec.id, new: newSec)
        awaitRenderedTitles(cv, count: 2, equals: ["A2", "B"])

        XCTAssertEqual(
            renderedTitles(cv, count: 2),
            ["A2", "B"],
            "replaceSection で同一 ID Cell の内容変化が表示へ反映されていない"
        )
    }

    /// header と Cell 内容が同時に変わる full 更新で、両方が最新の表示になる。
    /// Section 全体が再構成されるため、この場合の行 identity の維持は保証しない。
    func test_headerとCell内容の同時変更で両方が反映される() {
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let sec = Section(header: .text("ヘッダ旧"), cells: [cellA, cellB])
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        let (controller, cv, window) = hostController(store: store)
        defer { window.isHidden = true }
        _ = controller

        XCTAssertEqual(visibleHeaderLabel(cv, section: 0)?.text, "ヘッダ旧",
                       "前提: 初期表示の header UILabel が取得できない")
        XCTAssertEqual(renderedTitles(cv, count: 2), ["A", "B"], "初期表示")

        let newSec = Section(
            id: sec.id,
            header: .text("ヘッダ新"),
            cells: [LabelCell(id: cellA.id, title: "A2"), cellB]
        )
        store.replaceAll(SettingsRoot(sections: [newSec]))
        awaitCondition(
            "header と Cell 内容の同時変更が実描画へ届く",
            in: cv,
            actual: {
                "header=\(String(describing: visibleHeaderLabel(cv, section: 0)?.text)) "
                    + "titles=\(renderedTitles(cv, count: 2))"
            },
            until: {
                visibleHeaderLabel(cv, section: 0)?.text == "ヘッダ新"
                    && renderedTitles(cv, count: 2) == ["A2", "B"]
            }
        )

        XCTAssertEqual(visibleHeaderLabel(cv, section: 0)?.text, "ヘッダ新",
                       "header の内容変化が表示へ反映されていない")
        XCTAssertEqual(renderedTitles(cv, count: 2), ["A2", "B"],
                       "header と同時に変わった Cell の内容が表示へ反映されていない")
    }

    /// 同一 UUID のまま具象型が変わる Cell は、Native cell を交換して新しい内容を表示する。
    func test_同一IDで具象型が変わるCellの差し替え() {
        let sharedID = UUID()
        let other = LabelCell(title: "そのまま")
        let sec = Section(
            header: .text("S"),
            cells: [LabelCell(id: sharedID, title: "ラベル"), other]
        )
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        let (controller, cv, window) = hostController(store: store)
        defer { window.isHidden = true }
        _ = controller

        XCTAssertTrue(
            cv.cellForItem(at: IndexPath(item: 0, section: 0)) is LabelCellView,
            "前提: 初期表示の行が LabelCellView ではない"
        )

        let newSec = Section(
            id: sec.id,
            header: sec.header,
            cells: [SwitchCell(id: sharedID, title: "スイッチ"), other]
        )
        store.replaceAll(SettingsRoot(sections: [newSec]))
        awaitRenderedTitles(cv, count: 2, equals: ["スイッチ", "そのまま"])

        XCTAssertEqual(renderedTitles(cv, count: 2), ["スイッチ", "そのまま"],
                       "具象型が変わった Cell の内容が表示へ反映されていない")
        XCTAssertTrue(
            cv.cellForItem(at: IndexPath(item: 0, section: 0)) is SwitchCellView,
            "具象型の変更後も旧 Renderer の Native cell が残っている"
        )
    }

    /// header 変更で再構成される Section に、具象型が変わった Cell が同居していても
    /// 内容は最新で表示される。
    func test_header変更Section内で具象型が変わるCellも反映される() {
        let sharedID = UUID()
        let sec = Section(
            header: .text("ヘッダ旧"),
            cells: [LabelCell(id: sharedID, title: "ラベル")]
        )
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        let (controller, cv, window) = hostController(store: store)
        defer { window.isHidden = true }
        _ = controller

        let newSec = Section(
            id: sec.id,
            header: .text("ヘッダ新"),
            cells: [SwitchCell(id: sharedID, title: "スイッチ")]
        )
        store.replaceAll(SettingsRoot(sections: [newSec]))
        awaitCondition(
            "header 変更 Section 内の具象型変化が実描画へ届く",
            in: cv,
            actual: {
                "header=\(String(describing: visibleHeaderLabel(cv, section: 0)?.text)) "
                    + "titles=\(renderedTitles(cv, count: 1))"
            },
            until: {
                visibleHeaderLabel(cv, section: 0)?.text == "ヘッダ新"
                    && renderedTitles(cv, count: 1) == ["スイッチ"]
            }
        )

        XCTAssertEqual(visibleHeaderLabel(cv, section: 0)?.text, "ヘッダ新")
        XCTAssertEqual(renderedTitles(cv, count: 1), ["スイッチ"])
    }
}
#endif
