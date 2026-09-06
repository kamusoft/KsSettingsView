package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * `Theme` と `CellStyle` を合成した実効スタイル。
 *
 * Cell ごとの [CellStyle] の各フィールドは未指定を取り得る（Theme から継承する意図）。
 * 本クラスは「Cell 描画時に確定値として欲しいプラットフォーム型」を提供するため、
 * 未指定フィールドは Theme（または UI 層既定値）から補完する。
 *
 * 入力の `Theme` は、解決済み `Theme` を直接読む描画箇所が使う色（list 下地・Cell 背景・
 * separator・選択色・accent・disabled 文字・Header / Footer）が現在の外観の既定セットへ解決済みで
 * あることを前提とする（解決点は `KsSettingsView`。core/ADR-0030）。タイトルと説明文の外観既定は
 * 解決済み `Theme` には載らず、本クラスのアクセサが `darkTheme` を受けて最終段で
 * [KsThemePalette] から選ぶ。入力の解決順序は「`CellStyle.X` → `Theme.cellX` → 外観の既定」で
 * 統一する。アクセサ関数群（[effectiveTitleColor] / [effectiveValueTextColor] 等）は
 * `EffectiveStyle.Companion` および top-level 関数として提供する。
 *
 * @property titleColor タイトル文字色（ARGB Int、`cellTitleFontSize > 0` のとき size が上書き済み）
 * @property titleTypeface タイトルの Typeface
 * @property titleSizeSp タイトル文字サイズ（sp 単位の Float、`cellTitleFontSize > 0` のとき上書き済み）
 * @property descriptionColor 説明文の文字色（ARGB Int）
 * @property descriptionTypeface 説明文の Typeface
 * @property descriptionSizeSp 説明文文字サイズ（sp 単位の Float）
 * @property backgroundColor Cell 背景色（ARGB Int）
 * @property selectedColor Cell 選択時背景色（ARGB Int）
 * @property accentColor 選択系 Cell のアクセント色（ARGB Int）
 * @property valueTextColor valueText 系の文字色（ARGB Int）。LabelCell / CommandCell の値テキストと
 *   `EntryCell` の入力済みテキストが使う。どの段にも指定が無いときは `Theme.cellTitleColor`、
 *   それも未指定なら外観のタイトル既定へ落ちる
 * @property valueTextTypeface valueText の Typeface
 * @property valueTextSizeSp valueText の文字サイズ（sp）
 * @property hintTextColor hintText の文字色（ARGB Int）
 * @property hintTextTypeface hintText の Typeface
 * @property hintTextSizeSp hintText の文字サイズ（sp）
 * @property disabledTextColor isEnabled=false 時のテキスト色（ARGB Int）
 * @property iconSizeDp icon 領域の正方形枠の一辺（dp）。`CellStyle.iconSize` → `Theme.cellIconSize` → 既定 24dp の順で解決済み
 * @property iconRadiusDp icon 領域の枠にかける角丸半径（dp、0 は角丸なし）。`CellStyle.iconRadius` → `Theme.cellIconRadius` → 既定 0dp の順で解決済み
 * @property effectiveHeightDp 実効行高さ（dp）。`CellStyle.cellHeight ?? Theme.rowHeight ?? MIN_ROW_HEIGHT_DP` を MIN_ROW_HEIGHT_DP（= 60dp）で下限ガード
 * @property isFixedHeight 固定高さモードか（`!Theme.hasUnevenRows`）
 */
