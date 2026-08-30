package jp.kamusoft.kssettingsview.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [SectionBoxMetrics] による Section 装飾 4 属性の実効値解決を検証する。
 *
 * 未指定（`null`）は style ごとのライブラリ既定へ解決し、負および非有限（NaN・±∞）の寸法は 0 へ正規化する。
 * Classic は箱を描かないため角丸・ボーダーが落ち、余白の水平成分も無視される。
 */
class SectionBoxMetricsTest {

    /** dp → px 換算が 1:1 になる density。寸法の期待値を dp のまま書けるようにする。 */
    private val density = 1.0f

    private fun resolve(
        theme: Theme,
        style: KsSettingsViewStyle,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ): SectionBoxMetrics = SectionBoxMetrics.resolve(
        theme = theme,
        style = style,
        density = density,
        layoutDirection = layoutDirection,
    )

    // MARK: - 既定値の解決

    @Test
    fun `Modern の未指定はライブラリ既定の余白と角丸へ解決しボーダーは描かれない`() {
        val metrics = resolve(Theme(), KsSettingsViewStyle.Modern)

        assertEquals("上余白の既定は 22dp", 22.0f, metrics.marginTopPx, 0.001f)
        assertEquals("下余白の既定は 0", 0.0f, metrics.marginBottomPx, 0.001f)
        assertEquals("start 余白の既定は 16dp", 16.0f, metrics.marginLeftPx, 0.001f)
        assertEquals("end 余白の既定は 16dp", 16.0f, metrics.marginRightPx, 0.001f)
        assertEquals("角丸の既定は 26dp (core/ADR-0024)", 26.0f, metrics.cornerRadiusPx, 0.001f)
        assertEquals("ボーダー幅の実効既定は 0", 0.0f, metrics.borderWidthPx, 0.001f)
        assertEquals("ボーダー色の実効既定は透明", Color.Transparent.toArgb(), metrics.borderColor)
    }

    @Test
    fun `Classic の未指定は上下余白が Modern と同値になり水平は無視される`() {
        val metrics = resolve(Theme(), KsSettingsViewStyle.Classic)

        // Classic / Modern を切り替えても Section の上下間隔が変わらないよう既定値を揃える。
        assertEquals("上余白の既定は Modern と同値の 22dp", 22.0f, metrics.marginTopPx, 0.001f)
        assertEquals("下余白の既定は Modern と同値の 0", 0.0f, metrics.marginBottomPx, 0.001f)
        // 水平成分は Classic では常に 0 に落とす（Section 境界を全幅に保つ）。
        assertEquals("Classic の水平成分は無視される（左）", 0.0f, metrics.marginLeftPx, 0.001f)
        assertEquals("Classic の水平成分は無視される（右）", 0.0f, metrics.marginRightPx, 0.001f)
        assertEquals(0.0f, metrics.cornerRadiusPx, 0.001f)
        assertEquals(0.0f, metrics.borderWidthPx, 0.001f)
    }

    // MARK: - 指定値の反映

    @Test
    fun `Modern は指定された 4 属性をそのまま実効値にする`() {
        val theme = Theme(
            sectionMargin = PaddingValues(start = 8.dp, top = 4.dp, end = 10.dp, bottom = 6.dp),
            sectionCornerRadius = 20.dp,
            sectionBorderWidth = 2.dp,
            sectionBorderColor = Color(0xFF112233),
        )
        val metrics = resolve(theme, KsSettingsViewStyle.Modern)

        assertEquals(4.0f, metrics.marginTopPx, 0.001f)
        assertEquals(6.0f, metrics.marginBottomPx, 0.001f)
        assertEquals(8.0f, metrics.marginLeftPx, 0.001f)
        assertEquals(10.0f, metrics.marginRightPx, 0.001f)
        assertEquals(20.0f, metrics.cornerRadiusPx, 0.001f)
        assertEquals(2.0f, metrics.borderWidthPx, 0.001f)
        assertEquals(Color(0xFF112233).toArgb(), metrics.borderColor)
    }

    @Test
    fun `水平成分は start end 基準で layout direction に従って左右へ写る`() {
        val theme = Theme(
            sectionMargin = PaddingValues(start = 8.dp, top = 0.dp, end = 24.dp, bottom = 0.dp),
        )

        val ltr = resolve(theme, KsSettingsViewStyle.Modern, LayoutDirection.Ltr)
        assertEquals("LTR では start が左", 8.0f, ltr.marginLeftPx, 0.001f)
        assertEquals("LTR では end が右", 24.0f, ltr.marginRightPx, 0.001f)

        val rtl = resolve(theme, KsSettingsViewStyle.Modern, LayoutDirection.Rtl)
        assertEquals("RTL では start が右", 24.0f, rtl.marginLeftPx, 0.001f)
        assertEquals("RTL では end が左", 8.0f, rtl.marginRightPx, 0.001f)
    }

