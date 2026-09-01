// SectionAccessoryRenderingTests.swift
// KsSettingsViewUITests
//
// Section H/F の text / view ケースが正しく描画されるかを検証する。
// XCTest 環境では `UICollectionView` のレイアウトパスを完全には走らせないため、
// supplementaryViewProvider が想定通りのセル種別を返すかをホワイトボックス検証する。
//
// root の構築は internal init で行い、更新は `applyDiff(...)` を経由する
// （`controller.root` への直接代入は行わない）。
//
// 「表示中 supplementary への反映」節のテストは、provider 経由の新規生成ではなく
// window に載せた実物の supplementary view を観測する。

#if canImport(UIKit)
import XCTest
import UIKit
import SwiftUI
import KsSettingsViewTestSupport
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class SectionAccessoryRenderingTests: XCTestCase {
    private func runLayoutPass(_ controller: KsSettingsViewController) {
        let cv = controller.internalCollectionView
        cv.frame = CGRect(x: 0, y: 0, width: 320, height: 480)
        cv.layoutIfNeeded()
    }

    /// 指定 root を初期 root として controller を構築するヘルパ。
    private func makeController(root: SettingsRoot, theme: Theme = Theme()) -> KsSettingsViewController {
        let controller = KsSettingsViewController(root: root, theme: theme)
        _ = controller.view
        return controller
    }

    func test_textヘッダのsupplementaryが表示される() {
        let section = KsSettingsViewCore.Section(
            header: .text("一般"),
            cells: [LabelCell(title: "A")]
        )
        let controller = makeController(root: SettingsRoot(sections: [section]))
        runLayoutPass(controller)

        let cv = controller.internalCollectionView
        let indexPath = IndexPath(item: 0, section: 0)
        let view = cv.dataSource?.collectionView?(
            cv,
            viewForSupplementaryElementOfKind: UICollectionView.elementKindSectionHeader,
            at: indexPath
        )
        XCTAssertNotNil(view)
        // テキスト accessory も view 形式と同じ `UICollectionViewListCell` 経路で描画される。
        XCTAssertTrue(view is UICollectionViewListCell,
                      "テキストヘッダの supplementary view は UICollectionViewListCell でなければならない")
    }

    func test_view_swiftUIヘッダが描画コンフィグレーションを持つ() {
        let header = SectionAccessory.view(KsAnyView.swiftUI {
            Text("Profile")
        })
        let section = KsSettingsViewCore.Section(
            header: header,
            cells: [LabelCell(title: "A")]
        )
        let controller = makeController(root: SettingsRoot(sections: [section]))
        runLayoutPass(controller)

        let cv = controller.internalCollectionView
        let indexPath = IndexPath(item: 0, section: 0)
        let view = cv.dataSource?.collectionView?(
            cv,
            viewForSupplementaryElementOfKind: UICollectionView.elementKindSectionHeader,
            at: indexPath
        )
        guard let listCell = view as? UICollectionViewListCell else {
            XCTFail("supplementary view が UICollectionViewListCell ではない")
            return
        }
        XCTAssertNotNil(listCell.contentConfiguration)
    }

    func test_view_uiKitヘッダがaddSubviewされる() {
        let header = SectionAccessory.view(KsAnyView.uiKit {
            let v = UIView()
            v.backgroundColor = .red
            return v
        })
        let section = KsSettingsViewCore.Section(
            header: header,
            cells: [LabelCell(title: "A")]
        )
        let controller = makeController(root: SettingsRoot(sections: [section]))
        runLayoutPass(controller)

        let cv = controller.internalCollectionView
        let indexPath = IndexPath(item: 0, section: 0)
        let view = cv.dataSource?.collectionView?(
            cv,
            viewForSupplementaryElementOfKind: UICollectionView.elementKindSectionHeader,
            at: indexPath
        )
        guard let listCell = view as? UICollectionViewListCell else {
            XCTFail("supplementary view が UICollectionViewListCell ではない")
            return
        }
        XCTAssertFalse(listCell.contentView.subviews.isEmpty)
    }

    func test_view形式の中身差し替えで再描画される() {
        let header1 = SectionAccessory.view(KsAnyView.swiftUI { Text("v1") })
        let section1 = KsSettingsViewCore.Section(id: UUID(), header: header1, cells: [LabelCell(title: "A")])
        let controller = makeController(root: SettingsRoot(sections: [section1]))
        runLayoutPass(controller)

        // 同じ section.id で view 中身だけ差し替え（replaceSection Diff 経由）
        let header2 = SectionAccessory.view(KsAnyView.swiftUI { Text("v2") })
        let section2 = KsSettingsViewCore.Section(id: section1.id, header: header2, cells: section1.cells)
        controller.applyDiff(.replaceSection(sectionID: section1.id, new: section2))
        runLayoutPass(controller)

        let cv = controller.internalCollectionView
        let view = cv.dataSource?.collectionView?(
            cv,
            viewForSupplementaryElementOfKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: 0)
        )
        XCTAssertNotNil(view)
    }

    func test_text形式ヘッダの文字列更新でcontentConfigurationが新しいテキストを保持する() {
        let sectionID = UUID()
        let section1 = KsSettingsViewCore.Section(
            id: sectionID,
            header: .text("A"),
            cells: [LabelCell(title: "X")]
        )
        let controller = makeController(root: SettingsRoot(sections: [section1]))
        runLayoutPass(controller)

        // updateAccessory Diff 経由でテキストを更新
        controller.applyDiff(.updateAccessory(
            target: .sectionHeader(sectionID: sectionID),
            accessory: .section(.text("B"))
        ))
        runLayoutPass(controller)

        let cv = controller.internalCollectionView
        let view = cv.dataSource?.collectionView?(
            cv,
            viewForSupplementaryElementOfKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: 0)
        )
        // テキスト accessory は `UICollectionViewListCell.contentView` 内に追加された UILabel で
        // 描画される（`applyAccessoryLabel` 経路）。
        guard let listCell = view as? UICollectionViewListCell else {
            XCTFail("supplementary view が UICollectionViewListCell ではない")
            return
        }
        let label = listCell.contentView.subviews.compactMap { $0 as? UILabel }.first
        XCTAssertEqual(label?.text, "B",
                       "text 形式ヘッダの内容更新で UILabel.text が再構成されていない")
    }

    func test_view形式ヘッダの差し替えでapplyAccessoryToListCellが新しいcontentConfigurationを設定する() {
        let controller = makeController(root: SettingsRoot())

        let listCell = UICollectionViewListCell()

        // 1 回目: SwiftUI backing
        controller.applyAccessoryToListCell(
            listCell,
            accessoryText: nil,
            accessoryView: KsAnyView.swiftUI { Text("v1") },
            textColor: .label,
            verticalAlignment: .center
        )
        XCTAssertNotNil(listCell.contentConfiguration, "1 回目: contentConfiguration が設定されていない")

        // 2 回目: uiKit backing
        let markerView = UIView()
        markerView.tag = 4242
        controller.applyAccessoryToListCell(
            listCell,
            accessoryText: nil,
            accessoryView: KsAnyView.uiKit { markerView },
            textColor: .label,
            verticalAlignment: .center
        )
        XCTAssertNil(listCell.contentConfiguration, "uiKit backing 適用時に contentConfiguration がクリアされていない")
        XCTAssertTrue(
            listCell.contentView.subviews.contains(where: { $0.tag == 4242 }),
            "uiKit backing で挿入した UIView が contentView に存在しない"
        )

        // 3 回目: 再度 SwiftUI backing
        controller.applyAccessoryToListCell(
            listCell,
            accessoryText: nil,
            accessoryView: KsAnyView.swiftUI { Text("v3") },
            textColor: .label,
            verticalAlignment: .center
        )
        XCTAssertNotNil(listCell.contentConfiguration, "3 回目: SwiftUI backing 再適用時に contentConfiguration が再設定されていない")
        XCTAssertFalse(
            listCell.contentView.subviews.contains(where: { $0.tag == 4242 }),
            "前回の uiKit backing の subview がクリアされていない"
        )
    }

    func test_textフッタも描画できる() {
        let section = KsSettingsViewCore.Section(
            header: nil,
            footer: .text("バージョン 1.0"),
            cells: [LabelCell(title: "A")]
        )
        let controller = makeController(root: SettingsRoot(sections: [section]))
        runLayoutPass(controller)

        let cv = controller.internalCollectionView
        let indexPath = IndexPath(item: 0, section: 0)
        let view = cv.dataSource?.collectionView?(
            cv,
            viewForSupplementaryElementOfKind: UICollectionView.elementKindSectionFooter,
            at: indexPath
        )
        XCTAssertNotNil(view)
    }

    /// Footer の文字色は `Theme.footerTextColor` が使われる（Header の `headerTextColor` と区別される）。
    ///
    /// テキスト accessory は `UICollectionViewListCell.contentView` 内の UILabel で描画され、
    /// `label.textColor` に Theme 由来の色が適用される。
    func test_Footerの文字色はfooterTextColorが使われる() {
        let footerColor = UIColor(red: 0.5, green: 0.5, blue: 0.5, alpha: 1.0)
        let headerColor = UIColor(red: 1.0, green: 0.0, blue: 0.0, alpha: 1.0)
        let theme = Theme(
            headerTextColor: headerColor,
            footerTextColor: footerColor
        )
        let section = KsSettingsViewCore.Section(
            header: .text("H"),
            footer: .text("F"),
            cells: [LabelCell(title: "A")]
        )
        let controller = makeController(root: SettingsRoot(sections: [section]), theme: theme)
        runLayoutPass(controller)

        let cv = controller.internalCollectionView
        let footerIP = IndexPath(item: 0, section: 0)
        let footerCell = cv.dataSource?.collectionView?(
            cv,
            viewForSupplementaryElementOfKind: UICollectionView.elementKindSectionFooter,
            at: footerIP
        ) as? UICollectionViewListCell
        let footerLabel = footerCell?.contentView.subviews.compactMap { $0 as? UILabel }.first
        XCTAssertEqual(footerLabel?.textColor, footerColor)

        let headerIP = IndexPath(item: 0, section: 0)
        let headerCell = cv.dataSource?.collectionView?(
            cv,
            viewForSupplementaryElementOfKind: UICollectionView.elementKindSectionHeader,
            at: headerIP
        ) as? UICollectionViewListCell
        let headerLabel = headerCell?.contentView.subviews.compactMap { $0 as? UILabel }.first
        XCTAssertEqual(headerLabel?.textColor, headerColor)
    }

    /// `Theme.footerTextColor` を明示指定しない（= 既定の `Theme.defaultFooterTextColor` のまま）
    /// 場合、Footer ラベルの文字色は `defaultFooterTextColor` 相当の固定 RGB グレー（≒ #6D6D72）で
    /// 描画される。AiForms.Maui.SettingsView オリジナルの `UIColor.Gray` 固定 RGB に揃える方針。
    func test_Footerの文字色は未指定時にdefaultFooterTextColorが使われる() {
        // 既定 Theme（footerTextColor を明示指定しない）。
        let theme = Theme()
        let section = KsSettingsViewCore.Section(
            header: nil,
            footer: .text("F"),
            cells: [LabelCell(title: "A")]
        )
        let controller = makeController(root: SettingsRoot(sections: [section]), theme: theme)
        runLayoutPass(controller)

        let cv = controller.internalCollectionView
        let footerIP = IndexPath(item: 0, section: 0)
        // テキスト accessory は `UICollectionViewListCell.contentView` 内の UILabel で描画される。
        let footerCell = cv.dataSource?.collectionView?(
            cv,
            viewForSupplementaryElementOfKind: UICollectionView.elementKindSectionFooter,
            at: footerIP
        ) as? UICollectionViewListCell
        let footerLabel = footerCell?.contentView.subviews.compactMap { $0 as? UILabel }.first

        // 既定値（defaultFooterTextColor = UIColor(0.43, 0.43, 0.45, 1.0) ≒ #6D6D72）相当の
        // 固定グレーが使用されることを期待する。dynamic color（UIColor.secondaryLabel）への
        // 自動分岐は行わない。
        let expected = Theme.defaultFooterTextColor
        XCTAssertEqual(
            footerLabel?.textColor,
            expected,
            "footerTextColor 未指定時は defaultFooterTextColor 相当の固定 RGB グレーが使われる"
        )
    }

    // MARK: - Header / Footer の垂直配置

    /// Header テキストは UILabel が listCell.contentView の bottomAnchor に張り付く制約で描画される。
    /// AiForms `TextHeaderView.SetVerticalAlignment(LayoutAlignment.End)` 既定挙動。
    ///
    /// テキスト accessory は `UICollectionViewListCell.contentView` 内に UILabel + AutoLayout で
    /// 描画される（`applyAccessoryLabel` 経路）。
    func test_Headerテキストは下端揃えのUILabelで描画される() {
        let section = KsSettingsViewCore.Section(
            header: .text("CommandCell"),
            cells: [LabelCell(title: "A")],
            headerHeight: 60
        )
        let controller = makeController(root: SettingsRoot(sections: [section]))
        runLayoutPass(controller)

        let cv = controller.internalCollectionView
        let listCell = cv.dataSource?.collectionView?(
            cv,
            viewForSupplementaryElementOfKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: 0)
        ) as? UICollectionViewListCell
        XCTAssertNotNil(listCell, "Header の supplementary view（UICollectionViewListCell）が取得できない")
        let contentView = listCell?.contentView
        let label = contentView?.subviews.compactMap { $0 as? UILabel }.first
        XCTAssertNotNil(label, "Header の UILabel が contentView に追加されていない")
        XCTAssertEqual(label?.text, "CommandCell")

        // 制約検証: label.bottomAnchor が contentView.bottomAnchor と == 関係で結ばれている。
        let constraints = contentView?.constraints ?? []
        let bottomConstraint = constraints.first { c in
            (c.firstAnchor === label?.bottomAnchor || c.secondAnchor === label?.bottomAnchor) &&
                (c.firstAnchor === contentView?.bottomAnchor || c.secondAnchor === contentView?.bottomAnchor) &&
                c.relation == .equal
        }
        XCTAssertNotNil(bottomConstraint,
                        "Header label の bottomAnchor == contentView.bottomAnchor 制約が見つからない")
    }

    /// Footer テキストは UILabel が listCell.contentView の topAnchor に張り付く制約で描画される。
    /// AiForms `TextFooterView` の TopAnchor 既定挙動。
    func test_Footerテキストは上端揃えのUILabelで描画される() {
        let section = KsSettingsViewCore.Section(
            header: nil,
            footer: .text("You can select either TypeA or TypeB."),
            cells: [LabelCell(title: "A")]
        )
        let controller = makeController(root: SettingsRoot(sections: [section]))
        runLayoutPass(controller)

        let cv = controller.internalCollectionView
        let listCell = cv.dataSource?.collectionView?(
            cv,
            viewForSupplementaryElementOfKind: UICollectionView.elementKindSectionFooter,
            at: IndexPath(item: 0, section: 0)
        ) as? UICollectionViewListCell
        XCTAssertNotNil(listCell, "Footer の supplementary view（UICollectionViewListCell）が取得できない")
        let contentView = listCell?.contentView
        let label = contentView?.subviews.compactMap { $0 as? UILabel }.first
        XCTAssertNotNil(label, "Footer の UILabel が contentView に追加されていない")
        XCTAssertEqual(label?.text, "You can select either TypeA or TypeB.")

        // 制約検証: label.topAnchor が contentView.topAnchor と == 関係で結ばれている。
        let constraints = contentView?.constraints ?? []
        let topConstraint = constraints.first { c in
            (c.firstAnchor === label?.topAnchor || c.secondAnchor === label?.topAnchor) &&
                (c.firstAnchor === contentView?.topAnchor || c.secondAnchor === contentView?.topAnchor) &&
                c.relation == .equal
        }
        XCTAssertNotNil(topConstraint,
                        "Footer label の topAnchor == contentView.topAnchor 制約が見つからない")
    }

    // MARK: - 表示中 supplementary への反映

    /// controller を window に載せ、supplementary の実描画を確定させる。
    private func hostControllerInWindow(
        root: SettingsRoot,
        theme: Theme = Theme()
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let controller = KsSettingsViewController(root: root, theme: theme)
        let size = CGSize(width: 375, height: 600)
        let rootView = controller.view!
        rootView.frame = CGRect(origin: .zero, size: size)
        let window = UIWindow(frame: rootView.frame)
        window.addSubview(rootView)
        window.makeKeyAndVisible()
        rootView.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        awaitInitialRender(controller)
        return (controller, cv, window)
    }

    /// `SettingsRootStore` に接続した controller を window に載せ、supplementary の実描画を確定させる。
    /// 更新は Store の公開操作から Publisher 経由で controller へ届く経路を通る。
    ///
    /// Store 購読の `AnyCancellable` は controller 自身が所有し、購読は `[weak self]` で張られる。
    /// controller が解放されると更新が届かなくなるため、`window.rootViewController` に設定して
    /// window が controller を強参照する所有関係を作る (呼び出し側も戻り値の controller を
    /// テスト終了まで保持する)。
    private func hostStoreConnectedControllerInWindow(
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
        awaitInitialRender(controller)
        return (controller, cv, window)
    }

    /// 画面に表示されている実物の Header supplementary から UILabel を取得する。
    /// provider 経由の新規生成ではなく、collection view が現在保持している view を対象にする。
    private func visibleHeaderLabel(_ cv: UICollectionView, section: Int) -> UILabel? {
        let view = cv.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: section)
        )
        guard let listCell = view as? UICollectionViewListCell else { return nil }
        return listCell.contentView.subviews.compactMap { $0 as? UILabel }.first
    }

    /// 画面に表示されている実物の Footer supplementary から UILabel を取得する。
    private func visibleFooterLabel(_ cv: UICollectionView, section: Int) -> UILabel? {
        let view = cv.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionFooter,
            at: IndexPath(item: 0, section: section)
        )
        guard let listCell = view as? UICollectionViewListCell else { return nil }
        return listCell.contentView.subviews.compactMap { $0 as? UILabel }.first
    }

    /// 表示中の Host に対して未知 sectionID の `updateAccessory` を呼んでも、Store が Diff を
    /// 発行しないため Host の missing ID 検出 (DEBUG ビルドの assertion) に到達せず、表示も
    /// 変化しない (core/ADR-0020)。
    ///
    /// 呼び出し後に既知 sectionID の更新が表示へ届くことまで確認し、Store 購読が生きている
    /// ことも合わせて観察する。
    func test_Store経由の未知sectionIDのupdateAccessoryは表示に影響しない() {
        let section = KsSettingsViewCore.Section(
            header: .text("ヘッダA"),
            footer: .text("フッタA"),
            cells: [LabelCell(title: "X")]
        )
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section]))
        let (controller, cv, window) = hostStoreConnectedControllerInWindow(store: store)
        defer { window.isHidden = true }
        withExtendedLifetime(controller) {}

        XCTAssertEqual(visibleHeaderLabel(cv, section: 0)?.text, "ヘッダA",
                       "前提: 初期表示の header UILabel が取得できない")
        XCTAssertEqual(visibleFooterLabel(cv, section: 0)?.text, "フッタA",
                       "前提: 初期表示の footer UILabel が取得できない")

        let unknownID = UUID()
        store.updateAccessory(target: .sectionHeader(sectionID: unknownID), accessory: .section(.text("ヘッダX")))
        store.updateAccessory(target: .sectionFooter(sectionID: unknownID), accessory: .section(.text("フッタX")))
        waitForNegativeVerification(in: cv)

        XCTAssertEqual(visibleHeaderLabel(cv, section: 0)?.text, "ヘッダA",
                       "未知 sectionID の updateAccessory で header 表示が変化している")
        XCTAssertEqual(visibleFooterLabel(cv, section: 0)?.text, "フッタA",
                       "未知 sectionID の updateAccessory で footer 表示が変化している")

        store.updateAccessory(target: .sectionHeader(sectionID: section.id), accessory: .section(.text("ヘッダB")))
        awaitEqual(
            "表示中 header の実描画テキスト",
            expected: "ヘッダB" as String?,
            in: cv,
            actual: { visibleHeaderLabel(cv, section: 0)?.text }
        )

        XCTAssertEqual(visibleHeaderLabel(cv, section: 0)?.text, "ヘッダB",
                       "後続の既知 sectionID の更新が表示へ届かない (Store 購読が生きていない)")
    }

    /// Section identity を保ったまま header text を変える replaceSection は、
    /// 表示中の supplementary の UILabel まで更新する。
    func test_replaceSectionのtextヘッダ変更が表示中のsupplementaryに反映される() {
        let section1 = KsSettingsViewCore.Section(
            header: .text("ヘッダA"),
            cells: [LabelCell(title: "X")]
        )
        let (controller, cv, window) = hostControllerInWindow(root: SettingsRoot(sections: [section1]))
        defer { window.isHidden = true }

        XCTAssertEqual(visibleHeaderLabel(cv, section: 0)?.text, "ヘッダA",
                       "前提: 初期表示の header UILabel が取得できない")

        let section2 = KsSettingsViewCore.Section(
            id: section1.id,
            header: .text("ヘッダB"),
            cells: section1.cells
        )
        controller.applyDiff(.replaceSection(sectionID: section1.id, new: section2))
        awaitEqual(
            "表示中 header の実描画テキスト",
            expected: "ヘッダB" as String?,
            in: cv,
            actual: { visibleHeaderLabel(cv, section: 0)?.text }
        )

        XCTAssertEqual(visibleHeaderLabel(cv, section: 0)?.text, "ヘッダB",
                       "replaceSection の header text 変更が表示中の supplementary に反映されていない")
    }

    /// Section identity を保ったまま header text を変える full Diff も、
    /// 表示中の supplementary の UILabel まで更新する。
    func test_fullDiffのtextヘッダ変更が表示中のsupplementaryに反映される() {
        let section1 = KsSettingsViewCore.Section(
            header: .text("ヘッダA"),
            cells: [LabelCell(title: "X")]
        )
        let (controller, cv, window) = hostControllerInWindow(root: SettingsRoot(sections: [section1]))
        defer { window.isHidden = true }

        XCTAssertEqual(visibleHeaderLabel(cv, section: 0)?.text, "ヘッダA",
                       "前提: 初期表示の header UILabel が取得できない")

        let section2 = KsSettingsViewCore.Section(
            id: section1.id,
            header: .text("ヘッダB"),
            cells: section1.cells
        )
        controller.applyDiff(.full(SettingsRoot(sections: [section2])))
        awaitEqual(
            "表示中 header の実描画テキスト",
            expected: "ヘッダB" as String?,
            in: cv,
            actual: { visibleHeaderLabel(cv, section: 0)?.text }
        )

        XCTAssertEqual(visibleHeaderLabel(cv, section: 0)?.text, "ヘッダB",
                       "full Diff の header text 変更が表示中の supplementary に反映されていない")
    }

    /// header 不変の full Diff は reload を発行せず、表示中の Cell を再構成しない。
    /// header 変化の検出付き reload が無条件 reload に緩められると本テストが落ちる。
    func test_fullDiffでheader不変ならCellは再構成されない() {
        let section1 = KsSettingsViewCore.Section(
            header: .text("ヘッダA"),
            cells: [LabelCell(title: "X")]
        )
        let (controller, cv, window) = hostControllerInWindow(root: SettingsRoot(sections: [section1]))
        defer { window.isHidden = true }

        let beforeCell = cv.cellForItem(at: IndexPath(item: 0, section: 0))
        XCTAssertNotNil(beforeCell, "前提: 初期表示の Cell が取得できない")

        let section2 = KsSettingsViewCore.Section(
            id: section1.id,
            header: .text("ヘッダA"),
            cells: section1.cells
        )
        controller.applyDiff(.full(SettingsRoot(sections: [section2])))
        waitForNegativeVerification(in: cv)

        let afterCell = cv.cellForItem(at: IndexPath(item: 0, section: 0))
        XCTAssertTrue(beforeCell === afterCell,
                      "header 不変の full Diff で Cell が再構成されている (不要な reload が発行されている)")
    }

    // MARK: - headerHeight の表示反映

    /// 画面に表示されている実物の Header supplementary の実高さ (frame) を返す。
    /// layout attributes だけでなく、実際に配置された view の frame を観測する。
    private func visibleHeaderFrameHeight(_ cv: UICollectionView, section: Int) -> CGFloat? {
        let indexPath = IndexPath(item: 0, section: section)
        let view = cv.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionHeader,
            at: indexPath
        )
        return view?.frame.height
    }

    /// 画面に表示されている実物の Cell から title のテキストを取得する。
    private func visibleCellTitle(_ cv: UICollectionView, section: Int, item: Int) -> String? {
        let cell = cv.cellForItem(at: IndexPath(item: item, section: section))
        return (cell as? KsListCellBase)?.titleLabel.text
    }

    /// layout 側が持つ Header supplementary の高さ (layout attributes) を返す。
    private func layoutHeaderHeight(_ cv: UICollectionView, section: Int) -> CGFloat? {
        let indexPath = IndexPath(item: 0, section: section)
        let attributes = cv.collectionViewLayout.layoutAttributesForSupplementaryView(
            ofKind: UICollectionView.elementKindSectionHeader,
            at: indexPath
        )
        return attributes?.frame.height
    }

    /// Section identity を保ったまま headerHeight だけを変える Store の `replaceSection` は、
    /// 表示中の header の実高さへ反映される (core/ADR-0018 の Store 経路)。
    /// 更新は Store → Publisher → Controller の経路を通す。
    func test_Store経由のreplaceSectionのheaderHeight変更が表示中headerの実高さに反映される() throws {
        let section1 = KsSettingsViewCore.Section(
            header: .text("ヘッダA"),
            cells: [LabelCell(title: "X")],
            headerHeight: 40
        )
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section1]))
        let (controller, cv, window) = hostStoreConnectedControllerInWindow(store: store)
        // controller は window.rootViewController として強参照されるが、テスト終了まで生存させる
        // ことを明示する (解放されると Store 購読が切れて更新が届かなくなる)。
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        let beforeHeight = try XCTUnwrap(visibleHeaderFrameHeight(cv, section: 0),
                                         "前提: 初期表示の header supplementary が取得できない")
        XCTAssertEqual(beforeHeight, 40, accuracy: 0.5,
                       "前提: 初期表示の header 実高さが固定値 40 になっていない")

        let section2 = KsSettingsViewCore.Section(
            id: section1.id,
            header: section1.header,
            cells: section1.cells,
            headerHeight: 90
        )
        store.replaceSection(sectionID: section1.id, new: section2)
        awaitCondition(
            "headerHeight の変更が表示中 header の実高さへ届く",
            in: cv,
            actual: { "header 高さ \(String(describing: visibleHeaderFrameHeight(cv, section: 0)))" },
            until: {
                guard let height = visibleHeaderFrameHeight(cv, section: 0) else { return false }
                return abs(height - 90) <= 0.5
            }
        )

        let afterLayoutHeight = try XCTUnwrap(layoutHeaderHeight(cv, section: 0),
                                              "更新後の header layout attributes が取得できない")
        XCTAssertEqual(afterLayoutHeight, 90, accuracy: 0.5,
                       "replaceSection の headerHeight 変更が layout attributes に反映されていない")
        let afterFrameHeight = try XCTUnwrap(visibleHeaderFrameHeight(cv, section: 0),
                                             "更新後の表示中 header supplementary が取得できない")
        XCTAssertEqual(afterFrameHeight, 90, accuracy: 0.5,
                       "replaceSection の headerHeight 変更が表示中 header の実高さに反映されていない")
    }

    /// Section identity・header accessory を保ったまま headerHeight だけを変える Store の
    /// `replaceAll` (`.full` 相当) も、表示中の header の実高さへ反映される (core/ADR-0018)。
    /// DSL の headerHeight preflight が発行する `.full` の適用先がこの経路になる。
    func test_Store経由のreplaceAllのheaderHeight変更が表示中headerの実高さに反映される() throws {
        let section1 = KsSettingsViewCore.Section(
            header: .text("ヘッダA"),
            cells: [LabelCell(title: "X")],
            headerHeight: 40
        )
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section1]))
        let (controller, cv, window) = hostStoreConnectedControllerInWindow(store: store)
        // controller は window.rootViewController として強参照されるが、テスト終了まで生存させる
        // ことを明示する (解放されると Store 購読が切れて更新が届かなくなる)。
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        let beforeHeight = try XCTUnwrap(visibleHeaderFrameHeight(cv, section: 0),
                                         "前提: 初期表示の header supplementary が取得できない")
        XCTAssertEqual(beforeHeight, 40, accuracy: 0.5,
                       "前提: 初期表示の header 実高さが固定値 40 になっていない")

        let section2 = KsSettingsViewCore.Section(
            id: section1.id,
            header: section1.header,
            cells: section1.cells,
            headerHeight: 90
        )
        store.replaceAll(SettingsRoot(sections: [section2]))
        awaitCondition(
            "headerHeight の変更が表示中 header の実高さへ届く",
            in: cv,
            actual: { "header 高さ \(String(describing: visibleHeaderFrameHeight(cv, section: 0)))" },
            until: {
                guard let height = visibleHeaderFrameHeight(cv, section: 0) else { return false }
                return abs(height - 90) <= 0.5
            }
        )

        let afterLayoutHeight = try XCTUnwrap(layoutHeaderHeight(cv, section: 0),
                                              "更新後の header layout attributes が取得できない")
        XCTAssertEqual(afterLayoutHeight, 90, accuracy: 0.5,
                       "replaceAll の headerHeight 変更が layout attributes に反映されていない")
        let afterFrameHeight = try XCTUnwrap(visibleHeaderFrameHeight(cv, section: 0),
                                             "更新後の表示中 header supplementary が取得できない")
        XCTAssertEqual(afterFrameHeight, 90, accuracy: 0.5,
                       "replaceAll の headerHeight 変更が表示中 header の実高さに反映されていない")
    }

    /// 同一 Section ID・同一 header accessory のまま `headerHeight` と同一 ID Cell の内容を
    /// 同時に変えた root を、`replaceAll` **1 回**で適用する。header の実高さと Cell の title の
    /// 両方が新しくなり、行の Native cell は破棄されない (内容再適用が 1 回で完結する)。
    func test_replaceAll1回でheader高さとCell内容の両方が表示へ反映される() throws {
        let cellID = UUID()
        let section1 = KsSettingsViewCore.Section(
            header: .text("ヘッダA"),
            cells: [LabelCell(id: cellID, title: "旧タイトル")],
            headerHeight: 40
        )
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section1]))
        let (controller, cv, window) = hostStoreConnectedControllerInWindow(store: store)
        // controller は window.rootViewController として強参照されるが、テスト終了まで生存させる
        // ことを明示する (解放されると Store 購読が切れて更新が届かなくなる)。
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        let beforeHeight = try XCTUnwrap(visibleHeaderFrameHeight(cv, section: 0),
                                         "前提: 初期表示の header supplementary が取得できない")
        XCTAssertEqual(beforeHeight, 40, accuracy: 0.5,
                       "前提: 初期表示の header 実高さが固定値 40 になっていない")
        XCTAssertEqual(visibleCellTitle(cv, section: 0, item: 0), "旧タイトル",
                       "前提: 初期表示の Cell title が取得できない")
        let rowBefore = cv.cellForItem(at: IndexPath(item: 0, section: 0))
        XCTAssertNotNil(rowBefore, "前提: 初期表示の行が取得できない")

        let section2 = KsSettingsViewCore.Section(
            id: section1.id,
            header: section1.header,
            cells: [LabelCell(id: cellID, title: "新タイトル")],
            headerHeight: 90
        )
        store.replaceAll(SettingsRoot(sections: [section2]))
        awaitCondition(
            "replaceAll 1 回の header 高さと Cell 内容が表示へ届く",
            in: cv,
            actual: {
                "header 高さ \(String(describing: visibleHeaderFrameHeight(cv, section: 0))) / "
                    + "title \(String(describing: visibleCellTitle(cv, section: 0, item: 0)))"
            },
            until: {
                guard let height = visibleHeaderFrameHeight(cv, section: 0) else { return false }
                return abs(height - 90) <= 0.5
                    && visibleCellTitle(cv, section: 0, item: 0) == "新タイトル"
            }
        )

        let afterLayoutHeight = try XCTUnwrap(layoutHeaderHeight(cv, section: 0),
                                              "更新後の header layout attributes が取得できない")
        XCTAssertEqual(afterLayoutHeight, 90, accuracy: 0.5,
                       "headerHeight の変更が layout attributes に反映されていない")
        let afterFrameHeight = try XCTUnwrap(visibleHeaderFrameHeight(cv, section: 0),
                                             "更新後の表示中 header supplementary が取得できない")
        XCTAssertEqual(afterFrameHeight, 90, accuracy: 0.5,
                       "headerHeight の変更が表示中 header の実高さに反映されていない")
        XCTAssertEqual(visibleCellTitle(cv, section: 0, item: 0), "新タイトル",
                       "同時に変わった Cell の内容が表示へ反映されていない")
        XCTAssertTrue(
            rowBefore === cv.cellForItem(at: IndexPath(item: 0, section: 0)),
            "行の Native cell が破棄されている (内容再適用が同一行の再構成で完結していない)"
        )
    }

    /// `replaceAll` の直後に同一 Cell の `replaceCell` を重ねて適用しても、
    /// 表示は最新のまま壊れない (full 適用直後の snapshot と部分更新の整合)。
    func test_replaceAll直後に同一Cellのタイトルをさらに変えても表示が追従する() throws {
        let cellID = UUID()
        let section1 = KsSettingsViewCore.Section(
            header: .text("ヘッダA"),
            cells: [LabelCell(id: cellID, title: "旧タイトル")],
            headerHeight: 40
        )
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section1]))
        let (controller, cv, window) = hostStoreConnectedControllerInWindow(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        XCTAssertEqual(visibleCellTitle(cv, section: 0, item: 0), "旧タイトル",
                       "前提: 初期表示の Cell title が取得できない")

        let section2 = KsSettingsViewCore.Section(
            id: section1.id,
            header: section1.header,
            cells: [LabelCell(id: cellID, title: "中間タイトル")],
            headerHeight: 90
        )
        store.replaceAll(SettingsRoot(sections: [section2]))
        store.replaceCell(cellID: KsCellID(id: cellID), new: LabelCell(id: cellID, title: "最終タイトル"))
        awaitCondition(
            "full 適用の直後に重ねた部分更新が表示へ届く",
            in: cv,
            actual: {
                "header 高さ \(String(describing: visibleHeaderFrameHeight(cv, section: 0))) / "
                    + "title \(String(describing: visibleCellTitle(cv, section: 0, item: 0)))"
            },
            until: {
                guard let height = visibleHeaderFrameHeight(cv, section: 0) else { return false }
                return abs(height - 90) <= 0.5
                    && visibleCellTitle(cv, section: 0, item: 0) == "最終タイトル"
            }
        )

        let afterFrameHeight = try XCTUnwrap(visibleHeaderFrameHeight(cv, section: 0),
                                             "更新後の表示中 header supplementary が取得できない")
        XCTAssertEqual(afterFrameHeight, 90, accuracy: 0.5,
                       "後続の部分更新で header の高さが巻き戻っている")
        XCTAssertEqual(visibleCellTitle(cv, section: 0, item: 0), "最終タイトル",
                       "full 適用の直後に重ねた部分更新が表示へ反映されていない")
    }

    /// view 形式 header は等価比較でき (ケース一致のみで等価と扱われ) ないため、
    /// replaceSection では中身の変化を仮定して表示中の supplementary を再構成する。
    func test_replaceSectionのviewヘッダ差し替えが表示中のsupplementaryに反映される() {
        let marker1 = UIView()
        marker1.tag = 1111
        let section1 = KsSettingsViewCore.Section(
            header: .view(KsAnyView.uiKit { marker1 }),
            cells: [LabelCell(title: "X")]
        )
        let (controller, cv, window) = hostControllerInWindow(root: SettingsRoot(sections: [section1]))
        defer { window.isHidden = true }

        let headerPath = IndexPath(item: 0, section: 0)
        let before = cv.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionHeader, at: headerPath
        ) as? UICollectionViewListCell
        XCTAssertTrue(before?.contentView.subviews.contains(where: { $0.tag == 1111 }) == true,
                      "前提: 初期表示の view header が描画されていない")

        let marker2 = UIView()
        marker2.tag = 2222
        let section2 = KsSettingsViewCore.Section(
            id: section1.id,
            header: .view(KsAnyView.uiKit { marker2 }),
            cells: section1.cells
        )
        controller.applyDiff(.replaceSection(sectionID: section1.id, new: section2))
        awaitCondition(
            "差し替えた view header が表示中の supplementary へ載る",
            in: cv,
            actual: {
                let cell = cv.supplementaryView(
                    forElementKind: UICollectionView.elementKindSectionHeader, at: headerPath
                ) as? UICollectionViewListCell
                return "subview tags = \(cell?.contentView.subviews.map(\.tag) ?? [])"
            },
            until: {
                let cell = cv.supplementaryView(
                    forElementKind: UICollectionView.elementKindSectionHeader, at: headerPath
                ) as? UICollectionViewListCell
                return cell?.contentView.subviews.contains(where: { $0.tag == 2222 }) == true
            }
        )

        let after = cv.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionHeader, at: headerPath
        ) as? UICollectionViewListCell
        XCTAssertTrue(after?.contentView.subviews.contains(where: { $0.tag == 2222 }) == true,
                      "replaceSection の view header 差し替えが表示中の supplementary に反映されていない")
    }

    // MARK: - view accessory への headerHeight 適用
    //
    // Section Header の固定高さ (`Section.headerHeight` / `Theme.headerHeight`) は accessory の
    // 種別 (text / view) に依らず同じ優先順位で解決される。text accessory 側の解決は
    // `KsSettingsViewControllerTests` が押さえているため、ここでは view accessory の実描画高さを
    // 観測して同じ契約が成立することを固定する。

    /// 内容高さを持つ view accessory を作るための UIView。
    /// `intrinsicContentSize` で高さの希望値を出すため、固定高さ側の制約と衝突しない
    /// (固定高さが与えられた場合は intrinsic 側が譲り、内容ははみ出して clip される)。
    private final class IntrinsicHeightView: UIView {
        private let contentHeight: CGFloat

        init(height: CGFloat) {
            self.contentHeight = height
            super.init(frame: .zero)
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) {
            fatalError("init(coder:) は使用しない")
        }

        override var intrinsicContentSize: CGSize {
            CGSize(width: UIView.noIntrinsicMetric, height: contentHeight)
        }
    }

    /// 指定した内容高さを持つ view accessory を返す。
    private func intrinsicHeightAccessory(height: CGFloat) -> SectionAccessory {
        .view(KsAnyView.uiKit { IntrinsicHeightView(height: height) })
    }

    /// 画面に表示されている実物の Footer supplementary の実高さ (frame) を返す。
    private func visibleFooterFrameHeight(_ cv: UICollectionView, section: Int) -> CGFloat? {
        let view = cv.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionFooter,
            at: IndexPath(item: 0, section: section)
        )
        return view?.frame.height
    }

    /// `Section.headerHeight` が正値なら、header accessory が view でも固定高さになる。
    /// 内容 (200pt) が固定高さ (40pt) より大きくても header 領域は広がらない。
    func test_viewヘッダはSectionのheaderHeight正値で固定高さになる() throws {
        let section = KsSettingsViewCore.Section(
            header: intrinsicHeightAccessory(height: 200),
            cells: [LabelCell(title: "X")],
            headerHeight: 40
        )
        let (controller, cv, window) = hostControllerInWindow(root: SettingsRoot(sections: [section]))
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        let layoutHeight = try XCTUnwrap(layoutHeaderHeight(cv, section: 0),
                                         "header の layout attributes が取得できない")
        XCTAssertEqual(layoutHeight, 40, accuracy: 0.5,
                       "view accessory の header に Section.headerHeight の固定高さが適用されていない")
        let frameHeight = try XCTUnwrap(visibleHeaderFrameHeight(cv, section: 0),
                                        "表示中の header supplementary が取得できない")
        XCTAssertEqual(frameHeight, 40, accuracy: 0.5,
                       "view accessory の内容高さが固定高さを押し広げている")
    }

    /// `Section.headerHeight` が `-1` (自動) なら、view accessory でも `Theme.headerHeight` を採用する。
    func test_viewヘッダはThemeのheaderHeightにフォールバックする() throws {
        let section = KsSettingsViewCore.Section(
            header: intrinsicHeightAccessory(height: 200),
            cells: [LabelCell(title: "X")]
        )
        let (controller, cv, window) = hostControllerInWindow(
            root: SettingsRoot(sections: [section]),
            theme: Theme(headerHeight: 60)
        )
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        let frameHeight = try XCTUnwrap(visibleHeaderFrameHeight(cv, section: 0),
                                        "表示中の header supplementary が取得できない")
        XCTAssertEqual(frameHeight, 60, accuracy: 0.5,
                       "view accessory の header に Theme.headerHeight が適用されていない")
    }

    /// `Section.headerHeight` と `Theme.headerHeight` が共に正値なら Section 側が勝つ。
    func test_viewヘッダのSection指定はThemeより優先される() throws {
        let section = KsSettingsViewCore.Section(
            header: intrinsicHeightAccessory(height: 200),
            cells: [LabelCell(title: "X")],
            headerHeight: 80
        )
        let (controller, cv, window) = hostControllerInWindow(
            root: SettingsRoot(sections: [section]),
            theme: Theme(headerHeight: 60)
        )
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        let frameHeight = try XCTUnwrap(visibleHeaderFrameHeight(cv, section: 0),
                                        "表示中の header supplementary が取得できない")
        XCTAssertEqual(frameHeight, 80, accuracy: 0.5,
                       "view accessory で Section.headerHeight が Theme.headerHeight に負けている")
    }

    /// Section・Theme の双方が高さ未指定なら、view accessory の header は内容に応じた自動高さになる。
    func test_viewヘッダは高さ未指定なら内容に応じた自動高さになる() throws {
        let section = KsSettingsViewCore.Section(
            header: intrinsicHeightAccessory(height: 70),
            cells: [LabelCell(title: "X")]
        )
        let (controller, cv, window) = hostControllerInWindow(root: SettingsRoot(sections: [section]))
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        let frameHeight = try XCTUnwrap(visibleHeaderFrameHeight(cv, section: 0),
                                        "表示中の header supplementary が取得できない")
        XCTAssertEqual(frameHeight, 70, accuracy: 0.5,
                       "高さ未指定の view accessory の header が内容高さ (70pt) になっていない")
    }

    /// 固定高さの Section と自動高さの Section が同一 list に並んでも、互いの高さ解決に影響しない。
    /// レイアウト再計算 (`invalidateLayout`) を挟んでも解決結果は入れ替わらない。
    func test_固定高さと自動高さのviewヘッダが同一list内で互いに影響しない() throws {
        let fixedSection = KsSettingsViewCore.Section(
            header: intrinsicHeightAccessory(height: 200),
            cells: [LabelCell(title: "X")],
            headerHeight: 40
        )
        let autoSection = KsSettingsViewCore.Section(
            header: intrinsicHeightAccessory(height: 70),
            cells: [LabelCell(title: "Y")]
        )
        let (controller, cv, window) = hostControllerInWindow(
            root: SettingsRoot(sections: [fixedSection, autoSection])
        )
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        XCTAssertEqual(try XCTUnwrap(visibleHeaderFrameHeight(cv, section: 0)), 40, accuracy: 0.5,
                       "固定高さ側の header が自動高さに引きずられている")
        XCTAssertEqual(try XCTUnwrap(visibleHeaderFrameHeight(cv, section: 1)), 70, accuracy: 0.5,
                       "自動高さ側の header が固定高さに引きずられている")

        cv.collectionViewLayout.invalidateLayout()
        layoutNow(cv)

        XCTAssertEqual(try XCTUnwrap(visibleHeaderFrameHeight(cv, section: 0)), 40, accuracy: 0.5,
                       "レイアウト再計算で固定高さ側の header 高さが変化している")
        XCTAssertEqual(try XCTUnwrap(visibleHeaderFrameHeight(cv, section: 1)), 70, accuracy: 0.5,
                       "レイアウト再計算で自動高さ側の header 高さが変化している")
    }

    /// `headerHeight` は Header 専用であり、footer の view accessory は自動高さのまま影響を受けない。
    func test_viewフッタはheaderHeightの影響を受けない() throws {
        let section = KsSettingsViewCore.Section(
            header: .text("ヘッダA"),
            footer: intrinsicHeightAccessory(height: 70),
            cells: [LabelCell(title: "X")],
            headerHeight: 40
        )
        let (controller, cv, window) = hostControllerInWindow(root: SettingsRoot(sections: [section]))
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        XCTAssertEqual(try XCTUnwrap(visibleHeaderFrameHeight(cv, section: 0)), 40, accuracy: 0.5,
                       "前提: header に固定高さが適用されていない")
        let footerHeight = try XCTUnwrap(visibleFooterFrameHeight(cv, section: 0),
                                         "表示中の footer supplementary が取得できない")
        XCTAssertEqual(footerHeight, 70, accuracy: 0.5,
                       "footer の view accessory が headerHeight の固定高さを受けている")
    }
}
#endif
