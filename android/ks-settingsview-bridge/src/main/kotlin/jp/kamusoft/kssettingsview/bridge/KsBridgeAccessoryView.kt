package jp.kamusoft.kssettingsview.bridge

import android.view.View
import android.view.ViewGroup
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.SectionAccessory

/**
 * interop 境界の `View` を [KsAnyView] へ包む変換。
 *
 * Native の accessory は描画のたびに factory を呼んで View を得る契約だが、interop 境界を越えて
 * 渡されるのは生成済みのインスタンス 1 つである。そのため常に同じインスタンスを返す factory に
 * 包む（maui/ADR-0017）。
 */
internal object KsBridgeAccessoryView {

    /**
     * 常に同じ `View` を返す [KsAnyView] を作る。
     *
     * 返す前に既存の親から切り離す。切り離さずに返すと、リサイクル等で同じ View が別の
     * 描画先へ `addView` される際に `IllegalStateException` になる。
     *
     * @param view accessory として表示する View
     */
    fun anyView(view: View): KsAnyView = KsAnyView.AndroidView { _ ->
        (view.parent as? ViewGroup)?.removeView(view)
        view
    }

    /**
     * View と text から Section の accessory を解決する。View が指定されていれば View を優先する。
     *
     * @param view accessory として表示する View（`null` で未指定）
     * @param text accessory として表示する text（`null` で未指定）
     */
    fun sectionAccessory(view: View?, text: String?): SectionAccessory? = when {
        view != null -> SectionAccessory.View(anyView(view))
        text != null -> SectionAccessory.Text(text)
        else -> null
    }
}
