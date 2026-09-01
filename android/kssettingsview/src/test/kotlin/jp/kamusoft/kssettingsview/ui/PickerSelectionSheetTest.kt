package jp.kamusoft.kssettingsview.ui

import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
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
import java.time.Duration

/**
 * [PickerCell] の選択面（[PickerSelectionSheet]）の提示・確定・破棄・上限・強調色・
 * アクセシビリティ状態を検証する。
 *
 * 選択面は [PickerCellViewHolder] の行タップ経路から開き、実際の配線ごと検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PickerSelectionSheetTest {

    private val ctx: android.content.Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            MaterialR.style.Theme_Material3_Light_NoActionBar,
        )

    private val parent get() = FrameLayout(ctx)

    /** Cell の行タップで選択面を開き、表示された [PickerSelectionSheet] を返す。 */
    private fun openSheet(cell: PickerCell, theme: Theme = Theme()): PickerSelectionSheet {
        val vh = PickerCellViewHolder.create(parent)
        vh.bind(cell, theme)
        vh.views.root.performClick()
        return ShadowDialog.getLatestDialog() as PickerSelectionSheet
    }

    /**
     * ダイアログの実 View 階層（CoordinatorLayout 配下）を画面サイズで measure / layout し、
     * ボトムシートのコンテナを返す。
     */
    private fun layoutDialog(sheet: PickerSelectionSheet): FrameLayout {
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

    // MARK: - 選択面の提示

    @Test
    fun `タイトルは pageTitle を優先して解決する`() {
        val sheet = openSheet(
            PickerCell(
                title = "テーマ",
                pageTitle = "テーマを選択",
                items = listOf("ライト", "ダーク"),
            ),
        )
        assertEquals("テーマを選択", sheet.titleView.text?.toString())
    }

    @Test
    fun `pageTitle が null のときタイトルは title を使う`() {
        val sheet = openSheet(PickerCell(title = "テーマ", items = listOf("ライト", "ダーク")))
        assertEquals("テーマ", sheet.titleView.text?.toString())
    }

    @Test
    fun `候補は items の順序どおり全件列挙される`() {
        val sheet = openSheet(
            PickerCell(
                title = "サイズ",
                items = listOf("10", "20", "30"),
            ),
        )
        assertEquals(3, sheet.listView.adapter?.itemCount)
        assertEquals("10", sheet.bindRow(0).titleView.text?.toString())
        assertEquals("20", sheet.bindRow(1).titleView.text?.toString())
        assertEquals("30", sheet.bindRow(2).titleView.text?.toString())
    }

    @Test
    fun `items が空でも選択面は提示され候補は0件になる`() {
        val sheet = openSheet(PickerCell(title = "空", items = emptyList<String>()))
        assertTrue(sheet.isShowing)
        assertEquals(0, sheet.listView.adapter?.itemCount)
    }

    @Test
    fun `操作ラベルは OS の公開文字列リソースから解決される`() {
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B"),
                selectedIndices = emptySet(),
            ),
        )
        assertEquals(ctx.getString(android.R.string.cancel), sheet.cancelView.text?.toString())
        assertEquals(ctx.getString(android.R.string.ok), sheet.confirmView.text?.toString())
    }

    @Test
    fun `確定ボタンは複数選択モードでのみ表示される`() {
        val single = openSheet(PickerCell(title = "x", items = listOf("A")))
        assertEquals(View.GONE, single.confirmView.visibility)

        val multiple = openSheet(
            PickerCell(title = "x", items = listOf("A"), selectedIndices = emptySet()),
        )
        assertEquals(View.VISIBLE, multiple.confirmView.visibility)
    }

    @Test
    fun `キャンセルボタンでは callback を発火しない`() {
        var multi: Set<Int>? = null
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B"),
                selectedIndices = setOf(0),
                onMultiSelectionChanged = { multi = it },
            ),
        )
        sheet.bindRow(1).root.performClick()
        sheet.cancelView.performClick()

        assertNull(multi)
        assertFalse(sheet.isShowing)
    }

    @Test
    fun `選択面は外側タップで閉じられる設定になっている`() {
        val sheet = openSheet(PickerCell(title = "x", items = listOf("A")))
        assertTrue(shadowOf(sheet as android.app.Dialog).isCancelableOnTouchOutside)
    }

    @Test
    fun `下方向スワイプ相当の非表示遷移で閉じても callback を発火しない`() {
        var received: Set<Int>? = null
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B", "C"),
                selectedIndices = setOf(0),
                onMultiSelectionChanged = { received = it },
            ),
        )
        sheet.bindRow(1).root.performClick()

        // BottomSheetBehavior が非表示状態へ遷移する経路（下方向スワイプの帰着点）を通す。
        val container = sheet.findViewById<FrameLayout>(MaterialR.id.design_bottom_sheet)!!
        val coordinator = container.parent as ViewGroup
        coordinator.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
        )
        coordinator.layout(0, 0, 1080, 1920)
        val behavior = BottomSheetBehavior.from(container)
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
        // 非表示位置への settle アニメーションを完了させる。
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))

        assertEquals(BottomSheetBehavior.STATE_HIDDEN, behavior.state)
        assertNull(received)
        assertFalse(sheet.isShowing)
    }

    @Test
    fun `非確定 dismiss はどの経路でも callback を発火しない`() {
        // 外側タップと Back 操作は Dialog の cancel、下方向スワイプは BottomSheetDialog の
        // 非表示遷移を経て dismiss に至る。いずれの経路も確定 callback を通らないことを確認する。
        var cancelResult: Set<Int>? = null
        val outsideTap = openSheet(multiCell { cancelResult = it })
        outsideTap.bindRow(1).root.performClick()
        outsideTap.cancel()
        assertNull(cancelResult)

        var backResult: Set<Int>? = null
        val back = openSheet(multiCell { backResult = it })
        back.bindRow(1).root.performClick()
        @Suppress("DEPRECATION")
        back.onBackPressed()
        assertNull(backResult)

        var swipeResult: Set<Int>? = null
        val swipe = openSheet(multiCell { swipeResult = it })
        swipe.bindRow(1).root.performClick()
        swipe.dismiss()
        assertNull(swipeResult)
    }

    private fun multiCell(onMulti: (Set<Int>) -> Unit): PickerCell = PickerCell(
        title = "言語",
        items = listOf("A", "B", "C"),
        selectedIndices = setOf(0),
        onMultiSelectionChanged = onMulti,
    )

    // MARK: - ヘッダーのタップ領域・semantics・幅配分

    /** シート内容を指定幅で測定する。 */
    private fun measureContent(sheet: PickerSelectionSheet, widthPx: Int) {
        sheet.contentRoot.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(4000, View.MeasureSpec.AT_MOST),
        )
    }

    private fun desiredWidthOf(view: View): Int {
        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(unspecified, unspecified)
        return view.measuredWidth
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

    /**
     * シート面（ドラッグハンドル付近）をドラッグする。[dy] が負なら上方向。
     */
    private fun dragSheetSurface(
        sheet: PickerSelectionSheet,
        container: FrameLayout,
        dy: Float = -SHEET_DRAG_DISTANCE_PX,
    ) {
        val coordinator = container.parent as ViewGroup
        drag(
            coordinator,
            x = container.width / 2f,
            startY = container.top + SHEET_DRAG_START_OFFSET_PX,
            dy = dy,
        )
    }

    /** [view] の [x], [y]（view 座標）へ DOWN / UP を送ってタップを再現する。 */
    private fun tap(view: View, x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0).let {
            view.dispatchTouchEvent(it)
            it.recycle()
        }
        MotionEvent.obtain(downTime, downTime + 20, MotionEvent.ACTION_UP, x, y, 0).let {
            view.dispatchTouchEvent(it)
            it.recycle()
        }
        idle()
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `確定の有効タップ領域はスロット全域で48dp以上ある`() {
        val minTouchPx = (48 * ctx.resources.displayMetrics.density).toInt()
        var received: Set<Int>? = null
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B"),
                selectedIndices = setOf(1),
                onMultiSelectionChanged = { received = it },
            ),
        )
        layoutDialog(sheet)
        val slot = sheet.confirmSlot

        assertTrue("確定スロット幅 ${slot.width} が $minTouchPx 未満", slot.width >= minTouchPx)
        assertTrue("確定スロット高 ${slot.height} が $minTouchPx 未満", slot.height >= minTouchPx)
        // pill の外側（スロット左上）をタップしても確定が発火する = スロット全域が有効領域。
        assertTrue("pill の外側であること", slot.width > sheet.confirmView.width)
        tap(slot, 1f, 1f)

        assertEquals(setOf(1), received)
        assertFalse(sheet.isShowing)
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `取消の有効タップ領域はスロット全域で48dp以上ある`() {
        val minTouchPx = (48 * ctx.resources.displayMetrics.density).toInt()
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B"),
                selectedIndices = emptySet(),
            ),
        )
        layoutDialog(sheet)
        val slot = sheet.cancelSlot

        assertTrue("取消スロット幅 ${slot.width} が $minTouchPx 未満", slot.width >= minTouchPx)
        assertTrue("取消スロット高 ${slot.height} が $minTouchPx 未満", slot.height >= minTouchPx)
        // ラベルの外側（スロット右下）をタップしても取消が成立する。
        assertTrue("ラベルの外側であること", slot.width > sheet.cancelView.width)
        tap(slot, slot.width - 1f, slot.height - 1f)

        assertFalse(sheet.isShowing)
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `単一選択では確定スロットをタップしても確定 callback は発火しない`() {
        var received: Set<Int>? = null
        val sheet = openSheet(
            PickerCell(
                title = "テーマ",
                items = listOf(PickerItem("A"), PickerItem("B")),
                selectionMode = PickerSelectionMode.Single,
                selectedIndices = setOf(1),
                onMultiSelectionChanged = { received = it },
            ),
        )
        layoutDialog(sheet)
        tap(sheet.confirmSlot, 1f, 1f)

        assertNull(received)
        assertTrue(sheet.isShowing)
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `ヘッダーの総高は承認モック相当に収まる`() {
        val density = ctx.resources.displayMetrics.density
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B"),
                selectedIndices = emptySet(),
            ),
        )
        layoutDialog(sheet)
        val headerHeight = sheet.headerView.height

        // ヘッダー高は 47〜49dp を狙い、最小タップ領域 48dp と両立する範囲に収める。
        assertTrue(
            "ヘッダー高 $headerHeight が想定範囲外",
            headerHeight in (44 * density).toInt()..(52 * density).toInt(),
        )
    }

    @Test
    fun `取消と確定はボタンとしてアクセシビリティへ公開される`() {
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B"),
                selectedIndices = emptySet(),
            ),
        )
        assertEquals(android.widget.Button::class.java.name, nodeInfoOf(sheet.cancelView).className)
        assertEquals(android.widget.Button::class.java.name, nodeInfoOf(sheet.confirmView).className)
    }

    @Test
    fun `幅が足りないときに縮むのはタイトルで操作ラベルは切り詰められない`() {
        val sheet = openSheet(
            PickerCell(
                title = "テーマ",
                pageTitle = "とても長いページタイトルをここに入れて幅を圧迫させる設定項目の選択",
                items = listOf("A", "B"),
                selectedIndices = emptySet(),
            ),
        )
        // 十分な幅で一度測って、スロット幅とタイトルの希望幅を得る。
        measureContent(sheet, 2000)
        val slotWidth = sheet.cancelSlot.measuredWidth
        val cancelDesired = desiredWidthOf(sheet.cancelView)
        val confirmDesired = desiredWidthOf(sheet.confirmView)
        val titleDesired = desiredWidthOf(sheet.titleView)

        // タイトルが収まりきらない幅で測り直す（左右のスロットと余白は確保され、残りがタイトル）。
        val horizontalPadding = 2 * (16 * ctx.resources.displayMetrics.density).toInt()
        val narrow = slotWidth * 2 + horizontalPadding + titleDesired / 2
        measureContent(sheet, narrow)

        assertTrue("取消ラベルが切り詰められた", sheet.cancelView.measuredWidth >= cancelDesired)
        assertTrue("確定ラベルが切り詰められた", sheet.confirmView.measuredWidth >= confirmDesired)
        assertTrue("タイトルが縮んでいない", sheet.titleView.measuredWidth < titleDesired)
        assertEquals("左右スロットが非対称", sheet.cancelSlot.measuredWidth, sheet.confirmSlot.measuredWidth)
    }

    @Test
    fun `単一選択でも左右スロットは対称でタイトルが中央に来る`() {
        val sheet = openSheet(PickerCell(title = "テーマ", items = listOf("A", "B")))
        measureContent(sheet, 1080)
        assertEquals(sheet.cancelSlot.measuredWidth, sheet.confirmSlot.measuredWidth)
    }

    @Test
    fun `対称幅が収まらない狭幅では左右を固有幅へ縮退させ操作ラベルを画面内に収める`() {
        val sheet = openSheet(
            PickerCell(
                title = "テーマ",
                pageTitle = "とても長いページタイトル",
                items = listOf("A", "B"),
                selectedIndices = emptySet(),
            ),
        )
        // 大きな文字サイズ・長いロケール文字列に相当する条件を、取消ラベルの文字列長で作る。
        sheet.cancelView.text = "キャンセルする".repeat(20)

        // 320dp 相当の狭幅でレイアウトする。
        val width = (320 * ctx.resources.displayMetrics.density).toInt()
        measureContent(sheet, width)
        sheet.contentRoot.layout(0, 0, width, sheet.contentRoot.measuredHeight)

        val header = sheet.headerView
        assertTrue(
            "取消スロットがヘッダー左端より外にある",
            sheet.cancelSlot.left >= header.paddingLeft,
        )
        assertTrue(
            "確定スロットがヘッダー右端からはみ出している (${sheet.confirmSlot.right} > ${header.width - header.paddingRight})",
            sheet.confirmSlot.right <= header.width - header.paddingRight,
        )
        assertTrue("取消ラベルが切り詰められた", sheet.cancelView.width >= desiredWidthOf(sheet.cancelView))
        assertTrue("確定ラベルが切り詰められた", sheet.confirmView.width >= desiredWidthOf(sheet.confirmView))
        // 対称幅では収まらないため縮退している。
        assertTrue(
            "縮退していない (左右が対称のまま)",
            sheet.cancelSlot.width != sheet.confirmSlot.width,
        )
    }

    // MARK: - シート面

    @Test
    fun `確定ボタンの文字色は Theme の backgroundColor で描画される`() {
        val theme = Theme(
            backgroundColor = Color(0xFFF2EFE6),
            cellAccentColor = Color(0xFFCC9900),
        )
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B"),
                selectedIndices = emptySet(),
            ),
            theme = theme,
        )
        assertEquals(Color(0xFFF2EFE6).toArgb(), sheet.confirmView.currentTextColor)
    }

    @Test
    fun `取消ボタンの文字色と選択印は強調色で描画される`() {
        val theme = Theme(
            backgroundColor = Color(0xFFF2EFE6),
            cellAccentColor = Color(0xFFCC9900),
        )
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B"),
                selectedIndices = setOf(0),
            ),
            theme = theme,
        )
        assertEquals(Color(0xFFCC9900).toArgb(), sheet.cancelView.currentTextColor)
        assertEquals(Color(0xFFCC9900).toArgb(), sheet.bindRow(0).checkView.color)
    }

    @Test
    fun `シート面の色は Theme の cellBackgroundColor で tint される`() {
        val theme = Theme(cellBackgroundColor = Color(0xFF102030))
        val sheet = openSheet(PickerCell(title = "x", items = listOf("A")), theme = theme)
        val container = sheet.findViewById<FrameLayout>(MaterialR.id.design_bottom_sheet)!!
        assertEquals(Color(0xFF102030).toArgb(), container.backgroundTintList?.defaultColor)
    }

    // MARK: - 高さ

    @Test
    @Config(sdk = [33], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `初期表示の高さはコンテンツ高で画面高の約半分を超えない`() {
        val screenHeight = ctx.resources.displayMetrics.heightPixels
        val half = screenHeight / 2

        val few = openSheet(PickerCell(title = "テーマ", items = listOf("A", "B", "C")))
        val fewPeek = peekHeightOf(few)
        assertTrue("項目少数のシートはコンテンツ高に収まる", fewPeek in 1..half)

        val many = openSheet(PickerCell(title = "テーマ", items = (1..80).map { "項目 $it" }))
        val manyPeek = peekHeightOf(many)
        assertTrue("項目多数でも画面高の約半分を超えない", manyPeek <= half)
        assertTrue("項目多数では上限まで使う", manyPeek > fewPeek)
    }

    private fun peekHeightOf(sheet: PickerSelectionSheet): Int {
        val container = sheet.findViewById<FrameLayout>(MaterialR.id.design_bottom_sheet)!!
        return com.google.android.material.bottomsheet.BottomSheetBehavior.from(container).peekHeight
    }

    // MARK: - 単一選択の即時確定

    @Test
    fun `単一選択は項目タップで即確定して閉じる`() {
        val received = mutableListOf<Int>()
        val sheet = openSheet(
            PickerCell(
                title = "テーマ",
                items = listOf("ライト", "ダーク", "自動"),
                selectedIndex = 0,
                onSelectionChanged = { received.add(it) },
            ),
        )
        sheet.bindRow(2).root.performClick()

        assertEquals(listOf(2), received)
        assertFalse(sheet.isShowing)
    }

    @Test
    fun `単一選択は selectedIndex の項目にのみ選択印を表示する`() {
        val sheet = openSheet(
            PickerCell(
                title = "テーマ",
                items = listOf("ライト", "ダーク", "自動"),
                selectedIndex = 1,
            ),
        )
        assertFalse(sheet.bindRow(0).checkView.isChecked)
        assertTrue(sheet.bindRow(1).checkView.isChecked)
        assertFalse(sheet.bindRow(2).checkView.isChecked)
    }

    @Test
    fun `単一選択の範囲外 selectedIndex では選択印を表示しない`() {
        val sheet = openSheet(
            PickerCell(title = "テーマ", items = listOf("ライト", "ダーク"), selectedIndex = 5),
        )
        assertFalse(sheet.bindRow(0).checkView.isChecked)
        assertFalse(sheet.bindRow(1).checkView.isChecked)
    }

    // MARK: - 複数選択の確定・破棄と上限

    @Test
    fun `複数選択は確定操作で作業状態を1回だけ発火して閉じる`() {
        val received = mutableListOf<Set<Int>>()
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B", "C", "D"),
                selectedIndices = setOf(1),
                onMultiSelectionChanged = { received.add(it) },
            ),
        )
        sheet.bindRow(3).root.performClick()
        sheet.confirmView.performClick()

        assertEquals(listOf(setOf(1, 3)), received)
        assertFalse(sheet.isShowing)
    }

    @Test
    fun `複数選択の項目タップは callback を発火せず作業状態のみ変える`() {
        var received: Set<Int>? = null
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B", "C"),
                selectedIndices = setOf(0),
                onMultiSelectionChanged = { received = it },
            ),
        )
        sheet.bindRow(2).root.performClick()

        assertNull(received)
        assertEquals(setOf(0, 2), sheet.currentWorkingSelection())
    }

    @Test
    fun `複数選択のキャンセルは作業状態を破棄する`() {
        var received: Set<Int>? = null
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B", "C"),
                selectedIndices = setOf(0),
                onMultiSelectionChanged = { received = it },
            ),
        )
        sheet.bindRow(1).root.performClick()
        sheet.cancelView.performClick()

        assertNull(received)
    }

    @Test
    fun `上限到達時は新規チェックを無視して拒否の触覚フィードバックを要求する`() {
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B", "C", "D"),
                selectedIndices = setOf(0, 1, 2),
                maxSelectedNumber = 3,
            ),
        )
        val row = sheet.bindRow(3)
        row.root.performClick()

        assertFalse(row.checkView.isChecked)
        assertEquals(setOf(0, 1, 2), sheet.currentWorkingSelection())
        assertEquals(HapticFeedbackConstants.REJECT, shadowOf(row.root).lastHapticFeedbackPerformed())
    }

    @Test
    fun `拒否の触覚フィードバックが受け付けられなければ代替を要求する`() {
        val sheet = openSheet(limitReachedCell())
        val requested = mutableListOf<Int>()
        sheet.hapticRequest = { _, constant ->
            requested.add(constant)
            false
        }
        sheet.bindRow(3).root.performClick()

        assertEquals(
            listOf(HapticFeedbackConstants.REJECT, HapticFeedbackConstants.KEYBOARD_TAP),
            requested,
        )
    }

    @Test
    fun `拒否の触覚フィードバックが受け付けられれば代替は要求しない`() {
        val sheet = openSheet(limitReachedCell())
        val requested = mutableListOf<Int>()
        sheet.hapticRequest = { _, constant ->
            requested.add(constant)
            true
        }
        sheet.bindRow(3).root.performClick()

        assertEquals(listOf(HapticFeedbackConstants.REJECT), requested)
    }

    private fun limitReachedCell(): PickerCell = PickerCell(
        title = "言語",
        items = listOf("A", "B", "C", "D"),
        selectedIndices = setOf(0, 1, 2),
        maxSelectedNumber = 3,
    )

    @Test
    fun `上限到達時もチェック解除は可能`() {
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B", "C", "D"),
                selectedIndices = setOf(0, 1, 2),
                maxSelectedNumber = 3,
            ),
        )
        val row = sheet.bindRow(1)
        row.root.performClick()

        assertFalse(row.checkView.isChecked)
        assertEquals(setOf(0, 2), sheet.currentWorkingSelection())
    }

    // MARK: - モデル値の許容と非正規化

    @Test
    fun `範囲外 index は作業状態に保持され確定 callback にも残る`() {
        var received: Set<Int>? = null
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B", "C"),
                selectedIndices = setOf(1, 5),
                maxSelectedNumber = 0,
                onMultiSelectionChanged = { received = it },
            ),
        )
        sheet.bindRow(2).root.performClick()
        sheet.confirmView.performClick()

        assertEquals(setOf(1, 2, 5), received)
    }

    @Test
    fun `初期状態が上限超過でも新規チェックは無視され解除は可能`() {
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B", "C", "D", "E"),
                selectedIndices = setOf(0, 1, 2, 3),
                maxSelectedNumber = 3,
            ),
        )
        val newRow = sheet.bindRow(4)
        newRow.root.performClick()
        assertFalse(newRow.checkView.isChecked)
        assertEquals(setOf(0, 1, 2, 3), sheet.currentWorkingSelection())

        val checkedRow = sheet.bindRow(0)
        checkedRow.root.performClick()
        assertFalse(checkedRow.checkView.isChecked)
        assertEquals(setOf(1, 2, 3), sheet.currentWorkingSelection())
    }

    // MARK: - 選択印の強調色

    @Test
    fun `強調色は Cell 固有値を最優先で解決する`() {
        val sheet = openSheet(
            PickerCell(
                title = "テーマ",
                items = listOf("A", "B"),
                selectedIndex = 0,
                accentColor = Color(0xFFFF0000),
                style = CellStyle(accentColor = Color(0xFF00FF00)),
            ),
            theme = Theme(cellAccentColor = Color(0xFF0000FF)),
        )
        assertEquals(Color(0xFFFF0000).toArgb(), sheet.bindRow(0).checkView.color)
    }

    @Test
    fun `強調色は Cell 固有値が無いとき CellStyle へフォールバックする`() {
        val sheet = openSheet(
            PickerCell(
                title = "テーマ",
                items = listOf("A", "B"),
                selectedIndex = 0,
                style = CellStyle(accentColor = Color(0xFF00FF00)),
            ),
            theme = Theme(cellAccentColor = Color(0xFF0000FF)),
        )
        assertEquals(Color(0xFF00FF00).toArgb(), sheet.bindRow(0).checkView.color)
    }

    @Test
    fun `強調色は Cell 固有値も CellStyle も無いとき Theme へフォールバックする`() {
        val sheet = openSheet(
            PickerCell(title = "テーマ", items = listOf("A", "B"), selectedIndex = 0),
            theme = Theme(cellAccentColor = Color(0xFF0000FF)),
        )
        assertEquals(Color(0xFF0000FF).toArgb(), sheet.bindRow(0).checkView.color)
    }

    // MARK: - 候補行のタイポグラフィ

    @Test
    fun `候補行の文字サイズは Cell タイトルの実効値から解決される`() {
        val theme = Theme(cellTitleFontSize = 22.0)
        val sheet = openSheet(PickerCell(title = "テーマ", items = listOf("A", "B")), theme = theme)
        assertEquals(spToPx(22f), sheet.bindRow(0).titleView.textSize, 0.01f)
    }

    /** [sp] を現在の画面条件の px へ換算する。 */
    private fun spToPx(sp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, ctx.resources.displayMetrics)

    @Test
    fun `ヘッダーの文字サイズは候補行のサイズから導出される`() {
        val theme = Theme(cellTitleFontSize = 22.0)
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B"),
                selectedIndices = emptySet(),
            ),
            theme = theme,
        )
        assertEquals(spToPx(22f), sheet.bindRow(0).titleView.textSize, 0.01f)
        assertEquals(spToPx(23f), sheet.titleView.textSize, 0.01f)
        assertEquals(spToPx(21f), sheet.cancelView.textSize, 0.01f)
        assertEquals(spToPx(21f), sheet.confirmView.textSize, 0.01f)
    }

    @Test
    fun `既定 Theme でもヘッダーの文字サイズは候補行のサイズから導出される`() {
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B"),
                selectedIndices = emptySet(),
            ),
        )
        val itemTextSize = sheet.bindRow(0).titleView.textSize
        assertEquals(itemTextSize + spToPx(1f), sheet.titleView.textSize, 0.01f)
        assertEquals(itemTextSize - spToPx(1f), sheet.cancelView.textSize, 0.01f)
        assertEquals(itemTextSize - spToPx(1f), sheet.confirmView.textSize, 0.01f)
    }

    @Test
    fun `候補行の文字サイズは Cell タイトルと一致する`() {
        val theme = Theme()
        val vh = PickerCellViewHolder.create(parent)
        vh.bind(PickerCell(title = "テーマ", items = listOf("A", "B")), theme)
        vh.views.root.performClick()
        val sheet = ShadowDialog.getLatestDialog() as PickerSelectionSheet

        assertEquals(vh.views.titleView.textSize, sheet.bindRow(0).titleView.textSize, 0.01f)
    }

    // MARK: - 候補行のアクセシビリティ状態

    @Test
    fun `候補行は表示名と選択状態を公開する`() {
        val sheet = openSheet(
            PickerCell(
                title = "テーマ",
                items = listOf("ライト", "ダーク"),
                selectedIndex = 0,
            ),
        )
        val selected = sheet.bindRow(0)
        val unselected = sheet.bindRow(1)

        assertEquals("ライト", selected.root.contentDescription?.toString())
        assertTrue(selected.root.isSelected)
        assertTrue(nodeInfoOf(selected.root).isChecked)

        assertEquals("ダーク", unselected.root.contentDescription?.toString())
        assertFalse(unselected.root.isSelected)
        assertFalse(nodeInfoOf(unselected.root).isChecked)
    }

    @Test
    fun `候補行の公開される選択状態はトグル後に更新される`() {
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = listOf("A", "B", "C"),
                selectedIndices =emptySet(),
            ),
        )
        val row = sheet.bindRow(1)
        assertFalse(nodeInfoOf(row.root).isChecked)

        row.root.performClick()

        assertTrue(row.root.isSelected)
        assertTrue(nodeInfoOf(row.root).isChecked)
    }

    // MARK: - RecyclerView の実 bind 経路

    @Test
    fun `RecyclerView 経由で生成された行にも表示名と選択印が反映される`() {
        val sheet = openSheet(
            PickerCell(
                title = "テーマ",
                items = listOf("ライト", "ダーク", "自動"),
                selectedIndex = 1,
            ),
        )
        sheet.listView.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(4000, View.MeasureSpec.AT_MOST),
        )
        sheet.listView.layout(0, 0, 1080, sheet.listView.measuredHeight)

        assertEquals(3, sheet.listView.childCount)
        val first = sheet.listView.getChildAt(0) as ViewGroup
        val second = sheet.listView.getChildAt(1) as ViewGroup
        assertEquals("ライト", first.contentDescription?.toString())
        assertEquals("ダーク", second.contentDescription?.toString())
        assertFalse(first.isSelected)
        assertTrue(second.isSelected)
        assertFalse((first.getChildAt(1) as KsSimpleCheckView).isChecked)
        assertTrue((second.getChildAt(1) as KsSimpleCheckView).isChecked)
    }

    // MARK: - 初期スクロール位置（実レイアウト検証）

    /**
     * 実ダイアログ階層をレイアウトし、[position] の候補行がシートの可視領域（折り目まで）に
     * 収まっているかを返す。
     */
    private fun isRowWithinPeek(sheet: PickerSelectionSheet, position: Int): Boolean {
        val container = layoutDialog(sheet)
        val peek = com.google.android.material.bottomsheet.BottomSheetBehavior.from(container).peekHeight
        val holder = sheet.listView.findViewHolderForAdapterPosition(position) ?: return false
        val top = sheet.listView.top + holder.itemView.top
        val bottom = sheet.listView.top + holder.itemView.bottom
        return top >= 0 && bottom <= peek
    }

    private fun firstVisiblePositionOf(sheet: PickerSelectionSheet): Int {
        layoutDialog(sheet)
        return (sheet.listView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `単一選択は選択中の項目が可視領域に入った状態で開く`() {
        listOf(8 to 7, 16 to 15, 50 to 30).forEach { (count, selected) ->
            val sheet = openSheet(
                PickerCell(
                    title = "テーマ",
                    items = (1..count).map { "項目 $it" },
                    selectedIndex = selected,
                ),
            )
            assertTrue(
                "候補 $count 件・選択 $selected が可視領域に入っていない",
                isRowWithinPeek(sheet, selected),
            )
        }
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `複数選択は選択中の最小 index が可視領域に入った状態で開く`() {
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = (1..50).map { "項目 $it" },
                selectedIndices = setOf(40, 12, 25),
            ),
        )
        assertTrue(isRowWithinPeek(sheet, 12))
        assertEquals(12, (sheet.listView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition())
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `候補が多いときリストはシート内部でスクロールできる`() {
        val sheet = openSheet(PickerCell(title = "テーマ", items = (1..50).map { "項目 $it" }))
        val container = layoutDialog(sheet)
        val peek = com.google.android.material.bottomsheet.BottomSheetBehavior.from(container).peekHeight

        assertTrue(
            "リストにスクロール余地がない",
            sheet.listView.computeVerticalScrollRange() > sheet.listView.height,
        )
        assertTrue(
            "シート内容が可視領域を超えている (${sheet.contentRoot.height} > $peek)",
            sheet.contentRoot.height <= peek,
        )
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `上方向の操作でリスト高の制約が解除され全展開できる`() {
        var received: Set<Int>? = null
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = (1..50).map { "項目 $it" },
                selectedIndices = setOf(30),
                onMultiSelectionChanged = { received = it },
            ),
        )
        val container = layoutDialog(sheet)
        val behavior = BottomSheetBehavior.from(container)
        val peek = behavior.peekHeight
        val manager = sheet.listView.layoutManager as LinearLayoutManager

        // 初期表示: シート内容は折り目と一致し、選択行が可視領域にある。
        assertEquals(peek, sheet.contentRoot.height)
        assertEquals(30, manager.findFirstVisibleItemPosition())

        // シート面（ドラッグハンドル付近）を上方向へドラッグする。
        dragSheetSurface(sheet, container)
        assertEquals(BottomSheetBehavior.STATE_EXPANDED, behavior.state)
        layoutDialog(sheet)

        assertTrue(
            "シート内容が折り目より伸びていない (${sheet.contentRoot.height} <= $peek)",
            sheet.contentRoot.height > peek,
        )
        assertEquals(
            "展開でスクロール位置が飛んでいる",
            30,
            manager.findFirstVisibleItemPosition(),
        )
        // 全展開中もヘッダーは表示され続ける。
        assertEquals(View.VISIBLE, sheet.headerView.visibility)
        assertTrue(sheet.headerView.height > 0)
        assertTrue(sheet.headerView.bottom <= sheet.contentRoot.height)
        // 全展開後も内部スクロールで全候補へ到達できる。
        assertTrue(
            sheet.listView.computeVerticalScrollRange() > sheet.listView.height,
        )

        // 展開後も確定操作が機能する。
        tap(sheet.confirmSlot, 1f, 1f)
        assertEquals(setOf(30), received)
        assertFalse(sheet.isShowing)
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `展開後の取消では callback を発火しない`() {
        var received: Set<Int>? = null
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = (1..50).map { "項目 $it" },
                selectedIndices = setOf(30),
                onMultiSelectionChanged = { received = it },
            ),
        )
        val container = layoutDialog(sheet)
        dragSheetSurface(sheet, container)
        layoutDialog(sheet)

        tap(sheet.cancelSlot, sheet.cancelSlot.width - 1f, sheet.cancelSlot.height - 1f)

        assertNull(received)
        assertFalse(sheet.isShowing)
    }

    /** 末尾候補まで送ったときの、その行の下端（シート内容座標）。 */
    private fun lastRowBottomAfterScrollToEnd(sheet: PickerSelectionSheet): Int {
        val last = sheet.listView.adapter!!.itemCount - 1
        sheet.listView.scrollToPosition(last)
        layoutDialog(sheet)
        val holder = sheet.listView.findViewHolderForAdapterPosition(last)
            ?: error("末尾候補の行がレイアウトされていない")
        return sheet.listView.top + holder.itemView.bottom
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `ハンドルを短く下へ引いて離した後も末尾の候補へ到達できる`() {
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = (1..50).map { "項目 $it" },
                selectedIndices = emptySet(),
            ),
        )
        var container = layoutDialog(sheet)
        val behavior = BottomSheetBehavior.from(container)
        val peek = behavior.peekHeight
        val constrainedListHeight = sheet.listView.layoutParams.height

        // dismiss 閾値に届かない下方向ドラッグ（掴んで離すだけ）。
        dragSheetSurface(sheet, container, dy = SHORT_DOWN_DRAG_DISTANCE_PX)
        container = layoutDialog(sheet)

        assertTrue(sheet.isShowing)
        assertEquals(BottomSheetBehavior.STATE_COLLAPSED, behavior.state)
        assertEquals("リスト高が可視領域制約へ戻っていない", constrainedListHeight, sheet.listView.layoutParams.height)
        assertTrue(
            "シート内容が折り目を超えている (${sheet.contentRoot.height} > $peek)",
            sheet.contentRoot.height <= peek,
        )
        assertTrue(
            "末尾の候補が可視領域外にある (${lastRowBottomAfterScrollToEnd(sheet)} > $peek)",
            lastRowBottomAfterScrollToEnd(sheet) <= peek,
        )
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `全展開から折り目へ戻すとリスト高が可視領域制約へ戻る`() {
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = (1..50).map { "項目 $it" },
                selectedIndices = emptySet(),
            ),
        )
        var container = layoutDialog(sheet)
        val behavior = BottomSheetBehavior.from(container)
        val peek = behavior.peekHeight
        val constrainedListHeight = sheet.listView.layoutParams.height

        // 全展開してから折り目へ戻す。
        dragSheetSurface(sheet, container)
        container = layoutDialog(sheet)
        assertEquals(BottomSheetBehavior.STATE_EXPANDED, behavior.state)
        assertTrue(sheet.contentRoot.height > peek)

        dragSheetSurface(sheet, container, dy = SHEET_DRAG_DISTANCE_PX)
        container = layoutDialog(sheet)

        assertEquals(BottomSheetBehavior.STATE_COLLAPSED, behavior.state)
        assertEquals(constrainedListHeight, sheet.listView.layoutParams.height)
        assertEquals(peek, sheet.contentRoot.height)
        assertTrue(
            "末尾の候補が可視領域外にある",
            lastRowBottomAfterScrollToEnd(sheet) <= peek,
        )

        // 折り目へ戻したあとも、もう一度ドラッグすれば再び全展開できる。
        dragSheetSurface(sheet, container)
        layoutDialog(sheet)
        assertEquals(BottomSheetBehavior.STATE_EXPANDED, behavior.state)
        assertTrue(sheet.contentRoot.height > peek)
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `ハンドル起点の下方向ドラッグでは callback を発火せず dismiss する`() {
        var received: Set<Int>? = null
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = (1..50).map { "項目 $it" },
                selectedIndices = setOf(0),
                onMultiSelectionChanged = { received = it },
            ),
        )
        val container = layoutDialog(sheet)
        val behavior = BottomSheetBehavior.from(container)

        dragSheetSurface(sheet, container, dy = DISMISS_DOWN_DRAG_DISTANCE_PX)

        assertEquals(BottomSheetBehavior.STATE_HIDDEN, behavior.state)
        assertFalse(sheet.isShowing)
        assertNull(received)
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `候補リストのスクロールではシートを展開しない`() {
        val sheet = openSheet(
            PickerCell(
                title = "言語",
                items = (1..50).map { "項目 $it" },
                selectedIndices = setOf(0),
            ),
        )
        val container = layoutDialog(sheet)
        val behavior = BottomSheetBehavior.from(container)
        val peek = behavior.peekHeight
        val constrainedListHeight = sheet.listView.layoutParams.height

        // 候補リストはスクロールをシートへ伝播しない。
        assertFalse(
            sheet.listView.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH),
        )
        assertFalse(
            sheet.listView.dispatchNestedPreScroll(
                0,
                LIST_DRAG_DISTANCE_PX.toInt(),
                IntArray(2),
                IntArray(2),
                ViewCompat.TYPE_TOUCH,
            ),
        )

        // リスト面を上方向へドラッグする。
        val coordinator = container.parent as ViewGroup
        drag(
            coordinator,
            x = container.width / 2f,
            startY = container.top + sheet.listView.top + LIST_DRAG_START_OFFSET_PX,
            dy = -LIST_DRAG_DISTANCE_PX,
        )

        // リスト内部はスクロールし、シートは折り目のまま。
        assertTrue("リストが内部スクロールしていない", sheet.listView.computeVerticalScrollOffset() > 0)
        assertEquals(BottomSheetBehavior.STATE_COLLAPSED, behavior.state)
        assertEquals(peek, sheet.contentRoot.height)
        assertEquals(constrainedListHeight, sheet.listView.layoutParams.height)
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `未選択のときは先頭から表示する`() {
        val sheet = openSheet(PickerCell(title = "テーマ", items = (1..50).map { "項目 $it" }))
        assertEquals(0, firstVisiblePositionOf(sheet))
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `範囲外 index しかないときは先頭から表示する`() {
        val single = openSheet(
            PickerCell(title = "テーマ", items = (1..50).map { "項目 $it" }, selectedIndex = 99),
        )
        assertEquals(0, firstVisiblePositionOf(single))

        val multiple = openSheet(
            PickerCell(
                title = "言語",
                items = (1..50).map { "項目 $it" },
                selectedIndices = setOf(80, 99),
            ),
        )
        assertEquals(0, firstVisiblePositionOf(multiple))
    }

    // MARK: - 候補行の副表示

    /** 主表示のみ / 主表示 + 副表示 が混在する候補。 */
    private fun mixedItems(): List<PickerItem> = listOf(
        PickerItem("佐藤 花子", "プロダクトマネージャー"),
        PickerItem("全体アナウンス"),
        PickerItem("高橋 次郎", "QA エンジニア"),
    )

    @Test
    fun `subText を持つ候補行は主表示と副表示の両方を表示する`() {
        val sheet = openSheet(
            PickerCell(
                title = "通知先",
                items = listOf(PickerItem("佐藤 花子", "プロダクトマネージャー")),
            ),
        )
        val row = sheet.bindRow(0)

        assertEquals("佐藤 花子", row.titleView.text?.toString())
        assertEquals(View.VISIBLE, row.subTitleView.visibility)
        assertEquals("プロダクトマネージャー", row.subTitleView.text?.toString())
    }

    @Test
    fun `subText を持たない候補行は副表示を持たない`() {
        val sheet = openSheet(
            PickerCell(title = "通知先", items = listOf(PickerItem("全体アナウンス"))),
        )
        val row = sheet.bindRow(0)

        assertEquals("全体アナウンス", row.titleView.text?.toString())
        assertEquals(View.GONE, row.subTitleView.visibility)
    }

    @Test
    fun `混在リストでは subText を持つ行だけが副表示を持つ`() {
        val sheet = openSheet(PickerCell(title = "通知先", items = mixedItems()))

        assertEquals(View.VISIBLE, sheet.bindRow(0).subTitleView.visibility)
        assertEquals(View.GONE, sheet.bindRow(1).subTitleView.visibility)
        assertEquals(View.VISIBLE, sheet.bindRow(2).subTitleView.visibility)
    }

    @Test
    fun `空文字の subText は副表示なしとして扱われる`() {
        val sheet = openSheet(
            PickerCell(title = "通知先", items = listOf(PickerItem("全体アナウンス", ""))),
        )
        assertEquals(View.GONE, sheet.bindRow(0).subTitleView.visibility)
    }

    @Test
    fun `再利用された行では前の候補の副表示が残らない`() {
        val sheet = openSheet(PickerCell(title = "通知先", items = mixedItems()))
        // 同一の行 View を副表示ありの候補 → 副表示なしの候補の順に bind し直す。
        val row = sheet.bindRow(0)
        assertEquals(View.VISIBLE, row.subTitleView.visibility)
        sheet.bindRow(row, 1)

        assertEquals("全体アナウンス", row.titleView.text?.toString())
        assertEquals(View.GONE, row.subTitleView.visibility)
    }

    @Test
    fun `副表示は description 系統の実効値で描画される`() {
        val theme = Theme(
            cellDescriptionColor = Color(0xFF6D6D72),
            cellDescriptionFont = TextStyle(fontSize = 13.sp),
        )
        val sheet = openSheet(
            PickerCell(
                title = "通知先",
                items = listOf(PickerItem("佐藤 花子", "プロダクトマネージャー")),
            ),
            theme = theme,
        )
        val row = sheet.bindRow(0)

        assertEquals(Color(0xFF6D6D72).toArgb(), row.subTitleView.currentTextColor)
        assertEquals(spToPx(13f), row.subTitleView.textSize, 0.01f)
    }

    @Test
    fun `副表示の実効値は CellStyle が Theme より優先される`() {
        val theme = Theme(
            cellDescriptionColor = Color(0xFF6D6D72),
            cellDescriptionFont = TextStyle(fontSize = 13.sp),
        )
        val sheet = openSheet(
            PickerCell(
                title = "通知先",
                items = listOf(PickerItem("佐藤 花子", "プロダクトマネージャー")),
                style = CellStyle(
                    descriptionColor = Color(0xFF0000FF),
                    descriptionFont = TextStyle(fontSize = 18.sp),
                ),
            ),
            theme = theme,
        )
        val row = sheet.bindRow(0)

        assertEquals(Color(0xFF0000FF).toArgb(), row.subTitleView.currentTextColor)
        assertEquals(spToPx(18f), row.subTitleView.textSize, 0.01f)
    }

    @Test
    fun `副表示は1行に収めて末尾を省略する`() {
        val sheet = openSheet(
            PickerCell(
                title = "通知先",
                items = listOf(
                    PickerItem(
                        "鈴木 一郎",
                        "モバイルアプリ開発チーム / テックリード (iOS・Android 横断アーキテクチャ担当)",
                    ),
                ),
            ),
        )
        val row = sheet.bindRow(0)

        assertEquals(1, row.subTitleView.maxLines)
        assertEquals(android.text.TextUtils.TruncateAt.END, row.subTitleView.ellipsize)
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `副表示の長さが変わっても副表示あり行の行高は変わらない`() {
        val sheet = openSheet(
            PickerCell(
                title = "通知先",
                items = listOf(
                    PickerItem("佐藤 花子", "プロダクトマネージャー"),
                    PickerItem(
                        "鈴木 一郎",
                        "モバイルアプリ開発チーム / テックリード (iOS・Android 横断アーキテクチャ担当)",
                    ),
                    PickerItem("全体アナウンス"),
                ),
            ),
        )
        layoutDialog(sheet)

        // 行のテキスト列（主表示 + 副表示）の高さで比較する。行そのものの高さは選択印の寸法にも
        // 律速されるため、副表示の寄与だけを見るにはテキスト列を測る。
        val textsHeightAt = { position: Int ->
            ((sheet.listView.getChildAt(position) as ViewGroup).getChildAt(0) as ViewGroup).height
        }
        val short = textsHeightAt(0)
        val long = textsHeightAt(1)
        val none = textsHeightAt(2)

        assertEquals("副表示の長さで行高が変わっている", short, long)
        assertTrue("副表示あり行が副表示なし行より高くない ($short <= $none)", short > none)
        assertEquals("副表示の長さで行の総高が変わっている", sheet.listView.getChildAt(0).height, sheet.listView.getChildAt(1).height)
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `副表示付きの候補でも折り畳み高さと内部スクロールの契約は保たれる`() {
        val sheet = openSheet(
            PickerCell(
                title = "通知先",
                items = (1..50).map { PickerItem("項目 $it", "補足 $it") },
            ),
        )
        val container = layoutDialog(sheet)
        val peek = BottomSheetBehavior.from(container).peekHeight
        val screenHeight = ctx.resources.displayMetrics.heightPixels

        assertTrue(
            "シート内容が折り目を超えている (${sheet.contentRoot.height} > $peek)",
            sheet.contentRoot.height <= peek,
        )
        assertTrue("折り目が画面の約半分を超えている", peek <= screenHeight / 2)
        assertTrue(
            "リストにスクロール余地がない",
            sheet.listView.computeVerticalScrollRange() > sheet.listView.height,
        )
    }

    @Test
    @Config(sdk = [33], qualifiers = DEVICE_QUALIFIERS)
    fun `副表示が混在しても選択中の項目が可視領域に入った状態で開く`() {
        val sheet = openSheet(
            PickerCell(
                title = "通知先",
                // 3 件に 1 件だけ副表示を持たせ、行高を混在させる。
                items = (1..50).map {
                    if (it % 3 == 0) PickerItem("項目 $it") else PickerItem("項目 $it", "補足 $it")
                },
                selectedIndex = 30,
            ),
        )

        assertTrue("選択中の候補が可視領域に入っていない", isRowWithinPeek(sheet, 30))
        assertEquals(30, (sheet.listView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition())
    }

    @Test
    fun `候補行は副表示も含めてアクセシビリティへ公開される`() {
        val sheet = openSheet(
            PickerCell(
                title = "通知先",
                items = mixedItems(),
                selectionMode = PickerSelectionMode.Multiple,
                selectedIndices = setOf(0),
            ),
        )
        val withSub = sheet.bindRow(0)
        val withoutSub = sheet.bindRow(1)

        assertEquals("佐藤 花子, プロダクトマネージャー", withSub.root.contentDescription?.toString())
        assertEquals("全体アナウンス", withoutSub.root.contentDescription?.toString())
        assertTrue(nodeInfoOf(withSub.root).isChecked)
        assertFalse(nodeInfoOf(withoutSub.root).isChecked)
        // 副表示は行コンテナが一括で読み上げるため、単独のノードとしては公開しない。
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, withSub.subTitleView.importantForAccessibility)
    }

    @Test
    fun `RecyclerView 経由で生成された行にも副表示が反映される`() {
        val sheet = openSheet(PickerCell(title = "通知先", items = mixedItems()))
        sheet.listView.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(4000, View.MeasureSpec.AT_MOST),
        )
        sheet.listView.layout(0, 0, 1080, sheet.listView.measuredHeight)

        assertEquals(3, sheet.listView.childCount)
        val texts = { i: Int -> (sheet.listView.getChildAt(i) as ViewGroup).getChildAt(0) as ViewGroup }
        assertEquals("プロダクトマネージャー", (texts(0).getChildAt(1) as android.widget.TextView).text?.toString())
        assertEquals(View.GONE, texts(1).getChildAt(1).visibility)
        assertEquals(
            "佐藤 花子, プロダクトマネージャー",
            sheet.listView.getChildAt(0).contentDescription?.toString(),
        )
    }

    private fun nodeInfoOf(view: View): AccessibilityNodeInfo {
        val info = AccessibilityNodeInfo.obtain()
        view.onInitializeAccessibilityNodeInfo(info)
        return info
    }

    private companion object {
        /** 実機に近い画面条件（レイアウト実測を伴うテスト用）。 */
        const val DEVICE_QUALIFIERS = "w411dp-h891dp-xxhdpi"

        /** シート面ドラッグの開始位置（シート上端からの距離、px）。ドラッグハンドル付近。 */
        const val SHEET_DRAG_START_OFFSET_PX = 20f

        /** シート面ドラッグの移動量（px）。 */
        const val SHEET_DRAG_DISTANCE_PX = 600f

        /** dismiss 閾値に届かない短い下方向ドラッグの移動量（px）。 */
        const val SHORT_DOWN_DRAG_DISTANCE_PX = 80f

        /** dismiss に至る下方向ドラッグの移動量（px）。 */
        const val DISMISS_DOWN_DRAG_DISTANCE_PX = 1000f

        /** リスト面ドラッグの開始位置（リスト上端からの距離、px）。 */
        const val LIST_DRAG_START_OFFSET_PX = 200f

        /** リスト面ドラッグの移動量（px）。 */
        const val LIST_DRAG_DISTANCE_PX = 400f
    }
}
