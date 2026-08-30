using System;
using System.Collections.Generic;
using System.Linq;
using KsSettingsView.Sample.Maui.Pages;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui;

/// <summary>Sample アプリの画面区分。</summary>
public enum SampleScreenCategory
{
    /// <summary>ライブラリの使い方を示すデモ画面。プラットフォーム間で一致させる対象。</summary>
    Demo,

    /// <summary>MAUI facade にしか対応概念がないデモ画面。他 platform への追随義務を負わない。</summary>
    MauiSpecific,

    /// <summary>platform 固有の技術検証画面。デモ画面の集合には数えない。</summary>
    Verification,
}

/// <summary>
/// Sample アプリの画面一覧と表示名の一元定義。
/// </summary>
/// <remarks>
/// ルートメニューの項目文言と遷移先ページのタイトルは必ず <see cref="Title"/> を参照する。
/// 文言を 2 箇所に手書きすると表記ゆれが再発するため、定義はここ 1 箇所に閉じる。
/// 画面を増やすときは <see cref="All"/> へ 1 件足せば、一覧と遷移の両方が追随する。
///
/// Sample はプラットフォーム間のパリティ検証装置であり、デモ画面の文言と構成は全 platform で
/// 一致させる (cross/ADR-0016)。ただし <see cref="SampleScreenCategory.MauiSpecific"/> の画面は
/// MAUI facade にしか対応概念がないため一致の対象外で、他 platform に対応画面を作らない。
/// 対応する定義は
/// samples/ios/KsSettingsViewSample/SampleScreen.swift と
/// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleScreen.kt。
/// </remarks>
/// <param name="Category">一覧上の区分</param>
/// <param name="Title">ルートメニュー項目と画面タイトルに共通で使う表示名</param>
/// <param name="CreatePage">遷移先ページを作る関数</param>
public sealed record SampleScreen(
    SampleScreenCategory Category,
    string Title,
    Func<Page> CreatePage)
{
    /// <summary>Sample アプリが持つ全画面。並び順がそのまま一覧の並び順になる。</summary>
    public static IReadOnlyList<SampleScreen> All { get; } =
    [
        new(
            SampleScreenCategory.Demo,
            "基本 Cell 7 種デモ",
            static () => new BasicCellsDemoPage()),
        new(
            SampleScreenCategory.Demo,
            "入力 Cell 5 種デモ",
            static () => new InputCellsDemoPage()),
        new(
            SampleScreenCategory.Demo,
            "CustomCell デモ",
            static () => new CustomCellDemoPage()),
        new(
            SampleScreenCategory.Demo,
            "共通フィールド統合デモ",
            static () => new UnifyCellCommonFieldsDemoPage()),
        new(
            SampleScreenCategory.Demo,
            "isVisible デモ（条件付き非表示）",
            static () => new VisibilityDemoPage()),
        new(
            SampleScreenCategory.Demo,
            "Section 装飾デモ（style 切替）",
            static () => new SectionDecorationDemoPage()),
        new(
            SampleScreenCategory.MauiSpecific,
            "Header / Footer への View 配置デモ",
            static () => new AccessoryViewsDemoPage()),
        new(
            SampleScreenCategory.MauiSpecific,
            "CustomCell の MAUI 固有デモ",
            static () => new CustomCellMauiSpecificDemoPage()),
        new(
            SampleScreenCategory.MauiSpecific,
            "MAUI 固有 Cell 機能デモ",
            static () => new MauiSpecificCellFeaturesDemoPage()),
    ];

    /// <summary>一覧へ表示する区分ごとのまとまり。画面のない区分は現れない。</summary>
    public static IReadOnlyList<SampleScreenGroup> Groups { get; } =
    [
        .. All
            .GroupBy(static screen => screen.Category)
            .OrderBy(static group => group.Key)
            .Select(static group => new SampleScreenGroup(group.Key, group)),
    ];

    /// <summary>
    /// 遷移先ページを作り、タイトルを一元定義の文言に揃える。
    /// </summary>
    /// <remarks>
    /// タイトルをページ側に書かずここで与えることで、一覧の項目文言との一致を構造的に保証する。
    /// </remarks>
    /// <returns>タイトルを設定済みのページ</returns>
    public Page CreateTitledPage()
    {
        Page page = CreatePage();
        page.Title = Title;
        return page;
    }
}

/// <summary>
/// 一覧上の 1 区分と、そこに属する画面。
/// </summary>
/// <param name="category">区分</param>
/// <param name="screens">その区分に属する画面</param>
public sealed class SampleScreenGroup(SampleScreenCategory category, IEnumerable<SampleScreen> screens)
    : List<SampleScreen>(screens)
{
    /// <summary>一覧の見出しに表示する区分名。</summary>
    public string Name { get; } = category switch
    {
        SampleScreenCategory.Demo => "デモ",
        SampleScreenCategory.MauiSpecific => "MAUI 固有",
        SampleScreenCategory.Verification => "検証",
        _ => throw new ArgumentOutOfRangeException(nameof(category)),
    };
}
