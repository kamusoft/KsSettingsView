package jp.kamusoft.kssettingsview.ui

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.RootAccessory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `RootHeaderFooterAdapter` の itemCount / 通知挙動 / 予約 ID 確認。
 *
 * `RootHeaderFooterAdapter.view` の有無に応じて項目数と変更通知が正しく出ることを保証する。
 * 非 null 同士の差し替えでは、Cell の内容更新と同じ payload 付きの変更通知が出ることも保証する
 * (既定の ItemAnimator 下では payload なしで `SimpleItemAnimator.canReuseUpdatedViewHolder` が
 * false を返し ViewHolder が新規生成されて旧行とクロスフェードする。`KsSettingsView` 側の
 * change アニメーション無効化と合わせた二重担保。android/ADR-0001)。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RootHeaderFooterAdapterTest {

    @Test
    fun `view が null のとき itemCount は 0`() {
        val adapter = RootHeaderFooterAdapter(role = RootHeaderFooterAdapter.Role.HEADER)
        adapter.view = null
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `view が非 null のとき itemCount は 1`() {
        val adapter = RootHeaderFooterAdapter(role = RootHeaderFooterAdapter.Role.HEADER)
        adapter.view = RootAccessory.Text("Profile")
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `null から非 null への変化で notifyItemInserted_0 が発行される`() {
        val adapter = RootHeaderFooterAdapter(role = RootHeaderFooterAdapter.Role.HEADER)
        val observer = TestObserver()
        adapter.registerAdapterDataObserver(observer)
        adapter.view = RootAccessory.Text("Hi")
        assertEquals(1, observer.insertedCount)
        assertEquals(0, observer.removedCount)
    }

    @Test
    fun `非 null から null への変化で notifyItemRemoved_0 が発行される`() {
        val adapter = RootHeaderFooterAdapter(role = RootHeaderFooterAdapter.Role.HEADER)
        adapter.view = RootAccessory.Text("Hi")
        val observer = TestObserver()
        adapter.registerAdapterDataObserver(observer)
        adapter.view = null
        assertEquals(0, observer.insertedCount)
        assertEquals(1, observer.removedCount)
    }

    @Test
    fun `Header 役割の getItemId(0) は 1L である`() {
        val adapter = RootHeaderFooterAdapter(role = RootHeaderFooterAdapter.Role.HEADER)
        adapter.view = RootAccessory.Text("Hi")
        assertEquals(1L, adapter.getItemId(0))
    }

    @Test
    fun `Footer 役割の getItemId(0) は 2L である`() {
        val adapter = RootHeaderFooterAdapter(role = RootHeaderFooterAdapter.Role.FOOTER)
        adapter.view = RootAccessory.Text("Bye")
        assertEquals(2L, adapter.getItemId(0))
    }

    @Test
    fun `非 null から非 null への置換で notifyItemChanged_0 が発行される`() {
        val adapter = RootHeaderFooterAdapter(role = RootHeaderFooterAdapter.Role.HEADER)
        adapter.view = RootAccessory.Text("v1")
        val observer = TestObserver()
        adapter.registerAdapterDataObserver(observer)
        adapter.view = RootAccessory.Text("v2")
        assertEquals(1, observer.changedCount)
    }

    @Test
    fun `Text 形式の置換は内容 payload 付きで通知される`() {
        val adapter = RootHeaderFooterAdapter(role = RootHeaderFooterAdapter.Role.HEADER)
        adapter.view = RootAccessory.Text("v1")
        val observer = ChangeRecordingObserver()
        adapter.registerAdapterDataObserver(observer)
        adapter.view = RootAccessory.Text("v2")
        assertEquals(listOf(0), observer.changedPositions)
        assertEquals(listOf<Any?>(KsSettingsView.PAYLOAD_CONTENT), observer.payloads)
    }

    @Test
    fun `View 形式の置換は内容 payload 付きで通知される`() {
        val adapter = RootHeaderFooterAdapter(role = RootHeaderFooterAdapter.Role.FOOTER)
        adapter.view = RootAccessory.View(KsAnyView.AndroidView { ctx -> View(ctx) })
        val observer = ChangeRecordingObserver()
        adapter.registerAdapterDataObserver(observer)
        adapter.view = RootAccessory.View(KsAnyView.AndroidView { ctx -> View(ctx) })
        assertEquals(listOf(0), observer.changedPositions)
        assertEquals(listOf<Any?>(KsSettingsView.PAYLOAD_CONTENT), observer.payloads)
    }

    @Suppress("UNUSED_PARAMETER")
    private class TestObserver : RecyclerView.AdapterDataObserver() {
        var insertedCount = 0
        var removedCount = 0
        var changedCount = 0

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            insertedCount += itemCount
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            removedCount += itemCount
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            changedCount += itemCount
        }

        // ApplicationProvider を残すために import を維持
        @Suppress("unused")
        private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    }
}
