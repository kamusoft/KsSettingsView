package jp.kamusoft.kssettingsview.ui

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * Store 経路で `CellStyle` だけを差し替えた Cell が、表示中の行へ反映されることの検証。
 *
 * 外観の変更を受けて明示色を切り替える利用者は、Store 経路では `replaceCell` で style を
 * 差し替えた Cell に置き換える。この手段が成立する条件が「style だけの置換が行へ届き、
 * Section / Cell の identity が保たれる」ことなので、行の実描画色と、その置換で Adapter が
 * 発行した通知（内容更新だけで、行の削除・挿入を含まないこと）を観測する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StoreCellStyleReplaceTest {

    /** `KsSettingsView` を 1 つだけ載せるホスト Activity。 */
    class HostActivity : FragmentActivity() {
        lateinit var container: FrameLayout

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(MaterialR.style.Theme_Material3_Light_NoActionBar)
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

    private val before = Color(0xFF112233)
    private val after = Color(0xFFEEDDCC)

    @Test
    @Config(qualifiers = "notnight")
    fun `Store 経路で style だけ差し替えた Cell は行に反映される`() {
        val root = SettingsRoot(
            sections = listOf(
                Section(
                    id = "s1",
                    cells = listOf(
                        LabelCell(id = "c1", title = "テキスト", style = CellStyle(titleColor = before)),
                    ),
                ),
            ),
        )
        val store = SettingsRootStore(initialRoot = root)
        val view = showView(store)
        assertEquals(before.toArgb(), requireTitleColorOfRow(view))
        val observer = ChangeRecordingObserver()
        view.internalMainListAdapter().registerAdapterDataObserver(observer)

        store.replaceCell(
            cellId = "c1",
            new = LabelCell(id = "c1", title = "テキスト", style = CellStyle(titleColor = after)),
        )
        awaitConvergence(
            view,
            extraDiagnostics = { "現在の title 色: ${titleColorOfRow(view)}" },
        ) { titleColorOfRow(view) == after.toArgb() }
        view.internalMainListAdapter().unregisterAdapterDataObserver(observer)
        layout(view)

        assertEquals("style だけの置換が行へ届いていない", after.toArgb(), requireTitleColorOfRow(view))
        assertEquals(listOf("s1"), view.internalRoot().sections.map { it.id })
        assertEquals(
            "Cell の identity は変わらない",
            listOf("c1"),
            view.internalRoot().sections.flatMap { s -> s.cells.map { it.id } },
        )
        // 内部 root の id 一覧はテスト自身が同じ id を渡している以上、行が作り直されても通る。
        // 行の identity が保たれたことは、Adapter が発行した通知に削除・挿入が無いことで見る。
        assertEquals(
            "style だけの置換で行が作り直されている（内容更新以外の通知が発行された）",
            emptyList<String>(),
            observer.structuralNotifications,
        )
        assertTrue(
            "style の差分が内容更新として発行されていない (通知列: ${observer.notifications})",
            observer.payloads.contains(KsSettingsView.PAYLOAD_CONTENT),
        )
    }

    private fun showView(store: SettingsRootStore): KsSettingsView {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()
        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.bind(store)
        awaitConvergence(view) { view.internalMainListAdapter().currentList.isNotEmpty() }
        layout(view)
        return view
    }

    private fun layout(view: KsSettingsView) {
        val metrics = view.resources.displayMetrics
        val height = metrics.heightPixels * 3
        view.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, metrics.widthPixels, height)
        idle()
    }

    /**
     * 表示中の LabelCell 行のタイトル文字色（実描画値）。行がまだ無ければ null。
     *
     * 待機の述語と診断はこの値を「まだ収束していない」として扱えるよう null 許容にする。
     * 行の生成前に例外を投げると、待機ループが待たずに中断してしまう。
     */
    private fun titleColorOfRow(view: KsSettingsView): Int? =
        labelRowHolder(view)?.views?.titleView?.currentTextColor

    /** 収束後の検証用に、表示中の LabelCell 行のタイトル文字色を非 null で取り出す。 */
    private fun requireTitleColorOfRow(view: KsSettingsView): Int =
        requireNotNull(titleColorOfRow(view)) { "LabelCell 行が生成されていません: ${rowHolders(view)}" }

    /** 表示中の LabelCell 行の ViewHolder。まだ生成されていなければ null。 */
    private fun labelRowHolder(view: KsSettingsView): LabelCellViewHolder? =
        rowHolders(view).filterIsInstance<LabelCellViewHolder>().singleOrNull()

    /** RecyclerView に並んでいる行の ViewHolder。 */
    private fun rowHolders(view: KsSettingsView): List<RecyclerView.ViewHolder> {
        val rv = view.internalRecyclerView()
        return (0 until rv.childCount).map { rv.getChildViewHolder(rv.getChildAt(it)) }
    }
}
