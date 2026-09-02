package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import java.time.LocalTime

/**
 * [TimePickerCell] の選択面（[TimeSelectionSheet]）の提示・時制の決定・候補系列・確定 / 非確定
 * dismiss・構成変更・タイトル解決・Locale 追随を検証する（android/ADR-0018）。
 *
 * 選択面は [TimePickerCellViewHolder] の行タップ経路から開き、実際の配線ごと検証する。
 * ホストの XML テーマは意図的に Material3 派生にしない（同梱テーマの常時ラップにより、
 * ホストのテーマ前提なしで成立することを併せて示す。android/ADR-0020）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ja")
class TimeSelectionSheetTest {

    /**
     * `FragmentActivity` ではないホスト。
     *
     * 選択面が Fragment に依存せず、`ComponentActivity` だけのホストでも提示できることを
     * ここで観測する。
     */
    class HostActivity : ComponentActivity() {
        lateinit var container: FrameLayout

        override fun onCreate(savedInstanceState: Bundle?) {
            // Material3 派生ではないフレームワーク標準テーマ。
            setTheme(android.R.style.Theme_Material_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            container = FrameLayout(this)
            setContentView(container)
        }
    }

    private var controller: ActivityController<HostActivity>? = null

    @After
    fun tearDown() {
        controller?.close()
        controller = null
    }

    /** ホストのテーマを被せない素の Context（同梱テーマの常時ラップで成立する）。 */
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    private fun timeCell(
        time: LocalTime = LocalTime.of(14, 30),
        format: String = "HH:mm",
        is24Hour: Boolean = true,
        title: String = "起床時刻",
        pickerTitle: String? = null,
        accentColor: Color? = null,
        style: CellStyle = CellStyle(),
        isEnabled: Boolean = true,
        onValueChanged: ((LocalTime) -> Unit)? = null,
    ): TimePickerCell = TimePickerCell(
        title = title,
        pickerTitle = pickerTitle,
        time = time,
        format = format,
        is24Hour = is24Hour,
        accentColor = accentColor,
        style = style,
        isEnabled = isEnabled,
        onValueChanged = onValueChanged,
    )

    /** Cell の行タップで選択面を開き、表示された [TimeSelectionSheet] を返す。 */
    private fun openSheet(
        cell: TimePickerCell,
        theme: Theme = Theme(),
        host: Context = ctx,
    ): TimeSelectionSheet = requireNotNull(tapRow(cell, theme, host)) { "選択面が提示されていない" }

    /** Cell の行をタップし、表示された選択面を返す（提示されなければ `null`）。 */
    private fun tapRow(
        cell: TimePickerCell,
        theme: Theme = Theme(),
        host: Context = ctx,
    ): TimeSelectionSheet? {
        val vh = TimePickerCellViewHolder.create(FrameLayout(host))
        vh.bind(cell, theme)
        vh.views.root.performClick()
        return ShadowDialog.getLatestDialog() as? TimeSelectionSheet
    }

    /** 時系列の選択を [hour]（系列の表示値）へ移す（スナップ静止と同じ通知経路を通る）。 */
    private fun selectHour(sheet: TimeSelectionSheet, hour: Int) {
        val index = (0 until sheet.candidates.hourCount).first { sheet.candidates.hourAt(it) == hour }
        sheet.hourWheel.setSelectedIndex(index)
    }

    /** 分系列の選択を [minute] へ移す。 */
    private fun selectMinute(sheet: TimeSelectionSheet, minute: Int) {
        sheet.minuteWheel.setSelectedIndex(minute)
    }

    /** 午前/午後系列の選択を [index]（0 = 午前 / 1 = 午後）へ移す。 */
    private fun selectPeriod(sheet: TimeSelectionSheet, index: Int) {
        requireNotNull(sheet.periodWheel) { "午前/午後系列が提示されていない" }.setSelectedIndex(index)
    }

    /** ホイール行に載っている系列（等幅で並ぶ子）。 */
    private fun seriesRow(sheet: TimeSelectionSheet): LinearLayout {
        val wheelRow = sheet.contentRoot.getChildAt(WHEEL_ROW_INDEX) as FrameLayout
        return wheelRow.getChildAt(wheelRow.childCount - 1) as LinearLayout
    }

    // MARK: - ホスト前提に依存しない提示

    @Test
    fun `ComponentActivity ホストの行タップで選択面が提示される`() {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()

        val sheet = tapRow(timeCell(), host = activity)

        assertNotNull("ComponentActivity ホストで選択面が提示されていない", sheet)
        assertTrue("選択面が表示状態になっていない", sheet!!.isShowing)
    }

    @Test
    fun `無効 Cell の行タップでは選択面を提示しない`() {
        assertNull(tapRow(timeCell(isEnabled = false)))
    }

    @Test
    fun `タイトルは pickerTitle を優先して解決する`() {
        val sheet = openSheet(timeCell(title = "起床時刻", pickerTitle = "時刻を選択"))
        assertEquals("時刻を選択", sheet.titleView.text?.toString())
    }

    @Test
    fun `pickerTitle が null のときタイトルは title を使う`() {
        assertEquals("起床時刻", openSheet(timeCell(title = "起床時刻")).titleView.text?.toString())
    }

    @Test
    fun `操作ラベルは OS の公開文字列リソースから解決される`() {
        val sheet = openSheet(timeCell())
        assertEquals(ctx.getString(android.R.string.cancel), sheet.cancelView.text?.toString())
        assertEquals(ctx.getString(android.R.string.ok), sheet.confirmView.text?.toString())
    }

    // MARK: - 時制の決定と候補系列

    @Test
    fun `既定は 24 時間制の2系列で初期選択は cell の時刻`() {
        val sheet = openSheet(timeCell(time = LocalTime.of(14, 30)))

        assertTrue(sheet.candidates.is24Hour)
        assertNull("24時間制で午前午後系列が提示されている", sheet.periodWheel)
        assertEquals(2, seriesRow(sheet).childCount)
        assertEquals(24, sheet.candidates.hourCount)
        assertEquals(60, sheet.candidates.minuteCount)
        assertEquals("0", sheet.hourWheel.bindRow(0).text?.toString())
        assertEquals("23", sheet.hourWheel.bindRow(23).text?.toString())
        assertEquals("14", sheet.hourWheel.selectedDisplayText())
        assertEquals("30", sheet.minuteWheel.selectedDisplayText())
        assertEquals(LocalTime.of(14, 30), sheet.selectedTime)
    }

    @Test
    fun `is24Hour false は 12 時間制の3系列で初期選択は cell の時刻`() {
        val sheet = openSheet(timeCell(time = LocalTime.of(14, 30), is24Hour = false))

        assertFalse(sheet.candidates.is24Hour)
        assertNotNull("12時間制で午前午後系列が提示されていない", sheet.periodWheel)
        assertEquals(3, seriesRow(sheet).childCount)
        assertEquals(12, sheet.candidates.hourCount)
        assertEquals("1", sheet.hourWheel.bindRow(0).text?.toString())
        assertEquals("12", sheet.hourWheel.bindRow(11).text?.toString())
        assertEquals("2", sheet.hourWheel.selectedDisplayText())
        assertEquals("30", sheet.minuteWheel.selectedDisplayText())
        assertEquals("午後", sheet.periodWheel!!.selectedDisplayText())
        assertEquals(LocalTime.of(14, 30), sheet.selectedTime)
    }

    @Test
    fun `format の a は時制に影響しない`() {
        val sheet = openSheet(timeCell(format = "h:mm a"))

        assertTrue(sheet.candidates.is24Hour)
        assertNull("24時間制で午前午後系列が提示されている", sheet.periodWheel)
        assertEquals(2, seriesRow(sheet).childCount)
    }

    @Test
    fun `12 時間制の指定は format に依らず 3 系列になる`() {
        val sheet = openSheet(timeCell(format = "HH:mm", is24Hour = false))

        assertFalse(sheet.candidates.is24Hour)
        assertNotNull("12時間制で午前午後系列が提示されていない", sheet.periodWheel)
        assertEquals(3, seriesRow(sheet).childCount)
    }

    @Test
    fun `12 時間制の系列順は端末 Locale の時刻表記に従う（午前午後が先）`() {
        val sheet = openSheet(timeCell(is24Hour = false))

        val period = requireNotNull(sheet.periodWheel)
        assertEquals(
            "日本語の時刻表記は午前/午後が先に来る",
            listOf(period, sheet.hourWheel, sheet.minuteWheel),
            sheet.orderedWheels,
        )
        assertEquals(
            "ホイール行の並びが提示順と一致しない",
            sheet.orderedWheels,
            seriesRow(sheet).let { row -> (0 until row.childCount).map { row.getChildAt(it) } },
        )
    }

    @Test
    @Config(qualifiers = "en")
    fun `12 時間制の系列順は端末 Locale の時刻表記に従う（午前午後が後）`() {
        val sheet = openSheet(timeCell(is24Hour = false))

        val period = requireNotNull(sheet.periodWheel)
        assertEquals(
            "英語の時刻表記は AM/PM が後に来る",
            listOf(sheet.hourWheel, sheet.minuteWheel, period),
            sheet.orderedWheels,
        )
        assertEquals(
            "ホイール行の並びが提示順と一致しない",
            sheet.orderedWheels,
            seriesRow(sheet).let { row -> (0 until row.childCount).map { row.getChildAt(it) } },
        )
    }

    @Test
    fun `24 時間制の系列順は時 分で Locale に依らない`() {
        val sheet = openSheet(timeCell())

        assertEquals(listOf(sheet.hourWheel, sheet.minuteWheel), sheet.orderedWheels)
    }

    @Test
    fun `表示済み Cell の is24Hour 変更が次の選択面に反映される`() {
        val cell = timeCell(time = LocalTime.of(14, 30)).copy(id = "time-cell")
        val updated = cell.copy(is24Hour = false)
        // 更新が変化として検知される（同一 ID でも等価にならない）。
        assertNotEquals(cell, updated)

        val vh = TimePickerCellViewHolder.create(FrameLayout(ctx))
        vh.bind(cell, Theme())
        vh.views.root.performClick()
        val before = ShadowDialog.getLatestDialog() as TimeSelectionSheet
        assertTrue("更新前が 24 時間制になっていない", before.candidates.is24Hour)
        before.dismiss()

        vh.bind(updated, Theme())
        vh.views.root.performClick()
        val after = ShadowDialog.getLatestDialog() as TimeSelectionSheet

        assertFalse("更新後の選択面が 12 時間制になっていない", after.candidates.is24Hour)
        assertNotNull("12時間制で午前午後系列が提示されていない", after.periodWheel)
    }

    @Test
    fun `12 時間制の深夜は 12 午前として提示され確定で 0 時になる`() {
        val notified = mutableListOf<LocalTime>()
        val sheet = openSheet(
            timeCell(time = LocalTime.of(0, 30), is24Hour = false, onValueChanged = notified::add),
        )

        assertEquals("12", sheet.hourWheel.selectedDisplayText())
        assertEquals("30", sheet.minuteWheel.selectedDisplayText())
        assertEquals("午前", sheet.periodWheel!!.selectedDisplayText())

        sheet.confirmView.performClick()

        assertEquals(listOf(LocalTime.of(0, 30)), notified)
    }

    @Test
    fun `12 時間制の正午は 12 午後として提示され確定で 12 時になる`() {
        val notified = mutableListOf<LocalTime>()
        val sheet = openSheet(
            timeCell(time = LocalTime.of(12, 30), is24Hour = false, onValueChanged = notified::add),
        )

        assertEquals("12", sheet.hourWheel.selectedDisplayText())
        assertEquals("30", sheet.minuteWheel.selectedDisplayText())
        assertEquals("午後", sheet.periodWheel!!.selectedDisplayText())

        sheet.confirmView.performClick()

        assertEquals(listOf(LocalTime.of(12, 30)), notified)
    }

    @Test
    fun `12 時間制は午前午後の切替で 12 時間ずれた時刻になる`() {
        val notified = mutableListOf<LocalTime>()
        val sheet = openSheet(
            timeCell(time = LocalTime.of(9, 5), is24Hour = false, onValueChanged = notified::add),
        )

        assertEquals(LocalTime.of(9, 5), sheet.selectedTime)
        selectPeriod(sheet, TimeCandidates.INDEX_PM)
        assertEquals(LocalTime.of(21, 5), sheet.selectedTime)

        sheet.confirmView.performClick()

        assertEquals(listOf(LocalTime.of(21, 5)), notified)
    }

    @Test
    fun `分候補は 2 桁ゼロ詰めで時候補はゼロ詰めしない`() {
        val sheet = openSheet(timeCell(time = LocalTime.of(9, 5)))

        assertEquals("9", sheet.hourWheel.selectedDisplayText())
        assertEquals("05", sheet.minuteWheel.selectedDisplayText())
        assertEquals("00", sheet.minuteWheel.bindRow(0).text?.toString())
        assertEquals("59", sheet.minuteWheel.bindRow(59).text?.toString())
    }

    @Test
    fun `秒以下を持つ時刻でも分単位の選択として開ける`() {
        val sheet = openSheet(timeCell(time = LocalTime.of(7, 45, 30)))

        assertEquals(LocalTime.of(7, 45), sheet.selectedTime)
    }

    @Test
    @Config(qualifiers = "en")
    fun `午前午後のラベルは端末 Locale の表記から導出される`() {
        val sheet = openSheet(timeCell(time = LocalTime.of(14, 30), is24Hour = false))

        val period = requireNotNull(sheet.periodWheel)
        assertEquals("AM", period.bindRow(TimeCandidates.INDEX_AM).text?.toString())
        assertEquals("PM", period.bindRow(TimeCandidates.INDEX_PM).text?.toString())
    }

    // MARK: - 確定のみ反映

    @Test
    fun `確定で選択中の時刻を1回だけ通知して閉じる`() {
        val notified = mutableListOf<LocalTime>()
        val sheet = openSheet(timeCell(onValueChanged = notified::add))

        selectHour(sheet, 9)
        selectMinute(sheet, 5)
        sheet.confirmView.performClick()

        assertEquals(listOf(LocalTime.of(9, 5)), notified)
        assertFalse("確定後も選択面が残っている", sheet.isShowing)
    }

    @Test
    fun `取消では通知しない`() {
        val notified = mutableListOf<LocalTime>()
        val sheet = openSheet(timeCell(onValueChanged = notified::add))

        selectHour(sheet, 9)
        sheet.cancelView.performClick()

        assertTrue("取消で通知されている", notified.isEmpty())
        assertFalse("取消後も選択面が残っている", sheet.isShowing)
    }

    @Test
    fun `外側タップ相当の cancel では通知しない`() {
        val notified = mutableListOf<LocalTime>()
        val sheet = openSheet(timeCell(onValueChanged = notified::add))

        selectHour(sheet, 9)
        sheet.cancel()

        assertTrue("外側タップで通知されている", notified.isEmpty())
    }

    @Test
    fun `下スワイプ相当の dismiss では通知しない`() {
        val notified = mutableListOf<LocalTime>()
        val sheet = openSheet(timeCell(onValueChanged = notified::add))

        selectHour(sheet, 9)
        sheet.dismiss()

        assertTrue("dismiss で通知されている", notified.isEmpty())
    }

    // MARK: - 構成変更

    @Test
    fun `構成変更で Activity が再生成されると選択面は閉じられ再提示も通知もしない`() {
        val notified = mutableListOf<LocalTime>()
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val sheet = tapRowInHierarchy(ctrl.get(), timeCell(onValueChanged = notified::add))
        assertTrue("前提: 選択面が表示状態になっていない", sheet.isShowing)

        ctrl.recreate()

        assertFalse("再生成前の選択面が開いたまま残っている", sheet.isShowing)
        assertNull("再生成後に選択面が提示されている", shownSheet())
        assertTrue("再生成で通知されている", notified.isEmpty())
    }

    /**
     * ホストの View 階層へ実際に行を取り付けたうえでタップし、表示された選択面を返す。
     *
     * 選択面はタップした行からホストの lifecycle をたどって破棄に追随するため、行が階層に
     * 載っていない状態ではその配線を通れない。
     */
    private fun tapRowInHierarchy(activity: HostActivity, cell: TimePickerCell): TimeSelectionSheet {
        val vh = TimePickerCellViewHolder.create(activity.container)
        activity.container.addView(vh.views.root)
        vh.bind(cell, Theme())
        vh.views.root.performClick()
        return requireNotNull(ShadowDialog.getLatestDialog() as? TimeSelectionSheet) {
            "行タップで選択面が提示されていない"
        }
    }

    /** 現在表示中の [TimeSelectionSheet]（無ければ `null`）。 */
    private fun shownSheet(): TimeSelectionSheet? =
        ShadowDialog.getShownDialogs()
            .filterIsInstance<TimeSelectionSheet>()
            .lastOrNull { it.isShowing }

    // MARK: - styling の解決

    @Test
    fun `強調色は Cell 指定を最優先に段階解決される`() {
        val cellAccent = Color(0xFFAA0000)
        val styleAccent = Color(0xFF00AA00)
        val themeAccent = Color(0xFF0000AA)

        val fromCell = openSheet(
            timeCell(accentColor = cellAccent, style = CellStyle(accentColor = styleAccent)),
            Theme(cellAccentColor = themeAccent),
        )
        assertEquals(cellAccent.toArgb(), fromCell.cancelView.currentTextColor)

        val fromStyle = openSheet(
            timeCell(style = CellStyle(accentColor = styleAccent)),
            Theme(cellAccentColor = themeAccent),
        )
        assertEquals(styleAccent.toArgb(), fromStyle.cancelView.currentTextColor)

        val fromTheme = openSheet(timeCell(), Theme(cellAccentColor = themeAccent))
        assertEquals(themeAccent.toArgb(), fromTheme.cancelView.currentTextColor)
    }

    private companion object {
        /**
         * シート内容におけるホイール行の位置
         * （ドラッグハンドル → ヘッダー → 区切り線 → **ホイール行** → 下余白）。
         */
        const val WHEEL_ROW_INDEX: Int = 3
    }
}
