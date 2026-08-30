package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [CustomCell] の行が `RecyclerView` のリサイクル機構を実際に通ったときの、Composition と content
 * ツリーの扱いを検証する（android/ADR-0015）。
 *
 * # なぜ実 RecyclerView 経路で測るか
 *
 * 行の破棄境界は「プール滞在」「`itemViewCache` 滞在」「pooling container の解放」で異なり、これらは
 * `RecyclerView` の内部機構が決める。ViewHolder へ直接 `bind` / `reset` を呼ぶ形では、どの境界を
 * 通ったのかが再現できず、検証が実挙動から離れる。そのため本テストは Activity へ載せた
 * [KsSettingsView] をスクロールさせ、生成された行から観測する。
 *
 * # 観測点
 *
 * - Composition の生存・破棄: `ComposeView.hasComposition`
 * - 器の再利用: ViewHolder・`ComposeView`・埋め込み `View` のインスタンス同一性
 * - content の状態: builder が出力する `testTag`（実際に走った composition の結果）と、builder 内から
 *   書き戻すカウンタ
 *
 * プール経路を測るテストは `setItemViewCacheSize(0)` で `itemViewCache` を除外し、行が必ず
 * `RecycledViewPool` を経由するようにする。cache 経路を測るテストは既定設定のまま行う。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CustomCellRecycleTest {

    private lateinit var activity: ComponentActivity
    private lateinit var container: FrameLayout
    private lateinit var settingsView: KsSettingsView
    private lateinit var frameDriver: ComposeFrameDriver

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        settingsView = KsSettingsView(activity)
        container = FrameLayout(activity)
        frameDriver = ComposeFrameDriver()
        // composition が作られる前に差し込む。
        frameDriver.installOn(container)
        container.addView(settingsView)
        activity.setContentView(container)
    }

    @After
    fun tearDown() {
        frameDriver.stop()
    }

    // MARK: - プール生存と破棄境界

    @Test
    fun `行がプールへ入っても Composition は生存しプール放逐で破棄される`() {
        showCells(customCellsAt(0, 1, 2, fillerCount = 60))
        recyclerView.setItemViewCacheSize(0)
        pump()

        val composeViews = visibleCustomComposeViews()
        assertTrue("前提: CustomCell 行が表示されていない", composeViews.isNotEmpty())
        assertTrue(
            "前提: 表示中の行に Composition がない",
            composeViews.all { it.hasComposition },
        )

        scrollToEnd()
        assertNull("前提: 先頭の CustomCell 行が画面外へ出ていない", holderAt(0))
        assertTrue(
            "プール滞在中の行の Composition が破棄されている",
            composeViews.all { it.hasComposition },
        )

        recyclerView.recycledViewPool.clear()

        assertTrue(
            "プールからの放逐で Composition が破棄されない",
            composeViews.none { it.hasComposition },
        )
    }

    @Test
    fun `ホストの解放で保持中の行の Composition が破棄される`() {
        showCells(customCellsAt(0, 1, 2, fillerCount = 60))
        recyclerView.setItemViewCacheSize(0)
        pump()

        val visible = visibleCustomComposeViews()
        assertTrue("前提: CustomCell 行が表示されていない", visible.isNotEmpty())

        // 表示中の行とプール滞在中の行の両方を対象にするため、一部だけ画面外へ送る。
        scrollToEnd()
        val pooled = visible.toList()
        scrollToStart()
        val onScreen = visibleCustomComposeViews()
        assertTrue("前提: 再表示された行がない", onScreen.isNotEmpty())

        container.removeView(settingsView)
        idle()

        assertTrue(
            "window から外れても Composition が残っている（リーク）",
            (pooled + onScreen).none { it.hasComposition },
        )
    }

    @Test
    fun `itemViewCache 経由の再表示では content の状態と購読が維持される`() {
        var bump: (() -> Unit)? = null
        var disposeCount = 0
        val cell = CustomCell(id = "stateful", content = "x") {
            var counter by remember { mutableStateOf(0) }
            bump = { counter += 1 }
            DisposableEffect(Unit) { onDispose { disposeCount += 1 } }
            Box(Modifier.testTag("counter-$counter").fillMaxWidth().height(ROW_HEIGHT_DP.dp))
        }
        showCells(listOf(cell) + fillerCells(60))
        // 既定の itemViewCache（非ゼロ）のまま検証する。

        val holder = holderAt(0)
        assertNotNull("前提: 先頭行が生成されていない", holder)
        bump!!.invoke()
        pump()
        assertTrue("前提: 内部状態を初期値から動かせていない", tagsOf(holder!!.itemView).contains("counter-1"))

        // 1 行分だけスクロールして itemViewCache へ送り、そのまま戻す。
        scrollBy(ROW_HEIGHT_DP * 2)
        assertNull("前提: 先頭行が画面外へ出ていない", holderAt(0))
        scrollBy(-ROW_HEIGHT_DP * 2)

        val restored = holderAt(0)
        assertSame("cache 経由の再表示で ViewHolder が入れ替わっている", holder, restored)
        assertTrue(
            "cache 経由の再表示で content の状態が失われている",
            tagsOf(restored!!.itemView).contains("counter-1"),
        )
        assertEquals("cache 滞在中に購読が打ち切られている", 0, disposeCount)
    }

    // MARK: - content ノードツリーの再利用

    @Test
    fun `同一ラップ関数 builder 間で埋め込み View が再利用される`() {
        val probe = InteropProbe()
        // CustomCell はリストの両端にだけ置き、間を埋め草で離す。CustomCell 用の ViewHolder は
        // 1 つしか要らないため、端から端へスクロールすると同じ ViewHolder が使い回される。
        showCells(
            buildList {
                add(CustomCell(id = "a", content = "a", builder = probe.builder))
                addAll(fillerCells(60))
                add(CustomCell(id = "b", content = "b", builder = probe.builder))
            },
        )
        recyclerView.setItemViewCacheSize(0)
        pump()

        val firstHolder = holderAt(0)
        assertNotNull("前提: 先頭の CustomCell 行が生成されていない", firstHolder)
        assertEquals("前提: 埋め込みが 1 つだけ生成されていない", 1, probe.factoryCount)
        val firstView = probe.views.single()

        scrollToEnd()
        val lastHolder = holderAt(itemCount - 1)
        assertNotNull("前提: 末尾の CustomCell 行が生成されていない", lastHolder)

        assertSame("再 bind で ViewHolder が使い回されていない", firstHolder, lastHolder)
        assertEquals("埋め込みの factory が再実行されている", 1, probe.factoryCount)
        assertSame("埋め込み View のインスタンスが入れ替わっている", firstView, probe.views.single())
        assertTrue("再利用時に onReset が呼ばれていない", probe.resetCount >= 1)
        assertSame(
            "再利用後の行に同じ埋め込み View が現れていない",
            firstView,
            firstInteropProbe(lastHolder!!.itemView),
        )
        assertEquals("再利用後に新しい値が反映されていない", "b", firstView.boundValue)
    }

    @Test
    fun `構造が異なる builder 間の再 bind でも新しい出力だけが現れる`() {
        showCells(
            buildList {
                add(CustomCell(id = "a", content = "a", builder = taggedBuilder("alpha")))
                addAll(fillerCells(60))
                add(CustomCell(id = "b", content = "b", builder = taggedBuilder("beta")))
            },
        )
        recyclerView.setItemViewCacheSize(0)
        pump()

        val firstHolder = holderAt(0)
        assertNotNull("前提: 先頭の CustomCell 行が生成されていない", firstHolder)
        assertTrue(tagsOf(firstHolder!!.itemView).contains("alpha-a"))

        scrollToEnd()
        val lastHolder = holderAt(itemCount - 1)
        assertSame("前提: ViewHolder が使い回されていない", firstHolder, lastHolder)

        val tags = tagsOf(lastHolder!!.itemView)
        assertTrue("新しい builder の出力が現れていない", tags.contains("beta-b"))
        assertFalse("前の builder の出力が表示ツリーに残っている", tags.any { it.startsWith("alpha") })
    }

    // MARK: - content 状態の行間隔離

    @Test
    fun `間に再 composition を挟まない再 bind でも remember が持ち越されない`() {
        var initCount = 0
        var disposeCount = 0
        val builder: @Composable (String) -> Unit = { value ->
            // 初期化式が再実行されたかどうかを世代番号で見る。持ち越されると番号が変わらない。
            val generation = remember { initCount += 1; initCount }
            DisposableEffect(Unit) { onDispose { disposeCount += 1 } }
            Box(Modifier.testTag("$value-$generation").fillMaxWidth().height(ROW_HEIGHT_DP.dp))
        }
        val cellB = CustomCell(id = "b", content = "b", builder = builder)
        showCells(listOf(CustomCell(id = "a", content = "a", builder = builder)) + fillerCells(10))

        val holder = holderAt(0)
        assertNotNull("前提: 先頭の CustomCell 行が生成されていない", holder)
        assertTrue("前提: A の content が現れていない", tagsOf(holder!!.itemView).contains("a-1"))

        // リサイクルを挟まず、同じ ViewHolder を別 Cell の行として直接 bind し直す。非活性化が
        // 挟まらないため、行間の隔離は Cell の同一性キーだけが担う。
        holder.bind(cellB, Theme())
        settle()

        val tags = tagsOf(holder.itemView)
        assertTrue("B の content が初期状態で現れていない", tags.contains("b-2"))
        assertFalse("A の remember が持ち越されている", tags.any { it.startsWith("a-") })
        assertTrue("A の DisposableEffect が dispose されていない", disposeCount >= 1)
    }

    @Test
    fun `別 Cell への再 bind では remember が持ち越されず DisposableEffect が dispose される`() {
        var bump: (() -> Unit)? = null
        var disposeCount = 0
        val builder: @Composable (String) -> Unit = { value ->
            var counter by remember { mutableStateOf(0) }
            bump = { counter += 1 }
            DisposableEffect(Unit) { onDispose { disposeCount += 1 } }
            Box(Modifier.testTag("$value-$counter").fillMaxWidth().height(ROW_HEIGHT_DP.dp))
        }
        showCells(
            buildList {
                add(CustomCell(id = "a", content = "a", builder = builder))
                addAll(fillerCells(60))
                add(CustomCell(id = "b", content = "b", builder = builder))
            },
        )
        recyclerView.setItemViewCacheSize(0)
        pump()

        val holder = holderAt(0)
        assertNotNull("前提: 先頭の CustomCell 行が生成されていない", holder)
        bump!!.invoke()
        pump()
        assertTrue("前提: 内部状態を初期値から動かせていない", tagsOf(holder!!.itemView).contains("a-1"))
        assertEquals("前提: 先に dispose が走っている", 0, disposeCount)

        scrollToEnd()
        val reused = holderAt(itemCount - 1)
        assertSame("前提: ViewHolder が使い回されていない", holder, reused)

        val tags = tagsOf(reused!!.itemView)
        assertTrue("新しい Cell の content が初期状態で現れていない", tags.contains("b-0"))
        assertFalse("前の Cell の remember が持ち越されている", tags.any { it.startsWith("a-") })
        assertTrue("前の content の dispose が実行されていない", disposeCount >= 1)
    }

    // MARK: - reset による状態破棄と参照切断

    @Test
    fun `リサイクルされた行は前の content と listener を保持しない`() {
        showCells(
            buildList {
                add(
                    CustomCell(
                        id = "a",
                        content = "a",
                        onTap = { },
                        builder = taggedBuilder("alpha"),
                    ),
                )
                addAll(fillerCells(60))
            },
        )
        recyclerView.setItemViewCacheSize(0)
        pump()

        val holder = holderAt(0)
        assertNotNull("前提: 先頭の CustomCell 行が生成されていない", holder)
        assertTrue(tagsOf(holder!!.itemView).contains("alpha-a"))
        assertTrue("前提: タップ listener が設定されていない", holder.itemView.hasOnClickListeners())

        scrollToEnd()

        assertFalse("前の content が表示ツリーに残っている", tagsOf(holder.itemView).contains("alpha-a"))
        assertFalse("前のタップ listener が残っている", holder.itemView.hasOnClickListeners())
        assertFalse(holder.itemView.isClickable)
    }

    // MARK: - 表示のためのヘルパ

    private val recyclerView: RecyclerView
        get() = settingsView.internalRecyclerView()

    private val itemCount: Int
        get() = recyclerView.adapter!!.itemCount

    private fun showCells(cells: List<Cell>) {
        settingsView.setRootDirect(
            SettingsRoot(sections = listOf(Section(id = "s1", cells = cells))),
            theme = PINNED_GEOMETRY_THEME,
        )
        pump()
    }

    /** 高さ [ROW_HEIGHT_DP] の [LabelCell] を [count] 個作る（スクロール量を稼ぐ埋め草）。 */
    private fun fillerCells(count: Int): List<Cell> =
        (0 until count).map { LabelCell(id = "f$it", title = "filler $it") }

    private fun customCellsAt(
        vararg indices: Int,
        fillerCount: Int,
        builder: @Composable (String) -> Unit = taggedBuilder("probe"),
    ): List<Cell> {
        val customs = indices.map { CustomCell(id = "c$it", content = "c$it", builder = builder) }
        return customs + fillerCells(fillerCount)
    }

    /** content の値を tag に載せるだけの builder。 */
    private fun taggedBuilder(prefix: String): @Composable (String) -> Unit = { value ->
        Box(
            Modifier
                .testTag("$prefix-$value")
                .fillMaxWidth()
                .height(ROW_HEIGHT_DP.dp),
        )
    }

    /**
     * 差分計算の完了を待ってレイアウトを確定させる。
     *
     * `submitList` の差分計算は更新前後がどちらも非空のときバックグラウンドスレッドへ回り、結果は
     * メインスレッドへ post されてから反映される。単発の `idle()` では取りこぼすため、CPU を譲りつつ
     * 繰り返してからレイアウトを走らせる。
     */
    private fun pump() {
        repeat(PUMP_ROUNDS) {
            idle()
            Thread.yield()
        }
        settle()
    }

    /** 保留中の処理を流し切り、実寸でレイアウトを確定させる。 */
    private fun settle() {
        idle()
        frameDriver.frame()
        val metrics = activity.resources.displayMetrics
        container.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        container.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        frameDriver.frame()
        idle()
    }

    /**
     * 末尾まで少しずつスクロールする。
     *
     * `scrollToPosition` による位置指定のジャンプは全行の再レイアウトになり、行は一時 detach を経て
     * 取り外される。実際の指スクロールが通る「1 行ずつの取り外し」とは経路が異なるため、破棄境界の
     * 検証には使えない。刻んだスクロールで実経路をなぞる。
     */
    private fun scrollToEnd() {
        var guard = 0
        while (recyclerView.canScrollVertically(1) && guard++ < MAX_SCROLL_STEPS) {
            scrollBy(SCROLL_STEP_DP)
        }
    }

    /** 先頭まで少しずつスクロールして戻す。 */
    private fun scrollToStart() {
        var guard = 0
        while (recyclerView.canScrollVertically(-1) && guard++ < MAX_SCROLL_STEPS) {
            scrollBy(-SCROLL_STEP_DP)
        }
    }

    private fun scrollBy(dp: Int) {
        recyclerView.scrollBy(0, (dp * activity.resources.displayMetrics.density).toInt())
        settle()
    }

    private fun holderAt(position: Int): CustomCellViewHolder? =
        recyclerView.findViewHolderForAdapterPosition(position) as? CustomCellViewHolder

    private fun visibleCustomComposeViews(): List<ComposeView> =
        (0 until recyclerView.childCount)
            .mapNotNull { recyclerView.getChildAt(it) as? ComposeView }

    // MARK: - composition の観測ヘルパ

    private fun semanticsRoot(view: View): SemanticsNode? {
        if (view is ViewRootForTest) return view.semanticsOwner.rootSemanticsNode
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                semanticsRoot(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun tagsOf(view: View): List<String> {
        val root = semanticsRoot(view) ?: return emptyList()
        val out = mutableListOf<String>()
        fun walk(node: SemanticsNode) {
            node.config.getOrNull(SemanticsProperties.TestTag)?.let(out::add)
            node.children.forEach(::walk)
        }
        walk(root)
        return out
    }

    private fun firstInteropProbe(view: View): InteropProbeView? {
        if (view is InteropProbeView) return view
        if (view !is ViewGroup) return null
        for (i in 0 until view.childCount) {
            firstInteropProbe(view.getChildAt(i))?.let { return it }
        }
        return null
    }

    /**
     * `onReset` を指定した `AndroidView`（reusable なノードになる）を出力する観測用 builder。
     *
     * 埋め込み View の生成回数・再利用時の `onReset` 呼び出し・反映された値を数えることで、ノードが
     * 作り直されたのか再利用されたのかを撃ち分ける。
     */
    private class InteropProbe {
        var factoryCount: Int = 0
            private set
        var resetCount: Int = 0
            private set
        val views: MutableList<InteropProbeView> = mutableListOf()

        val builder: @Composable (String) -> Unit = { value ->
            AndroidView(
                factory = { context ->
                    factoryCount += 1
                    InteropProbeView(context).also { views += it }
                },
                modifier = Modifier.fillMaxWidth(),
                onReset = {
                    resetCount += 1
                    it.boundValue = null
                },
                update = { it.boundValue = value },
            )
        }
    }

    /** 埋め込みの同一性と反映値を観測するための View。 */
    private class InteropProbeView(context: Context) : View(context) {
        var boundValue: String? = null

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            setMeasuredDimension(
                resolveSize(0, widthMeasureSpec),
                (ROW_HEIGHT_DP * resources.displayMetrics.density).toInt(),
            )
        }
    }

    private companion object {
        /**
         * Section 既定 margin を 0 に固定した Theme。
         *
         * 本テストは「画面 1 枚分の高さに何行が同時に載るか」という幾何を前提に、端から端への
         * スクロールで ViewHolder が使い回されること（＝同時に生きる CustomCell 行は 1 つだけ）を
         * 観察する。ライブラリ既定の Section margin が変わるとその幾何が動いてしまうため、
         * 既定値から独立させて 0 に固定する。
         */
        val PINNED_GEOMETRY_THEME: Theme = Theme(sectionMargin = PaddingValues(0.dp))

        /** 埋め草・probe の行高さ（dp）。 */
        const val ROW_HEIGHT_DP: Int = 48

        /** 差分コミット待ちで main looper を回す回数。 */
        const val PUMP_ROUNDS: Int = 30

        /** 刻みスクロール 1 回分の移動量（dp）。 */
        const val SCROLL_STEP_DP: Int = ROW_HEIGHT_DP * 2

        /** 刻みスクロールの打ち切り回数（無限ループ防止）。 */
        const val MAX_SCROLL_STEPS: Int = 200
    }
}
