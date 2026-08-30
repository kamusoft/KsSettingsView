// KsBridgeColor.swift
// KsSettingsViewBridge
//
// interop 境界で色を輸送する ARGB 32bit 整数と `UIColor` の相互変換。

#if canImport(UIKit)
import Foundation
import UIKit

/// ARGB を詰めた 32bit 整数と `UIColor` を橋渡しする。
///
/// interop 境界では platform の色型を直接渡せないため、色は ARGB を詰めた 32bit 整数で表す
/// (maui/ADR-0004)。`nil` は「未指定」を意味する。
internal enum KsBridgeColor {

    /// ARGB を詰めた 32bit 整数を `UIColor` へ変換する。`nil` は未指定として `nil` を返す。
    /// - Parameter argb: ARGB を詰めた 32bit 整数 (未指定は `nil`)
    static func uiColor(_ argb: NSNumber?) -> UIColor? {
        guard let argb else { return nil }
        let value = UInt32(bitPattern: argb.int32Value)
        return UIColor(
            red: CGFloat((value >> 16) & 0xFF) / 255.0,
            green: CGFloat((value >> 8) & 0xFF) / 255.0,
            blue: CGFloat(value & 0xFF) / 255.0,
            alpha: CGFloat((value >> 24) & 0xFF) / 255.0
        )
    }
}
#endif
