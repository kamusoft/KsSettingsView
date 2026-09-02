// KsUITestWait.swift
// KsSettingsViewUITests
//
// UI テストで頻出する収束条件を述語として共有するヘルパ。

#if canImport(UIKit)
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

/// Controller を window へ載せた直後の初期スナップショットが実描画されるまで待つ。
///
/// 期待する Section 構造は visible projection から、Section に属さない Root accessory の
/// boundary supplementary は controller の設定から求め、行と supplementary の実体化まで
/// `awaitCollectionRender` が待つ。
@MainActor
func awaitInitialRender(
    _ controller: KsSettingsViewController,
    file: StaticString = #filePath,
    line: UInt = #line
) {
    let expected = KsSettingsViewController.computeVisibleSections(from: controller.root.sections)
    awaitCollectionRender(
        controller.internalCollectionView,
        "初期スナップショットの実描画",
        expectedItemCounts: expected.map(\.cells.count),
        requiredSupplementaryKinds: expectedRootSupplementaryKinds(controller),
        file: file,
        line: line
    )
}

/// 設定済みの Root accessory から、実体化していなければならない boundary supplementary の
/// elementKind を求める。
///
/// Root accessory は layout 全体の boundary supplementary であり、どの Section にも属さない。
/// Section 構造からは存在を導けないため、controller の設定から明示的に列挙する。
@MainActor
func expectedRootSupplementaryKinds(_ controller: KsSettingsViewController) -> [String] {
    var kinds: [String] = []
    if controller.rootHeader != nil {
        kinds.append(KsSettingsViewController.rootHeaderElementKind)
    }
    if controller.rootFooter != nil {
        kinds.append(KsSettingsViewController.rootFooterElementKind)
    }
    return kinds
}
#endif
