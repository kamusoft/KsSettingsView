package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.Section
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

/**
 * 使い捨ての観測用テスト。
 *
 * CustomCell の builder に「自己計測する View を包んだ `AndroidView`」を埋め込んだとき、その View が
 * 自分でレイアウト要求を出しただけで行の高さが追従するかを実測する。追従しなければ、行を対象にした
 * 一過性の再計測通知が別途要ることになる。
 *
 * 実測が済んだら削除する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CustomCellEmbeddedPlatformViewProbeTest {

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
     * 必要な高さを自分の測定結果として返し、内容が変わったらレイアウト要求を自分から出す ViewGroup。
     *
     * 任意の外部 UI を包んで自分で計測・配置する host view と同じ計測契約を再現する。
     */
    private class ProbeHostView(context: Context, height: Int) : ViewGroup(context) {
        var contentHeight: Int = height
            private set

        /** レイアウト要求を出した回数。 */
        var requestCount: Int = 0
            private set

        /** `onMeasure` が呼ばれた回数。上位が測り直したかの観測点。 */
        var measureCount: Int = 0
            private set

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            measureCount++
            setMeasuredDimension(resolveSize(0, widthMeasureSpec), contentHeight)
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) = Unit

        /** 内容のサイズが変わった状況を作る。 */
        fun setContentHeight(height: Int) {
            contentHeight = height
            requestCount++
            requestLayout()
        }

        /** レイアウト要求を出さずに必要サイズだけを変える (負の対照用)。 */
        fun setContentHeightSilently(height: Int) {
            contentHeight = height
        }
    }

    private class Attachment(
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
        val store = SettingsRootStore(initialRoot = root, initialTheme = Theme(hasUnevenRows = true))
        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.bind(store)
        val attachment = Attachment(view, activity)
        attachment.pump()
        return attachment
    }

    private fun rootWith(host: ProbeHostView): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(
                id = "s1",
                cells = listOf(
                    CustomCell(id = "c1", content = "不変") {
                        AndroidView(factory = { host })
                    },
                ),
            ),
        ),
    )

    /** 埋め込んだ View のレイアウト要求だけで行高さが追従するか。 */
    @Test
    fun `probe 通知なしで行高さが追従するか`() {
        val host = ProbeHostView(ctx, 60)
        val attachment = attach(rootWith(host))

        val before = attachment.rowHeightAt(0)
        val measuresBefore = host.measureCount

        host.setContentHeight(240)
        attachment.pump()

        val after = attachment.rowHeightAt(0)

        // 縮小方向も測る (成長だけ追従する失敗形を検出するため)。
        host.setContentHeight(80)
        attachment.pump()
        val shrunk = attachment.rowHeightAt(0)

        println(
            "[PROBE] android/no-notify: before=$before after=$after shrunk=$shrunk " +
                "measureCount=$measuresBefore->${host.measureCount} requestCount=${host.requestCount}",
        )

        assertEquals("前提: 初期高さが中身の高さになっていない", 60, before)
        assertTrue("行高さが伸びる方向へ追従しなかった (before=$before after=$after)", (after ?: 0) >= 240)
        assertTrue("行高さが縮む方向へ追従しなかった (after=$after shrunk=$shrunk)", (shrunk ?: 0) <= 100)
    }

    /**
     * 負の対照: レイアウト要求を出さずに必要サイズだけ変えても行は追従しないこと。
     *
     * これが追従してしまうなら、上の観測はレイアウト要求の効果ではなく測り直しの副作用になる。
     */
    @Test
    fun `probe 負の対照 レイアウト要求なしでは行高さが追従しない`() {
        val host = ProbeHostView(ctx, 60)
        val attachment = attach(rootWith(host))

        val before = attachment.rowHeightAt(0)
        val measuresBefore = host.measureCount
        host.setContentHeightSilently(240)
        attachment.pump()
        val after = attachment.rowHeightAt(0)
        println(
            "[PROBE] android/silent: before=$before after=$after " +
                "measureCount=$measuresBefore->${host.measureCount}",
        )

        assertEquals("レイアウト要求なしでも追従している = 追従の原因が特定できない", before, after)
    }
}
