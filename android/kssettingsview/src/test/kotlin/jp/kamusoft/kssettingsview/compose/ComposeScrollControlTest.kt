package jp.kamusoft.kssettingsview.compose

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import jp.kamusoft.kssettingsview.ui.CellListItem
import jp.kamusoft.kssettingsview.ui.KsScrollController
import jp.kamusoft.kssettingsview.ui.KsScrollPosition
import jp.kamusoft.kssettingsview.ui.SCROLL_TEST_HOST_HEIGHT
import jp.kamusoft.kssettingsview.ui.SettingsRootStore
import jp.kamusoft.kssettingsview.ui.Theme
import jp.kamusoft.kssettingsview.ui.adapterPositionOf
import jp.kamusoft.kssettingsview.ui.cellRow
import jp.kamusoft.kssettingsview.ui.drainMainLooperForUnchangedCheck
import jp.kamusoft.kssettingsview.ui.listBottom
import jp.kamusoft.kssettingsview.ui.listTop
import jp.kamusoft.kssettingsview.ui.rowAt
import jp.kamusoft.kssettingsview.ui.scrollSummary
import jp.kamusoft.kssettingsview.ui.scrollTestRoot
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import jp.kamusoft.kssettingsview.ui.KsSettingsView as KsSettingsViewLayout

