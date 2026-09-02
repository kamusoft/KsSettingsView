package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.icu.text.DateTimePatternGenerator
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 時刻ホイールの系列。
 *
 * 24時間制では [HOUR] / [MINUTE] の2系列、12時間制では [PERIOD] を加えた3系列になる。
 */
internal enum class TimeWheelSeries { HOUR, MINUTE, PERIOD }

/**
 * 時刻ホイールの表示文字列と系列名を、端末 Locale の表記慣行から導出する
 * （android/ADR-0018。自前の翻訳文字列は同梱しない）。
 *
 * 午前 / 午後のラベルは `DateTimeFormatter` の AM/PM パターン（`a`）を Locale で解決して得る。
 * 解決できない Locale では `java.text.DateFormatSymbols` の AM/PM 表記へフォールバックし、
 * それも空なら `Locale.ROOT` の表記を使う（いずれも OS 由来の文字列で、ライブラリは翻訳を持たない）。
 * 系列名（時 / 分 / 午前午後）は ICU の日付フィールド名から解決し、アクセシビリティへのみ公開する
 * （見た目には系列ラベルを置かない）。
 *
 * 数値の表記は Locale の数字体系に従う（`String.format` に Locale を渡す）。分は2桁ゼロ詰めにし、
 * 時と分の桁幅が揃うようにする。
 *
 * 12時間制の系列の並び順も Locale の12時間表記パターンから導く（[isPeriodLeading]）。自前の
 * Locale 一覧は持たず、OS のパターン解決に委ねる。
 */
internal class TimeWheelLabels(private val locale: Locale) {

    /** 午前 / 午後の表示文字列（index 0 が午前、1 が午後）。 */
    private val periodTexts: List<String> = resolvePeriodTexts(locale)

    /** 時系列の名前（例: 「時」/ "Hour"）。解決できない場合は `null`。 */
    val hourName: String? = fieldNameFor(DateTimePatternGenerator.HOUR)

    /** 分系列の名前（例: 「分」/ "Minute"）。解決できない場合は `null`。 */
    val minuteName: String? = fieldNameFor(DateTimePatternGenerator.MINUTE)

    /** 午前/午後系列の名前（例: 「午前/午後」/ "AM/PM"）。解決できない場合は `null`。 */
    val periodName: String? = fieldNameFor(DateTimePatternGenerator.DAYPERIOD)

    /**
     * 12時間制で午前/午後の系列を時より前に置くか。
     *
     * Locale の12時間表記パターンで午前/午後が時より前に来る（日本語の「午後 2:30」など）なら
     * `true`、後に来る（英語の "2:30 PM" など）なら `false` になる。
     */
    val isPeriodLeading: Boolean = resolvePeriodLeading(locale)

    /** [hour] の時候補の表示文字列。 */
    fun hour(hour: Int): String = String.format(locale, "%d", hour)

    /** [minute] の分候補の表示文字列（2桁ゼロ詰め）。 */
    fun minute(minute: Int): String = String.format(locale, "%02d", minute)

    /** [index] に対応する午前/午後の表示文字列。 */
    fun period(index: Int): String = periodTexts[index.coerceIn(0, periodTexts.lastIndex)]

    /** ICU の日付フィールド名（[DateTimePatternGenerator] の append item name）。 */
    private fun fieldNameFor(field: Int): String? = try {
        DateTimePatternGenerator.getInstance(locale).getAppendItemName(field).takeIf { it.isNotEmpty() }
    } catch (_: RuntimeException) {
        null
    }

