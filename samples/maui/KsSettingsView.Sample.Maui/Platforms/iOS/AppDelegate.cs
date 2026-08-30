using Foundation;
using Microsoft.Maui;
using Microsoft.Maui.Hosting;

namespace KsSettingsView.Sample.Maui;

/// <summary>iOS のアプリ委譲。</summary>
[Register("AppDelegate")]
public class AppDelegate : MauiUIApplicationDelegate
{
    /// <inheritdoc/>
    protected override MauiApp CreateMauiApp() => MauiProgram.CreateMauiApp();
}
