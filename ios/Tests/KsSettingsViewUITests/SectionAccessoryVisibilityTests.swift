// SectionAccessoryVisibilityTests.swift
// KsSettingsViewUITests
//
// Section Header / Footer の表示トグル (`isHeaderVisible` / `isFooterVisible`) の表示挙動と、
// Section を内部で再構築する操作をまたいだトグルの保持を検証する (core/ADR-0023)。
//
// 「領域を生成しない」の観測は layout が持つ boundary supplementary の layout attributes と、
// window に載せた実物の supplementary view で行う。supplementaryViewProvider を直接呼ぶ経路は
// 領域の有無に関係なく view を返すため、非生成の観測には使えない。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class SectionAccessoryVisibilityTests: XCTestCase {

    // MARK: - ホスティングと観測ヘルパ

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

    /// レイアウトと再構成を確定させる。
    private func pump(_ view: UIView, seconds: TimeInterval = 0.05) {
        view.setNeedsLayout()
        view.layoutIfNeeded()
        RunLoop.current.run(until: Date().addingTimeInterval(seconds))
        view.setNeedsLayout()
        view.layoutIfNeeded()
    }

    /// layout が当該 Section の Header 領域を持つか。
    private func hasHeaderArea(_ cv: UICollectionView, section: Int) -> Bool {
        return cv.collectionViewLayout.layoutAttributesForSupplementaryView(
            ofKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: section)
        ) != nil
    }

    /// layout が当該 Section の Footer 領域を持つか。
    private func hasFooterArea(_ cv: UICollectionView, section: Int) -> Bool {
        return cv.collectionViewLayout.layoutAttributesForSupplementaryView(
            ofKind: UICollectionView.elementKindSectionFooter,
            at: IndexPath(item: 0, section: section)
        ) != nil
    }

    /// 表示中の Header supplementary の UILabel テキスト。
    private func visibleHeaderText(_ cv: UICollectionView, section: Int) -> String? {
        let view = cv.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: section)
        )
        guard let listCell = view as? UICollectionViewListCell else { return nil }
        return listCell.contentView.subviews.compactMap { $0 as? UILabel }.first?.text
    }

    /// 表示中の Footer supplementary の UILabel テキスト。
    private func visibleFooterText(_ cv: UICollectionView, section: Int) -> String? {
        let view = cv.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionFooter,
            at: IndexPath(item: 0, section: section)
        )
        guard let listCell = view as? UICollectionViewListCell else { return nil }
        return listCell.contentView.subviews.compactMap { $0 as? UILabel }.first?.text
    }

    // MARK: - トグルによる非表示

    func test_内容があるHeaderをトグルで隠すとHeader領域が生成されない() {
        // GIVEN: header に text を持つ Section を isHeaderVisible = false で構築
        let section = KsSettingsViewCore.Section(
            header: .text("一般"),
            footer: .text("補足"),
            cells: [LabelCell(title: "A")],
            isHeaderVisible: false
        )
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section]))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        // THEN: Header 領域は生成されない
        XCTAssertFalse(hasHeaderArea(cv, section: 0),
                       "トグル false の Header で領域が生成されている")
    }

    func test_内容があるFooterをトグルで隠すとFooter領域が生成されない() {
        let section = KsSettingsViewCore.Section(
            header: .text("一般"),
            footer: .text("補足"),
            cells: [LabelCell(title: "A")],
            isFooterVisible: false
        )
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section]))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        XCTAssertFalse(hasFooterArea(cv, section: 0),
                       "トグル false の Footer で領域が生成されている")
    }

    func test_非空accessoryではトグル既定値で従来どおり表示される() {
        // GIVEN: 非空の header / footer を持ちトグルを指定しない Section
        let section = KsSettingsViewCore.Section(
            header: .text("一般"),
            footer: .text("補足"),
            cells: [LabelCell(title: "A")]
        )
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section]))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        XCTAssertTrue(hasHeaderArea(cv, section: 0), "既定値で Header 領域が生成されていない")
        XCTAssertTrue(hasFooterArea(cv, section: 0), "既定値で Footer 領域が生成されていない")
        XCTAssertEqual(visibleHeaderText(cv, section: 0), "一般")
        XCTAssertEqual(visibleFooterText(cv, section: 0), "補足")
    }

    func test_Headerを隠してもFooterとCellは表示されたまま() {
        let section = KsSettingsViewCore.Section(
            header: .text("一般"),
            footer: .text("補足"),
            cells: [LabelCell(title: "A"), LabelCell(title: "B")],
            isHeaderVisible: false
        )
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section]))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        XCTAssertFalse(hasHeaderArea(cv, section: 0))
        XCTAssertTrue(hasFooterArea(cv, section: 0), "Header を隠すと Footer まで消えている")
        XCTAssertEqual(visibleFooterText(cv, section: 0), "補足")
        XCTAssertEqual(cv.numberOfItems(inSection: 0), 2, "Header を隠すと Cell まで消えている")
    }

    // MARK: - replaceSection によるトグル変更の反映

    func test_replaceSectionでHeaderトグル変更が両方向に反映される() {
        let sectionID = UUID()
        let section = KsSettingsViewCore.Section(
            id: sectionID,
            header: .text("一般"),
            footer: .text("補足"),
            cells: [LabelCell(title: "A")]
        )
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section]))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }
        XCTAssertTrue(hasHeaderArea(cv, section: 0), "前提: 初期表示で Header 領域が無い")

        // WHEN: isHeaderVisible = false にした Section で置換
        store.replaceSection(sectionID: sectionID, new: KsSettingsViewCore.Section(
            id: sectionID,
            header: .text("一般"),
            footer: .text("補足"),
            cells: section.cells,
            isHeaderVisible: false
        ))
        pump(cv)

        // THEN: Header 領域が消える
        XCTAssertFalse(hasHeaderArea(cv, section: 0),
                       "replaceSection のトグル false が表示へ届いていない")
        XCTAssertTrue(hasFooterArea(cv, section: 0), "Footer 領域まで消えている")

        // WHEN: true に戻す
        store.replaceSection(sectionID: sectionID, new: KsSettingsViewCore.Section(
            id: sectionID,
            header: .text("一般"),
            footer: .text("補足"),
            cells: section.cells,
            isHeaderVisible: true
        ))
        pump(cv)

        // THEN: 再表示される
        XCTAssertTrue(hasHeaderArea(cv, section: 0),
                      "replaceSection のトグル true が表示へ届いていない")
        XCTAssertEqual(visibleHeaderText(cv, section: 0), "一般")
    }

    func test_replaceSectionでFooterトグル変更が両方向に反映される() {
        let sectionID = UUID()
        let cells: [any KsCell] = [LabelCell(title: "A")]
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [
            KsSettingsViewCore.Section(
                id: sectionID, header: .text("一般"), footer: .text("補足"), cells: cells
            )
        ]))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }
        XCTAssertTrue(hasFooterArea(cv, section: 0), "前提: 初期表示で Footer 領域が無い")

        store.replaceSection(sectionID: sectionID, new: KsSettingsViewCore.Section(
            id: sectionID, header: .text("一般"), footer: .text("補足"), cells: cells,
            isFooterVisible: false
        ))
        pump(cv)
        XCTAssertFalse(hasFooterArea(cv, section: 0),
                       "replaceSection のトグル false が Footer 表示へ届いていない")
        XCTAssertTrue(hasHeaderArea(cv, section: 0), "Header 領域まで消えている")

        store.replaceSection(sectionID: sectionID, new: KsSettingsViewCore.Section(
            id: sectionID, header: .text("一般"), footer: .text("補足"), cells: cells,
            isFooterVisible: true
        ))
        pump(cv)
        XCTAssertTrue(hasFooterArea(cv, section: 0),
                      "replaceSection のトグル true が Footer 表示へ届いていない")
        XCTAssertEqual(visibleFooterText(cv, section: 0), "補足")
    }

    // MARK: - 非表示中の内容更新

    func test_非表示中にupdateAccessoryしたHeader内容が再表示に反映される() {
        let sectionID = UUID()
        let cells: [any KsCell] = [LabelCell(title: "A")]
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [
            KsSettingsViewCore.Section(
                id: sectionID, header: .text("旧ヘッダ"), cells: cells, isHeaderVisible: false
            )
        ]))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }
        XCTAssertFalse(hasHeaderArea(cv, section: 0), "前提: 初期表示で Header が隠れていない")

        // WHEN: 非表示のまま header text を更新し、その後トグルを true に戻す
        store.updateAccessory(
            target: .sectionHeader(sectionID: sectionID),
            accessory: .section(.text("新ヘッダ"))
        )
        pump(cv)
        XCTAssertFalse(hasHeaderArea(cv, section: 0),
                       "非表示中の内容更新で Header が表示されてしまっている")

        store.replaceSection(sectionID: sectionID, new: KsSettingsViewCore.Section(
            id: sectionID, header: store.root.sections[0].header, cells: cells,
            isHeaderVisible: true
        ))
        pump(cv)

        // THEN: 更新後の text で表示される
        XCTAssertTrue(hasHeaderArea(cv, section: 0))
        XCTAssertEqual(visibleHeaderText(cv, section: 0), "新ヘッダ",
                       "非表示中の内容更新が再表示に反映されていない")
    }

    // MARK: - 内容不在の統一判定

    func test_空textのHeaderは領域を生成しない() {
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [
            KsSettingsViewCore.Section(
                header: .text(""), footer: .text("補足"), cells: [LabelCell(title: "A")]
            )
        ]))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        XCTAssertFalse(hasHeaderArea(cv, section: 0), "空 text の Header で領域が生成されている")
    }

    func test_空textのFooterは領域を生成しない() {
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [
            KsSettingsViewCore.Section(
                header: .text("一般"), footer: .text(""), cells: [LabelCell(title: "A")]
            )
        ]))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        XCTAssertFalse(hasFooterArea(cv, section: 0), "空 text の Footer で領域が生成されている")
    }

    // MARK: - 高さ解決は存在判定の後

    func test_header不在ならSectionのheaderHeight正値でも領域を生成しない() {
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [
            KsSettingsViewCore.Section(
                header: nil, footer: .text("補足"), cells: [LabelCell(title: "A")], headerHeight: 40
            )
        ]))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        XCTAssertFalse(hasHeaderArea(cv, section: 0),
                       "header 不在 + Section.headerHeight 正値で領域が生成されている")
    }

    func test_空textのheaderはSectionのheaderHeight正値でも領域を生成しない() {
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [
            KsSettingsViewCore.Section(
                header: .text(""), footer: .text("補足"), cells: [LabelCell(title: "A")], headerHeight: 40
            )
        ]))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        XCTAssertFalse(hasHeaderArea(cv, section: 0),
                       "空 text の header + Section.headerHeight 正値で領域が生成されている")
    }

    func test_header不在ならThemeのheaderHeightがあっても領域を生成しない() {
        let store = SettingsRootStore(
            initialRoot: SettingsRoot(sections: [
                KsSettingsViewCore.Section(
                    header: nil, footer: .text("補足"), cells: [LabelCell(title: "A")]
                )
            ]),
            initialTheme: Theme(headerHeight: 60)
        )
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        XCTAssertFalse(hasHeaderArea(cv, section: 0),
                       "header 不在 + Theme.headerHeight 正値で領域が生成されている")
    }

    func test_トグルfalseなら高さ指定があっても領域を生成しない() {
        let store = SettingsRootStore(
            initialRoot: SettingsRoot(sections: [
                KsSettingsViewCore.Section(
                    header: .text("一般"),
                    footer: .text("補足"),
                    cells: [LabelCell(title: "A")],
                    headerHeight: 40,
                    isHeaderVisible: false
                )
            ]),
            initialTheme: Theme(headerHeight: 60)
        )
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        XCTAssertFalse(hasHeaderArea(cv, section: 0),
                       "トグル false + 高さ指定で領域が生成されている")
    }

    /// 内容ありのまま表示する場合、高さ解決は従来どおり働く (高さ契約の退行防止)。
    func test_内容がありトグルtrueならheaderHeightの固定高さが効く() throws {
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [
            KsSettingsViewCore.Section(
                header: .text("一般"), cells: [LabelCell(title: "A")], headerHeight: 40
            )
        ]))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        let attributes = try XCTUnwrap(cv.collectionViewLayout.layoutAttributesForSupplementaryView(
            ofKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: 0)
        ), "表示する Header の領域が生成されていない")
        XCTAssertEqual(attributes.frame.height, 40, accuracy: 0.5,
                       "表示する Header に固定高さが適用されていない")
    }

    // MARK: - Cell 操作をまたいだトグルの保持

    /// Store の Cell 操作は Section を再構築するため、トグル値が既定 true へ戻らないことを確認する。
    func test_StoreのCell操作をまたいでトグルが保持される() {
        let sectionID = UUID()
        let cellA = LabelCell(title: "A")
        let cellB = LabelCell(title: "B")
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [
            KsSettingsViewCore.Section(
                id: sectionID,
                header: .text("一般"),
                footer: .text("補足"),
                cells: [cellA, cellB],
                isHeaderVisible: false,
                isFooterVisible: false
            )
        ]))

        func assertToggles(_ label: String) {
            let section = store.root.sections[0]
            XCTAssertFalse(section.isHeaderVisible, "\(label) で isHeaderVisible が失われた")
            XCTAssertFalse(section.isFooterVisible, "\(label) で isFooterVisible が失われた")
        }

        store.insertCell(LabelCell(title: "C"), in: sectionID, at: 0)
        assertToggles("insertCell")

        store.moveCell(cellID: KsCellID(cell: cellA), to: 0)
        assertToggles("moveCell")

        store.replaceCell(cellID: KsCellID(cell: cellA), new: LabelCell(id: cellA.id, title: "A2"))
        assertToggles("replaceCell")

        store.replaceCells([(cellID: KsCellID(cell: cellB), new: LabelCell(id: cellB.id, title: "B2"))])
        assertToggles("replaceCells")

        store.removeCell(cellID: KsCellID(cell: cellB))
        assertToggles("removeCell")

        store.updateAccessory(
            target: .sectionHeader(sectionID: sectionID),
            accessory: .section(.text("一般2"))
        )
        assertToggles("updateAccessory(header)")

        store.updateAccessory(
            target: .sectionFooter(sectionID: sectionID),
            accessory: .section(.text("補足2"))
        )
        assertToggles("updateAccessory(footer)")
    }

    /// Cell 操作を Controller の部分 Diff 経路に通しても、Header は非表示のままである。
    func test_Cell挿入をまたいでHeaderが非表示のまま() {
        let sectionID = UUID()
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [
            KsSettingsViewCore.Section(
                id: sectionID,
                header: .text("一般"),
                cells: [LabelCell(title: "A")],
                isHeaderVisible: false
            )
        ]))
        let (controller, cv, window) = hostInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }
        XCTAssertFalse(hasHeaderArea(cv, section: 0), "前提: 初期表示で Header が隠れていない")

        store.insertCell(LabelCell(title: "B"), in: sectionID, at: 1)
        pump(cv)

        XCTAssertEqual(cv.numberOfItems(inSection: 0), 2, "前提: Cell が挿入されていない")
        XCTAssertFalse(hasHeaderArea(cv, section: 0),
                       "Cell 挿入で Header のトグルが既定値へ戻っている")
    }

    /// visible projection の構築でもトグルが保持される。
    func test_visible_projectionでトグルが保持される() {
        let sections = [
            KsSettingsViewCore.Section(
                header: .text("一般"),
                footer: .text("補足"),
                cells: [LabelCell(title: "A")],
                isHeaderVisible: false,
                isFooterVisible: false
            )
        ]
        let projected = KsSettingsViewController.computeVisibleSections(from: sections)
        XCTAssertEqual(projected.count, 1)
        XCTAssertFalse(projected[0].isHeaderVisible)
        XCTAssertFalse(projected[0].isFooterVisible)
    }
}
#endif
