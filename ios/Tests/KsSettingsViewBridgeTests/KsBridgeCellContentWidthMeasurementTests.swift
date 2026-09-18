// KsBridgeCellContentWidthMeasurementTests.swift
// KsSettingsViewBridgeTests
//
// 行の内容として埋め込んだ view が、最初の計測から行の幅で測られることを実描画で確認する。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

/// 幅によって高さが変わる観測用の内容 view。
///
/// 折り返す内容を持つ view の計測契約を再現する。幅が提示されなければ折り返さない 1 行ぶんの
/// 高さを、幅が提示されれば折り返した後の高さを答える。どの幅で問われたかを順に控える。
private final class WrappingContentView: UIView {

    /// 幅が提示されないとき (折り返さないとき) の高さ。
    static let unwrappedHeight: CGFloat = 40

    /// 幅が提示されたとき (折り返した後) の高さ。
    static let wrappedHeight: CGFloat = 180

    /// 有限の幅で高さを問われたときの幅を、問われた順に控えたもの。
    private(set) var queriedWidths: [CGFloat] = []

    /// 有限の幅で高さを問われたときに答えた高さを、答えた順に控えたもの。
    private(set) var answeredHeights: [CGFloat] = []

    override var intrinsicContentSize: CGSize {
        let width = bounds.width > 0 ? bounds.width : CGFloat.infinity
        return CGSize(width: UIView.noIntrinsicMetric, height: Self.height(forWidth: width))
    }

    override func sizeThatFits(_ size: CGSize) -> CGSize {
        let height = Self.height(forWidth: size.width)
        if size.width.isFinite && size.width > 0 {
            queriedWidths.append(size.width)
            answeredHeights.append(height)
        }
        return CGSize(width: size.width, height: height)
    }

    /// 折り返しの有無で決まる高さ。
    /// - Parameter width: 提示された幅 (無限大・0 は「提示なし」として扱う)
    /// - Returns: 高さ
    static func height(forWidth width: CGFloat) -> CGFloat {
        (width.isFinite && width > 0) ? wrappedHeight : unwrappedHeight
    }
}

/// 高さを答えない観測用の内容 view。
private final class UnsizedContentView: UIView {

    override func sizeThatFits(_ size: CGSize) -> CGSize {
        CGSize(width: size.width, height: 0)
    }
}

@MainActor
final class KsBridgeCellContentWidthMeasurementTests: XCTestCase {

    /// 内容の view を 1 つ持つ Host を組み立てて window に載せる。
    private func attach(content: UIView) -> KsBridgeTestHost.Attachment {
        let dto = KsBridgeCustomCell(title: "")
        dto.view = content
        dto.contentToken = "token-1"
        return KsBridgeTestHost.attach(KsBridgeFixture.withCells([dto]))
    }

    private func rowHeight(_ attachment: KsBridgeTestHost.Attachment) -> CGFloat? {
        attachment.collectionView.cellForItem(at: IndexPath(item: 0, section: 0))?.frame.height
    }

    func test_内容は最初の計測から行の幅で測られる() {
        let content = WrappingContentView()

        let attachment = attach(content: content)

        guard let firstWidth = content.queriedWidths.first else {
            XCTFail("内容が幅付きで一度も測られていない (幅なしの答えで行の高さが決まっている)")
            return
        }
        let rowWidth = attachment.collectionView.bounds.width
        XCTAssertEqual(
            firstWidth,
            rowWidth,
            accuracy: 0.5,
            "最初の計測に渡された幅が行の幅と違う (firstWidth=\(firstWidth) rowWidth=\(rowWidth))"
        )
    }

    /// 行の高さが後から変わらないことを、内容が答えた高さの並びで固定する。
    ///
    /// 行の高さの最初の答えが定常値と違うと、行は一度その高さで作られてから測り直され、表示中の
    /// 行では高さの変化がアニメーションになる。答えの並びが最初から定常値だけであれば、測り直しも
    /// 高さの変化も起きない。
    func test_内容が答える高さは最初から折り返した後の高さで変わらない() {
        let content = WrappingContentView()

        _ = attach(content: content)

        guard let first = content.answeredHeights.first else {
            XCTFail("内容が幅付きで一度も測られていない (幅なしの答えで行の高さが決まっている)")
            return
        }
        XCTAssertEqual(
            first,
            WrappingContentView.wrappedHeight,
            accuracy: 0.5,
            "最初に答えた高さが折り返した後の高さと違う (first=\(first))"
        )
        XCTAssertEqual(
            Set(content.answeredHeights),
            [WrappingContentView.wrappedHeight],
            "答えた高さが途中で変わっている (行の高さが後から測り直される) heights=\(content.answeredHeights)"
        )
    }

    /// 収束後の見張り。
    ///
    /// 落ち着いた後の行の高さを見るだけなので、最初の答えが定常値かどうかは判定できない
    /// (行の高さは 1 レイアウトパス遅れて追いつくため、最初の答えが違っていても収束値は同じになる)。
    /// 修正の芯の固定は上の `test_内容が答える高さは最初から折り返した後の高さで変わらない` が持つ。
    func test_収束後の行の高さが折り返した後の高さになる() {
        let content = WrappingContentView()

        let attachment = attach(content: content)

        guard let height = rowHeight(attachment) else {
            XCTFail("行が取得できない")
            return
        }
        XCTAssertEqual(
            height,
            WrappingContentView.wrappedHeight,
            accuracy: 1,
            "行の高さが折り返した後の高さになっていない (height=\(height))"
        )
    }

    func test_高さを答えない内容の行は既定の行の高さで作られる() {
        let content = UnsizedContentView()

        let attachment = attach(content: content)

        guard let height = rowHeight(attachment) else {
            XCTFail("行が取得できない")
            return
        }
        XCTAssertGreaterThan(
            height,
            0,
            "高さを答えない内容で行が潰れている (既定の測り方に任せられていない)"
        )
    }
}
#endif
