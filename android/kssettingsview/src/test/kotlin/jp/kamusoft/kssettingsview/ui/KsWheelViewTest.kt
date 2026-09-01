package jp.kamusoft.kssettingsview.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * スナップ式ホイール部品 [KsWheelView] の初期位置・選択遷移・強調と減衰・
 * アクセシビリティ状態を検証する（android/ADR-0007）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
class KsWheelViewTest {

    private fun wheelStyle(
        accentColor: Int = ACCENT,
        itemTextColor: Int = ITEM_TEXT,
    ): KsWheelStyle = KsWheelStyle(
        accentColor = accentColor,
        surfaceColor = Color.WHITE,
        itemTextColor = itemTextColor,
        itemTypeface = Typeface.DEFAULT,
        itemTextSizeSp = 17f,
    )

    /**
     * 候補 [items] のホイールを生成し、Activity へ載せて実レイアウトまで通す。
     *
     * スクロールの慣性・スナップのアニメーションは window へ attach された View でのみ動くため、
     * Activity の contentView として載せる。
     */
    private fun buildWheel(
        items: List<String> = listOf("0", "25", "50", "75", "100"),
        initialIndex: Int = 2,
        style: KsWheelStyle = wheelStyle(),
    ): KsWheelView {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val wheel = KsWheelView(
            context = activity,
            itemCount = items.size,
            displayTextAt = { items[it] },
            initialIndex = initialIndex,
            wheelStyle = style,
        )
        activity.setContentView(wheel)
        idle()
        layoutWheel(wheel)
        return wheel
    }

    private fun layoutWheel(wheel: KsWheelView, widthPx: Int = 1080) {
        wheel.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        wheel.layout(0, 0, wheel.measuredWidth, wheel.measuredHeight)
    }

    private fun nodeInfoOf(view: View): AccessibilityNodeInfo {
        val info = AccessibilityNodeInfo.obtain()
        view.onInitializeAccessibilityNodeInfo(info)
        return info
    }

    // MARK: - 初期状態

    @Test
    fun `初期選択は指定された index の候補になる`() {
        val wheel = buildWheel(initialIndex = 2)
        assertEquals(2, wheel.selectedIndex)
        assertEquals("50", wheel.selectedDisplayText())
    }

    @Test
    fun `範囲外の初期 index は最も近い有効な候補へ丸める`() {
        assertEquals(4, buildWheel(initialIndex = 99).selectedIndex)
        assertEquals(0, buildWheel(initialIndex = -3).selectedIndex)
    }

    @Test
    fun `初期選択の候補は中央の選択位置へ置かれる`() {
        val wheel = buildWheel(initialIndex = 2)
        val row = wheel.rowViewAt(2)
        assertNotNull("選択中の行がレイアウトされていない", row)
        // 可視5行の中央（上に2行分の余白）に選択中の候補が来る。
        assertEquals(2 * row!!.height, row.top)
    }

    @Test
    fun `ホイールの高さは可視行数分で固定される`() {
        val wheel = buildWheel()
        val rowHeight = wheel.rowViewAt(2)!!.height
        assertEquals(rowHeight * KsWheelView.VISIBLE_ROW_COUNT, wheel.height)
    }

    @Test
    fun `候補の表示文字列はそのまま行に反映される`() {
        val wheel = buildWheel(items = listOf("10 px", "15 px", "20 px"), initialIndex = 1)
        assertEquals("10 px", wheel.bindRow(0).text?.toString())
        assertEquals("15 px", wheel.bindRow(1).text?.toString())
        assertEquals("20 px", wheel.bindRow(2).text?.toString())
    }

    // MARK: - 強調と減衰

    @Test
    fun `選択中行だけが強調色の太字で描画される`() {
        val wheel = buildWheel(initialIndex = 2)

        val selected = wheel.rowViewAt(2)!!
        assertEquals(ACCENT, selected.currentTextColor)
        assertEquals("選択中行が太字でない", Typeface.BOLD, selected.typeface.style)

        listOf(0, 1, 3, 4).forEach { index ->
            val row = wheel.rowViewAt(index)!!
            assertEquals("index=$index の色が既定でない", ITEM_TEXT, row.currentTextColor)
            assertEquals("index=$index が太字になっている", Typeface.NORMAL, row.typeface.style)
        }
    }

