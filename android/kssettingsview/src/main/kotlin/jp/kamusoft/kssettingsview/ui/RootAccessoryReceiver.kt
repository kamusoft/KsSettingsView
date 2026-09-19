package jp.kamusoft.kssettingsview.ui

import jp.kamusoft.kssettingsview.core.SettingsRootDiff

/**
 * Root の Header / Footer の更新を [SettingsRootStore] から直接知らされる相手。
 *
 * Root 対象は Store の現在状態に含まれない（core/ADR-0005）ため、通知を取りこぼすと復元できない。
 * Store は結び付いている相手をこの口で覚え、更新をその場で知らせる（core/ADR-0033）。
 *
 * 知らせはどのスレッドからでも届き得る。受け取り側は、渡された配送元が現在結び付いている Store と
 * 一致する更新だけを受理する。
 */
internal interface RootAccessoryReceiver {

    /**
     * Root 対象の更新を受け取る。
     *
     * @param source この更新を知らせた Store
     * @param update Root の Header / Footer の更新
     */
    fun receiveRootAccessoryUpdate(source: SettingsRootStore, update: SettingsRootDiff.UpdateAccessory)
}
