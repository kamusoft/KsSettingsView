// KsBridgeCellUpdate.swift
// KsSettingsViewBridge
//
// `replaceCells` へ渡す (対象 cellID, 新しい Cell) の組を表す `@objc` 互換 DTO。

#if canImport(UIKit)
import Foundation

/// 複数 Cell の内容をまとめて置換するときの 1 件分の指定。
///
/// `cellID` は Bridge が採番して返した既存 Cell の ID で、`cell` はその位置へ写し取る新しい内容。
/// `cell` 自身が持つ `cellID` は使われず、更新後も対象 Cell の identity は `cellID` のまま保たれる。
///
/// `cell` は共通基底型で受けるため、1 回のバッチに Cell 種の異なる更新を混載できる。
@objc(KsBridgeCellUpdate)
public final class KsBridgeCellUpdate: NSObject {

    /// 更新対象の cellID。
    @objc public let cellID: String

    /// 更新後の内容。
    @objc public let cell: KsBridgeCell

    /// 更新対象と新しい内容を指定して生成する。
    /// - Parameters:
    ///   - cellID: 更新対象の cellID
    ///   - cell: 更新後の内容
    @objc public init(cellID: String, cell: KsBridgeCell) {
        self.cellID = cellID
        self.cell = cell
        super.init()
    }
}
#endif
