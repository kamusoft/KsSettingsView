package jp.kamusoft.kssettingsview.ui

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import jp.kamusoft.kssettingsview.R

/**
 * [NumberPickerCell] 描画用 ViewHolder。
 *
 * 共通行レイアウト関数経由でレイアウトを構成し、accessory slot に chevron を表示する。
 * Cell タップで [NumberSelectionSheet]（ボトムシート + スナップ式ホイールの選択面）を提示する
 * （android/ADR-0007）。
 */
internal class NumberPickerCellViewHolder(
    internal val views: CellBaseViews,
    private val disclosureView: AppCompatImageView,
) : CellViewHolder<NumberPickerCell>(views.root) {

    override fun bind(cell: NumberPickerCell, theme: Theme) {
        val effective = EffectiveStyle.from(views.root.context, theme, cell.style)
        // valueText 明示指定を優先し、未指定なら unit を適用した自動表示にする。
        val displayValueText: String = cell.effectiveValueText()

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
            views.root.setOnClickListener { showSelectionSheet(cell, theme, effective) }
        } else {
            views.root.setOnClickListener(null)
            views.root.isClickable = false
        }

        applyEffectiveHeight(views.root, effective)
    }

    /**
     * 選択面（ボトムシート + スナップ式ホイール）を提示する（android/ADR-0007）。
     *
     * タイトルは `pickerTitle ?: title`、候補は [NumberCandidates] を件数と「index → 表示文字列」の
     * 関数として渡す（候補表示は `valueText` の明示指定によらず常に unit を適用する）。
     * 提示できない指定（[NumberCandidates.of] が `null`）では選択面を出さない。
     */
    private fun showSelectionSheet(cell: NumberPickerCell, theme: Theme, effective: EffectiveStyle) {
        val candidates = NumberCandidates.of(cell) ?: return
        NumberSelectionSheet(
            hostContext = views.root.context,
            sheetTitle = cell.pickerTitle ?: cell.title,
            itemCount = candidates.count,
            displayTextAt = candidates::displayTextAt,
            initialIndex = candidates.indexOf(cell.value),
            sheetStyle = PickerSheetStyle.from(cell, theme, effective),
            onConfirmed = { index -> cell.onValueChanged?.invoke(candidates.valueAt(index)) },
        ).showAnchoredTo(views.root)
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
        fun create(parent: ViewGroup): NumberPickerCellViewHolder {
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
            return NumberPickerCellViewHolder(views = views, disclosureView = disclosureView)
        }
    }
}

/**
 * [NumberPickerCell] の候補列を「先頭値・刻み・件数」で表す記述子。
 *
 * 候補値と表示文字列は index から都度算出し、列そのものは実体化しない。候補件数は `Int` の
 * 表現上限まで許容されるため、全件を `List` に展開すると有効な指定でメインスレッドが
 * 停止しうるのを避ける。
 *
 * @property first 先頭候補の値（`min`）
 * @property step 実効の刻み（`step <= 0` は 1 へ fallback 済み）
 * @property count 候補の件数
 * @property unit 表示文字列に付与する単位
 */
private class NumberCandidates(
    private val first: Int,
    private val step: Int,
    val count: Int,
    private val unit: String,
) {

    /**
     * [index] に対応する候補値。
     *
     * 「先頭 + index × step」を 64bit で計算してから `Int` へ戻すため、`max` 付近でも
     * `Int` のオーバーフローは起きない。
     */
    fun valueAt(index: Int): Int = (first.toLong() + index.toLong() * step).toInt()

    /** [index] に対応する候補の表示文字列（unit 適用後）。 */
    fun displayTextAt(index: Int): String = NumberPickerCell.format(valueAt(index), unit)

    /**
     * [value] に一致する候補の index。一致する候補が無ければ先頭候補（`0`）を返す。
     */
    fun indexOf(value: Int): Int {
        val offset = value.toLong() - first.toLong()
        if (offset < 0L || offset % step != 0L) return 0
        val index = offset / step
        return if (index < count) index.toInt() else 0
    }

    companion object {
        /** logcat 用タグ。 */
        private const val LOG_TAG = "NumberPickerCell"

        /**
         * [cell] の `min` / `max` / `step` から候補列を組み立てる。提示できない指定では `null` を返す。
         *
         * - `step <= 0` は 1 へ fallback する
         * - `min > max`（利用者の設定ミス）では警告ログを残して `null` を返す
         * - 候補件数は 64bit で算出し、`Int` の表現上限を超える指定でも警告ログを残して `null` を返す
         */
        fun of(cell: NumberPickerCell): NumberCandidates? {
            if (cell.min > cell.max) {
                // 利用者の設定ミス（min > max）。silent failure はデバッグが困難なため警告ログを残す。
                Log.w(
                    LOG_TAG,
                    "NumberPickerCell(id=${cell.id}) has invalid range: min=${cell.min} > max=${cell.max}." +
                        " Selection sheet will not be shown.",
                )
                return null
            }
            val step = if (cell.step <= 0) 1 else cell.step
            val count: Long = (cell.max.toLong() - cell.min.toLong()) / step + 1L
            if (count > Int.MAX_VALUE.toLong()) {
                Log.w(
                    LOG_TAG,
                    "NumberPickerCell(id=${cell.id}) has too many candidates: $count" +
                        " (min=${cell.min}, max=${cell.max}, step=$step). Selection sheet will not be shown.",
                )
                return null
            }
            return NumberCandidates(
                first = cell.min,
                step = step,
                count = count.toInt(),
                unit = cell.unit,
            )
        }
    }
}
