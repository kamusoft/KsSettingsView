using System;
using System.Collections.Generic;
using Microsoft.Maui.ApplicationModel;

namespace KsSettingsView.Sample.Maui;

/// <summary>ルートメニューで選ぶアプリ全体の外観。</summary>
public enum SampleAppearance
{
    /// <summary>端末の外観に従う。</summary>
    System,

    /// <summary>端末の設定に関わらずライト。</summary>
    Light,

    /// <summary>端末の設定に関わらずダーク。</summary>
    Dark,
}

/// <summary>
/// 外観の選択肢の表示文言と、MAUI の外観指定への対応づけ。
/// </summary>
/// <remarks>
/// 見出しと項目の文言、選択中の印の読み上げ文言は全 platform で一致させる (cross/ADR-0016)。
/// 文言を画面側に手書きすると表記ゆれが再発するため、定義はここ 1 箇所に閉じる。
///
/// 対応する定義は samples/ios/KsSettingsViewSample/SampleAppearance.swift と
/// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleAppearance.kt。
/// </remarks>
public static class SampleAppearances
{
    /// <summary>ルートメニューで外観の項目群につける見出し。</summary>
    public const string SectionTitle = "外観";

    /// <summary>選択中の項目に付く印の読み上げ文言。行の文言と合わせて読まれる。</summary>
    public const string SelectedLabel = "選択中";

    /// <summary>初回起動時の選択。</summary>
    public const SampleAppearance Default = SampleAppearance.System;

    /// <summary>ルートメニューに並べる全選択肢。並び順がそのまま項目の並び順になる。</summary>
    public static IReadOnlyList<SampleAppearance> All { get; } =
    [
        SampleAppearance.System,
        SampleAppearance.Light,
        SampleAppearance.Dark,
    ];

    /// <summary>ルートメニューに表示する項目名。</summary>
    /// <param name="appearance">表示する選択肢</param>
    /// <returns>項目名</returns>
    public static string Title(this SampleAppearance appearance) => appearance switch
    {
        SampleAppearance.System => "システム",
        SampleAppearance.Light => "ライト",
        SampleAppearance.Dark => "ダーク",
        _ => throw new ArgumentOutOfRangeException(nameof(appearance)),
    };

    /// <summary>
    /// アプリ全体へ適用する外観。
    /// </summary>
    /// <remarks>
    /// 「システム」は <see cref="AppTheme.Unspecified"/> (上書きなし) に対応し、端末の外観が
    /// そのまま効く。
    /// </remarks>
    /// <param name="appearance">選択中の選択肢</param>
    /// <returns>アプリ全体へ与える外観</returns>
    public static AppTheme ToAppTheme(this SampleAppearance appearance) => appearance switch
    {
        SampleAppearance.System => AppTheme.Unspecified,
        SampleAppearance.Light => AppTheme.Light,
        SampleAppearance.Dark => AppTheme.Dark,
        _ => throw new ArgumentOutOfRangeException(nameof(appearance)),
    };
}
