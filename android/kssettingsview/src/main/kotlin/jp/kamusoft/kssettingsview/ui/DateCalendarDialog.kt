package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** UTC の epoch ミリ秒を日付へ戻す（日単位の往復に使う）。 */
internal fun Long.toLocalDateUtc(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * カレンダー選択面（ダイアログの面）の高さの上限（dp）を、画面の高さから導く。
 *
 * 面が画面より高くなると、下端に置いた操作行が画面外へ落ちて確定・取消ができなくなる。
 * 影のための余白を差し引いた可視領域を上限とし、収まらない分はカレンダー側のスクロールで送る。
 *
 * @param screenHeightDp 画面の高さ（システムバーを除いた、アプリが使える高さ）
 * @param shadowMarginDp 面の外側へ確保する余白
 */
internal fun dateCalendarSurfaceMaxHeightDp(screenHeightDp: Int, shadowMarginDp: Float): Float =
    (screenHeightDp - shadowMarginDp * 2).coerceAtLeast(0f)

/**
 * カレンダー選択面が扱える日付の範囲（android/ADR-0019）。
 *
 * `minDate` / `maxDate` の未指定側には、ホイール型選択面（[DateCandidates]）と同じ既定
 * （[DateCandidates.DEFAULT_MIN_YEAR] 年の年初 / [DateCandidates.DEFAULT_MAX_YEAR] 年の年末）を
 * 適用する。両者の既定を揃えることで、同じ Cell 構成なら `uiStyle` によらず選べる日付が一致する。
 *
 * @property minDate 既定を適用した後の下限
 * @property maxDate 既定を適用した後の上限
 */
@OptIn(ExperimentalMaterial3Api::class)
internal class DateCalendarRange(
    val minDate: LocalDate,
    val maxDate: LocalDate,
) {

    /** 年選択に提示する年の範囲。 */
    val yearRange: IntRange = minDate.year..maxDate.year

    /** [date] が範囲に含まれるか（日単位の比較・境界日は有効）。 */
    fun contains(date: LocalDate): Boolean = !date.isBefore(minDate) && !date.isAfter(maxDate)

    /** [date] を範囲内へ丸める（範囲外なら最も近い範囲端）。 */
    fun clamp(date: LocalDate): LocalDate = when {
        date.isBefore(minDate) -> minDate
        date.isAfter(maxDate) -> maxDate
        else -> date
    }

    /**
     * カレンダー・テキスト入力の両モードが参照する選択可否。
     *
     * 日の可否は日単位で判定し、年の可否は [yearRange] で判定する。
     */
    val selectableDates: SelectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean =
            contains(utcTimeMillis.toLocalDateUtc())

        override fun isSelectableYear(year: Int): Boolean = year in yearRange
    }

    companion object {
        /** logcat 用タグ。 */
        private const val LOG_TAG = "DatePickerCell"

        /**
         * [cell] の `minDate` / `maxDate` から範囲を組み立てる。提示できない指定では `null` を返す。
         *
         * `minDate > maxDate`（利用者の設定ミス）では警告ログを残して `null` を返す。
         */
        fun of(cell: DatePickerCell): DateCalendarRange? {
            val min = cell.minDate ?: LocalDate.of(DateCandidates.DEFAULT_MIN_YEAR, 1, 1)
            val max = cell.maxDate ?: LocalDate.of(DateCandidates.DEFAULT_MAX_YEAR, 12, 31)
            if (min.isAfter(max)) {
                // silent failure はデバッグが困難なため、原因を切り分けられる粒度で警告ログを残す。
                val reason = if (cell.minDate != null && cell.maxDate != null) {
                    "invalid range: minDate=${cell.minDate} > maxDate=${cell.maxDate}"
                } else {
                    "empty range after applying defaults: effective min=$min > max=$max"
                }
                Log.w(
                    LOG_TAG,
                    "DatePickerCell(id=${cell.id}) has $reason. Calendar dialog will not be shown.",
                )
                return null
            }
            return DateCalendarRange(minDate = min, maxDate = max)
        }
    }
}

/**
 * カレンダー選択面の表示状態。
 *
 * 構成変更をまたいで選択面を持ち回すために必要な最小の情報だけを持つ（android/ADR-0019）。
 * 日付は端末タイムゾーンに依存しない日単位の値で保持する。
 *
 * @property selectedDate 選択中の日付（テキスト入力が未確定・不正で定まらないときは `null`）
 * @property displayedMonth 表示中の月（その月の初日として持つ）
 * @property isTextInput テキスト入力を表示中か（`false` はカレンダー表示）
 */
