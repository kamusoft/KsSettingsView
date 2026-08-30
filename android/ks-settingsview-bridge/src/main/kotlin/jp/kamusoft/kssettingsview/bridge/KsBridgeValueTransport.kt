package jp.kamusoft.kssettingsview.bridge

import android.text.InputType
import jp.kamusoft.kssettingsview.core.CellTitleAlignment
import jp.kamusoft.kssettingsview.ui.DatePickerUIStyle
import jp.kamusoft.kssettingsview.ui.PickerItem
import jp.kamusoft.kssettingsview.ui.PickerSelectionMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

/**
 * interop 境界の値表現を Native 型へ解釈し、通知方向では Native 型を境界表現へ戻す。
 *
 * 二値は Boolean、数値と選択 index は Int、文字列は String をそのまま渡す。壁時計値である時刻と
 * 日付は、タイムゾーンを含まない固定書式の ISO-8601 文字列で運ぶ（maui/ADR-0012）。enum は
 * 序数の Int で運び、`null`（欠落）は「未指定 → Native 既定」を意味する。
 *
 * 解釈できない文字列・未知の序数は例外にせず、Native 既定または型の既定値へ倒す。書式は
 * 区切り文字・桁数・暦日の妥当性まで厳密に判定するため、書式から外れた入力の結果は
 * platform をまたいで同一になる。生成側は facade と Bridge の双方が固定書式で書くため、
 * 解釈失敗は契約違反であり診断出力のみを行う。
 */
internal object KsBridgeValueTransport {

    /** 時刻の輸送書式（壁時計値・culture 非依存）。 */
    const val TIME_FORMAT: String = "HH:mm"

    /** 日付の輸送書式（壁時計値・culture 非依存）。 */
    const val DATE_FORMAT: String = "yyyy-MM-dd"

    /**
     * 日付の解釈・生成に用いるパターン。
     *
     * 厳密解決では `yyyy`（era 付きの年）が era の指定を要求するため、era を伴わない `uuuu` を
     * 使う。西暦の正の年に対する並びは輸送書式 [DATE_FORMAT] と同一になる。
     */
    private const val DATE_PATTERN: String = "uuuu-MM-dd"

    /**
     * 時刻の輸送書式に対する厳密な formatter。
     *
     * 桁数不足（`9:05`）や区切り文字違いは解釈失敗として扱う。
     */
    private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter
        .ofPattern(TIME_FORMAT, Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT)

    /**
     * 日付の輸送書式に対する厳密な formatter。
     *
     * 桁数不足（`2026-8-10`）・区切り文字違い（`2026/08/10`）に加えて、暦上存在しない日
     * （`2026-02-30`）も解釈失敗として扱う。
     */
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter
        .ofPattern(DATE_PATTERN, Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT)

    // MARK: - 時刻 / 日付

    /**
     * 輸送書式の時刻文字列を `LocalTime` へ解釈する。解釈できない場合は 00:00 で構築する。
     *
     * @param text `"HH:mm"` 形式の時刻文字列
     */
    fun time(text: String): LocalTime = parseTime(text) ?: run {
        diagnose(kind = "時刻", text = text, format = TIME_FORMAT)
        LocalTime.MIDNIGHT
    }

    /**
     * 輸送書式の日付文字列を `LocalDate` へ解釈する。解釈できない場合は 1970-01-01 で構築する。
     *
     * @param text `"yyyy-MM-dd"` 形式の日付文字列
     */
    fun date(text: String): LocalDate = parseDate(text) ?: run {
        diagnose(kind = "日付", text = text, format = DATE_FORMAT)
        EPOCH_DATE
    }

    /**
     * 未指定を許す日付文字列を `LocalDate?` へ解釈する。
     *
     * `null` は未指定をそのまま表し、解釈できない文字列も未指定（`null` = 型の既定値）へ倒す。
     *
     * @param text `"yyyy-MM-dd"` 形式の日付文字列（未指定は `null`）
     */
    fun optionalDate(text: String?): LocalDate? {
        if (text == null) return null
        return parseDate(text) ?: run {
            diagnose(kind = "日付", text = text, format = DATE_FORMAT)
            null
        }
    }

    /**
     * `LocalTime` を輸送書式の文字列へ変換する。
     *
     * @param time 変換対象
     */
    fun timeText(time: LocalTime): String = TIME_FORMATTER.format(time)

    /**
     * `LocalDate` を輸送書式の文字列へ変換する。
     *
     * @param date 変換対象
     */
    fun dateText(date: LocalDate): String = DATE_FORMATTER.format(date)

    // MARK: - enum の序数

