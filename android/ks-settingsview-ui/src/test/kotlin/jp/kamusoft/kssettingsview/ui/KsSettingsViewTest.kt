package jp.kamusoft.kssettingsview.ui

import androidx.recyclerview.widget.ConcatAdapter
import androidx.test.core.app.ApplicationProvider
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * KsSettingsView の基本動作テスト。
 *
 * root ツリーの直接差し込みは `setRootDirect` で行う（`bind(store)` のテストは別の場所で扱う）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsSettingsViewTest {

    @Test
    fun `初期化直後は空の SettingsRoot 相当で itemCount が 0 となる`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = KsSettingsView(ctx)
        val rv = view.internalRecyclerView()
        val adapter = rv.adapter
        assertNotNull(adapter)
        assertTrue(adapter is ConcatAdapter)
        assertEquals(0, adapter!!.itemCount)
    }

    @Test
    fun `setRootDirect で main の合計に Section H_F + Cell が反映される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = KsSettingsView(ctx)
        val sections = (1..2).map { i ->
            Section(
                id = "sec$i",
                header = SectionAccessory.Text("Header$i"),
                footer = SectionAccessory.Text("Footer$i"),
                cells = (1..3).map { j ->
                    LabelCell(id = "cell-$i-$j", title = "Cell$i-$j")
                },
            )
        }
        view.setRootDirect(SettingsRoot(sections = sections))

        val concatAdapter = view.internalRecyclerView().adapter as ConcatAdapter
        // header(0) + main(10) + footer(0) = 10
        assertEquals(10, concatAdapter.itemCount)
        assertEquals(0, view.internalHeaderAdapter().itemCount)
        assertEquals(0, view.internalFooterAdapter().itemCount)
    }

    @Test
    fun `内部 adapter は ConcatAdapter で 3 段構成である`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = KsSettingsView(ctx)
        val concatAdapter = view.internalRecyclerView().adapter as ConcatAdapter
        val adapters = concatAdapter.adapters
        assertEquals(3, adapters.size)
        assertTrue("0 番目は RootHeaderFooterAdapter (Header)", adapters[0] is RootHeaderFooterAdapter)
        assertTrue("1 番目は KsSettingsListAdapter", adapters[1] is KsSettingsListAdapter)
        assertTrue("2 番目は RootHeaderFooterAdapter (Footer)", adapters[2] is RootHeaderFooterAdapter)
    }

    @Test
    fun `rootHeader に Text を代入すると headerAdapter の itemCount が 1 となる`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = KsSettingsView(ctx)

        assertEquals(0, view.internalHeaderAdapter().itemCount)
        view.rootHeader = jp.kamusoft.kssettingsview.core.RootAccessory.Text("プロフィール")
        assertEquals(1, view.internalHeaderAdapter().itemCount)

        view.rootHeader = null
        assertEquals(0, view.internalHeaderAdapter().itemCount)
    }

    @Test
    fun `rootFooter に Text を代入すると footerAdapter の itemCount が 1 となる`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = KsSettingsView(ctx)

        assertEquals(0, view.internalFooterAdapter().itemCount)
        view.rootFooter = jp.kamusoft.kssettingsview.core.RootAccessory.Text("v1.0.0")
        assertEquals(1, view.internalFooterAdapter().itemCount)
    }
}
