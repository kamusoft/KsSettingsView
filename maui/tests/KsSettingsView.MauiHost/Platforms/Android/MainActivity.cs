using Android.App;
using Android.Content.PM;
using Microsoft.Maui;

namespace KsSettingsView.MauiHost;

/// <summary>
/// Android の起動 Activity。
/// </summary>
/// <remarks>
/// テーマは MAUI テンプレート既定の Maui.SplashTheme をそのまま使う。ks-settingsview-ui は
/// ライブラリ側で自前のテーマをかぶせて描画するため、ホストのテーマ種別を問わない (android/ADR-0020)。
/// </remarks>
[Activity(
    Theme = "@style/Maui.SplashTheme",
    MainLauncher = true,
    LaunchMode = LaunchMode.SingleTop,
    ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation | ConfigChanges.UiMode
        | ConfigChanges.ScreenLayout | ConfigChanges.SmallestScreenSize | ConfigChanges.Density)]
public class MainActivity : MauiAppCompatActivity
{
}
