package jp.kamusoft.kssettingsview.ui

import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    fun `CellStyle titleColor も Theme cellTitleColor も未指定ならライトのタイトル既定が採用される`() {
        val effective = EffectiveStyle.from(
            theme = Theme(),
            cellStyle = CellStyle(titleColor = ComposeColor.Unspecified),
            darkTheme = false,
        )
        assertEquals(KsThemePalette.Light.cellTitle.toArgb(), effective.titleColor)
    }

    @Test
    fun `CellStyle titleColor も Theme cellTitleColor も未指定ならダークのタイトル既定が採用される`() {
        val effective = EffectiveStyle.from(
            theme = Theme(),
            cellStyle = CellStyle(titleColor = ComposeColor.Unspecified),
            darkTheme = true,
        )
        assertEquals(KsThemePalette.Dark.cellTitle.toArgb(), effective.titleColor)
    }

    @Test
    fun `CellStyle titleColor が指定されていれば EffectiveStyle はそれを使う`() {
        val red = ComposeColor.Red
        val effective = EffectiveStyle.from(
            theme = Theme(),
            cellStyle = CellStyle(titleColor = red),
            darkTheme = false,
        )
        assertEquals(red.toArgb(), effective.titleColor)
    }

    @Test
    fun `背景色は Theme cellBackgroundColor から取得される`() {
        val theme = Theme(cellBackgroundColor = ComposeColor.Black)
        val effective = EffectiveStyle.from(
            theme = theme,
            cellStyle = CellStyle(),
            darkTheme = false,
        )
        assertEquals(Color.BLACK, effective.backgroundColor)
    }

    @Test
    fun `CellStyle backgroundColor 指定時は Theme cellBackgroundColor よりも優先される`() {
        val yellow = ComposeColor.Yellow
        val effective = EffectiveStyle.from(
            theme = Theme(cellBackgroundColor = ComposeColor.White),
            cellStyle = CellStyle(backgroundColor = yellow),
            darkTheme = false,
        )
        assertEquals(yellow.toArgb(), effective.backgroundColor)
    }

    @Test
    fun `CellStyle accentColor 指定時は Theme cellAccentColor よりも優先される`() {
        val green = ComposeColor.Green
        val effective = EffectiveStyle.from(
            theme = Theme(cellAccentColor = ComposeColor.Blue),
            cellStyle = CellStyle(accentColor = green),
            darkTheme = false,
        )
        assertEquals(green.toArgb(), effective.accentColor)
    }

    @Test
    fun `CellStyle valueTextColor 指定時は descriptionColor よりも優先される`() {
        val darkGray = ComposeColor(0xFF333333)
        val effective = EffectiveStyle.from(
            theme = Theme(),
            cellStyle = CellStyle(valueTextColor = darkGray),
            darkTheme = false,
        )
        assertEquals(darkGray.toArgb(), effective.valueTextColor)
    }

    @Test
    fun `disabledTextColor は Theme から取得される`() {
        val lightGray = ComposeColor(0xFFB3B3B3)
        val effective = EffectiveStyle.from(
            theme = Theme(disabledTextColor = lightGray),
            cellStyle = CellStyle(),
            darkTheme = false,
        )
        assertEquals(lightGray.toArgb(), effective.disabledTextColor)
    }

    @Test
    fun `effectiveHeightDp は CellStyle cellHeight が指定されていればそれを採用する`() {
        val effective = EffectiveStyle.from(
            theme = Theme(rowHeight = 80),
            cellStyle = CellStyle(cellHeight = 80.dp),
            darkTheme = false,
        )
        assertEquals(80, effective.effectiveHeightDp)
    }

    @Test
    fun `effectiveHeightDp は Theme rowHeight が指定されていればそれを採用する`() {
        val effective = EffectiveStyle.from(
            theme = Theme(rowHeight = 80),
            cellStyle = CellStyle(),
            darkTheme = false,
        )
        assertEquals(80, effective.effectiveHeightDp)
    }

    /**
     * Android の最終下限は `MIN_ROW_HEIGHT_DP = 60dp`。`Theme(rowHeight = 20)` でも下限ガードで 60 になる。
     */
    @Test
    fun `effectiveHeightDp は最低 60dp で下限ガードされる`() {
        val effective = EffectiveStyle.from(
            theme = Theme(rowHeight = 20),
            cellStyle = CellStyle(),
            darkTheme = false,
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
        val effective = EffectiveStyle.from(
            theme = Theme(),
            cellStyle = CellStyle(),
            darkTheme = false,
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
        val effective = EffectiveStyle.from(
            theme = Theme(rowHeight = 30),
            cellStyle = CellStyle(),
            darkTheme = false,
        )
        assertEquals(60, effective.effectiveHeightDp)
    }

    @Test
    fun `isFixedHeight は Theme hasUnevenRows の否定で決まる`() {
        val fixed = EffectiveStyle.from(
            theme = Theme(hasUnevenRows = false),
            cellStyle = CellStyle(),
            darkTheme = false,
        )
        val uneven = EffectiveStyle.from(
            theme = Theme(hasUnevenRows = true),
            cellStyle = CellStyle(),
            darkTheme = false,
        )
        assertEquals(true, fixed.isFixedHeight)
        assertEquals(false, uneven.isFixedHeight)
    }

    @Test
    fun `titleColor Theme のみ指定_合成値は Theme を採用する`() {
        val themeColor = ComposeColor(0xFF335A99)
        val effective = EffectiveStyle.from(
            theme = Theme(cellTitleColor = themeColor),
            cellStyle = CellStyle(),
            darkTheme = false,
        )
        assertEquals(themeColor.toArgb(), effective.titleColor)
    }

    @Test
    fun `titleColor CellStyle のみ指定_合成値は CellStyle を採用する`() {
        val cellColor = ComposeColor(0xFFE60000)
        val effective = EffectiveStyle.from(
            theme = Theme(),
            cellStyle = CellStyle(titleColor = cellColor),
            darkTheme = false,
        )
        assertEquals(cellColor.toArgb(), effective.titleColor)
    }

    @Test
    fun `titleColor 両方指定_CellStyle が Theme より優先される`() {
        val cellColor = ComposeColor.Red
        val themeColor = ComposeColor.Blue
        val effective = EffectiveStyle.from(
            theme = Theme(cellTitleColor = themeColor),
            cellStyle = CellStyle(titleColor = cellColor),
            darkTheme = false,
        )
        assertEquals(cellColor.toArgb(), effective.titleColor)
    }

    @Test
    fun `titleFont Theme のみ指定_合成値は Theme を採用する`() {
        val themeFont = TextStyle(fontSize = 22.sp)
        val effective = EffectiveStyle.from(
            theme = Theme(cellTitleFont = themeFont),
            cellStyle = CellStyle(),
            darkTheme = false,
        )
        assertEquals(22.0f, effective.titleSizeSp, 0.01f)
    }

    @Test
    fun `titleFont 両方指定_CellStyle が Theme より優先される`() {
        val cellFont = TextStyle(fontSize = 19.sp)
        val themeFont = TextStyle(fontSize = 22.sp)
        val effective = EffectiveStyle.from(
            theme = Theme(cellTitleFont = themeFont),
            cellStyle = CellStyle(titleFont = cellFont),
            darkTheme = false,
        )
        assertEquals(19.0f, effective.titleSizeSp, 0.01f)
    }
}
