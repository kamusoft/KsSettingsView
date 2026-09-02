package jp.kamusoft.kssettingsview.ui

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
 * [TimePickerCell] の `is24Hour` を Store の公開操作で変更したとき、Store → Host → 行の再バインドの
 * 実経路を通って次に開く選択面の系列構成へ届くことを検証する。
 *
 * `is24Hour` は生成後の変更が表示へ反映される動的反映プロパティであり、Store 経路と DSL 経路の
 * 双方に反映テストを持つ（core/ADR-0018 の対称テスト）。DSL 経路側は Compose の
 * `DSLTimePickerHourCycleRenderingTest` が受け持つ。
 *
 * ViewHolder へ直に bind するのではなく `SettingsRootStore.replaceCell` から流すことで、Store 通知の
 * 段で `is24Hour` の変化が取りこぼされる無音の失敗を検出できる状態にする。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ja")
class TimePickerHourCycleStoreUpdateTest {

    /** [KsSettingsView] を載せる器だけを持つホスト Activity。 */
    class HostActivity : ComponentActivity() {
        lateinit var container: FrameLayout

        override fun onCreate(savedInstanceState: Bundle?) {
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

    @Test
    fun `Store の内容更新で is24Hour の変更が次に開く選択面へ届く`() {
        val cell = TimePickerCell(id = "tp", title = "就寝", time = LocalTime.of(22, 15))
        val h = startHarness(cell)

        val before = h.tapRow()
        assertTrue("更新前の選択面が 24 時間制になっていない", before.candidates.is24Hour)
        before.dismiss()

        h.replaceCell(cell.copy(is24Hour = false))

        val after = h.tapRow()
        assertFalse("Store 経由の is24Hour 変更が選択面へ届いていない", after.candidates.is24Hour)
        assertNotNull("12時間制で午前午後系列が提示されていない", after.periodWheel)
    }

    // MARK: - 実経路の足場

    /** TimePickerCell 1 行だけの設定画面を Activity 上に組み立てる。 */
    private fun startHarness(cell: TimePickerCell): Harness {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()

        val store = SettingsRootStore(
            initialRoot = SettingsRoot(sections = listOf(Section(id = "s1", cells = listOf(cell)))),
        )
        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.bind(store)
        idle()

        return Harness(activity, view, store).apply { layout() }
    }

    /** [startHarness] が組み立てた設定画面。 */
    private inner class Harness(
        val activity: HostActivity,
        val view: KsSettingsView,
        val store: SettingsRootStore,
    ) {
        /** レイアウトを走らせ、RecyclerView に行を生成・再バインドさせる。 */
        fun layout() {
            val metrics = activity.resources.displayMetrics
            view.measure(
                View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        }

        /**
         * 同一 id の Cell を [updated] で置き換え、行の再バインドまで進める。
         *
         * `submitList` の差分計算はバックグラウンドで走るため、コミット完了を待ってから
         * レイアウトを走らせる。
         */
        fun replaceCell(updated: TimePickerCell) {
            store.replaceCell(updated.id, updated)
            awaitDifferCommit({ committedSummary() }) { committedCell(updated.id) == updated }
            layout()
        }

        /** 先頭の Cell 行をタップし、提示された選択面を返す。 */
        fun tapRow(): TimeSelectionSheet {
            val rv = view.internalRecyclerView()
            val holder = rv.findViewHolderForAdapterPosition(0)
            assertTrue(
                "TimePickerCell 行の ViewHolder が生成されていない (実際: $holder)",
                holder is TimePickerCellViewHolder,
            )
            holder!!.itemView.performClick()
            return requireNotNull(ShadowDialog.getLatestDialog() as? TimeSelectionSheet) {
                "行タップで選択面が提示されていない"
            }
        }

        /** Adapter がコミット済みの平坦リストにある [cellId] の TimePickerCell。 */
        fun committedCell(cellId: String): TimePickerCell? =
            view.internalMainListAdapter().currentList
                .filterIsInstance<CellListItem.CellRow>()
                .map { it.cell }
                .filterIsInstance<TimePickerCell>()
                .firstOrNull { it.id == cellId }

        /** コミット済みの平坦リストを、失敗メッセージ用に要約する。 */
        fun committedSummary(): List<String> =
            view.internalMainListAdapter().currentList.map { item ->
                when (item) {
                    is CellListItem.CellRow ->
                        "cell(is24Hour=${(item.cell as? TimePickerCell)?.is24Hour})"
                    is CellListItem.SectionHeader -> "header"
                    is CellListItem.SectionFooter -> "footer"
                }
            }
    }
}
