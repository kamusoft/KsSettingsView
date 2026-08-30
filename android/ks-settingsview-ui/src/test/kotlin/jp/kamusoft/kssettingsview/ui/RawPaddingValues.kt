package jp.kamusoft.kssettingsview.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * 与えられた成分をそのまま返す [PaddingValues] 実装。
 *
 * Compose 標準の `PaddingValues(...)` ファクトリは「0 以上」を構築時に要求し、負の成分と NaN を
 * 拒否するため、「不正な成分が描画側で 0 へ正規化される」ことを確かめるテストではこちらを使う。
 *
 * @property start 開始側の余白
 * @property top 上側の余白
 * @property end 終了側の余白
 * @property bottom 下側の余白
 */
internal data class RawPaddingValues(
    val start: Dp,
    val top: Dp,
    val end: Dp,
    val bottom: Dp,
) : PaddingValues {

    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        if (layoutDirection == LayoutDirection.Ltr) start else end

    override fun calculateTopPadding(): Dp = top

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        if (layoutDirection == LayoutDirection.Ltr) end else start

    override fun calculateBottomPadding(): Dp = bottom
}
