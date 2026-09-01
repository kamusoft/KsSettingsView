package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `EffectiveStyle` 新規アクセサ関数（`effectiveValueTextColor` 等）と、`cellTitleFontSize`
 * による fontSize 上書き、ButtonCell 4 段優先の解決順序を検証する。
 *
 * 本テストは `Context` を必要としない論理層のアクセサ群のみを対象とするため Robolectric は不要。
 */
class EffectiveStyleResolutionTest {

    // MARK: - effectiveTitleColor / effectiveTitleFont

    @Test
    fun `effectiveTitleColor は CellStyle 優先`() {
        val cellColor = Color.Red
        val themeColor = Color.Blue
        val result = EffectiveStyle.effectiveTitleColor(
            cellStyle = CellStyle(titleColor = cellColor),
            theme = Theme(cellTitleColor = themeColor),
        )
        assertEquals(cellColor, result)
    }

    @Test
    fun `effectiveTitleColor は Theme フォールバック`() {
        val themeColor = Color.Blue
        val result = EffectiveStyle.effectiveTitleColor(
            cellStyle = CellStyle(),
            theme = Theme(cellTitleColor = themeColor),
        )
        assertEquals(themeColor, result)
    }

    @Test
    fun `effectiveTitleColor は既定フォールバック`() {
        val result = EffectiveStyle.effectiveTitleColor(
            cellStyle = CellStyle(),
            theme = Theme(),
        )
        assertEquals(Theme.DEFAULT_CELL_TITLE_COLOR, result)
    }

    // MARK: - effectiveValueTextColor / effectiveValueTextFont

    @Test
    fun `effectiveValueTextColor は CellStyle 優先`() {
        val cellColor = Color.Red
        val themeValueColor = Color.Green
        val result = EffectiveStyle.effectiveValueTextColor(
            cellStyle = CellStyle(valueTextColor = cellColor),
            theme = Theme(cellValueTextColor = themeValueColor),
        )
        assertEquals(cellColor, result)
    }

    @Test
    fun `effectiveValueTextColor は Theme cellValueTextColor フォールバック`() {
        val themeValueColor = Color.Green
        val result = EffectiveStyle.effectiveValueTextColor(
            cellStyle = CellStyle(),
            theme = Theme(cellValueTextColor = themeValueColor),
        )
        assertEquals(themeValueColor, result)
    }

    @Test
    fun `effectiveValueTextColor は Theme cellTitleColor にフォールバック`() {
        // cellValueTextColor が null のとき cellTitleColor から落ちる
        val titleColor = Color(0xFF334455)
        val result = EffectiveStyle.effectiveValueTextColor(
            cellStyle = CellStyle(),
            theme = Theme(cellTitleColor = titleColor),
        )
        assertEquals(titleColor, result)
    }

    @Test
    fun `effectiveValueTextColor 全て null なら既定`() {
        val result = EffectiveStyle.effectiveValueTextColor(
            cellStyle = CellStyle(),
            theme = Theme(),
        )
        assertEquals(Theme.DEFAULT_CELL_TITLE_COLOR, result)
    }

    // MARK: - effectiveDescriptionColor

    @Test
    fun `effectiveDescriptionColor は CellStyle 優先`() {
        val cellColor = Color(0xFF222222)
        val result = EffectiveStyle.effectiveDescriptionColor(
            cellStyle = CellStyle(descriptionColor = cellColor),
            theme = Theme(cellDescriptionColor = Color.Blue),
        )
        assertEquals(cellColor, result)
    }

    @Test
    fun `effectiveDescriptionColor は Theme フォールバック`() {
        val themeColor = Color(0xFF333333)
        val result = EffectiveStyle.effectiveDescriptionColor(
            cellStyle = CellStyle(),
            theme = Theme(cellDescriptionColor = themeColor),
        )
        assertEquals(themeColor, result)
    }

    @Test
    fun `effectiveDescriptionColor は既定フォールバック`() {
        val result = EffectiveStyle.effectiveDescriptionColor(
            cellStyle = CellStyle(),
            theme = Theme(),
        )
        assertEquals(Theme.DEFAULT_CELL_DESCRIPTION_COLOR, result)
    }

    // MARK: - effectiveHintTextColor

    @Test
    fun `effectiveHintTextColor は CellStyle 優先`() {
        val result = EffectiveStyle.effectiveHintTextColor(
            cellStyle = CellStyle(hintTextColor = Color.Red),
            theme = Theme(cellHintTextColor = Color.Green),
        )
        assertEquals(Color.Red, result)
    }

