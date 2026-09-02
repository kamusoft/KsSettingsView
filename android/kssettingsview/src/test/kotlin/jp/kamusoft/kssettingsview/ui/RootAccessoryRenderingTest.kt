package jp.kamusoft.kssettingsview.ui

import androidx.recyclerview.widget.ConcatAdapter
import androidx.test.core.app.ApplicationProvider
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Root H/F 描画テスト。
 *
 * `KsSettingsView.rootHeader` / `rootFooter` に設定した Root H/F が描画されることを検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RootAccessoryRenderingTest {

    @Test
    fun `setRootDirect 単体時は ConcatAdapter には main のみ描画される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = KsSettingsView(ctx)
        view.setRootDirect(
            SettingsRoot(
                sections = listOf(
                    Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "Hi"))),
                ),
            ),
        )
        val concat = view.internalRecyclerView().adapter as ConcatAdapter
        // Root H/F は明示的に rootHeader / rootFooter を設定しないため空。
        assertEquals(0, view.internalHeaderAdapter().itemCount)
        assertEquals(0, view.internalFooterAdapter().itemCount)
        assertEquals(1, concat.itemCount)
    }

    @Test
    fun `rootHeader 設定で headerAdapter に Text が反映される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = KsSettingsView(ctx)
        view.rootHeader = RootAccessory.Text("プロフィール")
        assertEquals(1, view.internalHeaderAdapter().itemCount)
        assertEquals(RootAccessory.Text("プロフィール"), view.internalHeaderAdapter().view)
    }

    @Test
    fun `rootFooter 設定で footerAdapter に Text が反映される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = KsSettingsView(ctx)
        view.rootFooter = RootAccessory.Text("v1.0.0")
        assertEquals(1, view.internalFooterAdapter().itemCount)
    }
}
