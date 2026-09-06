using System;
using KsSettingsView;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.MauiHost;

/// <summary>
/// SettingsView を XAML で組んだ検証画面。
/// </summary>
/// <remarks>
/// 同じインスタンスを push / pop して Handler の切断と再接続を起こし、離脱中の変更を
/// 含む最新状態が復元されるかを確かめる。
/// header / footer へ置いた View については、内容変化・固定高さ・Section 差し替え・
/// インスタンス差し替え・View の取り外しを画面下のボタンから起こせる。
/// 「外観追随ボタン」の行は、アプリの外観の変化を購読して Cell の色プロパティへ値を
/// 入れ直したとき、表示したままの行がその色で描き直されることを確かめる。
/// </remarks>
public partial class SettingsPage : ContentPage
{
    private const string ShortHeaderText = "内容 1 行";

    private const string LongHeaderText = """
        内容 3 行
        高さが変わる内容変化の 2 行目
        自動高さならここまで伸びる
        """;

    private static readonly Color RootAccessoryBackground = Color.FromArgb("#DDEBFF");
    private static readonly Color RootAccessoryText = Color.FromArgb("#1F4E9C");
    private static readonly Color SwappedAccessoryBackground = Color.FromArgb("#F4E1D2");
    private static readonly Color SwappedAccessoryText = Color.FromArgb("#8A4B1F");

    // 外観追随の確認用。ライトとダークで見分けの付く 2 色を使う。
    private static readonly Color LightButtonTitle = Color.FromArgb("#FF008000");
    private static readonly Color DarkButtonTitle = Color.FromArgb("#FFFF00FF");

    // 購読先。解除は必ずこの参照に対して行い、購読の有無と食い違わないようにする。
    private Application? _themeSubscribedTo;

    private int _updateCount;
    private int _addedCount;
    private int _swapCount;
    private int _rootSwapCount;

    /// <summary>検証画面を作る。</summary>
    public SettingsPage()
    {
        InitializeComponent();
        BindingContext = new SettingsPageContext("バインド Section", "バインド Title", "バインド Value");

        Loaded += OnPageLoaded;
        Unloaded += OnPageUnloaded;
        ApplyThemeColors(Application.Current?.RequestedTheme == AppTheme.Dark);
    }

    /// <summary>再訪問の検証で使い回す唯一のインスタンス。</summary>
    public static SettingsPage Instance { get; } = new();

    /// <summary>Cell の ValueText を書き換える。</summary>
    public void UpdateValueText()
    {
        _updateCount++;
        ValueCell.ValueText = $"更新 {_updateCount}";
    }

    /// <summary>先頭 Section へ Cell を 1 件足す。</summary>
    public void AddCell()
    {
        _addedCount++;
        Settings.Root[0].Cells.Add(new LabelCell
        {
            Title = $"追加 {_addedCount}",
            ValueText = "追加された Cell",
        });
    }

    /// <summary>Root Header View を新しいインスタンスへ差し替える。</summary>
    public void SwapRootHeaderView()
    {
        _rootSwapCount++;
        Settings.RootHeaderView = new Border
        {
            BackgroundColor = RootAccessoryBackground,
            StrokeThickness = 0,
            Padding = new Thickness(16, 10),
            Content = new Label
            {
                TextColor = RootAccessoryText,
                Text = $"差し替え {_rootSwapCount} 回目の Root Header View",
            },
        };
    }

    /// <summary>
    /// 外観に応じた色を ButtonCell の TitleColor へ入れ直す。
    /// </summary>
    /// <remarks>
    /// Cell の色プロパティに書いた AppThemeBinding は外観の変化で評価し直されないため、
    /// 両外観で色を変えたい利用者はこの形で色を入れ直す。表示中の行がその色で描き直される
    /// ことが、この画面で確かめたい挙動そのものになる。
    /// </remarks>
    /// <param name="isDark">現在の外観がダークかどうか</param>
    private void ApplyThemeColors(bool isDark)
        => ThemeFollowingButton.TitleColor = isDark ? DarkButtonTitle : LightButtonTitle;

    private void OnPageLoaded(object? sender, EventArgs e)
    {
        ApplyThemeColors(Application.Current?.RequestedTheme == AppTheme.Dark);

        if (_themeSubscribedTo is not null || Application.Current is not { } application)
        {
            return;
        }

        application.RequestedThemeChanged += OnRequestedThemeChanged;
        _themeSubscribedTo = application;
    }

    private void OnPageUnloaded(object? sender, EventArgs e)
    {
        if (_themeSubscribedTo is not { } application)
        {
            return;
        }

        application.RequestedThemeChanged -= OnRequestedThemeChanged;
        _themeSubscribedTo = null;
    }

    private void OnRequestedThemeChanged(object? sender, AppThemeChangedEventArgs e)
        => ApplyThemeColors(e.RequestedTheme == AppTheme.Dark);

    private void OnUpdateClicked(object? sender, EventArgs e) => UpdateValueText();

    private void OnGrowClicked(object? sender, EventArgs e)
    {
        bool grow = GrowLabel.Text == ShortHeaderText;
        GrowLabel.Text = grow ? LongHeaderText : ShortHeaderText;
        GrowButton.Text = grow ? "Header の内容を減らす" : "Header の内容を増やす";
    }

    private void OnHeaderHeightClicked(object? sender, EventArgs e)
    {
        bool fix = AccessorySection.HeaderHeight is null;
        AccessorySection.HeaderHeight = fix ? 44 : null;
        HeaderHeightButton.Text = fix ? "HeaderHeight を自動に" : "HeaderHeight を 44 に";
    }

    private void OnToggleSectionClicked(object? sender, EventArgs e)
    {
        bool hide = AccessorySection.IsVisible;
        AccessorySection.IsVisible = !hide;
        SectionVisibilityButton.Text = hide ? "Section を戻す" : "Section を隠す";
    }

    private void OnSwapHeaderViewClicked(object? sender, EventArgs e)
    {
        _swapCount++;
        AccessorySection.HeaderView = new Border
        {
            BackgroundColor = SwappedAccessoryBackground,
            StrokeThickness = 0,
            Padding = new Thickness(16, 10),
            Content = new Label
            {
                TextColor = SwappedAccessoryText,
                Text = $"差し替え {_swapCount} 回目の Section Header View",
            },
        };
        DetachButton.Text = "Header View を外す";
    }

    private void OnToggleHeaderViewClicked(object? sender, EventArgs e)
    {
        bool detach = AccessorySection.HeaderView is not null;
        AccessorySection.HeaderView = detach ? null : SectionHeaderView;
        DetachButton.Text = detach ? "Header View を戻す" : "Header View を外す";
    }
}

/// <summary>バインドの届き先を確かめるための BindingContext。</summary>
/// <param name="SectionHeader">Section の header に束縛する値</param>
/// <param name="BoundTitle">Cell の Title に束縛する値</param>
/// <param name="BoundValue">Cell の ValueText に束縛する値</param>
public sealed record SettingsPageContext(string SectionHeader, string BoundTitle, string BoundValue);
