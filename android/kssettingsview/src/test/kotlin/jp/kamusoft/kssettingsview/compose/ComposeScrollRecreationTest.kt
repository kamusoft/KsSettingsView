package jp.kamusoft.kssettingsview.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.ui.KsScrollController
import jp.kamusoft.kssettingsview.ui.SCROLL_TEST_HOST_HEIGHT
import jp.kamusoft.kssettingsview.ui.SettingsRootStore
import jp.kamusoft.kssettingsview.ui.Theme
import jp.kamusoft.kssettingsview.ui.cellRow
import jp.kamusoft.kssettingsview.ui.listTop
import jp.kamusoft.kssettingsview.ui.scrollSummary
import jp.kamusoft.kssettingsview.ui.scrollTestRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.fail
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import jp.kamusoft.kssettingsview.ui.KsSettingsView as KsSettingsViewLayout

/**
 * Compose の `KsSettingsView(...)` の中の View Host でも、Activity の作り直しでスクロール位置が戻ることを
 * 検証する。
 *
 * `AndroidView` は中の View の状態を Compose の保存状態の仕組みへ登録し、作り直しで復元する。View Host は
 * 既定の id を持つので、その保存状態にスクロール位置の控えが含まれる。
 *
 * Navigation Compose の `NavHost` は画面ごとに `SaveableStateHolder` で状態を持ち、Activity の保存の後、
 * 画面の Composition を破棄するときにも同じ画面の状態を保存し直す (その時点の View Host は window から
 * 外れている)。実際のアプリの経路に合わせ、`SaveableStateHolder` の下に置いた形でも確かめる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ComposeScrollRecreationTest {

    /** 作り直しの後も同じ内容を組み立てるよう、[content] を `onCreate` で与えるホスト Activity。 */
    class HostActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(ComposeView(this).apply { setContent { content() } })
        }

        companion object {
            /** ViewModel のように作り直しをまたいで生きる Store。 */
            var store: SettingsRootStore = SettingsRootStore(initialRoot = scrollTestRoot())

            /** 作り直しをまたいで生きるハンドル。 */
            var controller: KsScrollController = KsScrollController()

            /** 各テストが与える画面の内容。 */
            var content: @Composable () -> Unit = {}
        }
    }

    private var activityController: ActivityController<HostActivity>? = null

    @After
    fun tearDown() {
        activityController?.close()
        activityController = null
        HostActivity.content = {}
    }

    private val hostModifier: Modifier = Modifier.fillMaxWidth().height(SCROLL_TEST_HOST_HEIGHT.dp)

    /** Navigation Compose の `NavHost` と同じく、画面の内容を `SaveableStateHolder` の下に置く。 */
    @Composable
    private fun NavigationLikeScreen(content: @Composable () -> Unit) {
        val holder = rememberSaveableStateHolder()
        holder.SaveableStateProvider(key = "scroll-demo") { content() }
    }

    /** Store の Cell `c<s>_<c>` と同じ構成の宣言。 */
    private fun dslContent(): DSLSettingsRootScope.() -> Unit = {
        for (s in 0 until 10) {
            Section(header = "Section $s", footer = "Footer $s") {
                for (c in 0 until 5) {
                    LabelCell(title = "Cell $s-$c").cellID("c${s}_$c")
                }
            }
        }
    }

    /** window の View ツリーにある Host をすべて集める。 */
    private fun findLayouts(): List<KsSettingsViewLayout> {
        val found = mutableListOf<KsSettingsViewLayout>()
        fun walk(view: View) {
            if (view is KsSettingsViewLayout) {
                found += view
                return
            }
            if (view is ViewGroup) for (i in 0 until view.childCount) walk(view.getChildAt(i))
        }
        activityController?.get()?.let { walk(it.window.decorView) }
        return found
    }

    private fun findLayout(): KsSettingsViewLayout? {
        fun walk(view: View): KsSettingsViewLayout? {
            if (view is KsSettingsViewLayout) return view
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) walk(view.getChildAt(i))?.let { return it }
            }
            return null
        }
        return activityController?.get()?.let { walk(it.window.decorView) }
    }

    /**
     * window の View ツリーを描画に通す。
     *
     * `AndroidView` の中の View が要求したレイアウトは、Compose がフレームの描画の中で測り直して反映する。
     * Robolectric は描画を行わないため、ここで描画に通してその経路を動かす。
     */
    private fun drawWindow() {
        val decor = activityController?.get()?.window?.decorView ?: return
        if (decor.width <= 0 || decor.height <= 0) return
        decor.draw(Canvas(Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)))
    }

    private fun awaitCondition(timeoutMillis: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            drawWindow()
            if (condition()) return
            if (System.nanoTime() >= deadline) {
                fail("待機条件が $timeoutMillis ms 以内に成立しなかった (${findLayout()?.let { scrollSummary(it) }})")
            }
            Thread.sleep(1)
        }
    }

    /** 表示が済み、[processed] 件の命令・復元が処理されてスクロールが止まるまで待つ。 */
    private fun awaitSettled(processed: Int): KsSettingsViewLayout {
        var layout: KsSettingsViewLayout? = null
        awaitCondition {
            layout = findLayout()
            val view = layout ?: return@awaitCondition false
            val rv = view.internalRecyclerView()
            rv.childCount > 0 &&
                view.internalProcessedScrollEntryCount() >= processed &&
                rv.scrollState == RecyclerView.SCROLL_STATE_IDLE &&
                !rv.isLayoutRequested &&
                !rv.hasPendingAdapterUpdates()
        }
        return layout!!
    }

    /**
     * 画面を開いて [cellId] の行を上端から 17px はみ出させ、Activity を作り直した後に同じずれで上端に
     * かかることを確かめる。
     */
    private fun assertRestoredAfterRecreate(cellId: String) {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        activityController = ctrl
        awaitSettled(processed = 0)
        HostActivity.controller.scrollTo(cellId, animated = false)
        val before = awaitSettled(processed = 1)
        before.internalRecyclerView().scrollBy(0, 17)
        assertEquals("前提: 行が上端からはみ出している", listTop(before) - 17, cellRow(before, cellId)!!.top)

        ctrl.recreate()
        val after = awaitSettled(processed = 1)

        assertNotSame("View Host は作り直されている", before, after)
        val row = cellRow(after, cellId) ?: throw AssertionError(scrollSummary(after))
        assertEquals("作り直す前に上端にかかっていた要素が同じずれで上端にかかる", listTop(after) - 17, row.top)
    }

    @Test
    fun `Compose の KsSettingsView でも Activity を作り直すと同じ要素が上端にかかる`() {
        HostActivity.store = SettingsRootStore(initialRoot = scrollTestRoot())
        HostActivity.controller = KsScrollController()
        HostActivity.content = {
            KsSettingsView(store = HostActivity.store, modifier = hostModifier, scrollController = HostActivity.controller)
        }
        assertRestoredAfterRecreate("c3_2")
    }

    @Test
    fun `NavHost と同じ保存状態の持ち方でも Store 方式の位置が戻る`() {
        HostActivity.store = SettingsRootStore(initialRoot = scrollTestRoot())
        HostActivity.controller = KsScrollController()
        HostActivity.content = {
            NavigationLikeScreen {
                KsSettingsView(store = HostActivity.store, modifier = hostModifier, scrollController = HostActivity.controller)
            }
        }
        assertRestoredAfterRecreate("c3_2")
    }

    @Test
    fun `NavHost と同じ保存状態の持ち方でも DSL 方式の位置が戻る`() {
        val content = dslContent()
        HostActivity.content = {
            NavigationLikeScreen {
                KsSettingsView(
                    modifier = hostModifier,
                    theme = Theme(),
                    content = content,
                )
            }
        }
        // DSL 方式の命令はコンポジションのフレームを待って引き直されるため、ここでは直接スクロールして
        // 上端にかかる行を作る。戻す経路 (保存状態 → Host の待ち行列) は命令の引き直しを通らない。
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        activityController = ctrl
        val before = awaitSettled(processed = 0)
        before.internalRecyclerView().scrollBy(0, 700)
        val anchor = before.captureScrollAnchor() ?: throw AssertionError(scrollSummary(before))
        assertTrue("前提: 上端にかかる行が上端からはみ出している ($anchor)", anchor.offsetFromTop > 0)

        ctrl.recreate()
        val after = awaitSettled(processed = 1)

        assertNotSame("View Host は作り直されている", before, after)
        assertEquals("作り直す前に上端にかかっていた要素が同じずれで上端にかかる", anchor, after.captureScrollAnchor())
    }

    @Test
    fun `NavHost と同じ保存状態の持ち方で既定 id の View Host が複数あるときは復元しない`() {
        val first = SettingsRootStore(initialRoot = scrollTestRoot())
        val second = SettingsRootStore(initialRoot = scrollTestRoot())
        val half = Modifier.fillMaxWidth().height((SCROLL_TEST_HOST_HEIGHT / 2).dp)
        HostActivity.content = {
            NavigationLikeScreen {
                Column {
                    KsSettingsView(store = first, modifier = half)
                    KsSettingsView(store = second, modifier = half)
                }
            }
        }
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        activityController = ctrl
        awaitCondition { findLayouts().size == 2 && findLayouts().all { it.internalRecyclerView().childCount > 0 } }
        for (layout in findLayouts()) layout.internalRecyclerView().scrollBy(0, 300)
        awaitCondition { findLayouts().all { listTop(it) > firstRowTop(it) } }

        ctrl.recreate()
        // 復元の要求は作り直した直後の保存状態から積まれるため、処理済みの件数で「何もしない」を確かめる。
        awaitCondition {
            val layouts = findLayouts()
            layouts.size == 2 && layouts.all { it.internalRecyclerView().childCount > 0 && !it.internalRecyclerView().isLayoutRequested }
        }

        for (layout in findLayouts()) {
            assertEquals("復元の要求は積まれない (${scrollSummary(layout)})", 0, layout.internalProcessedScrollEntryCount())
            assertFalse(
                "内容の先頭から表示される (${scrollSummary(layout)})",
                layout.internalRecyclerView().canScrollVertically(-1),
            )
        }
    }

    /** 表示範囲にある最初の行の上端。 */
    private fun firstRowTop(layout: KsSettingsViewLayout): Int {
        val rv = layout.internalRecyclerView()
        return (0 until rv.childCount).minOf { rv.getChildAt(it).top }
    }
}
