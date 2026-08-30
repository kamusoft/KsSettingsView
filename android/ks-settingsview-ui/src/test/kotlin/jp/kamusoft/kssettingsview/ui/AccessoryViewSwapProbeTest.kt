package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * view accessory を別インスタンスへ差し替えたときに、旧 view が表示から剥がれるかを観測する検証。
 *
 * 同一 View インスタンスを返し続ける factory を前提に、剥がれ方と再 attach の安全性を確かめる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AccessoryViewSwapProbeTest {

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

    /** 同一インスタンスを返し続ける factory。 */
    private fun constantAccessory(view: View): SectionAccessory.View =
        SectionAccessory.View(KsAnyView.AndroidView { view })

    /** 返す前に既存の親から外す factory。 */
    private fun detachingAccessory(view: View): SectionAccessory.View =
        SectionAccessory.View(
            KsAnyView.AndroidView {
                (view.parent as? ViewGroup)?.removeView(view)
                view
            },
        )

    private fun startHost(accessory: SectionAccessory.View): Host {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()

        val store = SettingsRootStore(
            initialRoot = SettingsRoot(
                sections = listOf(
                    Section(
                        id = "s1",
                        header = accessory,
                        cells = listOf(LabelCell(id = "c1", title = "A")),
                    ),
                ),
            ),
        )
        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.bind(store)
        idle()
        val host = Host(activity, view, store)
        host.layout()
        return host
    }

    private inner class Host(
        val activity: HostActivity,
        val view: KsSettingsView,
        val store: SettingsRootStore,
    ) {
        fun layout() {
            val metrics = activity.resources.displayMetrics
            view.measure(
                View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        }

        /** 表示中の Header 行に載っている accessory の中身。 */
        fun headerContent(): View? {
            val rv = view.internalRecyclerView()
            val holder = rv.findViewHolderForAdapterPosition(0) as? SectionAnyViewAccessoryViewHolder
            return (holder?.itemView as? FrameLayout)?.getChildAt(0)
        }

        fun committedSummary(): List<String> =
            view.internalMainListAdapter().currentList.map { it::class.simpleName ?: "?" }

        fun committedHeaderAccessory(): SectionAccessory? =
            view.internalMainListAdapter().currentList
                .filterIsInstance<CellListItem.SectionHeader>()
                .firstOrNull()
                ?.accessory

        /** `updateAccessory` で header の accessory を差し替え、表示へ届くまで進める。 */
        fun swapHeader(accessory: SectionAccessory) {
            store.updateAccessory(
                target = AccessoryTarget.SectionHeader("s1"),
                accessory = SettingsAccessory.Section(accessory),
            )
            awaitDifferCommit({ committedSummary() }) { committedHeaderAccessory() === accessory }
            layout()
        }
    }

    @Test
    fun `updateAccessory の view 差し替えで旧 view が親から剥がれる`() {
        val old = View(ctx)
        val host = startHost(constantAccessory(old))
        assertSame("前提: 初期表示に旧 view が載っていない", old, host.headerContent())

        val new = View(ctx)
        host.swapHeader(constantAccessory(new))

        assertSame("新しい view が表示されていない", new, host.headerContent())
        assertNull("旧 view が親から剥がれていない", old.parent)
    }

    @Test
    fun `view から text への切替で旧 view が親から剥がれる`() {
        val old = View(ctx)
        val host = startHost(constantAccessory(old))
        assertSame(old, host.headerContent())

        host.swapHeader(SectionAccessory.Text("テキスト"))

        assertNull("text へ切り替えても旧 view が親に残っている", old.parent)
    }

    /**
     * 同一インスタンスを返す factory のまま、一度 text へ落として再度同じ view を設定する。
     * 親から外れないまま再設定される経路が壊れるかどうかを観測する。
     */
    @Test
    fun `同一 view インスタンスを text 経由で再設定しても表示に復帰する`() {
        val reused = View(ctx)
        val host = startHost(constantAccessory(reused))

        host.swapHeader(SectionAccessory.Text("テキスト"))
        host.swapHeader(constantAccessory(reused))

        assertSame("再設定した view が表示されていない", reused, host.headerContent())
    }

    /**
     * detach 付き factory なら、別の親に付いたままの view を accessory へ設定できる。
     */
    @Test
    fun `detach 付き factory なら別の親に付いた view を設定できる`() {
        val reused = View(ctx)
        val strayParent = FrameLayout(ctx)
        strayParent.addView(reused)
        assertNotNull("前提: 別の親に付いた状態を作れていない", reused.parent)

        val host = startHost(detachingAccessory(reused))
        assertSame("detach 付き factory でも表示に載らない", reused, host.headerContent())
        assertTrue("旧親から外れていない", strayParent.childCount == 0)
    }

    /**
     * detach を入れない factory で、別の親に付いたままの view を accessory に設定した場合。
     * Android の `addView` は親付きの子を拒否するため、これが detach 対策が必要な理由になる。
     */
    @Test
    fun `detach なし factory で別の親に付いた view を設定すると IllegalStateException になる`() {
        val reused = View(ctx)
        val strayParent = FrameLayout(ctx)
        strayParent.addView(reused)

        assertThrows(IllegalStateException::class.java) {
            startHost(constantAccessory(reused))
        }
    }
}
