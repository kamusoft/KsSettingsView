// KsBridgeInteractionRelay.swift
// KsSettingsViewBridge
//
// Native Cell のコールバックを現在の delegate へ転送する中継。

#if canImport(UIKit)
import Foundation

/// Cell のコールバックと `KsBridgeInteractionDelegate` の間に立つ中継。
///
/// Cell のコールバック閉包はこの中継だけを掴み、delegate も Bridge も直接掴まない。中継は
/// delegate を弱参照で保持するため、delegate 実装 (facade 側) の回収を妨げない。delegate 未設定・
/// 解除後の通知は黙って破棄される。
///
/// 通知は Native の UI スレッド上で同期に届く経路でのみ使う。Bridge の全 API と同じスレッド契約
/// (maui/ADR-0005) に従うため、中継自身は同期化を行わない。
internal final class KsBridgeInteractionRelay: @unchecked Sendable {

    /// 通知先。未設定・解除後は `nil` で、通知は破棄される。
    internal weak var delegate: (any KsBridgeInteractionDelegate)?

    /// CommandCell のタップを転送する。
    func commandCellTapped(cellID: String) {
        delegate?.commandCellTapped(cellID: cellID)
    }

    /// ButtonCell のタップを転送する。
    func buttonCellTapped(cellID: String) {
        delegate?.buttonCellTapped(cellID: cellID)
    }

    /// CustomCell の行タップを転送する。
    func customCellTapped(cellID: String) {
        delegate?.customCellTapped(cellID: cellID)
    }

    /// SwitchCell の値変更を転送する。
    func switchCellChanged(cellID: String, isOn: Bool) {
        delegate?.switchCellChanged(cellID: cellID, isOn: isOn)
    }

    /// CheckboxCell の値変更を転送する。
    func checkboxCellChanged(cellID: String, isChecked: Bool) {
        delegate?.checkboxCellChanged(cellID: cellID, isChecked: isChecked)
    }

    /// SimpleCheckCell の値変更を転送する。
    func simpleCheckCellChanged(cellID: String, isChecked: Bool) {
        delegate?.simpleCheckCellChanged(cellID: cellID, isChecked: isChecked)
    }

    /// RadioCell の選択を転送する。
    func radioCellSelected(cellID: String, value: String) {
        delegate?.radioCellSelected(cellID: cellID, value: value)
    }

    /// EntryCell のテキスト変更を転送する。
    func entryCellTextChanged(cellID: String, text: String) {
        delegate?.entryCellTextChanged(cellID: cellID, text: text)
    }

    /// PickerCell (単一選択) の選択変更を転送する。
    func pickerCellSelectionChanged(cellID: String, index: Int) {
        delegate?.pickerCellSelectionChanged(cellID: cellID, index: index)
    }

    /// PickerCell (複数選択) の選択変更を、昇順・重複なしへ正規化して転送する。
    func pickerCellMultiSelectionChanged(cellID: String, indices: Set<Int>) {
        delegate?.pickerCellMultiSelectionChanged(
            cellID: cellID,
            indices: KsBridgeValueTransport.indexList(from: indices)
        )
    }

    /// NumberPickerCell の値変更を転送する。
    func numberPickerCellChanged(cellID: String, value: Int) {
        delegate?.numberPickerCellChanged(cellID: cellID, value: value)
    }

    /// TimePickerCell の時刻変更を輸送書式の文字列へ変換して転送する。
    func timePickerCellChanged(cellID: String, time: Date) {
        delegate?.timePickerCellChanged(
            cellID: cellID,
            time: KsBridgeValueTransport.timeText(from: time)
        )
    }

    /// DatePickerCell の日付変更を輸送書式の文字列へ変換して転送する。
    func datePickerCellChanged(cellID: String, date: Date) {
        delegate?.datePickerCellChanged(
            cellID: cellID,
            date: KsBridgeValueTransport.dateText(from: date)
        )
    }
}
#endif
