package jp.kamusoft.kssettingsview.bridge

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import jp.kamusoft.kssettingsview.ui.Theme

/**
 * Theme を interop 境界で輸送する DTO。
 *
 * 項目は Native の `Theme` の公開項目と 1 対 1 で対応する。色は ARGB を詰めた 32bit 整数、
 * フォントは [KsBridgeFont] の記述子、寸法とフラグは数値で表し、`null` は「未指定」を意味する
 * （maui/ADR-0004）。未指定の項目は `Theme` 側の未指定（既定値）として扱われる。
 *
 * この DTO は輸送専用であり、利用者向けの Theme 公開契約ではない。
 */
class KsBridgeTheme {

    // MARK: - 全体背景・装飾

    /** セパレータ色（ARGB） */
    var separatorColor: Int? = null

    /** SettingsView 自身の背景色（ARGB） */
    var backgroundColor: Int? = null

    /** Cell 既定背景色（ARGB） */
    var cellBackgroundColor: Int? = null

    /** Cell 選択時の背景色（ARGB） */
    var selectedColor: Int? = null

    /** アクセント色（ARGB） */
    var cellAccentColor: Int? = null

    /** 無効時のテキスト置換色（ARGB） */
    var disabledTextColor: Int? = null

    /** スクロールインジケータ表示 */
    var scrollIndicatorVisible: Boolean? = null

    // MARK: - 行高さ

    /** 行高さ基準値（論理単位、整数） */
    var rowHeight: Int? = null

    /** 可変高さフラグ */
    var hasUnevenRows: Boolean? = null

    // MARK: - Header / Footer

    /** Section ヘッダのテキスト色（ARGB） */
    var headerTextColor: Int? = null

    /** Section ヘッダの背景色（ARGB） */
    var headerBackgroundColor: Int? = null

    /** Section ヘッダ既定フォントサイズ（論理単位） */
    var headerFontSize: Double? = null

    /** Section ヘッダ既定フォント */
    var headerFont: KsBridgeFont? = null

    /** Section ヘッダの既定高さ（論理単位） */
    var headerHeight: Double? = null

    /** Section フッタのテキスト色（ARGB） */
    var footerTextColor: Int? = null

    /** Section フッタの背景色（ARGB） */
    var footerBackgroundColor: Int? = null

    /** Section フッタ既定フォントサイズ（論理単位） */
    var footerFontSize: Double? = null

    /** Section フッタ既定フォント */
    var footerFont: KsBridgeFont? = null

    // MARK: - Cell 全体既定

    /** Cell タイトル既定色（ARGB） */
    var cellTitleColor: Int? = null

    /** Cell タイトル既定フォント */
    var cellTitleFont: KsBridgeFont? = null

    /** Cell タイトル既定フォントサイズ（論理単位） */
    var cellTitleFontSize: Double? = null

    /** valueText 既定色（ARGB） */
    var cellValueTextColor: Int? = null

    /** valueText 既定フォント */
    var cellValueTextFont: KsBridgeFont? = null

    /** description 既定色（ARGB） */
    var cellDescriptionColor: Int? = null

    /** description 既定フォント */
    var cellDescriptionFont: KsBridgeFont? = null

    /** hintText 既定色（ARGB） */
    var cellHintTextColor: Int? = null

    /** hintText 既定フォント */
    var cellHintFont: KsBridgeFont? = null

    /** `EntryCell` の placeholder 既定色（ARGB） */
    var cellPlaceholderColor: Int? = null

    /** アイコン既定サイズ（dp） */
    var cellIconSize: Double? = null

    /** アイコン既定角丸半径（dp） */
    var cellIconRadius: Double? = null

    // MARK: - Section 装飾

    /**
     * Section 単位の外側余白の上成分（dp）。
     *
     * 余白の 4 成分は全体で 1 つの指定として扱う。1 つでも `null` なら余白全体が未指定になる。
     */
    var sectionMarginTop: Double? = null

    /** Section 単位の外側余白の start 成分（dp） */
    var sectionMarginLeading: Double? = null

    /** Section 単位の外側余白の下成分（dp） */
    var sectionMarginBottom: Double? = null

