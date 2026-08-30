package jp.kamusoft.kssettingsview.bridge

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * 行のリサイクルで content が非活性化される（deactivate）経路を明示的に通し、Bridge 経由で埋め込んだ
 * platform view が保全されることを検証する。
 *
 * # なぜ専用のテストが要るか
 *
 * `RecyclerView` の位置指定ジャンプ（`scrollToPosition`）は全行の作り直しになり、行の
 * `ComposeView` は window から外れた時点で composition ごと破棄される。この経路では非活性化が起きず、
 * 埋め込みは「作り直し」だけを通る。実際の指スクロールが通るのは 1 行ずつの取り外しであり、そこでは
 * composition がプール滞在中も生き残り、リサイクル時に content が非活性化される。埋め込みの
 * 取り外しはこの非活性化の副作用として起きるため、刻んだスクロールで実経路をなぞる必要がある。
 *
 * # 観測点
 *
 * - Composition の生存: `ComposeView.hasComposition`
 * - 器の再利用: ViewHolder と `ComposeView` のインスタンス同一性
 * - 埋め込みの保全: 内容 View のインスタンス同一性と、live な view 階層への出入り回数
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsBridgeCustomCellDeactivateTest {

    /**
     * 行の内容として埋め込む観測用の View。
     *
     * 破棄（dispose）には決定的な観測点がないため、「破棄されていない」ことは同一インスタンスが保持する
     * 状態（[marker]）が再表示をまたいで残ることで測る。
     */
    private class ProbeContentView(context: Context) : View(context) {

        /** live な view 階層へ入った回数。 */
        var attachCount: Int = 0
            private set

        /** live な view 階層から外れた回数。 */
        var detachCount: Int = 0
            private set

        /** 呼び出し側が書き込む任意の状態。作り直されていないことの裏付けに使う。 */
        var marker: String = ""

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            setMeasuredDimension(resolveSize(0, widthMeasureSpec), CONTENT_HEIGHT_PX)
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            attachCount++
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            detachCount++
        }
    }

    private lateinit var controller: ActivityController<KsBridgeTestHost.HostActivity>
    private lateinit var frameDriver: ComposeFrameDriver

    @Before
    fun setUp() {
        controller = Robolectric.buildActivity(KsBridgeTestHost.HostActivity::class.java).setup()
        frameDriver = ComposeFrameDriver()
        // Host を載せる前に差し込む（composition の生成より先である必要がある）。
        frameDriver.installOn(controller.get().container)
    }

    @After
    fun tearDown() {
        frameDriver.stop()
    }

    @Test
    fun `リサイクルを挟んだ再表示で同一 platform view が再親付けされる`() {
        val probe = ProbeContentView(controller.get())
        probe.marker = "内容"
        val host = attachHost(probes = listOf(probe), fillerCount = 60)

        // プール投入を保証するため itemViewCache を無効化する。
        host.recyclerView.setItemViewCacheSize(0)
        settle(host)

        val holder = holderAt(host, CELL_ROW)
        assertNotNull("前提: 内容を持つ行が生成されていない", holder)
        val composeView = holder!!.itemView as ComposeView
        assertSame("前提: 埋め込みが表示されていない", probe, firstProbe(composeView))
        val attachBefore = probe.attachCount

        scrollToEnd(host)

        assertNull("前提: 内容を持つ行が画面外へ出ていない", holderAt(host, CELL_ROW))
        assertTrue("プール滞在中に Composition が破棄されている", composeView.hasComposition)
        assertTrue("非活性化で埋め込みが取り外されていない", probe.detachCount >= 1)
        assertEquals("非活性化で内容の状態が失われている", "内容", probe.marker)

        scrollToStart(host)

        val restored = holderAt(host, CELL_ROW)
        assertNotNull("再表示で行が生成されていない", restored)
        assertSame("再表示で ViewHolder が入れ替わっている", holder, restored)
        assertSame("再表示で ComposeView が入れ替わっている", composeView, restored!!.itemView)
        assertSame("再表示で同一の埋め込み View が再親付けされていない", probe, firstProbe(composeView))
        assertTrue("再表示で埋め込みが取り付け直されていない", probe.attachCount > attachBefore)
        assertEquals("再表示で内容の状態が失われている", "内容", probe.marker)
    }

    @Test
    fun `非活性化は表示中の他の行の埋め込みを取り外さない`() {
        val recycled = ProbeContentView(controller.get())
        val kept = ProbeContentView(controller.get())
        val host = attachHost(probes = listOf(recycled, kept), fillerCount = 60)

        host.recyclerView.setItemViewCacheSize(0)
        settle(host)

        assertSame("前提: 1 行目の埋め込みが表示されていない", recycled, embeddedProbeAt(host, CELL_ROW))
        assertSame("前提: 2 行目の埋め込みが表示されていない", kept, embeddedProbeAt(host, CELL_ROW + 1))
        val keptDetachBefore = kept.detachCount

        // 1 行目が画面外へ出た時点で止める（2 行目を巻き込まないよう細かく送る）。
        var steps = 0
        while (holderAt(host, CELL_ROW) != null && steps++ < MAX_SCROLL_STEPS) {
            scrollBy(host, FINE_SCROLL_STEP_PX)
        }

        assertNull("前提: 1 行目が画面外へ出ていない", holderAt(host, CELL_ROW))
        assertTrue("前提: 1 行目の埋め込みが取り外されていない", recycled.detachCount >= 1)
        assertNotNull("前提: 2 行目が画面外へ出ている", holderAt(host, CELL_ROW + 1))
        assertSame(
            "表示中の行の埋め込みが奪われている",
            kept,
            embeddedProbeAt(host, CELL_ROW + 1),
        )
        assertEquals(
            "表示中の行の埋め込みが取り外されている",
            keptDetachBefore,
            kept.detachCount,
        )
        assertTrue("表示中の行の埋め込みが window から外れている", kept.isAttachedToWindow)
    }

    // MARK: - 表示のためのヘルパ

    /** 先頭に内容付きの Bridge CustomCell を並べ、残りを LabelCell で埋めた Host を載せる。 */
    private fun attachHost(
        probes: List<ProbeContentView>,
        fillerCount: Int,
    ): KsBridgeTestHost.Attachment {
        val bridge = KsSettingsBridge()
        val builder = KsBridgeRootBuilder()
        val section = builder.addSection(headerText = "S1", footerText = null)
        probes.forEachIndexed { index, probe ->
            builder.addCell(
                KsBridgeCustomCell(title = "").apply {
                    view = probe
                    contentToken = "token-$index"
                },
                section.sectionID,
            )
        }
        repeat(fillerCount) { index ->
            builder.addCell(KsBridgeLabelCell(title = "filler $index"), section.sectionID)
        }
        bridge.setRoot(builder)

        val attachment = KsBridgeTestHost.attach(bridge, controller)
        settle(attachment)
        return attachment
    }

    private fun holderAt(
        host: KsBridgeTestHost.Attachment,
        position: Int,
    ): RecyclerView.ViewHolder? = host.recyclerView.findViewHolderForAdapterPosition(position)

    private fun embeddedProbeAt(
        host: KsBridgeTestHost.Attachment,
        position: Int,
    ): ProbeContentView? = holderAt(host, position)?.let { firstProbe(it.itemView) }

    /** 子孫を深さ優先でたどり、最初に見つかった観測用 View を返す。 */
    private fun firstProbe(view: View): ProbeContentView? {
        if (view is ProbeContentView) return view
        if (view !is ViewGroup) return null
        for (index in 0 until view.childCount) {
            firstProbe(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun scrollToEnd(host: KsBridgeTestHost.Attachment) {
        var guard = 0
        while (host.recyclerView.canScrollVertically(1) && guard++ < MAX_SCROLL_STEPS) {
            scrollBy(host, SCROLL_STEP_PX)
        }
    }

    private fun scrollToStart(host: KsBridgeTestHost.Attachment) {
        var guard = 0
        while (host.recyclerView.canScrollVertically(-1) && guard++ < MAX_SCROLL_STEPS) {
            scrollBy(host, -SCROLL_STEP_PX)
        }
    }

    private fun scrollBy(host: KsBridgeTestHost.Attachment, dy: Int) {
        host.recyclerView.scrollBy(0, dy)
        settle(host)
    }

    /** 保留中の処理とフレームを流し、レイアウトを確定させる。 */
    private fun settle(host: KsBridgeTestHost.Attachment) {
        frameDriver.frame()
        KsBridgeTestHost.pump(host)
        frameDriver.frame()
    }

    private companion object {
        /** Section header の次、最初の Cell 行の位置。 */
        const val CELL_ROW: Int = 1

        /** 観測用 View が要求する高さ（px）。 */
        const val CONTENT_HEIGHT_PX: Int = 40

        /** 行を 1 つずつ画面外へ送るための細かい移動量（px）。 */
        const val FINE_SCROLL_STEP_PX: Int = 8

        /** 刻みスクロール 1 回分の移動量（px）。 */
        const val SCROLL_STEP_PX: Int = 96

        /** 刻みスクロールの打ち切り回数（無限ループ防止）。 */
        const val MAX_SCROLL_STEPS: Int = 300
    }
}
