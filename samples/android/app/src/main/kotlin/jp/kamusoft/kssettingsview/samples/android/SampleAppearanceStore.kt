package jp.kamusoft.kssettingsview.samples.android

import android.content.Context
import android.content.res.Configuration

/**
 * 外観の選択（[SampleAppearance]）の永続化と、Activity へ与える夜間モード上書きの組み立て。
 *
 * 反映は Activity 自身の Configuration 上書きで行い、OS のアプリ単位夜間モード設定
 * （`UiModeManager.setApplicationNightMode`）は使わない。後者は「端末に追随する」値を持たないため、
 * 「システム」を選び直したときに端末の設定へ戻せなくなる。
 *
 * 上書きした Configuration は Activity の Resources に効くので、`values-night/` のリソース・
 * Compose の `isSystemInDarkTheme()`・ライブラリ UI（Activity の Context から生成される）が
 * すべて同じ実効外観で解決される。
 */
object SampleAppearanceStore {

    private const val PREFS_NAME: String = "sample_appearance"
    private const val KEY_APPEARANCE: String = "appearance"

    /** 保存済みの選択を返す。未保存・未知の値なら [SampleAppearance.DEFAULT]。 */
    fun load(context: Context): SampleAppearance {
        val saved = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APPEARANCE, null)
            ?: return SampleAppearance.DEFAULT
        return SampleAppearance.entries.firstOrNull { it.name == saved } ?: SampleAppearance.DEFAULT
    }

    /** 選択を保存する。次回起動時は [load] がこの値を返す。 */
    fun save(context: Context, appearance: SampleAppearance) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APPEARANCE, appearance.name)
            .apply()
    }

    /**
     * 保存済みの選択に対応する Configuration の上書き。「システム」なら `null`（上書きなし）を返し、
     * 端末の夜間モードがそのまま効く。
     *
     * 返す Configuration は夜間モードのビットだけを持つ差分である。引数なしの `Configuration()` は
     * 全フィールドを「未設定」で作る差分用のコンストラクタで、`Configuration.updateFrom` は未設定の
     * フィールドを取り込まない。端末側のフォントスケール（`fontScale`）や表示密度はそのまま残り、
     * `uiMode` も type 側と night 側が別マスクで扱われるため type（normal / television 等）は保たれる。
     */
    fun nightModeOverride(context: Context): Configuration? {
        val night = when (load(context)) {
            SampleAppearance.System -> return null
            SampleAppearance.Light -> Configuration.UI_MODE_NIGHT_NO
            SampleAppearance.Dark -> Configuration.UI_MODE_NIGHT_YES
        }
        return Configuration().apply { uiMode = night }
    }
}
