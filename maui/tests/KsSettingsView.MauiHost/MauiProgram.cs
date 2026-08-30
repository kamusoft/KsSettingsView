using KsSettingsView.Maui;
using Microsoft.Maui.Hosting;

namespace KsSettingsView.MauiHost;

/// <summary>検証ホストの組み立て。</summary>
public static class MauiProgram
{
    /// <summary>KsSettingsView を組み込んだアプリを作る。</summary>
    public static MauiApp CreateMauiApp()
    {
        MauiAppBuilder builder = MauiApp.CreateBuilder();

        builder
            .UseMauiApp<App>()
            .AddKsSettingsView();

        return builder.Build();
    }
}
