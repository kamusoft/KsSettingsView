package jp.kamusoft.kssettingsview.ui

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import jp.kamusoft.kssettingsview.R

/**
 * [TimePickerCell] 描画用 ViewHolder。
 *
 * 共通行レイアウト関数経由でレイアウトを構成し、Cell タップで [TimeSelectionSheet]
 * （ボトムシート + 時分ホイールの選択面）を提示する（android/ADR-0018）。
 * 選択面はホスト Activity の型に前提を置かないため、`ComponentActivity` ホストでも成立する。
 */
internal class TimePickerCellViewHolder(
    internal val views: CellBaseViews,
    private val disclosureView: AppCompatImageView,
) : CellViewHolder<TimePickerCell>(views.root) {

    override fun bind(cell: TimePickerCell, theme: Theme) {
        val effective = EffectiveStyle.from(views.root.context, theme, cell.style)
        val displayValueText: String = cell.valueText ?: formatTime(cell.time, cell.format)

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
     * 選択面（ボトムシート + 時分ホイール）を提示する（android/ADR-0018）。
     *
     * タイトルは `pickerTitle ?: title`、時制と候補系列は `is24Hour` から導く（[TimeCandidates]）。
     * 表示文字列と午前/午後のラベルは端末 Locale から導出する。
     */
    private fun showSelectionSheet(cell: TimePickerCell, theme: Theme, effective: EffectiveStyle) {
        val ctx = views.root.context
        TimeSelectionSheet(
            hostContext = ctx,
            sheetTitle = cell.pickerTitle ?: cell.title,
            candidates = TimeCandidates.of(cell, TimeWheelLabels(ctx.primaryLocale())),
            initialTime = cell.time,
            sheetStyle = PickerSheetStyle.from(cell, theme, effective),
            onConfirmed = { newTime -> cell.onValueChanged?.invoke(newTime) },
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
        fun create(parent: ViewGroup): TimePickerCellViewHolder {
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
            return TimePickerCellViewHolder(views = views, disclosureView = disclosureView)
        }

        /**
         * pattern 文字列に対する [DateTimeFormatter] のキャッシュ。
         * 同一 format を持つ ViewHolder の bind ごとに `DateTimeFormatter.ofPattern` を
         * 再構築するコスト（内部の正規表現パース等）を避けるために共有する。
         * 不正パターンは負例としても再構築コストを抑えるため `Optional` 的に sentinel を入れず、
         * 例外が出た pattern はキャッシュせず毎回 fallback ルートを取らせる。
         */
        private val formatterCache: java.util.concurrent.ConcurrentHashMap<String, DateTimeFormatter> =
            java.util.concurrent.ConcurrentHashMap()

        private fun formatterFor(format: String): DateTimeFormatter? {
            formatterCache[format]?.let { return it }
            return try {
                val fmt = DateTimeFormatter.ofPattern(format)
                formatterCache[format] = fmt
                fmt
            } catch (_: Throwable) {
                null
            }
        }

        /**
         * `LocalTime` を `DateTimeFormatter.ofPattern(format)` で文字列化するヘルパ。
         * パターンエラー時はトースト不要の単純な toString フォールバック。
         */
        internal fun formatTime(time: LocalTime, format: String): String {
            val fmt = formatterFor(format) ?: return time.toString()
            return try {
                time.format(fmt)
            } catch (_: Throwable) {
                time.toString()
            }
        }
    }
}
