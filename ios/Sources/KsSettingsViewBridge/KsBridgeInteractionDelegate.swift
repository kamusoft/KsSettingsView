// KsBridgeInteractionDelegate.swift
// KsSettingsViewBridge
//
// interop 境界へユーザー操作を通知する `@objc` protocol。

#if canImport(UIKit)
import Foundation

/// Bridge が表示中の Cell に対するユーザー操作を通知する delegate。
///
/// Bridge instance あたり 1 個の通知チャネルで全 Cell の操作を運び、Cell 種別はメソッド名で
/// 識別する (maui/ADR-0003)。通知は cellID と新しい値を引数に取り、値の表現は interop 境界の
/// 輸送規約に従う (maui/ADR-0012) — 二値は Bool、数値と選択 index は Int、複数選択は昇順・
/// 重複なしの Int 配列、時刻は "HH:mm"、日付は "yyyy-MM-dd" の文字列。
///
/// 通知は Native の UI スレッド上で同期に呼ばれる。Bridge は delegate を弱参照で保持するため、
/// 実装インスタンスの寿命は呼び出し側が保証する。
///
/// selector は Swift の既定生成に任せず `@objc(...)` で固定する (第 1 引数ラベルの畳み込みを
/// 避け、binding 側の宣言と 1 文字違わず合わせるため)。
@objc(KsBridgeInteractionDelegate)
public protocol KsBridgeInteractionDelegate: NSObjectProtocol {

    /// CommandCell がタップされた。
    /// - Parameter cellID: 対象 Cell の cellID
    @objc(commandCellTapped:)
    func commandCellTapped(cellID: String)

    /// ButtonCell がタップされた。
    /// - Parameter cellID: 対象 Cell の cellID
    @objc(buttonCellTapped:)
    func buttonCellTapped(cellID: String)

    /// CustomCell の行がタップされた。
    ///
    /// タップ通知を持たせずに構築された CustomCell は行タップ動作そのものを持たないため、
    /// このメソッドは呼ばれない。
    /// - Parameter cellID: 対象 Cell の cellID
    @objc(customCellTapped:)
    func customCellTapped(cellID: String)

    /// SwitchCell の値が変わった。
    /// - Parameters:
    ///   - cellID: 対象 Cell の cellID
    ///   - isOn: 新しい ON/OFF 値
    @objc(switchCellChanged:isOn:)
    func switchCellChanged(cellID: String, isOn: Bool)

    /// CheckboxCell の値が変わった。
    /// - Parameters:
    ///   - cellID: 対象 Cell の cellID
    ///   - isChecked: 新しいチェック状態
    @objc(checkboxCellChanged:isChecked:)
    func checkboxCellChanged(cellID: String, isChecked: Bool)

    /// SimpleCheckCell の値が変わった。
    /// - Parameters:
    ///   - cellID: 対象 Cell の cellID
    ///   - isChecked: 新しいチェック状態
    @objc(simpleCheckCellChanged:isChecked:)
    func simpleCheckCellChanged(cellID: String, isChecked: Bool)

    /// RadioCell が選択された。
    /// - Parameters:
    ///   - cellID: 選択された Cell の cellID
    ///   - value: 選択された Cell の値
    @objc(radioCellSelected:value:)
    func radioCellSelected(cellID: String, value: String)

    /// EntryCell のテキストが変わった。
    /// - Parameters:
    ///   - cellID: 対象 Cell の cellID
    ///   - text: 新しいテキスト
    @objc(entryCellTextChanged:text:)
    func entryCellTextChanged(cellID: String, text: String)

    /// PickerCell (単一選択) の選択が変わった。
    /// - Parameters:
    ///   - cellID: 対象 Cell の cellID
    ///   - index: 新しい選択 index
    @objc(pickerCellSelectionChanged:index:)
    func pickerCellSelectionChanged(cellID: String, index: Int)

    /// PickerCell (複数選択) の選択が変わった。
    /// - Parameters:
    ///   - cellID: 対象 Cell の cellID
    ///   - indices: 新しい選択 index (昇順・重複なし)
    @objc(pickerCellMultiSelectionChanged:indices:)
    func pickerCellMultiSelectionChanged(cellID: String, indices: [Int])

    /// NumberPickerCell の値が変わった。
    /// - Parameters:
    ///   - cellID: 対象 Cell の cellID
    ///   - value: 新しい数値
    @objc(numberPickerCellChanged:value:)
    func numberPickerCellChanged(cellID: String, value: Int)

    /// TimePickerCell の時刻が変わった。
    /// - Parameters:
    ///   - cellID: 対象 Cell の cellID
    ///   - time: 新しい時刻 ("HH:mm")
    @objc(timePickerCellChanged:time:)
    func timePickerCellChanged(cellID: String, time: String)

    /// DatePickerCell の日付が変わった。
    /// - Parameters:
    ///   - cellID: 対象 Cell の cellID
    ///   - date: 新しい日付 ("yyyy-MM-dd")
    @objc(datePickerCellChanged:date:)
    func datePickerCellChanged(cellID: String, date: String)
}
#endif
