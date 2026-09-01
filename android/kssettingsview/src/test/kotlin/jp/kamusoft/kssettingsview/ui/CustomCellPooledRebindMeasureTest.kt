package jp.kamusoft.kssettingsview.ui

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import androidx.customview.poolingcontainer.isPoolingContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

/**
 * プールで非活性化された行を再 bind した直後に、再 composition を挟まず同期 measure する経路の回帰
 * テスト（android/ADR-0015）。
 *
 * # なぜこの経路が要るのか
 *
 * `RecyclerView` は `onBindViewHolder` と `measureChildWithMargins` を **同一のレイアウトパス** で
 * 続けて実行する。一方 [CustomCellViewHolder.bind] の書き込みは `MutableState` への代入であり、
 * composition へ届くのは次のフレーム（`Choreographer` の ANIMATION フェーズ）である。したがって
 * 「非活性のまま measure される」瞬間が必ず訪れる。Compose の `MeasurePassDelegate.remeasure` は
 * 非活性ノードに対して `IllegalArgumentException: measure is called on a deactivated node` を投げる
 * ため、この瞬間はそのままアプリの FATAL になる。
 *
 * # 再現の組み立て
 *
 * 実機と同じ条件を、`RecyclerView` のスケジューリングに依存せずに並べる。
 *
 * 1. 行を bind して測る（Composition と content ノードができる）
 * 2. [CustomCellViewHolder.reset] でプールへ入れ、window から外す。器は pooling container の内側に
 *    あるため Composition は生き残る（`DisposeOnDetachedFromWindowOrReleasedFromPool`）
 * 3. プール滞在中にフレームを 1 つ進める。ここで非活性化が composition に観測され、ノードが
 *    非活性になる
 * 4. 別の Cell として再 bind し、window へ戻して **フレームを進めずに** measure する
 *
 * 手順 4 の再 attach で `AndroidComposeView` は自分のルートノードの measurement を無効化するため、
 * 続く measure は必ずルートの再測定になり、非活性の子ノードへ到達する。
 *
 * # 確保される行の高さ
 *
 * 同じ手順で、非活性の間に確保される高さも押さえる。確保値が 0 や最低高へ退化すると、行が一度
 * 潰れて後続行がせり上がってから戻る。
 *
 * - 固定高さの行では、解決済みの行高さがそのまま確保値になる（新しい Cell の高さ）
 * - 可変高さの行では、解決値は最低高でしかない。直前に測った行高さを下限に使うため、content が
 *   最低高を超えていた行は縮まない
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CustomCellPooledRebindMeasureTest {

    private lateinit var activity: ComponentActivity

    /** `RecyclerView` に相当する器。プール相当の付け外しで Composition を破棄させないため印を付ける。 */
    private lateinit var container: FrameLayout

    private lateinit var frameDriver: ComposeFrameDriver

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        container = FrameLayout(activity)
        container.isPoolingContainer = true
        frameDriver = ComposeFrameDriver()
        // composition が作られる前に差し込む。
        frameDriver.installOn(container)
        activity.setContentView(container)
    }

    @After
    fun tearDown() {
        frameDriver.stop()
        // 器は pooling container なので、View を外しただけでは Composition が生き残る。破棄まで
        // 行わないと Recomposer と snapshot の監視がテストをまたいで積み上がる。
        for (i in 0 until container.childCount) {
            (container.getChildAt(i) as? ComposeView)?.disposeComposition()
        }
        container.removeAllViews()
    }

    @Test
    fun `プールで非活性化された行を再 bind した直後の measure が失敗しない`() {
        val holder = CustomCellViewHolder(activity)
        val composeView = holder.itemView as ComposeView

        holder.bind(cell("a"), Theme())
        container.addView(composeView)
        settle()
        measureRow(composeView)
        assertTrue("前提: A の content が現れていない", tagsOf(composeView).contains("probe-a"))

        // プールへ入れて window から外す。Composition は pooling container の内側なので生き残る。
        holder.reset()
        container.removeView(composeView)
        // プール滞在中にフレームが進み、非活性化が composition へ観測される。
        settle()
        assertTrue("前提: Composition が破棄されている", composeView.hasComposition)
        assertEquals("前提: content が非活性になっていない", emptyList<String>(), tagsOf(composeView))

        // プールから取り出して再 bind し、フレームを進めずに同じレイアウトパスで measure する。
        holder.bind(cell("b"), Theme())
        container.addView(composeView)
        measureRow(composeView)

        // 同期 measure が通ったうえで、次のフレームまでに新しい content が現れること。
        settle()
        measureRow(composeView)
        val tags = tagsOf(composeView)
        assertTrue("再 bind 後に新しい content が現れていない", tags.contains("probe-b"))
        assertTrue("前の content が残っている", tags.none { it == "probe-a" })
    }

    @Test
    fun `固定高さの行をプールから再 bind した直後は新しい Cell の高さが確保される`() {
        val holder = CustomCellViewHolder(activity)
        val composeView = holder.itemView as ComposeView

        holder.bind(cell("a", cellHeightDp = TALL_ROW_HEIGHT_DP), FIXED_HEIGHT_THEME)
        container.addView(composeView)
        settle()
        measureRow(composeView)
        assertEquals(
            "前提: A の行が固定高で測られていない",
            pxOf(TALL_ROW_HEIGHT_DP),
            composeView.measuredHeight,
        )

        holder.reset()
        container.removeView(composeView)
        settle()

        // 高さの低い Cell として再 bind し、フレームを進めずに測る。
        holder.bind(cell("b", cellHeightDp = SHORT_ROW_HEIGHT_DP), FIXED_HEIGHT_THEME)
        container.addView(composeView)
        measureRow(composeView)

        assertEquals("前提: この時点で content が活性に戻っている", emptyList<String>(), tagsOf(composeView))
        assertEquals(
            "確保された高さが新しい Cell の高さになっていない",
            pxOf(SHORT_ROW_HEIGHT_DP),
            reservedRowHeightPx(composeView),
        )
    }

    @Test
    fun `可変高さの行をプールから再 bind した直後は確保される高さが最低高まで縮まない`() {
        val holder = CustomCellViewHolder(activity)
        val composeView = holder.itemView as ComposeView

        // 最低高を超える content を持つ行。行の高さは content の自然高で決まる。
        holder.bind(cell("a", contentHeightDp = TALL_CONTENT_HEIGHT_DP), Theme())
        container.addView(composeView)
        settle()
        measureRow(composeView)
        assertEquals(
            "前提: A の行が content の自然高で測られていない",
            pxOf(TALL_CONTENT_HEIGHT_DP),
            composeView.measuredHeight,
        )

        holder.reset()
        // プールへ入る瞬間の測定。reset は空 content 化と非活性化を同一スナップショットに書くため
        // 両者は composition へ揃って届き、反映前のこの測定が観測するのは A の旧 content の高さ
        // (空の行にはならない)。この測定を挟んでも B の確保高さが崩れないことを後段で確認する。
        remeasureRow(composeView)
        container.removeView(composeView)
        settle()

        holder.bind(cell("b"), Theme())
        container.addView(composeView)
        measureRow(composeView)

        assertEquals("前提: この時点で content が活性に戻っている", emptyList<String>(), tagsOf(composeView))
        assertEquals(
            "確保された高さが最低高まで縮んでいる",
            pxOf(TALL_CONTENT_HEIGHT_DP),
            reservedRowHeightPx(composeView),
        )
    }

    private fun cell(
        id: String,
        cellHeightDp: Int? = null,
        contentHeightDp: Int = ROW_HEIGHT_DP,
    ): CustomCell<String> =
        CustomCell(
            id = id,
            style = if (cellHeightDp == null) CellStyle() else CellStyle(cellHeight = cellHeightDp.dp),
            content = id,
        ) { value ->
            Box(Modifier.testTag("probe-$value").fillMaxWidth().height(contentHeightDp.dp))
        }

    /**
     * View 階層の測定キャッシュを外し、Compose の measure を実際に通して測り直す。
     *
     * [measureRow] をそのまま呼んでも、measure spec が前回と同じなら View 階層の測定はキャッシュ
     * されたまま返る。プール滞在中（window の外）に出た再測定の要求は Compose 側のノードを汚すだけで
     * hosting View の `requestLayout` を伴わないため、キャッシュは自動では外れない。実機では毎フレーム
     * のレイアウト・描画パスが Compose の measure を必ず走らせるので、ここでは手で外す。
     */
    private fun remeasureRow(view: ViewGroup) {
        view.forceLayout()
        view.getChildAt(0).forceLayout()
        measureRow(view)
    }

    /** content が非活性の間に確保されている行の高さ（px）。 */
    private fun reservedRowHeightPx(view: ViewGroup): Int {
        remeasureRow(view)
        return view.measuredHeight
    }

    /** dp を、Compose の `Dp.roundToPx` と同じ丸めで px へ変換する。 */
    private fun pxOf(dp: Int): Int = (dp * activity.resources.displayMetrics.density).roundToInt()

    private fun measureRow(view: View) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(ROW_WIDTH_PX, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        view.layout(0, 0, ROW_WIDTH_PX, view.measuredHeight)
    }

    private fun settle() {
        idle()
        frameDriver.frame()
        idle()
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

    private fun semanticsRoot(view: View): SemanticsNode? {
        if (view is ViewRootForTest) return view.semanticsOwner.rootSemanticsNode
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                semanticsRoot(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private companion object {
        /** probe の高さ（dp）。 */
        const val ROW_HEIGHT_DP: Int = 48

        /** 行を測る幅（px）。行をまたいで変えない（実機と同じく幅は一定）。 */
        const val ROW_WIDTH_PX: Int = 1080

        /** 高さ確保の検証で A の行に与える固定高（dp）。 */
        const val TALL_ROW_HEIGHT_DP: Int = 120

        /** 高さ確保の検証で B の行に与える固定高（dp）。A と別の値にする。 */
        const val SHORT_ROW_HEIGHT_DP: Int = 72

        /** 可変高さの検証で A の content に与える高さ（dp）。最低高（60dp）を超える値にする。 */
        const val TALL_CONTENT_HEIGHT_DP: Int = 200

        /** 全 Cell を一律固定高さにする Theme。行高さは Cell 個別の `cellHeight` で決まる。 */
        val FIXED_HEIGHT_THEME: Theme = Theme(hasUnevenRows = false)
    }
}
