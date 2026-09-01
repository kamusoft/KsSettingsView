package jp.kamusoft.kssettingsview.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.ui.KsImage

/**
 * `DSLSettingsRootScope.Section(...)` の戻り値ハンドル。
 *
 * iOS の `Section(...)` modifier chain（`.sectionFooter("...")` 等）と整合的な
 * Compose 側の API を提供する。
 */
@SettingsRootDsl
class SectionHandle internal constructor(
    internal val scope: DSLSettingsRootScope,
    internal val index: Int,
)

/**
 * `DSLSectionScope.cell(...)` の戻り値ハンドル。
 *
 * iOS の Cell modifier chain（`.font(...)` / `.cellID("...")` 等）と整合的な
 * Compose 側の API を提供する。
 */
@SettingsRootDsl
class CellHandle internal constructor(
    internal val sectionScope: DSLSectionScope,
    internal val index: Int,
)

// =============================================================================
// SectionHandle modifier chain
// =============================================================================

/** Section に文字列ヘッダを上書き設定する。 */
fun SectionHandle.sectionHeader(text: String): SectionHandle {
    scope.updateSectionHeader(index, SectionAccessory.Text(text))
    return this
}

/** Section に任意 Composable ヘッダを上書き設定する。 */
fun SectionHandle.sectionHeader(content: @Composable () -> Unit): SectionHandle {
    scope.updateSectionHeader(index, SectionAccessory.View(KsAnyView.Compose { content() }))
    return this
}

/** Section に文字列フッタを上書き設定する。 */
fun SectionHandle.sectionFooter(text: String): SectionHandle {
    scope.updateSectionFooter(index, SectionAccessory.Text(text))
    return this
}

/** Section に任意 Composable フッタを上書き設定する。 */
fun SectionHandle.sectionFooter(content: @Composable () -> Unit): SectionHandle {
    scope.updateSectionFooter(index, SectionAccessory.View(KsAnyView.Compose { content() }))
    return this
}

/** Section に明示 ID（同一性ヒント）を設定する。 */
fun SectionHandle.sectionID(id: Any): SectionHandle {
    scope.overrideSectionIdAt(index, DSLIdentityHint.Explicit(id))
    return this
}

// =============================================================================
// CellHandle modifier chain
// =============================================================================

/** Cell のタイトル / ヒントテキスト用フォントを上書きする。 */
fun CellHandle.font(font: TextStyle): CellHandle {
    sectionScope.mutateCellStyleAt(index) { it.copy(titleFont = font) }
    return this
}

/** Cell 高さ（dp）を上書きする。 */
fun CellHandle.cellHeight(height: Dp): CellHandle {
    sectionScope.mutateCellStyleAt(index) { it.copy(cellHeight = height) }
    return this
}

/** Cell タイトル色を上書きする。 */
fun CellHandle.titleColor(color: Color): CellHandle {
    sectionScope.mutateCellStyleAt(index) { it.copy(titleColor = color) }
    return this
}

/** Cell 背景色を上書きする。 */
fun CellHandle.backgroundColor(color: Color): CellHandle {
    sectionScope.mutateCellStyleAt(index) { it.copy(backgroundColor = color) }
    return this
}

/**
 * Cell 無効化フラグの modifier。
 *
 * UI 層の `CellStyle` は無効化状態を持たないため、この modifier は引数によらず常に no-op で、
 * 受け取った [CellHandle] をそのまま返す。無効な Cell は各 Cell の initializer の `isEnabled` で
 * 構築する。
 */
@Suppress("UNUSED_PARAMETER")
fun CellHandle.disabled(flag: Boolean): CellHandle = this

/** Cell のアイコンを上書きする。 */
fun CellHandle.icon(icon: KsImage?): CellHandle {
    sectionScope.mutateCellIconAt(index, icon)
    return this
}

/** Cell の明示 ID（同一性ヒント）を設定する。 */
fun CellHandle.cellID(id: Any): CellHandle {
    sectionScope.overrideCellIdAt(index, DSLIdentityHint.Explicit(id))
    return this
}
