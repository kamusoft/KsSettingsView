package jp.kamusoft.kssettingsview.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [CustomCell] の値としての振る舞い（等価性・静的形・可視性・DSL 規約準拠）を検証する。
 *
 * 描画・タップ・スタイル適用は [CustomCellRenderingTest] が扱う。
 */
class CustomCellTest {

    /** 中身の描画結果に依存しないダミー builder。 */
    private val noopBuilder: @androidx.compose.runtime.Composable (String) -> Unit = { Box(Modifier) }

    // MARK: - 等価性

    @Test
    fun `builder だけが異なるインスタンスは等価`() {
        val a = CustomCell(id = "c1", content = "same") { Box(Modifier) }
        val b = CustomCell(id = "c1", content = "same") { Box(Modifier.dummySize()) }
        assertEquals("builder は等価性に参加しない", a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `onTap だけが異なるインスタンスは等価`() {
        val a = CustomCell(id = "c1", content = "same", onTap = { }, builder = noopBuilder)
        val b = CustomCell(id = "c1", content = "same", onTap = { }, builder = noopBuilder)
        val c = CustomCell(id = "c1", content = "same", onTap = null, builder = noopBuilder)
        assertEquals(a, b)
        assertEquals("onTap の有無も等価性に参加しない", a, c)
    }

    @Test
    fun `content が異なれば非等価`() {
        val a = CustomCell(id = "c1", content = "A", builder = noopBuilder)
        val b = CustomCell(id = "c1", content = "B", builder = noopBuilder)
        assertNotEquals(a, b)
    }

    @Test
    fun `showArrow だけが異なれば非等価`() {
        val a = CustomCell(id = "c1", content = "A", showArrow = false, builder = noopBuilder)
        val b = CustomCell(id = "c1", content = "A", showArrow = true, builder = noopBuilder)
        assertNotEquals("表示に効くスカラーは等価性に参加する", a, b)
    }

    @Test
    fun `id と style と isEnabled と isVisible も等価性に参加する`() {
        val base = CustomCell(id = "c1", content = "A", builder = noopBuilder)
        assertNotEquals(base, CustomCell(id = "c2", content = "A", builder = noopBuilder))
        assertNotEquals(
            base,
            CustomCell(
                id = "c1",
                style = CellStyle(cellHeight = 100.dp),
                content = "A",
                builder = noopBuilder,
            ),
        )
        assertNotEquals(
            base,
            CustomCell(id = "c1", content = "A", isEnabled = false, builder = noopBuilder),
        )
        assertNotEquals(
            base,
            CustomCell(id = "c1", content = "A", isVisible = false, builder = noopBuilder),
        )
    }

    @Test
    fun `content 型が異なれば非等価`() {
        val a = CustomCell(id = "c1", content = "1", builder = noopBuilder)
        val b = CustomCell(id = "c1", content = 1) { Box(Modifier) }
        assertNotEquals(a, b)
    }

    // MARK: - 静的コンテンツの省略形

    @Test
    fun `content なしの省略形は空 content を持つ`() {
        val cell = CustomCell(id = "c1") { Box(Modifier) }
        assertSame(CustomCellEmptyContent, cell.content)
    }

    @Test
    fun `省略形の等価性は content 以外の参加要素で決まる`() {
        val a = CustomCell(id = "c1") { Box(Modifier) }
        val b = CustomCell(id = "c1") { Box(Modifier.dummySize()) }
        assertEquals("空 content は常に相等なので id とスカラーだけで決まる", a, b)
        assertEquals(a.hashCode(), b.hashCode())

        val c = CustomCell(id = "c1", showArrow = true) { Box(Modifier) }
        assertNotEquals(a, c)
        val d = CustomCell(id = "c2") { Box(Modifier) }
        assertNotEquals(a, d)
    }

    // MARK: - 既定値と DSL 規約

    @Test
    fun `id の既定値は custom 接頭辞で自動採番される`() {
        val a = CustomCell(content = "x", builder = noopBuilder)
        val b = CustomCell(content = "x", builder = noopBuilder)
        assertTrue(a.id.startsWith("custom-"))
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun `既定値は showArrow false と onTap null と isEnabled true と isVisible true`() {
        val cell = CustomCell(content = "x", builder = noopBuilder)
        assertFalse(cell.showArrow)
        assertEquals(null, cell.onTap)
        assertTrue(cell.isEnabled)
        assertTrue(cell.isVisible)
    }

    @Test
    fun `withDSLId は id だけを差し替えた複製を返す`() {
        val onTap = { }
        val orig = CustomCell(
            id = "old",
            style = CellStyle(cellHeight = 120.dp),
            content = "A",
            showArrow = true,
            onTap = onTap,
            isEnabled = false,
            isVisible = false,
            builder = noopBuilder,
        )
        @Suppress("UNCHECKED_CAST")
        val copy = orig.withDSLId("new") as CustomCell<String>
        assertEquals("new", copy.id)
        assertEquals("A", copy.content)
        assertEquals(CellStyle(cellHeight = 120.dp), copy.style)
        assertTrue(copy.showArrow)
        assertSame(onTap, copy.onTap)
        assertFalse(copy.isEnabled)
        assertFalse(copy.isVisible)
    }

    @Test
    fun `withDSLStyle は style だけを差し替えた複製を返す`() {
        val orig = CustomCell(id = "c1", content = "A", showArrow = true, builder = noopBuilder)
        val newStyle = CellStyle(cellHeight = 80.dp)
        @Suppress("UNCHECKED_CAST")
        val copy = orig.withDSLStyle(newStyle) as CustomCell<String>
        assertEquals("c1", copy.id)
        assertEquals(newStyle, copy.style)
        assertEquals("A", copy.content)
        assertTrue(copy.showArrow)
    }

    @Test
    fun `DSL 規約のうち id と style には準拠し icon には準拠しない`() {
        val cell: Any = CustomCell(content = "A", builder = noopBuilder)
        assertTrue(cell is DSLReidentifiableCell)
        assertTrue(cell is DSLStyleModifiableCell)
        assertFalse("アイコン領域を持たないため icon modifier の対象外", cell is DSLIconModifiableCell)
    }

    // MARK: - 可視性フィルタへの参加

    @Test
    fun `VisibilityAware に準拠する`() {
        val cell: Any = CustomCell(content = "A", builder = noopBuilder)
        assertTrue(cell is VisibilityAware)
        assertTrue((cell as VisibilityAware).isVisible)
    }

    @Test
    fun `isVisible false の CustomCell は visible projection から除外される`() {
        val section = jp.kamusoft.kssettingsview.core.Section(
            id = "s1",
            cells = listOf(
                CustomCell(id = "v", content = "A", builder = noopBuilder),
                CustomCell(id = "h", content = "B", isVisible = false, builder = noopBuilder),
                LabelCell(id = "after", title = "後続"),
            ),
        )
        val rows = KsSettingsView.flatten(listOf(section))
            .filterIsInstance<CellListItem.CellRow>()
        assertEquals(listOf("v", "after"), rows.map { it.cell.id })
    }
}

/** builder の中身が異なることを型レベルで示すためだけの Modifier 差分。 */
private fun Modifier.dummySize(): Modifier = this.then(Modifier)
