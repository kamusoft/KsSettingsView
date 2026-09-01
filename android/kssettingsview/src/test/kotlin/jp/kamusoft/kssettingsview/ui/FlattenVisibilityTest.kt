package jp.kamusoft.kssettingsview.ui

import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `KsSettingsView.flatten` の可視性フィルタテスト。
 *
 * 非表示の Section / Cell が visible projection の平坦リストから除かれることを保証する。
 */
class FlattenVisibilityTest {

    @Test
    fun hidden_section_excluded_from_flatten() {
        val hidden = Section(
            id = "s1",
            header = SectionAccessory.Text("hidden"),
            footer = SectionAccessory.Text("hidden-footer"),
            cells = listOf(LabelCell(id = "c1", title = "A")),
            isVisible = false,
        )
        val visible = Section(
            id = "s2",
            header = SectionAccessory.Text("visible"),
            cells = listOf(LabelCell(id = "c2", title = "B")),
            isVisible = true,
        )
        val result = KsSettingsView.flatten(listOf(hidden, visible))

        // hidden Section 由来は何も含まれない
        assertTrue(result.none { (it as? CellListItem.SectionHeader)?.sectionId == "s1" })
        assertTrue(result.none { (it as? CellListItem.SectionFooter)?.sectionId == "s1" })
        assertTrue(result.none { (it as? CellListItem.CellRow)?.cell?.id == "c1" })

        // visible Section 由来は含まれる
        assertTrue(result.any { (it as? CellListItem.SectionHeader)?.sectionId == "s2" })
        assertTrue(result.any { (it as? CellListItem.CellRow)?.cell?.id == "c2" })
    }

    @Test
    fun hidden_cell_excluded_from_flatten() {
        val section = Section(
            id = "s1",
            header = SectionAccessory.Text("h"),
            cells = listOf(
                LabelCell(id = "v", title = "visible", isVisible = true),
                LabelCell(id = "h", title = "hidden", isVisible = false),
            ),
        )
        val result = KsSettingsView.flatten(listOf(section))
        val cellRows = result.filterIsInstance<CellListItem.CellRow>()
        assertEquals(1, cellRows.size)
        assertEquals("v", cellRows.first().cell.id)
    }

    @Test
    fun non_visibility_aware_cell_always_included_in_flatten() {
        // VisibilityAware に準拠しないダミー Cell を含む
        val nonVisibilityAware = NonVisibilityAwareDummyCell(id = "ext")
        val section = Section(
            id = "s1",
            cells = listOf(nonVisibilityAware),
        )
        val result = KsSettingsView.flatten(listOf(section))
        val cellRows = result.filterIsInstance<CellListItem.CellRow>()
        assertEquals(1, cellRows.size)
        assertEquals("ext", cellRows.first().cell.id)
    }

    /** VisibilityAware に準拠しないテスト用 Cell。 */
    private data class NonVisibilityAwareDummyCell(
        override val id: String,
    ) : jp.kamusoft.kssettingsview.core.Cell
}
