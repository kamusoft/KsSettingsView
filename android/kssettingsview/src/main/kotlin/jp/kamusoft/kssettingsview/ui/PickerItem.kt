package jp.kamusoft.kssettingsview.ui

/**
 * [PickerCell] の候補 1 件（主表示 + 任意の副表示）。
 *
 * `subText` の空文字列は「副表示なし」と同義であり、生成時に `null` へ正規化される。
 * 選択面はこの正規化後の値を見て、副表示を持つ行だけを 2 行構成で描画する。
 *
 * @property text 主表示テキスト
 * @property subText 副表示テキスト（`null` は副表示なし）
 */
class PickerItem(
    val text: String,
    subText: String? = null,
) {
    val subText: String? = subText?.takeIf { it.isNotEmpty() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickerItem) return false
        return text == other.text && subText == other.subText
    }

    override fun hashCode(): Int = 31 * text.hashCode() + (subText?.hashCode() ?: 0)

    override fun toString(): String = "PickerItem(text=$text, subText=$subText)"
}
