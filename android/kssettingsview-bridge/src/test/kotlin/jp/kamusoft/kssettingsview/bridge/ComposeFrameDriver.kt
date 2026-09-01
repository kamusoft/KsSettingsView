package jp.kamusoft.kssettingsview.bridge

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.compositionContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.robolectric.Shadows.shadowOf
import kotlin.coroutines.CoroutineContext

/**
 * Robolectric 上で Compose の再 composition を決定的に流すための駆動器。
 *
 * # なぜ必要か
 *
 * Compose の再 composition は `Choreographer` のフレームで走る。Robolectric はテストごとに
 * `Choreographer` を作り直すが、Compose 側でフレームを要求する dispatcher はプロセス内で一度だけ
 * 生成され、最初に取得した `Choreographer` を握り続ける。その結果、同一クラスの 2 件目以降のテストでは
 * window の `Recomposer` が要求したフレームが誰にも届かず、**初回 composition だけが走り、以降の更新が
 * 永久に保留される**。メインループを何度流しても、システム時刻を進めても解消しない。
 *
 * そこでテスト側で `Recomposer` を用意し、フレームは [frame] から明示的に送る。view ツリーの上位へ
 * 差し込んでおけば、配下の `ComposeView` はこれを親 `CompositionContext` として composition を作るため、
 * `Choreographer` に依存しない決定的な駆動になる。
 *
 * # 使い方
 *
 * 1. composition が作られる前（`ComposeView` が window へ attach される前）に [installOn] で
 *    view ツリーの上位へ差し込む
 * 2. 更新を反映させたい箇所で [frame] を呼ぶ
 * 3. テスト終了時に [stop] で `Recomposer` を止める
 *
 * # もう一方のコピーとの同期
 *
 * このクラスは `kssettingsview` と `kssettingsview-bridge` の test ソースに同一内容で置かれて
 * おり、両者の差は `package` 宣言だけである（テストソースを共有するビルド構成を持たないため）。
 * 片方だけを変更してはいけない。Compose / Robolectric の更新でこの駆動器の前提を見直すときも、
 * 両方のコピーを同時に確認する。
 */
internal class ComposeFrameDriver {

    private val frameClock = BroadcastFrameClock()

    private val scope = CoroutineScope(MainLooperDispatcher + frameClock)

    private val recomposer = Recomposer(scope.coroutineContext)

    init {
        scope.launch { recomposer.runRecomposeAndApplyChanges() }
        pumpLooper()
    }

    /** 配下の `ComposeView` がこの `Recomposer` を親に使うよう、view ツリーへ差し込む。 */
    fun installOn(view: View) {
        view.compositionContext = recomposer
    }

    /**
     * 保留中の state 変更を通知し、フレームを送って再 composition を完了させる。
     *
     * 再 composition が次の再 composition を生む場合があるため、保留がなくなるまで繰り返す。
     * 上限まで送っても保留が残る場合は例外で止める。保留を抱えたまま呼び出し元へ戻ると、単に未反映で
     * あるだけの状態を「変化がない」と読み違えたアサーションが通ってしまう。
     */
    fun frame() {
        repeat(MAX_FRAMES) {
            Snapshot.sendApplyNotifications()
            pumpLooper()
            if (!recomposer.hasPendingWork) return
            frameClock.sendFrame(0L)
            pumpLooper()
        }
        Snapshot.sendApplyNotifications()
        pumpLooper()
        check(!recomposer.hasPendingWork) {
            "再 composition が $MAX_FRAMES フレームでは収束しなかった"
        }
    }

    /** `Recomposer` を停止する。 */
    fun stop() {
        recomposer.cancel()
        pumpLooper()
        scope.cancel()
    }

    private fun pumpLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /**
     * メインスレッドの `Handler` へ委譲するだけの dispatcher。
     *
     * Compose 標準の dispatcher は `Choreographer` と結び付いており、テストごとに作り直される
     * `Choreographer` を跨げない。フレームは [frameClock] から送るため、ここでは実行スレッドを
     * メインへ揃えることだけを担う。
     */
    private object MainLooperDispatcher : CoroutineDispatcher() {
        private val handler = Handler(Looper.getMainLooper())

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            handler.post(block)
        }
    }

    private companion object {
        /** 収束待ちで送るフレームの上限（保留が消えない場合の打ち切り）。 */
        const val MAX_FRAMES: Int = 20
    }
}
