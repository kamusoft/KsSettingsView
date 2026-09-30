using KsSettingsView.Sample.Maui.ViewModels;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui.Pages;

/// <summary>
/// スクロール命令ハンドルで Cell・Section・先頭・末尾へ移る操作を試すデモページ。
/// </summary>
public partial class ScrollControlDemoPage : ContentPage
{
    /// <summary>デモページを作る。</summary>
    public ScrollControlDemoPage()
    {
        InitializeComponent();
        BindingContext = new ScrollControlDemoViewModel();
    }
}