    private companion object {

        /**
         * [locale] の午前 / 午後の表記を OS から解決する。
         *
         * `DateTimeFormatter` の AM/PM パターン → `java.text.DateFormatSymbols` → `Locale.ROOT` の
         * 順にフォールバックする。
         */
        fun resolvePeriodTexts(locale: Locale): List<String> {
            formatterPeriodTexts(locale)?.let { return it }
            symbolsPeriodTexts(locale)?.let { return it }
            return symbolsPeriodTexts(Locale.ROOT) ?: listOf("AM", "PM")
        }

        /** AM/PM パターン（`a`）を Locale で解決した表記。 */
        fun formatterPeriodTexts(locale: Locale): List<String>? = try {
            val formatter = DateTimeFormatter.ofPattern(AM_PM_PATTERN, locale)
            listOf(LocalTime.of(0, 0).format(formatter), LocalTime.of(12, 0).format(formatter))
                .takeIf { texts -> texts.all { it.isNotEmpty() } }
        } catch (_: RuntimeException) {
            null
        }

        /** `java.text.DateFormatSymbols` の AM/PM 表記。 */
        fun symbolsPeriodTexts(locale: Locale): List<String>? = try {
            java.text.DateFormatSymbols.getInstance(locale).amPmStrings
                ?.takeIf { it.size >= 2 }
                ?.let { listOf(it[0], it[1]) }
                ?.takeIf { texts -> texts.all { it.isNotEmpty() } }
        } catch (_: RuntimeException) {
            null
        }

        /**
         * [locale] の12時間表記パターンで、午前/午後が時より前に来るかを解決する。
         *
         * パターンは OS（ICU）に解決させ、引用符の外にあるパターン文字だけを見て順序を決める。
         * 午前/午後または時のパターン文字が見つからない Locale では、時を先頭に置く並びにする。
         */
        fun resolvePeriodLeading(locale: Locale): Boolean {
            val pattern = try {
                DateTimePatternGenerator.getInstance(locale).getBestPattern(TIME_12H_SKELETON)
            } catch (_: RuntimeException) {
                return false
            }
            val periodIndex = indexOfPatternChar(pattern, PERIOD_PATTERN_CHARS)
            val hourIndex = indexOfPatternChar(pattern, HOUR_PATTERN_CHARS)
            if (periodIndex < 0 || hourIndex < 0) return false
            return periodIndex < hourIndex
        }

        /**
         * [pattern] の中で [chars] のいずれかが最初に現れる位置（無ければ -1）。
         *
         * 引用符（`'`）で囲まれた部分は文字そのものを表すリテラルなので数えない。`''` は
         * 「引用符そのもの」のエスケープであり、引用の開閉としては数えない。
         */
        fun indexOfPatternChar(pattern: String, chars: String): Int {
            var inQuote = false
            var index = 0
            while (index < pattern.length) {
                val char = pattern[index]
                if (char == '\'') {
                    if (index + 1 < pattern.length && pattern[index + 1] == '\'') {
                        index += 2
                        continue
                    }
                    inQuote = !inQuote
                    index++
                    continue
                }
                if (!inQuote && char in chars) return index
                index++
            }
            return -1
        }

        /** AM/PM だけを表すパターン。 */
        const val AM_PM_PATTERN: String = "a"

        /** 12時間制の「時と分」を表すパターンの骨組み。 */
        const val TIME_12H_SKELETON: String = "hm"

        /** 午前/午後を表すパターン文字（`a` は AM/PM、`b` / `B` は時間帯表記）。 */
        const val PERIOD_PATTERN_CHARS: String = "abB"

        /** 時を表すパターン文字（12/24時間制と 0/1 起点の各表記）。 */
        const val HOUR_PATTERN_CHARS: String = "hHkK"
    }
}

/**
 * [TimePickerCell] の時刻候補を表す記述子（android/ADR-0018）。
 *
 * 時制は cell の `is24Hour` だけで決まる（core/ADR-0028。`format` も端末の24時間設定も参照しない）。
 * 24時間制では「時 0–23 / 分 0–59」の2系列、12時間制では「時 1–12 / 分 0–59 / 午前・午後」の
 * 3系列になる。
 *
 * @property is24Hour 24時間制か（`false` なら12時間制）
 * @property labels 表示文字列と系列名の解決器
 */
