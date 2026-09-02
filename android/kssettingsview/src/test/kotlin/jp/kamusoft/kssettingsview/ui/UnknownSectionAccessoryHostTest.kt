package jp.kamusoft.kssettingsview.ui

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * 表示中の Host に対する未知 sectionId の `updateAccessory` が、Host を沈黙させないことを検証する。
 *
 * Store が未知 sectionId の Diff を emit しないため（core/ADR-0020）、Host の missing ID 検出
 * （`KsCellRegistry.strictMode` による `IllegalStateException`）には到達しない。この例外は Diff 購読
 * コルーチンの内側で起きるためテストの呼び出し元へは伝わらず、購読ごと停止する形で現れる。
 * したがって「表示が変わらないこと」だけでは足りず、**後続の更新が表示へ届くこと**まで確かめて
 * 購読の生存を観察する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UnknownSectionAccessoryHostTest {

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

    private fun startActivity(): HostActivity {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        return ctrl.get()
    }

    /** 検証に使う root。header "S1" の Section 1 個に Cell A / B。 */
    private fun sampleStore(): SettingsRootStore = SettingsRootStore(
        initialRoot = SettingsRoot(
            sections = listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.Text("S1"),
                    cells = listOf(LabelCell(id = "c1", title = "A"), LabelCell(id = "c2", title = "B")),
                ),
            ),
        ),
    )

    /** レイアウトを走らせて RecyclerView に行を生成させる。 */
    private fun HostActivity.layoutSettingsView() {
        val metrics = resources.displayMetrics
        settingsView.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        settingsView.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    @Test
    fun `strictMode 既定のまま未知 sectionId の updateAccessory を呼んでも Host は沈黙しない`() {
        assertTrue("前提: strictMode の既定は true", KsCellRegistry.strictMode)

        val activity = startActivity()
        val view = activity.settingsView
        val store = sampleStore()
        view.bind(store)
        awaitConvergence(view) { committedTexts(view) == listOf("S1", "A", "B") }
        activity.layoutSettingsView()
        assertEquals(listOf("S1", "A", "B"), visibleRowTexts(view))

        // 未知 sectionId の header / footer 更新。Store が Diff を emit しないため Host には何も届かない。
        store.updateAccessory(
            target = AccessoryTarget.SectionHeader(sectionId = "bogus"),
            accessory = SettingsAccessory.Section(SectionAccessory.Text("X")),
        )
        store.updateAccessory(
            target = AccessoryTarget.SectionFooter(sectionId = "bogus"),
            accessory = SettingsAccessory.Section(SectionAccessory.Text("Y")),
        )
        // 「届かないこと」は条件成立を待つ形にできないため、キューを流し切ってから状態を確かめる。
        idle()
        activity.layoutSettingsView()

        assertEquals("未知 sectionId の呼び出しで表示は変化しない", listOf("S1", "A", "B"), visibleRowTexts(view))

        // 後続の既知 cellId の内容更新が表示へ届く（Diff 購読が生きている）。
        store.replaceCell("c2", LabelCell(id = "c2", title = "B-updated"))
        awaitConvergence(view) { committedTexts(view) == listOf("S1", "A", "B-updated") }
        activity.layoutSettingsView()

        assertEquals(
            "後続の replaceCell が表示へ届く（Diff 購読が生きている）",
            listOf("S1", "A", "B-updated"),
            visibleRowTexts(view),
        )
    }
}
