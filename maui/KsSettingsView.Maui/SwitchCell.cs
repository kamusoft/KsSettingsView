using KsSettingsView.Maui.Internals;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.Maui;

/// <summary>ON/OFF スイッチを持つ Cell。</summary>
public class SwitchCell : CellBase
{
    /// <summary><see cref="On"/> のバッキングプロパティ。</summary>
    /// <remarks>ユーザー操作で値が変わるため、既定の binding mode は TwoWay。</remarks>
    public static readonly BindableProperty OnProperty = BindableProperty.Create(
        nameof(On),
        typeof(bool),
        typeof(SwitchCell),
        false,
        defaultBindingMode: BindingMode.TwoWay);

    /// <summary><see cref="ValueText"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ValueTextProperty = BindableProperty.Create(
        nameof(ValueText),
        typeof(string),
        typeof(SwitchCell),
        default(string));

    /// <summary><see cref="AccentColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty AccentColorProperty = BindableProperty.Create(
        nameof(AccentColor),
        typeof(Color),
        typeof(SwitchCell),
        default(Color));

    /// <summary>現在の ON/OFF 値。</summary>
    public bool On
    {
        get => (bool)GetValue(OnProperty);
        set => SetValue(OnProperty, value);
    }

    /// <summary>行の右側に表示する値文字列。null で非表示。</summary>
    public string? ValueText
    {
        get => (string?)GetValue(ValueTextProperty);
        set => SetValue(ValueTextProperty, value);
    }

    /// <summary>この行の強調表示の色。null で既定スタイルを継承。</summary>
    public Color? AccentColor
    {
        get => (Color?)GetValue(AccentColorProperty);
        set => SetValue(AccentColorProperty, value);
    }

    /// <inheritdoc/>
    internal override KsCellSnapshot CreateSnapshot()
        => CreateSnapshot<KsSwitchCellSnapshot>() with
        {
            ValueText = ValueText,
            IsOn = On,
            AccentColor = KsWireValues.Color(AccentColor),
        };

    /// <inheritdoc/>
    internal override bool AffectsSnapshot(string? propertyName)
        => propertyName is nameof(ValueText) or nameof(On) or nameof(AccentColor)
            || base.AffectsSnapshot(propertyName);
}
