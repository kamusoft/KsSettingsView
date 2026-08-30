// PickerSelectionMode.swift
// KsSettingsViewUI
//
// `PickerCell` の選択モード（単一 / 複数）を表す UI 層独自列挙型。
// UIKit に対応する Native 型が無いため、Native 型直接公開の方針（core/ADR-0009）の例外として
// UI 層独自の列挙型を置く。

import Foundation

/// `PickerCell` の選択モードを表す列挙型。
///
/// UIKit に対応する Native 型（`single / multiple` を 1 つの列挙で表す型）が無いため、
/// UI 層独自の論理スイッチとして定義する。Cell の動作モードを表すだけのため
/// 「Native 型を独自値型でラップする」こととは目的が異なる。
public enum PickerSelectionMode: Hashable, Sendable {
    /// 単一選択モード。`PickerCell` の `selectedIndex` を TwoWay binding する。
    case single
    /// 複数選択モード。`PickerCell` の `selectedIndices` を TwoWay binding する。
    case multiple
}
