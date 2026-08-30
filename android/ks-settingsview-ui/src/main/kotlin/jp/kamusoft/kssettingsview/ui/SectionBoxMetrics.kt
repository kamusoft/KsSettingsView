package jp.kamusoft.kssettingsview.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * [Theme] の Section 装飾 4 属性（`sectionMargin` / `sectionCornerRadius` /
 * `sectionBorderWidth` / `sectionBorderColor`）を style ごとの実効値へ解決した結果。
 *
 * `null` は「未指定」を表し、style ごとのライブラリ既定へ解決する。負の寸法と非有限（NaN・±∞）の
 * 寸法は 0 へ正規化し、描画側が不正な geometry を受け取らないようにする。値はすべて px。
 *
 * 水平成分は `PaddingValues` の start / end を layout direction で左右へ写した結果を保持する。
 *
 * @property marginTopPx Section 単位の外側余白（上）
 * @property marginBottomPx Section 単位の外側余白（下）
 * @property marginLeftPx Section 単位の外側余白（左。start / end から解決済み）
 * @property marginRightPx Section 単位の外側余白（右。start / end から解決済み）
 * @property cornerRadiusPx 箱の角丸半径。[KsSettingsViewStyle.Classic] では常に 0
 * @property borderWidthPx 箱のボーダー幅。[KsSettingsViewStyle.Classic] では常に 0
 * @property borderColor 箱のボーダー色（`@ColorInt`）。[KsSettingsViewStyle.Classic] では常に透明
 */
internal data class SectionBoxMetrics(
    val marginTopPx: Float,
    val marginBottomPx: Float,
    val marginLeftPx: Float,
    val marginRightPx: Float,
    val cornerRadiusPx: Float,
    val borderWidthPx: Float,
    val borderColor: Int,
) {

    /**
     * 箱の寸法に対して幾何的に許される角丸半径へ clamp する。
     *
     * 半径が箱の短辺の半分を超えると描画結果が破綻するため、描画の直前にここで抑える。
     *
     * @param width 箱の幅（px）
     * @param height 箱の高さ（px）
     */
    fun clampedCornerRadius(width: Float, height: Float): Float {
        val limit = min(width, height) / 2.0f
        if (limit <= 0.0f) return 0.0f
        return min(cornerRadiusPx, limit)
    }

    companion object {

        /** [KsSettingsViewStyle.Modern] の既定余白（生値は両 platform で統一 — core/ADR-0027）。 */
        val MODERN_DEFAULT_MARGIN: PaddingValues =
            PaddingValues(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 0.dp)

        /**
         * [KsSettingsViewStyle.Classic] の既定余白（[MODERN_DEFAULT_MARGIN] と同値。core/ADR-0027）。
         *
         * Classic / Modern 切り替え時に Section の上下間隔が変わらないよう上下を揃える。
         * 水平成分は [resolve] が Classic で常に 0 へ落とす（Section 境界を全幅に保つ）ため、
         * 値としては Modern と同値でも表示には現れない。
         */
        val CLASSIC_DEFAULT_MARGIN: PaddingValues = MODERN_DEFAULT_MARGIN

        /** [KsSettingsViewStyle.Modern] の既定角丸半径（両 platform で生値 26 に統一。core/ADR-0024）。 */
        val MODERN_DEFAULT_CORNER_RADIUS: Dp = 26.dp

        /** ボーダー幅の既定（既定の Modern にボーダーは描かない）。 */
        val DEFAULT_BORDER_WIDTH: Dp = 0.dp

        /** ボーダー色の既定（透明）。 */
        val DEFAULT_BORDER_COLOR: Color = Color.Transparent

        /**
         * [Theme] と style から実効値を解決する。
         *
         * - `null` の属性は style ごとの既定へ解決する。
         * - 負および非有限（NaN・±∞）の余白成分・ボーダー幅・角丸半径は 0 として扱う。
         * - [KsSettingsViewStyle.Classic] は箱を描かないため角丸・ボーダーを 0 / 透明に落とし、
         *   余白の水平成分も 0 にする（Section 境界を全幅に保つ）。
         *
         * @param theme 解決元の Theme
         * @param style 見た目スタイル
         * @param density `DisplayMetrics.density`（dp → px 換算係数）
         * @param layoutDirection start / end を左右へ写すための layout direction
         */
        fun resolve(
            theme: Theme,
            style: KsSettingsViewStyle,
            density: Float,
            layoutDirection: LayoutDirection,
        ): SectionBoxMetrics {
            val isModern = (style == KsSettingsViewStyle.Modern)
            val margin = theme.sectionMargin
                ?: if (isModern) MODERN_DEFAULT_MARGIN else CLASSIC_DEFAULT_MARGIN

            // 非有限（NaN・±∞）は px 換算より前に 0 へ落とす。
            // NaN は max(0, ·) を素通りするため、描画側の roundToInt() へ渡すと例外になる。
            fun px(value: Dp): Float {
                val raw = value.value
                if (!raw.isFinite()) return 0.0f
                return max(0.0f, raw) * density
            }

            return SectionBoxMetrics(
                marginTopPx = px(margin.calculateTopPadding()),
                marginBottomPx = px(margin.calculateBottomPadding()),
                marginLeftPx = if (isModern) px(margin.calculateLeftPadding(layoutDirection)) else 0.0f,
                marginRightPx = if (isModern) px(margin.calculateRightPadding(layoutDirection)) else 0.0f,
                cornerRadiusPx = if (isModern) {
                    px(theme.sectionCornerRadius ?: MODERN_DEFAULT_CORNER_RADIUS)
                } else {
                    0.0f
                },
                borderWidthPx = if (isModern) {
                    px(theme.sectionBorderWidth ?: DEFAULT_BORDER_WIDTH)
                } else {
                    0.0f
                },
                borderColor = if (isModern) {
                    (theme.sectionBorderColor ?: DEFAULT_BORDER_COLOR).toArgb()
                } else {
                    DEFAULT_BORDER_COLOR.toArgb()
                },
            )
        }
    }
}
