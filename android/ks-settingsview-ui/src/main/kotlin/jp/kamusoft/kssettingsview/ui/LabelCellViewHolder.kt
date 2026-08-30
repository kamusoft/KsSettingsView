package jp.kamusoft.kssettingsview.ui

import android.view.View
import android.view.ViewGroup

/**
 * [LabelCell] 描画用 ViewHolder。
 *
 * [CellBaseViews] を保持し、`bind` 内で [applyCellBaseLayout] を呼び出して共通フィールド
 * （`title` / `description` / `valueText` / `icon` / `hintText`）を描画する。
 * `LabelCell` は trailing コントロールを持たないため、`accessoryHolder` は空のまま使用する
 * （共通行の描画は [applyCellBaseLayout] に委ねる。core/ADR-0011）。
 */
internal open class LabelCellViewHolder(
    internal val views: CellBaseViews,
) : CellViewHolder<LabelCell>(views.root) {

    override fun bind(cell: LabelCell, theme: Theme) {
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
        // isEnabled = false 時はタップ無効化（LabelCell は本来クリックリスナー無しだが
        // applyCellBackground で isClickable = true になっているため、ここで打ち消す）
        views.root.isClickable = cell.isEnabled
        // 実効行高さの反映
        applyEffectiveHeight(views.root, effective)
    }

    override fun reset() {
        views.titleView.text = null
        views.descriptionView.text = null
        views.valueTextView.text = null
        views.hintTextView.text = null
        views.iconView.setImageDrawable(null)
        views.iconView.visibility = View.GONE
    }

    companion object {
        fun create(parent: ViewGroup): LabelCellViewHolder {
            val views = buildCellBaseViews(parent)
            return LabelCellViewHolder(views = views)
        }
    }
}
