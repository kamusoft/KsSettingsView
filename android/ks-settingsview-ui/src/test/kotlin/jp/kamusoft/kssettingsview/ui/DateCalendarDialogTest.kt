package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import java.time.LocalDate
import java.util.TimeZone
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * [DatePickerCell] の [DatePickerUIStyle.Material] が使う選択面（[DateCalendarDialog]）の
 * 提示・範囲制限・日付の往復・今日ジャンプ・確定 / 非確定 dismiss と、
 * [DatePickerCellViewHolder.resolveDialogColors] の色束解決を検証する（android/ADR-0019）。
 *
 * 選択面は [DatePickerCellViewHolder] の行タップ経路から開き、実際の配線ごと検証する。
 * ホストの XML テーマは意図的に Material3 派生にしない（同梱テーマの常時ラップにより、
 * ホストのテーマ前提なしで成立することを併せて示す。android/ADR-0020）。
 *
 * 選択日・表示月・表示モードの操作は、カレンダー上の操作と同じ状態（[DateCalendarDialog.state]）を
 * 通す。Compose の描画そのものへの操作（日付セルのタップ・モード切替アイコン・操作行のボタン）は
 * 実機での視覚確認が担う。
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ja")
class DateCalendarDialogTest {

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

    /** 既定のタイムゾーン（テストで差し替えた場合に戻す）。 */
    private val defaultTimeZone: TimeZone = TimeZone.getDefault()

    @After
    fun tearDown() {
        TimeZone.setDefault(defaultTimeZone)
        controller?.close()
        controller = null
    }

    /** ホストのテーマを被せない素の Context（同梱テーマの常時ラップで成立する）。 */
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    /** 「今日」の既定の固定値（実行日に依存しない検証のため）。 */
    private val fixedToday: LocalDate = LocalDate.of(2026, 8, 27)

    private fun dateCell(
        date: LocalDate = LocalDate.of(2026, 8, 2),
        title: String = "予定日",
        pickerTitle: String? = null,
        minDate: LocalDate? = null,
        maxDate: LocalDate? = null,
        todayText: String? = null,
        isEnabled: Boolean = true,
        onValueChanged: ((LocalDate) -> Unit)? = null,
    ): DatePickerCell = DatePickerCell(
        title = title,
        pickerTitle = pickerTitle,
        date = date,
        minDate = minDate,
        maxDate = maxDate,
        todayText = todayText,
        uiStyle = DatePickerUIStyle.Material,
        isEnabled = isEnabled,
        onValueChanged = onValueChanged,
    )

    /** Cell の行タップで選択面を開き、表示された [DateCalendarDialog] を返す。 */
    private fun openDialog(
        cell: DatePickerCell,
        theme: Theme = Theme(),
        host: Context = ctx,
        today: LocalDate = fixedToday,
    ): DateCalendarDialog =
        requireNotNull(tapRow(cell, theme, host, today)) { "選択面が提示されていない" }

    /** Cell の行をタップし、表示された選択面を返す（提示されなければ `null`）。 */
    private fun tapRow(
        cell: DatePickerCell,
        theme: Theme = Theme(),
        host: Context = ctx,
        today: LocalDate = fixedToday,
    ): DateCalendarDialog? {
        val vh = DatePickerCellViewHolder.create(FrameLayout(host))
        vh.todayProvider = { today }
        vh.bind(cell, theme)
        vh.views.root.performClick()
        return ShadowDialog.getLatestDialog() as? DateCalendarDialog
    }

    /** 選択面が保持している選択日。 */
    private fun selectedDate(dialog: DateCalendarDialog): LocalDate? =
        dialog.state.selectedDateMillis?.toLocalDateUtc()

    /** 選択面が表示している月（初日として持つ）。 */
    private fun displayedMonth(dialog: DateCalendarDialog): LocalDate =
        dialog.state.displayedMonthMillis.toLocalDateUtc()

    /** カレンダー上で [date] を選ぶ操作と同じ状態変更。 */
    private fun select(dialog: DateCalendarDialog, date: LocalDate) {
        dialog.state.selectedDateMillis = date.toEpochMilliUtc()
    }

