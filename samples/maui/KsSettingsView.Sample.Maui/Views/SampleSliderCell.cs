using System;
using KsSettingsView;
using Microsoft.Maui;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui.Views;

/// <summary>
/// ラベル + スライダー + 数値の 1 行を組み立てる <see cref="CustomCell"/> の派生クラス。
/// </summary>
/// <remarks>
/// 独自の Cell 型を新設しなくても、<see cref="CustomCell"/> を継承して内容を組み立てるクラスを
/// 1 つ用意すれば「アプリ固有の Cell」を再利用単位として切り出せる、という利用パターンを示す。
/// 別ファイルに置いてあるのは、その再利用性そのものを示すため。
/// 対応する定義は samples/ios/KsSettingsViewSample/SampleSliderCell.swift と
/// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleSliderCell.kt。
/// </remarks>
public class SampleSliderCell : CustomCell
{
    /// <summary><see cref="Label"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty LabelProperty = BindableProperty.Create(
        nameof(Label),
        typeof(string),
        typeof(SampleSliderCell),
        string.Empty,
        propertyChanged: static (bindable, _, newValue) =>
            ((SampleSliderCell)bindable)._label.Text = (string?)newValue ?? string.Empty);

    /// <summary><see cref="Value"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ValueProperty = BindableProperty.Create(
        nameof(Value),
        typeof(int),
        typeof(SampleSliderCell),
        0,
        BindingMode.TwoWay,
        propertyChanged: static (bindable, _, newValue) => ((SampleSliderCell)bindable).ApplyValue((int)newValue));

    private readonly Microsoft.Maui.Controls.Label _label;
    private readonly Microsoft.Maui.Controls.Label _valueText;
    private readonly Slider _slider;

    /// <summary>スライダー行を作る。</summary>
    public SampleSliderCell()
    {
        _label = new Microsoft.Maui.Controls.Label
        {
            FontSize = 16,
            TextColor = SampleTheme.MauiDeepText,
            WidthRequest = 64,
            VerticalOptions = LayoutOptions.Center,
        };

        _slider = new Slider
        {
            Minimum = 0,
            Maximum = 100,
            MinimumTrackColor = SampleTheme.MauiAccent,
            ThumbColor = SampleTheme.MauiAccent,
            VerticalOptions = LayoutOptions.Center,
        };

        _valueText = new Microsoft.Maui.Controls.Label
        {
            FontSize = 14,
            TextColor = SampleTheme.MauiFooterText,
            WidthRequest = 40,
            HorizontalTextAlignment = TextAlignment.End,
            VerticalOptions = LayoutOptions.Center,
            Text = "0",
        };

        // ドラッグ中は表示だけを追従させ、確定したときにプロパティへ書き戻す。
        _slider.ValueChanged += (_, e) => _valueText.Text = ((int)Math.Round(e.NewValue)).ToString();
        _slider.DragCompleted += (_, _) => Value = (int)Math.Round(_slider.Value);

        Grid row = new()
        {
            ColumnDefinitions =
            [
                new ColumnDefinition(GridLength.Auto),
                new ColumnDefinition(GridLength.Star),
                new ColumnDefinition(GridLength.Auto),
            ],
            ColumnSpacing = 12,
            Padding = new Thickness(16, 10),
        };
        row.Add(_label, 0);
        row.Add(_slider, 1);
        row.Add(_valueText, 2);

        Content = row;
    }

    /// <summary>行頭に表示するラベル。</summary>
    public string Label
    {
        get => (string)GetValue(LabelProperty);
        set => SetValue(LabelProperty, value);
    }

    /// <summary>0〜100 のスライダーの値。</summary>
    public int Value
    {
        get => (int)GetValue(ValueProperty);
        set => SetValue(ValueProperty, value);
    }

    private void ApplyValue(int value)
    {
        _slider.Value = value;
        _valueText.Text = value.ToString();
    }
}
