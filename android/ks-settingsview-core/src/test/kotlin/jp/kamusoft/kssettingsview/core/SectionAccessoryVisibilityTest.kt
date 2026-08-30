package jp.kamusoft.kssettingsview.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * `Section.isHeaderVisible` / `Section.isFooterVisible` の既定値・等価性・保持のテスト。
 *
 * 既定値が `true` であること、明示指定できること、両トグルが独立して値等価性に参加すること、
 * `copy` による再構築でトグルが失われないことを確認する（core/ADR-0023）。
 */
class SectionAccessoryVisibilityTest {

    @Test
    @DisplayName("表示トグルは既定でいずれも true")
    fun toggles_default_true() {
        val section = Section(id = "s1")
        assertTrue(section.isHeaderVisible)
        assertTrue(section.isFooterVisible)
    }

    @Test
    @DisplayName("表示トグルは明示指定した値を保持する")
    fun toggles_explicit_values() {
        val section = Section(id = "s1", isHeaderVisible = false, isFooterVisible = false)
        assertFalse(section.isHeaderVisible)
        assertFalse(section.isFooterVisible)
    }

    @Test
    @DisplayName("等価性: isHeaderVisible のみ異なる Section は等価ではない")
    fun equality_isHeaderVisible_only_differs() {
        val a = Section(id = "s1", header = SectionAccessory.Text("h"), isHeaderVisible = true)
        val b = Section(id = "s1", header = SectionAccessory.Text("h"), isHeaderVisible = false)
        assertNotEquals(a, b)
    }

    @Test
    @DisplayName("等価性: isFooterVisible のみ異なる Section は等価ではない")
    fun equality_isFooterVisible_only_differs() {
        val a = Section(id = "s1", footer = SectionAccessory.Text("f"), isFooterVisible = true)
        val b = Section(id = "s1", footer = SectionAccessory.Text("f"), isFooterVisible = false)
        assertNotEquals(a, b)
    }

    @Test
    @DisplayName("等価性: Header トグルと Footer トグルは独立して参加する")
    fun equality_toggles_participate_independently() {
        val base = Section(
            id = "s1",
            header = SectionAccessory.Text("h"),
            footer = SectionAccessory.Text("f"),
        )
        // Header だけ隠した Section は Footer だけ隠した Section と等価にならない
        assertNotEquals(base.copy(isHeaderVisible = false), base.copy(isFooterVisible = false))
        // 同じ組み合わせなら等価で hash も一致する
        val a = base.copy(isHeaderVisible = false, isFooterVisible = true)
        val b = base.copy(isHeaderVisible = false, isFooterVisible = true)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    @DisplayName("copy による再構築でトグルが暗黙に true へ戻らない")
    fun copy_preserves_toggles() {
        val section = Section(
            id = "s1",
            header = SectionAccessory.Text("h"),
            footer = SectionAccessory.Text("f"),
            isHeaderVisible = false,
            isFooterVisible = false,
        )
        val updated = section.copy(cells = listOf(DummyCell(id = "c1")))
        assertFalse(updated.isHeaderVisible)
        assertFalse(updated.isFooterVisible)
    }

    /** 保持テスト用の最小 Cell。 */
    private data class DummyCell(override val id: String) : Cell
}
