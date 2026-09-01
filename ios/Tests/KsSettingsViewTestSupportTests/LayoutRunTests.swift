// LayoutRunTests.swift
// KsSettingsViewTestSupportTests
//
// レイアウト実行ヘルパが、レイアウトを走らせるだけで時間待機を伴わないことを検証する。

#if canImport(UIKit)
import XCTest
import Foundation
import UIKit
import KsSettingsViewTestSupport

/// `layoutSubviews` の実行回数を数える view。
private final class LayoutCountingView: UIView {
    private(set) var layoutCount = 0

    override func layoutSubviews() {
        super.layoutSubviews()
        layoutCount += 1
    }
}

@MainActor
final class LayoutRunTests: XCTestCase {

    /// レイアウトが同期的に走り、時間待機は発生しない。
    func testLayoutNowRunsLayoutWithoutSpendingTime() {
        let view = LayoutCountingView(frame: CGRect(x: 0, y: 0, width: 100, height: 100))
        view.layoutIfNeeded()
        let baseline = view.layoutCount

        let start = DispatchTime.now()
        layoutNow(view)
        let elapsed = TimeInterval(DispatchTime.now().uptimeNanoseconds - start.uptimeNanoseconds) / 1_000_000_000

        XCTAssertEqual(view.layoutCount, baseline + 1, "レイアウトが走っていない")
        // 閾値は、レイアウト実行の中へ時間待機が紛れ込んだら落ちる大きさに取る。
        // レイアウト 1 周は 1 ミリ秒に満たない一方、待機は最短でも数十ミリ秒を消費する。
        XCTAssertLessThan(elapsed, 0.02, "時間待機が発生している")
    }
}
#endif