internal data class EffectiveStyle(
    @ColorInt val titleColor: Int,
    val titleTypeface: Typeface,
    val titleSizeSp: Float,
    @ColorInt val descriptionColor: Int,
    val descriptionTypeface: Typeface,
    val descriptionSizeSp: Float,
    @ColorInt val backgroundColor: Int,
    @ColorInt val selectedColor: Int,
    @ColorInt val accentColor: Int,
    @ColorInt val valueTextColor: Int,
    val valueTextTypeface: Typeface,
    val valueTextSizeSp: Float,
    @ColorInt val hintTextColor: Int,
    val hintTextTypeface: Typeface,
    val hintTextSizeSp: Float,
    @ColorInt val disabledTextColor: Int,
    val iconSizeDp: Float,
    val iconRadiusDp: Float,
    val effectiveHeightDp: Int,
    val isFixedHeight: Boolean,
) {
    companion object {
        /** タイトル既定サイズ（sp）。プラットフォーム既定 17sp 相当。 */
        private const val DEFAULT_TITLE_SIZE_SP: Float = 17.0f

        /** 説明文既定サイズ（sp）。 */
        private const val DEFAULT_DESCRIPTION_SIZE_SP: Float = 14.0f

        /**
         * Android 側の最低行高さ（dp）。`Theme.rowHeight` / `CellStyle.cellHeight` の下限であり、
         * いずれも未指定のときに採用する既定の base 高さでもある。
         *
         * 60dp を採用する根拠：
         *   - オリジナル `AiForms.Maui.SettingsView` の `AiRecyclerView.UpdateRowHeight()`
         *     (`Native/Android/AiRecyclerView.cs:228-235`) が `RowHeight == -1` のとき自動的に
         *     `60` をセットし、続く `SettingsViewRecyclerAdapter.cs:483` で
         *     `max(rowHeight=60, MinRowHeight=44) = 60` を最終高さとする。
         *   - 原典の `MinRowHeight = 44dp` は最終高さに影響しない（`max(60, 44) = 60`）ため、
         *     Android の最終下限は 60dp に統一する。
         *   - iOS 側 (`minRowHeight = 48`) はオリジナル `AiTableView.cs:19` 踏襲のため
         *     据え置き。プラットフォーム慣習の差として許容する。
         */
        const val MIN_ROW_HEIGHT_DP: Int = 60

        /**
         * `Theme` と `CellStyle` を合成して [EffectiveStyle] を構築する。
         *
         * `cellStyle` の各フィールドが未指定の場合、`theme` の対応値もしくは UI 層既定値で補完する。
         *
         * @param theme 未指定色を外観の既定セットへ解決済みの `Theme`
         * @param cellStyle Cell 個別のスタイル
         * @param darkTheme ダーク外観なら `true`。タイトル・説明文・ButtonCell タイトルの最終段の
         *   既定を選ぶのに使う
         */
        fun from(theme: Theme, cellStyle: CellStyle, darkTheme: Boolean): EffectiveStyle {
            // 解決ロジックは Companion アクセサ群に集約し、本関数は「Compose 論理型 →
            // Android View 系のプラットフォーム型 (ARGB Int / Typeface / sp Float)」変換のみ担う。
            // SoT を 1 箇所にすることで解決順序ロジックが二重管理にならないようにする。

            // タイトル色: アクセサ経由で Color を取得し、Color → ARGB Int 変換。
            val titleColor: Int = effectiveTitleColor(cellStyle, theme, darkTheme).toArgb()

            // タイトルフォント: Compose TextStyle のアクセサで解決 → Typeface / sp Float に変換。
            val resolvedTitleStyle: TextStyle = effectiveTitleFont(cellStyle, theme)
            val titleTypeface = resolvedTitleStyle.toTypeface()
            val titleSizeSp: Float = resolvedTitleStyle.fontSize.toSpFloatOrNull()
                ?: DEFAULT_TITLE_SIZE_SP

            // 説明色: Companion アクセサ経由で解決。
            val descriptionColor = effectiveDescriptionColor(cellStyle, theme, darkTheme).toArgb()

            // 説明フォント: Companion アクセサ経由で TextStyle 取得 → Typeface / sp Float に変換。
            val resolvedDescriptionStyle: TextStyle = effectiveDescriptionFont(cellStyle, theme)
            val descriptionTypeface = if (resolvedDescriptionStyle === TextStyle.Default) {
                // 未指定時は Typeface.DEFAULT（既存挙動の互換）。
                Typeface.DEFAULT
            } else {
                resolvedDescriptionStyle.toTypeface()
            }
            val descriptionSizeSp: Float = resolvedDescriptionStyle.fontSize.toSpFloatOrNull()
                ?: DEFAULT_DESCRIPTION_SIZE_SP

            // 背景色 / 選択時色 / アクセント色: Companion アクセサ経由。
            val backgroundColor = effectiveBackgroundColor(cellStyle, theme).toArgb()
            val selectedColor = theme.selectedColor.toArgb()
            val accentColor = effectiveAccentColor(cellStyle, theme).toArgb()

            // 値テキスト色 / フォント: Companion アクセサ経由で取得 → ARGB / Typeface / sp Float へ。
            val valueTextColor: Int = effectiveValueTextColor(cellStyle, theme, darkTheme).toArgb()
            val resolvedValueTextStyle: TextStyle = effectiveValueTextFont(cellStyle, theme)
            val valueTextTypeface = if (resolvedValueTextStyle === TextStyle.Default) {
                // valueText フォント未指定時は title の Typeface を継承する（既存挙動互換）。
                titleTypeface
            } else {
                resolvedValueTextStyle.toTypeface()
            }
            val valueTextSizeSp: Float = resolvedValueTextStyle.fontSize.toSpFloatOrNull()
                ?: titleSizeSp

            // hintText 色: Companion アクセサ経由で解決（解決順序 CellStyle.hintTextColor →
            //   Theme.cellHintTextColor → Theme.cellAccentColor）。
            val hintTextColor: Int = effectiveHintTextColor(cellStyle, theme).toArgb()

            // hintText フォント: Companion アクセサ経由で TextStyle 取得 → Typeface / sp Float に変換。
            // 未指定時は description と同じ既定 (Typeface.DEFAULT / DEFAULT_DESCRIPTION_SIZE_SP) にフォールバック。
            val resolvedHintTextStyle: TextStyle = effectiveHintFont(cellStyle, theme)
            val hintTextTypeface = if (resolvedHintTextStyle === TextStyle.Default) {
                Typeface.DEFAULT
            } else {
                resolvedHintTextStyle.toTypeface()
            }
            val hintTextSizeSp: Float = resolvedHintTextStyle.fontSize.toSpFloatOrNull()
                ?: DEFAULT_DESCRIPTION_SIZE_SP

            // 無効時テキスト色: Theme.disabledTextColor
            val disabledTextColor = theme.disabledTextColor.toArgb()

            // icon 枠の一辺と角丸半径（dp）: Companion アクセサ経由で解決する。
            // 描画側（共通行レイアウト）はこの実効値だけを見て icon 領域を組み立てる。
            val iconSizeDp: Float = effectiveIconSize(cellStyle, theme).value
            val iconRadiusDp: Float = effectiveIconRadius(cellStyle, theme).value

            // 実効行高さ（dp）: Companion アクセサ経由で解決。
            val effectiveHeightDp = effectiveCellHeightDp(cellStyle, theme)

            return EffectiveStyle(
                titleColor = titleColor,
                titleTypeface = titleTypeface,
                titleSizeSp = titleSizeSp,
                descriptionColor = descriptionColor,
                descriptionTypeface = descriptionTypeface,
                descriptionSizeSp = descriptionSizeSp,
                backgroundColor = backgroundColor,
                selectedColor = selectedColor,
                accentColor = accentColor,
                valueTextColor = valueTextColor,
                valueTextTypeface = valueTextTypeface,
                valueTextSizeSp = valueTextSizeSp,
                hintTextColor = hintTextColor,
                hintTextTypeface = hintTextTypeface,
                hintTextSizeSp = hintTextSizeSp,
                disabledTextColor = disabledTextColor,
                iconSizeDp = iconSizeDp,
                iconRadiusDp = iconRadiusDp,
                effectiveHeightDp = effectiveHeightDp,
                isFixedHeight = !theme.hasUnevenRows,
            )
        }

        /**
         * `effectiveHeightDp` を px に変換するヘルパ。
         * `Resources.displayMetrics.density` で論理 dp → 物理 px 変換する。
         */
        fun dpToPx(context: Context, dp: Int): Int {
            val density = context.resources.displayMetrics.density
            return (dp * density).toInt()
        }

        // ============================================================================
        // EffectiveStyle アクセサ群（解決順序 `CellStyle → Theme → 既定`）
        //
        // 戻り値の型は Compose 型（`Color` / `TextStyle` / `Dp`）。`from(context, ...)` が
        // ARGB Int / Typeface へ変換するのは Android View 系 ViewHolder のためであり、
        // 本アクセサ群は Compose 描画系でも直接使えるよう論理型をそのまま返す。
        // ============================================================================

        /**
         * タイトル文字色を解決する。
         * 解決順序: `cellStyle.titleColor` → `theme.cellTitleColor` → 外観の既定
         *
         * 最終段が外観の既定を返すため、戻り値が未指定になることはない。
         */
        fun effectiveTitleColor(cellStyle: CellStyle, theme: Theme, darkTheme: Boolean): Color =
            cellStyle.titleColor
                .takeOrElse { theme.cellTitleColor }
                .takeOrElse { KsThemePalette.cellTitle(darkTheme) }

        /**
         * タイトルフォントを解決する。
         * 解決順序: `cellStyle.titleFont` → `theme.cellTitleFont` → 既定（`TextStyle.Default`）。
         * `theme.cellTitleFontSize > 0` のとき、最終 fontSize を上書きする。
         */
        fun effectiveTitleFont(cellStyle: CellStyle, theme: Theme): TextStyle {
            val base: TextStyle = cellStyle.titleFont
                ?: theme.cellTitleFont
                ?: TextStyle.Default
            return if (theme.cellTitleFontSize > 0) {
                base.copy(fontSize = theme.cellTitleFontSize.sp)
            } else {
                base
            }
        }

        /**
         * description 色を解決する。
         * 解決順序: `cellStyle.descriptionColor` → `theme.cellDescriptionColor` → 外観の既定
         */
        fun effectiveDescriptionColor(cellStyle: CellStyle, theme: Theme, darkTheme: Boolean): Color =
            cellStyle.descriptionColor
                .takeOrElse { theme.cellDescriptionColor }
                .takeOrElse { KsThemePalette.cellDescription(darkTheme) }

        /**
         * description フォントを解決する。
         * 解決順序: `cellStyle.descriptionFont` → `theme.cellDescriptionFont` → 既定。
         */
        fun effectiveDescriptionFont(cellStyle: CellStyle, theme: Theme): TextStyle {
            cellStyle.descriptionFont?.let { return it }
            theme.cellDescriptionFont?.let { return it }
            return TextStyle.Default
        }

        /**
         * valueText 色を解決する。
         * 解決順序: `cellStyle.valueTextColor` → `theme.cellValueTextColor` → `theme.cellTitleColor`
         * → 外観の既定（タイトルと同じロール）
         *
         * `theme.cellValueTextColor` は外観の既定を持たず、未指定のときは title の色を継承する。
         */
        fun effectiveValueTextColor(cellStyle: CellStyle, theme: Theme, darkTheme: Boolean): Color =
            cellStyle.valueTextColor
                .takeOrElse { theme.cellValueTextColor }
                .takeOrElse { theme.cellTitleColor }
                .takeOrElse { KsThemePalette.cellTitle(darkTheme) }

        /**
         * valueText フォントを解決する。
         * 解決順序: `cellStyle.valueTextFont` → `theme.cellValueTextFont` → `theme.cellTitleFont` → 既定。
         */
        fun effectiveValueTextFont(cellStyle: CellStyle, theme: Theme): TextStyle {
            cellStyle.valueTextFont?.let { return it }
            theme.cellValueTextFont?.let { return it }
            theme.cellTitleFont?.let { return it }
            return TextStyle.Default
        }

        /**
         * hintText 色を解決する。
         * 解決順序: `cellStyle.hintTextColor` → `theme.cellHintTextColor` → `theme.cellAccentColor`
         */
        fun effectiveHintTextColor(cellStyle: CellStyle, theme: Theme): Color =
            cellStyle.hintTextColor
                .takeOrElse { theme.cellHintTextColor }
                .takeOrElse { theme.cellAccentColor }

        /**
         * hintText フォントを解決する。
         * 解決順序: `cellStyle.hintTextFont` → `theme.cellHintFont` → 既定。
         */
        fun effectiveHintFont(cellStyle: CellStyle, theme: Theme): TextStyle {
            cellStyle.hintTextFont?.let { return it }
            theme.cellHintFont?.let { return it }
            return TextStyle.Default
        }

        /**
         * placeholder 文字色を解決する（Cell 固有値を伴わない `CellStyle` 以降の段）。
         * 解決順序: `cellStyle.placeholderColor` → `theme.cellPlaceholderColor` → プラットフォーム既定
         *
         * 戻り値の `Color.Unspecified` は「どの段にも指定が無い」ことを表し、描画側は同梱テーマ
         * （`Theme.Material3.DayNight` 派生）の hint 色（`android:textColorHint` の `ColorStateList`）を
         * 現在の外観で解決してそのまま使う。ホストアプリの XML テーマは参照しない。ライブラリ独自の
         * 既定色は持ち込まないため、placeholder は外観の既定セットにも含まれない。
         */
        fun effectivePlaceholderColor(cellStyle: CellStyle, theme: Theme): Color =
            cellStyle.placeholderColor.takeOrElse { theme.cellPlaceholderColor }

        /**
         * `EntryCell.placeholderColor` 用の 4 段優先 placeholder 色解決。
         *
         * 解決順序:
         *   1. `entryPlaceholderColor`（`EntryCell` 個別フィールド、Cell 固有値が最優先）
         *   2. `cellStyle.placeholderColor`
         *   3. `theme.cellPlaceholderColor`
         *   4. プラットフォーム既定（`Color.Unspecified`）
         */
        fun effectivePlaceholderColor(
            entryPlaceholderColor: Color?,
            cellStyle: CellStyle,
            theme: Theme,
        ): Color {
            entryPlaceholderColor?.let { return it }
            return effectivePlaceholderColor(cellStyle, theme)
        }

        /**
         * アイコンサイズ（正方形の一辺 dp）を解決する。
         * 解決順序: `cellStyle.iconSize` → `theme.cellIconSize` → 24dp
         *
         * 有効な指定は正の有限値のみで、それ以外（0 / 負値 / 非有限値）は未指定として次の段へ送る。
         */
        fun effectiveIconSize(cellStyle: CellStyle, theme: Theme): Dp {
            cellStyle.iconSize?.takeIf { it.isValidIconSize() }?.let { return it }
            theme.cellIconSize?.takeIf { it.isValidIconSize() }?.let { return it }
            return Theme.DEFAULT_CELL_ICON_SIZE_DP_VALUE.dp
        }

        /**
         * アイコン角丸半径（dp）を解決する。
         * 解決順序: `cellStyle.iconRadius` → `theme.cellIconRadius` → 0dp（角丸なし）
         *
         * 有効な指定は 0 以上の有限値のみで、それ以外（負値 / 非有限値）は未指定として次の段へ送る。
         */
        fun effectiveIconRadius(cellStyle: CellStyle, theme: Theme): Dp {
            cellStyle.iconRadius?.takeIf { it.isValidIconRadius() }?.let { return it }
            theme.cellIconRadius?.takeIf { it.isValidIconRadius() }?.let { return it }
            return Theme.DEFAULT_CELL_ICON_RADIUS_DP_VALUE.dp
        }

        /**
         * icon の一辺として有効な値か。
         *
         * 正の有限値だけを指定値として扱う。0 以下では icon が描画されず、負の dp は
         * `LayoutParams` の予約値（`MATCH_PARENT` / `WRAP_CONTENT`）と衝突するため、
         * 指定として受け付けずに次の段へ送る。
         */
        private fun Dp.isValidIconSize(): Boolean = value.isFinite() && value > 0f

        /**
         * icon の角丸半径として有効な値か。
         *
         * 0 以上の有限値だけを指定値として扱う（0 は「角丸なし」という意味のある指定）。
         * 負値・非有限値は描画できないため次の段へ送る。
         */
        private fun Dp.isValidIconRadius(): Boolean = value.isFinite() && value >= 0f

        /**
         * Cell 背景色を解決する。
         * 解決順序: `cellStyle.backgroundColor` → `theme.cellBackgroundColor`
         */
        fun effectiveBackgroundColor(cellStyle: CellStyle, theme: Theme): Color =
            cellStyle.backgroundColor.takeOrElse { theme.cellBackgroundColor }

        /**
         * accent 色を解決する。
         * 解決順序: `cellStyle.accentColor` → `theme.cellAccentColor`
         */
        fun effectiveAccentColor(cellStyle: CellStyle, theme: Theme): Color =
            cellStyle.accentColor.takeOrElse { theme.cellAccentColor }

        /**
         * 実効行高さ（dp）を解決する。
         *
         * 解決順序:
         *   1. `cellStyle.cellHeight` （正の値のとき採用）
         *   2. `theme.rowHeight` （正の値のとき採用）
         *   3. `MIN_ROW_HEIGHT_DP`（= 60dp、未指定時のオリジナル踏襲 base）
         *
         * いずれの場合も最終値は `MIN_ROW_HEIGHT_DP`（= 60dp）で下限ガードする。
         *
         * オリジナル `AiForms.Maui.SettingsView` の挙動：
         *   - `AiRecyclerView.UpdateRowHeight()` が `RowHeight == -1` のとき自動的に `60` をセット
         *   - `SettingsViewRecyclerAdapter.cs:483` で `max(rowHeight=60, MinRowHeight=44) = 60` を最終高さとする
         *
         * 原典の `MinRowHeight = 44dp` は最終高さに影響しない（常に 60dp 以上）ため、
         * Android では 60dp 一本に統一する。
         */
        fun effectiveCellHeightDp(cellStyle: CellStyle, theme: Theme): Int {
            val base: Int = when {
                cellStyle.cellHeight != null && cellStyle.cellHeight.value > 0 ->
                    cellStyle.cellHeight.value.toInt()
                theme.rowHeight > 0 -> theme.rowHeight
                else -> MIN_ROW_HEIGHT_DP
            }
            return maxOf(base, MIN_ROW_HEIGHT_DP)
        }

        /**
         * `ButtonCell.titleColor` 用の 4 段優先タイトル色解決（Compose 用、Context 不要）。
         *
         * 解決順序:
         *   1. `buttonCellTitleColor`（ButtonCell 個別フィールド、Cell 個別最優先）
         *   2. `cellStyle.titleColor`
         *   3. `theme.cellTitleColor`
         *   4. 外観に対応する ButtonCell の既定色（[KsThemePalette.buttonTitle]）
         *
         * 通常 Cell のタイトル既定（黒 / 白）と異なり、ButtonCell は「tappable に見える慣習色」として
         * 外観ごとのシステムブルー相当を既定に採る。この 4 段目は accent の指定とは独立であり、
         * 利用者が accent だけを変えても ButtonCell のタイトルは変わらない。
         * View 系 (TextView 等) の本番描画では [effectiveButtonTitleColorArgb] を使うこと。
         */
        fun effectiveButtonTitleColor(
            buttonCellTitleColor: Color?,
            cellStyle: CellStyle,
            theme: Theme,
            darkTheme: Boolean,
        ): Color {
            buttonCellTitleColor?.let { return it }
            return cellStyle.titleColor
                .takeOrElse { theme.cellTitleColor }
                .takeOrElse { KsThemePalette.buttonTitle(darkTheme) }
        }

        /**
         * `ButtonCell.titleColor` 用の 4 段優先タイトル色解決（Android View 系、ARGB Int を返す）。
         *
         * 解決順序は `ButtonCell.titleColor` → `CellStyle.titleColor` → `Theme.cellTitleColor`
         * → 外観に対応する ButtonCell の既定色の 4 段。SoT は本ヘルパに集約し、
         * `ButtonCellViewHolder` から直接呼ぶ。
         *
         * 4 段目はホストのテーマを参照せず、ライブラリが所有する外観別の既定値である。ライブラリ UI の
         * 配色はホストの XML テーマから隔離され、見た目の指定は `Theme` / `CellStyle` が正となる
         * （android/ADR-0020）。Compose 経路の [effectiveButtonTitleColor] と同じ既定色になるため、
         * 両経路の解決結果は一致する。
         */
        @ColorInt
        fun effectiveButtonTitleColorArgb(
            buttonCellTitleColor: Color?,
            cellStyle: CellStyle,
            theme: Theme,
            darkTheme: Boolean,
        ): Int = effectiveButtonTitleColor(
            buttonCellTitleColor = buttonCellTitleColor,
            cellStyle = cellStyle,
            theme = theme,
            darkTheme = darkTheme,
        ).toArgb()

        /**
         * Section / Root Header のテキストフォントを `TextStyle` として解決する。
         *
         * 解決順序（`Theme.headerFont` / `Theme.headerFontSize`）：
         * 1. `theme.headerFont != null` のとき、ベースを `headerFont` とする
         * 2. `null` のとき、ベースを `TextStyle.Default` とする
         * 3. `theme.headerFontSize > 0` のとき、ベースの `fontSize` を `headerFontSize.sp` で上書きする
         */
        fun effectiveHeaderFont(theme: Theme): TextStyle {
            val base = theme.headerFont ?: TextStyle.Default
            return if (theme.headerFontSize > 0.0) {
                base.copy(fontSize = theme.headerFontSize.sp)
            } else {
                base
            }
        }

        /**
         * Section / Root Footer のテキストフォントを `TextStyle` として解決する。
         *
         * 解決順序は [effectiveHeaderFont] と同形で `footerFont` / `footerFontSize` を参照する。
         */
        fun effectiveFooterFont(theme: Theme): TextStyle {
            val base = theme.footerFont ?: TextStyle.Default
            return if (theme.footerFontSize > 0.0) {
                base.copy(fontSize = theme.footerFontSize.sp)
            } else {
                base
            }
        }

        /**
         * `isHeader` の真偽で [effectiveHeaderFont] / [effectiveFooterFont] を呼び分けるラッパ。
         *
         * Section / Root の H/F ViewHolder から共通経路で呼ばれる。
         */
        fun effectiveHeaderOrFooterFont(theme: Theme, isHeader: Boolean): TextStyle =
            if (isHeader) effectiveHeaderFont(theme) else effectiveFooterFont(theme)
    }
}

