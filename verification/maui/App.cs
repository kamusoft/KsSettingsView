using Microsoft.Maui.Controls;

namespace MyApp;

/// <summary>消費者検証アプリのアプリケーション。</summary>
/// <remarks>
/// 表示するのは利用者向けドキュメントの最小例そのままの設定ページ 1 枚だけで、
/// 検証はビルドが成立することを見る。
/// </remarks>
public class App : Application
{
    /// <inheritdoc/>
    protected override Window CreateWindow(IActivationState? activationState)
        => new(new SettingsPage());
}
