using KsSettingsView.Sample.Maui.ViewModels;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui.Pages;

/// <summary>
/// 基本 Cell 7 種を Cell タイプ別構成で確認するデモページ。
/// </summary>
/// <remarks>
/// 配色は <see cref="SampleTheme"/> の共用 Theme を明示適用する。
/// </remarks>
public partial class BasicCellsDemoPage : ContentPage
{
    /// <summary>デモページを作る。</summary>
    public BasicCellsDemoPage()
    {
        InitializeComponent();
        SampleTheme.Apply(Settings);
        BindingContext = new BasicCellsDemoViewModel();
    }
}
