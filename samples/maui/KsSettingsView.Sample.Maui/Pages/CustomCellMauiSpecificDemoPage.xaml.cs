using System;
using KsSettingsView.Sample.Maui.ViewModels;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Controls.Shapes;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.Sample.Maui.Pages;

/// <summary>
/// CustomCell の MAUI facade 固有の意味論を確認するデモページ。
/// </summary>
/// <remarks>
/// 配色は <see cref="SampleTheme"/> の共用 Theme を明示適用する。
/// XAML で宣言できる内容は XAML 側に置き、差し替えのために作る View だけをここで組み立てる。
/// </remarks>
public partial class CustomCellMauiSpecificDemoPage : ContentPage
{
    private static readonly Color ContentTextColor = Color.FromArgb("#4A4A4A");

    private readonly CustomCellMauiSpecificDemoViewModel _viewModel = new();

    /// <summary>差し替えで行き来する 2 つの View。同じインスタンスへ戻す往復も通る。</summary>
    private readonly View _contentA;
    private readonly View _contentB;

    private bool _showingB;

    /// <summary>デモページを作る。</summary>
    public CustomCellMauiSpecificDemoPage()
    {
        InitializeComponent();
        SampleTheme.Apply(Settings);
        BindingContext = _viewModel;

        _contentA = CreateContentView("① Content A", Color.FromArgb("#F4E1D2"));
        _contentB = CreateContentView("① Content B", Color.FromArgb("#D2E7F4"));
        SwapCell.Content = _contentA;
        ReconnectCell.Content = CreateContentView("③ 離脱前の Content", Color.FromArgb("#DCE7EA"));
    }

    private static View CreateContentView(string text, Color background)
        => new Border
        {
            BackgroundColor = background,
            StrokeThickness = 0,
            StrokeShape = new RoundRectangle { CornerRadius = 8 },
            Margin = new Thickness(16, 10),
            Padding = new Thickness(12, 10),
            Content = new Label
            {
                Text = text,
                TextColor = ContentTextColor,
                FontAttributes = FontAttributes.Bold,
            },
        };

    private void OnSwapContentTapped(object? sender, EventArgs e)
    {
        _showingB = !_showingB;
        SwapCell.Content = _showingB ? _contentB : _contentA;
        SwapContentCell.Title = _showingB ? "Content を A に差し替える" : "Content を B に差し替える";
        ClearContentCell.Title = "Content を null にする";
    }

    private void OnToggleNullContentTapped(object? sender, EventArgs e)
    {
        bool restore = SwapCell.Content is null;
        SwapCell.Content = restore ? (_showingB ? _contentB : _contentA) : null;
        ClearContentCell.Title = restore ? "Content を null にする" : "Content を戻す";
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
        ReconnectCell.Content = CreateContentView(
            $"③ 離脱中に差し替えた Content（{_viewModel.ReconnectCount} 回目）",
            Color.FromArgb("#DCE7EA"));

        await stack.PushAsync(this, animated: false);
    }
}
