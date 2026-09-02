package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `Theme.backgroundColor` / `Theme.cellTitleColor` / `Theme.cellTitleFont` の新名が
 * 参照可能であることを確認する。
 *
 * [Theme] にこれらの名前のプロパティが存在しなければ本ターゲットのコンパイル自体が失敗するため、
 * 本テスト群は名前の存在をテストランタイムでも重ねて担保する位置づけ。
 */
class ThemeRenameTest {

    // MARK: - backgroundColor（旧 viewBackgroundColor）

    @Test
    fun `backgroundColor 新名で参照可能`() {
        val pink = Color(0xFFFF99AA)
        val theme = Theme(backgroundColor = pink)
        assertEquals(pink, theme.backgroundColor)
    }

    @Test
    fun `backgroundColor 既定値は DEFAULT_BACKGROUND_COLOR`() {
        val theme = Theme()
        assertEquals(Theme.DEFAULT_BACKGROUND_COLOR, theme.backgroundColor)
    }

    // MARK: - cellTitleColor（旧 titleColor）

    @Test
    fun `cellTitleColor 新名で参照可能`() {
        val blue = Color(0xFF0000FF)
        val theme = Theme(cellTitleColor = blue)
        assertEquals(blue, theme.cellTitleColor)
    }

    @Test
    fun `cellTitleColor 既定値は null`() {
        val theme = Theme()
        assertNull(theme.cellTitleColor)
    }

    // MARK: - cellTitleFont（旧 titleFont）

    @Test
    fun `cellTitleFont 新名で参照可能`() {
        val font = TextStyle(fontWeight = FontWeight.SemiBold)
        val theme = Theme(cellTitleFont = font)
        assertEquals(font, theme.cellTitleFont)
    }

    @Test
    fun `cellTitleFont 既定値は null`() {
        val theme = Theme()
        assertNull(theme.cellTitleFont)
    }

    // MARK: - backgroundColor / cellBackgroundColor が独立に保持される

    @Test
    fun `backgroundColor と cellBackgroundColor は独立`() {
        val viewBg = Color(0xFFF2EFE6)
        val cellBg = Color.White
        val theme = Theme(backgroundColor = viewBg, cellBackgroundColor = cellBg)
        assertEquals(viewBg, theme.backgroundColor)
        assertEquals(cellBg, theme.cellBackgroundColor)
        assertNotEquals(theme.backgroundColor, theme.cellBackgroundColor)
    }
}
