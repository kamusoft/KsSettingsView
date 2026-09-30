package jp.kamusoft.kssettingsview.ui

import android.os.Handler
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.core.SettingsRoot
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Host の待ち行列に積む要素。
 *
 * ハンドルからの命令と、控えた位置の復元を同じ待ち行列で扱う。復元も命令と同じくデータの反映と
 * レイアウトの後に実行され、後から積まれた命令より先に処理される。
 */
internal sealed class KsScrollQueueEntry {
    /** ハンドルから届いた命令。 */
    data class Command(val command: KsScrollCommand) : KsScrollQueueEntry()

    /** `restoreScrollAnchor` で要求された位置の復元。 */
    data class Restore(val anchor: KsScrollAnchor) : KsScrollQueueEntry()
}

/**
 * [KsSettingsView] のスクロール命令の待ち行列・位置の解決・送り方と、表示位置を控える窓口の実体。
 *
 * 命令は Store を経由せず、Host につないだハンドルから届く (core/ADR-0037)。受けた処理の中では
 * 実行せず、メインスレッドへ 1 回 post して遅らせる。Store の更新は `tryEmit` → `lifecycleScope` の
 * collect → `submitList` の差分計算 → commit と非同期に 2 段で表示へ届くため、1 回遅らせる間に
 * collect を先に通し、そのうえで最後に提出した一覧の commit と、それに続くレイアウトを待ってから
 * 実行する。これにより「Store へ項目を追加 → 直後に末尾へ」を同じ処理で書ける。
 *
 * 位置は実行の直前に、その時点で配置されている行から求める。配置されていない行は推定の高さを
 * 使わず、`LinearSmoothScroller` で対象へ向かいながら、必要な行が配置された時点で残りの距離を
 * 解き直して一方向に止める。
 *
 * @param recyclerView Host の内部 RecyclerView
 * @param headerAdapter Root Header の Adapter
 * @param mainListAdapter Section H/F と Cell の平坦リストの Adapter
 * @param footerAdapter Root Footer の Adapter
 * @param handler 命令を遅らせる post 先 (メインスレッド)
 * @param modelRoot 非表示の要素を含む現在の root (「非表示」と「存在しない」を区別するため)
 * @param isAttachedToWindow Host が window に取り付けられているか
 */