    @Test
    fun `中央ハイライト帯は選択位置に強調色の淡色で敷かれる`() {
        val wheel = buildWheel(initialIndex = 2)
        val band = wheel.bandView
        val selected = wheel.rowViewAt(2)!!

        // 帯は選択中行と同じ高さ・同じ位置にある。
        assertEquals(selected.height, band.height)
        assertEquals(selected.top, band.top)

        val bandColor = (band.background as android.graphics.drawable.GradientDrawable)
            .color!!.defaultColor
        assertEquals("帯の色相が強調色でない", ACCENT and 0x00FFFFFF, bandColor and 0x00FFFFFF)
        assertTrue("帯が淡色になっていない", android.graphics.Color.alpha(bandColor) < 255 / 2)
    }

    @Test
    fun `中央から離れるほど行はフェードして縮小する`() {
        val wheel = buildWheel(initialIndex = 2)
        val center = wheel.rowViewAt(2)!!
        val near = wheel.rowViewAt(1)!!
        val far = wheel.rowViewAt(0)!!

        assertEquals(1f, center.alpha, 0.01f)
        assertTrue("1行離れた候補がフェードしていない", near.alpha < center.alpha)
        assertTrue("2行離れた候補がさらにフェードしていない", far.alpha < near.alpha)

        assertTrue("中央行が拡大されていない", center.scaleX > 1f)
        assertTrue("1行離れた候補が縮小されていない", near.scaleX < center.scaleX)
        assertTrue("2行離れた候補がさらに縮小されていない", far.scaleX < near.scaleX)
        assertEquals(center.scaleX, center.scaleY, 0.001f)
    }

    // MARK: - 選択遷移（スナップ静止時のみ更新）

    @Test
    fun `移動中は選択中候補を更新しない`() {
        val wheel = buildWheel(initialIndex = 2)
        val rowHeight = wheel.rowViewAt(2)!!.height

        // 静止を伴わないスクロール（ドラッグ・慣性移動の途中に相当）。
        wheel.listView.scrollBy(0, rowHeight)

        assertEquals("静止前に選択が更新された", 2, wheel.selectedIndex)
        assertEquals("50", wheel.selectedDisplayText())
    }

    @Test
    fun `静止すると中央にスナップした候補が選択中になる`() {
        val wheel = buildWheel(initialIndex = 2)
        val rowHeight = wheel.rowViewAt(2)!!.height

        wheel.listView.smoothScrollBy(0, rowHeight)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        layoutWheel(wheel)

        assertEquals(3, wheel.selectedIndex)
        assertEquals("75", wheel.selectedDisplayText())
        assertEquals(ACCENT, wheel.rowViewAt(3)!!.currentTextColor)
        assertEquals(ITEM_TEXT, wheel.rowViewAt(2)!!.currentTextColor)
    }

    @Test
    fun `行間で指を離した時点では選択中候補を更新しない`() {
        val wheel = buildWheel(initialIndex = 2)
        val rowHeight = wheel.rowViewAt(2)!!.height

        // 候補位置の途中（0.6 行分）で指を離す。この時点でスクロールは静止状態になるが、
        // 候補は中央へ整列しておらず、ここからスナップの補正スクロールが始まる。
        dragWheelBy(wheel, -rowHeight * INTER_ROW_RATIO)

        assertTrue(
            "行間で止まっておらず、補正前の状態を再現できていない",
            wheel.rowViewAt(2)!!.top != 2 * rowHeight,
        )
        assertEquals("候補位置へ整列する前に選択が更新された", 2, wheel.selectedIndex)
        assertEquals("50", wheel.selectedDisplayText())
    }

