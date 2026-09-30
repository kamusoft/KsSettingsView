package jp.kamusoft.kssettingsview.ui

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.lang.ref.WeakReference

/**
 * 設定画面へスクロール命令を送るハンドルです。
 *
 * 画面のライフサイクルに依存しない値として作り、表示側へ接続して使います。View では
 * `KsSettingsView.scrollController` へ代入し、Compose では `KsSettingsView(...)` の
 * `scrollController` 引数で渡します。
 *
 * - 命令は、同じ処理の中で行ったデータの変更が表示に反映された後に実行されます。
 *   項目を追加した直後に [scrollToEnd] を呼べば、追加した項目を含む末尾へ届きます。
 * - 続けて出した命令は呼んだ順に処理され、最終位置は最後の命令のものになります。
 * - どこにも接続していないハンドルへの命令は何もしません。
 * - 1 つのハンドルが命令を届ける先は、最後に接続した画面だけです。
 * - ハンドルは接続先の画面を保持しません。画面が破棄されると未接続に戻ります。
 * - 命令はメインスレッドから呼びます。`KsCellRegistry.strictMode` が `true` のとき (既定) は、
 *   接続中のハンドルへメインスレッド以外から命令すると [IllegalStateException] を送出します。
 */
public class KsScrollController : KsScrollControlling {

    /**
     * 命令を届ける受け口。
     *
     * 受け口は Activity の Context を持つ View Host そのものに結び付くため、ハンドルが強参照すると
     * ViewModel が持つハンドル経由で画面が漏れる。弱参照で持ち、受け口が回収されれば未接続に戻る
     * (core/ADR-0037)。
     *
     * 書き込みはメインスレッドだけで行うが、`strictMode` でないときはメインスレッド以外の命令でも
     * 接続の有無を読むため、書き込みがどのスレッドからも見えるようにする。
     */
    @Volatile
    private var receiver: WeakReference<KsScrollCommandReceiver>? = null

    override fun scrollTo(id: Any, position: KsScrollPosition, animated: Boolean) {
        send(KsScrollCommand.ToCell(id = id, position = position, animated = animated))
    }

    override fun scrollToSection(id: Any, position: KsScrollPosition, animated: Boolean) {
        send(KsScrollCommand.ToSection(id = id, position = position, animated = animated))
    }

    override fun scrollToStart(animated: Boolean) {
        send(KsScrollCommand.ToStart(animated = animated))
    }

    override fun scrollToEnd(animated: Boolean) {
        send(KsScrollCommand.ToEnd(animated = animated))
    }

    /**
     * 接続中の受け口へ命令を渡す。未接続なら何もしない。
     *
     * メインスレッド以外からの呼び出しは、debug 相当 (`KsCellRegistry.strictMode`) では例外で知らせる。
     * それ以外では命令を捨てずにメインスレッドへ回して受け口へ渡す (待ち行列はメインスレッドだけで
     * 触る前提のため)。回した命令は、命令を出した時点の接続が実行時まで続いているときだけ渡す
     * (実行までの間に接続を外した・別の受け口へつなぎ替えた場合は、外した接続の未実行の命令として捨てる)。
     */
    private fun send(command: KsScrollCommand) {
        val connection = receiver ?: return
        val target = connection.get() ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            target.receive(command)
            return
        }
        check(!KsCellRegistry.strictMode) {
            "KsScrollController: scroll commands must be called on the main thread"
        }
        Handler(Looper.getMainLooper()).post {
            // 接続ごとに WeakReference を作り直すので、その同一性で同じ接続が続いているかを判定する。
            if (receiver === connection) connection.get()?.receive(command)
        }
    }

    /** 受け口へ接続する。既に別の受け口へ接続していれば置き換え、debug 相当で警告ログを出す。 */
    internal fun attach(receiver: KsScrollCommandReceiver) {
        val current = this.receiver?.get()
        // 同じ受け口への接続し直しは接続を変えない (メインスレッドへ回した命令の配送先の判定を保つため)。
        if (current === receiver) return
        if (current != null && KsCellRegistry.strictMode) {
            Log.w(
                LOG_TAG,
                "KsScrollController: the controller was attached to another settings view; " +
                    "the last attachment is used",
            )
        }
        this.receiver = WeakReference(receiver)
    }

    /**
     * 受け口から切り離す。現在の受け口と同一のときだけ外す (別の受け口へ移った後の切断で、移った先
     * との接続を壊さないため)。
     */
    internal fun detach(receiver: KsScrollCommandReceiver) {
        if (this.receiver?.get() !== receiver) return
        this.receiver = null
    }

    /** 現在の受け口 (テストでの接続状態の確認用)。 */
    internal val currentReceiver: KsScrollCommandReceiver?
        get() = receiver?.get()

    private companion object {
        private const val LOG_TAG = "KsScrollController"
    }
}