internal data class DateCalendarDisplayState(
    val selectedDate: LocalDate?,
    val displayedMonth: LocalDate,
    val isTextInput: Boolean,
) {

    /**
     * 選択日と表示月を [range] の内側へ丸めた状態を返す。
     *
     * 保存してから提示し直すまでの間に Cell の `minDate` / `maxDate` が変わっていても、
     * 提示する日付が範囲外にならないようにする。
     */
    fun clampedTo(range: DateCalendarRange): DateCalendarDisplayState = copy(
        selectedDate = selectedDate?.let { range.clamp(it) },
        displayedMonth = range.clamp(displayedMonth).withDayOfMonth(1),
    )
}

/**
 * [DatePickerCell] の [DatePickerUIStyle.Material] が使う選択面（カレンダーダイアログ）。
 *
 * 器は `ComponentDialog` + `ComposeView` で、中身は Compose Material3 の `DatePicker`（カレンダー /
 * テキスト入力の切替つき）と、その下の操作行（「今日」/ 取消 / 確定）で構成する（android/ADR-0019）。
 * `DialogFragment` を使わないため、ホスト Activity の型に前提を置かない。
 *
 * 器と中身はライブラリ所有の UI であり、ホストのテーマに関わらず同梱テーマをかぶせた Context から
 * 生成する（android/ADR-0020）。Compose 側の配色は色ロール（[PickerDialogColors]）で明示的に与え、
 * 対応表に現れない細部だけを Material3 の既定に任せる。
 *
 * 確定経路は確定操作だけで、そのとき選択中の日付を [onConfirmed] へ1回渡して閉じる。それ以外の
 * 閉じ方（取消操作・ダイアログ外側のタップ・Back 操作）では callback を発火しない。
 *
 * ダイアログ内の Compose からは `viewModel()` 系を使わない。`ComponentDialog` は `ViewTree` の
 * lifecycle / savedStateRegistry の所有者は供給するが、`ViewModelStore` の所有者は供給しない。
 *
 * @param hostContext ダイアログを表示する Context
 * @param dialogTitle ヘッダーに表示するタイトル
 * @param range 選択できる日付の範囲
 * @param initialDate 開いた時点で選択中にする日付（範囲内へ丸めた値を渡す）
 * @param todayText 「今日」操作のラベル（`null` で操作自体を提示しない）
 * @param today 「今日」の取得元（端末タイムゾーンの今日）
 * @param colors 解決済みの色ロール
 * @param restoredState 構成変更をまたいで引き継ぐ表示状態（`null` なら [initialDate] から開く）
 * @param onConfirmed 確定 callback（確定操作でのみ発火する）
 */
