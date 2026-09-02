package jp.kamusoft.kssettingsview.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * SettingsView 全体に適用される論理スタイル（UI 層所属）。
 *
 * フィールド型は Compose Native 型 `androidx.compose.ui.graphics.Color` /
 * `androidx.compose.ui.text.TextStyle` / `androidx.compose.ui.unit.Dp` を直接保持する。
 * `KsColor` / `KsFont` のような独自論理表現を経由しないため、利用者は Compose の慣れた色・
 * フォント API をそのまま渡せる。
 *
 * Compose `Color` は `@JvmInline value class Color(val value: ULong)` であり、
 * `data class` のフィールドとして自然に `equals` / `hashCode` に参加する。
 * `TextStyle` は通常のクラスだが `equals` を実装しているため `data class` フィールドとして
 * 利用可能。
 *
 * 「Cell 全体既定」フィールド群（`cellTitleColor` / `cellDescriptionColor` 等）は
 * 個別 Cell の `CellStyle.X` が `null` のときの **フォールバック値** として `EffectiveStyle`
 * 経由で参照される（解決順序: `CellStyle.X` → `Theme.cellX` → プラットフォーム既定）。
 *
 * `cellTitleFontSize` は `cellTitleFont` と並立する独立 `Double` フィールドで、
 * `> 0` のとき `cellTitleFont.fontSize` を **上書き** する（オリジナル
 * `AiForms.Maui.SettingsView.SettingsView.CellTitleFontSize` との運用互換のため）。
 *
 * 注意（`TextStyle?` 経由の等価性）：
 *   本 `data class` の自動生成 `equals` は Compose `TextStyle.equals` に委譲する。
 *   Compose `TextStyle` の `equals` は内部の `SpanStyle` / `ParagraphStyle` を比較し、
 *   `Color` / `fontWeight` / `fontSize` などの value class 系の差分は安定して検出できる。
 *   ただし `FontFamily` / `Brush` は実装上一部参照比較に依存することがあるため、
 *   Compose のメジャーアップグレード時には挙動回帰に注意する。
 *
 * Theme は Core ではなく UI 層に属し、色・フォント・サイズを Compose の Native 型
 * （`Color` / `TextStyle` / `Dp`）で保持する（core/ADR-0009）。
 *
 * @property separatorColor セパレータ色
 * @property backgroundColor SettingsView（`RecyclerView`）自身の背景色。`cellBackgroundColor` とは独立
 * @property cellBackgroundColor Cell 既定背景色
 * @property selectedColor Cell 選択時の背景色
 * @property cellAccentColor アクセント色（選択系 Cell の着色既定値）
 * @property disabledTextColor `isEnabled = false` 時のテキスト置換色
 * @property scrollIndicatorVisible スクロールインジケータ表示
 * @property rowHeight 行高さ基準値（論理単位、整数）。`-1` は未指定
 * @property hasUnevenRows 可変高さフラグ（`true` で個別 Cell ごとに可変 + 最低高さ保証、`false` で全 Cell 一律固定）。
 *   既定値は `true`（Auto 高さ + 下限保証）。これはオリジナル AiForms Android
 *   （`AiRecyclerView.UpdateRowHeight()` が `RowHeight = -1` のとき `60` を自動セットしつつ
 *   `cellbaseview.axml` 上は MinHeight 扱い）の挙動踏襲。
 *   「全 Cell を一律固定高さで揃えたい」場合のみ `Theme(hasUnevenRows = false)` を明示指定する。
 * @property headerTextColor Section ヘッダのテキスト色
 * @property headerBackgroundColor Section ヘッダの背景色
 * @property headerFontSize Section ヘッダ既定フォントサイズ（論理単位、`-1` は未指定）
 * @property headerFont Section ヘッダ既定フォント（family / weight / 装飾を含む。`null` は未指定）
 * @property headerHeight Section ヘッダの既定高さ（論理単位）。`-1.0` は未指定（自動）
 * @property footerTextColor Section フッタのテキスト色
 * @property footerBackgroundColor Section フッタの背景色
 * @property footerFontSize Section フッタ既定フォントサイズ（論理単位、`-1` は未指定）
 * @property footerFont Section フッタ既定フォント（`null` は未指定）
 * @property cellTitleColor Cell タイトル既定色（`null` は未指定 → プラットフォーム既定）
 * @property cellTitleFont Cell タイトル既定フォント（`null` は未指定 → プラットフォーム既定）
 * @property cellTitleFontSize Cell タイトル既定フォントサイズ（独立 `Double`、`-1.0` は未指定）。
 *   `> 0` のとき `cellTitleFont.fontSize` を上書きする
 * @property cellValueTextColor valueText 既定色。`null` は未指定 → `cellTitleColor` 等にフォールバック
 * @property cellValueTextFont valueText 既定フォント。`null` は未指定 → `cellTitleFont` 等にフォールバック
 * @property cellDescriptionColor description 既定色。`null` は未指定 → グレー（#6D6D72）相当にフォールバック
 * @property cellDescriptionFont description 既定フォント。`null` は未指定 → caption 系にフォールバック
 * @property cellHintTextColor hintText 既定色。`null` は未指定 → `cellAccentColor` にフォールバック
 * @property cellHintFont hintText 既定フォント。`null` は未指定 → footnote 相当にフォールバック
 * @property cellIconSize アイコンの既定サイズ（正方形の一辺 dp）。`null` は未指定 → 24dp
 * @property cellIconRadius アイコンの既定角丸半径（dp）。`null` は未指定 → 0dp（角丸なし）
 * @property sectionMargin Section 単位（Header・Cell の箱・Footer を一体とした表示単位）の**外側**余白。
 *   水平成分は start / end 基準で解釈する。`null` は未指定 → style ごとの既定
 *   （[SectionBoxMetrics] が解決する）。等価比較は `PaddingValues` の `equals` に委譲するため、
 *   可変な独自 `PaddingValues` 実装を同一参照のまま書き換えた場合の再描画は保証しない
 * @property sectionCornerRadius Modern の箱の角丸半径。`null` は未指定 → style ごとの既定
 * @property sectionBorderWidth Modern の箱のボーダー幅。`null` は未指定 → 実効 0dp（ボーダーなし）
 * @property sectionBorderColor Modern の箱のボーダー色。`null` は未指定 → 実効透明
 * @property cellPlaceholderColor `EntryCell` の placeholder 既定色。`null` は未指定 → プラットフォーム既定
 *   （ホストテーマの hint 色）にフォールバックし、ライブラリ独自の既定色を持ち込まない
 */
