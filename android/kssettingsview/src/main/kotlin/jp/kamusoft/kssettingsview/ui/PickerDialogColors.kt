package jp.kamusoft.kssettingsview.ui

import androidx.annotation.ColorInt

/**
 * 選択面（時刻シート / カレンダーダイアログ）へ適用する色ロールの束。
 *
 * 入力は「Theme / CellStyle / Cell から解決済みの 3 色」だけであり、
 * 派生色（アクセント上文字色・中間面・アクセントの淡い塗り・減光文字）は本クラスが導出する。
 * 色の生値をここに持ち込まないための境界であり、導出規則の SoT でもある。
 *
 * 3 色はいずれも利用側が渡す任意の `Color` であり、半透明でありうる。派生色は
 * [background] の面へ重ねた実効色として導出するため、半透明色を渡しても
 * 実際に描画される見た目と判定が食い違わない。
 *
 * 派生色のうち [disabledText] / [subduedText] / [disabledAccent] はカレンダー選択面の
 * 部位で使う。
 *
 * @property background 背景ロール（`Theme.backgroundColor` の解決値）
 * @property accent 強調ロール（`Cell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor`）
 * @property text 通常文字ロール（`CellStyle.titleColor` → `Theme.cellTitleColor` → プラットフォーム既定）
 */
internal data class PickerDialogColors(
    @ColorInt val background: Int,
    @ColorInt val accent: Int,
    @ColorInt val text: Int,
) {
    /**
     * アクセント面に直接載る文字の色。黒と白のうち [accentSurface] とのコントラスト比が高い方。
     *
     * 判定の対象は [accent] そのものではなく [accentSurface]。半透明のアクセントは
     * 背景を透かして薄く見えるため、RGB だけで判定すると実際の見た目と逆の文字色を選ぶ。
     */
    @get:ColorInt
    val onAccent: Int get() = ColorRoles.contrastingBlackOrWhite(accentSurface)

    /**
     * アクセント面が実際に描画されたときの色（[accent] を [background] の上に重ねた実効色）。
     *
     * [accent] が不透明なら [accent] と一致する。
     */
    @get:ColorInt
    val accentSurface: Int get() = ColorRoles.compositeOver(base = background, top = accent)

    /**
     * 無効・非活性の文字と枠に使う減光した通常文字色
     * （範囲外の日付・非フォーカス時の入力欄枠）。
     */
    @get:ColorInt
    val disabledText: Int
        get() = ColorRoles.blend(base = background, top = text, topAlpha = DISABLED_ALPHA)

    /** 補助表示（helper / placeholder）に使う、通常文字よりわずかに退いた文字色。 */
    @get:ColorInt
    val subduedText: Int
        get() = ColorRoles.blend(base = background, top = text, topAlpha = SUBDUED_ALPHA)

    /** 無効状態の操作（確定できないときの OK など）に使う減光したアクセント色。 */
    @get:ColorInt
    val disabledAccent: Int
        get() = ColorRoles.blend(base = background, top = accent, topAlpha = DISABLED_ALPHA)

    internal companion object {
        /**
         * 無効状態を表す減光率。
         *
         * material-components が disabled 状態の文字に使う既定の透過率（38%）に合わせる。
         */
        const val DISABLED_ALPHA: Float = 0.38f

        /**
         * 補助表示の減光率。
         *
         * helper 表示のデザイン確定値。無効表示より濃くして
         * 「読める補助情報」と「操作できない状態」を見分けられるようにする。
         */
        const val SUBDUED_ALPHA: Float = 0.70f
    }
}

/**
 * 色ロール導出の純関数群。
 *
 * `android.graphics.Color` に依存せずビット演算だけで完結させ、Robolectric 無しの
 * 単体テストからも直接検証できるようにする（`EffectiveStyle` の既定色定数と同じ方針）。
 */
internal object ColorRoles {

    /** 不透明な黒（ARGB）。アクセント上文字の候補。 */
    @ColorInt
    const val BLACK: Int = 0xFF000000.toInt()

    /** 不透明な白（ARGB）。アクセント上文字の候補。 */
    @ColorInt
    const val WHITE: Int = 0xFFFFFFFF.toInt()

