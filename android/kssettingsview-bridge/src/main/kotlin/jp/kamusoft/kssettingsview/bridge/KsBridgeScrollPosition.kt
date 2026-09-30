package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.ui.KsScrollPosition

/**
 * スクロール命令の位置の整数と [KsScrollPosition] を橋渡しする。
 *
 * interop 境界では enum をそのまま渡せないため、位置は整数（Start = 0 / Center = 1 / End = 2）で
 * 表す。定義域外の値は [KsScrollPosition.Start] として扱う — 命令の位置の既定が Start であり、
 * 未定義の値で命令を捨てるより既定の合わせ方で届けるほうが呼び出し側の意図に近いため。
 */
internal object KsBridgeScrollPosition {

    /**
     * 輸送された整数を [KsScrollPosition] へ変換する。
     *
     * @param value 位置の整数
     * @return 対応する位置。定義域外の値では [KsScrollPosition.Start]
     */
    fun position(value: Int): KsScrollPosition = when (value) {
        1 -> KsScrollPosition.Center
        2 -> KsScrollPosition.End
        else -> KsScrollPosition.Start
    }
}
