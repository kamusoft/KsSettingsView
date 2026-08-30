// KsBridgeProbe.swift
// KsSettingsViewBridge
//
// probe 専用の最小例 (add-maui-basic-input-cells / tasks 1.1〜1.5)。
// binding 生成と実行時の往復の可否だけを確かめる一時コードであり、Bridge の公開契約ではない。
// 検証が済んだら削除する。

#if canImport(UIKit)
import Foundation
import UIKit

/// probe 1.1: `@objc` protocol (delegate) が C# 側の実装で受けられるかの検証。
///
/// selector は Swift の既定生成 (`WithLabel:`) に任せず `@objc(...)` で明示する。
@objc(KsBridgeProbeDelegate)
public protocol KsBridgeProbeDelegate: NSObjectProtocol {

    /// cellID のみの通知。
    @objc(probeTapped:)
    func probeTapped(cellID: String)

    /// scalar 引数を伴う通知。
    @objc(probeSwitchChanged:isOn:)
    func probeSwitchChanged(cellID: String, isOn: Bool)

    /// 配列引数を伴う通知 (複数選択の wire 表現の検証)。
    @objc(probeIndicesChanged:indices:)
    func probeIndicesChanged(cellID: String, indices: [Int])

    /// 文字列引数を伴う通知 (ISO 時刻の wire 表現の検証)。
    @objc(probeTimeChanged:time:)
    func probeTimeChanged(cellID: String, time: String)
}

/// probe 1.4 / 1.3 / 1.5: 共通基底 DTO・platform 画像・nullable scalar の検証。
@objc(KsBridgeProbeCell)
public class KsBridgeProbeCell: NSObject {

    /// 基底が採番する Cell ID。
    @objc public let cellID: String

    /// 共通フィールド。
    @objc public var title: String = ""

    /// probe 1.3: platform 画像 (UIImage) を interop で受け取れるか。
    @objc public var icon: UIImage?

    /// probe 1.5: nullable scalar (boxed NSNumber) の binding 表現。
    @objc public var iconSize: NSNumber?

    /// probe 1.5: nullable scalar (enum の序数輸送) の binding 表現。
    @objc public var uiStyle: NSNumber?

    /// 基底の指定イニシャライザ。既存 DTO と同じく引数付き init だけを公開する。
    @objc public init(title: String) {
        self.cellID = UUID().uuidString
        self.title = title
        super.init()
    }
}

/// probe 1.4: 基底の派生 A。基底の init をそのまま使う。
@objc(KsBridgeProbeLabelCell)
public final class KsBridgeProbeLabelCell: KsBridgeProbeCell {}

/// probe 1.4: 基底の派生 B (固有フィールドと固有 init を持つ)。
@objc(KsBridgeProbeSwitchCell)
public final class KsBridgeProbeSwitchCell: KsBridgeProbeCell {

    /// 派生固有フィールド。
    @objc public var isOn: Bool = false

    /// 派生固有の指定イニシャライザ。
    @objc public init(title: String, isOn: Bool) {
        self.isOn = isOn
        super.init(title: title)
    }
}

/// probe の入口。delegate 保持と異種 DTO 混載を C# から駆動する。
@objc(KsBridgeProbe)
public final class KsBridgeProbe: NSObject {

    /// probe 1.1: delegate は ObjC 慣例どおり weak で保持する。
    @objc public weak var delegate: KsBridgeProbeDelegate?

    /// probe 1.4: 基底型の配列として異種 DTO を受け渡す。
    @objc public var cells: [KsBridgeProbeCell] = []

    /// probe 1.4: 基底型の引数で異種 DTO を受け取る。
    @discardableResult
    @objc(addCell:)
    public func addCell(_ cell: KsBridgeProbeCell) -> String {
        cells.append(cell)
        return cell.cellID
    }

    /// probe 1.3〜1.5: 受け取った DTO を Swift 側で判別・読み出しした結果を文字列で返す。
    @objc public func describeCells() -> String {
        return cells.map { cell -> String in
            let kind: String
            if let toggle = cell as? KsBridgeProbeSwitchCell {
                kind = "Switch(on=\(toggle.isOn))"
            } else if cell is KsBridgeProbeLabelCell {
                kind = "Label"
            } else {
                kind = "Unknown"
            }
            let iconKind = cell.icon.map { "UIImage(\(Int($0.size.width))x\(Int($0.size.height)))" } ?? "nil"
            let iconSize = cell.iconSize.map { "\($0.doubleValue)" } ?? "nil"
            let uiStyle = cell.uiStyle.map { "\($0.intValue)" } ?? "nil"
            return "\(kind) title=\(cell.title) icon=\(iconKind) iconSize=\(iconSize) uiStyle=\(uiStyle)"
        }.joined(separator: " | ")
    }

    /// probe 1.1: 保持中の delegate へ 4 種の通知を発火する。
    @objc public func fireAll() {
        guard let target = delegate else { return }
        target.probeTapped(cellID: "cell-1")
        target.probeSwitchChanged(cellID: "cell-2", isOn: true)
        target.probeIndicesChanged(cellID: "cell-3", indices: [1, 2, 3])
        target.probeTimeChanged(cellID: "cell-4", time: "09:30")
    }

    /// probe 1.1: delegate が weak で保持されている (解放済みなら通知されない) ことの確認用。
    @objc public var hasDelegate: Bool { delegate != nil }
}
#endif
