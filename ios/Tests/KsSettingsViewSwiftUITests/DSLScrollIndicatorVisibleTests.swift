// DSLScrollIndicatorVisibleTests.swift
// KsSettingsViewSwiftUITests
//
// 宣言 DSL の入口を通しても `Theme.scrollIndicatorVisible` が設定リストの縦スクロールインジケータへ
// 届くことを検証する。Store 方式と DSL 方式は同じ観測結果になることを両方で確かめる決まりになっている。
//
// Store 方式の同じ検証は `KsSettingsViewUITests.ScrollIndicatorVisibleTests` にある。DSL 方式は
// representable の update から `Store.applyTheme(_:)` へ渡す別経路なので、片方だけでは再評価で
// 届かない回帰を拾えない。

#if canImport(UIKit)
import XCTest
import UIKit
import SwiftUI
import KsSettingsViewTestSupport
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class DSLScrollIndicatorVisibleTests: XCTestCase {

    /// SwiftUI の再評価を起こすための観測対象。
    private final class ThemeModel: ObservableObject {
        @Published var scrollIndicatorVisible: Bool = true
    }

    /// DSL で `KsSettingsView` を書き、`ThemeModel` の変化で再評価される SwiftUI ラッパ。
    private struct HostView: SwiftUI.View {
        @ObservedObject var model: ThemeModel

        var body: some SwiftUI.View {
            KsSettingsView {
                ksSection("一般") {
                    LabelCell(title: "A")
                }
            }
            .theme(Theme(scrollIndicatorVisible: model.scrollIndicatorVisible))
        }
    }

    private var window: UIWindow?

    override func tearDown() {
        window?.isHidden = true
        window = nil
        super.tearDown()
    }

    func test_DSL再評価で渡したfalseが縦インジケータへ届く() throws {
        let model = ThemeModel()
        let host = UIHostingController(rootView: HostView(model: model))
        let size = CGSize(width: 375, height: 600)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = host
        window.makeKeyAndVisible()
        self.window = window
        host.view.frame = CGRect(origin: .zero, size: size)
        layoutNow(host.view)

        let controller = try XCTUnwrap(
            findSettingsController(in: host),
            "DSL 経由の KsSettingsViewController が子階層に見つからない"
        )
        let cv = controller.internalCollectionView
        XCTAssertTrue(cv.showsVerticalScrollIndicator, "前提: 既定では表示される")

        model.scrollIndicatorVisible = false
        awaitCondition(
            "DSL の再評価で縦インジケータが消える",
            in: host.view,
            actual: { "showsVerticalScrollIndicator = \(cv.showsVerticalScrollIndicator)" },
            until: { !cv.showsVerticalScrollIndicator }
        )

        XCTAssertFalse(
            cv.showsVerticalScrollIndicator,
            "DSL 経由の Theme 更新で縦インジケータが無効になっていない"
        )
    }

    func test_DSLに最初から渡したfalseが縦インジケータへ届く() throws {
        let model = ThemeModel()
        model.scrollIndicatorVisible = false
        let host = UIHostingController(rootView: HostView(model: model))
        let size = CGSize(width: 375, height: 600)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = host
        window.makeKeyAndVisible()
        self.window = window
        host.view.frame = CGRect(origin: .zero, size: size)
        layoutNow(host.view)

        let controller = try XCTUnwrap(findSettingsController(in: host))
        XCTAssertFalse(
            controller.internalCollectionView.showsVerticalScrollIndicator,
            "初期構築の DSL でも scrollIndicatorVisible = false が反映される"
        )
    }

    /// 子コントローラ階層から `KsSettingsViewController` を探す。
    private func findSettingsController(
        in parent: UIViewController
    ) -> KsSettingsViewController? {
        if let found = parent as? KsSettingsViewController { return found }
        for child in parent.children {
            if let found = findSettingsController(in: child) { return found }
        }
        return nil
    }
}
#endif
