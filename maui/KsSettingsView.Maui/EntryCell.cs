using KsSettingsView.Maui.Internals;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.Maui;

/// <summary>
/// テキスト入力欄を持つ Cell。
/// </summary>
/// <remarks>
/// 入力 control 自身が値を表示するため、値の表示口は <see cref="ValueText"/> 1 本だけを持つ。
/// </remarks>
public class EntryCell : CellBase
{
    /// <summary><see cref="ValueText"/> のバッキングプロパティ。</summary>
    /// <remarks>ユーザー操作で値が変わるため、既定の binding mode は TwoWay。</remarks>
    public static readonly BindableProperty ValueTextProperty = BindableProperty.Create(
        nameof(ValueText),
        typeof(string),
        typeof(EntryCell),
        string.Empty,
        defaultBindingMode: BindingMode.TwoWay);

    /// <summary><see cref="Placeholder"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty PlaceholderProperty = BindableProperty.Create(
        nameof(Placeholder),
        typeof(string),
        typeof(EntryCell),
        default(string));

    /// <summary><see cref="PlaceholderColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty PlaceholderColorProperty = BindableProperty.Create(
        nameof(PlaceholderColor),
        typeof(Color),
        typeof(EntryCell),
        default(Color));

    /// <summary><see cref="Keyboard"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty KeyboardProperty = BindableProperty.Create(
        nameof(Keyboard),
        typeof(Microsoft.Maui.Keyboard),
        typeof(EntryCell),
        default(Microsoft.Maui.Keyboard));

    /// <summary><see cref="IsPassword"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty IsPasswordProperty = BindableProperty.Create(
        nameof(IsPassword),
        typeof(bool),
        typeof(EntryCell),
        false);

    /// <summary><see cref="TextAlignment"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TextAlignmentProperty = BindableProperty.Create(
        nameof(TextAlignment),
        typeof(TextAlignment?),
        typeof(EntryCell),
        default(TextAlignment?));

    /// <summary><see cref="MaxLength"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty MaxLengthProperty = BindableProperty.Create(
        nameof(MaxLength),
        typeof(int?),
        typeof(EntryCell),
        default(int?));

    /// <summary><see cref="AccentColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty AccentColorProperty = BindableProperty.Create(
        nameof(AccentColor),
        typeof(Color),
        typeof(EntryCell),
        default(Color));

    /// <summary>入力欄の現在の値。</summary>
    public string ValueText
    {
        get => (string)GetValue(ValueTextProperty);
        set => SetValue(ValueTextProperty, value);
    }

    /// <summary>入力欄が空のときに表示する文字列。null で非表示。</summary>
    public string? Placeholder
    {
        get => (string?)GetValue(PlaceholderProperty);
        set => SetValue(PlaceholderProperty, value);
    }

    /// <summary>プレースホルダの文字色。null で既定スタイルを継承。</summary>
    /// <remarks>
    /// 未指定のときは <see cref="SettingsView.CellPlaceholderColor"/>、それも未指定なら
    /// 各 platform の既定色へ解決される。
    /// </remarks>
    public Color? PlaceholderColor
    {
        get => (Color?)GetValue(PlaceholderColorProperty);
        set => SetValue(PlaceholderColorProperty, value);
    }

    /// <summary>入力に使うキーボード。null で各 platform の既定。</summary>
    /// <remarks>
    /// 標準キーボード以外を指定した場合は既定のキーボードとして扱う。対応の取れないキーボードは
    /// Native 側の既定へ倒れる。
    /// </remarks>
    public Keyboard? Keyboard
    {
        get => (Keyboard?)GetValue(KeyboardProperty);
        set => SetValue(KeyboardProperty, value);
    }

    /// <summary>入力値をマスクするかどうか。</summary>
    public bool IsPassword
    {
        get => (bool)GetValue(IsPasswordProperty);
        set => SetValue(IsPasswordProperty, value);
    }

    /// <summary>入力値の揃え位置。null で Native 既定 (末尾寄せ)。</summary>
    public TextAlignment? TextAlignment
    {
        get => (TextAlignment?)GetValue(TextAlignmentProperty);
        set => SetValue(TextAlignmentProperty, value);
    }

    /// <summary>入力できる最大文字数。null で無制限。</summary>
    public int? MaxLength
    {
        get => (int?)GetValue(MaxLengthProperty);
        set => SetValue(MaxLengthProperty, value);
    }

    /// <summary>この行の強調表示の色。null で既定スタイルを継承。</summary>
    public Color? AccentColor
    {
        get => (Color?)GetValue(AccentColorProperty);
        set => SetValue(AccentColorProperty, value);
    }

    /// <inheritdoc/>
    internal override KsCellSnapshot CreateSnapshot()
        => CreateSnapshot<KsEntryCellSnapshot>() with
        {
            Text = ValueText ?? string.Empty,
            Placeholder = Placeholder,
            PlaceholderColor = KsWireValues.Color(PlaceholderColor),
            Keyboard = KsWireValues.Keyboard(Keyboard),
            IsPassword = IsPassword,
            TextAlignment = TextAlignment,
            MaxLength = MaxLength,
            AccentColor = KsWireValues.Color(AccentColor),
        };

    /// <inheritdoc/>
    internal override bool AffectsSnapshot(string? propertyName) => propertyName is
        nameof(ValueText) or nameof(Placeholder) or nameof(PlaceholderColor) or nameof(Keyboard)
        or nameof(IsPassword) or nameof(TextAlignment) or nameof(MaxLength) or nameof(AccentColor)
        || base.AffectsSnapshot(propertyName);
}
