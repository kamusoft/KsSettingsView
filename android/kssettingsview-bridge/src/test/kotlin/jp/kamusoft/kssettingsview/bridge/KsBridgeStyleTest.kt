package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.ui.KsSettingsView
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

/**
 * 輸送された序数が Native の見た目スタイルへ変換され、Host の世代をまたいで保たれることを検証する。
 *
 * 表示への反映は、指定した Section 余白が Cell 行の水平位置へ現れるかで観察する
 * （Modern は箱の水平余白を入れ、Classic は Section 境界を全幅に保つ）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsBridgeStyleTest {

    private var attachment: KsBridgeTestHost.Attachment? = null

    @After
    fun tearDown() {
        attachment?.close()
        attachment = null
    }

    /** 序数 0 / 1 は Classic / Modern に対応する。 */
    @Test
    fun `序数が style へ変換される`() {
        assertEquals(KsSettingsViewStyle.Classic, KsBridgeStyle.style(0))
        assertEquals(KsSettingsViewStyle.Modern, KsBridgeStyle.style(1))
    }

    /** 定義域外の序数は Classic へ正規化される。 */
    @Test
    fun `定義域外の序数は Classic へ正規化される`() {
        assertEquals(KsSettingsViewStyle.Classic, KsBridgeStyle.style(2))
        assertEquals(KsSettingsViewStyle.Classic, KsBridgeStyle.style(-1))
        assertEquals(KsSettingsViewStyle.Classic, KsBridgeStyle.style(Int.MAX_VALUE))
    }

    /** setStyle は生きている Host の style へ即座に適用され、箱の余白が表示に現れる。 */
    @Test
    fun `setStyle が表示中の Host へ適用される`() {
        val fixture = KsBridgeFixture.standard()
        fixture.bridge.setTheme(marginTheme())
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        assertEquals(KsSettingsViewStyle.Classic, styleOf(host))
        assertEquals("Classic では Section が全幅で並ぶ", 0, firstCellRowLeft(host))

        fixture.bridge.setStyle(1)
        KsBridgeTestHost.pump(host)

        assertEquals(KsSettingsViewStyle.Modern, styleOf(host))
        assertEquals(
            "Modern では指定した水平余白の分だけ行が内側へ寄る",
            expectedMarginPx(host),
            firstCellRowLeft(host),
        )
    }

    /** Classic へ戻す方向の切替も同じ経路で適用される。 */
    @Test
    fun `setStyle で Classic へ戻せる`() {
        val fixture = KsBridgeFixture.standard()
        fixture.bridge.setTheme(marginTheme())
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        fixture.bridge.setStyle(1)
        KsBridgeTestHost.pump(host)

        fixture.bridge.setStyle(0)
        KsBridgeTestHost.pump(host)

        assertEquals(KsSettingsViewStyle.Classic, styleOf(host))
        assertEquals(0, firstCellRowLeft(host))
    }

    /** Host 未生成のときに受けた style は、生成した Host へ適用される。 */
    @Test
    fun `Host 生成前の setStyle が生成時に適用される`() {
        val fixture = KsBridgeFixture.standard()
        fixture.bridge.setTheme(marginTheme())

        fixture.bridge.setStyle(1)
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        assertEquals(KsSettingsViewStyle.Modern, styleOf(host))
        assertEquals(expectedMarginPx(host), firstCellRowLeft(host))
    }

    /** Host を解放して作り直しても style は失われない。 */
    @Test
    fun `Host 再生成をまたいで style が維持される`() {
        val fixture = KsBridgeFixture.standard()
        fixture.bridge.setTheme(marginTheme())
        val first = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        fixture.bridge.setStyle(1)
        KsBridgeTestHost.pump(first)

        fixture.bridge.releaseHost()
        first.removeHost()
        val second = KsBridgeTestHost.attach(fixture.bridge, first.controller).also { attachment = it }

        assertNotSame("解放後は新しい Host が返る", first.hostView, second.hostView)
        assertEquals(KsSettingsViewStyle.Modern, styleOf(second))
        assertEquals(expectedMarginPx(second), firstCellRowLeft(second))
    }

    /** 定義域外の序数を受けた Host は Classic で表示される。 */
    @Test
    fun `定義域外の序数を受けた Host は Classic で表示される`() {
        val fixture = KsBridgeFixture.standard()
        fixture.bridge.setTheme(marginTheme())
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        fixture.bridge.setStyle(1)
        KsBridgeTestHost.pump(host)

        fixture.bridge.setStyle(7)
        KsBridgeTestHost.pump(host)

        assertEquals(KsSettingsViewStyle.Classic, styleOf(host))
        assertEquals(0, firstCellRowLeft(host))
    }

    /** 水平余白だけを明示した Theme。上下は 0 に固定して行の水平位置だけを観察する。 */
    private fun marginTheme(): KsBridgeTheme = KsBridgeTheme().apply {
        sectionMarginTop = 0.0
        sectionMarginLeading = MARGIN_DP
        sectionMarginBottom = 0.0
        sectionMarginTrailing = MARGIN_DP
    }

    /** Host の現在の見た目スタイル。 */
    private fun styleOf(attachment: KsBridgeTestHost.Attachment): KsSettingsViewStyle =
        (attachment.hostView as KsSettingsView).style

    /** 先頭 Section の先頭 Cell 行が実描画された左端 (px)。 */
    private fun firstCellRowLeft(attachment: KsBridgeTestHost.Attachment): Int =
        attachment.recyclerView.findViewHolderForAdapterPosition(CELL_A_POSITION)?.itemView?.left
            ?: error("Cell 行が実描画されていない")

    /** 指定した水平余白を px へ換算した値。 */
    private fun expectedMarginPx(attachment: KsBridgeTestHost.Attachment): Int {
        val density = attachment.controller.get().resources.displayMetrics.density
        return (MARGIN_DP * density).roundToInt()
    }

    private companion object {
        /** 標準構成で Cell "A" が並ぶ位置 (0 は Section header "S1")。 */
        const val CELL_A_POSITION: Int = 1

        /** 検証で指定する Section の水平余白 (dp)。 */
        const val MARGIN_DP: Double = 20.0
    }
}