internal class TimeCandidates(
    val is24Hour: Boolean,
    val labels: TimeWheelLabels,
) {

    /** 時候補の件数（24時間制なら 24、12時間制なら 12）。 */
    val hourCount: Int = if (is24Hour) HOUR_COUNT_24 else HOUR_COUNT_12

    /** 分候補の件数。 */
    val minuteCount: Int = MINUTE_COUNT

    /** 午前/午後候補の件数。 */
    val periodCount: Int = PERIOD_COUNT

    // MARK: - 時

    /** [index] に対応する時の表示値（24時間制なら 0–23、12時間制なら 1–12）。 */
    fun hourAt(index: Int): Int = if (is24Hour) index else index + 1

    /** [time] に対応する時候補の index。 */
    fun hourIndexOf(time: LocalTime): Int =
        if (is24Hour) time.hour else displayHourOf(time.hour) - 1

    /** [index] に対応する時候補の表示文字列。 */
    fun hourTextAt(index: Int): String = labels.hour(hourAt(index))

    // MARK: - 分

    /** [index] に対応する分（index と同値）。 */
    fun minuteAt(index: Int): Int = index

    /** [time] に対応する分候補の index。 */
    fun minuteIndexOf(time: LocalTime): Int = time.minute

    /** [index] に対応する分候補の表示文字列。 */
    fun minuteTextAt(index: Int): String = labels.minute(minuteAt(index))

    // MARK: - 午前 / 午後

    /** [time] に対応する午前/午後候補の index。 */
    fun periodIndexOf(time: LocalTime): Int = if (time.hour < HOURS_PER_PERIOD) INDEX_AM else INDEX_PM

    /** [index] に対応する午前/午後候補の表示文字列。 */
    fun periodTextAt(index: Int): String = labels.period(index)

    // MARK: - 組み立て

    /**
     * 各系列の選択 index から時刻を組み立てる。
     *
     * 12時間制では「12時台 = 0時台（午前）/ 12時台（午後）」の境界を含めて24時間制へ戻す
     * （12・午前 → `00:mm`、12・午後 → `12:mm`）。24時間制では [periodIndex] を参照しない。
     */
    fun timeOf(hourIndex: Int, minuteIndex: Int, periodIndex: Int): LocalTime {
        val hour = if (is24Hour) {
            hourAt(hourIndex)
        } else {
            val offset = if (periodIndex == INDEX_PM) HOURS_PER_PERIOD else 0
            (hourAt(hourIndex) % HOURS_PER_PERIOD) + offset
        }
        return LocalTime.of(hour, minuteAt(minuteIndex))
    }

    companion object {
        /** 午前の候補 index。 */
        const val INDEX_AM: Int = 0

        /** 午後の候補 index。 */
        const val INDEX_PM: Int = 1

        /** 24時間制の時候補の件数。 */
        private const val HOUR_COUNT_24: Int = 24

        /** 12時間制の時候補の件数。 */
        private const val HOUR_COUNT_12: Int = 12

        /** 分候補の件数。 */
        private const val MINUTE_COUNT: Int = 60

        /** 午前/午後候補の件数。 */
        private const val PERIOD_COUNT: Int = 2

        /** 半日の時間数（12時間制と24時間制の変換に使う）。 */
        private const val HOURS_PER_PERIOD: Int = 12

        /** 24時間制の [hour24] に対応する12時間制の表示時（0時・12時は 12 と表す）。 */
        private fun displayHourOf(hour24: Int): Int =
            if (hour24 % HOURS_PER_PERIOD == 0) HOURS_PER_PERIOD else hour24 % HOURS_PER_PERIOD

        /** [cell] の `is24Hour` から時制を決めて候補を組み立てる。 */
        fun of(cell: TimePickerCell, labels: TimeWheelLabels): TimeCandidates =
            TimeCandidates(is24Hour = cell.is24Hour, labels = labels)
    }
}

