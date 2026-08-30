using KsSettingsView.Sample.Maui.ViewModels;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui.Pages;

/// <summary>
/// 入力系 Cell 5 種を確認するデモページ。
/// </summary>
/// <remarks>
/// 各 Cell の値は ViewModel と双方向にバインドしてあり、ユーザー操作による値の変更が
/// 画面上部の直近イベント表示に現れる。配色は <see cref="SampleTheme"/> の共用 Theme を明示適用する。
/// </remarks>
public partial class InputCellsDemoPage : ContentPage
{
    /// <summary>デモページを作る。</summary>
    public InputCellsDemoPage()
    {
        InitializeComponent();
        SampleTheme.Apply(Settings);
        BindingContext = new InputCellsDemoViewModel();
    }
}