    @Test
    fun `effectiveHintTextColor は Theme フォールバック`() {
        val result = EffectiveStyle.effectiveHintTextColor(
            cellStyle = CellStyle(),
            theme = Theme(cellHintTextColor = Color.Red),
        )
        assertEquals(Color.Red, result)
    }

    @Test
    fun `effectiveHintTextColor は cellAccentColor にフォールバック`() {
        val accent = Color.Magenta
        val result = EffectiveStyle.effectiveHintTextColor(
            cellStyle = CellStyle(),
            theme = Theme(cellAccentColor = accent),
        )
        assertEquals(accent, result)
    }

    // MARK: - effectivePlaceholderColor

    @Test
    fun `effectivePlaceholderColor は Cell 固有値を最優先する`() {
        val result = EffectiveStyle.effectivePlaceholderColor(
            entryPlaceholderColor = Color.Red,
            cellStyle = CellStyle(placeholderColor = Color.Green),
            theme = Theme(cellPlaceholderColor = Color.Blue),
        )
        assertEquals(Color.Red, result)
    }

    @Test
    fun `effectivePlaceholderColor は CellStyle 優先`() {
        val result = EffectiveStyle.effectivePlaceholderColor(
            cellStyle = CellStyle(placeholderColor = Color.Green),
            theme = Theme(cellPlaceholderColor = Color.Blue),
        )
        assertEquals(Color.Green, result)
    }

    @Test
    fun `effectivePlaceholderColor は Theme フォールバック`() {
        val result = EffectiveStyle.effectivePlaceholderColor(
            entryPlaceholderColor = null,
            cellStyle = CellStyle(),
            theme = Theme(cellPlaceholderColor = Color.Blue),
        )
        assertEquals(Color.Blue, result)
    }

    @Test
    fun `effectivePlaceholderColor は全段未指定でプラットフォーム既定を表す null になる`() {
        assertNull(
            EffectiveStyle.effectivePlaceholderColor(
                entryPlaceholderColor = null,
                cellStyle = CellStyle(),
                theme = Theme(),
            ),
        )
    }

    // MARK: - effectiveIconSize / effectiveIconRadius

    @Test
    fun `effectiveIconSize は CellStyle 優先`() {
        val result = EffectiveStyle.effectiveIconSize(
            cellStyle = CellStyle(iconSize = 40.dp),
            theme = Theme(cellIconSize = 32.dp),
        )
        assertEquals(40.dp, result)
    }

    @Test
    fun `effectiveIconSize は Theme フォールバック`() {
        val result = EffectiveStyle.effectiveIconSize(
            cellStyle = CellStyle(),
            theme = Theme(cellIconSize = 32.dp),
        )
        assertEquals(32.dp, result)
    }

    @Test
    fun `effectiveIconSize は既定 24dp`() {
        val result = EffectiveStyle.effectiveIconSize(
            cellStyle = CellStyle(),
            theme = Theme(),
        )
        assertEquals(24.dp, result)
    }

    @Test
    fun `effectiveIconRadius は既定 0dp`() {
        val result = EffectiveStyle.effectiveIconRadius(
            cellStyle = CellStyle(),
            theme = Theme(),
        )
        assertEquals(0.dp, result)
    }

    @Test
    fun `effectiveIconRadius は CellStyle 優先`() {
        val result = EffectiveStyle.effectiveIconRadius(
            cellStyle = CellStyle(iconRadius = 4.dp),
            theme = Theme(cellIconRadius = 12.dp),
        )
        assertEquals(4.dp, result)
    }

    @Test
    fun `effectiveIconRadius は Theme フォールバック`() {
        val result = EffectiveStyle.effectiveIconRadius(
            cellStyle = CellStyle(),
            theme = Theme(cellIconRadius = 12.dp),
        )
        assertEquals(12.dp, result)
    }

    // MARK: - icon size / radius の無効値（未指定として次の段へ送る）

    @Test
    fun `effectiveIconSize は 0 以下の CellStyle 指定を無視して Theme へ解決する`() {
        for (invalid in listOf(0.dp, (-1).dp, (-24).dp)) {
            val result = EffectiveStyle.effectiveIconSize(
                cellStyle = CellStyle(iconSize = invalid),
                theme = Theme(cellIconSize = 32.dp),
            )
            assertEquals("iconSize = $invalid は未指定として扱う", 32.dp, result)
        }
    }

