// KsBridgeSectionHeaderHeightTests.swift
// KsSettingsViewBridgeTests
//
// Section のヘッダ高さの輸送を検証する。

#if canImport(UIKit)
import XCTest
@testable import KsSettingsViewBridge
@testable import KsSettingsViewCore

@MainActor
final class KsBridgeSectionHeaderHeightTests: XCTestCase {

    /// Native の `Section` が既定とする自動高さ。
    private var automaticHeaderHeight: Double {
        return KsSettingsViewCore.Section(id: UUID()).headerHeight
    }

    func test_Section_DTOのheaderHeight既定はnil() {
        let section = KsBridgeSection(headerText: "S", footerText: nil)

        XCTAssertNil(section.headerHeight)
    }

    func test_headerHeight未指定のSectionはNative既定の自動高さになる() {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        let section = builder.addSection(headerText: "S", footerText: nil)
        section.addCell(KsBridgeLabelCell(title: "A"))
        bridge.setRoot(builder)

        XCTAssertEqual(bridge.store.root.sections[0].headerHeight, automaticHeaderHeight)
    }

    func test_headerHeightを指定するとNativeのSectionへ適用される() {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        let section = builder.addSection(headerText: "S", footerText: nil)
        section.headerHeight = NSNumber(value: 60.0)
        section.addCell(KsBridgeLabelCell(title: "A"))
        bridge.setRoot(builder)

        XCTAssertEqual(bridge.store.root.sections[0].headerHeight, 60.0)
    }

    func test_insertSectionでもheaderHeightが輸送される() {
        let bridge = KsSettingsBridge()
        bridge.setRoot(KsBridgeRootBuilder())
        let inserted = KsBridgeSection(headerText: "S", footerText: nil)
        inserted.headerHeight = NSNumber(value: 44.0)

        bridge.insertSection(inserted, at: 0)

        XCTAssertEqual(bridge.store.root.sections[0].headerHeight, 44.0)
    }

    func test_replaceSectionでheaderHeightを差し替えられる() {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        let section = builder.addSection(headerText: "S", footerText: nil)
        section.headerHeight = NSNumber(value: 60.0)
        bridge.setRoot(builder)
        let sectionID = bridge.store.root.sections[0].id.uuidString

        let replacement = KsBridgeSection(headerText: "S", footerText: nil)
        replacement.headerHeight = NSNumber(value: 80.0)
        bridge.replaceSection(sectionID: sectionID, newSection: replacement)

        XCTAssertEqual(bridge.store.root.sections[0].headerHeight, 80.0)
    }

    func test_replaceSectionでheaderHeightをNative既定へ戻せる() {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        let section = builder.addSection(headerText: "S", footerText: nil)
        section.headerHeight = NSNumber(value: 60.0)
        bridge.setRoot(builder)
        let sectionID = bridge.store.root.sections[0].id.uuidString

        let replacement = KsBridgeSection(headerText: "S", footerText: nil)
        bridge.replaceSection(sectionID: sectionID, newSection: replacement)

        XCTAssertEqual(bridge.store.root.sections[0].headerHeight, automaticHeaderHeight)
    }
}
#endif
