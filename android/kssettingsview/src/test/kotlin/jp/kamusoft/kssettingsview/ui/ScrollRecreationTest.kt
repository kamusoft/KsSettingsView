package jp.kamusoft.kssettingsview.ui

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * Activity の作り直し（画面回転など）をまたいで、View Host のスクロール位置が戻ることを検証する。
 *
 * View Host は表示範囲の上端にかかる要素とずれを View 階層のインスタンス状態へ保存し、作り直した後に
 * attach と root の反映・レイアウトがそろった時点でその位置へ戻す。既定 id の View Host が同じ階層に
 * 複数あるときは、保存先が衝突するため保存も復元もしない（android/ADR-0021）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScrollRecreationTest {

    /**
     * `KsSettingsView` を自前で組み立てるホスト Activity。
     *
     * 作り直しの後も同じ手順で組み直されるよう、View の生成と bind を `onCreate` に置く。Store は
     * ViewModel のように作り直しをまたいで生きる値として、コンパニオンに置く。
     */
    class HostActivity : ComponentActivity() {
        lateinit var container: FrameLayout

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            container = FrameLayout(this)
            setContentView(container)
            views = stores.mapIndexed { index, store ->
                KsSettingsView(this).also { view ->
                    explicitViewIds.getOrNull(index)?.let { view.id = it }
                    container.addView(
                        view,
                        FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, SCROLL_TEST_HOST_HEIGHT),
                    )
                    view.bind(store)
                }
            }
        }

        companion object {
            /** `onCreate` で組み立てる View Host ごとの Store。 */
            var stores: List<SettingsRootStore> = emptyList()

            /** ホストが明示的に与える View の ID（並びは [stores] と対応する）。 */
            var explicitViewIds: List<Int> = emptyList()

            /** 直近の `onCreate` で組み立てた View 群。 */
            var views: List<KsSettingsView> = emptyList()
        }
    }

    private var controller: ActivityController<HostActivity>? = null

    @Before
    fun resetHost() {
        HostActivity.stores = emptyList()
        HostActivity.explicitViewIds = emptyList()
        HostActivity.views = emptyList()
    }

    @After
    fun tearDown() {
        controller?.close()
        controller = null
    }

    private fun launch(storeCount: Int): ActivityController<HostActivity> {
        HostActivity.stores = List(storeCount) { SettingsRootStore(initialRoot = scrollTestRoot()) }
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        HostActivity.views.forEach { awaitRootShown(it) }
        return ctrl
    }

    /** [view] を [cellId] の行が上端から [offset] だけ上にはみ出す位置へ送る。 */
    private fun scrollHost(view: KsSettingsView, cellId: String, offset: Int) {
        val handle = KsScrollController()
        view.scrollController = handle
        handle.scrollTo(cellId, animated = false)
        awaitScrollSettled(view, 1)
        view.internalRecyclerView().scrollBy(0, offset)
        assertEquals("前提: 行が上端からはみ出している", listTop(view) - offset, cellRow(view, cellId)!!.top)
    }

    private fun assertAtContentStart(view: KsSettingsView) {
        val lm = view.internalRecyclerView().layoutManager as androidx.recyclerview.widget.LinearLayoutManager
        val row = rowAt(view, 0) ?: throw AssertionError("先頭の行が配置されていない: ${scrollSummary(view)}")
        assertEquals("内容の先頭から表示される", listTop(view), lm.getDecoratedTop(row))
    }

    @Test
    fun `Activity を作り直しても同じ要素が上端にかかる`() {
        val ctrl = launch(storeCount = 1)
        scrollHost(HostActivity.views.single(), "c3_2", offset = 17)

        ctrl.recreate()
        val view = HostActivity.views.single()
        awaitRootShown(view)
        awaitScrollSettled(view, 1)

        val row = cellRow(view, "c3_2") ?: throw AssertionError(scrollSummary(view))
        assertEquals("作り直す前に上端にかかっていた要素が同じずれで上端にかかる", listTop(view) - 17, row.top)
    }

    @Test
    fun `既定 id の View Host が複数あるときは復元しない`() {
        val ctrl = launch(storeCount = 2)
        scrollHost(HostActivity.views[0], "c3_2", offset = 17)
        scrollHost(HostActivity.views[1], "c6_1", offset = 9)

        ctrl.recreate()
        HostActivity.views.forEach { awaitRootShown(it) }
        drainMainLooperForUnchangedCheck()

        HostActivity.views.forEach { view ->
            assertEquals("位置の復元は積まれない", 0, view.internalProcessedScrollEntryCount())
            assertAtContentStart(view)
        }
    }

    @Test
    fun `個別の id を与えた View Host はそれぞれの位置へ戻る`() {
        HostActivity.explicitViewIds = listOf(android.view.View.generateViewId(), android.view.View.generateViewId())
        val ctrl = launch(storeCount = 2)
        scrollHost(HostActivity.views[0], "c3_2", offset = 17)
        scrollHost(HostActivity.views[1], "c6_1", offset = 9)

        ctrl.recreate()
        val (first, second) = HostActivity.views
        listOf(first, second).forEach {
            awaitRootShown(it)
            awaitScrollSettled(it, 1)
        }

        assertEquals(listTop(first) - 17, cellRow(first, "c3_2")!!.top)
        assertEquals(listTop(second) - 9, cellRow(second, "c6_1")!!.top)
    }
}
