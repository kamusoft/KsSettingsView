package jp.kamusoft.kssettingsview.ui

import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `SectionAccessory` の Text / View（Compose / AndroidView）描画を検証。
 *
 * Section の header / footer が種別ごとの ViewHolder で描画され、
 * Theme / Section 側の高さ・フォント指定が反映されることを保証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SectionAccessoryRenderingTest {

    /** テスト用に空の @Composable を提供する補助関数。 */
    @Composable
    private fun DummyComposable() {
        // 実際の描画ノードは不要、Compose ランタイムが解釈できれば良い
    }

    @Test
    fun `SectionAccessory Text のヘッダ ViewHolder は TextView に文字列を描画する`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionTextAccessoryViewHolder.create(parent)
        holder.bind(
            accessory = SectionAccessory.Text("一般"),
            theme = Theme(),
            isHeader = true,
        )
        val tv = holder.itemView as TextView
        assertEquals("一般", tv.text.toString())
    }

    @Test
    fun `SectionAccessory View Compose backing は ComposeView を addView する`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionAnyViewAccessoryViewHolder.create(parent)
        holder.bind(
            accessory = SectionAccessory.View(
                view = KsAnyView.Compose { DummyComposable() },
            ),
            theme = Theme(),
            isHeader = true,
        )
        val container = holder.itemView as FrameLayout
        assertEquals("ComposeView が 1 つ addView される", 1, container.childCount)
        assertTrue(container.getChildAt(0) is ComposeView)
    }

    @Test
    fun `SectionAccessory View AndroidView backing は factory 生成 View を addView する`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionAnyViewAccessoryViewHolder.create(parent)
        holder.bind(
            accessory = SectionAccessory.View(
                view = KsAnyView.AndroidView { c -> LinearLayout(c) },
            ),
            theme = Theme(),
            isHeader = true,
        )
        val container = holder.itemView as FrameLayout
        assertEquals(1, container.childCount)
        assertTrue("factory が返した LinearLayout が addView される", container.getChildAt(0) is LinearLayout)
    }

    @Test
    fun `SectionAccessory View の中身を差し替えても同一 ViewHolder で再描画される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionAnyViewAccessoryViewHolder.create(parent)

        // 1 回目: ComposeView
        holder.bind(
            accessory = SectionAccessory.View(view = KsAnyView.Compose { DummyComposable() }),
            theme = Theme(),
            isHeader = true,
        )
        val container = holder.itemView as FrameLayout
        val firstChild: View = container.getChildAt(0)
        assertTrue(firstChild is ComposeView)

        // 2 回目: 別 KsAnyView を bind（Compose → Compose）
        holder.bind(
            accessory = SectionAccessory.View(view = KsAnyView.Compose { DummyComposable() }),
            theme = Theme(),
            isHeader = true,
        )
        // container は 1 つの子を持ち、ComposeView が再利用される
        // （`bindKsAnyView` は ViewHolder 単位で ComposeView を再利用し、内部 MutableState のみ更新）
        assertEquals(1, container.childCount)
        // ComposeView 自体は再利用されるので同一参照
        assertEquals(
            "ComposeView は ViewHolder 単位で再利用される",
            firstChild,
            container.getChildAt(0),
        )
        assertNotNull(container.getChildAt(0))
    }

    @Test
    fun `SectionAccessory View Compose から AndroidView に切り替えると ComposeView は破棄される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionAnyViewAccessoryViewHolder.create(parent)

        holder.bind(
            accessory = SectionAccessory.View(view = KsAnyView.Compose { DummyComposable() }),
            theme = Theme(),
            isHeader = true,
        )
        val container = holder.itemView as FrameLayout
        assertTrue(container.getChildAt(0) is ComposeView)

        // Compose → AndroidView 切替で container はクリアされ、新しい子が addView される
        holder.bind(
            accessory = SectionAccessory.View(view = KsAnyView.AndroidView { c -> LinearLayout(c) }),
            theme = Theme(),
            isHeader = true,
        )
        assertEquals(1, container.childCount)
        assertTrue(container.getChildAt(0) is LinearLayout)
    }

    @Test
    fun `SectionAnyViewAccessoryViewHolder の reset で ComposeView 再利用キャッシュが解放される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionAnyViewAccessoryViewHolder.create(parent)

        holder.bind(
            accessory = SectionAccessory.View(view = KsAnyView.Compose { DummyComposable() }),
            theme = Theme(),
            isHeader = true,
        )
        val container = holder.itemView as FrameLayout
        val firstChild: View = container.getChildAt(0)
        assertTrue(firstChild is ComposeView)

        // reset 後は container がクリアされ、tag のキャッシュも解放される
        holder.reset()
        assertEquals(0, container.childCount)

        // 再 bind すると新しい ComposeView が生成される（参照差で確認）
        holder.bind(
            accessory = SectionAccessory.View(view = KsAnyView.Compose { DummyComposable() }),
            theme = Theme(),
            isHeader = true,
        )
        assertEquals(1, container.childCount)
        assertTrue(container.getChildAt(0) is ComposeView)
        assertTrue(
            "reset 後の ComposeView は新規インスタンス",
            container.getChildAt(0) !== firstChild,
        )
    }

    // -------------------------------------------------------------------------
    // headerHeight 反映と Header/Footer 垂直配置
    // -------------------------------------------------------------------------

    @Test
    fun `Phase 15_6 Header bind で TextView gravity が BOTTOM_START になる`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionTextAccessoryViewHolder.create(parent)
        holder.bind(
            accessory = SectionAccessory.Text("CommandCell"),
            theme = Theme(),
            isHeader = true,
        )
        val tv = holder.itemView as TextView
        // AiForms `TextHeaderView.SetVerticalAlignment(LayoutAlignment.End)` 既定 = bottom 揃え
        val expected = android.view.Gravity.BOTTOM or android.view.Gravity.START
        assertEquals(
            "Header の TextView gravity は BOTTOM_START（下端揃え）でなければならない",
            expected,
            tv.gravity,
        )
    }

    @Test
    fun `Phase 15_6 Footer bind で TextView gravity が TOP_START になる`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionTextAccessoryViewHolder.create(parent)
        holder.bind(
            accessory = SectionAccessory.Text("You can select either TypeA or TypeB."),
            theme = Theme(),
            isHeader = false,
        )
        val tv = holder.itemView as TextView
        // AiForms `TextFooterView` 既定（TopAnchor 制約）= top 揃え
        val expected = android.view.Gravity.TOP or android.view.Gravity.START
        assertEquals(
            "Footer の TextView gravity は TOP_START（上端揃え）でなければならない",
            expected,
            tv.gravity,
        )
    }

    @Test
    fun `Phase 15_3 Header bind で headerHeight 正値が layoutParams height に反映される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionTextAccessoryViewHolder.create(parent)
        holder.bind(
            accessory = SectionAccessory.Text("CommandCell"),
            theme = Theme(),
            isHeader = true,
            headerHeight = 60.0,
        )
        val tv = holder.itemView as TextView
        val density = tv.resources.displayMetrics.density
        val expectedPx = (60.0 * density).toInt()
        assertEquals(
            "headerHeight = 60 が layoutParams.height に density 倍で反映される",
            expectedPx,
            tv.layoutParams.height,
        )
    }

    @Test
    fun `Phase 15_3 Header bind で headerHeight が -1 のとき layoutParams height は WRAP_CONTENT`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionTextAccessoryViewHolder.create(parent)
        holder.bind(
            accessory = SectionAccessory.Text("LabelCell"),
            theme = Theme(),
            isHeader = true,
            headerHeight = -1.0,
        )
        val tv = holder.itemView as TextView
        assertEquals(
            "headerHeight = -1.0 のとき layoutParams.height は WRAP_CONTENT のまま",
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            tv.layoutParams.height,
        )
    }

    @Test
    fun `Header bind で Section_headerHeight が -1 のとき Theme_headerHeight が fallback として layoutParams height に反映される`() {
        // Section ごとの `Section.headerHeight` が `-1.0` のときは `Theme.headerHeight` を採用する。
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionTextAccessoryViewHolder.create(parent)
        holder.bind(
            accessory = SectionAccessory.Text("CommandCell"),
            theme = Theme(headerHeight = 50.0),
            isHeader = true,
            headerHeight = -1.0,
        )
        val tv = holder.itemView as TextView
        val density = tv.resources.displayMetrics.density
        val expectedPx = (50.0 * density).toInt()
        assertEquals(
            "Section.headerHeight = -1.0 のとき Theme.headerHeight = 50 が fallback として反映される",
            expectedPx,
            tv.layoutParams.height,
        )
    }

    @Test
    fun `Header bind で Section_headerHeight 明示が Theme_headerHeight より優先される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionTextAccessoryViewHolder.create(parent)
        holder.bind(
            accessory = SectionAccessory.Text("CommandCell"),
            theme = Theme(headerHeight = 50.0),
            isHeader = true,
            headerHeight = 80.0,
        )
        val tv = holder.itemView as TextView
        val density = tv.resources.displayMetrics.density
        val expectedPx = (80.0 * density).toInt()
        assertEquals(
            "Section.headerHeight = 80 明示は Theme.headerHeight = 50 より優先される",
            expectedPx,
            tv.layoutParams.height,
        )
    }

    @Test
    fun `Header bind で Theme_headerFont が指定されると TextView_typeface と textSize に反映される`() {
        // `theme.headerFont` が `null` のとき TextView 既定、非 `null` のとき Typeface / size に反映する。
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionTextAccessoryViewHolder.create(parent)
        val customStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
        holder.bind(
            accessory = SectionAccessory.Text("CommandCell"),
            theme = Theme(headerFont = customStyle),
            isHeader = true,
        )
        val tv = holder.itemView as TextView
        assertNotNull("Theme.headerFont 指定時、TextView.typeface が設定される", tv.typeface)
        // fontSize = 18.sp は TextView.textSize（px）に反映される。
        val density = tv.resources.displayMetrics.scaledDensity
        assertEquals(
            "headerFont.fontSize = 18.sp は TextView.textSize に反映される",
            18.0f * density,
            tv.textSize,
            0.5f,
        )
    }

    @Test
    fun `Header bind で Theme_headerFontSize 0以上のとき TextView_textSize が反映される`() {
        // `Theme.headerFontSize > 0` のとき size を上書きする。
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionTextAccessoryViewHolder.create(parent)
        holder.bind(
            accessory = SectionAccessory.Text("CommandCell"),
            theme = Theme(headerFontSize = 24.0),
            isHeader = true,
        )
        val tv = holder.itemView as TextView
        val density = tv.resources.displayMetrics.scaledDensity
        val expectedPx = 24.0f * density
        assertEquals(
            "headerFontSize = 24.0 のとき TextView.textSize は 24 sp 相当",
            expectedPx,
            tv.textSize,
            0.5f,
        )
    }

    @Test
    fun `Footer bind で Theme_footerFontSize 0以上のとき TextView_textSize が反映される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionTextAccessoryViewHolder.create(parent)
        holder.bind(
            accessory = SectionAccessory.Text("Footer text"),
            theme = Theme(footerFontSize = 18.0),
            isHeader = false,
        )
        val tv = holder.itemView as TextView
        val density = tv.resources.displayMetrics.scaledDensity
        val expectedPx = 18.0f * density
        assertEquals(
            "footerFontSize = 18.0 のとき TextView.textSize は 18 sp 相当",
            expectedPx,
            tv.textSize,
            0.5f,
        )
    }

    @Test
    fun `Phase 15_3 flatten で Section_headerHeight が CellListItem_SectionHeader に伝搬する`() {
        val sections = listOf(
            Section(
                id = "s1",
                header = SectionAccessory.Text("CommandCell"),
                headerHeight = 60.0,
                cells = listOf(LabelCell(id = "c1", title = "X")),
            ),
        )
        val list = KsSettingsView.flatten(sections)
        val sh = list.first() as CellListItem.SectionHeader
        assertEquals(
            "CellListItem.SectionHeader.headerHeight は Section.headerHeight を反映する",
            60.0,
            sh.headerHeight,
            0.0001,
        )
    }

    // -------------------------------------------------------------------------
    // Section TextView の内部 padding ポリシー
    //   Cell 群に面する側（Header = 下 / Footer = 上）にだけ 4dp の余白を入れ、
    //   反対側は 0、横方向は 16dp 相当を維持する。
    // -------------------------------------------------------------------------

    @Test
    fun `Header bind で TextView は Cell 側 (下) にだけ 4dp の余白を持つ`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionTextAccessoryViewHolder.create(parent)
        holder.bind(
            accessory = SectionAccessory.Text("CommandCell"),
            theme = Theme(),
            isHeader = true,
            headerHeight = 60.0,
        )
        val tv = holder.itemView as TextView
        val density = tv.resources.displayMetrics.density
        assertEquals(
            "Header の TextView 上 padding は 0 でなければならない（Cell 側は下だけ）",
            0,
            tv.paddingTop,
        )
        assertEquals(
            "Header の TextView 下 padding は Cell 群との間隔 4dp でなければならない",
            (4 * density).toInt(),
            tv.paddingBottom,
        )
        val expectedSidePadding = (16 * density).toInt()
        assertEquals(
            "横方向 padding は 16dp 相当を維持する",
            expectedSidePadding,
            tv.paddingLeft,
        )
        assertEquals(
            "横方向 padding は 16dp 相当を維持する",
            expectedSidePadding,
            tv.paddingRight,
        )
    }

    @Test
    fun `Footer bind で TextView は Cell 側 (上) にだけ 4dp の余白を持つ`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: RecyclerView = RecyclerView(ctx)
        val holder = SectionTextAccessoryViewHolder.create(parent)
        holder.bind(
            accessory = SectionAccessory.Text("You can select either TypeA or TypeB."),
            theme = Theme(),
            isHeader = false,
        )
        val tv = holder.itemView as TextView
        assertEquals(
            "Footer の TextView 上 padding は Cell 群との間隔 4dp でなければならない",
            (4 * tv.resources.displayMetrics.density).toInt(),
            tv.paddingTop,
        )
        assertEquals(
            "Footer の TextView 下 padding は 0 でなければならない（Cell 側は上だけ）",
            0,
            tv.paddingBottom,
        )
    }

    @Test
    fun `KsSettingsView 経由でも SectionAccessory Text が描画される`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = KsSettingsView(ctx)
        view.setRootDirect(
            SettingsRoot(
                sections = listOf(
                    Section(
                        id = "s1",
                        header = SectionAccessory.Text("一般"),
                        cells = listOf(LabelCell(id = "c1", title = "Hi")),
                    ),
                ),
            ),
        )
        // 平坦リストは [SectionHeader, CellRow] = 2 件
        assertEquals(2, view.internalMainListAdapter().itemCount)
    }
}