    @Test
    fun `effectiveIconSize は非有限の CellStyle 指定を無視して Theme へ解決する`() {
        for (invalid in listOf(Dp(Float.NaN), Dp(Float.POSITIVE_INFINITY), Dp(Float.NEGATIVE_INFINITY))) {
            val result = EffectiveStyle.effectiveIconSize(
                cellStyle = CellStyle(iconSize = invalid),
                theme = Theme(cellIconSize = 32.dp),
            )
            assertEquals("iconSize = $invalid は未指定として扱う", 32.dp, result)
        }
    }

    @Test
    fun `effectiveIconSize は Theme の無効値も飛ばして既定へ解決する`() {
        for (invalid in listOf(0.dp, (-32).dp, Dp(Float.NaN), Dp(Float.POSITIVE_INFINITY))) {
            val result = EffectiveStyle.effectiveIconSize(
                cellStyle = CellStyle(iconSize = 0.dp),
                theme = Theme(cellIconSize = invalid),
            )
            assertEquals("cellIconSize = $invalid は未指定として扱う", 24.dp, result)
        }
    }

    @Test
    fun `effectiveIconRadius は負値と非有限の CellStyle 指定を無視して Theme へ解決する`() {
        for (invalid in listOf((-1).dp, (-12).dp, Dp(Float.NaN), Dp(Float.POSITIVE_INFINITY))) {
            val result = EffectiveStyle.effectiveIconRadius(
                cellStyle = CellStyle(iconRadius = invalid),
                theme = Theme(cellIconRadius = 12.dp),
            )
            assertEquals("iconRadius = $invalid は未指定として扱う", 12.dp, result)
        }
    }

    @Test
    fun `effectiveIconRadius は Theme の無効値も飛ばして既定へ解決する`() {
        for (invalid in listOf((-12).dp, Dp(Float.NaN), Dp(Float.NEGATIVE_INFINITY))) {
            val result = EffectiveStyle.effectiveIconRadius(
                cellStyle = CellStyle(),
                theme = Theme(cellIconRadius = invalid),
            )
            assertEquals("cellIconRadius = $invalid は未指定として扱う", 0.dp, result)
        }
    }

    @Test
    fun `effectiveIconRadius の 0dp は角丸なしの指定として採用する`() {
        val result = EffectiveStyle.effectiveIconRadius(
            cellStyle = CellStyle(iconRadius = 0.dp),
            theme = Theme(cellIconRadius = 12.dp),
        )
        assertEquals(0.dp, result)
    }

    // MARK: - effectiveBackgroundColor / effectiveAccentColor

    @Test
    fun `effectiveBackgroundColor は CellStyle 優先`() {
        val result = EffectiveStyle.effectiveBackgroundColor(
            cellStyle = CellStyle(backgroundColor = Color.Red),
            theme = Theme(cellBackgroundColor = Color.White),
        )
        assertEquals(Color.Red, result)
    }

    @Test
    fun `effectiveAccentColor は Theme フォールバック`() {
        val result = EffectiveStyle.effectiveAccentColor(
            cellStyle = CellStyle(),
            theme = Theme(cellAccentColor = Color.Cyan),
        )
        assertEquals(Color.Cyan, result)
    }

    // MARK: - cellTitleFontSize による fontSize 上書き

    @Test
    fun `cellTitleFontSize 正の値で titleFont の fontSize が上書きされる`() {
        val theme = Theme(
            cellTitleFont = TextStyle(fontSize = 14.sp),
            cellTitleFontSize = 20.0,
        )
        val result = EffectiveStyle.effectiveTitleFont(
            cellStyle = CellStyle(),
            theme = theme,
        )
        assertEquals(20.0f, result.fontSize.value, 0.01f)
    }

    @Test
    fun `cellTitleFontSize が minus1 のときは上書きしない`() {
        val theme = Theme(
            cellTitleFont = TextStyle(fontSize = 14.sp),
            cellTitleFontSize = -1.0,
        )
        val result = EffectiveStyle.effectiveTitleFont(
            cellStyle = CellStyle(),
            theme = theme,
        )
        assertEquals(14.0f, result.fontSize.value, 0.01f)
    }

    @Test
    fun `cellTitleFontSize がゼロのときは上書きしない`() {
        val theme = Theme(
            cellTitleFont = TextStyle(fontSize = 14.sp),
            cellTitleFontSize = 0.0,
        )
        val result = EffectiveStyle.effectiveTitleFont(
            cellStyle = CellStyle(),
            theme = theme,
        )
        assertEquals(14.0f, result.fontSize.value, 0.01f)
    }

    // MARK: - ButtonCell 4 段優先解決

