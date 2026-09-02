package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.ui.ButtonCell
import jp.kamusoft.kssettingsview.ui.CheckboxCell
import jp.kamusoft.kssettingsview.ui.CommandCell
import jp.kamusoft.kssettingsview.ui.DatePickerCell
import jp.kamusoft.kssettingsview.ui.EntryCell
import jp.kamusoft.kssettingsview.ui.NumberPickerCell
import jp.kamusoft.kssettingsview.ui.PickerCell
import jp.kamusoft.kssettingsview.ui.RadioCell
import jp.kamusoft.kssettingsview.ui.SimpleCheckCell
import jp.kamusoft.kssettingsview.ui.SwitchCell
import jp.kamusoft.kssettingsview.ui.TimePickerCell
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
 * 受け取った通知を発生順に `"メソッド名(引数)"` の形で記録する listener 実装。
 */
private class RecordingInteractionListener : KsBridgeInteractionListener {

    /** 受け取った通知の並び。 */
    val notifications: MutableList<String> = mutableListOf()

    override fun commandCellTapped(cellID: String) {
        notifications.add("commandCellTapped($cellID)")
    }

    override fun buttonCellTapped(cellID: String) {
        notifications.add("buttonCellTapped($cellID)")
    }

    override fun customCellTapped(cellID: String) {
        notifications.add("customCellTapped($cellID)")
    }

    override fun switchCellChanged(cellID: String, isOn: Boolean) {
        notifications.add("switchCellChanged($cellID,$isOn)")
    }

    override fun checkboxCellChanged(cellID: String, isChecked: Boolean) {
        notifications.add("checkboxCellChanged($cellID,$isChecked)")
    }

    override fun simpleCheckCellChanged(cellID: String, isChecked: Boolean) {
        notifications.add("simpleCheckCellChanged($cellID,$isChecked)")
    }

    override fun radioCellSelected(cellID: String, value: String) {
        notifications.add("radioCellSelected($cellID,$value)")
    }

    override fun entryCellTextChanged(cellID: String, text: String) {
        notifications.add("entryCellTextChanged($cellID,$text)")
    }

    override fun pickerCellSelectionChanged(cellID: String, index: Int) {
        notifications.add("pickerCellSelectionChanged($cellID,$index)")
    }

    override fun pickerCellMultiSelectionChanged(cellID: String, indices: IntArray) {
        notifications.add("pickerCellMultiSelectionChanged($cellID,${indices.toList()})")
    }

    override fun numberPickerCellChanged(cellID: String, value: Int) {
        notifications.add("numberPickerCellChanged($cellID,$value)")
    }

    override fun timePickerCellChanged(cellID: String, time: String) {
        notifications.add("timePickerCellChanged($cellID,$time)")
    }

    override fun datePickerCellChanged(cellID: String, date: String) {
        notifications.add("datePickerCellChanged($cellID,$date)")
    }
}