    /**
     * keyboard 種別の序数を `android.text.InputType` の定数へ変換する。未知の序数は既定へ倒す。
     *
     * 序数は facade の正規化 enum に対応する:
     * `0 = Default / 1 = Plain / 2 = Text / 3 = Chat / 4 = Url / 5 = Email / 6 = Numeric /
     * 7 = Telephone`。Android のキーボード種別に対応概念のない値（Plain / Text / Chat）は
     * 通常のテキスト入力へ写す。
     *
     * @param ordinal keyboard 種別の序数
     */
    fun keyboardType(ordinal: Int): Int = when (ordinal) {
        4 -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        5 -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        6 -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        7 -> InputType.TYPE_CLASS_PHONE
        else -> InputType.TYPE_CLASS_TEXT
    }

    /**
     * DatePicker の UI スタイル序数を `DatePickerUIStyle` へ変換する。
     *
     * 序数は `0 = Calendar / 1 = Wheels`（maui/ADR-0013）。`null` と未知の序数は「未指定」を表す
     * `null` を返し、呼び出し側が Native 既定を適用する。
     *
     * @param ordinal UI スタイルの序数（未指定は `null`）
     */
    fun datePickerUIStyle(ordinal: Int?): DatePickerUIStyle? = when (ordinal) {
        0 -> DatePickerUIStyle.Material
        1 -> DatePickerUIStyle.Spinner
        else -> null
    }

    /**
     * テキスト配置の序数を `CellTitleAlignment` へ変換する。
     *
     * 序数は `0 = Start / 1 = Center / 2 = End`。`null` と未知の序数は [fallback] を返す。
     *
     * @param ordinal 配置の序数（未指定は `null`）
     * @param fallback 未指定・未知のときに使う配置（Native 既定）
     */
    fun titleAlignment(ordinal: Int?, fallback: CellTitleAlignment): CellTitleAlignment =
        when (ordinal) {
            0 -> CellTitleAlignment.START
            1 -> CellTitleAlignment.CENTER
            2 -> CellTitleAlignment.END
            else -> fallback
        }

    /**
     * Picker の選択モード序数を `PickerSelectionMode` へ変換する。
     *
     * 序数は `0 = Single / 1 = Multiple`。未知の序数は単一選択へ倒す。
     *
     * @param ordinal 選択モードの序数
     */
    fun selectionMode(ordinal: Int): PickerSelectionMode =
        if (ordinal == 1) PickerSelectionMode.Multiple else PickerSelectionMode.Single

    // MARK: - Picker の候補

    /**
     * 輸送 DTO の候補列を Native の候補列へ変換する。
     *
     * 表示射影は上位層で適用済みのため、ここでは主表示と副表示をそのまま写す。副表示の空文字列を
     * 「なし」へ揃えるのは [PickerItem] 側の責務。
     *
     * @param items 輸送 DTO の候補列
     */
    fun pickerItems(items: List<KsBridgePickerItem>): List<PickerItem> =
        items.map { PickerItem(text = it.text, subText = it.subText) }

    // MARK: - 選択 index

    /**
     * 複数選択 index の配列を Native の集合へ変換する。
     *
     * 重複は集合化で除去される。範囲外の index は正規化せず透過する（Native の「モデル値を
     * 正規化しない」契約に従う）。
     *
     * @param indices 選択 index の配列
     */
    fun indexSet(indices: IntArray): Set<Int> = indices.toSet()

    /**
     * Native の選択 index 集合を輸送表現（昇順・重複なしの配列）へ変換する。
     *
     * @param indices 選択 index の集合
     */
    fun indexList(indices: Set<Int>): IntArray = indices.sorted().toIntArray()

    // MARK: - 内部

    /** 日付の型の既定値。解釈できない必須日付はこの値で構築する。 */
    private val EPOCH_DATE: LocalDate = LocalDate.of(1970, 1, 1)

    /** 輸送書式の文字列を `LocalTime` へ解釈する。書式から外れた入力は `null`。 */
    private fun parseTime(text: String): LocalTime? = try {
        LocalTime.parse(text, TIME_FORMATTER)
    } catch (_: DateTimeParseException) {
        null
    }

    /** 輸送書式の文字列を `LocalDate` へ解釈する。書式から外れた入力は `null`。 */
    private fun parseDate(text: String): LocalDate? = try {
        LocalDate.parse(text, DATE_FORMATTER)
    } catch (_: DateTimeParseException) {
        null
    }

    /** 解釈失敗を診断出力する。 */
    private fun diagnose(kind: String, text: String, format: String) {
        android.util.Log.w(
            "KsSettingsViewBridge",
            "${kind}文字列 '$text' が輸送書式 '$format' に一致しないため既定値で構築します",
        )
    }
}
