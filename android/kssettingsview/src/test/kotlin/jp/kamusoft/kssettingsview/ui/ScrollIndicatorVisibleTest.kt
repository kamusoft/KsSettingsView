package jp.kamusoft.kssettingsview.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * [Theme.scrollIndicatorVisible] が内部 `RecyclerView` の縦スクロールバーへ届くことを検証する。
 *
 * 表示フラグ（`isVerticalScrollBarEnabled`）だけでは描画されない点が本件の要であるため、
 * フラグの追従に加えて **スクロールバーの描画状態が初期化されていること** も観測する。View は
 * 構築時に `android:scrollbars` が `none` 以外のときだけ ScrollabilityCache を作り、作られて
 * いなければ `getVerticalScrollbarThumbDrawable()` は `null` を返す。すなわちこの drawable の
 * 有無が「フラグを立てれば描かれる View になっているか」の観測点になる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScrollIndicatorVisibleTest {

    /** [KsSettingsView] を 1 つだけ載せるホスト Activity。 */
    class HostActivity : FragmentActivity() {
        lateinit var container: FrameLayout
        lateinit var settingsView: KsSettingsView

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(MaterialR.style.Theme_Material3_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            container = FrameLayout(this)
            setContentView(container)
            settingsView = KsSettingsView(this)
            container.addView(settingsView)
        }
    }

    private var controller: ActivityController<HostActivity>? = null

    @After
    fun tearDown() {
        controller?.close()
        controller = null
    }

    private fun host(): HostActivity {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        return ctrl.get()
    }

    private fun sampleRoot(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(
                id = "s1",
                cells = listOf(LabelCell(id = "c1", title = "A")),
            ),
        ),
    )

    @Test
    fun `既定 Theme の適用で縦スクロールバーが有効になる`() {
        val view = host().settingsView
        val recyclerView = view.internalRecyclerView()

        // 先に無効化してから既定 Theme を当て直す。`android:scrollbars` が指定された View は構築時点で
        // 縦スクロールバーが有効なので、初期状態をそのまま見ると Theme の適用経路を通らずに通ってしまう。
        view.theme = Theme(scrollIndicatorVisible = false)
        idle()
        assertFalse("前提: 無効化が反映されている", recyclerView.isVerticalScrollBarEnabled)

        view.theme = Theme()
        idle()
        assertTrue(
            "Theme.scrollIndicatorVisible の既定は true なので、適用すると縦スクロールバーが有効になる",
            recyclerView.isVerticalScrollBarEnabled,
        )
    }

    @Test
    fun `内部 RecyclerView はスクロールバーの描画状態を持つ`() {
        val activity = host()
        val recyclerView = activity.settingsView.internalRecyclerView()

        assertNotNull(
            "設定リスト用の recyclerViewStyle 経由で android:scrollbars が届き、" +
                "スクロールバーの描画状態が初期化されている",
            recyclerView.verticalScrollbarThumbDrawable,
        )

        // ホストの Context から直接作った RecyclerView との対比。設定リスト用の Context を通さないと
        // 描画状態が作られないことを示し、本テストが Context の差し替えの回帰を検出できることを担保する。
        assertNull(
            "ホスト Context から生成した RecyclerView はスクロールバーの描画状態を持たない",
            RecyclerView(activity).verticalScrollbarThumbDrawable,
        )
    }

    /**
     * 内部 `RecyclerView` を作った Context 自身が、スクロールバー用の style を運んでいること。
     *
     * 「描画状態を持つか」だけを見ると、実行時に thumb を代入する経路でも満たせてしまい、生成に使う
     * Context の差し替えを検出できない。ここでは同じ Context からもう 1 つ `RecyclerView` を作り、
     * 実行時の代入を一切受けていない View が描画状態を持つかどうかで、Context 自体を観測する。
     */
    @Test
    fun `内部 RecyclerView の Context がスクロールバー用の style を運んでいる`() {
        val activity = host()
        val listContext = activity.settingsView.internalRecyclerView().context

        assertNotNull(
            "設定リストの Context から作った RecyclerView はスクロールバーの描画状態を持つ",
            RecyclerView(listContext).verticalScrollbarThumbDrawable,
        )
        assertNull(
            "前提: ホストの Context から作った RecyclerView は描画状態を持たない",
            RecyclerView(activity).verticalScrollbarThumbDrawable,
        )
    }

    @Test
    fun `scrollIndicatorVisible が false の Store を bind すると縦スクロールバーが無効になる`() {
        val view = host().settingsView

        view.bind(
            SettingsRootStore(
                initialRoot = sampleRoot(),
                initialTheme = Theme(scrollIndicatorVisible = false),
            ),
        )
        idle()

        assertFalse(
            "Theme.scrollIndicatorVisible = false は縦スクロールバーの無効化として反映される",
            view.internalRecyclerView().isVerticalScrollBarEnabled,
        )
    }

    /**
     * 表示を確定した後の Store 更新でも、縦スクロールバーの表示が追従すること。
     *
     * 最初から `false` の Store を bind するだけでは、Store の `theme` の変化を購読して表示へ届ける
     * 経路（`applyTheme` → collect → 反映）を通らない。表示中の更新を撃って観測する。
     */
    @Test
    fun `表示中の Store の Theme 更新で縦スクロールバーが無効になる`() {
        val activity = host()
        val view = activity.settingsView
        val recyclerView = view.internalRecyclerView()
        val store = SettingsRootStore(initialRoot = sampleRoot(), initialTheme = Theme())

        view.bind(store)
        awaitConvergence(view) { view.internalMainListAdapter().currentList.isNotEmpty() }
        layout(view)
        assertTrue("前提: 既定の Store では有効", recyclerView.isVerticalScrollBarEnabled)

        store.applyTheme(Theme(scrollIndicatorVisible = false))
        awaitConvergence(
            view = view,
            extraDiagnostics = { "isVerticalScrollBarEnabled=${recyclerView.isVerticalScrollBarEnabled}" },
        ) { !recyclerView.isVerticalScrollBarEnabled }

        assertFalse(
            "表示中の Store の Theme 更新が縦スクロールバーへ届く",
            recyclerView.isVerticalScrollBarEnabled,
        )
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `外観の切り替えでスクロールバーの thumb が解決し直される`() {
        val activity = host()
        val view = activity.settingsView
        val recyclerView = view.internalRecyclerView()

        val lightThumb = requireNotNull(recyclerView.verticalScrollbarThumbDrawable) {
            "前提: 構築時にスクロールバーの描画状態が作られている"
        }

        // Activity を再生成しないホストで夜間モードへ切り替わる経路。
        RuntimeEnvironment.setQualifiers("+night")
        view.dispatchConfigurationChanged(view.resources.configuration)
        idle()

        val nightThumb = requireNotNull(recyclerView.verticalScrollbarThumbDrawable)
        val expected = darkScrollbarThumb(activity)
        assertEquals(
            "夜間モードでは同梱テーマがダーク側で解決する thumb になる",
            expected.constantState,
            nightThumb.constantState,
        )
        assertNotEquals(
            "前提: thumb はライトとダークで別の drawable として解決される",
            lightThumb.constantState,
            expected.constantState,
        )
    }

    /** 現在の（夜間モードへ切り替えた後の）同梱テーマが解決するスクロールバー thumb。 */
    private fun darkScrollbarThumb(activity: HostActivity): Drawable {
        val attrs = intArrayOf(android.R.attr.scrollbarThumbVertical)
        val typed = activity.ksThemedContext().obtainStyledAttributes(attrs)
        return try {
            requireNotNull(typed.getDrawable(0)) { "同梱テーマが thumb を解決しない" }
        } finally {
            typed.recycle()
        }
    }

    @Test
    fun `theme の差し替えに縦スクロールバーの有効無効が追従する`() {
        val view = host().settingsView
        val recyclerView = view.internalRecyclerView()

        view.theme = Theme(scrollIndicatorVisible = false)
        idle()
        assertFalse(
            "theme 代入で false へ追従する",
            recyclerView.isVerticalScrollBarEnabled,
        )

        view.theme = Theme(scrollIndicatorVisible = true)
        idle()
        assertTrue(
            "theme 代入で true へ戻る",
            recyclerView.isVerticalScrollBarEnabled,
        )
    }

    // MARK: - 構造更新と同時の Theme 適用

    @Test
    fun `構造更新と同時に適用される Theme も反映される`() {
        val view = host().settingsView
        val recyclerView = view.internalRecyclerView()
        assertTrue("前提: 既定では有効", recyclerView.isVerticalScrollBarEnabled)

        // Section 構成の全体更新と Theme 適用が 1 回で届く経路 (`setRootDirect`)。
        view.setRootDirect(sampleRoot(), Theme(scrollIndicatorVisible = false))
        idle()

        assertFalse(
            "構造更新と同時に渡された Theme の scrollIndicatorVisible も反映される",
            recyclerView.isVerticalScrollBarEnabled,
        )
    }

    // MARK: - 行とスクロール位置の維持

    @Test
    fun `スクロールバーの表示だけを変えても行とスクロール位置は維持される`() {
        val activity = host()
        val view = activity.settingsView
        val recyclerView = view.internalRecyclerView()

        view.bind(SettingsRootStore(initialRoot = tallRoot(), initialTheme = Theme()))
        awaitConvergence(view) { view.internalMainListAdapter().currentList.isNotEmpty() }
        layout(view)

        // 画面に収まらない行数を持たせたうえで、途中までスクロールさせる。
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        layoutManager.scrollToPositionWithOffset(6, -24)
        idle()
        layout(view)

        val beforePosition = layoutManager.findFirstVisibleItemPosition()
        val beforeOffset = layoutManager.findViewByPosition(beforePosition)?.top
        val beforeRows = visibleRows(recyclerView)
        val beforeIds = visibleCellIds(view)
        assertTrue("前提: 行が表示されている", beforeRows.isNotEmpty())
        assertTrue("前提: 先頭以外までスクロールしている", beforePosition > 0)

        view.theme = Theme(scrollIndicatorVisible = false)
        idle()
        layout(view)

        assertFalse("前提: 表示設定が変わっている", recyclerView.isVerticalScrollBarEnabled)
        val afterRows = visibleRows(recyclerView)
        assertEquals("表示中の行数が変わらない", beforeRows.size, afterRows.size)
        for ((index, before) in beforeRows.withIndex()) {
            assertSame("表示中の行の View インスタンスが同じ", before, afterRows[index])
        }
        assertEquals("表示中の Section / Cell の ID が同じ", beforeIds, visibleCellIds(view))
        assertEquals(
            "先頭に見えている行の位置が同じ",
            beforePosition,
            layoutManager.findFirstVisibleItemPosition(),
        )
        assertEquals(
            "先頭に見えている行のずれが同じ",
            beforeOffset,
            layoutManager.findViewByPosition(beforePosition)?.top,
        )
    }

    /** 画面に収まらない行数を持つ設定ツリー。 */
    private fun tallRoot(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(
                id = "s1",
                header = SectionAccessory.Text("見出し"),
                cells = (1..40).map { LabelCell(id = "c$it", title = "行 $it") },
            ),
        ),
    )

    /** `RecyclerView` に今ぶら下がっている行の View。 */
    private fun visibleRows(recyclerView: RecyclerView): List<View> =
        (0 until recyclerView.childCount).map { recyclerView.getChildAt(it) }

    /** 表示中の行に対応する Section / Cell の ID。 */
    private fun visibleCellIds(view: KsSettingsView): List<String> {
        val recyclerView = view.internalRecyclerView()
        return (0 until recyclerView.childCount).map { index ->
            val position = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(index))
            when (val item = view.internalMainListAdapter().currentList.getOrNull(position)) {
                is CellListItem.CellRow -> "cell:${item.cell.id}"
                is CellListItem.SectionHeader -> "header:${item.sectionId}"
                is CellListItem.SectionFooter -> "footer:${item.sectionId}"
                null -> "none:$position"
            }
        }
    }

    private fun layout(view: KsSettingsView) {
        val metrics = view.resources.displayMetrics
        view.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        idle()
    }
}
