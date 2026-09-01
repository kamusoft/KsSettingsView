package jp.kamusoft.kssettingsview.compose

import android.text.InputType
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import jp.kamusoft.kssettingsview.ui.DatePickerUIStyle
import jp.kamusoft.kssettingsview.ui.DatePickerCell
import jp.kamusoft.kssettingsview.ui.EntryCell
import jp.kamusoft.kssettingsview.ui.NumberPickerCell
import jp.kamusoft.kssettingsview.ui.PickerCell
import jp.kamusoft.kssettingsview.ui.PickerSelectionMode
import jp.kamusoft.kssettingsview.ui.TimePickerCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * 入力系 Cell 5 種の DSL 拡張関数（`DSLSectionScope.EntryCell(...)` 等）が正しく
 * `DSLCellNode` に格納され、TwoWay binding 経由で MutableState を更新できることを検証する。
 */
class InputCellDslTest {

    // MARK: - EntryCell

    @Test
    fun `EntryCell DSL TwoWay で入力が MutableState に反映される`() {
        val text = mutableStateOf("init")
        val scope = DSLSettingsRootScope()
        scope.Section {
            EntryCell(title = "名前", text = text)
        }
        val tree = scope.build()
        val cell = tree[0].cellNodes[0].cell as EntryCell
        assertEquals("init", cell.text)
        // 内部の onTextChanged が MutableState を更新する
        cell.onTextChanged?.invoke("Taro")
        assertEquals("Taro", text.value)
    }

    @Test
    fun `EntryCell DSL keyboardType に Native InputType TYPE_CLASS_PHONE を渡せる`() {
        val text = mutableStateOf("")
        val scope = DSLSettingsRootScope()
        scope.Section {
            EntryCell(
                title = "電話",
                text = text,
                keyboardType = InputType.TYPE_CLASS_PHONE,
            )
        }
        val tree = scope.build()
        val cell = tree[0].cellNodes[0].cell as EntryCell
        assertEquals(InputType.TYPE_CLASS_PHONE, cell.keyboardType)
    }

    @Test
    fun `EntryCell DSL maxLength が data class に反映される`() {
        val text = mutableStateOf("")
        val scope = DSLSettingsRootScope()
        scope.Section {
            EntryCell(title = "x", text = text, maxLength = 5)
        }
        val tree = scope.build()
        val cell = tree[0].cellNodes[0].cell as EntryCell
        assertEquals(5, cell.maxLength)
    }

    @Test
    fun `EntryCell DSL callback overload が動作する`() {
        var captured: String? = null
        val scope = DSLSettingsRootScope()
        scope.Section {
            EntryCell(
                title = "x",
                text = "init",
                onTextChanged = { captured = it },
            )
        }
        val tree = scope.build()
        val cell = tree[0].cellNodes[0].cell as EntryCell
        assertEquals("init", cell.text)
        cell.onTextChanged?.invoke("changed")
        assertEquals("changed", captured)
    }

    @Test
    fun `EntryCell DSL placeholderColor が両 overload で data class に反映される`() {
        val color = Color(0xFFFF2D87)
        val text = mutableStateOf("")
        val scope = DSLSettingsRootScope()
        scope.Section {
            EntryCell(title = "TwoWay", text = text, placeholder = "p", placeholderColor = color)
            EntryCell(title = "callback", text = "", placeholder = "p", placeholderColor = color)
        }
        val tree = scope.build()

        assertEquals(color, (tree[0].cellNodes[0].cell as EntryCell).placeholderColor)
        assertEquals(color, (tree[0].cellNodes[1].cell as EntryCell).placeholderColor)
    }

    @Test
    fun `EntryCell DSL placeholderColor 未指定は null のまま`() {
        val text = mutableStateOf("")
        val scope = DSLSettingsRootScope()
        scope.Section {
            EntryCell(title = "x", text = text, placeholder = "p")
        }
        val tree = scope.build()
        assertNull((tree[0].cellNodes[0].cell as EntryCell).placeholderColor)
    }

    // MARK: - PickerCell single

