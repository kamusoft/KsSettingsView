package jp.kamusoft.kssettingsview.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import jp.kamusoft.kssettingsview.R

/**
 * ライブラリ所有 UI の生成に使う、同梱テーマ（`Theme.KsSettingsView.Internal`）をかぶせた [Context]。
 *
 * ライブラリの View・シート・ダイアログは、ホストアプリの XML テーマが何であっても本ラッパ経由の
 * Context から生成する。これにより Material ウィジェットが要求する属性が常に揃い、ホストテーマの
 * 属性値がライブラリ UI の配色へ漏れることもなくなる（android/ADR-0020）。
 *
 * ラップ元と区別できる型にしているのは、二重ラップの回避（[ksThemedContext]）と、利用者所有
 * コンテンツへ渡すホスト Context の取り出し（[ksHostContext]）のため。
 */
internal class KsThemedContext(base: Context) :
    ContextThemeWrapper(base, R.style.Theme_KsSettingsView_Internal)

/**
 * キャッシュした [KsThemedContext] と、それを作った時点の夜間モード。
 *
 * 同梱テーマは DayNight 派生であり、解決値は夜間モードで変わる。[ContextThemeWrapper] は生成時に
 * 一度だけテーマを組み立てるため、構成変更を Activity 再生成なしで処理するホストではラッパを作り直す
 * 必要がある。そのため夜間モードを併せて覚えておき、変化していたら作り直す。
 */
private class ThemedContextEntry(
    val nightMode: Int,
    val themed: WeakReference<KsThemedContext>,
)

/**
 * ラップ元 Context ごとの [KsThemedContext] のキャッシュ。
 *
 * キーは弱参照であり、値も [WeakReference] に包む。[KsThemedContext] はラップ元を強参照するため、
 * 値を直接持つとキー（Activity 等）がエントリ経由で到達可能になり回収されなくなる。生成した
 * ラッパを強参照するのは、そこから作られた View だけである。
 */
private val themedContextCache = WeakHashMap<Context, ThemedContextEntry>()

/** 現在の夜間モード（`Configuration.UI_MODE_NIGHT_*`）。 */
private fun Context.nightMode(): Int =
    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

/**
 * ラップ元として使う Context を求める。
 *
 * 中間の [ContextWrapper]（ホストがテーマを与えるために被せたラッパ等）は素通りし、UI の帰属先である
 * [Activity]、[Application]、またはラッパでない Context まで降りる。ホストのテーマは元々参照しないため
 * 中間ラッパを外しても解決結果は変わらず、ラップ元が Cell 行や選択面ごとに増えないことで、テーマを
 * 保持する Context の数が UI の帰属先の数に収まる。
 *
 * [Activity] で止めるのは、シートやダイアログの提示に Activity の Context が要るため。
 */
private fun Context.themeBaseContext(): Context {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity || current is Application) return current
        current = current.baseContext ?: return current
    }
    return current
}

/**
 * ライブラリ所有 UI を生成するための Context を返す。
 *
 * すでに同梱テーマをかぶせた Context ならそのまま返し、そうでなければ [themeBaseContext] をラップして
 * 返す（同じラップ元・同じ夜間モードに対しては同じラッパを返す）。ラッパはラップ元の生存期間を超えて
 * 保持しない。
 */
internal fun Context.ksThemedContext(): Context {
    if (this is KsThemedContext) return this
    val base = themeBaseContext()
    val nightMode = base.nightMode()
    synchronized(themedContextCache) {
        val cached = themedContextCache[base]
        if (cached != null && cached.nightMode == nightMode) {
            cached.themed.get()?.let { return it }
        }
        val themed = KsThemedContext(base)
        themedContextCache[base] = ThemedContextEntry(nightMode, WeakReference(themed))
        return themed
    }
}

/** 親 [ViewGroup] から、ライブラリ所有 UI を生成するための Context を得る。 */
internal fun ViewGroup.ksThemedContext(): Context = context.ksThemedContext()

/**
 * 利用者所有コンテンツ（[CustomCell] の content・`KsAnyView` 経由の利用者 View）を生成するための
 * Context を返す。
 *
 * 同梱テーマをかぶせた Context ならラップ元へ戻す。利用者の View が参照するテーマ属性は、ライブラリの
 * 同梱テーマではなくホストのテーマで解決させる（android/ADR-0020）。
 */
internal fun Context.ksHostContext(): Context =
    if (this is KsThemedContext) baseContext else this
