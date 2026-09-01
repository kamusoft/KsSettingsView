package jp.kamusoft.kssettingsview.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.graphics.toArgb

/**
 * [RadioCell] 描画用 ViewHolder。
 *
 * [CellBaseViews] の `accessoryHolder` に [KsSimpleCheckView] を配置し、`value == selectedValue` で
 * チェック表示を行う。共通行の描画は [applyCellBaseLayout] に委ねる（core/ADR-0011）。
 */
internal class RadioCellViewHolder(
    internal val views: CellBaseViews,
    private val checkView: KsSimpleCheckView,
) : CellViewHolder<RadioCell>(views.root) {

    override fun bind(cell: RadioCell, theme: Theme) {
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

        // 解決順序: RadioCell.accentColor → CellStyle.accentColor → Theme.cellAccentColor
        val resolvedAccent: Int = cell.accentColor?.toArgb() ?: effective.accentColor
        checkView.color = resolvedAccent
        checkView.isChecked = cell.value == cell.selectedValue
        checkView.isEnabled = cell.isEnabled

        if (cell.isEnabled) {
            val handler = cell.onSelected
            val value = cell.value
            views.root.isClickable = true
            views.root.setOnClickListener {
                if (!checkView.isChecked) {
                    checkView.isChecked = true
                    handler?.invoke(value)
                }
            }
        } else {
            views.root.setOnClickListener(null)
            views.root.isClickable = false
        }

        applyEffectiveHeight(views.root, effective)
    }

    override fun reset() {
        views.titleView.text = null
        views.root.setOnClickListener(null)
        views.root.isClickable = false
        checkView.isChecked = false
        checkView.isEnabled = true
    }

    /** テスト用：セルタップ相当の選択操作を行うフック。 */
    internal fun simulateContainerTap() {
        views.root.performClick()
    }

    companion object {
        fun create(parent: ViewGroup): RadioCellViewHolder {
            val views = buildCellBaseViews(parent)
            // アクセサリも共通行と同じ Context（同梱テーマ適用済み）から生成する。
            val ctx = views.root.context
            val size = (30 * ctx.resources.displayMetrics.density).toInt()
            val check = KsSimpleCheckView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(size, size)
                isFocusable = false
                isClickable = false
                contentDescription = "Selected"
            }
            views.accessoryHolder.addView(check)
            return RadioCellViewHolder(views = views, checkView = check)
        }
    }
}
