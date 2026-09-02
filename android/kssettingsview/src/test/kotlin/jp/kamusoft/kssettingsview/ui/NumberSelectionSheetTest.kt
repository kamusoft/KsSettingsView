package jp.kamusoft.kssettingsview.ui

import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

/**
 * [NumberPickerCell] の選択面（[NumberSelectionSheet]）の提示・候補生成・初期選択・
 * 確定 / 非確定 dismiss・強調色を検証する（android/ADR-0007）。
 *
 * 選択面は [NumberPickerCellViewHolder] の行タップ経路から開き、実際の配線ごと検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NumberSelectionSheetTest {

    private val ctx: android.content.Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            MaterialR.style.Theme_Material3_Light_NoActionBar,
        )

    private val parent get() = FrameLayout(ctx)

    /** Cell の行タップで選択面を開き、表示された [NumberSelectionSheet] を返す。 */
    private fun openSheet(cell: NumberPickerCell, theme: Theme = Theme()): NumberSelectionSheet =
        requireNotNull(tapRow(cell, theme)) { "選択面が提示されていない" }

    /** Cell の行をタップし、表示された Dialog を返す（提示されなければ `null`）。 */
    private fun tapRow(cell: NumberPickerCell, theme: Theme = Theme()): NumberSelectionSheet? {
        val vh = NumberPickerCellViewHolder.create(parent)
        vh.bind(cell, theme)
        vh.views.root.performClick()
        return ShadowDialog.getLatestDialog() as NumberSelectionSheet?
    }

    /**
     * ダイアログの実 View 階層（CoordinatorLayout 配下）を画面サイズで measure / layout し、
     * ボトムシートのコンテナを返す。
     */
    private fun layoutDialog(sheet: NumberSelectionSheet): FrameLayout {
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
        ShadowLog.getLogsForTag("NumberPickerCell").filter { it.type == Log.WARN }

    /** ホイールの候補表示（unit 適用後）を先頭から [count] 件取り出す。 */
    private fun candidateTexts(sheet: NumberSelectionSheet, count: Int): List<String> =
        (0 until count).map { sheet.wheelView.bindRow(it).text.toString() }

    // MARK: - 選択面の提示

    @Test
    fun `タイトルは pickerTitle を優先して解決する`() {
        val sheet = openSheet(
            NumberPickerCell(title = "サイズ", pickerTitle = "サイズを選択", min = 10, max = 30),
        )
        assertEquals("サイズを選択", sheet.titleView.text?.toString())
    }

    @Test
    fun `pickerTitle が null のときタイトルは title を使う`() {
        val sheet = openSheet(NumberPickerCell(title = "サイズ", min = 10, max = 30))
        assertEquals("サイズ", sheet.titleView.text?.toString())
    }

    @Test
    fun `操作ラベルは OS の公開文字列リソースから解決される`() {
        val sheet = openSheet(NumberPickerCell(title = "サイズ", min = 0, max = 10))
        assertEquals(ctx.getString(android.R.string.cancel), sheet.cancelView.text?.toString())
        assertEquals(ctx.getString(android.R.string.ok), sheet.confirmView.text?.toString())
    }

    @Test
    fun `無効 Cell の行タップでは選択面を提示しない`() {
        val vh = NumberPickerCellViewHolder.create(parent)
        vh.bind(NumberPickerCell(title = "サイズ", isEnabled = false), Theme())
        vh.views.root.performClick()
        assertNull(ShadowDialog.getLatestDialog())
    }

    // MARK: - 候補の列挙

    @Test
    fun `候補は step 刻みで昇順に列挙される`() {
        val sheet = openSheet(NumberPickerCell(title = "x", min = 0, max = 100, step = 25))
        assertEquals(5, sheet.wheelView.listView.adapter?.itemCount)
        assertEquals(listOf("0", "25", "50", "75", "100"), candidateTexts(sheet, 5))
    }

    @Test
    fun `step が 0 以下なら 1 へ fallback する`() {
        val sheet = openSheet(NumberPickerCell(title = "x", min = 1, max = 3, step = 0))
        assertEquals(3, sheet.wheelView.listView.adapter?.itemCount)
        assertEquals(listOf("1", "2", "3"), candidateTexts(sheet, 3))
    }

    @Test
    fun `min が max より大きいときは選択面を提示せず警告ログを残す`() {
        assertNull(tapRow(NumberPickerCell(title = "x", min = 10, max = 5)))
        assertTrue("警告ログが記録されていない", warnLogs().any { it.msg.contains("invalid range") })
    }

    @Test
    fun `候補件数が Int 上限を超える指定では選択面を提示せず警告ログを残す`() {
        assertNull(
            tapRow(NumberPickerCell(title = "x", min = Int.MIN_VALUE, max = Int.MAX_VALUE, step = 1)),
        )
        assertTrue("警告ログが記録されていない", warnLogs().any { it.msg.contains("too many candidates") })
    }

    @Test
    fun `max 付近の step 加算でも候補の列挙は終端する`() {
        val sheet = openSheet(
            NumberPickerCell(
                title = "x",
                min = Int.MAX_VALUE - 1,
                max = Int.MAX_VALUE,
                step = 5,
                value = Int.MAX_VALUE - 1,
            ),
        )
        assertEquals(1, sheet.wheelView.listView.adapter?.itemCount)
        assertEquals(listOf("${Int.MAX_VALUE - 1}"), candidateTexts(sheet, 1))
    }

    @Test
    fun `候補件数が Int 上限ちょうどでも候補列を実体化せずに提示する`() {
        // 候補件数 = Int.MAX_VALUE（提示できる上限ちょうど）。全件を List 化すると破綻するため、
        // 件数と index 単位の表示文字列だけで成立していることを確認する。
        val sheet = openSheet(
            NumberPickerCell(title = "x", min = 0, max = Int.MAX_VALUE - 1, step = 1, unit = "px"),
        )

        assertEquals(Int.MAX_VALUE, sheet.wheelView.listView.adapter?.itemCount)
        assertEquals("0 px", sheet.wheelView.bindRow(0).text?.toString())
        assertEquals(
            "${Int.MAX_VALUE - 1} px",
            sheet.wheelView.bindRow(Int.MAX_VALUE - 1).text?.toString(),
        )
    }

    @Test
    fun `大規模な候補列でも確定値は index から算出される`() {
        val received = mutableListOf<Int>()
        val sheet = openSheet(
            NumberPickerCell(
                title = "x",
                min = 0,
                max = Int.MAX_VALUE - 1,
                step = 1,
                value = Int.MAX_VALUE - 1,
                onValueChanged = { received.add(it) },
            ),
        )

        // 末尾候補（index = Int.MAX_VALUE - 1）が選択中。
        assertEquals(Int.MAX_VALUE - 1, sheet.wheelView.selectedIndex)
        sheet.confirmView.performClick()

        assertEquals(listOf(Int.MAX_VALUE - 1), received)
    }

    @Test
    fun `Int 全域にまたがる候補列でも候補値は 64bit で算出される`() {
        // min が負・範囲が Int 全域でも、先頭 + index × step が Int を溢れない。
        // 候補件数は (2^32 - 1) / 3 + 1 件で、上限 Int.MAX_VALUE を下回るため提示できる。
        val received = mutableListOf<Int>()
        val lastIndex = 1_431_655_765
        val sheet = openSheet(
            NumberPickerCell(
                title = "x",
                min = Int.MIN_VALUE,
                max = Int.MAX_VALUE,
                step = 3,
                value = Int.MAX_VALUE,
                onValueChanged = { received.add(it) },
            ),
        )

        assertEquals(lastIndex + 1, sheet.wheelView.listView.adapter?.itemCount)
        assertEquals("${Int.MIN_VALUE}", sheet.wheelView.bindRow(0).text?.toString())
        assertEquals("${Int.MAX_VALUE}", sheet.wheelView.bindRow(lastIndex).text?.toString())

        assertEquals(lastIndex, sheet.wheelView.selectedIndex)
        sheet.confirmView.performClick()
        assertEquals(listOf(Int.MAX_VALUE), received)
    }

    // MARK: - unit の適用

    @Test
    fun `候補表示には unit が適用される`() {
        val sheet = openSheet(NumberPickerCell(title = "x", min = 10, max = 20, step = 5, unit = "px"))
        assertEquals(listOf("10 px", "15 px", "20 px"), candidateTexts(sheet, 3))
    }

    @Test
    fun `valueText の明示指定は候補表示に影響しない`() {
        val sheet = openSheet(
            NumberPickerCell(
                title = "x",
                min = 10,
                max = 20,
                step = 5,
                unit = "px",
                valueText = "十五ピクセル",
            ),
        )
        assertEquals(listOf("10 px", "15 px", "20 px"), candidateTexts(sheet, 3))
    }

    // MARK: - 初期選択

    @Test
    fun `初期選択は現在値の候補になる`() {
        val sheet = openSheet(
            NumberPickerCell(title = "x", min = 0, max = 100, step = 25, value = 50),
        )
        assertEquals(2, sheet.wheelView.selectedIndex)
        assertEquals("50", sheet.wheelView.selectedDisplayText())
    }

    @Test
    fun `現在値が候補に含まれない場合は先頭候補が選択中になる`() {
        val sheet = openSheet(
            NumberPickerCell(title = "x", min = 0, max = 100, step = 25, value = 30),
        )
        assertEquals(0, sheet.wheelView.selectedIndex)
        assertEquals("0", sheet.wheelView.selectedDisplayText())
    }

    @Test
    fun `範囲外の現在値でも先頭候補が選択中になる`() {
        val below = openSheet(NumberPickerCell(title = "x", min = 10, max = 30, value = 5))
        assertEquals(0, below.wheelView.selectedIndex)

        val above = openSheet(NumberPickerCell(title = "x", min = 10, max = 30, value = 99))
        assertEquals(0, above.wheelView.selectedIndex)
    }

    // MARK: - 確定と非確定 dismiss

    @Test
    fun `確定で選択中の候補値を1回通知して閉じる`() {
        val received = mutableListOf<Int>()
        val sheet = openSheet(
            NumberPickerCell(
                title = "x",
                min = 0,
                max = 100,
                step = 25,
                value = 50,
                onValueChanged = { received.add(it) },
            ),
        )
        layoutDialog(sheet)
        // 候補 75 へ移してから確定する。
        sheet.wheelView.performAccessibilityAction(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
            null,
        )
        sheet.confirmView.performClick()

        assertEquals(listOf(75), received)
        assertFalse(sheet.isShowing)
    }

    @Test
    fun `キャンセルボタンでは callback を発火しない`() {
        var received: Int? = null
        val sheet = openSheet(numberCell { received = it })
        layoutDialog(sheet)
        sheet.wheelView.performAccessibilityAction(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
            null,
        )
        sheet.cancelView.performClick()

        assertNull(received)
        assertFalse(sheet.isShowing)
    }

    @Test
    fun `選択面は外側タップで閉じられる設定になっている`() {
        val sheet = openSheet(NumberPickerCell(title = "x", min = 0, max = 10))
        assertTrue(shadowOf(sheet as android.app.Dialog).isCancelableOnTouchOutside)
    }

    @Test
    fun `非確定 dismiss はどの経路でも callback を発火しない`() {
        // 外側タップと Back 操作は Dialog の cancel、下方向スワイプは BottomSheetDialog の
        // 非表示遷移を経て dismiss に至る。いずれの経路も確定 callback を通らないことを確認する。
        var cancelResult: Int? = null
        val outsideTap = openSheet(numberCell { cancelResult = it })
        outsideTap.cancel()
        assertNull(cancelResult)

        var backResult: Int? = null
        val back = openSheet(numberCell { backResult = it })
        @Suppress("DEPRECATION")
        back.onBackPressed()
        assertNull(backResult)

        var swipeResult: Int? = null
        val swipe = openSheet(numberCell { swipeResult = it })
        swipe.dismiss()
        assertNull(swipeResult)
    }

    @Test
    fun `下方向スワイプ相当の非表示遷移で閉じても callback を発火しない`() {
        var received: Int? = null
        val sheet = openSheet(numberCell { received = it })
        val container = layoutDialog(sheet)
        val behavior = BottomSheetBehavior.from(container)

        behavior.state = BottomSheetBehavior.STATE_HIDDEN
        // 非表示位置への settle アニメーションを完了させる。
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))

        assertEquals(BottomSheetBehavior.STATE_HIDDEN, behavior.state)
        assertNull(received)
        assertFalse(sheet.isShowing)
    }

    private fun numberCell(onValueChanged: (Int) -> Unit): NumberPickerCell = NumberPickerCell(
        title = "サイズ",
        min = 0,
        max = 100,
        step = 25,
        value = 50,
        onValueChanged = onValueChanged,
    )

    // MARK: - 候補領域の操作はシートを閉じない

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `候補領域の下方向操作では選択面を閉じず候補が遷移する`() {
        var received: Int? = null
        val sheet = openSheet(
            NumberPickerCell(
                title = "サイズ",
                min = 0,
                max = 100,
                step = 1,
                value = 50,
                onValueChanged = { received = it },
            ),
        )
        val container = layoutDialog(sheet)
        val behavior = BottomSheetBehavior.from(container)
        val wheel = sheet.wheelView

        // ホイールはスクロールをシートへ伝播しない。
        assertFalse(
            wheel.listView.startNestedScroll(
                androidx.core.view.ViewCompat.SCROLL_AXIS_VERTICAL,
                androidx.core.view.ViewCompat.TYPE_TOUCH,
            ),
        )

        // ホイール面を下方向へドラッグする（候補の遷移であり dismiss ではない）。
        val coordinator = container.parent as ViewGroup
        drag(
            coordinator,
            x = container.width / 2f,
            startY = container.top + wheel.top + wheel.height / 2f,
            dy = WHEEL_DRAG_DISTANCE_PX,
        )
        layoutDialog(sheet)

        assertTrue("選択面が閉じてしまった", sheet.isShowing)
        assertEquals(BottomSheetBehavior.STATE_EXPANDED, behavior.state)
        assertTrue("候補が遷移していない", wheel.selectedIndex < 50)
        assertNull("dismiss 経路で callback が発火した", received)
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
        // ドラッグ終了後の settle アニメーションを完了させる。
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
    }

    // MARK: - 強調色のスタイル解決

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `強調色は Cell 固有値を最優先で解決する`() {
        val sheet = openSheet(
            NumberPickerCell(
                title = "x",
                min = 0,
                max = 100,
                step = 25,
                value = 50,
                accentColor = Color(0xFFFF0000),
                style = CellStyle(accentColor = Color(0xFF00FF00)),
            ),
            theme = Theme(cellAccentColor = Color(0xFF0000FF)),
        )
        assertEquals(Color(0xFFFF0000).toArgb(), selectedRowColor(sheet))
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `強調色は Cell 固有値が無いとき CellStyle へフォールバックする`() {
        val sheet = openSheet(
            NumberPickerCell(
                title = "x",
                min = 0,
                max = 100,
                step = 25,
                value = 50,
                style = CellStyle(accentColor = Color(0xFF00FF00)),
            ),
            theme = Theme(cellAccentColor = Color(0xFF0000FF)),
        )
        assertEquals(Color(0xFF00FF00).toArgb(), selectedRowColor(sheet))
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `強調色は Cell 固有値も CellStyle も無いとき Theme へフォールバックする`() {
        val sheet = openSheet(
            NumberPickerCell(title = "x", min = 0, max = 100, step = 25, value = 50),
            theme = Theme(cellAccentColor = Color(0xFF0000FF)),
        )
        assertEquals(Color(0xFF0000FF).toArgb(), selectedRowColor(sheet))
    }

    /** レイアウト後の選択中行の文字色。 */
    private fun selectedRowColor(sheet: NumberSelectionSheet): Int {
        layoutDialog(sheet)
        val wheel = sheet.wheelView
        return wheel.rowViewAt(wheel.selectedIndex)!!.currentTextColor
    }

    // MARK: - シート面

    @Test
    fun `シート面の色は Theme の cellBackgroundColor で tint される`() {
        val sheet = openSheet(
            NumberPickerCell(title = "x", min = 0, max = 10),
            theme = Theme(cellBackgroundColor = Color(0xFF102030)),
        )
        val container = sheet.findViewById<FrameLayout>(MaterialR.id.design_bottom_sheet)!!
        assertEquals(Color(0xFF102030).toArgb(), container.backgroundTintList?.defaultColor)
    }

    private companion object {
        /** 実機に近い画面条件（レイアウト実測を伴うテスト用）。 */
        const val DEVICE_QUALIFIERS = "w411dp-h891dp-xxhdpi"

        /** ホイール面ドラッグの移動量（px）。 */
        const val WHEEL_DRAG_DISTANCE_PX = 300f
    }
}
