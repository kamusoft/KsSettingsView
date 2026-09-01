package jp.kamusoft.kssettingsview.ui

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentActivity
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * `KsSettingsView.unbind()` が Store 購読を解除し、以後の Store 更新が表示へ反映されなくなる
 * ことを検証する。
 *
 * `onDetachedFromWindow` による購読の停止と違い、`unbind()` は Store 参照ごと手放すため、
 * 再 attach しても購読が復活しないところまで見る。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StoreUnbindTest {

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

    /** 検証に使う root。Section 1 個に Cell A / B。 */
    private fun sampleStore(): SettingsRootStore = SettingsRootStore(
        initialRoot = SettingsRoot(
            sections = listOf(
                Section(
                    id = "s1",
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

    private fun startActivity(): HostActivity {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        return ctrl.get()
    }

    @Test
    fun `unbind 後の Store 更新は表示へ反映されない`() {
        val activity = startActivity()
        val view = activity.settingsView
        val store = sampleStore()
        view.bind(store)
        awaitConvergence(view) { committedTexts(view) == listOf("A", "B") }
        activity.layoutSettingsView()

        // 対照: 解除前の更新は購読経由で届く（購読が実在することの担保）。
        store.replaceCell("c2", LabelCell(id = "c2", title = "B-updated"))
        awaitConvergence(view) { committedTexts(view) == listOf("A", "B-updated") }
        activity.layoutSettingsView()
        assertEquals(listOf("A", "B-updated"), visibleRowTexts(view))

        view.unbind()

        // 「届かないこと」は条件成立を待つ形にできないため、キューを流し切ってから状態を確かめる。
        store.insertCell(LabelCell(id = "c3", title = "C"), sectionId = "s1", at = 2)
        store.replaceCell("c1", LabelCell(id = "c1", title = "A-updated"))
        store.applyTheme(Theme(backgroundColor = Color(0xFF102030)))
        idle()
        activity.layoutSettingsView()

        assertEquals("解除後の構造・内容更新は表示へ届かない", listOf("A", "B-updated"), visibleRowTexts(view))
        assertEquals("解除後の Theme 更新も届かない", Theme(), view.internalTheme())
        assertEquals(
            Theme().backgroundColor.toArgb(),
            (view.internalRecyclerView().background as ColorDrawable).color,
        )
    }

    @Test
    fun `unbind 後は再 attach しても購読が復活しない`() {
        val activity = startActivity()
        val view = activity.settingsView
        val store = sampleStore()
        view.bind(store)
        awaitConvergence(view) { committedTexts(view) == listOf("A", "B") }
        activity.layoutSettingsView()

        view.unbind()
        activity.container.removeView(view)
        activity.container.addView(view)
        // 「届かないこと」は条件成立を待つ形にできないため、キューを流し切ってから状態を確かめる。
        store.replaceCell("c1", LabelCell(id = "c1", title = "A-updated"))
        idle()
        activity.layoutSettingsView()

        assertEquals("再 attach でも Store の更新は届かない", listOf("A", "B"), visibleRowTexts(view))
    }

    @Test
    fun `unbind は冪等で bind し直せる`() {
        val activity = startActivity()
        val view = activity.settingsView
        val store = sampleStore()
        view.bind(store)
        awaitConvergence(view) { committedTexts(view) == listOf("A", "B") }
        activity.layoutSettingsView()

        view.unbind()
        view.unbind()
        assertEquals("解除の繰り返しで表示は失われない", listOf("A", "B"), visibleRowTexts(view))

        // 未 bind の View への解除も何も起こさない。
        KsSettingsView(activity).unbind()

        view.bind(store)
        store.replaceCell("c1", LabelCell(id = "c1", title = "A-updated"))
        awaitConvergence(view) { committedTexts(view) == listOf("A-updated", "B") }
        activity.layoutSettingsView()
        assertEquals("bind し直せば再び追従する", listOf("A-updated", "B"), visibleRowTexts(view))
    }
}
