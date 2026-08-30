using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Controls.Shapes;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.Sample.Maui.Views;

/// <summary>
/// 「ドット + タイトル / サブタイトル + trailing スロット」の共通行 View。
/// </summary>
/// <remarks>
/// CustomCell はライブラリ側が行の内側マージンを持たないため、標準 Cell と横位置を揃える 16 の
/// 余白はこの行 View が持つ。
/// 対応する定義は samples/ios/KsSettingsViewSample/CustomCellDemoView.swift の <c>SampleAccentRow</c> と
/// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/CustomCellDemoScreen.kt
/// の <c>SampleAccentRow</c>。
/// </remarks>
public class SampleAccentRow : Grid
{
    /// <summary><see cref="DotColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty DotColorProperty = BindableProperty.Create(
        nameof(DotColor),
        typeof(Color),
        typeof(SampleAccentRow),
        default(Color),
        propertyChanged: static (bindable, _, newValue) => ((SampleAccentRow)bindable).ApplyDotColor((Color?)newValue));

    /// <summary><see cref="Title"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TitleProperty = BindableProperty.Create(
        nameof(Title),
        typeof(string),
        typeof(SampleAccentRow),
        string.Empty,
        propertyChanged: static (bindable, _, newValue) =>
            ((SampleAccentRow)bindable)._title.Text = (string?)newValue ?? string.Empty);

    /// <summary><see cref="Subtitle"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty SubtitleProperty = BindableProperty.Create(
        nameof(Subtitle),
        typeof(string),
        typeof(SampleAccentRow),
        string.Empty,
        propertyChanged: static (bindable, _, newValue) =>
            ((SampleAccentRow)bindable)._subtitle.Text = (string?)newValue ?? string.Empty);

    /// <summary><see cref="TrailingPadding"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TrailingPaddingProperty = BindableProperty.Create(
        nameof(TrailingPadding),
        typeof(double),
        typeof(SampleAccentRow),
        16.0,
        propertyChanged: static (bindable, _, newValue) =>
            ((SampleAccentRow)bindable).Padding = new Thickness(16, 10, (double)newValue, 10));

    /// <summary><see cref="Trailing"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TrailingProperty = BindableProperty.Create(
        nameof(Trailing),
        typeof(View),
        typeof(SampleAccentRow),
        default(View),
        propertyChanged: static (bindable, oldValue, newValue) =>
            ((SampleAccentRow)bindable).ApplyTrailing(oldValue as View, newValue as View));

    private readonly Ellipse _dot;
    private readonly Label _title;
    private readonly Label _subtitle;

    /// <summary>共通行 View を作る。</summary>
    public SampleAccentRow()
    {
        ColumnDefinitions =
        [
            new ColumnDefinition(GridLength.Auto),
            new ColumnDefinition(GridLength.Star),
            new ColumnDefinition(GridLength.Auto),
        ];
        Padding = new Thickness(16, 10);

        _dot = new Ellipse
        {
            WidthRequest = 12,
            HeightRequest = 12,
            Margin = new Thickness(0, 0, 12, 0),
            VerticalOptions = LayoutOptions.Center,
            IsVisible = false,
        };
        SetColumn((BindableObject)_dot, 0);
        Add(_dot);

        _title = new Label { FontSize = 16, TextColor = SampleTheme.MauiDeepText };
        _subtitle = new Label { FontSize = 12, TextColor = SampleTheme.MauiFooterText };
        VerticalStackLayout texts = new()
        {
            Spacing = 2,
            VerticalOptions = LayoutOptions.Center,
        };
        texts.Add(_title);
        texts.Add(_subtitle);
        SetColumn((BindableObject)texts, 1);
        Add(texts);
    }

    /// <summary>行頭のドットの色。null でドットを表示しない。</summary>
    public Color? DotColor
    {
        get => (Color?)GetValue(DotColorProperty);
        set => SetValue(DotColorProperty, value);
    }

    /// <summary>1 行目に表示する文字列。</summary>
    public string Title
    {
        get => (string)GetValue(TitleProperty);
        set => SetValue(TitleProperty, value);
    }

    /// <summary>2 行目に表示する文字列。</summary>
    public string Subtitle
    {
        get => (string)GetValue(SubtitleProperty);
        set => SetValue(SubtitleProperty, value);
    }

    /// <summary>行の右端の余白。</summary>
    /// <remarks>Disclosure Indicator を出す行では、その分を詰めるために小さくする。</remarks>
    public double TrailingPadding
    {
        get => (double)GetValue(TrailingPaddingProperty);
        set => SetValue(TrailingPaddingProperty, value);
    }

    /// <summary>行末に置く View。</summary>
    public View? Trailing
    {
        get => (View?)GetValue(TrailingProperty);
        set => SetValue(TrailingProperty, value);
    }

    private void ApplyDotColor(Color? color)
    {
        _dot.Fill = color is null ? Brush.Transparent : new SolidColorBrush(color);
        _dot.IsVisible = color is not null;
    }

    private void ApplyTrailing(View? oldView, View? newView)
    {
        if (oldView is not null)
        {
            Remove(oldView);
        }

        if (newView is not null)
        {
            newView.VerticalOptions = LayoutOptions.Center;
            SetColumn((BindableObject)newView, 2);
            Add(newView);
        }
    }
}
