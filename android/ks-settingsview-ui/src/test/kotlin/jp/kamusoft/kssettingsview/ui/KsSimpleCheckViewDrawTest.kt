package jp.kamusoft.kssettingsview.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.view.ContextThemeWrapper
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [KsSimpleCheckView] のチェックマークが **View 自身の寸法** を基準に描かれることを固定する。
 *
 * View 階層を View より大きいソフトウェア Canvas（`Bitmap` へのスクリーンショット取得など）へ
 * 一括描画すると、`Canvas` の寸法は View の寸法と一致しない。線の座標を `canvas.width/height`
 * から算出すると、その経路でチェックマークが View の外側へはみ出して消える。
 * ここでは Canvas 側を View より大きく偽装し、記録された線が View の寸法比から導かれることを見る。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsSimpleCheckViewDrawTest {

    private val ctx: android.content.Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            MaterialR.style.Theme_Material3_Light_NoActionBar,
        )

    /**
     * `drawLine` の呼び出しを記録し、Canvas 自身の寸法は View より大きい値を返す Canvas。
     *
     * 実描画は行わず、座標の算出根拠だけを見る。
     */
    private class LineRecordingCanvas(
        private val canvasWidth: Int,
        private val canvasHeight: Int,
    ) : Canvas() {

        /** 記録した線分（開始点・終了点）。 */
        data class Line(val startX: Float, val startY: Float, val stopX: Float, val stopY: Float)

        val lines: MutableList<Line> = mutableListOf()

        override fun getWidth(): Int = canvasWidth

        override fun getHeight(): Int = canvasHeight

        override fun drawLine(
            startX: Float,
            startY: Float,
            stopX: Float,
            stopY: Float,
            paint: Paint,
        ) {
            lines += Line(startX, startY, stopX, stopY)
        }

        override fun save(): Int = 1

        override fun restore() = Unit

        override fun restoreToCount(saveCount: Int) = Unit
    }

    @Test
    fun `チェックマークは Canvas ではなく View 自身の寸法を基準に描かれる`() {
        val view = KsSimpleCheckView(ctx).apply { isChecked = true }
        val size = VIEW_SIZE
        view.measure(
            View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, size, size)
        // View より十分に大きい Canvas（一括描画時の Bitmap Canvas を模す）
        val canvas = LineRecordingCanvas(CANVAS_SIZE, CANVAS_SIZE)

        view.draw(canvas)

        assertEquals("チェックマークは 2 本線で描かれる", 2, canvas.lines.size)
        val w = size.toFloat()
        val h = size.toFloat()
        val first = canvas.lines[0]
        assertEquals(0.22f * w, first.startX, TOLERANCE)
        assertEquals(0.52f * h, first.startY, TOLERANCE)
        assertEquals(0.38f * w, first.stopX, TOLERANCE)
        assertEquals(0.68f * h, first.stopY, TOLERANCE)
        val second = canvas.lines[1]
        assertEquals(0.36f * w, second.startX, TOLERANCE)
        assertEquals(0.66f * h, second.startY, TOLERANCE)
        assertEquals(0.74f * w, second.stopX, TOLERANCE)
        assertEquals(0.28f * h, second.stopY, TOLERANCE)
        // 座標が Canvas 基準なら View の外側へ出るため、寸法内に収まることも併せて見る
        assertTrue(
            "線は View の寸法内に収まる",
            canvas.lines.all { line ->
                listOf(line.startX, line.stopX).all { it in 0f..w } &&
                    listOf(line.startY, line.stopY).all { it in 0f..h }
            },
        )
    }

    @Test
    fun `未チェックでは線を描かない`() {
        val view = KsSimpleCheckView(ctx)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(VIEW_SIZE, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(VIEW_SIZE, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, VIEW_SIZE, VIEW_SIZE)
        val canvas = LineRecordingCanvas(CANVAS_SIZE, CANVAS_SIZE)

        view.draw(canvas)

        assertEquals(0, canvas.lines.size)
    }

    private companion object {
        /** 検証に使う View の一辺（px）。 */
        const val VIEW_SIZE: Int = 40

        /** View より十分に大きい Canvas の一辺（px）。 */
        const val CANVAS_SIZE: Int = 200

        /** 浮動小数比較の許容差。 */
        const val TOLERANCE: Float = 0.01f
    }
}
