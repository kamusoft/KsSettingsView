package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * `minimumHeight` を `ConstraintLayout` の measure 結果に確実に反映する ConstraintLayout サブクラス。
 *
 * 標準の `ConstraintLayout` は、以下の条件が重なると `setMinimumHeight()` を尊重しない測定結果を
 * 返すケースがあることが知られている（実機 / 一部 API レベル / 一部端末）：
 *   - `layoutParams.height = WRAP_CONTENT`
 *   - 親（`RecyclerView` + `LinearLayoutManager`）から `heightSpec = UNSPECIFIED` または `AT_MOST`
 *     で measure される
 *   - 内部の制約解決が「子要素の合計高さ」だけを返し、`getSuggestedMinimumHeight()` を考慮しない
 *
 * 参考:
 *   - https://blog.ostebaronen.dk/2018/12/common-constraintlayout-mistakes.html
 *   - https://github.com/androidx/constraintlayout/issues/855
 *   - https://issuetracker.google.com/issues/136492486
 *
 * オリジナル `AiForms.Maui.SettingsView` の Android 実装
 * (`SettingsViewRecyclerAdapter.cs:483-487`) も、ConstraintLayout の同問題を回避するため
 * `holder.Body` と `nativeCell` の **両方** に `SetMinimumHeight` を呼んでいた。
 * 本クラスはその意図を Kotlin 側で `onMeasure` での再 measure という形で実現する。
 *
 * 動作:
 *   1. まず `super.onMeasure(...)` を呼んで標準の制約解決を行う
 *   2. `measuredHeight < minimumHeight` のとき、`heightMeasureSpec` を
 *      `MeasureSpec.EXACTLY(minimumHeight)` に書き換えて **再度** `super.onMeasure` を呼ぶ。
 *      これにより内部の制約解決が `minimumHeight` を高さとして再実行され、子 View の縦位置
 *      （chain bias / TOP+BOTTOM の CenterVertical 等）も新しい高さに合わせて再配置される。
 *   3. `measuredHeight >= minimumHeight` のとき（description 2 行等で内容が 60dp を超える Cell）は
 *      intrinsic な測定結果をそのまま採用するため、Auto 高さの上方向伸縮を阻害しない。
 */
internal class MinHeightConstraintLayout(context: Context) : ConstraintLayout(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val minH = minimumHeight
        if (minH > 0 && measuredHeight < minH) {
            // heightSpec を EXACTLY(minH) に差し替えて再度 super.onMeasure を呼ぶことで、
            // 内部制約解決が minH 高さで再実行され、子 View の縦中央配置も適切に揃う。
            // setMeasuredDimension だけだと measured 値は伸びるが子のレイアウトは元の小さい
            // 高さに基づくため、accessoryHolder の CenterVertical 等が崩れる。
            val newHeightSpec = View.MeasureSpec.makeMeasureSpec(minH, View.MeasureSpec.EXACTLY)
            super.onMeasure(widthMeasureSpec, newHeightSpec)
        }
    }
}