/**
 * Native Cell のコールバックが interaction listener へ転送されることを検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsBridgeInteractionListenerTest {

    // MARK: - Cell 種別ごとの通知

    /** タップと二値変更が Cell 種別ごとのメソッドで通知される。 */
    @Test
    fun `タップと二値変更が対応するメソッドで通知される`() {
        val command = KsBridgeCommandCell(title = "コマンド")
        val button = KsBridgeButtonCell(title = "ボタン")
        val toggle = KsBridgeSwitchCell(title = "スイッチ")
        val checkbox = KsBridgeCheckboxCell(title = "チェック")
        val simpleCheck = KsBridgeSimpleCheckCell(title = "シンプルチェック")
        val bridge = KsBridgeFixture.withCells(listOf(command, button, toggle, checkbox, simpleCheck))
        val recorder = RecordingInteractionListener()
        bridge.interactionListener = recorder

        val cells = KsBridgeFixture.storedCells(bridge)
        (cells[0] as CommandCell).onTap?.invoke()
        (cells[1] as ButtonCell).onTap?.invoke()
        (cells[2] as SwitchCell).onValueChanged?.invoke(true)
        (cells[3] as CheckboxCell).onValueChanged?.invoke(true)
        (cells[4] as SimpleCheckCell).onValueChanged?.invoke(false)

        assertEquals(
            listOf(
                "commandCellTapped(${command.cellID})",
                "buttonCellTapped(${button.cellID})",
                "switchCellChanged(${toggle.cellID},true)",
                "checkboxCellChanged(${checkbox.cellID},true)",
                "simpleCheckCellChanged(${simpleCheck.cellID},false)",
            ),
            recorder.notifications,
        )
    }

    /** radio 選択と entry のテキスト変更が対応するメソッドで通知される。 */
    @Test
    fun `radio 選択と entry 変更が対応するメソッドで通知される`() {
        val radio = KsBridgeRadioCell(title = "ラジオ").apply {
            groupID = "group"
            value = "A"
        }
        val entry = KsBridgeEntryCell(title = "入力")
        val bridge = KsBridgeFixture.withCells(listOf(radio, entry))
        val recorder = RecordingInteractionListener()
        bridge.interactionListener = recorder

        val cells = KsBridgeFixture.storedCells(bridge)
        (cells[0] as RadioCell).onSelected?.invoke("A")
        (cells[1] as EntryCell).onTextChanged?.invoke("abc")

        assertEquals(
            listOf(
                "radioCellSelected(${radio.cellID},A)",
                "entryCellTextChanged(${entry.cellID},abc)",
            ),
            recorder.notifications,
        )
    }

    /** picker の単一 / 複数選択と数値変更が対応するメソッドで通知される。 */
    @Test
    fun `picker 選択と数値変更が対応するメソッドで通知される`() {
        val single = KsBridgePickerCell(title = "単一選択").apply {
            items = listOf("A", "B", "C").map { KsBridgePickerItem(it) }
        }
        val multiple = KsBridgePickerCell(title = "複数選択").apply {
            items = listOf("A", "B", "C").map { KsBridgePickerItem(it) }
            selectionMode = 1
        }
        val number = KsBridgeNumberPickerCell(title = "数値")
        val bridge = KsBridgeFixture.withCells(listOf(single, multiple, number))
        val recorder = RecordingInteractionListener()
        bridge.interactionListener = recorder

        val cells = KsBridgeFixture.storedCells(bridge)
        (cells[0] as PickerCell).onSelectionChanged?.invoke(1)
        (cells[1] as PickerCell).onMultiSelectionChanged?.invoke(setOf(2, 0))
        (cells[2] as NumberPickerCell).onValueChanged?.invoke(42)

        assertEquals(
            listOf(
                "pickerCellSelectionChanged(${single.cellID},1)",
                "pickerCellMultiSelectionChanged(${multiple.cellID},[0, 2])",
                "numberPickerCellChanged(${number.cellID},42)",
            ),
            recorder.notifications,
        )
    }

    /** 時刻と日付の変更が輸送書式の ISO 文字列で通知される。 */
    @Test
    fun `時刻と日付の変更が ISO 文字列で通知される`() {
        val time = KsBridgeTimePickerCell(title = "時刻")
        val date = KsBridgeDatePickerCell(title = "日付")
        val bridge = KsBridgeFixture.withCells(listOf(time, date))
        val recorder = RecordingInteractionListener()
        bridge.interactionListener = recorder

        val cells = KsBridgeFixture.storedCells(bridge)
        (cells[0] as TimePickerCell).onValueChanged?.invoke(LocalTime.of(9, 5))
        (cells[1] as DatePickerCell).onValueChanged?.invoke(LocalDate.of(2026, 8, 10))

        assertEquals(
            listOf(
                "timePickerCellChanged(${time.cellID},09:05)",
                "datePickerCellChanged(${date.cellID},2026-08-10)",
            ),
            recorder.notifications,
        )
    }

    // MARK: - 通知先の設定と解除

    /** listener 未設定でも操作は例外なく破棄される。 */
    @Test
    fun `listener 未設定の操作は破棄される`() {
        val bridge = KsBridgeFixture.withCells(listOf(KsBridgeSwitchCell(title = "スイッチ")))

        val cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        assertNotNull(cell?.onValueChanged)
        cell?.onValueChanged?.invoke(true)
    }

    /** listener 解除後の操作は通知されない。 */
    @Test
    fun `listener 解除後の操作は通知されない`() {
        val toggle = KsBridgeSwitchCell(title = "スイッチ")
        val bridge = KsBridgeFixture.withCells(listOf(toggle))
        val recorder = RecordingInteractionListener()
        bridge.interactionListener = recorder

        val cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        cell?.onValueChanged?.invoke(true)
        bridge.interactionListener = null
        cell?.onValueChanged?.invoke(false)

        assertEquals(listOf("switchCellChanged(${toggle.cellID},true)"), recorder.notifications)
    }

    /** dispose 後の操作は通知されず、listener も解除される。 */
    @Test
    fun `dispose 後の操作は通知されない`() {
        val bridge = KsBridgeFixture.withCells(listOf(KsBridgeSwitchCell(title = "スイッチ")))
        val recorder = RecordingInteractionListener()
        bridge.interactionListener = recorder

        val cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        bridge.dispose()
        cell?.onValueChanged?.invoke(true)

        assertEquals(emptyList<String>(), recorder.notifications)
        assertNull(bridge.interactionListener)
    }

    /** listener 設定後に構築した Cell にも通知が届く。 */
    @Test
    fun `listener 設定後に構築した Cell にも通知が届く`() {
        val bridge = KsSettingsBridge()
        val recorder = RecordingInteractionListener()
        bridge.interactionListener = recorder

        val builder = KsBridgeRootBuilder()
        val section = builder.addSection(headerText = "S", footerText = null)
        val toggle = KsBridgeSwitchCell(title = "スイッチ")
        section.addCell(toggle)
        bridge.setRoot(builder)

        val cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        cell?.onValueChanged?.invoke(true)

        assertEquals(listOf("switchCellChanged(${toggle.cellID},true)"), recorder.notifications)
    }

    /** insertCell / replaceCell で作った行にも、その行の ID で通知が届く。 */
    @Test
    fun `insertCell と replaceCell で作った行にも通知が届く`() {
        val bridge = KsBridgeFixture.withCells(listOf(KsBridgeLabelCell(title = "ラベル")))
        val recorder = RecordingInteractionListener()
        bridge.interactionListener = recorder
        val sectionID = bridge.store.state.value.sections[0].id

        val inserted = KsBridgeCheckboxCell(title = "追加チェック")
        val insertedID = bridge.insertCell(inserted, sectionID, index = 1)
        val replacement = KsBridgeCommandCell(title = "差し替えコマンド")
        val replacedID = bridge.replaceCell(
            cellID = bridge.store.state.value.sections[0].cells[0].id,
            newCell = replacement,
        )

        val cells = KsBridgeFixture.storedCells(bridge)
        (cells[0] as CommandCell).onTap?.invoke()
        (cells[1] as CheckboxCell).onValueChanged?.invoke(true)

        assertEquals(
            listOf(
                "commandCellTapped($replacedID)",
                "checkboxCellChanged($insertedID,true)",
            ),
            recorder.notifications,
        )
    }
}
