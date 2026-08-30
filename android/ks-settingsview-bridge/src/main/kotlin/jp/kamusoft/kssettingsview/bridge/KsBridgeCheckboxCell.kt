package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.ui.CheckboxCell

/**
 * チェックボックス Cell（`CheckboxCell`）を輸送する DTO。
 *
 * 値変更は [KsBridgeInteractionListener.checkboxCellChanged] で通知される。
 */
class KsBridgeCheckboxCell @JvmOverloads constructor(
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
    override fun makeCell(id: String, relay: KsBridgeInteractionRelay): Cell = CheckboxCell(
        id = id,
        style = resolvedStyle,
        title = title,
        description = descriptionText,
        valueText = valueText,
        icon = resolvedIcon,
        hintText = hintText,
        isChecked = isChecked,
        accentColor = KsBridgeColor.color(accentColor),
        onValueChanged = { relay.checkboxCellChanged(id, it) },
        isEnabled = isEnabled,
        isVisible = isVisible,
    )
}
