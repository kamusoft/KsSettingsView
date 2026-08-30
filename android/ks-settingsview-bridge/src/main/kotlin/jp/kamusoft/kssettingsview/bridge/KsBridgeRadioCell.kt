package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.ui.RadioCell

/**
 * 同一グループ内で 1 つだけ選択される Cell（`RadioCell`）を輸送する DTO。
 *
 * 選択は [KsBridgeInteractionListener.radioCellSelected] で通知される。グループ内の他 Cell の
 * `selectedValue` を追随させるのは上位層の責務であり、Bridge は選択された Cell 自身の cellID と
 * 値だけを通知する。
 */
class KsBridgeRadioCell @JvmOverloads constructor(
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

    /** 同一選択グループの識別子 */
    var groupID: String = ""

    /** この Cell の値 */
    var value: String = ""

    /** グループ内の現在選択値（[value] と一致するときチェック表示） */
    var selectedValue: String = ""

    /** チェックマーク色（ARGB、未指定は `null`） */
    var accentColor: Int? = null

    @JvmSynthetic
    override fun makeCell(id: String, relay: KsBridgeInteractionRelay): Cell = RadioCell(
        id = id,
        style = resolvedStyle,
        title = title,
        description = descriptionText,
        valueText = valueText,
        icon = resolvedIcon,
        hintText = hintText,
        groupId = groupID,
        value = value,
        selectedValue = selectedValue,
        accentColor = KsBridgeColor.color(accentColor),
        onSelected = { relay.radioCellSelected(id, it) },
        isEnabled = isEnabled,
        isVisible = isVisible,
    )
}
