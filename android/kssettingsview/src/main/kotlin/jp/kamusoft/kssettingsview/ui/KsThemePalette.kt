package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color

/**
 * ライブラリが所有する既定色の生値（light / dark の 2 セット）。
 *
 * 公開面にはロール名の `Theme` を返す [KsSettingsViewDefaults] の factory だけを出し、生値は本
 * パレットに閉じる。light は既存利用者の見た目を変えないための現行値、dark はライト側と同じ出所
 * （iOS の外観別システムパレットを不透明に近似した値）から採った対であり、iOS 側の dark 既定と
 * 同じ生値を置く（core/ADR-0030）。
 *
 * valueText / hintText / placeholder / Section の枠線色は本パレットに持たない。これらは他の
 * フィールドへのフォールバックか platform 既定に委ねる契約であり、外観で決まる既定値を持たない。
 */
internal object KsThemePalette {

    /** ライト外観の既定色。 */
    internal object Light {

        /** SettingsView 全体の下地。 */
        val background: Color = Color(0xFFFFFFFF)

        /** Cell の背景。 */
        val cellBackground: Color = Color(0xFFFFFFFF)

        /** Cell 間の区切り線。 */
        val separator: Color = Color(0xFFC8C7CC)

        /** Cell 押下時の背景。 */
        val selected: Color = Color(0xFFD9D9D9)

        /** 選択系 Cell の強調色。 */
        val accent: Color = Color(0xFF007AFF)

        /** `isEnabled = false` の文字。 */
        val disabledText: Color = Color(0xFF999999)

        /** Header の背景。 */
        val headerBackground: Color = Color(0xFFF2F2F7)

        /** Header の文字。 */
        val headerText: Color = Color(0xFF6D6D72)

        /** Footer の背景。 */
        val footerBackground: Color = Color(0xFFF2F2F7)

        /** Footer の文字。 */
        val footerText: Color = Color(0xFF6D6D72)

        /** Cell タイトルの文字。 */
        val cellTitle: Color = Color(0xFF000000)

        /** Cell 説明文の文字。 */
        val cellDescription: Color = Color(0xFF6D6D72)

        /** ButtonCell タイトルの文字（tappable に見える慣習色）。 */
        val buttonTitle: Color = Color(0xFF007AFF)
    }

    /** ダーク外観の既定色。 */
    internal object Dark {

        /** SettingsView 全体の下地。 */
        val background: Color = Color(0xFF000000)

        /** Cell の背景。 */
        val cellBackground: Color = Color(0xFF1C1C1E)

        /** Cell 間の区切り線。 */
        val separator: Color = Color(0xFF38383A)

        /** Cell 押下時の背景。 */
        val selected: Color = Color(0xFF2C2C2E)

        /** 選択系 Cell の強調色。 */
        val accent: Color = Color(0xFF0A84FF)

        /** `isEnabled = false` の文字。 */
        val disabledText: Color = Color(0xFF636366)

        /** Header の背景。 */
        val headerBackground: Color = Color(0xFF000000)

        /** Header の文字。 */
        val headerText: Color = Color(0xFF8E8E93)

        /** Footer の背景。 */
        val footerBackground: Color = Color(0xFF000000)

        /** Footer の文字。 */
        val footerText: Color = Color(0xFF8E8E93)

        /** Cell タイトルの文字。 */
        val cellTitle: Color = Color(0xFFFFFFFF)

        /** Cell 説明文の文字。 */
        val cellDescription: Color = Color(0xFF8E8E93)

        /** ButtonCell タイトルの文字（tappable に見える慣習色）。 */
        val buttonTitle: Color = Color(0xFF0A84FF)
    }

    /**
     * 外観に対応する Cell タイトルの既定色を返す。
     *
     * タイトルと説明文の既定は [Theme] に載せない（[KsSettingsViewDefaults] の factory が返す既定
     * セットにも、解決済み [Theme] にも入れない）。代わりに [EffectiveStyle] の解決の最終段で外観から
     * 直接選ぶ。`Theme.cellTitleColor` が値を持つと、ButtonCell のタイトル解決
     * （ButtonCell → CellStyle → `Theme.cellTitleColor` → ButtonCell 既定）が最終段へ到達できず、
     * ライトでも ButtonCell のタイトルが通常タイトルの色になってしまう。
     */
    fun cellTitle(darkTheme: Boolean): Color =
        if (darkTheme) Dark.cellTitle else Light.cellTitle

    /** 外観に対応する Cell 説明文の既定色を返す。 */
    fun cellDescription(darkTheme: Boolean): Color =
        if (darkTheme) Dark.cellDescription else Light.cellDescription

    /**
     * 外観に対応する ButtonCell タイトルの既定色を返す。
     *
     * ButtonCell のタイトル既定は `Theme` のフィールドを持たないため、[Theme] の解決ではなく
     * 実効スタイルの解決時に外観から直接選ぶ。
     */
    fun buttonTitle(darkTheme: Boolean): Color =
        if (darkTheme) Dark.buttonTitle else Light.buttonTitle
}
