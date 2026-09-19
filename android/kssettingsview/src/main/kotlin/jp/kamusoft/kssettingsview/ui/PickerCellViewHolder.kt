package jp.kamusoft.kssettingsview.ui

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import jp.kamusoft.kssettingsview.R

/**
 * [PickerCell] 描画用 ViewHolder。
 *
 * 共通行レイアウト関数経由でレイアウトを構成し、accessory slot に chevron を表示する。
 * `valueText` が `null` のときは [PickerCell.autoValueText] で現在の選択値を自動表示する。
 * Cell タップで [PickerSelectionSheet]（ボトムシートの選択面）を提示する（android/ADR-0005）。
 */
internal class PickerCellViewHolder(
    internal val views: CellBaseViews,
    private val disclosureView: AppCompatImageView,
) : CellViewHolder<PickerCell>(views.root) {

    override fun bind(cell: PickerCell, theme: Theme) {
        val effective = EffectiveStyle.from(theme, cell.style, views.root.context.isKsDarkAppearance())

        // valueText 自動生成。明示指定があればそれを優先する。
        val displayValueText: String? = cell.valueText ?: cell.autoValueText().ifEmpty { null }

        applyCellBaseLayout(
            views = views,
            title = cell.title,
            description = cell.description,
            valueText = displayValueText,
            icon = cell.icon,
            hintText = cell.hintText,
            effective = effective,
            isEnabled = cell.isEnabled,
        )
        applyCellBackground(views.root, effective)

        disclosureView.visibility = View.VISIBLE

        if (cell.isEnabled) {
            views.root.isClickable = true
            views.root.setOnClickListener {
                showPickerSheet(cell, theme, effective)
            }
        } else {
            views.root.setOnClickListener(null)
            views.root.isClickable = false
        }

        applyEffectiveHeight(views.root, effective)
    }

    /**
     * 選択面（ボトムシート）を提示する（android/ADR-0005）。
     *
     * タイトルは `pageTitle ?: title`、候補は `items` の全項目を順序どおり列挙する。
     * 確定 callback は選択面側の確定操作でのみ発火し、モデル値の正規化は行わない。
     *
     * 閉じ切り callback は選択面が閉じたことを知る dismiss 経路で発火する（core/ADR-0034）。確定で
     * 閉じたかどうかは選択面が控えた確定値の有無で判別する。
     */
    private fun showPickerSheet(cell: PickerCell, theme: Theme, effective: EffectiveStyle) {
        val sheet = PickerSelectionSheet(
            hostContext = views.root.context,
            sheetTitle = cell.pageTitle ?: cell.title,
            items = cell.items,
            selectionMode = cell.selectionMode,
            selectedIndex = cell.selectedIndex,
            initialSelectedIndices = cell.selectedIndices,
            maxSelectedNumber = cell.maxSelectedNumber,
            sheetStyle = PickerSheetStyle.from(cell, theme, effective),
            scrollIndicatorVisible = theme.scrollIndicatorVisible,
            onSingleSelected = { index -> cell.onSelectionChanged?.invoke(index) },
            onMultiConfirmed = { indices -> cell.onMultiSelectionChanged?.invoke(indices) },
        )
        sheet.showAnchoredTo(views.root) {
            sheet.completedSelection?.let { index -> cell.onSelectionCompleted?.invoke(index) }
            sheet.completedMultiSelection?.let { indices ->
                cell.onMultiSelectionCompleted?.invoke(indices)
            }
        }
    }

    override fun reset() {
        views.titleView.text = null
        views.descriptionView.text = null
        views.valueTextView.text = null
        views.hintTextView.text = null
        views.iconView.setImageDrawable(null)
        views.iconView.visibility = View.GONE
        // disclosure を一度 GONE に戻して、bind 直前のフラットな初期状態を保つ。
        disclosureView.visibility = View.GONE
        views.root.setOnClickListener(null)
        views.root.isClickable = false
    }

    companion object {
        fun create(parent: ViewGroup): PickerCellViewHolder {
            val views = buildCellBaseViews(parent)
            // アクセサリも共通行と同じ Context（同梱テーマ適用済み）から生成する。
            val ctx = views.root.context
            val w = (18 * ctx.resources.displayMetrics.density).toInt()
            val h = (26 * ctx.resources.displayMetrics.density).toInt()
            val disclosureView = AppCompatImageView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(w, h)
                setImageResource(R.drawable.ic_navigate_next)
                contentDescription = "Disclosure indicator"
            }
            views.accessoryHolder.addView(disclosureView)
            return PickerCellViewHolder(views = views, disclosureView = disclosureView)
        }
    }
}
