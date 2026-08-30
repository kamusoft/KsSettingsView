package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.ui.DatePickerCell
import jp.kamusoft.kssettingsview.ui.DatePickerUIStyle

/**
 * 日付を選ぶ Cell（`DatePickerCell`）を輸送する DTO。
 *
 * 日付は壁時計値として `"yyyy-MM-dd"` の文字列で運ぶ（maui/ADR-0012）。値変更は
 * [KsBridgeInteractionListener.datePickerCellChanged] で同じ書式で通知される。
 * 選択面の形式は統一 enum の序数で運び、未指定のときは Native 既定を使う（maui/ADR-0013）。
 */
class KsBridgeDatePickerCell @JvmOverloads constructor(
    title: String,
    descriptionText: String? = null,
    valueText: String? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
) : KsBridgeCell(
    title = title,
    descriptionText = descriptionText,
    valueText = valueText,
    hintText = hintText,
    isEnabled = isEnabled,
    isVisible = isVisible,
) {

    /** 現在の日付（`"yyyy-MM-dd"`） */
    var date: String = "1970-01-01"

    /** 表示フォーマット（`DateTimeFormatter.ofPattern` の書式、未指定は `null` で Native 既定） */
    var format: String? = null

    /** 選択できる最小日付（`"yyyy-MM-dd"`、未指定は `null`） */
    var minDate: String? = null

    /** 選択できる最大日付（`"yyyy-MM-dd"`、未指定は `null`） */
    var maxDate: String? = null

    /** 選択面のタイトル（未指定は `null`） */
    var pickerTitle: String? = null

    /** 選択強調色（ARGB、未指定は `null`） */
    var accentColor: Int? = null

    /** 選択面の形式の序数（`0 = Calendar / 1 = Wheels`、未指定は `null` で Native 既定） */
    var uiStyle: Int? = null

    /** Today ボタンの表示文字列（`null` または空で非表示） */
    var todayText: String? = null

    /** 選択面の OK / CANCEL 操作色（ARGB、未指定は `null`） */
    var androidButtonColor: Int? = null

    @JvmSynthetic
    override fun makeCell(id: String, relay: KsBridgeInteractionRelay): Cell = DatePickerCell(
        id = id,
        style = resolvedStyle,
        title = title,
        description = descriptionText,
        valueText = valueText,
        icon = resolvedIcon,
        hintText = hintText,
        date = KsBridgeValueTransport.date(date),
        format = format ?: DEFAULT_FORMAT,
        minDate = KsBridgeValueTransport.optionalDate(minDate),
        maxDate = KsBridgeValueTransport.optionalDate(maxDate),
        pickerTitle = pickerTitle,
        uiStyle = KsBridgeValueTransport.datePickerUIStyle(uiStyle) ?: DEFAULT_UI_STYLE,
        todayText = todayText,
        androidButtonColor = KsBridgeColor.color(androidButtonColor),
        accentColor = KsBridgeColor.color(accentColor),
        onValueChanged = { relay.datePickerCellChanged(id, it) },
        isEnabled = isEnabled,
        isVisible = isVisible,
    )

    private companion object {
        /**
         * 未指定の項目に使う `DatePickerCell` 側の既定値の取得元。
         *
         * 値を写し取らず Native の既定から引く（Native 側を変えたときに輸送側が古い既定を
         * 渡し続けることを防ぐ）。
         */
        private val NATIVE_DEFAULTS: DatePickerCell = DatePickerCell(title = "")

        /** [format] 未指定のときに使う `DatePickerCell` 側の既定表示フォーマット。 */
        private val DEFAULT_FORMAT: String = NATIVE_DEFAULTS.format

        /** [uiStyle] 未指定のときに使う `DatePickerCell` 側の既定形式。 */
        private val DEFAULT_UI_STYLE: DatePickerUIStyle = NATIVE_DEFAULTS.uiStyle
    }
}