    /**
     * `surface` に対してコントラスト比が高い方の色（黒 or 白）を返す。
     *
     * WCAG 2.1 の相対輝度とコントラスト比の定義に従う。黒とのコントラスト比は
     * `(L + 0.05) / 0.05`、白とのコントラスト比は `1.05 / (L + 0.05)` であり、
     * 両者が等しくなる境界は `L + 0.05 = sqrt(0.05 * 1.05)` である。
     * 境界ちょうどの場合は黒を返す（比が等しく、可読性の差が無いため）。
     *
     * `surface` は「実際に目に入る面の色」であることを前提とし、そのアルファは見ない。
     * 半透明の色を直接渡すと実描画と食い違う（例: 白い面に載せた半透明の黒は明るいグレーに
     * 見えるが、RGB だけを見ると黒と判定される）ため、[compositeOver] で下地へ合成してから渡す。
     */
    @ColorInt
    fun contrastingBlackOrWhite(@ColorInt surface: Int): Int {
        val luminance = relativeLuminance(surface)
        val contrastWithBlack = (luminance + CONTRAST_OFFSET) / CONTRAST_OFFSET
        val contrastWithWhite = (1.0 + CONTRAST_OFFSET) / (luminance + CONTRAST_OFFSET)
        return if (contrastWithBlack >= contrastWithWhite) BLACK else WHITE
    }

    /**
     * `base` の面へ `top` をその固有アルファのまま重ねたときに見える色を返す。
     *
     * `top` が不透明なら `top` と一致する。
     */
    @ColorInt
    fun compositeOver(@ColorInt base: Int, @ColorInt top: Int): Int = blend(base, top, 1.0f)

    /**
     * `base` の面へ `top` を重ねた色を返す（アルファは `base` のものを保つ）。
     *
     * 重ねる強さは `top` 自身のアルファと `topAlpha` の積であり、実際に描画したときの
     * 見た目（source-over 合成）と一致する。`base` は不透明な面色を前提とし、
     * 戻り値は「その面が最終的に何色に見えるか」を表す。
     *
     * @param topAlpha `top` を重ねる比率。0.0（`base` のまま）〜 1.0（`top` の固有アルファのまま）
     */
    @ColorInt
    fun blend(@ColorInt base: Int, @ColorInt top: Int, topAlpha: Float): Int {
        val topOwnAlpha = ((top ushr 24) and 0xFF) / 255.0f
        val a = topOwnAlpha * topAlpha.coerceIn(0.0f, 1.0f)
        val alpha = (base ushr 24) and 0xFF
        val r = mix((base ushr 16) and 0xFF, (top ushr 16) and 0xFF, a)
        val g = mix((base ushr 8) and 0xFF, (top ushr 8) and 0xFF, a)
        val b = mix(base and 0xFF, top and 0xFF, a)
        return (alpha shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** WCAG 2.1 の相対輝度（0.0〜1.0）。面色の RGB だけを見る（アルファは見ない）。 */
    fun relativeLuminance(@ColorInt color: Int): Double {
        val r = linearize(((color ushr 16) and 0xFF) / 255.0)
        val g = linearize(((color ushr 8) and 0xFF) / 255.0)
        val b = linearize((color and 0xFF) / 255.0)
        return LUMA_R * r + LUMA_G * g + LUMA_B * b
    }

    private fun linearize(channel: Double): Double =
        if (channel <= SRGB_THRESHOLD) {
            channel / SRGB_LOW_DIVISOR
        } else {
            Math.pow((channel + SRGB_OFFSET) / SRGB_SCALE, SRGB_GAMMA)
        }

    private fun mix(base: Int, top: Int, topAlpha: Float): Int =
        (base + (top - base) * topAlpha).toInt().coerceIn(0, 255)

    /** コントラスト比の定義に現れる加算オフセット。 */
    private const val CONTRAST_OFFSET: Double = 0.05

    private const val LUMA_R: Double = 0.2126
    private const val LUMA_G: Double = 0.7152
    private const val LUMA_B: Double = 0.0722

    private const val SRGB_THRESHOLD: Double = 0.03928
    private const val SRGB_LOW_DIVISOR: Double = 12.92
    private const val SRGB_OFFSET: Double = 0.055
    private const val SRGB_SCALE: Double = 1.055
    private const val SRGB_GAMMA: Double = 2.4
}
