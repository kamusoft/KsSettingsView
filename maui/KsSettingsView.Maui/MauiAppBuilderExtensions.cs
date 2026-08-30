using System;
using KsSettingsView.Maui.Handlers;
using Microsoft.Maui.Hosting;

namespace KsSettingsView.Maui;

/// <summary>
/// KsSettingsView をアプリへ組み込むための <see cref="MauiAppBuilder"/> 拡張。
/// </summary>
public static class MauiAppBuilderExtensions
{
    /// <summary>
    /// KsSettingsView を利用可能にする。
    /// </summary>
    /// <remarks>
    /// 登録するのは <see cref="SettingsViewHandler"/> 1 件だけである。Cell の描画は Native 側が
    /// 受け持つため、Cell 種別ごとの Handler は存在しない。
    /// </remarks>
    /// <param name="builder">組み込み先のビルダー</param>
    /// <returns>連結できるよう <paramref name="builder"/> をそのまま返す</returns>
    public static MauiAppBuilder AddKsSettingsView(this MauiAppBuilder builder)
    {
        ArgumentNullException.ThrowIfNull(builder);

        builder.ConfigureMauiHandlers(static handlers =>
            handlers.AddHandler<SettingsView, SettingsViewHandler>());

        return builder;
    }
}
