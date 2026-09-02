package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.ui.SimpleCheckCell

/**
 * 行全体のタップでチェックを切り替える Cell（`SimpleCheckCell`）を輸送する DTO。
 *
 * 値変更は [KsBridgeInteractionListener.simpleCheckCellChanged] で通知される。
 */
class KsBridgeSimpleCheckCell @JvmOverloads constructor(
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

    /** チェック状態 */
    var isChecked: Boolean = false

    /** チェックマーク色（ARGB、未指定は `null`） */
    var accentColor: Int? = null

    @JvmSynthetic
    override fun makeCell(id: String, relay: KsBridgeInteractionRelay): Cell = SimpleCheckCell(
        id = id,
        style = resolvedStyle,
        title = title,
        description = descriptionText,
        valueText = valueText,
        icon = resolvedIcon,
        hintText = hintText,
        isChecked = isChecked,
        accentColor = KsBridgeColor.color(accentColor),
        onValueChanged = { relay.simpleCheckCellChanged(id, it) },
        isEnabled = isEnabled,
        isVisible = isVisible,
    )
}
