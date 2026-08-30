// KsBridgeCellConversionTests.swift
// KsSettingsViewBridgeTests
//
// Cell 種ごとの輸送 DTO が対応する Native Cell 型と値へ変換されることを検証する。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class KsBridgeCellConversionTests: XCTestCase {

    /// 不透明な緑 (ARGB) を表す輸送値。
    private static let opaqueGreen = NSNumber(value: Int32(bitPattern: 0xFF00FF00))

    // MARK: - 基本 Cell

    func test_LabelCellDTOがLabelCellへ変換される() {
        let dto = KsBridgeLabelCell(
            title: "ラベル",
            descriptionText: "説明",
            valueText: "値",
            hintText: "ヒント",
            isEnabled: false,
            isVisible: false
        )
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: LabelCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.title, "ラベル")
        XCTAssertEqual(cell?.description, "説明")
        XCTAssertEqual(cell?.valueText, "値")
        XCTAssertEqual(cell?.hintText, "ヒント")
        XCTAssertEqual(cell?.isEnabled, false)
        XCTAssertEqual(cell?.isVisible, false)
    }

    func test_CommandCellDTOがCommandCellへ変換される() {
        let dto = KsBridgeCommandCell(title: "コマンド")
        dto.valueText = "値"
        dto.hideArrow = true
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: CommandCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.title, "コマンド")
        XCTAssertEqual(cell?.valueText, "値")
        XCTAssertEqual(cell?.hideArrow, true)
        XCTAssertNotNil(cell?.onTap, "タップ通知のコールバックが注入される")
    }

    func test_ButtonCellDTOがButtonCellへ変換される() {
        let dto = KsBridgeButtonCell(title: "ボタン")
        dto.titleColor = Self.opaqueGreen
        dto.titleAlignment = NSNumber(value: 0)
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: ButtonCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.title, "ボタン")
        XCTAssertEqual(cell?.titleColor, UIColor(red: 0, green: 1, blue: 0, alpha: 1))
        XCTAssertEqual(cell?.titleAlignment, .start)
        XCTAssertNotNil(cell?.onTap)
    }

    func test_ButtonCellDTOのtitleAlignment未指定はNative既定になる() {
        let bridge = KsBridgeFixture.withCells([KsBridgeButtonCell(title: "ボタン")])

        let cell: ButtonCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.titleAlignment, .center)
    }

    func test_SwitchCellDTOがSwitchCellへ変換される() {
        let dto = KsBridgeSwitchCell(title: "スイッチ")
        dto.isOn = true
        dto.accentColor = Self.opaqueGreen
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.title, "スイッチ")
        XCTAssertEqual(cell?.isOn, true)
        XCTAssertEqual(cell?.accentColor, UIColor(red: 0, green: 1, blue: 0, alpha: 1))
        XCTAssertNotNil(cell?.onValueChanged)
    }

    func test_CheckboxCellDTOがCheckboxCellへ変換される() {
        let dto = KsBridgeCheckboxCell(title: "チェック")
        dto.isChecked = true
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: CheckboxCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.title, "チェック")
        XCTAssertEqual(cell?.isChecked, true)
        XCTAssertNotNil(cell?.onValueChanged)
    }

    func test_SimpleCheckCellDTOがSimpleCheckCellへ変換される() {
        let dto = KsBridgeSimpleCheckCell(title: "シンプルチェック")
        dto.isChecked = true
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: SimpleCheckCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.title, "シンプルチェック")
        XCTAssertEqual(cell?.isChecked, true)
        XCTAssertNotNil(cell?.onValueChanged)
    }

    func test_RadioCellDTOがRadioCellへ変換される() {
        let dto = KsBridgeRadioCell(title: "ラジオ")
        dto.groupID = "group"
        dto.value = "A"
        dto.selectedValue = "B"
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: RadioCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.groupId, "group")
        XCTAssertEqual(cell?.value, "A")
        XCTAssertEqual(cell?.selectedValue, "B")
        XCTAssertNotNil(cell?.onSelected)
    }

    // MARK: - 入力 Cell

    func test_EntryCellDTOがEntryCellへ変換される() {
        let dto = KsBridgeEntryCell(title: "入力")
        dto.text = "abc"
        dto.placeholder = "入力してください"
        dto.keyboard = 5
        dto.isPassword = true
        dto.textAlignment = NSNumber(value: 1)
        dto.maxLength = NSNumber(value: 10)
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: EntryCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.text, "abc")
        XCTAssertEqual(cell?.placeholder, "入力してください")
        XCTAssertEqual(cell?.keyboardType, .emailAddress)
        XCTAssertEqual(cell?.isPassword, true)
        XCTAssertEqual(cell?.textAlignment, .center)
        XCTAssertEqual(cell?.maxLength, 10)
        XCTAssertNotNil(cell?.onTextChanged)
    }

    func test_EntryCellDTOのtextAlignment未指定はNative既定になる() {
        let bridge = KsBridgeFixture.withCells([KsBridgeEntryCell(title: "入力")])

        let cell: EntryCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.textAlignment, .end)
        XCTAssertNil(cell?.maxLength, "未指定の最大文字数は無制限になる")
    }

    func test_EntryCellDTOのplaceholderColorがNativeの色へ写る() {
        let dto = KsBridgeEntryCell(title: "入力")
        dto.placeholder = "入力してください"
        dto.placeholderColor = Self.opaqueGreen
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: EntryCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.placeholderColor, UIColor(red: 0, green: 1, blue: 0, alpha: 1))
    }

    func test_EntryCellDTOのplaceholderColor未指定はNative側の未指定になる() {
        let bridge = KsBridgeFixture.withCells([KsBridgeEntryCell(title: "入力")])

        let cell: EntryCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertNil(cell?.placeholderColor)
    }

    func test_PickerCellDTOが単一選択のPickerCellへ変換される() {
        let dto = KsBridgePickerCell(title: "選択")
        dto.items = ["A", "B", "C"].map { KsBridgePickerItem(text: $0) }
        dto.selectedIndex = NSNumber(value: 2)
        dto.pageTitle = "選んでください"
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: PickerCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.selectionMode, .single)
        XCTAssertEqual(cell?.items, ["A", "B", "C"].map { PickerItem(text: $0) })
        XCTAssertEqual(cell?.selectedIndex, 2)
        XCTAssertEqual(cell?.pageTitle, "選んでください")
        XCTAssertNotNil(cell?.onSelectionChanged)
    }

    func test_PickerCellDTOが複数選択のPickerCellへ変換される() {
        let dto = KsBridgePickerCell(title: "選択")
        dto.items = ["A", "B", "C"].map { KsBridgePickerItem(text: $0) }
        dto.selectionMode = 1
        dto.selectedIndices = [2, 0]
        dto.maxSelectedNumber = 2
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: PickerCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.selectionMode, .multiple)
        XCTAssertEqual(cell?.selectedIndices, [0, 2])
        XCTAssertEqual(cell?.maxSelectedNumber, 2)
        XCTAssertNotNil(cell?.onMultiSelectionChanged)
    }

    func test_PickerCellDTOの副表示が候補ごとに保存される() {
        let dto = KsBridgePickerCell(title: "選択")
        dto.items = [
            KsBridgePickerItem(text: "A", subText: "補足A"),
            KsBridgePickerItem(text: "B"),
            KsBridgePickerItem(text: "C", subText: ""),
        ]
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: PickerCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.items.map(\.text), ["A", "B", "C"])
        XCTAssertEqual(
            cell?.items.map(\.subText),
            ["補足A", nil, nil],
            "副表示の有無は候補ごとに保たれ、空文字列は「なし」へ揃う"
        )
    }

    func test_NumberPickerCellDTOがNumberPickerCellへ変換される() {
        let dto = KsBridgeNumberPickerCell(title: "数値")
        dto.min = 5
        dto.max = 50
        dto.step = 5
        dto.value = 20
        dto.unit = "px"
        dto.pickerTitle = "数値を選択"
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: NumberPickerCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.min, 5)
        XCTAssertEqual(cell?.max, 50)
        XCTAssertEqual(cell?.step, 5)
        XCTAssertEqual(cell?.value, 20)
        XCTAssertEqual(cell?.unit, "px")
        XCTAssertEqual(cell?.pickerTitle, "数値を選択")
        XCTAssertNotNil(cell?.onValueChanged)
    }

    func test_TimePickerCellDTOがTimePickerCellへ変換される() {
        let dto = KsBridgeTimePickerCell(title: "時刻")
        dto.time = "09:30"
        dto.pickerTitle = "時刻を選択"
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: TimePickerCell? = KsBridgeFixture.storedCell(bridge)
        let components = Calendar.current.dateComponents([.hour, .minute], from: cell?.time ?? Date())
        XCTAssertEqual(components.hour, 9)
        XCTAssertEqual(components.minute, 30)
        XCTAssertEqual(cell?.format, "HH:mm", "未指定の表示フォーマットは Native 既定になる")
        XCTAssertEqual(cell?.is24Hour, true, "未指定の時制は Native 既定になる")
        XCTAssertEqual(cell?.pickerTitle, "時刻を選択")
        XCTAssertNotNil(cell?.onValueChanged)
    }

    func test_TimePickerCellDTOのis24HourがNativeへ写る() {
        let dto = KsBridgeTimePickerCell(title: "時刻")
        dto.time = "09:30"
        dto.is24Hour = NSNumber(value: false)
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: TimePickerCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.is24Hour, false)
    }

    func test_DatePickerCellDTOがDatePickerCellへ変換される() {
        let dto = KsBridgeDatePickerCell(title: "日付")
        dto.date = "2026-08-10"
        dto.minDate = "2026-01-01"
        dto.maxDate = "2026-12-31"
        dto.uiStyle = NSNumber(value: 0)
        dto.todayText = "今日"
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: DatePickerCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(KsBridgeValueTransport.dateText(from: cell?.date ?? Date()), "2026-08-10")
        XCTAssertEqual(cell.flatMap { $0.minDate }.map { KsBridgeValueTransport.dateText(from: $0) }, "2026-01-01")
        XCTAssertEqual(cell.flatMap { $0.maxDate }.map { KsBridgeValueTransport.dateText(from: $0) }, "2026-12-31")
        XCTAssertEqual(cell?.uiStyle, .calendar)
        XCTAssertEqual(cell?.format, "yyyy/MM/dd", "未指定の表示フォーマットは Native 既定になる")
        XCTAssertEqual(cell?.todayText, "今日")
        XCTAssertNotNil(cell?.onValueChanged)
    }

    func test_DatePickerCellDTOのuiStyle未指定はNative既定になる() {
        let dto = KsBridgeDatePickerCell(title: "日付")
        dto.date = "2026-08-10"
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: DatePickerCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.uiStyle, .wheels)
        XCTAssertNil(cell?.minDate)
        XCTAssertNil(cell?.maxDate)
    }

    // MARK: - 共通フィールド (style / icon)

    func test_style輸送値がCellStyleへ変換される() {
        let style = KsBridgeCellStyle()
        style.titleColor = Self.opaqueGreen
        style.iconSize = NSNumber(value: 32.0)
        style.iconRadius = NSNumber(value: 8.0)
        style.cellHeight = NSNumber(value: 60.0)
        style.titleFont = KsBridgeFont(familyName: nil, pointSize: 21, isBold: true, isItalic: false)
        let dto = KsBridgeSwitchCell(title: "スイッチ")
        dto.style = style
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.style.titleColor, UIColor(red: 0, green: 1, blue: 0, alpha: 1))
        XCTAssertEqual(cell?.style.iconSize, 32.0)
        XCTAssertEqual(cell?.style.iconRadius, 8.0)
        XCTAssertEqual(cell?.style.cellHeight, 60.0)
        XCTAssertEqual(cell?.style.titleFont?.pointSize, 21)
    }

    /// style 輸送値の 13 項目が、対応する `CellStyle` の項目へそれぞれ写されることを確認する。
    ///
    /// 全項目に相異なる値を入れて一括で突き合わせ、引数の取り違えを検出できるようにする。
    func test_style輸送値の全13項目が対応するCellStyle項目へ写される() {
        let style = KsBridgeCellStyle()
        style.titleColor = NSNumber(value: Int32(bitPattern: 0xFF01_0203))
        style.titleFont = KsBridgeFont(familyName: nil, pointSize: 11, isBold: false, isItalic: false)
        style.descriptionColor = NSNumber(value: Int32(bitPattern: 0xFF04_0506))
        style.descriptionFont = KsBridgeFont(familyName: nil, pointSize: 12, isBold: false, isItalic: false)
        style.valueTextColor = NSNumber(value: Int32(bitPattern: 0xFF07_0809))
        style.valueTextFont = KsBridgeFont(familyName: nil, pointSize: 13, isBold: false, isItalic: false)
        style.iconSize = NSNumber(value: 24.0)
        style.iconRadius = NSNumber(value: 6.0)
        style.cellHeight = NSNumber(value: 56.0)
        style.hintTextColor = NSNumber(value: Int32(bitPattern: 0xFF0A_0B0C))
        style.hintTextFont = KsBridgeFont(familyName: nil, pointSize: 14, isBold: false, isItalic: false)
        style.backgroundColor = NSNumber(value: Int32(bitPattern: 0xFF0D_0E0F))
        style.accentColor = NSNumber(value: Int32(bitPattern: 0xFF10_1112))
        let dto = KsBridgeSwitchCell(title: "スイッチ")
        dto.style = style
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        let resolved = cell?.style
        XCTAssertEqual(resolved?.titleColor, Self.color(0xFF01_0203))
        XCTAssertEqual(resolved?.titleFont?.pointSize, 11)
        XCTAssertEqual(resolved?.descriptionColor, Self.color(0xFF04_0506))
        XCTAssertEqual(resolved?.descriptionFont?.pointSize, 12)
        XCTAssertEqual(resolved?.valueTextColor, Self.color(0xFF07_0809))
        XCTAssertEqual(resolved?.valueTextFont?.pointSize, 13)
        XCTAssertEqual(resolved?.iconSize, 24.0)
        XCTAssertEqual(resolved?.iconRadius, 6.0)
        XCTAssertEqual(resolved?.cellHeight, 56.0)
        XCTAssertEqual(resolved?.hintTextColor, Self.color(0xFF0A_0B0C))
        XCTAssertEqual(resolved?.hintTextFont?.pointSize, 14)
        XCTAssertEqual(resolved?.backgroundColor, Self.color(0xFF0D_0E0F))
        XCTAssertEqual(resolved?.accentColor, Self.color(0xFF10_1112))
    }

    /// ARGB を詰めた値から期待する `UIColor` を作る。
    private static func color(_ argb: UInt32) -> UIColor {
        UIColor(
            red: CGFloat((argb >> 16) & 0xFF) / 255.0,
            green: CGFloat((argb >> 8) & 0xFF) / 255.0,
            blue: CGFloat(argb & 0xFF) / 255.0,
            alpha: CGFloat((argb >> 24) & 0xFF) / 255.0
        )
    }

    func test_style未指定は全項目未指定のCellStyleになる() {
        let bridge = KsBridgeFixture.withCells([KsBridgeLabelCell(title: "ラベル")])

        let cell: LabelCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.style, CellStyle())
    }

    func test_icon輸送値がKsImageへ包まれる() {
        let image = KsBridgeFixture.image()
        let dto = KsBridgeCommandCell(title: "コマンド")
        dto.icon = image
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: CommandCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.icon, KsImage.uiImage(image))
    }

    func test_icon未指定はnilになる() {
        let bridge = KsBridgeFixture.withCells([KsBridgeCommandCell(title: "コマンド")])

        let cell: CommandCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertNil(cell?.icon)
    }

    // MARK: - 異種 Cell の混載

    func test_異種CellをsetRootで混載できる() {
        let label = KsBridgeLabelCell(title: "ラベル")
        let toggle = KsBridgeSwitchCell(title: "スイッチ")
        let entry = KsBridgeEntryCell(title: "入力")
        let datePicker = KsBridgeDatePickerCell(title: "日付")
        let bridge = KsBridgeFixture.withCells([label, toggle, entry, datePicker])

        let cells = bridge.store.root.sections.first?.cells ?? []
        XCTAssertEqual(cells.count, 4)
        XCTAssertTrue(cells[0] is LabelCell)
        XCTAssertTrue(cells[1] is SwitchCell)
        XCTAssertTrue(cells[2] is EntryCell)
        XCTAssertTrue(cells[3] is DatePickerCell)
    }

    func test_異種CellをreplaceCellsで同一バッチ更新できる() {
        let label = KsBridgeLabelCell(title: "ラベル")
        let toggle = KsBridgeSwitchCell(title: "スイッチ")
        let bridge = KsBridgeFixture.withCells([label, toggle])

        let newToggle = KsBridgeSwitchCell(title: "スイッチ更新")
        newToggle.isOn = true
        let newEntry = KsBridgeEntryCell(title: "入力へ差し替え")
        bridge.replaceCells([
            KsBridgeCellUpdate(cellID: label.cellID, cell: newEntry),
            KsBridgeCellUpdate(cellID: toggle.cellID, cell: newToggle),
        ])

        let cells = bridge.store.root.sections.first?.cells ?? []
        XCTAssertEqual((cells[0] as? EntryCell)?.title, "入力へ差し替え")
        XCTAssertEqual((cells[1] as? SwitchCell)?.isOn, true)
        XCTAssertEqual(cells[0].id.uuidString, label.cellID, "内容更新は行の identity を変えない")
        XCTAssertEqual(cells[1].id.uuidString, toggle.cellID)
    }

    func test_混載したCellが実描画される() {
        let toggle = KsBridgeSwitchCell(title: "スイッチ")
        let entry = KsBridgeEntryCell(title: "入力")
        let bridge = KsBridgeFixture.withCells([toggle, entry])
        let attachment = KsBridgeTestHost.attach(bridge)

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["スイッチ", "入力"]])
    }
}
#endif
