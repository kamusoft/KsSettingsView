using KsSettingsView.Sample.Maui.ViewModels;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui.Pages;

/// <summary>
/// 末尾 / 中間 × Cell / Section の 4 パターンで可視性の切替を観察するデモページ。
/// </summary>
public partial class VisibilityDemoPage : ContentPage
{
    /// <summary>デモページを作る。</summary>
    public VisibilityDemoPage()
    {
        InitializeComponent();
        BindingContext = new VisibilityDemoViewModel();
    }
}
