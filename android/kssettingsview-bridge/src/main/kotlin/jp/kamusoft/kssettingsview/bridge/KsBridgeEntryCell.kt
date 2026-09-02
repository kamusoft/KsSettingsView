package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.CellTitleAlignment
import jp.kamusoft.kssettingsview.ui.EntryCell

/**
 * テキスト入力欄を持つ Cell（`EntryCell`）を輸送する DTO。
 *
 * `EntryCell` は値文字列を持たないため、基底の `valueText` は Native へ写されない。
 * テキスト変更は [KsBridgeInteractionListener.entryCellTextChanged] で通知される。
 */
class KsBridgeEntryCell @JvmOverloads constructor(
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

    /** 現在のテキスト値 */
    var text: String = ""

    /** プレースホルダ（未指定は `null`） */
    var placeholder: String? = null

    /** プレースホルダ文字色（ARGB、未指定は `null`） */
    var placeholderColor: Int? = null

    /**
     * キーボード種別の序数（`0 = Default / 1 = Plain / 2 = Text / 3 = Chat / 4 = Url /
     * 5 = Email / 6 = Numeric / 7 = Telephone`）
     */
    var keyboard: Int = 0

    /** パスワードマスクフラグ */
    var isPassword: Boolean = false

    /** テキスト配置の序数（`0 = Start / 1 = Center / 2 = End`、未指定は `null`） */
    var textAlignment: Int? = null

    /** caret 色および選択ハイライト色（ARGB、未指定は `null`） */
    var accentColor: Int? = null

    /** 最大文字数（未指定は `null` で無制限） */
    var maxLength: Int? = null

    @JvmSynthetic
    override fun makeCell(id: String, relay: KsBridgeInteractionRelay): Cell = EntryCell(
        id = id,
        style = resolvedStyle,
        title = title,
        description = descriptionText,
        icon = resolvedIcon,
        hintText = hintText,
        text = text,
        placeholder = placeholder,
        placeholderColor = KsBridgeColor.color(placeholderColor),
        keyboardType = KsBridgeValueTransport.keyboardType(keyboard),
        isPassword = isPassword,
        textAlignment = KsBridgeValueTransport.titleAlignment(
            ordinal = textAlignment,
            fallback = DEFAULT_TEXT_ALIGNMENT,
        ),
        accentColor = KsBridgeColor.color(accentColor),
        maxLength = maxLength,
        onTextChanged = { relay.entryCellTextChanged(id, it) },
        isEnabled = isEnabled,
        isVisible = isVisible,
    )

    private companion object {
        /**
         * [textAlignment] 未指定のときに使う `EntryCell` 側の既定配置。
         *
         * 値を写し取らず Native の既定から引く（Native 側を変えたときに輸送側が古い既定を
         * 渡し続けることを防ぐ）。
         */
        private val DEFAULT_TEXT_ALIGNMENT: CellTitleAlignment = EntryCell(title = "").textAlignment
    }
}
