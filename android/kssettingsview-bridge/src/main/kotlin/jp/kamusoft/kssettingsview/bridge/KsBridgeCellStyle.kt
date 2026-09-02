package jp.kamusoft.kssettingsview.bridge

import androidx.compose.ui.unit.dp
import jp.kamusoft.kssettingsview.ui.CellStyle

/**
 * Cell 個別スタイルを interop 境界で輸送する DTO。
 *
 * 項目は Native の `CellStyle` の公開項目と 1 対 1 で対応する。色は ARGB を詰めた 32bit 整数、
 * フォントは [KsBridgeFont] の記述子、寸法は数値で表し、`null` は「未指定 → Theme から継承」を
 * 意味する（maui/ADR-0004）。
 *
 * この DTO は輸送専用であり、利用者向けのスタイル公開契約ではない。
 */
class KsBridgeCellStyle {

    /** タイトル文字色（ARGB） */
    var titleColor: Int? = null

    /** タイトルフォント */
    var titleFont: KsBridgeFont? = null

    /** 説明文色（ARGB） */
    var descriptionColor: Int? = null

    /** 説明文フォント */
    var descriptionFont: KsBridgeFont? = null

    /** 値テキスト色（ARGB） */
    var valueTextColor: Int? = null

    /** 値テキストフォント */
    var valueTextFont: KsBridgeFont? = null

    /** アイコンサイズ（dp） */
    var iconSize: Double? = null

    /** アイコン角丸半径（dp） */
    var iconRadius: Double? = null

    /** Cell 高さ（dp） */
    var cellHeight: Double? = null

    /** ヒントテキスト色（ARGB） */
    var hintTextColor: Int? = null

    /** ヒントテキストフォント */
    var hintTextFont: KsBridgeFont? = null

    /** Cell 個別背景色（ARGB） */
    var backgroundColor: Int? = null

    /** Cell 個別 accent 色（ARGB） */
    var accentColor: Int? = null

    /** DTO から Native の `CellStyle` を解決する。未指定の項目は `null`（Theme 継承）のままにする。 */
    @JvmSynthetic
    internal fun resolve(): CellStyle = CellStyle(
        titleColor = KsBridgeColor.color(titleColor),
        titleFont = titleFont?.resolve(),
        descriptionColor = KsBridgeColor.color(descriptionColor),
        descriptionFont = descriptionFont?.resolve(),
        valueTextColor = KsBridgeColor.color(valueTextColor),
        valueTextFont = valueTextFont?.resolve(),
        iconSize = iconSize?.dp,
        iconRadius = iconRadius?.dp,
        cellHeight = cellHeight?.dp,
        hintTextColor = KsBridgeColor.color(hintTextColor),
        hintTextFont = hintTextFont?.resolve(),
        backgroundColor = KsBridgeColor.color(backgroundColor),
        accentColor = KsBridgeColor.color(accentColor),
    )
}
