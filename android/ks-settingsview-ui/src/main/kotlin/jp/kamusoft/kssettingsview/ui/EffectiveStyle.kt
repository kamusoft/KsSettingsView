package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
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
 * Cell ごとの [CellStyle] の各フィールドは `null` 可（Theme から継承する意図）。
 * 本クラスは「Cell 描画時に確定値として欲しいプラットフォーム型」を提供するため、
 * `null` フィールドは Theme（または UI 層既定値）から補完する。
 *
 * 入力の `Theme` / `CellStyle` の解決順序は「`CellStyle.X` → `Theme.cellX` → プラットフォーム既定」
 * で統一する。アクセサ関数群（[effectiveTitleColor] / [effectiveValueTextColor] 等）は本ファイル末尾の
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
 *   `EntryCell` の入力済みテキストが使う。どの段にも指定が無いときはホストテーマの既定文字色へ解決する
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
 * @property titleColorIsExplicit タイトル色が CellStyle または Theme いずれかで明示指定されたかを示すフラグ
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
    val titleColorIsExplicit: Boolean,
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
         * `cellStyle` の各フィールドが `null` の場合、`theme` の対応値もしくは UI 層既定値で補完する。
         */
        fun from(context: Context, theme: Theme, cellStyle: CellStyle): EffectiveStyle {
            // 解決ロジックは Companion アクセサ群に集約し、本関数は「Compose 論理型 →
            // Android View 系のプラットフォーム型 (ARGB Int / Typeface / sp Float)」変換のみ担う。
            // SoT を 1 箇所にすることで spec の解決順序ロジックが二重管理にならないようにする。
            val titleColorIsExplicit: Boolean =
                cellStyle.titleColor != null || theme.cellTitleColor != null

            // タイトル色: アクセサ経由で Color を取得し、Color → ARGB Int 変換。
            // ただし「両方 nil」のときの 4 段目だけは Android View 系では Context 経由の
            // `android.R.attr.textColorPrimary` 解決が必要なため、ここで分岐する。
            val titleColor: Int = if (titleColorIsExplicit) {
                effectiveTitleColor(cellStyle, theme).toArgb()
            } else {
                resolveDefaultTitleColor(context)
            }

            // タイトルフォント: Compose TextStyle のアクセサで解決 → Typeface / sp Float に変換。
            val resolvedTitleStyle: TextStyle = effectiveTitleFont(cellStyle, theme)
            val titleTypeface = resolvedTitleStyle.toTypeface()
            val titleSizeSp: Float = resolvedTitleStyle.fontSize.toSpFloatOrNull()
                ?: DEFAULT_TITLE_SIZE_SP

            // 説明色: Companion アクセサ経由で解決。
            val descriptionColor = effectiveDescriptionColor(cellStyle, theme).toArgb()

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
            // タイトル色と同じく、解決順の 4 段目（どの段にも指定が無い場合）だけは Android View 系では
            // Context 経由の `android.R.attr.textColorPrimary` 解決が必要なため、ここで分岐する。
            val valueTextColorIsExplicit: Boolean = cellStyle.valueTextColor != null ||
                theme.cellValueTextColor != null ||
                theme.cellTitleColor != null
            val valueTextColor: Int = if (valueTextColorIsExplicit) {
                effectiveValueTextColor(cellStyle, theme).toArgb()
            } else {
                resolveDefaultTitleColor(context)
            }
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
                titleColorIsExplicit = titleColorIsExplicit,
            )
        }

        /**
         * Title 色の既定値（3 段階目フォールバック）を Context のテーマの
         * `android.R.attr.textColorPrimary` から解決する。取得失敗時は黒（`#FF000000`）へ
         * フォールバックする。
         *
         * ここへ渡る Context はライブラリ所有 UI 用の Context（同梱テーマ適用済み）であり、
         * 解決値はホストのテーマに影響されない（android/ADR-0020）。
         */
        @ColorInt
        private fun resolveDefaultTitleColor(context: Context): Int {
            val tv = TypedValue()
            val resolved = context.theme.resolveAttribute(
                android.R.attr.textColorPrimary,
                tv,
                true,
            )
            if (!resolved) return DEFAULT_TITLE_COLOR
            // resourceId 経由（ColorStateList の可能性に対応）
            if (tv.resourceId != 0) {
                val csl = try {
                    androidx.core.content.ContextCompat.getColorStateList(context, tv.resourceId)
                } catch (_: Throwable) {
                    null
                }
                if (csl != null) return csl.defaultColor
                return try {
                    androidx.core.content.ContextCompat.getColor(context, tv.resourceId)
                } catch (_: Throwable) {
                    DEFAULT_TITLE_COLOR
                }
            }
            return tv.data
        }

        /**
         * `effectiveHeightDp` を px に変換するヘルパ。
         * `Resources.displayMetrics.density` で論理 dp → 物理 px 変換する。
         */
        fun dpToPx(context: Context, dp: Int): Int {
            val density = context.resources.displayMetrics.density
            return (dp * density).toInt()
        }

        /**
         * タイトル既定色（黒、`0xFF000000`）。
         *
         * `android.graphics.Color.BLACK` の値リテラル `0xFF000000.toInt()` をそのまま記述することで、
         * 単体テスト（非 Robolectric）でも `EffectiveStyle` のロードが Android Color API に依存
         * せずに成立するようにする。
         */
        @ColorInt
        private val DEFAULT_TITLE_COLOR: Int = 0xFF000000.toInt()

        /** 説明既定色（システムグレー、おおよそ #6D6D72）。 */
        @ColorInt
        private val DEFAULT_DESCRIPTION_COLOR: Int = 0xFF6D6D72.toInt()

        // ============================================================================
        // EffectiveStyle アクセサ群（解決順序 `CellStyle → Theme → 既定`）
        //
        // 戻り値の型は Compose 型（`Color` / `TextStyle` / `Dp`）。`from(context, ...)` が
        // ARGB Int / Typeface へ変換するのは Android View 系 ViewHolder のためであり、
        // 本アクセサ群は Compose 描画系でも直接使えるよう論理型をそのまま返す。
        // ============================================================================

        /**
         * タイトル文字色を解決する。
         * 解決順序: `cellStyle.titleColor` → `theme.cellTitleColor` → `DEFAULT_CELL_TITLE_COLOR`
         */
        fun effectiveTitleColor(cellStyle: CellStyle, theme: Theme): Color {
            cellStyle.titleColor?.let { return it }
            theme.cellTitleColor?.let { return it }
            return Theme.DEFAULT_CELL_TITLE_COLOR
        }

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
         * 解決順序: `cellStyle.descriptionColor` → `theme.cellDescriptionColor` → `DEFAULT_CELL_DESCRIPTION_COLOR`
         */
        fun effectiveDescriptionColor(cellStyle: CellStyle, theme: Theme): Color {
            cellStyle.descriptionColor?.let { return it }
            theme.cellDescriptionColor?.let { return it }
            return Theme.DEFAULT_CELL_DESCRIPTION_COLOR
        }

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
         * 解決順序: `cellStyle.valueTextColor` → `theme.cellValueTextColor` → `theme.cellTitleColor` → `DEFAULT_CELL_TITLE_COLOR`
         */
        fun effectiveValueTextColor(cellStyle: CellStyle, theme: Theme): Color {
            cellStyle.valueTextColor?.let { return it }
            theme.cellValueTextColor?.let { return it }
            theme.cellTitleColor?.let { return it }
            return Theme.DEFAULT_CELL_TITLE_COLOR
        }

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
        fun effectiveHintTextColor(cellStyle: CellStyle, theme: Theme): Color {
            cellStyle.hintTextColor?.let { return it }
            theme.cellHintTextColor?.let { return it }
            return theme.cellAccentColor
        }

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
         * 解決順序: `cellStyle.placeholderColor` → `theme.cellPlaceholderColor` → プラットフォーム既定（`null`）
         *
         * 戻り値の `null` は「どの段にも指定が無い」ことを表し、描画側はホストテーマの hint 色
         * （`android:textColorHint` の `ColorStateList`）をそのまま使う。ライブラリ独自の既定色は持ち込まない。
         */
        fun effectivePlaceholderColor(cellStyle: CellStyle, theme: Theme): Color? {
            cellStyle.placeholderColor?.let { return it }
            return theme.cellPlaceholderColor
        }

        /**
         * `EntryCell.placeholderColor` 用の 4 段優先 placeholder 色解決。
         *
         * 解決順序:
         *   1. `entryPlaceholderColor`（`EntryCell` 個別フィールド、Cell 固有値が最優先）
         *   2. `cellStyle.placeholderColor`
         *   3. `theme.cellPlaceholderColor`
         *   4. プラットフォーム既定（`null`）
         */
        fun effectivePlaceholderColor(
            entryPlaceholderColor: Color?,
            cellStyle: CellStyle,
            theme: Theme,
        ): Color? {
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
        fun effectiveBackgroundColor(cellStyle: CellStyle, theme: Theme): Color {
            cellStyle.backgroundColor?.let { return it }
            return theme.cellBackgroundColor
        }

        /**
         * accent 色を解決する。
         * 解決順序: `cellStyle.accentColor` → `theme.cellAccentColor`
         */
        fun effectiveAccentColor(cellStyle: CellStyle, theme: Theme): Color {
            cellStyle.accentColor?.let { return it }
            return theme.cellAccentColor
        }

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
         *   4. `DEFAULT_BUTTON_TITLE_COLOR`（既定、ButtonCell の慣習的なアクセント色 `SYSTEM_BLUE`）
         *
         * Note: 通常 Cell のタイトル既定 `DEFAULT_CELL_TITLE_COLOR` (黒) と異なり、ButtonCell は
         * 「tappable に見える慣習色」として、クロスプラットフォーム統一の `SYSTEM_BLUE` (#FF007AFF) を
         * 既定に採る。
         * View 系 (TextView 等) の本番描画では [effectiveButtonTitleColorArgb] を使うこと。
         */
        fun effectiveButtonTitleColor(
            buttonCellTitleColor: Color?,
            cellStyle: CellStyle,
            theme: Theme,
        ): Color {
            buttonCellTitleColor?.let { return it }
            cellStyle.titleColor?.let { return it }
            theme.cellTitleColor?.let { return it }
            return Theme.DEFAULT_BUTTON_TITLE_COLOR
        }

        /**
         * `ButtonCell.titleColor` 用の 4 段優先タイトル色解決（Android View 系、ARGB Int を返す）。
         *
         * 解決順序は `ButtonCell.titleColor` → `CellStyle.titleColor` → `Theme.cellTitleColor`
         * → 固定の既定色（`SYSTEM_BLUE_ARGB` = #FF007AFF）の 4 段。SoT は本ヘルパに集約し、
         * `ButtonCellViewHolder` から直接呼ぶ。
         *
         * 4 段目はホストのテーマを参照せず固定値である。ライブラリ UI の配色はホストの XML テーマから
         * 隔離され、見た目の指定は `Theme` / `CellStyle` が正となる（android/ADR-0020）。Compose 経路の
         * [effectiveButtonTitleColor] と同じ既定色になるため、両経路の解決結果は一致する。
         */
        @ColorInt
        fun effectiveButtonTitleColorArgb(
            buttonCellTitleColor: Color?,
            cellStyle: CellStyle,
            theme: Theme,
        ): Int = effectiveButtonTitleColor(
            buttonCellTitleColor = buttonCellTitleColor,
            cellStyle = cellStyle,
            theme = theme,
        ).toArgb()

        /** iOS の `.systemBlue` 相当の ARGB Int 値（#FF007AFF）。 */
        @ColorInt
        val SYSTEM_BLUE_ARGB: Int = 0xFF007AFF.toInt()

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
