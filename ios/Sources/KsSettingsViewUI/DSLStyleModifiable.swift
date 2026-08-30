// DSLStyleModifiable.swift
// KsSettingsViewUI
//
// DSL 経路から `CellStyle` 書き換えを許容する Cell が満たすべき規約（UI 層配置）。
//
// DSL 経由のスタイル書き換え経路は UI 層の `CellStyle` を扱う。`CellStyle` は UI 層所属で
// Core からは参照できないため（core/ADR-0009）、本プロトコルも UI 層に置く。`SwiftUI` 層の
// DSL Modifier は `KsSettingsViewSwiftUI → KsSettingsViewUI` の依存を辿って本プロトコルを
// 参照する。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore

/// DSL から `CellStyle` の書き換えを許容する Cell が満たすべきプロトコル（UI 層配置）。
///
/// `KsCell` の各具象 Cell は読み取り専用の `let style: CellStyle` を持つため、modifier から
/// `CellStyle` の各フィールドを書き換えるには「`style` のみ書き換えた自身の copy」を
/// 生成する API が必要。本プロトコルはその API を規定する。
///
/// `KsSettingsViewUI` 層の基本 Cell（`LabelCell` / `SwitchCell` / `CheckboxCell` /
/// `RadioCell` / `SimpleCheckCell` / `ButtonCell` / `CommandCell`）はすべて本プロトコルへ準拠する。
///
/// Core の `KsCell` プロトコルは `style` を要求しないため、本プロトコルが書き換え API と併せて
/// `style` 取得 API も束ねて要求する。これにより modifier 経路は
/// `cell as? any DSLStyleModifiable` 経由で `style` を取得できる。
public protocol DSLStyleModifiable: KsCell {
    /// Cell 個別の `CellStyle` を返す（読み取り専用）。
    var style: CellStyle { get }

    /// 自身を copy し、新しい `style` を持つ Cell を返す。
    /// - Parameter style: 新しい `CellStyle`
    func withStyle(_ style: CellStyle) -> Self
}
#endif
