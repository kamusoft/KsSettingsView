using KsSettingsView.Sample.Maui.ViewModels;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Controls.PlatformConfiguration;
using Microsoft.Maui.Controls.PlatformConfiguration.iOSSpecific;

namespace KsSettingsView.Sample.Maui.Pages;

/// <summary>
/// style 切替と Section 装飾プリセット切替を観察するデモページ。
/// </summary>
/// <remarks>
/// 下地の配色は <see cref="SampleTheme.ApplySectionDecorationDemo"/> で与え、Section 装飾の
/// 4 属性はプリセットからのバインドで与える。
///
/// iOS では操作部と設定 list に縦幅を譲るため、この画面だけタイトルを 1 行表示にする
/// (iOS ネイティブサンプルの同デモ画面と揃える)。
/// </remarks>
public partial class SectionDecorationDemoPage : ContentPage
{
    /// <summary>デモページを作る。</summary>
    public SectionDecorationDemoPage()
    {
        InitializeComponent();
        On<iOS>().SetLargeTitleDisplay(LargeTitleDisplayMode.Never);
        BindingContext = new SectionDecorationDemoViewModel();
        SampleThemeFollower.Attach(this, dark => SampleTheme.ApplySectionDecorationDemo(Settings, dark));
    }
}
