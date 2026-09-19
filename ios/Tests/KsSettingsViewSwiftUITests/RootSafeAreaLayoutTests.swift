// RootSafeAreaLayoutTests.swift
// KsSettingsViewSwiftUITests
//
// `KsSettingsView` を `UIHostingController` + `UIWindow` に載せ、セーフエリアを与えた状態で
// 実際に配置された `KsSettingsViewController.view` の frame を測る。
//
// 保持値 (`_respectsSafeArea`) を読むだけのテストでは、`body` の `.ignoresSafeArea(.container, edges:)`
// を外しても緑のままになる。配置そのものを実測することで既定の全面配置を回帰から守る。
//
// セーフエリアは `UIHostingController.additionalSafeAreaInsets` で与える。window 由来の
// セーフエリアは実行機の機種に依存するため、期待値は固定値ではなくホスト View の
// 実測 `safeAreaInsets` から導出する。

#if canImport(UIKit)
import XCTest
import SwiftUI
import UIKit
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class RootSafeAreaLayoutTests: XCTestCase {

    /// ホスト View の寸法。
    private static let hostSize = CGSize(width: 375, height: 600)
    /// 実行機のセーフエリアに上乗せする量。0 だと機種によっては全辺 0 になり検証にならない。
    private static let additionalInsets = UIEdgeInsets(top: 44, left: 0, bottom: 34, right: 0)

    private var window: UIWindow?

    override func tearDown() {
        window?.isHidden = true
        window = nil
        super.tearDown()
    }

    // MARK: - 既定 (全面配置)

    func test_既定ではStore方式の一覧がセーフエリアの外まで広がる() throws {
        let hosted = try host(KsSettingsView(store: Self.makeStore()))

        XCTAssertEqual(
            hosted.listFrameInHost,
            CGRect(origin: .zero, size: Self.hostSize),
            "既定の KsSettingsView がホストの全面に広がっていない"
        )
    }

    func test_既定ではDSL方式の一覧がセーフエリアの外まで広がる() throws {
        let hosted = try host(KsSettingsView { ksSection("S") { LabelCell(title: "A") } })

        XCTAssertEqual(
            hosted.listFrameInHost,
            CGRect(origin: .zero, size: Self.hostSize),
            "既定の DSL 方式 KsSettingsView がホストの全面に広がっていない"
        )
    }

    func test_既定ではStore方式とDSL方式の配置が一致する() throws {
        let storeBacked = try host(KsSettingsView(store: Self.makeStore()))
        let dslBacked = try host(KsSettingsView { ksSection("S") { LabelCell(title: "A") } })

        XCTAssertEqual(
            storeBacked.listFrameInHost,
            dslBacked.listFrameInHost,
            "Store 方式と DSL 方式で一覧の配置が一致していない"
        )
    }

    func test_既定ではbarに覆われる領域のinsetがUIKit側へ渡る() throws {
        let hosted = try host(KsSettingsView(store: Self.makeStore()))

        // 全面に広がった結果として、bar に覆われる量は Controller 側のセーフエリアとして残る。
        // この値が 0 になると UIKit の automatic な contentInset 調整が効かず、
        // 先頭 / 末尾 Cell が bar に隠れる。
        XCTAssertEqual(
            hosted.listSafeAreaInsets,
            hosted.hostSafeAreaInsets,
            "一覧側のセーフエリアがホストと一致していない (inset を UIKit に委ねられていない)"
        )
        XCTAssertGreaterThan(hosted.hostSafeAreaInsets.top, 0, "前提: ホストに上側のセーフエリアが与えられている")
        XCTAssertGreaterThan(hosted.hostSafeAreaInsets.bottom, 0, "前提: ホストに下側のセーフエリアが与えられている")
    }

    // MARK: - respectsSafeArea (opt-out)

    func test_respectsSafeAreaを付けると一覧がセーフエリアの内側に縮む() throws {
        let hosted = try host(KsSettingsView(store: Self.makeStore()).respectsSafeArea())
        let insets = hosted.hostSafeAreaInsets

        XCTAssertEqual(
            hosted.listFrameInHost,
            CGRect(
                x: insets.left,
                y: insets.top,
                width: Self.hostSize.width - insets.left - insets.right,
                height: Self.hostSize.height - insets.top - insets.bottom
            ),
            "respectsSafeArea() を付けても一覧がセーフエリアの内側に収まっていない"
        )
    }

    func test_respectsSafeAreaにfalseを渡すと既定の全面配置になる() throws {
        let hosted = try host(KsSettingsView(store: Self.makeStore()).respectsSafeArea(false))

        XCTAssertEqual(
            hosted.listFrameInHost,
            CGRect(origin: .zero, size: Self.hostSize),
            "respectsSafeArea(false) が既定の全面配置に戻っていない"
        )
    }

    func test_respectsSafeAreaは他のRootModifierと連鎖しても配置に効く() throws {
        let hosted = try host(
            KsSettingsView(store: Self.makeStore())
                .style(.modern)
                .respectsSafeArea()
                .rootHeader("H")
        )
        let insets = hosted.hostSafeAreaInsets

        XCTAssertEqual(
            hosted.listFrameInHost.origin.y,
            insets.top,
            "他の Root modifier と連鎖すると respectsSafeArea() が配置に効かない"
        )
    }

    // MARK: - ヘルパ

    /// ホストした結果の実測値。
    private struct Hosted {
        /// ホスト View 座標系での一覧の frame。
        let listFrameInHost: CGRect
        /// 一覧側に届いているセーフエリア。
        let listSafeAreaInsets: UIEdgeInsets
        /// ホスト View のセーフエリア (実行機依存のため実測して期待値の導出に使う)。
        let hostSafeAreaInsets: UIEdgeInsets
    }

    private static func makeStore() -> SettingsRootStore {
        SettingsRootStore(initialRoot: SettingsRoot(sections: [
            KsSettingsViewCore.Section(header: .text("S"), cells: [LabelCell(title: "A")])
        ]))
    }

    /// `KsSettingsView` を window 付きの `UIHostingController` に載せてレイアウトを確定させ、
    /// 配置された `KsSettingsViewController` の実測値を返す。
    private func host(
        _ view: KsSettingsView,
        file: StaticString = #filePath,
        line: UInt = #line
    ) throws -> Hosted {
        let host = UIHostingController(rootView: view)
        host.additionalSafeAreaInsets = Self.additionalInsets

        let window = UIWindow(frame: CGRect(origin: .zero, size: Self.hostSize))
        window.rootViewController = host
        window.makeKeyAndVisible()
        self.window = window

        host.view.frame = CGRect(origin: .zero, size: Self.hostSize)
        host.view.setNeedsLayout()
        host.view.layoutIfNeeded()

        let controller = try XCTUnwrap(
            Self.findSettingsController(in: host),
            "UIHostingController 配下に KsSettingsViewController が見つからない",
            file: file,
            line: line
        )
        let listView = try XCTUnwrap(controller.viewIfLoaded, "一覧の view が読み込まれていない", file: file, line: line)

        return Hosted(
            listFrameInHost: listView.convert(listView.bounds, to: host.view),
            listSafeAreaInsets: listView.safeAreaInsets,
            hostSafeAreaInsets: host.view.safeAreaInsets
        )
    }

    /// コントローラ階層を再帰的に辿り、最初の `KsSettingsViewController` を返す。
    private static func findSettingsController(in parent: UIViewController) -> KsSettingsViewController? {
        if let match = parent as? KsSettingsViewController {
            return match
        }
        for child in parent.children {
            if let found = findSettingsController(in: child) {
                return found
            }
        }
        return nil
    }
}
#endif
