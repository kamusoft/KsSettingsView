package jp.kamusoft.kssettingsview.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.takeOrElse

/**
 * `KsSettingsView` のライブラリ既定値。
 *
 * 既定色は外観（ライト / ダーク）ごとの 2 セットで所有し、ロール名を持つ [Theme] として返す。
 * 既定値を基準に派生値を作りたい場合は、生値の定数ではなく本 factory が返す [Theme] の値から
 * 派生させる。
 *
 * factory は端末の外観と同じ側を選んで渡す前提である。端末がライトのまま [darkTheme] を渡すような
 * 食い違った組み合わせでは、下に挙げる「factory が埋めない色」（Cell のタイトル文字色と説明文の
 * 文字色）だけが端末側の外観の既定になり、背景と判読しにくい配色になる。現在の外観に合わせるには
 * [theme] を使い、外観と無関係に固定したい場合は返された [Theme] を `copy` してそれらの色も
 * 明示する。
 *
 * factory が値を入れるのは、list 下地・Cell 背景・separator・選択色・accent・disabled 文字・
 * Header / Footer の文字と背景の 10 色である。次の色は factory が返す [Theme] でも
 * `Color.Unspecified` のままであり、描画時に外観と他フィールドから決まる:
 *
 * - Cell のタイトル文字色と説明文の文字色 — 外観に応じた既定色が描画時に選ばれる。タイトルの既定色は
 *   Cell の種類ごとに異なり、`ButtonCell` はタップできる見た目の色になる
 * - value テキスト・hint テキストの文字色 — それぞれタイトル・accent の色を引き継ぐ
 * - 入力欄の placeholder 文字色 — プラットフォームの既定
 * - Section 枠線の色 — 透明（枠線なし）
 *
 * これらの色を固定したい場合は、返された [Theme] を `copy` して該当フィールドへ明示的に指定する。
 */
public object KsSettingsViewDefaults {

    /** ライト外観の既定色を持つ [Theme]。 */
    public fun lightTheme(): Theme = Theme(
        separatorColor = KsThemePalette.Light.separator,
        backgroundColor = KsThemePalette.Light.background,
        cellBackgroundColor = KsThemePalette.Light.cellBackground,
        selectedColor = KsThemePalette.Light.selected,
        cellAccentColor = KsThemePalette.Light.accent,
        disabledTextColor = KsThemePalette.Light.disabledText,
        headerTextColor = KsThemePalette.Light.headerText,
        headerBackgroundColor = KsThemePalette.Light.headerBackground,
        footerTextColor = KsThemePalette.Light.footerText,
        footerBackgroundColor = KsThemePalette.Light.footerBackground,
    )

    /** ダーク外観の既定色を持つ [Theme]。 */
    public fun darkTheme(): Theme = Theme(
        separatorColor = KsThemePalette.Dark.separator,
        backgroundColor = KsThemePalette.Dark.background,
        cellBackgroundColor = KsThemePalette.Dark.cellBackground,
        selectedColor = KsThemePalette.Dark.selected,
        cellAccentColor = KsThemePalette.Dark.accent,
        disabledTextColor = KsThemePalette.Dark.disabledText,
        headerTextColor = KsThemePalette.Dark.headerText,
        headerBackgroundColor = KsThemePalette.Dark.headerBackground,
        footerTextColor = KsThemePalette.Dark.footerText,
        footerBackgroundColor = KsThemePalette.Dark.footerBackground,
    )

    /**
     * 外観に対応する既定 [Theme]。
     *
     * @param darkTheme ダーク外観なら `true`
     */
    public fun theme(darkTheme: Boolean): Theme = if (darkTheme) darkTheme() else lightTheme()

    /** 現在の外観（`isSystemInDarkTheme()`）に対応する既定 [Theme]。 */
    @Composable
    public fun theme(): Theme = theme(darkTheme = isSystemInDarkTheme())
}

/**
 * 未指定（`Color.Unspecified`）の色を、指定された外観の既定セットの値で埋めた [Theme] を返す。
 *
 * 埋めるのは「解決済み [Theme] を直接読む描画箇所（list 下地・Cell 背景・separator・選択色・
 * accent・disabled 文字・Header / Footer の文字と背景）が使う色」だけであり、明示指定された色は
 * そのまま素通しする。
 *
 * 次の色は解決後も未指定のまま残し、後段の既定へ委ねる:
 *
 * - `cellTitleColor` / `cellDescriptionColor` — [EffectiveStyle] の解決が外観を受けて最終段で
 *   [KsThemePalette] から選ぶ（既定セットを返す factory も同じ理由で埋めない）。ここで埋めてしまうと
 *   ButtonCell のタイトル解決（ButtonCell → CellStyle → `Theme.cellTitleColor` → ButtonCell 既定）の
 *   最終段へ到達できない
 * - `cellValueTextColor` / `cellHintTextColor` — 他フィールド（タイトル・accent）へのフォールバック
 * - `cellPlaceholderColor` — 同梱テーマの hint 色（platform 既定。現在の外観で解決）
 * - `sectionBorderColor` — 実効透明
 *
 * 描画に関わる箇所へは本関数の結果だけを渡す。各消費者が個別に既定へ倒すと、埋め忘れが
 * `Color.Unspecified` の ARGB 変換（透明な黒）として表面化する。
 *
 * @param darkTheme ダーク外観なら `true`
 */
internal fun Theme.resolvedFor(darkTheme: Boolean): Theme {
    val defaults = KsSettingsViewDefaults.theme(darkTheme)
    return copy(
        separatorColor = separatorColor.takeOrElse { defaults.separatorColor },
        backgroundColor = backgroundColor.takeOrElse { defaults.backgroundColor },
        cellBackgroundColor = cellBackgroundColor.takeOrElse { defaults.cellBackgroundColor },
        selectedColor = selectedColor.takeOrElse { defaults.selectedColor },
        cellAccentColor = cellAccentColor.takeOrElse { defaults.cellAccentColor },
        disabledTextColor = disabledTextColor.takeOrElse { defaults.disabledTextColor },
        headerTextColor = headerTextColor.takeOrElse { defaults.headerTextColor },
        headerBackgroundColor = headerBackgroundColor.takeOrElse { defaults.headerBackgroundColor },
        footerTextColor = footerTextColor.takeOrElse { defaults.footerTextColor },
        footerBackgroundColor = footerBackgroundColor.takeOrElse { defaults.footerBackgroundColor },
    )
}
