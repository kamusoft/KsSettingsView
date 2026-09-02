package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
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
 * accessory の中身がサイズを変えたときに、領域の高さを測り直す口が Store から Host へ届くことを
 * 実描画で検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AccessoryMeasureInvalidationTest {

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

    /**
     * 計測高さを後から変えられる accessory の中身。
     *
     * 中身が自分でレイアウト要求を出さないため、領域が追従するのは再計測要求が届いたときだけになる。
     */
    private class ProbeView(context: Context, var contentHeight: Int) : View(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            setMeasuredDimension(resolveSize(0, widthMeasureSpec), contentHeight)
        }
    }

    private class Attachment(
        val store: SettingsRootStore,
        val view: KsSettingsView,
        val activity: HostActivity,
    ) {
        fun layout() {
            val metrics = activity.resources.displayMetrics
            view.measure(
                View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        }

        fun pump() {
            idle()
            layout()
        }

        fun rowHeightAt(position: Int): Int? =
            view.internalRecyclerView().findViewHolderForAdapterPosition(position)?.itemView?.height
    }

    private fun attach(root: SettingsRoot): Attachment {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()
        val store = SettingsRootStore(initialRoot = root)
        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.bind(store)
        val attachment = Attachment(store, view, activity)
        attachment.pump()
        return attachment
    }

    /** Store の再計測要求で、対象 Section の header 行の高さが中身の新しい高さへ追従する。 */
    @Test
    fun `Store 経由の要求で section header の高さが追従する`() {
        val inner = ProbeView(ctx, 70)
        val attachment = attach(
            SettingsRoot(
                sections = listOf(
                    Section(
                        id = "s1",
                        header = SectionAccessory.View(KsAnyView.AndroidView { inner }),
                        cells = listOf(LabelCell(id = "c1", title = "A")),
                    ),
                ),
            ),
        )
        assertEquals("前提: 初期高さが中身の高さになっていない", 70, attachment.rowHeightAt(0))

        inner.contentHeight = 140
        attachment.store.invalidateAccessoryMeasurement(AccessoryTarget.SectionHeader("s1"))
        attachment.pump()

        assertEquals(140, attachment.rowHeightAt(0))
    }

    /** footer も同じ経路で追従する。 */
    @Test
    fun `Store 経由の要求で section footer の高さが追従する`() {
        val inner = ProbeView(ctx, 50)
        val attachment = attach(
            SettingsRoot(
                sections = listOf(
                    Section(
                        id = "s1",
                        header = SectionAccessory.Text("H"),
                        footer = SectionAccessory.View(KsAnyView.AndroidView { inner }),
                        cells = listOf(LabelCell(id = "c1", title = "A")),
                    ),
                ),
            ),
        )
        assertEquals(50, attachment.rowHeightAt(2))

        inner.contentHeight = 120
        attachment.store.invalidateAccessoryMeasurement(AccessoryTarget.SectionFooter("s1"))
        attachment.pump()

        assertEquals(120, attachment.rowHeightAt(2))
    }

    /** Root header も同じ経路で追従する。 */
    @Test
    fun `Store 経由の要求で root header の高さが追従する`() {
        val inner = ProbeView(ctx, 60)
        val attachment = attach(
            SettingsRoot(
                sections = listOf(Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A")))),
            ),
        )
        attachment.store.updateAccessory(
            target = AccessoryTarget.RootHeader,
            accessory = SettingsAccessory.Root(RootAccessory.View(KsAnyView.AndroidView { inner })),
        )
        attachment.pump()
        assertEquals("前提: Root header の初期高さが中身の高さになっていない", 60, attachment.rowHeightAt(0))

        inner.contentHeight = 130
        attachment.store.invalidateAccessoryMeasurement(AccessoryTarget.RootHeader)
        attachment.pump()

        assertEquals(130, attachment.rowHeightAt(0))
    }

    /** 固定高さの header では、再計測要求を出しても高さが変わらない。 */
    @Test
    fun `固定高さの header は再計測要求で変化しない`() {
        val inner = ProbeView(ctx, 40)
        val fixedHeight = (80.0 * ctx.resources.displayMetrics.density).toInt()
        val attachment = attach(
            SettingsRoot(
                sections = listOf(
                    Section(
                        id = "s1",
                        header = SectionAccessory.View(KsAnyView.AndroidView { inner }),
                        headerHeight = 80.0,
                        cells = listOf(LabelCell(id = "c1", title = "A")),
                    ),
                ),
            ),
        )
        assertEquals("前提: 固定高さが適用されていない", fixedHeight, attachment.rowHeightAt(0))

        inner.contentHeight = 200
        attachment.store.invalidateAccessoryMeasurement(AccessoryTarget.SectionHeader("s1"))
        attachment.pump()

        assertEquals(fixedHeight, attachment.rowHeightAt(0))
    }

    /** 現在状態に存在しない sectionID への要求は、表示を変えない。 */
    @Test
    fun `未知の sectionID への要求は表示を変えない`() {
        val inner = ProbeView(ctx, 70)
        val attachment = attach(
            SettingsRoot(
                sections = listOf(
                    Section(
                        id = "s1",
                        header = SectionAccessory.View(KsAnyView.AndroidView { inner }),
                        cells = listOf(LabelCell(id = "c1", title = "A")),
                    ),
                ),
            ),
        )

        inner.contentHeight = 140
        attachment.store.invalidateAccessoryMeasurement(AccessoryTarget.SectionHeader("unknown"))
        attachment.pump()

        assertEquals(
            "別の対象への要求で高さが変わってはいけない",
            70,
            attachment.rowHeightAt(0),
        )
    }

    /** Store から切り離したあとの要求は Host に届かない。 */
    @Test
    fun `unbind 後の要求は届かない`() {
        val inner = ProbeView(ctx, 70)
        val attachment = attach(
            SettingsRoot(
                sections = listOf(
                    Section(
                        id = "s1",
                        header = SectionAccessory.View(KsAnyView.AndroidView { inner }),
                        cells = listOf(LabelCell(id = "c1", title = "A")),
                    ),
                ),
            ),
        )

        attachment.view.unbind()
        inner.contentHeight = 140
        attachment.store.invalidateAccessoryMeasurement(AccessoryTarget.SectionHeader("s1"))
        attachment.pump()

        assertEquals(70, attachment.rowHeightAt(0))
    }
}
