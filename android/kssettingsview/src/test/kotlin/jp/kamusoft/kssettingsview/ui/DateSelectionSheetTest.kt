package jp.kamusoft.kssettingsview.ui

import android.content.pm.ApplicationInfo
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLog
import java.time.Duration
import java.time.LocalDate

/**
 * [DatePickerCell] の Spinner モードの選択面（[DateSelectionSheet]）の提示・候補範囲・
 * 日候補の追随・「今日」ジャンプ・確定 / 非確定 dismiss・強調色・Locale 追随・
 * アクセシビリティを検証する（android/ADR-0009）。
 *
 * 選択面は [DatePickerCellViewHolder] の行タップ経路から開き、実際の配線ごと検証する。
 * 「今日」は ViewHolder の [DatePickerCellViewHolder.todayProvider] へ固定日付を注入し、
 * 実行時刻に依存しない判定にする。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ja")
class DateSelectionSheetTest {

    private val ctx: android.content.Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            MaterialR.style.Theme_Material3_Light_NoActionBar,
        )

    private val parent get() = FrameLayout(ctx)

    /** Cell の行タップで選択面を開き、表示された [DateSelectionSheet] を返す。 */
    private fun openSheet(
        cell: DatePickerCell,
        theme: Theme = Theme(),
        today: LocalDate = FIXED_TODAY,
    ): DateSelectionSheet = requireNotNull(tapRow(cell, theme, today)) { "選択面が提示されていない" }

    /** Cell の行をタップし、表示された選択面を返す（提示されなければ `null`）。 */
    private fun tapRow(
        cell: DatePickerCell,
        theme: Theme = Theme(),
        today: LocalDate = FIXED_TODAY,
    ): DateSelectionSheet? {
        val vh = DatePickerCellViewHolder.create(parent)
        vh.todayProvider = { today }
        vh.bind(cell, theme)
        vh.views.root.performClick()
        // Material モードは別の選択面（カレンダーダイアログ）を開くため、型で選り分ける。
        return ShadowDialog.getLatestDialog() as? DateSelectionSheet
    }

    private fun spinnerCell(
        date: LocalDate = LocalDate.of(2026, 8, 2),
        minDate: LocalDate? = null,
        maxDate: LocalDate? = null,
        todayText: String? = null,
        title: String = "誕生日",
        pickerTitle: String? = null,
        accentColor: Color? = null,
        androidButtonColor: Color? = null,
        style: CellStyle = CellStyle(),
        isEnabled: Boolean = true,
        onValueChanged: ((LocalDate) -> Unit)? = null,
    ): DatePickerCell = DatePickerCell(
        title = title,
        pickerTitle = pickerTitle,
        date = date,
        minDate = minDate,
        maxDate = maxDate,
        uiStyle = DatePickerUIStyle.Spinner,
        todayText = todayText,
        androidButtonColor = androidButtonColor,
        accentColor = accentColor,
        style = style,
        isEnabled = isEnabled,
        onValueChanged = onValueChanged,
    )

    /**
     * ダイアログの実 View 階層（CoordinatorLayout 配下）を画面サイズで measure / layout し、
     * ボトムシートのコンテナを返す。
     */
    private fun layoutDialog(sheet: DateSelectionSheet): FrameLayout {
        val container = sheet.findViewById<FrameLayout>(MaterialR.id.design_bottom_sheet)!!
        val coordinator = container.parent as ViewGroup
        val metrics = ctx.resources.displayMetrics
        coordinator.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        coordinator.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        return container
    }

    private fun warnLogs(): List<ShadowLog.LogItem> =
        ShadowLog.getLogsForTag("DatePickerCell").filter { it.type == Log.WARN }

    private fun itemCountOf(wheel: KsWheelView): Int = wheel.listView.adapter?.itemCount ?: 0

    /** ホイールの候補表示を先頭から [count] 件取り出す。 */
    private fun candidateTexts(wheel: KsWheelView, count: Int): List<String> =
        (0 until count).map { wheel.bindRow(it).text.toString() }

    /** 年系列の選択を [year] へ移す（スナップ静止と同じ通知経路を通る）。 */
    private fun selectYear(sheet: DateSelectionSheet, year: Int) {
        sheet.yearWheel.setSelectedIndex(sheet.candidates.yearIndexOf(year))
    }

    /** 月系列の選択を [month] へ移す。 */
    private fun selectMonth(sheet: DateSelectionSheet, month: Int) {
        sheet.monthWheel.setSelectedIndex(
            sheet.candidates.monthIndexOf(sheet.selectedDate.year, month),
        )
    }

    /** 日系列の選択を [day] へ移す。 */
    private fun selectDay(sheet: DateSelectionSheet, day: Int) {
        sheet.dayWheel.setSelectedIndex(
            sheet.candidates.dayIndexOf(sheet.selectedDate.year, sheet.selectedDate.monthValue, day),
        )
    }

    private fun nodeInfoOf(view: View): AccessibilityNodeInfo {
        val info = AccessibilityNodeInfo.obtain()
        view.onInitializeAccessibilityNodeInfo(info)
        return info
    }

    // MARK: - 選択面の提示

    @Test
    fun `タイトルは pickerTitle を優先して解決する`() {
        val sheet = openSheet(spinnerCell(title = "誕生日", pickerTitle = "日付を選択"))
        assertEquals("日付を選択", sheet.titleView.text?.toString())
    }

    @Test
    fun `pickerTitle が null のときタイトルは title を使う`() {
        assertEquals("誕生日", openSheet(spinnerCell(title = "誕生日")).titleView.text?.toString())
    }

    @Test
    fun `操作ラベルは OS の公開文字列リソースから解決される`() {
        val sheet = openSheet(spinnerCell())
        assertEquals(ctx.getString(android.R.string.cancel), sheet.cancelView.text?.toString())
        assertEquals(ctx.getString(android.R.string.ok), sheet.confirmView.text?.toString())
    }

    @Test
    fun `年 月 日の3系列が提示されそれぞれ独立に選択できる`() {
        val sheet = openSheet(spinnerCell(date = LocalDate.of(2026, 8, 2)))

        // 3系列がそれぞれ独立した候補と選択を持つ。
        assertEquals("2026年", sheet.yearWheel.selectedDisplayText())
        assertEquals("8月", sheet.monthWheel.selectedDisplayText())
        assertEquals("2日", sheet.dayWheel.selectedDisplayText())

        selectDay(sheet, 20)
        assertEquals(LocalDate.of(2026, 8, 20), sheet.selectedDate)
        assertEquals("2026年", sheet.yearWheel.selectedDisplayText())
        assertEquals("8月", sheet.monthWheel.selectedDisplayText())
    }

    @Test
    fun `無効 Cell の行タップでは選択面を提示しない`() {
        assertNull(tapRow(spinnerCell(isEnabled = false)))
    }

    // MARK: - 候補の範囲と初期選択

    @Test
    fun `minDate と maxDate 未指定なら年候補は 1900 から 2100 までになる`() {
        val sheet = openSheet(spinnerCell(minDate = null, maxDate = null))
        assertEquals(201, itemCountOf(sheet.yearWheel))
        assertEquals("1900年", sheet.yearWheel.bindRow(0).text?.toString())
        assertEquals("2100年", sheet.yearWheel.bindRow(200).text?.toString())
    }

    @Test
    fun `minDate と maxDate で年候補が制限される`() {
        val sheet = openSheet(
            spinnerCell(
                date = LocalDate.of(2026, 8, 2),
                minDate = LocalDate.of(2020, 4, 1),
                maxDate = LocalDate.of(2030, 9, 30),
            ),
        )
        assertEquals(11, itemCountOf(sheet.yearWheel))
        assertEquals("2020年", sheet.yearWheel.bindRow(0).text?.toString())
        assertEquals("2030年", sheet.yearWheel.bindRow(10).text?.toString())
    }

    @Test
    fun `境界年では月候補が制限される`() {
        val sheet = openSheet(
            spinnerCell(date = LocalDate.of(2026, 8, 2), minDate = LocalDate.of(2020, 4, 1)),
        )
        selectYear(sheet, 2020)

        assertEquals(9, itemCountOf(sheet.monthWheel))
        assertEquals(
            listOf("4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"),
            candidateTexts(sheet.monthWheel, 9),
        )
    }

    @Test
    fun `境界年の上限側でも月候補が制限される`() {
        val sheet = openSheet(
            spinnerCell(date = LocalDate.of(2026, 8, 2), maxDate = LocalDate.of(2030, 3, 20)),
        )
        selectYear(sheet, 2030)

        assertEquals(3, itemCountOf(sheet.monthWheel))
        assertEquals(listOf("1月", "2月", "3月"), candidateTexts(sheet.monthWheel, 3))
    }

    @Test
    fun `初期選択は date になる`() {
        val sheet = openSheet(spinnerCell(date = LocalDate.of(2026, 8, 2)))
        assertEquals(LocalDate.of(2026, 8, 2), sheet.selectedDate)
        assertEquals("2026年", sheet.yearWheel.selectedDisplayText())
        assertEquals("8月", sheet.monthWheel.selectedDisplayText())
        assertEquals("2日", sheet.dayWheel.selectedDisplayText())
    }

    @Test
    fun `範囲外の date は最も近い範囲端へ丸めて提示する`() {
        val below = openSheet(
            spinnerCell(date = LocalDate.of(1970, 1, 1), minDate = LocalDate.of(2020, 4, 1)),
        )
        assertEquals(LocalDate.of(2020, 4, 1), below.selectedDate)
        assertEquals("2020年", below.yearWheel.selectedDisplayText())
        assertEquals("4月", below.monthWheel.selectedDisplayText())
        assertEquals("1日", below.dayWheel.selectedDisplayText())

        val above = openSheet(
            spinnerCell(date = LocalDate.of(2099, 12, 31), maxDate = LocalDate.of(2030, 9, 10)),
        )
        assertEquals(LocalDate.of(2030, 9, 10), above.selectedDate)
    }

    @Test
    fun `minDate が maxDate より後なら選択面を提示せず警告ログを残す`() {
        assertNull(
            tapRow(
                spinnerCell(
                    minDate = LocalDate.of(2030, 1, 1),
                    maxDate = LocalDate.of(2020, 12, 31),
                ),
            ),
        )
        assertTrue("警告ログが記録されていない", warnLogs().any { it.msg.contains("invalid range") })
    }

    @Test
    fun `既定適用後に範囲が空になる構成では選択面を提示せず警告ログを残す`() {
        assertNull(tapRow(spinnerCell(minDate = LocalDate.of(2200, 1, 1), maxDate = null)))
        assertTrue("警告ログが記録されていない", warnLogs().any { it.msg.contains("empty range") })
    }

    @Test
    fun `年候補件数が提示上限を超える指定では選択面を提示せず警告ログを残す`() {
        // LocalDate の全域を指定すると年候補は 1,999,999,999 件となり、提示上限 1,000,000 件を超える。
        assertNull(tapRow(spinnerCell(minDate = LocalDate.MIN, maxDate = LocalDate.MAX)))
        assertTrue(
            "警告ログが記録されていない",
            warnLogs().any { it.msg.contains("too many year candidates: 1999999999") },
        )
    }

    // MARK: - 年・月の変更への日候補の追随

    @Test
    fun `31日から日数の少ない月への変更は末日へ丸める`() {
        val sheet = openSheet(spinnerCell(date = LocalDate.of(2026, 1, 31)))
        selectMonth(sheet, 2)

        assertEquals(28, itemCountOf(sheet.dayWheel))
        assertEquals("28日", sheet.dayWheel.selectedDisplayText())
        assertEquals(LocalDate.of(2026, 2, 28), sheet.selectedDate)
    }

    @Test
    fun `閏年の2月は29日まで列挙される`() {
        val sheet = openSheet(spinnerCell(date = LocalDate.of(2028, 1, 15)))
        selectMonth(sheet, 2)

        assertEquals(29, itemCountOf(sheet.dayWheel))
        assertEquals("29日", sheet.dayWheel.bindRow(28).text?.toString())
        // 15日は 2月にも存在するため丸められない。
        assertEquals(LocalDate.of(2028, 2, 15), sheet.selectedDate)
    }

    @Test
    fun `年の変更でも日が追随する`() {
        val sheet = openSheet(spinnerCell(date = LocalDate.of(2028, 2, 29)))
        selectYear(sheet, 2027)

        assertEquals(28, itemCountOf(sheet.dayWheel))
        assertEquals("28日", sheet.dayWheel.selectedDisplayText())
        assertEquals(LocalDate.of(2027, 2, 28), sheet.selectedDate)
    }

    @Test
    fun `年 月の変更で範囲外になった日付は範囲内の最近傍へ丸める`() {
        val sheet = openSheet(
            spinnerCell(date = LocalDate.of(2021, 1, 31), minDate = LocalDate.of(2020, 4, 15)),
        )
        selectYear(sheet, 2020)

        assertEquals(LocalDate.of(2020, 4, 15), sheet.selectedDate)
        assertEquals("2020年", sheet.yearWheel.selectedDisplayText())
        assertEquals("4月", sheet.monthWheel.selectedDisplayText())
        assertEquals("15日", sheet.dayWheel.selectedDisplayText())
        // 境界月では日候補も 15日から始まる。
        assertEquals(16, itemCountOf(sheet.dayWheel))
        assertEquals("15日", sheet.dayWheel.bindRow(0).text?.toString())
    }

    @Test
    @Config(qualifiers = DEVICE_QUALIFIERS)
    fun `月系列のスクロール静止でも日候補が追随する`() {
        val sheet = openSheet(spinnerCell(date = LocalDate.of(2026, 1, 31)))
        layoutDialog(sheet)

        // 月ホイールを1候補ぶんスクロールして静止させる（1月 → 2月）。
        val rowHeight = sheet.monthWheel.rowViewAt(0)!!.height
        sheet.monthWheel.listView.smoothScrollBy(0, rowHeight)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        layoutDialog(sheet)

        assertEquals("2月", sheet.monthWheel.selectedDisplayText())
        assertEquals(28, itemCountOf(sheet.dayWheel))
        assertEquals(LocalDate.of(2026, 2, 28), sheet.selectedDate)
    }

    // MARK: - 今日へのジャンプ

    @Test
    fun `今日へジャンプすると3系列が現在日付になり callback は発火しない`() {
        val received = mutableListOf<LocalDate>()
        val sheet = openSheet(
            spinnerCell(
                date = LocalDate.of(2020, 1, 1),
                todayText = "今日",
                onValueChanged = { received.add(it) },
            ),
            today = FIXED_TODAY,
        )
        requireNotNull(sheet.todayView) { "「今日」操作が提示されていない" }.performClick()

        assertEquals(FIXED_TODAY, sheet.selectedDate)
        assertEquals("2026年", sheet.yearWheel.selectedDisplayText())
        assertEquals("8月", sheet.monthWheel.selectedDisplayText())
        assertEquals("2日", sheet.dayWheel.selectedDisplayText())
        assertTrue("ジャンプで callback が発火した", received.isEmpty())
    }

    @Test
    @Config(qualifiers = DEVICE_QUALIFIERS)
    fun `慣性移動中に今日へジャンプしても静止後の選択は今日のまま`() {
        val sheet = openSheet(
            spinnerCell(date = LocalDate.of(2020, 1, 1), todayText = "今日"),
            today = FIXED_TODAY,
        )
        layoutDialog(sheet)

        // 年ホイールを弾いた直後（静止する前）に「今日」を押す。「今日」chip は別 View のため、
        // タップだけでは年ホイールの慣性移動は止まらない。
        val rowHeight = sheet.yearWheel.rowViewAt(sheet.yearWheel.selectedIndex)!!.height
        sheet.yearWheel.listView.smoothScrollBy(0, rowHeight * FLING_ROW_COUNT)
        sheet.todayView!!.performClick()

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        layoutDialog(sheet)

        assertEquals(FIXED_TODAY, sheet.selectedDate)
        assertEquals("2026年", sheet.yearWheel.selectedDisplayText())
        assertEquals("8月", sheet.monthWheel.selectedDisplayText())
        assertEquals("2日", sheet.dayWheel.selectedDisplayText())
    }

    @Test
    fun `todayText が null なら今日へジャンプする操作を提示しない`() {
        assertNull(openSheet(spinnerCell(todayText = null)).todayView)
    }

    @Test
    fun `todayText が空文字なら今日へジャンプする操作を提示しない`() {
        assertNull(openSheet(spinnerCell(todayText = "")).todayView)
    }

    @Test
    fun `今日が範囲外なら選択中は変化しない`() {
        val sheet = openSheet(
            spinnerCell(
                date = LocalDate.of(2020, 6, 1),
                maxDate = LocalDate.of(2020, 12, 31),
                todayText = "今日",
            ),
            today = FIXED_TODAY,
        )
        sheet.todayView!!.performClick()

        assertEquals(LocalDate.of(2020, 6, 1), sheet.selectedDate)
        assertEquals("2020年", sheet.yearWheel.selectedDisplayText())
        assertEquals("6月", sheet.monthWheel.selectedDisplayText())
        assertEquals("1日", sheet.dayWheel.selectedDisplayText())
    }

    @Test
    fun `Material モードでは todayText があっても Spinner の選択面は開かない`() {
        // Material モードはカレンダーダイアログを提示する経路であり、3連ホイールの選択面は
        // 提示されない。todayText の有無で経路は変わらない
        // (Material 側の「今日」操作は DateCalendarDialogTest が担う)。
        assertNull(
            tapRow(
                DatePickerCell(
                    title = "誕生日",
                    date = LocalDate.of(2026, 8, 2),
                    uiStyle = DatePickerUIStyle.Material,
                    todayText = "今日",
                ),
            ),
        )
        assertNull(
            tapRow(
                DatePickerCell(
                    title = "誕生日",
                    date = LocalDate.of(2026, 8, 2),
                    uiStyle = DatePickerUIStyle.Material,
                ),
            ),
        )
    }

    // MARK: - 確定と非確定 dismiss

    @Test
    fun `確定で選択中の日付を1回通知して閉じる`() {
        val received = mutableListOf<LocalDate>()
        val sheet = openSheet(
            spinnerCell(date = LocalDate.of(2026, 8, 2), onValueChanged = { received.add(it) }),
        )
        selectMonth(sheet, 9)
        selectDay(sheet, 15)
        sheet.confirmView.performClick()

        assertEquals(listOf(LocalDate.of(2026, 9, 15)), received)
        assertFalse(sheet.isShowing)
    }

    @Test
    fun `キャンセルボタンでは callback を発火しない`() {
        var received: LocalDate? = null
        val sheet = openSheet(spinnerCell(onValueChanged = { received = it }))
        selectDay(sheet, 20)
        sheet.cancelView.performClick()

        assertNull(received)
        assertFalse(sheet.isShowing)
    }

    @Test
    fun `選択面は外側タップで閉じられる設定になっている`() {
        assertTrue(shadowOf(openSheet(spinnerCell()) as android.app.Dialog).isCancelableOnTouchOutside)
    }

    @Test
    fun `非確定 dismiss はどの経路でも callback を発火しない`() {
        // 外側タップと Back 操作は Dialog の cancel、下方向スワイプは BottomSheetDialog の
        // 非表示遷移を経て dismiss に至る。いずれの経路も確定 callback を通らない。
        var cancelResult: LocalDate? = null
        val outsideTap = openSheet(spinnerCell(onValueChanged = { cancelResult = it }))
        selectDay(outsideTap, 20)
        outsideTap.cancel()
        assertNull(cancelResult)

        var backResult: LocalDate? = null
        val back = openSheet(spinnerCell(onValueChanged = { backResult = it }))
        selectDay(back, 20)
        @Suppress("DEPRECATION")
        back.onBackPressed()
        assertNull(backResult)

        var swipeResult: LocalDate? = null
        val swipe = openSheet(spinnerCell(onValueChanged = { swipeResult = it }))
        selectDay(swipe, 20)
        swipe.dismiss()
        assertNull(swipeResult)
    }

    @Test
    fun `下方向スワイプ相当の非表示遷移で閉じても callback を発火しない`() {
        var received: LocalDate? = null
        val sheet = openSheet(spinnerCell(onValueChanged = { received = it }))
        val container = layoutDialog(sheet)
        val behavior = BottomSheetBehavior.from(container)

        behavior.state = BottomSheetBehavior.STATE_HIDDEN
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))

        assertEquals(BottomSheetBehavior.STATE_HIDDEN, behavior.state)
        assertNull(received)
        assertFalse(sheet.isShowing)
    }

    // MARK: - 選択操作の意味論

    @Test
    @Config(qualifiers = DEVICE_QUALIFIERS)
    fun `移動中の確定は直前にスナップ静止した候補を採用する`() {
        val received = mutableListOf<LocalDate>()
        val sheet = openSheet(
            spinnerCell(date = LocalDate.of(2026, 8, 2), onValueChanged = { received.add(it) }),
        )
        layoutDialog(sheet)

        // 日ホイールを候補位置の途中（0.6 行分）まで動かして指を離す。補正スクロールは進めない。
        val rowHeight = sheet.dayWheel.rowViewAt(0)!!.height
        dragWheelBy(sheet.dayWheel, -rowHeight * INTER_ROW_RATIO)
        sheet.confirmView.performClick()

        assertEquals(listOf(LocalDate.of(2026, 8, 2)), received)
    }

    @Test
    @Config(qualifiers = DEVICE_QUALIFIERS)
    fun `候補領域の下方向操作では選択面を閉じず候補が遷移する`() {
        var received: LocalDate? = null
        val sheet = openSheet(
            spinnerCell(date = LocalDate.of(2026, 8, 15), onValueChanged = { received = it }),
        )
        val container = layoutDialog(sheet)
        val behavior = BottomSheetBehavior.from(container)

        // ホイールはスクロールをシートへ伝播しない。
        assertFalse(
            sheet.dayWheel.listView.startNestedScroll(
                androidx.core.view.ViewCompat.SCROLL_AXIS_VERTICAL,
                androidx.core.view.ViewCompat.TYPE_TOUCH,
            ),
        )

        val coordinator = container.parent as ViewGroup

        // どの列を下方向へ操作しても、シートの dismiss ではなく候補の遷移になる。
        listOf(
            Triple("年", sheet.yearWheel) { date: LocalDate -> date.year },
            Triple("月", sheet.monthWheel) { date: LocalDate -> date.monthValue },
            Triple("日", sheet.dayWheel) { date: LocalDate -> date.dayOfMonth },
        ).forEach { (name, wheel, field) ->
            val before = field(sheet.selectedDate)
            val center = centerOf(wheel, coordinator)
            drag(coordinator, x = center.first, startY = center.second, dy = WHEEL_DRAG_DISTANCE_PX)
            layoutDialog(sheet)

            assertTrue("$name 列の操作で選択面が閉じてしまった", sheet.isShowing)
            assertEquals(BottomSheetBehavior.STATE_EXPANDED, behavior.state)
            assertTrue("$name 列の候補が遷移していない", field(sheet.selectedDate) < before)
            assertNull("dismiss 経路で callback が発火した", received)
        }
    }

    // MARK: - 強調色のスタイル解決

    @Test
    @Config(qualifiers = DEVICE_QUALIFIERS)
    fun `強調色は Cell 固有値を最優先で解決する`() {
        val sheet = openSheet(
            spinnerCell(
                accentColor = Color(0xFFFF0000),
                style = CellStyle(accentColor = Color(0xFF00FF00)),
            ),
            theme = Theme(cellAccentColor = Color(0xFF0000FF)),
        )
        assertEquals(Color(0xFFFF0000).toArgb(), selectedRowColor(sheet))
    }

    @Test
    @Config(qualifiers = DEVICE_QUALIFIERS)
    fun `強調色は Cell 固有値が無いとき CellStyle へフォールバックする`() {
        val sheet = openSheet(
            spinnerCell(style = CellStyle(accentColor = Color(0xFF00FF00))),
            theme = Theme(cellAccentColor = Color(0xFF0000FF)),
        )
        assertEquals(Color(0xFF00FF00).toArgb(), selectedRowColor(sheet))
    }

    @Test
    @Config(qualifiers = DEVICE_QUALIFIERS)
    fun `強調色は Cell 固有値も CellStyle も無いとき Theme へフォールバックする`() {
        val sheet = openSheet(
            spinnerCell(),
            theme = Theme(cellAccentColor = Color(0xFF0000FF)),
        )
        assertEquals(Color(0xFF0000FF).toArgb(), selectedRowColor(sheet))
    }

    @Test
    @Config(qualifiers = DEVICE_QUALIFIERS)
    fun `androidButtonColor は確定 キャンセル操作へ引き継がれる`() {
        val sheet = openSheet(
            spinnerCell(
                accentColor = Color(0xFFFF0000),
                androidButtonColor = Color(0xFF00AAFF),
            ),
            theme = Theme(cellAccentColor = Color(0xFF0000FF)),
        )

        assertEquals(Color(0xFF00AAFF).toArgb(), sheet.cancelView.currentTextColor)
        assertEquals(Color(0xFF00AAFF).toArgb(), confirmPillColor(sheet))
        // 選択中候補の強調は accentColor のまま（操作色に引きずられない）。
        assertEquals(Color(0xFFFF0000).toArgb(), selectedRowColor(sheet))
    }

    @Test
    @Config(qualifiers = DEVICE_QUALIFIERS)
    fun `androidButtonColor 未指定なら操作色も強調色の段階解決に従う`() {
        val sheet = openSheet(
            spinnerCell(accentColor = Color(0xFFFF0000)),
            theme = Theme(cellAccentColor = Color(0xFF0000FF)),
        )
        assertEquals(Color(0xFFFF0000).toArgb(), sheet.cancelView.currentTextColor)
        assertEquals(Color(0xFFFF0000).toArgb(), confirmPillColor(sheet))
    }

    /** レイアウト後の各ホイールの選択中行の文字色（3系列で同じ色になる）。 */
    private fun selectedRowColor(sheet: DateSelectionSheet): Int {
        layoutDialog(sheet)
        val colors = listOf(sheet.yearWheel, sheet.monthWheel, sheet.dayWheel)
            .map { it.rowViewAt(it.selectedIndex)!!.currentTextColor }
            .distinct()
        assertEquals("3系列の強調色が揃っていない", 1, colors.size)
        return colors.single()
    }

    /** 確定ラベルの pill 背景色。 */
    private fun confirmPillColor(sheet: DateSelectionSheet): Int {
        val ripple = sheet.confirmView.background as RippleDrawable
        return (ripple.getDrawable(0) as GradientDrawable).color!!.defaultColor
    }

    // MARK: - 候補表示の Locale 追随

    @Test
    fun `日本語 Locale では年月日の接尾辞つきで表示される`() {
        val sheet = openSheet(spinnerCell(date = LocalDate.of(2026, 8, 2)))
        assertEquals("2026年", sheet.yearWheel.selectedDisplayText())
        assertEquals("8月", sheet.monthWheel.selectedDisplayText())
        assertEquals("2日", sheet.dayWheel.selectedDisplayText())
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `英語 Locale では接尾辞なしの数値と月名で表示される`() {
        val sheet = openSheet(spinnerCell(date = LocalDate.of(2026, 8, 2)))
        assertEquals("2026", sheet.yearWheel.selectedDisplayText())
        assertEquals("Aug", sheet.monthWheel.selectedDisplayText())
        assertEquals("2", sheet.dayWheel.selectedDisplayText())
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `系列の並び順は Locale によらず年 月 日で固定される`() {
        val sheet = openSheet(spinnerCell(date = LocalDate.of(2026, 8, 2)))
        val row = sheet.yearWheel.parent as ViewGroup
        assertEquals(0, row.indexOfChild(sheet.yearWheel))
        assertEquals(1, row.indexOfChild(sheet.monthWheel))
        assertEquals(2, row.indexOfChild(sheet.dayWheel))
    }

    @Test
    @Config(qualifiers = "ar-rEG-ldrtl-w411dp-h891dp-xxhdpi")
    fun `RTL Locale でも3列は左から年 月 日の順に配置される`() {
        enableRtlSupport()
        val sheet = openSheet(spinnerCell(date = LocalDate.of(2026, 8, 2)))
        assertEquals(
            "RTL の Locale として構成されておらず、反転の検証になっていない",
            View.LAYOUT_DIRECTION_RTL,
            ctx.resources.configuration.layoutDirection,
        )
        // 実機では構成の layoutDirection が window の decor から内容へ伝わる。テストの window は
        // それを伝えないため、シート内容のルートへ Locale 追随を与えて RTL の条件に置く。
        sheet.contentRoot.layoutDirection = View.LAYOUT_DIRECTION_LOCALE
        layoutDialog(sheet)

        // 反転を止めるのは3列を並べるコンテナだけで、それを含む行までは RTL のまま届いている。
        assertEquals(
            "RTL がシート内容へ伝わっていない",
            View.LAYOUT_DIRECTION_RTL,
            (sheet.yearWheel.parent.parent as View).layoutDirection,
        )
        assertTrue(
            "年が月より右にある: year=${sheet.yearWheel.left} month=${sheet.monthWheel.left}",
            sheet.yearWheel.left < sheet.monthWheel.left,
        )
        assertTrue(
            "月が日より右にある: month=${sheet.monthWheel.left} day=${sheet.dayWheel.left}",
            sheet.monthWheel.left < sheet.dayWheel.left,
        )
    }

    /**
     * View 階層が RTL を解決できる条件を整える。
     *
     * View は「アプリが RTL 対応を宣言している」ときだけ layoutDirection を解決し、宣言が
     * なければ配置は常に LTR になる。宣言はライブラリではなく利用側アプリの持ち物のため、
     * 宣言済みのアプリに載った状態をテスト側で再現する。
     */
    private fun enableRtlSupport() {
        val info = ApplicationProvider.getApplicationContext<android.content.Context>().applicationInfo
        info.flags = info.flags or ApplicationInfo.FLAG_SUPPORTS_RTL
    }

    // MARK: - アクセシビリティ

    @Test
    fun `各系列は系列名とともに選択中候補を公開する`() {
        val sheet = openSheet(spinnerCell(date = LocalDate.of(2026, 8, 2)))

        val year = nodeInfoOf(sheet.yearWheel).contentDescription?.toString()
        val month = nodeInfoOf(sheet.monthWheel).contentDescription?.toString()
        val day = nodeInfoOf(sheet.dayWheel).contentDescription?.toString()

        assertTrue("年系列に選択値が含まれない: $year", year!!.contains("2026年"))
        assertTrue("月系列に選択値が含まれない: $month", month!!.contains("8月"))
        assertTrue("日系列に選択値が含まれない: $day", day!!.contains("2日"))
        // 系列名が付くことで、どの系列かを識別できる（3系列の公開文字列が互いに異なる）。
        assertTrue("年系列の名前が付いていない: $year", year.startsWith("年,"))
        assertTrue("月系列の名前が付いていない: $month", month.startsWith("月,"))
        assertTrue("日系列の名前が付いていない: $day", day.startsWith("日,"))
        assertNotEquals(year, month)
    }

    @Test
    fun `アクセシビリティ操作で系列ごとに候補を変更できる`() {
        val sheet = openSheet(spinnerCell(date = LocalDate.of(2026, 8, 2)))

        val handled = sheet.monthWheel.performAccessibilityAction(
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
            null,
        )

        assertTrue(handled)
        assertEquals(LocalDate.of(2026, 9, 2), sheet.selectedDate)
        assertTrue(
            "公開状態が更新されていない",
            nodeInfoOf(sheet.monthWheel).contentDescription!!.contains("9月"),
        )
    }

    @Test
    fun `アクセシビリティ操作での月変更でも日候補の追随と末日丸めが働く`() {
        val sheet = openSheet(spinnerCell(date = LocalDate.of(2026, 1, 31)))

        sheet.monthWheel.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null)

        assertEquals(LocalDate.of(2026, 2, 28), sheet.selectedDate)
        assertEquals(28, itemCountOf(sheet.dayWheel))
        assertEquals("28日", sheet.dayWheel.selectedDisplayText())
    }

    // MARK: - タッチ操作のヘルパ

    /**
     * ホイールを [dy] px だけドラッグして指を離す。
     *
     * 指を離す直前に同じ位置で間を置き、慣性移動（fling）を伴わずに静止するようにする。
     * 離した後の補正スクロールは進めないため、呼び出し直後は「行間で静止した」状態になる。
     */
    private fun dragWheelBy(wheel: KsWheelView, dy: Float, steps: Int = 8) {
        val target = wheel.listView
        val x = target.width / 2f
        val startY = target.height / 2f
        val slop = android.view.ViewConfiguration.get(target.context).scaledTouchSlop
        val total = dy + (if (dy < 0) -slop else slop)
        val downTime = SystemClock.uptimeMillis()

        fun dispatch(action: Int, y: Float, elapsed: Long) {
            MotionEvent.obtain(downTime, downTime + elapsed, action, x, y, 0).let {
                target.dispatchTouchEvent(it)
                it.recycle()
            }
        }

        dispatch(MotionEvent.ACTION_DOWN, startY, 0L)
        for (step in 1..steps) {
            dispatch(MotionEvent.ACTION_MOVE, startY + total * step / steps, step * 16L)
        }
        dispatch(MotionEvent.ACTION_MOVE, startY + total, steps * 16L + FINGER_HOLD_MS)
        dispatch(MotionEvent.ACTION_UP, startY + total, steps * 16L + FINGER_HOLD_MS + 16L)
    }

    /** [view] の中心座標を [reference] の座標系で返す（親を辿って位置を積み上げる）。 */
    private fun centerOf(view: View, reference: View): Pair<Float, Float> {
        var x = 0
        var y = 0
        var current: View? = view
        while (current != null && current !== reference) {
            x += current.left
            y += current.top
            current = current.parent as? View
        }
        return Pair((x + view.width / 2).toFloat(), (y + view.height / 2).toFloat())
    }

    /** [target] の ([x], [startY]) から [dy] だけ動かすドラッグを再現する。 */
    private fun drag(target: View, x: Float, startY: Float, dy: Float, steps: Int = 6) {
        val downTime = SystemClock.uptimeMillis()
        MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, startY, 0).let {
            target.dispatchTouchEvent(it)
            it.recycle()
        }
        for (step in 1..steps) {
            val y = startY + dy * step / steps
            MotionEvent.obtain(downTime, downTime + step * 16L, MotionEvent.ACTION_MOVE, x, y, 0).let {
                target.dispatchTouchEvent(it)
                it.recycle()
            }
        }
        MotionEvent.obtain(
            downTime,
            downTime + (steps + 1) * 16L,
            MotionEvent.ACTION_UP,
            x,
            startY + dy,
            0,
        ).let {
            target.dispatchTouchEvent(it)
            it.recycle()
        }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
    }

    private companion object {
        /** テストで固定する「今日」。 */
        val FIXED_TODAY: LocalDate = LocalDate.of(2026, 8, 2)

        /** 実機に近い画面条件（レイアウト実測を伴うテスト用）。 */
        // 先頭の "+" はクラス既定の qualifiers（ja）へ追記する指定。
        const val DEVICE_QUALIFIERS = "+w411dp-h891dp-xxhdpi"

        /** 行間で止めるドラッグの移動量（行高に対する比）。 */
        const val INTER_ROW_RATIO: Float = 0.6f

        /** 指を離す直前に静止させる時間（ms）。fling の速度を 0 にする。 */
        const val FINGER_HOLD_MS: Long = 200L

        /** ホイール面ドラッグの移動量（px）。 */
        const val WHEEL_DRAG_DISTANCE_PX = 300f

        /** 慣性移動を再現するときの移動量（行数）。 */
        const val FLING_ROW_COUNT: Int = 6
    }
}
