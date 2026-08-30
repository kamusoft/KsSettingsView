// DatePickerUIStyle.swift
// KsSettingsViewUI
//
// iOS の `DatePickerCell` UI スタイル切替用列挙型。
//
// Android 側の同名の `DatePickerUIStyle` (`Material` / `Spinner`) と並列のコンセプトで、
// iOS の `DatePickerCell` も 2 種類の UI を持つ：
//
// - `.wheels`   AiForms 互換の埋め込み InputView 方式（`UIDatePicker(.date)` + `.wheels`）
// - `.calendar` カレンダー grid を `.pageSheet` + `.custom` detent で表示する方式
//               (`UIDatePicker(.date)` + `.inline`)
//
// iOS UI 層所属。Android 側は同名の `DatePickerUIStyle` を持つが、値は
// プラットフォーム固有 (`Material` / `Spinner`) であり共有しない。

#if canImport(UIKit)
import Foundation

/// iOS `DatePickerCell` の UI スタイル切替を表す列挙型。
public enum DatePickerUIStyle: Hashable, Sendable {
    /// AiForms 互換の埋め込み InputView 方式（`UIDatePicker(.date) + .wheels`、Toolbar 付き）。
    case wheels
    /// `.pageSheet` + `.custom` detent でカレンダー grid を表示する方式（`UIDatePicker(.date) + .inline`）。
    case calendar
}
#endif
