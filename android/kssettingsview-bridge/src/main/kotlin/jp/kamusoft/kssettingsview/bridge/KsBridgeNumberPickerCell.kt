package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.ui.NumberPickerCell

/**
 * 数値を選ぶ Cell（`NumberPickerCell`）を輸送する DTO。
 *
 * 値変更は [KsBridgeInteractionListener.numberPickerCellChanged] で通知される。
 */
class KsBridgeNumberPickerCell @JvmOverloads constructor(
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

    /** 選択できる最小値 */
    var min: Int = 0

    /** 選択できる最大値 */
    var max: Int = 100

    /** 選択の刻み幅 */
    var step: Int = 1

    /** 現在の値 */
    var value: Int = 0

    /** 値に付ける単位文字列（空文字列で単位なし） */
    var unit: String = ""

    /** 選択面のタイトル（未指定は `null`） */
    var pickerTitle: String? = null

    /** 選択強調色（ARGB、未指定は `null`） */
    var accentColor: Int? = null

    @JvmSynthetic
    override fun makeCell(id: String, relay: KsBridgeInteractionRelay): Cell = NumberPickerCell(
        id = id,
        style = resolvedStyle,
        title = title,
        description = descriptionText,
        valueText = valueText,
        icon = resolvedIcon,
        hintText = hintText,
        min = min,
        max = max,
        step = step,
        value = value,
        unit = unit,
        pickerTitle = pickerTitle,
        accentColor = KsBridgeColor.color(accentColor),
        onValueChanged = { relay.numberPickerCellChanged(id, it) },
        isEnabled = isEnabled,
        isVisible = isVisible,
    )
}
