package jp.kamusoft.kssettingsview.bridge

import android.content.Context
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import jp.kamusoft.kssettingsview.ui.CustomCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * 受け取った CustomCell のタップだけを記録する listener 実装。
 */
private class CustomCellTapRecorder : KsBridgeInteractionListener {

    /** 通知された cellID の並び。 */
    val tappedCellIDs: MutableList<String> = mutableListOf()

    override fun customCellTapped(cellID: String) {
        tappedCellIDs.add(cellID)
    }

    override fun commandCellTapped(cellID: String) = Unit
    override fun buttonCellTapped(cellID: String) = Unit
    override fun switchCellChanged(cellID: String, isOn: Boolean) = Unit
    override fun checkboxCellChanged(cellID: String, isChecked: Boolean) = Unit
    override fun simpleCheckCellChanged(cellID: String, isChecked: Boolean) = Unit
    override fun radioCellSelected(cellID: String, value: String) = Unit
    override fun entryCellTextChanged(cellID: String, text: String) = Unit
    override fun pickerCellSelectionChanged(cellID: String, index: Int) = Unit
    override fun pickerCellMultiSelectionChanged(cellID: String, indices: IntArray) = Unit
    override fun numberPickerCellChanged(cellID: String, value: Int) = Unit
    override fun timePickerCellChanged(cellID: String, time: String) = Unit
    override fun datePickerCellChanged(cellID: String, date: String) = Unit
}