internal class KsSettingsViewScrollControl(
    private val recyclerView: RecyclerView,
    private val headerAdapter: RootHeaderFooterAdapter,
    private val mainListAdapter: KsSettingsListAdapter,
    private val footerAdapter: RootHeaderFooterAdapter,
    private val handler: Handler,
    private val modelRoot: () -> SettingsRoot,
    private val isAttachedToWindow: () -> Boolean,
) {

    /** 受け取ってから、まだ 1 回の遅延を経ていない要素。 */
    private val incomingEntries: MutableList<KsScrollQueueEntry> = mutableListOf()

    /** 遅延を経て、実行できる状態になるのを待っている要素。 */
    private val readyEntries: MutableList<KsScrollQueueEntry> = mutableListOf()

    /** 受け取った要素を遅らせる post を予約済みか。 */
    private var isDeferralScheduled: Boolean = false

    /**
     * アニメーションしない送りで、行を配置させるために `scrollToPositionWithOffset` を出した行き先。
     *
     * 次のレイアウトの後に、配置された行から最終位置を求めて詰める。詰め終わるまで後続の要素は
     * 実行しない (後続の要素は詰めた後の表示で解決されるべきため)。
     */
    private var pendingCorrection: ScrollPlan? = null

    /**
     * 処理し終えた要素の数。対象が見つからず何もしなかった要素も数える
     * (表示に変化を起こさない命令でも、処理が済んだ時点をテストが待てるようにするため)。
     */
    var processedEntryCount: Int = 0
        private set

    private val layoutManager: LinearLayoutManager
        get() = recyclerView.layoutManager as LinearLayoutManager

    // MARK: - 待ち行列

    /**
     * 要素を待ち行列に積み、1 回遅らせてから実行を試みる。
     *
     * 遅延は、同じ処理の中で後から行われるデータの変更 (Store の更新) の通知を、命令より先に Host へ
     * 届けるため。同じ処理の中で積まれた要素は、1 回の遅延をまとめて経る。
     */
    fun enqueue(entry: KsScrollQueueEntry) {
        incomingEntries += entry
        if (isDeferralScheduled) return
        isDeferralScheduled = true
        handler.post {
            isDeferralScheduled = false
            readyEntries += incomingEntries
            incomingEntries.clear()
            flushIfPossible()
        }
    }

    /** ハンドルから届いた未実行の命令を捨てる。位置の復元は Host 自身への要求なので残す。 */
    fun dropPendingCommands() {
        incomingEntries.removeAll { it is KsScrollQueueEntry.Command }
        readyEntries.removeAll { it is KsScrollQueueEntry.Command }
    }

    /**
     * 要素を実行できる状態か。
     *
     * window に取り付けられ、最後に提出した一覧の commit が済み、それを反映したレイアウトが終わって
     * いること。画面から外れている間 (上に別の画面を重ねた遷移など) に受けた命令は、取り付け直した
     * 後のレイアウトで実行する。
     */
    private fun canExecute(): Boolean =
        isAttachedToWindow() &&
            recyclerView.adapter != null &&
            mainListAdapter.hasCommittedLatestSubmission &&
            recyclerView.width > 0 &&
            recyclerView.height > 0 &&
            !recyclerView.isLayoutRequested &&
            !recyclerView.hasPendingAdapterUpdates() &&
            pendingCorrection == null

    /** 遅延を経た要素を、実行できる状態なら呼ばれた順に実行する。 */
    fun flushIfPossible() {
        while (readyEntries.isNotEmpty() && canExecute()) {
            val entry = readyEntries.removeAt(0)
            execute(entry)
            processedEntryCount++
        }
    }

    /**
     * Host のレイアウトの後に呼ぶ。アニメーションしない送りの残りを詰めてから、待っている要素を実行する。
     */
    fun onHostLayout() {
        val correction = pendingCorrection
        if (correction != null && !recyclerView.isLayoutRequested && isAttachedToWindow()) {
            pendingCorrection = null
            settle(correction)
        }
        flushIfPossible()
    }

    /**
     * 1 件の要素を実行する。実行の直前に最新の表示で対象を解決し、無ければ何もしない。
     *
     * 対象が無い要素は、先行のスクロールも止めない (何もしない命令が進行中のアニメーションの行き先を
     * 変えないため)。
     */
    private fun execute(entry: KsScrollQueueEntry) {
        val plan = planFor(entry) ?: return
        // 対象が解決できたときだけ、先行のスクロールを止めてから実行する。最終位置は最後の要素のものになる。
        recyclerView.stopScroll()
        val delta = resolveDelta(plan)
        if (plan.animated) {
            if (delta != null) {
                val clamped = clampToScrollableRange(delta)
                if (clamped != 0) recyclerView.smoothScrollBy(0, clamped)
            } else {
                val direction = searchDirection(plan)
                val scroller = AlignmentSmoothScroller(plan, direction)
                scroller.targetPosition = if (direction > 0) plan.lastPosition else plan.firstPosition
                layoutManager.startSmoothScroll(scroller)
            }
            return
        }
        if (delta != null) {
            settle(plan)
        } else {
            // 行き先の行が配置されていないので、まず行を配置させる。最終位置は次のレイアウトの後に詰める。
            layoutManager.scrollToPositionWithOffset(plan.placementPosition, 0)
            pendingCorrection = plan
        }
    }

    /**
     * アニメーションせずに、配置済みの行から求めた最終位置へ詰める。
     *
     * 送った先で新しく配置された行の高さが確定し、行き先の位置もずれ得るため、数回求め直して詰める。
     */
    private fun settle(plan: ScrollPlan) {
        repeat(MAX_SETTLE_PASSES) {
            val delta = resolveDelta(plan) ?: return
            val clamped = clampToScrollableRange(delta)
            if (clamped == 0) return
            recyclerView.scrollBy(0, clamped)
        }
    }

    // MARK: - 対象の解決

    /** 要素を行き先へ解決する。対象が表示に無ければ `null`。 */
    private fun planFor(entry: KsScrollQueueEntry): ScrollPlan? {
        val headerCount = headerAdapter.itemCount
        return when (entry) {
            is KsScrollQueueEntry.Restore -> {
                val position = positionOf(entry.anchor.element) ?: return null
                ScrollPlan(position, position, Alignment.Anchor(entry.anchor.offsetFromTop), animated = false)
            }
            is KsScrollQueueEntry.Command -> when (val command = entry.command) {
                is KsScrollCommand.ToCell -> {
                    val cellId = command.id as? String
                    if (cellId == null || !modelContainsCell(cellId)) {
                        warnUnknownTarget(kind = "cell", id = command.id)
                        return null
                    }
                    // 非表示の Cell は行を持たないので黙って何もしない。
                    val index = mainListAdapter.currentList.indexOfFirst {
                        it is CellListItem.CellRow && it.cell.id == cellId
                    }
                    if (index < 0) return null
                    val position = headerCount + index
                    ScrollPlan(position, position, Alignment.Range(command.position), command.animated)
                }
                is KsScrollCommand.ToSection -> {
                    val sectionId = command.id as? String
                    if (sectionId == null || modelRoot().sections.none { it.id == sectionId }) {
                        warnUnknownTarget(kind = "section", id = command.id)
                        return null
                    }
                    // 平坦リストは Section ごとに見出し → Cell → Footer の順に並ぶので、同じ Section の
                    // 最初の行と最後の行が範囲の上端と下端になる。非表示の Section と表示される行の無い
                    // Section は行を持たない。
                    val list = mainListAdapter.currentList
                    val first = list.indexOfFirst { it.sectionId == sectionId }
                    if (first < 0) return null
                    val last = list.indexOfLast { it.sectionId == sectionId }
                    ScrollPlan(headerCount + first, headerCount + last, Alignment.Range(command.position), command.animated)
                }
                is KsScrollCommand.ToStart -> {
                    if (totalItemCount() == 0) return null
                    ScrollPlan(0, 0, Alignment.ContentStart, command.animated)
                }
                is KsScrollCommand.ToEnd -> {
                    val total = totalItemCount()
                    if (total == 0) return null
                    ScrollPlan(total - 1, total - 1, Alignment.ContentEnd, command.animated)
                }
            }
        }
    }

    private fun modelContainsCell(cellId: String): Boolean =
        modelRoot().sections.any { section -> section.cells.any { it.id == cellId } }

    private fun warnUnknownTarget(kind: String, id: Any) {
        if (!KsCellRegistry.strictMode) return
        Log.w(LOG_TAG, "KsSettingsView: ignored a scroll command to an unknown $kind id $id")
    }

    private fun totalItemCount(): Int =
        headerAdapter.itemCount + mainListAdapter.itemCount + footerAdapter.itemCount

    // MARK: - 位置の解決

    /**
     * 行き先へ合わせるために必要なスクロール量 (正で下へ) を、配置済みの行から求める。
     *
     * 表示範囲は内容の余白 (padding) を除いた領域とする。表示範囲より高い範囲は上端に合わせる。
     * 必要な行が配置されていなければ `null`。
     */
    private fun resolveDelta(plan: ScrollPlan): Int? {
        val lm = layoutManager
        val top = recyclerView.paddingTop
        val bottom = recyclerView.height - recyclerView.paddingBottom
        return when (val alignment = plan.alignment) {
            Alignment.ContentStart -> lm.findViewByPosition(plan.firstPosition)?.let { lm.getDecoratedTop(it) - top }
            Alignment.ContentEnd -> lm.findViewByPosition(plan.lastPosition)?.let { lm.getDecoratedBottom(it) - bottom }
            is Alignment.Anchor -> lm.findViewByPosition(plan.firstPosition)?.let { it.top + alignment.offsetFromTop - top }
            is Alignment.Range -> resolveRangeDelta(plan, alignment.position, top, bottom)
        }
    }

    /**
     * 行の範囲 (Cell の行 1 つ、または Section の最初の行から最後の行まで) を位置へ合わせる量。
     *
     * 範囲の上端・下端は行そのものの上端・下端とし、Section 単位の外側の余白 (装飾の offset) は
     * 含めない。
     */
    private fun resolveRangeDelta(plan: ScrollPlan, position: KsScrollPosition, top: Int, bottom: Int): Int? {
        val lm = layoutManager
        val firstView = lm.findViewByPosition(plan.firstPosition) ?: return null
        val startDelta = firstView.top - top
        if (position == KsScrollPosition.Start) return startDelta
        val visibleHeight = bottom - top
        val lastView = lm.findViewByPosition(plan.lastPosition)
        if (lastView == null) {
            // 最後の行はまだ配置されていない。配置済みの行だけで表示範囲を超えていれば、表示範囲より
            // 高い範囲として上端に合わせる。そうでなければ高さが確定するまで解けない。
            val laidOutBottom = laidOutBottomWithin(plan.firstPosition..plan.lastPosition) ?: return null
            return if (laidOutBottom - firstView.top > visibleHeight) startDelta else null
        }
        if (lastView.bottom - firstView.top > visibleHeight) return startDelta
        return when (position) {
            KsScrollPosition.Start -> startDelta
            KsScrollPosition.Center -> ((firstView.top + lastView.bottom) - (top + bottom)) / 2
            KsScrollPosition.End -> lastView.bottom - bottom
        }
    }

    /** 範囲 [positions] に入る配置済みの行の下端のうち最大のもの。1 行も無ければ `null`。 */
    private fun laidOutBottomWithin(positions: IntRange): Int? {
        var result: Int? = null
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val position = recyclerView.getChildLayoutPosition(child)
            if (position !in positions) continue
            result = max(result ?: child.bottom, child.bottom)
        }
        return result
    }

    /**
     * スクロール量を、配置済みの両端の行から分かるスクロール可能範囲に収める。
     *
     * 内容の先頭・末尾の行が配置されていなければ収めない (その場合も RecyclerView は端で止まる)。
     */
    private fun clampToScrollableRange(delta: Int): Int {
        val lm = layoutManager
        if (delta > 0) {
            val last = lm.findViewByPosition(totalItemCount() - 1) ?: return delta
            val maxDown = max(0, lm.getDecoratedBottom(last) - (recyclerView.height - recyclerView.paddingBottom))
            return min(delta, maxDown)
        }
        if (delta < 0) {
            val first = lm.findViewByPosition(0) ?: return delta
            val maxUp = min(0, lm.getDecoratedTop(first) - recyclerView.paddingTop)
            return max(delta, maxUp)
        }
        return 0
    }

    /**
     * 行き先の行が配置されていないときに、どちらへ向かえば行き先に着くか (正で下)。
     *
     * 範囲が配置済みの行より下にあれば下、上にあれば上。範囲が画面にかかっているときは、最初の行が
     * 配置済みなら (最後の行が下にはみ出しているので) 下、最初の行が上にはみ出しているなら上。どちらの
     * 場合も、位置の種類 (上端・中央・下端) と範囲の高さによらず最終位置は同じ向きにある。
     */
    private fun searchDirection(plan: ScrollPlan): Int {
        when (plan.alignment) {
            Alignment.ContentStart -> return -1
            Alignment.ContentEnd -> return 1
            else -> Unit
        }
        var minPosition = Int.MAX_VALUE
        var maxPosition = Int.MIN_VALUE
        for (i in 0 until recyclerView.childCount) {
            val position = recyclerView.getChildLayoutPosition(recyclerView.getChildAt(i))
            if (position == RecyclerView.NO_POSITION) continue
            minPosition = min(minPosition, position)
            maxPosition = max(maxPosition, position)
        }
        if (minPosition == Int.MAX_VALUE) return 1
        return when {
            plan.lastPosition < minPosition -> -1
            plan.firstPosition > maxPosition -> 1
            layoutManager.findViewByPosition(plan.firstPosition) != null -> 1
            else -> -1
        }
    }

    // MARK: - 位置を控える・戻す

    /**
     * 表示範囲の上端にかかる最初の要素と、上端からのずれを控える。
     *
     * 控えた位置の復元がまだ実行されていないときは、その控えを返す (画面が戻り切る前に再び控えても
     * 位置を失わないため)。行が 1 つも配置されていなければ `null`。
     */
    fun captureAnchor(): KsScrollAnchor? {
        pendingRestoreAnchor()?.let { return it }
        if (recyclerView.adapter == null) return null
        val top = recyclerView.paddingTop
        var best: View? = null
        var bestPosition = RecyclerView.NO_POSITION
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            if (child.bottom <= top) continue
            val position = recyclerView.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION) continue
            if (best == null || child.top < best.top) {
                best = child
                bestPosition = position
            }
        }
        if (best == null) return null
        val element = elementAt(bestPosition) ?: return null
        return KsScrollAnchor(element, top - best.top)
    }

    /** 未実行の位置の復元のうち最後のもの。 */
    private fun pendingRestoreAnchor(): KsScrollAnchor? {
        val correction = pendingCorrection
        val queued = (readyEntries + incomingEntries).filterIsInstance<KsScrollQueueEntry.Restore>().lastOrNull()
        if (queued != null) return queued.anchor
        if (correction != null && correction.alignment is Alignment.Anchor) {
            return elementAt(correction.firstPosition)?.let { KsScrollAnchor(it, correction.alignment.offsetFromTop) }
        }
        return null
    }

    /** ConcatAdapter 全体での行位置を、控える要素へ対応づける。 */
    private fun elementAt(position: Int): KsScrollAnchor.Element? {
        val headerCount = headerAdapter.itemCount
        val mainCount = mainListAdapter.itemCount
        if (position < headerCount) return KsScrollAnchor.Element.RootHeader
        if (position >= headerCount + mainCount) {
            return if (footerAdapter.itemCount > 0) KsScrollAnchor.Element.RootFooter else null
        }
        return when (val item = mainListAdapter.currentList.getOrNull(position - headerCount)) {
            is CellListItem.CellRow -> KsScrollAnchor.Element.Cell(item.cell.id)
            is CellListItem.SectionHeader -> KsScrollAnchor.Element.SectionHeader(item.sectionId)
            is CellListItem.SectionFooter -> KsScrollAnchor.Element.SectionFooter(item.sectionId)
            null -> null
        }
    }

    /** 控えた要素の、現在の ConcatAdapter 全体での行位置。表示に無ければ `null`。 */
    private fun positionOf(element: KsScrollAnchor.Element): Int? {
        val headerCount = headerAdapter.itemCount
        val list = mainListAdapter.currentList
        val index = when (element) {
            KsScrollAnchor.Element.RootHeader -> return if (headerCount > 0) 0 else null
            KsScrollAnchor.Element.RootFooter ->
                return if (footerAdapter.itemCount > 0) headerCount + mainListAdapter.itemCount else null
            is KsScrollAnchor.Element.Cell ->
                list.indexOfFirst { it is CellListItem.CellRow && it.cell.id == element.id }
            is KsScrollAnchor.Element.SectionHeader ->
                list.indexOfFirst { it is CellListItem.SectionHeader && it.sectionId == element.sectionId }
            is KsScrollAnchor.Element.SectionFooter ->
                list.indexOfFirst { it is CellListItem.SectionFooter && it.sectionId == element.sectionId }
        }
        return if (index < 0) null else headerCount + index
    }

    // MARK: - 型

    /** 行き先の合わせ方。 */
    private sealed class Alignment {
        /** 行の範囲を [position] に合わせる。表示範囲より高い範囲は上端に合わせる。 */
        data class Range(val position: KsScrollPosition) : Alignment()

        /** 内容の最上端 (先頭の行の装飾を含む上端) を表示範囲の上端に合わせる。 */
        data object ContentStart : Alignment()

        /** 内容の最下端 (末尾の行の装飾を含む下端) を表示範囲の下端に合わせる。 */
        data object ContentEnd : Alignment()

        /** 行の上端が表示範囲の上端から [offsetFromTop] だけ上にある位置へ合わせる。 */
        data class Anchor(val offsetFromTop: Int) : Alignment()
    }

    /**
     * 行き先。行位置は ConcatAdapter 全体での位置。
     *
     * @param firstPosition 範囲の最初の行
     * @param lastPosition 範囲の最後の行
     */
    private class ScrollPlan(
        val firstPosition: Int,
        val lastPosition: Int,
        val alignment: Alignment,
        val animated: Boolean,
    ) {
        /** アニメーションしない送りで、行を配置させるために先頭へ置く行。 */
        val placementPosition: Int
            get() = if (alignment == Alignment.ContentEnd) lastPosition else firstPosition
    }

    /**
     * 行き先の行が配置されていない命令のアニメーション。
     *
     * 対象へ向かって一方向に進み、進む先の行が配置されるたびに行き先を解き直す。解けた時点で残りの
     * 距離を減速しながら進んで止まる。行き先が進行方向と逆にあると解けた場合も戻らず、その場で止まる
     * (行き過ぎて戻る動きを作らない)。
     */
    private inner class AlignmentSmoothScroller(
        private val plan: ScrollPlan,
        private val direction: Int,
    ) : LinearSmoothScroller(recyclerView.context) {

        override fun onSeekTargetStep(dx: Int, dy: Int, state: RecyclerView.State, action: Action) {
            if (tryLand(action)) return
            super.onSeekTargetStep(dx, dy, state, action)
        }

        override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
            if (tryLand(action)) return
            // 目標の行が配置されても行き先が解けない (範囲の最初の行が回収された) ときは、その場で止める。
            action.update(0, 0, 1, null)
        }

        private fun tryLand(action: Action): Boolean {
            val delta = resolveDelta(plan) ?: return false
            val clamped = clampToScrollableRange(delta)
            val forward = if (direction > 0) max(0, clamped) else min(0, clamped)
            if (forward == 0) {
                // 進行中のシーク用のスクロールを、動かない送りで置き換えて止める。
                action.update(0, 0, 1, null)
            } else {
                action.update(0, forward, calculateTimeForDeceleration(abs(forward)), mDecelerateInterpolator)
            }
            stop()
            return true
        }
    }

    private companion object {
        private const val LOG_TAG = "KsSettingsView"

        /** アニメーションしない送りで、最終位置を求め直して詰める回数の上限。 */
        private const val MAX_SETTLE_PASSES = 3
    }
}
