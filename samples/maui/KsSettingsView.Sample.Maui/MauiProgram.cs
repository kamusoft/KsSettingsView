using KsSettingsView.Maui;
using Microsoft.Maui.Hosting;

namespace KsSettingsView.Sample.Maui;

/// <summary>Sample アプリの組み立て。</summary>
public static class MauiProgram
{
    /// <summary>KsSettingsView を組み込んだアプリを作る。</summary>
    /// <returns>組み立て済みのアプリ</returns>
    public static MauiApp CreateMauiApp()
    {
        MauiAppBuilder builder = MauiApp.CreateBuilder();

        builder
            .UseMauiApp<App>()
            .AddKsSettingsView();

        return builder.Build();
    }
}