    /** Section 単位の外側余白の end 成分（dp） */
    var sectionMarginTrailing: Double? = null

    /** Section の箱の角丸半径（dp） */
    var sectionCornerRadius: Double? = null

    /** Section の箱のボーダー幅（dp） */
    var sectionBorderWidth: Double? = null

    /** Section の箱のボーダー色（ARGB） */
    var sectionBorderColor: Int? = null

    /** DTO から Native の `Theme` を解決する。未指定の項目は `Theme` の既定値を用いる。 */
    @JvmSynthetic
    internal fun resolve(): Theme {
        val base = Theme()
        return Theme(
            separatorColor = KsBridgeColor.color(separatorColor) ?: base.separatorColor,
            backgroundColor = KsBridgeColor.color(backgroundColor) ?: base.backgroundColor,
            cellBackgroundColor = KsBridgeColor.color(cellBackgroundColor) ?: base.cellBackgroundColor,
            selectedColor = KsBridgeColor.color(selectedColor) ?: base.selectedColor,
            cellAccentColor = KsBridgeColor.color(cellAccentColor) ?: base.cellAccentColor,
            disabledTextColor = KsBridgeColor.color(disabledTextColor) ?: base.disabledTextColor,
            scrollIndicatorVisible = scrollIndicatorVisible ?: base.scrollIndicatorVisible,
            rowHeight = rowHeight ?: base.rowHeight,
            hasUnevenRows = hasUnevenRows ?: base.hasUnevenRows,
            headerTextColor = KsBridgeColor.color(headerTextColor) ?: base.headerTextColor,
            headerBackgroundColor = KsBridgeColor.color(headerBackgroundColor) ?: base.headerBackgroundColor,
            headerFontSize = headerFontSize ?: base.headerFontSize,
            headerFont = headerFont?.resolve(),
            headerHeight = headerHeight ?: base.headerHeight,
            footerTextColor = KsBridgeColor.color(footerTextColor) ?: base.footerTextColor,
            footerBackgroundColor = KsBridgeColor.color(footerBackgroundColor) ?: base.footerBackgroundColor,
            footerFontSize = footerFontSize ?: base.footerFontSize,
            footerFont = footerFont?.resolve(),
            cellTitleColor = KsBridgeColor.color(cellTitleColor),
            cellTitleFont = cellTitleFont?.resolve(),
            cellTitleFontSize = cellTitleFontSize ?: base.cellTitleFontSize,
            cellValueTextColor = KsBridgeColor.color(cellValueTextColor),
            cellValueTextFont = cellValueTextFont?.resolve(),
            cellDescriptionColor = KsBridgeColor.color(cellDescriptionColor),
            cellDescriptionFont = cellDescriptionFont?.resolve(),
            cellHintTextColor = KsBridgeColor.color(cellHintTextColor),
            cellHintFont = cellHintFont?.resolve(),
            cellPlaceholderColor = KsBridgeColor.color(cellPlaceholderColor),
            cellIconSize = cellIconSize?.dp,
            cellIconRadius = cellIconRadius?.dp,
            sectionMargin = resolveSectionMargin(),
            sectionCornerRadius = sectionCornerRadius?.dp,
            sectionBorderWidth = sectionBorderWidth?.dp,
            sectionBorderColor = KsBridgeColor.color(sectionBorderColor),
        )
    }

    /**
     * margin の論理 4 成分から方向対応型を組み立てる。
     *
     * 4 成分は余白全体で 1 つの指定であり、1 つでも未指定なら余白全体を未指定として解決する。
     * 成分の値は検証せずそのまま運ぶ（負値・非有限の正規化は描画時の責務）ため、
     * 「0 以上」を構築時に要求する Compose 標準のファクトリではなく [KsBridgeSectionMargin] を使う。
     */
    private fun resolveSectionMargin(): PaddingValues? {
        val top = sectionMarginTop ?: return null
        val start = sectionMarginLeading ?: return null
        val bottom = sectionMarginBottom ?: return null
        val end = sectionMarginTrailing ?: return null
        return KsBridgeSectionMargin(
            start = start.dp,
            top = top.dp,
            end = end.dp,
            bottom = bottom.dp,
        )
    }
}
