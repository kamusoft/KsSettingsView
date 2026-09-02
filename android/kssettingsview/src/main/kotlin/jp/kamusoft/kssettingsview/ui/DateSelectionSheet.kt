package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.icu.text.DateTimePatternGenerator
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 3連ホイールの系列。
 *
 * 選択の同期時に「変更の起点になった系列」を識別し、その系列自身は動かさないために使う
 * （静止直後の系列へ再スクロールを指示しない）。
 */
internal enum class DateWheelSeries { YEAR, MONTH, DAY }

/**
 * 日付ホイールの表示文字列と系列名を、端末 Locale の日付表記慣行から導出する
 * （android/ADR-0009。自前の翻訳文字列は同梱しない）。
 *
 * 表示文字列は ICU の「skeleton から最適パターンを導く」経路
 * （[android.text.format.DateFormat.getBestDateTimePattern]）で解決する。日本語なら
 * 「2026年 / 8月 / 2日」、英語なら「2026 / Aug / 2」のように、その Locale の慣行に従う。
 * 系列名（年 / 月 / 日）も ICU の日付フィールド名から解決する。
 *
 * パターンが解決できない、あるいは `java.time` で解釈できない Locale では、数値のみの表記へ
 * フォールバックする（表示が空になるより数値で出るほうが実害が小さい）。
 */
internal class DateWheelLabels(private val locale: Locale) {

    private val yearFormatter: DateTimeFormatter? = formatterFor(SKELETON_YEAR)
    private val monthFormatter: DateTimeFormatter? = formatterFor(SKELETON_MONTH)
    private val dayFormatter: DateTimeFormatter? = formatterFor(SKELETON_DAY)

    /** 年系列の名前（例: 「年」/ "Year"）。解決できない場合は `null`。 */
    val yearName: String? = fieldNameFor(DateTimePatternGenerator.YEAR)

    /** 月系列の名前（例: 「月」/ "Month"）。解決できない場合は `null`。 */
    val monthName: String? = fieldNameFor(DateTimePatternGenerator.MONTH)

    /** 日系列の名前（例: 「日」/ "Day"）。解決できない場合は `null`。 */
    val dayName: String? = fieldNameFor(DateTimePatternGenerator.DAY)

    /** [year] の年候補の表示文字列。 */
    fun year(year: Int): String =
        format(yearFormatter, year, 1, 1) ?: year.toString()

    /** [year] 年 [month] 月の月候補の表示文字列。 */
    fun month(year: Int, month: Int): String =
        format(monthFormatter, year, month, 1) ?: month.toString()

    /** [year] 年 [month] 月 [day] 日の日候補の表示文字列。 */
    fun day(year: Int, month: Int, day: Int): String =
        format(dayFormatter, year, month, day) ?: day.toString()

    private fun format(formatter: DateTimeFormatter?, year: Int, month: Int, day: Int): String? {
        if (formatter == null) return null
        return try {
            LocalDate.of(year, month, day).format(formatter)
        } catch (_: RuntimeException) {
            null
        }
    }

    /** [skeleton] に対する Locale 最適パターンの [DateTimeFormatter]。解決できない場合は `null`。 */
    private fun formatterFor(skeleton: String): DateTimeFormatter? = try {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
        DateTimeFormatter.ofPattern(pattern, locale)
    } catch (_: RuntimeException) {
        null
    }

    /** ICU の日付フィールド名（[DateTimePatternGenerator] の append item name）。 */
    private fun fieldNameFor(field: Int): String? = try {
        DateTimePatternGenerator.getInstance(locale).getAppendItemName(field).takeIf { it.isNotEmpty() }
    } catch (_: RuntimeException) {
        null
    }

    private companion object {
        /** 年だけを表す skeleton。 */
        const val SKELETON_YEAR: String = "y"

        /** 月だけを表す skeleton（英語圏では "Aug" のような月名表記になる）。 */
        const val SKELETON_MONTH: String = "MMM"

        /** 日だけを表す skeleton。 */
        const val SKELETON_DAY: String = "d"
    }
}

