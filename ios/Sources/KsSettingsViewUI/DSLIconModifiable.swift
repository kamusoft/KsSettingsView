// DSLIconModifiable.swift
// KsSettingsViewUI
//
// DSL 経路から `icon` 書き換えを許容する Cell が満たすべき規約（UI 層配置）。
//
// SwiftUI DSL の `.icon(_ icon: KsImage)` modifier 経路を満たすために、UI 層で `icon` を持つ
// Cell が準拠するプロトコルとして定義する。
//
// `KsImage` は `KsSettingsViewUI` 所属の sealed enum であり、Core からは参照できないため
// 本プロトコルも UI 層配置となる。`DSLStyleModifiable` と同様、`KsSettingsViewSwiftUI`
// 層の DSL Modifier は `KsSettingsViewSwiftUI → KsSettingsViewUI` の依存を辿って参照する。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore

/// DSL から `icon` の書き換えを許容する Cell が満たすべきプロトコル（UI 層配置）。
///
/// `KsCell` の各具象 Cell は読み取り専用の `let icon: KsImage?` を持つため、modifier から
/// `icon` を書き換えるには「`icon` のみ書き換えた自身の copy」を生成する API が必要。
/// 本プロトコルはその API を規定する。
///
/// `icon: KsImage?` を持つ Cell が準拠する。アイコン領域を持たない `CustomCell` は準拠せず、
/// その場合 `.icon(_:)` modifier は no-op として扱う（API はすべての Cell に提供しつつ、
/// 対象 Cell に icon フィールドがないケースは無視する）。
public protocol DSLIconModifiable: KsCell {
    /// 自身を copy し、新しい `icon` を持つ Cell を返す。
    /// - Parameter icon: 新しい `KsImage`（`nil` でアイコンクリア）
    func withIcon(_ icon: KsImage?) -> Self
}
#endif
