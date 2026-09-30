package jp.kamusoft.kssettingsview.ui

import android.widget.FrameLayout
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.lang.ref.WeakReference

/**
 * スクロール命令ハンドルが、接続した View Host の解放を妨げないことを検証する。
 *
 * ハンドルは ViewModel が持って画面より長生きしうるため、Host (とそれが持つ Activity の Context) を
 * 強参照してはならない。
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
class ScrollControllerReleaseTest {

    private var activityController: ActivityController<ScrollTestHostActivity>? = null

    @After
    fun tearDown() {
        activityController?.close()
        activityController = null
    }

    @Test
    fun `ハンドルは Host の解放を妨げない`() {
        val ctrl = Robolectric.buildActivity(ScrollTestHostActivity::class.java).setup()
        activityController = ctrl
        val controller = KsScrollController()

        val reference = connectAndAbandonHost(ctrl.get(), controller)
        assertNotNull("前提: 接続中はハンドルが受け口を持つ", controller.currentReceiver)

        assertTrue("手放した Host が回収されない", awaitCollected(reference))
        assertNull("Host が回収されるとハンドルは未接続に戻る", controller.currentReceiver)
        controller.scrollToEnd()
        controller.scrollTo("c1_0")
        idle()
    }

    /**
     * Host を作って表示し、ハンドルを接続して命令を出した後、画面から外して参照を手放し、弱参照だけを返す。
     *
     * 強参照をテストメソッドのローカル変数に残すと、スタック上の参照で回収が妨げられ得るため、生成から
     * 手放しまでを別メソッドへ閉じ込める。
     */
    private fun connectAndAbandonHost(
        activity: ScrollTestHostActivity,
        controller: KsScrollController,
    ): WeakReference<KsSettingsView> {
        val view = KsSettingsView(activity)
        view.bind(SettingsRootStore(initialRoot = scrollTestRoot()))
        activity.container.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, SCROLL_TEST_HOST_HEIGHT))
        awaitRootShown(view)
        view.scrollController = controller
        controller.scrollToEnd(animated = false)
        awaitScrollSettled(view, 1)
        activity.container.removeView(view)
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
            idle()
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
