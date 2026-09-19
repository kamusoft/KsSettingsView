package jp.kamusoft.kssettingsview.ui

import android.os.Bundle
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
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
import java.lang.ref.WeakReference

/**
 * 長命の Store に bind したまま手放された Host が回収可能であることを検証する（core/ADR-0033）。
 *
 * Store は Root の header / footer を知らせる相手として Host を弱参照で覚える。強参照になっていると、
 * Store が生きているかぎり Host とその Context が生き残る。参照が切れた登録は、次に Root 対象を
 * 知らせる時点で取り除かれる。
 *
 * # 単独のテストクラスにしている理由
 *
 * 回収の判定は `System.gc()` の要求に依存し、同一 JVM に積み上がったヒープの状態に左右される。
 * 他の検証と同居させると、回収されるかどうかが同居するテストの数と順序で揺れて、判定が信用できなく
 * なる。参照保持の検証だけを独立させることで、失敗が「本当に解放されていない」ことだけを意味する
 * 状態を保つ。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RootAccessoryReceiverReleaseTest {

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

    @Test
    fun `unbind されずに手放された Host は回収でき Store の登録も取り除かれる`() {
        val activity = startActivity()
        val store = SettingsRootStore(
            initialRoot = SettingsRoot(
                sections = listOf(Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A")))),
            ),
        )

        val reference = bindAndAbandonHost(activity, store)
        assertEquals("前提: Host の登録が Store に残っている", 1, store.internalRootAccessoryReceiverCount())

        assertTrue("手放した Host が回収されない", awaitCollected(reference))

        store.updateAccessory(
            target = AccessoryTarget.RootHeader,
            accessory = SettingsAccessory.Root(RootAccessory.Text("ヘッダ")),
        )
        idle()

        assertEquals(
            "参照が切れた登録は次に知らせる時点で取り除かれる",
            0,
            store.internalRootAccessoryReceiverCount(),
        )
    }

    /**
     * Host を作って bind し、`unbind` を呼ばずに参照を手放して弱参照だけを返す。
     *
     * 強参照をテストメソッドのローカル変数に残すと、スタック上の参照で回収が妨げられ得るため、生成から
     * 手放しまでを別メソッドへ閉じ込める。
     */
    private fun bindAndAbandonHost(
        activity: HostActivity,
        store: SettingsRootStore,
    ): WeakReference<KsSettingsView> {
        val view = KsSettingsView(activity)
        view.bind(store)
        idle()
        return WeakReference(view)
    }

    /**
     * 弱参照が回収されるまで GC を促しながら待つ。
     *
     * `System.gc()` は要求でしかないため、確保と解放を挟んでヒープに圧力をかけながら繰り返す。
     */
    private fun awaitCollected(reference: WeakReference<*>): Boolean {
        repeat(GC_ATTEMPTS) {
            if (reference.get() == null) return true
            val pressure = ArrayList<ByteArray>()
            repeat(GC_PRESSURE_BLOCKS) { pressure.add(ByteArray(GC_PRESSURE_BYTES)) }
            pressure.clear()
            System.gc()
            System.runFinalization()
            Thread.sleep(GC_PAUSE_MILLIS)
        }
        return reference.get() == null
    }

    private companion object {
        const val GC_ATTEMPTS = 20
        const val GC_PRESSURE_BLOCKS = 16
        const val GC_PRESSURE_BYTES = 1 shl 20
        const val GC_PAUSE_MILLIS = 10L
    }
}
