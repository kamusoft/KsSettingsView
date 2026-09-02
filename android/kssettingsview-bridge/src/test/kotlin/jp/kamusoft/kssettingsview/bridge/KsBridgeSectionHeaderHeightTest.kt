package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.core.Section
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Section のヘッダ高さの輸送を検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsBridgeSectionHeaderHeightTest {

    /**
     * Native の `Section` が自動高さを表す値。
     *
     * 期待値は導出せず直書きで固定する（Native 既定が変わったときに輸送側の想定と食い違ったことを
     * 検出できるようにするため）。
     */
    @Test
    fun `Native の Section は自動高さを負値で表す`() {
        assertEquals(-1.0, Section(id = "auto").headerHeight, 0.0)
    }

    /** Section DTO の headerHeight の既定は未指定。 */
    @Test
    fun `Section DTO の headerHeight 既定は null`() {
        assertNull(KsBridgeSection(headerText = "S", footerText = null).headerHeight)
    }

    /** headerHeight 未指定の Section は Native 既定の自動高さになる。 */
    @Test
    fun `headerHeight 未指定の Section は Native 既定の自動高さになる`() {
        val bridge = KsBridgeFixture.withCells(listOf(KsBridgeLabelCell(title = "A")))

        assertEquals(
            -1.0,
            bridge.store.state.value.sections[0].headerHeight,
            0.0,
        )
    }

    /** headerHeight を指定すると Native の Section へ適用される。 */
    @Test
    fun `headerHeight を指定すると Native の Section へ適用される`() {
        val bridge = KsSettingsBridge()
        val builder = KsBridgeRootBuilder()
        val section = builder.addSection(headerText = "S", footerText = null)
        section.headerHeight = 60.0
        section.addCell(KsBridgeLabelCell(title = "A"))
        bridge.setRoot(builder)

        assertEquals(60.0, bridge.store.state.value.sections[0].headerHeight, 0.0)
    }

    /** insertSection でも headerHeight が輸送される。 */
    @Test
    fun `insertSection でも headerHeight が輸送される`() {
        val bridge = KsSettingsBridge()
        bridge.setRoot(KsBridgeRootBuilder())
        val inserted = KsBridgeSection(headerText = "S", footerText = null).apply {
            headerHeight = 44.0
        }

        bridge.insertSection(inserted, 0)

        assertEquals(44.0, bridge.store.state.value.sections[0].headerHeight, 0.0)
    }

    /** replaceSection で headerHeight を差し替えられる。 */
    @Test
    fun `replaceSection で headerHeight を差し替えられる`() {
        val bridge = KsSettingsBridge()
        val builder = KsBridgeRootBuilder()
        builder.addSection(headerText = "S", footerText = null).apply { headerHeight = 60.0 }
        bridge.setRoot(builder)
        val sectionID = bridge.store.state.value.sections[0].id

        val replacement = KsBridgeSection(headerText = "S", footerText = null).apply {
            headerHeight = 80.0
        }
        bridge.replaceSection(sectionID, replacement)

        assertEquals(80.0, bridge.store.state.value.sections[0].headerHeight, 0.0)
    }

    /** replaceSection で headerHeight を Native 既定へ戻せる。 */
    @Test
    fun `replaceSection で headerHeight を Native 既定へ戻せる`() {
        val bridge = KsSettingsBridge()
        val builder = KsBridgeRootBuilder()
        builder.addSection(headerText = "S", footerText = null).apply { headerHeight = 60.0 }
        bridge.setRoot(builder)
        val sectionID = bridge.store.state.value.sections[0].id

        bridge.replaceSection(sectionID, KsBridgeSection(headerText = "S", footerText = null))

        assertEquals(
            -1.0,
            bridge.store.state.value.sections[0].headerHeight,
            0.0,
        )
    }
}
