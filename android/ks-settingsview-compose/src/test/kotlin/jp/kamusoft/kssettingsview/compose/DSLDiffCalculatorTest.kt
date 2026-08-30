package jp.kamusoft.kssettingsview.compose

import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.ui.CheckboxCell
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.RadioCell
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `DSLDiffCalculator.compute(from, to)` の各 Diff 種別を検証する。
 */
class DSLDiffCalculatorTest {

    private fun tree(
        sections: List<Section> = emptyList(),
        rootHeader: RootAccessory? = null,
        rootFooter: RootAccessory? = null,
    ) = DSLDiffCalculator.ResolvedTree(sections, rootHeader, rootFooter)

    private fun cell(id: String, title: String = "Cell"): Cell = LabelCell(id = id, title = title)

    @Test
    fun `完全一致なら空のDiffが返る`() {
        val s = Section(id = "s1", header = SectionAccessory.Text("A"), cells = listOf(cell("c1")))
        val old = tree(listOf(s))
        val new = tree(listOf(s))
        assertEquals(emptyList<SettingsRootDiff>(), DSLDiffCalculator.compute(old, new))
    }

    @Test
    fun `Cell追加でInsertCellが発行される`() {
        val sid = "s1"
        val old = tree(listOf(Section(id = sid, cells = listOf(cell("c1")))))
        val new = tree(listOf(Section(id = sid, cells = listOf(cell("c1"), cell("c2")))))

        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(1, diffs.size)
        val ins = diffs[0] as SettingsRootDiff.InsertCell
        assertEquals(sid, ins.sectionId)
        assertEquals(1, ins.index)
        assertEquals("c2", ins.cell.id)
    }

    @Test
    fun `Cell削除でRemoveCellが発行される`() {
        val sid = "s1"
        val old = tree(listOf(Section(id = sid, cells = listOf(cell("c1"), cell("c2")))))
        val new = tree(listOf(Section(id = sid, cells = listOf(cell("c1")))))

        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(1, diffs.size)
        val rem = diffs[0] as SettingsRootDiff.RemoveCell
        assertEquals("c2", rem.cellId)
    }

    @Test
    fun `Cell内容変更では構造Diffを発行せずcontentUpdatesで検出される`() {
        // 「表示状態同期の三層分離」: 内容変化（title）では構造 Diff（ReplaceCell 含む）を発行しない。
        // 内容更新は contentUpdates が列挙し、ViewHolder の部分更新で反映される。
        val sid = "s1"
        val old = tree(listOf(Section(id = sid, cells = listOf(cell("c1", title = "Taro")))))
        val new = tree(listOf(Section(id = sid, cells = listOf(cell("c1", title = "Hanako")))))

        assertEquals(emptyList<SettingsRootDiff>(), DSLDiffCalculator.compute(old, new))
        val updates = DSLDiffCalculator.contentUpdates(old, new)
        assertEquals(1, updates.size)
        assertEquals("c1", updates[0].id)
    }

    @Test
    fun `内部状態のみ変化したCheckboxCellでは構造Diffを発行しない`() {
        // 「表示状態同期の三層分離」: 内部状態（isChecked）の変化でも構造 Diff（ReplaceCell）は発行しない。
        // 内容更新は contentUpdates で検出され ViewHolder の部分更新で反映される。タップ操作の即時反映は
        // ViewHolder の View 直接トグル（TwoWay）が担う。なお値型としての equals は isChecked を含むため、
        // contentUpdates は isChecked の変化を検出する。
        val sid = "s1"
        val old = tree(
            listOf(
                Section(
                    id = sid,
                    cells = listOf(CheckboxCell(id = "c1", title = "同意", isChecked = false)),
                ),
            ),
        )
        val new = tree(
            listOf(
                Section(
                    id = sid,
                    cells = listOf(CheckboxCell(id = "c1", title = "同意", isChecked = true)),
                ),
            ),
        )
        assertEquals(emptyList<SettingsRootDiff>(), DSLDiffCalculator.compute(old, new))
        // isChecked の変化は contentUpdates で検出される（値型 equals に isChecked を含むため）。
        val updates = DSLDiffCalculator.contentUpdates(old, new)
        assertEquals(1, updates.size)
        assertEquals(true, (updates[0] as CheckboxCell).isChecked)
    }

