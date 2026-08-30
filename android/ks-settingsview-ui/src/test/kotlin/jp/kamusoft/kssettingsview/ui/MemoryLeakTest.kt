package jp.kamusoft.kssettingsview.ui

import androidx.test.core.app.ApplicationProvider
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * メモリリーク防止のテスト。
 *
 * `KsSettingsView.onDetachedFromWindow` で内部 RecyclerView の adapter が `null` になることを検証。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MemoryLeakTest {

    @Test
    fun `onDetachedFromWindow で RecyclerView adapter が null になる`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = KsSettingsView(ctx)
        val rv = view.internalRecyclerView()
        assertNotNull("init 直後は adapter が設定されている", rv.adapter)

        view.internalDetachForTest()

        assertNull("detach 後は adapter が null", rv.adapter)
    }

    @Test
    fun `Store 経由で setRootDirect しても detach 後 adapter が null になる`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = KsSettingsView(ctx)
        val store = SettingsRootStore(
            initialRoot = SettingsRoot(
                sections = listOf(
                    Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A"))),
                ),
            ),
        )
        // bind() は findViewTreeLifecycleOwner() に依存するため Robolectric では null
        // になり Job が張られない。代わりに setRootDirect で初期 root を反映するだけにする
        // （本テストは onDetachedFromWindow の責務確認が主目的）。
        view.setRootDirect(store.state.value)

        view.internalDetachForTest()
        assertNull("detach 後は adapter が null", view.internalRecyclerView().adapter)
    }
}