    @Test
    fun `補正スクロールが完了して候補位置へ整列すると選択中候補が更新される`() {
        val wheel = buildWheel(initialIndex = 2)
        val rowHeight = wheel.rowViewAt(2)!!.height

        dragWheelBy(wheel, -rowHeight * INTER_ROW_RATIO)
        // スナップの補正スクロールを完了させる。
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        layoutWheel(wheel)

        assertEquals(3, wheel.selectedIndex)
        assertEquals("75", wheel.selectedDisplayText())
        assertEquals("候補が中央へ整列していない", 2 * rowHeight, wheel.rowViewAt(3)!!.top)
    }

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
        // タッチスロップ分は掴み始めに消費されるため、狙った移動量へ上乗せする。
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
        // 指を止めてから離す（速度 0 = fling なし）。
        dispatch(MotionEvent.ACTION_MOVE, startY + total, steps * 16L + FINGER_HOLD_MS)
        dispatch(MotionEvent.ACTION_UP, startY + total, steps * 16L + FINGER_HOLD_MS + 16L)
    }

    // MARK: - 候補の差し替えとプログラム的な選択

    @Test
    fun `候補を差し替えると件数と表示文字列が入れ替わる`() {
        val wheel = buildWheel(items = listOf("1日", "2日", "3日"), initialIndex = 2)
        val days = (1..28).map { "${it}日" }

        wheel.setCandidates(days.size, { days[it] }, selectedIndex = 27)
        layoutWheel(wheel)

        assertEquals(28, wheel.listView.adapter?.itemCount)
        assertEquals(27, wheel.selectedIndex)
        assertEquals("28日", wheel.selectedDisplayText())
        assertEquals("1日", wheel.bindRow(0).text?.toString())
    }

    @Test
    fun `候補の差し替えで範囲外になる選択は最も近い候補へ丸める`() {
        val wheel = buildWheel(items = (1..31).map { "${it}日" }, initialIndex = 30)
        val shorter = (1..28).map { "${it}日" }

        wheel.setCandidates(shorter.size, { shorter[it] }, selectedIndex = 30)

        assertEquals(27, wheel.selectedIndex)
        assertEquals("28日", wheel.selectedDisplayText())
    }

    @Test
    fun `候補の差し替えでは選択変更を通知しない`() {
        val wheel = buildWheel(items = listOf("1日", "2日", "3日"), initialIndex = 0)
        val notified = mutableListOf<Int>()
        wheel.onSelectionChanged = { notified.add(it) }

        val days = (1..28).map { "${it}日" }
        wheel.setCandidates(days.size, { days[it] }, selectedIndex = 5)

        assertEquals(5, wheel.selectedIndex)
        assertTrue("候補の差し替えで通知され再入しうる", notified.isEmpty())
    }

    @Test
    fun `プログラム的な選択の移動で選択中候補が変わり通知される`() {
        val wheel = buildWheel(initialIndex = 0)
        val notified = mutableListOf<Int>()
        wheel.onSelectionChanged = { notified.add(it) }

        wheel.setSelectedIndex(3)
        layoutWheel(wheel)

        assertEquals(3, wheel.selectedIndex)
        assertEquals("75", wheel.selectedDisplayText())
        assertEquals(listOf(3), notified)
        assertEquals("候補が中央へ整列していない", 2 * wheel.rowViewAt(3)!!.height, wheel.rowViewAt(3)!!.top)
    }

    @Test
    fun `範囲外への移動指示は無視され通知もされない`() {
        val wheel = buildWheel(initialIndex = 2)
        val notified = mutableListOf<Int>()
        wheel.onSelectionChanged = { notified.add(it) }

        wheel.setSelectedIndex(99)

        assertEquals(2, wheel.selectedIndex)
        assertTrue(notified.isEmpty())
    }

    // MARK: - 慣性移動中のプログラム的な移動

    @Test
    fun `慣性移動中にプログラム的に選択を移すと移動先に留まる`() {
        val items = (0..20).map { it.toString() }
        val wheel = buildWheel(items = items, initialIndex = 0)
        val rowHeight = wheel.rowViewAt(0)!!.height

        // 数行ぶんの慣性移動を始める。移動は次のフレームから進むため、この時点ではまだ静止前。
        wheel.listView.smoothScrollBy(0, rowHeight * FLING_ROW_COUNT)
        assertEquals(
            "慣性移動が静止してしまい、移動中の再現になっていない",
            RecyclerView.SCROLL_STATE_SETTLING,
            wheel.listView.scrollState,
        )

        // 静止する前に別経路で選択を移す。
        // 慣性移動が打ち切られていなければ、以降のフレームで移動が続いて別の候補へ着地する。
        wheel.setSelectedIndex(2)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        layoutWheel(wheel)

        assertEquals(2, wheel.selectedIndex)
        assertEquals("2", wheel.selectedDisplayText())
        assertEquals("候補が中央へ整列していない", 2 * rowHeight, wheel.rowViewAt(2)!!.top)
    }

    @Test
    fun `慣性移動を打ち切った中間位置は選択として通知されない`() {
        val items = (0..20).map { it.toString() }
        val wheel = buildWheel(items = items, initialIndex = 0)
        val notified = mutableListOf<Int>()
        wheel.onSelectionChanged = { notified.add(it) }
        val rowHeight = wheel.rowViewAt(0)!!.height

        // 候補位置へちょうど整列した状態から慣性移動を始める。ここで打ち切ると、
        // 整列済みの中間位置がそのまま「静止した候補」に見えてしまう。
        wheel.listView.scrollBy(0, rowHeight)
        wheel.listView.smoothScrollBy(0, rowHeight * FLING_ROW_COUNT)

        wheel.setSelectedIndex(4)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        layoutWheel(wheel)

        assertEquals(4, wheel.selectedIndex)
        assertEquals("中間位置が選択として通知された", listOf(4), notified)
    }

    @Test
    fun `慣性移動中の候補差し替えは差し替え後の選択に留まる`() {
        val wheel = buildWheel(items = (1..31).map { "${it}日" }, initialIndex = 0)
        val rowHeight = wheel.rowViewAt(0)!!.height
        val notified = mutableListOf<Int>()
        wheel.onSelectionChanged = { notified.add(it) }

        // 差し替え前の候補に向かって移動している最中に候補を入れ替える。
        wheel.listView.smoothScrollBy(0, rowHeight * FLING_ROW_COUNT)

        val shorter = (1..28).map { "${it}日" }
        wheel.setCandidates(shorter.size, { shorter[it] }, selectedIndex = 10)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        layoutWheel(wheel)

        assertEquals(10, wheel.selectedIndex)
        assertEquals("11日", wheel.selectedDisplayText())
        assertTrue("候補の差し替えで通知され再入しうる", notified.isEmpty())
    }

    @Test
    fun `スナップ静止で選択変更を通知する`() {
        val wheel = buildWheel(initialIndex = 2)
        val notified = mutableListOf<Int>()
        wheel.onSelectionChanged = { notified.add(it) }
        val rowHeight = wheel.rowViewAt(2)!!.height

        wheel.listView.smoothScrollBy(0, rowHeight)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))

        assertEquals(listOf(3), notified)
    }

    @Test
    fun `アクセシビリティ操作でも選択変更を通知する`() {
        val wheel = buildWheel(initialIndex = 2)
        val notified = mutableListOf<Int>()
        wheel.onSelectionChanged = { notified.add(it) }

        wheel.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null)
        // 端では移動しないため通知も増えない。
        buildWheel(initialIndex = 4).let { atEnd ->
            atEnd.onSelectionChanged = { notified.add(it) }
            atEnd.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null)
        }

        assertEquals(listOf(3), notified)
    }

    // MARK: - アクセシビリティ

    @Test
    fun `系列名を与えると選択中候補と併せて公開される`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val wheel = KsWheelView(
            context = activity,
            itemCount = 3,
            displayTextAt = { "${it + 1}月" },
            initialIndex = 1,
            wheelStyle = wheelStyle(),
            seriesLabel = "月",
        )

        assertEquals("月, 2月", wheel.contentDescription?.toString())
        assertEquals("月, 2月", nodeInfoOf(wheel).contentDescription?.toString())

        wheel.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null)
        assertEquals("月, 3月", nodeInfoOf(wheel).contentDescription?.toString())
    }

    @Test
    fun `選択中候補の表示文字列を公開する`() {
        val wheel = buildWheel(items = listOf("5 px", "10 px", "15 px", "20 px", "25 px"), initialIndex = 2)
        assertEquals("15 px", nodeInfoOf(wheel).contentDescription?.toString())
    }

    @Test
    fun `ホイールはスピナー相当のコントロールとして公開される`() {
        val wheel = buildWheel()
        assertEquals(android.widget.NumberPicker::class.java.name, nodeInfoOf(wheel).className)
    }

    @Test
    fun `次候補へのアクセシビリティ操作で選択中が進む`() {
        val wheel = buildWheel(items = listOf("5 px", "10 px", "15 px", "20 px", "25 px"), initialIndex = 2)

        val handled = wheel.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null)
        layoutWheel(wheel)

        assertTrue(handled)
        assertEquals(3, wheel.selectedIndex)
        assertEquals("20 px", nodeInfoOf(wheel).contentDescription?.toString())
        assertEquals(ACCENT, wheel.rowViewAt(3)!!.currentTextColor)
    }

    @Test
    fun `前候補へのアクセシビリティ操作で選択中が戻る`() {
        val wheel = buildWheel(items = listOf("5 px", "10 px", "15 px", "20 px", "25 px"), initialIndex = 2)

        val handled = wheel.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, null)

        assertTrue(handled)
        assertEquals(1, wheel.selectedIndex)
        assertEquals("10 px", nodeInfoOf(wheel).contentDescription?.toString())
    }

    @Test
    fun `末尾候補では次候補へ変更されない`() {
        val wheel = buildWheel(initialIndex = 4)

        val handled = wheel.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null)

        assertFalse(handled)
        assertEquals(4, wheel.selectedIndex)
        assertEquals("100", wheel.selectedDisplayText())
        assertFalse(
            "末尾で次候補アクションが提供されている",
            hasAction(nodeInfoOf(wheel), AccessibilityNodeInfo.ACTION_SCROLL_FORWARD),
        )
    }

    @Test
    fun `先頭候補では前候補へ変更されない`() {
        val wheel = buildWheel(initialIndex = 0)

        val handled = wheel.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, null)

        assertFalse(handled)
        assertEquals(0, wheel.selectedIndex)
        assertFalse(
            "先頭で前候補アクションが提供されている",
            hasAction(nodeInfoOf(wheel), AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD),
        )
        assertTrue(
            "先頭で次候補アクションが提供されていない",
            hasAction(nodeInfoOf(wheel), AccessibilityNodeInfo.ACTION_SCROLL_FORWARD),
        )
    }

    @Test
    fun `選択中候補が変わるとアクセシビリティイベントを送出する`() {
        val wheel = buildWheel(items = listOf("5 px", "10 px", "15 px", "20 px", "25 px"), initialIndex = 2)
        val sentEventTypes = recordSentAccessibilityEvents(wheel)

        wheel.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null)

        assertTrue(
            "選択の変化が支援技術へ通知されていない",
            sentEventTypes.contains(AccessibilityEvent.TYPE_VIEW_SELECTED),
        )
    }

    @Test
    fun `選択中候補が変わらないときはアクセシビリティイベントを送出しない`() {
        val wheel = buildWheel(initialIndex = 4)
        val sentEventTypes = recordSentAccessibilityEvents(wheel)

        // 末尾候補では次候補へ移動しないため、公開状態も変化しない。
        wheel.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null)

        assertTrue("選択が変わっていないのに通知された", sentEventTypes.isEmpty())
    }

    /** [wheel] が送出したアクセシビリティイベントの種別を記録するリストを返す。 */
    private fun recordSentAccessibilityEvents(wheel: KsWheelView): List<Int> {
        val sentEventTypes = mutableListOf<Int>()
        wheel.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun sendAccessibilityEvent(host: View, eventType: Int) {
                sentEventTypes.add(eventType)
                super.sendAccessibilityEvent(host, eventType)
            }
        }
        return sentEventTypes
    }

    private fun hasAction(info: AccessibilityNodeInfo, actionId: Int): Boolean =
        info.actionList.any { it.id == actionId }

    private companion object {
        const val ACCENT: Int = 0xFFCC9900.toInt()
        const val ITEM_TEXT: Int = 0xFF1C1B1F.toInt()

        /** 行間で止めるドラッグの移動量（行高に対する比）。 */
        const val INTER_ROW_RATIO: Float = 0.6f

        /** 指を離す直前に静止させる時間（ms）。fling の速度を 0 にする。 */
        const val FINGER_HOLD_MS: Long = 200L

        /** 慣性移動を再現するときの移動量（行数）。 */
        const val FLING_ROW_COUNT: Int = 6
    }
}

/** 実機に近い画面条件（レイアウト実測を伴うテスト用）。 */
private const val DEVICE_QUALIFIERS = "w411dp-h891dp-xxhdpi"
