using System;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.Sample.Maui;

/// <summary>
/// 起動直後のルートメニュー。
/// </summary>
/// <remarks>
/// 項目文言・並び順・区分はすべて <see cref="SampleScreen"/> の定義を参照するため、
/// 遷移先ページのタイトルと同一定義になる。
/// </remarks>
public class MenuPage : ContentPage
{
    /// <summary>ルートメニューを作る。</summary>
    public MenuPage()
    {
        Title = "KsSettingsView Sample";

        CollectionView list = new()
        {
            IsGrouped = true,
            ItemsSource = SampleScreen.Groups,
            SelectionMode = SelectionMode.Single,
            GroupHeaderTemplate = new DataTemplate(CreateGroupHeader),
            ItemTemplate = new DataTemplate(CreateItem),
        };
        list.SelectionChanged += OnSelectionChanged;

        Content = list;
    }

    private static Label CreateGroupHeader()
    {
        Label header = new()
        {
            FontAttributes = FontAttributes.Bold,
            FontSize = 13,
            TextColor = Colors.Gray,
            Margin = new Thickness(16, 16, 16, 8),
        };
        header.SetBinding(Label.TextProperty, static (SampleScreenGroup group) => group.Name);
        return header;
    }

    private static Label CreateItem()
    {
        Label item = new()
        {
            FontSize = 17,
            Margin = new Thickness(16, 14),
        };
        item.SetBinding(Label.TextProperty, static (SampleScreen screen) => screen.Title);
        return item;
    }

    private async void OnSelectionChanged(object? sender, SelectionChangedEventArgs e)
    {
        if (sender is not CollectionView list || e.CurrentSelection.Count == 0)
        {
            return;
        }

        SampleScreen screen = (SampleScreen)e.CurrentSelection[0];

        // 押した行がハイライトされたまま遷移しないよう、push の前に選択状態を解除する。
        // 解除で再入したハンドラは、選択なしとして冒頭で戻る。
        list.SelectedItem = null;

        await Navigation.PushAsync(screen.CreateTitledPage());
    }
}
