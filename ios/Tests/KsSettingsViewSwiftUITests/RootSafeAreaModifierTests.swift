// RootSafeAreaModifierTests.swift
// KsSettingsViewSwiftUITests
//
// Root modifier `respectsSafeArea(_:)` の既定値・切り替え・他の Root modifier との連鎖、
// および copy 返却（元の値が変わらないこと）を検証する。
// 配置そのもの（セーフエリアを無視して全面に広がること）は SwiftUI のレイアウト結果であり、
// ここでは modifier が保持する設定値の契約だけを検証する。

#if canImport(UIKit)
import XCTest
import SwiftUI
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class RootSafeAreaModifierTests: XCTestCase {

    private func makeStore() -> SettingsRootStore {
        SettingsRootStore(initialRoot: SettingsRoot(sections: [
            KsSettingsViewCore.Section(header: .text("S"), cells: [LabelCell(title: "A")])
        ]))
    }

    func test_既定ではセーフエリアを尊重しない() {
        let storeBacked = KsSettingsView(store: makeStore())
        XCTAssertFalse(storeBacked._respectsSafeArea)

        let dslBacked = KsSettingsView {
            ksSection("S") { LabelCell(title: "A") }
        }
        XCTAssertFalse(dslBacked._respectsSafeArea)
    }

    func test_respectsSafeAreaは引数を省略するとtrueになる() {
        let view = KsSettingsView(store: makeStore()).respectsSafeArea()
        XCTAssertTrue(view._respectsSafeArea)
    }

    func test_respectsSafeAreaにfalseを渡すと既定の全面配置に戻る() {
        let view = KsSettingsView(store: makeStore())
            .respectsSafeArea()
            .respectsSafeArea(false)
        XCTAssertFalse(view._respectsSafeArea)
    }

    func test_respectsSafeAreaはcopyを返し元の値を変えない() {
        let original = KsSettingsView(store: makeStore())
        let modified = original.respectsSafeArea()

        XCTAssertFalse(original._respectsSafeArea)
        XCTAssertTrue(modified._respectsSafeArea)
    }

    func test_他のRootModifierと連鎖しても互いの値を失わない() {
        // style → respectsSafeArea → rootHeader の順
        let ordered = KsSettingsView(store: makeStore())
            .style(.modern)
            .respectsSafeArea()
            .rootHeader("H")
        XCTAssertEqual(ordered._style, .modern)
        XCTAssertTrue(ordered._respectsSafeArea)
        XCTAssertEqual(ordered._rootHeader, .text("H"))

        // 逆順で連鎖しても同じ結果になる
        let reversed = KsSettingsView(store: makeStore())
            .rootHeader("H")
            .respectsSafeArea()
            .style(.modern)
        XCTAssertEqual(reversed._style, .modern)
        XCTAssertTrue(reversed._respectsSafeArea)
        XCTAssertEqual(reversed._rootHeader, .text("H"))
    }

    func test_respectsSafeAreaはrootFooterとthemeの値を保つ() {
        let theme = Theme()
        let view = KsSettingsView(store: makeStore())
            .rootFooter("F")
            .theme(theme)
            .respectsSafeArea()

        XCTAssertTrue(view._respectsSafeArea)
        XCTAssertEqual(view._rootFooter, .text("F"))
        XCTAssertEqual(view._theme, theme)
    }

    func test_DSL方式でもrespectsSafeAreaが保持される() {
        let view = KsSettingsView {
            ksSection("S") { LabelCell(title: "A") }
        }
        .respectsSafeArea()

        XCTAssertTrue(view._respectsSafeArea)
        if case .dsl = view.backing {
            // DSL 方式のまま保たれる
        } else {
            XCTFail("DSL backing が保持されていない")
        }
    }
}
#endif
