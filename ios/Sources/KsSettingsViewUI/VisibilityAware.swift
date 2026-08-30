// VisibilityAware.swift
// KsSettingsViewUI
//
// Cell の可視性フィルタを「opt-in」で受け取るための UI 層プロトコル。
// 可視性は構造同期・内容同期とは別経路の visible projection として扱う（core/ADR-0010）。

import Foundation

/// Cell が `isVisible: Bool` プロパティを公開する opt-in 抽象。
///
/// UI 層のフィルタ層（`KsSettingsViewController` の visible projection 構築）は
/// `(cell as? VisibilityAware)?.isVisible ?? true` の形で問い合わせる。
///
/// - Core 抽象 `KsCell` プロトコルにはこの要求を追加しない（Core 純化方針）。
/// - UI 層が提供する Cell は `CustomCell` を含めてすべて opt-in 準拠する。
/// - 非準拠の Cell（ライブラリ利用者が独自定義した `KsCell` 準拠型など）はフィルタの判定で
///   常に visible（`true`）として扱われ、描画される。
public protocol VisibilityAware {
    /// Cell の可視性。`true` で UI 層の visible projection に含める、`false` で除外する。
    var isVisible: Bool { get }
}
