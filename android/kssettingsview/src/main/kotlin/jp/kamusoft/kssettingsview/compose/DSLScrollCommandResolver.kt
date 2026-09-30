package jp.kamusoft.kssettingsview.compose

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.ui.KsCellRegistry
import jp.kamusoft.kssettingsview.ui.KsScrollCommand
import jp.kamusoft.kssettingsview.ui.KsScrollCommandReceiver
import jp.kamusoft.kssettingsview.ui.KsScrollController
import java.lang.ref.WeakReference
import jp.kamusoft.kssettingsview.ui.KsSettingsView as KsSettingsViewLayout

/**
 * DSL 方式の `KsSettingsView(...)` の命令ハンドルの受け口。ハンドルは Host ではなくこの受け口に接続する。
 *
 * 利用者が知っているのは自分で書いた明示 ID (`cellID(...)` / `sectionID(...)`) か `forEach` の key
 * だけなので、それを最終 ID ([DSLIdentityId.id] で決まる文字列) へ引き直してから Host の待ち行列へ渡す。
 *
 * 引き直しは、命令を受けた処理の中で起きた状態変更による宣言の更新が内部 Store へ届いてから行う。
 * 受けた命令は Compose の snapshot state ([enqueuedCount]) の変化として知らせ、ラッパーの
 * `LaunchedEffect` がコンポジションのフレームを 1 回待ってから [resolvePending] を呼ぶ。同じ処理で
 * 行った状態変更は、同じ snapshot の適用で再コンポジションと `AndroidView` の update を予約するため、
 * そのフレームの後には宣言の更新が内部 Store へ流れている。宣言の更新を伴わない命令は、待った時点の
 * (変わっていない) 宣言ツリーで解決する。
 *
 * @param sections 初回の宣言ツリーの Section 列 (最終 ID で解決済み)
 */
internal class DSLScrollCommandResolver(sections: List<Section>) : KsScrollCommandReceiver {

    /** 引き直した命令を渡す Host。Host の寿命はコンポジションが管理するため弱参照で持つ。 */
    private var hostReference: WeakReference<KsSettingsViewLayout>? = null

    /** 引き直した命令を渡す Host。 */
    var host: KsSettingsViewLayout?
        get() = hostReference?.get()
        set(value) {
            hostReference = value?.let { WeakReference(it) }
        }

    /** 接続中のハンドル。Host が接続口でハンドルを強参照するのと同じく、ここでも強参照で持つ。 */
    var connectedController: KsScrollController? = null
        private set

    /** 宣言ツリーの世代。宣言の更新が内部 Store へ流れるたびに進む。 */
    var treeGeneration: Int = 0
        private set

    /** 最新の宣言ツリーの Section 列 (最終 ID で解決済み)。 */
    private var sections: List<Section> = sections

    /** 受け取って、まだ引き直していない命令と、受けた時点の宣言ツリーの世代。 */
    private val pending: MutableList<Pair<KsScrollCommand, Int>> = mutableListOf()

    /**
     * これまでに受け取った命令の延べ件数。引き直しを駆動する側はこの値の変化を待つ。
     *
     * 残件数ではなく単調増加の延べ件数にするのは、「積む → 引き直し切る → また積む」で同じ値に戻り、
     * 変化として観測されないことを避けるため。
     */
    var enqueuedCount: Int by mutableIntStateOf(0)
        private set

    /** 直近に引き直した命令を受けた時点と引き直した時点の世代 (テストでの観測用)。 */
    var lastResolvedGenerations: Pair<Int, Int>? = null
        private set

    /** 命令ハンドルをこの受け口へ接続する。別のハンドルへ差し替えるときは、前のハンドルを切り離す。 */
    fun connect(controller: KsScrollController?) {
        val current = connectedController
        if (current === controller) return
        current?.detach(this)
        connectedController = controller
        // 外した接続から届いていた未実行の命令は、まだ引き直していないものも、引き直して Host の
        // 待ち行列へ渡したものも捨てる (Host の接続口と同じ扱い。控えた位置の復元は Host が残す)。
        pending.clear()
        host?.internalDropPendingScrollCommands()
        controller?.attach(this)
    }

    /** 宣言の更新を内部 Store へ流したことを知らせ、引き直しに使う宣言ツリーを差し替える。 */
    fun treeDidUpdate(sections: List<Section>) {
        this.sections = sections
        treeGeneration++
    }

    override fun receive(command: KsScrollCommand) {
        pending += command to treeGeneration
        enqueuedCount++
    }

    /** 保留中の命令を現在の宣言ツリーで引き直し、呼ばれた順に Host へ渡す。 */
    fun resolvePending() {
        if (pending.isEmpty()) return
        val commands = pending.toList()
        pending.clear()
        val receiver = host?.internalScrollCommandReceiver()
        for ((command, receivedGeneration) in commands) {
            lastResolvedGenerations = receivedGeneration to treeGeneration
            val resolved = resolve(command) ?: continue
            receiver?.receive(resolved)
        }
    }

    /**
     * 命令の ID を最終 ID へ引き直す。明示 ID と `forEach` の key の両方の形で計算し、宣言ツリーに
     * 在るほうを採る。両方あれば明示 ID を採る (core/ADR-0036)。どちらも無ければ `null`。
     */
    private fun resolve(command: KsScrollCommand): KsScrollCommand? = when (command) {
        is KsScrollCommand.ToCell -> {
            val cellIds = sections.flatMapTo(HashSet()) { section -> section.cells.map { it.id } }
            finalId(command.id, cellIds)?.let { command.copy(id = it) }
                ?: run {
                    warnUnresolved(kind = "cell", id = command.id)
                    null
                }
        }
        is KsScrollCommand.ToSection -> {
            val sectionIds = sections.mapTo(HashSet()) { it.id }
            finalId(command.id, sectionIds)?.let { command.copy(id = it) }
                ?: run {
                    warnUnresolved(kind = "section", id = command.id)
                    null
                }
        }
        is KsScrollCommand.ToStart, is KsScrollCommand.ToEnd -> command
    }

    private fun finalId(id: Any, existing: Set<String>): String? {
        val explicit = DSLIdentityId.id(from = DSLIdentityHint.Explicit(id))
        if (explicit in existing) return explicit
        val forEachKey = DSLIdentityId.id(from = DSLIdentityHint.ForEach(id))
        if (forEachKey in existing) return forEachKey
        return null
    }

    private fun warnUnresolved(kind: String, id: Any) {
        if (!KsCellRegistry.strictMode) return
        Log.w(LOG_TAG, "KsSettingsView: ignored a scroll command to an unknown $kind id $id")
    }

    private companion object {
        private const val LOG_TAG = "KsSettingsView"
    }
}
