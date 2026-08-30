// KsBridgeAccessoryTarget.swift
// KsSettingsViewBridge
//
// `updateAccessory` の更新対象を interop 境界で表す `@objc` 互換の列挙体。

import Foundation

/// Accessory (Root / Section の header・footer) の更新対象。
///
/// `sectionHeader` / `sectionFooter` を指定するときは、あわせて対象 Section の sectionID を渡す。
/// `rootHeader` / `rootFooter` では sectionID は参照されない。
@objc(KsBridgeAccessoryTarget)
public enum KsBridgeAccessoryTarget: Int {
    /// Root レベルのヘッダ
    case rootHeader = 0
    /// Root レベルのフッタ
    case rootFooter = 1
    /// 指定 Section のヘッダ
    case sectionHeader = 2
    /// 指定 Section のフッタ
    case sectionFooter = 3
}
