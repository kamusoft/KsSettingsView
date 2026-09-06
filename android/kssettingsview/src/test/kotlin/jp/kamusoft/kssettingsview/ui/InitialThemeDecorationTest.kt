package jp.kamusoft.kssettingsview.ui

import android.os.Bundle
import android.widget.FrameLayout
import androidx.compose.ui.graphics.Color
import com.google.android.material.R as MaterialR
import androidx.fragment.app.FragmentActivity
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * 初期 Theme を持つ [SettingsRootStore] を [KsSettingsView.bind] したとき、
 * `ItemDecoration` にも初期 Theme が届くことを検証する。
 *
 * `ItemDecoration` は Theme を構築時に受け取って保持するため、Adapter の `theme` プロパティや
 * `RecyclerView` 背景色といった代理値が正しくても、`ItemDecoration` だけが構築時の既定 [Theme] の
 * まま取り残されうる（セパレータ色などが初期 Theme に追従しない）。したがって本テストは代理値では
 * なく `ItemDecoration` 自体が保持する Theme を観測する。
 *
 * bind の到達経路は 2 つあり、いずれも検証対象とする。
 *
 * - attach 済み View への `bind`
 * - attach 前の `bind`（Compose `AndroidView.factory` 内で bind する経路に相当）
 *
 * 検証対象（`ItemDecoration` と内部 Theme）は bind / attach の中で同期に更新されるため、
 * `submitList` の差分コミット完了を待つ必要はない。購読開始などでキューに残ったメッセージだけを
 * [idle] で流してから観測する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InitialThemeDecorationTest {

    /** `KsSettingsView` を 1 つだけ載せるホスト Activity。 */
    class HostActivity : FragmentActivity() {
        lateinit var container: FrameLayout
        lateinit var settingsView: KsSettingsView

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(MaterialR.style.Theme_Material3_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            container = FrameLayout(this)
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

    /**
     * 検証に使う初期 Theme。
     *
     * 既定 [Theme] と異なる値であることを各テストで確認したうえで比較する。既定と同値だと
     * 「取り残されたまま」でも一致してしまい、観測が空振りするためである。
     */
    private fun customTheme(): Theme = Theme(
        separatorColor = Color(0xFFE6DAB9),
        backgroundColor = Color(0xFF102030),
    )

    private fun sampleRoot(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(
                id = "s1",
                header = SectionAccessory.Text("見出し"),
                cells = listOf(
                    LabelCell(id = "c1", title = "A"),
                    LabelCell(id = "c2", title = "B"),
                ),
            ),
        ),
    )

    /** 現在の `ItemDecoration` が保持している Theme。 */
    private fun decorationTheme(view: KsSettingsView): Theme =
        (view.internalCurrentDecoration() as ClassicSectionDecoration).theme

    /**
     * 描画側が受け取る想定の Theme（未指定色をライト外観の既定で解決した形）。
     *
     * Robolectric の既定 Configuration は非夜間であり、View もライトで解決する。
     */
    private fun expectedResolved(theme: Theme): Theme = theme.resolvedFor(darkTheme = false)

    @Test
    fun `attach 済み View に初期 Theme 付き Store を bind すると ItemDecoration が初期 Theme になる`() {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()
        val view = activity.settingsView
        val theme = customTheme()
        assertNotEquals("初期 Theme は既定 Theme と異なる値である", Theme(), theme)

        view.bind(SettingsRootStore(initialRoot = sampleRoot(), initialTheme = theme))
        idle()

        assertEquals("bind 直後に ItemDecoration が初期 Theme になる", expectedResolved(theme), decorationTheme(view))
        assertEquals("解決済み Theme も初期 Theme 由来になる", expectedResolved(theme), view.internalTheme())
        assertEquals("利用者が読む Theme は解決前のまま", theme, view.theme)
    }

    @Test
    fun `attach 前に初期 Theme 付き Store を bind すると ItemDecoration が初期 Theme になる`() {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()

        // attach 前に bind する経路（Compose `AndroidView.factory` 相当）を作るため、
        // Activity が持つ View とは別に組み立てる。
        val view = KsSettingsView(activity)
        val theme = customTheme()
        assertNotEquals("初期 Theme は既定 Theme と異なる値である", Theme(), theme)

        view.bind(SettingsRootStore(initialRoot = sampleRoot(), initialTheme = theme))
        idle()

        assertEquals("attach 前でも ItemDecoration が初期 Theme になる", expectedResolved(theme), decorationTheme(view))

        // attach 後も初期 Theme のままであること。attach 時の再取り込みと Theme の同値スキップを
        // 通過しても、ItemDecoration が既定 Theme へ戻らないことを見る。
        activity.container.addView(view)
        idle()

        assertEquals("attach 後も ItemDecoration は初期 Theme のまま", expectedResolved(theme), decorationTheme(view))
        assertEquals("解決済み Theme も初期 Theme 由来のまま", expectedResolved(theme), view.internalTheme())
        assertEquals("利用者が読む Theme は解決前のまま", theme, view.theme)
    }

    @Test
    fun `Modern スタイルでも初期 Theme が ItemDecoration に届く`() {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()
        val view = activity.settingsView
        view.style = KsSettingsViewStyle.Modern
        val theme = customTheme()

        view.bind(SettingsRootStore(initialRoot = sampleRoot(), initialTheme = theme))
        idle()

        assertEquals(
            "Modern スタイルの ItemDecoration も初期 Theme になる",
            expectedResolved(theme),
            (view.internalCurrentDecoration() as ModernSectionDecoration).theme,
        )
    }
}
