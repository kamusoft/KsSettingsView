// SectionAccessoryThemeRefreshTests.swift
// KsSettingsViewUITests
//
// `applyTheme(_:)` が表示中の Section Header / Footer をどこまで追従させるかを検証する。
//
// text 形式は Header が `headerTextColor`、Footer が `footerTextColor` とそれぞれのフォントへ
// 追従する。View 形式は追従対象の文字を持たず、再適用すると `KsAnyView` の factory が
// 再実行されて View の内部状態を失うため、対象外とする。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class SectionAccessoryThemeRefreshTests: XCTestCase {

    /// `KsAnyView` の factory が何回実行されたかを数えるカウンタ。
    private final class FactoryInvocationCounter {
        private(set) var count = 0
        func record() { count += 1 }
    }

    private var window: UIWindow?

    override func tearDown() {
        window?.isHidden = true
        window = nil
        super.tearDown()
    }

    /// 指定 Section を持つ controller を window に載せ、supplementary が生成された状態にする。
    private func hostController(
        sections: [KsSettingsViewCore.Section],
        theme: Theme = Theme()
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
        awaitCondition(
            "初期 Section と先頭 Section の accessory supplementary が生成される",
            in: cv,
            actual: {
                "Section \(cv.numberOfSections) / header \(hasSupplementary(cv, .header, 0))"
                    + " / footer \(hasSupplementary(cv, .footer, 0))"
            },
            until: {
                guard cv.numberOfSections == sections.count, let first = sections.first else {
                    return false
                }
                if first.header != nil, !hasSupplementary(cv, .header, 0) { return false }
                if first.footer != nil, !hasSupplementary(cv, .footer, 0) { return false }
                return true
            }
        )
        return (controller, cv)
    }

    /// Section accessory の位置。
    @MainActor
    private enum SectionAccessorySlot {
        case header
        case footer

        var elementKind: String {
            switch self {
            case .header: return UICollectionView.elementKindSectionHeader
            case .footer: return UICollectionView.elementKindSectionFooter
            }
        }
    }

    /// 指定 section の accessory supplementary が生成されているかを返す。
    private func hasSupplementary(
        _ cv: UICollectionView,
        _ slot: SectionAccessorySlot,
        _ section: Int
    ) -> Bool {
        return cv.supplementaryView(
            forElementKind: slot.elementKind,
            at: IndexPath(item: 0, section: section)
        ) != nil
    }

    /// 指定 section の accessory label の文字色が期待値になるまで待つ。
    private func awaitLabelColor(
        _ cv: UICollectionView,
        _ slot: SectionAccessorySlot,
        section: Int = 0,
        equals expected: UIColor,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        awaitEqual(
            "Section accessory の文字色",
            expected: expected as UIColor?,
            in: cv,
            file: file,
            line: line,
            actual: { sectionLabel(cv, elementKind: slot.elementKind, section: section)?.textColor }
        )
    }

    /// 指定 section の accessory label のフォントサイズが期待値になるまで待つ。
    private func awaitLabelFontSize(
        _ cv: UICollectionView,
        _ slot: SectionAccessorySlot,
        section: Int = 0,
        equals expected: CGFloat,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        awaitCondition(
            "Section accessory のフォントサイズ (期待値: \(expected))",
            in: cv,
            actual: {
                let size = sectionLabel(cv, elementKind: slot.elementKind, section: section)?.font.pointSize
                return "pointSize = \(String(describing: size))"
            },
            file: file,
            line: line,
            until: {
                guard let font = sectionLabel(
                    cv, elementKind: slot.elementKind, section: section
                )?.font else { return false }
                return abs(font.pointSize - expected) <= 0.01
            }
        )
    }

    /// 指定 section の supplementary の text を描く UILabel を取り出す。
    private func sectionLabel(
        _ cv: UICollectionView,
        elementKind: String,
        section: Int
    ) -> UILabel? {
        let view = cv.supplementaryView(
            forElementKind: elementKind,
            at: IndexPath(item: 0, section: section)
        )
        return (view as? UICollectionViewListCell)?
            .contentView.subviews
            .compactMap { $0 as? UILabel }
            .first
    }

    // MARK: - text 形式は Theme に追従する

    /// `headerTextColor` を変えた Theme を適用すると、表示中の Section Header の文字色が追従する。
    func test_applyThemeでSectionHeaderのテキスト色が追従する() throws {
        let initialColor = UIColor(red: 0.9, green: 0.1, blue: 0.1, alpha: 1.0)
        let updatedColor = UIColor(red: 0.1, green: 0.2, blue: 0.9, alpha: 1.0)
        let (controller, cv) = hostController(
            sections: [
                KsSettingsViewCore.Section(header: .text("一般"), cells: [LabelCell(title: "A")])
            ],
            theme: Theme(headerTextColor: initialColor)
        )

        let label = try XCTUnwrap(
            sectionLabel(cv, elementKind: UICollectionView.elementKindSectionHeader, section: 0),
            "前提: Section Header のテキスト label が表示されていない"
        )
        XCTAssertEqual(label.textColor, initialColor, "前提: 初期 Theme の文字色が反映されていない")

        controller.applyTheme(Theme(headerTextColor: updatedColor))
        awaitLabelColor(cv, .header, equals: updatedColor)

        let refreshed = try XCTUnwrap(
            sectionLabel(cv, elementKind: UICollectionView.elementKindSectionHeader, section: 0)
        )
        XCTAssertEqual(
            refreshed.textColor,
            updatedColor,
            "applyTheme 後の Section Header の文字色は新しい Theme の headerTextColor になる"
        )
    }

    /// Section Footer は `footerTextColor` に追従する（Header の色とは区別される）。
    func test_applyThemeでSectionFooterのテキスト色が追従する() throws {
        let initialColor = UIColor(red: 0.5, green: 0.5, blue: 0.5, alpha: 1.0)
        let updatedColor = UIColor(red: 0.0, green: 0.5, blue: 0.25, alpha: 1.0)
        let (controller, cv) = hostController(
            sections: [
                KsSettingsViewCore.Section(
                    header: .text("一般"),
                    footer: .text("補足説明"),
                    cells: [LabelCell(title: "A")]
                )
            ],
            theme: Theme(footerTextColor: initialColor)
        )

        let label = try XCTUnwrap(
            sectionLabel(cv, elementKind: UICollectionView.elementKindSectionFooter, section: 0),
            "前提: Section Footer のテキスト label が表示されていない"
        )
        XCTAssertEqual(label.textColor, initialColor, "前提: 初期 Theme の文字色が反映されていない")

        controller.applyTheme(Theme(footerTextColor: updatedColor))
        awaitLabelColor(cv, .footer, equals: updatedColor)

        let refreshed = try XCTUnwrap(
            sectionLabel(cv, elementKind: UICollectionView.elementKindSectionFooter, section: 0)
        )
        XCTAssertEqual(
            refreshed.textColor,
            updatedColor,
            "applyTheme 後の Section Footer の文字色は新しい Theme の footerTextColor になる"
        )
    }

    /// Header の色変更は Footer の色に影響しない（両者が別の Theme 属性から解決される）。
    func test_SectionHeaderとFooterはそれぞれのTheme属性に追従する() throws {
        let headerAfter = UIColor(red: 0.8, green: 0.0, blue: 0.4, alpha: 1.0)
        let footerAfter = UIColor(red: 0.0, green: 0.4, blue: 0.8, alpha: 1.0)
        let (controller, cv) = hostController(
            sections: [
                KsSettingsViewCore.Section(
                    header: .text("一般"),
                    footer: .text("補足説明"),
                    cells: [LabelCell(title: "A")]
                )
            ],
            theme: Theme(
                headerTextColor: UIColor(red: 0.9, green: 0.1, blue: 0.1, alpha: 1.0),
                footerTextColor: UIColor(red: 0.5, green: 0.5, blue: 0.5, alpha: 1.0)
            )
        )

        controller.applyTheme(Theme(headerTextColor: headerAfter, footerTextColor: footerAfter))
        awaitLabelColor(cv, .header, equals: headerAfter)
        awaitLabelColor(cv, .footer, equals: footerAfter)

        let header = try XCTUnwrap(
            sectionLabel(cv, elementKind: UICollectionView.elementKindSectionHeader, section: 0)
        )
        let footer = try XCTUnwrap(
            sectionLabel(cv, elementKind: UICollectionView.elementKindSectionFooter, section: 0)
        )
        XCTAssertEqual(header.textColor, headerAfter, "Header は headerTextColor へ追従する")
        XCTAssertEqual(footer.textColor, footerAfter, "Footer は footerTextColor へ追従する")
    }

    /// 文字色だけでなくフォントも追従する（`headerFontSize` の変更が label.font へ届く）。
    func test_applyThemeでSectionHeaderのフォントが追従する() throws {
        let (controller, cv) = hostController(
            sections: [
                KsSettingsViewCore.Section(header: .text("一般"), cells: [LabelCell(title: "A")])
            ],
            theme: Theme(headerFontSize: 12)
        )

        let label = try XCTUnwrap(
            sectionLabel(cv, elementKind: UICollectionView.elementKindSectionHeader, section: 0)
        )
        XCTAssertEqual(label.font.pointSize, 12, accuracy: 0.01,
                       "前提: 初期 Theme の headerFontSize が反映されていない")

        controller.applyTheme(Theme(headerFontSize: 24))
        awaitLabelFontSize(cv, .header, equals: 24)

        let refreshed = try XCTUnwrap(
            sectionLabel(cv, elementKind: UICollectionView.elementKindSectionHeader, section: 0)
        )
        XCTAssertEqual(
            refreshed.font.pointSize, 24, accuracy: 0.01,
            "applyTheme 後の Section Header のフォントサイズは新しい Theme の headerFontSize になる"
        )
    }

    /// Section Footer のフォントは `footerFontSize` から解決される。
    func test_applyThemeでSectionFooterのフォントが追従する() throws {
        let (controller, cv) = hostController(
            sections: [
                KsSettingsViewCore.Section(
                    header: .text("一般"),
                    footer: .text("補足説明"),
                    cells: [LabelCell(title: "A")]
                )
            ],
            theme: Theme(footerFontSize: 11)
        )

        let label = try XCTUnwrap(
            sectionLabel(cv, elementKind: UICollectionView.elementKindSectionFooter, section: 0)
        )
        XCTAssertEqual(label.font.pointSize, 11, accuracy: 0.01,
                       "前提: 初期 Theme の footerFontSize が反映されていない")

        controller.applyTheme(Theme(footerFontSize: 22))
        awaitLabelFontSize(cv, .footer, equals: 22)

        let refreshed = try XCTUnwrap(
            sectionLabel(cv, elementKind: UICollectionView.elementKindSectionFooter, section: 0)
        )
        XCTAssertEqual(
            refreshed.font.pointSize, 22, accuracy: 0.01,
            "applyTheme 後の Section Footer のフォントサイズは新しい Theme の footerFontSize になる"
        )
    }

    /// 複数 Section が表示されている場合、そのすべての Header が追従する。
    func test_複数SectionのHeaderがすべて追従する() throws {
        let updatedColor = UIColor(red: 0.1, green: 0.2, blue: 0.9, alpha: 1.0)
        let (controller, cv) = hostController(
            sections: [
                KsSettingsViewCore.Section(header: .text("一般"), cells: [LabelCell(title: "A")]),
                KsSettingsViewCore.Section(header: .text("高度"), cells: [LabelCell(title: "B")]),
                KsSettingsViewCore.Section(header: .text("その他"), cells: [LabelCell(title: "C")])
            ],
            theme: Theme(headerTextColor: UIColor(red: 0.9, green: 0.1, blue: 0.1, alpha: 1.0))
        )

        controller.applyTheme(Theme(headerTextColor: updatedColor))
        for section in 0..<3 {
            awaitLabelColor(cv, .header, section: section, equals: updatedColor)
        }

        for section in 0..<3 {
            let label = try XCTUnwrap(
                sectionLabel(
                    cv,
                    elementKind: UICollectionView.elementKindSectionHeader,
                    section: section
                ),
                "section=\(section) の Header label が取得できない"
            )
            XCTAssertEqual(
                label.textColor,
                updatedColor,
                "section=\(section) の Header の文字色が新しい Theme に追従していない"
            )
        }
    }

    // MARK: - View 形式は再構成しない

    /// View 形式の Section Header は Theme 変更で factory を再実行しない。
    func test_View形式のSectionHeaderはapplyThemeでfactoryが再実行されない() {
        let counter = FactoryInvocationCounter()
        let (controller, cv) = hostController(
            sections: [
                KsSettingsViewCore.Section(
                    header: .view(KsAnyView.uiKit {
                        counter.record()
                        let view = UIView()
                        view.heightAnchor.constraint(equalToConstant: 40).isActive = true
                        return view
                    }),
                    cells: [LabelCell(title: "A")]
                )
            ],
            theme: Theme(headerTextColor: .red)
        )

        let before = counter.count
        XCTAssertGreaterThan(before, 0, "前提: View 形式の Section Header が一度も生成されていない")

        controller.applyTheme(Theme(headerTextColor: .blue))
        waitForNegativeVerification(in: cv)

        XCTAssertEqual(
            counter.count,
            before,
            "View 形式の Section Header は Theme 変更で factory を再実行しない（内部状態を失うため）"
        )
    }

    /// View 形式の Section Footer も同じく factory を再実行しない。
    func test_View形式のSectionFooterはapplyThemeでfactoryが再実行されない() {
        let counter = FactoryInvocationCounter()
        let (controller, cv) = hostController(
            sections: [
                KsSettingsViewCore.Section(
                    header: .text("一般"),
                    footer: .view(KsAnyView.uiKit {
                        counter.record()
                        let view = UIView()
                        view.heightAnchor.constraint(equalToConstant: 40).isActive = true
                        return view
                    }),
                    cells: [LabelCell(title: "A")]
                )
            ],
            theme: Theme(footerTextColor: .red)
        )

        let before = counter.count
        XCTAssertGreaterThan(before, 0, "前提: View 形式の Section Footer が一度も生成されていない")

        controller.applyTheme(Theme(footerTextColor: .blue))
        waitForNegativeVerification(in: cv)

        XCTAssertEqual(
            counter.count,
            before,
            "View 形式の Section Footer は Theme 変更で factory を再実行しない（内部状態を失うため）"
        )
    }

    /// 同じ Section 内で View 形式 Header と text 形式 Footer が混在しても、
    /// text 形式だけが追従し View 形式は再構成されない。
    func test_View形式Headerとtext形式Footerの混在ではtext側だけが追従する() throws {
        let counter = FactoryInvocationCounter()
        let updatedColor = UIColor(red: 0.0, green: 0.5, blue: 0.25, alpha: 1.0)
        let (controller, cv) = hostController(
            sections: [
                KsSettingsViewCore.Section(
                    header: .view(KsAnyView.uiKit {
                        counter.record()
                        let view = UIView()
                        view.heightAnchor.constraint(equalToConstant: 40).isActive = true
                        return view
                    }),
                    footer: .text("補足説明"),
                    cells: [LabelCell(title: "A")]
                )
            ],
            theme: Theme(footerTextColor: UIColor(red: 0.5, green: 0.5, blue: 0.5, alpha: 1.0))
        )

        let before = counter.count
        XCTAssertGreaterThan(before, 0, "前提: View 形式の Section Header が一度も生成されていない")

        controller.applyTheme(Theme(footerTextColor: updatedColor))
        awaitLabelColor(cv, .footer, equals: updatedColor)

        let footer = try XCTUnwrap(
            sectionLabel(cv, elementKind: UICollectionView.elementKindSectionFooter, section: 0)
        )
        XCTAssertEqual(footer.textColor, updatedColor, "text 形式の Footer は追従する")
        XCTAssertEqual(
            counter.count,
            before,
            "同じ Section の View 形式 Header は factory を再実行しない"
        )
    }

    /// Store 経由の Theme 変更でも Section Header が追従する（購読経路の配線確認）。
    func test_Store経由のTheme変更でもSectionHeaderのテキスト色が追従する() throws {
        let initialColor = UIColor(red: 0.9, green: 0.1, blue: 0.1, alpha: 1.0)
        let updatedColor = UIColor(red: 0.2, green: 0.7, blue: 0.3, alpha: 1.0)
        let store = SettingsRootStore(
            initialRoot: SettingsRoot(sections: [
                KsSettingsViewCore.Section(header: .text("一般"), cells: [LabelCell(title: "A")])
            ]),
            initialTheme: Theme(headerTextColor: initialColor)
        )
        let controller = KsSettingsViewController(store: store)
        let size = CGSize(width: 375, height: 700)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        self.window = window
        controller.view!.frame = CGRect(origin: .zero, size: size)
        controller.view!.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        awaitNonNil(
            "Section Header のテキスト label が生成される",
            in: cv,
            produce: {
                sectionLabel(cv, elementKind: UICollectionView.elementKindSectionHeader, section: 0)
            }
        )

        let label = try XCTUnwrap(
            sectionLabel(cv, elementKind: UICollectionView.elementKindSectionHeader, section: 0)
        )
        XCTAssertEqual(label.textColor, initialColor, "前提: 初期 Theme の文字色が反映されていない")

        store.applyTheme(Theme(headerTextColor: updatedColor))
        awaitLabelColor(cv, .header, equals: updatedColor)

        let refreshed = try XCTUnwrap(
            sectionLabel(cv, elementKind: UICollectionView.elementKindSectionHeader, section: 0)
        )
        XCTAssertEqual(
            refreshed.textColor,
            updatedColor,
            "Store 経由の Theme 変更でも Section Header の文字色は新しい Theme へ追従する"
        )
    }
}
#endif
