using KsSettingsView.Sample.Maui.ViewModels;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui.Pages;

/// <summary>
/// 基本 Cell 7 種を Cell タイプ別構成で確認するデモページ。
/// </summary>
/// <remarks>
/// 配色は <see cref="SampleTheme"/> の共用 Theme を明示適用し、実効外観に応じて light / dark を
/// 選ぶ。ButtonCell のタイトル色は Cell 側へ明示指定するため、Theme と同じ経路で適用し直す。
/// </remarks>
public partial class BasicCellsDemoPage : ContentPage
{
    /// <summary>デモページを作る。</summary>
    public BasicCellsDemoPage()
    {
        InitializeComponent();
        BindingContext = new BasicCellsDemoViewModel();
        SampleThemeFollower.Attach(this, ApplyTheme);
    }

    private void ApplyTheme(bool dark)
    {
        SampleTheme.Apply(Settings, dark);
        LogoutButton.TitleColor = SampleTheme.MauiTitleText(dark);
    }
}
