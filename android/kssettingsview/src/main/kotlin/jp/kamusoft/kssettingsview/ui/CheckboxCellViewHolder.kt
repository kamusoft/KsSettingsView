package jp.kamusoft.kssettingsview.ui

import android.content.res.ColorStateList
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.checkbox.MaterialCheckBox

/**
 * [CheckboxCell] 描画用 ViewHolder。
 *
 * [CellBaseViews] の `accessoryHolder` に [MaterialCheckBox] を配置し、共通行の描画は
 * [applyCellBaseLayout] に委ねる（core/ADR-0011）。
 */
internal class CheckboxCellViewHolder(
    internal val views: CellBaseViews,
    private val checkBox: MaterialCheckBox,
) : CellViewHolder<CheckboxCell>(views.root) {

    private var currentHandler: ((Boolean) -> Unit)? = null

    override fun bind(cell: CheckboxCell, theme: Theme) {
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

        // チェック状態の初期表示。プログラム的 setChecked で古いリスナーが発火しないよう
        // 一度 listener を外してから設定する。
        checkBox.setOnCheckedChangeListener(null)
        checkBox.isChecked = cell.isChecked

        val accent = cell.accentColor?.toArgb() ?: effective.accentColor
        checkBox.buttonTintList = ColorStateList.valueOf(accent)

        currentHandler = cell.onValueChanged
        attachHandler()

        checkBox.isEnabled = cell.isEnabled
        if (cell.isEnabled) {
            views.root.isClickable = true
            views.root.setOnClickListener {
                checkBox.toggle()
            }
        } else {
            views.root.setOnClickListener(null)
            views.root.isClickable = false
        }

        applyEffectiveHeight(views.root, effective)
    }

    private fun attachHandler() {
        val handler = currentHandler
        if (handler != null) {
            checkBox.setOnCheckedChangeListener { _, value ->
                handler.invoke(value)
            }
        } else {
            checkBox.setOnCheckedChangeListener(null)
        }
    }

    override fun reset() {
        views.titleView.text = null
        views.descriptionView.text = null
        currentHandler = null
        checkBox.setOnCheckedChangeListener(null)
        views.root.setOnClickListener(null)
        views.root.isClickable = false
    }

    /** テスト用：セルタップ相当のトグルを行うフック。 */
    internal fun simulateContainerTap() {
        views.root.performClick()
    }

    companion object {
        fun create(parent: ViewGroup): CheckboxCellViewHolder {
            val views = buildCellBaseViews(parent)
            // アクセサリも共通行と同じ Context（同梱テーマ適用済み）から生成する。
            val ctx = views.root.context
            val sizePx = (24 * ctx.resources.displayMetrics.density).toInt()
            val checkBox = MaterialCheckBox(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
                // MaterialCheckBox 既定の内側 padding（タッチ域確保のための padding）を無効化し、
                // 他アクセサリ（Switch / Radio / SimpleCheck）と右端 X 座標を揃える。
                minimumWidth = 0
                minimumHeight = 0
                setPadding(0, 0, 0, 0)
                isFocusable = false
                isClickable = false
                contentDescription = "Checked"
            }
            views.accessoryHolder.addView(checkBox)
            return CheckboxCellViewHolder(views = views, checkBox = checkBox)
        }
    }
}
