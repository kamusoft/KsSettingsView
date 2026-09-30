package jp.kamusoft.kssettingsview.ui

import android.widget.FrameLayout
import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import kotlin.math.abs

/**
 * View Host ([KsSettingsView]) のスクロール制御を検証する。
 *
 * 命令ハンドルの接続・データの反映後の実行・位置の解決 (Cell / Section / 両端)・一方向の着地・
 * 表示位置を控える・戻す窓口を、Store を bind した実際の Host で確かめる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScrollControlTest {

    private var activityController: ActivityController<ScrollTestHostActivity>? = null

    private lateinit var activity: ScrollTestHostActivity

    @Before
    fun setUp() {
        val ctrl = Robolectric.buildActivity(ScrollTestHostActivity::class.java).setup()
        activityController = ctrl
        activity = ctrl.get()
        ShadowLog.clear()
    }

    @After
    fun tearDown() {
        KsCellRegistry.strictMode = true
        activityController?.close()
        activityController = null
    }

    // MARK: - Host の用意

    /** Store を bind した Host を画面に取り付け、最初の表示が済むまで待つ。 */
    private fun showHost(
        store: SettingsRootStore = SettingsRootStore(initialRoot = scrollTestRoot()),
        configure: KsSettingsView.() -> Unit = {},
    ): KsSettingsView {
        val view = KsSettingsView(activity)
        view.configure()
        view.bind(store)
        activity.container.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, SCROLL_TEST_HOST_HEIGHT))
        awaitRootShown(view)
        return view
    }

    private fun connect(view: KsSettingsView): KsScrollController =
        KsScrollController().also { view.scrollController = it }

    /** 期待した行が配置されていないときに、その時点の表示を載せて失敗させる。 */
    private fun missing(view: KsSettingsView, message: String = "対象の行が配置されていない"): Nothing =
        throw AssertionError("$message: ${scrollSummary(view)}")

    private fun assertNear(message: String, expected: Int, actual: Int, tolerance: Int = 1) {
        if (abs(expected - actual) > tolerance) fail("$message (期待: $expected / 実際: $actual)")
    }

    /** 内容の最下端 (末尾の行の装飾を含む下端) が表示範囲の下端にあることを確かめる。 */
    private fun assertAtContentEnd(view: KsSettingsView) {
        val lm = view.internalRecyclerView().layoutManager as androidx.recyclerview.widget.LinearLayoutManager
        val last = view.internalRecyclerView().adapter!!.itemCount - 1
        val row = rowAt(view, last) ?: missing(view, "末尾の行が配置されていない")
        assertNear("内容の最下端が表示範囲の下端に来る", listBottom(view), lm.getDecoratedBottom(row))
    }

    private fun assertAtContentStart(view: KsSettingsView) {
        val lm = view.internalRecyclerView().layoutManager as androidx.recyclerview.widget.LinearLayoutManager
        val row = rowAt(view, 0) ?: missing(view, "先頭の行が配置されていない")
        assertNear("内容の最上端が表示範囲の上端に来る", listTop(view), lm.getDecoratedTop(row))
    }

    // MARK: - スクロール命令ハンドルの公開

    @Test
    fun `未接続のハンドルへの命令は何も起こさない`() {
        val controller = KsScrollController()

        controller.scrollTo("c1_0")
        controller.scrollToSection("s1")
        controller.scrollToStart()
        controller.scrollToEnd()
        idle()

        assertNull("未接続のまま", controller.currentReceiver)
    }

    @Test
    fun `interface 型の変数からも同じ命令を出せる`() {
        val view = showHost()
        val controlling: KsScrollControlling = connect(view)

        controlling.scrollToEnd()
        awaitScrollSettled(view, 1)

        assertAtContentEnd(view)
    }

    @Test
    fun `メインスレッド以外からの命令は strictMode で例外になる`() {
        val view = showHost()
        val controller = connect(view)
        var thrown: Throwable? = null

        val thread = Thread {
            try {
                controller.scrollToEnd()
            } catch (e: Throwable) {
                thrown = e
            }
        }
        thread.start()
        thread.join()

        assertTrue("IllegalStateException が送出される (実際: $thrown)", thrown is IllegalStateException)
    }

    @Test
    fun `strictMode でなければメインスレッド以外からの命令はメインスレッドで実行される`() {
        KsCellRegistry.strictMode = false
        val view = showHost()
        val controller = connect(view)

        val thread = Thread { controller.scrollToEnd() }
        thread.start()
        thread.join()
        awaitScrollSettled(view, 1)

        assertAtContentEnd(view)
    }

    @Test
    fun `メインスレッドへ回した命令は実行前につなぎ替えるとどの Host も動かさない`() {
        KsCellRegistry.strictMode = false
        val hostA = showHost()
        val hostB = showHost()
        val controller = KsScrollController()
        hostA.scrollController = controller

        val thread = Thread { controller.scrollToEnd(animated = false) }
        thread.start()
        thread.join()
        // メインスレッドへ回した命令がまだ実行されていない間に、A を外して B へつなぎ替える。
        assertEquals("前提: 命令はまだ A に届いていない", 0, hostA.internalProcessedScrollEntryCount())
        hostA.scrollController = null
        hostB.scrollController = controller
        drainMainLooperForUnchangedCheck()

        assertEquals("A に向けた命令は A に届かない", 0, hostA.internalProcessedScrollEntryCount())
        assertEquals("A に向けた命令は B にも届かない", 0, hostB.internalProcessedScrollEntryCount())
        assertAtContentStart(hostA)
        assertAtContentStart(hostB)

        // つなぎ替えた後に出した命令は B に届く。
        val after = Thread { controller.scrollToEnd(animated = false) }
        after.start()
        after.join()
        awaitScrollSettled(hostB, 1)
        assertAtContentEnd(hostB)
        assertEquals("A は命令を受けない", 0, hostA.internalProcessedScrollEntryCount())
    }

    // MARK: - Host への接続と最後の接続

    @Test
    fun `最後に接続した Host だけが命令を受ける`() {
        val hostA = showHost()
        val hostB = showHost()
        val controller = KsScrollController()
        hostA.scrollController = controller
        hostB.scrollController = controller

        controller.scrollToEnd()
        awaitScrollSettled(hostB, 1)

        assertAtContentEnd(hostB)
        assertEquals("Host A は命令を受けない", 0, hostA.internalProcessedScrollEntryCount())
        assertAtContentStart(hostA)
        assertTrue(
            "置き換えを警告ログで知らせる",
            ShadowLog.getLogsForTag("KsScrollController").any { it.msg.contains("attached to another settings view") },
        )
    }

    @Test
    fun `接続を外したハンドルの命令は何も起こさない`() {
        val view = showHost()
        val controller = connect(view)
        view.scrollController = null

        controller.scrollToEnd()
        drainMainLooperForUnchangedCheck()

        assertEquals("命令は Host に届かない", 0, view.internalProcessedScrollEntryCount())
        assertAtContentStart(view)
    }

    @Test
    fun `unbind で接続が外れ bind し直した後は再代入で命令できる`() {
        val store = SettingsRootStore(initialRoot = scrollTestRoot())
        val view = showHost(store)
        val controller = connect(view)

        view.unbind()
        view.bind(store)
        assertNull("unbind で scrollController は null に戻る", view.scrollController)
        controller.scrollToEnd()
        drainMainLooperForUnchangedCheck()
        assertEquals("bind し直しただけでは命令は届かない", 0, view.internalProcessedScrollEntryCount())
        assertAtContentStart(view)

        view.scrollController = controller
        controller.scrollToEnd()
        awaitScrollSettled(view, 1)

        assertAtContentEnd(view)
    }

    @Test
    fun `ハンドルを差し替えると未実行の命令は捨てられ位置の復元は残る`() {
        val store = SettingsRootStore(initialRoot = scrollTestRoot())
        val source = showHost(store)
        connect(source).scrollTo("c3_2", animated = false)
        awaitScrollSettled(source, 1)
        val anchor = requireNotNull(source.captureScrollAnchor())
        activity.container.removeView(source)

        val view = KsSettingsView(activity)
        view.bind(store)
        val first = KsScrollController()
        view.scrollController = first
        view.restoreScrollAnchor(anchor)
        first.scrollToEnd()
        view.scrollController = KsScrollController()
        activity.container.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, SCROLL_TEST_HOST_HEIGHT))
        awaitScrollSettled(view, 1)
        drainMainLooperForUnchangedCheck()

        assertEquals("位置の復元だけが処理される", 1, view.internalProcessedScrollEntryCount())
        assertNear("復元した位置にある", listTop(view), cellRow(view, "c3_2")!!.top)
    }

    // MARK: - 命令はデータの反映の後に実行される

    @Test
    fun `Cell の追加と同じ処理で出した末尾への命令が新しい末尾へ届く`() {
        val store = SettingsRootStore(initialRoot = scrollTestRoot())
        val view = showHost(store)
        val controller = connect(view)

        store.insertCell(LabelCell(id = "added", title = "Added"), sectionId = "s9", at = 5)
        controller.scrollToEnd()
        awaitScrollSettled(view, 1)

        assertEquals("追加した Cell が一覧に入っている", "added", committedCellIds(view).last())
        assertNotNull("追加した Cell の行が見えている", cellRow(view, "added"))
        assertAtContentEnd(view)
    }

    @Test
    fun `反映を続けて提出しても命令は最後の反映の後に実行される`() {
        val store = SettingsRootStore(initialRoot = scrollTestRoot())
        val view = showHost(store) {
            rootFooter = null
        }
        val controller = connect(view)

        store.insertCell(LabelCell(id = "added1", title = "Added 1"), sectionId = "s9", at = 5)
        store.insertCell(LabelCell(id = "added2", title = "Added 2"), sectionId = "s9", at = 6)
        store.insertCell(LabelCell(id = "added3", title = "Added 3"), sectionId = "s9", at = 7)
        controller.scrollToEnd()
        awaitScrollSettled(view, 1)

        assertEquals("3 つ目の Cell まで一覧に入っている", "added3", committedCellIds(view).last())
        assertNotNull("3 つ目に追加した Cell の行が見えている", cellRow(view, "added3"))
        assertAtContentEnd(view)
    }

    @Test
    fun `続けて出した命令は最後の命令の位置で止まる`() {
        val view = showHost()
        val controller = connect(view)

        controller.scrollTo("c7_3")
        controller.scrollToSection("s2")
        awaitScrollSettled(view, 2)

        val header = rowAt(view, sectionHeaderPosition(view, "s2")) ?: missing(view)
        assertNear("Section の見出しが上端に来る", listTop(view), header.top)
    }

    @Test
    fun `実行前に対象が削除された命令は飛ばされ次の命令は実行される`() {
        val store = SettingsRootStore(initialRoot = scrollTestRoot())
        val view = showHost(store)
        val controller = connect(view)

        controller.scrollTo("c5_1")
        store.removeCell("c5_1")
        controller.scrollToEnd()
        awaitScrollSettled(view, 2)

        assertEquals("削除された Cell は一覧に無い", -1, cellPosition(view, "c5_1"))
        assertTrue(
            "削除された Cell への命令は削除の後に解決される",
            ShadowLog.getLogsForTag("KsSettingsView").any { it.msg.contains("unknown cell id c5_1") },
        )
        assertAtContentEnd(view)
    }

    @Test
    fun `表示中の行の上に見出しを足した直後の命令は足した後の位置で解決される`() {
        val sections = scrollTestRoot().sections.toMutableList()
        sections[0] = sections[0].copy(header = null)
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = sections))
        val view = showHost(store)
        val controller = connect(view)
        assertNotNull("前提: 対象の行は表示中", cellRow(view, "c0_4"))

        store.updateAccessory(AccessoryTarget.SectionHeader("s0"), SettingsAccessory.Section(SectionAccessory.Text("New header")))
        controller.scrollTo("c0_4", animated = true)
        awaitScrollSettled(view, 1)

        val row = cellRow(view, "c0_4") ?: missing(view)
        assertNear("足した見出しの分だけずれた行が上端に来る", listTop(view), row.top)
    }

    @Test
    fun `画面に取り付ける前に出した命令は初回の表示の後に実行される`() {
        val store = SettingsRootStore(initialRoot = scrollTestRoot())
        val view = KsSettingsView(activity)
        view.bind(store)
        val controller = connect(view)

        controller.scrollToEnd()
        idle()
        assertEquals("取り付け前には実行されない", 0, view.internalProcessedScrollEntryCount())
        activity.container.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, SCROLL_TEST_HOST_HEIGHT))
        awaitScrollSettled(view, 1)

        assertAtContentEnd(view)
    }

    @Test
    fun `画面から外れている間に出した命令は取り付け直した後に実行される`() {
        val view = showHost()
        val controller = connect(view)
        activity.container.removeView(view)

        controller.scrollToSection("s6", animated = false)
        idle()
        assertEquals("外れている間は実行されない", 0, view.internalProcessedScrollEntryCount())
        activity.container.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, SCROLL_TEST_HOST_HEIGHT))
        awaitScrollSettled(view, 1)

        val header = rowAt(view, sectionHeaderPosition(view, "s6")) ?: missing(view)
        assertNear("取り付け直した後に Section の見出しが上端に来る", listTop(view), header.top)
    }

    // MARK: - Cell への命令の位置

    @Test
    fun `表示範囲の外の Cell を中央に合わせる`() {
        val view = showHost()
        val controller = connect(view)

        controller.scrollTo("c6_2", KsScrollPosition.Center)
        awaitScrollSettled(view, 1)

        val row = cellRow(view, "c6_2") ?: missing(view)
        assertNear("行の中央が表示範囲の中央に来る", (listTop(view) + listBottom(view)) / 2, (row.top + row.bottom) / 2)
    }

    @Test
    fun `表示範囲の外の Cell を下端に合わせる`() {
        val view = showHost()
        val controller = connect(view)

        controller.scrollTo("c6_2", KsScrollPosition.End, animated = false)
        awaitScrollSettled(view, 1)

        val row = cellRow(view, "c6_2") ?: missing(view)
        assertNear("行の下端が表示範囲の下端に来る", listBottom(view), row.bottom)
    }

    @Test
    fun `内容の末尾付近の Cell を上端に合わせようとするとスクロール可能範囲の端で止まる`() {
        val view = showHost()
        val controller = connect(view)

        controller.scrollTo("c9_4", KsScrollPosition.Start)
        awaitScrollSettled(view, 1)

        val row = cellRow(view, "c9_4") ?: missing(view)
        assertTrue("行は上端まで上がらない (top=${row.top})", row.top > listTop(view))
        assertAtContentEnd(view)
    }

    @Test
    fun `非表示の Cell と存在しない ID への命令は位置を変えない`() {
        val root = scrollTestRoot()
        val hidden = root.sections[5].let { section ->
            section.copy(cells = section.cells.mapIndexed { i, c -> if (i == 1) (c as LabelCell).copy(isVisible = false) else c })
        }
        val store = SettingsRootStore(initialRoot = root.copy(sections = root.sections.toMutableList().also { it[5] = hidden }))
        val view = showHost(store)
        val controller = connect(view)

        controller.scrollTo("c5_1")
        controller.scrollTo("no-such-cell")
        controller.scrollTo(12345)
        awaitScrollSettled(view, 3)

        assertAtContentStart(view)
        val warnings = ShadowLog.getLogsForTag("KsSettingsView").map { it.msg }
        assertTrue("存在しない ID を警告ログで知らせる: $warnings", warnings.any { it.contains("unknown cell id no-such-cell") })
        assertFalse("非表示の Cell は警告しない: $warnings", warnings.any { it.contains("c5_1") })
    }

    // MARK: - Section への命令は見出しごと見せる

    @Test
    fun `Start で見出しが表示範囲の上端に来る`() {
        val view = showHost()
        val controller = connect(view)

        controller.scrollToSection("s7")
        awaitScrollSettled(view, 1)

        val header = rowAt(view, sectionHeaderPosition(view, "s7")) ?: missing(view)
        assertNear("見出しの上端が表示範囲の上端に来る", listTop(view), header.top)
    }

    @Test
    fun `見出しの無い Section は最初の Cell が上端に来る`() {
        val root = scrollTestRoot()
        val sections = root.sections.toMutableList()
        sections[6] = sections[6].copy(header = null)
        val view = showHost(SettingsRootStore(initialRoot = SettingsRoot(sections = sections)))
        val controller = connect(view)

        controller.scrollToSection("s6")
        awaitScrollSettled(view, 1)

        val row = cellRow(view, "c6_0") ?: missing(view)
        assertNear("最初の Cell の上端が表示範囲の上端に来る", listTop(view), row.top)
    }

    @Test
    fun `Section の範囲を中央と下端に合わせる`() {
        val view = showHost()
        val controller = connect(view)

        controller.scrollToSection("s4", KsScrollPosition.End)
        awaitScrollSettled(view, 1)
        val footerPosition = adapterPositionOf(view) { it is CellListItem.SectionFooter && it.sectionId == "s4" }
        val footer = rowAt(view, footerPosition) ?: missing(view)
        assertNear("Footer の下端が表示範囲の下端に来る", listBottom(view), footer.bottom)

        controller.scrollToSection("s1", KsScrollPosition.Center)
        awaitScrollSettled(view, 2)
        val header = rowAt(view, sectionHeaderPosition(view, "s1")) ?: missing(view)
        val footer1 = rowAt(view, adapterPositionOf(view) { it is CellListItem.SectionFooter && it.sectionId == "s1" })
            ?: missing(view)
        assertNear(
            "範囲の中央が表示範囲の中央に来る",
            (listTop(view) + listBottom(view)) / 2,
            (header.top + footer1.bottom) / 2,
        )
    }

    @Test
    fun `表示範囲より高い Section は上端に合わせる`() {
        val sections = scrollTestRoot().sections.toMutableList()
        sections[5] = scrollTestSection(5, cellsPerSection = 12)
        val view = showHost(SettingsRootStore(initialRoot = SettingsRoot(sections = sections)))
        val controller = connect(view)

        controller.scrollToSection("s5", KsScrollPosition.End)
        awaitScrollSettled(view, 1)

        val header = rowAt(view, sectionHeaderPosition(view, "s5")) ?: missing(view)
        assertNear("見出しの上端が表示範囲の上端に来る", listTop(view), header.top)
    }

    @Test
    fun `非表示の Section と表示される行の無い Section への命令は位置を変えない`() {
        val sections = scrollTestRoot().sections.toMutableList()
        sections[4] = sections[4].copy(isVisible = false)
        sections.add(5, Section(id = "empty"))
        val view = showHost(SettingsRootStore(initialRoot = SettingsRoot(sections = sections)))
        val controller = connect(view)

        controller.scrollToSection("s4")
        controller.scrollToSection("empty")
        awaitScrollSettled(view, 2)

        assertAtContentStart(view)
    }

    // MARK: - 先頭・末尾への命令は内容の両端へ送る

    @Test
    fun `末尾への命令で Root Footer の下端まで見える`() {
        val view = showHost {
            rootFooter = RootAccessory.Text("Root footer")
        }
        val controller = connect(view)

        controller.scrollToEnd()
        awaitScrollSettled(view, 1)

        val footerPosition = view.internalRecyclerView().adapter!!.itemCount - 1
        val footer = rowAt(view, footerPosition) ?: missing(view)
        assertNear("Root Footer の下端が表示範囲の下端に来る", listBottom(view), footer.bottom)
    }

    @Test
    fun `先頭への命令で Root Header の上端まで戻る`() {
        val store = SettingsRootStore(initialRoot = scrollTestRoot())
        val view = showHost(store) {
            rootHeader = RootAccessory.Text("Root header")
        }
        val controller = connect(view)
        controller.scrollToEnd(animated = false)
        awaitScrollSettled(view, 1)
        assertNull("前提: Root Header は画面外にある", rowAt(view, 0))

        controller.scrollToStart()
        awaitScrollSettled(view, 2)

        val header = rowAt(view, 0) ?: missing(view)
        assertNear("Root Header の上端が表示範囲の上端に来る", listTop(view), header.top)
        assertAtContentStart(view)
    }

    @Test
    fun `Store 経由で付けた Root Footer も末尾に含まれる`() {
        val store = SettingsRootStore(initialRoot = scrollTestRoot())
        val view = showHost(store)
        val controller = connect(view)

        store.updateAccessory(AccessoryTarget.RootFooter, SettingsAccessory.Root(RootAccessory.Text("Root footer")))
        controller.scrollToEnd()
        awaitScrollSettled(view, 1)

        assertEquals("Root Footer の行がある", 1, view.internalFooterAdapter().itemCount)
        assertAtContentEnd(view)
    }

    // MARK: - アニメーションは一方向で着地する

    @Test
    fun `中央合わせのアニメーションが行き過ぎて戻らない`() {
        val view = showHost()
        val controller = connect(view)
        val recorder = ScrollDeltaRecorder()
        view.internalRecyclerView().addOnScrollListener(recorder)

        controller.scrollTo("c8_3", KsScrollPosition.Center, animated = true)
        awaitScrollSettled(view, 1)

        assertTrue("スクロールが複数回に分けて進む: ${recorder.deltas}", recorder.deltas.size > 1)
        assertTrue("下方向にだけ進む: ${recorder.deltas}", recorder.deltas.all { it > 0 })
        val row = cellRow(view, "c8_3") ?: missing(view)
        assertNear("行の中央が表示範囲の中央に来る", (listTop(view) + listBottom(view)) / 2, (row.top + row.bottom) / 2)
    }

    @Test
    fun `下端合わせのアニメーションが行き過ぎて戻らない`() {
        val view = showHost()
        val controller = connect(view)
        val recorder = ScrollDeltaRecorder()
        view.internalRecyclerView().addOnScrollListener(recorder)

        controller.scrollTo("c8_3", KsScrollPosition.End, animated = true)
        awaitScrollSettled(view, 1)

        assertTrue("スクロールが複数回に分けて進む: ${recorder.deltas}", recorder.deltas.size > 1)
        assertTrue("下方向にだけ進む: ${recorder.deltas}", recorder.deltas.all { it > 0 })
        val row = cellRow(view, "c8_3") ?: missing(view)
        assertNear("行の下端が表示範囲の下端に来る", listBottom(view), row.bottom)
    }

    @Test
    fun `上方向の中央合わせのアニメーションも行き過ぎて戻らない`() {
        val view = showHost()
        val controller = connect(view)
        controller.scrollToEnd(animated = false)
        awaitScrollSettled(view, 1)
        val recorder = ScrollDeltaRecorder()
        view.internalRecyclerView().addOnScrollListener(recorder)

        controller.scrollToSection("s1", KsScrollPosition.Center, animated = true)
        awaitScrollSettled(view, 2)

        assertTrue("スクロールが複数回に分けて進む: ${recorder.deltas}", recorder.deltas.size > 1)
        assertTrue("上方向にだけ進む: ${recorder.deltas}", recorder.deltas.all { it < 0 })
        val header = rowAt(view, sectionHeaderPosition(view, "s1")) ?: missing(view)
        val footer = rowAt(view, adapterPositionOf(view) { it is CellListItem.SectionFooter && it.sectionId == "s1" })
            ?: missing(view)
        assertNear("範囲の中央が表示範囲の中央に来る", (listTop(view) + listBottom(view)) / 2, (header.top + footer.bottom) / 2)
    }

    @Test
    fun `アニメーション中に出した対象の無い命令は先行のアニメーションを止めない`() {
        val root = scrollTestRoot()
        val sections = root.sections.toMutableList()
        sections[5] = sections[5].copy(cells = sections[5].cells.mapIndexed { i, c -> if (i == 1) (c as LabelCell).copy(isVisible = false) else c })
        sections[4] = sections[4].copy(isVisible = false)
        sections.add(Section(id = "empty"))
        val view = showHost(SettingsRootStore(initialRoot = SettingsRoot(sections = sections)))
        val controller = connect(view)
        val rv = view.internalRecyclerView()

        controller.scrollToEnd(animated = true)
        awaitMainLooperCondition(diagnostics = { scrollSummary(view) }) {
            view.internalProcessedScrollEntryCount() >= 1 &&
                rv.scrollState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING
        }

        // 存在しない ID・非表示の Cell / Section・表示される行の無い Section への命令。
        controller.scrollTo("no-such-cell", animated = false)
        controller.scrollTo("c5_1", animated = false)
        controller.scrollToSection("s4", animated = false)
        controller.scrollToSection("empty", animated = false)
        controller.scrollToSection("no-such-section", animated = true)
        awaitMainLooperCondition(diagnostics = { scrollSummary(view) }) { view.internalProcessedScrollEntryCount() >= 6 }
        assertEquals(
            "対象の無い命令では先行のアニメーションは止まらない (${scrollSummary(view)})",
            androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING,
            rv.scrollState,
        )

        awaitScrollSettled(view, 6)
        assertAtContentEnd(view)
    }

    @Test
    fun `アニメーションなしの命令は即座に最終位置へ移る`() {
        val view = showHost()
        val controller = connect(view)
        val recorder = ScrollDeltaRecorder()
        view.internalRecyclerView().addOnScrollListener(recorder)
        var sawSettling = false
        view.internalRecyclerView().addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING) sawSettling = true
            }
        })

        controller.scrollToSection("s6", KsScrollPosition.Start, animated = false)
        awaitScrollSettled(view, 1)

        assertFalse("アニメーションしない", sawSettling)
        val header = rowAt(view, sectionHeaderPosition(view, "s6")) ?: missing(view)
        assertNear("次の配置の時点で見出しの上端が表示範囲の上端にある", listTop(view), header.top)
    }

    // MARK: - 位置を控える・戻す窓口

    /** Host を [cellId] の行が上端から [offset] だけ上にはみ出す位置へ送り、控えを取って取り外す。 */
    private fun captureOnHost(store: SettingsRootStore, cellId: String, offset: Int): KsScrollAnchor {
        val source = showHost(store)
        connect(source).scrollTo(cellId, animated = false)
        awaitScrollSettled(source, 1)
        source.internalRecyclerView().scrollBy(0, offset)
        val anchor = requireNotNull(source.captureScrollAnchor()) { scrollSummary(source) }
        activity.container.removeView(source)
        return anchor
    }

    /** 新しい Host を bind し、控えを戻してから画面に取り付ける。 */
    private fun restoreOnNewHost(store: SettingsRootStore, anchor: KsScrollAnchor, afterRestore: (KsScrollController) -> Unit = {}): KsSettingsView {
        val view = KsSettingsView(activity)
        view.bind(store)
        val controller = connect(view)
        view.restoreScrollAnchor(anchor)
        afterRestore(controller)
        activity.container.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, SCROLL_TEST_HOST_HEIGHT))
        return view
    }

    @Test
    fun `控えた位置を新しい Host で戻す`() {
        val store = SettingsRootStore(initialRoot = scrollTestRoot())
        val anchor = captureOnHost(store, "c3_2", offset = 17)

        val view = restoreOnNewHost(store, anchor)
        awaitScrollSettled(view, 1)

        val row = cellRow(view, "c3_2") ?: missing(view)
        assertEquals("同じ要素が同じずれで上端にかかる", listTop(view) - 17, row.top)
    }

    @Test
    fun `控えた後に上に項目が増えても同じ要素へ戻る`() {
        val store = SettingsRootStore(initialRoot = scrollTestRoot())
        val anchor = captureOnHost(store, "c3_2", offset = 17)
        store.insertSection(scrollTestSection(20), at = 0)
        store.insertCell(LabelCell(id = "above", title = "Above"), sectionId = "s3", at = 0)

        val view = restoreOnNewHost(store, anchor)
        awaitScrollSettled(view, 1)

        val row = cellRow(view, "c3_2") ?: missing(view)
        assertEquals("同じ要素が同じずれで上端にかかる", listTop(view) - 17, row.top)
    }

    @Test
    fun `控えた要素が消えていたら何もしない`() {
        val store = SettingsRootStore(initialRoot = scrollTestRoot())
        val anchor = captureOnHost(store, "c3_2", offset = 17)
        store.removeCell("c3_2")

        val view = restoreOnNewHost(store, anchor)
        awaitScrollSettled(view, 1)

        assertAtContentStart(view)
    }

    @Test
    fun `戻した後に出した命令が最終位置を決める`() {
        val store = SettingsRootStore(initialRoot = scrollTestRoot())
        val anchor = captureOnHost(store, "c3_2", offset = 17)

        val view = restoreOnNewHost(store, anchor) { controller -> controller.scrollToEnd() }
        awaitScrollSettled(view, 2)

        assertAtContentEnd(view)
    }

    @Test
    fun `Section の見出しと Root Header を控えて戻せる`() {
        val store = SettingsRootStore(initialRoot = scrollTestRoot())
        val source = showHost(store) { rootHeader = RootAccessory.Text("Root header") }
        connect(source).scrollToSection("s4", animated = false)
        awaitScrollSettled(source, 1)
        source.internalRecyclerView().scrollBy(0, 5)
        val headerAnchor = requireNotNull(source.captureScrollAnchor())
        source.scrollController!!.scrollToStart(animated = false)
        awaitScrollSettled(source, 2)
        source.internalRecyclerView().scrollBy(0, 3)
        val rootAnchor = requireNotNull(source.captureScrollAnchor())
        activity.container.removeView(source)

        val view = restoreOnNewHost(store, headerAnchor)
        view.rootHeader = RootAccessory.Text("Root header")
        awaitScrollSettled(view, 1)
        val header = rowAt(view, sectionHeaderPosition(view, "s4")) ?: missing(view)
        assertEquals("Section の見出しが同じずれで上端にかかる", listTop(view) - 5, header.top)

        view.restoreScrollAnchor(rootAnchor)
        awaitScrollSettled(view, 2)
        val root = rowAt(view, 0) ?: missing(view)
        assertEquals("Root Header が同じずれで上端にかかる", listTop(view) - 3, root.top)
    }

    @Test
    fun `復元が実行される前に控えると未実行の控えを返す`() {
        val store = SettingsRootStore(initialRoot = scrollTestRoot())
        val anchor = captureOnHost(store, "c3_2", offset = 17)
        val view = KsSettingsView(activity)
        view.bind(store)
        view.restoreScrollAnchor(anchor)

        assertEquals(anchor, view.captureScrollAnchor())
    }

    @Test
    fun `KsScrollAnchor は Parcel を往復しても等しい`() {
        val anchors = listOf(
            KsScrollAnchor(KsScrollAnchor.Element.Cell("c1"), 12),
            KsScrollAnchor(KsScrollAnchor.Element.SectionHeader("s1"), 0),
            KsScrollAnchor(KsScrollAnchor.Element.SectionFooter("s2"), -3),
            KsScrollAnchor(KsScrollAnchor.Element.RootHeader, 4),
            KsScrollAnchor(KsScrollAnchor.Element.RootFooter, 5),
        )
        for (anchor in anchors) {
            val parcel = android.os.Parcel.obtain()
            anchor.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val restored = KsScrollAnchor.CREATOR.createFromParcel(parcel)
            parcel.recycle()
            assertEquals(anchor, restored)
        }
    }

    @Test
    fun `同じハンドルを接続し直しても受け口は変わらない`() {
        val view = showHost()
        val controller = connect(view)
        val receiver = controller.currentReceiver

        view.scrollController = controller

        assertSame(receiver, controller.currentReceiver)
    }
}
