package jp.kamusoft.kssettingsview.ui

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import java.time.LocalDate

/**
 * ボトムシート型の選択面（[PickerSelectionSheet] / [NumberSelectionSheet] / [DateSelectionSheet]）が、
 * それを開いた行のホストの破棄に追随して閉じることを検証する。
 *
 * 選択面の window はホストの window に紐づくため、ホストが壊れるときに畳まないと閉じる主体を
 * 失った window だけが残る。行はホストの View 階層へ実際に取り付け、行から lifecycle をたどる
 * 配線ごと観測する。閉じるのは非確定の閉じ方であり、値の通知は発生しない。
 *
 * [TimeSelectionSheet] の同じ性質は `TimeSelectionSheetTest` が受け持つ。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ja")
class SheetHostDestructionTest {

    /** `FragmentActivity` ではないホスト。 */
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

    private fun launch(): ActivityController<HostActivity> =
        Robolectric.buildActivity(HostActivity::class.java).setup().also { controller = it }

    /** ホストの View 階層へ取り付けた [row] をタップし、表示された選択面を返す。 */
    private fun tap(row: View): Dialog {
        row.performClick()
        return requireNotNull(ShadowDialog.getLatestDialog()) { "行タップで選択面が提示されていない" }
    }

    /** ホストの破棄をまたいだ選択面の状態を確かめる。 */
    private fun assertClosedByDestruction(
        controller: ActivityController<HostActivity>,
        sheet: Dialog,
        notified: List<Any>,
    ) {
        assertTrue("前提: 選択面が表示状態になっていない", sheet.isShowing)

        controller.recreate()

        assertFalse("ホストの破棄後も選択面が開いたまま残っている", sheet.isShowing)
        assertTrue("閉じる際に通知された: $notified", notified.isEmpty())
    }

    @Test
    fun `候補選択面はホストの破棄で閉じられ通知しない`() {
        val notified = mutableListOf<Int>()
        val ctrl = launch()
        val holder = PickerCellViewHolder.create(ctrl.get().container)
        ctrl.get().container.addView(holder.views.root)
        holder.bind(
            PickerCell(
                title = "色",
                items = listOf("赤", "青", "緑"),
                selectedIndex = 0,
                onSelectionChanged = { notified.add(it) },
            ),
            Theme(),
        )

        assertClosedByDestruction(ctrl, tap(holder.views.root), notified)
    }

    @Test
    fun `数値選択面はホストの破棄で閉じられ通知しない`() {
        val notified = mutableListOf<Int>()
        val ctrl = launch()
        val holder = NumberPickerCellViewHolder.create(ctrl.get().container)
        ctrl.get().container.addView(holder.views.root)
        holder.bind(
            NumberPickerCell(
                title = "サイズ",
                min = 0,
                max = 10,
                onValueChanged = { notified.add(it) },
            ),
            Theme(),
        )

        assertClosedByDestruction(ctrl, tap(holder.views.root), notified)
    }

    @Test
    fun `日付ホイール選択面はホストの破棄で閉じられ通知しない`() {
        val notified = mutableListOf<LocalDate>()
        val ctrl = launch()
        val holder = DatePickerCellViewHolder.create(ctrl.get().container)
        ctrl.get().container.addView(holder.views.root)
        holder.bind(
            DatePickerCell(
                title = "誕生日",
                date = LocalDate.of(2026, 6, 15),
                minDate = LocalDate.of(2026, 1, 1),
                maxDate = LocalDate.of(2026, 12, 31),
                uiStyle = DatePickerUIStyle.Spinner,
                onValueChanged = { notified.add(it) },
            ),
            Theme(),
        )

        assertClosedByDestruction(ctrl, tap(holder.views.root), notified)
    }
}
