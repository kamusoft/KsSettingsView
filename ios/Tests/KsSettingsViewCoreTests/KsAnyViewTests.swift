// KsAnyViewTests.swift
// KsSettingsViewCoreTests
//
// `KsAnyView` の swiftUI / uiKit 二択 backing とファクトリ API、および差分検出に
// 参加しないことを検証する。
//
// 主目的は、`KsAnyView` が `Hashable` / `Equatable` に準拠していないことを型レベルで
// 保証することである。直接 `XCTAssertEqual(KsAnyView(...), KsAnyView(...))` のような
// テストは「コンパイルできてしまうこと」が誤った保証になるため、ここでは
// 「Hashable/Equatable に conform するならこの関数が呼べてしまう」というメタ関数を
// `KsAnyView` で呼び出さないことで間接的に確認する。

import SwiftUI
import XCTest
#if canImport(UIKit)
import UIKit
#endif
@testable import KsSettingsViewCore

final class KsAnyViewTests: XCTestCase {

    // MARK: - 構築

    func test_swiftUI_ファクトリで構築できる() {
        let anyView = KsAnyView.swiftUI { Text("hello") }
        // backing が swiftUI ケースであること
        if case .swiftUI = anyView.backing {
            // OK
        } else {
            XCTFail("backing should be .swiftUI")
        }
    }

    #if canImport(UIKit)
    func test_uiKit_ファクトリで構築できる() {
        let anyView = KsAnyView.uiKit { UIView() }
        // backing が uiKit ケースであること
        if case .uiKit = anyView.backing {
            // OK
        } else {
            XCTFail("backing should be .uiKit")
        }
    }
    #endif

    func test_swiftUI_ファクトリは_AnyView_に型消去する() {
        let anyView = KsAnyView.swiftUI { Text("hello") }
        guard case let .swiftUI(build) = anyView.backing else {
            XCTFail("backing should be .swiftUI")
            return
        }
        // build() を呼んで AnyView を取得（クラッシュしないことの確認）
        let view = build()
        XCTAssertNotNil(view as Any)
    }

    #if canImport(UIKit)
    func test_uiKit_ファクトリは呼ばれるたびに新規_UIView_を返せる() {
        var callCount = 0
        let anyView = KsAnyView.uiKit {
            callCount += 1
            return UIView()
        }
        guard case let .uiKit(factory) = anyView.backing else {
            XCTFail("backing should be .uiKit")
            return
        }
        _ = factory()
        _ = factory()
        XCTAssertEqual(callCount, 2)
    }
    #endif

    // MARK: - Hashable / Equatable に準拠していないことの型レベル保証
    //
    // - `isHashable(_:)` / `isEquatable(_:)` は型条件付きジェネリック関数として定義する。
    //   `KsAnyView` を渡すと「制約を満たさない」のでコンパイルエラーになる…と書きたいが、
    //   コンパイルエラー自体はテストでアサーションできない。代わりに以下のアプローチを取る:
    //
    //   - `KsAnyView as? any Hashable` が `nil` になることをランタイム検証する。
    //     プロトコル準拠していない場合、`as?` キャストは nil を返す。
    //   - 同様に `KsAnyView as? any Equatable` も `nil` になる。
    //
    //   これにより「`KsAnyView` は `Hashable` / `Equatable` に conform していない」ことを
    //   実行時に確認でき、もし将来誤って準拠が追加された場合にこのテストが失敗する。

    func test_KsAnyView_は_Hashable_に準拠していない() {
        let anyView = KsAnyView.swiftUI { Text("a") }
        // any Hashable へのキャストは nil（KsAnyView は Hashable に conform していない）
        let asHashable = anyView as Any as? any Hashable
        XCTAssertNil(asHashable, "KsAnyView は Hashable に準拠してはならない（Decision 3）")
    }

    func test_KsAnyView_は_Equatable_に準拠していない() {
        let anyView = KsAnyView.swiftUI { Text("a") }
        // any Equatable へのキャストは nil（KsAnyView は Equatable に conform していない）
        let asEquatable = anyView as Any as? any Equatable
        XCTAssertNil(asEquatable, "KsAnyView は Equatable に準拠してはならない（Decision 3）")
    }
}