    @Test
    fun `タイトル変化したCheckboxCellでは構造Diffを発行せずcontentUpdatesで検出される`() {
        // 「表示状態同期の三層分離」: 内容変化（title）では構造 Diff（ReplaceCell 含む）を発行しない。
        // 内容更新は contentUpdates が列挙し、ViewHolder の部分更新経路（notifyItemChanged）で反映される。
        val sid = "s1"
        val old = tree(
            listOf(Section(id = sid, cells = listOf(CheckboxCell(id = "c1", title = "A", isChecked = true)))),
        )
        val new = tree(
            listOf(Section(id = sid, cells = listOf(CheckboxCell(id = "c1", title = "B", isChecked = true)))),
        )
        // 構造 Diff は空（ReplaceCell を発行しない）
        assertEquals(emptyList<SettingsRootDiff>(), DSLDiffCalculator.compute(old, new))
        // 内容更新は contentUpdates で検出される
        val updates = DSLDiffCalculator.contentUpdates(old, new)
        assertEquals(1, updates.size)
        assertEquals("c1", updates[0].id)
        assertEquals("B", (updates[0] as CheckboxCell).title)
    }

    @Test
    fun `内部状態が同一のCheckboxCellでは空Diff`() {
        val sid = "s1"
        val cb = CheckboxCell(id = "c1", title = "同意", isChecked = true)
        val old = tree(listOf(Section(id = sid, cells = listOf(cb))))
        val new = tree(listOf(Section(id = sid, cells = listOf(cb.copy()))))
        assertEquals(emptyList<SettingsRootDiff>(), DSLDiffCalculator.compute(old, new))
    }

    @Test
    fun `selectedValueが変化したRadioCellでは構造Diffを発行せずcontentUpdatesで検出される`() {
        // RadioCell の selectedValue 変化（グループ連動）も内容変化として扱う。構造 Diff（ReplaceCell）は
        // 発行せず、contentUpdates が列挙して ViewHolder の部分更新（notifyItemChanged）で ✓ を移す。
        val sid = "s1"
        val old = tree(
            listOf(
                Section(
                    id = sid,
                    cells = listOf(RadioCell(id = "r1", title = "L", groupId = "g", value = "light", selectedValue = "dark")),
                ),
            ),
        )
        val new = tree(
            listOf(
                Section(
                    id = sid,
                    cells = listOf(RadioCell(id = "r1", title = "L", groupId = "g", value = "light", selectedValue = "light")),
                ),
            ),
        )
        // 構造 Diff は空（ReplaceCell を発行しない）
        assertEquals(emptyList<SettingsRootDiff>(), DSLDiffCalculator.compute(old, new))
        // 内容更新は contentUpdates で検出される
        val updates = DSLDiffCalculator.contentUpdates(old, new)
        assertEquals(1, updates.size)
        assertEquals("light", (updates[0] as RadioCell).selectedValue)
    }

    @Test
    fun `Section追加でInsertSectionが発行される`() {
        val s1 = Section(id = "s1", cells = listOf(cell("c1")))
        val s2 = Section(id = "s2", cells = listOf(cell("c2")))
        val old = tree(listOf(s1))
        val new = tree(listOf(s1, s2))

        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(1, diffs.size)
        val ins = diffs[0] as SettingsRootDiff.InsertSection
        assertEquals(1, ins.index)
        assertEquals("s2", ins.section.id)
    }

    @Test
    fun `Section削除でRemoveSectionが発行される`() {
        val s1 = Section(id = "s1", cells = listOf(cell("c1")))
        val s2 = Section(id = "s2", cells = listOf(cell("c2")))
        val old = tree(listOf(s1, s2))
        val new = tree(listOf(s1))

        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(1, diffs.size)
        val rem = diffs[0] as SettingsRootDiff.RemoveSection
        assertEquals("s2", rem.sectionId)
    }

    @Test
    fun `Section H F 変更でUpdateAccessoryが発行される`() {
        val sid = "s1"
        val old = tree(listOf(Section(id = sid, header = SectionAccessory.Text("旧"), cells = listOf(cell("c1")))))
        val new = tree(listOf(Section(id = sid, header = SectionAccessory.Text("新"), cells = listOf(cell("c1")))))

        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(1, diffs.size)
        val upd = diffs[0] as SettingsRootDiff.UpdateAccessory
        assertEquals(AccessoryTarget.SectionHeader(sectionId = sid), upd.target)
        assertEquals(SettingsAccessory.Section(SectionAccessory.Text("新")), upd.accessory)
    }

    @Test
    fun `Root Header 変更でUpdateAccessoryが発行される`() {
        val old = tree(rootHeader = RootAccessory.Text("旧"))
        val new = tree(rootHeader = RootAccessory.Text("新"))

        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(1, diffs.size)
        val upd = diffs[0] as SettingsRootDiff.UpdateAccessory
        assertEquals(AccessoryTarget.RootHeader, upd.target)
        assertEquals(SettingsAccessory.Root(RootAccessory.Text("新")), upd.accessory)
    }

