package jp.kamusoft.kssettingsview.core

import androidx.compose.runtime.Composable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Section ドメインモデルの仕様検証。
 *
 * 検証する性質:
 *   - 文字列ヘッダでの構築と取り出し
 *   - 任意 View ヘッダでの構築と取り出し
 *   - 空セクション（cells が空リスト）を許容する
 *   - 等価性が cells を含めて決まる
 */
class SectionTest {

    @Test
    @DisplayName("構築（文字列ヘッダ）: 全フィールドを保持し、header から元の文字列を取り出せる")
    fun build_with_text_header_holds_all_fields() {
        val cells = listOf<Cell>(DummyLabelCell(id = "c1", title = "a"))

        val section = Section(
            id = "sec",
            header = SectionAccessory.Text("一般"),
            footer = SectionAccessory.Text("footer"),
            cells = cells,
        )

        assertEquals("sec", section.id)
        assertEquals(SectionAccessory.Text("一般"), section.header)
        assertEquals(SectionAccessory.Text("footer"), section.footer)
        assertEquals(1, section.cells.size)

        // ケース別取り出し: header から元の文字列を取り出せる
        val header = section.header
        assertTrue(header is SectionAccessory.Text)
        assertEquals("一般", (header as SectionAccessory.Text).value)
    }

    @Test
    @DisplayName("構築（任意 View ヘッダ）: header から view ケースを取り出せる")
    fun build_with_view_header_returns_view_case() {
        val composable: @Composable () -> Unit = {}
        val anyView = KsAnyView.Compose(composable)

        val section = Section(
            id = "sec",
            header = SectionAccessory.View(anyView),
            footer = null,
            cells = emptyList(),
        )

        val header = section.header
        assertTrue(header is SectionAccessory.View)
    }

    @Test
    @DisplayName("空 cells で構築でき isEmpty が真")
    fun empty_cells_can_be_built() {
        val section = Section(id = "sec")

        assertTrue(section.cells.isEmpty())
        assertNull(section.header)
        assertNull(section.footer)
    }

    @Test
    @DisplayName("等価性: 同フィールドは等しい")
    fun equality_same_fields_are_equal() {
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
        assertEquals(s1, s2)
        assertEquals(s1.hashCode(), s2.hashCode())
    }

    @Test
    @DisplayName("等価性: id が異なれば等しくない")
    fun equality_different_id_are_not_equal() {
        val s1 = Section(id = "a")
        val s2 = Section(id = "b")
        assertNotEquals(s1, s2)
    }

    @Test
    @DisplayName("等価性（任意 View ヘッダ）: 中身を無視して等しい")
    fun equality_view_header_ignore_inner_view() {
        val composable1: @Composable () -> Unit = {}
        val composable2: @Composable () -> Unit = {}
        val s1 = Section(id = "sec", header = SectionAccessory.View(KsAnyView.Compose(composable1)))
        val s2 = Section(id = "sec", header = SectionAccessory.View(KsAnyView.Compose(composable2)))
        assertEquals(s1, s2)
        assertEquals(s1.hashCode(), s2.hashCode())
    }

    @Test
    @DisplayName("異種 Cell を List<Cell> に格納できる（Cell インターフェース）")
    fun heterogeneous_cells_can_be_stored() {
        val cells: List<Cell> = listOf(
            DummyLabelCell(id = "c1", title = "label"),
            DummySwitchCell(id = "c2", isOn = true),
        )
        val section = Section(id = "sec", cells = cells)
        assertEquals(2, section.cells.size)
    }

    // ----------------------------------------------------------------------
    // headerHeight（refine-basic-cells-sample-layout）
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("headerHeight 既定値は -1.0（自動）")
    fun headerHeight_default_is_minus_one() {
        val section = Section(id = "sec")
        assertEquals(-1.0, section.headerHeight)
    }

    @Test
    @DisplayName("headerHeight 明示指定で値を保持する")
    fun headerHeight_explicit_value_is_kept() {
        val section = Section(
            id = "sec",
            header = SectionAccessory.Text("一般"),
            footer = null,
            cells = emptyList(),
            headerHeight = 40.0,
        )
        assertEquals(40.0, section.headerHeight)
    }

    @Test
    @DisplayName("headerHeight は等価性判定に含まれる")
    fun headerHeight_participates_in_equality() {
        val s1 = Section(id = "sec", headerHeight = -1.0)
        val s2 = Section(id = "sec", headerHeight = 40.0)
        assertNotEquals(s1, s2)

        val s3 = Section(id = "sec", headerHeight = 40.0)
        assertEquals(s2, s3)
        assertEquals(s2.hashCode(), s3.hashCode())
    }
}
