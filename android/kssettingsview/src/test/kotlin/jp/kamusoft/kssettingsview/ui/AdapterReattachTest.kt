package jp.kamusoft.kssettingsview.ui

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * detach → 再 attach をまたいでリスト内容が保たれることを検証する。
 *
 * `onDetachedFromWindow` は内部 RecyclerView の adapter 参照を切るため、再 attach 時に戻し直さないと
 * 内部状態を保ったままリストだけが空で復帰する。ViewPager2 のオフスクリーンページや Compose
 * `AndroidView` の付け外しのように、View を作り直さず detach / attach するホストで到達する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AdapterReattachTest {

    /** `KsSettingsView` を 1 つだけ載せるホスト Activity。 */
    class HostActivity : FragmentActivity() {
        lateinit var container: FrameLayout
        lateinit var settingsView: KsSettingsView

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(MaterialR.style.Theme_Material3_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            container = FrameLayout(this)
            setContentView(container)
            settingsView = KsSettingsView(this)
            container.addView(settingsView)
        }
    }

    private var controller: ActivityController<HostActivity>? = null

    @After
    fun tearDown() {
        controller?.close()
        controller = null
    }

    /**
     * 検証に使う root。
     *
     * Section ヘッダ 1 + 可視 Cell 2（`c1` / `c3`）+ 不可視 Cell 1（`c2`）で、平坦リストは 3 件になる。
     * 不可視 Cell を混ぜているのは、再 attach 後の復帰内容が可視射影として正しいことまで見るため。
     */
    private fun sampleRoot(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(
                id = "s1",
                header = SectionAccessory.Text("見出し"),
                cells = listOf(
                    LabelCell(id = "c1", title = "A"),
                    LabelCell(id = "c2", title = "B", isVisible = false),
                    LabelCell(id = "c3", title = "C"),
                ),
            ),
        ),
    )

    /** レイアウトを走らせて RecyclerView に行を生成させる。 */
    private fun HostActivity.layoutSettingsView(target: KsSettingsView = settingsView) {
        val metrics = resources.displayMetrics
        target.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        target.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    /** Adapter の差分通知を記録するオブザーバ。 */
    private class RecordingObserver : RecyclerView.AdapterDataObserver() {
        val events = mutableListOf<String>()

        override fun onChanged() {
            events += "onChanged"
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            events += "changed($positionStart, $itemCount)"
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            events += "changed($positionStart, $itemCount, $payload)"
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            events += "inserted($positionStart, $itemCount)"
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            events += "removed($positionStart, $itemCount)"
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            events += "moved($fromPosition, $toPosition, $itemCount)"
        }
    }

    @Test
    fun `detach 後に再 attach すると adapter が戻る`() {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()
        val view = activity.settingsView
        view.setRootDirect(sampleRoot())
        idle()

        val rv = view.internalRecyclerView()
        val beforeAdapter = rv.adapter
        assertNotNull("attach 中は adapter が設定されている", beforeAdapter)
        assertEquals("Section ヘッダ 1 + 可視 Cell 2", 3, beforeAdapter!!.itemCount)

        // View 自体は作り直さずに detach → 再 attach する。
        activity.container.removeView(view)
        idle()
        assertNull("detach 中は adapter 参照が切れている", rv.adapter)

        activity.container.addView(view)
        idle()

        val afterAdapter = rv.adapter
        assertNotNull("再 attach 後は adapter が戻っている", afterAdapter)
        assertSame("戻るのは detach 前と同一の adapter インスタンス", beforeAdapter, afterAdapter)
        assertEquals(
            "再 attach 後もアイテム数が detach 前と一致する",
            beforeAdapter.itemCount,
            afterAdapter!!.itemCount,
        )
    }

    @Test
    fun `detach 後に再 attach するとリストの並びと可視状態が保たれる`() {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()
        val view = activity.settingsView
        view.setRootDirect(sampleRoot())
        idle()
        activity.layoutSettingsView()

        val before = visibleRowTexts(view)
        assertEquals("detach 前の並び（不可視の B は出ない）", listOf("見出し", "A", "C"), before)

        activity.container.removeView(view)
        idle()

        activity.container.addView(view)
        idle()
        activity.layoutSettingsView()

        assertEquals("再 attach 後も並びと可視状態が detach 前と一致する", before, visibleRowTexts(view))
    }

    @Test
    fun `detach 中の Store 更新が再 attach 後に反映される`() {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()
        val view = activity.settingsView
        val store = SettingsRootStore(
            initialRoot = SettingsRoot(
                sections = listOf(
                    Section(
                        id = "s1",
                        cells = listOf(LabelCell(id = "c1", title = "A"), LabelCell(id = "c2", title = "B")),
                    ),
                ),
            ),
        )
        view.bind(store)
        awaitConvergence(view) { committedTexts(view) == listOf("A", "B") }

        // attach 中の更新は購読経由で届く（購読が実在することの確認）。
        store.replaceCell("c2", LabelCell(id = "c2", title = "B2"))
        awaitConvergence(view) { committedTexts(view) == listOf("A", "B2") }
        assertEquals("attach 中の Store 更新は購読経由で届く", listOf("A", "B2"), cellTitles(view))

        activity.container.removeView(view)
        idle()

        // detach 中の更新は購読が切れているため View には届かない（Diff は replay されない）。
        // 「届かないこと」は条件成立を待つ形にできないため、キューを流し切ってから状態を確かめる。
        store.insertCell(LabelCell(id = "c3", title = "C"), sectionId = "s1", at = 2)
        idle()
        assertEquals("detach 中は Diff が View に届かない", listOf("A", "B2"), cellTitles(view))

        activity.container.addView(view)
        awaitConvergence(view) { committedTexts(view) == listOf("A", "B2", "C") }
        activity.layoutSettingsView()

        assertEquals(
            "再 attach 時に Store の現在値を取り込み直す",
            listOf("A", "B2", "C"),
            cellTitles(view),
        )
        assertEquals(
            "表示行も Store の現在値と一致する",
            listOf("A", "B2", "C"),
            visibleRowTexts(view),
        )
    }

    @Test
    fun `detach 中の Theme 変更が再 attach 後に反映される`() {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()
        val view = activity.settingsView
        val store = SettingsRootStore(initialRoot = sampleRoot())
        view.bind(store)
        awaitConvergence(view) { committedTexts(view) == listOf("見出し", "A", "C") }
        activity.layoutSettingsView()

        val newTheme = Theme(
            separatorColor = Color(0xFF405060),
            backgroundColor = Color(0xFF102030),
        )

        activity.container.removeView(view)
        idle()
        // 「届かないこと」は条件成立を待つ形にできないため、キューを流し切ってから状態を確かめる。
        store.applyTheme(newTheme)
        idle()
        assertEquals("detach 中は Theme も View に届かない", Theme(), view.internalTheme())

        activity.container.addView(view)
        // attach のトラバーサル内で反映されること（メッセージを 1 つも回さない時点で確認する）。
        // 再 attach 後の最初のレイアウト・描画は attach と同じトラバーサルで走るため、
        // ここで未反映だと 1 フレーム古い配色で描かれる。
        assertEquals("attach 直後に Store の現在 Theme が反映される", newTheme, view.internalTheme())

        // 後続のアサーションはいずれも同期状態を見るため、残っているメッセージだけ流す。
        idle()
        activity.layoutSettingsView()

        assertEquals("再 attach 後は Store の現在 Theme が反映される", newTheme, view.internalTheme())
        assertEquals(
            "RecyclerView 背景も新 Theme の色になる",
            newTheme.backgroundColor.toArgb(),
            (view.internalRecyclerView().background as ColorDrawable).color,
        )
        assertEquals(
            "ItemDecoration も新 Theme で作り直される",
            newTheme,
            (view.internalCurrentDecoration() as ClassicSectionDecoration).theme,
        )
    }

    @Test
    fun `初回 attach での再適用は差分通知を出さない`() {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()

        // attach 前に bind する経路（Compose `AndroidView.factory` 相当）を作るため、
        // Activity が持つ View とは別に組み立てる。
        val view = KsSettingsView(activity)
        val store = SettingsRootStore(initialRoot = sampleRoot())
        view.bind(store)
        awaitConvergence(view) { committedTexts(view) == listOf("見出し", "A", "C") }

        val observer = RecordingObserver()
        val adapter = view.internalMainListAdapter()
        adapter.registerAdapterDataObserver(observer)
        try {
            // attach 時の再適用は bind() と同内容のため、差分計算は空振りして通知を出さない。
            // 同内容でも差分コミット時に内部リストの参照は差し替わるため、その差し替えを
            // 「差分計算が完了した」ことの待機条件に使い、完了後に通知の不在を確かめる。
            val committedBefore = adapter.currentList
            activity.container.addView(view)
            awaitConvergence(view) { adapter.currentList !== committedBefore }
            assertEquals("同内容の再適用は差分通知を出さない", emptyList<String>(), observer.events)

            // 対照: 内容が変われば同じ経路で通知が出る（観測が空振りしていないことの担保）。
            view.setRootDirect(
                SettingsRoot(
                    sections = listOf(
                        Section(
                            id = "s1",
                            header = SectionAccessory.Text("見出し"),
                            cells = listOf(
                                LabelCell(id = "c1", title = "A"),
                                LabelCell(id = "c2", title = "B", isVisible = false),
                                LabelCell(id = "c3", title = "C"),
                                LabelCell(id = "c4", title = "D"),
                            ),
                        ),
                    ),
                ),
            )
            awaitConvergence(view) { observer.events.isNotEmpty() }
            assertTrue("内容が変われば差分通知が出る", observer.events.isNotEmpty())
        } finally {
            adapter.unregisterAdapterDataObserver(observer)
        }
    }
}
