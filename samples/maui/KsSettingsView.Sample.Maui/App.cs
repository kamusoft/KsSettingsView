using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui;

/// <summary>Sample アプリのアプリケーション。</summary>
/// <remarks>
/// 入口はデモ一覧ページで、各画面へは <see cref="NavigationPage"/> の push / pop で行き来する。
/// </remarks>
public class App : Application
{
    /// <inheritdoc/>
    protected override Window CreateWindow(IActivationState? activationState)
    {
        var navigation = new NavigationPage(new MenuPage())
        {
            // バー配色は Android テーマへのフォールバックに委ねず MAUI 層で明示する。
            // テンプレート既定テーマ (Maui.MainTheme) のフォールバックは暗色地に黒文字となり判読できない。
            BarBackgroundColor = Color.FromArgb("#2C3E50"),
            BarTextColor = Colors.White,
        };
        // iOS ネイティブサンプルのナビゲーションバー表示 (Large Title) と揃える (cross/ADR-0016)
        Microsoft.Maui.Controls.PlatformConfiguration.iOSSpecific.NavigationPage
            .SetPrefersLargeTitles(navigation, true);
        return new Window(navigation);
    }
}
