// KsBridgeThemeTests.swift
// KsSettingsViewBridgeTests
//
// 輸送 DTO の Theme が Store の `applyTheme` へ変換されることを検証する。

#if canImport(UIKit)
import XCTest
import UIKit
import Combine
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class KsBridgeThemeTests: XCTestCase {

    /// 不透明な緑 (ARGB) を表す輸送値。
    private static let opaqueGreen = NSNumber(value: Int32(bitPattern: 0xFF00FF00))

    /// modern を表す見た目スタイルの序数。
    private static let modernOrdinal = 1

    /// 輸送 DTO の各項目が Native の Theme へ 1:1 で写される。
    func test_setTheme_の輸送値がThemeへ変換される() {
        let fixture = KsBridgeFixture.standard()

        let theme = KsBridgeTheme()
        theme.backgroundColor = Self.opaqueGreen
        theme.cellTitleColor = NSNumber(value: Int32(bitPattern: 0xFFFF0000))
        theme.rowHeight = NSNumber(value: 56)
        theme.hasUnevenRows = NSNumber(value: false)
        theme.cellTitleFont = KsBridgeFont(familyName: nil, pointSize: 21, isBold: true, isItalic: false)
        theme.cellIconSize = NSNumber(value: 32.0)
        fixture.bridge.setTheme(theme)

        let applied = fixture.bridge.store.theme
        XCTAssertEqual(applied.backgroundColor, UIColor(red: 0, green: 1, blue: 0, alpha: 1))
        XCTAssertEqual(applied.cellTitleColor, UIColor(red: 1, green: 0, blue: 0, alpha: 1))
        XCTAssertEqual(applied.rowHeight, 56)
        XCTAssertFalse(applied.hasUnevenRows)
        XCTAssertEqual(applied.cellTitleFont?.pointSize, 21)
        XCTAssertEqual(applied.cellIconSize, 32.0)
    }

    /// placeholder 既定色の輸送値が Native の `Theme.cellPlaceholderColor` へ写される。
    func test_setTheme_のcellPlaceholderColorがThemeへ変換される() {
        let fixture = KsBridgeFixture.standard()

        let theme = KsBridgeTheme()
        theme.cellPlaceholderColor = Self.opaqueGreen
        fixture.bridge.setTheme(theme)

        XCTAssertEqual(
            fixture.bridge.store.theme.cellPlaceholderColor,
            UIColor(red: 0, green: 1, blue: 0, alpha: 1)
        )
    }

    /// placeholder 既定色を未指定にした DTO は Native 側でも未指定になる。
    func test_setTheme_のcellPlaceholderColor未指定はTheme側の未指定になる() {
        let fixture = KsBridgeFixture.standard()

        fixture.bridge.setTheme(KsBridgeTheme())

        XCTAssertNil(fixture.bridge.store.theme.cellPlaceholderColor)
    }

    /// 未指定 (nil) の項目は Theme 側の未指定として扱われる。
    func test_setTheme_の未指定項目はTheme側の未指定になる() {
        let fixture = KsBridgeFixture.standard()

        fixture.bridge.setTheme(KsBridgeTheme())

        let applied = fixture.bridge.store.theme
        XCTAssertEqual(applied, Theme(), "全項目未指定の DTO は既定 Theme と等価")
        XCTAssertNil(applied.cellTitleColor)
        XCTAssertNil(applied.cellTitleFont)
    }

    /// Theme 変更で表示属性が再評価され、設定ツリーの構造と identity は変化しない。
    func test_setTheme_で構造とidentityは変化しない() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        var diffs: [SettingsRootDiff] = []
        let subscription = fixture.bridge.store.diffPublisher.sink { diffs.append($0) }
        defer { subscription.cancel() }

        let beforeSections = attachment.controller.internalDataSource?.snapshot().sectionIdentifiers ?? []
        let beforeItems = attachment.controller.internalDataSource?.snapshot().itemIdentifiers ?? []

        let theme = KsBridgeTheme()
        theme.cellTitleColor = Self.opaqueGreen
        fixture.bridge.setTheme(theme)
        KsBridgeTestHost.pump(attachment)

        XCTAssertEqual(diffs.count, 0, "Theme 変更は構造 Diff を発行しない")
        XCTAssertEqual(
            attachment.controller.internalDataSource?.snapshot().sectionIdentifiers ?? [],
            beforeSections
        )
        XCTAssertEqual(
            attachment.controller.internalDataSource?.snapshot().itemIdentifiers ?? [],
            beforeItems
        )
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A", "B"], ["C"]])
        let firstRow = attachment.collectionView.cellForItem(at: IndexPath(item: 0, section: 0))
        XCTAssertEqual((firstRow as? KsListCellBase)?.titleLabel.textColor,
                       UIColor(red: 0, green: 1, blue: 0, alpha: 1),
                       "表示属性が再評価される")
    }

    /// 余白の論理 4 成分は方向対応型へ組み立てられ、残る 3 属性もそのまま写される。
    func test_Section装飾の輸送値がThemeへ変換される() {
        let fixture = KsBridgeFixture.standard()

        let theme = KsBridgeTheme()
        theme.sectionMarginTop = NSNumber(value: 12.0)
        theme.sectionMarginLeading = NSNumber(value: 24.0)
        theme.sectionMarginBottom = NSNumber(value: 4.0)
        theme.sectionMarginTrailing = NSNumber(value: 6.0)
        theme.sectionCornerRadius = NSNumber(value: 18.0)
        theme.sectionBorderWidth = NSNumber(value: 2.0)
        theme.sectionBorderColor = Self.opaqueGreen
        fixture.bridge.setTheme(theme)

        let applied = fixture.bridge.store.theme
        XCTAssertEqual(
            applied.sectionMargin,
            NSDirectionalEdgeInsets(top: 12, leading: 24, bottom: 4, trailing: 6)
        )
        XCTAssertEqual(applied.sectionCornerRadius, 18)
        XCTAssertEqual(applied.sectionBorderWidth, 2)
        XCTAssertEqual(applied.sectionBorderColor, UIColor(red: 0, green: 1, blue: 0, alpha: 1))
    }

    /// Section 装飾の未指定 (nil) は Theme 側の未指定として扱われる。
    func test_Section装飾の未指定項目はTheme側の未指定になる() {
        let fixture = KsBridgeFixture.standard()

        fixture.bridge.setTheme(KsBridgeTheme())

        let applied = fixture.bridge.store.theme
        XCTAssertNil(applied.sectionMargin)
        XCTAssertNil(applied.sectionCornerRadius)
        XCTAssertNil(applied.sectionBorderWidth)
        XCTAssertNil(applied.sectionBorderColor)
    }

    /// 余白の 4 成分は全体で 1 つの指定であり、1 つでも欠けると余白全体が未指定になる。
    func test_部分nilのmarginは全体が未指定になる() {
        let fixture = KsBridgeFixture.standard()

        let theme = KsBridgeTheme()
        theme.sectionMarginTop = NSNumber(value: 12.0)
        theme.sectionMarginLeading = NSNumber(value: 24.0)
        theme.sectionMarginBottom = NSNumber(value: 4.0)
        theme.sectionMarginTrailing = nil
        theme.sectionCornerRadius = NSNumber(value: 18.0)
        fixture.bridge.setTheme(theme)

        let applied = fixture.bridge.store.theme
        XCTAssertNil(applied.sectionMargin, "trailing が欠けたら余白全体が未指定")
        XCTAssertEqual(applied.sectionCornerRadius, 18, "他の属性は影響を受けない")
    }

    /// 負値・非有限の装飾値は検証されず、そのままの値で Theme へ届く。
    func test_負値と非有限のSection装飾は素通しされる() {
        let fixture = KsBridgeFixture.standard()

        let theme = KsBridgeTheme()
        theme.sectionMarginTop = NSNumber(value: Double.nan)
        theme.sectionMarginLeading = NSNumber(value: -8.0)
        theme.sectionMarginBottom = NSNumber(value: Double.infinity)
        theme.sectionMarginTrailing = NSNumber(value: -Double.infinity)
        theme.sectionCornerRadius = NSNumber(value: -4.0)
        theme.sectionBorderWidth = NSNumber(value: Double.nan)
        fixture.bridge.setTheme(theme)

        let applied = fixture.bridge.store.theme
        guard let margin = applied.sectionMargin else {
            return XCTFail("sectionMargin が解決されていない")
        }
        XCTAssertTrue(margin.top.isNaN, "NaN の上成分がそのまま届く")
        XCTAssertEqual(margin.leading, -8)
        XCTAssertEqual(margin.bottom, .infinity)
        XCTAssertEqual(margin.trailing, -.infinity)
        XCTAssertEqual(applied.sectionCornerRadius, -4)
        XCTAssertTrue(applied.sectionBorderWidth?.isNaN == true, "NaN のボーダー幅がそのまま届く")
    }

    /// 負値・非有限の装飾値を持つ Theme でも、描画は例外なく 0 として行われる。
    func test_負値と非有限のSection装飾でも描画時に0へ正規化される() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)
        fixture.bridge.setStyle(Self.modernOrdinal)
        KsBridgeTestHost.pump(attachment)

        let theme = KsBridgeTheme()
        theme.sectionMarginTop = NSNumber(value: Double.nan)
        theme.sectionMarginLeading = NSNumber(value: -8.0)
        theme.sectionMarginBottom = NSNumber(value: Double.infinity)
        theme.sectionMarginTrailing = NSNumber(value: -Double.infinity)
        theme.sectionCornerRadius = NSNumber(value: Double.nan)
        theme.sectionBorderWidth = NSNumber(value: -2.0)
        fixture.bridge.setTheme(theme)
        KsBridgeTestHost.pump(attachment)

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A", "B"], ["C"]])
        let firstRow = attachment.collectionView.layoutAttributesForItem(
            at: IndexPath(item: 0, section: 0)
        )
        XCTAssertEqual(firstRow?.frame.minX, 0, "不正な余白は 0 として描画される")
    }

    /// 同値の Theme を再指定しても Theme 更新は通知されない。
    func test_同値ThemeでのsetTheme再呼び出しは通知されない() {
        let fixture = KsBridgeFixture.standard()

        var notifications = 0
        let subscription = fixture.bridge.store.$theme.dropFirst().sink { _ in notifications += 1 }
        defer { subscription.cancel() }

        let first = KsBridgeTheme()
        first.cellTitleColor = Self.opaqueGreen
        fixture.bridge.setTheme(first)
        XCTAssertEqual(notifications, 1)

        let same = KsBridgeTheme()
        same.cellTitleColor = Self.opaqueGreen
        fixture.bridge.setTheme(same)

        XCTAssertEqual(notifications, 1, "同値 Theme は再通知されない")
    }
}
#endif
