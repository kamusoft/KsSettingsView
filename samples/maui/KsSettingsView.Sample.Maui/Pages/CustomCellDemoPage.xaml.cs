using KsSettingsView.Sample.Maui.ViewModels;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui.Pages;

/// <summary>
/// CustomCell を 5 構成で目視確認するデモページ。
/// </summary>
/// <remarks>配色は <see cref="SampleTheme"/> の共用 Theme を明示適用する。</remarks>
public partial class CustomCellDemoPage : ContentPage
{
    private readonly CustomCellDemoViewModel _viewModel = new();

    /// <summary>デモページを作る。</summary>
    public CustomCellDemoPage()
    {
        InitializeComponent();
        BindingContext = _viewModel;
        SampleThemeFollower.Attach(this, dark => SampleTheme.Apply(Settings, dark));
    }
}
