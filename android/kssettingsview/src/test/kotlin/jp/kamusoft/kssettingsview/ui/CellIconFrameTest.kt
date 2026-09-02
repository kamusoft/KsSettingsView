package jp.kamusoft.kssettingsview.ui

import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * 共通行の icon 領域が「解決済み icon size の正方形枠 + 枠に対する角丸」になることを検証する。
 *
 * 枠の一辺は `CellStyle.iconSize` → `Theme.cellIconSize` → 既定の順で解決した値だけで決まり、
 * 画像の実寸や縦横比には依存しない。角丸は枠に対してかかり、aspect fit した画像の描画矩形には
 * 追従しない（core/ADR-0025）。観測は `bind` 後に measure / layout を走らせた実 View に対して行う。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CellIconFrameTest {

    /** `KsSettingsView` を 1 つだけ載せるホスト Activity（Theme 変更の反映経路の検証に使う）。 */
    class HostActivity : FragmentActivity() {
        lateinit var settingsView: KsSettingsView

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(MaterialR.style.Theme_Material3_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            val container = FrameLayout(this)
            setContentView(container)
            settingsView = KsSettingsView(this)
            container.addView(settingsView)
        }
    }

    private var controller: ActivityController<HostActivity>? = null

    @After
    fun tearDown() {
        controller?.close()
        controller = null
    }

    private val ctx: android.content.Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            MaterialR.style.Theme_Material3_Light_NoActionBar,
        )

    private val parent get() = FrameLayout(ctx)

    private val density get() = ctx.resources.displayMetrics.density

    /** dp 値を共通行レイアウトと同じ丸めで px へ変換する。 */
    private fun px(dp: Float): Int = (dp * density).toInt()

    /** 縦横比が 1:1 でない drawable（intrinsic size を [w] × [h] に固定する）。 */
    private class FixedSizeDrawable(private val w: Int, private val h: Int) :
        ColorDrawable(0xFF00FF00.toInt()) {
        override fun getIntrinsicWidth(): Int = w
        override fun getIntrinsicHeight(): Int = h
    }

    private fun squareIcon(): KsImage = KsImage.Drawable(FixedSizeDrawable(24, 24))

    /** 親幅を 320dp に固定して measure / layout する。 */
    private fun layoutRow(root: View) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(px(320f), View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
    }

    /** [cell] を bind してレイアウトまで済ませた ViewHolder を返す。 */
    private fun bindRow(cell: LabelCell, theme: Theme = Theme()): LabelCellViewHolder {
        val vh = LabelCellViewHolder.create(parent)
        vh.bind(cell, theme)
        layoutRow(vh.views.root)
        return vh
    }

    /** [view] の `ViewOutlineProvider` が現在与える outline。 */
    private fun outlineOf(view: View): Outline {
        val outline = Outline()
        view.outlineProvider.getOutline(view, outline)
        return outline
    }

    /** icon 領域の実寸（レイアウト後の幅・高さ）が [expectedPx] の正方形であることを確認する。 */
    private fun assertSquareFrame(views: CellBaseViews, expectedPx: Int) {
        val lp = views.iconView.layoutParams
        assertEquals("icon 枠の LayoutParams 幅", expectedPx, lp.width)
        assertEquals("icon 枠の LayoutParams 高さ", expectedPx, lp.height)
        assertEquals("icon 領域の実測幅", expectedPx, views.iconView.width)
        assertEquals("icon 領域の実測高さ", expectedPx, views.iconView.height)
        assertEquals(
            "icon 領域は幅と高さが同じ正方形",
            views.iconView.width,
            views.iconView.height,
        )
    }

    // MARK: - 枠の寸法

    @Test
    fun `Theme の cellIconSize が icon 枠の一辺になる`() {
        val vh = bindRow(
            LabelCell(title = "通知", icon = squareIcon()),
            Theme(cellIconSize = 40.dp),
        )

        assertEquals(View.VISIBLE, vh.views.iconView.visibility)
        assertSquareFrame(vh.views, px(40f))
        assertEquals(
            "画像は枠へ縦横比を保って収める",
            ImageView.ScaleType.FIT_CENTER,
            vh.views.iconView.scaleType,
        )
    }

    @Test
    fun `CellStyle の iconSize は Theme より優先される`() {
        val vh = bindRow(
            LabelCell(
                title = "通知",
                icon = squareIcon(),
                style = CellStyle(iconSize = 32.dp),
            ),
            Theme(cellIconSize = 40.dp),
        )

        assertSquareFrame(vh.views, px(32f))
    }

    @Test
    fun `icon size 未指定なら iOS と同じ生値の既定枠になる`() {
        // 既定値の生値は platform 間で共有する契約（iOS の `Theme.defaultCellIconSize` /
        // `defaultCellIconRadius` と同じ 24 / 0）。
        assertEquals(
            "icon size の既定は iOS と同じ生値",
            24.0f,
            Theme.DEFAULT_CELL_ICON_SIZE_DP_VALUE,
            0.0f,
        )
        assertEquals(
            "icon radius の既定は iOS と同じ生値（角丸なし）",
            0.0f,
            Theme.DEFAULT_CELL_ICON_RADIUS_DP_VALUE,
            0.0f,
        )

        val vh = bindRow(LabelCell(title = "通知", icon = squareIcon()), Theme())

        assertSquareFrame(vh.views, px(Theme.DEFAULT_CELL_ICON_SIZE_DP_VALUE))
        assertFalse("既定では角丸 clip をしない", vh.views.iconView.clipToOutline)
    }

    @Test
    fun `非正方形の drawable でも icon 枠は正方形のまま`() {
        val vh = bindRow(
            LabelCell(
                title = "通知",
                icon = KsImage.Drawable(FixedSizeDrawable(w = 120, h = 24)),
            ),
            Theme(cellIconSize = 40.dp),
        )

        assertSquareFrame(vh.views, px(40f))
        assertEquals(
            "画像は枠を超えず縦横比を保って収まる",
            ImageView.ScaleType.FIT_CENTER,
            vh.views.iconView.scaleType,
        )
    }

    @Test
    fun `icon を持たない Cell では icon 領域が表示されず title は通常の開始位置に置かれる`() {
        val vh = bindRow(LabelCell(title = "通知"), Theme(cellIconSize = 40.dp))
        val views = vh.views

        assertEquals(View.GONE, views.iconView.visibility)
        assertEquals(
            "title は icon 領域の余白を伴わず行左端の余白 16dp のところから始まる",
            px(CELL_ROW_HORIZONTAL_MARGIN_DP.toFloat()),
            views.contentRow.left + views.titleView.left,
        )
    }

    /**
     * 最低行高（60dp）を超える icon を指定した行では、行高が icon の一辺 + 上下の行余白になり、
     * icon が行の上下端（= 罫線）に密着しない。
     *
     * `Theme.cellIconSize` / `CellStyle.iconSize` は公開 API であり、アバター用途などで
     * 最低行高を超える値を指定できる。行の上下余白は `iconView` の TOP / BOTTOM マージンが持つ。
     */
    @Test
    fun `行高より大きい icon を指定しても icon は行の上下端に密着しない`() {
        val iconSizeDp = 80f
        val vh = bindRow(
            LabelCell(title = "通知", icon = squareIcon()),
            Theme(cellIconSize = androidx.compose.ui.unit.Dp(iconSizeDp)),
        )
        val views = vh.views
        val root = views.root
        val icon = views.iconView
        val rowMarginV = px(CELL_ROW_VERTICAL_MARGIN_DP.toFloat())

        assertEquals("icon 枠の一辺は指定どおり", px(iconSizeDp), icon.height)
        assertEquals(
            "行高は icon の一辺 + 上下の行余白",
            px(iconSizeDp) + rowMarginV * 2,
            root.height,
        )
        assertEquals("icon 上端と行上端の間隔は 4dp", rowMarginV, icon.top)
        assertEquals("icon 下端と行下端の間隔は 4dp", rowMarginV, root.height - icon.bottom)
    }

    @Test
    fun `Theme 変更で表示中の行の icon 枠が更新される`() {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()
        val view = activity.settingsView
        val store = SettingsRootStore(
            initialRoot = SettingsRoot(
                sections = listOf(
                    Section(
                        id = "s1",
                        cells = listOf(
                            LabelCell(id = "c1", title = "通知", icon = squareIcon()),
                        ),
                    ),
                ),
            ),
            initialTheme = Theme(cellIconSize = 24.dp),
        )
        view.bind(store)
        idle()
        layoutHost(view)

        val iconView = firstCellRow(view).views.iconView
        assertEquals("切替前の icon 枠", px(24f), iconView.width)

        view.theme = Theme(cellIconSize = 48.dp)
        idle()
        layoutHost(view)

        val after = firstCellRow(view).views.iconView
        assertEquals("表示中の行の View が使い回されている", iconView, after)
        assertEquals("切替後の icon 枠（幅）", px(48f), after.width)
        assertEquals("切替後の icon 枠（高さ）", px(48f), after.height)
    }

    /** ホストの `KsSettingsView` を実サイズで measure / layout する。 */
    private fun layoutHost(view: KsSettingsView) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, 1080, 1920)
    }

    /** 表示中の行から最初の [LabelCellViewHolder] を取り出す。 */
    private fun firstCellRow(view: KsSettingsView): LabelCellViewHolder {
        val rv: RecyclerView = view.internalRecyclerView()
        return (0 until rv.childCount)
            .mapNotNull { rv.getChildViewHolder(rv.getChildAt(it)) as? LabelCellViewHolder }
            .first()
    }

    // MARK: - 枠に対する角丸

    @Test
    fun `Theme の cellIconRadius で枠が角丸に clip される`() {
        val vh = bindRow(
            LabelCell(title = "通知", icon = squareIcon()),
            Theme(cellIconSize = 40.dp, cellIconRadius = 12.dp),
        )
        val iconView = vh.views.iconView

        assertTrue("角丸指定があるとき clip する", iconView.clipToOutline)
        val outline = outlineOf(iconView)
        assertEquals("角丸半径は解決済み radius", 12f * density, outline.radius, 0.5f)
        val rect = Rect()
        assertTrue("clip 形状は矩形として取得できる", outline.getRect(rect))
        assertEquals("clip 形状は icon 枠そのもの", Rect(0, 0, px(40f), px(40f)), rect)
    }

    @Test
    fun `CellStyle の iconRadius は Theme より優先される`() {
        val vh = bindRow(
            LabelCell(
                title = "通知",
                icon = squareIcon(),
                style = CellStyle(iconRadius = 4.dp),
            ),
            Theme(cellIconSize = 40.dp, cellIconRadius = 12.dp),
        )

        assertTrue(vh.views.iconView.clipToOutline)
        assertEquals(
            "角丸半径は CellStyle の値",
            4f * density,
            outlineOf(vh.views.iconView).radius,
            0.5f,
        )
    }

    @Test
    fun `角丸未指定なら clip しない`() {
        val vh = bindRow(
            LabelCell(title = "通知", icon = squareIcon()),
            Theme(cellIconSize = 40.dp),
        )

        assertFalse("角丸なしのとき clip しない", vh.views.iconView.clipToOutline)
    }

    @Test
    fun `角丸は正方形枠に対してかかり画像の描画矩形には追従しない`() {
        val vh = bindRow(
            LabelCell(
                title = "通知",
                // 枠へ aspect fit すると描画矩形は 40dp × 8dp になる縦横比。
                icon = KsImage.Drawable(FixedSizeDrawable(w = 120, h = 24)),
            ),
            Theme(cellIconSize = 40.dp, cellIconRadius = 12.dp),
        )

        val rect = Rect()
        assertTrue(outlineOf(vh.views.iconView).getRect(rect))
        assertEquals(
            "clip 形状は画像の描画矩形ではなく icon 枠の正方形",
            Rect(0, 0, px(40f), px(40f)),
            rect,
        )
        assertEquals(
            "角丸半径は画像の寸法に依存しない",
            12f * density,
            outlineOf(vh.views.iconView).radius,
            0.5f,
        )
    }

    @Test
    fun `再 bind で radius の変更と解除が反映される`() {
        val vh = LabelCellViewHolder.create(parent)

        fun bindWithRadius(radius: Dp?) {
            vh.bind(
                LabelCell(id = "c1", title = "通知", icon = squareIcon()),
                Theme(cellIconSize = 40.dp, cellIconRadius = radius),
            )
            layoutRow(vh.views.root)
        }

        bindWithRadius(12.dp)
        assertEquals(12f * density, outlineOf(vh.views.iconView).radius, 0.5f)

        bindWithRadius(4.dp)
        assertTrue("角丸のまま再 bind される", vh.views.iconView.clipToOutline)
        assertEquals(
            "新しい radius で clip し直す",
            4f * density,
            outlineOf(vh.views.iconView).radius,
            0.5f,
        )

        bindWithRadius(null)
        assertFalse("角丸なしで再 bind すると clip が解除される", vh.views.iconView.clipToOutline)
    }
}