    // MARK: - ホスト前提に依存しない提示

    @Test
    fun `ComponentActivity ホストの行タップで選択面が提示される`() {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()

        val dialog = tapRow(dateCell(), host = activity)

        assertNotNull("ComponentActivity ホストで選択面が提示されていない", dialog)
        assertTrue("選択面が表示状態になっていない", dialog!!.isShowing)
    }

    @Test
    fun `無効 Cell の行タップでは選択面を提示しない`() {
        assertNull(tapRow(dateCell(isEnabled = false)))
    }

    @Test
    fun `タイトルは pickerTitle を優先して解決する`() {
        val dialog = openDialog(dateCell(title = "予定日", pickerTitle = "日付を選択"))
        assertEquals("日付を選択", dialog.dialogTitle)
    }

    @Test
    fun `pickerTitle が null のときタイトルは title を使う`() {
        assertEquals("予定日", openDialog(dateCell(title = "予定日")).dialogTitle)
    }

    @Test
    fun `todayText 未指定では今日操作を提示しない`() {
        assertNull(openDialog(dateCell()).todayText)
    }

    @Test
    fun `todayText が空文字なら今日操作を提示しない`() {
        assertNull(openDialog(dateCell(todayText = "")).todayText)
    }

    @Test
    fun `todayText 指定時は今日操作を提示する`() {
        assertEquals("今日", openDialog(dateCell(todayText = "今日")).todayText)
    }

    @Test
    fun `開いた時点の選択日と表示月は cell の日付になる`() {
        val dialog = openDialog(dateCell(date = LocalDate.of(2026, 8, 2)))

        assertEquals(LocalDate.of(2026, 8, 2), selectedDate(dialog))
        assertEquals(LocalDate.of(2026, 8, 1), displayedMonth(dialog))
    }

    @Test
    fun `カレンダー表示で開き入力モードへ切り替えられる`() {
        val dialog = openDialog(dateCell())

        assertEquals(DisplayMode.Picker, dialog.state.displayMode)

        dialog.state.displayMode = DisplayMode.Input
        assertEquals(DisplayMode.Input, dialog.state.displayMode)

        dialog.state.displayMode = DisplayMode.Picker
        assertEquals(DisplayMode.Picker, dialog.state.displayMode)
    }

    // MARK: - 確定のみ反映

    @Test
    fun `確定で選択日を1回だけ通知して閉じる`() {
        val notified = mutableListOf<LocalDate>()
        val dialog = openDialog(dateCell(onValueChanged = { notified.add(it) }))

        select(dialog, LocalDate.of(2026, 8, 9))
        dialog.confirmSelection()

        assertEquals(listOf(LocalDate.of(2026, 8, 9)), notified)
        assertFalse("確定後も選択面が開いている", dialog.isShowing)
    }

    @Test
    fun `取消では通知しない`() {
        val notified = mutableListOf<LocalDate>()
        val dialog = openDialog(dateCell(onValueChanged = { notified.add(it) }))

        select(dialog, LocalDate.of(2026, 8, 9))
        dialog.cancel()

        assertTrue("非確定の閉じ方で通知された: $notified", notified.isEmpty())
        assertFalse(dialog.isShowing)
    }

    @Test
    fun `外側タップや Back での dismiss では通知しない`() {
        val notified = mutableListOf<LocalDate>()
        val dialog = openDialog(dateCell(onValueChanged = { notified.add(it) }))

        select(dialog, LocalDate.of(2026, 8, 9))
        dialog.dismiss()

        assertTrue("非確定の閉じ方で通知された: $notified", notified.isEmpty())
    }

    @Test
    fun `選択日が定まらない状態では確定しても通知しない`() {
        val notified = mutableListOf<LocalDate>()
        val dialog = openDialog(dateCell(onValueChanged = { notified.add(it) }))

        dialog.state.selectedDateMillis = null
        dialog.confirmSelection()

        assertTrue(notified.isEmpty())
        assertTrue("確定できない状態で選択面が閉じている", dialog.isShowing)
    }

