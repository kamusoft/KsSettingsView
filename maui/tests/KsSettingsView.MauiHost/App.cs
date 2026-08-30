using Microsoft.Maui.Controls;

namespace KsSettingsView.MauiHost;

/// <summary>検証ホストのアプリケーション。</summary>
public class App : Application
{
    /// <inheritdoc/>
    protected override Window CreateWindow(IActivationState? activationState)
        => new(new NavigationPage(new MenuPage()));
}