@OptIn(ExperimentalMaterial3Api::class)
internal class DateCalendarDialog(
    hostContext: Context,
    internal val dialogTitle: String,
    private val range: DateCalendarRange,
    initialDate: LocalDate,
    internal val todayText: String?,
    private val today: () -> LocalDate,
    internal val colors: PickerDialogColors,
    restoredState: DateCalendarDisplayState? = null,
    private val onConfirmed: (LocalDate) -> Unit,
) : ComponentDialog(hostContext.ksThemedContext()) {

    /**
     * カレンダーの選択状態。
     *
     * コンポジションの外で作って保持する。「今日」ジャンプはこの状態を書き換えるだけで成立し、
     * 表示中の View 階層を駆動する必要がない。構成変更をまたいだ提示（[restoredState] 指定）でも
     * 同じ状態を初期値ごと組み立てるため、復元後の操作も通常の提示とまったく同じ経路を通る。
     */
    internal val state: DatePickerState = DatePickerState(
        locale = context.primaryLocale(),
        initialSelectedDateMillis = if (restoredState != null) {
            restoredState.selectedDate?.toEpochMilliUtc()
        } else {
            initialDate.toEpochMilliUtc()
        },
        initialDisplayedMonthMillis =
        (restoredState?.displayedMonth ?: initialDate.withDayOfMonth(1)).toEpochMilliUtc(),
        yearRange = range.yearRange,
        initialDisplayMode = if (restoredState?.isTextInput == true) DisplayMode.Input else DisplayMode.Picker,
        selectableDates = range.selectableDates,
    )

    init {
        setContentView(
            ComposeView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                setContent { DialogContent() }
            },
        )
        // 面の描画は Compose 側の Surface が担うため、window 自身の背景は透過にする。
        window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        // ダイアログ外側のタップで閉じられる（確定 callback は発火しない）。
        setCanceledOnTouchOutside(true)
    }

    override fun onStart() {
        super.onStart()
        // window の幅を明示する。既定の内容合わせでは window 側の制限で幅が縮み、カレンダーの
        // 最終列（曜日1列分）とヘッダの日付が切れてしまう。画面が狭い端末では画面幅に収める。
        val metrics = context.resources.displayMetrics
        val desired = ((MAX_WIDTH_DP + SHADOW_MARGIN_DP * 2) * metrics.density).toInt()
        val available = (metrics.widthPixels * MAX_WIDTH_SCREEN_FRACTION).toInt()
        window?.setLayout(minOf(desired, available), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    /** 現在の表示状態を取り出す（構成変更をまたいで持ち回すため）。 */
    internal fun displayState(): DateCalendarDisplayState = DateCalendarDisplayState(
        selectedDate = state.selectedDateMillis?.toLocalDateUtc(),
        displayedMonth = state.displayedMonthMillis.toLocalDateUtc(),
        isTextInput = state.displayMode == DisplayMode.Input,
    )

    // MARK: - 操作

    /**
     * 選択日と表示月を今日へ移動する。
     *
     * 今日が範囲外なら選択状態を変えない。テキスト入力を表示中でもカレンダー表示へ戻して成立させる。
     * 状態を書き換えるだけであり、確定 callback は発火しない。
     */
    internal fun jumpToToday() {
        val target = today()
        if (!range.contains(target)) return
        state.selectedDateMillis = target.toEpochMilliUtc()
        state.displayedMonthMillis = target.withDayOfMonth(1).toEpochMilliUtc()
        state.displayMode = DisplayMode.Picker
    }

    /**
     * 確定操作。その時点で選択中の日付を1回だけ通知して閉じる。
     *
     * テキスト入力が未確定・不正で選択日が定まらない場合は何もしない（操作自体も無効表示になる）。
     */
    internal fun confirmSelection() {
        val selected = state.selectedDateMillis ?: return
        onConfirmed(selected.toLocalDateUtc())
        dismiss()
    }

    // MARK: - 内容

    @Composable
    private fun DialogContent() {
        MaterialTheme(colorScheme = dialogColorScheme()) {
            // 画面が低いとき（横向きなど）に面がはみ出すと操作行が画面外へ落ちるため、面の高さを
            // 可視領域で頭打ちにする。上限は Configuration から導き、構成変更にそのまま追随する。
            val surfaceMaxHeight = dateCalendarSurfaceMaxHeightDp(
                screenHeightDp = LocalConfiguration.current.screenHeightDp,
                shadowMarginDp = SHADOW_MARGIN_DP,
            ).dp
            Box(modifier = Modifier.padding(SHADOW_MARGIN_DP.dp)) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = MAX_WIDTH_DP.dp)
                        .heightIn(max = surfaceMaxHeight),
                    shape = RoundedCornerShape(CORNER_RADIUS_DP.dp),
                    color = Color(colors.background),
                    // 面の色は色ロールそのものを出す（明度を足す色調はかけない）。
                    tonalElevation = 0.dp,
                    shadowElevation = SHADOW_ELEVATION_DP.dp,
                ) {
                    Column {
                        Box(
                            // 高さが足りないときに縮むのはカレンダー側だけにする。`fill = false` に
                            // より、収まる高さでは従来どおり内容の自然な高さで並ぶ。
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            DatePicker(
                                state = state,
                                title = { DialogTitle() },
                                dateFormatter = DatePickerDefaults.dateFormatter(
                                    selectedDateSkeleton = SELECTED_DATE_SKELETON,
                                ),
                                showModeToggle = true,
                                colors = datePickerColors(),
                            )
                        }
                        ActionRow()
                    }
                }
            }
        }
    }

    @Composable
    private fun DialogTitle() {
        Text(
            text = dialogTitle,
            modifier = Modifier.padding(
                start = TITLE_PADDING_START_DP.dp,
                end = TITLE_PADDING_END_DP.dp,
                top = TITLE_PADDING_TOP_DP.dp,
            ),
        )
    }

    /**
     * 操作行。「今日」は左端に置き、`todayText` が指定されたときだけ現れる。
     * 右側は取消・確定の順で、現行のカレンダー選択面と同じ並びにする。
     */
    @Composable
    private fun ActionRow() {
        val actionColors = ButtonDefaults.textButtonColors(
            contentColor = Color(colors.accent),
            disabledContentColor = Color(colors.disabledAccent),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = ACTION_ROW_PADDING_HORIZONTAL_DP.dp,
                    end = ACTION_ROW_PADDING_HORIZONTAL_DP.dp,
                    bottom = ACTION_ROW_PADDING_BOTTOM_DP.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            todayText?.let { label ->
                TextButton(onClick = { jumpToToday() }, colors = actionColors) { Text(label) }
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { cancel() }, colors = actionColors) {
                Text(context.getString(android.R.string.cancel))
            }
            TextButton(
                onClick = { confirmSelection() },
                // 選択日が定まらない状態では確定できない。
                enabled = state.selectedDateMillis != null,
                colors = actionColors,
            ) {
                Text(context.getString(android.R.string.ok))
            }
        }
    }

    /**
     * 配色体系の土台を色ロールから組み立てる。
     *
     * 部位ごとの指定（[datePickerColors]）が届かない細部 — リップル・文字選択のハンドル・
     * 区切り線など — も色ロールの近傍に収まるようにする。明暗の土台は端末の夜間モードに追随し、
     * 同梱テーマ（DayNight）と同じ側を向く。
     */
    @Composable
    private fun dialogColorScheme() = with(
        if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
    ) {
        copy(
            primary = Color(colors.accent),
            onPrimary = Color(colors.onAccent),
            surface = Color(colors.background),
            onSurface = Color(colors.text),
            surfaceVariant = Color(colors.background),
            onSurfaceVariant = Color(colors.text),
            background = Color(colors.background),
            onBackground = Color(colors.text),
            outline = Color(colors.disabledText),
            outlineVariant = Color(colors.disabledText),
        )
    }

    /**
     * 色ロールを Material3 の配色へ写像する。
     *
     * - 背景ロール: ダイアログの面（カレンダー・年選択・テキスト入力の各面）
     * - 強調ロール: 選択日の塗り・今日の枠・年選択の選択状態・入力欄の枠とキャレット
     * - 通常文字ロール: ヘッダ・曜日・日付数字・年月表示・入力ラベル（範囲外はアルファを落とす）
     * - アクセント上文字ロール: 選択日の数字・選択中の年の文字
     *
     * ここに現れない部位は Material3 の既定のままにする。
     */
    @Composable
    private fun datePickerColors(): DatePickerColors {
        val background = Color(colors.background)
        val accent = Color(colors.accent)
        val onAccent = Color(colors.onAccent)
        val text = Color(colors.text)
        val disabledText = Color(colors.disabledText)
        val disabledAccent = Color(colors.disabledAccent)
        val subduedText = Color(colors.subduedText)
        return DatePickerDefaults.colors(
            containerColor = background,
            titleContentColor = text,
            headlineContentColor = text,
            weekdayContentColor = text,
            subheadContentColor = text,
            navigationContentColor = text,
            yearContentColor = text,
            disabledYearContentColor = disabledText,
            currentYearContentColor = accent,
            selectedYearContentColor = onAccent,
            disabledSelectedYearContentColor = disabledText,
            selectedYearContainerColor = accent,
            disabledSelectedYearContainerColor = disabledAccent,
            dayContentColor = text,
            disabledDayContentColor = disabledText,
            selectedDayContentColor = onAccent,
            disabledSelectedDayContentColor = disabledText,
            selectedDayContainerColor = accent,
            disabledSelectedDayContainerColor = disabledAccent,
            todayContentColor = text,
            todayDateBorderColor = accent,
            dateTextFieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = text,
                unfocusedTextColor = text,
                disabledTextColor = disabledText,
                cursorColor = accent,
                focusedBorderColor = accent,
                unfocusedBorderColor = disabledText,
                disabledBorderColor = disabledText,
                focusedLabelColor = text,
                unfocusedLabelColor = text,
                disabledLabelColor = disabledText,
                focusedSupportingTextColor = subduedText,
                unfocusedSupportingTextColor = subduedText,
            ),
        )
    }

    private companion object {
        /**
         * ヘッダの選択日表示に使う日付の骨格。
         *
         * 年・月・日に曜日の略称を添える表記で、表記の並びと区切りは Locale から解決される。
         */
        const val SELECTED_DATE_SKELETON: String = "yMMMEd"

        /** ダイアログの面の最大幅（dp）。Material3 のカレンダーの実寸に合わせる。 */
        const val MAX_WIDTH_DP: Float = 360f

        /** ダイアログの面の角丸半径（dp）。 */
        const val CORNER_RADIUS_DP: Float = 28f

        /** ダイアログの面が落とす影の高さ（dp）。 */
        const val SHADOW_ELEVATION_DP: Float = 6f

        /** 影が切れないように面の外側へ確保する余白（dp）。 */
        const val SHADOW_MARGIN_DP: Float = 12f

        /**
         * 画面幅に対して window が占めてよい比率。
         *
         * 画面が [MAX_WIDTH_DP] より狭い端末で、ダイアログの左右にモーダルの背景が見える余地を残す。
         */
        const val MAX_WIDTH_SCREEN_FRACTION: Float = 0.96f

        /** タイトルの左余白（dp）。Material3 のヘッダ内の余白に合わせる。 */
        const val TITLE_PADDING_START_DP: Float = 24f

        /** タイトルの右余白（dp）。 */
        const val TITLE_PADDING_END_DP: Float = 12f

        /** タイトルの上余白（dp）。 */
        const val TITLE_PADDING_TOP_DP: Float = 16f

        /** 操作行の左右余白（dp）。 */
        const val ACTION_ROW_PADDING_HORIZONTAL_DP: Float = 8f

        /** 操作行の下余白（dp）。 */
        const val ACTION_ROW_PADDING_BOTTOM_DP: Float = 8f
    }
}
