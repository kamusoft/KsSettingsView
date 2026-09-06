package jp.kamusoft.kssettingsview.compose

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import jp.kamusoft.kssettingsview.ui.ButtonCell
import jp.kamusoft.kssettingsview.ui.CheckboxCell
import jp.kamusoft.kssettingsview.ui.CommandCell
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.RadioCell
import jp.kamusoft.kssettingsview.ui.SimpleCheckCell
import jp.kamusoft.kssettingsview.ui.SwitchCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 基本 Cell 7 種の DSL 拡張関数（`DSLSectionScope.LabelCell(...)` 等）が正しく
 * `DSLCellNode` に格納され、`CellHandle` chain（`.cellHeight(...)` / `.cellID(...)`）が
 * 動作することを検証する。
 */
class BasicCellDslTest {

    @Test
    fun `LabelCell DSL 拡張関数で data class が DSLCellNode に格納される`() {
        val scope = DSLSettingsRootScope()
        scope.Section(header = "一般") {
            LabelCell(title = "通知")
        }
        val tree = scope.build()
        assertEquals(1, tree.size)
        val sectionNode = tree[0]
        assertEquals(1, sectionNode.cellNodes.size)
        val cell = sectionNode.cellNodes[0].cell
        assertTrue("DSL 拡張関数経由で LabelCell が格納されること", cell is LabelCell)
        assertEquals("通知", (cell as LabelCell).title)
    }

    @Test
    fun `7 種の DSL 拡張関数すべてが対応する data class を生成する`() {
        val scope = DSLSettingsRootScope()
        scope.Section(header = "全種") {
            LabelCell(title = "label")
            CommandCell(title = "command")
            ButtonCell(title = "button")
            SwitchCell(title = "switch", isOn = true)
            CheckboxCell(title = "checkbox", isChecked = false)
            RadioCell(title = "radio", groupId = "g", value = "a", selectedValue = "a")
            SimpleCheckCell(title = "simple", isChecked = false)
        }
        val tree = scope.build()
        val cells = tree[0].cellNodes.map { it.cell }
        assertEquals(7, cells.size)
        assertTrue(cells[0] is LabelCell)
        assertTrue(cells[1] is CommandCell)
        assertTrue(cells[2] is ButtonCell)
        assertTrue(cells[3] is SwitchCell)
        assertTrue(cells[4] is CheckboxCell)
        assertTrue(cells[5] is RadioCell)
        assertTrue(cells[6] is SimpleCheckCell)
    }

    @Test
    fun `SwitchCell DSL 拡張関数 MutableState 版で値変更が伝播する`() {
        val state = mutableStateOf(false)
        val scope = DSLSettingsRootScope()
        scope.Section {
            SwitchCell(title = "通知", isOn = state)
        }
        val tree = scope.build()
        val switchCell = tree[0].cellNodes[0].cell as SwitchCell
        // 初期値が反映される
        assertEquals(false, switchCell.isOn)
        // 内部の onValueChanged が state を更新する
        switchCell.onValueChanged?.invoke(true)
        assertEquals(true, state.value)
    }

    @Test
    fun `CellHandle chain で cellHeight が反映される`() {
        val scope = DSLSettingsRootScope()
        scope.Section {
            LabelCell(title = "高さ").cellHeight(80.dp)
        }
        val tree = scope.build()
        val cell = tree[0].cellNodes[0].cell as jp.kamusoft.kssettingsview.ui.DSLStyleModifiableCell
        assertEquals(80.dp, cell.style.cellHeight)
    }

    @Test
    fun `CellHandle chain で cellID 明示指定が identityHint に反映される`() {
        val scope = DSLSettingsRootScope()
        scope.Section {
            LabelCell(title = "通知").cellID("notification-toggle")
        }
        val tree = scope.build()
        val node = tree[0].cellNodes[0]
        val hint = node.identityHint
        assertNotNull(hint)
        assertTrue(hint is DSLIdentityHint.Explicit)
        assertEquals("notification-toggle", (hint as DSLIdentityHint.Explicit).id)
    }

    /**
     * DSL の色引数は省略すると未指定（`Color.Unspecified`）になる。
     *
     * 既定値が `Color.Black` のような具体色へ退行すると、`CellStyle` / `Theme` / 外観の既定へ
     * 進む段が Cell 固有段で止まる。省略した 5 本すべてを観測する。
     */
    @Test
    fun `基本 Cell の DSL は色引数を省略すると未指定になる`() {
        val scope = DSLSettingsRootScope()
        scope.Section(header = "省略") {
            ButtonCell(title = "button")
            SwitchCell(title = "switch", isOn = true)
            CheckboxCell(title = "checkbox", isChecked = false)
            RadioCell(title = "radio", groupId = "g", value = "a", selectedValue = "a")
            SimpleCheckCell(title = "simple", isChecked = false)
        }
        val cells = scope.build()[0].cellNodes.map { it.cell }

        assertEquals(Color.Unspecified, (cells[0] as ButtonCell).titleColor)
        assertEquals(Color.Unspecified, (cells[1] as SwitchCell).accentColor)
        assertEquals(Color.Unspecified, (cells[2] as CheckboxCell).accentColor)
        assertEquals(Color.Unspecified, (cells[3] as RadioCell).accentColor)
        assertEquals(Color.Unspecified, (cells[4] as SimpleCheckCell).accentColor)
    }

    @Test
    fun `基本 Cell の DSL は明示した色引数をそのまま渡す`() {
        val explicit = Color(0xFF12AB34)
        val scope = DSLSettingsRootScope()
        scope.Section(header = "明示") {
            ButtonCell(title = "button", titleColor = explicit)
            SwitchCell(title = "switch", isOn = true, accentColor = explicit)
            CheckboxCell(title = "checkbox", isChecked = false, accentColor = explicit)
            RadioCell(title = "radio", groupId = "g", value = "a", selectedValue = "a", accentColor = explicit)
            SimpleCheckCell(title = "simple", isChecked = false, accentColor = explicit)
        }
        val cells = scope.build()[0].cellNodes.map { it.cell }

        assertEquals(explicit, (cells[0] as ButtonCell).titleColor)
        assertEquals(explicit, (cells[1] as SwitchCell).accentColor)
        assertEquals(explicit, (cells[2] as CheckboxCell).accentColor)
        assertEquals(explicit, (cells[3] as RadioCell).accentColor)
        assertEquals(explicit, (cells[4] as SimpleCheckCell).accentColor)
    }

    @Test
    fun `CommandCell DSL 拡張関数で onTap がそのまま data class に渡る`() {
        var called = 0
        val scope = DSLSettingsRootScope()
        scope.Section {
            CommandCell(title = "License", onTap = { called++ })
        }
        val tree = scope.build()
        val cell = tree[0].cellNodes[0].cell as CommandCell
        cell.onTap?.invoke()
        assertEquals(1, called)
    }
}
