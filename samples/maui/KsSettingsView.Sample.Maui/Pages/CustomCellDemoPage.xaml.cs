using KsSettingsView.Sample.Maui.ViewModels;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui.Pages;

/// <summary>
/// CustomCell を 5 構成で目視確認するデモページ。
/// </summary>
/// <remarks>配色は SampleStyles.xaml の共用 Style で与える。</remarks>
public partial class CustomCellDemoPage : ContentPage
{
    private readonly CustomCellDemoViewModel _viewModel = new();

    /// <summary>デモページを作る。</summary>
    public CustomCellDemoPage()
    {
        InitializeComponent();
        BindingContext = _viewModel;
    }
}
