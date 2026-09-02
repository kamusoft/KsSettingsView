package jp.kamusoft.kssettingsview.compose

import androidx.compose.runtime.Composable
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.CustomCell
import jp.kamusoft.kssettingsview.ui.CustomCellEmptyContent

/**
 * [CustomCell] を `DSLSectionScope` に直置きするための拡張関数群。
 *
 * 事前登録なしで `Section("...") { CustomCell(content = x) { ... } }` と書ける
 * （core/ADR-0014）。戻り値の `CellHandle` から `.cellHeight(...)` / `.cellID(...)` 等の
 * Cell modifier chain が使える。アイコン領域を持たないため `.icon(...)` は効かない。
 */

// =============================================================================
// 構築ヘルパ（同名拡張関数のシャドウ回避）
// =============================================================================

private fun <C : Any> buildCustomCell(
    content: C,
    showArrow: Boolean,
    style: CellStyle,
    onTap: (() -> Unit)?,
    isEnabled: Boolean,
    isVisible: Boolean,
    builder: @Composable (C) -> Unit,
): CustomCell<C> = CustomCell(
    style = style,
    content = content,
    showArrow = showArrow,
    onTap = onTap,
    isEnabled = isEnabled,
    isVisible = isVisible,
    builder = builder,
)

private fun buildStaticCustomCell(
    showArrow: Boolean,
    style: CellStyle,
    onTap: (() -> Unit)?,
    isEnabled: Boolean,
    isVisible: Boolean,
    builder: @Composable () -> Unit,
): CustomCell<CustomCellEmptyContent> = CustomCell(
    style = style,
    showArrow = showArrow,
    onTap = onTap,
    isEnabled = isEnabled,
    isVisible = isVisible,
    builder = builder,
)

// =============================================================================
// DSL 拡張関数
// =============================================================================

/**
 * `Section { CustomCell(content = value) { v -> ... } }` のように任意 Compose コンテンツの行を
 * 直置きする。
 *
 * @param content 描画の元になる値。値等価（`equals` / `hashCode`）を正しく実装した型を渡すこと
 * @param showArrow `true` で trailing に Disclosure Indicator を表示する
 * @param style Cell 個別スタイル。行レベルの項目（背景色・cellHeight）だけが効く
 * @param onTap 行タップ時のコールバック
 * @param isEnabled `false` で行タップと content 内部の操作を抑止する
 * @param isVisible `false` で visible projection から除外される
 * @param builder [content] から Compose コンテンツを組み立てる
 */
public fun <C : Any> DSLSectionScope.CustomCell(
    content: C,
    showArrow: Boolean = false,
    style: CellStyle = CellStyle(),
    onTap: (() -> Unit)? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    builder: @Composable (C) -> Unit,
): CellHandle = cell(
    buildCustomCell(
        content = content,
        showArrow = showArrow,
        style = style,
        onTap = onTap,
        isEnabled = isEnabled,
        isVisible = isVisible,
        builder = builder,
    ),
)

/**
 * `Section { CustomCell { ... } }` のように content を持たない静的コンテンツの行を直置きする。
 *
 * @param showArrow `true` で trailing に Disclosure Indicator を表示する
 * @param style Cell 個別スタイル。行レベルの項目（背景色・cellHeight）だけが効く
 * @param onTap 行タップ時のコールバック
 * @param isEnabled `false` で行タップと content 内部の操作を抑止する
 * @param isVisible `false` で visible projection から除外される
 * @param builder 表示する Compose コンテンツ
 */
public fun DSLSectionScope.CustomCell(
    showArrow: Boolean = false,
    style: CellStyle = CellStyle(),
    onTap: (() -> Unit)? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    builder: @Composable () -> Unit,
): CellHandle = cell(
    buildStaticCustomCell(
        showArrow = showArrow,
        style = style,
        onTap = onTap,
        isEnabled = isEnabled,
        isVisible = isVisible,
        builder = builder,
    ),
)
