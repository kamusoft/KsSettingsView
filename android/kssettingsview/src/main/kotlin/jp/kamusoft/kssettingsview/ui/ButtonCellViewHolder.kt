package jp.kamusoft.kssettingsview.ui

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import jp.kamusoft.kssettingsview.core.CellTitleAlignment

/**
 * [ButtonCell] 描画用 ViewHolder。
 *
 * `icon` / `valueText` / `hintText` がすべて `null` のときはボタンスタイル
 * （[CellBaseViews] の本体行 `contentRow`（= 内部の `titleView`）のみを Cell 全体に広げ、
 * `titleAlignment` 全体反映、青色テキスト）。
 * いずれかが指定された場合は通常レイアウト（[applyCellBaseLayout] 経由 + title 列内 `gravity`
 * で `titleAlignment` を表現）に切り替える。
 */
internal class ButtonCellViewHolder(
    internal val views: CellBaseViews,
) : CellViewHolder<ButtonCell>(views.root) {

    /**
     * ボタンスタイル時に title 表示に使う TextView。`views.titleView` と同一インスタンス。
     * テスト互換のため `buttonTextView` プロパティ名で公開する。
     */
    internal val buttonTextView: android.widget.TextView get() = views.titleView

    /**
     * ボタンスタイル時に切り替える constraint set。本体行 `contentRow` を root 全体に広げる。
     *
     * `titleView` は `contentRow` の子であり root の直接の子ではないため、ConstraintSet は
     * `contentRow` を対象にする。`contentRow` が root 全体に広がり、`titleView` を
     * `0dp + weight = 1`（主行の全幅）に切り替えることで、`gravity` による全体中央揃えが
     * 成立する（android/ADR-0002 / core/ADR-0026）。
     *
     * root は無余白（padding を持たない）ため、行端との距離は共通行と同じ余白
     * （横 [CELL_ROW_HORIZONTAL_MARGIN_DP] / 縦 [CELL_ROW_VERTICAL_MARGIN_DP]）を
     * ここでマージンとして与える。
     *
     * `ConstraintSet.applyTo` は制約だけでなく対象 View の translation も設定値で上書きするため、
     * 本体行の光学中心補正（[CELL_ROW_OPTICAL_CENTER_OFFSET_DP]、android/ADR-0004）を
     * この set にも持たせる。持たせないとボタンスタイルへ切り替えた行だけ補正が 0 に戻る。
     */
    private val buttonStyleSet = ConstraintSet().apply {
        val density = views.root.resources.displayMetrics.density
        val rowMarginH = (CELL_ROW_HORIZONTAL_MARGIN_DP * density).toInt()
        val rowMarginV = (CELL_ROW_VERTICAL_MARGIN_DP * density).toInt()
        setTranslationY(views.contentRow.id, CELL_ROW_OPTICAL_CENTER_OFFSET_DP * density)
        // contentRow を root 全体（行の余白の内側）に広げる
        connect(
            views.contentRow.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            rowMarginH,
        )
        connect(
            views.contentRow.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            rowMarginH,
        )
        connect(
            views.contentRow.id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            rowMarginV,
        )
        connect(
            views.contentRow.id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            rowMarginV,
        )
        constrainWidth(views.contentRow.id, ConstraintSet.MATCH_CONSTRAINT)
        constrainHeight(views.contentRow.id, ConstraintSet.WRAP_CONTENT)
    }

    /** 通常レイアウト用の constraint set（既定の [buildCellBaseViews] が組んだ配置）を退避する。 */
    private val normalLayoutSet = ConstraintSet().apply {
        clone(views.root)
    }

    override fun bind(cell: ButtonCell, theme: Theme) {
        val effective = EffectiveStyle.from(views.root.context, theme, cell.style)
        val hasAux = cell.icon != null || cell.valueText != null || cell.hintText != null

        // ボタン文字色の 4 段階優先順位（SoT は EffectiveStyle.effectiveButtonTitleColorArgb に一本化）。
        val baseColor = EffectiveStyle.effectiveButtonTitleColorArgb(
            buttonCellTitleColor = cell.titleColor,
            cellStyle = cell.style,
            theme = theme,
        )
        // isEnabled = false 時は disabledTextColor に置換
        val titleColor = if (cell.isEnabled) baseColor else effective.disabledTextColor

        if (hasAux) {
            // ----- 通常レイアウト（aux いずれか指定時） -----
            // 通常レイアウト用 constraint set に戻す（reset 時にボタンスタイルに切り替わっていた場合の復帰）。
            normalLayoutSet.applyTo(views.root)

            applyCellBaseLayout(
                views = views,
                title = cell.title,
                description = null,
                valueText = cell.valueText,
                icon = cell.icon,
                hintText = cell.hintText,
                effective = effective,
                isEnabled = cell.isEnabled,
            )
            // ボタンの文字色 (4 段優先) を titleView に適用。
            views.titleView.setTextColor(titleColor)
            // titleAlignment を title 列内の gravity に反映（通常レイアウトでは title 列内のみ）。
            views.titleView.gravity = gravityFor(cell.titleAlignment)
            // accessoryHolder は ButtonCell では使わない。
            views.accessoryHolder.removeAllViews()
            views.accessoryHolder.visibility = View.GONE
        } else {
            // ----- ボタンスタイル（aux すべて null） -----
            // 非表示にする View を GONE 化
            views.iconView.visibility = View.GONE
            views.descriptionView.visibility = View.GONE
            views.valueTextView.visibility = View.GONE
            views.accessoryHolder.visibility = View.GONE
            views.hintTextView.visibility = View.GONE

            // contentRow（= その子の titleView）を root 全体に広げる
            buttonStyleSet.applyTo(views.root)
            // 行内 trailing が無いので title が主行の全幅を使う（titleAlignment の
            // 中央揃え・右揃えはこの配分に依存する）。
            applyTitleWidthMode(views, fillsRow = true)
            views.titleView.visibility = View.VISIBLE
            views.titleView.text = cell.title
            views.titleView.typeface = effective.titleTypeface
            views.titleView.textSize = effective.titleSizeSp
            views.titleView.setTextColor(titleColor)
            // ボタンスタイルでは titleAlignment を Cell 全体反映（gravity）として表現
            views.titleView.gravity = gravityFor(cell.titleAlignment)
        }

        applyCellBackground(views.root, effective)

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
        views.titleView.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        views.descriptionView.text = null
        views.valueTextView.text = null
        views.hintTextView.text = null
        views.iconView.setImageDrawable(null)
        views.iconView.visibility = View.GONE
        views.root.setOnClickListener(null)
        views.root.isClickable = false
        // 次回 bind で normalLayoutSet が再適用されるため、ここでは constraint をリセットしない。
    }

    /**
     * [CellTitleAlignment] を `Gravity` 値に変換する。
     * 垂直方向は常に `CENTER_VERTICAL` と OR 結合する。
     */
    private fun gravityFor(alignment: CellTitleAlignment): Int {
        val horizontal = when (alignment) {
            CellTitleAlignment.START -> Gravity.START
            CellTitleAlignment.CENTER -> Gravity.CENTER_HORIZONTAL
            CellTitleAlignment.END -> Gravity.END
        }
        return horizontal or Gravity.CENTER_VERTICAL
    }

    companion object {
        fun create(parent: ViewGroup): ButtonCellViewHolder {
            val views = buildCellBaseViews(parent)
            return ButtonCellViewHolder(views = views)
        }
    }
}
