package jp.kamusoft.kssettingsview.bridge

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.ui.KsScrollAnchor
import jp.kamusoft.kssettingsview.ui.KsScrollPosition
import jp.kamusoft.kssettingsview.ui.KsSettingsView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Bridge のスクロール命令 API と、Host の作り直しをまたぐスクロール位置の保持を検証する。
 *
 * 位置は Activity に載せた実物の Host (`KsSettingsView`) の行の配置から読む。Host の内部状態は
 * 別モジュールの internal のため参照せず、行のテキストと公開 API (`captureScrollAnchor`) で観測する。
 * `captureScrollAnchor` はまだ実行していない位置の復元があるとその控えを返すため、「復元が処理され
 * 終わった後の位置」を確かめる条件にも使える。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsBridgeScrollControlTest {

    /** 見出し・5 行を持つ Section を 10 個並べた、Host の高さより十分に長い内容。 */
    private class Fixture {
        val bridge = KsSettingsBridge()
        val sectionIDs: List<String>
        private val cellIDs: List<List<String>>

        init {
            val builder = KsBridgeRootBuilder()
            val sections = (0 until SECTION_COUNT).map { s ->
                builder.addSection(headerText = "Section $s", footerText = null)
            }
            sectionIDs = sections.map { it.sectionID }
            cellIDs = sections.mapIndexed { s, section ->
                (0 until CELLS_PER_SECTION).map { c ->
                    builder.addLabelCell(KsBridgeLabelCell(title = title(s, c)), section.sectionID)
                        ?: error("Cell を追加できなかった")
                }
            }
            bridge.setRoot(builder)
        }

        fun cellID(section: Int, cell: Int): String = cellIDs[section][cell]
    }

    private var activityController: ActivityController<KsBridgeTestHost.HostActivity>? = null

    private lateinit var activity: KsBridgeTestHost.HostActivity

    @Before
    fun setUp() {
        val ctrl = Robolectric.buildActivity(KsBridgeTestHost.HostActivity::class.java).setup()
        activityController = ctrl
        activity = ctrl.get()
    }

    @After
    fun tearDown() {
        activityController?.close()
        activityController = null
    }

    // MARK: - Host の用意と観測

    /** Bridge から Host を作る。取り付けは [show] で行う。 */
    private fun makeHost(bridge: KsSettingsBridge): KsSettingsView =
        bridge.makeHostView(activity) as? KsSettingsView ?: error("Bridge が Native Host を返さなかった")

    /** Host を画面に取り付け、最初の表示が済むまで待つ。 */
    private fun show(host: KsSettingsView): KsSettingsView {
        activity.container.addView(host, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, HOST_HEIGHT))
        awaitCondition(diagnostics = { summary(host) }) {
            val rv = recyclerView(host)
            rv.childCount > 0 && !rv.isLayoutRequested && !rv.hasPendingAdapterUpdates()
        }
        return host
    }

    /** Host を画面から取り外す。 */
    private fun remove(host: KsSettingsView) {
        activity.container.removeView(host)
    }

    private fun recyclerView(host: KsSettingsView): RecyclerView = (host as ViewGroup).getChildAt(0) as RecyclerView

    private fun listTop(host: KsSettingsView): Int = recyclerView(host).paddingTop

    private fun listBottom(host: KsSettingsView): Int = recyclerView(host).let { it.height - it.paddingBottom }

    /** [text] を表示している行 (配置されていなければ `null`)。 */
    private fun rowShowing(host: KsSettingsView, text: String): View? {
        val rv = recyclerView(host)
        return (0 until rv.childCount).map { rv.getChildAt(it) }.firstOrNull { text in texts(it) }
    }

    private fun texts(view: View): List<String> = when (view) {
        is TextView -> listOf(view.text.toString())
        is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
        else -> emptyList()
    }

    /** スクロールが止まり、レイアウトが確定しているか。 */
    private fun isSettled(host: KsSettingsView): Boolean {
        val rv = recyclerView(host)
        return rv.scrollState == RecyclerView.SCROLL_STATE_IDLE &&
            (rv.layoutManager as LinearLayoutManager).isSmoothScrolling.not() &&
            !rv.isLayoutRequested &&
            !host.isLayoutRequested
    }

    private fun summary(host: KsSettingsView): String {
        val rv = recyclerView(host)
        val rows = (0 until rv.childCount).map { rv.getChildAt(it) }
            .sortedBy { it.top }
            .joinToString { "${texts(it).firstOrNull()}@${it.top}..${it.bottom}" }
        return "state=${rv.scrollState} layoutRequested=${rv.isLayoutRequested} rows=[$rows]"
    }

    /** [host] を、[text] の行が上端から [offset] だけ上にはみ出す位置へ送る。 */
    private fun scrollHost(bridge: KsSettingsBridge, host: KsSettingsView, cellID: String, text: String, offset: Int) {
        bridge.scrollToCell(cellID, POSITION_START, false)
        awaitCondition(diagnostics = { summary(host) }) {
            isSettled(host) && rowShowing(host, text)?.top == listTop(host)
        }
        recyclerView(host).scrollBy(0, offset)
        assertEquals("前提: 行が上端からはみ出している", listTop(host) - offset, rowShowing(host, text)?.top)
    }

    /** [text] の行が上端から [offset] だけ上にはみ出す位置になるまで待つ。 */
    private fun awaitRowAtTop(host: KsSettingsView, text: String, offset: Int) {
        awaitCondition(diagnostics = { summary(host) }) {
            isSettled(host) && rowShowing(host, text)?.top == listTop(host) - offset
        }
    }

    /**
     * [host] の現在の位置が描画され、Bridge がその描画で位置を控えるまで待つ。
     *
     * 実機では利用者がページを離れる前に必ず描画が起きる。テストでは行を直接動かした直後に取り外せる
     * ため、描画が済んだことを待ってから取り外す。
     */
    private fun awaitFrameShowing(bridge: KsSettingsBridge, host: KsSettingsView) {
        val current = host.captureScrollAnchor() ?: error("前提: 取り付けられた Host の位置を控えられる")
        host.invalidate()
        awaitCondition(diagnostics = { "observed=${bridge.hostAnchorTracker?.lastObservedAnchor} current=$current" }) {
            bridge.hostAnchorTracker?.lastObservedAnchor == current
        }
    }

    /**
     * 内容の先頭から表示している (まだ実行していない位置の復元も無い) ことを確かめる。
     *
     * [startAnchor] は内容の先頭にある Host で控えた位置。未実行の復元があれば控えはそちらになる。
     */
    private fun assertAtStart(host: KsSettingsView, startAnchor: KsScrollAnchor, message: String) {
        assertEquals(message, startAnchor, host.captureScrollAnchor())
    }

    /** 内容の最上端 (先頭の行の装飾を含む上端) が表示範囲の上端にあるか。 */
    private fun isAtContentStart(host: KsSettingsView): Boolean {
        val rv = recyclerView(host)
        val first = (rv.layoutManager as LinearLayoutManager).findViewByPosition(0) ?: return false
        return rv.layoutManager!!.getDecoratedTop(first) == listTop(host)
    }

    private fun assertContentStart(host: KsSettingsView, message: String) {
        if (!isAtContentStart(host)) fail("$message: ${summary(host)}")
    }

    private fun awaitAtContentStart(host: KsSettingsView) {
        awaitCondition(diagnostics = { summary(host) }) { isSettled(host) && isAtContentStart(host) }
    }

    // MARK: - スクロール命令の Bridge API

    @Test
    fun `Host 生成後の命令で表示範囲の外の Cell の中央が表示範囲の中央に来る`() {
        val fixture = Fixture()
        val host = show(makeHost(fixture.bridge))
        val text = title(6, 2)
        assertNull("前提: 対象の行は表示範囲の外にある", rowShowing(host, text))

        fixture.bridge.scrollToCell(fixture.cellID(6, 2), POSITION_CENTER, false)

        val center = (listTop(host) + listBottom(host)) / 2
        awaitCondition(diagnostics = { summary(host) }) {
            val row = rowShowing(host, text)
            isSettled(host) && row != null && abs((row.top + row.bottom) / 2 - center) <= 1
        }
    }

    @Test
    fun `Section への命令は見出しを上端に合わせる`() {
        val fixture = Fixture()
        val host = show(makeHost(fixture.bridge))

        fixture.bridge.scrollToSection(fixture.sectionIDs[5], POSITION_START, false)

        awaitRowAtTop(host, "Section 5", offset = 0)
    }

    @Test
    fun `末尾と先頭への命令が内容の両端へ届く`() {
        val fixture = Fixture()
        val host = show(makeHost(fixture.bridge))
        val lastText = title(SECTION_COUNT - 1, CELLS_PER_SECTION - 1)

        fixture.bridge.scrollToEnd(false)
        awaitCondition(diagnostics = { summary(host) }) {
            val row = rowShowing(host, lastText)
            isSettled(host) && row != null && abs(row.bottom - listBottom(host)) <= 1
        }

        fixture.bridge.scrollToStart(false)
        awaitAtContentStart(host)
    }

    @Test
    fun `位置の整数は 0 1 2 を start center end に対応させ範囲外は start とする`() {
        assertEquals(KsScrollPosition.Start, KsBridgeScrollPosition.position(0))
        assertEquals(KsScrollPosition.Center, KsBridgeScrollPosition.position(1))
        assertEquals(KsScrollPosition.End, KsBridgeScrollPosition.position(2))
        assertEquals(KsScrollPosition.Start, KsBridgeScrollPosition.position(3))
        assertEquals(KsScrollPosition.Start, KsBridgeScrollPosition.position(-1))
        assertEquals(KsScrollPosition.Start, KsBridgeScrollPosition.position(Int.MAX_VALUE))
    }

    @Test
    fun `範囲外の位置は start として扱う`() {
        val fixture = Fixture()
        val host = show(makeHost(fixture.bridge))

        fixture.bridge.scrollToCell(fixture.cellID(4, 1), 7, false)

        awaitRowAtTop(host, title(4, 1), offset = 0)
    }

    @Test
    fun `Host の生成前に出した命令は何も起こさず後から生成した Host の位置にも影響しない`() {
        val fixture = Fixture()

        fixture.bridge.scrollToCell(fixture.cellID(6, 2), POSITION_CENTER, false)
        fixture.bridge.scrollToSection(fixture.sectionIDs[5], POSITION_END, false)
        fixture.bridge.scrollToStart(false)
        fixture.bridge.scrollToEnd(false)
        val host = show(makeHost(fixture.bridge))
        drainMainLooperForUnchangedCheck()

        assertContentStart(host, "Host の生成前の命令は生成した Host の位置に影響しない")
    }

    @Test
    fun `releaseHost の後に出した命令は何も起こさず後から生成した Host の位置にも影響しない`() {
        val fixture = Fixture()
        val hostA = show(makeHost(fixture.bridge))
        fixture.bridge.releaseHost()
        remove(hostA)
        val startAnchor = fixture.bridge.scrollAnchor ?: error("前提: 先頭の位置が控えられる")

        fixture.bridge.scrollToCell(fixture.cellID(6, 2), POSITION_CENTER, false)
        fixture.bridge.scrollToSection(fixture.sectionIDs[5], POSITION_END, false)
        fixture.bridge.scrollToStart(false)
        fixture.bridge.scrollToEnd(false)
        val hostB = show(makeHost(fixture.bridge))
        drainMainLooperForUnchangedCheck()

        assertAtStart(hostB, startAnchor, "Host が無い間の命令は後から生成した Host の位置に影響しない")
    }

    @Test
    fun `破棄後の命令は何も起こさない`() {
        val fixture = Fixture()
        val host = show(makeHost(fixture.bridge))
        val before = host.captureScrollAnchor()
        assertNotNull("前提: 位置を控えられる", before)

        fixture.bridge.dispose()
        fixture.bridge.scrollToEnd(true)
        fixture.bridge.scrollToCell(fixture.cellID(6, 2), POSITION_CENTER, false)
        drainMainLooperForUnchangedCheck()

        assertEquals("保持し続けている Host の位置は変わらない", before, host.captureScrollAnchor())
        assertNull("破棄で控えを捨てる", fixture.bridge.scrollAnchor)
    }

    @Test
    fun `canonical UUID でない ID と未知の ID への命令は位置を変えない`() {
        val fixture = Fixture()
        val host = show(makeHost(fixture.bridge))
        val before = host.captureScrollAnchor()

        fixture.bridge.scrollToCell("not-a-uuid", POSITION_CENTER, false)
        fixture.bridge.scrollToSection("not-a-uuid", POSITION_CENTER, false)
        fixture.bridge.scrollToCell(KsBridgeIdentifier.make(), POSITION_CENTER, false)
        fixture.bridge.scrollToSection(KsBridgeIdentifier.make(), POSITION_CENTER, false)
        drainMainLooperForUnchangedCheck()

        assertEquals(before, host.captureScrollAnchor())
    }

    // MARK: - Host の作り直しをまたぐスクロール位置の保持

    @Test
    fun `Host を作り直しても同じ要素が同じずれで上端にかかる`() {
        val fixture = Fixture()
        val hostA = show(makeHost(fixture.bridge))
        val text = title(5, 3)
        scrollHost(fixture.bridge, hostA, fixture.cellID(5, 3), text, offset = 13)

        fixture.bridge.releaseHost()
        remove(hostA)
        val hostB = show(makeHost(fixture.bridge))

        awaitRowAtTop(hostB, text, offset = 13)
        assertNull("戻した時点で控えを使い切る", fixture.bridge.scrollAnchor)
    }

    @Test
    fun `window から外れた後に手放した Host でも外れる直前の要素が同じずれで上端にかかる`() {
        // MAUI の Handler の切断と同じ順序。ページの view が先に階層から外れ、その後に解放が届く。
        val fixture = Fixture()
        val hostA = show(makeHost(fixture.bridge))
        val text = title(5, 3)
        scrollHost(fixture.bridge, hostA, fixture.cellID(5, 3), text, offset = 13)
        awaitFrameShowing(fixture.bridge, hostA)

        remove(hostA)
        assertNull("前提: 外れた Host からは控えられない", hostA.captureScrollAnchor())
        fixture.bridge.releaseHost()
        val hostB = show(makeHost(fixture.bridge))

        awaitRowAtTop(hostB, text, offset = 13)
    }

    @Test
    fun `window から外れた後に手放すとき外れる前に最後に表示した位置を控える`() {
        val fixture = Fixture()
        val hostA = show(makeHost(fixture.bridge))
        scrollHost(fixture.bridge, hostA, fixture.cellID(2, 1), title(2, 1), offset = 7)
        awaitFrameShowing(fixture.bridge, hostA)
        val text = title(6, 2)
        scrollHost(fixture.bridge, hostA, fixture.cellID(6, 2), text, offset = 5)
        awaitFrameShowing(fixture.bridge, hostA)

        remove(hostA)
        fixture.bridge.releaseHost()
        val hostB = show(makeHost(fixture.bridge))

        awaitRowAtTop(hostB, text, offset = 5)
    }

    @Test
    fun `Host が無い間に上に項目が増えても同じ要素へ戻る`() {
        val fixture = Fixture()
        val hostA = show(makeHost(fixture.bridge))
        val text = title(5, 3)
        scrollHost(fixture.bridge, hostA, fixture.cellID(5, 3), text, offset = 13)
        fixture.bridge.releaseHost()
        remove(hostA)

        fixture.bridge.insertCell(KsBridgeLabelCell(title = "Inserted A"), fixture.sectionIDs[0], 0)
        fixture.bridge.insertCell(KsBridgeLabelCell(title = "Inserted B"), fixture.sectionIDs[3], 0)
        val hostB = show(makeHost(fixture.bridge))

        awaitRowAtTop(hostB, text, offset = 13)
    }

    @Test
    fun `作り直しの直後に出した命令が戻した位置より優先される`() {
        val fixture = Fixture()
        val hostA = show(makeHost(fixture.bridge))
        val startAnchor = hostA.captureScrollAnchor() ?: error("前提: 先頭の位置を控えられる")
        val text = title(5, 3)
        scrollHost(fixture.bridge, hostA, fixture.cellID(5, 3), text, offset = 13)
        fixture.bridge.releaseHost()
        remove(hostA)

        val hostB = makeHost(fixture.bridge)
        fixture.bridge.scrollToStart(false)
        show(hostB)

        // 未実行の復元が残っている間は控えがその位置を返すため、この条件は復元と命令の両方が処理された
        // 後にしか成立しない。
        awaitCondition(diagnostics = { summary(hostB) }) {
            isSettled(hostB) && hostB.captureScrollAnchor() == startAnchor
        }
    }

    @Test
    fun `控えは一度だけ使われる`() {
        val fixture = Fixture()
        val hostA = show(makeHost(fixture.bridge))
        val startAnchor = hostA.captureScrollAnchor() ?: error("前提: 先頭の位置を控えられる")
        val text = title(5, 3)
        scrollHost(fixture.bridge, hostA, fixture.cellID(5, 3), text, offset = 13)
        fixture.bridge.releaseHost()
        remove(hostA)

        val hostB = show(makeHost(fixture.bridge))
        awaitRowAtTop(hostB, text, offset = 13)
        fixture.bridge.scrollToStart(false)
        awaitCondition(diagnostics = { summary(hostB) }) {
            isSettled(hostB) && hostB.captureScrollAnchor() == startAnchor
        }
        fixture.bridge.releaseHost()
        remove(hostB)

        val hostC = show(makeHost(fixture.bridge))
        drainMainLooperForUnchangedCheck()

        // 1 回目の控えが再び渡されていれば、実行前でも控えはその位置を返す。
        assertAtStart(hostC, startAnchor, "2 回目に手放したときの位置 (内容の先頭) で表示される")
    }

    @Test
    fun `控えられる内容が無い Host を解放すると控えを持たない`() {
        val fixture = Fixture()
        makeHost(fixture.bridge)

        fixture.bridge.releaseHost()

        assertNull("取り付け前の Host からは控えない", fixture.bridge.scrollAnchor)
    }

    // MARK: - 待機

    /** [condition] が成立するまで、メインスレッドのキューを 1 件ずつ進めながら待つ。 */
    private fun awaitCondition(timeoutMillis: Long = 5_000, diagnostics: () -> String, condition: () -> Boolean) {
        val looper = shadowOf(Looper.getMainLooper())
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (true) {
            if (condition()) return
            if (looper.isIdle) {
                // 他スレッドの差分計算が投稿するまで待つ。yield は OS へのヒントに留まるため sleep を使う。
                Thread.sleep(1)
            } else {
                looper.runOneTask()
            }
            if (System.nanoTime() >= deadline) {
                if (condition()) return
                fail("メインスレッドの待機条件が $timeoutMillis ms 以内に成立しなかった (${diagnostics()})")
            }
        }
    }

    /**
     * 「何も起こらないこと」を確かめるために、メインスレッドのキューを上限つきで進める
     * (不変性の確認に使う固定量の待機。cross/ADR-0027)。収束待ちには使わない。
     */
    private fun drainMainLooperForUnchangedCheck(maxTasks: Int = 1_000) {
        val looper = shadowOf(Looper.getMainLooper())
        var processed = 0
        while (processed < maxTasks && !looper.isIdle) {
            looper.runOneTask()
            processed++
        }
    }

    private companion object {
        const val SECTION_COUNT = 10
        const val CELLS_PER_SECTION = 5
        const val HOST_HEIGHT = 400
        const val POSITION_START = 0
        const val POSITION_CENTER = 1
        const val POSITION_END = 2

        fun title(section: Int, cell: Int): String = "Cell $section-$cell"
    }
}
