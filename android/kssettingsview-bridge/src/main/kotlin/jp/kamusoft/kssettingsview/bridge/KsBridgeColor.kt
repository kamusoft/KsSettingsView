package jp.kamusoft.kssettingsview.bridge

import androidx.compose.ui.graphics.Color

/**
 * interop 境界の色表現（ARGB を詰めた 32bit 整数）と Compose の `Color` を橋渡しする。
 *
 * 色は platform の色型を直接渡せないため、`0xAARRGGBB` の並びで整数化して運ぶ
 * （maui/ADR-0004）。`null` は「未指定」を意味し、上位の既定（Theme 継承など）へ倒す。
 */
internal object KsBridgeColor {

    /**
     * ARGB を詰めた 32bit 整数を `Color` へ変換する。
     *
     * @param argb `0xAARRGGBB` の並びの整数（未指定は `null`）
     * @return 変換した色。未指定のときは `null`
     */
    fun color(argb: Int?): Color? = argb?.let { Color(it) }
}
