package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.ui.LabelCell

/**
 * 読み取り専用の表示 Cell（`LabelCell`）を interop 境界で輸送する DTO。
 *
 * `LabelCell` は全 Cell 共通のフィールドだけで構成されるため、固有のフィールドを持たず
 * [KsBridgeCell] の共通フィールドと変換をそのまま使う。
 */
class KsBridgeLabelCell @JvmOverloads constructor(
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

    @JvmSynthetic
    override fun makeCell(id: String, relay: KsBridgeInteractionRelay): Cell = LabelCell(
        id = id,
        style = resolvedStyle,
        title = title,
        description = descriptionText,
        valueText = valueText,
        icon = resolvedIcon,
        hintText = hintText,
        isEnabled = isEnabled,
        isVisible = isVisible,
    )
}
