// KsBridgeInteractionDelegateTests.swift
// KsSettingsViewBridgeTests
//
// Native Cell のコールバックが interaction delegate へ転送されることを検証する。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

/// 受け取った通知を発生順に記録する delegate 実装。
private final class RecordingInteractionDelegate: NSObject, KsBridgeInteractionDelegate {

    /// 受け取った通知を "メソッド名(引数)" の形で記録する。
    private(set) var notifications: [String] = []

    func commandCellTapped(cellID: String) {
        notifications.append("commandCellTapped(\(cellID))")
    }

    func buttonCellTapped(cellID: String) {
        notifications.append("buttonCellTapped(\(cellID))")
    }

    func customCellTapped(cellID: String) {
        notifications.append("customCellTapped(\(cellID))")
    }

    func switchCellChanged(cellID: String, isOn: Bool) {
        notifications.append("switchCellChanged(\(cellID),\(isOn))")
    }

    func checkboxCellChanged(cellID: String, isChecked: Bool) {
        notifications.append("checkboxCellChanged(\(cellID),\(isChecked))")
    }

    func simpleCheckCellChanged(cellID: String, isChecked: Bool) {
        notifications.append("simpleCheckCellChanged(\(cellID),\(isChecked))")
    }

    func radioCellSelected(cellID: String, value: String) {
        notifications.append("radioCellSelected(\(cellID),\(value))")
    }

    func entryCellTextChanged(cellID: String, text: String) {
        notifications.append("entryCellTextChanged(\(cellID),\(text))")
    }

    func pickerCellSelectionChanged(cellID: String, index: Int) {
        notifications.append("pickerCellSelectionChanged(\(cellID),\(index))")
    }

    func pickerCellMultiSelectionChanged(cellID: String, indices: [Int]) {
        notifications.append("pickerCellMultiSelectionChanged(\(cellID),\(indices))")
    }

    func numberPickerCellChanged(cellID: String, value: Int) {
        notifications.append("numberPickerCellChanged(\(cellID),\(value))")
    }

    func timePickerCellChanged(cellID: String, time: String) {
        notifications.append("timePickerCellChanged(\(cellID),\(time))")
    }

    func datePickerCellChanged(cellID: String, date: String) {
        notifications.append("datePickerCellChanged(\(cellID),\(date))")
    }
}

@MainActor
final class KsBridgeInteractionDelegateTests: XCTestCase {

    /// 壁時計の時刻・日付から `Date` を作る。
    private func makeDate(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0) -> Date {
        var components = DateComponents()
        components.year = year
        components.month = month
        components.day = day
        components.hour = hour
        components.minute = minute
        return Calendar.current.date(from: components) ?? Date(timeIntervalSince1970: 0)
    }

    // MARK: - Cell 種別ごとの通知

    func test_タップと二値変更が対応するメソッドで通知される() {
        let command = KsBridgeCommandCell(title: "コマンド")
        let button = KsBridgeButtonCell(title: "ボタン")
        let toggle = KsBridgeSwitchCell(title: "スイッチ")
        let checkbox = KsBridgeCheckboxCell(title: "チェック")
        let simpleCheck = KsBridgeSimpleCheckCell(title: "シンプルチェック")
        let bridge = KsBridgeFixture.withCells([command, button, toggle, checkbox, simpleCheck])
        let recorder = RecordingInteractionDelegate()
        bridge.interactionDelegate = recorder

        let cells = bridge.store.root.sections.first?.cells ?? []
        (cells[0] as? CommandCell)?.onTap?()
        (cells[1] as? ButtonCell)?.onTap?()
        (cells[2] as? SwitchCell)?.onValueChanged?(true)
        (cells[3] as? CheckboxCell)?.onValueChanged?(true)
        (cells[4] as? SimpleCheckCell)?.onValueChanged?(false)

        XCTAssertEqual(recorder.notifications, [
            "commandCellTapped(\(command.cellID))",
            "buttonCellTapped(\(button.cellID))",
            "switchCellChanged(\(toggle.cellID),true)",
            "checkboxCellChanged(\(checkbox.cellID),true)",
            "simpleCheckCellChanged(\(simpleCheck.cellID),false)",
        ])
    }

    func test_radio選択とentry変更が対応するメソッドで通知される() {
        let radio = KsBridgeRadioCell(title: "ラジオ")
        radio.groupID = "group"
        radio.value = "A"
        let entry = KsBridgeEntryCell(title: "入力")
        let bridge = KsBridgeFixture.withCells([radio, entry])
        let recorder = RecordingInteractionDelegate()
        bridge.interactionDelegate = recorder

        let cells = bridge.store.root.sections.first?.cells ?? []
        (cells[0] as? RadioCell)?.onSelected?("A")
        (cells[1] as? EntryCell)?.onTextChanged?("abc")

        XCTAssertEqual(recorder.notifications, [
            "radioCellSelected(\(radio.cellID),A)",
            "entryCellTextChanged(\(entry.cellID),abc)",
        ])
    }