/**
 * [DatePickerCell] の日付候補を、`minDate` / `maxDate` から導いた有効範囲として表す記述子
 * （android/ADR-0009）。
 *
 * 年・月・日の各系列の候補は範囲の境界で切り詰められる（境界年では月が、境界月では日が制限される）。
 * 候補列は実体化せず、index から値と表示文字列を都度算出する（`LocalDate` の有効範囲は
 * 約20億年あり、全件を `List` に展開すると有効な指定でメインスレッドが停止しうる）。
 *
 * @property minDate 有効範囲の下限（`DatePickerCell.minDate`、未指定なら既定下限）
 * @property maxDate 有効範囲の上限（`DatePickerCell.maxDate`、未指定なら既定上限）
 * @property labels 表示文字列と系列名の解決器
 */
internal class DateCandidates(
    val minDate: LocalDate,
    val maxDate: LocalDate,
    val labels: DateWheelLabels,
) {

    /** 年候補の件数。 */
    val yearCount: Int = maxDate.year - minDate.year + 1

    // MARK: - 年

    /** [index] に対応する年。 */
    fun yearAt(index: Int): Int = minDate.year + index

    /** [year] に対応する年候補の index。 */
    fun yearIndexOf(year: Int): Int = year - minDate.year

    /** [index] に対応する年候補の表示文字列。 */
    fun yearTextAt(index: Int): String = labels.year(yearAt(index))

    // MARK: - 月

    /** [year] における最初の月候補（境界年では `minDate` の月から始まる）。 */
    fun firstMonth(year: Int): Int = if (year == minDate.year) minDate.monthValue else 1

    /** [year] における最後の月候補（境界年では `maxDate` の月で終わる）。 */
    fun lastMonth(year: Int): Int = if (year == maxDate.year) maxDate.monthValue else 12

    /** [year] における月候補の件数。 */
    fun monthCount(year: Int): Int = lastMonth(year) - firstMonth(year) + 1

    /** [year] における [index] 番目の月。 */
    fun monthAt(year: Int, index: Int): Int = firstMonth(year) + index

    /** [year] における [month] の月候補 index。 */
    fun monthIndexOf(year: Int, month: Int): Int = month - firstMonth(year)

    /** [year] における [index] 番目の月候補の表示文字列。 */
    fun monthTextAt(year: Int, index: Int): String = labels.month(year, monthAt(year, index))

    // MARK: - 日

    /** [year] 年 [month] 月における最初の日候補（境界月では `minDate` の日から始まる）。 */
    fun firstDay(year: Int, month: Int): Int =
        if (year == minDate.year && month == minDate.monthValue) minDate.dayOfMonth else 1

    /**
     * [year] 年 [month] 月における最後の日候補。
     *
     * 実日数（閏年を含む）を上限とし、境界月ではさらに `maxDate` の日で切り詰める。
     */
    fun lastDay(year: Int, month: Int): Int =
        if (year == maxDate.year && month == maxDate.monthValue) {
            maxDate.dayOfMonth
        } else {
            YearMonth.of(year, month).lengthOfMonth()
        }

    /** [year] 年 [month] 月における日候補の件数。 */
    fun dayCount(year: Int, month: Int): Int = lastDay(year, month) - firstDay(year, month) + 1

    /** [year] 年 [month] 月における [index] 番目の日。 */
    fun dayAt(year: Int, month: Int, index: Int): Int = firstDay(year, month) + index

    /** [year] 年 [month] 月における [day] の日候補 index。 */
    fun dayIndexOf(year: Int, month: Int, day: Int): Int = day - firstDay(year, month)

    /** [year] 年 [month] 月における [index] 番目の日候補の表示文字列。 */
    fun dayTextAt(year: Int, month: Int, index: Int): String =
        labels.day(year, month, dayAt(year, month, index))

    // MARK: - 丸め

    /**
     * 年・月・日から有効な日付を組み立てる。
     *
     * 日が新しい年・月の実日数を超える場合は末日へ丸め（1/31 → 2月 → 2/28）、
     * それでも [minDate]..[maxDate] の範囲外になる場合は範囲内の最も近い日付（範囲端）へ丸める。
     */
    fun resolve(year: Int, month: Int, day: Int): LocalDate {
        val safeMonth = month.coerceIn(1, 12)
        val endOfMonth = YearMonth.of(year, safeMonth).lengthOfMonth()
        val date = LocalDate.of(year, safeMonth, day.coerceIn(1, endOfMonth))
        return when {
            date.isBefore(minDate) -> minDate
            date.isAfter(maxDate) -> maxDate
            else -> date
        }
    }

    /** [date] が有効範囲に含まれるか。 */
    fun contains(date: LocalDate): Boolean = !date.isBefore(minDate) && !date.isAfter(maxDate)

    companion object {
        /** logcat 用タグ。 */
        private const val LOG_TAG = "DatePickerCell"

        /** `minDate` 未指定時の既定下限の年。 */
        const val DEFAULT_MIN_YEAR: Int = 1900

        /** `maxDate` 未指定時の既定上限の年。 */
        const val DEFAULT_MAX_YEAR: Int = 2100

        /**
         * 年候補として提示できる件数の上限。
         *
         * ホイールのスクロール範囲が破綻する水準（端末密度により約 1,200 万〜2,400 万件）より
         * 1桁以上低く抑えた防御上限で、実用的な年範囲（既定は 201 件）がこれに触れることはない。
         */
        const val MAX_YEAR_CANDIDATE_COUNT: Long = 1_000_000L

        /**
         * [cell] の `minDate` / `maxDate` から候補範囲を組み立てる。提示できない指定では `null` を返す。
         *
         * - 未指定側には既定（[DEFAULT_MIN_YEAR] 年の年初 / [DEFAULT_MAX_YEAR] 年の年末）を適用する
         * - `minDate > maxDate`（利用者の設定ミス）では警告ログを残して `null` を返す
         * - 既定を適用した結果として範囲が空になる構成でも、警告ログを残して `null` を返す
         * - 年候補の件数は 64bit で算出し、[MAX_YEAR_CANDIDATE_COUNT] を超える指定でも
         *   警告ログを残して `null` を返す
         */
        fun of(cell: DatePickerCell, labels: DateWheelLabels): DateCandidates? {
            val min = cell.minDate ?: LocalDate.of(DEFAULT_MIN_YEAR, 1, 1)
            val max = cell.maxDate ?: LocalDate.of(DEFAULT_MAX_YEAR, 12, 31)
            if (min.isAfter(max)) {
                // silent failure はデバッグが困難なため、原因を切り分けられる粒度で警告ログを残す。
                val reason = if (cell.minDate != null && cell.maxDate != null) {
                    "invalid range: minDate=${cell.minDate} > maxDate=${cell.maxDate}"
                } else {
                    "empty range after applying defaults: effective min=$min > max=$max"
                }
                Log.w(
                    LOG_TAG,
                    "DatePickerCell(id=${cell.id}) has $reason. Selection sheet will not be shown.",
                )
                return null
            }
            // 年候補の件数は Int に収まる指定でも提示に耐えないため、Int の表現上限ではなく
            // 提示上限（MAX_YEAR_CANDIDATE_COUNT）で判定する。算出自体は桁溢れを避けて 64bit で行う。
            val yearCount: Long = max.year.toLong() - min.year.toLong() + 1L
            if (yearCount > MAX_YEAR_CANDIDATE_COUNT) {
                Log.w(
                    LOG_TAG,
                    "DatePickerCell(id=${cell.id}) has too many year candidates: $yearCount" +
                        " (limit=$MAX_YEAR_CANDIDATE_COUNT, min=$min, max=$max)." +
                        " Selection sheet will not be shown.",
                )
                return null
            }
            return DateCandidates(minDate = min, maxDate = max, labels = labels)
        }
    }
}

