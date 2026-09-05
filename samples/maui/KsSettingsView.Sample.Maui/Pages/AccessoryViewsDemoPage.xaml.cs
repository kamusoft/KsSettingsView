using System;
using KsSettingsView.Sample.Maui.ViewModels;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.Sample.Maui.Pages;

/// <summary>
/// Root / Section の Header・Footer へ任意の View を置いて、その表示と更新を確認するデモページ。
/// </summary>
/// <remarks>
/// 配色は <see cref="SampleTheme"/> の共用 Theme を明示適用する。
/// XAML で宣言できる accessory は XAML 側に置き、実行中に作り直すものだけをここで組み立てる。
/// </remarks>
public partial class AccessoryViewsDemoPage : ContentPage
{
    private static readonly Color RootAccessoryBackground = Color.FromArgb("#DDEBFF");
    private static readonly Color RootAccessoryText = Color.FromArgb("#1F4E9C");
    private static readonly Color SwapAccessoryTextColor = Color.FromArgb("#4A4A4A");

    /// <summary>差し替えのたびに切り替える背景色。色が変われば別インスタンスになっている。</summary>
    private static readonly Color[] SwapAccessoryBackgrounds =
    [
        Color.FromArgb("#F4E1D2"),
        Color.FromArgb("#D2E7F4"),
    ];

    private readonly AccessoryViewsDemoViewModel _viewModel = new();

    private int _swapCount;

    /// <summary>デモページを作る。</summary>
    public AccessoryViewsDemoPage()
    {
        InitializeComponent();
        BindingContext = _viewModel;
        SwapSection.HeaderView = CreateSwapHeaderView();
        SampleThemeFollower.Attach(this, dark => SampleTheme.Apply(Settings, dark));
    }

    private static View CreateAccessoryView(string text, Color background, Color textColor)
        => new Border
        {
            BackgroundColor = background,
            StrokeThickness = 0,
            Padding = new Thickness(16, 12),
            Content = new Label
            {
                Text = text,
                TextColor = textColor,
                FontAttributes = FontAttributes.Bold,
            },
        };

    private View CreateSwapHeaderView()
    {
        string text = _swapCount == 0
            ? "④ 差し替え前の Header View"
            : $"④ 差し替え {_swapCount} 回目の Header View";
        Color background = SwapAccessoryBackgrounds[_swapCount % SwapAccessoryBackgrounds.Length];
        return CreateAccessoryView(text, background, SwapAccessoryTextColor);
    }

    private void OnChangeBoundTextTapped(object? sender, EventArgs e) => _viewModel.ChangeBoundText();

    private void OnToggleGrowingTextTapped(object? sender, EventArgs e) => _viewModel.ToggleGrowingText();

    private void OnToggleCoexistViewTapped(object? sender, EventArgs e)
    {
        bool showView = CoexistSection.HeaderView is null;
        CoexistSection.HeaderView = showView ? CoexistHeaderView : null;
        CoexistToggleCell.Title = showView ? "Header View を外す" : "Header View を戻す";
    }

    private void OnSwapHeaderViewTapped(object? sender, EventArgs e)
    {
        _swapCount++;
        SwapSection.HeaderView = CreateSwapHeaderView();
    }

    private async void OnReconnectTapped(object? sender, EventArgs e)
    {
        if (Parent is not NavigationPage navigation)
        {
            return;
        }

        // 一覧まで戻ると Handler が切れる。同じページインスタンスを押し直して再接続させる。
        INavigation stack = navigation.Navigation;
        await stack.PopAsync(animated: false);

        _viewModel.CountReconnect(Settings.Handler is null);
        Settings.RootHeaderView = CreateAccessoryView(
            $"① Root Header View（離脱中に差し替え {_viewModel.ReconnectCount} 回目）",
            RootAccessoryBackground,
            RootAccessoryText);

        await stack.PushAsync(this, animated: false);
    }
}
