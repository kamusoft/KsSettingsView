package jp.kamusoft.kssettingsview.compose

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
import jp.kamusoft.kssettingsview.ui.LabelCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compose 宣言 DSL の Header / Footer 表示トグルのテスト。
 *
 * DSL 引数が resolved `Section` へ転写されること、トグルだけの変化が preflight で
 * `Full` 更新へ振り分けられること（core/ADR-0023）を検証する。トグルは accessory の値を
 * 変えないため、preflight が無ければ差分は無音で 0 件になる。
 */
class DSLAccessoryVisibilityTest {

    /** DSL ビルダーを評価し、resolved な `List<Section>` を返すヘルパ。 */
    private fun evaluate(content: DSLSettingsRootScope.() -> Unit): List<Section> {
        val scope = DSLSettingsRootScope().apply(content)
        return DSLRootTree(sectionNodes = scope.build()).resolvedSections()
    }

    private fun tree(sections: List<Section>) = DSLDiffCalculator.ResolvedTree(
        sections = sections,
        rootHeader = null as RootAccessory?,
        rootFooter = null,
    )

    // MARK: - DSL 引数の転写

    @Test
    fun `DSL のトグル引数が resolved Section へ転写される`() {
        val sections = evaluate {
            Section(
                header = "一般",
                footer = "補足",
                isHeaderVisible = false,
                isFooterVisible = false,
            ) {
                cell(LabelCell(id = "c1", title = "A"))
            }
        }
        assertEquals(1, sections.size)
        assertFalse(sections[0].isHeaderVisible)
        assertFalse(sections[0].isFooterVisible)
    }

    @Test
    fun `トグル指定なしの DSL 構築では既定値 true になる`() {
        val sections = evaluate {
            Section(header = "一般", footer = "補足") {
                cell(LabelCell(id = "c1", title = "A"))
            }
        }
        assertTrue(sections[0].isHeaderVisible)
        assertTrue(sections[0].isFooterVisible)
    }

    @Test
    fun `Section modifier の accessory 上書きをまたいでトグルが保持される`() {
        val sections = evaluate {
            Section(isHeaderVisible = false, isFooterVisible = false) {
                cell(LabelCell(id = "c1", title = "A"))
            }.sectionHeader("後付けヘッダ").sectionFooter("後付けフッタ")
        }
        assertEquals(SectionAccessory.Text("後付けヘッダ"), sections[0].header)
        assertEquals(SectionAccessory.Text("後付けフッタ"), sections[0].footer)
        assertFalse("accessory 上書きでトグルが暗黙に true へ戻らない", sections[0].isHeaderVisible)
        assertFalse(sections[0].isFooterVisible)
    }

    // MARK: - preflight 検出

    /** header / footer / Cell を据え置き、トグルだけを変えた旧/新ツリーの組を返す。 */
    private fun toggleTrees(
        oldHeaderVisible: Boolean = true,
        oldFooterVisible: Boolean = true,
        newHeaderVisible: Boolean = true,
        newFooterVisible: Boolean = true,
        oldTitle: String = "A",
        newTitle: String = "A",
    ): Pair<DSLDiffCalculator.ResolvedTree, DSLDiffCalculator.ResolvedTree> {
        val old = evaluate {
            Section(
                header = "一般",
                footer = "補足",
                isHeaderVisible = oldHeaderVisible,
                isFooterVisible = oldFooterVisible,
            ) {
                cell(LabelCell(id = "c1", title = oldTitle))
            }
        }
        val new = evaluate {
            Section(
                header = "一般",
                footer = "補足",
                isHeaderVisible = newHeaderVisible,
                isFooterVisible = newFooterVisible,
            ) {
                cell(LabelCell(id = "c1", title = newTitle))
            }
        }
        return tree(old) to tree(new)
    }

    @Test
    fun `Header トグルのみの変化で Full が発行される`() {
        val (old, new) = toggleTrees(newHeaderVisible = false)
        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(1, diffs.size)
        val full = diffs[0] as SettingsRootDiff.Full
        assertFalse(full.root.sections[0].isHeaderVisible)
        assertTrue(full.root.sections[0].isFooterVisible)
    }

    @Test
    fun `Footer トグルのみの変化で Full が発行される`() {
        val (old, new) = toggleTrees(newFooterVisible = false)
        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(1, diffs.size)
        val full = diffs[0] as SettingsRootDiff.Full
        assertFalse(full.root.sections[0].isFooterVisible)
        assertTrue(full.root.sections[0].isHeaderVisible)
    }

    @Test
    fun `トグル変化と Cell 内容変化の併発でも Full のみ発行され contentUpdates は空`() {
        val (old, new) = toggleTrees(
            newHeaderVisible = false,
            oldTitle = "旧",
            newTitle = "新",
        )
        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(1, diffs.size)
        assertTrue(diffs[0] is SettingsRootDiff.Full)
        assertEquals(emptyList<Cell>(), DSLDiffCalculator.contentUpdates(old, new))
    }

    @Test
    fun `トグル不変なら Full を発行せず内容更新は contentUpdates で列挙される`() {
        val (old, new) = toggleTrees(
            oldHeaderVisible = false,
            newHeaderVisible = false,
            oldTitle = "旧",
            newTitle = "新",
        )
        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(emptyList<SettingsRootDiff>(), diffs)
        val updates = DSLDiffCalculator.contentUpdates(old, new)
        assertEquals(1, updates.size)
        assertEquals("新", (updates[0] as LabelCell).title)
    }

    @Test
    fun `containsAccessoryVisibilityChange はトグル変化のみを検出する`() {
        val base = Section(
            id = "s1",
            header = SectionAccessory.Text("一般"),
            footer = SectionAccessory.Text("補足"),
        )
        assertTrue(
            DSLDiffCalculator.containsAccessoryVisibilityChange(
                tree(listOf(base)),
                tree(listOf(base.copy(isHeaderVisible = false))),
            ),
        )
        assertTrue(
            DSLDiffCalculator.containsAccessoryVisibilityChange(
                tree(listOf(base)),
                tree(listOf(base.copy(isFooterVisible = false))),
            ),
        )
        assertFalse(
            DSLDiffCalculator.containsAccessoryVisibilityChange(
                tree(listOf(base)),
                tree(listOf(base)),
            ),
        )
        assertFalse(
            "別 ID の Section は比較対象にならない",
            DSLDiffCalculator.containsAccessoryVisibilityChange(
                tree(listOf(base)),
                tree(listOf(base.copy(id = "s2", isHeaderVisible = false))),
            ),
        )
    }
}
