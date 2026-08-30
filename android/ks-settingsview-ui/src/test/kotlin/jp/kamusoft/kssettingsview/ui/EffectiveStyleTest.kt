package jp.kamusoft.kssettingsview.ui

import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * `EffectiveStyle` の合成ルールテスト。
 *
 * [Theme] / [CellStyle] は UI 層に置かれ、フィールド型に Compose の `Color` / `TextStyle` / `Dp`
 * を用いる（core/ADR-0009）。[EffectiveStyle] は両者を合成しつつ、内部で `toArgb()` 等の
 * プラットフォーム変換を行う。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EffectiveStyleTest {

    @Test
    fun `CellStyle titleColor も Theme titleColor も null なら textColorPrimary が採用される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = Theme(),
            cellStyle = CellStyle(titleColor = null),
        )
        assertEquals(0xFF, Color.alpha(effective.titleColor))
        assertEquals(false, effective.titleColorIsExplicit)
    }

    @Test
    fun `CellStyle titleColor が指定されていれば EffectiveStyle はそれを使う`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val red = ComposeColor.Red
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = Theme(),
            cellStyle = CellStyle(titleColor = red),
        )
        assertEquals(red.toArgb(), effective.titleColor)
    }

    @Test
    fun `背景色は Theme cellBackgroundColor から取得される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val theme = Theme(cellBackgroundColor = ComposeColor.Black)
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = theme,
            cellStyle = CellStyle(),
        )
        assertEquals(Color.BLACK, effective.backgroundColor)
    }

    @Test
    fun `CellStyle backgroundColor 指定時は Theme cellBackgroundColor よりも優先される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val yellow = ComposeColor.Yellow
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = Theme(cellBackgroundColor = ComposeColor.White),
            cellStyle = CellStyle(backgroundColor = yellow),
        )
        assertEquals(yellow.toArgb(), effective.backgroundColor)
    }

    @Test
    fun `CellStyle accentColor 指定時は Theme cellAccentColor よりも優先される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val green = ComposeColor.Green
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = Theme(cellAccentColor = ComposeColor.Blue),
            cellStyle = CellStyle(accentColor = green),
        )
        assertEquals(green.toArgb(), effective.accentColor)
    }

    @Test
    fun `CellStyle valueTextColor 指定時は descriptionColor よりも優先される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val darkGray = ComposeColor(0xFF333333)
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = Theme(),
            cellStyle = CellStyle(valueTextColor = darkGray),
        )
        assertEquals(darkGray.toArgb(), effective.valueTextColor)
    }

    @Test
    fun `disabledTextColor は Theme から取得される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val lightGray = ComposeColor(0xFFB3B3B3)
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = Theme(disabledTextColor = lightGray),
            cellStyle = CellStyle(),
        )
        assertEquals(lightGray.toArgb(), effective.disabledTextColor)
    }

    @Test
    fun `effectiveHeightDp は CellStyle cellHeight が指定されていればそれを採用する`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = Theme(rowHeight = 80),
            cellStyle = CellStyle(cellHeight = 80.dp),
        )
        assertEquals(80, effective.effectiveHeightDp)
    }

    @Test
    fun `effectiveHeightDp は Theme rowHeight が指定されていればそれを採用する`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = Theme(rowHeight = 80),
            cellStyle = CellStyle(),
        )
        assertEquals(80, effective.effectiveHeightDp)
    }

    /**
     * Android の最終下限は `MIN_ROW_HEIGHT_DP = 60dp`。`Theme(rowHeight = 20)` でも下限ガードで 60 になる。
     */
    @Test
    fun `effectiveHeightDp は最低 60dp で下限ガードされる`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = Theme(rowHeight = 20),
            cellStyle = CellStyle(),
        )
        assertEquals(60, effective.effectiveHeightDp)
        assertEquals(EffectiveStyle.MIN_ROW_HEIGHT_DP, effective.effectiveHeightDp)
    }

    // Theme.rowHeight 未指定時の既定 60dp

    /**
     * `Theme()` 引数なし（`rowHeight = -1`、`hasUnevenRows = true`）かつ `CellStyle()` も未指定の場合、
     * オリジナル `AiForms.Maui.SettingsView` の `AiRecyclerView.UpdateRowHeight()` 踏襲で
     * `MIN_ROW_HEIGHT_DP = 60` を base として採用し、結果として `effectiveHeightDp = 60` を返す。
     */
    @Test
    fun `effectiveHeightDp は Theme rowHeight 未指定時に 60dp を採用する`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = Theme(),
            cellStyle = CellStyle(),
        )
        assertEquals(60, effective.effectiveHeightDp)
        assertEquals(EffectiveStyle.MIN_ROW_HEIGHT_DP, effective.effectiveHeightDp)
    }

    /**
     * `Theme(rowHeight = 30)` のときは base = 30 を採用するが、`MIN_ROW_HEIGHT_DP = 60dp` で下限ガード
     * されるため最終値は `60` となる。
     */
    @Test
    fun `effectiveHeightDp は Theme rowHeight 30dp 指定時に下限 60dp で打ち止める`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = Theme(rowHeight = 30),
            cellStyle = CellStyle(),
        )
        assertEquals(60, effective.effectiveHeightDp)
    }

    @Test
    fun `isFixedHeight は Theme hasUnevenRows の否定で決まる`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val fixed = EffectiveStyle.from(
            context = ctx,
            theme = Theme(hasUnevenRows = false),
            cellStyle = CellStyle(),
        )
        val uneven = EffectiveStyle.from(
            context = ctx,
            theme = Theme(hasUnevenRows = true),
            cellStyle = CellStyle(),
        )
        assertEquals(true, fixed.isFixedHeight)
        assertEquals(false, uneven.isFixedHeight)
    }

    @Test
    fun `titleColor Theme のみ指定_合成値は Theme を採用し titleColorIsExplicit は true`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val themeColor = ComposeColor(0xFF335A99)
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = Theme(cellTitleColor = themeColor),
            cellStyle = CellStyle(),
        )
        assertEquals(themeColor.toArgb(), effective.titleColor)
        assertEquals(true, effective.titleColorIsExplicit)
    }

    @Test
    fun `titleColor CellStyle のみ指定_合成値は CellStyle を採用し titleColorIsExplicit は true`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cellColor = ComposeColor(0xFFE60000)
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = Theme(),
            cellStyle = CellStyle(titleColor = cellColor),
        )
        assertEquals(cellColor.toArgb(), effective.titleColor)
        assertEquals(true, effective.titleColorIsExplicit)
    }

    @Test
    fun `titleColor 両方指定_CellStyle が Theme より優先される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cellColor = ComposeColor.Red
        val themeColor = ComposeColor.Blue
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = Theme(cellTitleColor = themeColor),
            cellStyle = CellStyle(titleColor = cellColor),
        )
        assertEquals(cellColor.toArgb(), effective.titleColor)
        assertEquals(true, effective.titleColorIsExplicit)
    }

    @Test
    fun `titleFont Theme のみ指定_合成値は Theme を採用する`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val themeFont = TextStyle(fontSize = 22.sp)
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = Theme(cellTitleFont = themeFont),
            cellStyle = CellStyle(),
        )
        assertEquals(22.0f, effective.titleSizeSp, 0.01f)
    }

    @Test
    fun `titleFont 両方指定_CellStyle が Theme より優先される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cellFont = TextStyle(fontSize = 19.sp)
        val themeFont = TextStyle(fontSize = 22.sp)
        val effective = EffectiveStyle.from(
            context = ctx,
            theme = Theme(cellTitleFont = themeFont),
            cellStyle = CellStyle(titleFont = cellFont),
        )
        assertEquals(19.0f, effective.titleSizeSp, 0.01f)
    }
}
