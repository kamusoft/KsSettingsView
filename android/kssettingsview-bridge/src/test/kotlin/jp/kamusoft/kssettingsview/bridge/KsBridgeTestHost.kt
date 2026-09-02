package jp.kamusoft.kssettingsview.bridge

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as MaterialR
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

/**
 * Bridge のテストで表示内容を観察するためのユーティリティ。
 *
 * Bridge の公開 API 経由で Host を生成し、Activity へ載せて実描画を確定させる。検証は内部状態では
 * なく、実描画された行テキストと Adapter が発行した通知で行う。
 */
internal object KsBridgeTestHost {

    /** Bridge が生成した Host を 1 つだけ載せるホスト Activity。 */
    class HostActivity : FragmentActivity() {
        lateinit var container: FrameLayout

        override fun onCreate(savedInstanceState: Bundle?) {
            // MaterialSwitch 等が要求する Theme.Material3.* 派生テーマを適用する。
            setTheme(MaterialR.style.Theme_Material3_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            container = FrameLayout(this)
            setContentView(container)
        }
    }

    /** Activity に載せた Host を保持する。 */
    class Attachment(
        val controller: ActivityController<HostActivity>,
        val hostView: View,
    ) {
        /** Host 内部の `RecyclerView`。 */
        val recyclerView: RecyclerView
            get() = (hostView as ViewGroup).getChildAt(0) as RecyclerView

        /** Host を view 階層から取り外す。解放した Host の残置と取り外しを撃ち分けるために使う。 */
        fun removeHost() {
            (hostView.parent as? ViewGroup)?.removeView(hostView)
        }

        /** Activity を閉じる。 */
        fun close() {
            controller.close()
        }
    }

    /** Bridge から Host を生成し、新しい Activity に載せて実描画を確定させる。 */
    fun attach(bridge: KsSettingsBridge): Attachment =
        attach(bridge, Robolectric.buildActivity(HostActivity::class.java).setup())

    /**
     * Bridge から Host を生成し、既存のホスト Activity に載せて実描画を確定させる。
     *
     * 解放と再生成をまたぐ検証で、同じ Activity 上に Host を載せ替えるために使う。
     */
    fun attach(
        bridge: KsSettingsBridge,
        controller: ActivityController<HostActivity>,
    ): Attachment = attach(bridge, controller, controller.get())

    /**
     * 既存のホスト Activity 上で、指定した `Context` から生成した Host を載せて実描画を確定させる。
     *
     * Host の生成に使う `Context` を Activity 以外に差し替えられるようにするための入口。
     */
    fun attach(
        bridge: KsSettingsBridge,
        controller: ActivityController<HostActivity>,
        context: Context,
    ): Attachment {
        val host = bridge.makeHostView(context) ?: error("Bridge が Native Host を返さなかった")
        controller.get().container.addView(host)
        val attachment = Attachment(controller = controller, hostView = host)
        pump(attachment)
        return attachment
    }

    /**
     * 差分計算の完了を待ってレイアウトを確定させる。
     *
     * `submitList` の差分計算は両リストが非空のときバックグラウンドスレッドへ回り、結果が
     * メインスレッドへ post されるまでにわずかな時間がかかる。単発の `idle()` では取りこぼすため、
     * 短い待ちを挟んで繰り返してからレイアウトを走らせる。
     */
    fun pump(attachment: Attachment) {
        repeat(30) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
        shadowOf(Looper.getMainLooper()).idle()
        layout(attachment)
    }

    /** Host を実寸でレイアウトし、`RecyclerView` に行を生成させる。 */
    private fun layout(attachment: Attachment) {
        val metrics = attachment.controller.get().resources.displayMetrics
        val host = attachment.hostView
        host.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        host.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    /**
     * 実描画された行のテキストを並び順に返す。
     *
     * Section header / footer 行と Cell 行の双方を含み、1 行あたりの全 `TextView` のテキストを
     * `/` で連結する。Cell の内容・並び順・accessory の表示をまとめて観察するための表現。
     */
    fun renderedRows(attachment: Attachment): List<String> {
        val rv = attachment.recyclerView
        val count = rv.adapter?.itemCount ?: 0
        return (0 until count).map { position ->
            val holder = rv.findViewHolderForAdapterPosition(position)
                ?: error("position $position の行が実描画されていない")
            textOf(holder.itemView)
        }
    }

    /**
     * 実描画された Cell 行のテキストのみを並び順に返す。
     *
     * Section header / footer 行は itemView が `TextView` そのものであるのに対し、Cell 行は
     * `ViewGroup` を itemView に持つため、この違いで選り分ける。
     */
    fun renderedCellTexts(attachment: Attachment): List<String> {
        val rv = attachment.recyclerView
        val count = rv.adapter?.itemCount ?: 0
        return (0 until count).mapNotNull { position ->
            val itemView = rv.findViewHolderForAdapterPosition(position)?.itemView ?: return@mapNotNull null
            if (itemView is TextView) null else textOf(itemView)
        }
    }

    /** View 配下の全 `TextView` のテキストを `/` で連結する。 */
    private fun textOf(view: View): String = collectTexts(view).joinToString("/")

    private fun collectTexts(view: View): List<String> = when (view) {
        is TextView -> listOfNotNull(view.text?.toString()?.takeIf { it.isNotBlank() })
        is ViewGroup -> (0 until view.childCount).flatMap { collectTexts(view.getChildAt(it)) }
        else -> emptyList()
    }
}
