// KsSettingsViewControllerTests.swift
// KsSettingsViewUITests
//
// `KsSettingsViewController` の基本的な振る舞いを検証する。
//
// `KsSettingsViewController` は root setter を公開しないため、本テストは internal init
//（root を直接受け取る）または Store 経由で controller を構築する。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class KsSettingsViewControllerTests: XCTestCase {
    func test_internal_initで構築したcontrollerはSection数とセル数がsnapshotに反映される() {
        let section1 = Section(
            header: .text("一般"),
            cells: [LabelCell(title: "A"), LabelCell(title: "B")]
        )
        let section2 = Section(
            header: .text("高度"),
            cells: [LabelCell(title: "C")]
        )
        let controller = KsSettingsViewController(
            root: SettingsRoot(sections: [section1, section2])
        )
        _ = controller.view

        guard let dataSource = controller.internalDataSource else {
            XCTFail("DataSource が初期化されていない")
            return
        }
        let snapshot = dataSource.snapshot()
        XCTAssertEqual(snapshot.numberOfSections, 2)
        XCTAssertEqual(snapshot.numberOfItems(inSection: section1.id), 2)
        XCTAssertEqual(snapshot.numberOfItems(inSection: section2.id), 1)
    }

    func test_初期化直後は空SettingsRoot相当のスナップショットが構成される() {
        let controller = KsSettingsViewController(root: SettingsRoot())
        _ = controller.view

        guard let dataSource = controller.internalDataSource else {
            XCTFail("DataSource が初期化されていない")
            return
        }
        let snapshot = dataSource.snapshot()
        XCTAssertEqual(snapshot.numberOfSections, 0)
        XCTAssertEqual(snapshot.numberOfItems, 0)
    }

    func test_UICollectionViewはCompositionalLayoutで構成される() {
        let controller = KsSettingsViewController(root: SettingsRoot())
        _ = controller.view

        XCTAssertTrue(controller.internalCollectionView.collectionViewLayout is UICollectionViewCompositionalLayout)
    }

    func test_view_subviewsからUICollectionViewを取り出せる() {
        let controller = KsSettingsViewController(root: SettingsRoot())
        _ = controller.view

        let cv = controller.view.subviews.compactMap { $0 as? UICollectionView }.first
        XCTAssertNotNil(cv, "view.subviews から UICollectionView を取り出せない")
        XCTAssertTrue(cv?.collectionViewLayout is UICollectionViewCompositionalLayout)
    }

    func test_Store経由で初期化したcontrollerはStoreのrootでsnapshotが構成される() {
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [
            Section(header: .text("S"), cells: [LabelCell(title: "A")])
        ]))
        let controller = KsSettingsViewController(store: store)
        _ = controller.view

        guard let dataSource = controller.internalDataSource else {
            XCTFail("DataSource が初期化されていない")
            return
        }
        XCTAssertEqual(dataSource.snapshot().numberOfSections, 1)
        XCTAssertEqual(dataSource.snapshot().numberOfItems, 1)
    }

    // MARK: - 表示判定の AND 合成

    func test_shouldShowHeader_はトグルと内容ありのANDになる() {
        // 内容あり × トグル true → 表示
        XCTAssertTrue(KsSettingsViewController.shouldShowHeader(
            for: Section(header: .text("H"), cells: [])
        ))
        // 内容あり × トグル false → 非表示
        XCTAssertFalse(KsSettingsViewController.shouldShowHeader(
            for: Section(header: .text("H"), cells: [], isHeaderVisible: false)
        ))
        // 内容なし (nil) × トグル true → 非表示
        XCTAssertFalse(KsSettingsViewController.shouldShowHeader(
            for: Section(header: nil, cells: [])
        ))
        // 内容なし (空 text) × トグル true → 非表示
        XCTAssertFalse(KsSettingsViewController.shouldShowHeader(
            for: Section(header: .text(""), cells: [])
        ))
        // 内容なし × トグル false → 非表示
        XCTAssertFalse(KsSettingsViewController.shouldShowHeader(
            for: Section(header: nil, cells: [], isHeaderVisible: false)
        ))
    }

    func test_shouldShowFooter_はトグルと内容ありのANDになる() {
        XCTAssertTrue(KsSettingsViewController.shouldShowFooter(
            for: Section(footer: .text("F"), cells: [])
        ))
        XCTAssertFalse(KsSettingsViewController.shouldShowFooter(
            for: Section(footer: .text("F"), cells: [], isFooterVisible: false)
        ))
        XCTAssertFalse(KsSettingsViewController.shouldShowFooter(
            for: Section(footer: nil, cells: [])
        ))
        XCTAssertFalse(KsSettingsViewController.shouldShowFooter(
            for: Section(footer: .text(""), cells: [])
        ))
    }

    /// view accessory は中身が空でも常に「内容あり」として扱う。
    func test_view_accessoryは常に内容ありとして扱われる() {
        let section = Section(
            header: .view(KsAnyView.uiKit { UIView() }),
            footer: .view(KsAnyView.uiKit { UIView() }),
            cells: []
        )
        XCTAssertTrue(KsSettingsViewController.shouldShowHeader(for: section))
        XCTAssertTrue(KsSettingsViewController.shouldShowFooter(for: section))
    }

    /// Header トグルと Footer トグルは互いに独立している。
    func test_HeaderトグルはFooterの表示判定に影響しない() {
        let section = Section(
            header: .text("一般"),
            footer: .text("補足"),
            cells: [LabelCell(title: "A")],
            isHeaderVisible: false
        )
        XCTAssertFalse(KsSettingsViewController.shouldShowHeader(for: section))
        XCTAssertTrue(KsSettingsViewController.shouldShowFooter(for: section),
                      "Header を隠しても Footer の表示判定は変わらない")
    }

    // MARK: - 罫線インセット規則
    //
    // `AiForms.Maui.SettingsView` と同じく、アイコンの有無に関わらず罫線インセットは
    // 標準左マージン（16pt）で揃うため、`titleLeadingPosition` は常に 16 を返す。

    /// アイコン無し Cell の titleLeadingPosition は 16pt（標準左マージン）。
    func test_titleLeadingPosition_アイコン無しは16pt() {
        let controller = KsSettingsViewController(root: SettingsRoot())
        _ = controller.view
        let cell = LabelCell(title: "noIcon")
        XCTAssertEqual(controller.titleLeadingPosition(for: cell), 16)
    }

    /// アイコン有り LabelCell の titleLeadingPosition も 16pt（AiForms オリジナルに揃え）。
    func test_titleLeadingPosition_アイコン有りLabelCellも16pt() {
        let controller = KsSettingsViewController(root: SettingsRoot())
        _ = controller.view
        let cell = LabelCell(title: "withIcon", icon: KsImage.systemName("bell"))
        XCTAssertEqual(controller.titleLeadingPosition(for: cell), 16)
    }

    /// アイコン有り CommandCell の titleLeadingPosition も 16pt（AiForms オリジナルに揃え）。
    func test_titleLeadingPosition_アイコン有りCommandCellも16pt() {
        let controller = KsSettingsViewController(root: SettingsRoot())
        _ = controller.view
        let cell = CommandCell(title: "withIcon", icon: KsImage.systemName("bell"))
        XCTAssertEqual(controller.titleLeadingPosition(for: cell), 16)
    }

    /// セクション最初の Cell は topSeparatorInsets が leading=0（端から端）になる。アイコン有無に関わらず。
    func test_separatorConfiguration_セクション最初はtopInsetが0() {
        let controller = KsSettingsViewController(root: SettingsRoot(sections: [
            Section(cells: [
                LabelCell(title: "first", icon: KsImage.systemName("bell")),
                LabelCell(title: "second"),
            ])
        ]))
        _ = controller.view
        let base = UIListSeparatorConfiguration(listAppearance: .plain)
        let config = controller.separatorConfiguration(for: IndexPath(item: 0, section: 0), base: base)
        XCTAssertEqual(config.topSeparatorInsets.leading, 0, "セクション最初は top inset leading=0")
        XCTAssertEqual(config.topSeparatorVisibility, .visible)
    }

    /// セクション最後の Cell は bottomSeparatorInsets が leading=0（端から端）になる。アイコン有無に関わらず。
    func test_separatorConfiguration_セクション最後はbottomInsetが0() {
        let controller = KsSettingsViewController(root: SettingsRoot(sections: [
            Section(cells: [
                LabelCell(title: "first"),
                LabelCell(title: "last", icon: KsImage.systemName("bell")),
            ])
        ]))
        _ = controller.view
        let base = UIListSeparatorConfiguration(listAppearance: .plain)
        let config = controller.separatorConfiguration(for: IndexPath(item: 1, section: 0), base: base)
        XCTAssertEqual(config.bottomSeparatorInsets.leading, 0, "セクション最後は bottom inset leading=0")
        XCTAssertEqual(config.bottomSeparatorVisibility, .visible)
    }

    /// セクション内中間 Cell の bottomSeparatorInsets は固定 16pt（AiForms オリジナルに揃え）。
    func test_separatorConfiguration_中間Cellの下罫線は固定16pt() {
        let controller = KsSettingsViewController(root: SettingsRoot(sections: [
            Section(cells: [
                LabelCell(title: "first"),
                LabelCell(title: "middle", icon: KsImage.systemName("bell")),
                LabelCell(title: "last"),
            ])
        ]))
        _ = controller.view
        let base = UIListSeparatorConfiguration(listAppearance: .plain)
        let config = controller.separatorConfiguration(for: IndexPath(item: 1, section: 0), base: base)
        // アイコン有無に関わらず固定 16pt
        XCTAssertEqual(config.bottomSeparatorInsets.leading, 16)
    }

    /// アイコン有り / 無し混在セクションで、全ての中間 Cell の bottom separator inset が一律 16pt であること。
    func test_separatorConfiguration_アイコン混在セクションは全Cellで固定16pt() {
        let controller = KsSettingsViewController(root: SettingsRoot(sections: [
            Section(cells: [
                LabelCell(title: "withIcon1", icon: KsImage.systemName("bell")),
                LabelCell(title: "noIcon1"),
                LabelCell(title: "withIcon2", icon: KsImage.systemName("star")),
                LabelCell(title: "noIcon2"),
                LabelCell(title: "lastWithIcon", icon: KsImage.systemName("gear")),
            ])
        ]))
        _ = controller.view
        let base = UIListSeparatorConfiguration(listAppearance: .plain)

        // 中間 Cell（item 0..3）の bottom separator はすべて 16pt 固定
        for item in 0..<4 {
            let config = controller.separatorConfiguration(
                for: IndexPath(item: item, section: 0),
                base: base
            )
            XCTAssertEqual(
                config.bottomSeparatorInsets.leading,
                16,
                "item=\(item) の中間 Cell bottom separator は固定 16pt"
            )
        }
        // 最後の Cell（item 4）は bottom が端から端
        let lastConfig = controller.separatorConfiguration(
            for: IndexPath(item: 4, section: 0),
            base: base
        )
        XCTAssertEqual(lastConfig.bottomSeparatorInsets.leading, 0, "最後の Cell は bottom leading=0")
    }

    /// 単一 Cell のセクション: isFirst かつ isLast。top も bottom も leading=0。
    func test_separatorConfiguration_単一Cellセクションは上下罫線とも端から端() {
        let controller = KsSettingsViewController(root: SettingsRoot(sections: [
            Section(cells: [LabelCell(title: "only", icon: KsImage.systemName("bell"))])
        ]))
        _ = controller.view
        let base = UIListSeparatorConfiguration(listAppearance: .plain)
        let config = controller.separatorConfiguration(for: IndexPath(item: 0, section: 0), base: base)
        XCTAssertEqual(config.topSeparatorInsets.leading, 0)
        XCTAssertEqual(config.bottomSeparatorInsets.leading, 0)
        XCTAssertEqual(config.topSeparatorVisibility, .visible)
        XCTAssertEqual(config.bottomSeparatorVisibility, .visible)
    }

    // MARK: - separator 色の Theme 追従

    /// セパレータ色付き Theme で初期化したとき、separator 構成の `color` が Theme の色になる。
    func test_separatorConfiguration_初期Themeのセパレータ色がcolorに反映される() {
        let themeColor = UIColor(red: 0.9, green: 0.855, blue: 0.725, alpha: 1.0)
        let controller = KsSettingsViewController(
            root: SettingsRoot(sections: [
                Section(cells: [LabelCell(title: "first"), LabelCell(title: "last")])
            ]),
            theme: Theme(separatorColor: themeColor)
        )
        _ = controller.view
        let base = UIListSeparatorConfiguration(listAppearance: .plain)

        for item in 0..<2 {
            let config = controller.separatorConfiguration(
                for: IndexPath(item: item, section: 0),
                base: base
            )
            XCTAssertEqual(
                config.color,
                themeColor,
                "item=\(item) の separator 色は Theme の separatorColor になる"
            )
        }
    }

    /// 実行時に `applyTheme(_:)` で Theme を差し替えると、separator 構成の `color` が新 Theme に追従する。
    func test_separatorConfiguration_applyTheme後は新Themeのセパレータ色に追従する() {
        let initialColor = UIColor(red: 0.9, green: 0.855, blue: 0.725, alpha: 1.0)
        let updatedColor = UIColor(red: 0.1, green: 0.2, blue: 0.3, alpha: 1.0)
        let controller = KsSettingsViewController(
            root: SettingsRoot(sections: [
                Section(cells: [LabelCell(title: "only")])
            ]),
            theme: Theme(separatorColor: initialColor)
        )
        _ = controller.view
        let base = UIListSeparatorConfiguration(listAppearance: .plain)
        let indexPath = IndexPath(item: 0, section: 0)

        XCTAssertEqual(
            controller.separatorConfiguration(for: indexPath, base: base).color,
            initialColor
        )

        controller.applyTheme(Theme(separatorColor: updatedColor))

        XCTAssertEqual(
            controller.separatorConfiguration(for: indexPath, base: base).color,
            updatedColor,
            "applyTheme 後の separator 色は新 Theme の separatorColor になる"
        )
    }

    /// Store の `applyTheme(_:)` 経由で Theme を差し替えた場合も separator 構成の `color` が追従する。
    func test_separatorConfiguration_Store経由のTheme変更でセパレータ色が追従する() {
        let initialColor = UIColor(red: 0.9, green: 0.855, blue: 0.725, alpha: 1.0)
        let updatedColor = UIColor(red: 0.0, green: 0.5, blue: 0.25, alpha: 1.0)
        let store = SettingsRootStore(
            initialRoot: SettingsRoot(sections: [
                Section(cells: [LabelCell(title: "only")])
            ]),
            initialTheme: Theme(separatorColor: initialColor)
        )
        let controller = KsSettingsViewController(store: store)
        _ = controller.view
        let base = UIListSeparatorConfiguration(listAppearance: .plain)
        let indexPath = IndexPath(item: 0, section: 0)

        XCTAssertEqual(
            controller.separatorConfiguration(for: indexPath, base: base).color,
            initialColor
        )

        store.applyTheme(Theme(separatorColor: updatedColor))

        XCTAssertEqual(
            controller.separatorConfiguration(for: indexPath, base: base).color,
            updatedColor,
            "Store 経由の Theme 変更後も separator 色は新 Theme の separatorColor になる"
        )
    }

    // MARK: - Section.headerHeight の `.absolute(headerHeight)` 反映

    /// テスト用のダミー original boundary supplementary item を生成する。
    /// sectionProvider 経路で `.list(using:)` から取得した既定 item を模擬する。
    private func makeOriginalHeaderItem() -> NSCollectionLayoutBoundarySupplementaryItem {
        return NSCollectionLayoutBoundarySupplementaryItem(
            layoutSize: NSCollectionLayoutSize(
                widthDimension: .fractionalWidth(1.0),
                heightDimension: .estimated(44) // `.list(using:)` 既定相当
            ),
            elementKind: UICollectionView.elementKindSectionHeader,
            alignment: .top
        )
    }

    /// `Section.headerHeight = 80` 指定時、`makeHeaderBoundaryItem` は
    /// `layoutSize.heightDimension == .absolute(80)` の boundary supplementary item を返す。
    func test_makeHeaderBoundaryItem_headerHeight80のとき_absolute80になる() {
        let section = Section(
            header: .text("CommandCell"),
            cells: [LabelCell(title: "A")],
            headerHeight: 80
        )
        let original = makeOriginalHeaderItem()
        let item = KsSettingsViewController.makeHeaderBoundaryItem(for: section, original: original)
        XCTAssertNotNil(item, "headerHeight 正値のとき item は生成される")
        guard let item = item else { return }
        // `.absolute(80)` の確認: `NSCollectionLayoutDimension` は内部で
        // `isAbsolute` / `dimension` を持ち、debugDescription に反映される。
        // 公開 API では `widthDimension` / `heightDimension` の値同等性を直接比較できないため、
        // 反映後の Dimension の `isAbsolute` プロパティと `dimension` (CGFloat) を検証する。
        XCTAssertTrue(item.layoutSize.heightDimension.isAbsolute,
                      "headerHeight 正値のとき heightDimension は .absolute でなければならない（.estimated だと指定値が無視される）")
        XCTAssertEqual(item.layoutSize.heightDimension.dimension, 80,
                       "headerHeight = 80 のとき heightDimension の値は 80pt")
    }

    /// `Section.headerHeight = -1`（自動）+ `header` 非空のとき、
    /// `makeHeaderBoundaryItem` は `.estimated(20)` を返す（自動高さの既定密度）。
    func test_makeHeaderBoundaryItem_headerHeight未指定_header非空のとき_estimated20になる() {
        let section = Section(
            header: .text("LabelCell"),
            cells: [LabelCell(title: "A")]
            // headerHeight = -1（既定、自動高さ）
        )
        let original = makeOriginalHeaderItem()
        let item = KsSettingsViewController.makeHeaderBoundaryItem(for: section, original: original)
        XCTAssertNotNil(item, "header 非空 + headerHeight 未指定のとき item は生成される")
        guard let item = item else { return }
        XCTAssertTrue(item.layoutSize.heightDimension.isEstimated,
                      "headerHeight == -1 のとき heightDimension は .estimated")
        XCTAssertEqual(item.layoutSize.heightDimension.dimension, 20,
                       "自動高さの既定は `.estimated(20)`（システム既定の `.estimated(44)` より小さく取る）")
    }

    /// `Section.headerHeight = -1`（自動）+ `header == nil` のとき、
    /// `makeHeaderBoundaryItem` は `nil` を返す（supplementary 自体を生成しない）。
    func test_makeHeaderBoundaryItem_headerHeight未指定_header_nilのとき_nilを返す() {
        let section = Section(
            header: nil,
            cells: [LabelCell(title: "A")]
            // headerHeight = -1（既定）
        )
        let original = makeOriginalHeaderItem()
        let item = KsSettingsViewController.makeHeaderBoundaryItem(for: section, original: original)
        XCTAssertNil(item, "header 未指定かつ headerHeight 未指定のとき supplementary item は生成しない")
    }

    /// 高さの解決は存在判定の後に適用される。`header == nil` なら `Section.headerHeight` が
    /// 正値でも supplementary item は生成しない。
    func test_makeHeaderBoundaryItem_headerHeight40でも_header_nilなら_nilを返す() {
        let section = Section(
            header: nil,
            cells: [LabelCell(title: "A")],
            headerHeight: 40
        )
        let original = makeOriginalHeaderItem()
        let item = KsSettingsViewController.makeHeaderBoundaryItem(for: section, original: original)
        XCTAssertNil(item, "headerHeight は高さを決めるだけで Header の存在を作らない")
    }

    /// 空 text の header も内容不在として扱うため、`headerHeight` 正値でも生成しない。
    func test_makeHeaderBoundaryItem_headerHeight40でも_header空textなら_nilを返す() {
        let section = Section(
            header: .text(""),
            cells: [LabelCell(title: "A")],
            headerHeight: 40
        )
        let original = makeOriginalHeaderItem()
        let item = KsSettingsViewController.makeHeaderBoundaryItem(for: section, original: original)
        XCTAssertNil(item, "空 text の header は内容不在であり高さ指定でも領域を作らない")
    }

    /// `header == nil` なら `Theme.headerHeight` が正値でも supplementary item は生成しない。
    func test_makeHeaderBoundaryItem_themeHeaderHeight指定でも_header_nilなら_nilを返す() {
        let section = Section(
            header: nil,
            cells: [LabelCell(title: "A")]
        )
        let theme = Theme(headerHeight: 60)
        let original = makeOriginalHeaderItem()
        let item = KsSettingsViewController.makeHeaderBoundaryItem(
            for: section, original: original, theme: theme
        )
        XCTAssertNil(item, "Theme.headerHeight も Header の存在を作らない")
    }

    /// トグル `false` なら内容と高さ指定があっても supplementary item は生成しない。
    func test_makeHeaderBoundaryItem_トグルfalseなら高さ指定があっても_nilを返す() {
        let section = Section(
            header: .text("一般"),
            cells: [LabelCell(title: "A")],
            headerHeight: 40,
            isHeaderVisible: false
        )
        let theme = Theme(headerHeight: 60)
        let original = makeOriginalHeaderItem()
        let item = KsSettingsViewController.makeHeaderBoundaryItem(
            for: section, original: original, theme: theme
        )
        XCTAssertNil(item, "トグル false の Header には高さを解決しない")
    }

    /// `Section.headerHeight = -1`（自動）+ `Theme.headerHeight > 0` のとき、
    /// `makeHeaderBoundaryItem` は `Theme.headerHeight` を `.absolute(...)` として返す。
    /// `Section.headerHeight` が `-1.0`（自動）のときは `Theme.headerHeight` を採用する。
    func test_makeHeaderBoundaryItem_section未指定_themeHeaderHeight指定で_absoluteになる() {
        let section = Section(
            header: .text("LabelCell"),
            cells: [LabelCell(title: "A")]
            // headerHeight = -1（既定、自動高さ）
        )
        let theme = Theme(headerHeight: 60)
        let original = makeOriginalHeaderItem()
        let item = KsSettingsViewController.makeHeaderBoundaryItem(
            for: section, original: original, theme: theme
        )
        XCTAssertNotNil(item, "Theme.headerHeight 正値のとき item は生成される")
        guard let item = item else { return }
        XCTAssertTrue(item.layoutSize.heightDimension.isAbsolute,
                      "Theme.headerHeight 正値のときも heightDimension は .absolute")
        XCTAssertEqual(item.layoutSize.heightDimension.dimension, 60,
                       "Theme.headerHeight = 60 のとき heightDimension の値は 60pt")
    }

    /// `Section.headerHeight = 80`（明示） + `Theme.headerHeight = 60`（fallback 候補）のとき、
    /// Section 側が優先され `.absolute(80)` になる（spec: Section ごとの値が `-1` のときのみ Theme fallback）。
    func test_makeHeaderBoundaryItem_section明示はthemeより優先される() {
        let section = Section(
            header: .text("LabelCell"),
            cells: [LabelCell(title: "A")],
            headerHeight: 80
        )
        let theme = Theme(headerHeight: 60)
        let original = makeOriginalHeaderItem()
        let item = KsSettingsViewController.makeHeaderBoundaryItem(
            for: section, original: original, theme: theme
        )
        XCTAssertNotNil(item)
        guard let item = item else { return }
        XCTAssertTrue(item.layoutSize.heightDimension.isAbsolute)
        XCTAssertEqual(item.layoutSize.heightDimension.dimension, 80,
                       "Section.headerHeight 明示は Theme.headerHeight より優先される")
    }

    /// `Section.headerHeight = -1` + `Theme.headerHeight = -1` のとき、従来通り `.estimated(20)` を返す。
    func test_makeHeaderBoundaryItem_section未指定_theme未指定なら従来estimated20() {
        let section = Section(
            header: .text("LabelCell"),
            cells: [LabelCell(title: "A")]
        )
        let theme = Theme() // headerHeight = -1
        let original = makeOriginalHeaderItem()
        let item = KsSettingsViewController.makeHeaderBoundaryItem(
            for: section, original: original, theme: theme
        )
        XCTAssertNotNil(item)
        guard let item = item else { return }
        XCTAssertTrue(item.layoutSize.heightDimension.isEstimated)
        XCTAssertEqual(item.layoutSize.heightDimension.dimension, 20)
    }

    // MARK: - 視覚的セル高さ（CellStyle.cellHeight）検証
    //
    // iOS の高さ適用は `KsCellViewSupport.applyEffectiveHeight` が
    // `contentView.heightAnchor.constraint(... priority = .required - 1)` を張る形で行うが、
    // これは `UICollectionViewListCell` の self-sizing 経路（UIListContentConfiguration）と
    // 干渉しうるため、制約を張っただけでは frame.height への反映を保証できない。
    //
    // 本セクションは指定 cellHeight が描画 frame に反映されることを実測で保証する。
    //
    // 参照: `AiForms.Maui.SettingsView` の iOS 実装は `GetHeightForRow` が `cell.Height` を
    //   直接返し、UITableView の rect 計算に反映させている。

    /// 指定 SettingsRoot で controller を構築し、指定 indexPath のセル frame.height を返す。
    @MainActor
    private func measuredCellHeight(
        for root: SettingsRoot,
        theme: Theme = Theme(),
        indexPath: IndexPath,
        containerSize: CGSize = CGSize(width: 375, height: 1000)
    ) -> CGFloat? {
        let controller = KsSettingsViewController(root: root, theme: theme)
        let view = controller.view!
        view.frame = CGRect(origin: .zero, size: containerSize)
        view.layoutIfNeeded()

        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: containerSize)
        cv.layoutIfNeeded()
        cv.setNeedsLayout()
        cv.layoutIfNeeded()

        let cell = cv.cellForItem(at: indexPath)
        return cell?.frame.height
    }

    /// `CellStyle(cellHeight: 80)` を指定した CommandCell の描画 frame.height が 80pt 以上に
    /// なることを検証する。
    ///
    /// `Theme.hasUnevenRows = true` の場合は `>= 80pt`、`false` の場合は `== 80pt`（固定）。
    func test_視覚的セル高さ_cellHeight80指定時_セルのframe高さが80になる() {
        let theme = Theme(hasUnevenRows: true)
        let section = Section(
            header: .text("CommandCell"),
            cells: [
                CommandCell(
                    style: CellStyle(cellHeight: 80),
                    title: "Tanaka Taro",
                    description: "tanaka.taro@example.com"
                ),
                CommandCell(title: "プロフィール")
            ]
        )
        let root = SettingsRoot(sections: [section])

        guard let measured = measuredCellHeight(for: root, theme: theme, indexPath: IndexPath(item: 0, section: 0)) else {
            XCTFail("セル frame.height が取得できない（layoutIfNeeded 後に描画されているはず）")
            return
        }
        XCTAssertGreaterThanOrEqual(
            measured, 80 - 0.5,
            "CellStyle.cellHeight = 80 指定時、セルの描画 frame.height は 80pt 以上でなければならない（実測 \(measured)pt）"
        )
    }

    /// `cellHeight = 120` 指定でも frame.height が指定値どおりになることを検証する（回帰テスト）。
    func test_視覚的セル高さ_cellHeight120指定時_セルのframe高さが120になる() {
        let theme = Theme(hasUnevenRows: true)
        let section = Section(
            header: .text("Tall"),
            cells: [
                CommandCell(
                    style: CellStyle(cellHeight: 120),
                    title: "Tall Row"
                )
            ]
        )
        let root = SettingsRoot(sections: [section])
        guard let measured = measuredCellHeight(for: root, theme: theme, indexPath: IndexPath(item: 0, section: 0)) else {
            XCTFail("セル frame.height が取得できない")
            return
        }
        XCTAssertGreaterThanOrEqual(
            measured, 120 - 0.5,
            "cellHeight = 120 指定時、セルの描画 frame.height は 120pt 以上でなければならない（実測 \(measured)pt）"
        )
    }
}
#endif
