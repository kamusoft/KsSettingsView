package jp.kamusoft.kssettingsview.bridge

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * 与えられた成分をそのまま返す [PaddingValues] 実装。
 *
 * Compose 標準の `PaddingValues(...)` ファクトリは「0 以上」を構築時に要求し、負の成分と NaN を
 * 拒否する。輸送層は値を検証せず素通しし、負値・非有限（NaN・±∞）の正規化は描画時に行うのが
 * Section 装飾の契約のため、interop 境界からの値はこの実装で運ぶ。
 *
 * @property start 開始側の余白
 * @property top 上側の余白
 * @property end 終了側の余白
 * @property bottom 下側の余白
 */
internal data class KsBridgeSectionMargin(
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
