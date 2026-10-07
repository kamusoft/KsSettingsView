package jp.kamusoft.kssettingsview.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.ui.ComposeFrameDriver
import jp.kamusoft.kssettingsview.ui.KsScrollController
import jp.kamusoft.kssettingsview.ui.SCROLL_TEST_HOST_HEIGHT
import jp.kamusoft.kssettingsview.ui.SettingsRootStore
import jp.kamusoft.kssettingsview.ui.Theme
import jp.kamusoft.kssettingsview.ui.cellRow
import jp.kamusoft.kssettingsview.ui.listTop
import jp.kamusoft.kssettingsview.ui.scrollSummary
import jp.kamusoft.kssettingsview.ui.scrollTestRoot
import org.junit.Assert.assertEquals
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
 *
 * `AndroidView` は中の View ごとに別の入れ物へ状態を保存するため、既定の id の View Host が 1 画面に複数
 * あっても、遷移の間に別の画面の View Host と居合わせても、それぞれの位置が戻る (android/ADR-0023)。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ComposeScrollRecreationTest {

    /** 作り直しの後も同じ内容を組み立てるよう、[content] を `onCreate` で与えるホスト Activity。 */
    class HostActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(
                ComposeView(this).apply {
                    frameDriver?.installOn(this)
                    setContent { content() }
                },
            )
        }

        companion object {
            /** ViewModel のように作り直しをまたいで生きる Store。 */
            var store: SettingsRootStore = SettingsRootStore(initialRoot = scrollTestRoot())

            /** 作り直しをまたいで生きるハンドル。 */
            var controller: KsScrollController = KsScrollController()

            /** 各テストが与える画面の内容。 */
            var content: @Composable () -> Unit = {}

            /** 表示中の画面の key。遷移の間は前の画面と次の画面の 2 つが入る。 */
            val visibleScreens: MutableState<List<String>> = mutableStateOf(emptyList())

            /** 表示中の画面を替えるテストが、再 composition を自分で進めるために差し込む駆動器。 */
            internal var frameDriver: ComposeFrameDriver? = null
        }
    }

    private var activityController: ActivityController<HostActivity>? = null

    @After
    fun tearDown() {
        activityController?.close()
        activityController = null
        HostActivity.content = {}
        HostActivity.visibleScreens.value = emptyList()
        HostActivity.frameDriver?.stop()
        HostActivity.frameDriver = null
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

    /**
     * レイアウトの最中に出されたまま親へ届いていない一覧のレイアウト要求を、出し直す。
     *
     * 起動した後に足した画面の最初のレイアウトは、Robolectric では [drawWindow] の描画の中でだけ走る
     * (起動時の画面は window のレイアウトの中で走る)。位置の復元は Host のレイアウトの中で一覧へ
     * レイアウトを要求するが、window のレイアウトの外では、最中に出た要求をレイアウトの後に出し直す
     * 仕組み (`ViewRootImpl` が行う) が働かず、親の要求の印だけが下りて一覧の要求が残る。その仕組みと
     * 同じく、要求を残している一覧からもう一度要求させる。
     */
    private fun reissueLayoutRequestsMadeDuringLayout() {
        for (layout in findLayouts()) {
            val list = layout.internalRecyclerView()
            if (list.isLayoutRequested && !layout.isLayoutRequested) list.requestLayout()
        }
    }

    private fun awaitCondition(timeoutMillis: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            drawWindow()
            if (HostActivity.frameDriver != null) reissueLayoutRequestsMadeDuringLayout()
            if (condition()) return
            if (System.nanoTime() >= deadline) {
                fail("待機条件が $timeoutMillis ms 以内に成立しなかった (${findLayouts().map { scrollSummary(it) }})")
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
    fun `NavHost と同じ保存状態の持ち方で 1 画面に複数置いた KsSettingsView はそれぞれの位置へ戻る`() {
        val first = SettingsRootStore(initialRoot = scrollTestRoot())
        val second = SettingsRootStore(initialRoot = scrollTestRoot(sectionCount = SECOND_SCREEN_SECTIONS))
        val firstController = KsScrollController()
        val secondController = KsScrollController()
        val half = Modifier.fillMaxWidth().height((SCROLL_TEST_HOST_HEIGHT / 2).dp)
        HostActivity.content = {
            NavigationLikeScreen {
                Column {
                    KsSettingsView(store = first, modifier = half, scrollController = firstController)
                    KsSettingsView(store = second, modifier = half, scrollController = secondController)
                }
            }
        }
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        activityController = ctrl
        awaitScreens(FIRST_SCREEN_SECTIONS, SECOND_SCREEN_SECTIONS)
        val firstBefore = layoutShowing(FIRST_SCREEN_SECTIONS)!!
        val secondBefore = layoutShowing(SECOND_SCREEN_SECTIONS)!!
        firstController.scrollTo("c3_2", animated = false)
        secondController.scrollTo("c6_1", animated = false)
        awaitSettledOn(firstBefore, processed = 1)
        awaitSettledOn(secondBefore, processed = 1)
        firstBefore.internalRecyclerView().scrollBy(0, 17)
        secondBefore.internalRecyclerView().scrollBy(0, 9)
        assertEquals("前提: 行が上端からはみ出している", listTop(firstBefore) - 17, cellRow(firstBefore, "c3_2")!!.top)
        assertEquals("前提: 行が上端からはみ出している", listTop(secondBefore) - 9, cellRow(secondBefore, "c6_1")!!.top)

        ctrl.recreate()
        awaitScreens(FIRST_SCREEN_SECTIONS, SECOND_SCREEN_SECTIONS)
        val firstAfter = layoutShowing(FIRST_SCREEN_SECTIONS)!!
        val secondAfter = layoutShowing(SECOND_SCREEN_SECTIONS)!!
        assertNotSame("View Host は作り直されている", firstBefore, firstAfter)
        assertNotSame("View Host は作り直されている", secondBefore, secondAfter)
        awaitSettledOn(firstAfter, processed = 1)
        awaitSettledOn(secondAfter, processed = 1)

        val firstRow = cellRow(firstAfter, "c3_2") ?: throw AssertionError(scrollSummary(firstAfter))
        val secondRow = cellRow(secondAfter, "c6_1") ?: throw AssertionError(scrollSummary(secondAfter))
        assertEquals("1 つめは自分の位置へ戻る", listTop(firstAfter) - 17, firstRow.top)
        assertEquals("2 つめは自分の位置へ戻る", listTop(secondAfter) - 9, secondRow.top)
    }

    // MARK: - 画面どうしが居合わせる進む・戻る

    /**
     * Navigation Compose の `NavHost` と同じく、表示中の画面をそれぞれ `SaveableStateHolder` の下に置く。
     *
     * `NavHost` は遷移の間、前の画面と次の画面を同時にコンポジションへ置く。[HostActivity.visibleScreens] に
     * 2 つの key を入れるとその状態になり、1 つに戻すと外れた画面のコンポジションが破棄される。
     */
    @Composable
    private fun NavigationLikeStack(screen: @Composable (String) -> Unit) {
        val holder = rememberSaveableStateHolder()
        Box {
            for (screenKey in HostActivity.visibleScreens.value) {
                key(screenKey) {
                    holder.SaveableStateProvider(key = screenKey) { screen(screenKey) }
                }
            }
        }
    }

    /** 最初に表示する画面を [screens] にして Activity を起動する。 */
    private fun launchWithScreens(vararg screens: String) {
        HostActivity.frameDriver = ComposeFrameDriver()
        HostActivity.visibleScreens.value = screens.toList()
        activityController = Robolectric.buildActivity(HostActivity::class.java).setup()
    }

    /** 表示中の画面を [screens] に替え、コンポジションへ反映する。 */
    private fun showScreens(vararg screens: String) {
        HostActivity.visibleScreens.value = screens.toList()
        HostActivity.frameDriver!!.frame()
    }

    /** Section の数が [sectionCount] の内容を表示している Host (画面の見分けに使う)。 */
    private fun layoutShowing(sectionCount: Int): KsSettingsViewLayout? =
        findLayouts().singleOrNull { it.internalRoot().sections.size == sectionCount }

    /** window の View ツリーにある Host が [sectionCounts] の内容の画面だけになり、行の配置が済むまで待つ。 */
    private fun awaitScreens(vararg sectionCounts: Int) {
        awaitCondition {
            val layouts = findLayouts()
            layouts.map { it.internalRoot().sections.size }.sorted() == sectionCounts.sorted() &&
                layouts.all {
                    val rv = it.internalRecyclerView()
                    rv.childCount > 0 && !rv.isLayoutRequested && !rv.hasPendingAdapterUpdates()
                }
        }
    }

    /** [layout] が [processed] 件の命令・復元を処理してスクロールが止まるまで待つ。 */
    private fun awaitSettledOn(layout: KsSettingsViewLayout, processed: Int) {
        awaitCondition {
            val rv = layout.internalRecyclerView()
            rv.childCount > 0 &&
                layout.internalProcessedScrollEntryCount() >= processed &&
                rv.scrollState == RecyclerView.SCROLL_STATE_IDLE &&
                !rv.isLayoutRequested &&
                !rv.hasPendingAdapterUpdates()
        }
    }

    @Test
    fun `KsSettingsView の画面から別の KsSettingsView の画面へ進んで戻ると位置が戻る`() {
        val first = SettingsRootStore(initialRoot = scrollTestRoot())
        val second = SettingsRootStore(initialRoot = scrollTestRoot(sectionCount = SECOND_SCREEN_SECTIONS))
        val controller = KsScrollController()
        HostActivity.content = {
            NavigationLikeStack { screenKey ->
                when (screenKey) {
                    FIRST_SCREEN -> KsSettingsView(store = first, modifier = hostModifier, scrollController = controller)
                    SECOND_SCREEN -> KsSettingsView(store = second, modifier = hostModifier)
                    else -> Box(hostModifier)
                }
            }
        }
        launchWithScreens(FIRST_SCREEN)
        awaitScreens(FIRST_SCREEN_SECTIONS)
        val opened = layoutShowing(FIRST_SCREEN_SECTIONS)!!
        controller.scrollTo("c3_2", animated = false)
        awaitSettledOn(opened, processed = 1)
        opened.internalRecyclerView().scrollBy(0, 17)
        assertEquals("前提: 行が上端からはみ出している", listTop(opened) - 17, cellRow(opened, "c3_2")!!.top)

        // 進む: 遷移の間は 2 つの画面が居合わせ、遷移が済むと前の画面が外れる。
        showScreens(FIRST_SCREEN, SECOND_SCREEN)
        awaitScreens(FIRST_SCREEN_SECTIONS, SECOND_SCREEN_SECTIONS)
        showScreens(SECOND_SCREEN)
        awaitScreens(SECOND_SCREEN_SECTIONS)

        // 戻る: 遷移の間は 2 つの画面が居合わせ、遷移が済むと行き先だった画面が外れる。
        showScreens(SECOND_SCREEN, FIRST_SCREEN)
        awaitScreens(FIRST_SCREEN_SECTIONS, SECOND_SCREEN_SECTIONS)
        showScreens(FIRST_SCREEN)
        awaitScreens(FIRST_SCREEN_SECTIONS)

        val returned = layoutShowing(FIRST_SCREEN_SECTIONS)!!
        assertNotSame("View Host は作り直されている", opened, returned)
        awaitSettledOn(returned, processed = 1)
        val row = cellRow(returned, "c3_2") ?: throw AssertionError(scrollSummary(returned))
        assertEquals("進む前に上端にかかっていた要素が同じずれで上端にかかる", listTop(returned) - 17, row.top)

        // 一度居合わせた後に、KsSettingsView を使わない画面へ進んで戻る。
        showScreens(FIRST_SCREEN, PLAIN_SCREEN)
        awaitScreens(FIRST_SCREEN_SECTIONS)
        showScreens(PLAIN_SCREEN)
        awaitCondition { findLayouts().isEmpty() }
        showScreens(PLAIN_SCREEN, FIRST_SCREEN)
        awaitScreens(FIRST_SCREEN_SECTIONS)
        showScreens(FIRST_SCREEN)
        awaitScreens(FIRST_SCREEN_SECTIONS)

        val returnedAgain = layoutShowing(FIRST_SCREEN_SECTIONS)!!
        assertNotSame("View Host は作り直されている", returned, returnedAgain)
        awaitSettledOn(returnedAgain, processed = 1)
        val rowAgain = cellRow(returnedAgain, "c3_2") ?: throw AssertionError(scrollSummary(returnedAgain))
        assertEquals("居合わせた後の行き来でも同じずれで上端にかかる", listTop(returnedAgain) - 17, rowAgain.top)
    }

    /**
     * window から外れる時点で親をたどって最初に当たる「親からの保存を受けない」View と、外れた後に保存を
     * 求められた回数を控える View。
     */
    private class SaveProbeView(context: Context) : View(context) {
        /** window から外れる時点ごとの、親をたどって最初に当たる親からの保存を受けない View（無ければ `null`）。 */
        val saveBoundariesAtDetach: MutableList<View?> = mutableListOf()

        /** window から外れた後に保存を求められた回数。 */
        var saveCountAfterDetach: Int = 0
            private set

        init {
            // ID を持たない View には保存が求められない。
            id = generateViewId()
        }

        override fun onDetachedFromWindow() {
            saveBoundariesAtDetach +=
                generateSequence(parent as? View) { it.parent as? View }.firstOrNull { !it.isSaveFromParentEnabled }
            super.onDetachedFromWindow()
        }

        override fun onSaveInstanceState(): Parcelable? {
            if (!isAttachedToWindow) saveCountAfterDetach++
            return super.onSaveInstanceState()
        }
    }

    @Test
    fun `AndroidView の中の View は window から外れる時点で親からの保存を受けない入れ物の下にある`() {
        var probe: SaveProbeView? = null
        HostActivity.content = {
            NavigationLikeStack { screenKey ->
                if (screenKey == FIRST_SCREEN) {
                    AndroidView(modifier = hostModifier, factory = { ctx -> SaveProbeView(ctx).also { probe = it } })
                } else {
                    Box(hostModifier)
                }
            }
        }
        launchWithScreens(FIRST_SCREEN)
        awaitCondition { probe?.isAttachedToWindow == true }
        val view = probe!!
        val holder = view.parent

        showScreens(FIRST_SCREEN, PLAIN_SCREEN)
        showScreens(PLAIN_SCREEN)
        awaitCondition { !view.isAttachedToWindow && view.saveCountAfterDetach > 0 }

        assertTrue(
            "前提: 画面の破棄では、window から外れた後に保存が求められる (外れる時点の控えが要る理由)",
            view.saveCountAfterDetach > 0,
        )
        assertEquals(
            "外れる時点では、View を直接持つ入れ物が親からの保存を受けない View として当たる (外れるのは 1 回)",
            listOf(holder),
            view.saveBoundariesAtDetach,
        )
    }

    private companion object {
        const val FIRST_SCREEN = "first"
        const val SECOND_SCREEN = "second"
        const val PLAIN_SCREEN = "plain"

        /** 1 つめの画面の Section の数 ([scrollTestRoot] の既定)。 */
        const val FIRST_SCREEN_SECTIONS = 10

        /** 2 つめの画面の Section の数。1 つめの画面と見分けるために変えてある。 */
        const val SECOND_SCREEN_SECTIONS = 12
    }
}
