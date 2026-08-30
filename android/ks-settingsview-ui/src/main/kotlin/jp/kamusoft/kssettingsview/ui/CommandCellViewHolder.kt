package jp.kamusoft.kssettingsview.ui

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView

/**
 * [CommandCell] 描画用 ViewHolder。
 *
 * [CellBaseViews] の `accessoryHolder` に chevron（[AppCompatImageView]）を配置し、共通行の描画は
 * [applyCellBaseLayout] に委ねる（core/ADR-0011）。chevron は `CommandCell.hideArrow` で隠せる。
 */
internal class CommandCellViewHolder(
    internal val views: CellBaseViews,
    private val disclosureView: AppCompatImageView,
) : CellViewHolder<CommandCell>(views.root) {

    override fun bind(cell: CommandCell, theme: Theme) {
        val effective = EffectiveStyle.from(views.root.context, theme, cell.style)
        applyCellBaseLayout(
            views = views,
            title = cell.title,
            description = cell.description,
            valueText = cell.valueText,
            icon = cell.icon,
            hintText = cell.hintText,
            effective = effective,
            isEnabled = cell.isEnabled,
        )
        applyCellBackground(views.root, effective)

        // Disclosure Indicator の表示
        disclosureView.visibility = if (cell.hideArrow) View.GONE else View.VISIBLE

        // タップ通知（毎回 setOnClickListener を上書きすることで、再利用時の旧クロージャ参照を防ぐ）
        val onTap = cell.onTap
        if (cell.isEnabled && onTap != null) {
            views.root.isClickable = true
            views.root.setOnClickListener { onTap.invoke() }
        } else if (cell.isEnabled) {
            views.root.setOnClickListener(null)
        } else {
            views.root.setOnClickListener(null)
            views.root.isClickable = false
        }

        applyEffectiveHeight(views.root, effective)
    }

    override fun reset() {
        views.titleView.text = null
        views.descriptionView.text = null
        views.valueTextView.text = null
        views.hintTextView.text = null
        views.iconView.setImageDrawable(null)
        views.iconView.visibility = View.GONE
        views.root.setOnClickListener(null)
        views.root.isClickable = false
    }

    companion object {
        fun create(parent: ViewGroup): CommandCellViewHolder {
            val views = buildCellBaseViews(parent)
            // アクセサリも共通行と同じ Context（同梱テーマ適用済み）から生成する。
            val ctx = views.root.context
            // オリジナル ic_navigate_next.xml（18x26dp）相当のサイズで配置する
            val w = (CELL_DISCLOSURE_WIDTH_DP * ctx.resources.displayMetrics.density).toInt()
            val h = (CELL_DISCLOSURE_HEIGHT_DP * ctx.resources.displayMetrics.density).toInt()
            val disclosureView = AppCompatImageView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(w, h)
                setImageResource(R.drawable.ic_navigate_next)
                contentDescription = "Disclosure indicator"
            }
            views.accessoryHolder.addView(disclosureView)
            return CommandCellViewHolder(views = views, disclosureView = disclosureView)
        }
    }
}
