using Microsoft.Maui.Controls;

namespace KsSettingsView.MauiHost;

/// <summary>
/// 検証操作の入口。
/// </summary>
/// <remarks>
/// 設定画面は同じインスタンスを push し直すため、離脱中に加えた変更が再訪問で
/// 復元されるかどうかをここから確かめられる。
/// </remarks>
public class MenuPage : ContentPage
{
    /// <summary>入口の画面を作る。</summary>
    public MenuPage()
    {
        Title = "KsSettingsView MAUI Host";

        Button open = new() { Text = "設定画面を開く" };
        open.Clicked += async (_, _) => await Navigation.PushAsync(SettingsPage.Instance);

        Button updateOffscreen = new() { Text = "離脱中に ValueText を更新" };
        updateOffscreen.Clicked += (_, _) => SettingsPage.Instance.UpdateValueText();

        Button addOffscreen = new() { Text = "離脱中に Cell を追加" };
        addOffscreen.Clicked += (_, _) => SettingsPage.Instance.AddCell();

        Button swapRootHeaderOffscreen = new() { Text = "離脱中に Root Header View を差し替え" };
        swapRootHeaderOffscreen.Clicked += (_, _) => SettingsPage.Instance.SwapRootHeaderView();

        Content = new VerticalStackLayout
        {
            Padding = 24,
            Spacing = 12,
            Children = { open, updateOffscreen, addOffscreen, swapRootHeaderOffscreen },
        };
    }
}
