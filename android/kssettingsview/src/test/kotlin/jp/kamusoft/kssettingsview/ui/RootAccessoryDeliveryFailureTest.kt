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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import kotlin.coroutines.cancellation.CancellationException

/**
 * ある受け口での反映の失敗が、他の受け口への知らせと Store の通知の発行を妨げないことを検証する
 * （core/ADR-0033）。
 *
 * Root 対象の更新は、Store が結び付いている相手へその場で知らせる。知らせた先で例外が出ても、
 * 更新を渡した呼び出し元へは伝わらず、後続の相手への知らせも `diffs` への発行も続く。この握り潰しは
 * 入れ違えても他の検証が落ちないため、失敗する相手を先に並べた形で固定する。
 *
 * ただしコルーチンのキャンセルの合図だけは握り潰さず呼び出し元へ伝える。あわせて固定する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RootAccessoryDeliveryFailureTest {

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

    /** 知らせを受けるたびに失敗する受け口。 */
    private class FailingReceiver : RootAccessoryReceiver {

        /** 知らされた回数。 */
        var deliveries: Int = 0
            private set

        override fun receiveRootAccessoryUpdate(
            source: SettingsRootStore,
            update: SettingsRootDiff.UpdateAccessory,
        ) {
            deliveries += 1
            throw IllegalStateException("root accessory could not be applied")
        }
    }

    /** 知らせを受けるたびにコルーチンのキャンセルの合図を投げる受け口。 */
    private class CancellingReceiver : RootAccessoryReceiver {

        /** 知らされた回数。 */
        var deliveries: Int = 0
            private set

        override fun receiveRootAccessoryUpdate(
            source: SettingsRootStore,
            update: SettingsRootDiff.UpdateAccessory,
        ) {
            deliveries += 1
            throw CancellationException("root accessory delivery was cancelled")
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

    @Test
    fun `ある受け口での反映の失敗は他の Host の受け取りと通知の発行を妨げない`() {
        val activity = startActivity()

        val store = SettingsRootStore(initialRoot = initialRoot())
        // 失敗する相手を先に登録し、その後ろに並ぶ Host まで知らせが進むことを見る。
        val failing = FailingReceiver()
        store.registerRootAccessoryReceiver(failing)

        val view = KsSettingsView(activity)
        view.bind(store)
        activity.container.addView(view)
        awaitConvergence(view) { committedTexts(view) == initialRows }
        activity.layoutSettingsView(view)

        val emitted = mutableListOf<SettingsRootDiff>()
        val collectScope = CoroutineScope(Dispatchers.Unconfined)
        collectScope.launch { store.diffs.collect { emitted.add(it) } }

        // 例外が呼び出し元へ伝わるとここで落ちる。
        store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("ヘッダ"))
        idle()
        activity.layoutSettingsView(view)

        assertEquals("前提: 失敗する相手にも知らせが届いている", 1, failing.deliveries)
        assertEquals(
            "後続の Host は値を受け取り表示へ反映する",
            listOf("ヘッダ") + initialRows,
            visibleRowTexts(view),
        )
        assertEquals(
            "Root 対象の通知は失敗に関わらず 1 回発行される",
            listOf(AccessoryTarget.RootHeader),
            emitted.filterIsInstance<SettingsRootDiff.UpdateAccessory>().map { it.target },
        )
        assertEquals("発行される通知は Root 対象の更新だけ", 1, emitted.size)

        collectScope.cancel()
    }

    @Test
    fun `受け口でのキャンセルの合図は握り潰さず呼び出し元へ伝える`() {
        val store = SettingsRootStore(initialRoot = initialRoot())
        val cancelling = CancellingReceiver()
        store.registerRootAccessoryReceiver(cancelling)

        assertThrows(CancellationException::class.java) {
            store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("ヘッダ"))
        }

        assertEquals("前提: 受け口に知らせが届いている", 1, cancelling.deliveries)
    }
}
