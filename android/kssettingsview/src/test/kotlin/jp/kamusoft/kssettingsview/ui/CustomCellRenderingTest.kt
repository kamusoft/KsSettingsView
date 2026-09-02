package jp.kamusoft.kssettingsview.ui

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [CustomCellViewHolder] の描画・再利用・タップ・スタイル適用を検証する。
 *
 * 任意 content の観測は、builder が `testTag` 付きの probe を出力し、composition の semantics
 * ツリーからその tag を探す方式で行う。semantics ツリーは実際に走った composition の結果であり、
 * builder が呼ばれたことだけでなく「行のどこにどれだけの幅で置かれたか」まで読める。
 *
 * タップ系は実 [MotionEvent] を `dispatchTouchEvent` に流し、Compose のヒットテストと
 * View のクリック配送を通した結果を見る。
 */
@OptIn(ExperimentalFoundationApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CustomCellRenderingTest {

    private lateinit var activity: ComponentActivity

    /** 行を載せる器。`Activity.setContentView` は LayoutParams を上書きするため 1 枚挟む。 */
    private lateinit var container: android.widget.FrameLayout

    /** 再 composition を決定的に流す駆動器。 */
    private lateinit var frameDriver: ComposeFrameDriver

    private val density: Float
        get() = activity.resources.displayMetrics.density

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        container = android.widget.FrameLayout(activity)
        frameDriver = ComposeFrameDriver()
        // composition が作られる前に差し込む。
        frameDriver.installOn(container)
        activity.setContentView(container)
    }

    @After
    fun tearDown() {
        frameDriver.stop()
    }

    /** ViewHolder を生成し、bind 後に Activity へ載せて composition を走らせる。 */
    private fun bindAndAttach(
        host: ComponentActivity,
        cell: CustomCell<*>,
        theme: Theme = Theme(),
        holder: CustomCellViewHolder = CustomCellViewHolder(host),
    ): CustomCellViewHolder {
        holder.bind(cell, theme)
        if (holder.itemView.parent == null) {
            container.addView(holder.itemView)
        }
        idle()
        frameDriver.frame()
        return holder
    }

    /** 実測レイアウトを走らせる（Compose のヒットテストと semantics の座標に必要）。 */
    private fun layoutRow(view: View, widthPx: Int = ROW_WIDTH_PX) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        view.layout(0, 0, widthPx, view.measuredHeight)
        idle()
        frameDriver.frame()
    }

    // MARK: - semantics ツリーの探索ヘルパ

    private fun semanticsRoot(view: View): SemanticsNode? {
        if (view is ViewRootForTest) return view.semanticsOwner.rootSemanticsNode
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                semanticsRoot(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun allNodes(view: View): List<SemanticsNode> {
        val root = semanticsRoot(view) ?: return emptyList()
        val out = mutableListOf<SemanticsNode>()
        fun walk(node: SemanticsNode) {
            out.add(node)
            node.children.forEach(::walk)
        }
        walk(root)
        return out
    }

    private fun testTags(view: View): List<String> =
        allNodes(view).mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }

    private fun contentDescriptions(view: View): List<String> =
        allNodes(view).flatMap { it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }

    private fun nodeWithTag(view: View, tag: String): SemanticsNode? =
        allNodes(view).firstOrNull { it.config.getOrNull(SemanticsProperties.TestTag) == tag }

    /** content の値をそのまま tag に載せる probe。 */
    private fun probeBuilder(heightDp: Int = 60): @Composable (String) -> Unit = { value ->
        Box(
            Modifier
                .testTag("probe-$value")
                .fillMaxWidth()
                .height(heightDp.dp),
        )
    }

    // MARK: - content 駆動の描画

    @Test
    fun `bind すると builder の出力が composition に現れる`() {
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A", builder = probeBuilder()),
        )
        assertTrue(testTags(holder.itemView).contains("probe-A"))
    }

    @Test
    fun `content を更新すると builder の出力が入れ替わる`() {
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A", builder = probeBuilder()),
        )
        assertTrue(testTags(holder.itemView).contains("probe-A"))

        bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "B", builder = probeBuilder()),
            holder = holder,
        )
        val tags = testTags(holder.itemView)
        assertTrue("新しい content の出力が現れる", tags.contains("probe-B"))
        assertFalse("前の content の出力は残らない", tags.contains("probe-A"))
    }

    @Test
    fun `reset で前の content とタップ listener が残らない`() {
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A", onTap = { }, builder = probeBuilder()),
        )
        assertTrue(testTags(holder.itemView).contains("probe-A"))
        assertTrue(holder.itemView.hasOnClickListeners())

        holder.reset()
        idle()
        frameDriver.frame()
        assertFalse("前の content 表示は残らない", testTags(holder.itemView).contains("probe-A"))
        assertFalse("前のタップ listener は残らない", holder.itemView.hasOnClickListeners())
        assertFalse(holder.itemView.isClickable)
    }

    @Test
    fun `reset 後に別 Cell を bind すると新しい content だけが描画される`() {
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A", onTap = { }, builder = probeBuilder()),
        )
        holder.reset()
        bindAndAttach(
            activity,
            CustomCell(id = "c2", content = "B", builder = probeBuilder()),
            holder = holder,
        )
        val tags = testTags(holder.itemView)
        assertEquals(listOf("probe-B"), tags)
        assertFalse("前のタップ listener は残らない", holder.itemView.hasOnClickListeners())
    }

    @Test
    fun `content なしの省略形も builder の出力を描画する`() {
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1") { Box(Modifier.testTag("static").fillMaxWidth().height(40.dp)) },
        )
        assertTrue(testTags(holder.itemView).contains("static"))
    }

    // MARK: - Disclosure Indicator

    @Test
    fun `showArrow で Disclosure Indicator が合成される`() {
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A", showArrow = true, builder = probeBuilder()),
        )
        assertTrue(contentDescriptions(holder.itemView).contains("Disclosure indicator"))
    }

    @Test
    fun `既定では Disclosure Indicator は表示されず content が行全域を占有する`() {
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A", builder = probeBuilder()),
        )
        layoutRow(holder.itemView)
        assertFalse(contentDescriptions(holder.itemView).contains("Disclosure indicator"))
        val probe = nodeWithTag(holder.itemView, "probe-A")
        assertNotNull(probe)
        assertEquals(ROW_WIDTH_PX, probe!!.size.width)
    }

    @Test
    fun `showArrow のとき content の占有幅は indicator の領域を除いた範囲になる`() {
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A", showArrow = true, builder = probeBuilder()),
        )
        layoutRow(holder.itemView)
        val probe = nodeWithTag(holder.itemView, "probe-A")
        assertNotNull(probe)
        val indicatorArea =
            ((CELL_DISCLOSURE_WIDTH_DP + CELL_ROW_HORIZONTAL_MARGIN_DP) * density).toInt()
        assertEquals(ROW_WIDTH_PX - indicatorArea, probe!!.size.width)
    }

    // MARK: - 行タップ

    @Test
    fun `onTap 指定時に行タップで発火する`() {
        var taps = 0
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A", onTap = { taps++ }, builder = probeBuilder()),
        )
        layoutRow(holder.itemView)
        tap(holder.itemView, 100f, 30f)
        assertEquals(1, taps)
    }

    @Test
    fun `子要素の操作では行タップが発火しない`() {
        var rowTaps = 0
        var childTaps = 0
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A", onTap = { rowTaps++ }) {
                Box(
                    Modifier
                        .testTag("child")
                        .fillMaxWidth()
                        .height(60.dp)
                        .clickable { childTaps++ },
                )
            },
        )
        layoutRow(holder.itemView)
        tap(holder.itemView, 100f, 30f)
        assertEquals("子のアクションだけが実行される", 1, childTaps)
        assertEquals("行の onTap は呼ばれない", 0, rowTaps)
    }

    @Test
    fun `既定では行タップ動作を持たず content 内のコントロールが機能する`() {
        var childTaps = 0
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A") {
                Box(
                    Modifier
                        .testTag("child")
                        .fillMaxWidth()
                        .height(60.dp)
                        .clickable { childTaps++ },
                )
            },
        )
        layoutRow(holder.itemView)
        assertFalse("行レベルのタップ処理を持たない", holder.itemView.hasOnClickListeners())
        tap(holder.itemView, 100f, 30f)
        assertEquals(1, childTaps)
    }

    @Test
    fun `onTap がなくても有効な行は押下 feedback のために clickable を持つ`() {
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A", builder = probeBuilder()),
        )
        assertTrue("callback がなくても ripple 表示のため clickable", holder.itemView.isClickable)
        assertTrue(holder.itemView.isEnabled)
        assertFalse("clickable であっても callback は持たない", holder.itemView.hasOnClickListeners())
    }

    @Test
    fun `無効時は行タップが発火しない`() {
        var taps = 0
        val holder = bindAndAttach(
            activity,
            CustomCell(
                id = "c1",
                content = "A",
                onTap = { taps++ },
                isEnabled = false,
                builder = probeBuilder(),
            ),
        )
        layoutRow(holder.itemView)
        tap(holder.itemView, 100f, 30f)
        assertEquals(0, taps)
        assertFalse(holder.itemView.hasOnClickListeners())
        assertFalse("無効な行は押下 feedback も持たない", holder.itemView.isEnabled)
    }

    @Test
    fun `無効時は content 内の操作も抑止される`() {
        var rowTaps = 0
        var childTaps = 0
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A", onTap = { rowTaps++ }, isEnabled = false) {
                Box(
                    Modifier
                        .testTag("child")
                        .fillMaxWidth()
                        .height(60.dp)
                        .clickable { childTaps++ },
                )
            },
        )
        layoutRow(holder.itemView)
        tap(holder.itemView, 100f, 30f)
        assertEquals("content 内のアクションは実行されない", 0, childTaps)
        assertEquals(0, rowTaps)
    }

    @Test
    fun `無効時は accessibility action 経由でも content 内の操作が発火しない`() {
        var childTaps = 0
        var longClicks = 0
        var sliderValue = 0f
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A", isEnabled = false) {
                Column {
                    Box(
                        Modifier
                            .testTag("child")
                            .fillMaxWidth()
                            .height(40.dp)
                            .combinedClickable(
                                onClick = { childTaps++ },
                                onLongClick = { longClicks++ },
                            ),
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 0f..100f,
                    )
                }
            },
        )
        layoutRow(holder.itemView)

        // TalkBack 等の accessibility service はポインタを配送せず semantics action を直接実行する。
        // 行内の全ノードについて操作系 action を総当たりで実行し、どれも content へ届かないことを見る。
        assertEquals(
            "無効な行は操作系 action を 1 つも公開しない",
            0,
            performAllActions(holder.itemView),
        )
        assertFalse(
            "content 内のノードは accessibility ツリーに露出しない",
            testTags(holder.itemView).contains("child"),
        )

        assertEquals("click は content へ届かない", 0, childTaps)
        assertEquals("long click は content へ届かない", 0, longClicks)
        assertEquals("Slider の値は変わらない", 0f, sliderValue, 0f)
    }

    @Test
    fun `有効なら accessibility action で content 内の操作が発火する`() {
        var childTaps = 0
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A", isEnabled = true) {
                Box(
                    Modifier
                        .testTag("child")
                        .fillMaxWidth()
                        .height(60.dp)
                        .clickable { childTaps++ },
                )
            },
        )
        layoutRow(holder.itemView)
        performAllActions(holder.itemView)
        assertEquals("無効化していない行では action がそのまま機能する", 1, childTaps)
    }

    // MARK: - スタイルの適用範囲

    @Test
    fun `hasUnevenRows が true なら cellHeight は最低高として働く`() {
        val theme = Theme(hasUnevenRows = true)
        val holder = bindAndAttach(
            activity,
            CustomCell(
                id = "c1",
                style = CellStyle(cellHeight = 100.dp),
                content = "A",
                builder = probeBuilder(heightDp = 40),
            ),
            theme = theme,
        )
        val expectedMin = (100 * density).toInt()
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, holder.itemView.layoutParams.height)
        assertEquals(expectedMin, holder.itemView.minimumHeight)
        layoutRow(holder.itemView)
        assertEquals("content が低いときは最低高が効く", expectedMin, holder.itemView.measuredHeight)

        // content の自然高が指定値を超えると、その分だけ行が伸びる
        bindAndAttach(
            activity,
            CustomCell(
                id = "c1",
                style = CellStyle(cellHeight = 100.dp),
                content = "B",
                builder = probeBuilder(heightDp = 200),
            ),
            theme = theme,
            holder = holder,
        )
        layoutRow(holder.itemView)
        assertEquals((200 * density).toInt(), holder.itemView.measuredHeight)
    }

    @Test
    fun `hasUnevenRows が false なら cellHeight で固定できる`() {
        val holder = bindAndAttach(
            activity,
            CustomCell(
                id = "c1",
                style = CellStyle(cellHeight = 100.dp),
                content = "A",
                builder = probeBuilder(heightDp = 200),
            ),
            theme = Theme(hasUnevenRows = false),
        )
        val expected = (100 * density).toInt()
        assertEquals(expected, holder.itemView.layoutParams.height)
        assertEquals(0, holder.itemView.minimumHeight)
        layoutRow(holder.itemView)
        assertEquals(
            "content の自然高が指定値を超えても固定される",
            expected,
            holder.itemView.measuredHeight,
        )
    }

    @Test
    fun `content は行に収まるとき縦中央 収まらないとき上端揃えになる`() {
        // 収まるとき: 100dp の行に 40dp の content → 上下 30dp ずつの余白（縦中央）
        val fits = bindAndAttach(
            activity,
            CustomCell(
                id = "c1",
                style = CellStyle(cellHeight = 100.dp),
                content = "A",
                builder = probeBuilder(heightDp = 40),
            ),
            theme = Theme(hasUnevenRows = false),
        )
        layoutRow(fits.itemView)
        assertEquals(
            "収まるときは縦中央",
            (30 * density).toInt(),
            nodeWithTag(fits.itemView, "probe-A")!!.positionInRoot.y.toInt(),
        )

        // 収まらないとき: 60dp の行に、制約を振り切って 200dp を要求する content
        val overflows = bindAndAttach(
            activity,
            CustomCell(id = "c2", style = CellStyle(cellHeight = 60.dp), content = "B") {
                Box(
                    Modifier
                        .testTag("probe-B")
                        .fillMaxWidth()
                        .requiredHeight(200.dp),
                )
            },
            theme = Theme(hasUnevenRows = false),
        )
        layoutRow(overflows.itemView)
        assertEquals(
            "収まらないときは上端揃え（縦中央だと上へはみ出して負のオフセットになる）",
            0,
            nodeWithTag(overflows.itemView, "probe-B")!!.positionInRoot.y.toInt(),
        )
    }

    @Test
    fun `content のサイズ変化に行高さが追従する`() {
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A", builder = probeBuilder(heightDp = 80)),
        )
        layoutRow(holder.itemView)
        assertEquals((80 * density).toInt(), holder.itemView.measuredHeight)

        bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "B", builder = probeBuilder(heightDp = 240)),
            holder = holder,
        )
        layoutRow(holder.itemView)
        assertEquals((240 * density).toInt(), holder.itemView.measuredHeight)
    }

    @Test
    fun `builder 内部の状態変化だけでも行高さが追従する`() {
        // content 差し替え（再バインド）を伴わず、composition 内部の state だけで高さが変わる形。
        var toggle: (() -> Unit)? = null
        val holder = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A") {
                var isExpanded by remember { mutableStateOf(false) }
                toggle = { isExpanded = !isExpanded }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(if (isExpanded) 240.dp else 80.dp),
                )
            },
        )
        layoutRow(holder.itemView)
        assertEquals((80 * density).toInt(), holder.itemView.measuredHeight)

        toggle!!.invoke()
        idle()
        frameDriver.frame()

        // 明示的な再計測を挟まずに測定結果が変わることが、composition 内部の変化が hosting View の
        // 再レイアウトへ伝播していることの証拠になる。
        assertEquals(
            "再バインドなしでも行高さが追従する",
            (240 * density).toInt(),
            holder.itemView.measuredHeight,
        )
    }

    @Test
    fun `背景色は行レベルの style として適用される`() {
        val holder = bindAndAttach(
            activity,
            CustomCell(
                id = "c1",
                style = CellStyle(backgroundColor = Color(0xFF123456)),
                content = "A",
                builder = probeBuilder(),
            ),
        )
        assertEquals(Color(0xFF123456).toArgb(), backgroundColorOf(holder.itemView))
    }

    @Test
    fun `テキスト系スタイルは content の描画に影響しない`() {
        val plain = bindAndAttach(
            activity,
            CustomCell(id = "c1", content = "A", builder = probeBuilder()),
        )
        layoutRow(plain.itemView)
        val plainWidth = nodeWithTag(plain.itemView, "probe-A")!!.size.width
        val plainTags = testTags(plain.itemView)

        val styled = bindAndAttach(
            activity,
            CustomCell(
                id = "c1",
                style = CellStyle(
                    titleColor = Color.Red,
                    titleFont = androidx.compose.ui.text.TextStyle(fontSize = 40.sp),
                    valueTextColor = Color.Blue,
                    hintTextColor = Color.Green,
                ),
                content = "A",
                builder = probeBuilder(),
            ),
        )
        layoutRow(styled.itemView)

        assertEquals("composition の構成は変わらない", plainTags, testTags(styled.itemView))
        assertEquals("content の占有幅も変わらない", plainWidth, nodeWithTag(styled.itemView, "probe-A")!!.size.width)
    }

    // MARK: - 登録

    @Test
    fun `KsSettingsView 初期化で CustomCell が自動登録される`() {
        KsCellRegistry.clear()
        try {
            assertFalse(KsCellRegistry.isRegistered(CustomCell::class))
            val view = KsSettingsView(activity)
            assertTrue(KsCellRegistry.isRegistered(CustomCell::class))

            // strictMode（既定 true）でも例外にならず viewType が解決できる
            assertTrue(KsCellRegistry.strictMode)
            val cell = CustomCell(id = "c1", content = "A", builder = probeBuilder())
            assertEquals(VIEW_TYPE_CUSTOM_CELL, KsCellRegistry.viewTypeOf(cell))

            val vh = KsCellRegistry.createViewHolder(view, VIEW_TYPE_CUSTOM_CELL)
            assertTrue(vh is CustomCellViewHolder)
        } finally {
            KsCellRegistry.clear()
        }
    }

    @Test
    fun `Registry 未操作のまま CustomCell を含む root を表示できる`() {
        KsCellRegistry.clear()
        try {
            val view = KsSettingsView(activity)
            view.setRootDirect(
                jp.kamusoft.kssettingsview.core.SettingsRoot(
                    sections = listOf(
                        jp.kamusoft.kssettingsview.core.Section(
                            id = "s1",
                            cells = listOf(
                                CustomCell(id = "c1", content = "A", builder = probeBuilder()),
                                LabelCell(id = "c2", title = "後続"),
                            ),
                        ),
                    ),
                ),
            )
            idle()
            assertEquals(2, view.internalRecyclerView().adapter!!.itemCount)
        } finally {
            KsCellRegistry.clear()
        }
    }

    // MARK: - タッチ / accessibility 配送ヘルパ

    /**
     * semantics ツリーの全ノードに対し、操作系 action を総当たりで実行する。
     *
     * accessibility service（TalkBack / Switch Access）はポインタを配送せず semantics action を
     * 直接呼ぶため、その経路を模す。戻り値は実行できた action の数。
     */
    private fun performAllActions(view: View): Int {
        var performed = 0
        allNodes(view).forEach { node ->
            node.config.getOrNull(SemanticsActions.OnClick)?.action?.let {
                it()
                performed++
            }
            node.config.getOrNull(SemanticsActions.OnLongClick)?.action?.let {
                it()
                performed++
            }
            node.config.getOrNull(SemanticsActions.SetProgress)?.action?.let {
                it(50f)
                performed++
            }
        }
        return performed
    }

    private fun tap(view: View, x: Float, y: Float) {
        val t = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(down)
        down.recycle()
        idle()
        val up = MotionEvent.obtain(t, t + 10, MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(up)
        up.recycle()
        idle()
    }

    /** Cell 背景（Ripple でラップされる）から実際の塗り色を取り出す。 */
    private fun backgroundColorOf(view: View): Int {
        fun dig(drawable: Drawable?): Int? = when (drawable) {
            is ColorDrawable -> drawable.color
            is LayerDrawable -> (0 until drawable.numberOfLayers)
                .firstNotNullOfOrNull { dig(drawable.getDrawable(it)) }
            else -> null
        }
        return dig(view.background) ?: error("背景から色を取得できない")
    }

    private companion object {
        const val ROW_WIDTH_PX: Int = 1080
    }
}
