using System;
using System.Collections.Generic;
using System.Linq;
using KsSettingsView.Sample.Maui.ViewModels;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.Sample.Maui;

/// <summary>
/// 起動直後のルートメニュー。
/// </summary>
/// <remarks>
/// 先頭に外観の項目群を置き、続いて画面の区分ごとの項目群を並べる。項目文言・並び順・区分は
/// すべて <see cref="SampleAppearances"/> と <see cref="SampleScreen"/> の定義を参照するため、
/// 外観の文言は 3 platform 共通、画面の文言は遷移先ページのタイトルと同一定義になる。
/// </remarks>
public class MenuPage : ContentPage
{
    private readonly IReadOnlyList<MenuRow> _appearanceRows;

    /// <summary>ルートメニューを作る。</summary>
    public MenuPage()
    {
        Title = "KsSettingsView Sample";

        _appearanceRows =
        [
            .. SampleAppearances.All.Select(static appearance => new MenuRow(appearance)),
        ];
        MarkSelectedAppearance(SampleAppearanceStore.Load());

        CollectionView list = new()
        {
            IsGrouped = true,
            ItemsSource = BuildGroups(),
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
        header.SetBinding(Label.TextProperty, static (MenuGroup group) => group.Name);
        return header;
    }

    private static Grid CreateItem()
    {
        Label title = new()
        {
            FontSize = 17,
            Margin = new Thickness(16, 14),
        };
        title.SetBinding(Label.TextProperty, static (MenuRow row) => row.Title);

        // 選択中の外観の印。色は指定せず、行の文言と同じく実効外観の既定色で描く。
        Label check = new()
        {
            Text = "✓",
            FontSize = 17,
            Margin = new Thickness(16, 14),
        };
        check.SetBinding(IsVisibleProperty, static (MenuRow row) => row.IsSelected);
        // 行の文言と合わせて選択中であることを読み上げる。
        SemanticProperties.SetDescription(check, SampleAppearances.SelectedLabel);

        Grid item = new()
        {
            ColumnDefinitions =
            {
                new ColumnDefinition(GridLength.Star),
                new ColumnDefinition(GridLength.Auto),
            },
        };
        item.Add(title);
        item.Add(check, column: 1);
        return item;
    }

    private IReadOnlyList<MenuGroup> BuildGroups() =>
    [
        new MenuGroup(SampleAppearances.SectionTitle, _appearanceRows),
        .. SampleScreen.Groups.Select(static group =>
            new MenuGroup(group.Name, group.Select(static screen => new MenuRow(screen)))),
    ];

    private void MarkSelectedAppearance(SampleAppearance selected)
    {
        foreach (MenuRow row in _appearanceRows)
        {
            row.IsSelected = row.Appearance == selected;
        }
    }

    private async void OnSelectionChanged(object? sender, SelectionChangedEventArgs e)
    {
        if (sender is not CollectionView list || e.CurrentSelection.Count == 0)
        {
            return;
        }

        MenuRow row = (MenuRow)e.CurrentSelection[0];

        // 押した行がハイライトされたまま遷移しないよう、push の前に選択状態を解除する。
        // 解除で再入したハンドラは、選択なしとして冒頭で戻る。
        list.SelectedItem = null;

        if (row.Appearance is { } appearance)
        {
            SampleAppearanceStore.Save(appearance);
            SampleAppearanceStore.Apply(appearance);
            MarkSelectedAppearance(appearance);
            return;
        }

        if (row.Screen is { } screen)
        {
            await Navigation.PushAsync(screen.CreateTitledPage());
        }
    }
}

/// <summary>
/// ルートメニューの 1 行。外観の選択肢か、デモ画面への入口のどちらかを表す。
/// </summary>
internal sealed class MenuRow : SampleViewModel
{
    private bool _isSelected;

    /// <summary>外観の選択肢の行を作る。</summary>
    /// <param name="appearance">この行が表す選択肢</param>
    public MenuRow(SampleAppearance appearance)
    {
        Appearance = appearance;
        Title = appearance.Title();
    }

    /// <summary>デモ画面への入口の行を作る。</summary>
    /// <param name="screen">この行が表す画面</param>
    public MenuRow(SampleScreen screen)
    {
        Screen = screen;
        Title = screen.Title;
    }

    /// <summary>行に表示する文言。</summary>
    public string Title { get; }

    /// <summary>外観の選択肢の行ならその選択肢。</summary>
    public SampleAppearance? Appearance { get; }

    /// <summary>デモ画面への入口の行ならその画面。</summary>
    public SampleScreen? Screen { get; }

    /// <summary>外観の選択肢の行で、それが選択中かどうか。</summary>
    public bool IsSelected
    {
        get => _isSelected;
        set => Set(ref _isSelected, value);
    }
}

/// <summary>ルートメニューの 1 区分と、そこに属する行。</summary>
/// <param name="name">区分の見出しに表示する名前</param>
/// <param name="rows">その区分に属する行</param>
internal sealed class MenuGroup(string name, IEnumerable<MenuRow> rows) : List<MenuRow>(rows)
{
    /// <summary>区分の見出しに表示する名前。</summary>
    public string Name { get; } = name;
}
