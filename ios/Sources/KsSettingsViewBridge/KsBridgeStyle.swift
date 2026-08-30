// KsBridgeStyle.swift
// KsSettingsViewBridge
//
// interop 境界で見た目スタイルを輸送する序数と `KsSettingsViewStyle` の変換。

#if canImport(UIKit)
import Foundation
import KsSettingsViewUI

/// 見た目スタイルの序数と `KsSettingsViewStyle` を橋渡しする。
///
/// interop 境界では enum をそのまま渡せないため、スタイルは列挙の序数 (classic = 0 / modern = 1)
/// で表す。定義域外の序数は `.classic` へ正規化する — 呼び出し側の公開契約が
/// 「非 nullable・既定 classic」であり、未定義の値に倒す先を持たないため。
internal enum KsBridgeStyle {

    /// 輸送された序数を `KsSettingsViewStyle` へ変換する。
    /// - Parameter ordinal: 見た目スタイルの序数
    /// - Returns: 対応するスタイル。定義域外の序数では `.classic`
    static func style(from ordinal: Int) -> KsSettingsViewStyle {
        switch ordinal {
        case 1:
            return .modern
        default:
            return .classic
        }
    }
}
#endif
