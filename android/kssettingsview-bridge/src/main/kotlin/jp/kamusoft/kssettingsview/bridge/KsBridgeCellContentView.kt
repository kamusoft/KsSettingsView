package jp.kamusoft.kssettingsview.bridge

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView

/**
 * interop 境界の `View` を Cell の内容として Compose ツリーへ埋め込む変換。
 *
 * トップレベル関数ではなく object のメンバとして置く。Kotlin のトップレベル関数はファイル単位の
 * public なクラス（`*Kt`）に載るため、そのままでは interop 境界の公開表面に空の型が現れる。
 */
internal object KsBridgeCellContentView {

    /**
     * 輸送された [view] を行の内容として描画する。
     *
     * Native の `CustomCell` は描画のたびに builder を呼んで内容を組み立てる契約だが、interop 境界を
     * 越えて渡されるのは生成済みのインスタンス 1 つである。そのため factory は常に同じインスタンスを
     * 返す（maui/ADR-0017）。
     *
     * 返す前に既存の親から切り離す。切り離さずに返すと、行のリサイクルや再バインドで同じ View が
     * 別の描画先へ `addView` される際に `IllegalStateException` になる。
     *
     * 埋め込みは [token] で識別する。`AndroidView` の factory は同じ呼び出し位置につき一度しか
     * 呼ばれないため、[token] を [key] へ与えないと token が変わっても古い View が行に残り続ける。
     * [key] を挟むことで、token が同じ間の再バインドでは埋め込み自体が作り直されず View インスタンスが
     * 維持され、token が変わったときだけ埋め込みが作り直されて新しい View へ差し替わる。
     *
     * 内容は行の幅いっぱいに広げる（full-bleed）。
     *
     * 内容の上でのタップは [onRowTap] へ返す。埋め込みは自分の占める領域全体をポインタ入力の
     * 受け口として登録するため、内容の上で起きたタッチは行の View まで下りて来ず、行が持つ click
     * listener だけでは内容の上で行タップが発火しない。埋め込みの外側でタップを検出し直すことで、
     * 内容を持たない行と同じ発火経路に戻す。埋め込まれた View がタッチを引き取ったとき（内容の中の
     * ボタン・ジェスチャ等）はポインタの変化が消費済みになるためこの検出は始まらず、内容の中の操作と
     * 行タップの二重発火は起きない。行が無効なときも上位が押下を消費するため検出は始まらない。
     *
     * 検出そのものは [onRowTap] の有無によらず常に置き、通知先の有無だけを差し替える。タップ購読の
     * 切り替えで modifier の構成が変わると埋め込みが作り直され、内容 View が付け替わってしまうため。
     *
     * @param view 行の内容として表示する View（`null` で内容なし）
     * @param token 内容として埋め込む View の世代
     * @param onRowTap 内容の上でのタップで呼ぶ行タップ（`null` で行タップ動作を持たない）
     */
    @Composable
    fun Content(view: View?, token: String, onRowTap: (() -> Unit)?) {
        if (view == null) {
            return
        }

        // 検出中のジェスチャを跨いで差し替えられるよう、最新の通知先を参照で持つ。
        val currentRowTap by rememberUpdatedState(onRowTap)

        key(token) {
            AndroidView(
                factory = {
                    (view.parent as? ViewGroup)?.removeView(view)
                    view
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures { currentRowTap?.invoke() }
                    },
            )
        }
    }
}
