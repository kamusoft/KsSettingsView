// AccessoryTarget.swift
// KsSettingsViewCore
//
// `SettingsRootDiff.updateAccessory` で Accessory の更新対象（Root H/F / Section H/F）を
// 表現する sum type。
//
// Root と Section で H/F の装飾責務が分かれるため（core/ADR-0005）、位置を型で表現して
// `SettingsRootDiff.updateAccessory` の適用先を一意に決める。

import Foundation

/// Diff API での Accessory 更新対象を表現する sum type。
///
/// - `rootHeader` / `rootFooter`: Root レベル H/F
/// - `sectionHeader(sectionID:)` / `sectionFooter(sectionID:)`: 指定 Section の H/F
///
/// 全ケースは `Hashable` 自動合成の対象となる（associated value は `UUID` のみ）。
public enum AccessoryTarget: Hashable, Sendable {
    /// Root レベルのヘッダ
    case rootHeader
    /// Root レベルのフッタ
    case rootFooter
    /// 指定 Section のヘッダ
    case sectionHeader(sectionID: UUID)
    /// 指定 Section のフッタ
    case sectionFooter(sectionID: UUID)
}