/**
 * interop 境界を越えて渡した `View` が CustomCell の内容として表示されるまでと、その View
 * インスタンスがトークンの変化にだけ追従することを実描画で検証する。
 *
 * # 安定性の観測点
 *
 * 埋め込みの作り直し（materialize）と剥がし（detach）は、内容 View が live な view 階層へ
 * 出入りした回数で測る。埋め込みが作り直されると内容 View は旧 holder から外されて新しい holder へ
 * 入り直すため、window への attach / detach として観測できる。
 *
 * Android には破棄（dispose）の決定的な観測点がなく、`AndroidView` は内容 View を取り外すだけで
 * 破棄しない。そこで「破棄されていない」ことは、同一インスタンスが保持する状態
 * （[ProbeContentView.marker]）が再発行をまたいで残ることで測る。
 *
 * # ホスト Activity を閉じない理由
 *
 * 内容の埋め込みは Compose のコンポジション上にあり、再発行の反映には再コンポジションが要る。
 * Robolectric では一度 Activity を破棄すると同一 JVM 内の後続テストで再コンポジションが走らなく
 * なり、「内容が変わらないこと」を測る検証がすべて空振りで緑になる。そのため本テストは Activity を
 * 明示的に閉じず、テスト間の後始末は Robolectric の環境リセットに委ねる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsBridgeCustomCellTest {

    /**
     * 行の内容として埋め込む観測用の View。
     *
     * 自分で必要な高さを答え、live な view 階層への出入りを数える。
     */
    private class ProbeContentView(
        context: Context,
        var contentHeight: Int = 40,
    ) : View(context) {

        /** live な view 階層へ入った回数。 */
        var attachCount: Int = 0
            private set

        /** live な view 階層から外れた回数。 */
        var detachCount: Int = 0
            private set

        /** 入った先の親の並び。作り直された埋め込みの数を数えるために使う。 */
        val parents: MutableList<ViewParentIdentity> = mutableListOf()

        /** 呼び出し側が書き込む任意の状態。作り直されていないことの裏付けに使う。 */
        var marker: String = ""

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            setMeasuredDimension(resolveSize(0, widthMeasureSpec), contentHeight)
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            attachCount++
            parents.add(ViewParentIdentity(parent))
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            detachCount++
        }
    }

    /** 親の同一性だけを比較するための包み。 */
    private class ViewParentIdentity(val parent: Any?) {
        override fun equals(other: Any?): Boolean =
            other is ViewParentIdentity && other.parent === parent

        override fun hashCode(): Int = System.identityHashCode(parent)
    }

    /** 観測用 View を生成する Context。 */
    private val appContext: Context
        get() = ApplicationProvider.getApplicationContext()

    /** 指定した計測高さの観測用 View を生成する。 */
    private fun probe(height: Int = 40): ProbeContentView = ProbeContentView(appContext, height)

    /** 指定 View を内容に持つ DTO を組み立てる。 */
    private fun customCell(
        view: View?,
        token: String,
        hasTapHandler: Boolean = false,
    ): KsBridgeCustomCell = KsBridgeCustomCell(title = "").apply {
        this.view = view
        this.contentToken = token
        this.hasTapHandler = hasTapHandler
    }

    /** 指定行の itemView を返す（未描画なら `null`）。 */
    private fun itemViewAt(host: KsBridgeTestHost.Attachment, position: Int): View? =
        host.recyclerView.findViewHolderForAdapterPosition(position)?.itemView

    /** 指定行に実描画されている観測用 View を返す。 */
    private fun embeddedProbe(
        host: KsBridgeTestHost.Attachment,
        position: Int = CELL_ROW,
    ): ProbeContentView? = itemViewAt(host, position)?.let { firstProbe(it) }

    /** 指定行の描画に観測用 View が含まれているかを返す。 */
    private fun isDisplayedInRow(
        view: ProbeContentView,
        host: KsBridgeTestHost.Attachment,
        position: Int = CELL_ROW,
    ): Boolean = embeddedProbe(host, position) === view

    /**
     * 指定行に埋め込まれた内容の中心へ、押して離す一連のタッチを実際に流す。
     *
     * 行の `performClick()` はタッチの配送経路を通らないため、埋め込まれた内容と行のどちらが
     * タッチを引き取るかを測れない。実イベントを流すことで、その取り合いの結果まで観測する。
     */
    private fun touchContent(host: KsBridgeTestHost.Attachment, position: Int = CELL_ROW) {
        val itemView = itemViewAt(host, position) ?: error("position $position の行が実描画されていない")
        val content = firstProbe(itemView) ?: error("position $position の行に内容が埋め込まれていない")

        val rowOrigin = IntArray(2).also { itemView.getLocationOnScreen(it) }
        val contentOrigin = IntArray(2).also { content.getLocationOnScreen(it) }
        val x = (contentOrigin[0] - rowOrigin[0] + content.width / 2).toFloat()
        val y = (contentOrigin[1] - rowOrigin[1] + content.height / 2).toFloat()

        val downTime = SystemClock.uptimeMillis()
        itemView.dispatchTouchEvent(
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0),
        )
        itemView.dispatchTouchEvent(
            MotionEvent.obtain(downTime, downTime + TOUCH_DURATION_MS, MotionEvent.ACTION_UP, x, y, 0),
        )
        // タップの確定はコンポジションのコルーチンで行われるため、その実行まで進める。
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** 子孫を深さ優先でたどり、最初に見つかった観測用 View を返す。 */
    private fun firstProbe(view: View): ProbeContentView? {
        if (view is ProbeContentView) return view
        if (view !is ViewGroup) return null
        for (index in 0 until view.childCount) {
            firstProbe(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    // MARK: - DTO の変換

    /** DTO の各項目が Native の CustomCell へ写る。 */
    @Test
    fun `CustomCell DTO が CustomCell へ変換される`() {
        val dto = customCell(probe(), token = "token-1", hasTapHandler = true).apply {
            showArrowIndicator = true
            isEnabled = false
            isVisible = false
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))

        val cell: CustomCell<*>? = KsBridgeFixture.storedCell(bridge)
        assertEquals("content にはトークンが格納される", "token-1", cell?.content)
        assertEquals(true, cell?.showArrow)
        assertEquals(false, cell?.isEnabled)
        assertEquals(false, cell?.isVisible)
        assertNotNull("タップ購読ありでは行タップのコールバックが注入される", cell?.onTap)
    }

    /** タップ購読なしの DTO は行タップ動作を持たない。 */
    @Test
    fun `タップ購読なしの DTO は行タップ動作を持たない`() {
        val bridge = KsBridgeFixture.withCells(listOf(customCell(probe(), token = "token-1")))

        val cell: CustomCell<*>? = KsBridgeFixture.storedCell(bridge)
        assertNull("購読なしの行は onTap を持たず、内容の中の操作を妨げない", cell?.onTap)
    }

    /** 等価性はトークンで決まり、View インスタンスの違いは参加しない。 */
    @Test
    fun `同一トークンの CustomCell は等価になる`() {
        val id = "cell-1"
        val relay = KsBridgeInteractionRelay()
        val left = customCell(probe(), token = "token-1").makeCell(id, relay)
        val right = customCell(probe(), token = "token-1").makeCell(id, relay)

        assertEquals("等価性はトークンで決まり、View の違いは参加しない", left, right)

        val changed = customCell(probe(), token = "token-2").makeCell(id, relay)
        assertNotEquals("トークンが変われば等価ではなくなる", left, changed)
    }

    // MARK: - 実描画

    /** `setRoot` で輸送した View が行の内容として表示される。 */
    @Test
    fun `setRoot で輸送した view が行の内容として表示される`() {
        val probe = probe()
        val bridge = KsBridgeFixture.withCells(listOf(customCell(probe, token = "token-1")))

        val host = KsBridgeTestHost.attach(bridge)

        assertSame("輸送した View インスタンスがそのまま行に表示される", probe, embeddedProbe(host))
        assertEquals(
            "内容は行の幅いっぱいに描画される",
            itemViewAt(host, CELL_ROW)?.width,
            probe.width,
        )
    }

    /** 既に別の親に付いている View を輸送しても、行の内容として取り付け直される。 */
    @Test
    fun `既存の親を持つ view も行の内容として取り付けられる`() {
        val probe = probe()
        val previousParent = android.widget.FrameLayout(appContext)
        previousParent.addView(probe)
        assertSame("前提: 別の親に付いていない", previousParent, probe.parent)

        val bridge = KsBridgeFixture.withCells(listOf(customCell(probe, token = "token-1")))
        val host = KsBridgeTestHost.attach(bridge)

        assertSame("既存の親から切り離して行へ取り付けられる", probe, embeddedProbe(host))
        assertEquals("元の親からは外れる", 0, previousParent.childCount)
    }

    /** `replaceCells` の一括更新経路でも輸送した View が表示される。 */
    @Test
    fun `replaceCells で輸送した view が行の内容として表示される`() {
        val first = probe()
        val second = probe()
        val dto = customCell(first, token = "token-1")
        val bridge = KsBridgeFixture.withCells(listOf(dto, KsBridgeLabelCell(title = "後続")))
        val host = KsBridgeTestHost.attach(bridge)

        bridge.replaceCells(
            listOf(
                KsBridgeCellUpdate(dto.cellID, customCell(second, token = "token-2")),
                KsBridgeCellUpdate(KsBridgeFixture.unusedIdentifier(), KsBridgeLabelCell(title = "X")),
            ),
        )
        KsBridgeTestHost.pump(host)

        assertSame("バッチ更新でも輸送した View が行に表示される", second, embeddedProbe(host))
    }

    /** View 未指定の DTO は空の内容の行になる。 */
    @Test
    fun `view 未指定の DTO は空の内容の行になる`() {
        val bridge = KsBridgeFixture.withCells(
            listOf(customCell(view = null, token = "token-1"), KsBridgeLabelCell(title = "後続")),
        )

        val host = KsBridgeTestHost.attach(bridge)

        assertNotNull("内容なしでも行そのものは出力される", itemViewAt(host, CELL_ROW))
        assertEquals(
            "後続の行も通常どおり並ぶ",
            listOf("S", "", "後続"),
            KsBridgeTestHost.renderedRows(host),
        )
    }

    // MARK: - view インスタンスの安定性

    /**
     * 同一トークンで再発行しても、行に埋め込まれた View は同じインスタンスのまま維持される。
     *
     * 埋め込みの作り直し 0 回・剥がし 0 回を測る。再発行が内容のコンポジションまで届いたことは、
     * 同時に立てた Disclosure Indicator のぶん内容の幅が狭まることで裏付ける（幅が変わらなければ
     * 再コンポジション自体が走っておらず、「作り直されていない」という観測は空振りになる）。
     */
    @Test
    fun `同一トークンの再発行では view インスタンスが維持される`() {
        val probe = probe()
        probe.marker = "初期状態"
        val dto = customCell(probe, token = "token-1")
        val bridge = KsBridgeFixture.withCells(listOf(dto))
        val host = KsBridgeTestHost.attach(bridge)

        assertSame("前提: 輸送した View が行に埋め込まれていない", probe, embeddedProbe(host))
        val rowWidth = itemViewAt(host, CELL_ROW)!!.width
        assertEquals("前提: 内容が行の幅いっぱいに描画されていない", rowWidth, probe.width)
        val attachBefore = probe.attachCount
        val detachBefore = probe.detachCount
        val parentsBefore = probe.parents.toList()

        bridge.replaceCell(
            dto.cellID,
            customCell(probe, token = "token-1").apply { showArrowIndicator = true },
        )
        KsBridgeTestHost.pump(host)

        assertTrue(
            "前提: 内容の再コンポジションが走っておらず、再発行が届いたと言えない",
            probe.width < rowWidth,
        )
        assertSame(
            "同一トークンの再発行では同じインスタンスが表示され続ける",
            probe,
            embeddedProbe(host),
        )
        assertEquals("埋め込みが作り直されている", attachBefore, probe.attachCount)
        assertEquals("埋め込みが剥がされている", detachBefore, probe.detachCount)
        assertEquals("埋め込みの置き場所が入れ替わっている", parentsBefore, probe.parents)
        assertEquals("同一トークンの再発行で内容の状態は失われない", "初期状態", probe.marker)
    }

    /**
     * トークンが変われば行の内容が新しい View へ入れ替わり、旧 View は行から外れる。
     *
     * 新しい内容の埋め込みは 1 回だけ起き、旧 View は行の描画から外れる。Bridge は旧 View を
     * 破棄しないため、状態は保たれたまま再利用できる。
     */
    @Test
    fun `トークン変更で行の内容が新しい view へ置き換わる`() {
        val first = probe()
        first.marker = "旧内容"
        val second = probe(height = 80)
        val dto = customCell(first, token = "token-1")
        val bridge = KsBridgeFixture.withCells(listOf(dto))
        val host = KsBridgeTestHost.attach(bridge)

        assertSame("前提: 最初の View が行に埋め込まれていない", first, embeddedProbe(host))

        bridge.replaceCell(dto.cellID, customCell(second, token = "token-2"))
        KsBridgeTestHost.pump(host)

        assertSame("トークン変更で行の内容が新しい View になる", second, embeddedProbe(host))
        assertEquals("新しい View の埋め込みは 1 回だけ起きる", 1, second.attachCount)
        assertEquals("旧 View の埋め込みは 1 回だけ剥がされる", 1, first.detachCount)
        assertFalse("旧 View は行の描画から外れる", isDisplayedInRow(first, host))
        assertEquals("旧 View の破棄は Bridge の責務ではない", "旧内容", first.marker)
    }

    /**
     * 同じ View を掴んだままトークンだけを変えても、行の内容として取り付け直される。
     *
     * トークンが変われば埋め込みは作り直されるため、同じ View が一度剥がされて入り直す。
     */
    @Test
    fun `同一 view のままトークンだけ変えても表示が続く`() {
        val probe = probe()
        probe.marker = "内容"
        val dto = customCell(probe, token = "token-1")
        val bridge = KsBridgeFixture.withCells(listOf(dto))
        val host = KsBridgeTestHost.attach(bridge)
        assertEquals("前提: 最初の埋め込みが起きていない", 1, probe.attachCount)

        bridge.replaceCell(dto.cellID, customCell(probe, token = "token-2"))
        KsBridgeTestHost.pump(host)

        assertSame(probe, embeddedProbe(host))
        assertEquals("埋め込みが作り直される", 2, probe.attachCount)
        assertEquals("旧い埋め込みからは剥がされる", 1, probe.detachCount)
        assertEquals("取り付け直しで内容の状態は失われない", "内容", probe.marker)
    }

    /** 行高さは内容の計測結果に追従する。 */
    @Test
    fun `行高さが内容の計測結果に追従する`() {
        val probe = probe(height = 40)
        val bridge = KsBridgeFixture.withCells(listOf(customCell(probe, token = "token-1")))
        val host = KsBridgeTestHost.attach(bridge)

        assertEquals(40, probe.height)

        probe.contentHeight = 120
        probe.requestLayout()
        KsBridgeTestHost.pump(host)

        assertEquals("内容の計測結果に行の内容領域が追従する", 120, probe.height)
        assertEquals("行の高さが内容の計測結果に追従する", 120, itemViewAt(host, CELL_ROW)?.height)
    }

    // MARK: - リサイクル

    /** 画面外へ出て戻る（行の再利用が起きる）間、同一 View が例外なく再表示される。 */
    @Test
    fun `リサイクルを挟んだ再表示で内容が壊れない`() {
        val probe = probe()
        probe.marker = "内容"
        val bridge = KsSettingsBridge()
        val builder = KsBridgeRootBuilder()
        for (sectionIndex in 0 until 12) {
            val section = builder.addSection(headerText = "S$sectionIndex", footerText = null)
            for (cellIndex in 0 until 5) {
                if (sectionIndex == 0 && cellIndex == 0) {
                    builder.addCell(customCell(probe, token = "token-1"), section.sectionID)
                } else {
                    builder.addCell(
                        KsBridgeLabelCell(title = "C$sectionIndex-$cellIndex"),
                        section.sectionID,
                    )
                }
            }
        }
        bridge.setRoot(builder)

        val host = KsBridgeTestHost.attach(bridge)
        assertSame("前提: 先頭 Cell 行に View が埋め込まれていない", probe, embeddedProbe(host))

        val recyclerView = host.recyclerView
        recyclerView.scrollToPosition(recyclerView.adapter!!.itemCount - 1)
        KsBridgeTestHost.pump(host)
        assertNull("前提: 先頭 Cell 行が画面外へ出ていない", itemViewAt(host, CELL_ROW))
        for (position in 0 until recyclerView.adapter!!.itemCount) {
            val itemView = itemViewAt(host, position) ?: continue
            assertNull(
                "画面外へ出た内容が別の行に残っている (position=$position)",
                firstProbe(itemView),
            )
        }

        recyclerView.scrollToPosition(0)
        KsBridgeTestHost.pump(host)

        assertSame("再利用後の行に同一 View が再表示される", probe, embeddedProbe(host))
        assertEquals("リサイクルで内容の状態は失われない", "内容", probe.marker)
    }

    // MARK: - タップ通知

    /** 行タップが cellID 付きで通知される。 */
    @Test
    fun `行タップが customCellTapped で通知される`() {
        val dto = customCell(probe(), token = "token-1", hasTapHandler = true)
        val bridge = KsBridgeFixture.withCells(listOf(dto))
        val recorder = CustomCellTapRecorder()
        bridge.interactionListener = recorder
        val host = KsBridgeTestHost.attach(bridge)

        val before: CustomCell<*>? = KsBridgeFixture.storedCell(bridge)
        itemViewAt(host, CELL_ROW)!!.performClick()

        assertEquals(listOf(dto.cellID), recorder.tappedCellIDs)
        assertEquals(
            "タップに書き戻しは伴わない",
            before,
            KsBridgeFixture.storedCell<CustomCell<*>>(bridge),
        )
    }

    /** タップ購読なしの行はタップしても通知されない。 */
    @Test
    fun `タップ購読なしの行はタップしても通知されない`() {
        val dto = customCell(probe(), token = "token-1")
        val bridge = KsBridgeFixture.withCells(listOf(dto))
        val recorder = CustomCellTapRecorder()
        bridge.interactionListener = recorder
        val host = KsBridgeTestHost.attach(bridge)

        itemViewAt(host, CELL_ROW)!!.performClick()

        assertEquals(emptyList<String>(), recorder.tappedCellIDs)
    }

    /** 購読の有無は同一トークンのままの再発行で切り替わり、View は維持される。 */
    @Test
    fun `タップ購読の有無は再発行で切り替わる`() {
        val probe = probe()
        val dto = customCell(probe, token = "token-1")
        val bridge = KsBridgeFixture.withCells(listOf(dto))
        val recorder = CustomCellTapRecorder()
        bridge.interactionListener = recorder
        val host = KsBridgeTestHost.attach(bridge)
        val attachBefore = probe.attachCount

        bridge.replaceCell(
            dto.cellID,
            customCell(probe, token = "token-1", hasTapHandler = true),
        )
        KsBridgeTestHost.pump(host)
        itemViewAt(host, CELL_ROW)!!.performClick()

        assertEquals(listOf(dto.cellID), recorder.tappedCellIDs)
        assertSame("購読の切り替えで View は入れ替わらない", probe, embeddedProbe(host))
        assertEquals("購読の切り替えで埋め込みは作り直されない", attachBefore, probe.attachCount)
    }

    /** 内容の上をタッチしても行タップが通知される。 */
    @Test
    fun `内容の上のタッチで行タップが通知される`() {
        val dto = customCell(probe(), token = "token-1", hasTapHandler = true)
        val bridge = KsBridgeFixture.withCells(listOf(dto))
        val recorder = CustomCellTapRecorder()
        bridge.interactionListener = recorder
        val host = KsBridgeTestHost.attach(bridge)

        touchContent(host)

        assertEquals(
            "内容の上のタッチでも行タップが届く",
            listOf(dto.cellID),
            recorder.tappedCellIDs,
        )
    }

    /** 内容の中の操作がタッチを引き取った行では、行タップと二重に発火しない。 */
    @Test
    fun `内容がタッチを引き取ると行タップは通知されない`() {
        val probe = probe()
        var contentClicks = 0
        probe.setOnClickListener { contentClicks++ }
        val dto = customCell(probe, token = "token-1", hasTapHandler = true)
        val bridge = KsBridgeFixture.withCells(listOf(dto))
        val recorder = CustomCellTapRecorder()
        bridge.interactionListener = recorder
        val host = KsBridgeTestHost.attach(bridge)

        touchContent(host)

        assertEquals("内容の中の操作は行われる", 1, contentClicks)
        assertEquals(
            "内容が引き取ったタッチで行タップは発火しない",
            emptyList<String>(),
            recorder.tappedCellIDs,
        )
    }

    /** タップ購読なしの行でも、内容の中の操作は妨げられない。 */
    @Test
    fun `タップ購読なしの行でも内容の中の操作は妨げられない`() {
        val probe = probe()
        var contentClicks = 0
        probe.setOnClickListener { contentClicks++ }
        val bridge = KsBridgeFixture.withCells(listOf(customCell(probe, token = "token-1")))
        val recorder = CustomCellTapRecorder()
        bridge.interactionListener = recorder
        val host = KsBridgeTestHost.attach(bridge)

        touchContent(host)

        assertEquals("購読なしの行でも内容の中の操作は届く", 1, contentClicks)
        assertEquals(emptyList<String>(), recorder.tappedCellIDs)
    }

    /** 購読の切り替えは、内容の上のタッチによる行タップにも往復で反映される。 */
    @Test
    fun `購読の切り替えは内容の上のタッチに反映される`() {
        val probe = probe()
        val dto = customCell(probe, token = "token-1")
        val bridge = KsBridgeFixture.withCells(listOf(dto))
        val recorder = CustomCellTapRecorder()
        bridge.interactionListener = recorder
        val host = KsBridgeTestHost.attach(bridge)

        touchContent(host)
        assertEquals(
            "購読前は内容の上のタッチで通知されない",
            emptyList<String>(),
            recorder.tappedCellIDs,
        )

        bridge.replaceCell(dto.cellID, customCell(probe, token = "token-1", hasTapHandler = true))
        KsBridgeTestHost.pump(host)
        touchContent(host)
        assertEquals("購読後は内容の上のタッチで通知される", listOf(dto.cellID), recorder.tappedCellIDs)

        bridge.replaceCell(dto.cellID, customCell(probe, token = "token-1"))
        KsBridgeTestHost.pump(host)
        touchContent(host)
        assertEquals(
            "購読解除後は内容の上のタッチで通知されない",
            listOf(dto.cellID),
            recorder.tappedCellIDs,
        )
        assertSame("購読の切り替えで View は入れ替わらない", probe, embeddedProbe(host))
    }

    /** 無効な行では、内容の上をタッチしても行タップは通知されない。 */
    @Test
    fun `無効な行では内容の上のタッチで行タップが通知されない`() {
        val dto = customCell(probe(), token = "token-1", hasTapHandler = true).apply {
            isEnabled = false
        }
        val bridge = KsBridgeFixture.withCells(listOf(dto))
        val recorder = CustomCellTapRecorder()
        bridge.interactionListener = recorder
        val host = KsBridgeTestHost.attach(bridge)

        touchContent(host)

        assertEquals(
            "無効な行は内容の上のタッチでも行タップを発火しない",
            emptyList<String>(),
            recorder.tappedCellIDs,
        )
    }

    private companion object {
        /** `KsBridgeFixture.withCells` が組み立てる構成での先頭 Cell 行の位置（0 は Section header 行）。 */
        private const val CELL_ROW: Int = 1

        /** タッチを押してから離すまでの間隔（長押し判定に掛からない短さ）。 */
        private const val TOUCH_DURATION_MS: Long = 10
    }
}
