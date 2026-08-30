package jp.kamusoft.kssettingsview.bridge

import android.graphics.drawable.ColorDrawable
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Native Host の生成と内部 Store への接続を検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsBridgeHostTest {

    private var attachment: KsBridgeTestHost.Attachment? = null

    @After
    fun tearDown() {
        attachment?.close()
        attachment = null
    }

    /** Host を先に取り付けてから `setRoot` を呼んでも表示へ反映される。 */
    @Test
    fun `Host 生成後の setRoot が表示へ反映される`() {
        val bridge = KsSettingsBridge()
        val host = KsBridgeTestHost.attach(bridge).also { attachment = it }
        assertEquals(emptyList<String>(), KsBridgeTestHost.renderedRows(host))

        val builder = KsBridgeRootBuilder()
        val section = builder.addSection(headerText = "S", footerText = null)
        builder.addLabelCell(KsBridgeLabelCell(title = "A"), section.sectionID)
        bridge.setRoot(builder)
        KsBridgeTestHost.pump(host)

        assertEquals(listOf("S", "A"), KsBridgeTestHost.renderedRows(host))
    }

    /** `setRoot` の後に生成した Host は購読開始前の現在状態から表示を復元する。 */
    @Test
    fun `setRoot 後に生成した Host が現在状態を復元する`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(host))
    }

    /** Bridge は同時に 1 つの Host を持ち、生成 API を繰り返し呼んでも同じ Host を返す。 */
    @Test
    fun `makeHostView は同じ Host を返す`() {
        val bridge = KsSettingsBridge()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val first = bridge.makeHostView(context)
        val second = bridge.makeHostView(context)

        assertNotNull(first)
        assertSame(first, second)
    }

    /** 破棄済みの Bridge は Host を生成しない。 */
    @Test
    fun `破棄後の makeHostView は null を返す`() {
        val bridge = KsSettingsBridge()
        bridge.dispose()

        assertNull(bridge.makeHostView(ApplicationProvider.getApplicationContext()))
    }

    /** 解放後の生成 API は別インスタンスの Host を返し、Store 現在状態から表示を復元する。 */
    @Test
    fun `解放後の再生成は Store 現在状態を復元する`() {
        val fixture = KsBridgeFixture.standard()
        val first = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(first))

        fixture.bridge.releaseHost()
        first.removeHost()
        val second = KsBridgeTestHost.attach(fixture.bridge, first.controller).also { attachment = it }

        assertNotSame("解放後は新しい Host が返る", first.hostView, second.hostView)
        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(second))
    }

    /** Host 不在の間に適用した更新は、再生成した Host の表示に反映される。 */
    @Test
    fun `解放中の更新は再生成時に反映される`() {
        val fixture = KsBridgeFixture.standard()
        val first = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(first))

        fixture.bridge.releaseHost()
        first.removeHost()
        fixture.bridge.replaceCell(fixture.cellA.cellID, KsBridgeLabelCell(title = "A-updated"))
        fixture.bridge.updateAccessory(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = fixture.section1.sectionID,
            text = "S1-updated",
        )
        fixture.bridge.setTheme(KsBridgeTheme().apply { backgroundColor = OPAQUE_GREEN })

        val second = KsBridgeTestHost.attach(fixture.bridge, first.controller).also { attachment = it }

        assertEquals(
            listOf("S1-updated", "A-updated", "B", "S2", "C"),
            KsBridgeTestHost.renderedRows(second),
        )
        assertEquals(
            "解放中に適用した Theme も再生成した Host に反映される",
            OPAQUE_GREEN,
            (second.recyclerView.background as ColorDrawable).color,
        )
    }

    /** 解放後は解放前と別の `Context` で Host を作り直せる。 */
    @Test
    fun `解放後は別の Context で再生成できる`() {
        val fixture = KsBridgeFixture.standard()
        val first = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val firstContext = first.hostView.context

        fixture.bridge.releaseHost()
        first.removeHost()
        val otherContext = ContextThemeWrapper(
            first.controller.get(),
            MaterialR.style.Theme_Material3_Light_NoActionBar,
        )
        val second = KsBridgeTestHost.attach(fixture.bridge, first.controller, otherContext)
            .also { attachment = it }

        assertNotSame("解放前とは別の Context で生成できる", firstContext, second.hostView.context)
        assertSame(otherContext, second.hostView.context)
        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(second))
    }

    private companion object {
        /** 不透明な緑（ARGB）を表す輸送値。 */
        const val OPAQUE_GREEN: Int = 0xFF00FF00.toInt()
    }
}
