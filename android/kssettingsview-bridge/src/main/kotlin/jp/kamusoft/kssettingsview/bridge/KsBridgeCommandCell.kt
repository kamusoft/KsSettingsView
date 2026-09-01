package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.ui.CommandCell

/**
 * タップで処理を実行する Cell（`CommandCell`）を輸送する DTO。
 *
 * タップは [KsBridgeInteractionListener.commandCellTapped] で通知される。
 *
 * @property hideArrow Disclosure Indicator を非表示にするフラグ
 */
class KsBridgeCommandCell @JvmOverloads constructor(
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

    /** Disclosure Indicator を非表示にするフラグ */
    var hideArrow: Boolean = false

    @JvmSynthetic
    override fun makeCell(id: String, relay: KsBridgeInteractionRelay): Cell = CommandCell(
        id = id,
        style = resolvedStyle,
        title = title,
        description = descriptionText,
        valueText = valueText,
        icon = resolvedIcon,
        hintText = hintText,
        hideArrow = hideArrow,
        onTap = { relay.commandCellTapped(id) },
        isEnabled = isEnabled,
        isVisible = isVisible,
    )
}
