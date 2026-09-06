package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.FragmentActivity
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/**
 * 未指定色が現在の外観（ライト / ダーク）の既定セットへ解決され、外観の変化に追随することの検証。
 *
 * 観測点は代理値ではなく実際に描画へ使われる値とする（`RecyclerView` の背景 drawable、
 * `ItemDecoration` が保持する Theme、行の実効スタイル、Section Header の TextView の色）。
 * Theme の内部フィールドだけを見ると、消費者へ配り忘れていても緑になる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ThemeAppearanceResolutionTest {

    /** `KsSettingsView` を 1 つだけ載せるホスト Activity。 */
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
        // qualifiers は VM 単位の状態なので、夜間へ倒したテストの影響を後続へ持ち越さない。
        RuntimeEnvironment.setQualifiers("notnight")
    }

    private val light = KsSettingsViewDefaults.lightTheme()
    private val dark = KsSettingsViewDefaults.darkTheme()

    private fun sampleRoot(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(
                id = "s1",
                header = SectionAccessory.Text("見出し"),
                footer = SectionAccessory.Text("説明"),
                cells = listOf(
                    LabelCell(id = "c1", title = "A", description = "説明文"),
                    ButtonCell(id = "c2", title = "B"),
                ),
            ),
        ),
    )

    /** [theme] を渡した View を表示し、レイアウトまで済ませて返す。 */
    private fun showView(theme: Theme = Theme(), root: SettingsRoot = sampleRoot()): KsSettingsView {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()
        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.bind(SettingsRootStore(initialRoot = root, initialTheme = theme))
        awaitConvergence(view) { view.internalMainListAdapter().currentList.isNotEmpty() }
        layout(view)
        return view
    }

    private fun layout(view: KsSettingsView) {
        val metrics = view.resources.displayMetrics
        val height = metrics.heightPixels * 3
        view.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, metrics.widthPixels, height)
        idle()
    }

    /** `RecyclerView` に実際に敷かれている背景色。 */
    private fun listBackground(view: KsSettingsView): Int =
        (view.internalRecyclerView().background as ColorDrawable).color

    /** 現在の `ItemDecoration` が保持している Theme。 */
    private fun decorationTheme(view: KsSettingsView): Theme =
        (view.internalCurrentDecoration() as ClassicSectionDecoration).theme

    /** 生成された行の ViewHolder を上から順に取り出す。 */
    private fun rowHolders(view: KsSettingsView): List<Any> {
        val rv = view.internalRecyclerView()
        return (0 until rv.childCount).map { rv.getChildViewHolder(rv.getChildAt(it)) }
    }

    /** Section Header の TextView（先頭行）。 */
    private fun headerTextView(view: KsSettingsView): TextView =
        rowHolders(view).filterIsInstance<SectionTextAccessoryViewHolder>()
            .first().itemView as TextView

    /** 表示中の LabelCell 行の実効スタイル。 */
    private fun labelRowStyle(view: KsSettingsView): EffectiveStyle {
        val holder = rowHolders(view).filterIsInstance<LabelCellViewHolder>().single()
        return EffectiveStyle.from(
            theme = view.internalTheme(),
            cellStyle = CellStyle(),
            darkTheme = holder.views.root.context.isKsDarkAppearance(),
        )
    }

    /** ButtonCell 行のタイトル文字色（View 経路の実描画値）。 */
    private fun buttonTitleColor(view: KsSettingsView): Int =
        rowHolders(view).filterIsInstance<ButtonCellViewHolder>().single()
            .buttonTextView.currentTextColor

    /** 夜間モードへ切り替え、表示中の View へ構成変更を届ける。 */
    private fun switchToNight(view: KsSettingsView) {
        RuntimeEnvironment.setQualifiers("+night")
        view.dispatchConfigurationChanged(view.resources.configuration)
        idle()
        layout(view)
    }

    // MARK: - 構築直後の装飾

    /**
     * `bind` も Full diff も通さず `applyDiff` だけで内容を入れる経路の装飾が解決済みであること。
     *
     * 装飾は構築時に一度だけ組み立てられ、その後は `style` の変更・Full diff・Theme 適用でしか
     * 組み直されない。構築時に未解決の Theme を掴むと、この経路だけ separator と Section の箱が
     * 透明で描かれる。
     */
    @Test
    @Config(qualifiers = "night")
    fun `applyDiff だけで内容を入れる View の装飾も解決済みの色を持つ`() {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val view = KsSettingsView(ctrl.get())
        ctrl.get().container.addView(view)

        view.applyDiff(
            SettingsRootDiff.InsertSection(
                index = 0,
                section = Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A"))),
            ),
        )
        idle()
        layout(view)

        val decoration = decorationTheme(view)
        assertEquals("separator は夜間の既定へ解決済み", dark.separatorColor, decoration.separatorColor)
        assertEquals("Cell 背景は夜間の既定へ解決済み", dark.cellBackgroundColor, decoration.cellBackgroundColor)
        assertEquals("list 下地は夜間の既定へ解決済み", dark.backgroundColor, decoration.backgroundColor)
    }

    // MARK: - 外観ごとの初期解決

    @Test
    @Config(qualifiers = "notnight")
    fun `Theme を渡さない View はライトの既定色で描画される`() {
        val view = showView()

        assertEquals(light.backgroundColor.toArgb(), listBackground(view))
        assertEquals(light.separatorColor, decorationTheme(view).separatorColor)
        assertEquals(light.cellBackgroundColor, decorationTheme(view).cellBackgroundColor)
        assertEquals(light.headerTextColor.toArgb(), headerTextView(view).currentTextColor)
        assertEquals(KsThemePalette.Light.cellTitle.toArgb(), labelRowStyle(view).titleColor)
        assertEquals(KsThemePalette.Light.cellDescription.toArgb(), labelRowStyle(view).descriptionColor)
    }

    @Test
    @Config(qualifiers = "night")
    fun `Theme を渡さない View は夜間モードでダークの既定色で描画される`() {
        val view = showView()

        assertEquals(dark.backgroundColor.toArgb(), listBackground(view))
        assertEquals(dark.separatorColor, decorationTheme(view).separatorColor)
        assertEquals(dark.cellBackgroundColor, decorationTheme(view).cellBackgroundColor)
        assertEquals(dark.headerTextColor.toArgb(), headerTextView(view).currentTextColor)
        assertEquals(KsThemePalette.Dark.cellTitle.toArgb(), labelRowStyle(view).titleColor)
        assertEquals(KsThemePalette.Dark.cellDescription.toArgb(), labelRowStyle(view).descriptionColor)
    }

    @Test
    @Config(qualifiers = "night")
    fun `一部だけ上書きした Theme は残りが夜間の既定へ追随する`() {
        val accent = Color(0xFF12AB34)
        val view = showView(Theme(cellAccentColor = accent))

        assertEquals("明示した accent は変わらない", accent.toArgb(), labelRowStyle(view).accentColor)
        assertEquals(dark.backgroundColor.toArgb(), listBackground(view))
        assertEquals(KsThemePalette.Dark.cellTitle.toArgb(), labelRowStyle(view).titleColor)
    }

    /**
     * factory を端末の外観と食い違う側で渡したときの実効値を契約として固定する。
     *
     * factory が値を入れる 10 色は渡した側のセットになり、factory が埋めないタイトルと説明文だけが
     * 端末側の外観の既定になる。この組み合わせは判読しにくいため、factory は端末の外観と同じ側を
     * 選んで渡す前提であることが `KsSettingsViewDefaults` の契約として書かれている。
     */
    @Test
    @Config(qualifiers = "night")
    fun `夜間に lightTheme を渡すとタイトルだけ夜間の既定になる`() {
        val view = showView(KsSettingsViewDefaults.lightTheme())

        assertEquals(
            "渡した light セットの Cell 背景がそのまま使われる",
            light.cellBackgroundColor.toArgb(),
            labelRowStyle(view).backgroundColor,
        )
        assertEquals(
            "タイトルは端末側の外観（夜間）の既定になる",
            KsThemePalette.Dark.cellTitle.toArgb(),
            labelRowStyle(view).titleColor,
        )
        assertEquals(
            "説明文も端末側の外観（夜間）の既定になる",
            KsThemePalette.Dark.cellDescription.toArgb(),
            labelRowStyle(view).descriptionColor,
        )
    }

    @Test
    @Config(qualifiers = "night")
    fun `利用者が読む Theme は解決前のまま`() {
        val accent = Color(0xFF12AB34)
        val view = showView(Theme(cellAccentColor = accent))

        assertEquals(accent, view.theme.cellAccentColor)
        assertEquals("accent 以外は未指定のまま", Color.Unspecified, view.theme.backgroundColor)
        assertEquals(Color.Unspecified, view.theme.cellTitleColor)
    }

    // MARK: - ButtonCell の既定は外観で選ばれ accent と独立

    @Test
    @Config(qualifiers = "notnight")
    fun `ButtonCell のタイトル既定はライトで accent を明示しても変わらない`() {
        val view = showView(Theme(cellAccentColor = Color(0xFF12AB34)))

        assertEquals(KsThemePalette.Light.buttonTitle.toArgb(), buttonTitleColor(view))
        assertNotEquals(Color(0xFF12AB34).toArgb(), buttonTitleColor(view))
    }

    @Test
    @Config(qualifiers = "night")
    fun `ButtonCell のタイトル既定は夜間で accent を明示しても変わらない`() {
        val view = showView(Theme(cellAccentColor = Color(0xFF12AB34)))

        assertEquals(KsThemePalette.Dark.buttonTitle.toArgb(), buttonTitleColor(view))
        assertNotEquals(Color(0xFF12AB34).toArgb(), buttonTitleColor(view))
    }

    @Test
    fun `ButtonCell のタイトル既定は Compose 経路と View 経路で一致する`() {
        for (darkTheme in listOf(false, true)) {
            val resolved = Theme().resolvedFor(darkTheme)
            val compose = EffectiveStyle.effectiveButtonTitleColor(
                buttonCellTitleColor = Color.Unspecified,
                cellStyle = CellStyle(),
                theme = resolved,
                darkTheme = darkTheme,
            )
            val argb = EffectiveStyle.effectiveButtonTitleColorArgb(
                buttonCellTitleColor = Color.Unspecified,
                cellStyle = CellStyle(),
                theme = resolved,
                darkTheme = darkTheme,
            )
            assertEquals("darkTheme=$darkTheme の 2 経路が一致", compose.toArgb(), argb)
            assertEquals(KsThemePalette.buttonTitle(darkTheme).toArgb(), argb)
        }
    }

    // MARK: - 夜間モードの変更で再解決する

    @Test
    @Config(qualifiers = "notnight")
    fun `表示中に夜間モードへ切り替わると未指定色が追随し identity は保たれる`() {
        val view = showView()
        assertEquals(light.backgroundColor.toArgb(), listBackground(view))
        val sectionIdsBefore = view.internalRoot().sections.map { it.id }
        val cellIdsBefore = view.internalRoot().sections.flatMap { s -> s.cells.map { it.id } }
        val decorationBefore = view.internalCurrentDecoration()

        switchToNight(view)

        assertEquals(dark.backgroundColor.toArgb(), listBackground(view))
        assertEquals(dark.separatorColor, decorationTheme(view).separatorColor)
        assertEquals(dark.headerTextColor.toArgb(), headerTextView(view).currentTextColor)
        assertEquals(KsThemePalette.Dark.cellTitle.toArgb(), labelRowStyle(view).titleColor)
        assertEquals("Section の identity は変わらない", sectionIdsBefore, view.internalRoot().sections.map { it.id })
        assertEquals(
            "Cell の identity は変わらない",
            cellIdsBefore,
            view.internalRoot().sections.flatMap { s -> s.cells.map { it.id } },
        )
        assertNotEquals("ItemDecoration は新 Theme で作り直される", decorationBefore, view.internalCurrentDecoration())
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `明示指定色は夜間モードへの切替後も変わらない`() {
        val explicitBackground = Color(0xFF102030)
        val view = showView(Theme(backgroundColor = explicitBackground))

        switchToNight(view)

        assertEquals(explicitBackground.toArgb(), listBackground(view))
        assertEquals("未指定の色だけが dark セットへ変わる", dark.separatorColor, decorationTheme(view).separatorColor)
    }

    /**
     * 利用者が `CellStyle` に明示した色は、ライブラリが外観の既定へ置き換えない。
     *
     * 同じ行の未指定の色（Cell 背景）だけが dark 既定へ再解決されることを同時に観測して、
     * 「切替そのものが起きていないから変わらなかった」という空振りを弾く。
     */
    @Test
    @Config(qualifiers = "notnight")
    fun `明示した CellStyle 色は夜間モードへの切替後も変わらない`() {
        val explicitTitle = Color(0xFF3366CC)
        val root = SettingsRoot(
            sections = listOf(
                Section(
                    id = "s1",
                    cells = listOf(
                        LabelCell(id = "styled", title = "明示", style = CellStyle(titleColor = explicitTitle)),
                        LabelCell(id = "plain", title = "未指定"),
                    ),
                ),
            ),
        )
        val view = showView(root = root)
        assertEquals(explicitTitle.toArgb(), labelTitleColor(view, "明示"))
        assertEquals(KsThemePalette.Light.cellTitle.toArgb(), labelTitleColor(view, "未指定"))

        switchToNight(view)

        assertEquals("明示した title 色は変わらない", explicitTitle.toArgb(), labelTitleColor(view, "明示"))
        assertEquals(
            "未指定の title 色だけが dark 既定へ変わる",
            KsThemePalette.Dark.cellTitle.toArgb(),
            labelTitleColor(view, "未指定"),
        )
    }

    /**
     * Cell 固有値として明示した accent も外観の切替で置き換えない。
     *
     * 観測点は Switch のオン Track（実効 accent がそのまま出る位置）と、同じ行の背景
     * （未指定なので dark 既定へ変わる）。
     */
    @Test
    @Config(qualifiers = "notnight")
    fun `明示した Cell 固有色は夜間モードへの切替後も変わらない`() {
        val explicitAccent = Color(0xFFCC3366)
        val root = SettingsRoot(
            sections = listOf(
                Section(
                    id = "s1",
                    cells = listOf(
                        SwitchCell(id = "sw", title = "通知", isOn = true, accentColor = explicitAccent),
                    ),
                ),
            ),
        )
        val view = showView(root = root)
        assertEquals(explicitAccent.toArgb(), switchOnTrackColor(view))
        assertEquals(light.cellBackgroundColor.toArgb(), switchRowBackgroundColor(view))

        switchToNight(view)

        assertEquals("明示した accent は変わらない", explicitAccent.toArgb(), switchOnTrackColor(view))
        assertEquals(
            "未指定の行背景だけが dark 既定へ変わる",
            dark.cellBackgroundColor.toArgb(),
            switchRowBackgroundColor(view),
        )
    }

    /** [title] を表示している LabelCell 行のタイトル文字色（実描画値）。 */
    private fun labelTitleColor(view: KsSettingsView, title: String): Int {
        val holder = rowHolders(view).filterIsInstance<LabelCellViewHolder>()
            .single { it.views.titleView.text == title }
        return holder.views.titleView.currentTextColor
    }

    /** SwitchCell 行のオン状態 Track 色（実効 accent がそのまま出る位置）。 */
    private fun switchOnTrackColor(view: KsSettingsView): Int {
        val holder = rowHolders(view).filterIsInstance<SwitchCellViewHolder>().single()
        val sw = requireNotNull(findMaterialSwitch(holder.itemView as ViewGroup)) {
            "MaterialSwitch が行に見つからない"
        }
        return sw.trackTintList!!.getColorForState(intArrayOf(android.R.attr.state_checked), 0)
    }

    /** SwitchCell 行の背景色（未指定なら外観の既定が出る位置）。 */
    private fun switchRowBackgroundColor(view: KsSettingsView): Int {
        val holder = rowHolders(view).filterIsInstance<SwitchCellViewHolder>().single()
        val ripple = holder.itemView.background as android.graphics.drawable.RippleDrawable
        return (ripple.getDrawable(0) as ColorDrawable).color
    }

    private fun findMaterialSwitch(root: ViewGroup): MaterialSwitch? {
        for (index in 0 until root.childCount) {
            when (val child: View = root.getChildAt(index)) {
                is MaterialSwitch -> return child
                is ViewGroup -> findMaterialSwitch(child)?.let { return it }
                else -> Unit
            }
        }
        return null
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `構造更新を挟んでも未指定色は夜間へ追随する`() {
        val accent = Color(0xFF12AB34)
        val view = showView(Theme(cellAccentColor = accent))

        // Full diff → Section 置換 → 可視性変更 の順に構造更新を通す。
        view.applyDiff(jp.kamusoft.kssettingsview.core.SettingsRootDiff.Full(sampleRoot()))
        idle()
        view.applyDiff(
            jp.kamusoft.kssettingsview.core.SettingsRootDiff.ReplaceSection(
                sectionId = "s1",
                newSection = sampleRoot().sections.single().copy(header = SectionAccessory.Text("差し替え見出し")),
            ),
        )
        idle()
        view.applyDiff(
            jp.kamusoft.kssettingsview.core.SettingsRootDiff.ReplaceCell(
                cellId = "c1",
                newCell = LabelCell(id = "c1", title = "A", description = "説明文", isVisible = false),
            ),
        )
        idle()
        layout(view)

        assertEquals("構造更新で利用者 Theme は書き戻されない", Color.Unspecified, view.theme.backgroundColor)

        switchToNight(view)

        assertEquals(accent, view.theme.cellAccentColor)
        assertEquals(dark.backgroundColor.toArgb(), listBackground(view))
        assertEquals(dark.separatorColor, decorationTheme(view).separatorColor)
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `夜間モードが変わらない構成変更では Theme を再適用しない`() {
        val view = showView()
        val decorationBefore = view.internalCurrentDecoration()

        // 画面向きだけを変える（夜間モードは据え置き）。
        val rotated = android.content.res.Configuration(view.resources.configuration).apply {
            orientation = android.content.res.Configuration.ORIENTATION_LANDSCAPE
        }
        view.dispatchConfigurationChanged(rotated)
        idle()

        assertSame("ItemDecoration は作り直されない", decorationBefore, view.internalCurrentDecoration())
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `attach 時に解決時と外観が違えば再解決する`() {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()

        // attach 前にライトで構築する。Store は bind しない（attach 時の Store 再同期が
        // 解決し直す経路に相乗りせず、attach 時の照合そのものを観測するため）。
        val view = KsSettingsView(activity)
        view.applyDiff(jp.kamusoft.kssettingsview.core.SettingsRootDiff.Full(sampleRoot()))
        idle()
        assertEquals(light.backgroundColor.toArgb(), listBackground(view))

        // attach 前に外観だけが夜間へ変わる（構成変更の通知は View へ届かない）。
        RuntimeEnvironment.setQualifiers("+night")
        activity.container.addView(view)
        idle()

        assertEquals(dark.backgroundColor.toArgb(), listBackground(view))
        assertEquals(dark.separatorColor, decorationTheme(view).separatorColor)
    }

    // MARK: - 選択面とカレンダーの選択面

    @Test
    @Config(qualifiers = "night")
    fun `選択面の配色は夜間の既定セットになる`() {
        val view = showView()
        val resolved = view.internalTheme()
        val effective = EffectiveStyle.from(resolved, CellStyle(), darkTheme = true)

        val style = PickerSheetStyle.from(
            cell = PickerCell(title = "選択", items = listOf("A", "B")),
            theme = resolved,
            effective = effective,
        )

        assertEquals(dark.cellBackgroundColor.toArgb(), style.sheetBackgroundColor)
        assertEquals(dark.separatorColor.toArgb(), style.separatorColor)
        assertEquals(dark.selectedColor.toArgb(), style.rippleColor)
        assertEquals(dark.backgroundColor.toArgb(), style.onAccentTextColor)
        assertEquals(dark.cellAccentColor.toArgb(), style.accentColor)
        assertEquals(KsThemePalette.Dark.cellTitle.toArgb(), style.itemTextColor)
        assertEquals(KsThemePalette.Dark.cellDescription.toArgb(), style.itemSubTextColor)
    }

    @Test
    @Config(qualifiers = "night")
    fun `カレンダーの選択面の配色は夜間の既定セットになる`() {
        val view = showView()
        val resolved = view.internalTheme()
        val effective = EffectiveStyle.from(resolved, CellStyle(), darkTheme = true)

        val colors = resolveDatePickerDialogColors(
            cell = DatePickerCell(title = "日付"),
            theme = resolved,
            effective = effective,
        )

        assertEquals(dark.backgroundColor.toArgb(), colors.background)
        assertEquals(dark.cellAccentColor.toArgb(), colors.accent)
        assertEquals(KsThemePalette.Dark.cellTitle.toArgb(), colors.text)
    }

    /**
     * 表示中の選択面は開いた時点の色のままだが、閉じて開き直すと新しい外観の色で描かれる。
     *
     * 選択面は開くたびに現在の解決済み Theme からスタイルを組み立てるため、開き直しの観測点は
     * 「切替後に組み立て直したスタイル」になる。
     */
    @Test
    @Config(qualifiers = "notnight")
    fun `選択面は開き直すと新しい外観の配色になる`() {
        val view = showView()
        val cell = PickerCell(title = "選択", items = listOf("A", "B"))

        fun currentSheetStyle(darkTheme: Boolean) = PickerSheetStyle.from(
            cell = cell,
            theme = view.internalTheme(),
            effective = EffectiveStyle.from(view.internalTheme(), CellStyle(), darkTheme),
        )

        assertEquals(light.cellBackgroundColor.toArgb(), currentSheetStyle(false).sheetBackgroundColor)

        switchToNight(view)

        assertEquals(dark.cellBackgroundColor.toArgb(), currentSheetStyle(true).sheetBackgroundColor)
        assertEquals(KsThemePalette.Dark.cellTitle.toArgb(), currentSheetStyle(true).itemTextColor)
    }

    /**
     * 表示中に開いたままの選択面は、外観が切り替わっても開いた時点の配色のまま描かれる。
     *
     * 選択面はシート内の実描画色（ヘッダーのタイトル文字色とシート背景）で観測する。開き直し側の
     * 追随は [`選択面を開き直すとテーマ属性も夜間側で解決される`] が担う。
     */
    @Test
    @Config(qualifiers = "notnight")
    fun `開いたままの選択面は外観が切り替わっても再配色されない`() {
        val view = showView(root = pickerRoot())
        val sheet = openPickerSheet(view)
        val openedTitleColor = sheet.titleView.currentTextColor
        val openedSurface = sheetSurfaceTint(sheet)

        switchToNight(view)

        assertEquals(
            "開いたままのシートのタイトル色は開いた時点の値のまま",
            openedTitleColor,
            sheet.titleView.currentTextColor,
        )
        assertEquals(
            "開いたままのシートの面の色は開いた時点の値のまま",
            openedSurface,
            sheetSurfaceTint(sheet),
        )
        // 観測が空振りしていないことの確認: 切替後に組み立て直したスタイルは夜間側になっている。
        assertNotEquals(
            openedSurface,
            PickerSheetStyle.from(
                cell = PickerCell(title = "選択", items = listOf("A", "B")),
                theme = view.internalTheme(),
                effective = EffectiveStyle.from(view.internalTheme(), CellStyle(), darkTheme = true),
            ).sheetBackgroundColor,
        )
    }

    /**
     * 選択面を閉じて外観の切替後に開き直すと、同梱テーマの属性も現在の外観で解決される。
     *
     * 明示的に塗る色（[PickerSheetStyle] 由来）だけでなく、Material のシート内部要素が引く
     * テーマ属性まで追随することを観測点に取る。行の Context は同梱テーマをかぶせたラッパであり、
     * ラッパは生成時のテーマを保持するため、外観が変わったらラップ元から作り直される必要がある。
     */
    @Test
    @Config(qualifiers = "notnight")
    fun `選択面を開き直すとテーマ属性も夜間側で解決される`() {
        val view = showView(root = pickerRoot())

        val lightSheet = openPickerSheet(view)
        val lightSurface = surfaceColorOf(lightSheet.context)
        lightSheet.dismiss()
        idle()

        switchToNight(view)

        val nightSurface = surfaceColorOf(openPickerSheet(view).context)

        assertEquals(
            "開き直した選択面は現在の外観の同梱テーマで解決される",
            surfaceColorOf(view.context.ksThemedContext()),
            nightSurface,
        )
        // 観測が空振りしていないことの確認: 2 つの外観は実際に異なる colorSurface を持つ。
        assertNotEquals("ライトと夜間で colorSurface が異なる", lightSurface, nightSurface)
    }

    /**
     * 選択面・カレンダーの選択面が共通で通る Context 解決が、外観の切替後に作り直されること。
     *
     * シートとダイアログはいずれも行の Context に `ksThemedContext()` を掛けて生成する。行の
     * Context は既に同梱テーマをかぶせたラッパであり、素通しすると生成時の外観のテーマが残る。
     */
    @Test
    @Config(qualifiers = "notnight")
    fun `行の Context から得る同梱テーマ付き Context は外観の切替後に作り直される`() {
        val view = showView(root = datePickerRoot())
        val rowContext = rowHolders(view).filterIsInstance<DatePickerCellViewHolder>()
            .single().views.root.context
        val lightSurface = surfaceColorOf(rowContext.ksThemedContext())

        switchToNight(view)

        val nightSurface = surfaceColorOf(rowContext.ksThemedContext())

        assertEquals(
            "行の Context 由来でも現在の外観の同梱テーマで解決される",
            surfaceColorOf(view.context.ksThemedContext()),
            nightSurface,
        )
        assertNotEquals("ライトと夜間で colorSurface が異なる", lightSurface, nightSurface)
    }

    /**
     * placeholder 色をどの段にも指定しない行の hint 色が、外観の切替後に引き直されること。
     *
     * この段はライブラリ既定ではなくホストテーマの `android:textColorHint` へ委ねる契約であり、
     * 委ね先は「現在の外観の」値でなければならない。行の View は作り直されないため、生成時の
     * 値を持ち続けると暗い地に暗い placeholder が残る。
     */
    @Test
    @Config(qualifiers = "notnight")
    fun `placeholder 未指定の行の hint 色は夜間モードへの切替後に引き直される`() {
        val view = showView(root = entryRoot())
        val editText = rowHolders(view).filterIsInstance<EntryCellViewHolder>().single().editText
        val lightHint = editText.hintTextColors.defaultColor

        switchToNight(view)

        val nightHint = editText.hintTextColors.defaultColor
        assertEquals(
            "切替後の hint 色は現在の外観の同梱テーマが解決する textColorHint",
            hintTextColorOf(view.context.ksThemedContext()),
            nightHint,
        )
        // 観測が空振りしていないことの確認: 2 つの外観は実際に異なる textColorHint を持つ。
        assertNotEquals("ライトと夜間で textColorHint が異なる", lightHint, nightHint)
    }

    /**
     * `SwitchCell` のオフ色が、外観の切替後に現在の外観のテーマ attr から導出し直されること。
     *
     * オフ色は accent の色相を載せつつ明度をテーマ attr（`colorSurfaceContainerHighest` /
     * `colorOutline`）から取る（android/ADR-0017）。attr の解決元が古いままだと、ダークで
     * ライトの明度のまま描かれる。track は明度そのものが土台の attr と一致することで観測する。
     */
    @Test
    @Config(qualifiers = "notnight")
    fun `Switch のオフ色は夜間モードへの切替後に引き直される`() {
        val view = showView(root = switchRoot())
        val switchView = switchViewOf(view)
        val lightOffTrack = offColorOf(switchView.trackTintList)
        val lightOffThumb = offColorOf(switchView.thumbTintList)

        switchToNight(view)

        val nightOffTrack = offColorOf(switchView.trackTintList)
        assertEquals(
            "オフ track の明度は現在の外観の colorSurfaceContainerHighest から取られる",
            lightnessOf(surfaceContainerHighestOf(view.context.ksThemedContext())),
            lightnessOf(nightOffTrack),
            0.001f,
        )
        // 観測が空振りしていないことの確認: 2 つの外観でオフ色が実際に変わる。
        assertNotEquals("ライトと夜間でオフ track の色が異なる", lightOffTrack, nightOffTrack)
        assertNotEquals(
            "ライトと夜間でオフ thumb の色が異なる",
            lightOffThumb,
            offColorOf(switchView.thumbTintList),
        )
    }

    /** [EntryCell] 1 つだけの root（placeholder 色はどの段にも指定しない）。 */
    private fun entryRoot(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(
                id = "s1",
                cells = listOf(EntryCell(id = "e1", title = "名前", placeholder = "入力")),
            ),
        ),
    )

    /** [SwitchCell] 1 つだけの root。 */
    private fun switchRoot(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(id = "s1", cells = listOf(SwitchCell(id = "w1", title = "通知"))),
        ),
    )

    /** 表示中の SwitchCell 行に載っているスイッチ本体。 */
    private fun switchViewOf(view: KsSettingsView): MaterialSwitch =
        rowHolders(view).filterIsInstance<SwitchCellViewHolder>().single()
            .views.accessoryHolder.getChildAt(0) as MaterialSwitch

    /** state_checked を含まない状態（= オフ）で選ばれる色。 */
    private fun offColorOf(colors: ColorStateList?): Int =
        colors!!.getColorForState(IntArray(0), 0)

    /** [context] のテーマが解決する `android:textColorHint` の既定色。 */
    private fun hintTextColorOf(context: Context): Int {
        val attrs = context.obtainStyledAttributes(intArrayOf(android.R.attr.textColorHint))
        return try {
            attrs.getColorStateList(0)!!.defaultColor
        } finally {
            attrs.recycle()
        }
    }

    /** [context] のテーマが解決する `colorSurfaceContainerHighest`。 */
    private fun surfaceContainerHighestOf(context: Context): Int = MaterialColors.getColor(
        context,
        MaterialR.attr.colorSurfaceContainerHighest,
        android.graphics.Color.LTGRAY,
    )

    /** HSL の明度成分。 */
    private fun lightnessOf(color: Int): Float {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        return hsl[2]
    }

    /** [PickerCell] 1 つだけの root。 */
    private fun pickerRoot(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(
                id = "s1",
                cells = listOf(PickerCell(id = "p1", title = "選択", items = listOf("A", "B"))),
            ),
        ),
    )

    /** [DatePickerCell] 1 つだけの root。 */
    private fun datePickerRoot(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(id = "s1", cells = listOf(DatePickerCell(id = "d1", title = "日付"))),
        ),
    )

    /** 行をタップして選択面を提示し、提示されたシートを返す。 */
    private fun openPickerSheet(view: KsSettingsView): PickerSelectionSheet {
        rowHolders(view).filterIsInstance<PickerCellViewHolder>().single().views.root.performClick()
        idle()
        return ShadowDialog.getLatestDialog() as PickerSelectionSheet
    }

    /** [context] のテーマが解決する `colorSurface`。 */
    private fun surfaceColorOf(context: Context): Int {
        val tv = TypedValue()
        context.theme.resolveAttribute(MaterialR.attr.colorSurface, tv, true)
        return if (tv.resourceId != 0) ContextCompat.getColor(context, tv.resourceId) else tv.data
    }

    /** シート面（`design_bottom_sheet` の container）に実際に乗っている色。 */
    private fun sheetSurfaceTint(sheet: PickerSelectionSheet): Int =
        sheet.findViewById<View>(MaterialR.id.design_bottom_sheet)!!.backgroundTintList!!.defaultColor

    // MARK: - 実値変換点に Unspecified を残さない

    @Test
    @Config(qualifiers = "night")
    fun `解決済み Theme は実値変換される色をすべて指定済みで持つ`() {
        val view = showView()
        val resolved = view.internalTheme()
        val converted = listOf(
            "backgroundColor" to resolved.backgroundColor,
            "cellBackgroundColor" to resolved.cellBackgroundColor,
            "separatorColor" to resolved.separatorColor,
            "selectedColor" to resolved.selectedColor,
            "cellAccentColor" to resolved.cellAccentColor,
            "disabledTextColor" to resolved.disabledTextColor,
            "headerTextColor" to resolved.headerTextColor,
            "headerBackgroundColor" to resolved.headerBackgroundColor,
            "footerTextColor" to resolved.footerTextColor,
            "footerBackgroundColor" to resolved.footerBackgroundColor,
        )
        for ((name, value) in converted) {
            assertNotEquals("$name が Unspecified のまま ARGB へ落ちる", Color.Unspecified, value)
        }
    }

    @Test
    @Config(qualifiers = "night")
    fun `実効スタイルの全色は Unspecified の ARGB 変換にならない`() {
        val view = showView()
        val style = labelRowStyle(view)
        val unspecifiedArgb = Color.Unspecified.toArgb()
        val colors = listOf(
            "titleColor" to style.titleColor,
            "descriptionColor" to style.descriptionColor,
            "backgroundColor" to style.backgroundColor,
            "selectedColor" to style.selectedColor,
            "accentColor" to style.accentColor,
            "valueTextColor" to style.valueTextColor,
            "hintTextColor" to style.hintTextColor,
            "disabledTextColor" to style.disabledTextColor,
        )
        for ((name, value) in colors) {
            assertNotEquals("$name が Unspecified の ARGB 変換になっている", unspecifiedArgb, value)
        }
    }

    /**
     * sentinel を理解する経路（placeholder）は例外として、未指定が後段へ落ちることを確かめる。
     *
     * placeholder はライブラリ既定色を持たず、未指定のときホストテーマの hint 色をそのまま使う。
     * ここで既定セットの値が入ってしまうと、その契約が壊れる。
     */
    @Test
    @Config(qualifiers = "night")
    fun `placeholder は解決後も未指定でホスト既定へ委ねる`() {
        val view = showView()
        assertEquals(Color.Unspecified, view.internalTheme().cellPlaceholderColor)
        assertEquals(
            Color.Unspecified,
            EffectiveStyle.effectivePlaceholderColor(CellStyle(), view.internalTheme()),
        )
    }

    // MARK: - Section 装飾の枠線

    @Test
    @Config(qualifiers = "night")
    fun `未指定の枠線色は解決後も透明として描かれる`() {
        val view = showView(Theme(sectionBorderWidth = androidx.compose.ui.unit.Dp(2.0f)))
        view.style = KsSettingsViewStyle.Modern
        layout(view)

        assertEquals(Color.Unspecified, view.internalTheme().sectionBorderColor)
        val metrics = SectionBoxMetrics.resolve(
            theme = view.internalTheme(),
            style = KsSettingsViewStyle.Modern,
            density = view.resources.displayMetrics.density,
            layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
        )
        assertEquals(Color.Transparent.toArgb(), metrics.borderColor)
    }
}