/**
 * [DatePickerCell] の `Spinner` スタイルの選択面（ボトムシート + 年/月/日の3連ホイール）。
 *
 * 器は [NumberSelectionSheet] と同系で、「ドラッグハンドル + ヘッダー + コンテンツ」で構成する
 * （android/ADR-0009。ヘッダーの意匠は android/ADR-0005 / ADR-0007 と共有）。コンテンツは
 * 年・月・日の3連ホイールで、中央の選択位置には3列を横断する1本の accent 淡色帯を敷く。
 * `todayText` 指定時のみ、ホイール下に「今日」へジャンプする chip を置く。
 *
 * 確定経路は確定ボタンだけで、そのとき選択中の年・月・日から組み立てた `LocalDate` を
 * [onConfirmed] へ1回渡して閉じる。それ以外の閉じ方（取消ボタン・シート外側タップ・Back 操作・
 * 下方向スワイプ）では callback を発火しない。「今日」へのジャンプもホイール位置を動かすだけで
 * 発火しない。
 *
 * 年・月の選択が変わると、月・日の候補を新しい範囲へ差し替え、組み立てた日付が有効範囲を外れる
 * 場合は範囲内の最も近い日付へ丸める。
 *
 * @param hostContext シートを表示する Context
 * @param sheetTitle ヘッダー中央に表示するタイトル
 * @param candidates 有効範囲から導いた日付候補
 * @param initialDate 選択面を開いた時点の日付（範囲外なら最も近い範囲端へ丸める）
 * @param todayText 「今日」へジャンプする操作のラベル（`null` / 空文字なら操作を提示しない）
 * @param today 「今日」の取得（端末の既定タイムゾーンにおける今日。テストから注入できる）
 * @param sheetStyle 解決済みのスタイル値（強調色は Cell → CellStyle → Theme の段階解決済み）
 * @param actionColor ヘッダーの確定・取消操作の色（`androidButtonColor` 指定時はその色）
 * @param onConfirmed 確定 callback（確定操作でのみ発火する）
 */