    @Test
    fun `effectiveButtonTitleColor は ButtonCell 個別が最優先`() {
        val result = EffectiveStyle.effectiveButtonTitleColor(
            buttonCellTitleColor = Color.Red,
            cellStyle = CellStyle(titleColor = Color.Green),
            theme = Theme(cellTitleColor = Color.Blue),
        )
        assertEquals(Color.Red, result)
    }

    @Test
    fun `effectiveButtonTitleColor は ButtonCell が null なら CellStyle 採用`() {
        val result = EffectiveStyle.effectiveButtonTitleColor(
            buttonCellTitleColor = null,
            cellStyle = CellStyle(titleColor = Color.Green),
            theme = Theme(cellTitleColor = Color.Blue),
        )
        assertEquals(Color.Green, result)
    }

    @Test
    fun `effectiveButtonTitleColor は ButtonCell と CellStyle が null なら Theme 採用`() {
        val result = EffectiveStyle.effectiveButtonTitleColor(
            buttonCellTitleColor = null,
            cellStyle = CellStyle(),
            theme = Theme(cellTitleColor = Color.Blue),
        )
        assertEquals(Color.Blue, result)
    }

    @Test
    fun `effectiveButtonTitleColor は全て null なら既定 ButtonTitleColor`() {
        // ButtonCell.titleColor は 4 段優先で解決し、4 段目はプラットフォーム既定の
        // Button 慣習色（クロスプラットフォーム既定 SYSTEM_BLUE 相当）となる。
        // Compose 経路 (Context 不要) では `Theme.DEFAULT_BUTTON_TITLE_COLOR` を返す。
        val result = EffectiveStyle.effectiveButtonTitleColor(
            buttonCellTitleColor = null,
            cellStyle = CellStyle(),
            theme = Theme(),
        )
        assertEquals(Theme.DEFAULT_BUTTON_TITLE_COLOR, result)
    }

    // MARK: - TextStyle equals 安定性

    @Test
    fun `同一 TextStyle インスタンスを使った 2 つの Theme は data class equals で等価`() {
        val font = TextStyle(fontSize = 16.sp)
        val theme1 = Theme(cellTitleFont = font)
        val theme2 = Theme(cellTitleFont = font)
        assertEquals(theme1, theme2)
    }

    @Test
    fun `内容同一の TextStyle を渡した 2 つの Theme も equals で等価`() {
        // TextStyle は data class ではないが equals を実装している。fontSize / fontWeight など
        // 主要属性が同値なら参照不一致でも等価判定が成立する。
        val font1 = TextStyle(fontSize = 16.sp)
        val font2 = TextStyle(fontSize = 16.sp)
        val theme1 = Theme(cellTitleFont = font1)
        val theme2 = Theme(cellTitleFont = font2)
        assertEquals(theme1, theme2)
    }

    // ===== effectiveHeaderFont / effectiveFooterFont =====

    @Test
    fun `effectiveHeaderFont は headerFont 未指定 fontSize 未指定なら既定`() {
        val theme = Theme()
        val result = EffectiveStyle.effectiveHeaderFont(theme = theme)
        assertEquals(TextStyle.Default, result)
    }

    @Test
    fun `effectiveHeaderFont は headerFont 指定だけならそのまま使う`() {
        val custom = TextStyle(fontSize = 18.sp)
        val theme = Theme(headerFont = custom)
        val result = EffectiveStyle.effectiveHeaderFont(theme = theme)
        assertEquals(custom, result)
    }

    @Test
    fun `effectiveHeaderFont は headerFontSize 優先で size 上書き`() {
        val base = TextStyle(fontSize = 14.sp)
        val theme = Theme(headerFontSize = 24.0, headerFont = base)
        val result = EffectiveStyle.effectiveHeaderFont(theme = theme)
        assertEquals(24.sp, result.fontSize)
    }

    @Test
    fun `effectiveHeaderFont は headerFontSize 単独でも size 反映`() {
        val theme = Theme(headerFontSize = 22.0)
        val result = EffectiveStyle.effectiveHeaderFont(theme = theme)
        assertEquals(22.sp, result.fontSize)
    }

    @Test
    fun `effectiveFooterFont は footerFont 未指定なら既定`() {
        val theme = Theme()
        val result = EffectiveStyle.effectiveFooterFont(theme = theme)
        assertEquals(TextStyle.Default, result)
    }

    @Test
    fun `effectiveFooterFont は footerFontSize 優先で size 上書き`() {
        val base = TextStyle(fontSize = 14.sp)
        val theme = Theme(footerFontSize = 28.0, footerFont = base)
        val result = EffectiveStyle.effectiveFooterFont(theme = theme)
        assertEquals(28.sp, result.fontSize)
    }
}
