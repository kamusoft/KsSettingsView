package jp.kamusoft.kssettingsview.ui

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
 * window 既定の `Recomposer` は、プロセス内で一度だけ生成される Compose の UI dispatcher
 * （`AndroidUiDispatcher.Main`）の上で動く。この dispatcher はテストをまたいで使い回され、Robolectric が
 * テストごとに行う初期化と噛み合わない。噛み合わない点は 2 つある。
 *
 * 1. dispatcher は、処理をメインスレッドへ予約すると「予約済み」の印を立て、予約した処理が
 *    実行されるまで次の予約を出さない。Robolectric はテストの終わりにメインスレッドのキューを
 *    中身ごと捨てるので、予約が消化されないままテストが終わると、印だけが立ったまま残る。その
 *    プロセスで後に走るテストでは、window の `Recomposer` は**初回 composition だけを行い、以降の
 *    更新が永久に保留される**（実測）
 * 2. dispatcher は、最初に Compose を使ったテストの `Choreographer` を握り続ける。Robolectric は
 *    テストごとに時計を戻し、`Choreographer` を作り直すので、握られた `Choreographer` は後のテストで
 *    フレームを配れなくなることがある。先のテストが時計を大きく進めていた場合と、フレームの配信を
 *    止める API を使った場合に、フレームが届かなくなる（実測。時計が先のテストの時刻へ追いつくまで
 *    フレームを見送ること、止めていた間のフレームを配る合図が古い `Choreographer` へ届かないことが
 *    原因、というのは推定）
 *
 * そこでテスト側で `Recomposer` を用意し、フレームは [frame] から明示的に送る。view ツリーの上位へ
 * 差し込んでおけば、配下の `ComposeView` はこれを親 `CompositionContext` として composition を作る。
 * この駆動器は自前の `Recomposer`・dispatcher・フレームの時計を使い、Compose の UI dispatcher も
 * `Choreographer` も通らないので、上の 2 つのどちらにも左右されない決定的な駆動になる。
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
     * Compose 標準の dispatcher は、先に走ったテストの影響（残った「予約済み」の印、握り続ける
     * `Choreographer`）で止まりうるため使わない。フレームは [frameClock] から送るため、ここでは
     * 実行スレッドをメインへ揃えることだけを担う。
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
