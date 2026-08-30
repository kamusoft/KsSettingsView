package jp.kamusoft.kssettingsview.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 7 Cell の `isVisible` 既定値・等価性・[VisibilityAware] 準拠・`copy()` 経路保持テスト。
 */
class CellVisibilityTest {

    // ---- 既定値 ----

    @Test
    fun labelCell_isVisible_default_true() {
        assertTrue(LabelCell(title = "a").isVisible)
    }

    @Test
    fun commandCell_isVisible_default_true() {
        assertTrue(CommandCell(title = "a").isVisible)
    }

    @Test
    fun buttonCell_isVisible_default_true() {
        assertTrue(ButtonCell(title = "a").isVisible)
    }

    @Test
    fun switchCell_isVisible_default_true() {
        assertTrue(SwitchCell(title = "a").isVisible)
    }

    @Test
    fun checkboxCell_isVisible_default_true() {
        assertTrue(CheckboxCell(title = "a").isVisible)
    }

    @Test
    fun radioCell_isVisible_default_true() {
        assertTrue(RadioCell(title = "a", groupId = "g", value = "v", selectedValue = "v").isVisible)
    }

    @Test
    fun simpleCheckCell_isVisible_default_true() {
        assertTrue(SimpleCheckCell(title = "a").isVisible)
    }

    // ---- VisibilityAware 準拠 ----

    @Test
    fun all_7_cells_conform_to_VisibilityAware() {
        val cells: List<Any> = listOf(
            LabelCell(title = "a"),
            CommandCell(title = "a"),
            ButtonCell(title = "a"),
            SwitchCell(title = "a"),
            CheckboxCell(title = "a"),
            RadioCell(title = "a", groupId = "g", value = "v", selectedValue = "v"),
            SimpleCheckCell(title = "a"),
        )
        for (cell in cells) {
            assertTrue(
                "${cell.javaClass.simpleName} must conform to VisibilityAware",
                cell is VisibilityAware,
            )
        }
    }

    // ---- equals / hashCode に isVisible を含める ----

    @Test
    fun labelCell_equality_includes_isVisible() {
        val a = LabelCell(id = "x", title = "t", isVisible = true)
        val b = LabelCell(id = "x", title = "t", isVisible = false)
        assertNotEquals(a, b)
    }

    @Test
    fun commandCell_equality_includes_isVisible_excludes_onTap() {
        val a = CommandCell(id = "x", title = "t", isVisible = true)
        val b = CommandCell(id = "x", title = "t", isVisible = false)
        assertNotEquals(a, b)
    }

    @Test
    fun switchCell_equality_includes_isVisible() {
        val a = SwitchCell(id = "x", title = "t", isOn = true, isVisible = true)
        val b = SwitchCell(id = "x", title = "t", isOn = true, isVisible = false)
        assertNotEquals(a, b)
    }

    @Test
    fun buttonCell_equality_includes_isVisible() {
        val a = ButtonCell(id = "x", title = "t", isVisible = true)
        val b = ButtonCell(id = "x", title = "t", isVisible = false)
        assertNotEquals(a, b)
    }

    @Test
    fun checkboxCell_equality_includes_isVisible() {
        val a = CheckboxCell(id = "x", title = "t", isVisible = true)
        val b = CheckboxCell(id = "x", title = "t", isVisible = false)
        assertNotEquals(a, b)
    }

    @Test
    fun radioCell_equality_includes_isVisible() {
        val a = RadioCell(id = "x", title = "t", groupId = "g", value = "v", selectedValue = "v", isVisible = true)
        val b = RadioCell(id = "x", title = "t", groupId = "g", value = "v", selectedValue = "v", isVisible = false)
        assertNotEquals(a, b)
    }

    @Test
    fun simpleCheckCell_equality_includes_isVisible() {
        val a = SimpleCheckCell(id = "x", title = "t", isVisible = true)
        val b = SimpleCheckCell(id = "x", title = "t", isVisible = false)
        assertNotEquals(a, b)
    }

    // ---- copy() 経路で isVisible 保持 ----

    @Test
    fun labelCell_copy_preserves_isVisible() {
        val original = LabelCell(title = "t", isVisible = false)
        assertFalse(original.copy(title = "new").isVisible)
    }

    @Test
    fun switchCell_copy_preserves_isVisible() {
        val original = SwitchCell(title = "t", isVisible = false)
        assertFalse(original.copy(isOn = true).isVisible)
    }

    // ---- withDSLId / withDSLStyle / withDSLIcon で isVisible 保持 ----

    @Test
    fun labelCell_withDSLId_preserves_isVisible() {
        val original = LabelCell(title = "t", isVisible = false)
        val rebound = original.withDSLId("new-id") as LabelCell
        assertFalse(rebound.isVisible)
        assertEquals("new-id", rebound.id)
    }

    @Test
    fun commandCell_withDSLStyle_preserves_isVisible() {
        val original = CommandCell(title = "t", isVisible = false)
        val restyled = original.withDSLStyle(CellStyle()) as CommandCell
        assertFalse(restyled.isVisible)
    }
}
