package jp.kamusoft.kssettingsview.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * SettingsRoot ドメインモデルの仕様検証。
 *
 * `sections` を保持すること、等価性が `sections` のみで決まること、
 * 空 sections でも構築できることを確認する。本型は Theme を持たない
 * （Theme は UI 層の `SettingsRootStore.applyTheme(_)` / `KsSettingsView(theme = ...)` 経路で渡す）。
 */
class SettingsRootTest {

    @Test
    @DisplayName("構築: sections を保持する")
    fun build_holds_sections() {
        val sections = listOf(Section(id = "general", header = SectionAccessory.Text("General")))

        val root = SettingsRoot(sections = sections)

        assertEquals(1, root.sections.size)
        assertEquals("general", root.sections[0].id)
    }

    @Test
    @DisplayName("等価性: 同一 sections は等しい")
    fun equality_same_sections_are_equal() {
        val cells = listOf<Cell>(DummyLabelCell(id = "c1", title = "a"))
        val s1 = Section(
            id = "sec",
            header = SectionAccessory.Text("h"),
            footer = SectionAccessory.Text("f"),
            cells = cells,
        )
        val s2 = Section(
            id = "sec",
            header = SectionAccessory.Text("h"),
            footer = SectionAccessory.Text("f"),
            cells = cells,
        )

        val r1 = SettingsRoot(sections = listOf(s1))
        val r2 = SettingsRoot(sections = listOf(s2))

        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    @DisplayName("等価性: 異なる sections は等しくない")
    fun equality_different_sections_are_not_equal() {
        val r1 = SettingsRoot(sections = emptyList())
        val r2 = SettingsRoot(sections = listOf(Section(id = "x")))
        assertNotEquals(r1, r2)
    }

    @Test
    @DisplayName("空 sections でも構築できる")
    fun empty_sections_can_be_built() {
        val root = SettingsRoot()

        assertTrue(root.sections.isEmpty())
    }
}
