// KsCellRegistry+BasicCells.swift
// KsSettingsViewUI
//
// 基本 Cell 7 種（LabelCell / CommandCell / ButtonCell / SwitchCell / CheckboxCell /
// RadioCell / SimpleCheckCell）を `KsCellRegistry` にまとめて登録する API。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore

extension KsCellRegistry {
    /// 基本 Cell 7 種をまとめて登録する。
    ///
    /// 個別に register を書く代わりに本 API を 1 度呼ぶだけで、Sample アプリや
    /// ユーザーアプリで全 7 種が利用できるようになる。
    ///
    /// すでに登録済みの Cell 型に対しては上書き登録（後勝ち）になる。
    public func registerBasicCells() {
        register(cellType: LabelCell.self, rendererType: LabelCellView.self)
        register(cellType: CommandCell.self, rendererType: CommandCellView.self)
        register(cellType: ButtonCell.self, rendererType: ButtonCellView.self)
        register(cellType: SwitchCell.self, rendererType: SwitchCellView.self)
        register(cellType: CheckboxCell.self, rendererType: CheckboxCellView.self)
        register(cellType: RadioCell.self, rendererType: RadioCellView.self)
        register(cellType: SimpleCheckCell.self, rendererType: SimpleCheckCellView.self)
    }
}
#endif
