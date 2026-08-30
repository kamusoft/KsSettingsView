// KsBridgeValueTransportTests.swift
// KsSettingsViewBridgeTests
//
// interop 境界の値表現と Native 型の相互変換を検証する。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class KsBridgeValueTransportTests: XCTestCase {

    // MARK: - 時刻 / 日付

    func test_時刻文字列がDateへ往復する() {
        let parsed = KsBridgeValueTransport.time(from: "09:05")

        let components = Calendar.current.dateComponents([.hour, .minute], from: parsed)
        XCTAssertEqual(components.hour, 9)
        XCTAssertEqual(components.minute, 5)
        XCTAssertEqual(KsBridgeValueTransport.timeText(from: parsed), "09:05")
    }

    func test_日付文字列がDateへ往復する() {
        let parsed = KsBridgeValueTransport.date(from: "2026-08-10")

        let components = Calendar.current.dateComponents([.year, .month, .day], from: parsed)
        XCTAssertEqual(components.year, 2026)
        XCTAssertEqual(components.month, 8)
        XCTAssertEqual(components.day, 10)
        XCTAssertEqual(KsBridgeValueTransport.dateText(from: parsed), "2026-08-10")
    }

    func test_解釈できない時刻文字列は既定値になる() {
        let parsed = KsBridgeValueTransport.time(from: "とけい")

        let components = Calendar.current.dateComponents([.hour, .minute], from: parsed)
        XCTAssertEqual(components.hour, 0)
        XCTAssertEqual(components.minute, 0)
    }

    func test_解釈できない日付文字列は既定値になる() {
        let parsed = KsBridgeValueTransport.date(from: "とてもひづけ")

        XCTAssertEqual(KsBridgeValueTransport.dateText(from: parsed), "1970-01-01")
    }

    func test_輸送書式から外れた表記は解釈されず既定値になる() {
        // 区切り文字違い・桁数不足は書式に一致しないものとして扱う
        XCTAssertEqual(KsBridgeValueTransport.dateText(from: KsBridgeValueTransport.date(from: "2026/08/10")),
                       "1970-01-01")
        XCTAssertEqual(KsBridgeValueTransport.dateText(from: KsBridgeValueTransport.date(from: "2026-8-10")),
                       "1970-01-01")
        XCTAssertEqual(KsBridgeValueTransport.timeText(from: KsBridgeValueTransport.time(from: "9:05")),
                       "00:00")
        XCTAssertNil(KsBridgeValueTransport.optionalDate(from: "2026/08/10"))
    }

    func test_解釈できない任意日付文字列は未指定になる() {
        XCTAssertNil(KsBridgeValueTransport.optionalDate(from: "not-a-date"))
        XCTAssertNil(KsBridgeValueTransport.optionalDate(from: nil))
        XCTAssertNotNil(KsBridgeValueTransport.optionalDate(from: "2026-08-10"))
    }

    func test_解釈できない日付を持つDTOも他フィールドを反映して構築される() {
        let dto = KsBridgeDatePickerCell(title: "日付")
        dto.date = "8/10/2026"
        dto.pickerTitle = "日付を選択"
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: DatePickerCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(KsBridgeValueTransport.dateText(from: cell?.date ?? Date()), "1970-01-01")
        XCTAssertEqual(cell?.title, "日付")
        XCTAssertEqual(cell?.pickerTitle, "日付を選択")
    }

    func test_解釈できない日付をinsertCellとreplaceCellへ渡しても同じ既定値になる() {
        let bridge = KsBridgeFixture.withCells([KsBridgeLabelCell(title: "ラベル")])
        let sectionID = bridge.store.root.sections[0].id.uuidString

        let inserted = KsBridgeDatePickerCell(title: "挿入")
        inserted.date = "invalid"
        bridge.insertCell(inserted, sectionID: sectionID, at: 1)

        let replacement = KsBridgeDatePickerCell(title: "差し替え")
        replacement.date = "invalid"
        bridge.replaceCell(cellID: bridge.store.root.sections[0].cells[0].id.uuidString, newCell: replacement)

        let cells = bridge.store.root.sections.first?.cells ?? []
        for cell in cells.compactMap({ $0 as? DatePickerCell }) {
            XCTAssertEqual(KsBridgeValueTransport.dateText(from: cell.date), "1970-01-01")
        }
        XCTAssertEqual(cells.compactMap { $0 as? DatePickerCell }.count, 2)
    }

    // MARK: - enum の序数

    func test_keyboard序数がUIKeyboardTypeへ変換される() {
        XCTAssertEqual(KsBridgeValueTransport.keyboardType(from: 0), .default)
        XCTAssertEqual(KsBridgeValueTransport.keyboardType(from: 1), .default)
        XCTAssertEqual(KsBridgeValueTransport.keyboardType(from: 2), .default)
        XCTAssertEqual(KsBridgeValueTransport.keyboardType(from: 3), .default)
        XCTAssertEqual(KsBridgeValueTransport.keyboardType(from: 4), .URL)
        XCTAssertEqual(KsBridgeValueTransport.keyboardType(from: 5), .emailAddress)
        XCTAssertEqual(KsBridgeValueTransport.keyboardType(from: 6), .decimalPad)
        XCTAssertEqual(KsBridgeValueTransport.keyboardType(from: 7), .phonePad)
    }

    func test_対応の取れないkeyboard序数はNative既定へ倒れる() {
        XCTAssertEqual(KsBridgeValueTransport.keyboardType(from: 99), .default)
        XCTAssertEqual(KsBridgeValueTransport.keyboardType(from: -1), .default)
    }

    func test_uiStyle序数がDatePickerUIStyleへ変換される() {
        XCTAssertEqual(KsBridgeValueTransport.datePickerUIStyle(from: NSNumber(value: 0)), .calendar)
        XCTAssertEqual(KsBridgeValueTransport.datePickerUIStyle(from: NSNumber(value: 1)), .wheels)
        XCTAssertNil(KsBridgeValueTransport.datePickerUIStyle(from: nil), "未指定は Native 既定を使う")
        XCTAssertNil(KsBridgeValueTransport.datePickerUIStyle(from: NSNumber(value: 99)))
    }

    func test_配置序数がCellTitleAlignmentへ変換される() {
        XCTAssertEqual(KsBridgeValueTransport.titleAlignment(from: NSNumber(value: 0), fallback: .center), .start)
        XCTAssertEqual(KsBridgeValueTransport.titleAlignment(from: NSNumber(value: 1), fallback: .start), .center)
        XCTAssertEqual(KsBridgeValueTransport.titleAlignment(from: NSNumber(value: 2), fallback: .start), .end)
        XCTAssertEqual(KsBridgeValueTransport.titleAlignment(from: nil, fallback: .center), .center)
        XCTAssertEqual(KsBridgeValueTransport.titleAlignment(from: NSNumber(value: 9), fallback: .end), .end)
    }

    func test_選択モード序数がPickerSelectionModeへ変換される() {
        XCTAssertEqual(KsBridgeValueTransport.selectionMode(from: 0), .single)
        XCTAssertEqual(KsBridgeValueTransport.selectionMode(from: 1), .multiple)
        XCTAssertEqual(KsBridgeValueTransport.selectionMode(from: 99), .single)
    }

    // MARK: - 選択 index

    func test_複数選択indexは順序と重複を問わず同じ集合になる() {
        XCTAssertEqual(KsBridgeValueTransport.indexSet(from: [2, 0]), [0, 2])
        XCTAssertEqual(KsBridgeValueTransport.indexSet(from: [0, 2, 2]), [0, 2])
    }

    func test_通知方向の複数選択indexは昇順になる() {
        XCTAssertEqual(KsBridgeValueTransport.indexList(from: [2, 0, 1]), [0, 1, 2])
    }

    func test_順序違いの複数選択DTOは同一のNative値になる() {
        let ascending = KsBridgePickerCell(title: "選択")
        ascending.items = ["A", "B", "C"].map { KsBridgePickerItem(text: $0) }
        ascending.selectionMode = 1
        ascending.selectedIndices = [0, 2]
        let descending = KsBridgePickerCell(title: "選択")
        descending.items = ["A", "B", "C"].map { KsBridgePickerItem(text: $0) }
        descending.selectionMode = 1
        descending.selectedIndices = [2, 0]
        let bridge = KsBridgeFixture.withCells([ascending])

        let before: PickerCell? = KsBridgeFixture.storedCell(bridge)
        bridge.replaceCell(cellID: ascending.cellID, newCell: descending)
        let after: PickerCell? = KsBridgeFixture.storedCell(bridge)

        XCTAssertEqual(before?.selectedIndices, after?.selectedIndices)
        XCTAssertEqual(after?.selectedIndices, [0, 2])
    }

    func test_範囲外の選択indexは正規化せず透過する() {
        let single = KsBridgePickerCell(title: "単一選択")
        single.items = ["A", "B"].map { KsBridgePickerItem(text: $0) }
        single.selectedIndex = NSNumber(value: 9)
        let multiple = KsBridgePickerCell(title: "複数選択")
        multiple.items = ["A", "B"].map { KsBridgePickerItem(text: $0) }
        multiple.selectionMode = 1
        multiple.selectedIndices = [-1, 7]
        let bridge = KsBridgeFixture.withCells([single, multiple])

        let cells = bridge.store.root.sections.first?.cells ?? []
        XCTAssertEqual((cells[0] as? PickerCell)?.selectedIndex, 9)
        XCTAssertEqual((cells[1] as? PickerCell)?.selectedIndices, [-1, 7])
    }
}
#endif
