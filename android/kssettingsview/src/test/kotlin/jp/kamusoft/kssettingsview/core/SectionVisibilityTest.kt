package jp.kamusoft.kssettingsview.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * `Section.isVisible` の既定値・等価性・互換性テスト。
 *
 * 既定値が `true` であること、明示指定できること、`isVisible` が `data class` の
 * 等価性判定に含まれること、既存の構築経路と互換であることを確認する。
 */
class SectionVisibilityTest {

    @Test
    @DisplayName("isVisible は既定値 true")
    fun isVisible_default_true() {
        val section = Section(id = "s1")
        assertTrue(section.isVisible)
    }

    @Test
    @DisplayName("isVisible に false を指定できる")
    fun isVisible_false_explicit() {
        val section = Section(id = "s1", isVisible = false)
        assertFalse(section.isVisible)
    }

    @Test
    @DisplayName("等価性: isVisible のみ異なる Section は等価ではない")
    fun equality_isVisible_only_differs() {
        val a = Section(id = "s1", header = SectionAccessory.Text("h"), isVisible = true)
        val b = Section(id = "s1", header = SectionAccessory.Text("h"), isVisible = false)
        assertNotEquals(a, b)
    }

    @Test
    @DisplayName("等価性: 全フィールド一致なら等価")
    fun equality_all_fields_equal() {
        val a = Section(
            id = "s1",
            header = SectionAccessory.Text("h"),
            footer = SectionAccessory.Text("f"),
            cells = emptyList(),
            headerHeight = 10.0,
            isVisible = false,
        )
        val b = Section(
            id = "s1",
            header = SectionAccessory.Text("h"),
            footer = SectionAccessory.Text("f"),
            cells = emptyList(),
            headerHeight = 10.0,
            isVisible = false,
        )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    @DisplayName("既存呼び出し互換: isVisible 未指定でも既定 true で構築できる")
    fun backward_compat_default() {
        val section = Section(id = "s1", header = SectionAccessory.Text("一般"))
        assertTrue(section.isVisible)
    }
}
