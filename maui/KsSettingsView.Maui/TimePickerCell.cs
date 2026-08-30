using System;
using KsSettingsView.Maui.Internals;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.Maui;

/// <summary>
/// 時刻を選ぶ Cell。
/// </summary>
/// <remarks><see cref="Time"/> はタイムゾーンを持たない壁時計の時刻を表す。</remarks>
public class TimePickerCell : CellBase
{
    /// <summary><see cref="ValueText"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ValueTextProperty = BindableProperty.Create(
        nameof(ValueText),
        typeof(string),
        typeof(TimePickerCell),
        default(string));

    /// <summary><see cref="Time"/> のバッキングプロパティ。</summary>
    /// <remarks>ユーザー操作で値が変わるため、既定の binding mode は TwoWay。</remarks>
    public static readonly BindableProperty TimeProperty = BindableProperty.Create(
        nameof(Time),
        typeof(TimeSpan),
        typeof(TimePickerCell),
        default(TimeSpan),
        defaultBindingMode: BindingMode.TwoWay);

    /// <summary><see cref="Format"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty FormatProperty = BindableProperty.Create(
        nameof(Format),
        typeof(string),
        typeof(TimePickerCell),
        default(string));

    /// <summary><see cref="Is24Hour"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty Is24HourProperty = BindableProperty.Create(
        nameof(Is24Hour),
        typeof(bool),
        typeof(TimePickerCell),
        true);

    /// <summary><see cref="PickerTitle"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty PickerTitleProperty = BindableProperty.Create(
        nameof(PickerTitle),
        typeof(string),
        typeof(TimePickerCell),
        default(string));

    /// <summary><see cref="AccentColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty AccentColorProperty = BindableProperty.Create(
        nameof(AccentColor),
        typeof(Color),
        typeof(TimePickerCell),
        default(Color));

    /// <summary>行の右側に表示する値文字列。null なら現在の時刻から作られる。</summary>
    public string? ValueText
    {
        get => (string?)GetValue(ValueTextProperty);
        set => SetValue(ValueTextProperty, value);
    }

    /// <summary>現在の時刻。</summary>
    public TimeSpan Time
    {
        get => (TimeSpan)GetValue(TimeProperty);
        set => SetValue(TimeProperty, value);
    }

    /// <summary>時刻の表示フォーマット。null で Native 既定。</summary>
    /// <remarks>書式の解釈は platform の日時フォーマッタに従う。</remarks>
    public string? Format
    {
        get => (string?)GetValue(FormatProperty);
        set => SetValue(FormatProperty, value);
    }

    /// <summary>選択面の時制。true で 24時間制、false で 12時間制。</summary>
    /// <remarks>
    /// 選択面の時制はこの値だけで決まり、<see cref="Format"/> や端末の地域・24時間表示設定は
    /// 関与しない (core/ADR-0028)。
    /// </remarks>
    public bool Is24Hour
    {
        get => (bool)GetValue(Is24HourProperty);
        set => SetValue(Is24HourProperty, value);
    }

    /// <summary>選択面のタイトル。null でタイトルなし。</summary>
    public string? PickerTitle
    {
        get => (string?)GetValue(PickerTitleProperty);
        set => SetValue(PickerTitleProperty, value);
    }

    /// <summary>この行の強調表示の色。null で既定スタイルを継承。</summary>
    public Color? AccentColor
    {
        get => (Color?)GetValue(AccentColorProperty);
        set => SetValue(AccentColorProperty, value);
    }

    /// <inheritdoc/>
    internal override KsCellSnapshot CreateSnapshot()
        => CreateSnapshot<KsTimePickerCellSnapshot>() with
        {
            ValueText = ValueText,
            Time = KsWireValues.Time(Time),
            Format = Format,
            Is24Hour = Is24Hour,
            PickerTitle = PickerTitle,
            AccentColor = KsWireValues.Color(AccentColor),
        };

    /// <inheritdoc/>
    internal override bool AffectsSnapshot(string? propertyName) => propertyName is
        nameof(ValueText) or nameof(Time) or nameof(Format) or nameof(Is24Hour)
        or nameof(PickerTitle) or nameof(AccentColor)
        || base.AffectsSnapshot(propertyName);
}
