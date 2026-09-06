package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * 単一 Cell に適用される論理スタイル（UI 層所属）。
 *
 * 未指定は「Theme から継承」を意味する。色は `Color` を保持し未指定を `Color.Unspecified` で
 * 表し、フォントは `TextStyle?`、サイズは `Dp?` を保持して未指定を `null` で表す。いずれも
 * `KsColor` / `KsFont` のような中間論理表現を経由しない。
 *
 * Compose `Color` は `@JvmInline value class` であり、`data class` の自動 `equals` /
 * `hashCode` がそのまま使える。`TextStyle` も `equals` を実装している。
 *
 * スタイルは Core ではなく UI 層に属し、Native 型（Compose の `Color` / `TextStyle` / `Dp`）で
 * 表現する。
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
 * @property backgroundColor Cell 個別背景色（未指定のとき `Theme.cellBackgroundColor`）
 * @property accentColor Cell 個別 accent 色（未指定のとき `Theme.cellAccentColor`）
 * @property placeholderColor `EntryCell` の placeholder 文字色（未指定のとき `Theme.cellPlaceholderColor`）
 */
public data class CellStyle(
    val titleColor: Color = Color.Unspecified,
    val titleFont: TextStyle? = null,
    val descriptionColor: Color = Color.Unspecified,
    val descriptionFont: TextStyle? = null,
    val valueTextColor: Color = Color.Unspecified,
    val valueTextFont: TextStyle? = null,
    val iconSize: Dp? = null,
    val iconRadius: Dp? = null,
    val cellHeight: Dp? = null,
    val hintTextColor: Color = Color.Unspecified,
    val hintTextFont: TextStyle? = null,
    val backgroundColor: Color = Color.Unspecified,
    val accentColor: Color = Color.Unspecified,
    val placeholderColor: Color = Color.Unspecified,
)