/**
 * [TimePickerCell] の選択面（ボトムシート + 時分ホイール）。
 *
 * 器は [NumberSelectionSheet] / [DateSelectionSheet] と同系で、「ドラッグハンドル + ヘッダー +
 * ホイール行」で構成する（android/ADR-0018。ヘッダーの意匠は android/ADR-0005 / ADR-0007 と共有）。
 * 中央の選択位置には全系列を横断する1本の accent 淡色帯を敷き、系列は等幅で並べる。
 *
 * 系列構成は cell の `is24Hour` から決まる（[TimeCandidates]）。24時間制では「時 / 分」の2連、
 * 12時間制では午前/午後を加えた3連になり、その並び順は端末 Locale の12時間表記に従う
 * （[orderedWheels]）。系列の名前は見た目には置かず、アクセシビリティへのみ公開する。
 *
 * 確定経路は確定ボタンだけで、そのとき選択中の時・分・（12時間制では）午前/午後から組み立てた
 * `LocalTime` を [onConfirmed] へ1回渡して閉じる。それ以外の閉じ方（取消ボタン・シート外側タップ・
 * Back 操作・下方向スワイプ）では callback を発火しない。構成変更（回転）では再提示しない
 * （シート系選択面の既存契約と同じ）。
 *
 * 器と中身はライブラリ所有の UI であり、ホストのテーマに関わらず同梱テーマをかぶせた Context から
 * 生成する（android/ADR-0020）。
 *
 * @param hostContext シートを表示する Context
 * @param sheetTitle ヘッダー中央に表示するタイトル
 * @param candidates cell の `is24Hour` から導いた時刻候補
 * @param initialTime 選択面を開いた時点の時刻（秒以下は切り捨てる）
 * @param sheetStyle 解決済みのスタイル値（強調色は Cell → CellStyle → Theme の段階解決済み）
 * @param onConfirmed 確定 callback（確定操作でのみ発火する）
 */
