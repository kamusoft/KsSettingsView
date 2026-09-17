package jp.kamusoft.kssettingsview.ui

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentActivity
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * Header / Footer 背景色の既定が透明であり、何も指定しない領域に list 下地が見えることの検証。
 *
 * 既定が透明であることは `KsSettingsViewDefaults` の値としても固定しているが、それだけでは描画へ
 * 届いたかが分からない。ここでは実際に表示した Header / Footer の `TextView` に敷かれている背景を
 * 観測し、透明であること (= 下に敷かれた list 下地がそのまま見えること) を確かめる。
 *
 * 未指定色は描画時に現在の外観の既定へ解決されるため、夜間モードでない Configuration と夜間モードの
 * 両方で見る。片方だけでは、もう一方のセットが旧既定 (不透明) のまま取り残されていても通る。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HeaderFooterBackgroundDefaultTest {

    /** `KsSettingsView` を 1 つだけ載せるホスト Activity。 */
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

    /** 検証に使う list 下地。既定のどちらのセットとも違う値にして、取り違えを検出できるようにする。 */
    private val listBackdrop = Color(0xFF123456)

    private fun rootWithTextAccessories(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(
                id = "s1",
                header = SectionAccessory.Text("見出し"),
                footer = SectionAccessory.Text("説明"),
                cells = listOf(LabelCell(id = "c1", title = "A")),
            ),
        ),
    )

    private fun showView(theme: Theme): KsSettingsView {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()
        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.bind(SettingsRootStore(initialRoot = rootWithTextAccessories(), initialTheme = theme))
        awaitConvergence(view) { view.internalMainListAdapter().currentList.isNotEmpty() }
        val metrics = view.resources.displayMetrics
        view.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        idle()
        return view
    }

    /** Section H/F の `TextView` に実際に敷かれている背景色 (ARGB)。 */
    private fun accessoryBackgrounds(view: KsSettingsView): List<Int> {
        val recyclerView = view.internalRecyclerView()
        return (0 until recyclerView.childCount)
            .map { recyclerView.getChildViewHolder(recyclerView.getChildAt(it)) }
            .filterIsInstance<SectionTextAccessoryViewHolder>()
            .map { ((it.itemView as TextView).background as ColorDrawable).color }
    }

    /** `RecyclerView` に実際に敷かれている list 下地 (ARGB)。 */
    private fun listBackground(view: KsSettingsView): Int =
        (view.internalRecyclerView().background as ColorDrawable).color

    @Test
    @Config(qualifiers = "notnight")
    fun `何も指定しない Header と Footer には list 下地が見える`() {
        val view = showView(Theme(backgroundColor = listBackdrop))

        val backgrounds = accessoryBackgrounds(view)
        assertEquals("前提: Header と Footer の行が生成されている", 2, backgrounds.size)
        for (background in backgrounds) {
            assertEquals(
                "Header / Footer にライブラリの背景は描画されない (透明)",
                Color.Transparent.toArgb(),
                background,
            )
        }
        assertEquals("list 下地は明示した色のまま", listBackdrop.toArgb(), listBackground(view))
    }

    @Test
    @Config(qualifiers = "night")
    fun `夜間モードでも何も指定しない Header と Footer には list 下地が見える`() {
        val view = showView(Theme(backgroundColor = listBackdrop))

        val backgrounds = accessoryBackgrounds(view)
        assertEquals("前提: Header と Footer の行が生成されている", 2, backgrounds.size)
        for (background in backgrounds) {
            assertEquals(
                "夜間モードでも Header / Footer にライブラリの背景は描画されない (透明)",
                Color.Transparent.toArgb(),
                background,
            )
        }
        assertEquals("list 下地は明示した色のまま", listBackdrop.toArgb(), listBackground(view))
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `明示指定した Header と Footer の背景色はそのまま描画される`() {
        val header = Color(0xFFAA3355)
        val footer = Color(0xFF3355AA)
        val view = showView(
            Theme(
                backgroundColor = listBackdrop,
                headerBackgroundColor = header,
                footerBackgroundColor = footer,
            ),
        )

        val backgrounds = accessoryBackgrounds(view)
        assertEquals("前提: Header と Footer の行が生成されている", 2, backgrounds.size)
        assertEquals("Header は明示した色で描画される", header.toArgb(), backgrounds[0])
        assertEquals("Footer は明示した色で描画される", footer.toArgb(), backgrounds[1])
        assertNotEquals("前提: 明示した色は透明ではない", Color.Transparent.toArgb(), backgrounds[0])
    }
}
