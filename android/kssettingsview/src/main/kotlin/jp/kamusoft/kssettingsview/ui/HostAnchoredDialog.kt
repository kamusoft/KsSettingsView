package jp.kamusoft.kssettingsview.ui

import android.app.Dialog
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner

/**
 * [anchor] が属するホストの破棄に追随する形でダイアログを表示する。
 *
 * ダイアログの window はホストの window に紐づく。ホストが破棄されるときに畳まないと、
 * 閉じる主体を失った window だけが残って leak になる。ホストの破棄でこのダイアログを閉じ、
 * 閉じた時点で購読も解除する。
 *
 * [anchor] から lifecycle をたどれない場合は購読せず、表示だけを行う。
 *
 * 購読解除のためダイアログの [Dialog.setOnDismissListener] をこの関数が占有する。
 * 閉じたときの処理は listener を自分で設定するのではなく [onDismissed] に渡すこと。
 *
 * @param anchor ホストの lifecycle をたどる起点にする View（選択面を開いた行など）
 * @param onDismissed 閉じたときに追加で呼ぶ処理
 */
internal fun Dialog.showAnchoredTo(anchor: View, onDismissed: (() -> Unit)? = null) {
    val lifecycle = anchor.findViewTreeLifecycleOwner()?.lifecycle
    val observer = if (lifecycle != null) {
        object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                dismiss()
            }
        }.also { lifecycle.addObserver(it) }
    } else {
        null
    }
    setOnDismissListener {
        observer?.let { lifecycle?.removeObserver(it) }
        onDismissed?.invoke()
    }
    show()
}
