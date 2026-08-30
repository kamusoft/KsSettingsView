package jp.kamusoft.kssettingsview.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [Theme]（UI 層）のテスト。
 *
 * [Theme] は UI 層に置かれ、フィールド型に Compose の Native 型を用いる（core/ADR-0009）。
 * 本テストは既定値・等価性と、`backgroundColor` / `cellTitleColor` / `cellTitleFont` /
 * `cellTitleFontSize`、Cell 全体既定（valueText / description / hint / icon）、
 * Header/Footer Font といった各フィールドの振る舞いを検証する。
 */
class ThemeTest {

    @Test
    fun `デフォルトコンストラクタは中立的な既定値を持つ`() {
        val theme = Theme()
        // backgroundColor は白系
        assertEquals(Theme.DEFAULT_BACKGROUND_COLOR, theme.backgroundColor)
        // rowHeight = -1, hasUnevenRows = true（refine-cell-layout-after-unify-review で
        // オリジナル AiForms iOS / Android 踏襲の「Auto 高さ + 下限保証」既定に変更）
        assertEquals(-1, theme.rowHeight)
        assertTrue(theme.hasUnevenRows)
        // headerFontSize / footerFontSize = -1
        assertEquals(-1.0, theme.headerFontSize, 0.0001)
        assertEquals(-1.0, theme.footerFontSize, 0.0001)
        // headerHeight = -1.0
        assertEquals(-1.0, theme.headerHeight, 0.0001)
        // cellTitleFontSize = -1.0
        assertEquals(-1.0, theme.cellTitleFontSize, 0.0001)
        // disabledTextColor はやや薄い灰色（#999999）
        assertEquals(Color(0xFF999999), theme.disabledTextColor)
        // cellTitleColor / cellTitleFont は null
        assertNull(theme.cellTitleColor)
        assertNull(theme.cellTitleFont)
        // 新規 Cell 全体既定はすべて null
        assertNull(theme.cellValueTextColor)
        assertNull(theme.cellValueTextFont)
        assertNull(theme.cellDescriptionColor)
        assertNull(theme.cellDescriptionFont)
        assertNull(theme.cellHintTextColor)
        assertNull(theme.cellHintFont)
        assertNull(theme.cellIconSize)
        assertNull(theme.cellIconRadius)
        // 新規 Header/Footer Font も null
        assertNull(theme.headerFont)
        assertNull(theme.footerFont)
        // scrollIndicatorVisible は true
        assertTrue(theme.scrollIndicatorVisible)
    }

    @Test
    fun `cellTitleColor と cellTitleFont は nullable で既定 null`() {
        val theme = Theme()
        assertNull(theme.cellTitleColor)
        assertNull(theme.cellTitleFont)
    }

    @Test
    fun `Compose Color を直接受け取れる`() {
        // KsColor などの中間型を介さず、Compose Color を直接渡せる
        val theme = Theme(
            separatorColor = Color(0xFFE6DAB9),
            cellBackgroundColor = Color.White,
            cellAccentColor = Color(0xFFFFBF00),
        )
        assertEquals(Color(0xFFE6DAB9), theme.separatorColor)
        assertEquals(Color.White, theme.cellBackgroundColor)
        assertEquals(Color(0xFFFFBF00), theme.cellAccentColor)
    }

    @Test
    fun `backgroundColor と cellBackgroundColor は独立`() {
        val theme = Theme(
            backgroundColor = Color(0xFFF2EFE6),
            cellBackgroundColor = Color.White,
        )
        assertEquals(Color(0xFFF2EFE6), theme.backgroundColor)
        assertEquals(Color.White, theme.cellBackgroundColor)
        assertNotEquals(theme.backgroundColor, theme.cellBackgroundColor)
    }

