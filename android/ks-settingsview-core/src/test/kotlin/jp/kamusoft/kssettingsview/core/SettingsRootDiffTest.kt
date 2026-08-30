package jp.kamusoft.kssettingsview.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * SettingsRootDiff の仕様検証。
 *
 * 全ケースの生成と payload 取り出し、および `data class` の等価性契約を確認する。
 * Theme 更新のケースは本型に含まれない（UI 層の `SettingsRootStore.applyTheme(_)` が担う）。
 */
class SettingsRootDiffTest {

    // MARK: - 生成・payload 取り出しテスト

    @Test
    @DisplayName("Full: 生成と payload 取り出し")
    fun full_construct_and_extract() {
        val root = SettingsRoot()
        val diff: SettingsRootDiff = SettingsRootDiff.Full(root)
        assertTrue(diff is SettingsRootDiff.Full)
        assertEquals(root, (diff as SettingsRootDiff.Full).root)
    }

    @Test
    @DisplayName("InsertSection: 生成と payload 取り出し")
    fun insertSection_construct_and_extract() {
        val section = Section(id = "s1", header = SectionAccessory.Text("S"))
        val diff: SettingsRootDiff = SettingsRootDiff.InsertSection(index = 2, section = section)
        assertTrue(diff is SettingsRootDiff.InsertSection)
        val actual = diff as SettingsRootDiff.InsertSection
        assertEquals(2, actual.index)
        assertEquals(section, actual.section)
    }

    @Test
    @DisplayName("RemoveSection: 生成と payload 取り出し")
    fun removeSection_construct_and_extract() {
        val diff: SettingsRootDiff = SettingsRootDiff.RemoveSection(sectionId = "s1")
        assertTrue(diff is SettingsRootDiff.RemoveSection)
        assertEquals("s1", (diff as SettingsRootDiff.RemoveSection).sectionId)
    }

    @Test
    @DisplayName("MoveSection: 生成と payload 取り出し")
    fun moveSection_construct_and_extract() {
        val diff: SettingsRootDiff = SettingsRootDiff.MoveSection(from = 1, to = 4)
        assertTrue(diff is SettingsRootDiff.MoveSection)
        val actual = diff as SettingsRootDiff.MoveSection
        assertEquals(1, actual.from)
        assertEquals(4, actual.to)
    }

    @Test
    @DisplayName("ReplaceSection: 生成と payload 取り出し")
    fun replaceSection_construct_and_extract() {
        val newSection = Section(id = "s1", header = SectionAccessory.Text("new"))
        val diff: SettingsRootDiff = SettingsRootDiff.ReplaceSection(sectionId = "s1", newSection = newSection)
        assertTrue(diff is SettingsRootDiff.ReplaceSection)
        val actual = diff as SettingsRootDiff.ReplaceSection
        assertEquals("s1", actual.sectionId)
        assertEquals(newSection, actual.newSection)
    }

    @Test
    @DisplayName("InsertCell: 生成と payload 取り出し")
    fun insertCell_construct_and_extract() {
        val cell: Cell = DummyLabelCell(id = "c1", title = "A")
        val diff: SettingsRootDiff = SettingsRootDiff.InsertCell(sectionId = "s1", index = 3, cell = cell)
        assertTrue(diff is SettingsRootDiff.InsertCell)
        val actual = diff as SettingsRootDiff.InsertCell
        assertEquals("s1", actual.sectionId)
        assertEquals(3, actual.index)
        assertEquals(cell, actual.cell)
    }

    @Test
    @DisplayName("RemoveCell: 生成と payload 取り出し")
    fun removeCell_construct_and_extract() {
        val diff: SettingsRootDiff = SettingsRootDiff.RemoveCell(cellId = "c1")
        assertTrue(diff is SettingsRootDiff.RemoveCell)
        assertEquals("c1", (diff as SettingsRootDiff.RemoveCell).cellId)
    }

    @Test
    @DisplayName("ReplaceCell: 生成と payload 取り出し")
    fun replaceCell_construct_and_extract() {
        val newCell: Cell = DummyLabelCell(id = "c1", title = "new")
        val diff: SettingsRootDiff = SettingsRootDiff.ReplaceCell(cellId = "c1", newCell = newCell)
        assertTrue(diff is SettingsRootDiff.ReplaceCell)
        val actual = diff as SettingsRootDiff.ReplaceCell
        assertEquals("c1", actual.cellId)
        assertEquals(newCell, actual.newCell)
    }

    @Test
    @DisplayName("MoveCell: 生成と payload 取り出し")
    fun moveCell_construct_and_extract() {
        val diff: SettingsRootDiff = SettingsRootDiff.MoveCell(cellId = "c1", toIndex = 5)
        assertTrue(diff is SettingsRootDiff.MoveCell)
        val actual = diff as SettingsRootDiff.MoveCell
        assertEquals("c1", actual.cellId)
        assertEquals(5, actual.toIndex)
    }

