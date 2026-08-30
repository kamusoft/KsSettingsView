using System;
using KsSettingsView.Maui.Internals;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.Maui;

/// <summary>
/// 日付を選ぶ Cell。
/// </summary>
/// <remarks>
/// <see cref="Date"/> はタイムゾーンを持たない壁時計の日付を表し、時刻部分は意味を持たない。
/// </remarks>
public class DatePickerCell : CellBase
{
    /// <summary><see cref="ValueText"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ValueTextProperty = BindableProperty.Create(
        nameof(ValueText),
        typeof(string),
        typeof(DatePickerCell),
        default(string));

    /// <summary><see cref="Date"/> のバッキングプロパティ。</summary>
    /// <remarks>ユーザー操作で値が変わるため、既定の binding mode は TwoWay。</remarks>
    public static readonly BindableProperty DateProperty = BindableProperty.Create(
        nameof(Date),
        typeof(DateTime),
        typeof(DatePickerCell),
        KsWireValues.DefaultDate,
        defaultBindingMode: BindingMode.TwoWay);

    /// <summary><see cref="MinimumDate"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty MinimumDateProperty = BindableProperty.Create(
        nameof(MinimumDate),
        typeof(DateTime?),
        typeof(DatePickerCell),
        default(DateTime?));

    /// <summary><see cref="MaximumDate"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty MaximumDateProperty = BindableProperty.Create(
        nameof(MaximumDate),
        typeof(DateTime?),
        typeof(DatePickerCell),
        default(DateTime?));

    /// <summary><see cref="Format"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty FormatProperty = BindableProperty.Create(
        nameof(Format),
        typeof(string),
        typeof(DatePickerCell),
        default(string));

    /// <summary><see cref="TodayText"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TodayTextProperty = BindableProperty.Create(
        nameof(TodayText),
        typeof(string),
        typeof(DatePickerCell),
        default(string));

    /// <summary><see cref="PickerTitle"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty PickerTitleProperty = BindableProperty.Create(
        nameof(PickerTitle),
        typeof(string),
        typeof(DatePickerCell),
        default(string));

    /// <summary><see cref="UIStyle"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty UIStyleProperty = BindableProperty.Create(
        nameof(UIStyle),
        typeof(DatePickerUIStyle?),
        typeof(DatePickerCell),
        default(DatePickerUIStyle?));

    /// <summary><see cref="AndroidButtonColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty AndroidButtonColorProperty = BindableProperty.Create(
        nameof(AndroidButtonColor),
        typeof(Color),
        typeof(DatePickerCell),
        default(Color));

    /// <summary><see cref="AccentColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty AccentColorProperty = BindableProperty.Create(
        nameof(AccentColor),
        typeof(Color),
        typeof(DatePickerCell),
        default(Color));

    /// <summary>行の右側に表示する値文字列。null なら現在の日付から作られる。</summary>
    public string? ValueText
    {
        get => (string?)GetValue(ValueTextProperty);
        set => SetValue(ValueTextProperty, value);
    }

    /// <summary>現在の日付。</summary>
    public DateTime Date
    {
        get => (DateTime)GetValue(DateProperty);
        set => SetValue(DateProperty, value);
    }

    /// <summary>選べる最小の日付。null で下限なし。</summary>
    public DateTime? MinimumDate
    {
        get => (DateTime?)GetValue(MinimumDateProperty);
        set => SetValue(MinimumDateProperty, value);
    }

    /// <summary>選べる最大の日付。null で上限なし。</summary>
    public DateTime? MaximumDate
    {
        get => (DateTime?)GetValue(MaximumDateProperty);
        set => SetValue(MaximumDateProperty, value);
    }

    /// <summary>日付の表示フォーマット。null で Native 既定。</summary>
    /// <remarks>書式の解釈は platform の日時フォーマッタに従う。</remarks>
    public string? Format
    {
        get => (string?)GetValue(FormatProperty);
        set => SetValue(FormatProperty, value);
    }

    /// <summary>今日へ移動する操作の表示文字列。null または空で非表示。</summary>
    public string? TodayText
    {
        get => (string?)GetValue(TodayTextProperty);
        set => SetValue(TodayTextProperty, value);
    }

    /// <summary>選択面のタイトル。null でタイトルなし。</summary>
    public string? PickerTitle
    {
        get => (string?)GetValue(PickerTitleProperty);
        set => SetValue(PickerTitleProperty, value);
    }

    /// <summary>選択面の形式。null で各 platform の既定。</summary>
    public DatePickerUIStyle? UIStyle
    {
        get => (DatePickerUIStyle?)GetValue(UIStyleProperty);
        set => SetValue(UIStyleProperty, value);
    }

    /// <summary>Android の選択面の OK / CANCEL 操作の色。null で Native 既定。</summary>
    /// <remarks>Android 固有の指定であり、他の platform では表示・挙動に影響しない。</remarks>
    public Color? AndroidButtonColor
    {
        get => (Color?)GetValue(AndroidButtonColorProperty);
        set => SetValue(AndroidButtonColorProperty, value);
    }

    /// <summary>この行の強調表示の色。null で既定スタイルを継承。</summary>
    public Color? AccentColor
    {
        get => (Color?)GetValue(AccentColorProperty);
        set => SetValue(AccentColorProperty, value);
    }

    /// <inheritdoc/>
    internal override KsCellSnapshot CreateSnapshot()
        => CreateSnapshot<KsDatePickerCellSnapshot>() with
        {
            ValueText = ValueText,
            Date = KsWireValues.Date(Date),
            MinDate = KsWireValues.OptionalDate(MinimumDate),
            MaxDate = KsWireValues.OptionalDate(MaximumDate),
            Format = Format,
            TodayText = TodayText,
            PickerTitle = PickerTitle,
            UIStyle = UIStyle,
            AndroidButtonColor = KsWireValues.Color(AndroidButtonColor),
            AccentColor = KsWireValues.Color(AccentColor),
        };

    /// <inheritdoc/>
    internal override bool AffectsSnapshot(string? propertyName) => propertyName is
        nameof(ValueText) or nameof(Date) or nameof(MinimumDate) or nameof(MaximumDate)
        or nameof(Format) or nameof(TodayText) or nameof(PickerTitle) or nameof(UIStyle)
        or nameof(AndroidButtonColor) or nameof(AccentColor)
        || base.AffectsSnapshot(propertyName);
}
