// AccessoryBackgroundColorTests.swift
// KsSettingsViewUITests
//
// `Theme.headerBackgroundColor` / `Theme.footerBackgroundColor` が Header / Footer の描画へ
// 届くことを検証する。
//
// 塗る対象は text 形式の accessory に限る。View 形式の accessory は利用者の View が見た目を
// 決める領域であり、ライブラリからは背景を塗らない（Android の accessory ViewHolder と同じ範囲）。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class AccessoryBackgroundColorTests: XCTestCase {

    private var window: UIWindow?

    override func tearDown() {
        window?.isHidden = true
        window = nil
        super.tearDown()
    }

    /// Section / Root accessory を持つ controller を window に載せ、supplementary を実体化する。
    private func hostController(
        sections: [KsSettingsViewCore.Section],
        rootHeader: RootAccessory? = nil,
        rootFooter: RootAccessory? = nil,
        theme: Theme
    ) -> (KsSettingsViewController, UICollectionView) {
        let controller = KsSettingsViewController(
            root: SettingsRoot(sections: sections),
            theme: theme
        )
        let size = CGSize(width: 375, height: 700)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        self.window = window

        let rootView = controller.view!
        rootView.frame = CGRect(origin: .zero, size: size)
        rootView.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)

        controller.rootHeader = rootHeader
        controller.rootFooter = rootFooter
        awaitCondition(
            "Section / Root の accessory supplementary が生成される",
            in: cv,
            actual: {
                "sections = \(cv.numberOfSections)"
                    + " / rootHeader = \(self.rootCell(cv, .rootHeader) != nil)"
                    + " / rootFooter = \(self.rootCell(cv, .rootFooter) != nil)"
            },
            until: {
                guard cv.numberOfSections == sections.count, let first = sections.first else {
                    return false
                }
                if first.header != nil, self.sectionCell(cv, .header, 0) == nil { return false }
                if first.footer != nil, self.sectionCell(cv, .footer, 0) == nil { return false }
                if rootHeader != nil, self.rootCell(cv, .rootHeader) == nil { return false }
                if rootFooter != nil, self.rootCell(cv, .rootFooter) == nil { return false }
                return true
            }
        )
        return (controller, cv)
    }

    /// accessory の位置。
    @MainActor
    private enum Slot {
        case header
        case footer
        case rootHeader
        case rootFooter

        var elementKind: String {
            switch self {
            case .header: return UICollectionView.elementKindSectionHeader
            case .footer: return UICollectionView.elementKindSectionFooter
            case .rootHeader: return KsSettingsViewController.rootHeaderElementKind
            case .rootFooter: return KsSettingsViewController.rootFooterElementKind
            }
        }
    }

    /// 指定 section の accessory supplementary（`UICollectionViewListCell`）を取り出す。
    private func sectionCell(
        _ cv: UICollectionView,
        _ slot: Slot,
        _ section: Int
    ) -> UICollectionViewListCell? {
        return cv.supplementaryView(
            forElementKind: slot.elementKind,
            at: IndexPath(item: 0, section: section)
        ) as? UICollectionViewListCell
    }

    /// Root accessory の boundary supplementary（`UICollectionViewListCell`）を取り出す。
    ///
    /// Root H/F は layout 全体の boundary supplementary で indexPath を持たないため、
    /// 表示中の supplementary を kind で列挙して取得する。
    private func rootCell(_ cv: UICollectionView, _ slot: Slot) -> UICollectionViewListCell? {
        return cv.visibleSupplementaryViews(ofKind: slot.elementKind)
            .compactMap { $0 as? UICollectionViewListCell }
            .first
    }

    /// 指定 accessory の背景色が期待値になるまで待つ。
    private func awaitBackgroundColor(
        _ cv: UICollectionView,
        _ slot: Slot,
        section: Int = 0,
        equals expected: UIColor,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        awaitCondition(
            "accessory の背景色 (期待値: \(expected))",
            in: cv,
            actual: { "\(String(describing: self.backgroundColor(cv, slot, section: section)))" },
            file: file,
            line: line,
            until: { self.backgroundColor(cv, slot, section: section) == expected }
        )
    }

    /// 指定 accessory に設定されている背景色。
    private func backgroundColor(
        _ cv: UICollectionView,
        _ slot: Slot,
        section: Int = 0
    ) -> UIColor? {
        let cell: UICollectionViewListCell?
        switch slot {
        case .header, .footer:
            cell = sectionCell(cv, slot, section)
        case .rootHeader, .rootFooter:
            cell = rootCell(cv, slot)
        }
        return cell?.backgroundConfiguration?.backgroundColor
    }

    private static let headerBefore = UIColor(red: 0.9, green: 0.8, blue: 0.1, alpha: 1.0)
    private static let footerBefore = UIColor(red: 0.1, green: 0.8, blue: 0.9, alpha: 1.0)
    private static let headerAfter = UIColor(red: 0.2, green: 0.1, blue: 0.7, alpha: 1.0)
    private static let footerAfter = UIColor(red: 0.7, green: 0.1, blue: 0.2, alpha: 1.0)

    private func textSections() -> [KsSettingsViewCore.Section] {
        return [
            KsSettingsViewCore.Section(
                header: .text("一般"),
                footer: .text("補足説明"),
                cells: [LabelCell(title: "A")]
            )
        ]
    }

    // MARK: - Section accessory

    func test_SectionHeaderとFooterにそれぞれの背景色が反映される() throws {
        let (_, cv) = hostController(
            sections: textSections(),
            theme: Theme(
                headerBackgroundColor: Self.headerBefore,
                footerBackgroundColor: Self.footerBefore
            )
        )

        XCTAssertEqual(
            backgroundColor(cv, .header), Self.headerBefore,
            "Section Header の背景は headerBackgroundColor になる"
        )
        XCTAssertEqual(
            backgroundColor(cv, .footer), Self.footerBefore,
            "Section Footer の背景は footerBackgroundColor になる"
        )
    }

    func test_applyThemeでSectionHeaderとFooterの背景色が追従する() throws {
        let (controller, cv) = hostController(
            sections: textSections(),
            theme: Theme(
                headerBackgroundColor: Self.headerBefore,
                footerBackgroundColor: Self.footerBefore
            )
        )

        let beforeIDs = controller.internalDataSource?.snapshot().itemIdentifiers
        let beforeSections = controller.internalDataSource?.snapshot().sectionIdentifiers

        controller.applyTheme(
            Theme(
                headerBackgroundColor: Self.headerAfter,
                footerBackgroundColor: Self.footerAfter
            )
        )
        awaitBackgroundColor(cv, .header, equals: Self.headerAfter)
        awaitBackgroundColor(cv, .footer, equals: Self.footerAfter)

        XCTAssertEqual(
            controller.internalDataSource?.snapshot().sectionIdentifiers, beforeSections,
            "背景色の差し替えで Section の identity が変わっている"
        )
        XCTAssertEqual(
            controller.internalDataSource?.snapshot().itemIdentifiers, beforeIDs,
            "背景色の差し替えで Cell の identity が変わっている"
        )
    }

    // MARK: - Root accessory

    func test_RootHeaderとFooterにそれぞれの背景色が反映される() throws {
        let (_, cv) = hostController(
            sections: [KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])],
            rootHeader: .text("タイトル"),
            rootFooter: .text("脚注"),
            theme: Theme(
                headerBackgroundColor: Self.headerBefore,
                footerBackgroundColor: Self.footerBefore
            )
        )

        XCTAssertEqual(
            backgroundColor(cv, .rootHeader), Self.headerBefore,
            "Root Header の背景は headerBackgroundColor になる"
        )
        XCTAssertEqual(
            backgroundColor(cv, .rootFooter), Self.footerBefore,
            "Root Footer の背景は footerBackgroundColor になる"
        )
    }

    func test_applyThemeでRootHeaderとFooterの背景色が追従する() throws {
        let (controller, cv) = hostController(
            sections: [KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])],
            rootHeader: .text("タイトル"),
            rootFooter: .text("脚注"),
            theme: Theme(
                headerBackgroundColor: Self.headerBefore,
                footerBackgroundColor: Self.footerBefore
            )
        )

        controller.applyTheme(
            Theme(
                headerBackgroundColor: Self.headerAfter,
                footerBackgroundColor: Self.footerAfter
            )
        )
        awaitBackgroundColor(cv, .rootHeader, equals: Self.headerAfter)
        awaitBackgroundColor(cv, .rootFooter, equals: Self.footerAfter)
    }

    // MARK: - View 形式は塗らない

    func test_View形式のSectionHeaderには背景色を塗らない() throws {
        let (_, cv) = hostController(
            sections: [
                KsSettingsViewCore.Section(
                    header: .view(KsAnyView.uiKit { UIView() }),
                    cells: [LabelCell(title: "A")]
                )
            ],
            theme: Theme(headerBackgroundColor: Self.headerBefore)
        )

        XCTAssertNil(
            backgroundColor(cv, .header),
            "View 形式の accessory はライブラリが背景を塗らない（利用者の View が見た目を決める）"
        )
    }

    // MARK: - 既定は透明

    /// 何も指定しない Header / Footer の領域は、どちらの外観でも list 下地がそのまま見える。
    func test_何も指定しないHeaderFooterにはlist下地が見える() throws {
        let backdrop = UIColor(red: 0.07, green: 0.20, blue: 0.34, alpha: 1.0)
        let (_, cv) = hostController(
            sections: textSections(),
            theme: Theme(backgroundColor: backdrop)
        )

        for slot in [Slot.header, Slot.footer] {
            let configured = try XCTUnwrap(
                backgroundColor(cv, slot),
                "既定 Theme でも背景色は設定される（値が透明になる）"
            )
            for style in [UIUserInterfaceStyle.light, .dark] {
                let resolved = configured.resolvedColor(
                    with: UITraitCollection(userInterfaceStyle: style)
                )
                XCTAssertEqual(
                    alpha(of: resolved), 0, accuracy: 0.001,
                    "\(slot) の既定背景は \(style) でも透明で、list 下地が透ける"
                )
            }
        }
        XCTAssertEqual(cv.backgroundColor, backdrop, "list 下地は明示した色のまま")
    }

    /// 既定値の定数そのものが、ライトとダークのどちらでも透明に解決される。
    func test_既定値の定数は両外観で透明に解決される() {
        for color in [Theme.defaultHeaderBackgroundColor, Theme.defaultFooterBackgroundColor] {
            for style in [UIUserInterfaceStyle.light, .dark] {
                let resolved = color.resolvedColor(
                    with: UITraitCollection(userInterfaceStyle: style)
                )
                XCTAssertEqual(
                    alpha(of: resolved), 0, accuracy: 0.001,
                    "既定値の定数は \(style) でも透明"
                )
            }
        }
    }

    /// 固定の透明色を明示した Theme は既定 Theme と等価ではなく、どちらも list 下地が見える。
    func test_固定の透明色を明示したThemeは既定と等価でない() throws {
        let fixedClear = UIColor(white: 0, alpha: 0)
        let explicit = Theme(headerBackgroundColor: fixedClear)
        XCTAssertNotEqual(explicit, Theme(), "固定色の明示は既定と等価にならない")

        let backdrop = UIColor(red: 0.07, green: 0.20, blue: 0.34, alpha: 1.0)
        for theme in [Theme(backgroundColor: backdrop),
                      Theme(backgroundColor: backdrop, headerBackgroundColor: fixedClear)] {
            let (_, cv) = hostController(sections: textSections(), theme: theme)
            let configured = try XCTUnwrap(backgroundColor(cv, .header))
            XCTAssertEqual(
                alpha(of: configured.resolvedColor(with: UITraitCollection(userInterfaceStyle: .light))),
                0, accuracy: 0.001,
                "どちらの Theme でも Header の領域には list 下地が見える"
            )
        }
    }

    /// 色のアルファ成分。
    private func alpha(of color: UIColor) -> CGFloat {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        color.getRed(&r, green: &g, blue: &b, alpha: &a)
        return a
    }

    // MARK: - 外観の切替と accessory の差し替え

    /// 利用者が両外観の値を持つ色を渡した場合、外観の切替でその色のダーク側の値になる。
    func test_利用者のdynamicな背景色は外観切替に追従する() throws {
        let lightSide = UIColor(red: 0.9, green: 0.8, blue: 0.7, alpha: 1.0)
        let darkSide = UIColor(red: 0.2, green: 0.4, blue: 0.6, alpha: 1.0)
        let userColor = UIColor { trait in
            trait.userInterfaceStyle == .dark ? darkSide : lightSide
        }
        let (controller, cv) = hostController(
            sections: textSections(),
            theme: Theme(headerBackgroundColor: userColor)
        )
        let window = try XCTUnwrap(self.window)
        switchAppearance(window, to: .light, controller: controller)

        let configured = try XCTUnwrap(backgroundColor(cv, .header))
        XCTAssertEqual(
            configured.resolvedColor(with: headerTraits(cv)), lightSide,
            "前提: ライト外観では利用者の色のライト側になる"
        )

        switchAppearance(window, to: .dark, controller: controller)
        // 外観の切替は trait の伝播を伴う。伝播前に読むと切替前の値を見てしまう。
        awaitCondition(
            "Header の領域へダーク外観が伝播する",
            in: cv,
            actual: { "userInterfaceStyle = \(self.headerTraits(cv).userInterfaceStyle.rawValue)" },
            until: { self.headerTraits(cv).userInterfaceStyle == .dark }
        )

        let afterSwitch = try XCTUnwrap(backgroundColor(cv, .header))
        XCTAssertEqual(
            afterSwitch.resolvedColor(with: headerTraits(cv)), darkSide,
            "ダークへ切り替えると利用者の色のダーク側で描かれる"
        )
    }

    /// window の外観を切り替え、レイアウトを確定させる。
    private func switchAppearance(
        _ window: UIWindow,
        to style: UIUserInterfaceStyle,
        controller: KsSettingsViewController
    ) {
        window.overrideUserInterfaceStyle = style
        window.layoutIfNeeded()
        layoutNow(controller.internalCollectionView)
    }

    /// Header の supplementary が置かれている trait collection。
    private func headerTraits(_ cv: UICollectionView) -> UITraitCollection {
        return sectionCell(cv, .header, 0)?.traitCollection ?? cv.traitCollection
    }

    /// text 形式から View 形式へ差し替えた領域に、以前の背景色を残さない。
    func test_text形式からView形式へ差し替えた領域に背景色を残さない() throws {
        let sectionID = UUID()
        let cell = LabelCell(title: "A")
        let (controller, cv) = hostController(
            sections: [
                KsSettingsViewCore.Section(
                    id: sectionID,
                    header: .text("一般"),
                    cells: [cell]
                )
            ],
            theme: Theme(headerBackgroundColor: Self.headerBefore)
        )
        XCTAssertEqual(
            backgroundColor(cv, .header), Self.headerBefore,
            "前提: text 形式の Header に背景色が乗っていない"
        )

        controller.applyDiff(
            .full(SettingsRoot(sections: [
                KsSettingsViewCore.Section(
                    id: sectionID,
                    header: .view(KsAnyView.uiKit { UIView() }),
                    cells: [cell]
                )
            ]))
        )
        awaitCondition(
            "Header が View 形式へ差し替わる",
            in: cv,
            actual: { "背景 = \(String(describing: self.backgroundColor(cv, .header)))" },
            until: { self.backgroundColor(cv, .header) == nil }
        )

        XCTAssertNil(
            backgroundColor(cv, .header),
            "View 形式へ差し替えた Header に以前の背景色が残っている"
        )
    }
}
#endif
