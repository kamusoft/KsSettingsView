package jp.kamusoft.kssettingsview.ui

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import jp.kamusoft.kssettingsview.R
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import java.time.LocalDate

/**
 * Activity 再生成（画面回転など）をまたいだカレンダー選択面の表示継続を検証する（android/ADR-0019）。
 *
 * 選択面は `DialogFragment` ではないため、Activity と共に破棄される。継続は [KsSettingsView] が
 * View 階層のインスタンス状態として (対象 Cell の id・選択日・表示月・表示モード) を保存し、
 * 再生成後に同一 id の [DatePickerCell]（[DatePickerUIStyle.Material]）が現 root にちょうど 1 つ
 * あるときだけ提示し直すことで成立する。対応付けできないときは提示せず、どの Cell へも値を
 * 書き込まない。
 *
 * 選択面は `KsSettingsView` の行タップ（RecyclerView が生成した実際の行）から開き、行から
 * 保存・復元までの配線ごと検証する。ホストは `FragmentActivity` ではない [ComponentActivity] で、
 * XML テーマも Material3 派生にしない（android/ADR-0020）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ja")
class DateCalendarRecreationTest {

    /**
     * `KsSettingsView` を自前で組み立てるホスト Activity。
     *
     * 再生成後も同じ手順で組み直されるよう、View の生成と root の反映を `onCreate` に置く。
     * 生成内容はコンパニオンの設定値から読む（テストごとに差し替える）。
     */
    class HostActivity : ComponentActivity() {

        lateinit var container: FrameLayout

        override fun onCreate(savedInstanceState: Bundle?) {
            // Material3 派生ではないフレームワーク標準テーマ。
            setTheme(android.R.style.Theme_Material_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            container = FrameLayout(this)
            setContentView(container)
            settingsViews = rootsProvider().mapIndexed { index, root ->
                KsSettingsView(this).also { view ->
                    explicitViewIds.getOrNull(index)?.let { view.id = it }
                    view.restoreTodayProvider = todayProvider
                    container.addView(view)
                    view.setRootDirect(root, hostTheme)
                }
            }
        }

        companion object {
            /** `onCreate` で組み立てる `KsSettingsView` の数と、それぞれに反映する root。 */
            var rootsProvider: () -> List<SettingsRoot> = { listOf(SettingsRoot()) }

            /** ホストが明示的に与える View の ID（並びは [rootsProvider] と対応する）。 */
            var explicitViewIds: List<Int> = emptyList()

            /** 各 `KsSettingsView` に反映する Theme。 */
            var hostTheme: Theme = Theme()

            /** 復元した選択面の「今日」操作が参照する今日。 */
            var todayProvider: () -> LocalDate = { FIXED_TODAY }

            /** 直近の `onCreate` で組み立てた View 群。 */
            var settingsViews: List<KsSettingsView> = emptyList()
        }
    }

    private var controller: ActivityController<HostActivity>? = null

    @Before
    fun resetHostConfiguration() {
        HostActivity.rootsProvider = { listOf(SettingsRoot()) }
        HostActivity.explicitViewIds = emptyList()
        HostActivity.hostTheme = Theme()
        HostActivity.todayProvider = { FIXED_TODAY }
        HostActivity.settingsViews = emptyList()
    }

    @After
    fun tearDown() {
        controller?.close()
        controller = null
    }

    // MARK: - 構成変更をまたぐ状態維持

    @Test
    fun `再生成をまたいで選択日と表示月と表示モードを維持して提示し直す`() {
        val notified = mutableListOf<LocalDate>()
        val ctrl = launch(rootOf(dateCell()))
        val dialog = openDialog(HostActivity.settingsViews.single())
        moveTo(dialog, selected = PICKED_DATE, month = OTHER_MONTH, textInput = true)
        // 再生成前の状態が初期値と区別できることを先に確かめる（維持の検証が空振りしないように）。
        assertEquals(PICKED_DATE, selectedDate(dialog))
        assertEquals(OTHER_MONTH, displayedMonth(dialog))

        recreate(ctrl, rootOf(dateCell(onValueChanged = { notified.add(it) })))

        val restored = requireNotNull(shownDialog()) { "再生成後に選択面が提示されていない" }
        assertEquals(PICKED_DATE, selectedDate(restored))
        assertEquals(OTHER_MONTH, displayedMonth(restored))
        assertEquals(DisplayMode.Input, restored.state.displayMode)
        assertTrue("再生成そのもので通知された: $notified", notified.isEmpty())
    }

    @Test
    fun `再生成後の選択面の確定は維持した選択日を1回だけ通知する`() {
        val notified = mutableListOf<LocalDate>()
        val ctrl = launch(rootOf(dateCell()))
        moveTo(openDialog(HostActivity.settingsViews.single()), selected = PICKED_DATE)

        recreate(ctrl, rootOf(dateCell(onValueChanged = { notified.add(it) })))
        val restored = requireNotNull(shownDialog())
        restored.confirmSelection()

        assertEquals(listOf(PICKED_DATE), notified)
        assertFalse("確定後も選択面が開いている", restored.isShowing)
    }

    @Test
    fun `再生成後の選択面でも取消では通知しない`() {
        val notified = mutableListOf<LocalDate>()
        val ctrl = launch(rootOf(dateCell()))
        moveTo(openDialog(HostActivity.settingsViews.single()), selected = PICKED_DATE)

        recreate(ctrl, rootOf(dateCell(onValueChanged = { notified.add(it) })))
        val restored = requireNotNull(shownDialog())
        restored.cancel()

        assertTrue("非確定の閉じ方で通知された: $notified", notified.isEmpty())
        assertFalse(restored.isShowing)
    }

    @Test
    fun `再生成後の選択面でも今日ジャンプが成立し通知しない`() {
        val notified = mutableListOf<LocalDate>()
        val ctrl = launch(rootOf(dateCell(todayText = "今日")))
        moveTo(openDialog(HostActivity.settingsViews.single()), selected = PICKED_DATE, month = OTHER_MONTH)

        recreate(ctrl, rootOf(dateCell(todayText = "今日", onValueChanged = { notified.add(it) })))
        val restored = requireNotNull(shownDialog())
        assertEquals("今日操作が提示されていない", "今日", restored.todayText)
        restored.jumpToToday()

        assertEquals(FIXED_TODAY, selectedDate(restored))
        assertEquals(FIXED_TODAY.withDayOfMonth(1), displayedMonth(restored))
        assertTrue("今日ジャンプで通知された: $notified", notified.isEmpty())
        assertTrue("今日ジャンプで選択面が閉じている", restored.isShowing)
    }

    @Test
    fun `再生成後の選択面は提示時と同じ色ロールで組み立てられる`() {
        HostActivity.hostTheme = Theme(
            backgroundColor = Color(THEME_BACKGROUND),
            cellAccentColor = Color(THEME_ACCENT),
            cellTitleColor = Color(THEME_TEXT),
        )
        val ctrl = launch(rootOf(dateCell()))
        val before = openDialog(HostActivity.settingsViews.single())
        assertEquals("提示時の色ロールがホストの Theme を反映していない", THEME_ACCENT, before.colors.accent)

        recreate(ctrl, rootOf(dateCell()))

        val restored = requireNotNull(shownDialog())
        assertEquals("復元後の色ロールが提示時とずれている", before.colors, restored.colors)
    }

    @Test
    fun `再生成後の選択面は現 Cell の範囲制限を反映する`() {
        val ctrl = launch(rootOf(dateCell()))
        moveTo(openDialog(HostActivity.settingsViews.single()), selected = PICKED_DATE)

        // 再生成後の Cell は選択日より後を下限にしている。範囲外の状態のまま提示しない。
        recreate(ctrl, rootOf(dateCell(minDate = NARROWED_MIN, maxDate = NARROWED_MAX)))

        val restored = requireNotNull(shownDialog())
        assertEquals(NARROWED_MIN, selectedDate(restored))
        assertFalse(restored.state.selectableDates.isSelectableDate(PICKED_DATE.toEpochMilliUtc()))
    }

    @Test
    fun `選択面を開いていなければ再生成後に提示されない`() {
        val ctrl = launch(rootOf(dateCell()))

        recreate(ctrl, rootOf(dateCell()))

        assertNull("開いていないのに選択面が提示された", shownDialog())
    }

    @Test
    fun `状態保存だけで破棄が続かなければ選択面は開いたまま選択も保つ`() {
        val notified = mutableListOf<LocalDate>()
        val ctrl = launch(rootOf(dateCell(onValueChanged = { notified.add(it) })))
        val dialog = openDialog(HostActivity.settingsViews.single())
        moveTo(dialog, selected = PICKED_DATE, month = OTHER_MONTH)
        assertTrue("前提: 選択面が表示状態になっていない", dialog.isShowing)

        // ホーム画面や他アプリへ移るときも状態保存だけが起こる（Activity は破棄されない）。
        ctrl.saveInstanceState(Bundle())

        assertTrue("状態保存だけで選択面が閉じられた", dialog.isShowing)
        assertEquals("状態保存で選択日が失われた", PICKED_DATE, selectedDate(dialog))
        assertEquals("状態保存で表示月が失われた", OTHER_MONTH, displayedMonth(dialog))
        assertTrue("状態保存で通知された: $notified", notified.isEmpty())
    }

    @Test
    fun `ホストの破棄をまたぐときは再生成前の選択面が閉じられ通知しない`() {
        val notified = mutableListOf<LocalDate>()
        val ctrl = launch(rootOf(dateCell(onValueChanged = { notified.add(it) })))
        val dialog = openDialog(HostActivity.settingsViews.single())
        moveTo(dialog, selected = PICKED_DATE)
        assertTrue("前提: 選択面が表示状態になっていない", dialog.isShowing)

        recreate(ctrl, rootOf(dateCell(onValueChanged = { notified.add(it) })))

        assertFalse("再生成前の選択面が開いたまま残っている", dialog.isShowing)
        assertTrue("閉じる際に通知された: $notified", notified.isEmpty())
    }

    // MARK: - 対応付け不能時のフォールバック

    @Test
    fun `該当 id が現 root に無ければ再提示せず誤発火しない`() {
        val notified = mutableListOf<String>()
        val ctrl = launch(rootOf(dateCell()))
        moveTo(openDialog(HostActivity.settingsViews.single()), selected = PICKED_DATE)

        recreate(
            ctrl,
            rootOf(
                dateCell(id = "other-a", onValueChanged = { notified.add("a") }),
                dateCell(id = "other-b", onValueChanged = { notified.add("b") }),
            ),
        )

        assertNull("対応付けできないのに選択面が提示された", shownDialog())
        assertEquals(emptyList<String>(), notified)
    }

    @Test
    fun `uiStyle が変更されていたら再提示しない`() {
        val notified = mutableListOf<LocalDate>()
        val ctrl = launch(rootOf(dateCell()))
        moveTo(openDialog(HostActivity.settingsViews.single()), selected = PICKED_DATE)

        recreate(
            ctrl,
            rootOf(dateCell(uiStyle = DatePickerUIStyle.Spinner, onValueChanged = { notified.add(it) })),
        )

        assertNull("同型でないのに選択面が提示された", shownDialog())
        assertEquals(emptyList<LocalDate>(), notified)
    }

    @Test
    fun `同一 id の候補が複数なら再提示しない`() {
        val notified = mutableListOf<LocalDate>()
        val ctrl = launch(rootOf(dateCell()))
        moveTo(openDialog(HostActivity.settingsViews.single()), selected = PICKED_DATE)

        recreate(
            ctrl,
            SettingsRoot(
                sections = listOf(
                    Section(id = "s1", cells = listOf(dateCell(onValueChanged = { notified.add(it) }))),
                    Section(id = "s2", cells = listOf(dateCell(onValueChanged = { notified.add(it) }))),
                ),
            ),
        )

        assertNull("候補が一意でないのに選択面が提示された", shownDialog())
        assertEquals(emptyList<LocalDate>(), notified)
    }

    // MARK: - View の ID と復元の成立条件

    @Test
    fun `ID 未設定の KsSettingsView はライブラリ既定の ID を自分へ付ける`() {
        val view = KsSettingsView(ApplicationProvider.getApplicationContext())

        assertEquals(R.id.ks_settings_view, view.id)
    }

    @Test
    fun `ホストが与えた ID は上書きしない`() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(android.R.attr.id, "@android:id/list")
            .build()

        val view = KsSettingsView(ApplicationProvider.getApplicationContext(), attrs)

        assertEquals(android.R.id.list, view.id)
    }

    @Test
    fun `ホストが個別の ID を与えた複数インスタンスでは対象の View だけが復元する`() {
        val notified = mutableListOf<String>()
        HostActivity.explicitViewIds = listOf(android.R.id.list, android.R.id.text1)
        val ctrl = launch(rootOf(dateCell(id = FIRST_CELL_ID)), rootOf(dateCell(id = SECOND_CELL_ID)))
        assertEquals("2 インスタンス構成になっていない", 2, HostActivity.settingsViews.size)
        moveTo(openDialog(HostActivity.settingsViews.first()), selected = PICKED_DATE)

        HostActivity.explicitViewIds = listOf(android.R.id.list, android.R.id.text1)
        recreate(
            ctrl,
            rootOf(dateCell(id = FIRST_CELL_ID, onValueChanged = { notified.add("first") })),
            rootOf(dateCell(id = SECOND_CELL_ID, onValueChanged = { notified.add("second") })),
        )

        val restored = requireNotNull(shownDialog()) { "対象の View で選択面が提示されていない" }
        assertEquals(PICKED_DATE, selectedDate(restored))
        assertEquals(
            "提示された選択面が 1 つでない",
            1,
            ShadowDialog.getShownDialogs().filterIsInstance<DateCalendarDialog>().count { it.isShowing },
        )
        restored.confirmSelection()
        assertEquals(listOf("first"), notified)
    }

    @Test
    fun `ID 未設定のインスタンスが複数あるときは復元しない`() {
        val notified = mutableListOf<String>()
        val ctrl = launch(rootOf(dateCell(id = FIRST_CELL_ID)), rootOf(dateCell(id = SECOND_CELL_ID)))
        assertEquals("2 インスタンス構成になっていない", 2, HostActivity.settingsViews.size)
        // 同じ ID の保存先は後から保存したインスタンスが勝つ。所有者を取り違えたまま復元が成立
        // しうるのは、最後に保存するインスタンスが選択面を持っている場合である。
        moveTo(openDialog(HostActivity.settingsViews.last()), selected = PICKED_DATE)

        recreate(
            ctrl,
            rootOf(dateCell(id = FIRST_CELL_ID, onValueChanged = { notified.add("first") })),
            rootOf(dateCell(id = SECOND_CELL_ID, onValueChanged = { notified.add("second") })),
        )

        assertNull("所有者が一意でないのに選択面が提示された", shownDialog())
        assertEquals(emptyList<String>(), notified)
    }

    // MARK: - 起動と再生成

    private fun launch(vararg roots: SettingsRoot): ActivityController<HostActivity> {
        HostActivity.rootsProvider = { roots.toList() }
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        idle()
        layoutAll(ctrl.get())
        idle()
        return ctrl
    }

    private fun recreate(controller: ActivityController<HostActivity>, vararg roots: SettingsRoot) {
        HostActivity.rootsProvider = { roots.toList() }
        controller.recreate()
        idle()
        layoutAll(controller.get())
        idle()
    }

    /** レイアウトを走らせ、各 `KsSettingsView` の RecyclerView に行を生成させる。 */
    private fun layoutAll(activity: HostActivity) {
        val metrics = activity.resources.displayMetrics
        for (view in HostActivity.settingsViews) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        }
    }

    // MARK: - 選択面の操作と参照

    /** 先頭行をタップして選択面を開く。 */
    private fun openDialog(view: KsSettingsView): DateCalendarDialog {
        val holder = view.internalRecyclerView().findViewHolderForAdapterPosition(0)
        assertTrue(
            "DatePickerCell 行の ViewHolder が生成されていない (実際: $holder)",
            holder is DatePickerCellViewHolder,
        )
        (holder as DatePickerCellViewHolder).todayProvider = { FIXED_TODAY }
        holder.views.root.performClick()
        val dialog = ShadowDialog.getLatestDialog()
        assertTrue("行タップで選択面が提示されていない (実際: $dialog)", dialog is DateCalendarDialog)
        return dialog as DateCalendarDialog
    }

    /** 表示中のカレンダー選択面（無ければ `null`）。 */
    private fun shownDialog(): DateCalendarDialog? =
        ShadowDialog.getShownDialogs()
            .filterIsInstance<DateCalendarDialog>()
            .lastOrNull { it.isShowing }

    /**
     * カレンダー上での選択・モード切替・月送りと同じ状態変更。
     *
     * モード切替は表示月を選択日の月へ戻す（`DatePickerState` の仕様）ため、月送りは切替の後に行う。
     */
    private fun moveTo(
        dialog: DateCalendarDialog,
        selected: LocalDate? = null,
        month: LocalDate? = null,
        textInput: Boolean = false,
    ) {
        selected?.let { dialog.state.selectedDateMillis = it.toEpochMilliUtc() }
        if (textInput) dialog.state.displayMode = DisplayMode.Input
        month?.let { dialog.state.displayedMonthMillis = it.withDayOfMonth(1).toEpochMilliUtc() }
    }

    private fun selectedDate(dialog: DateCalendarDialog): LocalDate? =
        dialog.state.selectedDateMillis?.toLocalDateUtc()

    private fun displayedMonth(dialog: DateCalendarDialog): LocalDate =
        dialog.state.displayedMonthMillis.toLocalDateUtc()

    // MARK: - 素材

    private fun rootOf(vararg cells: Cell): SettingsRoot =
        SettingsRoot(sections = listOf(Section(id = "section", cells = cells.toList())))

    private fun dateCell(
        id: String = FIRST_CELL_ID,
        date: LocalDate = INITIAL_DATE,
        minDate: LocalDate? = null,
        maxDate: LocalDate? = null,
        todayText: String? = null,
        uiStyle: DatePickerUIStyle = DatePickerUIStyle.Material,
        onValueChanged: ((LocalDate) -> Unit)? = null,
    ): DatePickerCell = DatePickerCell(
        id = id,
        title = "予定日",
        date = date,
        minDate = minDate,
        maxDate = maxDate,
        todayText = todayText,
        uiStyle = uiStyle,
        onValueChanged = onValueChanged,
    )

    private companion object {
        private const val FIRST_CELL_ID = "settings.plan.date"
        private const val SECOND_CELL_ID = "settings.deadline.date"

        /** 実行時刻に依存させないための固定の「今日」。 */
        private val FIXED_TODAY: LocalDate = LocalDate.of(2026, 8, 27)

        /** 行タップで開いた時点の選択日。 */
        private val INITIAL_DATE: LocalDate = LocalDate.of(2026, 8, 2)

        /** 開いた後にカレンダー上で選び直す日付。 */
        private val PICKED_DATE: LocalDate = LocalDate.of(2026, 8, 19)

        /** 開いた後に月送りで移る月（の初日）。 */
        private val OTHER_MONTH: LocalDate = LocalDate.of(2026, 10, 1)

        /** 再生成後に狭めた範囲（[PICKED_DATE] を含まない）。 */
        private val NARROWED_MIN: LocalDate = LocalDate.of(2026, 9, 1)
        private val NARROWED_MAX: LocalDate = LocalDate.of(2026, 9, 30)

        private const val THEME_BACKGROUND: Int = 0xFFF2EFE6.toInt()
        private const val THEME_ACCENT: Int = 0xFFFFBF00.toInt()
        private const val THEME_TEXT: Int = 0xFF555555.toInt()
    }
}
