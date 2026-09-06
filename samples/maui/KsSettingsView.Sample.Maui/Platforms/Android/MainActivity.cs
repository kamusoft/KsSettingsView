using Android.App;
using Android.Content.PM;
using Android.Content.Res;
using Android.OS;
using Microsoft.Maui;

namespace KsSettingsView.Sample.Maui;

/// <summary>
/// Android の起動 Activity。
/// </summary>
/// <remarks>
/// テーマは MAUI テンプレート既定の Maui.SplashTheme をそのまま使う。KsSettingsView は
/// ライブラリ側で自前のテーマをかぶせて描画するため、ホストのテーマ種別を問わない (android/ADR-0020)。
///
/// 夜間モード (uiMode) は Activity 側で受け取ったうえで、自分で再生成する。両方が要る:
/// MAUI が <see cref="Microsoft.Maui.Controls.Application.RequestedTheme"/> を更新するのは
/// Activity が uiMode を受け取ったときだけで、受け取らずに再生成させると更新されないまま
/// ライトに留まる。一方、受け取るだけでは解決済みのリソースを持つ既存 View が残り、
/// ページの下地や既定色の文字がライトのまま描かれる。
/// </remarks>
[Activity(
    Theme = "@style/Maui.SplashTheme",
    MainLauncher = true,
    LaunchMode = LaunchMode.SingleTop,
    ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation
        | ConfigChanges.ScreenLayout | ConfigChanges.SmallestScreenSize | ConfigChanges.Density
        | ConfigChanges.UiMode)]
public class MainActivity : MauiAppCompatActivity
{
    private UiMode _nightMode;

    /// <inheritdoc/>
    protected override void OnCreate(Bundle? savedInstanceState)
    {
        base.OnCreate(savedInstanceState);
        _nightMode = CurrentNightMode(Resources?.Configuration);
    }

    /// <inheritdoc/>
    public override void OnConfigurationChanged(Configuration newConfig)
    {
        base.OnConfigurationChanged(newConfig);

        UiMode night = CurrentNightMode(newConfig);
        if (night == _nightMode)
        {
            return;
        }

        _nightMode = night;
        Recreate();
    }

    private static UiMode CurrentNightMode(Configuration? configuration)
        => (configuration?.UiMode ?? UiMode.NightUndefined) & UiMode.NightMask;
}
