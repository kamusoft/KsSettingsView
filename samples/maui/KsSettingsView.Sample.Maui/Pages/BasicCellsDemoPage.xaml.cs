using KsSettingsView.Sample.Maui.ViewModels;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui.Pages;

/// <summary>
/// 基本 Cell 7 種を Cell タイプ別構成で確認するデモページ。
/// </summary>
/// <remarks>
/// 配色は SampleStyles.xaml の共用 Style で与える。ButtonCell のタイトル色だけは Cell 側へ
/// 明示指定するため、XAML の AppThemeBinding で直接書く。
/// </remarks>
public partial class BasicCellsDemoPage : ContentPage
{
    /// <summary>デモページを作る。</summary>
    public BasicCellsDemoPage()
    {
        InitializeComponent();
        BindingContext = new BasicCellsDemoViewModel();
    }
}
