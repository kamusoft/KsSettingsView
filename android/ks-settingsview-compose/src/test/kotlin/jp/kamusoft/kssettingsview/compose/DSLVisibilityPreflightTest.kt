package jp.kamusoft.kssettingsview.compose

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
import jp.kamusoft.kssettingsview.ui.LabelCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compose DSL の可視性 preflight 検出テスト。
 *
 * Section / Cell の `isVisible` 変化を検出した場合は、構造 Diff や内容更新に混ぜず、
 * visible projection を作り直す Full 更新へ切り替えることを検証する。
 */
class DSLVisibilityPreflightTest {

    private fun tree(
        sections: List<Section> = emptyList(),
        rootHeader: RootAccessory? = null,
        rootFooter: RootAccessory? = null,
    ) = DSLDiffCalculator.ResolvedTree(sections, rootHeader, rootFooter)

    @Test
    fun `Cell isVisible 変化のみで Full 発行`() {
        val sectionId = "s1"
        val cellId = "c1"
        val old = tree(listOf(Section(id = sectionId, cells = listOf(
            LabelCell(id = cellId, title = "A", isVisible = true)
        ))))
        val new = tree(listOf(Section(id = sectionId, cells = listOf(
            LabelCell(id = cellId, title = "A", isVisible = false)
        ))))

        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(1, diffs.size)
        assertTrue(diffs[0] is SettingsRootDiff.Full)
    }

    @Test
    fun `Cell isVisible 変化 + 内容変化で Full 発行 contents 内包`() {
        val sectionId = "s1"
        val cellId = "c1"
        val old = tree(listOf(Section(id = sectionId, cells = listOf(
            LabelCell(id = cellId, title = "旧", isVisible = true)
        ))))
        val new = tree(listOf(Section(id = sectionId, cells = listOf(
            LabelCell(id = cellId, title = "新", isVisible = false)
        ))))

        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(1, diffs.size)
        assertTrue(diffs[0] is SettingsRootDiff.Full)
    }

    @Test
    fun `Section isVisible 変化のみで Full 発行`() {
        val sectionId = "s1"
        val old = tree(listOf(Section(
            id = sectionId,
            header = SectionAccessory.Text("一般"),
            cells = listOf(LabelCell(id = "c1", title = "A")),
            isVisible = true,
        )))
        val new = tree(listOf(Section(
            id = sectionId,
            header = SectionAccessory.Text("一般"),
            cells = listOf(LabelCell(id = "c1", title = "A")),
            isVisible = false,
        )))

        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(1, diffs.size)
        assertTrue(diffs[0] is SettingsRootDiff.Full)
    }

    @Test
    fun `可視性変化時 contentUpdates は空リスト`() {
        val sectionId = "s1"
        val cellId = "c1"
        val old = tree(listOf(Section(id = sectionId, cells = listOf(
            LabelCell(id = cellId, title = "旧", isVisible = true)
        ))))
        val new = tree(listOf(Section(id = sectionId, cells = listOf(
            LabelCell(id = cellId, title = "新", isVisible = false)
        ))))
        // 可視性変化があるため contentUpdates は空
        assertEquals(emptyList<Cell>(), DSLDiffCalculator.contentUpdates(old, new))
    }

    @Test
    fun `可視性変化なしなら contentUpdates は内容変化を返す`() {
        val sectionId = "s1"
        val cellId = "c1"
        val old = tree(listOf(Section(id = sectionId, cells = listOf(
            LabelCell(id = cellId, title = "旧")
        ))))
        val new = tree(listOf(Section(id = sectionId, cells = listOf(
            LabelCell(id = cellId, title = "新")
        ))))
        val updates = DSLDiffCalculator.contentUpdates(old, new)
        assertEquals(1, updates.size)
        assertEquals("新", (updates.first() as LabelCell).title)
    }

    @Test
    fun `containsVisibilityChange 変化なしなら false`() {
        val sectionId = "s1"
        val old = tree(listOf(Section(id = sectionId, isVisible = true)))
        val new = tree(listOf(Section(id = sectionId, isVisible = true)))
        assertEquals(false, DSLDiffCalculator.containsVisibilityChange(old, new))
    }

    @Test
    fun `containsVisibilityChange Section 変化を検出`() {
        val sectionId = "s1"
        val old = tree(listOf(Section(id = sectionId, isVisible = true)))
        val new = tree(listOf(Section(id = sectionId, isVisible = false)))
        assertEquals(true, DSLDiffCalculator.containsVisibilityChange(old, new))
    }
}
