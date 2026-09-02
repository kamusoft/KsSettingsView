package jp.kamusoft.kssettingsview.bridge

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Builder による root 構築と `setRoot`、Bridge 採番 ID による後続操作を検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsBridgeRootTest {

    private var attachment: KsBridgeTestHost.Attachment? = null

    @After
    fun tearDown() {
        attachment?.close()
        attachment = null
    }

    /** Builder で構築した Section と LabelCell が Native の設定 list に表示される。 */
    @Test
    fun `setRoot で構築どおりの Section と LabelCell が表示される`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(host))
    }

    /** `setRoot` の再呼び出しで表示が新しい root へ全置換される。 */
    @Test
    fun `setRoot の再呼び出しで表示が全置換される`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(host))

        val builder = KsBridgeRootBuilder()
        val section = builder.addSection(headerText = "NEW", footerText = null)
        builder.addLabelCell(KsBridgeLabelCell(title = "X"), section.sectionID)
        fixture.bridge.setRoot(builder)
        KsBridgeTestHost.pump(host)

        assertEquals(listOf("NEW", "X"), KsBridgeTestHost.renderedRows(host))
    }

    /** Builder が返した cellID をそのまま更新 API へ渡すと対象 Cell の内容が更新される。 */
    @Test
    fun `採番された cellID で replaceCell が反映される`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        fixture.bridge.replaceCell(fixture.cellB.cellID, KsBridgeLabelCell(title = "B-updated"))
        KsBridgeTestHost.pump(host)

        assertEquals(listOf("S1", "A", "B-updated", "S2", "C"), KsBridgeTestHost.renderedRows(host))
    }

    /** Builder は自分が保持していない sectionID への Cell 追加を no-op として扱う。 */
    @Test
    fun `Builder の未知 sectionID への addLabelCell は null を返し追加されない`() {
        val builder = KsBridgeRootBuilder()
        val section = builder.addSection(headerText = "S", footerText = null)

        val result = builder.addLabelCell(KsBridgeLabelCell(title = "A"), KsBridgeFixture.unusedIdentifier())

        assertNull(result)
        assertEquals(0, section.cells.size)
    }

    /** Bridge が採番していない文字列を ID に渡した Cell 操作は状態も表示も変えない。 */
    @Test
    fun `不正な ID の removeCell は no-op`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        fixture.bridge.removeCell(KsBridgeFixture.UNKNOWN_IDENTIFIER)
        fixture.bridge.removeCell(KsBridgeFixture.unusedIdentifier())
        KsBridgeTestHost.pump(host)

        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(host))
        assertEquals(3, fixture.bridge.store.state.value.sections.sumOf { it.cells.size })
    }
}
