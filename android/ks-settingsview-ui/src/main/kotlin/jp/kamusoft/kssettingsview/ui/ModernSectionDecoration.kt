package jp.kamusoft.kssettingsview.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.LayoutDirection
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 最新 OS 設定画面風の角丸グルーピング描画。
 *
 * - `getItemOffsets`: Section 単位の上下外側マージンと、Section 単位の行 (Section Header /
 *   Cell / Section Footer) への左右 inset を確保
 * - `onDraw`: Section 内 Cell 範囲の角丸背景を描画（Cell の下）
 * - `onDrawOver`: 角丸からはみ出した Cell 背景の被覆、Section 内の中間 separator、
 *   箱のボーダーを描画（Cell の上）
 *
 * 箱が覆うのは Section 内の **Cell 行だけ**であり、Section Header / Footer 行と
 * Root Header / Footer は箱に含めない。Section Header / Footer 行は箱の外側にありながら
 * 左右 inset は共有するため、文字の水平位置が箱の中の Cell と揃う。
 *
 * 寸法は [SectionBoxMetrics] が [Theme] の Section 装飾 4 属性から描画のたびに解決する。
 *
 * Section 内 Cell の上下端は `bindingAdapterPosition` 経由で前後の `CellListItem.sectionId` を
 * 比較する `O(1)` 判定で決定する。
 *
 * [KsSettingsViewStyle.Modern] のときに適用する `ItemDecoration`。
 * フラットな見た目は [ClassicSectionDecoration] が担当する。
 *
 * @param theme 装飾値・色を取得するための Theme（実行中に変更される可能性があるため `var`）
 */
