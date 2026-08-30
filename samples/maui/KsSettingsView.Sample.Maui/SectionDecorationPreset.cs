using System.Collections.Generic;
using Microsoft.Maui;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.Sample.Maui;

/// <summary>
/// Section 装飾デモが切り替える Section 装飾 4 属性
/// (SectionMargin / SectionCornerRadius / SectionBorderWidth / SectionBorderColor) の組。
/// </summary>
/// <remarks>
/// 各プリセットは色定義を持たず、<see cref="SampleTheme"/> の色定数だけを参照する
/// (色値の二重管理を作らない)。null の属性は style ごとのライブラリ既定へ解決される。
///
/// 対応する定義は samples/ios/KsSettingsViewSample/SectionDecorationPreset.swift と
/// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SectionDecorationPreset.kt。
/// </remarks>
/// <param name="Title">プリセット選択 UI に表示する名前</param>
/// <param name="SectionMargin">Section 単位の外側余白 (null で style ごとの既定)</param>
/// <param name="SectionCornerRadius">箱の角丸半径 (null で style ごとの既定)</param>
/// <param name="SectionBorderWidth">箱のボーダー幅 (null で実効 0)</param>
/// <param name="SectionBorderColor">箱のボーダー色 (null で実効透明)</param>
public sealed record SectionDecorationPreset(
    string Title,
    Thickness? SectionMargin = null,
    double? SectionCornerRadius = null,
    double? SectionBorderWidth = null,
    Color? SectionBorderColor = null)
{
    /// <summary>4 属性すべて未指定。style ごとのライブラリ既定へ解決される。</summary>
    public static readonly SectionDecorationPreset Standard = new("既定");

    /// <summary>余白を広く・角丸を小さくした組。</summary>
    public static readonly SectionDecorationPreset WideMargin = new(
        "余白広め・角丸小",
        SectionMargin: new Thickness(32, 32, 32, 0),
        SectionCornerRadius: 8);

    /// <summary>既定の余白・角丸のままボーダーを指定した組。</summary>
    public static readonly SectionDecorationPreset Bordered = new(
        "ボーダーあり",
        SectionBorderWidth: 2,
        SectionBorderColor: SampleTheme.DemoSectionBorder);

    /// <summary>プリセット選択 UI に並べる全プリセット。並び順がそのまま選択肢の並び順になる。</summary>
    public static IReadOnlyList<SectionDecorationPreset> All { get; } =
    [
        Standard,
        WideMargin,
        Bordered,
    ];
}