internal class DateSelectionSheet(
    hostContext: Context,
    sheetTitle: String,
    internal val candidates: DateCandidates,
    initialDate: LocalDate,
    todayText: String?,
    private val today: () -> LocalDate,
    private val sheetStyle: PickerSheetStyle,
    @ColorInt actionColor: Int,
    private val onConfirmed: (LocalDate) -> Unit,
) : BottomSheetDialog(hostContext.ksThemedContext()) {

    /** シート内容のルート（ドラッグハンドル + ヘッダー + 3連ホイール + 「今日」）。 */
    internal val contentRoot: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    /**
     * 選択中の日付。
     *
     * 常に有効範囲内に保たれる（範囲外の [initialDate] は最も近い範囲端へ丸めて開く）。
     * 各系列の選択はスナップ静止時にのみ更新されるため、慣性移動の途中で確定しても採用されるのは
     * 直前に静止した候補から組み立てた日付になる。
     */
    internal var selectedDate: LocalDate =
        candidates.resolve(initialDate.year, initialDate.monthValue, initialDate.dayOfMonth)
        private set

    /** 年ホイール。 */
    internal val yearWheel: KsWheelView = buildWheel(
        itemCount = candidates.yearCount,
        displayTextAt = candidates::yearTextAt,
        initialIndex = candidates.yearIndexOf(selectedDate.year),
        seriesLabel = candidates.labels.yearName,
    )

    /** 月ホイール。 */
    internal val monthWheel: KsWheelView = buildWheel(
        itemCount = candidates.monthCount(selectedDate.year),
        displayTextAt = monthTextAt(selectedDate.year),
        initialIndex = candidates.monthIndexOf(selectedDate.year, selectedDate.monthValue),
        seriesLabel = candidates.labels.monthName,
    )

    /** 日ホイール。 */
    internal val dayWheel: KsWheelView = buildWheel(
        itemCount = candidates.dayCount(selectedDate.year, selectedDate.monthValue),
        displayTextAt = dayTextAt(selectedDate.year, selectedDate.monthValue),
        initialIndex = candidates.dayIndexOf(
            selectedDate.year,
            selectedDate.monthValue,
            selectedDate.dayOfMonth,
        ),
        seriesLabel = candidates.labels.dayName,
    )

    /** ヘッダー（取消 / タイトル / 確定）。操作色だけ [actionColor] へ差し替えた値で描く。 */
    internal val headerView: SheetHeaderView = SheetHeaderView(
        context = context,
        style = sheetStyle.copy(accentColor = actionColor),
        title = sheetTitle,
        showConfirm = true,
        onCancel = { cancel() },
        onConfirm = { confirmSelection() },
    )

    /** 「今日」へジャンプする chip（`todayText` 未指定時は生成しない）。 */
    internal val todayView: TextView? =
        if (todayText.isNullOrEmpty()) null else buildTodayChip(todayText)

    /** ヘッダー左の取消ラベル。 */
    internal val cancelView: TextView get() = headerView.cancelView

    /** ヘッダー中央のタイトル。 */
    internal val titleView: TextView get() = headerView.titleView

    /** ヘッダー右の確定ラベル。 */
    internal val confirmView: TextView get() = headerView.confirmView

    /**
     * 選択の同期中かどうか。
     *
     * 同期の過程で行うプログラム的な選択更新が、系列の選択変更通知として戻ってきて再入するのを防ぐ。
     */
    private var isSyncing: Boolean = false

    init {
        yearWheel.onSelectionChanged = { index ->
            onSeriesChanged(DateWheelSeries.YEAR) {
                candidates.resolve(
                    candidates.yearAt(index),
                    selectedDate.monthValue,
                    selectedDate.dayOfMonth,
                )
            }
        }
        monthWheel.onSelectionChanged = { index ->
            onSeriesChanged(DateWheelSeries.MONTH) {
                candidates.resolve(
                    selectedDate.year,
                    candidates.monthAt(selectedDate.year, index),
                    selectedDate.dayOfMonth,
                )
            }
        }
        dayWheel.onSelectionChanged = { index ->
            // 日候補は常に有効範囲内のため、丸めもホイールの差し替えも要らない。
            if (!isSyncing) {
                selectedDate = LocalDate.of(
                    selectedDate.year,
                    selectedDate.monthValue,
                    candidates.dayAt(selectedDate.year, selectedDate.monthValue, index),
                )
            }
        }

        contentRoot.addView(buildSheetDragHandle(context, sheetStyle.separatorColor))
        contentRoot.addView(headerView)
        contentRoot.addView(buildDivider())
        contentRoot.addView(buildWheelRow())
        todayView?.let {
            // 「今日」を出さないときは区切り線ごと構成から外す。
            contentRoot.addView(buildDivider())
            contentRoot.addView(buildTodayBar(it))
        }
        contentRoot.addView(buildBottomPadding())
        setContentView(contentRoot)
        // シート外側のタップで閉じられる（確定 callback は発火しない）。
        setCanceledOnTouchOutside(true)
    }

    override fun onStart() {
        super.onStart()
        val container = findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        applySheetSurfaceColor(container, sheetStyle.sheetBackgroundColor)
        // 内容の高さが固定（ホイールの可視行数分）なので、常に内容高で全展開して表示する。
        // 折り目を経由しないため、下方向のドラッグはそのまま dismiss になる。
        BottomSheetBehavior.from(container).apply {
            isFitToContents = true
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    // MARK: - View 構築

    private fun buildWheel(
        itemCount: Int,
        displayTextAt: (Int) -> String,
        initialIndex: Int,
        seriesLabel: String?,
    ): KsWheelView = KsWheelView(
        context = context,
        itemCount = itemCount,
        displayTextAt = displayTextAt,
        initialIndex = initialIndex,
        wheelStyle = KsWheelStyle.from(sheetStyle),
        seriesLabel = seriesLabel,
        // 中央の帯は3列を横断する1本を親側で敷くため、各ホイールには持たせない。
        showsBand = false,
    )

    /**
     * 3連ホイールの行を構築する。
     *
     * 中央の選択位置を示す帯を1本だけ敷き、その上に等幅の3ホイールを載せる。
     *
     * 列の並びは年→月→日で固定する。`LinearLayout` は RTL の Locale では子を右から左へ並べる
     * ため、行の [android.view.View.setLayoutDirection] を LTR に固定して、どの Locale でも
     * 年が左・日が右に来るようにする。
     *
     * 行そのものを「自分でスクロールを扱う領域」として宣言する（`isNestedScrollingEnabled`）。
     * `BottomSheetBehavior` はシート内で最初に見つけたスクロールする子だけを対象に
     * 「その子の上での縦ドラッグはシートのドラッグにしない」と判定するため、宣言が無いと
     * 2列目・3列目のホイールを下方向へ操作したときにシートの dismiss になってしまう。
     * 行全体を対象にすることで、どの列を操作しても候補の遷移になる。
     */
    private fun buildWheelRow(): View {
        val wheels = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )
            listOf(yearWheel, monthWheel, dayWheel).forEach { wheel ->
                addView(
                    wheel,
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
                )
            }
        }
        return FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            isNestedScrollingEnabled = true
            addView(buildBand())
            addView(wheels)
        }
    }

    /** 3列を横断する中央の選択位置の帯。 */
    private fun buildBand(): View = View(context).apply {
        val inset = context.sheetDp(SheetMetrics.PADDING_HORIZONTAL_DP)
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            context.sheetDp(KsWheelView.ROW_HEIGHT_DP),
            Gravity.CENTER_VERTICAL,
        ).apply {
            marginStart = inset
            marginEnd = inset
        }
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = context.sheetDp(KsWheelView.BAND_CORNER_RADIUS_DP).toFloat()
            setColor(ColorUtils.setAlphaComponent(sheetStyle.accentColor, KsWheelView.BAND_ALPHA))
        }
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /** 「今日」へジャンプする outline chip。 */
    private fun buildTodayChip(label: String): TextView = TextView(context).apply {
        text = label
        setTextColor(sheetStyle.accentColor)
        typeface = Typeface.create(sheetStyle.itemTypeface, Typeface.BOLD)
        textSize = sheetStyle.headerActionTextSizeSp
        isSingleLine = true
        gravity = Gravity.CENTER
        minHeight = context.sheetDp(SheetMetrics.MIN_TOUCH_TARGET_DP)
        setPadding(
            context.sheetDp(TODAY_PADDING_H_DP),
            context.sheetDp(TODAY_PADDING_V_DP),
            context.sheetDp(TODAY_PADDING_H_DP),
            context.sheetDp(TODAY_PADDING_V_DP),
        )
        val outline = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = context.sheetDp(SheetMetrics.CONFIRM_CORNER_RADIUS_DP).toFloat()
            setStroke(TODAY_STROKE_PX, sheetStyle.accentColor)
        }
        background = RippleDrawable(ColorStateList.valueOf(sheetStyle.rippleColor), outline, null)
        isClickable = true
        isFocusable = true
        publishAsSheetButton(this)
        setOnClickListener { jumpToToday() }
    }

    /** 「今日」chip を中央に置く行。 */
    private fun buildTodayBar(chip: TextView): View = FrameLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        setPadding(
            0,
            context.sheetDp(TODAY_BAR_PADDING_TOP_DP),
            0,
            context.sheetDp(TODAY_BAR_PADDING_BOTTOM_DP),
        )
        addView(
            chip,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL,
            ),
        )
    }

    /** ヘッダーとホイールの境界などに引く区切り線。 */
    private fun buildDivider(): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            DIVIDER_THICKNESS_PX,
        )
        setBackgroundColor(sheetStyle.separatorColor)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /** 内容下端とシート下端のあいだの余白。 */
    private fun buildBottomPadding(): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            context.sheetDp(BOTTOM_PADDING_DP),
        )
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    // MARK: - 選択の同期

    /** [year] 年の月候補の表示文字列を返す関数。 */
    private fun monthTextAt(year: Int): (Int) -> String = { index -> candidates.monthTextAt(year, index) }

    /** [year] 年 [month] 月の日候補の表示文字列を返す関数。 */
    private fun dayTextAt(year: Int, month: Int): (Int) -> String =
        { index -> candidates.dayTextAt(year, month, index) }

    /**
     * 系列の選択が変わったときの処理。[resolve] で組み立てた日付を全系列へ反映する。
     *
     * 同期中の通知は無視する（プログラム的な選択更新による再入を防ぐ）。
     */
    private fun onSeriesChanged(series: DateWheelSeries, resolve: () -> LocalDate) {
        if (isSyncing) return
        applySelection(resolve(), changedSeries = series)
    }

    /**
     * 選択中の日付を [date] にし、各系列の候補と選択位置を追随させる。
     *
     * [changedSeries] は変更の起点になった系列で、その系列自身は動かさない（静止直後の系列へ
     * 再スクロールを指示しないため）。年・月の選択は範囲の丸めによって変わらないことが保証される
     * （候補が範囲内に制限されているため、丸めが動かすのは日だけ）。
     */
    private fun applySelection(date: LocalDate, changedSeries: DateWheelSeries?) {
        selectedDate = date
        isSyncing = true
        try {
            if (changedSeries != DateWheelSeries.YEAR) {
                yearWheel.setSelectedIndex(candidates.yearIndexOf(date.year))
            }
            if (changedSeries != DateWheelSeries.MONTH) {
                monthWheel.setCandidates(
                    itemCount = candidates.monthCount(date.year),
                    displayTextAt = monthTextAt(date.year),
                    selectedIndex = candidates.monthIndexOf(date.year, date.monthValue),
                )
            }
            dayWheel.setCandidates(
                itemCount = candidates.dayCount(date.year, date.monthValue),
                displayTextAt = dayTextAt(date.year, date.monthValue),
                selectedIndex = candidates.dayIndexOf(date.year, date.monthValue, date.dayOfMonth),
            )
        } finally {
            isSyncing = false
        }
    }

    /**
     * 3系列の選択をデバイスの現在日付へ移す。
     *
     * 今日が有効範囲外のときは何もしない。値の確定は確定操作だけなので、ここでは callback を
     * 発火しない。
     */
    private fun jumpToToday() {
        val todayDate = today()
        if (!candidates.contains(todayDate)) return
        applySelection(todayDate, changedSeries = null)
    }

    /**
     * 確定操作。その時点で選択中の年・月・日から組み立てた日付を1回だけ通知して閉じる。
     */
    private fun confirmSelection() {
        onConfirmed(selectedDate)
        dismiss()
    }

    companion object {
        /** 区切り線の太さ（1 物理 pixel 固定。dp 換算しない）。 */
        private const val DIVIDER_THICKNESS_PX: Int = 1

        /** 内容下端とシート下端のあいだの余白（dp）。 */
        private const val BOTTOM_PADDING_DP: Float = 14f

        /** 「今日」chip の左右 padding（dp）。 */
        private const val TODAY_PADDING_H_DP: Float = 20f

        /** 「今日」chip の上下 padding（dp）。 */
        private const val TODAY_PADDING_V_DP: Float = 6f

        /** 「今日」chip の枠線の太さ（1 物理 pixel 固定。dp 換算しない）。 */
        private const val TODAY_STROKE_PX: Int = 1

        /** 「今日」行の上 padding（dp。区切り線と chip のあいだ）。 */
        private const val TODAY_BAR_PADDING_TOP_DP: Float = 10f

        /** 「今日」行の下 padding（dp。chip と内容下端のあいだ）。 */
        private const val TODAY_BAR_PADDING_BOTTOM_DP: Float = 4f
    }
}

/**
 * 端末の第一 Locale（表示文字列の導出に使う）。
 */
internal fun Context.primaryLocale(): Locale {
    val locales = resources.configuration.locales
    return if (locales.isEmpty) Locale.getDefault() else locales[0]
}
