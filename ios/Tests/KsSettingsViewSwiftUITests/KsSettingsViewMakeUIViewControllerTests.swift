// KsSettingsViewMakeUIViewControllerTests.swift
// KsSettingsViewSwiftUITests
//
// SwiftUI ラッパで指定した `style` が UIKit 側のコントローラへ伝わることを直接検証する。
//
//   GIVEN: SwiftUI で `KsSettingsView(... style: .modern)` を記述
//   WHEN : `makeUIViewController(context:)` が呼ばれる
//   THEN : 生成された `KsSettingsViewController` の `style` が `.modern` で初期化される
//
// SwiftUI の `UIViewControllerRepresentable.Context` はテストコードから直接生成できないため、
// `KsSettingsView` を `UIHostingController` に載せて実際の SwiftUI ライフサイクルを駆動し、
// その結果として `makeUIViewController(context:)` が呼ばれて生成された
// `KsSettingsViewController` を子コントローラ階層から探索して `style` を検証する。
//
// あわせて、Context を要しないテスト容易化フック `makeController()`（`makeUIViewController` と
// 同一のコントローラ構築ロジックを共有する）でも `style` 伝達を決定的に検証しておく。

#if canImport(UIKit)
import XCTest
import UIKit
import SwiftUI
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class KsSettingsViewMakeUIViewControllerTests: XCTestCase {

    // MARK: - makeUIViewController(context:) 経路（UIHostingController で実際に駆動）

    func test_modern指定でホスティングするとmakeUIViewControllerがmodernのcontrollerを生成する() {
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [
            KsSettingsViewCore.Section(header: .text("S"), cells: [LabelCell(title: "A")])
        ]))
        let view = KsSettingsView(store: store, style: .modern)

        let controller = hostAndExtractSettingsController(view)
        XCTAssertEqual(controller.style, .modern, "makeUIViewController 経由で生成された controller の style が .modern でない")
    }

    func test_classic指定でホスティングするとmakeUIViewControllerがclassicのcontrollerを生成する() {
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [
            KsSettingsViewCore.Section(header: .text("S"), cells: [LabelCell(title: "A")])
        ]))
        let view = KsSettingsView(store: store, style: .classic)

        let controller = hostAndExtractSettingsController(view)
        XCTAssertEqual(controller.style, .classic, "makeUIViewController 経由で生成された controller の style が .classic でない")
    }

    // MARK: - makeController() フック経路（Context 不要・決定的）

    func test_makeControllerはmodernのcontrollerを生成する() {
        let store = SettingsRootStore(initialRoot: SettingsRoot())
        let view = KsSettingsView(store: store, style: .modern)
        let controller = view.makeController()
        _ = controller.view
        XCTAssertEqual(controller.style, .modern)
    }

    func test_makeControllerはclassicのcontrollerを生成する() {
        let store = SettingsRootStore(initialRoot: SettingsRoot())
        let view = KsSettingsView(store: store, style: .classic)
        let controller = view.makeController()
        _ = controller.view
        XCTAssertEqual(controller.style, .classic)
    }

    // MARK: - ヘルパ

    /// `KsSettingsView` を `UIHostingController` に載せて SwiftUI のライフサイクルを駆動し、
    /// `makeUIViewController(context:)` で生成された `KsSettingsViewController` を子階層から探索して返す。
    private func hostAndExtractSettingsController(
        _ view: KsSettingsView,
        file: StaticString = #filePath,
        line: UInt = #line
    ) -> KsSettingsViewController {
        let host = UIHostingController(rootView: view)

        // SwiftUI に representable の make/update を走らせるため、実際にウィンドウへ載せてレイアウトを強制する。
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 320, height: 480))
        window.rootViewController = host
        window.makeKeyAndVisible()
        host.view.layoutIfNeeded()

        guard let controller = Self.findSettingsController(in: host) else {
            XCTFail("UIHostingController 配下に KsSettingsViewController が見つからない（makeUIViewController が呼ばれていない可能性）", file: file, line: line)
            // 失敗時のフォールバック（このパスには到達しない想定）。
            return KsSettingsViewController(store: SettingsRootStore(initialRoot: SettingsRoot()), style: .classic)
        }
        return controller
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