    @Test
    fun `data class equals は同値で true、別値で false`() {
        val a = Theme(separatorColor = Color(0xFFE6DAB9))
        val b = Theme(separatorColor = Color(0xFFE6DAB9))
        val c = Theme(separatorColor = Color(0xFFFFBF00))
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `cellTitleFont に TextStyle を渡せる`() {
        val font = TextStyle(fontWeight = FontWeight.Bold)
        val theme = Theme(cellTitleFont = font)
        assertEquals(font, theme.cellTitleFont)
    }

    @Test
    fun `新規 Cell 全体既定フィールドを保持できる`() {
        // Cell 全体既定（cellHintTextColor / cellIconSize 等）を Theme に直接渡せる
        val font = TextStyle(fontWeight = FontWeight.Medium)
        val theme = Theme(
            cellValueTextColor = Color.Red,
            cellValueTextFont = font,
            cellDescriptionColor = Color.Blue,
            cellDescriptionFont = font,
            cellHintTextColor = Color.Green,
            cellHintFont = font,
            cellIconSize = androidx.compose.ui.unit.Dp(32f),
            cellIconRadius = androidx.compose.ui.unit.Dp(4f),
        )
        assertEquals(Color.Red, theme.cellValueTextColor)
        assertEquals(Color.Blue, theme.cellDescriptionColor)
        assertEquals(Color.Green, theme.cellHintTextColor)
        assertEquals(androidx.compose.ui.unit.Dp(32f), theme.cellIconSize)
        assertEquals(androidx.compose.ui.unit.Dp(4f), theme.cellIconRadius)
    }

    @Test
    fun `headerFont と footerFont を保持できる`() {
        val headerFont = TextStyle(fontWeight = FontWeight.Bold)
        val footerFont = TextStyle(fontWeight = FontWeight.Light)
        val theme = Theme(
            headerFont = headerFont,
            footerFont = footerFont,
            headerHeight = 60.0,
        )
        assertEquals(headerFont, theme.headerFont)
        assertEquals(footerFont, theme.footerFont)
        assertEquals(60.0, theme.headerHeight, 0.0001)
    }

    // MARK: - Section 装飾 4 属性

    @Test
    fun `Section 装飾 4 属性の既定はすべて未指定`() {
        val theme = Theme()
        assertNull(theme.sectionMargin)
        assertNull(theme.sectionCornerRadius)
        assertNull(theme.sectionBorderWidth)
        assertNull(theme.sectionBorderColor)
    }

    @Test
    fun `Section 装飾 4 属性を保持できる`() {
        val margin = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
        val theme = Theme(
            sectionMargin = margin,
            sectionCornerRadius = 20.dp,
            sectionBorderWidth = 2.dp,
            sectionBorderColor = Color(0xFF335577),
        )
        assertEquals(margin, theme.sectionMargin)
        assertEquals(20.dp, theme.sectionCornerRadius)
        assertEquals(2.dp, theme.sectionBorderWidth)
        assertEquals(Color(0xFF335577), theme.sectionBorderColor)
    }

    @Test
    fun `Section 装飾 4 属性は値等価性に参加する`() {
        val base = Theme(
            sectionMargin = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
            sectionCornerRadius = 20.dp,
            sectionBorderWidth = 2.dp,
            sectionBorderColor = Color(0xFF335577),
        )

        assertNotEquals("margin の差は検出する", base, base.copy(sectionMargin = null))
        assertNotEquals("cornerRadius の差は検出する", base, base.copy(sectionCornerRadius = 21.dp))
        assertNotEquals("borderWidth の差は検出する", base, base.copy(sectionBorderWidth = 3.dp))
        assertNotEquals("borderColor の差は検出する", base, base.copy(sectionBorderColor = Color(0xFF000000)))
    }

    @Test
    fun `sectionMargin の等価比較は PaddingValues の equals へ委譲する`() {
        val a = Theme(sectionMargin = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp))
        val b = Theme(sectionMargin = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp))
        val c = Theme(sectionMargin = PaddingValues(start = 16.dp, top = 13.dp, end = 16.dp, bottom = 12.dp))

        assertEquals("別インスタンスでも成分が同じなら等しい", a, b)
        assertEquals("hashCode も一致する", a.hashCode(), b.hashCode())
        assertNotEquals("成分が違えば等しくない", a, c)
    }
}