public data class Theme(
    val separatorColor: Color = DEFAULT_SEPARATOR_COLOR,
    val backgroundColor: Color = DEFAULT_BACKGROUND_COLOR,
    val cellBackgroundColor: Color = Color.White,
    val selectedColor: Color = DEFAULT_SELECTED_COLOR,
    val cellAccentColor: Color = DEFAULT_ACCENT_COLOR,
    val disabledTextColor: Color = DEFAULT_DISABLED_TEXT_COLOR,
    val scrollIndicatorVisible: Boolean = true,
    val rowHeight: Int = -1,
    val hasUnevenRows: Boolean = true,
    val headerTextColor: Color = DEFAULT_HEADER_TEXT_COLOR,
    val headerBackgroundColor: Color = DEFAULT_HEADER_BACKGROUND_COLOR,
    val headerFontSize: Double = -1.0,
    val headerFont: TextStyle? = null,
    val headerHeight: Double = -1.0,
    val footerTextColor: Color = DEFAULT_FOOTER_TEXT_COLOR,
    val footerBackgroundColor: Color = DEFAULT_FOOTER_BACKGROUND_COLOR,
    val footerFontSize: Double = -1.0,
    val footerFont: TextStyle? = null,
    val cellTitleColor: Color? = null,
    val cellTitleFont: TextStyle? = null,
    val cellTitleFontSize: Double = -1.0,
    val cellValueTextColor: Color? = null,
    val cellValueTextFont: TextStyle? = null,
    val cellDescriptionColor: Color? = null,
    val cellDescriptionFont: TextStyle? = null,
    val cellHintTextColor: Color? = null,
    val cellHintFont: TextStyle? = null,
    val cellIconSize: Dp? = null,
    val cellIconRadius: Dp? = null,
    val sectionMargin: PaddingValues? = null,
    val sectionCornerRadius: Dp? = null,
    val sectionBorderWidth: Dp? = null,
    val sectionBorderColor: Color? = null,
    val cellPlaceholderColor: Color? = null,
) {
    public companion object {
        /** システム標準の灰色 separator（おおよそ #C8C7CC） */
        public val DEFAULT_SEPARATOR_COLOR: Color = Color(0xFFC8C7CC)

        /** 選択時のグレー（おおよそ #D9D9D9） */
        public val DEFAULT_SELECTED_COLOR: Color = Color(0xFFD9D9D9)

        /**
         * アクセント既定色（システム強調色相当の青、おおよそ #007AFF）。
         * iOS の tint / Material のアクセントに合わせたクロスプラットフォーム既定値。
         */
        public val DEFAULT_ACCENT_COLOR: Color = Color(0xFF007AFF)

        /** ヘッダ既定背景色（システムグループ化背景に近い #F2F2F7） */
        public val DEFAULT_HEADER_BACKGROUND_COLOR: Color = Color(0xFFF2F2F7)

        /** フッタ既定背景色（現状はヘッダと同値だが、将来的に独立進化できるよう別定数として宣言） */
        public val DEFAULT_FOOTER_BACKGROUND_COLOR: Color = DEFAULT_HEADER_BACKGROUND_COLOR

        /** ヘッダ既定テキスト色（おおよそ #6D6D72） */
        public val DEFAULT_HEADER_TEXT_COLOR: Color = Color(0xFF6D6D72)

        /** フッタ既定テキスト色（ヘッダと同色） */
        public val DEFAULT_FOOTER_TEXT_COLOR: Color = Color(0xFF6D6D72)

        /** SettingsView 全体の既定背景色（白系、`cellBackgroundColor` と同等のニュートラル既定）。 */
        public val DEFAULT_BACKGROUND_COLOR: Color = Color(0xFFFFFFFF)

        /** `isEnabled = false` 時のテキスト色（やや薄い灰色、おおよそ #999999） */
        public val DEFAULT_DISABLED_TEXT_COLOR: Color = Color(0xFF999999)

        // ===== Cell 全体既定 / フォールバック先既定値（EffectiveStyle と共有） =====

        /** `cellTitleColor` 未指定時のフォールバック色（黒）。 */
        public val DEFAULT_CELL_TITLE_COLOR: Color = Color(0xFF000000)

        /**
         * ButtonCell の `titleColor` 4 段解決で「いずれも未指定」のときに使う既定色。
         *
         * iOS の `.systemBlue` (#FF007AFF) と一致する青色。Compose 経路・View 経路のいずれも
         * ホストのテーマを参照せず本定数を既定に採るため、両経路の解決結果は一致する
         * (android/ADR-0020)。`EffectiveStyle.effectiveButtonTitleColor` /
         * `EffectiveStyle.effectiveButtonTitleColorArgb` の 4 段目フォールバック値として参照される。
         */
        public val DEFAULT_BUTTON_TITLE_COLOR: Color = Color(0xFF007AFF)

        /** `cellDescriptionColor` 未指定時のフォールバック色（やや薄いグレー、おおよそ #6D6D72）。 */
        public val DEFAULT_CELL_DESCRIPTION_COLOR: Color = Color(0xFF6D6D72)

        /** `cellIconSize` 未指定時のフォールバックサイズ（24dp 相当のスカラー値、Dp は呼び出し側で解釈）。 */
        public const val DEFAULT_CELL_ICON_SIZE_DP_VALUE: Float = 24.0f

        /** `cellIconRadius` 未指定時のフォールバック半径（0dp = 角丸なし）。 */
        public const val DEFAULT_CELL_ICON_RADIUS_DP_VALUE: Float = 0.0f
    }
}
