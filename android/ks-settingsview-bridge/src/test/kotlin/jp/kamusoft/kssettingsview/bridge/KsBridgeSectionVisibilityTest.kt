package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.ui.SwitchCell
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 受け取ったスイッチ変更だけを記録する listener 実装。
 */
private class SwitchRecordingListener : KsBridgeInteractionListener {

    /** 通知された（cellID, 新しい値）の並び。 */
    val changes: MutableList<Pair<String, Boolean>> = mutableListOf()

    override fun switchCellChanged(cellID: String, isOn: Boolean) {
        changes.add(cellID to isOn)
    }

    override fun commandCellTapped(cellID: String) = Unit
    override fun buttonCellTapped(cellID: String) = Unit
    override fun customCellTapped(cellID: String) = Unit
    override fun checkboxCellChanged(cellID: String, isChecked: Boolean) = Unit
    override fun simpleCheckCellChanged(cellID: String, isChecked: Boolean) = Unit
    override fun radioCellSelected(cellID: String, value: String) = Unit
    override fun entryCellTextChanged(cellID: String, text: String) = Unit
    override fun pickerCellSelectionChanged(cellID: String, index: Int) = Unit
    override fun pickerCellMultiSelectionChanged(cellID: String, indices: IntArray) = Unit
    override fun numberPickerCellChanged(cellID: String, value: Int) = Unit
    override fun timePickerCellChanged(cellID: String, time: String) = Unit
    override fun datePickerCellChanged(cellID: String, date: String) = Unit
}

