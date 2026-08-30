package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import androidx.annotation.ColorInt

/**
 * チェックマーク（2 本線）を Canvas に直接描画するカスタム View。
 *
 * `AiForms.Maui.SettingsView` の `SimpleCheck.cs`（`Native/Android/Cells/SimpleCheck.cs`）の
 * `OnDraw` と同じ描画ロジックを持つ。標準 RadioButton / CheckBox / `TextView "✓"` ではなく、
 * オリジナルと同じ「2 本の線で手描きしたチェックマーク」で選択状態を表現する。
 *
 * - [isChecked]（オリジナルの `Selected`）が `true` のときのみチェックマークを描画する。
 * - [color]（オリジナルの `Color`）で線色を指定する。
 * - 線幅は 2dp（オリジナル `StrokeWidth = ToPixels(2)`）、`AntiAlias` 有効。
 * - 線の座標は View 自身の寸法比率で算出する（オリジナルの canvas 比率と同義。
 *   ソフトウェア Canvas への一括描画時は canvas が View より大きくなり得るため View 寸法を使う）:
 *   - 1 本目: (22%, 52%) → (38%, 68%)
 *   - 2 本目: (36%, 66%) → (74%, 28%)
 *
 * 行内の選択印を描く各所（[RadioCellViewHolder] / [SimpleCheckCellViewHolder] /
 * [PickerSelectionSheet]）で共有する単一のカスタム View として実装する
 * （チェックマークの見た目がいずれも同一のため）。
 *
 * # `isEnabled` による disabled 表現
 * `View` 標準の [isEnabled] を活用し、`false` のときは [color] のアルファを下げて
 * 薄くチェックマークを描画する。これにより呼び出し側（[RadioCellViewHolder] /
 * [SimpleCheckCellViewHolder]）が `checkView.alpha = 0.5f` を直接書く必要がなくなり、
 * 「Cell 全体ではなく内部チェック表示の disabled 表現」をコード上で明確化する
 * （Cell 全体を半透明化する方式は採らない）。
 */
internal class KsSimpleCheckView(context: Context) : View(context) {

    private val paint = Paint()

    /** チェックマークの線色（ARGB Int）。 */
    @ColorInt
    var color: Int = android.graphics.Color.BLACK
        set(value) {
            field = value
            invalidate()
        }

    /** チェック状態。オリジナルの `Selected` 相当。変更時に再描画する。 */
    var isChecked: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    init {
        // View を直接継承する場合、描画させるには WILL_NOT_DRAW を解除する必要がある
        // （オリジナルの `SetWillNotDraw(false)` に対応）。
        setWillNotDraw(false)
    }

    /** [setEnabled] 反映時に再描画して disabled 表現を更新する。 */
    override fun setEnabled(enabled: Boolean) {
        val changed = enabled != isEnabled
        super.setEnabled(enabled)
        if (changed) {
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isChecked) {
            return
        }

        paint.style = Paint.Style.STROKE
        // disabled 時はチェックマーク色のアルファを下げて薄く描画する
        // （refine-basic-cells-style Suggestion-1: 内部 View に disabled 表現を移譲）。
        paint.color = if (isEnabled) color else applyDisabledAlpha(color)
        // 2dp を px に換算（オリジナル `_context.ToPixels(2)` 相当）
        paint.strokeWidth = 2f * resources.displayMetrics.density
        paint.isAntiAlias = true

        // canvas.width/height はソフトウェア Canvas への一括描画時に View より大きくなり得るため
        // View 自身の寸法を使う
        val w = width.toFloat()
        val h = height.toFloat()

        // 1 本目: (22%, 52%) → (38%, 68%)
        canvas.drawLine(
            0.22f * w, 0.52f * h,
            0.38f * w, 0.68f * h,
            paint,
        )

        // 2 本目: (36%, 66%) → (74%, 28%)
        canvas.drawLine(
            0.36f * w, 0.66f * h,
            0.74f * w, 0.28f * h,
            paint,
        )
    }

    /**
     * 既存色のアルファを disabled 用に低下させる。元色のアルファを 50% に乗算する。
     * （RGB 成分は維持。`Color.argb` で再構築する。）
     */
    @ColorInt
    private fun applyDisabledAlpha(@ColorInt baseColor: Int): Int {
        val originalAlpha = (baseColor ushr 24) and 0xFF
        val newAlpha = (originalAlpha * DISABLED_ALPHA_FACTOR).toInt().coerceIn(0, 0xFF)
        val r = (baseColor ushr 16) and 0xFF
        val g = (baseColor ushr 8) and 0xFF
        val b = baseColor and 0xFF
        return (newAlpha shl 24) or (r shl 16) or (g shl 8) or b
    }

    companion object {
        /** disabled 時に元色のアルファに乗算する係数（0.5 = 半透明相当）。 */
        private const val DISABLED_ALPHA_FACTOR: Float = 0.5f
    }
}
