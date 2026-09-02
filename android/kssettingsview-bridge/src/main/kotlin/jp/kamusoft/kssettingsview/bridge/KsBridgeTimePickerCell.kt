package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.ui.TimePickerCell

/**
 * 時刻を選ぶ Cell（`TimePickerCell`）を輸送する DTO。
 *
 * 時刻は壁時計値として `"HH:mm"` の文字列で運ぶ（maui/ADR-0012）。値変更は
 * [KsBridgeInteractionListener.timePickerCellChanged] で同じ書式で通知される。
 */
class KsBridgeTimePickerCell @JvmOverloads constructor(
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

    /** 現在の時刻（`"HH:mm"`） */
    var time: String = "00:00"

    /** 表示フォーマット（`DateTimeFormatter.ofPattern` の書式、未指定は `null` で Native 既定） */
    var format: String? = null

    /** 選択面の時制（`true` = 24時間制 / `false` = 12時間制、未指定は `null` で Native 既定） */
    var is24Hour: Boolean? = null

    /** 選択面のタイトル（未指定は `null`） */
    var pickerTitle: String? = null

    /** 選択強調色（ARGB、未指定は `null`） */
    var accentColor: Int? = null

    @JvmSynthetic
    override fun makeCell(id: String, relay: KsBridgeInteractionRelay): Cell = TimePickerCell(
        id = id,
        style = resolvedStyle,
        title = title,
        description = descriptionText,
        valueText = valueText,
        icon = resolvedIcon,
        hintText = hintText,
        time = KsBridgeValueTransport.time(time),
        format = format ?: DEFAULT_FORMAT,
        is24Hour = is24Hour ?: DEFAULT_IS_24_HOUR,
        pickerTitle = pickerTitle,
        accentColor = KsBridgeColor.color(accentColor),
        onValueChanged = { relay.timePickerCellChanged(id, it) },
        isEnabled = isEnabled,
        isVisible = isVisible,
    )

    private companion object {
        /**
         * [format] 未指定のときに使う `TimePickerCell` 側の既定表示フォーマット。
         *
         * 値を写し取らず Native の既定から引く（Native 側を変えたときに輸送側が古い既定を
         * 渡し続けることを防ぐ）。
         */
        private val DEFAULT_FORMAT: String = TimePickerCell(title = "").format

        /**
         * [is24Hour] 未指定のときに使う `TimePickerCell` 側の既定時制。
         *
         * [DEFAULT_FORMAT] と同じく Native の既定から引く。
         */
        private val DEFAULT_IS_24_HOUR: Boolean = TimePickerCell(title = "").is24Hour
    }
}
