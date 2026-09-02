package jp.kamusoft.kssettingsview.bridge

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import jp.kamusoft.kssettingsview.core.SectionAccessory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * interop 境界を越えて渡した `View` が accessory として表示されるまでを、実描画で検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsBridgeAccessoryViewTest {

    private var attachment: KsBridgeTestHost.Attachment? = null

    @After
    fun tearDown() {
        attachment?.close()
        attachment = null
    }

    /**
     * 計測高さを後から変えられる accessory の中身。
     *
     * [contentHeight] の更新で、中身が自分の計測結果を変えた状態を作る。
     */
    private class ProbeView(context: Context, var contentHeight: Int) : View(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            setMeasuredDimension(resolveSize(0, widthMeasureSpec), contentHeight)
        }
    }

    /** accessory の中身を生成する Context。 */
    private val appContext: Context
        get() = ApplicationProvider.getApplicationContext()

    /** 指定した計測高さの中身を生成する。 */
    private fun probe(height: Int): ProbeView = ProbeView(appContext, height)

    /** 指定行の itemView を返す（未描画なら `null`）。 */
    private fun itemViewAt(host: KsBridgeTestHost.Attachment, position: Int): View? =
        host.recyclerView.findViewHolderForAdapterPosition(position)?.itemView

    /** 指定行の container 配下に描画されている accessory の中身を返す。 */
    private fun hostedViewAt(host: KsBridgeTestHost.Attachment, position: Int): View? =
        (itemViewAt(host, position) as? ViewGroup)?.getChildAt(0)

    // MARK: - updateAccessoryView

    /** Section header へ渡した View が実描画される。 */
    @Test
    fun `updateAccessoryView の section header が表示される`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val probe = probe(40)

        fixture.bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = fixture.section1.sectionID,
            view = probe,
        )
        KsBridgeTestHost.pump(host)

        assertSame(probe, hostedViewAt(host, 0))
        assertEquals(listOf("", "A", "B", "S2", "C"), KsBridgeTestHost.renderedRows(host))
    }

    /** Section footer へ渡した View が実描画される。 */
    @Test
    fun `updateAccessoryView の section footer が表示される`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val probe = probe(40)

        fixture.bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.SectionFooter,
            sectionID = fixture.section1.sectionID,
            view = probe,
        )
        KsBridgeTestHost.pump(host)

        // S1 / A / B の後ろに footer 行が挿入される。
        assertSame(probe, hostedViewAt(host, 3))
    }

    /** Root header / footer へ渡した View が実描画される。 */
    @Test
    fun `updateAccessoryView の root 対象が表示される`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val headerProbe = probe(40)
        val footerProbe = probe(40)

        fixture.bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.RootHeader,
            sectionID = null,
            view = headerProbe,
        )
        fixture.bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.RootFooter,
            sectionID = null,
            view = footerProbe,
        )
        KsBridgeTestHost.pump(host)

        val lastPosition = host.recyclerView.adapter!!.itemCount - 1
        assertSame(headerProbe, hostedViewAt(host, 0))
        assertSame(footerProbe, hostedViewAt(host, lastPosition))
    }

    /** `null` を渡すと view accessory が解除され、accessory 未指定と同じ表示に戻る。 */
    @Test
    fun `updateAccessoryView の null で解除される`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val probe = probe(40)

        fixture.bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = fixture.section1.sectionID,
            view = probe,
        )
        KsBridgeTestHost.pump(host)
        assertSame(probe, hostedViewAt(host, 0))

        fixture.bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = fixture.section1.sectionID,
            view = null,
        )
        KsBridgeTestHost.pump(host)

        assertEquals(listOf("A", "B", "S2", "C"), KsBridgeTestHost.renderedRows(host))
        assertNull(fixture.bridge.store.state.value.sections.first().header)
    }

    /** Bridge が採番していない canonical UUID への `updateAccessoryView` は、状態も表示も変えない。 */
    @Test
    fun `updateAccessoryView の未使用 sectionID は no-op になる`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val unusedID = KsBridgeFixture.unusedIdentifier()

        fixture.bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = unusedID,
            view = probe(40),
        )
        fixture.bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.SectionFooter,
            sectionID = unusedID,
            view = probe(40),
        )
        KsBridgeTestHost.pump(host)

        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(host))
        assertEquals(
            listOf<SectionAccessory?>(SectionAccessory.Text("S1"), SectionAccessory.Text("S2")),
            fixture.bridge.store.state.value.sections.map { it.header },
        )

        fixture.bridge.replaceCell(fixture.cellA.cellID, KsBridgeLabelCell(title = "A2"))
        KsBridgeTestHost.pump(host)

        assertEquals(
            "後続操作が表示へ届く（Host の Diff 購読が生きている）",
            listOf("S1", "A2", "B", "S2", "C"),
            KsBridgeTestHost.renderedRows(host),
        )
    }

    /** canonical UUID として解釈できない sectionID は Bridge の入口で弾かれる。 */
    @Test
    fun `updateAccessoryView の非 canonical sectionID は Store へ渡らない`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        fixture.bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = KsBridgeFixture.UNKNOWN_IDENTIFIER,
            view = probe(40),
        )
        KsBridgeTestHost.pump(host)

        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(host))
    }

    /** 破棄済みの Bridge では `updateAccessoryView` が表示を変えない。 */
    @Test
    fun `updateAccessoryView は破棄後に no-op になる`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        fixture.bridge.dispose()
        fixture.bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = fixture.section1.sectionID,
            view = probe(40),
        )
        KsBridgeTestHost.pump(host)

        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(host))
    }

    // MARK: - KsBridgeSection の headerView / footerView

    /** `setRoot` の構築経路で `headerView` / `footerView` が輸送される。 */
    @Test
    fun `setRoot で view accessory 付き Section が表示される`() {
        val bridge = KsSettingsBridge()
        val builder = KsBridgeRootBuilder()
        val section = KsBridgeSection()
        val headerProbe = probe(40)
        val footerProbe = probe(40)
        section.headerView = headerProbe
        section.footerView = footerProbe
        section.addCell(KsBridgeLabelCell(title = "A"))
        builder.addSection(section)
        bridge.setRoot(builder)

        val host = KsBridgeTestHost.attach(bridge).also { attachment = it }

        assertSame(headerProbe, hostedViewAt(host, 0))
        assertSame(footerProbe, hostedViewAt(host, 2))
    }

    /** `replaceSection` の構築経路でも `headerView` が輸送される。 */
    @Test
    fun `replaceSection で view accessory が輸送される`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val probe = probe(40)

        val replacement = KsBridgeSection()
        replacement.headerView = probe
        replacement.addCell(KsBridgeLabelCell(title = "A"))
        fixture.bridge.replaceSection(fixture.section1.sectionID, replacement)
        KsBridgeTestHost.pump(host)

        assertSame(probe, hostedViewAt(host, 0))
        assertEquals(listOf("", "A", "S2", "C"), KsBridgeTestHost.renderedRows(host))
    }

    /** text と View の両方を指定した Section では View が表示される。 */
    @Test
    fun `text と view の両指定は view が優先される`() {
        val bridge = KsSettingsBridge()
        val builder = KsBridgeRootBuilder()
        val section = KsBridgeSection(headerText = "TEXT-H", footerText = "TEXT-F")
        val headerProbe = probe(40)
        section.headerView = headerProbe
        section.addCell(KsBridgeLabelCell(title = "A"))
        builder.addSection(section)
        bridge.setRoot(builder)

        val host = KsBridgeTestHost.attach(bridge).also { attachment = it }

        assertSame(headerProbe, hostedViewAt(host, 0))
        assertEquals(
            "view 未指定の footer は text がそのまま表示される",
            listOf("", "A", "TEXT-F"),
            KsBridgeTestHost.renderedRows(host),
        )
    }

    // MARK: - 再バインド安全性

    /** 画面外へ出て戻る（accessory の再バインドが起きる）間、同一 View が例外なく再表示される。 */
    @Test
    fun `リサイクルを挟んだ再表示が失敗しない`() {
        val bridge = KsSettingsBridge()
        val builder = KsBridgeRootBuilder()
        val sectionIDs = (0 until 12).map { sectionIndex ->
            val section = builder.addSection(headerText = "S$sectionIndex", footerText = null)
            repeat(5) { cellIndex ->
                builder.addLabelCell(
                    KsBridgeLabelCell(title = "C$sectionIndex-$cellIndex"),
                    section.sectionID,
                )
            }
            section.sectionID
        }
        bridge.setRoot(builder)

        val host = KsBridgeTestHost.attach(bridge).also { attachment = it }
        val probe = probe(40)
        bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = sectionIDs[0],
            view = probe,
        )
        KsBridgeTestHost.pump(host)
        assertSame(probe, hostedViewAt(host, 0))

        val recyclerView = host.recyclerView
        recyclerView.scrollToPosition(recyclerView.adapter!!.itemCount - 1)
        KsBridgeTestHost.pump(host)
        assertNull("前提: 先頭 header が画面外へ出ていない", itemViewAt(host, 0))

        recyclerView.scrollToPosition(0)
        KsBridgeTestHost.pump(host)

        assertSame(probe, hostedViewAt(host, 0))
    }

    /** Host を作り直しても、同一 View が新しい Host で例外なく再表示される。 */
    @Test
    fun `Host 作り直しでも同一 view が再表示される`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val probe = probe(40)

        fixture.bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = fixture.section1.sectionID,
            view = probe,
        )
        KsBridgeTestHost.pump(host)
        assertSame(probe, hostedViewAt(host, 0))

        fixture.bridge.releaseHost()
        host.removeHost()
        val rebuilt = KsBridgeTestHost.attach(fixture.bridge, host.controller)

        assertSame(probe, hostedViewAt(rebuilt, 0))
    }

    // MARK: - 再計測要求

    /** 中身が計測結果を変えたあとの再計測要求で、accessory 行の高さが追従する。 */
    @Test
    fun `invalidateAccessoryMeasurement で section header の高さが追従する`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val probe = probe(40)

        fixture.bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = fixture.section1.sectionID,
            view = probe,
        )
        KsBridgeTestHost.pump(host)
        assertEquals(40, itemViewAt(host, 0)?.height)

        probe.contentHeight = 100
        fixture.bridge.invalidateAccessoryMeasurement(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = fixture.section1.sectionID,
        )
        KsBridgeTestHost.pump(host)

        assertEquals(100, itemViewAt(host, 0)?.height)
    }

    /** Section footer も同じ経路で高さが追従する。 */
    @Test
    fun `invalidateAccessoryMeasurement で section footer の高さが追従する`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val probe = probe(30)

        fixture.bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.SectionFooter,
            sectionID = fixture.section1.sectionID,
            view = probe,
        )
        KsBridgeTestHost.pump(host)
        assertEquals(30, itemViewAt(host, 3)?.height)

        probe.contentHeight = 90
        fixture.bridge.invalidateAccessoryMeasurement(
            target = KsBridgeAccessoryTarget.SectionFooter,
            sectionID = fixture.section1.sectionID,
        )
        KsBridgeTestHost.pump(host)

        assertEquals(90, itemViewAt(host, 3)?.height)
    }

    /** Root header も同じ経路で高さが追従する。 */
    @Test
    fun `invalidateAccessoryMeasurement で root header の高さが追従する`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val probe = probe(50)

        fixture.bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.RootHeader,
            sectionID = null,
            view = probe,
        )
        KsBridgeTestHost.pump(host)
        assertEquals(50, itemViewAt(host, 0)?.height)

        probe.contentHeight = 120
        fixture.bridge.invalidateAccessoryMeasurement(
            target = KsBridgeAccessoryTarget.RootHeader,
            sectionID = null,
        )
        KsBridgeTestHost.pump(host)

        assertEquals(120, itemViewAt(host, 0)?.height)
    }

    /** 未知の sectionID への再計測要求は、表示を変えずに no-op になる。 */
    @Test
    fun `invalidateAccessoryMeasurement の未使用 sectionID は no-op になる`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val probe = probe(40)

        fixture.bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = fixture.section1.sectionID,
            view = probe,
        )
        KsBridgeTestHost.pump(host)

        probe.contentHeight = 100
        fixture.bridge.invalidateAccessoryMeasurement(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = KsBridgeFixture.unusedIdentifier(),
        )
        KsBridgeTestHost.pump(host)

        assertEquals(
            "別の対象への要求で高さが変わってはいけない",
            40,
            itemViewAt(host, 0)?.height,
        )
    }

    /** 固定高さの Section header は、再計測要求でも高さが変わらない。 */
    @Test
    fun `固定高さの header は再計測要求で変化しない`() {
        val bridge = KsSettingsBridge()
        val builder = KsBridgeRootBuilder()
        val section = builder.addSection(headerText = "S1", footerText = null)
        section.headerHeight = 60.0
        builder.addLabelCell(KsBridgeLabelCell(title = "A"), section.sectionID)
        bridge.setRoot(builder)

        val host = KsBridgeTestHost.attach(bridge).also { attachment = it }
        val probe = probe(20)
        bridge.updateAccessoryView(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = section.sectionID,
            view = probe,
        )
        KsBridgeTestHost.pump(host)

        val fixedHeight = (60.0 * appContext.resources.displayMetrics.density).toInt()
        assertEquals(fixedHeight, itemViewAt(host, 0)?.height)

        probe.contentHeight = 200
        bridge.invalidateAccessoryMeasurement(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = section.sectionID,
        )
        KsBridgeTestHost.pump(host)

        assertEquals(fixedHeight, itemViewAt(host, 0)?.height)
    }
}
