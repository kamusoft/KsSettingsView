package jp.kamusoft.kssettingsview.bridge

import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.ui.KsSettingsView

/**
 * Host の Adapter が発行した通知を種別ごとに数える観察用オブザーバ。
 *
 * Bridge の各操作が「構造変更」「内容更新」「Theme 再評価」のどれとして Host へ届いたかを、
 * 内部状態ではなく Adapter の通知から判定するために使う。
 */
internal class KsBridgeAdapterRecorder : RecyclerView.AdapterDataObserver() {

    /** 構造変更（挿入・削除・移動・全件無効化）の通知件数。 */
    var structuralCount: Int = 0
        private set

    /** 内容更新（`Theme` 以外の payload を伴う範囲変更）の通知件数。 */
    var contentChangeCount: Int = 0
        private set

    /** Theme 再評価（`KsSettingsView.PAYLOAD_THEME` を伴う範囲変更）の通知件数。 */
    var themeChangeCount: Int = 0
        private set

    /** 全通知件数。 */
    val totalCount: Int
        get() = structuralCount + contentChangeCount + themeChangeCount

    override fun onChanged() {
        structuralCount++
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        contentChangeCount += itemCount
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
        if (payload == KsSettingsView.PAYLOAD_THEME) {
            themeChangeCount += itemCount
        } else {
            contentChangeCount += itemCount
        }
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        structuralCount += itemCount
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        structuralCount += itemCount
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        structuralCount += itemCount
    }

    /** 記録を破棄して登録を解除する。 */
    fun detach(attachment: KsBridgeTestHost.Attachment) {
        attachment.recyclerView.adapter?.unregisterAdapterDataObserver(this)
    }

    companion object {
        /** Host の Adapter に観察を開始する。 */
        fun attach(attachment: KsBridgeTestHost.Attachment): KsBridgeAdapterRecorder {
            val recorder = KsBridgeAdapterRecorder()
            attachment.recyclerView.adapter?.registerAdapterDataObserver(recorder)
            return recorder
        }
    }
}
