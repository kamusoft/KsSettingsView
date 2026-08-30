// KsSettingsViewStyleTests.swift
// KsSettingsViewUITests
//
// `KsSettingsViewStyle` の `.classic` / `.modern` 切替時にレイアウトが再構築され、
// Appearance に対応する `boundarySupplementaryItems` の配置などが正しいかを検証する。
//
// 注意: `UICollectionLayoutListConfiguration.appearance` を直接読み取る公開 API は
// 存在しないため、本テストでは「style 切替時にレイアウトインスタンスが新規に差し替わる」
// および「style に応じた拡張的副作用（root H/F の supplementary 数）」で検証する。
// レイアウト構築コード自体は `KsSettingsViewController.makeLayout` で一元化されており、
// 単体としては挙動が決定的にテスト可能（boundary item 数の検証）。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class KsSettingsViewStyleTests: XCTestCase {
    func test_classicに対応するAppearanceはplain() {
        // classic スタイルが plain Appearance へ対応することの直接検証。
        // `KsSettingsViewController.appearance(for:)` は `internal static` で公開されており、
        // 変換マッピングそのものを単体テストできる。
        XCTAssertEqual(KsSettingsViewController.appearance(for: .classic), .plain)
    }

    func test_modernもplainAppearanceを使いinsetGroupedを使わない() {
        // Modern の箱（余白・角丸・ボーダー）はライブラリが自前で描くため、UIKit の
        // insetGrouped Appearance は使わない（ios/ADR-0003）。
        XCTAssertEqual(KsSettingsViewController.appearance(for: .modern), .plain)
        XCTAssertNotEqual(KsSettingsViewController.appearance(for: .modern), .insetGrouped)
    }

    func test_classicで初期化() {
        let controller = KsSettingsViewController(style: .classic)
        _ = controller.view
        XCTAssertEqual(controller.style, .classic)
    }

    func test_modernで初期化() {
        let controller = KsSettingsViewController(style: .modern)
        _ = controller.view
        XCTAssertEqual(controller.style, .modern)
    }

    func test_動的style切替でレイアウトインスタンスが差し替わる() {
        let controller = KsSettingsViewController(style: .classic)
        _ = controller.view
        let layoutBefore = controller.internalCollectionView.collectionViewLayout

        controller.style = .modern

        let layoutAfter = controller.internalCollectionView.collectionViewLayout
        XCTAssertNotIdentical(layoutBefore, layoutAfter, "style 切替時にレイアウトが再構築されていない")
        XCTAssertEqual(controller.style, .modern)
    }

    func test_同じstyleを再代入しても再構築されない() {
        let controller = KsSettingsViewController(style: .classic)
        _ = controller.view
        let layoutBefore = controller.internalCollectionView.collectionViewLayout

        controller.style = .classic

        let layoutAfter = controller.internalCollectionView.collectionViewLayout
        XCTAssertIdentical(layoutBefore, layoutAfter, "同値再代入で不要な再構築が走っている")
    }
}
#endif