    @Test
    fun `PickerCell DSL 単一選択 overload が解決される`() {
        val sel = mutableStateOf<Int?>(1)
        val scope = DSLSettingsRootScope()
        scope.Section {
            PickerCell(
                title = "テーマ",
                items = listOf("ライト", "ダーク"),
                selectedIndex = sel,
            )
        }
        val tree = scope.build()
        val cell = tree[0].cellNodes[0].cell as PickerCell
        assertEquals(PickerSelectionMode.Single, cell.selectionMode)
        assertEquals(1, cell.selectedIndex)
        cell.onSelectionChanged?.invoke(0)
        assertEquals(0, sel.value)
    }

    // MARK: - PickerCell multiple

    @Test
    fun `PickerCell DSL 複数選択 overload が解決される`() {
        val sel = mutableStateOf<Set<Int>>(setOf(0, 2))
        val scope = DSLSettingsRootScope()
        scope.Section {
            PickerCell(
                title = "通知種別",
                items = listOf("A", "B", "C"),
                selectedIndices = sel,
                maxSelectedNumber = 2,
            )
        }
        val tree = scope.build()
        val cell = tree[0].cellNodes[0].cell as PickerCell
        assertEquals(PickerSelectionMode.Multiple, cell.selectionMode)
        assertEquals(setOf(0, 2), cell.selectedIndices)
        assertEquals(2, cell.maxSelectedNumber)
        cell.onMultiSelectionChanged?.invoke(setOf(1))
        assertEquals(setOf(1), sel.value)
    }

    // MARK: - NumberPickerCell

    @Test
    fun `NumberPickerCell DSL TwoWay で値が反映される`() {
        val value = mutableStateOf(50)
        val scope = DSLSettingsRootScope()
        scope.Section {
            NumberPickerCell(title = "音量", value = value, min = 0, max = 100, step = 5)
        }
        val tree = scope.build()
        val cell = tree[0].cellNodes[0].cell as NumberPickerCell
        assertEquals(50, cell.value)
        assertEquals(0, cell.min)
        assertEquals(100, cell.max)
        assertEquals(5, cell.step)
        cell.onValueChanged?.invoke(75)
        assertEquals(75, value.value)
    }

    @Test
    fun `NumberPickerCell DSL で unit を指定できる`() {
        val value = mutableStateOf(15)
        val scope = DSLSettingsRootScope()
        scope.Section {
            NumberPickerCell(title = "サイズ", value = value, unit = "px")
        }
        val tree = scope.build()
        val cell = tree[0].cellNodes[0].cell as NumberPickerCell
        assertEquals("px", cell.unit)
    }

    @Test
    fun `NumberPickerCell DSL の unit 既定値は空文字`() {
        val value = mutableStateOf(15)
        val scope = DSLSettingsRootScope()
        scope.Section {
            NumberPickerCell(title = "サイズ", value = value)
        }
        val tree = scope.build()
        val cell = tree[0].cellNodes[0].cell as NumberPickerCell
        assertEquals("", cell.unit)
    }

    // MARK: - TimePickerCell

    @Test
    fun `TimePickerCell DSL TwoWay で LocalTime が反映される`() {
        val time = mutableStateOf(LocalTime.of(7, 30))
        val scope = DSLSettingsRootScope()
        scope.Section {
            TimePickerCell(title = "アラーム", time = time)
        }
        val tree = scope.build()
        val cell = tree[0].cellNodes[0].cell as TimePickerCell
        assertEquals(LocalTime.of(7, 30), cell.time)
        cell.onValueChanged?.invoke(LocalTime.of(8, 0))
        assertEquals(LocalTime.of(8, 0), time.value)
        assertTrue("既定は 24 時間制", cell.is24Hour)
    }

    @Test
    fun `TimePickerCell DSL の is24Hour が native cell へ透過する`() {
        val time = mutableStateOf(LocalTime.of(22, 15))
        val scope = DSLSettingsRootScope()
        scope.Section {
            TimePickerCell(title = "就寝", time = time, is24Hour = false)
        }
        val cell = scope.build()[0].cellNodes[0].cell as TimePickerCell
        assertFalse(cell.is24Hour)
    }

