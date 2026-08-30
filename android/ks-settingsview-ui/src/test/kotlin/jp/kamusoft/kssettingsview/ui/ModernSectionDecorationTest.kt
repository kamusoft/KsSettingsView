package jp.kamusoft.kssettingsview.ui

import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * `style = Modern` の Section 箱描画・余白・separator・合成契約を実表示経路で検証する。
 *
 * 観測は実際に `KsSettingsView` へ Store を bind し、レイアウトを走らせたうえで
 * `ItemDecoration` の `getItemOffsets` / `onDraw` / `onDrawOver` を呼び、
 * [DecorationCanvasRecorder] が記録した描画呼び出しに対して行う。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ModernSectionDecorationTest {

    /** `KsSettingsView` を 1 つだけ載せるホスト Activity。 */
    class HostActivity : FragmentActivity() {
        lateinit var container: FrameLayout
        lateinit var settingsView: KsSettingsView

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(MaterialR.style.Theme_Material3_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            container = FrameLayout(this)
            setContentView(container)
            settingsView = KsSettingsView(this)
            container.addView(settingsView)
        }
    }

    private var controller: ActivityController<HostActivity>? = null

    @After
    fun tearDown() {
        controller?.close()
        controller = null
    }

    private val viewportWidth = 1080
    private val viewportHeight = 1920

    /** 下地色と箱の色を区別できる Theme。既定は両方とも白のため観測が空振りする。 */
    private fun contrastTheme(
        sectionMargin: PaddingValues? = null,
        sectionCornerRadius: androidx.compose.ui.unit.Dp? = null,
        sectionBorderWidth: androidx.compose.ui.unit.Dp? = null,
        sectionBorderColor: Color? = null,
    ) = Theme(
        backgroundColor = Color(0xFFEFEFF4),
        cellBackgroundColor = Color(0xFFFFFFFF),
        separatorColor = Color(0xFFC8C7CC),
        sectionMargin = sectionMargin,
        sectionCornerRadius = sectionCornerRadius,
        sectionBorderWidth = sectionBorderWidth,
        sectionBorderColor = sectionBorderColor,
    )

    /** Modern で [root] を表示し、レイアウトまで済ませた Activity を返す。 */
    private fun host(
        root: SettingsRoot,
        theme: Theme = contrastTheme(),
        rootHeader: RootAccessory? = null,
        rootFooter: RootAccessory? = null,
    ): HostActivity {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()
        val view = activity.settingsView
        view.style = KsSettingsViewStyle.Modern
        if (rootHeader != null) view.rootHeader = rootHeader
        if (rootFooter != null) view.rootFooter = rootFooter
        view.bind(SettingsRootStore(initialRoot = root, initialTheme = theme))
        idle()
        layout(activity)
        return activity
    }

    private fun layout(activity: HostActivity) {
        val view = activity.settingsView
        view.measure(
            View.MeasureSpec.makeMeasureSpec(viewportWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(viewportHeight, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, viewportWidth, viewportHeight)
    }

    private fun decoration(activity: HostActivity): ModernSectionDecoration =
        activity.settingsView.internalCurrentDecoration() as ModernSectionDecoration

    private fun recyclerView(activity: HostActivity): RecyclerView =
        activity.settingsView.internalRecyclerView()

    private fun record(activity: HostActivity, over: Boolean): DecorationCanvasRecorder {
        val recorder = DecorationCanvasRecorder()
        val rv = recyclerView(activity)
        val state = RecyclerView.State()
        if (over) {
            decoration(activity).onDrawOver(recorder, rv, state)
        } else {
            decoration(activity).onDraw(recorder, rv, state)
        }
        return recorder
    }

    /** 画面に出ている行を「平坦リスト項目 → child View」の対応で取り出す。 */
    private fun rows(activity: HostActivity): List<Pair<CellListItem?, View>> {
        val rv = recyclerView(activity)
        return (0 until rv.childCount).map { index ->
            val child = rv.getChildAt(index)
            val holder = rv.getChildViewHolder(child)
            val bindingAdapter = holder?.bindingAdapter as? KsSettingsListAdapter
            val position = holder?.bindingAdapterPosition ?: -1
            val item = if (bindingAdapter != null && position >= 0) {
                bindingAdapter.currentList.getOrNull(position)
            } else {
                null
            }
            item to child
        }
    }

    private fun offsetsFor(activity: HostActivity, child: View): Rect {
        val outRect = Rect()
        decoration(activity).getItemOffsets(
            outRect,
            child,
            recyclerView(activity),
            RecyclerView.State(),
        )
        return outRect
    }

    /** 高さが 1px 以内の `drawRect` = separator の描画呼び出し。 */
    private fun separatorRects(recorder: DecorationCanvasRecorder) =
        recorder.rects.filter { (it.bottom - it.top) > 0.0f && (it.bottom - it.top) <= 1.5f }

    private fun sectionWithAccessories(
        id: String = "s1",
        cellCount: Int = 3,
        cellStyle: CellStyle = CellStyle(),
    ) = Section(
        id = id,
        header = SectionAccessory.Text("見出し $id"),
        footer = SectionAccessory.Text("補足 $id"),
        cells = (0 until cellCount).map { LabelCell(id = "$id-c$it", title = "$id-Cell$it", style = cellStyle) },
    )

    // MARK: - 箱の範囲

    @Test
    fun `箱は Section の Cell 行だけを覆い Header と Footer は箱の外に置かれる`() {
        val activity = host(SettingsRoot(sections = listOf(sectionWithAccessories())))
        val recorder = record(activity, over = false)

        assertEquals("Section 1 つにつき箱は 1 つ", 1, recorder.roundRects.size)
        val box = recorder.roundRects.first()

        val visible = rows(activity)
        val header = visible.first { it.first is CellListItem.SectionHeader }.second
        val footer = visible.first { it.first is CellListItem.SectionFooter }.second
        val cells = visible.filter { it.first is CellListItem.CellRow }.map { it.second }

        assertEquals("箱の上端は先頭 Cell の上端", cells.first().top.toFloat(), box.top, 0.5f)
        assertEquals("箱の下端は末尾 Cell の下端", cells.last().bottom.toFloat(), box.bottom, 0.5f)
        assertTrue("Header は箱の上外側にある", header.bottom <= box.top)
        assertTrue("Footer は箱の下外側にある", footer.top >= box.bottom)
    }

    @Test
    fun `箱の左右は sectionMargin の水平成分だけ内側に入る`() {
        val activity = host(SettingsRoot(sections = listOf(sectionWithAccessories())))
        val box = record(activity, over = false).roundRects.first()
        val rv = recyclerView(activity)
        val density = rv.resources.displayMetrics.density

        assertEquals("左端は 16dp inset", rv.paddingLeft + 16.0f * density, box.left, 0.5f)
        assertEquals("右端は 16dp inset", rv.width - rv.paddingRight - 16.0f * density, box.right, 0.5f)
    }

    @Test
    fun `水平 inset は Section Header と Footer 行にも入り箱の中の Cell と水平で揃う`() {
        val activity = host(
            SettingsRoot(sections = listOf(sectionWithAccessories())),
            rootHeader = RootAccessory.Text("Root Header"),
            rootFooter = RootAccessory.Text("Root Footer"),
        )
        idle()
        layout(activity)

        val density = recyclerView(activity).resources.displayMetrics.density
        val insetPx = (16.0f * density).toInt()

        var sectionRowCount = 0
        var rootRowCount = 0
        for ((item, child) in rows(activity)) {
            val offsets = offsetsFor(activity, child)
            if (item == null) {
                // Root Header / Footer は Section 装飾の対象外。
                rootRowCount++
                assertEquals("Root Header / Footer 行に左 inset は入らない", 0, offsets.left)
                assertEquals("Root Header / Footer 行に右 inset は入らない", 0, offsets.right)
            } else {
                sectionRowCount++
                assertEquals("Section 単位の行の左 inset (${item::class.simpleName})", insetPx, offsets.left)
                assertEquals("Section 単位の行の右 inset (${item::class.simpleName})", insetPx, offsets.right)
            }
        }
        assertEquals("Section Header / Cell x3 / Section Footer の 5 行を観測する", 5, sectionRowCount)
        assertEquals("Root Header / Footer の 2 行を観測する", 2, rootRowCount)
    }

    @Test
    fun `末尾に Cell を挿入すると箱が挿入後の末尾 Cell まで伸びる`() {
        val activity = host(SettingsRoot(sections = listOf(sectionWithAccessories(id = "s1", cellCount = 2))))
        val view = activity.settingsView
        val before = record(activity, over = false).roundRects.first()

        view.applyDiff(
            SettingsRootDiff.InsertCell(
                sectionId = "s1",
                index = 2,
                cell = LabelCell(id = "s1-added", title = "s1-Added"),
            ),
        )
        awaitDifferCommit({ committedTexts(view) }) { committedTexts(view).contains("s1-Added") }
        layout(activity)

        val after = record(activity, over = false).roundRects.first()
        // 挿入した行は「末尾の child」ではなく title で特定する。
        val addedRow = rows(activity).first { (item, _) ->
            ((item as? CellListItem.CellRow)?.cell as? LabelCell)?.title == "s1-Added"
        }.second

        assertTrue("箱の上端は挿入行より上にある", after.top <= addedRow.top.toFloat() + 0.5f)
        assertEquals("箱の下端は挿入行の下端まで伸びる", addedRow.bottom.toFloat(), after.bottom, 0.5f)
        assertTrue(
            "箱は挿入前より縦に伸びる (before=${before.bottom - before.top} / after=${after.bottom - after.top})",
            (after.bottom - after.top) > (before.bottom - before.top),
        )
        val footer = rows(activity).first { it.first is CellListItem.SectionFooter }.second
        assertTrue("Footer は箱の下外側のまま", footer.top >= after.bottom)
        assertEquals("Cell が 3 件になり中間 separator は 2 本", 2, separatorRects(record(activity, over = true)).size)
    }

    @Test
    fun `末尾の Cell を削除すると箱が削除後の末尾 Cell まで縮む`() {
        val activity = host(SettingsRoot(sections = listOf(sectionWithAccessories(id = "s1", cellCount = 3))))
        val view = activity.settingsView
        val before = record(activity, over = false).roundRects.first()

        view.applyDiff(SettingsRootDiff.RemoveCell(cellId = "s1-c2"))
        awaitDifferCommit({ committedTexts(view) }) { !committedTexts(view).contains("s1-Cell2") }
        layout(activity)

        val after = record(activity, over = false).roundRects.first()
        val remainingCells = rows(activity).filter { it.first is CellListItem.CellRow }.map { it.second }

        assertEquals("残る Cell は 2 件", 2, remainingCells.size)
        assertEquals("箱の下端は削除後の末尾 Cell の下端", remainingCells.last().bottom.toFloat(), after.bottom, 0.5f)
        assertTrue(
            "箱は削除前より縦に縮む (before=${before.bottom - before.top} / after=${after.bottom - after.top})",
            (after.bottom - after.top) < (before.bottom - before.top),
        )
        assertEquals("Cell が 2 件になり中間 separator は 1 本", 1, separatorRects(record(activity, over = true)).size)
    }

    @Test
    fun `可視 Cell を持たない Section には箱も separator も作らない`() {
        val activity = host(
            SettingsRoot(
                sections = listOf(
                    Section(id = "s1", header = SectionAccessory.Text("見出しだけ"), cells = emptyList()),
                ),
            ),
        )

        assertTrue("箱は描かれない", record(activity, over = false).roundRects.isEmpty())
        assertTrue("separator も描かれない", separatorRects(record(activity, over = true)).isEmpty())
        assertTrue(
            "Header 行は表示されている",
            rows(activity).any { it.first is CellListItem.SectionHeader },
        )
    }

    // MARK: - sectionMargin の Section 単位適用

    @Test
    fun `sectionMargin は Header の上と Footer の下に入り Header と箱の間には入らない`() {
        val activity = host(
            SettingsRoot(
                sections = listOf(
                    sectionWithAccessories(id = "s1", cellCount = 2),
                    sectionWithAccessories(id = "s2", cellCount = 2),
                ),
            ),
            theme = contrastTheme(
                sectionMargin = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 30.dp),
            ),
        )
        val density = recyclerView(activity).resources.displayMetrics.density
        val topPx = (20.0f * density).toInt()
        val bottomPx = (30.0f * density).toInt()

        val visible = rows(activity)
        fun offsetsOf(predicate: (CellListItem) -> Boolean): Rect {
            val child = visible.first { it.first?.let(predicate) == true }.second
            return offsetsFor(activity, child)
        }

        val s1Header = offsetsOf { it is CellListItem.SectionHeader && it.sectionId == "s1" }
        assertEquals("list 先頭の Header の上に top 余白が入る", topPx, s1Header.top)

        val s1FirstCell = offsetsOf { it is CellListItem.CellRow && it.sectionId == "s1" }
        assertEquals("Header と箱の間には余白を入れない", 0, s1FirstCell.top)

        val s1Footer = offsetsOf { it is CellListItem.SectionFooter && it.sectionId == "s1" }
        assertEquals("Footer の下に bottom 余白が入る", bottomPx, s1Footer.bottom)

        val s2Header = offsetsOf { it is CellListItem.SectionHeader && it.sectionId == "s2" }
        assertEquals("次 Section の Header の上に top 余白が入る", topPx, s2Header.top)

        val s2Footer = offsetsOf { it is CellListItem.SectionFooter && it.sectionId == "s2" }
        assertEquals("list 末尾の Footer の下に bottom 余白が入る", bottomPx, s2Footer.bottom)
    }

    @Test
    fun `Section 単位の上下余白は Root Header の内側と Root Footer の内側に入る`() {
        val activity = host(
            SettingsRoot(sections = listOf(sectionWithAccessories(id = "s1", cellCount = 2))),
            theme = contrastTheme(
                sectionMargin = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 30.dp),
            ),
            rootHeader = RootAccessory.Text("Root Header"),
            rootFooter = RootAccessory.Text("Root Footer"),
        )
        idle()
        layout(activity)

        val density = recyclerView(activity).resources.displayMetrics.density
        val topPx = (20.0f * density).toInt()
        val bottomPx = (30.0f * density).toInt()

        val rv = recyclerView(activity)
        val rootHeaderRow = rv.getChildAt(0)
        val rootFooterRow = rv.getChildAt(rv.childCount - 1)

        assertEquals("Root Header の上に余白は入らない", 0, offsetsFor(activity, rootHeaderRow).top)
        assertEquals("Root Footer の下に余白は入らない", 0, offsetsFor(activity, rootFooterRow).bottom)

        val visible = rows(activity)
        val sectionHeader = visible.first { it.first is CellListItem.SectionHeader }.second
        val sectionFooter = visible.first { it.first is CellListItem.SectionFooter }.second
        assertEquals(
            "Root Header と先頭 Section の間に top 余白が入る",
            topPx,
            offsetsFor(activity, sectionHeader).top,
        )
        assertEquals(
            "末尾 Section と Root Footer の間に bottom 余白が入る",
            bottomPx,
            offsetsFor(activity, sectionFooter).bottom,
        )
    }

    @Test
    fun `Root Header と Footer が無ければ Section の上下余白がそのまま list 端に出る`() {
        val activity = host(
            SettingsRoot(sections = listOf(sectionWithAccessories(id = "s1", cellCount = 2))),
            theme = contrastTheme(
                sectionMargin = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 30.dp),
            ),
        )
        val density = recyclerView(activity).resources.displayMetrics.density
        val rv = recyclerView(activity)

        assertEquals(
            "list 先頭行の上に top 余白が入る",
            (20.0f * density).toInt(),
            offsetsFor(activity, rv.getChildAt(0)).top,
        )
        assertEquals(
            "list 末尾行の下に bottom 余白が入る",
            (30.0f * density).toInt(),
            offsetsFor(activity, rv.getChildAt(rv.childCount - 1)).bottom,
        )
    }

    // MARK: - separator 規則

    @Test
    fun `separator は Cell 間にだけ描かれ箱の上下端には描かれない`() {
        val activity = host(SettingsRoot(sections = listOf(sectionWithAccessories(cellCount = 3))))
        val separators = separatorRects(record(activity, over = true))
        val cells = rows(activity).filter { it.first is CellListItem.CellRow }.map { it.second }

        assertEquals("3 Cell なら中間 separator は 2 本", 2, separators.size)
        val expectedBottoms = cells.dropLast(1).map { it.bottom.toFloat() }.sorted()
        assertEquals(
            "separator は先頭・中間 Cell の下端に引かれる",
            expectedBottoms,
            separators.map { it.bottom }.sorted(),
        )
        val boxTop = cells.first().top.toFloat()
        val boxBottom = cells.last().bottom.toFloat()
        assertTrue("箱の上端に separator は出ない", separators.none { it.top <= boxTop + 0.5f })
        assertTrue("箱の下端に separator は出ない", separators.none { it.bottom >= boxBottom - 0.5f })
    }

    @Test
    fun `単一 Cell の Section には separator を描かない`() {
        val activity = host(SettingsRoot(sections = listOf(sectionWithAccessories(cellCount = 1))))
        assertTrue(separatorRects(record(activity, over = true)).isEmpty())
    }

    @Test
    fun `中間 separator は箱の内側の端から左右同量 inset される`() {
        val activity = host(
            SettingsRoot(sections = listOf(sectionWithAccessories(cellCount = 3))),
            theme = contrastTheme(sectionBorderWidth = 2.dp, sectionBorderColor = Color(0xFF335577)),
        )
        val density = recyclerView(activity).resources.displayMetrics.density
        val box = record(activity, over = false).roundRects.first()
        val separators = separatorRects(record(activity, over = true))
        val inset = 16.0f * density + 2.0f * density

        assertTrue("separator が描かれている", separators.isNotEmpty())
        for (separator in separators) {
            assertEquals("leading 側は箱の内側の端から inset", box.left + inset, separator.left, 0.5f)
            assertEquals("trailing 側も同量 inset", box.right - inset, separator.right, 0.5f)
        }
    }

    @Test
    fun `separator の色は separatorColor で太さは 1 物理 pixel`() {
        val activity = host(SettingsRoot(sections = listOf(sectionWithAccessories(cellCount = 3))))
        val separators = separatorRects(record(activity, over = true))

        assertTrue(separators.isNotEmpty())
        for (separator in separators) {
            assertEquals("太さは 1px 固定", 1.0f, separator.bottom - separator.top, 0.001f)
            assertEquals("色は separatorColor", Color(0xFFC8C7CC).toArgb(), separator.color)
        }
    }

    @Test
    fun `背景色付き Cell が並んでも separator は Cell の上に描かれる`() {
        val activity = host(
            SettingsRoot(
                sections = listOf(
                    sectionWithAccessories(
                        cellCount = 3,
                        cellStyle = CellStyle(backgroundColor = Color(0xFFAABBCC)),
                    ),
                ),
            ),
        )

        assertTrue(
            "Cell の下 (onDraw) には separator を描かない",
            separatorRects(record(activity, over = false)).isEmpty(),
        )
        assertEquals(
            "Cell の上 (onDrawOver) に separator を描く",
            2,
            separatorRects(record(activity, over = true)).size,
        )
    }

    // MARK: - 合成契約

    @Test
    fun `角丸の外へ出た Cell 背景は下地色で覆われる`() {
        val activity = host(
            SettingsRoot(sections = listOf(sectionWithAccessories(cellCount = 2))),
            theme = contrastTheme(sectionCornerRadius = 12.dp),
        )
        val box = record(activity, over = false).roundRects.first()
        val recorder = record(activity, over = true)

        val clipIndex = recorder.ops.indexOfFirst { it is DecorationCanvasRecorder.Op.ClipOut }
        assertTrue("角丸の外をクリップする", clipIndex >= 0)

        val cover = recorder.ops
            .drop(clipIndex)
            .filterIsInstance<DecorationCanvasRecorder.Op.Rect>()
            .firstOrNull {
                it.color == Color(0xFFEFEFF4).toArgb() && it.style == Paint.Style.FILL
            }
        assertNotNull("クリップ後に下地色で箱の外接矩形を塗る", cover)
        assertEquals("覆う範囲は箱の外接矩形", box.left, cover!!.left, 0.5f)
        assertEquals(box.top, cover.top, 0.5f)
        assertEquals(box.right, cover.right, 0.5f)
        assertEquals(box.bottom, cover.bottom, 0.5f)
    }

    @Test
    fun `押下中の Cell でも角の被覆は箱の外接矩形を覆う`() {
        val activity = host(
            SettingsRoot(sections = listOf(sectionWithAccessories(cellCount = 2))),
            theme = contrastTheme(sectionCornerRadius = 12.dp),
        )
        val firstCell = rows(activity).first { it.first is CellListItem.CellRow }.second
        firstCell.isPressed = true

        val box = record(activity, over = false).roundRects.first()
        val recorder = record(activity, over = true)
        val clipIndex = recorder.ops.indexOfFirst { it is DecorationCanvasRecorder.Op.ClipOut }
        assertTrue("押下中でも角丸の外をクリップする", clipIndex >= 0)

        val cover = recorder.ops
            .drop(clipIndex)
            .filterIsInstance<DecorationCanvasRecorder.Op.Rect>()
            .firstOrNull { it.color == Color(0xFFEFEFF4).toArgb() && it.style == Paint.Style.FILL }
        assertNotNull("押下背景も含めて角の外側を下地色で覆う", cover)
        assertEquals(box.top, cover!!.top, 0.5f)
        assertEquals(box.bottom, cover.bottom, 0.5f)
    }

    @Test
    fun `ボーダーは separator と Cell 背景より後に描かれ最前面に来る`() {
        val activity = host(
            SettingsRoot(sections = listOf(sectionWithAccessories(cellCount = 3))),
            theme = contrastTheme(sectionBorderWidth = 2.dp, sectionBorderColor = Color(0xFF335577)),
        )
        val recorder = record(activity, over = true)
        val density = recyclerView(activity).resources.displayMetrics.density

        val borderIndex = recorder.ops.indexOfLast {
            it is DecorationCanvasRecorder.Op.RoundRect && it.style == Paint.Style.STROKE
        }
        assertTrue("ボーダーが描かれる", borderIndex >= 0)
        val border = recorder.ops[borderIndex] as DecorationCanvasRecorder.Op.RoundRect
        assertEquals("線幅は指定値", 2.0f * density, border.strokeWidth, 0.001f)
        assertEquals("色は指定値", Color(0xFF335577).toArgb(), border.color)

        val lastSeparatorIndex = recorder.ops.indexOfLast {
            it is DecorationCanvasRecorder.Op.Rect && (it.bottom - it.top) <= 1.5f
        }
        assertTrue("ボーダーは separator より後に描く", borderIndex > lastSeparatorIndex)

        val lastCoverIndex = recorder.ops.indexOfLast {
            it is DecorationCanvasRecorder.Op.Rect && it.style == Paint.Style.FILL && (it.bottom - it.top) > 1.5f
        }
        assertTrue("ボーダーは角の被覆より後に描く", borderIndex > lastCoverIndex)
    }

    @Test
    fun `ボーダー未指定なら描画しない`() {
        val activity = host(SettingsRoot(sections = listOf(sectionWithAccessories(cellCount = 2))))
        val strokes = record(activity, over = true).roundRects.filter { it.style == Paint.Style.STROKE }
        assertTrue("既定の Modern にボーダーは描かれない", strokes.isEmpty())
    }

    // MARK: - 長い Section の箱端

    @Test
    fun `viewport より長い Section の中間部では偽の箱端が出ない`() {
        val activity = host(
            SettingsRoot(sections = listOf(sectionWithAccessories(id = "s1", cellCount = 120))),
            theme = contrastTheme(sectionCornerRadius = 12.dp),
        )
        val rv = recyclerView(activity)
        val density = rv.resources.displayMetrics.density
        (rv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(60, 0)
        idle()
        layout(activity)

        val visibleCells = rows(activity).filter { it.first is CellListItem.CellRow }
        assertTrue("中間部までスクロールできている", visibleCells.isNotEmpty())
        val positions = visibleCells.mapNotNull { (item, _) ->
            (item as CellListItem.CellRow).cell.id.removePrefix("s1-c").toIntOrNull()
        }
        assertTrue("Section の先頭 Cell は画面外にある", positions.min() > 0)
        assertTrue("Section の末尾 Cell も画面外にある", positions.max() < 119)

        val box = record(activity, over = false).roundRects.first()
        val radiusPx = 12.0f * density
        assertTrue(
            "箱の上端は viewport の外へ延長され角丸が画面内に現れない (top=${box.top})",
            box.top <= -radiusPx,
        )
        assertTrue(
            "箱の下端も viewport の外へ延長される (bottom=${box.bottom}, height=${rv.height})",
            box.bottom >= rv.height + radiusPx,
        )
    }

    // MARK: - 負値・非有限値の正規化

    @Test
    fun `負の寸法を持つ Theme でも 0 として描画され例外を出さない`() {
        val activity = host(
            SettingsRoot(sections = listOf(sectionWithAccessories(cellCount = 2))),
            theme = contrastTheme(
                sectionMargin = RawPaddingValues(start = (-16).dp, top = (-8).dp, end = (-16).dp, bottom = (-8).dp),
                sectionBorderWidth = (-4).dp,
            ),
        )
        val rv = recyclerView(activity)
        val box = record(activity, over = false).roundRects.first()

        assertEquals("左端は inset 0", rv.paddingLeft.toFloat(), box.left, 0.5f)
        assertEquals("右端は inset 0", (rv.width - rv.paddingRight).toFloat(), box.right, 0.5f)
        for ((_, child) in rows(activity)) {
            val offsets = offsetsFor(activity, child)
            assertEquals(0, offsets.top)
            assertEquals(0, offsets.bottom)
            assertEquals(0, offsets.left)
            assertEquals(0, offsets.right)
        }
        assertTrue(
            "ボーダーは描かれない",
            record(activity, over = true).roundRects.none { it.style == Paint.Style.STROKE },
        )
    }

    @Test
    fun `非有限の寸法を持つ Theme でも 0 として描画され例外を出さない`() {
        val activity = host(
            SettingsRoot(sections = listOf(sectionWithAccessories(cellCount = 2))),
            theme = contrastTheme(
                sectionMargin = RawPaddingValues(
                    start = Float.POSITIVE_INFINITY.dp,
                    top = Float.NaN.dp,
                    end = Float.NEGATIVE_INFINITY.dp,
                    bottom = Float.NaN.dp,
                ),
                sectionCornerRadius = Float.POSITIVE_INFINITY.dp,
                sectionBorderWidth = Float.NaN.dp,
            ),
        )
        val rv = recyclerView(activity)
        val box = record(activity, over = false).roundRects.first()

        assertEquals("左端は inset 0", rv.paddingLeft.toFloat(), box.left, 0.5f)
        assertEquals("右端は inset 0", (rv.width - rv.paddingRight).toFloat(), box.right, 0.5f)
        assertEquals("非有限の角丸半径は 0 として描く", 0.0f, box.radius, 0.001f)
        for ((_, child) in rows(activity)) {
            val offsets = offsetsFor(activity, child)
            assertEquals(0, offsets.top)
            assertEquals(0, offsets.bottom)
            assertEquals(0, offsets.left)
            assertEquals(0, offsets.right)
        }
        assertTrue(
            "ボーダーは描かれない",
            record(activity, over = true).roundRects.none { it.style == Paint.Style.STROKE },
        )
    }

    // MARK: - Theme 変更 / style 切替

    @Test
    fun `実行時の Theme 変更で角丸が再解決される`() {
        val activity = host(
            SettingsRoot(sections = listOf(sectionWithAccessories(cellCount = 2))),
            theme = contrastTheme(sectionCornerRadius = 4.dp),
        )
        val density = recyclerView(activity).resources.displayMetrics.density
        val before = record(activity, over = false).roundRects.first()
        assertEquals(4.0f * density, before.radius, 0.001f)

        val titlesBefore = committedTexts(activity.settingsView)
        activity.settingsView.theme = contrastTheme(sectionCornerRadius = 24.dp)
        idle()
        layout(activity)

        val after = record(activity, over = false).roundRects.first()
        assertEquals("新しい角丸半径で描き直す", 24.0f * density, after.radius, 0.001f)
        assertEquals("Section / Cell の identity は変わらない", titlesBefore, committedTexts(activity.settingsView))
    }

    @Test
    fun `Classic から Modern への切替で内容を保ったまま箱型の装飾になる`() {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()
        val view = activity.settingsView
        view.bind(
            SettingsRootStore(
                initialRoot = SettingsRoot(sections = listOf(sectionWithAccessories(cellCount = 2))),
                initialTheme = contrastTheme(),
            ),
        )
        idle()
        layout(activity)

        val before = committedTexts(view)
        assertTrue(view.internalCurrentDecoration() is ClassicSectionDecoration)

        view.style = KsSettingsViewStyle.Modern
        idle()
        layout(activity)

        assertTrue("Modern の装飾へ切り替わる", view.internalCurrentDecoration() is ModernSectionDecoration)
        assertEquals("Cell の内容と順序は変わらない", before, committedTexts(view))
        assertEquals("箱が描かれる", 1, record(activity, over = false).roundRects.size)
    }
}
