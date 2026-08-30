// RootAccessoryThemeRefreshTests.swift
// KsSettingsViewUITests
//
// `applyTheme(_:)` が表示中の Root Header / Footer をどこまで追従させるかを検証する。
//
// text 形式は文字色とフォントが新しい Theme へ追従する。View 形式は追従対象の文字を持たず、
// 再適用すると `KsAnyView` の factory が再実行されて View の内部状態を失うため、対象外とする。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class RootAccessoryThemeRefreshTests: XCTestCase {

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

    /// Root accessory を持つ controller を window に載せ、supplementary が実際に生成された状態にする。
    private func hostController(
        rootHeader: RootAccessory? = nil,
        rootFooter: RootAccessory? = nil,
        theme: Theme = Theme()
    ) -> (KsSettingsViewController, UICollectionView) {
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

        controller.rootHeader = rootHeader
        controller.rootFooter = rootFooter
        pump(cv, seconds: 0.3)
        return (controller, cv)
    }

    private func pump(_ view: UIView, seconds: TimeInterval = 0.05) {
        view.setNeedsLayout()
        view.layoutIfNeeded()
        RunLoop.current.run(until: Date().addingTimeInterval(seconds))
        view.setNeedsLayout()
        view.layoutIfNeeded()
    }

    /// 表示中の Root accessory の text を描く UILabel を取り出す。
    private func rootLabel(_ cv: UICollectionView, elementKind: String) -> UILabel? {
        return cv.visibleSupplementaryViews(ofKind: elementKind)
            .compactMap { $0 as? UICollectionViewListCell }
            .flatMap { $0.contentView.subviews }
            .compactMap { $0 as? UILabel }
            .first
    }

    // MARK: - text 形式は Theme に追従する

    /// `headerTextColor` だけを変えた Theme を適用すると、表示中の Root Header の文字色が追従する。
    func test_applyThemeでRootHeaderのテキスト色が追従する() throws {
        let initialColor = UIColor(red: 0.9, green: 0.1, blue: 0.1, alpha: 1.0)
        let updatedColor = UIColor(red: 0.1, green: 0.2, blue: 0.9, alpha: 1.0)
        let (controller, cv) = hostController(
            rootHeader: .text("プロフィール"),
            theme: Theme(headerTextColor: initialColor)
        )

        let label = try XCTUnwrap(
            rootLabel(cv, elementKind: KsSettingsViewController.rootHeaderElementKind),
            "前提: Root Header のテキスト label が表示されていない"
        )
        XCTAssertEqual(label.textColor, initialColor, "前提: 初期 Theme の文字色が反映されていない")

        controller.applyTheme(Theme(headerTextColor: updatedColor))
        pump(cv)

        let refreshed = try XCTUnwrap(
            rootLabel(cv, elementKind: KsSettingsViewController.rootHeaderElementKind)
        )
        XCTAssertEqual(
            refreshed.textColor,
            updatedColor,
            "applyTheme 後の Root Header の文字色は新しい Theme の headerTextColor になる"
        )
    }

    /// Root Footer も Root Header と同じく `headerTextColor` を流用して追従する。
    func test_applyThemeでRootFooterのテキスト色が追従する() throws {
        let initialColor = UIColor(red: 0.9, green: 0.1, blue: 0.1, alpha: 1.0)
        let updatedColor = UIColor(red: 0.0, green: 0.5, blue: 0.25, alpha: 1.0)
        let (controller, cv) = hostController(
            rootFooter: .text("v1.0.0"),
            theme: Theme(headerTextColor: initialColor)
        )

        let label = try XCTUnwrap(
            rootLabel(cv, elementKind: KsSettingsViewController.rootFooterElementKind),
            "前提: Root Footer のテキスト label が表示されていない"
        )
        XCTAssertEqual(label.textColor, initialColor, "前提: 初期 Theme の文字色が反映されていない")

        controller.applyTheme(Theme(headerTextColor: updatedColor))
        pump(cv)

        let refreshed = try XCTUnwrap(
            rootLabel(cv, elementKind: KsSettingsViewController.rootFooterElementKind)
        )
        XCTAssertEqual(
            refreshed.textColor,
            updatedColor,
            "applyTheme 後の Root Footer の文字色は新しい Theme の headerTextColor になる"
        )
    }

    /// 文字色だけでなくフォントも追従する（`headerFontSize` の変更が label.font へ届く）。
    func test_applyThemeでRootHeaderのフォントが追従する() throws {
        let (controller, cv) = hostController(
            rootHeader: .text("プロフィール"),
            theme: Theme(headerFontSize: 12)
        )

        let label = try XCTUnwrap(
            rootLabel(cv, elementKind: KsSettingsViewController.rootHeaderElementKind)
        )
        XCTAssertEqual(label.font.pointSize, 12, accuracy: 0.01,
                       "前提: 初期 Theme の headerFontSize が反映されていない")

        controller.applyTheme(Theme(headerFontSize: 24))
        pump(cv)

        let refreshed = try XCTUnwrap(
            rootLabel(cv, elementKind: KsSettingsViewController.rootHeaderElementKind)
        )
        XCTAssertEqual(
            refreshed.font.pointSize, 24, accuracy: 0.01,
            "applyTheme 後の Root Header のフォントサイズは新しい Theme の headerFontSize になる"
        )
    }

    /// Root Footer のフォントは `footerFontSize` から解決される。
    func test_applyThemeでRootFooterのフォントが追従する() throws {
        let (controller, cv) = hostController(
            rootFooter: .text("v1.0.0"),
            theme: Theme(footerFontSize: 11)
        )

        let label = try XCTUnwrap(
            rootLabel(cv, elementKind: KsSettingsViewController.rootFooterElementKind)
        )
        XCTAssertEqual(label.font.pointSize, 11, accuracy: 0.01,
                       "前提: 初期 Theme の footerFontSize が反映されていない")

        controller.applyTheme(Theme(footerFontSize: 22))
        pump(cv)

        let refreshed = try XCTUnwrap(
            rootLabel(cv, elementKind: KsSettingsViewController.rootFooterElementKind)
        )
        XCTAssertEqual(
            refreshed.font.pointSize, 22, accuracy: 0.01,
            "applyTheme 後の Root Footer のフォントサイズは新しい Theme の footerFontSize になる"
        )
    }

    // MARK: - View 形式は再構成しない

    /// View 形式の Root Header は Theme 変更で factory を再実行しない。
    ///
    /// factory の再実行は View の内部状態（編集途中のテキスト・スクロール位置・first responder）を
    /// 失わせるため、文字を描かない View 形式には Theme 追従のための再適用を行わない。
    func test_View形式のRootHeaderはapplyThemeでfactoryが再実行されない() {
        let counter = FactoryInvocationCounter()
        let (controller, cv) = hostController(
            rootHeader: .view(KsAnyView.uiKit {
                counter.record()
                let view = UIView()
                view.heightAnchor.constraint(equalToConstant: 40).isActive = true
                return view
            }),
            theme: Theme(headerTextColor: .red)
        )

        let before = counter.count
        XCTAssertGreaterThan(before, 0, "前提: View 形式の Root Header が一度も生成されていない")

        controller.applyTheme(Theme(headerTextColor: .blue))
        pump(cv)

        XCTAssertEqual(
            counter.count,
            before,
            "View 形式の Root Header は Theme 変更で factory を再実行しない（内部状態を失うため）"
        )
    }

    /// View 形式の Root Footer も同じく factory を再実行しない。
    func test_View形式のRootFooterはapplyThemeでfactoryが再実行されない() {
        let counter = FactoryInvocationCounter()
        let (controller, cv) = hostController(
            rootFooter: .view(KsAnyView.uiKit {
                counter.record()
                let view = UIView()
                view.heightAnchor.constraint(equalToConstant: 40).isActive = true
                return view
            }),
            theme: Theme(headerTextColor: .red)
        )

        let before = counter.count
        XCTAssertGreaterThan(before, 0, "前提: View 形式の Root Footer が一度も生成されていない")

        controller.applyTheme(Theme(headerTextColor: .blue))
        pump(cv)

        XCTAssertEqual(
            counter.count,
            before,
            "View 形式の Root Footer は Theme 変更で factory を再実行しない（内部状態を失うため）"
        )
    }

    /// Store 経由の Theme 変更でも text 形式の Root Header が追従する（購読経路の配線確認）。
    func test_Store経由のTheme変更でもRootHeaderのテキスト色が追従する() throws {
        let initialColor = UIColor(red: 0.9, green: 0.1, blue: 0.1, alpha: 1.0)
        let updatedColor = UIColor(red: 0.2, green: 0.7, blue: 0.3, alpha: 1.0)
        let store = SettingsRootStore(
            initialRoot: SettingsRoot(sections: [
                KsSettingsViewCore.Section(cells: [LabelCell(title: "A")])
            ]),
            initialTheme: Theme(headerTextColor: initialColor)
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
        controller.rootHeader = .text("プロフィール")
        pump(cv, seconds: 0.3)

        let label = try XCTUnwrap(
            rootLabel(cv, elementKind: KsSettingsViewController.rootHeaderElementKind)
        )
        XCTAssertEqual(label.textColor, initialColor, "前提: 初期 Theme の文字色が反映されていない")

        store.applyTheme(Theme(headerTextColor: updatedColor))
        pump(cv, seconds: 0.3)

        let refreshed = try XCTUnwrap(
            rootLabel(cv, elementKind: KsSettingsViewController.rootHeaderElementKind)
        )
        XCTAssertEqual(
            refreshed.textColor,
            updatedColor,
            "Store 経由の Theme 変更でも Root Header の文字色は新しい Theme へ追従する"
        )
    }
}
#endif
