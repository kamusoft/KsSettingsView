package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * 単一 Cell に適用される論理スタイル（UI 層所属）。
 *
 * 各フィールドは nullable（`null` は「未指定 → Theme から継承」を意味する）。
 * 色は `Color?`、フォントは `TextStyle?`、サイズは `Dp?` を直接保持し、`KsColor` /
 * `KsFont` のような中間論理表現を経由しない。
 *
 * Compose `Color` は `@JvmInline value class` であり、`data class` の自動 `equals` /
 * `hashCode` がそのまま使える。`TextStyle` も `equals` を実装している。
 *
 * スタイルを Core ではなく UI 層に置き、Native 型（Compose の `Color` / `TextStyle` / `Dp`）で
 * 表現するのは core/ADR-0009 による。
 *
 * @property titleColor タイトル文字色
 * @property titleFont タイトルフォント
 * @property descriptionColor 説明文色
 * @property descriptionFont 説明文フォント
 * @property valueTextColor 値テキスト色（LabelCell / CommandCell の右寄せ値）
 * @property valueTextFont 値テキストフォント
 * @property iconSize アイコンサイズ（`Dp`）
 * @property iconRadius アイコン角丸半径（`Dp`）
 * @property cellHeight Cell 高さ（`Dp`）
 * @property hintTextColor ヒントテキスト色
 * @property hintTextFont ヒントテキストフォント
 * @property backgroundColor Cell 個別背景色（`null` のとき `Theme.cellBackgroundColor`）
 * @property accentColor Cell 個別 accent 色（`null` のとき `Theme.cellAccentColor`）
 * @property placeholderColor `EntryCell` の placeholder 文字色（`null` のとき `Theme.cellPlaceholderColor`）
 */
public data class CellStyle(
    val titleColor: Color? = null,
    val titleFont: TextStyle? = null,
    val descriptionColor: Color? = null,
    val descriptionFont: TextStyle? = null,
    val valueTextColor: Color? = null,
    val valueTextFont: TextStyle? = null,
    val iconSize: Dp? = null,
    val iconRadius: Dp? = null,
    val cellHeight: Dp? = null,
    val hintTextColor: Color? = null,
    val hintTextFont: TextStyle? = null,
    val backgroundColor: Color? = null,
    val accentColor: Color? = null,
    val placeholderColor: Color? = null,
)
