package jp.kamusoft.kssettingsview.bridge

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.View
import androidx.compose.ui.graphics.Color
import com.google.android.material.R as MaterialR
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.lang.ref.WeakReference

/**
 * Bridge の破棄と Host の単独解放を検証する。
 *
 * 破棄が冪等で破棄後の操作が no-op になること、`releaseHost` が Store を維持したまま Host だけを
 * 手放し、旧 Host を Store からも Bridge からも切り離すことを扱う。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsBridgeLifecycleTest {

    private var attachment: KsBridgeTestHost.Attachment? = null

    @After
    fun tearDown() {
        attachment?.close()
        attachment = null
    }

    /** 破棄 API を繰り返し呼んでもエラーやクラッシュにならない。 */
    @Test
    fun `dispose は冪等`() {
        val fixture = KsBridgeFixture.standard()

        fixture.bridge.dispose()
        fixture.bridge.dispose()
        fixture.bridge.dispose()

        assertTrue(fixture.bridge.isDisposed)
    }

    /** 破棄後の内容更新と Theme 適用は no-op で、表示中の Host も変化しない。 */
    @Test
    fun `破棄後の replaceCell と setTheme は表示を変えない`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val themeBefore = fixture.bridge.store.theme.value
        val observer = KsBridgeAdapterRecorder.attach(host)

        fixture.bridge.dispose()
        fixture.bridge.replaceCell(fixture.cellA.cellID, KsBridgeLabelCell(title = "A-updated"))
        val theme = KsBridgeTheme().apply { backgroundColor = OPAQUE_GREEN }
        fixture.bridge.setTheme(theme)
        KsBridgeTestHost.pump(host)

        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(host))
        assertEquals(0, observer.totalCount)
        assertEquals(themeBefore, fixture.bridge.store.theme.value)
        observer.detach(host)
    }

    /** 破棄後の構造操作と root 全置換も状態を変えない。 */
    @Test
    fun `破棄後の setRoot と構造操作は状態を変えない`() {
        val fixture = KsBridgeFixture.standard()
        val before = fixture.bridge.store.state.value

        fixture.bridge.dispose()
        fixture.bridge.setRoot(KsBridgeRootBuilder())
        assertNull(fixture.bridge.insertSection(KsBridgeSection(headerText = "N", footerText = null), index = 0))
        fixture.bridge.removeSection(fixture.section1.sectionID)
        fixture.bridge.moveSection(from = 0, to = 1)
        fixture.bridge.replaceSection(
            fixture.section1.sectionID,
            KsBridgeSection(headerText = "N", footerText = null),
        )
        assertNull(fixture.bridge.insertCell(KsBridgeLabelCell(title = "N"), fixture.section1.sectionID, index = 0))
        fixture.bridge.removeCell(fixture.cellA.cellID)
        fixture.bridge.moveCell(fixture.cellA.cellID, index = 0)
        fixture.bridge.replaceCells(
            listOf(KsBridgeCellUpdate(fixture.cellA.cellID, KsBridgeLabelCell(title = "N"))),
        )
        fixture.bridge.updateAccessory(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = fixture.section1.sectionID,
            text = "N",
        )

        assertEquals(before, fixture.bridge.store.state.value)
    }

    /** 解放後に Store を更新しても、view 階層に残置した旧 Host の表示は変化しない。 */
    @Test
    fun `解放後の Store 更新は旧 handle に反映されない`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        // 対照: 解放前の更新は購読経由で旧 Host に届く（購読が実在することの担保）。
        fixture.bridge.replaceCell(fixture.cellB.cellID, KsBridgeLabelCell(title = "B-updated"))
        KsBridgeTestHost.pump(host)
        val rowsBefore = KsBridgeTestHost.renderedRows(host)
        assertEquals(listOf("S1", "A", "B-updated", "S2", "C"), rowsBefore)
        val backgroundBefore = (host.recyclerView.background as ColorDrawable).color

        fixture.bridge.releaseHost()
        fixture.bridge.replaceCell(fixture.cellA.cellID, KsBridgeLabelCell(title = "A-updated"))
        fixture.bridge.setTheme(KsBridgeTheme().apply { backgroundColor = OPAQUE_GREEN })
        KsBridgeTestHost.pump(host)

        assertEquals("解放後の内容更新は旧 Host へ届かない", rowsBefore, KsBridgeTestHost.renderedRows(host))
        assertEquals(
            "解放後の Theme 更新も旧 Host へ届かない",
            backgroundBefore,
            (host.recyclerView.background as ColorDrawable).color,
        )
        assertEquals(
            "Store 自体は更新されている",
            Color(OPAQUE_GREEN),
            fixture.bridge.store.theme.value.backgroundColor,
        )
    }

    /** 解放 API を繰り返し呼んでもエラーにならず、Store の設定ツリーと Theme は維持される。 */
    @Test
    fun `releaseHost は冪等で Store を維持する`() {
        val fixture = KsBridgeFixture.standard()
        fixture.bridge.setTheme(KsBridgeTheme().apply { backgroundColor = OPAQUE_GREEN })
        val first = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        fixture.bridge.releaseHost()
        fixture.bridge.releaseHost()
        fixture.bridge.releaseHost()
        first.removeHost()

        val restored = KsBridgeTestHost.attach(fixture.bridge, first.controller).also { attachment = it }
        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(restored))
        assertEquals(
            OPAQUE_GREEN,
            (restored.recyclerView.background as ColorDrawable).color,
        )
    }

    /** Host を生成していない Bridge への解放呼び出しは no-op で、その後の生成に影響しない。 */
    @Test
    fun `Host 未生成での releaseHost は no-op`() {
        val fixture = KsBridgeFixture.standard()

        fixture.bridge.releaseHost()

        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(host))
    }

    /** 破棄済みの Bridge への解放呼び出しは no-op で、Host 生成は引き続き null を返す。 */
    @Test
    fun `dispose 後の releaseHost は no-op`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        fixture.bridge.dispose()

        fixture.bridge.releaseHost()

        assertNull(fixture.bridge.makeHostView(host.controller.get()))
    }

    /**
     * 解放後、Bridge は旧 Host とその `Context` への参照を保持しない。
     *
     * Host の生成には Activity をそのまま渡さず、この検証だけが参照する `ContextThemeWrapper` を
     * 被せる。Activity 自体は Robolectric の `ActivityController` からも参照され続けるため、
     * 「Bridge が生成時の `Context` を手放したか」を Activity の回収可否では判定できない。
     *
     * 外部参照の破棄（view 階層からの取り外しとローカル参照の解放）は呼び出し側の責務なので
     * 検証側で行い、その後に残る参照が Bridge 側にないことを回収可否で見る。
     */
    @Test
    fun `解放後に旧 Host への参照を保持しない`() {
        val fixture = KsBridgeFixture.standard()
        val controller = Robolectric.buildActivity(KsBridgeTestHost.HostActivity::class.java).setup()
        val refs = try {
            releaseAndWeakenHost(fixture.bridge, controller)
        } finally {
            controller.close()
        }

        assertTrue("旧 Host が回収されない", awaitCollected(refs.first))
        assertTrue("旧 Host の Context が回収されない", awaitCollected(refs.second))
    }

    /**
     * Host を生成・表示して解放し、旧 Host と生成に使った `Context` の弱参照だけを返す。
     *
     * 強参照をローカル変数に残したまま回収を待つと、スタック上の参照で回収が妨げられ得る。
     * 生成から解放までを別のメソッドに閉じ込め、戻り値を弱参照だけにすることでそれを避ける。
     */
    private fun releaseAndWeakenHost(
        bridge: KsSettingsBridge,
        controller: ActivityController<KsBridgeTestHost.HostActivity>,
    ): Pair<WeakReference<View>, WeakReference<Context>> {
        val hostContext = ContextThemeWrapper(
            controller.get(),
            MaterialR.style.Theme_Material3_Light_NoActionBar,
        )
        val attachment = KsBridgeTestHost.attach(bridge, controller, hostContext)
        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(attachment))

        bridge.releaseHost()
        attachment.removeHost()
        shadowOf(Looper.getMainLooper()).idle()

        return WeakReference(attachment.hostView) to WeakReference<Context>(hostContext)
    }

    /** 弱参照が回収されるまで GC を促しながら待つ。 */
    private fun awaitCollected(reference: WeakReference<*>): Boolean {
        repeat(50) {
            if (reference.get() == null) return true
            System.gc()
            System.runFinalization()
            Thread.sleep(10)
        }
        return reference.get() == null
    }

    private companion object {
        /** 不透明な緑（ARGB）を表す輸送値。 */
        const val OPAQUE_GREEN: Int = 0xFF00FF00.toInt()
    }
}