    func test_picker選択と数値変更が対応するメソッドで通知される() {
        let single = KsBridgePickerCell(title: "単一選択")
        single.items = ["A", "B", "C"].map { KsBridgePickerItem(text: $0) }
        let multiple = KsBridgePickerCell(title: "複数選択")
        multiple.items = ["A", "B", "C"].map { KsBridgePickerItem(text: $0) }
        multiple.selectionMode = 1
        let number = KsBridgeNumberPickerCell(title: "数値")
        let bridge = KsBridgeFixture.withCells([single, multiple, number])
        let recorder = RecordingInteractionDelegate()
        bridge.interactionDelegate = recorder

        let cells = bridge.store.root.sections.first?.cells ?? []
        (cells[0] as? PickerCell)?.onSelectionChanged?(1)
        (cells[1] as? PickerCell)?.onMultiSelectionChanged?([2, 0])
        (cells[2] as? NumberPickerCell)?.onValueChanged?(42)

        XCTAssertEqual(recorder.notifications, [
            "pickerCellSelectionChanged(\(single.cellID),1)",
            "pickerCellMultiSelectionChanged(\(multiple.cellID),[0, 2])",
            "numberPickerCellChanged(\(number.cellID),42)",
        ])
    }

    func test_時刻と日付の変更がISO文字列で通知される() {
        let time = KsBridgeTimePickerCell(title: "時刻")
        let date = KsBridgeDatePickerCell(title: "日付")
        let bridge = KsBridgeFixture.withCells([time, date])
        let recorder = RecordingInteractionDelegate()
        bridge.interactionDelegate = recorder

        let cells = bridge.store.root.sections.first?.cells ?? []
        (cells[0] as? TimePickerCell)?.onValueChanged?(makeDate(year: 2026, month: 8, day: 10, hour: 9, minute: 5))
        (cells[1] as? DatePickerCell)?.onValueChanged?(makeDate(year: 2026, month: 8, day: 10))

        XCTAssertEqual(recorder.notifications, [
            "timePickerCellChanged(\(time.cellID),09:05)",
            "datePickerCellChanged(\(date.cellID),2026-08-10)",
        ])
    }

    // MARK: - 通知先の設定と解除

    func test_delegate未設定の操作は破棄される() {
        let toggle = KsBridgeSwitchCell(title: "スイッチ")
        let bridge = KsBridgeFixture.withCells([toggle])

        let cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertNotNil(cell?.onValueChanged)
        cell?.onValueChanged?(true)
    }

    func test_delegate解除後の操作は通知されない() {
        let toggle = KsBridgeSwitchCell(title: "スイッチ")
        let bridge = KsBridgeFixture.withCells([toggle])
        let recorder = RecordingInteractionDelegate()
        bridge.interactionDelegate = recorder

        let cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        cell?.onValueChanged?(true)
        bridge.interactionDelegate = nil
        cell?.onValueChanged?(false)

        XCTAssertEqual(recorder.notifications, ["switchCellChanged(\(toggle.cellID),true)"])
    }

    func test_dispose後の操作は通知されない() {
        let toggle = KsBridgeSwitchCell(title: "スイッチ")
        let bridge = KsBridgeFixture.withCells([toggle])
        let recorder = RecordingInteractionDelegate()
        bridge.interactionDelegate = recorder

        let cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        bridge.dispose()
        cell?.onValueChanged?(true)

        XCTAssertEqual(recorder.notifications, [])
        XCTAssertNil(bridge.interactionDelegate)
    }

    func test_delegateは弱参照で保持される() {
        let bridge = KsBridgeFixture.withCells([KsBridgeSwitchCell(title: "スイッチ")])

        autoreleasepool {
            let recorder = RecordingInteractionDelegate()
            bridge.interactionDelegate = recorder
            XCTAssertNotNil(bridge.interactionDelegate)
        }

        XCTAssertNil(bridge.interactionDelegate, "Cell のコールバック閉包は delegate を掴まない")
    }

    func test_delegate設定後に構築したCellにも通知が届く() {
        let bridge = KsSettingsBridge()
        let recorder = RecordingInteractionDelegate()
        bridge.interactionDelegate = recorder

        let builder = KsBridgeRootBuilder()
        let section = builder.addSection(headerText: "S", footerText: nil)
        let toggle = KsBridgeSwitchCell(title: "スイッチ")
        section.addCell(toggle)
        bridge.setRoot(builder)

        let cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        cell?.onValueChanged?(true)

        XCTAssertEqual(recorder.notifications, ["switchCellChanged(\(toggle.cellID),true)"])
    }

    func test_insertCellとreplaceCellで作った行にも通知が届く() {
        let bridge = KsBridgeFixture.withCells([KsBridgeLabelCell(title: "ラベル")])
        let recorder = RecordingInteractionDelegate()
        bridge.interactionDelegate = recorder
        let sectionID = bridge.store.root.sections[0].id.uuidString

        let inserted = KsBridgeCheckboxCell(title: "追加チェック")
        let insertedID = bridge.insertCell(inserted, sectionID: sectionID, at: 1)
        let replacement = KsBridgeCommandCell(title: "差し替えコマンド")
        let replacedID = bridge.replaceCell(cellID: bridge.store.root.sections[0].cells[0].id.uuidString,
                                            newCell: replacement)

        let cells = bridge.store.root.sections.first?.cells ?? []
        (cells[0] as? CommandCell)?.onTap?()
        (cells[1] as? CheckboxCell)?.onValueChanged?(true)

        XCTAssertEqual(recorder.notifications, [
            "commandCellTapped(\(replacedID ?? ""))",
            "checkboxCellChanged(\(insertedID ?? ""),true)",
        ])
    }
}
#endif
