using System;
using System.Linq;
using Microsoft.Maui.ApplicationModel;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Storage;

namespace KsSettingsView.Sample.Maui;

/// <summary>
/// 外観の選択 (<see cref="SampleAppearance"/>) の永続化と、アプリ全体への反映。
/// </summary>
/// <remarks>
/// 反映先は <see cref="Application.UserAppTheme"/> で、ページの下地とライブラリ UI が追随する。
/// ナビゲーションバーの配色はサンプルが固定値で与えており、外観に依らず変わらない。
///
/// 対応する定義は
/// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleAppearanceStore.kt。
/// </remarks>
public static class SampleAppearanceStore
{
    private const string PreferenceKey = "appearance";

    /// <summary>保存済みの選択を返す。未保存・未知の値なら <see cref="SampleAppearances.Default"/>。</summary>
    /// <returns>選択中の外観</returns>
    public static SampleAppearance Load()
    {
        string? saved = Preferences.Default.Get<string?>(PreferenceKey, null);
        return Enum.TryParse(saved, out SampleAppearance appearance) && SampleAppearances.All.Contains(appearance)
            ? appearance
            : SampleAppearances.Default;
    }

    /// <summary>選択を保存する。次回起動時は <see cref="Load"/> がこの値を返す。</summary>
    /// <param name="appearance">保存する外観</param>
    public static void Save(SampleAppearance appearance)
        => Preferences.Default.Set(PreferenceKey, appearance.ToString());

    /// <summary>選択をアプリ全体の外観へ反映する。</summary>
    /// <param name="appearance">反映する外観</param>
    public static void Apply(SampleAppearance appearance)
    {
        if (Application.Current is { } application)
        {
            application.UserAppTheme = appearance.ToAppTheme();
        }
    }
}