    // MARK: - DatePickerCell

    @Test
    fun `DatePickerCell DSL TwoWay で LocalDate が反映される`() {
        val date = mutableStateOf(LocalDate.of(2000, 1, 1))
        val scope = DSLSettingsRootScope()
        scope.Section {
            DatePickerCell(title = "誕生日", date = date)
        }
        val tree = scope.build()
        val cell = tree[0].cellNodes[0].cell as DatePickerCell
        assertEquals(LocalDate.of(2000, 1, 1), cell.date)
        assertEquals(DatePickerUIStyle.Material, cell.uiStyle)
        cell.onValueChanged?.invoke(LocalDate.of(2000, 12, 31))
        assertEquals(LocalDate.of(2000, 12, 31), date.value)
    }

    @Test
    fun `DatePickerCell DSL uiStyle Spinner が data class に反映される`() {
        val date = mutableStateOf(LocalDate.of(2026, 6, 1))
        val scope = DSLSettingsRootScope()
        scope.Section {
            DatePickerCell(
                title = "予約日",
                date = date,
                uiStyle = DatePickerUIStyle.Spinner,
            )
        }
        val tree = scope.build()
        val cell = tree[0].cellNodes[0].cell as DatePickerCell
        assertEquals(DatePickerUIStyle.Spinner, cell.uiStyle)
    }

    @Test
    fun `DatePickerCell DSL の todayText が data class に反映される`() {
        val date = mutableStateOf(LocalDate.of(2026, 8, 2))
        val scope = DSLSettingsRootScope()
        scope.Section {
            DatePickerCell(title = "誕生日", date = date, todayText = "今日")
        }
        val tree = scope.build()
        val cell = tree[0].cellNodes[0].cell as DatePickerCell
        assertEquals("今日", cell.todayText)
    }

    @Test
    fun `DatePickerCell DSL の todayText 既定は null`() {
        val date = mutableStateOf(LocalDate.of(2026, 8, 2))
        val scope = DSLSettingsRootScope()
        scope.Section {
            DatePickerCell(title = "誕生日", date = date)
        }
        val tree = scope.build()
        assertNull((tree[0].cellNodes[0].cell as DatePickerCell).todayText)
    }

    // MARK: - CellHandle chain

    @Test
    fun `CellHandle chain で cellHeight が EntryCell にも反映される`() {
        val text = mutableStateOf("")
        val scope = DSLSettingsRootScope()
        scope.Section {
            EntryCell(title = "x", text = text).cellHeight(80.dp)
        }
        val tree = scope.build()
        val cell = tree[0].cellNodes[0].cell as jp.kamusoft.kssettingsview.ui.DSLStyleModifiableCell
        assertEquals(80.dp, cell.style.cellHeight)
    }

    // MARK: - 5 種一括配置

    @Test
    fun `入力系 Cell 5 種すべての DSL 拡張関数で対応する data class が生成される`() {
        val text = mutableStateOf("")
        val selSingle = mutableStateOf<Int?>(0)
        val volume = mutableStateOf(0)
        val time = mutableStateOf(LocalTime.NOON)
        val date = mutableStateOf(LocalDate.of(2026, 1, 1))
        val scope = DSLSettingsRootScope()
        scope.Section(header = "全種") {
            EntryCell(title = "entry", text = text)
            PickerCell(title = "picker", items = listOf("A"), selectedIndex = selSingle)
            NumberPickerCell(title = "number", value = volume)
            TimePickerCell(title = "time", time = time)
            DatePickerCell(title = "date", date = date)
        }
        val cells = scope.build()[0].cellNodes.map { it.cell }
        assertEquals(5, cells.size)
        assertTrue(cells[0] is EntryCell)
        assertTrue(cells[1] is PickerCell)
        assertTrue(cells[2] is NumberPickerCell)
        assertTrue(cells[3] is TimePickerCell)
        assertTrue(cells[4] is DatePickerCell)
    }
}
