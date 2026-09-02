package jp.kamusoft.kssettingsview.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.SectionAccessory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `ClassicSectionDecoration` の罫線描画ロジック（罫線インセット規則）を検証する。
 *
 * 罫線の左インセットはセクション内の位置で決まる。
 *
 * - セクション最初 Cell の上端罫線 → 左インセット 0（端から端）
 * - セクション最後 Cell の下端罫線 → 左インセット 0（端から端）
 * - セクション内中間 Cell の下端罫線 → 左インセット 16dp 相当
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ClassicSectionDecorationTest {

    private val ctx: android.content.Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            MaterialR.style.Theme_Material3_Light_NoActionBar,
        )

    // MARK: - bottomSeparatorLeftFor（純粋関数）の規則検証

    @Test
    fun `bottomSeparatorLeftFor はセクション最後 Cell でインセット 0 を返す`() {
        val edgeLeft = 0f
        val midInset = 48f // 仮想 3x density × 16dp
        val left = ClassicSectionDecoration.bottomSeparatorLeftFor(
            isSectionBottom = true,
            edgeLeft = edgeLeft,
            midSeparatorInsetPx = midInset,
        )
        // セクション最後 Cell の下端罫線 → 端から端（インセット 0）。
        assertEquals(edgeLeft, left, 0f)
    }

    @Test
    fun `bottomSeparatorLeftFor はセクション内中間 Cell で 16dp 相当のインセットを返す`() {
        val edgeLeft = 0f
        val midInset = 48f // 例: density = 3.0 のとき 16dp = 48px 相当
        val left = ClassicSectionDecoration.bottomSeparatorLeftFor(
            isSectionBottom = false,
            edgeLeft = edgeLeft,
            midSeparatorInsetPx = midInset,
        )
        // セクション内中間 Cell の下端罫線 → 左に midSeparatorInsetPx 分インセット。
        assertEquals(edgeLeft + midInset, left, 0f)
    }

    @Test
    fun `bottomSeparatorLeftFor は paddingLeft を考慮した edgeLeft を起点にする`() {
        val edgeLeft = 10f // 仮想 padding
        val midInset = 48f
        val bottom = ClassicSectionDecoration.bottomSeparatorLeftFor(
            isSectionBottom = true,
            edgeLeft = edgeLeft,
            midSeparatorInsetPx = midInset,
        )
        val mid = ClassicSectionDecoration.bottomSeparatorLeftFor(
            isSectionBottom = false,
            edgeLeft = edgeLeft,
            midSeparatorInsetPx = midInset,
        )
        // セクション境界は edgeLeft そのまま、中間は edgeLeft + midInset。
        assertEquals(edgeLeft, bottom, 0f)
        assertEquals(edgeLeft + midInset, mid, 0f)
    }

    // MARK: - 実 RecyclerView + onDrawOver 経由の統合的検証

    /**
     * 1 セクション内に 3 Cell（最初・中間・最後）を配置し、`onDrawOver` で記録された
     * `drawRect` 呼び出しから「左座標がインセット規則に従っている」ことを検証する。
     *
     * - 1 つ目の Cell（セクション最初）→ 上端罫線（インセット 0）と下端罫線（中間扱い、16dp インセット）
     * - 2 つ目の Cell（セクション中間）→ 下端罫線（中間扱い、16dp インセット）
     * - 3 つ目の Cell（セクション最後）→ 下端罫線（境界扱い、インセット 0）
     */
    @Test
    fun `onDrawOver は中間 Cell の下端を 16dp インセット セクション境界を 0 インセットで描画する`() {
        val (recyclerView, decoration, density) = prepareSingleSectionRecycler(cellCount = 3)

        val recorder = RecordingCanvas()
        decoration.onDrawOver(recorder, recyclerView, RecyclerView.State())

        // 「描画矩形の高さが概ね 1px の rect」 = 罫線描画呼び出しに絞り込む。
        val separatorRects = recorder.rects.filter { (it.bottom - it.top) > 0f && (it.bottom - it.top) <= 1.5f }
        assertTrue(
            "罫線描画 drawRect が 1 件以上記録される (記録総数=${recorder.rects.size})",
            separatorRects.isNotEmpty(),
        )

        val midInsetPx = 16f * density

        // 各矩形の left 座標は「edgeLeft（= paddingLeft）」または「edgeLeft + 16dp 相当」の
        // どちらかでなければならない。
        val edgeLeft = recyclerView.paddingLeft.toFloat()
        for (r in separatorRects) {
            val isEdge = kotlin.math.abs(r.left - edgeLeft) < 0.5f
            val isMid = kotlin.math.abs(r.left - (edgeLeft + midInsetPx)) < 0.5f
            assertTrue(
                "罫線の left=${r.left} は edge(${edgeLeft}) または mid(${edgeLeft + midInsetPx}) のいずれか",
                isEdge || isMid,
            )
        }

        // 少なくとも 1 件は「edge（インセット 0）」、少なくとも 1 件は「mid（16dp インセット）」が
        // 出ているはず（最初 Cell の上端 = edge、最後 Cell の下端 = edge、中間 Cell の下端 = mid）。
        val hasEdge = separatorRects.any { kotlin.math.abs(it.left - edgeLeft) < 0.5f }
        val hasMid = separatorRects.any { kotlin.math.abs(it.left - (edgeLeft + midInsetPx)) < 0.5f }
        assertTrue("セクション境界の 0 インセット罫線が描画されている", hasEdge)
        assertTrue("セクション内中間 Cell の 16dp インセット罫線が描画されている", hasMid)
    }

    @Test
    fun `onDrawOver は単一 Cell セクションで上下とも 0 インセットで描画する`() {
        val (recyclerView, decoration, _) = prepareSingleSectionRecycler(cellCount = 1)

        val recorder = RecordingCanvas()
        decoration.onDrawOver(recorder, recyclerView, RecyclerView.State())

        val separatorRects = recorder.rects.filter { (it.bottom - it.top) > 0f && (it.bottom - it.top) <= 1.5f }
        val edgeLeft = recyclerView.paddingLeft.toFloat()

        // 1 Cell のみのセクションでは「最初 Cell かつ最後 Cell」となるため、上端も下端も
        // 端から端（インセット 0）で描画される。
        for (r in separatorRects) {
            assertEquals(
                "単一 Cell セクションは上下とも端から端（インセット 0）",
                edgeLeft,
                r.left,
                0.5f,
            )
        }
        // 上端罫線（1 本）+ 下端罫線（1 本）= 2 本は出ているはず。
        assertTrue("単一 Cell セクションでも 2 本以上の罫線が描画される", separatorRects.size >= 2)
    }

    // MARK: - sectionMargin の上下適用

    @Test
    fun `sectionMargin 未指定なら Classic 既定の上余白が Section の先頭に入る`() {
        val density = ctx.resources.displayMetrics.density
        val (recyclerView, decoration, _) = prepareTwoSectionRecycler(theme = Theme())
        val expectedTopPx =
            (SectionBoxMetrics.CLASSIC_DEFAULT_MARGIN.calculateTopPadding().value * density).toInt()

        val offsets = (0 until recyclerView.childCount).map { index ->
            val outRect = Rect()
            decoration.getItemOffsets(
                outRect,
                recyclerView.getChildAt(index),
                recyclerView,
                RecyclerView.State(),
            )
            outRect
        }

        // Classic 既定 margin は Modern と同値（上のみ / 下 0）。上余白は Section の先頭行にだけ入り、
        // 水平成分は Classic では無視される（Section 境界を全幅に保つ）。
        assertTrue(
            "Section の先頭行に Classic 既定の上余白が入っていない",
            offsets.any { it.top == expectedTopPx },
        )
        for (rect in offsets) {
            assertEquals("Classic の下余白の既定は 0", 0, rect.bottom)
            assertEquals("Classic では水平成分が無視されていない（左）", 0, rect.left)
            assertEquals("Classic では水平成分が無視されていない（右）", 0, rect.right)
        }
    }

    @Test
    fun `sectionMargin は上下成分だけが Section 単位の offset になる`() {
        val density = ctx.resources.displayMetrics.density
        val theme = Theme(
            sectionMargin = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 30.dp),
        )
        val (recyclerView, decoration, _) = prepareTwoSectionRecycler(theme = theme)
        val topPx = (20.0f * density).toInt()
        val bottomPx = (30.0f * density).toInt()

        val offsets = (0 until recyclerView.childCount).map { index ->
            val outRect = Rect()
            decoration.getItemOffsets(
                outRect,
                recyclerView.getChildAt(index),
                recyclerView,
                RecyclerView.State(),
            )
            outRect
        }

        // 2 セクション × 2 Cell = 4 行。上から s1-c0 / s1-c1 / s2-c0 / s2-c1。
        assertEquals("4 行が並ぶ", 4, offsets.size)
        assertEquals("list 先頭に上余白", topPx, offsets[0].top)
        assertEquals("Section 内の 2 行目に上余白は入らない", 0, offsets[1].top)
        assertEquals("Section 末尾に下余白", bottomPx, offsets[1].bottom)
        assertEquals("次 Section の先頭に上余白", topPx, offsets[2].top)
        assertEquals("list 末尾に下余白", bottomPx, offsets[3].bottom)

        for (outRect in offsets) {
            assertEquals("leading 成分は無視する", 0, outRect.left)
            assertEquals("trailing 成分は無視する", 0, outRect.right)
        }
    }

    @Test
    fun `sectionMargin を入れても罫線の水平方向は全幅のまま`() {
        val theme = Theme(
            sectionMargin = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 30.dp),
        )
        val (recyclerView, decoration, density) = prepareTwoSectionRecycler(theme = theme)

        val recorder = RecordingCanvas()
        decoration.onDrawOver(recorder, recyclerView, RecyclerView.State())

        val separatorRects = recorder.rects.filter { (it.bottom - it.top) > 0f && (it.bottom - it.top) <= 1.5f }
        assertTrue("罫線が描画される", separatorRects.isNotEmpty())
        val edgeLeft = recyclerView.paddingLeft.toFloat()
        val right = (recyclerView.width - recyclerView.paddingRight).toFloat()
        val midInsetPx = 16f * density
        for (r in separatorRects) {
            assertEquals("右端は全幅のまま", right, r.right, 0.5f)
            val isEdge = kotlin.math.abs(r.left - edgeLeft) < 0.5f
            val isMid = kotlin.math.abs(r.left - (edgeLeft + midInsetPx)) < 0.5f
            assertTrue("左端は従来の inset 規則のまま (left=${r.left})", isEdge || isMid)
        }
    }

    // MARK: - ヘルパ

    /**
     * `cellCount` 個の Cell を 1 セクションに含めた RecyclerView を構築し、measure / layout 済みの
     * 状態で返す。`KsSettingsListAdapter` + `ConcatAdapter` の組み合わせは `ClassicSectionDecoration`
     * が期待する構造そのもの。
     *
     * @return (recyclerView, decoration, density)
     */
    private fun prepareSingleSectionRecycler(cellCount: Int): Triple<RecyclerView, ClassicSectionDecoration, Float> {
        val context = ctx
        // 基本 Cell 群（LabelCell など）の ViewHolder を `KsCellRegistry` に登録する。
        // `KsSettingsView` を経由しない直接構築の場合、registry の自動初期化が走らないため
        // ここで明示的に呼ぶ必要がある（`KsSettingsView.init` でも同じ呼び出しがある）。
        KsCellRegistry.registerBasicCells(context)

        val recyclerView = RecyclerView(context)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val mainAdapter = KsSettingsListAdapter()
        val concat = ConcatAdapter(mainAdapter)
        recyclerView.adapter = concat

        // 1 セクション = N Cell の CellRow を投入（Header / Footer は含めない）。
        val items: List<CellListItem> = (0 until cellCount).map { idx ->
            CellListItem.CellRow(
                sectionId = "s1",
                cell = LabelCell(id = "c$idx", title = "Cell $idx"),
            )
        }
        // submitList は AsyncListDiffer の挙動で非同期になる場合があるため、submitList を呼んだ後に
        // メインループを流し切って内部リストへ反映させる。
        mainAdapter.submitList(items)
        idle()

        // 1080×1920 相当のサイズで measure / layout する。
        val width = 1080
        val height = 1920
        val parent = FrameLayout(context)
        parent.addView(
            recyclerView,
            FrameLayout.LayoutParams(width, height),
        )
        parent.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        parent.layout(0, 0, width, height)
        // RecyclerView の子をフォースバインドしておく（layout pass 経由）。

        val decoration = ClassicSectionDecoration(theme = Theme())
        // ItemDecoration として attach されることで実際の動作に近づく（重複描画は本テストでは行わない）。
        recyclerView.addItemDecoration(decoration)

        val density = context.resources.displayMetrics.density
        return Triple(recyclerView, decoration, density)
    }

    /**
     * 2 セクション × 2 Cell の RecyclerView を構築し、measure / layout 済みの状態で返す。
     * Section 単位の余白は Section の境界でしか観測できないため、境界を 1 つ持つ構成にする。
     *
     * @return (recyclerView, decoration, density)
     */
    private fun prepareTwoSectionRecycler(theme: Theme): Triple<RecyclerView, ClassicSectionDecoration, Float> {
        val context = ctx
        KsCellRegistry.registerBasicCells(context)

        val recyclerView = RecyclerView(context)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val mainAdapter = KsSettingsListAdapter()
        val concat = ConcatAdapter(mainAdapter)
        recyclerView.adapter = concat

        val items: List<CellListItem> = listOf("s1", "s2").flatMap { sectionId ->
            (0 until 2).map { index ->
                CellListItem.CellRow(
                    sectionId = sectionId,
                    cell = LabelCell(id = "$sectionId-c$index", title = "$sectionId Cell $index"),
                )
            }
        }
        mainAdapter.submitList(items)
        idle()

        val width = 1080
        val height = 1920
        val parent = FrameLayout(context)
        parent.addView(recyclerView, FrameLayout.LayoutParams(width, height))
        parent.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        parent.layout(0, 0, width, height)

        val decoration = ClassicSectionDecoration(theme = theme)
        recyclerView.addItemDecoration(decoration)

        return Triple(recyclerView, decoration, context.resources.displayMetrics.density)
    }

    // MARK: - Canvas 記録器

    /**
     * `drawRect` 呼び出しの矩形を全件記録するための Canvas。
     *
     * `ClassicSectionDecoration.onDrawOver` の検証専用。`drawRect(left, top, right, bottom, paint)`
     * の 4 引数版のみを使用する前提（実装側もそれだけを使用している）。
     */
    private class RecordingCanvas : Canvas() {
        data class Rect4(val left: Float, val top: Float, val right: Float, val bottom: Float)

        val rects: MutableList<Rect4> = mutableListOf()

        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
            rects += Rect4(left, top, right, bottom)
        }
    }
}
