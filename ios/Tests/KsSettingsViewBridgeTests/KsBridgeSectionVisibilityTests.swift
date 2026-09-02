// KsBridgeSectionVisibilityTests.swift
// KsSettingsViewBridgeTests
//
// Section の可視性輸送と、内容差し替え時の cellID 温存を検証する。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

/// 受け取ったスイッチ変更を記録する delegate 実装。
private final class SwitchRecordingDelegate: NSObject, KsBridgeInteractionDelegate {

    /// 通知された (cellID, 新しい値) の並び。
    private(set) var changes: [(cellID: String, isOn: Bool)] = []

    func switchCellChanged(cellID: String, isOn: Bool) {
        changes.append((cellID: cellID, isOn: isOn))
    }

    func commandCellTapped(cellID: String) {}
    func buttonCellTapped(cellID: String) {}
    func customCellTapped(cellID: String) {}
    func checkboxCellChanged(cellID: String, isChecked: Bool) {}
    func simpleCheckCellChanged(cellID: String, isChecked: Bool) {}
    func radioCellSelected(cellID: String, value: String) {}
    func entryCellTextChanged(cellID: String, text: String) {}
    func pickerCellSelectionChanged(cellID: String, index: Int) {}
    func pickerCellMultiSelectionChanged(cellID: String, indices: [Int]) {}
    func numberPickerCellChanged(cellID: String, value: Int) {}
    func timePickerCellChanged(cellID: String, time: String) {}
    func datePickerCellChanged(cellID: String, date: String) {}
}

@MainActor
final class KsBridgeSectionVisibilityTests: XCTestCase {

    // MARK: - isVisible の輸送

    func test_Section_DTOのisVisible既定はtrue() {
        let section = KsBridgeSection(headerText: "S", footerText: nil)

        XCTAssertTrue(section.isVisible)
    }

    func test_非表示SectionをsetRootで輸送すると表示から除外される() {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        let visible = builder.addSection(headerText: "表示", footerText: nil)
        visible.addCell(KsBridgeLabelCell(title: "A"))
        let hidden = builder.addSection(headerText: "非表示", footerText: nil)
        hidden.isVisible = false
        hidden.addCell(KsBridgeLabelCell(title: "B"))
        bridge.setRoot(builder)

        XCTAssertEqual(bridge.store.root.sections.count, 2, "モデルには保持される")
        XCTAssertFalse(bridge.store.root.sections[1].isVisible)

        let attachment = KsBridgeTestHost.attach(bridge)
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A"]])
    }

