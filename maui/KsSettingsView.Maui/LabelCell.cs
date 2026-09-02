using KsSettingsView.Internals;
using Microsoft.Maui.Controls;

namespace KsSettingsView;

/// <summary>
/// 値を読み取り専用で表示する Cell。
/// </summary>
public class LabelCell : CellBase
{
    /// <summary><see cref="ValueText"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ValueTextProperty = BindableProperty.Create(
        nameof(ValueText),
        typeof(string),
        typeof(LabelCell),
        default(string));

    /// <summary>行の右側に表示する値文字列。null で非表示。</summary>
    public string? ValueText
    {
        get => (string?)GetValue(ValueTextProperty);
        set => SetValue(ValueTextProperty, value);
    }

    /// <inheritdoc/>
    internal override KsCellSnapshot CreateSnapshot()
        => CreateSnapshot<KsLabelCellSnapshot>() with { ValueText = ValueText };

    /// <inheritdoc/>
    internal override bool AffectsSnapshot(string? propertyName)
        => propertyName == nameof(ValueText) || base.AffectsSnapshot(propertyName);
}
