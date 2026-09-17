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
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * 表示中の画面に対して複数 Section の view accessory を続けて差し替えたとき、
 * すべてが表示へ反映されることを確認する。
 *
 * 更新は Section ごとの単発呼び出しであり、反映通知は Section ごとに発行される。
 * 先行する通知が後続に追い越されて破棄されると、一部の Section だけ古い view のまま残る。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ConsecutiveAccessoryViewSwapTest {

    /** Material3 テーマを持つ器。Host はこの上に置く。 */
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

    /** 同一インスタンスを返し続ける factory で view accessory を作る。 */
    private fun accessoryOf(view: View): SectionAccessory.View =
        SectionAccessory.View(KsAnyView.AndroidView { view })

    private val sectionIds = listOf("s1", "s2", "s3")

    @Test
    fun `複数 Section の view accessory を続けて差し替えるとすべて表示へ反映される`() {
        val initial = sectionIds.map { View(ctx) }
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()

        val store = SettingsRootStore(
            initialRoot = SettingsRoot(
                sections = sectionIds.mapIndexed { index, id ->
                    Section(
                        id = id,
                        header = accessoryOf(initial[index]),
                        cells = listOf(LabelCell(id = "c$index", title = "行 $index")),
                    )
                },
            ),
        )
        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.bind(store)
        idle()
        layout(activity, view)

        // 前提: 初期表示に 3 件の view accessory が載っている。
        sectionIds.indices.forEach { index ->
            assertSame(
                "前提: Section $index の初期 accessory が表示に載っていない",
                initial[index],
                headerContentAt(view, index),
            )
        }

        // 同じ操作の中で 3 件を続けて差し替える (間にメインループを流さない)。
        val replacements = sectionIds.map { View(ctx) }
        val swapped = sectionIds.mapIndexed { index, id ->
            val accessory = accessoryOf(replacements[index])
            store.updateAccessory(
                target = AccessoryTarget.SectionHeader(id),
                accessory = SettingsAccessory.Section(accessory),
            )
            accessory
        }

        awaitDifferCommit(
            committedSummary = { committedTexts(view) },
        ) {
            val committed = committedHeaderAccessories(view)
            committed.size == swapped.size &&
                committed.indices.all { committed[it] === swapped[it] }
        }
        layout(activity, view)

        sectionIds.indices.forEach { index ->
            assertSame(
                "Section $index の差し替えた accessory が表示へ反映されていない",
                replacements[index],
                headerContentAt(view, index),
            )
        }
        assertEquals(
            "旧 accessory が親に残っている",
            emptyList<Int>(),
            initial.indices.filter { initial[it].parent != null },
        )
    }

    /** 割り当て領域を確定させて行を生成させる。 */
    private fun layout(activity: HostActivity, view: KsSettingsView) {
        val metrics = activity.resources.displayMetrics
        view.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    /** コミット済みリストに載っている Section header の accessory を上から順に取り出す。 */
    private fun committedHeaderAccessories(view: KsSettingsView): List<SectionAccessory?> =
        view.internalMainListAdapter().currentList
            .filterIsInstance<CellListItem.SectionHeader>()
            .map { it.accessory }

    /** 指定 Section の header 行に実際に載っている view。 */
    private fun headerContentAt(view: KsSettingsView, sectionIndex: Int): View? {
        val rv = view.internalRecyclerView()
        // 平坦リストは Section ごとに [SectionHeader, CellRow] の 2 行になる。
        val position = sectionIndex * 2
        val holder = rv.findViewHolderForAdapterPosition(position)
            as? SectionAnyViewAccessoryViewHolder
        return (holder?.itemView as? FrameLayout)?.getChildAt(0)
    }
}