/**
 * Compose `TextStyle` から Android `Typeface` を解決する。
 *
 * フォントファミリの解決は `TextStyle.fontFamily` に直接アクセスせず、
 * `fontWeight` を CSS 数値ウェイトとして取り扱う方式で `Typeface.DEFAULT` をベースに合成する。
 * Compose の `FontFamily` は `Resources` 経由のフォント解決を必要とするため、UI 層の
 * `EffectiveStyle.from` 経路では「フォントファミリ未指定 = システムフォント」として扱う
 * シンプルな実装に留める。
 */
internal fun TextStyle.toTypeface(): Typeface {
    val numericWeight = fontWeight?.weight ?: 400
    // API 28+ で利用可能な numericWeight オーバーロードを使用（minSdk 29 のため常に可）。
    return Typeface.create(Typeface.DEFAULT, numericWeight, /* italic = */ false)
}

/**
 * `TextUnit` を sp 単位の `Float` に変換するヘルパ。
 *
 * - `TextUnit.Unspecified`、または sp/em 以外の単位は `null` を返す。
 * - `TextUnitType.Em` は基準サイズが不明（17 sp ベース）として 17.0f * value とみなす。
 *   実用上 Theme の `titleFont` などで Em は使われないため、保守的なフォールバックとして扱う。
 */
internal fun TextUnit.toSpFloatOrNull(): Float? {
    if (this == TextUnit.Unspecified) return null
    return when (this.type) {
        TextUnitType.Sp -> this.value
        TextUnitType.Em -> this.value * 17.0f
        else -> null
    }
}
