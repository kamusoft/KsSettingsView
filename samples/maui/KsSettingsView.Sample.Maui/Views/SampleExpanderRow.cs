using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Controls.Shapes;

namespace KsSettingsView.Sample.Maui.Views;

/// <summary>
/// 展開 / 折りたたみで高さが変わる行 View。
/// </summary>
/// <remarks>
/// 見出し部が行の内容に含まれるタップ可能要素であり、行そのもののタップ動作は使わない
/// (タップ動作を持たない CustomCell は内容の中の操作を妨げない)。
/// 対応する定義は samples/ios/KsSettingsViewSample/CustomCellDemoView.swift の <c>SampleExpanderRow</c> と
/// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/CustomCellDemoScreen.kt
/// の <c>SampleExpanderRow</c>。
/// </remarks>
public class SampleExpanderRow : VerticalStackLayout
{
    /// <summary><see cref="Title"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TitleProperty = BindableProperty.Create(
        nameof(Title),
        typeof(string),
        typeof(SampleExpanderRow),
        string.Empty,
        propertyChanged: static (bindable, _, newValue) =>
            ((SampleExpanderRow)bindable)._title.Text = (string?)newValue ?? string.Empty);

    /// <summary><see cref="Body"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty BodyProperty = BindableProperty.Create(
        nameof(Body),
        typeof(string),
        typeof(SampleExpanderRow),
        string.Empty,
        propertyChanged: static (bindable, _, newValue) =>
            ((SampleExpanderRow)bindable)._body.Text = (string?)newValue ?? string.Empty);

    /// <summary><see cref="IsExpanded"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty IsExpandedProperty = BindableProperty.Create(
        nameof(IsExpanded),
        typeof(bool),
        typeof(SampleExpanderRow),
        false,
        propertyChanged: static (bindable, _, newValue) => ((SampleExpanderRow)bindable).ApplyExpanded((bool)newValue));

    private readonly Label _marker;
    private readonly Label _title;
    private readonly Label _body;
    private readonly Border _bodyBlock;

    /// <summary>展開 / 折りたたみ行を作る。</summary>
    public SampleExpanderRow()
    {
        Padding = new Thickness(16, 10);
        Spacing = 0;

        _marker = new Label
        {
            Text = "▶",
            FontSize = 13,
            TextColor = SampleTheme.MauiAccent,
            Margin = new Thickness(0, 0, 8, 0),
            VerticalOptions = LayoutOptions.Center,
        };
        _title = new Label
        {
            FontSize = 16,
            TextColor = SampleTheme.MauiDeepText,
            VerticalOptions = LayoutOptions.Center,
        };

        HorizontalStackLayout header = new();
        header.Add(_marker);
        header.Add(_title);

        TapGestureRecognizer toggle = new();
        toggle.Tapped += (_, _) => IsExpanded = !IsExpanded;
        header.GestureRecognizers.Add(toggle);
        Add(header);

        _body = new Label
        {
            FontSize = 13,
            LineHeight = 1.6,
            TextColor = SampleTheme.DemoExpandText,
        };
        _bodyBlock = new Border
        {
            StrokeThickness = 0,
            StrokeShape = new RoundRectangle { CornerRadius = 8 },
            BackgroundColor = SampleTheme.DemoExpandBackground,
            Padding = new Thickness(12, 10),
            Margin = new Thickness(0, 8, 0, 0),
            IsVisible = false,
            Content = _body,
        };
        Add(_bodyBlock);
    }

    /// <summary>見出しに表示する文字列。</summary>
    public string Title
    {
        get => (string)GetValue(TitleProperty);
        set => SetValue(TitleProperty, value);
    }

    /// <summary>展開したときに現れる本文。</summary>
    public string Body
    {
        get => (string)GetValue(BodyProperty);
        set => SetValue(BodyProperty, value);
    }

    /// <summary>本文を展開しているかどうか。</summary>
    public bool IsExpanded
    {
        get => (bool)GetValue(IsExpandedProperty);
        set => SetValue(IsExpandedProperty, value);
    }

    private void ApplyExpanded(bool expanded)
    {
        _marker.Text = expanded ? "▼" : "▶";
        _bodyBlock.IsVisible = expanded;
    }
}
