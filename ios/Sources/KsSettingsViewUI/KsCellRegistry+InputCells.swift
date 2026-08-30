// KsCellRegistry+InputCells.swift
// KsSettingsViewUI
//
// 入力系 Cell 5 種（EntryCell / PickerCell / NumberPickerCell / TimePickerCell / DatePickerCell）を
// `KsCellRegistry` にまとめて登録する API。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore

extension KsCellRegistry {
    /// 入力系 Cell 5 種をまとめて登録する。
    ///
    /// 個別に register を書く代わりに本 API を 1 度呼ぶだけで、Sample アプリや
    /// ユーザーアプリで全 5 種が利用できるようになる。
    ///
    /// すでに登録済みの Cell 型に対しては上書き登録（後勝ち）になる。
    public func registerInputCells() {
        register(cellType: EntryCell.self, rendererType: EntryCellView.self)
        register(cellType: PickerCell.self, rendererType: PickerCellView.self)
        register(cellType: NumberPickerCell.self, rendererType: NumberPickerCellView.self)
        register(cellType: TimePickerCell.self, rendererType: TimePickerCellView.self)
        register(cellType: DatePickerCell.self, rendererType: DatePickerCellView.self)
    }
}
#endif
