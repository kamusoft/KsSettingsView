package jp.kamusoft.kssettingsview.ui

import android.content.Context
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import jp.kamusoft.kssettingsview.core.Cell

/**
 * `ComposeView` を内包する [CellViewHolder] の基盤クラス。
 *
 * 派生 ViewHolder（[CustomCell] を描画するもの等）が `ComposeView` を利用する際、破棄戦略として
 * `DisposeOnDetachedFromWindowOrReleasedFromPool` を明示指定する（android/ADR-0015）。行が画面外へ
 * 出て `RecyclerView` のリサイクル機構に取り込まれただけでは Composition を破棄せず、プールからの
 * 放逐や pooling container（`RecyclerView` 自身）の解放通知で破棄する。行の出入りごとに Composition
 * と content ツリーを作り直さないため、リサイクルの利得が器だけでなく中身にも及ぶ。
 *
 * @param T 描画対象の Cell 型
 * @param context ComposeView 構築に必要な Context
 */
internal abstract class ComposeCellViewHolder<T : Cell>(
    context: Context,
) : CellViewHolder<T>(buildComposeView(context)) {

    /**
     * 内部 ComposeView。`setContent { ... }` 等で派生クラスが描画する。
     */
    protected val composeView: ComposeView = itemView as ComposeView

    companion object {
        /**
         * `ComposeView` を生成し、pool-aware な破棄戦略を必ず設定するヘルパ。
         *
         * `super` 呼び出し以前に [itemView] へ渡せるよう、companion 関数として外出ししている。
         */
        private fun buildComposeView(context: Context): ComposeView {
            val view = ComposeView(context)
            // プール滞在中は Composition を保持し、プールからの放逐と pooling container の解放通知で
            // 破棄する（android/ADR-0015）。将来変わり得る既定値には委ねず明示指定する。
            view.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool,
            )
            return view
        }
    }
}
