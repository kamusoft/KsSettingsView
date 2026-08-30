using KsSettingsView.Sample.Maui.ViewModels;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui.Pages;

/// <summary>MAUI facade 固有の PickerCell 機能を確認するデモページ。</summary>
public partial class MauiSpecificCellFeaturesDemoPage : ContentPage
{
    /// <summary>デモページを作る。</summary>
    public MauiSpecificCellFeaturesDemoPage()
    {
        InitializeComponent();
        SampleTheme.Apply(Settings);
        BindingContext = new MauiSpecificCellFeaturesDemoViewModel();
    }
}
