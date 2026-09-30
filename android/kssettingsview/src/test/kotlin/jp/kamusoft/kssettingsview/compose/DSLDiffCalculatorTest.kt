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
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
import jp.kamusoft.kssettingsview.ui.SettingsRootStore
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

    // MARK: - 移動の最小化（追加・削除でずれただけの項目には移動を出さない）

    /** [ids] の順に LabelCell を並べた 1 Section のツリーを作る。 */
    private fun singleSection(ids: List<String>, sid: String = "s1") =
        tree(listOf(Section(id = sid, cells = ids.map { cell(it) })))

    /**
     * 差分列を実際の Store に順に適用し、適用後の Section / Cell の ID の並びを返す。
     *
     * Store の各操作が差分をどう解釈するか（移動は取り除いた後の位置へ挿入）まで含めて、
     * 宣言どおりの並びに着地するかを確かめるために使う。
     */
    private fun applyToStore(
        from: DSLDiffCalculator.ResolvedTree,
        diffs: List<SettingsRootDiff>,
    ): List<Pair<String, List<String>>> {
        val store = SettingsRootStore(SettingsRoot(sections = from.sections))
        for (diff in diffs) {
            when (diff) {
                is SettingsRootDiff.Full -> store.replaceAll(diff.root)
                is SettingsRootDiff.InsertSection -> store.insertSection(diff.section, at = diff.index)
                is SettingsRootDiff.RemoveSection -> store.removeSection(diff.sectionId)
                is SettingsRootDiff.MoveSection -> store.moveSection(from = diff.from, to = diff.to)
                is SettingsRootDiff.ReplaceSection -> store.replaceSection(diff.sectionId, diff.newSection)
                is SettingsRootDiff.InsertCell -> store.insertCell(diff.cell, diff.sectionId, at = diff.index)
                is SettingsRootDiff.RemoveCell -> store.removeCell(diff.cellId)
                is SettingsRootDiff.ReplaceCell -> store.replaceCell(diff.cellId, diff.newCell)
                is SettingsRootDiff.MoveCell -> store.moveCell(diff.cellId, to = diff.toIndex)
                is SettingsRootDiff.UpdateAccessory -> store.updateAccessory(diff.target, diff.accessory)
            }
        }
        return store.state.value.sections.map { section -> section.id to section.cells.map { it.id } }
    }

    private fun layoutOf(t: DSLDiffCalculator.ResolvedTree): List<Pair<String, List<String>>> =
        t.sections.map { section -> section.id to section.cells.map { it.id } }

    private fun moveCells(diffs: List<SettingsRootDiff>) = diffs.filterIsInstance<SettingsRootDiff.MoveCell>()

    @Test
    fun `途中への1項目の挿入ではInsertCellだけを出し後ろの項目へ移動を出さない`() {
        val before = (0 until 30).map { "item-$it" }
        val after = before.toMutableList().apply { add(12, "inserted") }
        val old = singleSection(before)
        val new = singleSection(after)

        val diffs = DSLDiffCalculator.compute(old, new)

        assertEquals(1, diffs.size)
        val ins = diffs[0] as SettingsRootDiff.InsertCell
        assertEquals(12, ins.index)
        assertEquals("inserted", ins.cell.id)
        assertEquals(layoutOf(new), applyToStore(old, diffs))
    }

    @Test
    fun `途中の1項目の削除ではRemoveCellだけを出し後ろの項目へ移動を出さない`() {
        val before = (0 until 30).map { "item-$it" }
        val after = before.filter { it != "item-5" }
        val old = singleSection(before)
        val new = singleSection(after)

        val diffs = DSLDiffCalculator.compute(old, new)

        assertEquals(listOf<SettingsRootDiff>(SettingsRootDiff.RemoveCell(cellId = "item-5")), diffs)
        assertEquals(layoutOf(new), applyToStore(old, diffs))
    }

    @Test
    fun `先頭の項目を末尾へ送る並べ替えでは移動を1件だけ出す`() {
        val old = singleSection(listOf("a", "b", "c", "d", "e"))
        val new = singleSection(listOf("b", "c", "d", "e", "a"))

        val diffs = DSLDiffCalculator.compute(old, new)

        assertEquals(listOf(SettingsRootDiff.MoveCell(cellId = "a", toIndex = 4)), diffs)
        assertEquals(layoutOf(new), applyToStore(old, diffs))
    }

    @Test
    fun `逆順への並べ替えでは必要な移動を出し宣言の並びに着地する`() {
        val old = singleSection(listOf("a", "b", "c", "d"))
        val new = singleSection(listOf("d", "c", "b", "a"))

        val diffs = DSLDiffCalculator.compute(old, new)

        // 相対順序を保てるのは 1 項目だけなので、残りの 3 項目に移動が要る
        assertEquals(3, moveCells(diffs).size)
        assertEquals(layoutOf(new), applyToStore(old, diffs))
    }

    @Test
    fun `挿入と削除と並べ替えの組み合わせでも宣言の並びに着地する`() {
        val old = singleSection(listOf("a", "b", "c", "d", "e", "f", "g"))
        // b を削除、x と y を挿入、f を先頭側へ、a を後ろへ
        val new = singleSection(listOf("x", "f", "c", "d", "a", "y", "e", "g"))

        val diffs = DSLDiffCalculator.compute(old, new)

        // 相対順序が変わったのは f と a の 2 項目だけ（c・d・e・g は並びを保つ）
        assertEquals(setOf("f", "a"), moveCells(diffs).map { it.cellId }.toSet())
        assertEquals(layoutOf(new), applyToStore(old, diffs))
    }

    @Test
    fun `並べ替えの組み合わせを網羅しても常に宣言の並びに着地し移動は最小になる`() {
        // 5 項目の全順列 × 追加・削除の有無で、Store に適用した結果が宣言と一致し、
        // 移動の件数が「項目数 - 相対順序を保てる最大の項目数」に等しいことを確かめる。
        val base = listOf("a", "b", "c", "d", "e")
        for (perm in permutations(base)) {
            for (variant in 0 until 3) {
                val target = when (variant) {
                    0 -> perm
                    1 -> perm.toMutableList().apply { add(2, "new") }
                    else -> perm.filter { it != "c" }
                }
                val old = singleSection(base)
                val new = singleSection(target)
                val diffs = DSLDiffCalculator.compute(old, new)
                assertEquals("perm=$perm variant=$variant", layoutOf(new), applyToStore(old, diffs))

                val kept = target.filter { it in base }
                val keptOld = base.filter { it in kept }
                val expectedMoves = kept.size - lisLength(kept.map { keptOld.indexOf(it) })
                assertEquals("perm=$perm variant=$variant", expectedMoves, moveCells(diffs).size)
            }
        }
    }

    @Test
    fun `Section の途中への挿入では後ろの Section へ移動を出さず宣言の並びに着地する`() {
        val s = (0 until 5).map { Section(id = "s$it", cells = listOf(cell("c$it"))) }
        val inserted = Section(id = "new", cells = listOf(cell("cn")))
        val old = tree(s)
        val new = tree(s.toMutableList().apply { add(0, inserted) })

        val diffs = DSLDiffCalculator.compute(old, new)

        assertEquals(1, diffs.size)
        assertTrue(diffs[0] is SettingsRootDiff.InsertSection)
        assertEquals(layoutOf(new), applyToStore(old, diffs))
    }

    @Test
    fun `Section の挿入と並べ替えの組み合わせでも宣言の並びに着地する`() {
        val s = (0 until 5).associate { "s$it" to Section(id = "s$it", cells = listOf(cell("c$it"))) }
        val inserted = Section(id = "new", cells = listOf(cell("cn")))
        val old = tree(listOf("s0", "s1", "s2", "s3", "s4").map { s.getValue(it) })
        val new = tree(listOf("s4", "s0", "new", "s2", "s1").map { if (it == "new") inserted else s.getValue(it) })

        val diffs = DSLDiffCalculator.compute(old, new)

        // s3 の削除、new の挿入に加え、相対順序が変わった s4 と s1（または等価な 2 件）だけを移す
        assertEquals(2, diffs.filterIsInstance<SettingsRootDiff.MoveSection>().size)
        assertEquals(layoutOf(new), applyToStore(old, diffs))
    }

    private fun permutations(items: List<String>): List<List<String>> {
        if (items.size <= 1) return listOf(items)
        return items.flatMap { head -> permutations(items - head).map { listOf(head) + it } }
    }

    /** 狭義の最長増加部分列の長さ（期待値の算出用。O(n^2) の素朴な実装）。 */
    private fun lisLength(values: List<Int>): Int {
        if (values.isEmpty()) return 0
        val best = IntArray(values.size) { 1 }
        for (i in values.indices) {
            for (j in 0 until i) {
                if (values[j] < values[i]) best[i] = maxOf(best[i], best[j] + 1)
            }
        }
        return best.max()
    }
}
