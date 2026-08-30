using KsSettingsView.Maui.Internals;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.Maui;

/// <summary>一定の刻みで数値を選ぶ Cell。</summary>
public class NumberPickerCell : CellBase
{
    /// <summary><see cref="ValueText"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ValueTextProperty = BindableProperty.Create(
        nameof(ValueText),
        typeof(string),
        typeof(NumberPickerCell),
        default(string));

    /// <summary><see cref="Min"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty MinProperty = BindableProperty.Create(
        nameof(Min),
        typeof(int),
        typeof(NumberPickerCell),
        0);

    /// <summary><see cref="Max"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty MaxProperty = BindableProperty.Create(
        nameof(Max),
        typeof(int),
        typeof(NumberPickerCell),
        100);

    /// <summary><see cref="Step"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty StepProperty = BindableProperty.Create(
        nameof(Step),
        typeof(int),
        typeof(NumberPickerCell),
        1);

    /// <summary><see cref="Number"/> のバッキングプロパティ。</summary>
    /// <remarks>ユーザー操作で値が変わるため、既定の binding mode は TwoWay。</remarks>
    public static readonly BindableProperty NumberProperty = BindableProperty.Create(
        nameof(Number),
        typeof(int),
        typeof(NumberPickerCell),
        0,
        defaultBindingMode: BindingMode.TwoWay);

    /// <summary><see cref="Unit"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty UnitProperty = BindableProperty.Create(
        nameof(Unit),
        typeof(string),
        typeof(NumberPickerCell),
        string.Empty);

    /// <summary><see cref="PickerTitle"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty PickerTitleProperty = BindableProperty.Create(
        nameof(PickerTitle),
        typeof(string),
        typeof(NumberPickerCell),
        default(string));

    /// <summary><see cref="AccentColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty AccentColorProperty = BindableProperty.Create(
        nameof(AccentColor),
        typeof(Color),
        typeof(NumberPickerCell),
        default(Color));

    /// <summary>行の右側に表示する値文字列。null なら現在の値から作られる。</summary>
    public string? ValueText
    {
        get => (string?)GetValue(ValueTextProperty);
        set => SetValue(ValueTextProperty, value);
    }

    /// <summary>選べる最小値。</summary>
    public int Min
    {
        get => (int)GetValue(MinProperty);
        set => SetValue(MinProperty, value);
    }

    /// <summary>選べる最大値。</summary>
    public int Max
    {
        get => (int)GetValue(MaxProperty);
        set => SetValue(MaxProperty, value);
    }

    /// <summary>選択の刻み幅。</summary>
    public int Step
    {
        get => (int)GetValue(StepProperty);
        set => SetValue(StepProperty, value);
    }

    /// <summary>現在の値。</summary>
    public int Number
    {
        get => (int)GetValue(NumberProperty);
        set => SetValue(NumberProperty, value);
    }

    /// <summary>値に添える単位文字列。空文字列で単位なし。</summary>
    public string Unit
    {
        get => (string)GetValue(UnitProperty);
        set => SetValue(UnitProperty, value);
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
        => CreateSnapshot<KsNumberPickerCellSnapshot>() with
        {
            ValueText = ValueText,
            Min = Min,
            Max = Max,
            Step = Step,
            Number = Number,
            Unit = Unit ?? string.Empty,
            PickerTitle = PickerTitle,
            AccentColor = KsWireValues.Color(AccentColor),
        };

    /// <inheritdoc/>
    internal override bool AffectsSnapshot(string? propertyName) => propertyName is
        nameof(ValueText) or nameof(Min) or nameof(Max) or nameof(Step) or nameof(Number)
        or nameof(Unit) or nameof(PickerTitle) or nameof(AccentColor)
        || base.AffectsSnapshot(propertyName);
}
