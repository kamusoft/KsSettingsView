package jp.kamusoft.kssettingsview.ui

import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.graphics.Color
import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `KsSettingsView.applyDiff(_:)` の全 11 ケースに対する内部状態検証テスト。
 *
 * [SettingsRootDiff] の各差分種別を適用したとき、内部保持 root と Theme、および Root H/F が
 * 期待どおり更新されることを保証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ApplyDiffTest {

    private var savedStrictMode: Boolean = true

    @Before
    fun setUp() {
        // applyDiff のエラーハンドリングを Release 相当（クラッシュしない）にしてテストを実行する。
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

    // MARK: - Full

    @Test
    fun `applyDiff Full で全体差し替えされる`() {
        val view = makeView()
        val newRoot = SettingsRoot(
            sections = listOf(
                Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A"))),
                Section(id = "s2", cells = listOf(LabelCell(id = "c2", title = "B"))),
            ),
        )
        view.applyDiff(SettingsRootDiff.Full(newRoot))
        assertEquals(2, view.internalRoot().sections.size)
    }

    // MARK: - InsertSection

    @Test
    fun `applyDiff InsertSection で sections に追加される`() {
        val view = makeView()
        val newSection = Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A")))
        view.applyDiff(SettingsRootDiff.InsertSection(index = 0, section = newSection))
        assertEquals(1, view.internalRoot().sections.size)
        assertEquals("s1", view.internalRoot().sections[0].id)
    }

    // MARK: - RemoveSection

    @Test
    fun `applyDiff RemoveSection で sections から削除される`() {
        val s1 = Section(id = "s1")
        val s2 = Section(id = "s2")
        val view = makeView(sections = listOf(s1, s2))

        view.applyDiff(SettingsRootDiff.RemoveSection(sectionId = "s1"))
        assertEquals(1, view.internalRoot().sections.size)
        assertEquals("s2", view.internalRoot().sections[0].id)
    }

    // MARK: - MoveSection

    @Test
    fun `applyDiff MoveSection で順序が変わる`() {
        val view = makeView(
            sections = listOf(
                Section(id = "s1"),
                Section(id = "s2"),
                Section(id = "s3"),
            ),
        )
        view.applyDiff(SettingsRootDiff.MoveSection(from = 0, to = 2))
        assertEquals(listOf("s2", "s3", "s1"), view.internalRoot().sections.map { it.id })
    }

    // MARK: - ReplaceSection

    @Test
    fun `applyDiff ReplaceSection で Section が置換される`() {
        val s1 = Section(id = "s1", header = SectionAccessory.Text("old"))
        val view = makeView(sections = listOf(s1))

        val newSection = Section(
            id = "s1",
            header = SectionAccessory.Text("new"),
            cells = listOf(LabelCell(id = "x", title = "X")),
        )
        view.applyDiff(SettingsRootDiff.ReplaceSection(sectionId = "s1", newSection = newSection))

        assertEquals(SectionAccessory.Text("new"), view.internalRoot().sections[0].header)
        assertEquals(1, view.internalRoot().sections[0].cells.size)
    }

    // MARK: - InsertCell

    @Test
    fun `applyDiff InsertCell で Cell が追加される`() {
        val s1 = Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A")))
        val view = makeView(sections = listOf(s1))

        val newCell = LabelCell(id = "c2", title = "B")
        view.applyDiff(SettingsRootDiff.InsertCell(sectionId = "s1", index = 0, cell = newCell))

        assertEquals(2, view.internalRoot().sections[0].cells.size)
        assertEquals("c2", view.internalRoot().sections[0].cells[0].id)
    }

    // MARK: - RemoveCell

    @Test
    fun `applyDiff RemoveCell で Cell が削除される`() {
        val s1 = Section(
            id = "s1",
            cells = listOf(LabelCell(id = "c1", title = "A"), LabelCell(id = "c2", title = "B")),
        )
        val view = makeView(sections = listOf(s1))

        view.applyDiff(SettingsRootDiff.RemoveCell(cellId = "c1"))
        assertEquals(1, view.internalRoot().sections[0].cells.size)
        assertEquals("c2", view.internalRoot().sections[0].cells[0].id)
    }

    // MARK: - ReplaceCell

    @Test
    fun `applyDiff ReplaceCell で Cell が置換される`() {
        val s1 = Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "old")))
        val view = makeView(sections = listOf(s1))

        val newCell = LabelCell(id = "c1", title = "new")
        view.applyDiff(SettingsRootDiff.ReplaceCell(cellId = "c1", newCell = newCell))

        val replaced = view.internalRoot().sections[0].cells[0] as LabelCell
        assertEquals("new", replaced.title)
    }

    // MARK: - MoveCell

    @Test
    fun `applyDiff MoveCell で Cell が移動する`() {
        val s1 = Section(
            id = "s1",
            cells = listOf(
                LabelCell(id = "c1", title = "A"),
                LabelCell(id = "c2", title = "B"),
                LabelCell(id = "c3", title = "C"),
            ),
        )
        val view = makeView(sections = listOf(s1))

        view.applyDiff(SettingsRootDiff.MoveCell(cellId = "c1", toIndex = 2))
        assertEquals(listOf("c2", "c3", "c1"), view.internalRoot().sections[0].cells.map { it.id })
    }

    // MARK: - UpdateAccessory

    @Test
    fun `applyDiff UpdateAccessory RootHeader で rootHeader が更新される`() {
        val view = makeView()
        view.applyDiff(
            SettingsRootDiff.UpdateAccessory(
                target = AccessoryTarget.RootHeader,
                accessory = SettingsAccessory.Root(RootAccessory.Text("X")),
            ),
        )
        assertEquals(RootAccessory.Text("X"), view.rootHeader)
    }

    @Test
    fun `applyDiff UpdateAccessory RootFooter で rootFooter が更新される`() {
        val view = makeView()
        view.applyDiff(
            SettingsRootDiff.UpdateAccessory(
                target = AccessoryTarget.RootFooter,
                accessory = SettingsAccessory.Root(RootAccessory.Text("Y")),
            ),
        )
        assertEquals(RootAccessory.Text("Y"), view.rootFooter)
    }

    @Test
    fun `applyDiff UpdateAccessory SectionHeader で Section の header が更新される`() {
        val s1 = Section(id = "s1", header = SectionAccessory.Text("old"))
        val view = makeView(sections = listOf(s1))

        view.applyDiff(
            SettingsRootDiff.UpdateAccessory(
                target = AccessoryTarget.SectionHeader(sectionId = "s1"),
                accessory = SettingsAccessory.Section(SectionAccessory.Text("new")),
            ),
        )

        assertEquals(SectionAccessory.Text("new"), view.internalRoot().sections[0].header)
    }

    // MARK: - Theme 更新（applyDiff 経路は通らない）

    @Test
    fun `view theme プロパティ更新で internalTheme が反映される（Diff 経路ではない）`() {
        val view = makeView(
            sections = listOf(
                Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A"))),
            ),
        )
        val newTheme = Theme(separatorColor = Color.Red)
        view.theme = newTheme

        assertEquals(newTheme, view.internalTheme())
    }

    // MARK: - エラーハンドリング（strictMode = false 相当）

    @Test
    fun `applyDiff RemoveCell で存在しない cellId はクラッシュせず内部状態は不変`() {
        val s1 = Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A")))
        val view = makeView(sections = listOf(s1))

        view.applyDiff(SettingsRootDiff.RemoveCell(cellId = "non-existent"))
        assertEquals(1, view.internalRoot().sections[0].cells.size)
    }

    @Test
    fun `applyDiff RemoveSection で存在しない sectionId はクラッシュせず内部状態は不変`() {
        val s1 = Section(id = "s1")
        val view = makeView(sections = listOf(s1))

        view.applyDiff(SettingsRootDiff.RemoveSection(sectionId = "non-existent"))
        assertEquals(1, view.internalRoot().sections.size)
    }
}
