using KsSettingsView.Internals;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsSettingsView;

/// <summary>
/// 同一グループ内で 1 つだけ選択される Cell。
/// </summary>
/// <remarks>
/// <see cref="Value"/> と <see cref="SelectedValue"/> が一致する行が選択表示になる。
/// ユーザーが行を選ぶと、同じ <see cref="GroupId"/> を持つ全ての行の <see cref="SelectedValue"/> が
/// 新しい値になる。
/// </remarks>
public class RadioCell : CellBase
{
    /// <summary><see cref="GroupId"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty GroupIdProperty = BindableProperty.Create(
        nameof(GroupId),
        typeof(string),
        typeof(RadioCell),
        string.Empty);

    /// <summary><see cref="Value"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ValueProperty = BindableProperty.Create(
        nameof(Value),
        typeof(string),
        typeof(RadioCell),
        string.Empty);

    /// <summary><see cref="SelectedValue"/> のバッキングプロパティ。</summary>
    /// <remarks>ユーザー操作で値が変わるため、既定の binding mode は TwoWay。</remarks>
    public static readonly BindableProperty SelectedValueProperty = BindableProperty.Create(
        nameof(SelectedValue),
        typeof(string),
        typeof(RadioCell),
        string.Empty,
        defaultBindingMode: BindingMode.TwoWay);

    /// <summary><see cref="ValueText"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ValueTextProperty = BindableProperty.Create(
        nameof(ValueText),
        typeof(string),
        typeof(RadioCell),
        default(string));

    /// <summary><see cref="AccentColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty AccentColorProperty = BindableProperty.Create(
        nameof(AccentColor),
        typeof(Color),
        typeof(RadioCell),
        default(Color));

    /// <summary>同一選択グループの識別子。</summary>
    public string GroupId
    {
        get => (string)GetValue(GroupIdProperty);
        set => SetValue(GroupIdProperty, value);
    }

    /// <summary>この行が表す値。</summary>
    public string Value
    {
        get => (string)GetValue(ValueProperty);
        set => SetValue(ValueProperty, value);
    }

    /// <summary>グループ内の現在の選択値。</summary>
    public string SelectedValue
    {
        get => (string)GetValue(SelectedValueProperty);
        set => SetValue(SelectedValueProperty, value);
    }

    /// <summary>行の右側に表示する値文字列。null で非表示。</summary>
    /// <remarks>選択の判定に使う <see cref="Value"/> とは別で、表示だけに使う。</remarks>
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
        => CreateSnapshot<KsRadioCellSnapshot>() with
        {
            ValueText = ValueText,
            GroupId = GroupId ?? string.Empty,
            Value = Value ?? string.Empty,
            SelectedValue = SelectedValue ?? string.Empty,
            AccentColor = KsWireValues.Color(AccentColor),
        };

    /// <inheritdoc/>
    internal override bool AffectsSnapshot(string? propertyName) => propertyName is
        nameof(ValueText) or nameof(GroupId) or nameof(Value) or nameof(SelectedValue)
        or nameof(AccentColor)
        || base.AffectsSnapshot(propertyName);
}
