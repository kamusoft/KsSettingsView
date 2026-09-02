package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * view accessory（`SectionAccessory.View`）の Section Header にも固定高さが効くことを検証する。
 *
 * 高さの解決規則は Text accessory と共通（`Section.headerHeight` > `Theme.headerHeight` > 自動）で、
 * iOS 側の解決と対称になる。表示済みの Header の高さが変わったときは、accessory が保持する View を
 * 作り直さずに高さだけを更新する必要があるため、動的変更は Store → Host → RecyclerView の実経路で
 * 検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ViewAccessoryHeaderHeightTest {

    /** [KsSettingsView] を載せる器だけを持つホスト Activity。 */
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

    /** dp 指定の Header 高さを、bind が使うのと同じ換算で px へ直す。 */
    private fun dpToPx(dp: Double): Int = (dp * ctx.resources.displayMetrics.density).toInt()

    /** 高さ検証用の view accessory（中身は固有の識別ができる [EditText]）。 */
    private fun viewAccessory(initialText: String = ""): SectionAccessory.View =
        SectionAccessory.View(
            KsAnyView.AndroidView { c -> EditText(c).apply { setText(initialText) } },
        )

    /** [sections] をコミット済みにした Adapter を返す（`flatten` と `submitList` の実経路を通す）。 */
    private fun adapterOf(sections: List<Section>, theme: Theme = Theme()): KsSettingsListAdapter {
        val adapter = KsSettingsListAdapter()
        adapter.theme = theme
        adapter.submitList(KsSettingsView.flatten(sections))
        idle()
        return adapter
    }

    /**
     * 領域より小さい内容（20dp）を持つ accessory の中身。占有範囲の差が測れるようにする。
     *
     * 素の `View` は上限付きの測定要求に対して上限いっぱいを返すため、占有範囲の差が測れない。
     * 最小高さを内容として尊重する [FrameLayout] を使い、「内容なり」と「領域いっぱい」を区別する。
     */
    private fun shortContent(): KsAnyView.AndroidView =
        KsAnyView.AndroidView { c -> FrameLayout(c).apply { minimumHeight = dpToPx(20.0) } }

    /** Header 領域に載っている accessory の中身（container の唯一の子）。 */
    private fun hostedView(holder: SectionAnyViewAccessoryViewHolder): View =
        (holder.itemView as FrameLayout).getChildAt(0)

    /**
     * 行の高さは親が layoutParams から決めるため、器へ載せて実際に measure する。
     *
     * 同一 ViewHolder を使い回すテストでは 2 回目以降すでに器へ載っているので、その器を再利用する。
     */
    private fun measureRow(holder: SectionAnyViewAccessoryViewHolder) {
        val itemView = holder.itemView
        val parent = itemView.parent as? FrameLayout
            ?: FrameLayout(ctx).also { it.addView(itemView) }
        parent.measure(
            View.MeasureSpec.makeMeasureSpec(dpToPx(360.0), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
    }

    /** [position] を View accessory 用の ViewHolder へ bind し、その ViewHolder を返す。 */
    private fun bindAnyView(
        adapter: KsSettingsListAdapter,
        position: Int,
        holder: SectionAnyViewAccessoryViewHolder = SectionAnyViewAccessoryViewHolder.create(RecyclerView(ctx)),
    ): SectionAnyViewAccessoryViewHolder {
        adapter.onBindViewHolder(holder, position)
        return holder
    }

    // MARK: - 静的な高さ解決

    @Test
    fun `view accessory の Header は Section_headerHeight 正値で固定高さになる`() {
        val adapter = adapterOf(
            listOf(Section(id = "s1", header = viewAccessory(), headerHeight = 60.0)),
        )
        val holder = bindAnyView(adapter, 0)
        assertEquals(
            "Section.headerHeight = 60 が layoutParams.height に density 倍で反映される",
            dpToPx(60.0),
            holder.itemView.layoutParams.height,
        )
    }

    @Test
    fun `view accessory の Header は Section_headerHeight 未指定で Theme_headerHeight を採用する`() {
        val adapter = adapterOf(
            listOf(Section(id = "s1", header = viewAccessory(), headerHeight = -1.0)),
            theme = Theme(headerHeight = 50.0),
        )
        val holder = bindAnyView(adapter, 0)
        assertEquals(
            "Section.headerHeight = -1.0 のとき Theme.headerHeight = 50 が fallback として反映される",
            dpToPx(50.0),
            holder.itemView.layoutParams.height,
        )
    }

    @Test
    fun `view accessory の Header は Section_headerHeight を Theme_headerHeight より優先する`() {
        val adapter = adapterOf(
            listOf(Section(id = "s1", header = viewAccessory(), headerHeight = 80.0)),
            theme = Theme(headerHeight = 50.0),
        )
        val holder = bindAnyView(adapter, 0)
        assertEquals(
            "Section.headerHeight = 80 明示は Theme.headerHeight = 50 より優先される",
            dpToPx(80.0),
            holder.itemView.layoutParams.height,
        )
    }

    @Test
    fun `view accessory の Header は高さ未指定なら自動高さのまま`() {
        val adapter = adapterOf(
            listOf(Section(id = "s1", header = viewAccessory(), headerHeight = -1.0)),
        )
        val holder = bindAnyView(adapter, 0)
        assertEquals(
            "Section・Theme とも未指定なら layoutParams.height は WRAP_CONTENT",
            ViewGroup.LayoutParams.WRAP_CONTENT,
            holder.itemView.layoutParams.height,
        )
    }

    @Test
    fun `固定高さと自動高さの Header は ViewHolder を再利用しても互いに影響しない`() {
        val adapter = adapterOf(
            listOf(
                Section(id = "s1", header = viewAccessory(), headerHeight = 60.0),
                Section(id = "s2", header = viewAccessory(), headerHeight = -1.0),
            ),
        )
        // 同一 ViewHolder を両 Header へ交互に bind する（RecyclerView の再利用と同じ経路）。
        val holder = SectionAnyViewAccessoryViewHolder.create(RecyclerView(ctx))

        bindAnyView(adapter, 0, holder)
        assertEquals(dpToPx(60.0), holder.itemView.layoutParams.height)

        bindAnyView(adapter, 1, holder)
        assertEquals(
            "自動高さの Header へ再利用しても前の固定高さを引きずらない",
            ViewGroup.LayoutParams.WRAP_CONTENT,
            holder.itemView.layoutParams.height,
        )

        bindAnyView(adapter, 0, holder)
        assertEquals(
            "固定高さの Header へ戻せば再び固定高さになる",
            dpToPx(60.0),
            holder.itemView.layoutParams.height,
        )
    }

    @Test
    fun `view accessory の Footer は headerHeight 指定の対象外`() {
        val adapter = adapterOf(
            listOf(
                Section(
                    id = "s1",
                    header = viewAccessory(),
                    headerHeight = 60.0,
                    footer = viewAccessory(),
                ),
            ),
            theme = Theme(headerHeight = 50.0),
        )
        // 平坦リストは [SectionHeader, SectionFooter] の 2 件。
        val holder = bindAnyView(adapter, 1)
        assertEquals(
            "Footer は headerHeight の対象外で自動高さのまま",
            ViewGroup.LayoutParams.WRAP_CONTENT,
            holder.itemView.layoutParams.height,
        )
    }

    @Test
    fun `固定高さは内容が収まらない場合でも指定値のまま維持される`() {
        val tallContent = KsAnyView.AndroidView { c ->
            View(c).apply { minimumHeight = dpToPx(300.0) }
        }
        val adapter = adapterOf(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.View(tallContent),
                    headerHeight = 40.0,
                ),
            ),
        )
        val holder = bindAnyView(adapter, 0)

        // 行の高さは親が layoutParams から決めるため、器へ載せて実際に measure する。
        val parent = FrameLayout(ctx)
        parent.addView(holder.itemView)
        parent.measure(
            View.MeasureSpec.makeMeasureSpec(dpToPx(360.0), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )

        assertEquals(
            "内容が指定値より大きくても Header の高さは指定値のまま（超過分は器の外へ出ない）",
            dpToPx(40.0),
            holder.itemView.measuredHeight,
        )
    }

    @Test
    fun `固定高さのとき hosted view は Header 領域いっぱいに広がる`() {
        val adapter = adapterOf(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.View(shortContent()),
                    headerHeight = 60.0,
                ),
            ),
        )
        val holder = bindAnyView(adapter, 0)
        measureRow(holder)

        assertEquals("Header 領域は指定値の固定高さ", dpToPx(60.0), holder.itemView.measuredHeight)
        assertEquals(
            "内容が領域より小さくても hosted view は領域いっぱいまで広がる",
            dpToPx(60.0),
            hostedView(holder).measuredHeight,
        )
    }

    @Test
    fun `自動高さのとき hosted view は内容なりの高さになる`() {
        val adapter = adapterOf(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.View(shortContent()),
                    headerHeight = -1.0,
                ),
            ),
        )
        val holder = bindAnyView(adapter, 0)
        measureRow(holder)

        assertEquals(
            "自動高さでは hosted view は内容なりの高さのまま",
            dpToPx(20.0),
            hostedView(holder).measuredHeight,
        )
        assertEquals("Header 領域も内容なりの高さ", dpToPx(20.0), holder.itemView.measuredHeight)
    }

    @Test
    fun `hosted view の占有範囲は固定と自動の切り替えに追随する`() {
        val adapter = adapterOf(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.View(shortContent()),
                    headerHeight = 60.0,
                ),
                Section(
                    id = "s2",
                    header = SectionAccessory.View(shortContent()),
                    headerHeight = -1.0,
                ),
            ),
        )
        // 同一 ViewHolder を固定 → 自動 → 固定の順に使い回す（RecyclerView の再利用と同じ経路）。
        val holder = SectionAnyViewAccessoryViewHolder.create(RecyclerView(ctx))

        bindAnyView(adapter, 0, holder)
        measureRow(holder)
        assertEquals(dpToPx(60.0), hostedView(holder).measuredHeight)

        bindAnyView(adapter, 1, holder)
        measureRow(holder)
        assertEquals(
            "自動高さの Header へ再利用したら hosted view も内容なりへ戻る",
            dpToPx(20.0),
            hostedView(holder).measuredHeight,
        )

        bindAnyView(adapter, 0, holder)
        measureRow(holder)
        assertEquals(
            "固定高さの Header へ戻せば hosted view も再び領域いっぱいになる",
            dpToPx(60.0),
            hostedView(holder).measuredHeight,
        )
    }

    // MARK: - 表示済み Header の動的な高さ変更（実経路）

    @Test
    fun `自動高さから固定高さへの変更が表示へ反映される`() {
        val host = startHost(headerHeight = -1.0)
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, host.headerHolder.itemView.layoutParams.height)

        host.changeHeaderHeight(48.0)

        assertEquals(dpToPx(48.0), host.headerHolder.itemView.layoutParams.height)
    }

    @Test
    fun `固定高さから自動高さへの変更が表示へ反映される`() {
        val host = startHost(headerHeight = 48.0)
        assertEquals(dpToPx(48.0), host.headerHolder.itemView.layoutParams.height)

        host.changeHeaderHeight(-1.0)

        assertEquals(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            host.headerHolder.itemView.layoutParams.height,
        )
    }

    @Test
    fun `固定高さの値変更が表示へ反映される`() {
        val host = startHost(headerHeight = 48.0)

        host.changeHeaderHeight(96.0)

        assertEquals(dpToPx(96.0), host.headerHolder.itemView.layoutParams.height)
    }

    @Test
    fun `高さのみの変更では accessory の View と入力中の内容が維持される`() {
        val host = startHost(headerHeight = -1.0)
        val editorBefore = host.headerEditor
        editorBefore.setText("入力中")

        host.changeHeaderHeight(48.0)

        assertEquals(dpToPx(48.0), host.headerHolder.itemView.layoutParams.height)
        assertSame(
            "高さだけの変更では accessory の View インスタンスが維持される",
            editorBefore,
            host.headerEditor,
        )
        assertEquals(
            "維持された View の内部状態（入力中のテキスト）も失われない",
            "入力中",
            host.headerEditor.text.toString(),
        )
    }

    @Test
    fun `高さのみの変更でも hosted view の占有範囲が追随する`() {
        val host = startHost(
            headerHeight = -1.0,
            accessory = SectionAccessory.View(shortContent()),
        )
        assertEquals(
            "自動高さでは hosted view は内容なりの高さ",
            dpToPx(20.0),
            host.headerContent.measuredHeight,
        )

        host.changeHeaderHeight(48.0)

        assertEquals(dpToPx(48.0), host.headerHolder.itemView.measuredHeight)
        assertEquals(
            "固定高さへの変更で hosted view も領域いっぱいへ広がる",
            dpToPx(48.0),
            host.headerContent.measuredHeight,
        )

        host.changeHeaderHeight(-1.0)

        assertEquals(
            "自動高さへ戻せば hosted view は内容なりへ戻る",
            dpToPx(20.0),
            host.headerContent.measuredHeight,
        )
    }

    /**
     * view accessory の Header を 1 つ持つ設定画面を Activity 上に組み立てる。
     *
     * Store → Host → RecyclerView の実経路で高さの変更を届けるため、レイアウトまで済ませて
     * 表示中の ViewHolder を取り出せる状態にする。
     */
    private fun startHost(
        headerHeight: Double,
        accessory: SectionAccessory.View = viewAccessory(),
    ): Host {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()

        val store = SettingsRootStore(
            initialRoot = SettingsRoot(
                sections = listOf(
                    Section(
                        id = "s1",
                        header = accessory,
                        headerHeight = headerHeight,
                        cells = listOf(LabelCell(id = "c1", title = "A")),
                    ),
                ),
            ),
        )
        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.bind(store)
        idle()

        val host = Host(activity, view, store, accessory)
        host.layout()
        return host
    }

    /** [startHost] が組み立てた設定画面。 */
    private inner class Host(
        val activity: HostActivity,
        val view: KsSettingsView,
        val store: SettingsRootStore,
        val accessory: SectionAccessory.View,
    ) {
        /** レイアウトを走らせ、RecyclerView に行を生成・再バインドさせる。 */
        fun layout() {
            val metrics = activity.resources.displayMetrics
            view.measure(
                View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        }

        /** 表示中の Header 行の ViewHolder。 */
        val headerHolder: SectionAnyViewAccessoryViewHolder
            get() {
                val rv = view.internalRecyclerView()
                val found = rv.findViewHolderForAdapterPosition(0)
                assertTrue(
                    "Header 行の ViewHolder が生成されていない (実際: $found)",
                    found is SectionAnyViewAccessoryViewHolder,
                )
                return found as SectionAnyViewAccessoryViewHolder
            }

        /** Header に表示されている accessory の中身。 */
        val headerContent: View
            get() = (headerHolder.itemView as FrameLayout).getChildAt(0)

        /** Header に表示されている accessory の中身（入力欄として組み立てた場合）。 */
        val headerEditor: EditText
            get() = headerContent as EditText

        /**
         * accessory は据え置きで `Section.headerHeight` だけを変え、表示へ届くまで進める。
         *
         * `submitList` の差分計算はバックグラウンドで走るため、コミット完了を待ってから
         * レイアウトを走らせて再バインドさせる。
         */
        fun changeHeaderHeight(newHeight: Double) {
            store.replaceSection(
                sectionId = "s1",
                new = Section(
                    id = "s1",
                    header = accessory,
                    headerHeight = newHeight,
                    cells = listOf(LabelCell(id = "c1", title = "A")),
                ),
            )
            awaitDifferCommit({ committedSummary() }) { committedHeaderHeight() == newHeight }
            layout()
        }

        /** Adapter がコミット済みの Header 行の固定高さ。 */
        fun committedHeaderHeight(): Double? =
            view.internalMainListAdapter().currentList
                .filterIsInstance<CellListItem.SectionHeader>()
                .firstOrNull()
                ?.headerHeight

        /** コミット済みの平坦リストを、失敗メッセージ用に要約する。 */
        fun committedSummary(): List<String> =
            view.internalMainListAdapter().currentList.map { item ->
                when (item) {
                    is CellListItem.SectionHeader -> "header(h=${item.headerHeight})"
                    is CellListItem.SectionFooter -> "footer"
                    is CellListItem.CellRow -> "cell(${(item.cell as? LabelCell)?.title})"
                }
            }
    }
}