/**
 * Compose の `KsSettingsView(...)` のスクロール制御を検証する。
 *
 * `scrollController` 引数の接続 (Store 方式・DSL 方式)、DSL 方式での明示 ID と `forEach` の key の
 * 引き直し、状態の変更と同じ処理から出した命令の解決、`rememberScrollController` を確かめる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ComposeScrollControlTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val hostModifier: Modifier = Modifier.fillMaxWidth().height(SCROLL_TEST_HOST_HEIGHT.dp)

    // MARK: - 観測と待機

    private fun findLayout(): KsSettingsViewLayout {
        fun walk(view: View): KsSettingsViewLayout? {
            if (view is KsSettingsViewLayout) return view
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) walk(view.getChildAt(i))?.let { return it }
            }
            return null
        }
        return requireNotNull(walk(composeRule.activity.window.decorView)) { "KsSettingsView が見つからない" }
    }

    /**
     * [condition] が成立するまで、Compose のフレームとメインスレッドのキューを進めながら待つ。
     *
     * 差分計算はバックグラウンドで走るため、1 周ごとに短く sleep して実行機会を譲る。時間切れは
     * その時点の表示を載せて失敗させる。
     */
    private fun awaitCondition(layout: () -> KsSettingsViewLayout?, timeoutMillis: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (true) {
            composeRule.waitForIdle()
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            if (System.nanoTime() >= deadline) {
                fail("待機条件が $timeoutMillis ms 以内に成立しなかった (${layout()?.let { scrollSummary(it) }})")
            }
            Thread.sleep(1)
        }
    }

    /** 最初の表示 (行の配置) が済むまで待つ。 */
    private fun awaitShown(): KsSettingsViewLayout {
        var layout: KsSettingsViewLayout? = null
        awaitCondition({ layout }) {
            layout = findLayout()
            val rv = layout!!.internalRecyclerView()
            rv.childCount > 0 && !rv.isLayoutRequested && !rv.hasPendingAdapterUpdates()
        }
        return layout!!
    }

    /** 命令が [count] 件処理され、スクロールが止まるまで待つ。 */
    private fun awaitSettled(layout: KsSettingsViewLayout, count: Int) {
        val rv = layout.internalRecyclerView()
        awaitCondition({ layout }) {
            layout.internalProcessedScrollEntryCount() >= count &&
                rv.scrollState == RecyclerView.SCROLL_STATE_IDLE &&
                rv.layoutManager?.isSmoothScrolling == false &&
                !rv.isLayoutRequested
        }
    }

    private fun assertNear(message: String, expected: Int, actual: Int) {
        if (abs(expected - actual) > 1) fail("$message (期待: $expected / 実際: $actual)")
    }

    private fun explicitCellId(id: Any): String = DSLIdentityId.id(from = DSLIdentityHint.Explicit(id))

    private fun forEachId(id: Any): String = DSLIdentityId.id(from = DSLIdentityHint.ForEach(id))

    /** 見出しと 5 つの Cell を持つ Section を [count] 個並べる (目印の Cell を [marker] で差し込める)。 */
    private fun DSLSettingsRootScope.fillerSections(
        range: IntRange,
        marker: DSLSectionScope.(section: Int, cell: Int) -> Unit = { _, _ -> },
    ) {
        for (s in range) {
            Section(header = "Section $s") {
                for (c in 0 until 5) {
                    LabelCell(title = "Cell $s-$c")
                    marker(s, c)
                }
            }
        }
    }

    // MARK: - rememberScrollController

    @Test
    fun `rememberScrollController は再コンポジションで同じハンドルを返す`() {
        var tick by mutableIntStateOf(0)
        val seen = mutableListOf<KsScrollController>()
        composeRule.setContent {
            @Suppress("UNUSED_EXPRESSION")
            tick
            seen += rememberScrollController()
        }
        composeRule.waitForIdle()
        composeRule.runOnUiThread { tick++ }
        composeRule.waitForIdle()

        assertTrue("再コンポジションが起きている: ${seen.size}", seen.size >= 2)
        assertTrue("同じインスタンスが返る", seen.all { it === seen.first() })
    }

    // MARK: - Store 方式

    @Test
    fun `Store 方式では Store の Cell ID で指せる`() {
        val store = SettingsRootStore(initialRoot = scrollTestRoot())
        val controller = KsScrollController()
        composeRule.setContent {
            KsSettingsView(store = store, modifier = hostModifier, scrollController = controller)
        }
        val layout = awaitShown()

        composeRule.runOnUiThread { controller.scrollTo("c6_2") }
        awaitSettled(layout, 1)

        val row = cellRow(layout, "c6_2") ?: throw AssertionError(scrollSummary(layout))
        assertNear("その Cell が表示範囲の上端に来る", listTop(layout), row.top)
        assertSame("ハンドルは内部の Host に接続される", controller, layout.scrollController)
    }

    // MARK: - DSL 方式

    @Test
    fun `明示 ID で Cell を指せる`() {
        val controller = KsScrollController()
        composeRule.setContent {
            KsSettingsView(modifier = hostModifier, theme = Theme(), scrollController = controller) {
                fillerSections(0 until 10) { s, c -> if (s == 6 && c == 2) cellID("wifi") }
            }
        }
        val layout = awaitShown()

        composeRule.runOnUiThread { controller.scrollTo("wifi", KsScrollPosition.Center) }
        awaitSettled(layout, 1)

        val row = cellRow(layout, explicitCellId("wifi")) ?: throw AssertionError(scrollSummary(layout))
        assertNear("行の中央が表示範囲の中央に来る", (listTop(layout) + listBottom(layout)) / 2, (row.top + row.bottom) / 2)
    }

    @Test
    fun `forEach の key で Section を指せる`() {
        val controller = KsScrollController()
        composeRule.setContent {
            KsSettingsView(modifier = hostModifier, theme = Theme(), scrollController = controller) {
                forEach(items = (40 until 50).toList(), key = { it }) { item ->
                    Section(header = "Item $item") {
                        for (c in 0 until 5) LabelCell(title = "Cell $item-$c")
                    }
                }
            }
        }
        val layout = awaitShown()

        composeRule.runOnUiThread { controller.scrollToSection(42) }
        awaitSettled(layout, 1)

        val headerPosition = adapterPositionOf(layout) { it is CellListItem.SectionHeader && it.sectionId == forEachId(42) }
        val header = rowAt(layout, headerPosition) ?: throw AssertionError(scrollSummary(layout))
        assertNear("その Section の見出しの上端が表示範囲の上端に来る", listTop(layout), header.top)
    }

    @Test
    fun `同じ値が明示 ID と key の両方にあるときは明示 ID の要素を採る`() {
        val controller = KsScrollController()
        composeRule.setContent {
            KsSettingsView(modifier = hostModifier, theme = Theme(), scrollController = controller) {
                fillerSections(0 until 3)
                Section(header = "Keyed") {
                    forEach(items = listOf("a"), key = { it }) { LabelCell(title = "Y") }
                }
                fillerSections(4 until 7)
                Section(header = "Explicit") {
                    LabelCell(title = "X").cellID("a")
                }
                fillerSections(8 until 10)
            }
        }
        val layout = awaitShown()

        composeRule.runOnUiThread { controller.scrollTo("a") }
        awaitSettled(layout, 1)

        val x = cellRow(layout, explicitCellId("a")) ?: throw AssertionError(scrollSummary(layout))
        assertNear("明示 ID の Cell X が表示範囲の上端に来る", listTop(layout), x.top)
    }

    @Test
    fun `状態の変更と同じ処理から出した命令は新しいツリーで解決される`() {
        val controller = KsScrollController()
        val initial = (0 until 30).map { "k$it" }
        var items by mutableStateOf(initial)
        composeRule.setContent {
            KsSettingsView(modifier = hostModifier, theme = Theme(), scrollController = controller) {
                Section(header = "Items") {
                    forEach(items = items, key = { it }) { LabelCell(title = it) }
                }
                fillerSections(1 until 4)
            }
        }
        val layout = awaitShown()
        val resolver = findResolver(controller)
        val generationBefore = resolver.treeGeneration
        val newRowId = forEachId("new")

        // 追加は forEach の末尾に置く。途中へ挿入すると宣言の差分が後ろの項目の移動を含み、Host へは
        // 項目ごとの提出として届く。命令はその最後の提出の反映を待って実行されるため、待ち時間が
        // バックグラウンドの差分計算の回数に比例して伸び、この検証の意味と無関係に負荷で揺れる。
        composeRule.runOnUiThread {
            items = initial + "new"
            controller.scrollTo("new")
        }
        // 検証したい意味 (追加した項目の行が表示範囲の上端に来ること) そのものを待つ。
        awaitCondition({ layout }) {
            val row = cellRow(layout, newRowId)
            layout.internalProcessedScrollEntryCount() >= 1 && row != null && abs(row.top - listTop(layout)) <= 1
        }

        val (received, resolved) = requireNotNull(resolver.lastResolvedGenerations)
        assertEquals("命令は宣言の更新前に受けている", generationBefore, received)
        assertTrue("宣言の更新の後に引き直している (受けた世代 $received / 引き直した世代 $resolved)", resolved > received)
        val row = cellRow(layout, newRowId) ?: throw AssertionError(scrollSummary(layout))
        assertNear("追加した項目の Cell が表示範囲の上端に来る", listTop(layout), row.top)
    }

    @Test
    fun `DSL 方式でどちらにも無い値は何もしない`() {
        val controller = KsScrollController()
        composeRule.setContent {
            KsSettingsView(modifier = hostModifier, theme = Theme(), scrollController = controller) {
                fillerSections(0 until 10)
            }
        }
        val layout = awaitShown()
        val rv = layout.internalRecyclerView()
        val before = rv.computeVerticalScrollOffset()

        composeRule.runOnUiThread {
            controller.scrollTo("missing")
            controller.scrollToSection("missing")
            controller.scrollToEnd(animated = false)
        }
        awaitSettled(layout, 1)

        assertEquals("引き直せない命令は Host へ渡らず、末尾への命令だけが処理される", 1, layout.internalProcessedScrollEntryCount())
        assertTrue("末尾への命令は実行される", rv.computeVerticalScrollOffset() > before)
    }

    @Test
    fun `DSL 方式のハンドルは内部の受け口に接続され Host には直接つながない`() {
        val controller = KsScrollController()
        composeRule.setContent {
            KsSettingsView(modifier = hostModifier, theme = Theme(), scrollController = controller) {
                fillerSections(0 until 2)
            }
        }
        val layout = awaitShown()

        assertTrue("ハンドルは引き直しの受け口に接続される", controller.currentReceiver is DSLScrollCommandResolver)
        assertEquals("Host の scrollController には代入しない", null, layout.scrollController)
    }

    @Test
    fun `引き直す前にハンドルを差し替えるとその命令はどの Host も動かさない`() {
        val first = KsScrollController()
        val second = KsScrollController()
        var current by mutableStateOf<KsScrollController?>(first)
        composeRule.setContent {
            KsSettingsView(modifier = hostModifier, theme = Theme(), scrollController = current) {
                fillerSections(0 until 10)
            }
        }
        val layout = awaitShown()
        val rv = layout.internalRecyclerView()
        val resolver = findResolver(first)
        val before = rv.computeVerticalScrollOffset()

        // 命令がまだ受け口で引き直しを待っている間 (resolvePending より前) に差し替える。
        composeRule.runOnUiThread {
            first.scrollToEnd(animated = false)
            current = second
            // コンポジションの更新を待たずに、更新で行われるのと同じ接続の差し替えをこの時点で行う。
            resolver.connect(second)
            // 引き直しを駆動する側の実行を待たずに、この時点で引き直しを走らせる。
            resolver.resolvePending()
        }
        composeRule.waitForIdle()
        drainMainLooperForUnchangedCheck()
        composeRule.waitForIdle()

        assertEquals("外したハンドルの引き直し前の命令は Host へ届かない", 0, layout.internalProcessedScrollEntryCount())
        assertEquals("位置は変わらない", before, rv.computeVerticalScrollOffset())
        assertSame("差し替え後のハンドルは受け口に接続されている", resolver, second.currentReceiver)
    }

    @Test
    fun `Host へ渡った後の未実行の命令もハンドルの差し替えで捨てられ位置の復元は残る`() {
        val first = KsScrollController()
        val second = KsScrollController()
        var current by mutableStateOf<KsScrollController?>(first)
        composeRule.setContent {
            KsSettingsView(modifier = hostModifier, theme = Theme(), scrollController = current) {
                fillerSections(0 until 10)
            }
        }
        val layout = awaitShown()
        val rv = layout.internalRecyclerView()
        val resolver = findResolver(first)
        lateinit var anchor: jp.kamusoft.kssettingsview.ui.KsScrollAnchor
        composeRule.runOnUiThread {
            rv.scrollBy(0, 500)
            anchor = requireNotNull(layout.captureScrollAnchor()) { scrollSummary(layout) }
            rv.scrollBy(0, -10_000)
        }

        // 受け口が引き直して Host の待ち行列へ渡した後、Host が実行する前に差し替える。
        composeRule.runOnUiThread {
            layout.restoreScrollAnchor(anchor)
            first.scrollToEnd(animated = false)
            resolver.resolvePending()
            current = second
            // コンポジションの更新を待たずに、更新で行われるのと同じ接続の差し替えをこの時点で行う。
            resolver.connect(second)
            assertEquals("前提: 差し替えの時点で Host はまだ何も実行していない", 0, layout.internalProcessedScrollEntryCount())
        }
        awaitSettled(layout, 1)
        drainMainLooperForUnchangedCheck()
        composeRule.waitForIdle()

        assertEquals("位置の復元だけが処理され、末尾への命令は捨てられる", 1, layout.internalProcessedScrollEntryCount())
        assertEquals("控えた位置に戻っている", anchor, layout.captureScrollAnchor())
        assertSame("差し替え後のハンドルは受け口に接続されている", resolver, second.currentReceiver)
    }

    @Test
    fun `Host へ渡った後の未実行の命令もハンドルを外すと捨てられる`() {
        val first = KsScrollController()
        var current by mutableStateOf<KsScrollController?>(first)
        composeRule.setContent {
            KsSettingsView(modifier = hostModifier, theme = Theme(), scrollController = current) {
                fillerSections(0 until 10)
            }
        }
        val layout = awaitShown()
        val rv = layout.internalRecyclerView()
        val resolver = findResolver(first)
        val before = rv.computeVerticalScrollOffset()

        composeRule.runOnUiThread {
            first.scrollToEnd(animated = false)
            resolver.resolvePending()
            current = null
            resolver.connect(null)
        }
        composeRule.waitForIdle()
        drainMainLooperForUnchangedCheck()
        composeRule.waitForIdle()

        assertEquals("Host へ渡った未実行の命令も捨てられる", 0, layout.internalProcessedScrollEntryCount())
        assertEquals("位置は変わらない", before, rv.computeVerticalScrollOffset())
    }

    /** DSL 方式の引き直しの受け口 (ハンドルの接続先) を取り出す。 */
    private fun findResolver(controller: KsScrollController): DSLScrollCommandResolver =
        controller.currentReceiver as? DSLScrollCommandResolver
            ?: throw AssertionError("ハンドルが引き直しの受け口に接続されていない: ${controller.currentReceiver}")
}
