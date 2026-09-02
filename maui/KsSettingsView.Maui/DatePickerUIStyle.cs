namespace KsSettingsView;

/// <summary>
/// <see cref="DatePickerCell"/> の選択面の形式 (maui/ADR-0013)。
/// </summary>
/// <remarks>
/// 両 platform の形式を 1 つの列挙で表す。未指定 (null) のときは各 platform の既定に従う。
/// </remarks>
public enum DatePickerUIStyle
{
    /// <summary>カレンダー形式。iOS はカレンダー、Android は Material 形式で表示される。</summary>
    Calendar,

    /// <summary>ホイール形式。iOS はホイール、Android は Spinner 形式で表示される。</summary>
    Wheels,
}
