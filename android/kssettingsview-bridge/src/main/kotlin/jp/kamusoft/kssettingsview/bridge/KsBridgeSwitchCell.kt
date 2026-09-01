package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.ui.SwitchCell

/**
 * ON/OFF スイッチを持つ Cell（`SwitchCell`）を輸送する DTO。
 *
 * 値変更は [KsBridgeInteractionListener.switchCellChanged] で通知される。
 */
class KsBridgeSwitchCell @JvmOverloads constructor(
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

    /** 現在の ON/OFF 値 */
    var isOn: Boolean = false

    /** スイッチ ON 時の色（ARGB、未指定は `null`） */
    var accentColor: Int? = null

    @JvmSynthetic
    override fun makeCell(id: String, relay: KsBridgeInteractionRelay): Cell = SwitchCell(
        id = id,
        style = resolvedStyle,
        title = title,
        description = descriptionText,
        valueText = valueText,
        icon = resolvedIcon,
        hintText = hintText,
        isOn = isOn,
        accentColor = KsBridgeColor.color(accentColor),
        onValueChanged = { relay.switchCellChanged(id, it) },
        isEnabled = isEnabled,
        isVisible = isVisible,
    )
}
