package jp.kamusoft.kssettingsview.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.DSLIconModifiableCell
import jp.kamusoft.kssettingsview.ui.DSLStyleModifiableCell
import jp.kamusoft.kssettingsview.ui.KsImage

/**
 * `Cell` 向け DSL Modifier 拡張関数。
 *
 * すべて data class copy で新インスタンスを返す（イミュータブル）。
 * Cell が [DSLStyleModifiableCell] を実装している場合のみ `style` 系 modifier が動作する。
 *
 * `CellStyle` は Core ではなく UI 層（`jp.kamusoft.kssettingsview.ui` パッケージ）に属し、
 * 色・フォントの引数型は
 * Compose の Native 型（`Color` / `TextStyle`）を直接使う（core/ADR-0009）。
 */

/** タイトル / ヒントテキスト用フォントを上書きする。 */
public fun Cell.font(font: TextStyle): Cell {
    return mutateStyle { it.copy(titleFont = font) }
}

/** Cell 高さ（dp）を上書きする。 */
public fun Cell.cellHeight(height: Dp): Cell {
    return mutateStyle { it.copy(cellHeight = height) }
}

/** タイトル色を上書きする。 */
public fun Cell.titleColor(color: Color): Cell {
    return mutateStyle { it.copy(titleColor = color) }
}

/** 背景色を上書きする。 */
public fun Cell.backgroundColor(color: Color): Cell {
    return mutateStyle { it.copy(backgroundColor = color) }
}

/**
 * Cell 無効化フラグの暫定 modifier。
 *
 * Core 層の `CellStyle` には `disabled` フィールドが存在しないため、本提案の範囲では
 * no-op として扱う（後続提案で正式対応）。
 */
@Suppress("UNUSED_PARAMETER")
public fun Cell.disabled(flag: Boolean): Cell = this

/**
 * Cell のアイコンを上書きする。
 *
 * Cell が [DSLIconModifiableCell] を実装している場合のみ反映され、未実装な Cell に
 * 対しては no-op になる（アイコン領域を持たない `CustomCell`）。
 */
public fun Cell.icon(icon: KsImage?): Cell {
    return if (this is DSLIconModifiableCell) {
        this.withDSLIcon(icon)
    } else {
        this
    }
}

/**
 * Cell の明示 ID を指定する。
 *
 * 仕様 `settings-view-android-ui` "Cell ID 判定の優先順位 2"：
 * `Cell.cellID(id: Any)` 拡張関数で明示指定された場合はその値を Cell ID 採番ヒントとして
 * 採用しなければならない。本実装は **メソッドチェーン形式** で利用された Cell を
 * [DSLExplicitIdCell] で **ラップ** して返す sentinel パターンを採用する。
 */
public fun Cell.cellID(id: Any): Cell {
    // すでにラップ済みの場合は最新の明示 ID で上書きする。
    val baseCell = if (this is DSLExplicitIdCell) this.wrapped else this
    return DSLExplicitIdCell(wrapped = baseCell, explicitId = id)
}

/** `style` のみを書き換える内部ヘルパ。 */
internal fun Cell.mutateStyle(transform: (CellStyle) -> CellStyle): Cell {
    return if (this is DSLStyleModifiableCell) {
        withDSLStyle(transform(this.style))
    } else {
        this
    }
}
