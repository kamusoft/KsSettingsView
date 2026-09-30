package jp.kamusoft.kssettingsview.ui

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot

/*
 * スクロール制御の Robolectric テストが共有する、ホスト・データ・位置の観測・待機のユーティリティ。
 */

/** スクロール制御のテストで Host の高さに使う値 (px)。内容はこれより十分に高くする。 */
internal const val SCROLL_TEST_HOST_HEIGHT: Int = 400

/** `KsSettingsView` を載せる器だけを持つホスト Activity。 */
class ScrollTestHostActivity : ComponentActivity() {
    lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = FrameLayout(this)
        setContentView(container)
    }
}

/**
 * 見出し・Cell・Footer を持つ Section を並べた root。
 *
 * 見出しと Footer の行は Cell の行と高さが異なるため、行の高さが混在した一覧になる。
 * Cell の id は `c<Section>_<Cell>`、Section の id は `s<Section>`。
 */
internal fun scrollTestRoot(sectionCount: Int = 10, cellsPerSection: Int = 5): SettingsRoot = SettingsRoot(
    sections = (0 until sectionCount).map { s -> scrollTestSection(s, cellsPerSection) },
)

internal fun scrollTestSection(index: Int, cellsPerSection: Int = 5): Section = Section(
    id = "s$index",
    header = SectionAccessory.Text("Section $index"),
    footer = SectionAccessory.Text("Footer $index"),
    cells = (0 until cellsPerSection).map { c -> LabelCell(id = "c${index}_$c", title = "Cell $index-$c") },
)

/** 一覧の表示範囲の上端 (内容の余白を除く)。 */
internal fun listTop(view: KsSettingsView): Int = view.internalRecyclerView().paddingTop

/** 一覧の表示範囲の下端 (内容の余白を除く)。 */
internal fun listBottom(view: KsSettingsView): Int {
    val rv = view.internalRecyclerView()
    return rv.height - rv.paddingBottom
}

/** ConcatAdapter 全体での、[predicate] に当たる平坦リストの行の位置 (無ければ -1)。 */
internal fun adapterPositionOf(view: KsSettingsView, predicate: (CellListItem) -> Boolean): Int {
    val index = view.internalMainListAdapter().currentList.indexOfFirst(predicate)
    return if (index < 0) -1 else view.internalHeaderAdapter().itemCount + index
}

internal fun cellPosition(view: KsSettingsView, cellId: String): Int =
    adapterPositionOf(view) { it is CellListItem.CellRow && it.cell.id == cellId }

internal fun sectionHeaderPosition(view: KsSettingsView, sectionId: String): Int =
    adapterPositionOf(view) { it is CellListItem.SectionHeader && it.sectionId == sectionId }

/** 行位置 [position] に配置されている行の View (配置されていなければ `null`)。 */
internal fun rowAt(view: KsSettingsView, position: Int): View? =
    (view.internalRecyclerView().layoutManager as LinearLayoutManager).findViewByPosition(position)

internal fun cellRow(view: KsSettingsView, cellId: String): View? = rowAt(view, cellPosition(view, cellId))

/** 表示範囲の上端にかかる最初の行の位置と上端 (テスト失敗時の要約用)。 */
internal fun scrollSummary(view: KsSettingsView): String {
    val rv = view.internalRecyclerView()
    val rows = (0 until rv.childCount).map { rv.getChildAt(it) }
        .sortedBy { it.top }
        .joinToString { "${rv.getChildAdapterPosition(it)}@${it.top}..${it.bottom}" }
    return "processed=${view.internalProcessedScrollEntryCount()} state=${rv.scrollState} " +
        "smooth=${rv.layoutManager?.isSmoothScrolling} layoutRequested=${rv.isLayoutRequested} rows=[$rows]"
}

/**
 * 命令が [count] 件処理され、スクロールが止まり、レイアウトが確定するまで待つ。
 *
 * 処理済みの件数は、対象が見つからず何もしなかった命令も数えるため、表示を変えない命令の処理完了も
 * この条件で待てる。
 */
internal fun awaitScrollSettled(view: KsSettingsView, count: Int) {
    val rv = view.internalRecyclerView()
    awaitMainLooperCondition(diagnostics = { scrollSummary(view) }) {
        view.internalProcessedScrollEntryCount() >= count &&
            rv.scrollState == RecyclerView.SCROLL_STATE_IDLE &&
            rv.layoutManager?.isSmoothScrolling == false &&
            !rv.isLayoutRequested &&
            !view.isLayoutRequested
    }
}

/** Host の平坦リストの commit と、それを反映したレイアウトが済むまで待つ。 */
internal fun awaitRootShown(view: KsSettingsView) {
    val rv = view.internalRecyclerView()
    // 非表示の Cell は行にならないので、root を表示用に展開した結果と比べる。
    fun expected(): List<String> = KsSettingsView.flatten(view.internalRoot().sections)
        .mapNotNull { (it as? CellListItem.CellRow)?.cell?.id }
    awaitMainLooperCondition(diagnostics = { "committed=${committedCellIds(view)} / expected=${expected()} ${scrollSummary(view)}" }) {
        view.internalMainListAdapter().hasCommittedLatestSubmission &&
            committedCellIds(view) == expected() &&
            rv.childCount > 0 &&
            !rv.isLayoutRequested &&
            !rv.hasPendingAdapterUpdates()
    }
}

/** RecyclerView の縦方向のスクロール量を、スクロールのたびに記録する。 */
internal class ScrollDeltaRecorder : RecyclerView.OnScrollListener() {
    val deltas: MutableList<Int> = mutableListOf()

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (dy != 0) deltas += dy
    }
}
