package jp.kamusoft.kssettingsview.bridge

import android.text.InputType
import jp.kamusoft.kssettingsview.core.CellTitleAlignment
import jp.kamusoft.kssettingsview.ui.DatePickerCell
import jp.kamusoft.kssettingsview.ui.DatePickerUIStyle
import jp.kamusoft.kssettingsview.ui.PickerCell
import jp.kamusoft.kssettingsview.ui.PickerSelectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * interop 境界の値表現と Native 型の相互変換を検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsBridgeValueTransportTest {

    // MARK: - 時刻 / 日付

    /** 輸送書式の時刻文字列が LocalTime へ往復する。 */
    @Test
    fun `時刻文字列が LocalTime へ往復する`() {
        val parsed = KsBridgeValueTransport.time("09:05")

        assertEquals(LocalTime.of(9, 5), parsed)
        assertEquals("09:05", KsBridgeValueTransport.timeText(parsed))
    }

    /** 輸送書式の日付文字列が LocalDate へ往復する。 */
    @Test
    fun `日付文字列が LocalDate へ往復する`() {
        val parsed = KsBridgeValueTransport.date("2026-08-10")

        assertEquals(LocalDate.of(2026, 8, 10), parsed)
        assertEquals("2026-08-10", KsBridgeValueTransport.dateText(parsed))
    }

    /** 解釈できない時刻文字列は 00:00 になる。 */
    @Test
    fun `解釈できない時刻文字列は既定値になる`() {
        assertEquals(LocalTime.MIDNIGHT, KsBridgeValueTransport.time("とけい"))
    }

    /** 解釈できない日付文字列は 1970-01-01 になる。 */
    @Test
    fun `解釈できない日付文字列は既定値になる`() {
        assertEquals("1970-01-01", KsBridgeValueTransport.dateText(KsBridgeValueTransport.date("とてもひづけ")))
    }

    /** 区切り文字違い・桁数不足・暦上存在しない日は解釈されず既定値になる。 */
    @Test
    fun `輸送書式から外れた表記は解釈されず既定値になる`() {
        assertEquals("1970-01-01", KsBridgeValueTransport.dateText(KsBridgeValueTransport.date("2026/08/10")))
        assertEquals("1970-01-01", KsBridgeValueTransport.dateText(KsBridgeValueTransport.date("2026-8-10")))
        assertEquals("1970-01-01", KsBridgeValueTransport.dateText(KsBridgeValueTransport.date("2026-02-30")))
        assertEquals("00:00", KsBridgeValueTransport.timeText(KsBridgeValueTransport.time("9:05")))
        assertNull(KsBridgeValueTransport.optionalDate("2026/08/10"))
    }

    /** 未指定を許す日付は、null と解釈失敗のどちらも未指定になる。 */
    @Test
    fun `解釈できない任意日付文字列は未指定になる`() {
        assertNull(KsBridgeValueTransport.optionalDate("not-a-date"))
        assertNull(KsBridgeValueTransport.optionalDate(null))
        assertNotNull(KsBridgeValueTransport.optionalDate("2026-08-10"))
    }

    /** 解釈できない日付を持つ DTO でも、他フィールドは反映して構築される。 */
    @Test
    fun `解釈できない日付を持つ DTO も他フィールドを反映して構築される`() {
        val dto = KsBridgeDatePickerCell(title = "日付").apply {
            date = "8/10/2026"
            pickerTitle = "日付を選択"
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: DatePickerCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals(LocalDate.of(1970, 1, 1), cell?.date)
        assertEquals("日付", cell?.title)
        assertEquals("日付を選択", cell?.pickerTitle)
    }

    /** 解釈できない日付は、insertCell / replaceCell のどちらの経路でも同じ既定値になる。 */
    @Test
    fun `解釈できない日付を insertCell と replaceCell へ渡しても同じ既定値になる`() {
        val bridge = KsBridgeFixture.withCells(listOf(KsBridgeLabelCell(title = "ラベル")))
        val sectionID = bridge.store.state.value.sections[0].id

        val inserted = KsBridgeDatePickerCell(title = "挿入").apply { date = "invalid" }
        bridge.insertCell(inserted, sectionID, index = 1)
        val replacement = KsBridgeDatePickerCell(title = "差し替え").apply { date = "invalid" }
        bridge.replaceCell(bridge.store.state.value.sections[0].cells[0].id, replacement)

        val datePickers = KsBridgeFixture.storedCells(bridge).filterIsInstance<DatePickerCell>()
        assertEquals(2, datePickers.size)
        datePickers.forEach { assertEquals(LocalDate.of(1970, 1, 1), it.date) }
    }

    // MARK: - enum の序数

    /** keyboard 序数が Android の InputType 定数へ変換される。 */
    @Test
    fun `keyboard 序数が InputType へ変換される`() {
        assertEquals(InputType.TYPE_CLASS_TEXT, KsBridgeValueTransport.keyboardType(0))
        assertEquals(InputType.TYPE_CLASS_TEXT, KsBridgeValueTransport.keyboardType(1))
        assertEquals(InputType.TYPE_CLASS_TEXT, KsBridgeValueTransport.keyboardType(2))
        assertEquals(InputType.TYPE_CLASS_TEXT, KsBridgeValueTransport.keyboardType(3))
        assertEquals(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
            KsBridgeValueTransport.keyboardType(4),
        )
        assertEquals(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            KsBridgeValueTransport.keyboardType(5),
        )
        assertEquals(
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
            KsBridgeValueTransport.keyboardType(6),
        )
        assertEquals(InputType.TYPE_CLASS_PHONE, KsBridgeValueTransport.keyboardType(7))
    }

    /** 対応の取れない keyboard 序数は Native 既定へ倒れる。 */
    @Test
    fun `対応の取れない keyboard 序数は Native 既定へ倒れる`() {
        assertEquals(InputType.TYPE_CLASS_TEXT, KsBridgeValueTransport.keyboardType(99))
        assertEquals(InputType.TYPE_CLASS_TEXT, KsBridgeValueTransport.keyboardType(-1))
    }

    /** uiStyle 序数が DatePickerUIStyle へ変換され、未指定・未知は Native 既定に委ねられる。 */
    @Test
    fun `uiStyle 序数が DatePickerUIStyle へ変換される`() {
        assertEquals(DatePickerUIStyle.Material, KsBridgeValueTransport.datePickerUIStyle(0))
        assertEquals(DatePickerUIStyle.Spinner, KsBridgeValueTransport.datePickerUIStyle(1))
        assertNull("未指定は Native 既定を使う", KsBridgeValueTransport.datePickerUIStyle(null))
        assertNull(KsBridgeValueTransport.datePickerUIStyle(99))
    }

    /** 配置序数が CellTitleAlignment へ変換され、未指定・未知は fallback になる。 */
    @Test
    fun `配置序数が CellTitleAlignment へ変換される`() {
        assertEquals(
            CellTitleAlignment.START,
            KsBridgeValueTransport.titleAlignment(0, CellTitleAlignment.CENTER),
        )
        assertEquals(
            CellTitleAlignment.CENTER,
            KsBridgeValueTransport.titleAlignment(1, CellTitleAlignment.START),
        )
        assertEquals(
            CellTitleAlignment.END,
            KsBridgeValueTransport.titleAlignment(2, CellTitleAlignment.START),
        )
        assertEquals(
            CellTitleAlignment.CENTER,
            KsBridgeValueTransport.titleAlignment(null, CellTitleAlignment.CENTER),
        )
        assertEquals(
            CellTitleAlignment.END,
            KsBridgeValueTransport.titleAlignment(9, CellTitleAlignment.END),
        )
    }

    /** 選択モード序数が PickerSelectionMode へ変換され、未知は単一選択へ倒れる。 */
    @Test
    fun `選択モード序数が PickerSelectionMode へ変換される`() {
        assertEquals(PickerSelectionMode.Single, KsBridgeValueTransport.selectionMode(0))
        assertEquals(PickerSelectionMode.Multiple, KsBridgeValueTransport.selectionMode(1))
        assertEquals(PickerSelectionMode.Single, KsBridgeValueTransport.selectionMode(99))
    }

    // MARK: - 選択 index

    /** 複数選択 index は順序と重複を問わず同じ集合になる。 */
    @Test
    fun `複数選択 index は順序と重複を問わず同じ集合になる`() {
        assertEquals(setOf(0, 2), KsBridgeValueTransport.indexSet(intArrayOf(2, 0)))
        assertEquals(setOf(0, 2), KsBridgeValueTransport.indexSet(intArrayOf(0, 2, 2)))
    }

    /** 通知方向の複数選択 index は昇順・重複なしへ正規化される。 */
    @Test
    fun `通知方向の複数選択 index は昇順になる`() {
        assertEquals(
            listOf(0, 1, 2),
            KsBridgeValueTransport.indexList(setOf(2, 0, 1)).toList(),
        )
    }

    /** 順序違いの複数選択 DTO は同一の Native 値になる。 */
    @Test
    fun `順序違いの複数選択 DTO は同一の Native 値になる`() {
        val ascending = KsBridgePickerCell(title = "選択").apply {
            items = listOf("A", "B", "C").map { KsBridgePickerItem(it) }
            selectionMode = 1
            selectedIndices = intArrayOf(0, 2)
        }
        val descending = KsBridgePickerCell(title = "選択").apply {
            items = listOf("A", "B", "C").map { KsBridgePickerItem(it) }
            selectionMode = 1
            selectedIndices = intArrayOf(2, 0)
        }
        val bridge = KsBridgeFixture.withCells(listOf(ascending))

        val before: PickerCell? = KsBridgeFixture.storedCell(bridge)
        bridge.replaceCell(ascending.cellID, descending)
        val after: PickerCell? = KsBridgeFixture.storedCell(bridge)

        assertEquals(before?.selectedIndices, after?.selectedIndices)
        assertEquals(setOf(0, 2), after?.selectedIndices)
    }

    /** 範囲外の選択 index は正規化せず透過する。 */
    @Test
    fun `範囲外の選択 index は正規化せず透過する`() {
        val single = KsBridgePickerCell(title = "単一選択").apply {
            items = listOf("A", "B").map { KsBridgePickerItem(it) }
            selectedIndex = 9
        }
        val multiple = KsBridgePickerCell(title = "複数選択").apply {
            items = listOf("A", "B").map { KsBridgePickerItem(it) }
            selectionMode = 1
            selectedIndices = intArrayOf(-1, 7)
        }
        val bridge = KsBridgeFixture.withCells(listOf(single, multiple))

        val cells = KsBridgeFixture.storedCells(bridge)
        assertEquals(9, (cells[0] as PickerCell).selectedIndex)
        assertEquals(setOf(-1, 7), (cells[1] as PickerCell).selectedIndices)
    }
}
