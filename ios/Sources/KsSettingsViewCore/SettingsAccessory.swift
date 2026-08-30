// SettingsAccessory.swift
// KsSettingsViewCore
//
// `SettingsRootDiff.updateAccessory` で `RootAccessory` / `SectionAccessory` を
// 統一的に扱うための sum type。

import Foundation

/// `updateAccessory` Diff で Root / Section の Accessory を統一して扱うための sum type。
///
/// - `root(RootAccessory)`: Root レベル H/F に使用
/// - `section(SectionAccessory)`: Section レベル H/F に使用
///
/// 内部 `RootAccessory.view` / `SectionAccessory.view` ケースの `KsAnyView` は
/// 既存の `RootAccessory` / `SectionAccessory` の `Hashable` 実装に従い「ケース一致のみで等価」
/// と扱われる。本型は両ケースの判別子と内部値で Hashable 自動合成される
/// （`RootAccessory` / `SectionAccessory` 自身が `Hashable` であるため）。
///
/// 本型は `RootAccessory` / `SectionAccessory` を置き換えない。Store API や
/// 利用者コードでは個別型を使い、Diff DTO 内部での統一表現専用とする。
public enum SettingsAccessory: Hashable, Sendable {
    /// Root レベル H/F に使用
    case root(RootAccessory)
    /// Section レベル H/F に使用
    case section(SectionAccessory)
}
