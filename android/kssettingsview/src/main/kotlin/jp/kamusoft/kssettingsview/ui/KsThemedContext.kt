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
    ContextThemeWrapper(base, R.style.Theme_KsSettingsView_Internal) {

    /**
     * このラッパを組み立てた時点の夜間モード（`Configuration.UI_MODE_NIGHT_*`）。
     *
     * 同梱テーマは DayNight 派生であり、[ContextThemeWrapper] はテーマを生成時に一度だけ組み立てる。
     * ラッパ自身の `resources` はラップ元へ委譲するため現在の夜間モードを返すが、既に組み上がった
     * テーマの属性値は切り替わらない。現在値と突き合わせて古さを判定できるよう、生成時の値を覚えておく。
     */
    val createdNightMode: Int = base.nightMode()
}

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
 * 現在の外観がダークか。
 *
 * 未指定色の既定を外観から選ぶときの判定源であり、同梱テーマ付き Context の作り分け
 * （[ksThemedContext]）と同じ Configuration の夜間モードを見る。両者の判定源を揃えることで、
 * 同梱テーマが解決する属性値とライブラリ既定色の外観がずれない。
 */
internal fun Context.isKsDarkAppearance(): Boolean =
    nightMode() == Configuration.UI_MODE_NIGHT_YES

/** [Configuration] から直接夜間モードを判定する（Configuration 変更の通知を受け取る経路用）。 */
internal fun Configuration.isKsDarkAppearance(): Boolean =
    (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

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
 * すでに同梱テーマをかぶせた Context でも、それを組み立てた時点の夜間モードが現在値と一致するときだけ
 * そのまま返す。一致しなければ（Activity を再生成せずに外観が切り替わったホストで、既存の行の Context
 * から選択面を開き直す場合など）ラップ元まで戻って作り直し、現在の外観でテーマ属性が解決される Context
 * を返す。同じラップ元・同じ夜間モードに対しては同じラッパを返す。ラッパはラップ元の生存期間を超えて
 * 保持しない。
 */
internal fun Context.ksThemedContext(): Context {
    if (this is KsThemedContext && createdNightMode == nightMode()) return this
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