    func test_replaceSectionでisVisibleを切り替えると表示が追随する() {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        let section = builder.addSection(headerText: "S", footerText: nil)
        section.addCell(KsBridgeLabelCell(title: "A"))
        bridge.setRoot(builder)
        let attachment = KsBridgeTestHost.attach(bridge)
        let sectionID = bridge.store.root.sections[0].id.uuidString

        let hidden = KsBridgeSection(headerText: "S", footerText: nil, cells: [KsBridgeLabelCell(title: "A")])
        hidden.isVisible = false
        bridge.replaceSection(sectionID: sectionID, newSection: hidden)
        KsBridgeTestHost.awaitRenderedTitles(attachment, equals: [])

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [])
    }

    // MARK: - Header / Footer 表示トグルの輸送

    func test_Section_DTOの表示トグル既定はtrue() {
        let section = KsBridgeSection(headerText: "S", footerText: "F")

        XCTAssertTrue(section.isHeaderVisible)
        XCTAssertTrue(section.isFooterVisible)
    }

    func test_トグル未指定のDTOはcoreのSectionへtrueを伝搬する() {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        builder.addSection(headerText: "S", footerText: "F")
        bridge.setRoot(builder)

        XCTAssertTrue(bridge.store.root.sections[0].isHeaderVisible)
        XCTAssertTrue(bridge.store.root.sections[0].isFooterVisible)
    }

    func test_DTOのisHeaderVisibleはcoreのSectionへ伝搬する() {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        let section = builder.addSection(headerText: "S", footerText: "F")
        section.isHeaderVisible = false
        bridge.setRoot(builder)

        XCTAssertFalse(bridge.store.root.sections[0].isHeaderVisible)
        XCTAssertTrue(bridge.store.root.sections[0].isFooterVisible, "footer 側は巻き込まれない")
    }

    func test_DTOのisFooterVisibleはcoreのSectionへ伝搬する() {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        let section = builder.addSection(headerText: "S", footerText: "F")
        section.isFooterVisible = false
        bridge.setRoot(builder)

        XCTAssertFalse(bridge.store.root.sections[0].isFooterVisible)
        XCTAssertTrue(bridge.store.root.sections[0].isHeaderVisible, "header 側は巻き込まれない")
    }

    func test_replaceSectionで表示トグルを切り替えるとHeaderが消える() {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        let section = builder.addSection(headerText: "S", footerText: nil)
        section.addCell(KsBridgeLabelCell(title: "A"))
        bridge.setRoot(builder)
        let attachment = KsBridgeTestHost.attach(bridge)
        let sectionID = bridge.store.root.sections[0].id.uuidString
        XCTAssertEqual(KsBridgeTestHost.headerText(attachment, section: 0), "S")

        let hidden = KsBridgeSection(headerText: "S", footerText: nil, cells: [KsBridgeLabelCell(title: "A")])
        hidden.isHeaderVisible = false
        bridge.replaceSection(sectionID: sectionID, newSection: hidden)
        KsBridgeTestHost.awaitHeaderText(attachment, section: 0, equals: nil)

        XCTAssertNil(KsBridgeTestHost.headerText(attachment, section: 0))
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A"]], "Cell は残る")
    }

    // MARK: - cellID の温存

    func test_adoptCellIDで採番済みIDを引き継げる() {
        let original = KsBridgeLabelCell(title: "A")
        let replacement = KsBridgeLabelCell(title: "A")

        XCTAssertNotEqual(replacement.cellID, original.cellID)
        XCTAssertTrue(replacement.adoptCellID(original.cellID))
        XCTAssertEqual(replacement.cellID, original.cellID)
    }

    func test_adoptCellIDはcanonicalUUIDでない値を無視する() {
        let cell = KsBridgeLabelCell(title: "A")
        let before = cell.cellID

        XCTAssertFalse(cell.adoptCellID(KsBridgeFixture.unknownIdentifier))
        XCTAssertEqual(cell.cellID, before)
    }

    func test_ID引き継ぎ済みDTOでのreplaceSectionは通知IDを温存する() {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        let section = builder.addSection(headerText: "S", footerText: nil)
        let toggle = KsBridgeSwitchCell(title: "スイッチ")
        section.addCell(toggle)
        bridge.setRoot(builder)
        let recorder = SwitchRecordingDelegate()
        bridge.interactionDelegate = recorder
        let sectionID = bridge.store.root.sections[0].id.uuidString

        let replacementToggle = KsBridgeSwitchCell(title: "スイッチ")
        replacementToggle.adoptCellID(toggle.cellID)
        let replacement = KsBridgeSection(headerText: "S 更新", footerText: nil, cells: [replacementToggle])
        bridge.replaceSection(sectionID: sectionID, newSection: replacement)

        let cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        cell?.onValueChanged?(true)

        XCTAssertEqual(bridge.store.root.sections[0].cells[0].id.uuidString, toggle.cellID)
        XCTAssertEqual(recorder.changes.map { $0.cellID }, [toggle.cellID])
        XCTAssertEqual(recorder.changes.map { $0.isOn }, [true])
    }

    func test_ID引き継ぎのないreplaceSectionはcellIDを再採番する() {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        let section = builder.addSection(headerText: "S", footerText: nil)
        let toggle = KsBridgeSwitchCell(title: "スイッチ")
        section.addCell(toggle)
        bridge.setRoot(builder)
        let recorder = SwitchRecordingDelegate()
        bridge.interactionDelegate = recorder
        let sectionID = bridge.store.root.sections[0].id.uuidString

        let replacementToggle = KsBridgeSwitchCell(title: "スイッチ")
        let replacement = KsBridgeSection(headerText: "S", footerText: nil, cells: [replacementToggle])
        bridge.replaceSection(sectionID: sectionID, newSection: replacement)

        let cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        cell?.onValueChanged?(true)

        XCTAssertNotEqual(replacementToggle.cellID, toggle.cellID)
        XCTAssertEqual(recorder.changes.map { $0.cellID }, [replacementToggle.cellID])
    }

    func test_isVisible差し替え後も温存したcellIDで通知される() {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        let section = builder.addSection(headerText: "S", footerText: nil)
        let toggle = KsBridgeSwitchCell(title: "スイッチ")
        section.addCell(toggle)
        bridge.setRoot(builder)
        let recorder = SwitchRecordingDelegate()
        bridge.interactionDelegate = recorder
        let sectionID = bridge.store.root.sections[0].id.uuidString

        // 非表示へ、続けて表示へ戻す (どちらも同じ Section を差し替える経路)
        for visible in [false, true] {
            let cell = KsBridgeSwitchCell(title: "スイッチ")
            cell.adoptCellID(toggle.cellID)
            let replacement = KsBridgeSection(headerText: "S", footerText: nil, cells: [cell])
            replacement.isVisible = visible
            bridge.replaceSection(sectionID: sectionID, newSection: replacement)
        }

        let cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        cell?.onValueChanged?(true)

        XCTAssertTrue(bridge.store.root.sections[0].isVisible)
        XCTAssertEqual(recorder.changes.map { $0.cellID }, [toggle.cellID])
    }
}
#endif
