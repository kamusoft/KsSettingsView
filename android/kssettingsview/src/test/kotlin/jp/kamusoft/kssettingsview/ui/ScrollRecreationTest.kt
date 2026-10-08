package jp.kamusoft.kssettingsview.ui

import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.core.os.BundleCompat
import jp.kamusoft.kssettingsview.R
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
 * attach と root の反映・レイアウトがそろった時点でその位置へ戻す。既定 id の View Host が同じ保存の
 * 入れ物に複数あるときは、保存先が衝突するため保存も復元もしない。ホストが View Host ごとに保存の入れ物を
 * 分けていれば衝突しないので、それぞれの位置へ戻る（android/ADR-0021、android/ADR-0023）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScrollRecreationTest {

    /**
     * `KsSettingsView` を自前で組み立てるホスト Activity。
     *
     * 作り直しの後も同じ手順で組み直されるよう、View の生成と bind を `onCreate` に置く。Store は
     * ViewModel のように作り直しをまたいで生きる値として、コンパニオンに置く。
     *
     * [saveSeparations] で指定した View Host は、AndroidX Fragment が Fragment の View に対して行うのと
     * 同じ形で、保存の入れ物を分ける。分けた範囲の根に「親からの保存を受けない」設定を付け、
     * その範囲の状態を Activity の保存状態の中の専用の入れ物へ保存し、作り直しの後にそこから戻す。
     */
    class HostActivity : ComponentActivity() {
        lateinit var container: FrameLayout

        /** 保存の入れ物を分けた範囲の根。キーは [stores] の中の位置で、分けない View Host は持たない。 */
        private var separatedRoots: Map<Int, View> = emptyMap()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            container = FrameLayout(this)
            setContentView(container)
            val roots = mutableMapOf<Int, View>()
            views = stores.mapIndexed { index, store ->
                KsSettingsView(this).also { view ->
                    explicitViewIds.getOrNull(index)?.let { view.id = it }
                    val params =
                        FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, SCROLL_TEST_HOST_HEIGHT)
                    val separatedRoot: View? = when (saveSeparations.getOrNull(index)) {
                        null -> null
                        SaveSeparation.ParentOfHost -> FrameLayout(this).also { it.addView(view, params) }
                        SaveSeparation.HostItself -> view
                    }
                    container.addView(separatedRoot ?: view, params)
                    view.bind(store)
                    if (separatedRoot != null) {
                        separatedRoot.isSaveFromParentEnabled = false
                        roots[index] = separatedRoot
                        savedInstanceState
                            ?.let { BundleCompat.getSparseParcelableArray(it, separatedStateKey(index), Parcelable::class.java) }
                            ?.let { separatedRoot.restoreHierarchyState(it) }
                    }
                }
            }
            separatedRoots = roots
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            separatedRoots.forEach { (index, root) ->
                val states = SparseArray<Parcelable>()
                root.saveHierarchyState(states)
                outState.putSparseParcelableArray(separatedStateKey(index), states)
            }
        }

        private fun separatedStateKey(index: Int): String = "separated-view-state-$index"

        /** 保存の入れ物を分ける範囲の根をどこに置くか。 */
        enum class SaveSeparation {
            /** View Host を 1 つだけ持つ親を根にする（Fragment の View の中に View Host を置いた形）。 */
            ParentOfHost,

            /** View Host 自身を根にする（View Host を Fragment の View そのものにした形）。 */
            HostItself,
        }

        companion object {
            /** `onCreate` で組み立てる View Host ごとの Store。 */
            var stores: List<SettingsRootStore> = emptyList()

            /** ホストが明示的に与える View の ID（並びは [stores] と対応する）。 */
            var explicitViewIds: List<Int> = emptyList()

            /** 直近の `onCreate` で組み立てた View 群。 */
            var views: List<KsSettingsView> = emptyList()

            /**
             * View Host ごとの、保存の入れ物を分ける形（並びは [stores] と対応する）。`null` と指定の無い
             * View Host は分けず、Activity の保存に任せる。
             */
            var saveSeparations: List<SaveSeparation?> = emptyList()
        }
    }

    private var controller: ActivityController<HostActivity>? = null

    @Before
    fun resetHost() {
        HostActivity.stores = emptyList()
        HostActivity.explicitViewIds = emptyList()
        HostActivity.views = emptyList()
        HostActivity.saveSeparations = emptyList()
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
    fun `保存の入れ物を分けた親の下に置いた既定 id の View Host はそれぞれの位置へ戻る`() {
        HostActivity.saveSeparations = List(2) { HostActivity.SaveSeparation.ParentOfHost }
        assertEachHostRestoredAfterRecreate()
    }

    @Test
    fun `自身が保存の入れ物の根になっている既定 id の View Host はそれぞれの位置へ戻る`() {
        HostActivity.saveSeparations = List(2) { HostActivity.SaveSeparation.HostItself }
        assertEachHostRestoredAfterRecreate()
    }

    @Test
    fun `直置きの既定 id の View Host と保存の入れ物を分けた親の下の既定 id の View Host はそれぞれの位置へ戻る`() {
        HostActivity.saveSeparations = listOf(null, HostActivity.SaveSeparation.ParentOfHost)
        assertEachHostRestoredAfterRecreate()
    }

    @Test
    fun `自身が保存の入れ物の根になっている View Host だけが戻り、同じ入れ物に残る既定 id の 2 つは復元しない`() {
        HostActivity.saveSeparations = listOf(HostActivity.SaveSeparation.HostItself, null, null)
        val ctrl = launch(storeCount = 3)
        assertEquals(
            "前提: すべて既定 id のまま",
            List(3) { R.id.ks_settings_view },
            HostActivity.views.map { it.id },
        )
        scrollHost(HostActivity.views[0], "c3_2", offset = 17)
        scrollHost(HostActivity.views[1], "c6_1", offset = 9)
        scrollHost(HostActivity.views[2], "c4_3", offset = 5)

        ctrl.recreate()
        val (separated, firstShared, secondShared) = HostActivity.views
        HostActivity.views.forEach { awaitRootShown(it) }
        awaitScrollSettled(separated, 1)
        drainMainLooperForUnchangedCheck()

        assertEquals(listTop(separated) - 17, cellRow(separated, "c3_2")!!.top)
        listOf(firstShared, secondShared).forEach { view ->
            assertEquals("位置の復元は積まれない", 0, view.internalProcessedScrollEntryCount())
            assertAtContentStart(view)
        }
    }

    /** 既定 id の View Host を 2 つ置いて別々の位置へ送り、作り直した後にそれぞれの位置へ戻ることを確かめる。 */
    private fun assertEachHostRestoredAfterRecreate() {
        val ctrl = launch(storeCount = 2)
        assertEquals(
            "前提: どちらも既定 id のまま",
            listOf(R.id.ks_settings_view, R.id.ks_settings_view),
            HostActivity.views.map { it.id },
        )
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
