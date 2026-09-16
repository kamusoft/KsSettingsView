using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui;

/// <summary>
/// Sample アプリ共用のリソース辞書。
/// </summary>
/// <remarks>
/// アプリの Resources へ併合し、各デモページから <c>StaticResource</c> で参照する。
/// 中身は SampleStyles.xaml が持つ。
/// </remarks>
public partial class SampleStyles : ResourceDictionary
{
    /// <summary>リソース辞書を作る。</summary>
    public SampleStyles() => InitializeComponent();
}
