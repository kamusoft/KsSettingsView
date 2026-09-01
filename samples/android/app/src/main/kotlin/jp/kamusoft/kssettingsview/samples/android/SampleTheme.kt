package jp.kamusoft.kssettingsview.samples.android

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import jp.kamusoft.kssettingsview.ui.Theme

/**
 * Sample アプリ共用の MAUI 互換 Theme 定義。
 *
 * MAUI 原典 Sample（AiForms.Maui.SettingsView/Sample/Views/MainPage.xaml）に限りなく
 * 一致する見た目を目指した色定数と Theme をここに一元化し、基本 Cell 7 種デモ /
 * 入力 Cell 5 種デモの双方が同じ定義を参照する（色値の二重管理を作らない）。
 *
 * 対応する iOS 側定義: samples/ios/KsSettingsViewSample/SampleTheme.swift
 */
object SampleTheme {

    // =============================================================================
    // MAUI 互換色定数
    // =============================================================================
    //
    // 各色は `AiForms.Maui.SettingsView` の Sample が持つ StylesAndColors を踏襲する。
    // 指定形式は Compose `Color(0xFFXXXXXX)` の hex リテラル。

    /** PaleBackColorPrimary 相当（#F2EFE6）。SettingsView 全体の背景色。 */
    val mauiViewBackground: Color = Color(0xFFF2EFE6)

    /** Cell 背景（白）。 */
    val mauiCellBackground: Color = Color(0xFFFFFFFF)

    /** DisabledColor 相当（#E6DAB9）。セパレータ色。 */
    val mauiSeparator: Color = Color(0xFFE6DAB9)

    /** AccentColor の半透明（#50FFBF00）。タッチ時の塗り色。 */
    val mauiSelected: Color = Color(0x50FFBF00)

    /** AccentColor（#FFBF00）。スイッチ ON / チェックボックス accent 等。 */
    val mauiAccent: Color = Color(0xFFFFBF00)

    /** TitleTextColor（#CC9900）。ヘッダ文字色。 */
    val mauiHeaderText: Color = Color(0xFFCC9900)

    /** PaleTextColor（#999999）。フッタ文字色。 */
    val mauiFooterText: Color = Color(0xFF999999)

    /** 無効時テキスト色（#999999）。 */
    val mauiDisabledText: Color = Color(0xFF999999)

    /** MAUI Sample の TitleTextColor 用（ButtonCell の `CellStyle(titleColor)` で使用）。 */
    val mauiTitleText: Color = Color(0xFFCC9900)

    /** DeepTextColor（#555555）。Cell タイトルの文字色。 */
    val mauiDeepText: Color = Color(0xFF555555)

    // =============================================================================
    // 共通フィールド統合デモ用の共有アクセントパレット
    // =============================================================================
    //
    // 共通フィールド統合デモが Cell に明示指定する色は、プラットフォーム間で同一の RGBA に
    // しなければサンプル間パリティ規約 (cross/ADR-0016「各 Cell に渡すパラメータを一致させる」)
    // を満たせない。値は「iOS を正とする」原則に従い iOS の `UIColor.systemXxx` を
    // light appearance で解決した実測値 (iOS 26.5 シミュレータ) を採用する。
    // 対応する iOS 側定義: samples/ios/KsSettingsViewSample/SampleTheme.swift

    /** RadioCell「ライト」の accentColor（iOS `UIColor.systemOrange` 相当 #FF8D28）。 */
    val demoAccentOrange: Color = Color(0xFFFF8D28)

    /** RadioCell「ダーク」の accentColor（iOS `UIColor.systemPurple` 相当 #CB30E0）。 */
    val demoAccentPurple: Color = Color(0xFFCB30E0)

    /** RadioCell「自動」の accentColor（iOS `UIColor.systemTeal` 相当 #00C3D0）。 */
    val demoAccentTeal: Color = Color(0xFF00C3D0)

    /** SimpleCheckCell「通知 1」の accentColor（iOS `UIColor.systemPink` 相当 #FF2D55）。 */
    val demoAccentPink: Color = Color(0xFFFF2D55)

    /** SimpleCheckCell「通知 2」の accentColor（iOS `UIColor.systemGreen` 相当 #34C759）。 */
    val demoAccentGreen: Color = Color(0xFF34C759)

    /** ButtonCell「登録」の titleColor（iOS `UIColor.systemBlue` 相当 #0088FF）。 */
    val demoTitleBlue: Color = Color(0xFF0088FF)

