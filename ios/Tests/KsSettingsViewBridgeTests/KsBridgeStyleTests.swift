// KsBridgeStyleTests.swift
// KsSettingsViewBridgeTests
//
// 輸送された序数が Native の見た目スタイルへ変換され、Host の世代をまたいで保たれることを検証する。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

/// 表示への反映は、指定した Section 余白が Cell 行の水平位置へ現れるかで観察する
/// (modern は箱の水平余白を入れ、classic は Section 境界を全幅に保つ)。
@MainActor
final class KsBridgeStyleTests: XCTestCase {

    /// 検証で指定する Section の水平余白 (pt)。
    private static let marginPoints: CGFloat = 20

    /// 序数 0 / 1 は classic / modern に対応する。
    func test_序数がstyleへ変換される() {
        XCTAssertEqual(KsBridgeStyle.style(from: 0), .classic)
        XCTAssertEqual(KsBridgeStyle.style(from: 1), .modern)
    }

    /// 定義域外の序数は classic へ正規化される。
    func test_定義域外の序数はclassicへ正規化される() {
        XCTAssertEqual(KsBridgeStyle.style(from: 2), .classic)
        XCTAssertEqual(KsBridgeStyle.style(from: -1), .classic)
        XCTAssertEqual(KsBridgeStyle.style(from: Int.max), .classic)
    }

    /// setStyle は生きている Host の style へ即座に適用され、箱の余白が表示に現れる。
    func test_setStyleが表示中のHostへ適用される() {
        let fixture = KsBridgeFixture.standard()
        fixture.bridge.setTheme(Self.marginTheme())
        let attachment = KsBridgeTestHost.attach(fixture.bridge)
        XCTAssertEqual(attachment.controller.style, .classic)
        XCTAssertEqual(Self.firstRowLeading(attachment), 0, "classic では Section が全幅で並ぶ")

        fixture.bridge.setStyle(1)
        Self.awaitFirstRowLeading(attachment, equals: Self.marginPoints)

        XCTAssertEqual(attachment.controller.style, .modern)
        XCTAssertEqual(
            Self.firstRowLeading(attachment),
            Self.marginPoints,
            "modern では指定した水平余白の分だけ行が内側へ寄る"
        )
    }

    /// classic へ戻す方向の切替も同じ経路で適用される。
    func test_setStyleでclassicへ戻せる() {
        let fixture = KsBridgeFixture.standard()
        fixture.bridge.setTheme(Self.marginTheme())
        let attachment = KsBridgeTestHost.attach(fixture.bridge)
        fixture.bridge.setStyle(1)
        Self.awaitFirstRowLeading(attachment, equals: Self.marginPoints)

        fixture.bridge.setStyle(0)
        Self.awaitFirstRowLeading(attachment, equals: 0)

        XCTAssertEqual(attachment.controller.style, .classic)
        XCTAssertEqual(Self.firstRowLeading(attachment), 0)
    }

    /// Host 未生成のときに受けた style は、生成した Host へ適用される。
    func test_Host生成前のsetStyleが生成時に適用される() {
        let fixture = KsBridgeFixture.standard()
        fixture.bridge.setTheme(Self.marginTheme())

        fixture.bridge.setStyle(1)
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        XCTAssertEqual(attachment.controller.style, .modern)
        XCTAssertEqual(Self.firstRowLeading(attachment), Self.marginPoints)
    }

    /// Host を解放して作り直しても style は失われない。
    func test_Host再生成をまたいでstyleが維持される() {
        let fixture = KsBridgeFixture.standard()
        fixture.bridge.setTheme(Self.marginTheme())
        let first = KsBridgeTestHost.attach(fixture.bridge)
        fixture.bridge.setStyle(1)
        Self.awaitFirstRowLeading(first, equals: Self.marginPoints)

        fixture.bridge.releaseHost()
        let second = KsBridgeTestHost.attach(fixture.bridge)

        XCTAssertFalse(first.controller === second.controller, "解放後は新しい Host が返る")
        XCTAssertEqual(second.controller.style, .modern)
        XCTAssertEqual(Self.firstRowLeading(second), Self.marginPoints)
    }

    /// 定義域外の序数を受けた Host は classic で表示される。
    func test_定義域外の序数を受けたHostはclassicで表示される() {
        let fixture = KsBridgeFixture.standard()
        fixture.bridge.setTheme(Self.marginTheme())
        let attachment = KsBridgeTestHost.attach(fixture.bridge)
        fixture.bridge.setStyle(1)
        Self.awaitFirstRowLeading(attachment, equals: Self.marginPoints)

        fixture.bridge.setStyle(7)
        Self.awaitFirstRowLeading(attachment, equals: 0)

        XCTAssertEqual(attachment.controller.style, .classic)
        XCTAssertEqual(Self.firstRowLeading(attachment), 0)
    }

    /// 水平余白だけを明示した Theme。上下は 0 に固定して行の水平位置だけを観察する。
    private static func marginTheme() -> KsBridgeTheme {
        let theme = KsBridgeTheme()
        theme.sectionMarginTop = NSNumber(value: 0.0)
        theme.sectionMarginLeading = NSNumber(value: Double(marginPoints))
        theme.sectionMarginBottom = NSNumber(value: 0.0)
        theme.sectionMarginTrailing = NSNumber(value: Double(marginPoints))
        return theme
    }

    /// 先頭行の水平位置が期待値になるまで待つ。
    ///
    /// style の切替は `applyFullSnapshot` / `reconfigureVisibleCells` を通って layout へ届くため、
    /// 行の水平位置が切替後の値になったことを完了条件とする。
    private static func awaitFirstRowLeading(
        _ attachment: KsBridgeTestHost.Attachment,
        equals expected: CGFloat,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        awaitEqual(
            "先頭行の水平位置",
            expected: expected,
            in: attachment.collectionView,
            file: file,
            line: line,
            actual: { firstRowLeading(attachment) }
        )
    }

    /// 先頭 Section の先頭行が実描画された水平位置 (leading) を返す。
    private static func firstRowLeading(_ attachment: KsBridgeTestHost.Attachment) -> CGFloat {
        let indexPath = IndexPath(item: 0, section: 0)
        guard let attributes = attachment.collectionView
            .layoutAttributesForItem(at: indexPath) else {
            return -1
        }
        return attributes.frame.minX
    }
}
#endif
