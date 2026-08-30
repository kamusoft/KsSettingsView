package jp.kamusoft.kssettingsview.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.annotation.ColorInt

/**
 * Section 単位の角丸背景を描画するヘルパ。
 *
 * `ModernSectionDecoration` の `onDraw` から呼ばれる軽量ヘルパ。
 * 通常の `Drawable` ではなく `Canvas` 直描画用の関数群として提供する
 * （ItemDecoration での描画は座標計算済みのため、Drawable のステートフルな draw より直接的）。
 */
internal object RoundedSectionBackgroundDrawable {

    /**
     * 指定矩形に角丸背景を描画する。
     *
     * @param canvas 描画先 Canvas
     * @param rect 描画矩形（外側マージンは呼び出し側で考慮済み）
     * @param cornerRadiusPx 角丸半径（px）
     * @param backgroundColor 背景色（@ColorInt）
     * @param paint 再利用する Paint インスタンス（呼び出し側で確保）
     */
    fun draw(
        canvas: Canvas,
        rect: RectF,
        cornerRadiusPx: Float,
        @ColorInt backgroundColor: Int,
        paint: Paint,
    ) {
        paint.color = backgroundColor
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
    }
}