    /**
     * アクセント 6 色の循環パレット。
     *
     * CustomCell デモの「スクロール耐性（ダミー #01–#40）」が行ごとに色を循環させるために使う。
     * 並び順は iOS 側 `SampleTheme.demoAccentPalette` と一致させること。
     */
    val demoAccentPalette: List<Color> = listOf(
        demoAccentOrange,
        demoAccentPurple,
        demoAccentTeal,
        demoAccentPink,
        demoAccentGreen,
        demoTitleBlue,
    )

    // =============================================================================
    // 入力 Cell デモ用の追加色
    // =============================================================================
    //
    // 入力 Cell デモが `EntryCell.placeholderColor` に明示指定する色。
    // 他のデモ色と同じく、iOS / Android / MAUI で同一 RGBA を渡すためここに一元化する
    // (cross/ADR-0016)。

    /** placeholder 色デモ行の `placeholderColor`（#D6885A）。 */
    val demoPlaceholderOrange: Color = Color(0xFFD6885A)

    // =============================================================================
    // CustomCell デモ用の追加色
    // =============================================================================
    //
    // CustomCell デモの content（利用者が書く任意 Composable）が使う色。
    // Cell の内装は利用者責務のため Theme には載らないが、iOS / Android で同一 RGBA を
    // 渡す必要がある（cross/ADR-0016 のサンプル間パリティ）ので SampleTheme に一元化する。

    /** ピル（行タップカウンタ / ダミー行の連番）の背景（#FAF3D9）。 */
    val demoPillBackground: Color = Color(0xFFFAF3D9)

    /** 展開本文ブロックの背景（#FAF7EE）。 */
    val demoExpandBackground: Color = Color(0xFFFAF7EE)

    /** 展開本文の文字色（#777777）。 */
    val demoExpandText: Color = Color(0xFF777777)

    // =============================================================================
    // Section 装飾デモ用の追加色
    // =============================================================================

    /** Section 装飾デモの `sectionBorderColor` に渡す灰色（#C7C7CC）。 */
    val demoSectionBorder: Color = Color(0xFFC7C7CC)

    /** バッジ型アイコンの地色（オレンジ #FF9500）。 */
    val demoIconOrange: Color = Color(0xFFFF9500)

    /** バッジ型アイコンの地色（青 #007AFF）。 */
    val demoIconBlue: Color = Color(0xFF007AFF)

    /** バッジ型アイコンの地色（明るい青 #0A84FF）。 */
    val demoIconVividBlue: Color = Color(0xFF0A84FF)

    // =============================================================================
    // Theme
    // =============================================================================

    /** MAUI 互換 Theme。 */
    val maui: Theme = Theme(
        separatorColor = mauiSeparator,
        backgroundColor = mauiViewBackground,
        cellBackgroundColor = mauiCellBackground,
        selectedColor = mauiSelected,
        cellAccentColor = mauiAccent,
        disabledTextColor = mauiDisabledText,
        rowHeight = -1,
        hasUnevenRows = true,
        headerTextColor = mauiHeaderText,
        headerBackgroundColor = mauiViewBackground,
        footerTextColor = mauiFooterText,
        footerBackgroundColor = mauiViewBackground,
        cellTitleColor = mauiDeepText,
    )

    /**
     * Section 装飾デモ用 Theme。
     *
     * 下地（`backgroundColor`）と Header / Footer の背景に PaleBackColorPrimary を敷き、
     * 箱（`cellBackgroundColor`）・separator・Header / Footer 文字色はライブラリ既定のまま残す。
     * アイコンはバッジ型（[SampleIconBadge]）に合わせたサイズ・角丸を指定する。
     * Section 装飾の 4 属性だけを引数で差し替えてプリセットを作る。
     *
     * @param sectionMargin Section 単位の外側余白（`null` で style ごとの既定）
     * @param sectionCornerRadius 箱の角丸半径（`null` で style ごとの既定）
     * @param sectionBorderWidth 箱のボーダー幅（`null` で実効 0dp）
     * @param sectionBorderColor 箱のボーダー色（`null` で実効透明）
     */
    fun sectionDecorationDemo(
        sectionMargin: PaddingValues? = null,
        sectionCornerRadius: Dp? = null,
        sectionBorderWidth: Dp? = null,
        sectionBorderColor: Color? = null,
    ): Theme = Theme(
        backgroundColor = mauiViewBackground,
        cellAccentColor = demoAccentGreen,
        headerBackgroundColor = mauiViewBackground,
        footerBackgroundColor = mauiViewBackground,
        cellIconSize = SampleIconBadge.size,
        cellIconRadius = SampleIconBadge.cornerRadius,
        sectionMargin = sectionMargin,
        sectionCornerRadius = sectionCornerRadius,
        sectionBorderWidth = sectionBorderWidth,
        sectionBorderColor = sectionBorderColor,
    )
}