/**
 * Section の可視性輸送と、内容差し替え時の cellID 温存を検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsBridgeSectionVisibilityTest {

    private var attachment: KsBridgeTestHost.Attachment? = null

    @After
    fun tearDown() {
        attachment?.close()
        attachment = null
    }

    // MARK: - isVisible の輸送

    /** Section DTO の isVisible の既定は true。 */
    @Test
    fun `Section DTO の isVisible 既定は true`() {
        assertTrue(KsBridgeSection(headerText = "S", footerText = null).isVisible)
    }

    /** 非表示 Section は model に保持されたまま表示から除外される。 */
    @Test
    fun `非表示 Section を setRoot で輸送すると表示から除外される`() {
        val bridge = KsSettingsBridge()
        val builder = KsBridgeRootBuilder()
        val visible = builder.addSection(headerText = "表示", footerText = null)
        visible.addCell(KsBridgeLabelCell(title = "A"))
        val hidden = builder.addSection(headerText = "非表示", footerText = null)
        hidden.isVisible = false
        hidden.addCell(KsBridgeLabelCell(title = "B"))
        bridge.setRoot(builder)

        assertEquals("モデルには保持される", 2, bridge.store.state.value.sections.size)
        assertFalse(bridge.store.state.value.sections[1].isVisible)

        val host = KsBridgeTestHost.attach(bridge).also { attachment = it }
        assertEquals(listOf("表示", "A"), KsBridgeTestHost.renderedRows(host))
    }

    /** replaceSection で isVisible を切り替えると表示が追随する。 */
    @Test
    fun `replaceSection で isVisible を切り替えると表示が追随する`() {
        val bridge = KsBridgeFixture.withCells(listOf(KsBridgeLabelCell(title = "A")))
        val host = KsBridgeTestHost.attach(bridge).also { attachment = it }
        val sectionID = bridge.store.state.value.sections[0].id

        val hidden = KsBridgeSection(
            headerText = "S",
            footerText = null,
            cells = listOf(KsBridgeLabelCell(title = "A")),
        ).apply { isVisible = false }
        bridge.replaceSection(sectionID, hidden)
        KsBridgeTestHost.pump(host)

        assertEquals(emptyList<String>(), KsBridgeTestHost.renderedRows(host))
    }

    // MARK: - Header / Footer 表示トグルの輸送

    /** Section DTO の表示トグルの既定は両方 true。 */
    @Test
    fun `Section DTO の表示トグル既定は true`() {
        val section = KsBridgeSection(headerText = "S", footerText = "F")

        assertTrue(section.isHeaderVisible)
        assertTrue(section.isFooterVisible)
    }

    /** トグル未指定の DTO は core の Section へ true を伝搬する。 */
    @Test
    fun `トグル未指定の DTO は core の Section へ true を伝搬する`() {
        val bridge = KsSettingsBridge()
        val builder = KsBridgeRootBuilder()
        builder.addSection(headerText = "S", footerText = "F")
        bridge.setRoot(builder)

        assertTrue(bridge.store.state.value.sections[0].isHeaderVisible)
        assertTrue(bridge.store.state.value.sections[0].isFooterVisible)
    }

    /** DTO の isHeaderVisible は core の Section へ伝搬する。 */
    @Test
    fun `DTO の isHeaderVisible は core の Section へ伝搬する`() {
        val bridge = KsSettingsBridge()
        val builder = KsBridgeRootBuilder()
        builder.addSection(headerText = "S", footerText = "F").isHeaderVisible = false
        bridge.setRoot(builder)

        assertFalse(bridge.store.state.value.sections[0].isHeaderVisible)
        assertTrue("footer 側は巻き込まれない", bridge.store.state.value.sections[0].isFooterVisible)
    }

    /** DTO の isFooterVisible は core の Section へ伝搬する。 */
    @Test
    fun `DTO の isFooterVisible は core の Section へ伝搬する`() {
        val bridge = KsSettingsBridge()
        val builder = KsBridgeRootBuilder()
        builder.addSection(headerText = "S", footerText = "F").isFooterVisible = false
        bridge.setRoot(builder)

        assertFalse(bridge.store.state.value.sections[0].isFooterVisible)
        assertTrue("header 側は巻き込まれない", bridge.store.state.value.sections[0].isHeaderVisible)
    }

    /** replaceSection で表示トグルを切り替えても配下 Cell は残る。 */
    @Test
    fun `replaceSection で表示トグルを切り替えても Cell は残る`() {
        val bridge = KsBridgeFixture.withCells(listOf(KsBridgeLabelCell(title = "A")))
        val host = KsBridgeTestHost.attach(bridge).also { attachment = it }
        val sectionID = bridge.store.state.value.sections[0].id

        val hidden = KsBridgeSection(
            headerText = "S",
            footerText = null,
            cells = listOf(KsBridgeLabelCell(title = "A")),
        ).apply { isHeaderVisible = false }
        bridge.replaceSection(sectionID, hidden)
        KsBridgeTestHost.pump(host)

        assertFalse(bridge.store.state.value.sections[0].isHeaderVisible)
        assertEquals(listOf("A"), KsBridgeTestHost.renderedRows(host))
    }

    // MARK: - cellID の温存

    /** adoptCellID で採番済み ID を引き継げる。 */
    @Test
    fun `adoptCellID で採番済み ID を引き継げる`() {
        val original = KsBridgeLabelCell(title = "A")
        val replacement = KsBridgeLabelCell(title = "A")

        assertNotEquals(original.cellID, replacement.cellID)
        assertTrue(replacement.adoptCellID(original.cellID))
        assertEquals(original.cellID, replacement.cellID)
    }

    /** adoptCellID は canonical UUID でない値を無視する。 */
    @Test
    fun `adoptCellID は canonical UUID でない値を無視する`() {
        val cell = KsBridgeLabelCell(title = "A")
        val before = cell.cellID

        assertFalse(cell.adoptCellID(KsBridgeFixture.UNKNOWN_IDENTIFIER))
        assertEquals(before, cell.cellID)
    }

    /** ID 引き継ぎ済み DTO での replaceSection は通知 ID を温存する。 */
    @Test
    fun `ID 引き継ぎ済み DTO での replaceSection は通知 ID を温存する`() {
        val toggle = KsBridgeSwitchCell(title = "スイッチ")
        val bridge = KsBridgeFixture.withCells(listOf(toggle))
        val recorder = SwitchRecordingListener()
        bridge.interactionListener = recorder
        val sectionID = bridge.store.state.value.sections[0].id

        val replacementToggle = KsBridgeSwitchCell(title = "スイッチ").apply {
            adoptCellID(toggle.cellID)
        }
        val replacement = KsBridgeSection(
            headerText = "S 更新",
            footerText = null,
            cells = listOf(replacementToggle),
        )
        bridge.replaceSection(sectionID, replacement)

        val cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        cell?.onValueChanged?.invoke(true)

        assertEquals(toggle.cellID, bridge.store.state.value.sections[0].cells[0].id)
        assertEquals(listOf(toggle.cellID to true), recorder.changes)
    }

    /** ID 引き継ぎのない replaceSection は cellID を再採番する。 */
    @Test
    fun `ID 引き継ぎのない replaceSection は cellID を再採番する`() {
        val toggle = KsBridgeSwitchCell(title = "スイッチ")
        val bridge = KsBridgeFixture.withCells(listOf(toggle))
        val recorder = SwitchRecordingListener()
        bridge.interactionListener = recorder
        val sectionID = bridge.store.state.value.sections[0].id

        val replacementToggle = KsBridgeSwitchCell(title = "スイッチ")
        val replacement = KsBridgeSection(
            headerText = "S",
            footerText = null,
            cells = listOf(replacementToggle),
        )
        bridge.replaceSection(sectionID, replacement)

        val cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        cell?.onValueChanged?.invoke(true)

        assertNotEquals(toggle.cellID, replacementToggle.cellID)
        assertEquals(listOf(replacementToggle.cellID to true), recorder.changes)
    }

    /** isVisible の往復差し替え後も、温存した cellID で通知される。 */
    @Test
    fun `isVisible 差し替え後も温存した cellID で通知される`() {
        val toggle = KsBridgeSwitchCell(title = "スイッチ")
        val bridge = KsBridgeFixture.withCells(listOf(toggle))
        val recorder = SwitchRecordingListener()
        bridge.interactionListener = recorder
        val sectionID = bridge.store.state.value.sections[0].id

        // 非表示へ、続けて表示へ戻す（どちらも同じ Section を差し替える経路）
        listOf(false, true).forEach { visible ->
            val cell = KsBridgeSwitchCell(title = "スイッチ").apply { adoptCellID(toggle.cellID) }
            val replacement = KsBridgeSection(
                headerText = "S",
                footerText = null,
                cells = listOf(cell),
            ).apply { isVisible = visible }
            bridge.replaceSection(sectionID, replacement)
        }

        val cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        cell?.onValueChanged?.invoke(true)

        assertTrue(bridge.store.state.value.sections[0].isVisible)
        assertEquals(listOf(toggle.cellID to true), recorder.changes)
    }
}
