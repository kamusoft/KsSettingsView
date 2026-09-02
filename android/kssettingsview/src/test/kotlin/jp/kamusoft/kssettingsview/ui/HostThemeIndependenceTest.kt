package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.R as AppCompatR
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.google.android.material.materialswitch.MaterialSwitch
import androidx.test.core.app.ApplicationProvider
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/**
 * ライブラリ UI がホストの XML テーマに依存しないこと（android/ADR-0020）を検証する。
 *
 * 観測の軸は 3 つ。
 *
 * - **成立**: Material3 派生ではないホスト（素の AppCompat / フレームワーク標準テーマ）で、全 Cell 種と
 *   ボトムシート系の選択面が例外なく表示・操作できる
 * - **隔離**: ホストのテーマ属性値が変わってもライブラリ UI の配色が変わらない
 * - **越境しない**: 利用者所有のコンテンツ（[CustomCell] の content・[KsAnyView] 経由の利用者 View）は
 *   ホストの Context のまま解決される
 *
 * 本テストの Context は意図的に `ContextThemeWrapper` で Material3 テーマを被せない。他のテストが
 * 行っている「テスト用に Material3 テーマを明示する」準備が不要になったことも、ここで併せて示す。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HostThemeIndependenceTest {

    /**
     * ホストのテーマを外から与えられる Activity。
     *
     * テーマ ID は生成前に [pendingTheme] へ置く（`onCreate` 前に決まっている必要があるため）。
     */
    class HostActivity : FragmentActivity() {
        lateinit var container: FrameLayout

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(pendingTheme)
            super.onCreate(savedInstanceState)
            container = FrameLayout(this)
            setContentView(container)
        }

        companion object {
            /** 次に生成する Activity へ適用するテーマ。 */
            var pendingTheme: Int = AppCompatR.style.Theme_AppCompat_Light
        }
    }

    private var controller: ActivityController<HostActivity>? = null

    private var frameDriver: ComposeFrameDriver? = null

    @After
    fun tearDown() {
        frameDriver?.stop()
        frameDriver = null
        controller?.close()
        controller = null
    }

    /** 素の AppCompat テーマ（Material3 派生ではない）。 */
    private val appCompatTheme: Int get() = AppCompatR.style.Theme_AppCompat_Light

    /** 素の AppCompat テーマのダーク側（[appCompatTheme] と属性値が異なるホストとして使う）。 */
    private val appCompatDarkTheme: Int get() = AppCompatR.style.Theme_AppCompat

    /** AppCompat / Material のいずれの派生でもない、フレームワーク標準の最小テーマ。 */
    private val frameworkTheme: Int get() = android.R.style.Theme_Material_Light_NoActionBar

    private fun startHost(theme: Int): HostActivity {
        HostActivity.pendingTheme = theme
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        return ctrl.get()
    }

    /** 全 Cell 種を 1 つずつ並べた root。 */
    private fun allCellsRoot(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(
                id = "s1",
                header = SectionAccessory.Text("全 Cell 種"),
                cells = listOf(
                    LabelCell(title = "label"),
                    CommandCell(title = "command"),
                    ButtonCell(title = "button"),
                    SwitchCell(title = "switch"),
                    CheckboxCell(title = "checkbox"),
                    RadioCell(title = "radio", groupId = "g", value = "a", selectedValue = "a"),
                    SimpleCheckCell(title = "simple-check"),
                    EntryCell(title = "entry"),
                    PickerCell(title = "picker", items = listOf("A", "B")),
                    NumberPickerCell(title = "number", min = 0, max = 10),
                    TimePickerCell(title = "time"),
                    DatePickerCell(title = "date"),
                    CustomCell(content = "custom", builder = { _ -> }),
                ),
            ),
        ),
    )

    /**
     * [root] を表示した [KsSettingsView] を組み立て、レイアウトまで済ませて返す。
     *
     * `RecyclerView` は可視領域に収まる行しか生成しないため、全行を一度に生成させたい場合は
     * [heightScale] で画面高の倍率を上げる。
     */
    private fun showRoot(
        activity: HostActivity,
        root: SettingsRoot,
        heightScale: Int = 1,
    ): KsSettingsView {
        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.bind(SettingsRootStore(initialRoot = root))
        awaitConvergence(view) { view.internalMainListAdapter().currentList.isNotEmpty() }
        val metrics = activity.resources.displayMetrics
        val height = metrics.heightPixels * heightScale
        view.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, metrics.widthPixels, height)
        idle()
        return view
    }

    /** 全 Cell 種の root を表示し、生成された行の ViewHolder 種別を検証する。 */
    private fun assertAllCellRowsCreated(activity: HostActivity) {
        val view = showRoot(activity, allCellsRoot(), heightScale = 6)
        val holders = rowHolders(view)

        // Section Header 1 行 + Cell 13 行。
        assertEquals("全 Cell 種の行が生成される (実際: $holders)", 14, holders.size)
        assertTrue(
            "未登録 Cell のプレースホルダへ落ちた行がない (実際: $holders)",
            holders.none { it is EmptyPlaceholderViewHolder },
        )
        val expectedTypes = listOf(
            SectionTextAccessoryViewHolder::class,
            LabelCellViewHolder::class,
            CommandCellViewHolder::class,
            ButtonCellViewHolder::class,
            SwitchCellViewHolder::class,
            CheckboxCellViewHolder::class,
            RadioCellViewHolder::class,
            SimpleCheckCellViewHolder::class,
            EntryCellViewHolder::class,
            PickerCellViewHolder::class,
            NumberPickerCellViewHolder::class,
            TimePickerCellViewHolder::class,
            DatePickerCellViewHolder::class,
            CustomCellViewHolder::class,
        )
        assertEquals(
            "全 Cell 種の ViewHolder が並ぶ",
            expectedTypes,
            holders.map { it::class },
        )
        // 非 Material3 テーマで初期化例外を出していた Material ウィジェットが生成できている。
        assertNotNull(
            "SwitchCell の MaterialSwitch が生成されている",
            holders.filterIsInstance<SwitchCellViewHolder>().single()
                .views.accessoryHolder.getChildAt(0) as? MaterialSwitch,
        )
    }

    /** 生成された行の ViewHolder を上から順に取り出す。 */
    private fun rowHolders(view: KsSettingsView): List<Any> {
        val rv = view.internalRecyclerView()
        return (0 until rv.childCount).mapNotNull { rv.getChildViewHolder(rv.getChildAt(it)) }
    }

    /** テーマ属性を色として解決する（解決できなければ [fallback]）。 */
    private fun resolveColor(context: Context, attr: Int, fallback: Int = 0): Int {
        val tv = TypedValue()
        if (!context.theme.resolveAttribute(attr, tv, true)) return fallback
        if (tv.resourceId != 0) {
            val csl = androidx.core.content.ContextCompat.getColorStateList(context, tv.resourceId)
            if (csl != null) return csl.defaultColor
            return androidx.core.content.ContextCompat.getColor(context, tv.resourceId)
        }
        return tv.data
    }

    // MARK: - ホストテーマ前提の撤廃

    @Test
    fun `全 Cell 種はフレームワーク標準テーマのホストで例外なく表示される`() {
        assertAllCellRowsCreated(startHost(frameworkTheme))
    }

    @Test
    fun `全 Cell 種は素の AppCompat テーマのホストで例外なく表示される`() {
        assertAllCellRowsCreated(startHost(appCompatTheme))
    }

    @Test
    fun `テーマを被せない素の Context でも全 Cell 種の ViewHolder を生成できる`() {
        // テストが Material3 テーマを明示する準備（ContextThemeWrapper の儀式）なしで成立すること。
        val parent = FrameLayout(ApplicationProvider.getApplicationContext<Context>())

        val holders = listOf(
            LabelCellViewHolder.create(parent),
            CommandCellViewHolder.create(parent),
            ButtonCellViewHolder.create(parent),
            SwitchCellViewHolder.create(parent),
            CheckboxCellViewHolder.create(parent),
            RadioCellViewHolder.create(parent),
            SimpleCheckCellViewHolder.create(parent),
            EntryCellViewHolder.create(parent),
            PickerCellViewHolder.create(parent),
            NumberPickerCellViewHolder.create(parent),
            TimePickerCellViewHolder.create(parent),
            DatePickerCellViewHolder.create(parent),
        )

        assertEquals("入力 Cell を含む 12 種の ViewHolder が生成される", 12, holders.size)
        holders.filterIsInstance<SwitchCellViewHolder>().single().bind(SwitchCell(title = "s"), Theme())
        holders.filterIsInstance<CheckboxCellViewHolder>().single().bind(CheckboxCell(title = "c"), Theme())
    }

    // MARK: - ホストテーマからの視覚隔離

    @Test
    fun `ホストのテーマが変わってもタイトル既定色は変わらない`() {
        val lightHost = startHost(appCompatTheme)
        val lightColor = titleColorOf(lightHost)
        val lightHostAttr = resolveColor(lightHost, android.R.attr.textColorPrimary)
        controller?.close()

        val darkHost = startHost(appCompatDarkTheme)
        val darkColor = titleColorOf(darkHost)
        val darkHostAttr = resolveColor(darkHost, android.R.attr.textColorPrimary)

        // 観測が空振りしないことの確認: 2 つのホストは実際に異なる textColorPrimary を持つ。
        assertNotEquals(
            "検証に使う 2 つのホストテーマは textColorPrimary が異なる",
            lightHostAttr,
            darkHostAttr,
        )
        assertEquals("タイトル既定色はホストのテーマに追従しない", lightColor, darkColor)
    }

    /** [host] 上に LabelCell を 1 行表示し、そのタイトル文字色を返す。 */
    private fun titleColorOf(host: HostActivity): Int {
        val view = showRoot(
            host,
            SettingsRoot(sections = listOf(Section(id = "s", cells = listOf(LabelCell(title = "t"))))),
        )
        val holder = rowHolders(view).filterIsInstance<LabelCellViewHolder>().single()
        return holder.views.titleView.currentTextColor
    }

    @Test
    fun `ホストのテーマが変わっても SwitchCell の配色は変わらない`() {
        val lightHost = startHost(appCompatTheme)
        val lightTints = switchTintsOf(lightHost)
        val lightHostAttr = resolveColor(lightHost, AppCompatR.attr.colorPrimary)
        controller?.close()

        val darkHost = startHost(appCompatDarkTheme)
        val darkTints = switchTintsOf(darkHost)
        val darkHostAttr = resolveColor(darkHost, AppCompatR.attr.colorPrimary)

        assertNotEquals(
            "検証に使う 2 つのホストテーマは colorPrimary が異なる",
            lightHostAttr,
            darkHostAttr,
        )
        assertEquals("SwitchCell の配色はホストのテーマに追従しない", lightTints, darkTints)
    }

    /** [host] 上に SwitchCell を 1 行表示し、thumb / track の on / off 色を返す。 */
    private fun switchTintsOf(host: HostActivity): List<Int> {
        val view = showRoot(
            host,
            SettingsRoot(sections = listOf(Section(id = "s", cells = listOf(SwitchCell(title = "t"))))),
        )
        val holder = rowHolders(view).filterIsInstance<SwitchCellViewHolder>().single()
        val sw = holder.views.accessoryHolder.getChildAt(0) as MaterialSwitch
        val checked = intArrayOf(android.R.attr.state_checked)
        val unchecked = intArrayOf(-android.R.attr.state_checked)
        return listOf(
            sw.thumbTintList!!.getColorForState(checked, 0),
            sw.thumbTintList!!.getColorForState(unchecked, 0),
            sw.trackTintList!!.getColorForState(checked, 0),
            sw.trackTintList!!.getColorForState(unchecked, 0),
        )
    }

    @Test
    fun `ButtonCell のタイトル既定色はホストの colorPrimary に追従しない`() {
        val activity = startHost(appCompatTheme)
        val hostPrimary = resolveColor(activity, AppCompatR.attr.colorPrimary)
        assertNotEquals(
            "検証に使うホストテーマの colorPrimary は固定既定色と異なる",
            EffectiveStyle.SYSTEM_BLUE_ARGB,
            hostPrimary,
        )

        val view = showRoot(
            activity,
            SettingsRoot(sections = listOf(Section(id = "s", cells = listOf(ButtonCell(title = "b"))))),
        )
        val holder = rowHolders(view).filterIsInstance<ButtonCellViewHolder>().single()

        assertEquals(
            "ButtonCell のタイトル既定色は固定値",
            EffectiveStyle.SYSTEM_BLUE_ARGB,
            holder.views.titleView.currentTextColor,
        )
    }

    // MARK: - 利用者所有コンテンツはホストテーマのまま

    @Test
    fun `KsAnyView の利用者 View はホストの Context で解決される`() {
        val activity = startHost(appCompatTheme)
        val hostPrimary = resolveColor(activity, AppCompatR.attr.colorPrimary)

        var factoryContext: Context? = null
        val root = SettingsRoot(
            sections = listOf(
                Section(
                    id = "s",
                    header = SectionAccessory.View(
                        KsAnyView.AndroidView { ctx ->
                            factoryContext = ctx
                            View(ctx)
                        },
                    ),
                    cells = listOf(LabelCell(title = "t")),
                ),
            ),
        )
        val view = showRoot(activity, root)
        val libraryRowContext = rowHolders(view)
            .filterIsInstance<LabelCellViewHolder>().single().views.root.context

        val userContext = requireNotNull(factoryContext) { "利用者 View の factory が呼ばれていない" }
        assertEquals(
            "利用者 View はホストテーマの colorPrimary を解決する",
            hostPrimary,
            resolveColor(userContext, AppCompatR.attr.colorPrimary),
        )
        // 観測が空振りしないことの確認: ライブラリ所有の行は同梱テーマ側の値を解決している。
        assertNotEquals(
            "ライブラリ所有の行はホストとは別の colorPrimary を解決する",
            hostPrimary,
            resolveColor(libraryRowContext, AppCompatR.attr.colorPrimary),
        )
    }

    @Test
    fun `CustomCell の content はホストの Context で解決される`() {
        val activity = startHost(appCompatTheme)
        val hostPrimary = resolveColor(activity, AppCompatR.attr.colorPrimary)

        val driver = ComposeFrameDriver()
        frameDriver = driver
        driver.installOn(activity.container)

        var contentPrimary: Int? = null
        val builder: @Composable (String) -> Unit = { _ ->
            contentPrimary = resolveColor(LocalContext.current, AppCompatR.attr.colorPrimary)
        }
        // 行は Registry が張った factory 経由で生成させ、本番の Context 配線ごと観測する。
        val view = showRoot(
            activity,
            SettingsRoot(
                sections = listOf(
                    Section(
                        id = "s",
                        cells = listOf(
                            CustomCell(content = "x", builder = builder),
                            LabelCell(title = "t"),
                        ),
                    ),
                ),
            ),
        )
        driver.frame()
        val libraryRowContext = rowHolders(view)
            .filterIsInstance<LabelCellViewHolder>().single().views.root.context

        assertEquals(
            "CustomCell の content はホストテーマの colorPrimary を解決する",
            hostPrimary,
            contentPrimary,
        )
        // 観測が空振りしないことの確認: ライブラリ所有の行は同梱テーマ側の値を解決している。
        assertNotEquals(
            "ライブラリ所有の行はホストとは別の colorPrimary を解決する",
            hostPrimary,
            resolveColor(libraryRowContext, AppCompatR.attr.colorPrimary),
        )
    }

    // MARK: - 選択面のホストテーマ非依存

    @Test
    fun `非 Material3 テーマのホストでも選択面を提示して確定できる`() {
        val activity = startHost(frameworkTheme)
        var confirmed: Int? = null
        val view = showRoot(
            activity,
            SettingsRoot(
                sections = listOf(
                    Section(
                        id = "s",
                        cells = listOf(
                            NumberPickerCell(
                                title = "n",
                                min = 0,
                                max = 10,
                                value = 3,
                                onValueChanged = { confirmed = it },
                            ),
                        ),
                    ),
                ),
            ),
        )
        val holder = rowHolders(view).filterIsInstance<NumberPickerCellViewHolder>().single()

        holder.views.root.performClick()
        val sheet = ShadowDialog.getLatestDialog() as? NumberSelectionSheet
        assertNotNull("選択面が提示される", sheet)
        assertTrue("選択面が表示状態になる", sheet!!.isShowing)

        sheet.confirmView.performClick()
        assertEquals("確定操作で選択中の値が通知される", 3, confirmed)
    }

    @Test
    fun `非 Material3 テーマのホストで選択面を取消しても通知しない`() {
        val activity = startHost(frameworkTheme)
        var confirmed: Int? = null
        val view = showRoot(
            activity,
            SettingsRoot(
                sections = listOf(
                    Section(
                        id = "s",
                        cells = listOf(
                            NumberPickerCell(
                                title = "n",
                                min = 0,
                                max = 10,
                                value = 3,
                                onValueChanged = { confirmed = it },
                            ),
                        ),
                    ),
                ),
            ),
        )
        val holder = rowHolders(view).filterIsInstance<NumberPickerCellViewHolder>().single()

        holder.views.root.performClick()
        val sheet = ShadowDialog.getLatestDialog() as NumberSelectionSheet
        sheet.cancelView.performClick()

        assertEquals("取消では通知しない", null, confirmed)
        assertTrue("取消で選択面が閉じる", !sheet.isShowing)
    }
}
