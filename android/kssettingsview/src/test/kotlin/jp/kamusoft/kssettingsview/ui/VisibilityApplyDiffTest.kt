package jp.kamusoft.kssettingsview.ui

import androidx.test.core.app.ApplicationProvider
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `KsSettingsView` の可視性関連 applyDiff 振る舞いテスト。
 *
 * `ReplaceCell` / `ReplaceSection` が可視性を切り替える差分だったとき、
 * 平坦リストの整合が崩れないことを保証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VisibilityApplyDiffTest {

    private var savedStrictMode: Boolean = true

    @Before
    fun setUp() {
        savedStrictMode = KsCellRegistry.strictMode
        KsCellRegistry.strictMode = false
    }

    @After
    fun tearDown() {
        KsCellRegistry.strictMode = savedStrictMode
    }

    private fun makeView(sections: List<Section> = emptyList()): KsSettingsView {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = KsSettingsView(ctx)
        view.setRootDirect(SettingsRoot(sections = sections))
        return view
    }

    @Test
    fun `ReplaceCell isVisible true to false で model 更新 + flatten から除外`() {
        val visible = LabelCell(id = "c1", title = "A", isVisible = true)
        val s1 = Section(id = "s1", header = SectionAccessory.Text("S"), cells = listOf(visible))
        val view = makeView(listOf(s1))

        // 同一 id・isVisible のみ false に変えた新 Cell に置換
        val hidden = LabelCell(id = "c1", title = "A", isVisible = false)
        view.applyDiff(SettingsRootDiff.ReplaceCell(cellId = "c1", newCell = hidden))

        // model 上は更新されている（hidden Cell が保持されている）
        val updatedRoot = view.internalRoot()
        assertEquals(false, (updatedRoot.sections.first().cells.first() as LabelCell).isVisible)

        // visible projection（flatten 結果）から hidden Cell は除外される
        val flattened = KsSettingsView.flatten(updatedRoot.sections)
        assertTrue("CellRow for c1 must be filtered out by flatten",
            flattened.none { (it as? CellListItem.CellRow)?.cell?.id == "c1" })
    }

    @Test
    fun `ReplaceCell isVisible 同一なら通常の内容更新経路`() {
        val a = LabelCell(id = "c1", title = "旧", isVisible = true)
        val s1 = Section(id = "s1", cells = listOf(a))
        val view = makeView(listOf(s1))

        val a2 = LabelCell(id = "c1", title = "新", isVisible = true)
        view.applyDiff(SettingsRootDiff.ReplaceCell(cellId = "c1", newCell = a2))

        // model 上更新
        val updated = view.internalRoot().sections.first().cells.first() as LabelCell
        assertEquals("新", updated.title)
    }

    @Test
    fun `ReplaceSection は常に Full 経路で model 完全置換`() {
        val s1 = Section(id = "s1", header = SectionAccessory.Text("旧"), cells = listOf(
            LabelCell(id = "c1", title = "A")
        ))
        val view = makeView(listOf(s1))

        val newSection = Section(
            id = "s1",
            header = SectionAccessory.Text("新"),
            cells = listOf(
                LabelCell(id = "x", title = "X"),
                LabelCell(id = "y", title = "Y"),
            ),
        )
        view.applyDiff(SettingsRootDiff.ReplaceSection(sectionId = "s1", newSection = newSection))

        // model 側で完全置換され、新しい cells / header が反映されている
        val updatedSection = view.internalRoot().sections.first()
        assertEquals(SectionAccessory.Text("新"), updatedSection.header)
        assertEquals(2, updatedSection.cells.size)
        assertEquals("x", updatedSection.cells[0].id)
        assertEquals("y", updatedSection.cells[1].id)
    }

    @Test
    fun `Section isVisible false は flatten から完全に除外される`() {
        val s1 = Section(
            id = "s1",
            header = SectionAccessory.Text("hidden"),
            footer = SectionAccessory.Text("hidden footer"),
            cells = listOf(LabelCell(id = "c1", title = "A")),
            isVisible = false,
        )
        val flattened = KsSettingsView.flatten(listOf(s1))
        assertEquals(0, flattened.size)
    }
}
