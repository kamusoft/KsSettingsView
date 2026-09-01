package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle

/**
 * 見た目スタイルの序数と [KsSettingsViewStyle] を橋渡しする。
 *
 * interop 境界では enum をそのまま渡せないため、スタイルは列挙の序数（Classic = 0 / Modern = 1）
 * で表す。定義域外の序数は [KsSettingsViewStyle.Classic] へ正規化する — 呼び出し側の公開契約が
 * 「非 nullable・既定 Classic」であり、未定義の値に倒す先を持たないため。
 */
internal object KsBridgeStyle {

    /**
     * 輸送された序数を [KsSettingsViewStyle] へ変換する。
     *
     * @param ordinal 見た目スタイルの序数
     * @return 対応するスタイル。定義域外の序数では [KsSettingsViewStyle.Classic]
     */
    fun style(ordinal: Int): KsSettingsViewStyle = when (ordinal) {
        1 -> KsSettingsViewStyle.Modern
        else -> KsSettingsViewStyle.Classic
    }
}
