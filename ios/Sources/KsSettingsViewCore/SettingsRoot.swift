// SettingsRoot.swift
// KsSettingsViewCore
//
// 設定画面全体の状態を表すルート値型。
//
// 本型は `theme` を持たない。Theme は UI 層が View 側 modifier / 引数で個別に受け取る経路に
// 一本化する（iOS: `.theme(_:)` modifier、Android: `KsSettingsView(theme = ...)`）。

import Foundation

/// 設定画面全体の状態を表すルート値型。
///
/// 複数の `Section` のみを保持するイミュータブル値型。Theme は UI 層の責務であり本型は持たない
/// （core/ADR-0009）。`UICollectionViewDiffableDataSource` のスナップショット差分に
/// 耐える `Hashable` 契約を満たす。
///
/// 等価性判定は `sections` のみで決定される。
public struct SettingsRoot: Hashable, Sendable {
    /// セクション群
    public let sections: [Section]

    /// 任意フィールドを指定して `SettingsRoot` を生成する。
    /// - Parameter sections: セクション群（既定で空配列）
    public init(sections: [Section] = []) {
        self.sections = sections
    }

    // MARK: - Hashable 手動実装
    //
    // `sections` のみで等価性判定を行う。Theme は本型が持たず、UI 層 modifier / 引数で受け取る。

    public static func == (lhs: SettingsRoot, rhs: SettingsRoot) -> Bool {
        return lhs.sections == rhs.sections
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(sections)
    }
}
