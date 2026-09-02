package jp.kamusoft.kssettingsview.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.CustomCell
import jp.kamusoft.kssettingsview.ui.CustomCellEmptyContent
import jp.kamusoft.kssettingsview.ui.DSLIconModifiableCell
import jp.kamusoft.kssettingsview.ui.KsImage
import jp.kamusoft.kssettingsview.ui.LabelCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `DSLSectionScope.CustomCell(...)` 拡張関数（content あり / なしの 2 形）が
 * `DSLCellNode` に格納され、`CellHandle` chain が機能することを検証する。
 *
 * 併せて、等価な content の再評価が内容更新を発生させないこと（no-rebind）を
 * [DSLDiffCalculator] 経由で確認する。
 */
class CustomCellDslTest {

    @Test
    fun `content ありの拡張関数で CustomCell が DSLCellNode に格納される`() {
        val scope = DSLSettingsRootScope()
        scope.Section(header = "カスタム") {
            CustomCell(content = "スライダー値: 30") { Box(Modifier) }
        }
        val tree = scope.build()
        assertEquals(1, tree.size)
        val cell = tree[0].cellNodes[0].cell
        assertTrue(cell is CustomCell<*>)
        assertEquals("スライダー値: 30", (cell as CustomCell<*>).content)
    }

    @Test
    fun `content なしの拡張関数で静的 CustomCell が格納される`() {
        val scope = DSLSettingsRootScope()
        scope.Section {
            CustomCell { Box(Modifier) }
        }
        val cell = scope.build()[0].cellNodes[0].cell
        assertTrue(cell is CustomCell<*>)
        assertSame(CustomCellEmptyContent, (cell as CustomCell<*>).content)
    }

    @Test
    fun `拡張関数の引数が CustomCell に渡る`() {
        val onTap = { }
        val scope = DSLSettingsRootScope()
        scope.Section {
            CustomCell(
                content = 42,
                showArrow = true,
                style = CellStyle(cellHeight = 90.dp),
                onTap = onTap,
                isEnabled = false,
                isVisible = false,
            ) { Box(Modifier) }
        }
        val cell = scope.build()[0].cellNodes[0].cell as CustomCell<*>
        assertEquals(42, cell.content)
        assertTrue(cell.showArrow)
        assertEquals(CellStyle(cellHeight = 90.dp), cell.style)
        assertSame(onTap, cell.onTap)
        assertFalse(cell.isEnabled)
        assertFalse(cell.isVisible)
    }

    @Test
    fun `CellHandle chain の cellHeight が CustomCell の style に反映される`() {
        val scope = DSLSettingsRootScope()
        scope.Section {
            CustomCell(content = "A") { Box(Modifier) }.cellHeight(140.dp)
        }
        val cell = scope.build()[0].cellNodes[0].cell as CustomCell<*>
        assertEquals(140.dp, cell.style.cellHeight)
    }

    @Test
    fun `CellHandle chain の cellID で ID が位置移動を跨いで安定する`() {
        fun evaluate(content: DSLSectionScope.() -> Unit): List<Section> {
            val scope = DSLSettingsRootScope()
            scope.Section(header = "S1", block = content)
            return DSLRootTree(sectionNodes = scope.build()).resolvedSections()
        }
        val first = evaluate {
            CustomCell(content = "A") { Box(Modifier) }.cellID("custom-row")
        }
        val second = evaluate {
            LabelCell(title = "先頭に挿入")
            CustomCell(content = "A") { Box(Modifier) }.cellID("custom-row")
        }
        assertEquals(first[0].cells[0].id, second[0].cells[1].id)
    }

    @Test
    fun `id 省略時は安定位置から採番される`() {
        fun buildTree(): List<Section> {
            val scope = DSLSettingsRootScope()
            scope.Section(header = "S") {
                LabelCell(title = "先頭")
                CustomCell(content = "A") { Box(Modifier) }
            }
            return DSLRootTree(sectionNodes = scope.build()).resolvedSections()
        }
        assertEquals(buildTree()[0].cells[1].id, buildTree()[0].cells[1].id)
    }

    @Test
    fun `icon modifier は CustomCell に効かない`() {
        val scope = DSLSettingsRootScope()
        scope.Section {
            CustomCell(content = "A") { Box(Modifier) }.icon(KsImage.Resource(0))
        }
        val cell = scope.build()[0].cellNodes[0].cell
        assertFalse("icon modifier の対象インターフェースに準拠しない", cell is DSLIconModifiableCell)
        assertTrue(cell is CustomCell<*>)
    }

    // MARK: - 再評価時の差分

    private fun treeOf(vararg cells: jp.kamusoft.kssettingsview.core.Cell) =
        DSLDiffCalculator.ResolvedTree(
            listOf(Section(id = "s1", cells = cells.toList())),
            null,
            null,
        )

    @Test
    fun `同値 content の再評価では内容更新が発生しない`() {
        val old = treeOf(CustomCell(id = "c1", content = "A") { Box(Modifier) })
        // builder / onTap は DSL 再評価のたびに新しいクロージャになる
        val new = treeOf(CustomCell(id = "c1", content = "A", onTap = { }) { Box(Modifier.then(Modifier)) })
        assertEquals(emptyList<Any>(), DSLDiffCalculator.compute(old, new))
        assertEquals(emptyList<Any>(), DSLDiffCalculator.contentUpdates(old, new))
    }

    @Test
    fun `content の変更で内容更新が発生する`() {
        val old = treeOf(CustomCell(id = "c1", content = "A") { Box(Modifier) })
        val new = treeOf(CustomCell(id = "c1", content = "B") { Box(Modifier) })
        val updates = DSLDiffCalculator.contentUpdates(old, new)
        assertEquals(1, updates.size)
        assertEquals("c1", updates[0].id)
        assertEquals("B", (updates[0] as CustomCell<*>).content)
    }

    @Test
    fun `showArrow の変更で内容更新が発生する`() {
        val old = treeOf(CustomCell(id = "c1", content = "A", showArrow = false) { Box(Modifier) })
        val new = treeOf(CustomCell(id = "c1", content = "A", showArrow = true) { Box(Modifier) })
        assertEquals(1, DSLDiffCalculator.contentUpdates(old, new).size)
    }

    @Test
    fun `静的 CustomCell の再評価でも内容更新が発生しない`() {
        val old = treeOf(CustomCell(id = "c1") { Box(Modifier) })
        val new = treeOf(CustomCell(id = "c1") { Box(Modifier.then(Modifier)) })
        assertEquals(emptyList<Any>(), DSLDiffCalculator.contentUpdates(old, new))
    }
}
