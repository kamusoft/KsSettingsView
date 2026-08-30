package jp.kamusoft.kssettingsview.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * CellTitleAlignment 列挙型の仕様検証。
 *
 * `START` / `CENTER` / `END` の 3 ケースが定義され、等価性・hashCode 契約を満たすことを確認する。
 */
class CellTitleAlignmentTest {

    @Test
    @DisplayName("3 ケースが定義されている")
    fun three_cases_defined() {
        // 全ケースが参照可能であること
        val s: CellTitleAlignment = CellTitleAlignment.START
        val c: CellTitleAlignment = CellTitleAlignment.CENTER
        val e: CellTitleAlignment = CellTitleAlignment.END
        assertNotEquals(s, c)
        assertNotEquals(s, e)
        assertNotEquals(c, e)
    }

    @Test
    @DisplayName("等価性 / hashCode 契約")
    fun equality_and_hashcode_contract() {
        assertEquals(CellTitleAlignment.CENTER, CellTitleAlignment.CENTER)
        assertEquals(CellTitleAlignment.START.hashCode(), CellTitleAlignment.START.hashCode())
        // Set 格納可能
        val set: Set<CellTitleAlignment> = setOf(
            CellTitleAlignment.START,
            CellTitleAlignment.CENTER,
            CellTitleAlignment.END,
            CellTitleAlignment.START,
        )
        assertEquals(3, set.size)
    }
}
