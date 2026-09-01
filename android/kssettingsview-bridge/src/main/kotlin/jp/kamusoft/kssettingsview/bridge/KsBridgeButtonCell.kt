package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.CellTitleAlignment
import jp.kamusoft.kssettingsview.ui.ButtonCell

/**
 * ボタン用途の Cell（`ButtonCell`）を輸送する DTO。
 *
 * `ButtonCell` は説明文を持たないため、基底の `descriptionText` は Native へ写されない。
 * タップは [KsBridgeInteractionListener.buttonCellTapped] で通知される。
 */
class KsBridgeButtonCell @JvmOverloads constructor(
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

    /** ボタンテキストの色（ARGB、未指定は `null`） */
    var titleColor: Int? = null

    /** タイトルの水平方向の揃え位置の序数（`0 = Start / 1 = Center / 2 = End`、未指定は `null`） */
    var titleAlignment: Int? = null

    @JvmSynthetic
    override fun makeCell(id: String, relay: KsBridgeInteractionRelay): Cell = ButtonCell(
        id = id,
        style = resolvedStyle,
        title = title,
        valueText = valueText,
        icon = resolvedIcon,
        hintText = hintText,
        titleColor = KsBridgeColor.color(titleColor),
        onTap = { relay.buttonCellTapped(id) },
        titleAlignment = KsBridgeValueTransport.titleAlignment(
            ordinal = titleAlignment,
            fallback = DEFAULT_TITLE_ALIGNMENT,
        ),
        isEnabled = isEnabled,
        isVisible = isVisible,
    )

    private companion object {
        /**
         * [titleAlignment] 未指定のときに使う `ButtonCell` 側の既定配置。
         *
         * 値を写し取らず Native の既定から引く（Native 側を変えたときに輸送側が古い既定を
         * 渡し続けることを防ぐ）。
         */
        private val DEFAULT_TITLE_ALIGNMENT: CellTitleAlignment =
            ButtonCell(title = "").titleAlignment
    }
}
