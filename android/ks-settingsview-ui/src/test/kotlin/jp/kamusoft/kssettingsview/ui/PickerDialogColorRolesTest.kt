package jp.kamusoft.kssettingsview.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 選択ダイアログ（時刻 / 日付）の色ロール導出（[ColorRoles] / [PickerDialogColors]）を検証する。
 *
 * 本テストは `android.graphics.Color` を使わないビット演算のみを対象とするため Robolectric は不要。
 */
class PickerDialogColorRolesTest {

    // MARK: - アクセント上の文字色（黒 / 白の自動選択）

    @Test
    fun `明るいアクセントには黒が選ばれる`() {
        // MAUI 互換 Theme の cellAccentColor (#FFBF00)。相対輝度 0.585 で黒とのコントラストが高い。
        assertEquals(ColorRoles.BLACK, ColorRoles.contrastingBlackOrWhite(0xFFFFBF00.toInt()))
    }

    @Test
    fun `暗いアクセントには白が選ばれる`() {
        assertEquals(ColorRoles.WHITE, ColorRoles.contrastingBlackOrWhite(0xFF333333.toInt()))
    }

    @Test
    fun `純白には黒 純黒には白が選ばれる`() {
        assertEquals(ColorRoles.BLACK, ColorRoles.contrastingBlackOrWhite(0xFFFFFFFF.toInt()))
        assertEquals(ColorRoles.WHITE, ColorRoles.contrastingBlackOrWhite(0xFF000000.toInt()))
    }

    @Test
    fun `判定が切り替わる境界近傍のグレー`() {
        // 黒 / 白のコントラスト比が等しくなる相対輝度は sqrt(0.05 * 1.05) - 0.05 ≒ 0.17913。
        // 無彩色ではこれが 8bit 値 117 / 118 の間に落ちる。
        assertEquals(ColorRoles.WHITE, ColorRoles.contrastingBlackOrWhite(0xFF757575.toInt()))
        assertEquals(ColorRoles.BLACK, ColorRoles.contrastingBlackOrWhite(0xFF767676.toInt()))
    }

    // MARK: - 合成

    @Test
    fun `blend は透過率 0 で base を 不透明な top の透過率 1 で top を返す`() {
        val base = 0xFFF2EFE6.toInt()
        val top = 0xFFFFBF00.toInt()
        assertEquals(base, ColorRoles.blend(base, top, 0.0f))
        assertEquals(top, ColorRoles.blend(base, top, 1.0f))
    }

    @Test
    fun `blend は base のアルファを保つ`() {
        val result = ColorRoles.blend(base = 0x80FFFFFF.toInt(), top = 0xFF000000.toInt(), topAlpha = 0.5f)
        assertEquals(0x80, (result ushr 24) and 0xFF)
    }

    @Test
    fun `blend は top 自身のアルファを重ねる強さに掛け合わせる`() {
        val base = 0xFFF2EFE6.toInt()
        // 242,239,230 に 255,191,0 を 50%（top のアルファ 0x80）で → 248,214,114
        assertEquals(0xFFF8D672.toInt(), ColorRoles.blend(base, 0x80FFBF00.toInt(), 1.0f))
        // 同じ強さは「不透明な top を 50% の比率で重ねる」と一致する
        assertEquals(
            ColorRoles.blend(base, 0xFFFFBF00.toInt(), 128f / 255f),
            ColorRoles.blend(base, 0x80FFBF00.toInt(), 1.0f),
        )
    }

    @Test
    fun `compositeOver は不透明な top をそのまま 完全透明な top では base を返す`() {
        val base = 0xFFF2EFE6.toInt()
        assertEquals(0xFFFFBF00.toInt(), ColorRoles.compositeOver(base, 0xFFFFBF00.toInt()))
        assertEquals(base, ColorRoles.compositeOver(base, 0x00FFBF00))
    }

    // MARK: - PickerDialogColors の派生色

    private val mauiColors = PickerDialogColors(
        background = 0xFFF2EFE6.toInt(),
        accent = 0xFFFFBF00.toInt(),
        text = 0xFF555555.toInt(),
    )

