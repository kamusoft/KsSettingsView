package jp.kamusoft.kssettingsview.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `MinHeightConstraintLayout` の `onMeasure` 下限ガード挙動を検証する Robolectric テスト。
 *
 * 標準 ConstraintLayout が `setMinimumHeight()` を尊重しない測定条件でも、
 * 測定後の下限ガードで最低高さが保証されることを確かめる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MinHeightConstraintLayoutTest {

    private val ctx: android.content.Context
        get() = ApplicationProvider.getApplicationContext()

    /**
     * 子要素の合計高さが `minimumHeight` 未満のときは、`measuredHeight` が `minimumHeight` まで
     * 引き上げられることを確認する（オリジナル `AiForms.Maui.SettingsView` 踏襲の下限保証）。
     */
    @Test
    fun `子要素が低いとき measuredHeight は minimumHeight まで引き上げられる`() {
        val layout = MinHeightConstraintLayout(ctx)
        val density = ctx.resources.displayMetrics.density
        val minHeightPx = (60 * density).toInt()
        layout.minimumHeight = minHeightPx
        layout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

        // 子要素: 高さ 10dp 相当の TextView を 1 個だけ持たせる（合計 < minHeight）
        val child = TextView(ctx).apply {
            layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                (10 * density).toInt(),
            )
        }
        layout.addView(child)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(
            (400 * density).toInt(),
            View.MeasureSpec.EXACTLY,
        )
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        layout.measure(widthSpec, heightSpec)

        assertEquals(
            "minimumHeight 未満の子要素なら measuredHeight == minimumHeight に引き上げる",
            minHeightPx,
            layout.measuredHeight,
        )
    }

    /**
     * 子要素の合計高さが `minimumHeight` 以上のときは、intrinsic 測定結果がそのまま採用され
     * 上方向への伸縮を阻害しないことを確認する。
     */
    @Test
    fun `子要素が高いとき measuredHeight は intrinsic な値を採用する`() {
        val layout = MinHeightConstraintLayout(ctx)
        val density = ctx.resources.displayMetrics.density
        val minHeightPx = (60 * density).toInt()
        val childHeightPx = (120 * density).toInt()
        layout.minimumHeight = minHeightPx
        layout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

        // 子要素: 高さ 120dp 相当の TextView。MATCH_CONSTRAINT 制約は使わず単純な固定高さで配置。
        val child = TextView(ctx).apply {
            id = View.generateViewId()
            layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                childHeightPx,
            )
        }
        layout.addView(child)

        // 子要素を parent の TOP / BOTTOM に直接バインドして高さを伝播させる
        val set = androidx.constraintlayout.widget.ConstraintSet()
        set.clone(layout)
        set.connect(
            child.id,
            androidx.constraintlayout.widget.ConstraintSet.TOP,
            androidx.constraintlayout.widget.ConstraintSet.PARENT_ID,
            androidx.constraintlayout.widget.ConstraintSet.TOP,
        )
        set.connect(
            child.id,
            androidx.constraintlayout.widget.ConstraintSet.START,
            androidx.constraintlayout.widget.ConstraintSet.PARENT_ID,
            androidx.constraintlayout.widget.ConstraintSet.START,
        )
        set.applyTo(layout)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(
            (400 * density).toInt(),
            View.MeasureSpec.EXACTLY,
        )
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        layout.measure(widthSpec, heightSpec)

        // intrinsic な測定結果は子要素の 120dp 以上であり、minHeight = 60dp を採用してはならない
        assertTrue(
            "子要素が minimumHeight より高いなら intrinsic 採用（measuredHeight (${layout.measuredHeight}) >= ${childHeightPx})",
            layout.measuredHeight >= childHeightPx,
        )
    }

    /**
     * `minimumHeight = 0` のときは標準 `ConstraintLayout` と同様の挙動になることを確認する
     * （補正処理が悪影響を及ぼさない）。
     */
    @Test
    fun `minimumHeight が 0 のとき measuredHeight は intrinsic 値`() {
        val layout = MinHeightConstraintLayout(ctx)
        val density = ctx.resources.displayMetrics.density
        layout.minimumHeight = 0
        layout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

        val widthSpec = View.MeasureSpec.makeMeasureSpec(
            (400 * density).toInt(),
            View.MeasureSpec.EXACTLY,
        )
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        layout.measure(widthSpec, heightSpec)

        // 子要素無しのため measuredHeight は 0（または super 由来の極小値）。下限ガードが
        // 干渉しないことを確認する。
        assertEquals(
            "minimumHeight = 0 のとき intrinsic 値そのまま",
            0,
            layout.measuredHeight,
        )
    }
}
