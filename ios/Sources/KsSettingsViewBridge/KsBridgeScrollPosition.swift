// KsBridgeScrollPosition.swift
// KsSettingsViewBridge
//
// interop 境界でスクロール命令の位置を輸送する整数と `KsScrollPosition` の変換。

#if canImport(UIKit)
import Foundation
import KsSettingsViewUI

/// スクロール命令の位置の整数と `KsScrollPosition` を橋渡しする。
///
/// interop 境界では enum をそのまま渡せないため、位置は整数 (start = 0 / center = 1 / end = 2) で
/// 表す。定義域外の値は `.start` として扱う — 命令の位置の既定が start であり、未定義の値で命令を
/// 捨てるより既定の合わせ方で届けるほうが呼び出し側の意図に近いため。
internal enum KsBridgeScrollPosition {

    /// 輸送された整数を `KsScrollPosition` へ変換する。
    /// - Parameter value: 位置の整数
    /// - Returns: 対応する位置。定義域外の値では `.start`
    static func position(from value: Int) -> KsScrollPosition {
        switch value {
        case 1:
            return .center
        case 2:
            return .end
        default:
            return .start
        }
    }
}
#endif