    @Test
    fun `onAccent はアクセントとコントラストの高い方`() {
        assertEquals(ColorRoles.BLACK, mauiColors.onAccent)
        assertEquals(ColorRoles.WHITE, mauiColors.copy(accent = 0xFF102030.toInt()).onAccent)
    }

    @Test
    fun `不透明アクセントの onAccent は背景に依らない`() {
        val accent = 0xFF333333.toInt()
        assertEquals(ColorRoles.WHITE, mauiColors.copy(background = 0xFFFFFFFF.toInt(), accent = accent).onAccent)
        assertEquals(ColorRoles.WHITE, mauiColors.copy(background = 0xFF000000.toInt(), accent = accent).onAccent)
    }

    @Test
    fun `明るい背景に載る半透明の黒アクセントには黒が選ばれる`() {
        // 白背景に 25% の黒 → 実効面は明るいグレー。RGB だけを見ると「黒い面」と誤判定してしまう。
        val colors = mauiColors.copy(background = 0xFFFFFFFF.toInt(), accent = 0x40000000)
        assertEquals(ColorRoles.BLACK, colors.onAccent)
    }

    @Test
    fun `暗い背景に載る半透明の白アクセントには白が選ばれる`() {
        val colors = mauiColors.copy(background = 0xFF101010.toInt(), accent = 0x40FFFFFF.toInt())
        assertEquals(ColorRoles.WHITE, colors.onAccent)
    }

    @Test
    fun `半透明の有彩色アクセントも合成後の明度で判定する`() {
        // 暗い紺色でも 25% なら白背景を透かして明るく見えるため、黒文字が読みやすい。
        val colors = mauiColors.copy(background = 0xFFFFFFFF.toInt(), accent = 0x40102030)
        assertEquals(ColorRoles.BLACK, colors.onAccent)
    }

    @Test
    fun `accentSurface は背景へ合成したアクセントの実効色`() {
        assertEquals(mauiColors.accent, mauiColors.accentSurface)
        // 242,239,230 に 255,191,0 を 50% → 248,214,114
        assertEquals(0xFFF8D672.toInt(), mauiColors.copy(accent = 0x80FFBF00.toInt()).accentSurface)
    }

    // MARK: - 減光した派生色（日付選択ダイアログの無効表示・補助表示）

    @Test
    fun `無効表示の文字は通常文字を背景へ 38 パーセントで重ねた色`() {
        // 242,239,230 に 85,85,85 を 38% → 182,180,174
        assertEquals(0xFFB6B4AE.toInt(), mauiColors.disabledText)
    }

    @Test
    fun `無効表示の操作はアクセントを背景へ 38 パーセントで重ねた色`() {
        // 242,239,230 に 255,191,0 を 38% → 246,220,142
        assertEquals(0xFFF6DC8E.toInt(), mauiColors.disabledAccent)
    }

    @Test
    fun `補助表示の文字は無効表示より濃い`() {
        // 242,239,230 に 85,85,85 を 70% → 132,131,128
        assertEquals(0xFF848380.toInt(), mauiColors.subduedText)
        // 補助表示は「読める情報」、無効表示は「操作できない状態」なので取り違えない。
        assertNotEquals(mauiColors.subduedText, mauiColors.disabledText)
    }

    @Test
    fun `減光した派生色は暗い背景では明るい側へ寄る`() {
        val dark = PickerDialogColors(
            background = 0xFF101010.toInt(),
            accent = 0xFFFFBF00.toInt(),
            text = 0xFFEEEEEE.toInt(),
        )
        // 16 に 238 を 38% → 100
        assertEquals(0xFF646464.toInt(), dark.disabledText)
        // 減光後も背景より明るく、通常文字より暗い（明暗関係が反転しない）。
        assertTrue(ColorRoles.relativeLuminance(dark.background) < ColorRoles.relativeLuminance(dark.disabledText))
        assertTrue(ColorRoles.relativeLuminance(dark.disabledText) < ColorRoles.relativeLuminance(dark.text))
    }
}