internal class ModernSectionDecoration(
    var theme: Theme,
) : RecyclerView.ItemDecoration() {

    private val paint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val tmpRect: RectF = RectF()

    private val tmpPath: Path = Path()

    /**
     * 画面内に描く 1 Section 分の箱の矩形。
     *
     * Section 端が viewport の外にあるときは、その側を画面外へ延長した矩形になる。
     * 角丸・端ボーダーは矩形の端にだけ現れるため、延長した側は画面内に現れない。
     */
    private data class SectionBox(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    )

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val metrics = resolveMetrics(parent)
        val (top, bottom) = SectionUnitMargin.verticalOffsets(
            view = view,
            parent = parent,
            marginTopPx = metrics.marginTopPx.roundToInt(),
            marginBottomPx = metrics.marginBottomPx.roundToInt(),
        )

        // 左右 inset は Section 単位（Section Header・Cell・Section Footer）の全行に入れる。
        // Header / Footer の文字は箱の中の Cell と水平で揃う（箱が覆うのは Cell 行だけ）。
        // Root Header / Footer は Section 装飾の対象外なので inset しない。
        val isSectionRow = sectionRowItem(view, parent) != null
        val left = if (isSectionRow) metrics.marginLeftPx.roundToInt() else 0
        val right = if (isSectionRow) metrics.marginRightPx.roundToInt() else 0

        outRect.set(left, top, right, bottom)
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val metrics = resolveMetrics(parent)
        val boxes = collectSectionBoxes(parent, metrics)
        if (boxes.isEmpty()) return

        val backgroundColor = theme.cellBackgroundColor.toArgb()
        for (box in boxes.values) {
            tmpRect.set(box.left, box.top, box.right, box.bottom)
            RoundedSectionBackgroundDrawable.draw(
                canvas = c,
                rect = tmpRect,
                cornerRadiusPx = metrics.clampedCornerRadius(tmpRect.width(), tmpRect.height()),
                backgroundColor = backgroundColor,
                paint = paint,
            )
        }
    }

    /**
     * Cell の上に重ねる装飾を描く。
     *
     * 描画順は「角の被覆 → 中間 separator → ボーダー」で、ボーダーが最前面に来る。
     * Cell は自身の背景（`RippleDrawable`）で行全体を不透明に塗るため、角の被覆と separator を
     * `onDraw` で描くと Cell 背景に上書きされて消える。いずれも children 描画後に走る
     * `onDrawOver` で描く必要がある。
     */
    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val metrics = resolveMetrics(parent)
        val boxes = collectSectionBoxes(parent, metrics)
        if (boxes.isEmpty()) return

        // Cell 背景・押下背景が角丸の外へはみ出した分を下地色で覆い、箱の形状へ収める。
        val canvasColor = theme.backgroundColor.toArgb()
        for (box in boxes.values) {
            clipCornersOutside(c, box, metrics, canvasColor)
        }

        drawIntermediateSeparators(c, parent, metrics, boxes)

        if (metrics.borderWidthPx > 0.0f) {
            for (box in boxes.values) {
                drawBorder(c, box, metrics)
            }
        }
    }

    // MARK: - 描画パーツ

    /**
     * 箱の外接矩形のうち角丸の外側にあたる部分を下地色で塗り、Cell 背景の角を落とす。
     *
     * 画面外へ延長した側は角丸そのものが画面外にあるため、この被覆も画面内には現れない。
     */
    private fun clipCornersOutside(
        c: Canvas,
        box: SectionBox,
        metrics: SectionBoxMetrics,
        canvasColor: Int,
    ) {
        tmpRect.set(box.left, box.top, box.right, box.bottom)
        val radius = metrics.clampedCornerRadius(tmpRect.width(), tmpRect.height())
        if (radius <= 0.0f) return

        tmpPath.reset()
        tmpPath.addRoundRect(tmpRect, radius, radius, Path.Direction.CW)

        val saveCount = c.save()
        c.clipOutPath(tmpPath)
        paint.color = canvasColor
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        paint.isAntiAlias = true
        c.drawRect(tmpRect, paint)
        c.restoreToCount(saveCount)
    }

    /**
     * Section 内の中間 separator を描く。
     *
     * 箱の上下端（Section 先頭 Cell の上端 / 末尾 Cell の下端）には描かない。縁が区切りを兼ねる。
     * 左右とも箱の内側の端（ボーダーがあればその内側）から同量だけ inset し、箱が罫線で
     * 分断されて見えないようにする。太さは Classic と同じく 1 物理 pixel 固定。
     */
    private fun drawIntermediateSeparators(
        c: Canvas,
        parent: RecyclerView,
        metrics: SectionBoxMetrics,
        boxes: Map<String, SectionBox>,
    ) {
        val density = parent.resources.displayMetrics.density
        val inset = SEPARATOR_INSET_DP * density + metrics.borderWidthPx
        val separatorColor = theme.separatorColor.toArgb()

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i) ?: continue
            val holder = parent.getChildViewHolder(child) ?: continue
            val bindingAdapter = holder.bindingAdapter as? KsSettingsListAdapter ?: continue
            val position = holder.bindingAdapterPosition
            if (position < 0) continue
            val list = bindingAdapter.currentList
            val item = list.getOrNull(position) as? CellListItem.CellRow ?: continue

            // Section 末尾 Cell の下端は箱の縁が担うため描かない。
            val next = list.getOrNull(position + 1)
            val isSectionLastCell = next !is CellListItem.CellRow || next.sectionId != item.sectionId
            if (isSectionLastCell) continue

            val box = boxes[item.sectionId] ?: continue
            val left = box.left + inset
            val right = box.right - inset
            if (right <= left) continue

            val bottom = child.bottom.toFloat() + child.translationY

            paint.style = Paint.Style.FILL
            paint.isAntiAlias = false
            paint.color = separatorColor
            paint.alpha = (child.alpha * 255.0f).coerceIn(0.0f, 255.0f).toInt()
            c.drawRect(left, bottom - SEPARATOR_THICKNESS_PX, right, bottom, paint)
        }
        paint.isAntiAlias = true
        paint.alpha = 255
    }

    /** 箱のボーダーを最前面に描く。線幅の半分だけ内側へ寄せ、線が箱の外へはみ出さないようにする。 */
    private fun drawBorder(c: Canvas, box: SectionBox, metrics: SectionBoxMetrics) {
        val half = metrics.borderWidthPx / 2.0f
        tmpRect.set(box.left, box.top, box.right, box.bottom)
        val radius = metrics.clampedCornerRadius(tmpRect.width(), tmpRect.height())
        tmpRect.set(box.left + half, box.top + half, box.right - half, box.bottom - half)
        if (tmpRect.width() <= 0.0f || tmpRect.height() <= 0.0f) return

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = metrics.borderWidthPx
        paint.isAntiAlias = true
        paint.color = metrics.borderColor
        val strokeRadius = max(0.0f, radius - half)
        c.drawRoundRect(tmpRect, strokeRadius, strokeRadius, paint)
        paint.style = Paint.Style.FILL
    }

    // MARK: - 幾何計算

    /** 現在の [Theme] と RecyclerView の表示条件から Section 装飾の実効値を解決する。 */
    private fun resolveMetrics(parent: RecyclerView): SectionBoxMetrics = SectionBoxMetrics.resolve(
        theme = theme,
        style = KsSettingsViewStyle.Modern,
        density = parent.resources.displayMetrics.density,
        layoutDirection = if (parent.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        },
    )

    /**
     * 画面内の Cell 行から Section ごとの箱を組み立てる。
     *
     * 集計対象は `CellListItem.CellRow` の child だけで、Section Header / Footer 行と
     * Root Header / Footer 行は含めない。可視 Cell を 1 つも持たない Section の箱は作らない。
     *
     * Section の先頭 / 末尾 Cell が画面内に実在しないときは、その側の端を viewport の外へ
     * 延長する。画面内の最初 / 最後の可視 Cell を Section 端と誤認して角丸や端ボーダーを
     * 描かないようにするためである。
     */
    private fun collectSectionBoxes(
        parent: RecyclerView,
        metrics: SectionBoxMetrics,
    ): Map<String, SectionBox> {
        val concatAdapter = parent.adapter as? ConcatAdapter ?: return emptyMap()
        if (concatAdapter.adapters.none { it is KsSettingsListAdapter }) return emptyMap()

        val tops = LinkedHashMap<String, Float>()
        val bottoms = HashMap<String, Float>()
        val hasRealTop = HashSet<String>()
        val hasRealBottom = HashSet<String>()

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i) ?: continue
            val holder = parent.getChildViewHolder(child) ?: continue
            val bindingAdapter = holder.bindingAdapter as? KsSettingsListAdapter ?: continue
            val position = holder.bindingAdapterPosition
            if (position < 0) continue
            val list = bindingAdapter.currentList
            val item = list.getOrNull(position) as? CellListItem.CellRow ?: continue

            // ItemAnimator のアニメーション中は child.translationY が補間されるため、
            // 静的レイアウト座標に translationY を加味した見た目の座標で箱を組み立てる。
            val sectionId = item.sectionId
            val translationY = child.translationY
            val top = child.top.toFloat() + translationY
            val bottom = child.bottom.toFloat() + translationY

            tops[sectionId] = tops[sectionId]?.coerceAtMost(top) ?: top
            bottoms[sectionId] = bottoms[sectionId]?.coerceAtLeast(bottom) ?: bottom

            val previous = list.getOrNull(position - 1)
            val next = list.getOrNull(position + 1)
            if (previous !is CellListItem.CellRow || previous.sectionId != sectionId) {
                hasRealTop += sectionId
            }
            if (next !is CellListItem.CellRow || next.sectionId != sectionId) {
                hasRealBottom += sectionId
            }
        }
        if (tops.isEmpty()) return emptyMap()

        val left = parent.paddingLeft + metrics.marginLeftPx
        val right = parent.width - parent.paddingRight - metrics.marginRightPx
        // 画面外へ逃がす量。角丸とボーダーが viewport に掛からない距離を取る。
        val overhang = metrics.cornerRadiusPx + metrics.borderWidthPx + 1.0f

        val boxes = LinkedHashMap<String, SectionBox>(tops.size)
        for ((sectionId, rawTop) in tops) {
            val rawBottom = bottoms[sectionId] ?: continue
            val isTopEdgeReal = sectionId in hasRealTop
            val isBottomEdgeReal = sectionId in hasRealBottom
            val top = if (isTopEdgeReal) rawTop else min(rawTop, 0.0f) - overhang
            val bottom = if (isBottomEdgeReal) {
                rawBottom
            } else {
                max(rawBottom, parent.height.toFloat()) + overhang
            }
            boxes[sectionId] = SectionBox(left = left, top = top, right = right, bottom = bottom)
        }
        return boxes
    }

    /**
     * 対象 child が Section 単位の行（Section Header / Cell / Section Footer）なら、その項目を返す。
     * Root Header / Footer など main list 以外の行では `null`。
     */
    private fun sectionRowItem(view: View, parent: RecyclerView): CellListItem? {
        val holder = parent.getChildViewHolder(view) ?: return null
        val bindingAdapter = holder.bindingAdapter as? KsSettingsListAdapter ?: return null
        val position = holder.bindingAdapterPosition
        if (position < 0) return null
        return bindingAdapter.currentList.getOrNull(position)
    }

    companion object {
        /**
         * 中間 separator の inset（dp）。箱の内側の端からこの量だけ左右へ寄せる。
         * icon の有無で変えない（Classic の中間 separator と同じ規則）。
         */
        internal const val SEPARATOR_INSET_DP: Float = 16.0f

        /** separator の太さ。Classic と同じく 1 物理 pixel 固定（dp 換算しない）。 */
        internal const val SEPARATOR_THICKNESS_PX: Float = 1.0f
    }
}
