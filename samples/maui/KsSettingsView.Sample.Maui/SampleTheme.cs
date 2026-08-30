using KsSettingsView.Maui;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.Sample.Maui;

/// <summary>
/// Sample アプリ共用の Theme 定義と、デモが Cell へ明示指定する色。
/// </summary>
/// <remarks>
/// 色定数と Theme をここに一元化し、基本 Cell 7 種デモ / 入力 Cell 5 種デモの双方が同じ定義を
/// 参照する (色値の二重管理を作らない)。配色は AiForms.Maui.SettingsView の Sample に合わせてある。
///
/// Sample はプラットフォーム間の検証装置であり、Cell へ渡す色は全 platform で同一の RGBA にする
/// (cross/ADR-0016)。そのため platform 固有の semantic color は使わず、ここに固定値を置く。
/// 対応する定義は samples/ios/KsSettingsViewSample/SampleTheme.swift と
/// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleTheme.kt。
/// </remarks>
public static class SampleTheme
{
    /// <summary>SettingsView 全体の背景色 (#F2EFE6)。</summary>
    public static readonly Color MauiViewBackground = Color.FromArgb("#F2EFE6");

    /// <summary>Cell の背景色 (白)。</summary>
    public static readonly Color MauiCellBackground = Color.FromArgb("#FFFFFF");

    /// <summary>セパレータ色 (#E6DAB9)。</summary>
    public static readonly Color MauiSeparator = Color.FromArgb("#E6DAB9");

    /// <summary>タッチ時の塗り色 (#50FFBF00 — 強調色の半透明)。</summary>
    public static readonly Color MauiSelected = Color.FromArgb("#50FFBF00");

    /// <summary>強調色 (#FFBF00)。スイッチ ON・チェック印などに使われる。</summary>
    public static readonly Color MauiAccent = Color.FromArgb("#FFBF00");

    /// <summary>Section ヘッダの文字色 (#CC9900)。</summary>
    public static readonly Color MauiHeaderText = Color.FromArgb("#CC9900");

    /// <summary>Section フッタの文字色 (#999999)。</summary>
    public static readonly Color MauiFooterText = Color.FromArgb("#999999");

    /// <summary>無効な行のテキスト色 (#999999)。</summary>
    public static readonly Color MauiDisabledText = Color.FromArgb("#999999");

    /// <summary>ButtonCell の <see cref="CellBase.TitleColor"/> に使うタイトル色 (#CC9900)。</summary>
    public static readonly Color MauiTitleText = Color.FromArgb("#CC9900");

    /// <summary>Cell タイトルの既定文字色 (#555555)。</summary>
    public static readonly Color MauiDeepText = Color.FromArgb("#555555");

    /// <summary>RadioCell「ライト」の強調色 (#FF8D28)。</summary>
    public static readonly Color DemoAccentOrange = Color.FromArgb("#FF8D28");

    /// <summary>RadioCell「ダーク」の強調色 (#CB30E0)。</summary>
    public static readonly Color DemoAccentPurple = Color.FromArgb("#CB30E0");

    /// <summary>RadioCell「自動」の強調色 (#00C3D0)。</summary>
    public static readonly Color DemoAccentTeal = Color.FromArgb("#00C3D0");

    /// <summary>SimpleCheckCell「通知 1」の強調色 (#FF2D55)。</summary>
    public static readonly Color DemoAccentPink = Color.FromArgb("#FF2D55");

    /// <summary>SimpleCheckCell「通知 2」の強調色 (#34C759)。</summary>
    public static readonly Color DemoAccentGreen = Color.FromArgb("#34C759");

    /// <summary>ButtonCell「登録」のタイトル色 (#0088FF)。</summary>
    public static readonly Color DemoTitleBlue = Color.FromArgb("#0088FF");

    /// <summary>placeholder 色デモ行の <see cref="KsSettingsView.Maui.EntryCell.PlaceholderColor"/> (#D6885A)。</summary>
    public static readonly Color DemoPlaceholderOrange = Color.FromArgb("#D6885A");

