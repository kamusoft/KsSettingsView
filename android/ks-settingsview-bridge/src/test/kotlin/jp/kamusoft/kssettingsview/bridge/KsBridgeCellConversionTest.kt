package jp.kamusoft.kssettingsview.bridge

import android.text.InputType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.kamusoft.kssettingsview.core.CellTitleAlignment
import jp.kamusoft.kssettingsview.ui.ButtonCell
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.CheckboxCell
import jp.kamusoft.kssettingsview.ui.CommandCell
import jp.kamusoft.kssettingsview.ui.DatePickerCell
import jp.kamusoft.kssettingsview.ui.DatePickerUIStyle
import jp.kamusoft.kssettingsview.ui.EntryCell
import jp.kamusoft.kssettingsview.ui.KsImage
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.NumberPickerCell
import jp.kamusoft.kssettingsview.ui.PickerCell
import jp.kamusoft.kssettingsview.ui.PickerSelectionMode
import jp.kamusoft.kssettingsview.ui.RadioCell
import jp.kamusoft.kssettingsview.ui.SimpleCheckCell
import jp.kamusoft.kssettingsview.ui.SwitchCell
import jp.kamusoft.kssettingsview.ui.TimePickerCell
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * Cell 種ごとの輸送 DTO が対応する Native Cell 型と値へ変換されることを検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsBridgeCellConversionTest {

    private var attachment: KsBridgeTestHost.Attachment? = null

    @After
    fun tearDown() {
        attachment?.close()
        attachment = null
    }

    /** 不透明な緑を表す ARGB 輸送値。 */
    private val opaqueGreen: Int = 0xFF00FF00.toInt()

    /** 不透明な緑の Compose 表現。 */
    private val opaqueGreenColor: Color = Color(opaqueGreen)

    // MARK: - 基本 Cell

    /** LabelCell DTO の共通フィールドが Native の LabelCell へ写される。 */
    @Test
    fun `LabelCell DTO が LabelCell へ変換される`() {
        val dto = KsBridgeLabelCell(
            title = "ラベル",
            descriptionText = "説明",
            valueText = "値",
            hintText = "ヒント",
            isEnabled = false,
            isVisible = false,
        )
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: LabelCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals("ラベル", cell?.title)
        assertEquals("説明", cell?.description)
        assertEquals("値", cell?.valueText)
        assertEquals("ヒント", cell?.hintText)
        assertEquals(false, cell?.isEnabled)
        assertEquals(false, cell?.isVisible)
    }

    /** CommandCell DTO の hideArrow とタップ通知の注入が Native へ届く。 */
    @Test
    fun `CommandCell DTO が CommandCell へ変換される`() {
        val dto = KsBridgeCommandCell(title = "コマンド").apply {
            valueText = "値"
            hideArrow = true
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: CommandCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals("コマンド", cell?.title)
        assertEquals("値", cell?.valueText)
        assertEquals(true, cell?.hideArrow)
        assertNotNull("タップ通知のコールバックが注入される", cell?.onTap)
    }

    /** ButtonCell DTO の色と配置序数が Native の値へ変換される。 */
    @Test
    fun `ButtonCell DTO が ButtonCell へ変換される`() {
        val dto = KsBridgeButtonCell(title = "ボタン").apply {
            titleColor = opaqueGreen
            titleAlignment = 0
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: ButtonCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals("ボタン", cell?.title)
        assertEquals(opaqueGreenColor, cell?.titleColor)
        assertEquals(CellTitleAlignment.START, cell?.titleAlignment)
        assertNotNull(cell?.onTap)
    }

    /** ButtonCell DTO の titleAlignment 未指定は Native 既定（中央寄せ）になる。 */
    @Test
    fun `ButtonCell DTO の titleAlignment 未指定は Native 既定になる`() {
        val bridge = KsBridgeFixture.withCells(listOf(KsBridgeButtonCell(title = "ボタン")))

        val cell: ButtonCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals(CellTitleAlignment.CENTER, cell?.titleAlignment)
    }

    /** SwitchCell DTO の二値と accent 色が Native へ写される。 */
    @Test
    fun `SwitchCell DTO が SwitchCell へ変換される`() {
        val dto = KsBridgeSwitchCell(title = "スイッチ").apply {
            isOn = true
            accentColor = opaqueGreen
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals("スイッチ", cell?.title)
        assertEquals(true, cell?.isOn)
        assertEquals(opaqueGreenColor, cell?.accentColor)
        assertNotNull(cell?.onValueChanged)
    }

    /** CheckboxCell DTO のチェック状態が Native へ写される。 */
    @Test
    fun `CheckboxCell DTO が CheckboxCell へ変換される`() {
        val dto = KsBridgeCheckboxCell(title = "チェック").apply { isChecked = true }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: CheckboxCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals("チェック", cell?.title)
        assertEquals(true, cell?.isChecked)
        assertNotNull(cell?.onValueChanged)
    }

    /** SimpleCheckCell DTO のチェック状態が Native へ写される。 */
    @Test
    fun `SimpleCheckCell DTO が SimpleCheckCell へ変換される`() {
        val dto = KsBridgeSimpleCheckCell(title = "シンプルチェック").apply { isChecked = true }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: SimpleCheckCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals("シンプルチェック", cell?.title)
        assertEquals(true, cell?.isChecked)
        assertNotNull(cell?.onValueChanged)
    }

    /** RadioCell DTO の group / value / selectedValue が Native へ写される。 */
    @Test
    fun `RadioCell DTO が RadioCell へ変換される`() {
        val dto = KsBridgeRadioCell(title = "ラジオ").apply {
            groupID = "group"
            value = "A"
            selectedValue = "B"
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: RadioCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals("group", cell?.groupId)
        assertEquals("A", cell?.value)
        assertEquals("B", cell?.selectedValue)
        assertNotNull(cell?.onSelected)
    }

    // MARK: - 入力 Cell

    /** EntryCell DTO の入力設定が Native の InputType / 配置 / 最大文字数へ変換される。 */
    @Test
    fun `EntryCell DTO が EntryCell へ変換される`() {
        val dto = KsBridgeEntryCell(title = "入力").apply {
            text = "abc"
            placeholder = "入力してください"
            keyboard = 5
            isPassword = true
            textAlignment = 1
            maxLength = 10
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: EntryCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals("abc", cell?.text)
        assertEquals("入力してください", cell?.placeholder)
        assertEquals(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            cell?.keyboardType,
        )
        assertEquals(true, cell?.isPassword)
        assertEquals(CellTitleAlignment.CENTER, cell?.textAlignment)
        assertEquals(10, cell?.maxLength)
        assertNotNull(cell?.onTextChanged)
    }

    /**
     * keyboard 種別とパスワード指定は独立した状態として運ばれる。
     *
     * 伏せ字の inputType 合成は表示層の責務であり、Bridge は keyboard 種別へパスワードの
     * variation を混ぜない。
     */
    @Test
    fun `EntryCell DTO は keyboard 種別とパスワード指定を独立に運ぶ`() {
        val expected = mapOf(
            0 to InputType.TYPE_CLASS_TEXT,
            1 to InputType.TYPE_CLASS_TEXT,
            2 to InputType.TYPE_CLASS_TEXT,
            3 to InputType.TYPE_CLASS_TEXT,
            4 to (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI),
            5 to (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS),
            6 to (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL),
            7 to InputType.TYPE_CLASS_PHONE,
        )

        for ((ordinal, inputType) in expected) {
            val dto = KsBridgeEntryCell(title = "入力").apply {
                keyboard = ordinal
                isPassword = true
            }
            val bridge = KsBridgeFixture.withCells(listOf(dto))

            val cell: EntryCell? = KsBridgeFixture.storedCell(bridge)
            assertEquals("keyboard 序数 $ordinal", inputType, cell?.keyboardType)
            assertEquals("keyboard 序数 $ordinal", true, cell?.isPassword)
        }
    }

    /** EntryCell DTO の配置・最大文字数の未指定は Native 既定になる。 */
    @Test
    fun `EntryCell DTO の textAlignment 未指定は Native 既定になる`() {
        val bridge = KsBridgeFixture.withCells(listOf(KsBridgeEntryCell(title = "入力")))

        val cell: EntryCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals(CellTitleAlignment.END, cell?.textAlignment)
        assertNull("未指定の最大文字数は無制限になる", cell?.maxLength)
    }

    /** EntryCell DTO の placeholder 色が Native の色型へ写される。 */
    @Test
    fun `EntryCell DTO の placeholderColor が Native の色へ写る`() {
        val dto = KsBridgeEntryCell(title = "入力").apply {
            placeholder = "未入力"
            placeholderColor = OPAQUE_GREEN
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: EntryCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals(Color(OPAQUE_GREEN), cell?.placeholderColor)
    }

    /** EntryCell DTO の placeholder 色の未指定は Native 側の未指定になる。 */
    @Test
    fun `EntryCell DTO の placeholderColor 未指定は Native 側の未指定になる`() {
        val bridge = KsBridgeFixture.withCells(listOf(KsBridgeEntryCell(title = "入力")))

        val cell: EntryCell? = KsBridgeFixture.storedCell(bridge)
        assertNull(cell?.placeholderColor)
    }

    /** 単一選択モードの PickerCell DTO が index 輸送で Native へ写される。 */
    @Test
    fun `PickerCell DTO が単一選択の PickerCell へ変換される`() {
        val dto = KsBridgePickerCell(title = "選択").apply {
            items = listOf("A", "B", "C").map { KsBridgePickerItem(it) }
            selectedIndex = 2
            pageTitle = "選んでください"
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: PickerCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals(PickerSelectionMode.Single, cell?.selectionMode)
        assertEquals(listOf("A", "B", "C"), cell?.items?.map { it.text })
        assertEquals(2, cell?.selectedIndex)
        assertEquals("選んでください", cell?.pageTitle)
        assertNotNull(cell?.onSelectionChanged)
    }

    /** 複数選択モードの PickerCell DTO が集合輸送で Native へ写される。 */
    @Test
    fun `PickerCell DTO が複数選択の PickerCell へ変換される`() {
        val dto = KsBridgePickerCell(title = "選択").apply {
            items = listOf("A", "B", "C").map { KsBridgePickerItem(it) }
            selectionMode = 1
            selectedIndices = intArrayOf(2, 0)
            maxSelectedNumber = 2
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: PickerCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals(PickerSelectionMode.Multiple, cell?.selectionMode)
        assertEquals(setOf(0, 2), cell?.selectedIndices)
        assertEquals(2, cell?.maxSelectedNumber)
        assertNotNull(cell?.onMultiSelectionChanged)
    }

    /** 副表示の有無が候補ごとに保たれ、空文字列の副表示は「なし」へ揃う。 */
    @Test
    fun `PickerCell DTO の副表示が候補ごとに保存される`() {
        val dto = KsBridgePickerCell(title = "選択").apply {
            items = listOf(
                KsBridgePickerItem("A", "補足A"),
                KsBridgePickerItem("B"),
                KsBridgePickerItem("C", ""),
            )
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: PickerCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals(listOf("A", "B", "C"), cell?.items?.map { it.text })
        assertEquals(listOf("補足A", null, null), cell?.items?.map { it.subText })
    }

    /** NumberPickerCell DTO の範囲・刻み・単位が Native へ写される。 */
    @Test
    fun `NumberPickerCell DTO が NumberPickerCell へ変換される`() {
        val dto = KsBridgeNumberPickerCell(title = "数値").apply {
            min = 5
            max = 50
            step = 5
            value = 20
            unit = "px"
            pickerTitle = "数値を選択"
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: NumberPickerCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals(5, cell?.min)
        assertEquals(50, cell?.max)
        assertEquals(5, cell?.step)
        assertEquals(20, cell?.value)
        assertEquals("px", cell?.unit)
        assertEquals("数値を選択", cell?.pickerTitle)
        assertNotNull(cell?.onValueChanged)
    }

    /** TimePickerCell DTO の ISO 時刻文字列が LocalTime へ変換される。 */
    @Test
    fun `TimePickerCell DTO が TimePickerCell へ変換される`() {
        val dto = KsBridgeTimePickerCell(title = "時刻").apply {
            time = "09:30"
            pickerTitle = "時刻を選択"
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: TimePickerCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals(LocalTime.of(9, 30), cell?.time)
        assertEquals("未指定の表示フォーマットは Native 既定になる", "HH:mm", cell?.format)
        assertEquals("未指定の時制は Native 既定になる", true, cell?.is24Hour)
        assertEquals("時刻を選択", cell?.pickerTitle)
        assertNotNull(cell?.onValueChanged)
    }

    /** TimePickerCell DTO の is24Hour が Native の時制へ写る。 */
    @Test
    fun `TimePickerCell DTO の is24Hour が Native へ写る`() {
        val dto = KsBridgeTimePickerCell(title = "時刻").apply {
            time = "09:30"
            is24Hour = false
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: TimePickerCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals(false, cell?.is24Hour)
    }

    /** DatePickerCell DTO の ISO 日付文字列と uiStyle 序数が Native へ変換される。 */
    @Test
    fun `DatePickerCell DTO が DatePickerCell へ変換される`() {
        val dto = KsBridgeDatePickerCell(title = "日付").apply {
            date = "2026-08-10"
            minDate = "2026-01-01"
            maxDate = "2026-12-31"
            uiStyle = 0
            todayText = "今日"
            androidButtonColor = opaqueGreen
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: DatePickerCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals(LocalDate.of(2026, 8, 10), cell?.date)
        assertEquals(LocalDate.of(2026, 1, 1), cell?.minDate)
        assertEquals(LocalDate.of(2026, 12, 31), cell?.maxDate)
        assertEquals(DatePickerUIStyle.Material, cell?.uiStyle)
        assertEquals("未指定の表示フォーマットは Native 既定になる", "yyyy/MM/dd", cell?.format)
        assertEquals("今日", cell?.todayText)
        assertEquals(opaqueGreenColor, cell?.androidButtonColor)
        assertNotNull(cell?.onValueChanged)
    }

    /** DatePickerCell DTO の uiStyle 未指定は Native 既定（Material）になる。 */
    @Test
    fun `DatePickerCell DTO の uiStyle 未指定は Native 既定になる`() {
        val dto = KsBridgeDatePickerCell(title = "日付").apply { date = "2026-08-10" }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: DatePickerCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals(DatePickerUIStyle.Material, cell?.uiStyle)
        assertNull(cell?.minDate)
        assertNull(cell?.maxDate)
    }

    /** uiStyle 序数 1（Wheels）は Android の Spinner 形式へ写される。 */
    @Test
    fun `DatePickerCell DTO の uiStyle 序数 1 は Spinner になる`() {
        val dto = KsBridgeDatePickerCell(title = "日付").apply { uiStyle = 1 }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: DatePickerCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals(DatePickerUIStyle.Spinner, cell?.uiStyle)
    }

    // MARK: - 共通フィールド（style / icon）

    /** style 輸送値の色・寸法・フォントが Native の CellStyle へ変換される。 */
    @Test
    fun `style 輸送値が CellStyle へ変換される`() {
        val dto = KsBridgeSwitchCell(title = "スイッチ").apply {
            style = KsBridgeCellStyle().apply {
                titleColor = opaqueGreen
                iconSize = 32.0
                iconRadius = 8.0
                cellHeight = 60.0
                titleFont = KsBridgeFont(pointSize = 21.0, isBold = true)
            }
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals(opaqueGreenColor, cell?.style?.titleColor)
        assertEquals(32.0.dp, cell?.style?.iconSize)
        assertEquals(8.0.dp, cell?.style?.iconRadius)
        assertEquals(60.0.dp, cell?.style?.cellHeight)
        assertNotNull(cell?.style?.titleFont)
    }

    /**
     * style 輸送値の 13 項目が、対応する `CellStyle` の項目へそれぞれ写される。
     *
     * 全項目に相異なる値を入れて一括で突き合わせ、引数の取り違えを検出できるようにする。
     */
    @Test
    fun `style 輸送値の全 13 項目が対応する CellStyle 項目へ写される`() {
        val dto = KsBridgeSwitchCell(title = "スイッチ").apply {
            style = KsBridgeCellStyle().apply {
                titleColor = 0xFF010203.toInt()
                titleFont = KsBridgeFont(pointSize = 11.0)
                descriptionColor = 0xFF040506.toInt()
                descriptionFont = KsBridgeFont(pointSize = 12.0)
                valueTextColor = 0xFF070809.toInt()
                valueTextFont = KsBridgeFont(pointSize = 13.0)
                iconSize = 24.0
                iconRadius = 6.0
                cellHeight = 56.0
                hintTextColor = 0xFF0A0B0C.toInt()
                hintTextFont = KsBridgeFont(pointSize = 14.0)
                backgroundColor = 0xFF0D0E0F.toInt()
                accentColor = 0xFF101112.toInt()
            }
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: SwitchCell? = KsBridgeFixture.storedCell(bridge)
        val style = cell?.style
        assertEquals(Color(0xFF010203.toInt()), style?.titleColor)
        assertEquals(11.0.sp, style?.titleFont?.fontSize)
        assertEquals(Color(0xFF040506.toInt()), style?.descriptionColor)
        assertEquals(12.0.sp, style?.descriptionFont?.fontSize)
        assertEquals(Color(0xFF070809.toInt()), style?.valueTextColor)
        assertEquals(13.0.sp, style?.valueTextFont?.fontSize)
        assertEquals(24.0.dp, style?.iconSize)
        assertEquals(6.0.dp, style?.iconRadius)
        assertEquals(56.0.dp, style?.cellHeight)
        assertEquals(Color(0xFF0A0B0C.toInt()), style?.hintTextColor)
        assertEquals(14.0.sp, style?.hintTextFont?.fontSize)
        assertEquals(Color(0xFF0D0E0F.toInt()), style?.backgroundColor)
        assertEquals(Color(0xFF101112.toInt()), style?.accentColor)
    }

    /** style 未指定は全項目未指定の CellStyle になる。 */
    @Test
    fun `style 未指定は全項目未指定の CellStyle になる`() {
        val bridge = KsBridgeFixture.withCells(listOf(KsBridgeLabelCell(title = "ラベル")))

        val cell: LabelCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals(CellStyle(), cell?.style)
    }

    /** icon 輸送値の Drawable が Native の KsImage へ包まれる。 */
    @Test
    fun `icon 輸送値が KsImage へ包まれる`() {
        val drawable = KsBridgeFixture.drawable()
        val dto = KsBridgeCommandCell(title = "コマンド").apply { icon = drawable }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: CommandCell? = KsBridgeFixture.storedCell(bridge)
        assertEquals(KsImage.Drawable(drawable), cell?.icon)
    }

    /** icon 未指定は null になる。 */
    @Test
    fun `icon 未指定は null になる`() {
        val bridge = KsBridgeFixture.withCells(listOf(KsBridgeCommandCell(title = "コマンド")))

        val cell: CommandCell? = KsBridgeFixture.storedCell(bridge)
        assertNull(cell?.icon)
    }

    // MARK: - 異種 Cell の混載

    /** 共通基底型のコレクションへ異種 Cell を混ぜて setRoot できる。 */
    @Test
    fun `異種 Cell を setRoot で混載できる`() {
        val bridge = KsBridgeFixture.withCells(
            listOf(
                KsBridgeLabelCell(title = "ラベル"),
                KsBridgeSwitchCell(title = "スイッチ"),
                KsBridgeEntryCell(title = "入力"),
                KsBridgeDatePickerCell(title = "日付"),
            ),
        )

        val cells = KsBridgeFixture.storedCells(bridge)
        assertEquals(4, cells.size)
        assertTrue(cells[0] is LabelCell)
        assertTrue(cells[1] is SwitchCell)
        assertTrue(cells[2] is EntryCell)
        assertTrue(cells[3] is DatePickerCell)
    }

    /** 1 回の replaceCells に Cell 種の異なる更新を混載でき、行の identity は変わらない。 */
    @Test
    fun `異種 Cell を replaceCells で同一バッチ更新できる`() {
        val label = KsBridgeLabelCell(title = "ラベル")
        val toggle = KsBridgeSwitchCell(title = "スイッチ")
        val bridge = KsBridgeFixture.withCells(listOf(label, toggle))

        val newEntry = KsBridgeEntryCell(title = "入力へ差し替え")
        val newToggle = KsBridgeSwitchCell(title = "スイッチ更新").apply { isOn = true }
        bridge.replaceCells(
            listOf(
                KsBridgeCellUpdate(cellID = label.cellID, cell = newEntry),
                KsBridgeCellUpdate(cellID = toggle.cellID, cell = newToggle),
            ),
        )

        val cells = KsBridgeFixture.storedCells(bridge)
        assertEquals("入力へ差し替え", (cells[0] as? EntryCell)?.title)
        assertEquals(true, (cells[1] as? SwitchCell)?.isOn)
        assertEquals("内容更新は行の identity を変えない", label.cellID, cells[0].id)
        assertEquals(toggle.cellID, cells[1].id)
    }

    /** 混載した異種 Cell が実描画される。 */
    @Test
    fun `混載した Cell が実描画される`() {
        val bridge = KsBridgeFixture.withCells(
            listOf(KsBridgeSwitchCell(title = "スイッチ"), KsBridgeEntryCell(title = "入力")),
        )
        val host = KsBridgeTestHost.attach(bridge).also { attachment = it }

        assertEquals(listOf("S", "スイッチ", "入力"), KsBridgeTestHost.renderedRows(host))
    }

    private companion object {
        /** 不透明な緑（ARGB）を表す輸送値。 */
        const val OPAQUE_GREEN: Int = 0xFF00FF00.toInt()
    }
}