    @Test
    fun `ホストが破棄されると選択面を閉じる`() {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()
        val vh = DatePickerCellViewHolder.create(activity.container)
        activity.container.addView(vh.views.root)
        vh.bind(dateCell(), Theme())
        vh.views.root.performClick()
        val dialog = ShadowDialog.getLatestDialog() as DateCalendarDialog
        assertTrue(dialog.isShowing)

        ctrl.pause().stop().destroy()

        assertFalse("ホスト破棄後も選択面が開いたまま残っている", dialog.isShowing)
    }

    // MARK: - 範囲制限

    @Test
    fun `範囲外の日付は選択できず範囲内の日付は選択できる`() {
        val dialog = openDialog(
            dateCell(
                date = LocalDate.of(2026, 8, 10),
                minDate = LocalDate.of(2026, 8, 5),
                maxDate = LocalDate.of(2026, 8, 20),
            ),
        )
        val selectable = dialog.state.selectableDates

        assertFalse(selectable.isSelectableDate(LocalDate.of(2026, 8, 4).toEpochMilliUtc()))
        assertFalse(selectable.isSelectableDate(LocalDate.of(2026, 8, 21).toEpochMilliUtc()))
        assertTrue("境界日が選べない", selectable.isSelectableDate(LocalDate.of(2026, 8, 5).toEpochMilliUtc()))
        assertTrue("境界日が選べない", selectable.isSelectableDate(LocalDate.of(2026, 8, 20).toEpochMilliUtc()))
        assertTrue(selectable.isSelectableDate(LocalDate.of(2026, 8, 12).toEpochMilliUtc()))
    }

    @Test
    fun `年候補は範囲の年に限られる`() {
        val dialog = openDialog(
            dateCell(
                date = LocalDate.of(2026, 8, 10),
                minDate = LocalDate.of(2025, 8, 5),
                maxDate = LocalDate.of(2027, 8, 20),
            ),
        )

        assertEquals(2025..2027, dialog.state.yearRange)
        assertFalse(dialog.state.selectableDates.isSelectableYear(2024))
        assertTrue(dialog.state.selectableDates.isSelectableYear(2025))
        assertTrue(dialog.state.selectableDates.isSelectableYear(2027))
        assertFalse(dialog.state.selectableDates.isSelectableYear(2028))
    }

    @Test
    fun `年候補の既定は 1900 年から 2100 年まで`() {
        assertEquals(1900..2100, openDialog(dateCell()).state.yearRange)
    }

    @Test
    fun `minDate だけの指定では上限側に既定が入る`() {
        val dialog = openDialog(dateCell(date = LocalDate.of(2026, 8, 2), minDate = LocalDate.of(2026, 1, 1)))
        assertEquals(2026..2100, dialog.state.yearRange)
    }

    @Test
    fun `範囲より前の初期値は minDate へ丸めて提示する`() {
        val dialog = openDialog(
            dateCell(date = LocalDate.of(2026, 1, 1), minDate = LocalDate.of(2026, 8, 5)),
        )

        assertEquals(LocalDate.of(2026, 8, 5), selectedDate(dialog))
        assertEquals(LocalDate.of(2026, 8, 1), displayedMonth(dialog))
    }

    @Test
    fun `範囲より後の初期値は maxDate へ丸めて提示する`() {
        val dialog = openDialog(
            dateCell(date = LocalDate.of(2027, 1, 1), maxDate = LocalDate.of(2026, 8, 20)),
        )

        assertEquals(LocalDate.of(2026, 8, 20), selectedDate(dialog))
    }

    @Test
    fun `minDate が maxDate より後なら選択面を提示しない`() {
        val cell = dateCell(minDate = LocalDate.of(2026, 9, 1), maxDate = LocalDate.of(2026, 8, 1))
        assertNull(tapRow(cell))
    }

    // MARK: - 面の高さの上限

    @Test
    fun `面の高さの上限は画面の高さから影の余白を差し引いた値になる`() {
        assertEquals(872f, dateCalendarSurfaceMaxHeightDp(screenHeightDp = 896, shadowMarginDp = 12f), 0f)
    }

