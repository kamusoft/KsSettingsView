// KsCellRegistry+CustomCell.swift
// KsSettingsViewUI
//
// `CustomCell` を `KsCellRegistry` に登録する API。
// 基本 Cell 7 種（`registerBasicCells()`）/ 入力 Cell 5 種（`registerInputCells()`）と同列の
// 標準登録集合として扱い、利用者が Registry を操作しなくても描画できるようにする。

#if canImport(UIKit)
import Foundation
import UIKit
import KsSettingsViewCore

extension KsCellRegistry {
    /// `CustomCell` の Renderer を登録する。
    ///
    /// `CustomCell` は非ジェネリック型（content / builder を内部で型消去して保持する）のため、
    /// 実体型ごとの登録は不要で本 API を 1 度呼ぶだけで全ての CustomCell が解決される。
    ///
    /// `KsSettingsViewController` の初期化で `autoRegisterCustomCell`（既定 `true`）により
    /// 自動的に呼ばれるため、利用者の明示登録は通常不要。
    ///
    /// すでに登録済みの場合は上書き登録（後勝ち）になる。
    public func registerCustomCell() {
        register(cellType: CustomCell.self, rendererType: CustomCellView.self)
    }
}
#endif
