package jp.kamusoft.kssettingsview.bridge

import androidx.compose.ui.unit.dp
import jp.kamusoft.kssettingsview.ui.CellStyle

/**
 * Cell 個別スタイルを interop 境界で輸送する DTO。
 *
 * 輸送するのは MAUI の Cell が CellStyle 段として公開する項目 — title / description / valueText /
 * hint の色とフォント、icon の寸法、行の高さ、行の背景色。色は ARGB を詰めた 32bit 整数、
 * フォントは [KsBridgeFont] の記述子、寸法は数値で表し、`null` は「未指定 → Theme から継承」を
 * 意味する（maui/ADR-0004）。
 *
 * Native の `CellStyle` が持つ accent と placeholder の CellStyle 段は MAUI から設定する手段が
 * なく、MAUI の `AccentColor` / `PlaceholderColor` は Cell 固有段として Cell 種別ごとの DTO
 * （[KsBridgeSwitchCell] / [KsBridgeEntryCell] 等）で運ぶ。[accentColor] の枠は wire 形式として
 * 残るが MAUI からは設定されず、placeholder の枠はこの DTO に無い。
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

    /**
     * DTO から Native の `CellStyle` を解決する。
     *
     * 未指定の項目は未指定のまま（色は `Color.Unspecified`、それ以外は `null`）にして Theme 継承へ
     * 送る。明示された ARGB はそのまま色として保つ。
     */
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
