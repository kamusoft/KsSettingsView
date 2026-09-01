package jp.kamusoft.kssettingsview.ui

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.ui.graphics.toArgb
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import jp.kamusoft.kssettingsview.R

/**
 * [DatePickerCell] 描画用 ViewHolder。
 *
 * `uiStyle` の値で UI を切り替える：
 * - [DatePickerUIStyle.Material]: カレンダーダイアログ（[DateCalendarDialog]、android/ADR-0019）を表示
 * - [DatePickerUIStyle.Spinner]: ボトムシート + 年/月/日の3連ホイール（[DateSelectionSheet]、
 *   android/ADR-0009）を表示
 */
internal class DatePickerCellViewHolder(
    internal val views: CellBaseViews,
    private val disclosureView: AppCompatImageView,
) : CellViewHolder<DatePickerCell>(views.root) {

    /**
     * 「今日」の取得元。
     *
     * 端末の既定タイムゾーンにおける今日を返す。テストから固定日付を注入して、
     * 実行時刻に依存しない検証を行えるようにするための差し替え点。
     */
    internal var todayProvider: () -> LocalDate = { LocalDate.now() }

    override fun bind(cell: DatePickerCell, theme: Theme) {
        val effective = EffectiveStyle.from(views.root.context, theme, cell.style)
        val displayValueText: String = cell.valueText ?: formatDate(cell.date, cell.format)

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
            // ダイアログ配色は「表示時点の実効テーマ色」を反映する。bind で解決済みの色を捕捉して
            // クリック時にそのまま渡す（`showCalendarDialog` 側から Theme を引き直さない）。
            val dialogColors = resolveDialogColors(cell, theme, effective)
            views.root.setOnClickListener {
                when (cell.uiStyle) {
                    DatePickerUIStyle.Material -> showCalendarDialog(cell, dialogColors)
                    DatePickerUIStyle.Spinner -> showDateSelectionSheet(cell, theme, effective)
                }
            }
        } else {
            views.root.setOnClickListener(null)
            views.root.isClickable = false
        }

        applyEffectiveHeight(views.root, effective)
    }

    /**
     * 日付選択ダイアログへ渡す色ロールを解決する。
     *
     * - 背景: `Theme.backgroundColor`（Cell 背景ではなく SettingsView 全体の背景色）
     * - 強調: `DatePickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor`
     *   （後半 2 段は [EffectiveStyle] の既存解決をそのまま使う）
     * - 通常文字: `CellStyle.titleColor` → `Theme.cellTitleColor` → プラットフォーム既定
     */
    internal fun resolveDialogColors(
        cell: DatePickerCell,
        theme: Theme,
        effective: EffectiveStyle,
    ): PickerDialogColors = resolveDatePickerDialogColors(cell, theme, effective)

    /**
     * カレンダー選択面（ダイアログ）を提示する（[DatePickerUIStyle.Material]、android/ADR-0019）。
     *
     * タイトルは `pickerTitle ?: title`、選択できる日付は `minDate` / `maxDate` から導いた範囲
     * （[DateCalendarRange]）で、開いた時点の `cell.date` が範囲外なら範囲端へ丸めて提示する。
     * 提示できない指定（[DateCalendarRange.of] が `null`）では選択面を出さない。
     */
    private fun showCalendarDialog(cell: DatePickerCell, dialogColors: PickerDialogColors) {
        val range = DateCalendarRange.of(cell) ?: return
        val dialog = DateCalendarDialog(
            hostContext = views.root.context,
            dialogTitle = cell.pickerTitle ?: cell.title,
            range = range,
            initialDate = range.clamp(cell.date),
            todayText = cell.todayText?.takeIf { it.isNotEmpty() },
            today = todayProvider,
            colors = dialogColors,
            onConfirmed = { newDate -> cell.onValueChanged?.invoke(newDate) },
        )
        // 構成変更をまたいで提示を続けられるよう、行を載せている View へ表示中であることを預ける。
        val forgetDialog = views.root.findKsSettingsViewHost()?.trackCalendarDialog(cell.id, dialog)
        dialog.showAnchoredTo(views.root, forgetDialog)
    }

    /**
     * 選択面（ボトムシート + 年/月/日の3連ホイール）を提示する（android/ADR-0009）。
     *
     * タイトルは `pickerTitle ?: title`、候補は `minDate` / `maxDate` から導いた有効範囲
     * （[DateCandidates]）で、表示文字列は端末 Locale から導出する。提示できない指定
     * （[DateCandidates.of] が `null`）では選択面を出さない。
     *
     * ヘッダーの確定 / 取消操作の色は `androidButtonColor` を最優先し、未指定なら強調色の
     * 段階解決（Cell → CellStyle → Theme）に従う。
     */
    private fun showDateSelectionSheet(cell: DatePickerCell, theme: Theme, effective: EffectiveStyle) {
        val ctx = views.root.context
        val candidates = DateCandidates.of(cell, DateWheelLabels(ctx.primaryLocale())) ?: return
        val sheetStyle = PickerSheetStyle.from(cell, theme, effective)
        DateSelectionSheet(
            hostContext = ctx,
            sheetTitle = cell.pickerTitle ?: cell.title,
            candidates = candidates,
            initialDate = cell.date,
            todayText = cell.todayText,
            today = todayProvider,
            sheetStyle = sheetStyle,
            actionColor = cell.androidButtonColor?.toArgb() ?: sheetStyle.accentColor,
            onConfirmed = { newDate -> cell.onValueChanged?.invoke(newDate) },
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
        fun create(parent: ViewGroup): DatePickerCellViewHolder {
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
            return DatePickerCellViewHolder(views = views, disclosureView = disclosureView)
        }

        /**
         * pattern 文字列に対する [DateTimeFormatter] のキャッシュ。
         * 同一 format を持つ ViewHolder の bind ごとに `DateTimeFormatter.ofPattern` を
         * 再構築するコスト（内部の正規表現パース等）を避けるために共有する。
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
         * `LocalDate` を `DateTimeFormatter.ofPattern(format)` で文字列化するヘルパ。
         */
        internal fun formatDate(date: LocalDate, format: String): String {
            val fmt = formatterFor(format) ?: return date.toString()
            return try {
                date.format(fmt)
            } catch (_: Throwable) {
                date.toString()
            }
        }
    }
}

/**
 * `LocalDate` の UTC epoch ms 表現を返す。
 *
 * カレンダー選択面は日付を epoch ms で保持する。基準を UTC に固定することで、端末タイムゾーンに
 * よって日付が前後にずれない日単位の往復になる（戻しは [toLocalDateUtc]）。
 */
internal fun LocalDate.toEpochMilliUtc(): Long =
    this.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/**
 * 日付選択ダイアログへ渡す色ロールを解決する。
 *
 * - 背景: `Theme.backgroundColor`（Cell 背景ではなく SettingsView 全体の背景色）
 * - 強調: `DatePickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor`
 *   （後半 2 段は [EffectiveStyle] の既存解決をそのまま使う）
 * - 通常文字: `CellStyle.titleColor` → `Theme.cellTitleColor` → プラットフォーム既定
 *
 * 行タップからの表示と Activity 再生成後の復元が同じ解決規則を使うため、ViewHolder の
 * 外側に置く。
 */
internal fun resolveDatePickerDialogColors(
    cell: DatePickerCell,
    theme: Theme,
    effective: EffectiveStyle,
): PickerDialogColors = PickerDialogColors(
    background = theme.backgroundColor.toArgb(),
    accent = cell.accentColor?.toArgb() ?: effective.accentColor,
    text = effective.titleColor,
)
