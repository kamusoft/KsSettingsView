package jp.kamusoft.kssettingsview.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * `ItemDecoration` の描画呼び出しを順序付きで記録する Canvas。
 *
 * `Paint` は描画側で使い回されるため、記録時に色・塗り方・線幅を写し取る。
 * 記録順がそのまま重なり順（後の呼び出しが前面）になるので、合成契約の検証にも使う。
 */
internal class DecorationCanvasRecorder : Canvas() {

    /** 記録した描画呼び出し。 */
    sealed interface Op {
        /** `drawRect` の記録。 */
        data class Rect(
            val left: Float,
            val top: Float,
            val right: Float,
            val bottom: Float,
            val color: Int,
            val style: Paint.Style,
        ) : Op

        /** `drawRoundRect` の記録。 */
        data class RoundRect(
            val left: Float,
            val top: Float,
            val right: Float,
            val bottom: Float,
            val radius: Float,
            val color: Int,
            val style: Paint.Style,
            val strokeWidth: Float,
        ) : Op

        /** `clipOutPath` の記録（クリップ形状そのものは扱わず、実施の有無だけを見る）。 */
        object ClipOut : Op
    }

    val ops: MutableList<Op> = mutableListOf()

    /** 記録された `drawRect` だけを順に返す。 */
    val rects: List<Op.Rect> get() = ops.filterIsInstance<Op.Rect>()

    /** 記録された `drawRoundRect` だけを順に返す。 */
    val roundRects: List<Op.RoundRect> get() = ops.filterIsInstance<Op.RoundRect>()

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        ops += Op.Rect(left, top, right, bottom, paint.color, paint.style)
    }

    override fun drawRect(rect: RectF, paint: Paint) {
        ops += Op.Rect(rect.left, rect.top, rect.right, rect.bottom, paint.color, paint.style)
    }

    override fun drawRoundRect(rect: RectF, rx: Float, ry: Float, paint: Paint) {
        ops += Op.RoundRect(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom,
            radius = rx,
            color = paint.color,
            style = paint.style,
            strokeWidth = paint.strokeWidth,
        )
    }

    override fun clipOutPath(path: Path): Boolean {
        ops += Op.ClipOut
        return true
    }

    override fun save(): Int = 1

    override fun restore() = Unit

    override fun restoreToCount(saveCount: Int) = Unit
}