    @Test
    fun `Classic は水平成分を無視し上下成分だけを実効値にする`() {
        val theme = Theme(
            sectionMargin = PaddingValues(start = 16.dp, top = 9.dp, end = 16.dp, bottom = 7.dp),
            sectionCornerRadius = 20.dp,
            sectionBorderWidth = 3.dp,
            sectionBorderColor = Color(0xFF112233),
        )
        val metrics = resolve(theme, KsSettingsViewStyle.Classic)

        assertEquals("上成分は効く", 9.0f, metrics.marginTopPx, 0.001f)
        assertEquals("下成分は効く", 7.0f, metrics.marginBottomPx, 0.001f)
        assertEquals("leading 成分は無視する", 0.0f, metrics.marginLeftPx, 0.001f)
        assertEquals("trailing 成分は無視する", 0.0f, metrics.marginRightPx, 0.001f)
        assertEquals("Classic は箱を描かないため角丸は 0", 0.0f, metrics.cornerRadiusPx, 0.001f)
        assertEquals("Classic は箱を描かないためボーダー幅は 0", 0.0f, metrics.borderWidthPx, 0.001f)
        assertEquals("Classic のボーダー色は透明", Color.Transparent.toArgb(), metrics.borderColor)
    }

    // MARK: - 負値の正規化と角丸の clamp

    @Test
    fun `負の寸法は 0 として扱う`() {
        // Compose 標準の PaddingValues ファクトリは負値を構築時に拒否するため、
        // 成分をそのまま返す実装で「描画側の正規化」を観測する。
        val theme = Theme(
            sectionMargin = RawPaddingValues(start = (-8).dp, top = (-4).dp, end = (-2).dp, bottom = (-1).dp),
            sectionCornerRadius = (-12).dp,
            sectionBorderWidth = (-3).dp,
        )
        val metrics = resolve(theme, KsSettingsViewStyle.Modern)

        assertEquals(0.0f, metrics.marginTopPx, 0.001f)
        assertEquals(0.0f, metrics.marginBottomPx, 0.001f)
        assertEquals(0.0f, metrics.marginLeftPx, 0.001f)
        assertEquals(0.0f, metrics.marginRightPx, 0.001f)
        assertEquals(0.0f, metrics.cornerRadiusPx, 0.001f)
        assertEquals(0.0f, metrics.borderWidthPx, 0.001f)
    }

    @Test
    fun `非有限の寸法は 0 として扱う`() {
        // Compose 標準の PaddingValues ファクトリは NaN の成分を構築時に拒否するため、
        // 成分をそのまま返す実装で「描画側の正規化」を観測する。
        val theme = Theme(
            sectionMargin = RawPaddingValues(
                start = Float.NaN.dp,
                top = Float.POSITIVE_INFINITY.dp,
                end = Float.NEGATIVE_INFINITY.dp,
                bottom = Float.NaN.dp,
            ),
            sectionCornerRadius = Float.POSITIVE_INFINITY.dp,
            sectionBorderWidth = Float.NaN.dp,
        )
        val metrics = resolve(theme, KsSettingsViewStyle.Modern)

        assertEquals("+∞ の上余白は 0", 0.0f, metrics.marginTopPx, 0.001f)
        assertEquals("NaN の下余白は 0", 0.0f, metrics.marginBottomPx, 0.001f)
        assertEquals("NaN の start 余白は 0", 0.0f, metrics.marginLeftPx, 0.001f)
        assertEquals("-∞ の end 余白は 0", 0.0f, metrics.marginRightPx, 0.001f)
        assertEquals("+∞ の角丸は 0", 0.0f, metrics.cornerRadiusPx, 0.001f)
        assertEquals("NaN のボーダー幅は 0", 0.0f, metrics.borderWidthPx, 0.001f)
    }

    @Test
    fun `角丸は箱の短辺の半分へ clamp される`() {
        val theme = Theme(sectionCornerRadius = 100.dp)
        val metrics = resolve(theme, KsSettingsViewStyle.Modern)

        assertEquals("指定値そのものは保持する", 100.0f, metrics.cornerRadiusPx, 0.001f)
        assertEquals("短辺 40 の箱では 20 へ抑える", 20.0f, metrics.clampedCornerRadius(300.0f, 40.0f), 0.001f)
        assertEquals("短辺が幅側でも同じ", 15.0f, metrics.clampedCornerRadius(30.0f, 400.0f), 0.001f)
        assertEquals("寸法 0 の箱では 0", 0.0f, metrics.clampedCornerRadius(0.0f, 0.0f), 0.001f)
    }

    @Test
    fun `指定値が箱に収まるときは clamp されない`() {
        val theme = Theme(sectionCornerRadius = 12.dp)
        val metrics = resolve(theme, KsSettingsViewStyle.Modern)

        assertEquals(12.0f, metrics.clampedCornerRadius(300.0f, 200.0f), 0.001f)
    }
}
