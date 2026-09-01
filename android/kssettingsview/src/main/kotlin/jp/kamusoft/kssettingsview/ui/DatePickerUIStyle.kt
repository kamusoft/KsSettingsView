package jp.kamusoft.kssettingsview.ui

/**
 * [DatePickerCell] の Android 側 UI スタイルを表す列挙型（Android UI 層所属）。
 *
 * Material と Spinner の UI を切り替えるための論理スイッチで、
 * `AiForms.Maui.SettingsView` の `DatePickerCell.IsAndroidSpinnerStyle` に対応する。
 *
 * - [Material]: 既定。カレンダー選択面（[DateCalendarDialog]、android/ADR-0019）を表示
 * - [Spinner]: ボトムシート + 年/月/日の3連ホイール（[DateSelectionSheet]、android/ADR-0009）を
 *   表示（AiForms の `IsAndroidSpinnerStyle = true` 相当）
 *
 * クロスプラットフォーム命名規約：iOS UI 層にも同名の `DatePickerUIStyle` が存在するが、
 * ケースは別物（iOS: `wheels` / `calendar`）。型名は対称、ケースはプラットフォーム固有 UI を
 * 反映する設計。これにより `DatePickerCell.uiStyle` というプロパティ名を両プラットフォームで
 * 共通化できる。
 */
public enum class DatePickerUIStyle {
    /** Material Design スタイル（カレンダー選択面、既定）。 */
    Material,

    /** Spinner スタイル（ボトムシート + 年/月/日の3連ホイール）。 */
    Spinner,
}