    @Test
    fun `画面が低いほど上限も下がり操作行の余地が残る`() {
        val portrait = dateCalendarSurfaceMaxHeightDp(screenHeightDp = 896, shadowMarginDp = 12f)
        val landscape = dateCalendarSurfaceMaxHeightDp(screenHeightDp = 393, shadowMarginDp = 12f)

        assertTrue(landscape < portrait)
        assertTrue(landscape <= 393f)
    }

    @Test
    fun `画面が余白より低くても上限は負にならない`() {
        assertEquals(0f, dateCalendarSurfaceMaxHeightDp(screenHeightDp = 10, shadowMarginDp = 12f), 0f)
    }

    // MARK: - タイムゾーンに依存しない日付の往復

    @Test
    fun `UTC から東へ離れたタイムゾーンでも確定日は選択日と一致する`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
        assertConfirmedDateMatchesSelection()
    }

    @Test
    fun `UTC から西へ離れたタイムゾーンでも確定日は選択日と一致する`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Midway"))
        assertConfirmedDateMatchesSelection()
    }

    private fun assertConfirmedDateMatchesSelection() {
        val notified = mutableListOf<LocalDate>()
        val dialog = openDialog(dateCell(onValueChanged = { notified.add(it) }))
        val target = LocalDate.of(2026, 8, 15)

        select(dialog, target)
        assertEquals("提示中の選択日がずれている", target, selectedDate(dialog))
        dialog.confirmSelection()

        assertEquals(listOf(target), notified)
    }

    // MARK: - 今日ジャンプ

    @Test
    fun `今日ジャンプは選択日と表示月を今日へ移し通知しない`() {
        val notified = mutableListOf<LocalDate>()
        val dialog = openDialog(
            cell = dateCell(date = LocalDate.of(2020, 3, 5), todayText = "今日", onValueChanged = { notified.add(it) }),
            today = fixedToday,
        )

        dialog.jumpToToday()

        assertEquals(fixedToday, selectedDate(dialog))
        assertEquals(fixedToday.withDayOfMonth(1), displayedMonth(dialog))
        assertTrue("今日ジャンプで通知された: $notified", notified.isEmpty())
        assertTrue("今日ジャンプで選択面が閉じている", dialog.isShowing)
    }

    @Test
    fun `今日が範囲外なら今日ジャンプで選択状態は変化しない`() {
        val dialog = openDialog(
            cell = dateCell(
                date = LocalDate.of(2026, 8, 2),
                maxDate = LocalDate.of(2026, 8, 10),
                todayText = "今日",
            ),
            today = LocalDate.of(2026, 8, 27),
        )
        val before = selectedDate(dialog)
        val beforeMonth = displayedMonth(dialog)

        dialog.jumpToToday()

        assertEquals(before, selectedDate(dialog))
        assertEquals(beforeMonth, displayedMonth(dialog))
    }

    @Test
    fun `境界日の今日は範囲内として今日ジャンプが成立する`() {
        val dialog = openDialog(
            cell = dateCell(
                date = LocalDate.of(2026, 8, 2),
                maxDate = fixedToday,
                todayText = "今日",
            ),
            today = fixedToday,
        )

        dialog.jumpToToday()

        assertEquals(fixedToday, selectedDate(dialog))
    }

    @Test
    fun `今日ジャンプの連続実行は冪等`() {
        val dialog = openDialog(cell = dateCell(todayText = "今日"), today = fixedToday)

        dialog.jumpToToday()
        val afterFirst = selectedDate(dialog) to displayedMonth(dialog)
        dialog.jumpToToday()
        dialog.jumpToToday()

        assertEquals(afterFirst, selectedDate(dialog) to displayedMonth(dialog))
    }

    @Test
    fun `入力モード表示中の今日ジャンプはカレンダー表示へ戻して成立する`() {
        val dialog = openDialog(cell = dateCell(todayText = "今日"), today = fixedToday)
        dialog.state.displayMode = DisplayMode.Input

        dialog.jumpToToday()

        assertEquals(DisplayMode.Picker, dialog.state.displayMode)
        assertEquals(fixedToday, selectedDate(dialog))
    }

    @Test
    fun `今日ジャンプ後の確定は今日を通知する`() {
        val notified = mutableListOf<LocalDate>()
        val dialog = openDialog(
            cell = dateCell(date = LocalDate.of(2020, 3, 5), todayText = "今日", onValueChanged = { notified.add(it) }),
            today = fixedToday,
        )

        dialog.jumpToToday()
        dialog.confirmSelection()

        assertEquals(listOf(fixedToday), notified)
    }

    // MARK: - Spinner 経路の不変

    @Test
    fun `Spinner 指定ではカレンダーダイアログではなくホイールの選択面を出す`() {
        val vh = DatePickerCellViewHolder.create(FrameLayout(ctx))
        vh.bind(dateCell().copy(uiStyle = DatePickerUIStyle.Spinner), Theme())
        vh.views.root.performClick()

        assertTrue(
            "Spinner 経路の選択面が変わっている",
            ShadowDialog.getLatestDialog() is DateSelectionSheet,
        )
    }

    // MARK: - 色束の解決

    private fun viewHolder(): DatePickerCellViewHolder =
        DatePickerCellViewHolder.create(FrameLayout(ctx))

    @Test
    fun `アクセント色は Cell 固有値を優先する`() {
        val theme = Theme(cellAccentColor = ComposeColor(0xFF00FF00))
        val cellStyle = CellStyle(accentColor = ComposeColor(0xFF0000FF))
        val cell = DatePickerCell(
            title = "予約日",
            style = cellStyle,
            accentColor = ComposeColor(0xFFFF0000),
        )
        val effective = EffectiveStyle.from(ctx, theme, cellStyle)

        val resolved = viewHolder().resolveDialogColors(cell, theme, effective)

        assertEquals(ComposeColor(0xFFFF0000).toArgb(), resolved.accent)
    }

    @Test
    fun `アクセント色は Cell 未指定なら CellStyle へフォールバックする`() {
        val theme = Theme(cellAccentColor = ComposeColor(0xFF00FF00))
        val cellStyle = CellStyle(accentColor = ComposeColor(0xFF0000FF))
        val cell = DatePickerCell(title = "予約日", style = cellStyle, accentColor = null)
        val effective = EffectiveStyle.from(ctx, theme, cellStyle)

        val resolved = viewHolder().resolveDialogColors(cell, theme, effective)

        assertEquals(ComposeColor(0xFF0000FF).toArgb(), resolved.accent)
    }

    @Test
    fun `アクセント色は CellStyle 未指定なら Theme へフォールバックする`() {
        val theme = Theme(cellAccentColor = ComposeColor(0xFF00FF00))
        val cellStyle = CellStyle()
        val cell = DatePickerCell(title = "予約日", accentColor = null)
        val effective = EffectiveStyle.from(ctx, theme, cellStyle)

        val resolved = viewHolder().resolveDialogColors(cell, theme, effective)

        assertEquals(ComposeColor(0xFF00FF00).toArgb(), resolved.accent)
    }

    @Test
    fun `背景は Theme backgroundColor 文字は実効タイトル色を採る`() {
        val theme = Theme(
            backgroundColor = ComposeColor(0xFFF2EFE6),
            cellBackgroundColor = ComposeColor(0xFFFFFFFF),
            cellTitleColor = ComposeColor(0xFF555555),
        )
        val cellStyle = CellStyle()
        val cell = DatePickerCell(title = "予約日")
        val effective = EffectiveStyle.from(ctx, theme, cellStyle)

        val resolved = viewHolder().resolveDialogColors(cell, theme, effective)

        // ダイアログ背景は Cell 背景ではなく SettingsView 全体の背景色を使う。
        assertEquals(ComposeColor(0xFFF2EFE6).toArgb(), resolved.background)
        assertEquals(ComposeColor(0xFF555555).toArgb(), resolved.text)
    }
}
