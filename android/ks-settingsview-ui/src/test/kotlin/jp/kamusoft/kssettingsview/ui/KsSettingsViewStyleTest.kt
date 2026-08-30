package jp.kamusoft.kssettingsview.ui

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * KsSettingsViewStyle 切替に伴う ItemDecoration の入れ替えを検証。
 *
 * クラシック / モダンの切替で、RecyclerView に付く ItemDecoration が
 * 過不足なく差し替わることを保証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsSettingsViewStyleTest {

    @Test
    fun `Classic 初期化で ClassicSectionDecoration が登録される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = KsSettingsView(ctx)
        view.style = KsSettingsViewStyle.Classic
        val decoration = view.internalCurrentDecoration()
        assertTrue("ClassicSectionDecoration が登録されている", decoration is ClassicSectionDecoration)
    }

    @Test
    fun `Modern 初期化で ModernSectionDecoration が登録される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = KsSettingsView(ctx)
        view.style = KsSettingsViewStyle.Modern
        val decoration = view.internalCurrentDecoration()
        assertTrue("ModernSectionDecoration が登録されている", decoration is ModernSectionDecoration)
    }

    @Test
    fun `setter 経由の動的切替で ItemDecoration が入れ替わる`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = KsSettingsView(ctx)

        view.style = KsSettingsViewStyle.Classic
        assertTrue(view.internalCurrentDecoration() is ClassicSectionDecoration)

        view.style = KsSettingsViewStyle.Modern
        assertTrue(view.internalCurrentDecoration() is ModernSectionDecoration)

        // RecyclerView 上の ItemDecoration 数も 1 に保たれていること
        val rv = view.internalRecyclerView()
        // 既存 decoration は removeItemDecoration されてから新 decoration が addItemDecoration される
        // ため、合計で 1 つのみが残る
        // RecyclerView.getItemDecorationCount で確認
        assertTrue(rv.itemDecorationCount == 1)
    }
}
