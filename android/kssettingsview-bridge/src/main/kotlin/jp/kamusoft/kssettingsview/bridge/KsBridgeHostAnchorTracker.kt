package jp.kamusoft.kssettingsview.bridge

import android.view.View
import android.view.ViewTreeObserver
import jp.kamusoft.kssettingsview.ui.KsScrollAnchor
import jp.kamusoft.kssettingsview.ui.KsSettingsView

/**
 * Host が window に取り付けられている間、描画のたびにその表示位置を控え続ける。
 *
 * Host の `captureScrollAnchor()` は、window から外れた後は行を持たないため `null` を返す。
 * MAUI の Handler の切断は、ページが画面から外れた後に届く（ページの view が先に階層から外れる）ため、
 * 解放の時点で控えると位置を失う。外れる直前の描画で控えた位置を、解放の時点の控えとして使う。
 *
 * 控えは描画の直前（`OnPreDrawListener`）に取り直す。スクロール・内容の反映・レイアウトはいずれも
 * 描画を伴うため、利用者が最後に見た位置が控えに残る。
 *
 * @param host 控える対象の Host
 */
internal class KsBridgeHostAnchorTracker(
    private val host: KsSettingsView,
) : View.OnAttachStateChangeListener, ViewTreeObserver.OnPreDrawListener {

    /** 取り付けられている間に最後に控えた位置。控えられる内容が無かったときは `null`。 */
    @get:JvmSynthetic
    internal var lastObservedAnchor: KsScrollAnchor? = null
        private set

    /** 描画の通知を受けている ViewTreeObserver。取り付けられていない間は `null`。 */
    private var observer: ViewTreeObserver? = null

    init {
        host.addOnAttachStateChangeListener(this)
        if (host.isAttachedToWindow) startObserving()
    }

    /**
     * Host を手放す時点の控えを返す。
     *
     * 取り付けられていれば Host の現在の位置を控える。外れていれば、未実行の位置の復元があればその控え、
     * 無ければ外れる直前に控えた位置を返す。
     */
    fun anchorForRelease(): KsScrollAnchor? {
        val current = host.captureScrollAnchor()
        if (current != null || host.isAttachedToWindow) return current
        return lastObservedAnchor
    }

    /** 控えるのをやめ、Host への登録を外す。 */
    fun stop() {
        host.removeOnAttachStateChangeListener(this)
        stopObserving()
    }

    override fun onViewAttachedToWindow(v: View) {
        startObserving()
    }

    override fun onViewDetachedFromWindow(v: View) {
        stopObserving()
    }

    override fun onPreDraw(): Boolean {
        lastObservedAnchor = host.captureScrollAnchor()
        return true
    }

    private fun startObserving() {
        if (observer != null) return
        val treeObserver = host.viewTreeObserver
        treeObserver.addOnPreDrawListener(this)
        observer = treeObserver
    }

    private fun stopObserving() {
        val treeObserver = observer ?: return
        observer = null
        // 外れる途中では Host の viewTreeObserver が別物に替わっていることがあるため、登録した相手から外す。
        if (treeObserver.isAlive) treeObserver.removeOnPreDrawListener(this)
    }
}
