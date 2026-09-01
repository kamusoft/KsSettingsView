package jp.kamusoft.kssettingsview.bridge

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.kamusoft.kssettingsview.ui.Theme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 輸送 DTO の Theme が Store の `applyTheme` へ変換されることを検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsBridgeThemeTest {

    private var attachment: KsBridgeTestHost.Attachment? = null

    @After
    fun tearDown() {
        attachment?.close()
        attachment = null
    }

    /** 輸送 DTO の各項目が Native の Theme へ 1:1 で写される。 */
    @Test
    fun `setTheme の輸送値が Theme へ変換される`() {
        val fixture = KsBridgeFixture.standard()

        val theme = KsBridgeTheme().apply {
            backgroundColor = OPAQUE_GREEN
            cellTitleColor = OPAQUE_RED
            rowHeight = 56
            hasUnevenRows = false
            cellTitleFont = KsBridgeFont(familyName = null, pointSize = 21.0, isBold = true, isItalic = false)
            cellIconSize = 32.0
        }
        fixture.bridge.setTheme(theme)

        val applied = fixture.bridge.store.theme.value
        assertEquals(Color(OPAQUE_GREEN), applied.backgroundColor)
        assertEquals(Color(OPAQUE_RED), applied.cellTitleColor)
        assertEquals(56, applied.rowHeight)
        assertFalse(applied.hasUnevenRows)
        assertEquals(21.0.sp, applied.cellTitleFont?.fontSize)
        assertEquals(32.0.dp, applied.cellIconSize)
    }

    /** 未指定 (null) の項目は Theme 側の未指定として扱われる。 */
    @Test
    fun `setTheme の未指定項目は Theme 側の未指定になる`() {
        val fixture = KsBridgeFixture.standard()

        fixture.bridge.setTheme(KsBridgeTheme())

        val applied = fixture.bridge.store.theme.value
        assertEquals("全項目未指定の DTO は既定 Theme と等価", Theme(), applied)
        assertNull(applied.cellTitleColor)
        assertNull(applied.cellTitleFont)
    }

    /** placeholder の Theme 段の色が Native の Theme へ写される。 */
    @Test
    fun `setTheme の cellPlaceholderColor が Theme へ変換される`() {
        val fixture = KsBridgeFixture.standard()

        fixture.bridge.setTheme(KsBridgeTheme().apply { cellPlaceholderColor = OPAQUE_RED })

        assertEquals(Color(OPAQUE_RED), fixture.bridge.store.theme.value.cellPlaceholderColor)
    }

    /** placeholder の Theme 段の未指定は Native 側の未指定になる。 */
    @Test
    fun `setTheme の cellPlaceholderColor 未指定は Theme 側の未指定になる`() {
        val fixture = KsBridgeFixture.standard()

        fixture.bridge.setTheme(KsBridgeTheme())

        assertNull(fixture.bridge.store.theme.value.cellPlaceholderColor)
    }

    /** 余白の論理 4 成分は方向対応型へ組み立てられ、残る 3 属性もそのまま写される。 */
    @Test
    fun `Section 装飾の輸送値が Theme へ変換される`() {
        val fixture = KsBridgeFixture.standard()

        val theme = KsBridgeTheme().apply {
            sectionMarginTop = 12.0
            sectionMarginLeading = 24.0
            sectionMarginBottom = 4.0
            sectionMarginTrailing = 6.0
            sectionCornerRadius = 18.0
            sectionBorderWidth = 2.0
            sectionBorderColor = OPAQUE_GREEN
        }
        fixture.bridge.setTheme(theme)

        val applied = fixture.bridge.store.theme.value
        val margin = applied.sectionMargin ?: error("sectionMargin が解決されていない")
        assertEquals(12.0.dp, margin.calculateTopPadding())
        assertEquals(24.0.dp, margin.calculateStartPadding(LayoutDirection.Ltr))
        assertEquals(4.0.dp, margin.calculateBottomPadding())
        assertEquals(6.0.dp, margin.calculateEndPadding(LayoutDirection.Ltr))
        assertEquals(18.0.dp, applied.sectionCornerRadius)
        assertEquals(2.0.dp, applied.sectionBorderWidth)
        assertEquals(Color(OPAQUE_GREEN), applied.sectionBorderColor)
    }

    /** Section 装飾の未指定 (null) は Theme 側の未指定として扱われる。 */
    @Test
    fun `Section 装飾の未指定項目は Theme 側の未指定になる`() {
        val fixture = KsBridgeFixture.standard()

        fixture.bridge.setTheme(KsBridgeTheme())

        val applied = fixture.bridge.store.theme.value
        assertNull(applied.sectionMargin)
        assertNull(applied.sectionCornerRadius)
        assertNull(applied.sectionBorderWidth)
        assertNull(applied.sectionBorderColor)
    }

    /** 余白の 4 成分は全体で 1 つの指定であり、1 つでも欠けると余白全体が未指定になる。 */
    @Test
    fun `部分 null の margin は全体が未指定になる`() {
        val fixture = KsBridgeFixture.standard()

        val theme = KsBridgeTheme().apply {
            sectionMarginTop = 12.0
            sectionMarginLeading = 24.0
            sectionMarginBottom = 4.0
            sectionMarginTrailing = null
            sectionCornerRadius = 18.0
        }
        fixture.bridge.setTheme(theme)

        val applied = fixture.bridge.store.theme.value
        assertNull("trailing が欠けたら余白全体が未指定", applied.sectionMargin)
        assertEquals("他の属性は影響を受けない", 18.0.dp, applied.sectionCornerRadius)
    }

    /** 負値・非有限の装飾値は検証されず、そのままの値で Theme へ届く。 */
    @Test
    fun `負値と非有限の Section 装飾は素通しされる`() {
        val fixture = KsBridgeFixture.standard()

        val theme = KsBridgeTheme().apply {
            sectionMarginTop = Double.NaN
            sectionMarginLeading = -8.0
            sectionMarginBottom = Double.POSITIVE_INFINITY
            sectionMarginTrailing = Double.NEGATIVE_INFINITY
            sectionCornerRadius = -4.0
            sectionBorderWidth = Double.NaN
        }
        fixture.bridge.setTheme(theme)

        val applied = fixture.bridge.store.theme.value
        val margin = applied.sectionMargin ?: error("sectionMargin が解決されていない")
        assertTrue("NaN の上成分がそのまま届く", margin.calculateTopPadding().value.isNaN())
        assertEquals(-8.0.dp, margin.calculateStartPadding(LayoutDirection.Ltr))
        assertEquals(Float.POSITIVE_INFINITY, margin.calculateBottomPadding().value)
        assertEquals(
            Float.NEGATIVE_INFINITY,
            margin.calculateEndPadding(LayoutDirection.Ltr).value,
        )
        assertEquals(-4.0.dp, applied.sectionCornerRadius)
        assertTrue(
            "NaN のボーダー幅がそのまま届く",
            applied.sectionBorderWidth?.value?.isNaN() == true,
        )
    }

    /** 負値・非有限の装飾値を持つ Theme でも、描画は例外なく 0 として行われる。 */
    @Test
    fun `負値と非有限の Section 装飾でも描画時に 0 へ正規化される`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        fixture.bridge.setStyle(MODERN_ORDINAL)
        KsBridgeTestHost.pump(host)

        fixture.bridge.setTheme(
            KsBridgeTheme().apply {
                sectionMarginTop = Double.NaN
                sectionMarginLeading = -8.0
                sectionMarginBottom = Double.POSITIVE_INFINITY
                sectionMarginTrailing = Double.NEGATIVE_INFINITY
                sectionCornerRadius = Double.NaN
                sectionBorderWidth = -2.0
            },
        )
        KsBridgeTestHost.pump(host)

        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(host))
        assertEquals(
            "不正な余白は 0 として描画される",
            0,
            host.recyclerView.findViewHolderForAdapterPosition(CELL_A_POSITION)?.itemView?.left,
        )
    }

    /** Theme 変更で表示属性が再評価され、設定ツリーの構造と identity は変化しない。 */
    @Test
    fun `setTheme で構造と identity は変化しない`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val observer = KsBridgeAdapterRecorder.attach(host)
        val cellRowBefore = host.recyclerView.findViewHolderForAdapterPosition(1)?.itemView

        fixture.bridge.setTheme(KsBridgeTheme().apply { cellTitleColor = OPAQUE_GREEN })
        KsBridgeTestHost.pump(host)

        assertEquals("構造変更は発生しない", 0, observer.structuralCount)
        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(host))
        assertEquals(
            "行の identity は変化しない",
            cellRowBefore,
            host.recyclerView.findViewHolderForAdapterPosition(1)?.itemView,
        )
        assertEquals(
            "表示属性が再評価される",
            Color(OPAQUE_GREEN).toArgb(),
            titleColorOf(host, position = 1),
        )
        observer.detach(host)
    }

    /** 同値の Theme を再指定しても Theme 更新は通知されない。 */
    @Test
    fun `同値 Theme での setTheme 再呼び出しは通知されない`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val observer = KsBridgeAdapterRecorder.attach(host)

        fixture.bridge.setTheme(KsBridgeTheme().apply { cellTitleColor = OPAQUE_GREEN })
        KsBridgeTestHost.pump(host)
        val afterFirst = observer.themeChangeCount
        assertEquals("初回の Theme 適用は通知される", 5, afterFirst)

        fixture.bridge.setTheme(KsBridgeTheme().apply { cellTitleColor = OPAQUE_GREEN })
        KsBridgeTestHost.pump(host)

        assertEquals("同値 Theme は再通知されない", afterFirst, observer.themeChangeCount)
        observer.detach(host)
    }

    /** 指定 position の Cell 行に描画されたタイトルの文字色を返す。 */
    private fun titleColorOf(attachment: KsBridgeTestHost.Attachment, position: Int): Int {
        val itemView = attachment.recyclerView.findViewHolderForAdapterPosition(position)?.itemView
            ?: error("position $position の行が実描画されていない")
        return firstTextView(itemView)?.currentTextColor ?: error("タイトルの TextView が見つからない")
    }

    private fun firstTextView(view: View): TextView? = when (view) {
        is TextView -> view
        is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { firstTextView(view.getChildAt(it)) }
        else -> null
    }

    private companion object {
        /** 不透明な緑（ARGB）を表す輸送値。 */
        const val OPAQUE_GREEN: Int = 0xFF00FF00.toInt()

        /** 不透明な赤（ARGB）を表す輸送値。 */
        const val OPAQUE_RED: Int = 0xFFFF0000.toInt()

        /** 標準構成で Cell "A" が並ぶ位置（0 は Section header "S1"）。 */
        const val CELL_A_POSITION: Int = 1

        /** Modern を表す見た目スタイルの序数。 */
        const val MODERN_ORDINAL: Int = 1
    }
}