    @Test
    fun `Cell移動でMoveCellが発行される`() {
        val sid = "s1"
        val a = cell("a")
        val b = cell("b")
        val c = cell("c")
        val old = tree(listOf(Section(id = sid, cells = listOf(a, b, c))))
        val new = tree(listOf(Section(id = sid, cells = listOf(a, c, b))))

        val diffs = DSLDiffCalculator.compute(old, new)
        assertTrue(diffs.any { it is SettingsRootDiff.MoveCell })
    }

    // MARK: - headerHeight 変化の preflight

    /**
     * headerHeight だけが変わったツリーの組を作る。
     *
     * header text・Cell・Section ID は据え置き、`headerHeight` のみ [oldHeight] → [newHeight] とする。
     */
    private fun headerHeightTrees(
        oldHeight: Double,
        newHeight: Double,
        oldTitle: String = "Cell",
        newTitle: String = "Cell",
    ): Pair<DSLDiffCalculator.ResolvedTree, DSLDiffCalculator.ResolvedTree> {
        val sid = "s1"
        val old = tree(
            listOf(
                Section(
                    id = sid,
                    header = SectionAccessory.Text("一般"),
                    cells = listOf(cell("c1", title = oldTitle)),
                    headerHeight = oldHeight,
                ),
            ),
        )
        val new = tree(
            listOf(
                Section(
                    id = sid,
                    header = SectionAccessory.Text("一般"),
                    cells = listOf(cell("c1", title = newTitle)),
                    headerHeight = newHeight,
                ),
            ),
        )
        return old to new
    }

    @Test
    fun `headerHeight が正値間で変化すると Full のみ発行され contentUpdates は空`() {
        val (old, new) = headerHeightTrees(oldHeight = 40.0, newHeight = 80.0)

        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(1, diffs.size)
        val full = diffs[0] as SettingsRootDiff.Full
        assertEquals(80.0, full.root.sections[0].headerHeight, 0.0)
        assertEquals(emptyList<Cell>(), DSLDiffCalculator.contentUpdates(old, new))
    }

    @Test
    fun `headerHeight が自動から固定へ変化すると Full のみ発行され contentUpdates は空`() {
        val (old, new) = headerHeightTrees(oldHeight = -1.0, newHeight = 80.0)

        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(1, diffs.size)
        val full = diffs[0] as SettingsRootDiff.Full
        assertEquals(80.0, full.root.sections[0].headerHeight, 0.0)
        assertEquals(emptyList<Cell>(), DSLDiffCalculator.contentUpdates(old, new))
    }

    @Test
    fun `headerHeight が固定から自動へ変化すると Full のみ発行され contentUpdates は空`() {
        val (old, new) = headerHeightTrees(oldHeight = 80.0, newHeight = -1.0)

        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(1, diffs.size)
        val full = diffs[0] as SettingsRootDiff.Full
        assertEquals(-1.0, full.root.sections[0].headerHeight, 0.0)
        assertEquals(emptyList<Cell>(), DSLDiffCalculator.contentUpdates(old, new))
    }

    @Test
    fun `headerHeight と Cell 内容の同時変更でも Full のみ発行され contentUpdates は空`() {
        // Full の適用（`setRootDirect` 経路）が Cell 内容の反映を内包するため、内容更新は
        // 二重に流さない。新ツリーの Cell 内容は Full が運ぶ root に含まれる。
        val (old, new) = headerHeightTrees(
            oldHeight = 40.0,
            newHeight = 80.0,
            oldTitle = "旧",
            newTitle = "新",
        )

        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(1, diffs.size)
        val full = diffs[0] as SettingsRootDiff.Full
        assertEquals(80.0, full.root.sections[0].headerHeight, 0.0)
        assertEquals("新", (full.root.sections[0].cells[0] as LabelCell).title)
        assertEquals(emptyList<Cell>(), DSLDiffCalculator.contentUpdates(old, new))
    }

    @Test
    fun `headerHeight 不変で内容だけ変わると Full を発行せず contentUpdates で列挙される`() {
        val (old, new) = headerHeightTrees(
            oldHeight = 40.0,
            newHeight = 40.0,
            oldTitle = "旧",
            newTitle = "新",
        )

        val diffs = DSLDiffCalculator.compute(old, new)
        assertEquals(emptyList<SettingsRootDiff>(), diffs)
        val updates = DSLDiffCalculator.contentUpdates(old, new)
        assertEquals(1, updates.size)
        assertEquals("新", (updates[0] as LabelCell).title)
    }

    @Test
    fun `containsHeaderHeightChange は同一 headerHeight で false`() {
        val (old, new) = headerHeightTrees(oldHeight = 40.0, newHeight = 40.0)
        assertEquals(false, DSLDiffCalculator.containsHeaderHeightChange(old, new))
    }
}
