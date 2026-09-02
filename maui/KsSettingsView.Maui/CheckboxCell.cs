using KsSettingsView.Internals;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsSettingsView;

/// <summary>独立した二値をチェックボックスで表す Cell。</summary>
public class CheckboxCell : CellBase
{
    /// <summary><see cref="Checked"/> のバッキングプロパティ。</summary>
    /// <remarks>ユーザー操作で値が変わるため、既定の binding mode は TwoWay。</remarks>
    public static readonly BindableProperty CheckedProperty = BindableProperty.Create(
        nameof(Checked),
        typeof(bool),
        typeof(CheckboxCell),
        false,
        defaultBindingMode: BindingMode.TwoWay);

    /// <summary><see cref="ValueText"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ValueTextProperty = BindableProperty.Create(
        nameof(ValueText),
        typeof(string),
        typeof(CheckboxCell),
        default(string));

    /// <summary><see cref="AccentColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty AccentColorProperty = BindableProperty.Create(
        nameof(AccentColor),
        typeof(Color),
        typeof(CheckboxCell),
        default(Color));

    /// <summary>現在のチェック状態。</summary>
    public bool Checked
    {
        get => (bool)GetValue(CheckedProperty);
        set => SetValue(CheckedProperty, value);
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
        => CreateSnapshot<KsCheckboxCellSnapshot>() with
        {
            ValueText = ValueText,
            IsChecked = Checked,
            AccentColor = KsWireValues.Color(AccentColor),
        };

    /// <inheritdoc/>
    internal override bool AffectsSnapshot(string? propertyName)
        => propertyName is nameof(ValueText) or nameof(Checked) or nameof(AccentColor)
            || base.AffectsSnapshot(propertyName);
}
