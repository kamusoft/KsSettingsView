// DSLCellIdentity.swift
// KsSettingsViewCore
//
// DSL 経路で Cell の `id` 書き換えを許容する Cell が満たすべき規約（Core 配置）。
//
// Core に置くのは ID 書き換え規約（`DSLReidentifiable`）のみとする。スタイル合成経路の規約
// （`DSLStyleModifiable`）は `CellStyle` を参照するため UI 層（`KsSettingsViewUI`）に属する
// （core/ADR-0009）。

import Foundation

/// DSL から ID 書き換えを許容する Cell が満たすべきプロトコル（Core 配置）。
///
/// `KsCell` プロトコルの `id: UUID` を、DSL の同一性判定戦略で算出した安定 UUID に
/// 差し替えるための再構築メソッドを提供する。DSL は再評価のたびに宣言ツリーから安定 ID を
/// 解決し直すため、その ID を Cell 側へ反映する経路が要る（core/ADR-0008）。
///
/// 具象 Cell（UI 層の `LabelCell` / `SwitchCell` 等）は本プロトコルへの準拠を必須とする。
public protocol DSLReidentifiable: KsCell {
    /// 自身を copy し、新しい `id` を持つ Cell を返す。
    /// - Parameter id: DSL ラッパが採番した安定 UUID
    func withDSLID(_ id: UUID) -> Self
}
