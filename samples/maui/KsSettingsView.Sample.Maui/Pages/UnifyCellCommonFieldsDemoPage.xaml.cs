using KsSettingsView.Sample.Maui.ViewModels;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui.Pages;

/// <summary>
/// 7 種 Cell の共通フィールドと RadioCell / SimpleCheckCell の強調色を一覧するデモページ。
/// </summary>
/// <remarks>Theme は明示せず、各 platform の既定スタイルで描かれる様子を確認する。</remarks>
public partial class UnifyCellCommonFieldsDemoPage : ContentPage
{
    /// <summary>デモページを作る。</summary>
    public UnifyCellCommonFieldsDemoPage()
    {
        InitializeComponent();
        BindingContext = new UnifyCellCommonFieldsDemoViewModel();
    }
}
