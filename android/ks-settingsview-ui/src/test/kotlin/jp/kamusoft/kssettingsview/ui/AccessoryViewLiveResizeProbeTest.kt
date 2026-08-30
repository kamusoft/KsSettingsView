package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
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
 * 自動高さの view accessory で、中身のサイズだけが変わったときに Header 領域の高さが追従するかを
 * 観測する検証。accessory の差し替えを伴わず、`requestLayout()` の伝播だけで届くかを見る。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AccessoryViewLiveResizeProbeTest {

    class HostActivity : FragmentActivity() {
        lateinit var container: FrameLayout

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(MaterialR.style.Theme_Material3_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            container = FrameLayout(this)
            setContentView(container)
        }
    }

    private var controller: ActivityController<HostActivity>? = null

    @After
    fun tearDown() {
        controller?.close()
        controller = null
    }

    private val ctx: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun dpToPx(dp: Double): Int = (dp * ctx.resources.displayMetrics.density).toInt()

    @Test
    fun `内容のサイズ変化が requestLayout だけで Header 領域の高さへ届く`() {
        val content = FrameLayout(ctx).apply { minimumHeight = dpToPx(30.0) }
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()

        val store = SettingsRootStore(
            initialRoot = SettingsRoot(
                sections = listOf(
                    Section(
                        id = "s1",
                        header = SectionAccessory.View(KsAnyView.AndroidView { content }),
                        headerHeight = -1.0,
                        cells = listOf(LabelCell(id = "c1", title = "A")),
                    ),
                ),
            ),
        )
        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.bind(store)
        idle()

        fun layout() {
            val metrics = activity.resources.displayMetrics
            view.measure(
                View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        }

        fun headerRowHeight(): Int {
            val rv = view.internalRecyclerView()
            return rv.findViewHolderForAdapterPosition(0)!!.itemView.height
        }

        layout()
        assertEquals("前提: 初期の Header 高さが内容なりになっていない", dpToPx(30.0), headerRowHeight())

        // accessory は据え置きで中身のサイズだけを変え、レイアウト要求のみで伝播させる
        content.minimumHeight = dpToPx(90.0)
        content.requestLayout()
        idle()
        layout()

        assertEquals(
            "中身のサイズ変化が Header 領域の高さへ届いていない",
            dpToPx(90.0),
            headerRowHeight(),
        )
    }
}
