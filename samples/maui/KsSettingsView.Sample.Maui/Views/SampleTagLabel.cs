using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Controls.Shapes;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.Sample.Maui.Views;

/// <summary>
/// バッジ / ピル用の角丸ラベル。
/// </summary>
/// <remarks>
/// 対応する定義は samples/ios/KsSettingsViewSample/CustomCellDemoView.swift の <c>SampleTagLabel</c> と
/// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/CustomCellDemoScreen.kt
/// の <c>SampleTagLabel</c>。
/// </remarks>
public class SampleTagLabel : Border
{
    /// <summary><see cref="Text"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TextProperty = BindableProperty.Create(
        nameof(Text),
        typeof(string),
        typeof(SampleTagLabel),
        string.Empty,
        propertyChanged: static (bindable, _, newValue) =>
            ((SampleTagLabel)bindable)._label.Text = (string?)newValue ?? string.Empty);

    /// <summary><see cref="TagBackground"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TagBackgroundProperty = BindableProperty.Create(
        nameof(TagBackground),
        typeof(Color),
        typeof(SampleTagLabel),
        SampleTheme.DemoPillBackground,
        propertyChanged: static (bindable, _, newValue) =>
            ((SampleTagLabel)bindable).BackgroundColor = (Color?)newValue);

    /// <summary><see cref="TagForeground"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TagForegroundProperty = BindableProperty.Create(
        nameof(TagForeground),
        typeof(Color),
        typeof(SampleTagLabel),
        SampleTheme.MauiHeaderText,
        propertyChanged: static (bindable, _, newValue) =>
            ((SampleTagLabel)bindable)._label.TextColor = (Color?)newValue);

    private readonly Label _label;

    /// <summary>角丸ラベルを作る。</summary>
    public SampleTagLabel()
    {
        _label = new Label
        {
            FontSize = 12,
            FontAttributes = FontAttributes.Bold,
            TextColor = SampleTheme.MauiHeaderText,
        };

        StrokeThickness = 0;
        StrokeShape = new RoundRectangle { CornerRadius = 10 };
        BackgroundColor = SampleTheme.DemoPillBackground;
        Padding = new Thickness(10, 3);
        VerticalOptions = LayoutOptions.Center;
        Content = _label;
    }

    /// <summary>ラベルに表示する文字列。</summary>
    public string Text
    {
        get => (string)GetValue(TextProperty);
        set => SetValue(TextProperty, value);
    }

    /// <summary>角丸の塗り色。</summary>
    public Color TagBackground
    {
        get => (Color)GetValue(TagBackgroundProperty);
        set => SetValue(TagBackgroundProperty, value);
    }

    /// <summary>文字色。</summary>
    public Color TagForeground
    {
        get => (Color)GetValue(TagForegroundProperty);
        set => SetValue(TagForegroundProperty, value);
    }
}
