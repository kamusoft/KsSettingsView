package jp.kamusoft.kssettingsview.ui

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Root 対象の配送の途中で binding が差し替わっても、以前の Store の値が表示に出ないことを検証する
 * （core/ADR-0033）。
 *
 * Store の知らせはどのスレッドからでも届くため、「以前の Store の値は反映しない」という保証は、
 * bind / unbind と配送が順番に起きる場合だけでは足りない。ある Store が配送している最中に別の Store へ
 * bind し直す・unbind するという重なりでも、受理されるのは現在の binding と一致する更新だけになる。
 *
 * 重なりは、同じ Store に先に登録した受け口で配送を止めて作る。配送は登録した順に進むため、先頭の
 * 受け口が待っている間、Host へはまだ知らせが届いていない。その間にメインスレッドで binding を
 * 差し替え、止めていた配送を再開させると、Host は差し替え後に以前の Store の知らせを受け取る。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RootAccessoryRebindRaceTest {

    /** `KsSettingsView` を載せる器だけを持つホスト Activity。 */
    class HostActivity : FragmentActivity() {
        lateinit var container: FrameLayout

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(MaterialR.style.Theme_Material3_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            container = FrameLayout(this)
            setContentView(container)
        }
    }

    /**
     * 知らせを受けた場で配送を止め、解放されるまで待つ受け口。
     *
     * 止めている間に binding を差し替えることで、配送とのすれ違いを作る。
     */
    private class BarrierReceiver : RootAccessoryReceiver {

        /** 配送が止まった（この受け口まで届いた）ことを知らせる。 */
        val reached: CountDownLatch = CountDownLatch(1)

        private val released: CountDownLatch = CountDownLatch(1)

        override fun receiveRootAccessoryUpdate(
            source: SettingsRootStore,
            update: SettingsRootDiff.UpdateAccessory,
        ) {
            reached.countDown()
            released.await(AWAIT_SECONDS, TimeUnit.SECONDS)
        }

        /** 止めていた配送を再開させる。 */
        fun release() {
            released.countDown()
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

    /** レイアウトを走らせて RecyclerView に行を生成させる。 */
    private fun HostActivity.layoutSettingsView(target: KsSettingsView) {
        val metrics = resources.displayMetrics
        target.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        target.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    private fun initialRoot(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(
                id = "s1",
                header = SectionAccessory.Text("初期見出し"),
                cells = listOf(LabelCell(id = "c1", title = "A")),
            ),
        ),
    )

    private val initialRows = listOf("初期見出し", "A")

    /** 中身に [label] を表示する TextView を持つ、任意 View 形式の Root accessory。 */
    private fun viewAccessory(label: String): SettingsAccessory.Root = SettingsAccessory.Root(
        RootAccessory.View(
            KsAnyView.AndroidView { context -> TextView(context).apply { text = label } },
        ),
    )

    /** 取り付けてから、Cell 行のコミットとレイアウトを済ませる。 */
    private fun HostActivity.attachAndSettle(target: KsSettingsView) {
        container.addView(target)
        awaitConvergence(target) { committedTexts(target) == initialRows }
        layoutSettingsView(target)
    }

    /**
     * [barrier] で配送を止めた状態で [duringDelivery] を行い、配送を再開してから表示を確かめる。
     *
     * @return 配送の再開後にメインループを流し切り、レイアウトを済ませた時点の表示行
     */
    private fun deliverAcross(
        activity: HostActivity,
        view: KsSettingsView,
        store: SettingsRootStore,
        barrier: BarrierReceiver,
        duringDelivery: () -> Unit,
    ): List<String> {
        val worker = Thread {
            store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("ヘッダ"))
        }
        worker.start()

        assertTrue(
            "前提: 配送が Host より前の受け口で止まる",
            barrier.reached.await(AWAIT_SECONDS, TimeUnit.SECONDS),
        )
        duringDelivery()
        barrier.release()
        worker.join(TimeUnit.SECONDS.toMillis(AWAIT_SECONDS))
        assertTrue("配送を行うスレッドが終わらない", !worker.isAlive)

        idle()
        activity.layoutSettingsView(view)
        return visibleRowTexts(view)
    }

    @Test
    fun `配送の途中で別 Store へ bind し直すと以前の Store の値は反映されない`() {
        val activity = startActivity()

        val view = KsSettingsView(activity)
        val first = SettingsRootStore(initialRoot = initialRoot())
        val second = SettingsRootStore(initialRoot = initialRoot())
        val barrier = BarrierReceiver()
        first.registerRootAccessoryReceiver(barrier)
        view.bind(first)
        activity.attachAndSettle(view)

        val rows = deliverAcross(activity, view, first, barrier) { view.bind(second) }

        assertEquals("配送とすれ違った以前の Store の値は表示されない", initialRows, rows)
    }

    @Test
    fun `配送の途中で unbind すると以前の Store の値は反映されない`() {
        val activity = startActivity()

        val view = KsSettingsView(activity)
        val store = SettingsRootStore(initialRoot = initialRoot())
        val barrier = BarrierReceiver()
        store.registerRootAccessoryReceiver(barrier)
        view.bind(store)
        activity.attachAndSettle(view)

        val rows = deliverAcross(activity, view, store, barrier) { view.unbind() }

        assertEquals("配送とすれ違った unbind 後の値は表示されない", initialRows, rows)
    }

    private companion object {
        const val AWAIT_SECONDS = 5L
    }
}