    /// <summary>ピル (行タップカウンタ / ダミー行の連番) の背景 (#FAF3D9)。</summary>
    public static readonly Color DemoPillBackground = Color.FromArgb("#FAF3D9");

    /// <summary>展開本文ブロックの背景 (#FAF7EE)。</summary>
    public static readonly Color DemoExpandBackground = Color.FromArgb("#FAF7EE");

    /// <summary>展開本文の文字色 (#777777)。</summary>
    public static readonly Color DemoExpandText = Color.FromArgb("#777777");

    /// <summary>Section 装飾デモの <see cref="SettingsView.SectionBorderColor"/> に渡す灰色 (#C7C7CC)。</summary>
    public static readonly Color DemoSectionBorder = Color.FromArgb("#C7C7CC");

    /// <summary>
    /// アクセント 6 色の循環パレット。
    /// </summary>
    /// <remarks>
    /// CustomCell デモの「スクロール耐性（ダミー #01–#40）」が行ごとに色を循環させるために使う。
    /// 並び順は iOS / Android 側の <c>demoAccentPalette</c> と一致させる。
    /// </remarks>
    public static readonly IReadOnlyList<Color> DemoAccentPalette =
    [
        DemoAccentOrange,
        DemoAccentPurple,
        DemoAccentTeal,
        DemoAccentPink,
        DemoAccentGreen,
        DemoTitleBlue,
    ];

    /// <summary>
    /// 共用の Theme を <see cref="SettingsView"/> へ適用する。
    /// </summary>
    /// <remarks>
    /// 行の高さは基準値なし (-1) + 行ごとの可変高さで、内容に応じて各行が伸縮する。
    /// </remarks>
    /// <param name="view">Theme を適用する SettingsView</param>
    public static void Apply(SettingsView view)
    {
        ArgumentNullException.ThrowIfNull(view);

        view.SeparatorColor = MauiSeparator;
        view.BackgroundColor = MauiViewBackground;
        view.CellBackgroundColor = MauiCellBackground;
        view.SelectedColor = MauiSelected;
        view.CellAccentColor = MauiAccent;
        view.DisabledTextColor = MauiDisabledText;
        view.RowHeight = -1;
        view.HasUnevenRows = true;
        view.HeaderTextColor = MauiHeaderText;
        view.HeaderBackgroundColor = MauiViewBackground;
        view.FooterTextColor = MauiFooterText;
        view.FooterBackgroundColor = MauiViewBackground;
        view.CellTitleColor = MauiDeepText;
    }

    /// <summary>
    /// Section 装飾デモの下地 Theme を <see cref="SettingsView"/> へ適用する。
    /// </summary>
    /// <remarks>
    /// 下地 (<see cref="VisualElement.BackgroundColor"/>) と Header / Footer の背景に
    /// PaleBackColorPrimary を敷き、箱 (<see cref="SettingsView.CellBackgroundColor"/>)・separator・
    /// Header / Footer 文字色はライブラリ既定のまま残す。アイコンはバッジ型
    /// (<see cref="SampleIconBadge"/>) に合わせたサイズ・角丸を指定する。
    ///
    /// Section 装飾の 4 属性はプリセット切替で変わるため、ここでは触らずページ側のバインドが持つ。
    /// </remarks>
    /// <param name="view">Theme を適用する SettingsView</param>
    public static void ApplySectionDecorationDemo(SettingsView view)
    {
        ArgumentNullException.ThrowIfNull(view);

        view.BackgroundColor = MauiViewBackground;
        view.CellAccentColor = DemoAccentGreen;
        view.HeaderBackgroundColor = MauiViewBackground;
        view.FooterBackgroundColor = MauiViewBackground;
        view.CellIconSize = SampleIconBadge.Size;
        view.CellIconRadius = SampleIconBadge.CornerRadius;
    }
}
