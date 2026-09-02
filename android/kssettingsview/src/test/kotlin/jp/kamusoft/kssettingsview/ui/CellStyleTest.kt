package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [CellStyle]（UI 層）のテスト。
 *
 * [CellStyle] は UI 層に置かれ、フィールド型に Compose の Native 型を用いる（core/ADR-0009）。
 * 本テストは構築・等価性・デフォルト値（全フィールドが null）の動作を検証する。
 */
class CellStyleTest {

    @Test
    fun `デフォルトコンストラクタは全フィールド null`() {
        val style = CellStyle()
        assertNull(style.titleColor)
        assertNull(style.titleFont)
        assertNull(style.descriptionColor)
        assertNull(style.descriptionFont)
        assertNull(style.valueTextColor)
        assertNull(style.valueTextFont)
        assertNull(style.iconSize)
        assertNull(style.iconRadius)
        assertNull(style.cellHeight)
        assertNull(style.hintTextColor)
        assertNull(style.hintTextFont)
        assertNull(style.backgroundColor)
        assertNull(style.accentColor)
    }

    @Test
    fun `Compose Color を直接受け取れる`() {
        val style = CellStyle(
            titleColor = Color.Red,
            backgroundColor = Color(0xFFFFFFFF),
            accentColor = Color.Green,
        )
        assertEquals(Color.Red, style.titleColor)
        assertEquals(Color.White, style.backgroundColor)
        assertEquals(Color.Green, style.accentColor)
    }

    @Test
    fun `Dp を直接受け取れる`() {
        val style = CellStyle(
            iconSize = 24.dp,
            iconRadius = 4.dp,
            cellHeight = 80.dp,
        )
        assertEquals(24.dp, style.iconSize)
        assertEquals(4.dp, style.iconRadius)
        assertEquals(80.dp, style.cellHeight)
    }

    @Test
    fun `TextStyle を直接受け取れる`() {
        val font = TextStyle.Default
        val style = CellStyle(titleFont = font, descriptionFont = font)
        assertEquals(font, style.titleFont)
        assertEquals(font, style.descriptionFont)
    }

    @Test
    fun `data class equals は同値で true、別値で false`() {
        val a = CellStyle(titleColor = Color.Red)
        val b = CellStyle(titleColor = Color.Red)
        val c = CellStyle(titleColor = Color.Blue)
        assertEquals(a, b)
        assertNotEquals(a, c)
    }
}