internal class TimeSelectionSheet(
    hostContext: Context,
    sheetTitle: String,
    internal val candidates: TimeCandidates,
    initialTime: LocalTime,
    private val sheetStyle: PickerSheetStyle,
    private val onConfirmed: (LocalTime) -> Unit,
) : BottomSheetDialog(hostContext.ksThemedContext()) {

    /** シート内容のルート（ドラッグハンドル + ヘッダー + ホイール行）。 */
    internal val contentRoot: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    /**
     * 選択中の時刻。
     *
     * 各系列の選択はスナップ静止時にのみ更新されるため、慣性移動の途中で確定しても採用されるのは
     * 直前に静止した候補から組み立てた時刻になる。
     */
    internal var selectedTime: LocalTime = initialTime.truncatedTo(ChronoUnit.MINUTES)
        private set

    /** 時ホイール。 */
    internal val hourWheel: KsWheelView = buildWheel(
        itemCount = candidates.hourCount,
        displayTextAt = candidates::hourTextAt,
        initialIndex = candidates.hourIndexOf(selectedTime),
        seriesLabel = candidates.labels.hourName,
    )

    /** 分ホイール。 */
    internal val minuteWheel: KsWheelView = buildWheel(
        itemCount = candidates.minuteCount,
        displayTextAt = candidates::minuteTextAt,
        initialIndex = candidates.minuteIndexOf(selectedTime),
        seriesLabel = candidates.labels.minuteName,
    )

    /** 午前/午後ホイール（24時間制では生成しない）。 */
    internal val periodWheel: KsWheelView? = if (candidates.is24Hour) {
        null
    } else {
        buildWheel(
            itemCount = candidates.periodCount,
            displayTextAt = candidates::periodTextAt,
            initialIndex = candidates.periodIndexOf(selectedTime),
            seriesLabel = candidates.labels.periodName,
        )
    }

    /**
     * 提示順に並べたホイール。
     *
     * 24時間制では「時 / 分」。12時間制では午前/午後の位置を端末 Locale の12時間表記から決め、
     * 午前/午後が先に来る Locale では「午前/午後 / 時 / 分」、後に来る Locale では
     * 「時 / 分 / 午前/午後」になる。
     */
    internal val orderedWheels: List<KsWheelView> =
        if (periodWheel != null && candidates.labels.isPeriodLeading) {
            listOf(periodWheel, hourWheel, minuteWheel)
        } else {
            listOfNotNull(hourWheel, minuteWheel, periodWheel)
        }

    /** ヘッダー（取消 / タイトル / 確定）。 */
    internal val headerView: SheetHeaderView = SheetHeaderView(
        context = context,
        style = sheetStyle,
        title = sheetTitle,
        showConfirm = true,
        onCancel = { cancel() },
        onConfirm = { confirmSelection() },
    )

    /** ヘッダー左の取消ラベル。 */
    internal val cancelView: TextView get() = headerView.cancelView

    /** ヘッダー中央のタイトル。 */
    internal val titleView: TextView get() = headerView.titleView

    /** ヘッダー右の確定ラベル。 */
    internal val confirmView: TextView get() = headerView.confirmView

    /** ヘッダー左のスロット（取消ラベルの当たり判定を担う）。 */
    internal val cancelSlot: FrameLayout get() = headerView.cancelSlot

    /** ヘッダー右のスロット（確定ラベルの当たり判定を担う）。 */
    internal val confirmSlot: FrameLayout get() = headerView.confirmSlot

    init {
        hourWheel.onSelectionChanged = { onSeriesChanged(TimeWheelSeries.HOUR) }
        minuteWheel.onSelectionChanged = { onSeriesChanged(TimeWheelSeries.MINUTE) }
        periodWheel?.onSelectionChanged = { onSeriesChanged(TimeWheelSeries.PERIOD) }

        contentRoot.addView(buildSheetDragHandle(context, sheetStyle.separatorColor))
        contentRoot.addView(headerView)
        contentRoot.addView(buildDivider())
        contentRoot.addView(buildWheelRow())
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
        // 中央の帯は系列を横断する1本を親側で敷くため、各ホイールには持たせない。
        showsBand = false,
    )

    /**
     * ホイール行を構築する。
     *
     * 中央の選択位置を示す帯を1本だけ敷き、その上に等幅の系列（2連 / 3連）を載せる。
     *
     * 列の並びは [orderedWheels] が決める。`LinearLayout` は RTL の Locale では子を右から左へ
     * 並べるため、行の [android.view.View.setLayoutDirection] を LTR に固定して、どの Locale でも
     * 先頭の系列が左に来るようにする（[DateSelectionSheet] と同じ扱い）。
     *
     * 行そのものを「自分でスクロールを扱う領域」として宣言する（`isNestedScrollingEnabled`）。
     * `BottomSheetBehavior` はシート内で最初に見つけたスクロールする子だけを対象に判定するため、
     * 宣言が無いと2列目以降のホイールを下方向へ操作したときにシートの dismiss になってしまう。
     */
    private fun buildWheelRow(): View {
        val wheels = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )
            orderedWheels.forEach { wheel ->
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

    /** 全系列を横断する中央の選択位置の帯。 */
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

    /** ヘッダーとホイールの境界に引く区切り線。 */
    private fun buildDivider(): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            DIVIDER_THICKNESS_PX,
        )
        setBackgroundColor(sheetStyle.separatorColor)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /** ホイール下端とシート下端のあいだの余白。 */
    private fun buildBottomPadding(): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            context.sheetDp(BOTTOM_PADDING_DP),
        )
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    // MARK: - 選択

    /**
     * 系列の選択が変わったときの処理。
     *
     * 時刻の各系列は互いに独立（日付の年→月→日のような候補の従属が無い）なので、候補の差し替えも
     * 他系列への再スクロールも要らない。現在の各系列の選択から時刻を組み立て直すだけでよい。
     *
     * @param series 変更の起点になった系列（現状は分岐に使わないが、経路を追える形で受け取る）
     */
    @Suppress("UNUSED_PARAMETER")
    private fun onSeriesChanged(series: TimeWheelSeries) {
        selectedTime = candidates.timeOf(
            hourIndex = hourWheel.selectedIndex,
            minuteIndex = minuteWheel.selectedIndex,
            periodIndex = periodWheel?.selectedIndex ?: TimeCandidates.INDEX_AM,
        )
    }

    /**
     * 確定操作。その時点で選択中の時・分・（12時間制では）午前/午後から組み立てた時刻を
     * 1回だけ通知して閉じる。
     */
    private fun confirmSelection() {
        onConfirmed(selectedTime)
        dismiss()
    }

    companion object {
        /** 区切り線の太さ（1 物理 pixel 固定。dp 換算しない）。 */
        private const val DIVIDER_THICKNESS_PX: Int = 1

        /** ホイール下の余白（dp）。[NumberSelectionSheet] と同値。 */
        private const val BOTTOM_PADDING_DP: Float = 18f
    }
}
