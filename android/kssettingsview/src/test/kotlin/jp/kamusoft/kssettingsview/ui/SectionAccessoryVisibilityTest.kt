package jp.kamusoft.kssettingsview.ui

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
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
 * Section Header / Footer の表示トグルと、内容不在の統一判定を実表示経路で検証する。
 *
 * 表示判定は「トグル && 内容あり」の AND 合成であり、「内容の不在」は null または空 text とする
 * （core/ADR-0023）。観測は Store 更新が RecyclerView の行として現れたところ（[visibleRowTexts]）で
 * 行い、モデル側の値だけを見て通したことにしない。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SectionAccessoryVisibilityTest {

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

    private fun startActivity(): HostActivity {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        return ctrl.get()
    }

    /** レイアウトを走らせて RecyclerView に行を生成させる。 */
    private fun HostActivity.layoutSettingsView() {
        val metrics = resources.displayMetrics
        settingsView.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        settingsView.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    /**
     * Header「一般」・Cell「A」・Footer「補足」を持つ Section 1 つの Section を返す。
     *
     * トグルを指定しなければ 3 行すべてが表示される構成であり、トグルの効果が
     * 「消えた行」として観測できる。
     */
    private fun sampleSection(
        isHeaderVisible: Boolean = true,
        isFooterVisible: Boolean = true,
    ) = Section(
        id = "s1",
        header = SectionAccessory.Text("一般"),
        footer = SectionAccessory.Text("補足"),
        cells = listOf(LabelCell(id = "c1", title = "A")),
        isHeaderVisible = isHeaderVisible,
        isFooterVisible = isFooterVisible,
    )

    /** [sections] を初期状態に持つ Store を bind し、初期表示の収束まで待った Activity を返す。 */
    private fun hostWithSections(sections: List<Section>): Pair<HostActivity, SettingsRootStore> {
        val activity = startActivity()
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = sections))
        activity.settingsView.bind(store)
        idle()
        activity.layoutSettingsView()
        return activity to store
    }

    // MARK: - トグルによる非表示

    @Test
    fun `内容がある Header をトグルで隠す`() {
        val (activity, _) = hostWithSections(listOf(sampleSection(isHeaderVisible = false)))
        assertEquals(
            "Header だけが消え、Cell と Footer は残る",
            listOf("A", "補足"),
            visibleRowTexts(activity.settingsView),
        )
    }

    @Test
    fun `内容がある Footer をトグルで隠す`() {
        val (activity, _) = hostWithSections(listOf(sampleSection(isFooterVisible = false)))
        assertEquals(
            "Footer だけが消え、Header と Cell は残る",
            listOf("一般", "A"),
            visibleRowTexts(activity.settingsView),
        )
    }

    @Test
    fun `非空 accessory ではトグル未指定で従来どおり表示される`() {
        val (activity, _) = hostWithSections(listOf(sampleSection()))
        assertEquals(
            listOf("一般", "A", "補足"),
            visibleRowTexts(activity.settingsView),
        )
    }

    @Test
    fun `トグルは Header と Footer で独立して効く`() {
        // 同一 root に「Footer だけ隠した Section」と「Header だけ隠した Section」を並べ、
        // 一方のトグルが他方および Cell に波及しないことを 1 つの表示結果で見る。
        val footerHidden = sampleSection(isFooterVisible = false)
        val headerHidden = Section(
            id = "s2",
            header = SectionAccessory.Text("詳細"),
            footer = SectionAccessory.Text("注記"),
            cells = listOf(LabelCell(id = "c2", title = "B")),
            isHeaderVisible = false,
        )
        val (activity, _) = hostWithSections(listOf(footerHidden, headerHidden))
        assertEquals(
            listOf("一般", "A", "B", "注記"),
            visibleRowTexts(activity.settingsView),
        )
    }

    // MARK: - 内容不在の統一判定（iOS への対称化）

    @Test
    fun `空 text の Header は行を生成しない`() {
        val section = sampleSection().copy(header = SectionAccessory.Text(""))
        val (activity, _) = hostWithSections(listOf(section))
        assertEquals(listOf("A", "補足"), visibleRowTexts(activity.settingsView))
    }

    @Test
    fun `空 text の Footer は行を生成しない`() {
        val section = sampleSection().copy(footer = SectionAccessory.Text(""))
        val (activity, _) = hostWithSections(listOf(section))
        assertEquals(listOf("一般", "A"), visibleRowTexts(activity.settingsView))
    }

    @Test
    fun `内容不在の Header はトグル true でも行を生成しない`() {
        val nullHeader = sampleSection().copy(header = null, isHeaderVisible = true)
        assertFalse(KsSettingsView.shouldShowHeader(nullHeader))
        val emptyHeader = sampleSection().copy(
            header = SectionAccessory.Text(""),
            isHeaderVisible = true,
        )
        assertFalse(KsSettingsView.shouldShowHeader(emptyHeader))
    }

    @Test
    fun `View accessory は中身に依らず内容ありとして扱う`() {
        val section = sampleSection().copy(
            header = SectionAccessory.View(KsAnyView.AndroidView { ctx -> View(ctx) }),
        )
        assertTrue(KsSettingsView.shouldShowHeader(section))
        assertFalse(
            "トグルが false なら View accessory でも表示しない",
            KsSettingsView.shouldShowHeader(section.copy(isHeaderVisible = false)),
        )
    }

    @Test
    fun `高さ指定は Header の存在を作らない`() {
        // header 不在 + headerHeight 正値でも Header 行は生成されない（高さ解決は存在判定の後）。
        val section = sampleSection().copy(header = null, headerHeight = 40.0)
        val (activity, _) = hostWithSections(listOf(section))
        assertEquals(listOf("A", "補足"), visibleRowTexts(activity.settingsView))
    }

    // MARK: - replaceSection によるトグル変更の反映

    @Test
    fun `replaceSection でトグル変更が両方向に反映され Cell は保持される`() {
        val (activity, store) = hostWithSections(listOf(sampleSection()))
        val view = activity.settingsView
        assertEquals(listOf("一般", "A", "補足"), visibleRowTexts(view))

        // 表示 → 非表示
        store.replaceSection("s1", sampleSection(isHeaderVisible = false))
        awaitConvergence(view) { committedTexts(view) == listOf("A", "補足") }
        activity.layoutSettingsView()
        assertEquals(listOf("A", "補足"), visibleRowTexts(view))
        assertEquals(
            "同一 ID の Cell 群は保持される",
            listOf("c1"),
            view.internalRoot().sections.first().cells.map { it.id },
        )

        // 非表示 → 表示
        store.replaceSection("s1", sampleSection(isHeaderVisible = true))
        awaitConvergence(view) { committedTexts(view) == listOf("一般", "A", "補足") }
        activity.layoutSettingsView()
        assertEquals(listOf("一般", "A", "補足"), visibleRowTexts(view))
        assertEquals(
            listOf("c1"),
            view.internalRoot().sections.first().cells.map { it.id },
        )
    }

    // MARK: - トグルの独立性と保持

    @Test
    fun `Cell 操作をまたいでトグルが保持される`() {
        val (activity, store) = hostWithSections(listOf(sampleSection(isHeaderVisible = false)))
        val view = activity.settingsView

        store.insertCell(LabelCell(id = "c2", title = "B"), sectionId = "s1", at = 1)
        awaitConvergence(view) { committedTexts(view) == listOf("A", "B", "補足") }
        activity.layoutSettingsView()
        assertEquals("挿入後も Header は非表示のまま", listOf("A", "B", "補足"), visibleRowTexts(view))

        store.moveCell("c2", to = 0)
        awaitConvergence(view) { committedTexts(view) == listOf("B", "A", "補足") }
        activity.layoutSettingsView()
        assertEquals("移動後も Header は非表示のまま", listOf("B", "A", "補足"), visibleRowTexts(view))

        store.replaceCell("c1", LabelCell(id = "c1", title = "A2"))
        awaitConvergence(view) { committedTexts(view) == listOf("B", "A2", "補足") }
        activity.layoutSettingsView()
        assertEquals("置換後も Header は非表示のまま", listOf("B", "A2", "補足"), visibleRowTexts(view))

        store.removeCell("c2")
        awaitConvergence(view) { committedTexts(view) == listOf("A2", "補足") }
        activity.layoutSettingsView()
        assertEquals("削除後も Header は非表示のまま", listOf("A2", "補足"), visibleRowTexts(view))

        assertFalse(
            "model 上もトグルは false のまま保持される",
            view.internalRoot().sections.first().isHeaderVisible,
        )
    }

    @Test
    fun `非表示中の内容更新が再表示に反映される`() {
        val (activity, store) = hostWithSections(listOf(sampleSection(isHeaderVisible = false)))
        val view = activity.settingsView
        assertEquals(listOf("A", "補足"), visibleRowTexts(view))

        // 非表示のまま header の内容だけを差し替える（行が無いので表示は変わらない）
        store.updateAccessory(
            target = AccessoryTarget.SectionHeader(sectionId = "s1"),
            accessory = SettingsAccessory.Section(SectionAccessory.Text("更新後")),
        )
        idle()
        activity.layoutSettingsView()
        assertEquals("非表示中は行が現れない", listOf("A", "補足"), visibleRowTexts(view))

        // 再表示すると、非表示中に更新された最新の内容で行が出る
        val latest = store.state.value.sections.first().copy(isHeaderVisible = true)
        store.replaceSection("s1", latest)
        awaitConvergence(view) { committedTexts(view) == listOf("更新後", "A", "補足") }
        activity.layoutSettingsView()
        assertEquals(listOf("更新後", "A", "補足"), visibleRowTexts(view))
    }
}