    @Test
    @DisplayName("UpdateAccessory: Root Header の text 更新")
    fun updateAccessory_root_header_text() {
        val target: AccessoryTarget = AccessoryTarget.RootHeader
        val accessory: SettingsAccessory = SettingsAccessory.Root(RootAccessory.Text("プロフィール"))
        val diff: SettingsRootDiff = SettingsRootDiff.UpdateAccessory(target = target, accessory = accessory)
        assertTrue(diff is SettingsRootDiff.UpdateAccessory)
        val actual = diff as SettingsRootDiff.UpdateAccessory
        assertEquals(target, actual.target)
        assertEquals(accessory, actual.accessory)
    }

    @Test
    @DisplayName("UpdateAccessory: Section Header の削除（accessory = null）")
    fun updateAccessory_section_header_null() {
        val target: AccessoryTarget = AccessoryTarget.SectionHeader(sectionId = "s1")
        val diff: SettingsRootDiff = SettingsRootDiff.UpdateAccessory(target = target, accessory = null)
        assertTrue(diff is SettingsRootDiff.UpdateAccessory)
        val actual = diff as SettingsRootDiff.UpdateAccessory
        assertEquals(target, actual.target)
        assertNull(actual.accessory)
    }

    // MARK: - 等価性テスト

    @Test
    @DisplayName("等価性: 異なるケースは不等")
    fun equality_different_case_not_equal() {
        val a: SettingsRootDiff = SettingsRootDiff.RemoveSection(sectionId = "s1")
        val b: SettingsRootDiff = SettingsRootDiff.MoveSection(from = 0, to = 1)
        assertNotEquals(a, b)
    }

    @Test
    @DisplayName("等価性: 同一ケースでも異なる payload は不等")
    fun equality_same_case_different_payload_not_equal() {
        val a: SettingsRootDiff = SettingsRootDiff.MoveSection(from = 0, to = 1)
        val b: SettingsRootDiff = SettingsRootDiff.MoveSection(from = 0, to = 2)
        assertNotEquals(a, b)
    }

    @Test
    @DisplayName("等価性: InsertCell 同一 Cell は等価")
    fun equality_insertCell_same_cell_equal() {
        val cell: Cell = DummyLabelCell(id = "c1", title = "A")
        val a: SettingsRootDiff = SettingsRootDiff.InsertCell(sectionId = "s1", index = 0, cell = cell)
        val b: SettingsRootDiff = SettingsRootDiff.InsertCell(sectionId = "s1", index = 0, cell = cell)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    @DisplayName("等価性: InsertCell 異なる Cell フィールドは不等")
    fun equality_insertCell_different_cell_not_equal() {
        val cellA: Cell = DummyLabelCell(id = "c1", title = "A")
        val cellB: Cell = DummyLabelCell(id = "c1", title = "B")
        val a: SettingsRootDiff = SettingsRootDiff.InsertCell(sectionId = "s1", index = 0, cell = cellA)
        val b: SettingsRootDiff = SettingsRootDiff.InsertCell(sectionId = "s1", index = 0, cell = cellB)
        assertNotEquals(a, b)
    }

    @Test
    @DisplayName("等価性: ReplaceCell 同一は等価")
    fun equality_replaceCell_same_equal() {
        val newCell: Cell = DummyLabelCell(id = "c1", title = "NEW")
        val a: SettingsRootDiff = SettingsRootDiff.ReplaceCell(cellId = "c1", newCell = newCell)
        val b: SettingsRootDiff = SettingsRootDiff.ReplaceCell(cellId = "c1", newCell = newCell)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    @DisplayName("等価性: UpdateAccessory 同一は等価")
    fun equality_updateAccessory_same_equal() {
        val target: AccessoryTarget = AccessoryTarget.RootHeader
        val accessory: SettingsAccessory = SettingsAccessory.Root(RootAccessory.Text("X"))
        val a: SettingsRootDiff = SettingsRootDiff.UpdateAccessory(target = target, accessory = accessory)
        val b: SettingsRootDiff = SettingsRootDiff.UpdateAccessory(target = target, accessory = accessory)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    @DisplayName("等価性: UpdateAccessory null と非 null は不等")
    fun equality_updateAccessory_null_vs_nonnull_not_equal() {
        val target: AccessoryTarget = AccessoryTarget.RootHeader
        val a: SettingsRootDiff = SettingsRootDiff.UpdateAccessory(
            target = target,
            accessory = SettingsAccessory.Root(RootAccessory.Text("X")),
        )
        val b: SettingsRootDiff = SettingsRootDiff.UpdateAccessory(target = target, accessory = null)
        assertNotEquals(a, b)
    }
}
