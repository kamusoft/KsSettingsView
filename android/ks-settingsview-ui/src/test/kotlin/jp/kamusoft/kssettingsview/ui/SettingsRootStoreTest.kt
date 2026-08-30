package jp.kamusoft.kssettingsview.ui

import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.RadioCell
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `SettingsRootStore` の各メソッドが期待通り `state` を更新し、`diffs` を emit することを検証する。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRootStoreTest {

    // MARK: - 初期化

    @Test
    fun `init で initialRoot が state value に反映される`() {
        val initial = SettingsRoot(
            sections = listOf(
                Section(id = "s1", header = SectionAccessory.Text("S"), cells = listOf(LabelCell(id = "c1", title = "A"))),
            ),
        )
        val store = SettingsRootStore(initialRoot = initial)
        assertEquals(initial, store.state.value)
    }

    // MARK: - replaceAll

    @Test
    fun `replaceAll で state が更新され Full Diff が emit される`() = runTest {
        val store = SettingsRootStore(initialRoot = SettingsRoot())

        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        val newRoot = SettingsRoot(
            sections = listOf(
                Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A"))),
            ),
        )
        store.replaceAll(newRoot)
        advanceUntilIdle()

        assertEquals(newRoot, store.state.value)
        assertEquals(1, emitted.size)
        assertEquals(SettingsRootDiff.Full(newRoot), emitted[0])

        job.cancel()
    }

    // MARK: - insertSection

    @Test
    fun `insertSection で sections に追加され InsertSection Diff が emit される`() = runTest {
        val store = SettingsRootStore(initialRoot = SettingsRoot())
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        val newSection = Section(id = "s1", header = SectionAccessory.Text("X"))
        store.insertSection(newSection, at = 0)
        advanceUntilIdle()

        assertEquals(1, store.state.value.sections.size)
        assertEquals(newSection, store.state.value.sections[0])
        assertEquals(SettingsRootDiff.InsertSection(index = 0, section = newSection), emitted[0])

        job.cancel()
    }

    // MARK: - removeSection

    @Test
    fun `removeSection で sections から削除され RemoveSection Diff が emit される`() = runTest {
        val sec = Section(id = "s1", header = SectionAccessory.Text("X"))
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(sec)))
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        store.removeSection(sectionId = "s1")
        advanceUntilIdle()

        assertEquals(0, store.state.value.sections.size)
        assertEquals(SettingsRootDiff.RemoveSection(sectionId = "s1"), emitted[0])

        job.cancel()
    }

    // MARK: - moveSection

    @Test
    fun `moveSection で順序が変わり MoveSection Diff が emit される`() = runTest {
        val s1 = Section(id = "s1")
        val s2 = Section(id = "s2")
        val s3 = Section(id = "s3")
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(s1, s2, s3)))
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        store.moveSection(from = 0, to = 2)
        advanceUntilIdle()

        assertEquals(listOf("s2", "s3", "s1"), store.state.value.sections.map { it.id })
        assertEquals(SettingsRootDiff.MoveSection(from = 0, to = 2), emitted[0])

        job.cancel()
    }

    // MARK: - replaceSection

    @Test
    fun `replaceSection で Section が置換され ReplaceSection Diff が emit される`() = runTest {
        val old = Section(id = "s1", header = SectionAccessory.Text("old"))
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(old)))
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        val new = Section(id = "s1", header = SectionAccessory.Text("new"))
        store.replaceSection(sectionId = "s1", new = new)
        advanceUntilIdle()

        assertEquals(SectionAccessory.Text("new"), store.state.value.sections[0].header)
        assertEquals(SettingsRootDiff.ReplaceSection(sectionId = "s1", newSection = new), emitted[0])

        job.cancel()
    }

    // MARK: - insertCell

    @Test
    fun `insertCell で Section の cells に追加され InsertCell Diff が emit される`() = runTest {
        val sec = Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A")))
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(sec)))
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        val newCell = LabelCell(id = "c2", title = "B")
        store.insertCell(cell = newCell, sectionId = "s1", at = 0)
        advanceUntilIdle()

        assertEquals(2, store.state.value.sections[0].cells.size)
        assertEquals("c2", store.state.value.sections[0].cells[0].id)
        assertEquals(SettingsRootDiff.InsertCell(sectionId = "s1", index = 0, cell = newCell), emitted[0])

        job.cancel()
    }

    // MARK: - removeCell

    @Test
    fun `removeCell で Cell が削除され RemoveCell Diff が emit される`() = runTest {
        val sec = Section(
            id = "s1",
            cells = listOf(
                LabelCell(id = "c1", title = "A"),
                LabelCell(id = "c2", title = "B"),
            ),
        )
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(sec)))
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        store.removeCell(cellId = "c1")
        advanceUntilIdle()

        assertEquals(1, store.state.value.sections[0].cells.size)
        assertEquals("c2", store.state.value.sections[0].cells[0].id)
        assertEquals(SettingsRootDiff.RemoveCell(cellId = "c1"), emitted[0])

        job.cancel()
    }

    // MARK: - replaceCell

    @Test
    fun `replaceCell で Cell が置換され ReplaceCell Diff が emit される`() = runTest {
        val sec = Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "old")))
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(sec)))
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        val newCell = LabelCell(id = "c1", title = "new")
        store.replaceCell(cellId = "c1", new = newCell)
        advanceUntilIdle()

        val replaced = store.state.value.sections[0].cells[0] as LabelCell
        assertEquals("new", replaced.title)
        assertEquals(SettingsRootDiff.ReplaceCell(cellId = "c1", newCell = newCell), emitted[0])

        job.cancel()
    }

    // MARK: - moveCell

    @Test
    fun `moveCell で Cell が移動し MoveCell Diff が emit される`() = runTest {
        val sec = Section(
            id = "s1",
            cells = listOf(
                LabelCell(id = "c1", title = "A"),
                LabelCell(id = "c2", title = "B"),
                LabelCell(id = "c3", title = "C"),
            ),
        )
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(sec)))
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        store.moveCell(cellId = "c1", to = 2)
        advanceUntilIdle()

        assertEquals(listOf("c2", "c3", "c1"), store.state.value.sections[0].cells.map { it.id })
        assertEquals(SettingsRootDiff.MoveCell(cellId = "c1", toIndex = 2), emitted[0])

        job.cancel()
    }

    // MARK: - updateAccessory

    @Test
    fun `updateAccessory_RootHeader は state を変更せず Diff のみ emit される`() = runTest {
        val store = SettingsRootStore(initialRoot = SettingsRoot())
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        val acc = SettingsAccessory.Root(RootAccessory.Text("X"))
        store.updateAccessory(target = AccessoryTarget.RootHeader, accessory = acc)
        advanceUntilIdle()

        assertEquals(0, store.state.value.sections.size)
        assertEquals(
            SettingsRootDiff.UpdateAccessory(target = AccessoryTarget.RootHeader, accessory = acc),
            emitted[0],
        )

        job.cancel()
    }

    @Test
    fun `updateAccessory_SectionHeader で Section の header が更新される`() = runTest {
        val sec = Section(id = "s1", header = SectionAccessory.Text("old"))
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(sec)))
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        val acc = SettingsAccessory.Section(SectionAccessory.Text("new"))
        store.updateAccessory(
            target = AccessoryTarget.SectionHeader(sectionId = "s1"),
            accessory = acc,
        )
        advanceUntilIdle()

        assertEquals(SectionAccessory.Text("new"), store.state.value.sections[0].header)
        assertEquals(
            SettingsRootDiff.UpdateAccessory(
                target = AccessoryTarget.SectionHeader(sectionId = "s1"),
                accessory = acc,
            ),
            emitted[0],
        )

        job.cancel()
    }

    /** 既知 sectionId の header / footer はともに現在状態へ反映され、対応する Diff が emit される。 */
    @Test
    fun `updateAccessory_既知sectionIdはheaderもfooterも反映されDiffがemitされる`() = runTest {
        val sec = Section(
            id = "s1",
            header = SectionAccessory.Text("H-old"),
            footer = SectionAccessory.Text("F-old"),
        )
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(sec)))
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        val headerAcc = SettingsAccessory.Section(SectionAccessory.Text("H-new"))
        val footerAcc = SettingsAccessory.Section(SectionAccessory.Text("F-new"))
        store.updateAccessory(target = AccessoryTarget.SectionHeader(sectionId = "s1"), accessory = headerAcc)
        store.updateAccessory(target = AccessoryTarget.SectionFooter(sectionId = "s1"), accessory = footerAcc)
        advanceUntilIdle()

        assertEquals(SectionAccessory.Text("H-new"), store.state.value.sections[0].header)
        assertEquals(SectionAccessory.Text("F-new"), store.state.value.sections[0].footer)
        assertEquals(
            listOf(
                SettingsRootDiff.UpdateAccessory(
                    target = AccessoryTarget.SectionHeader(sectionId = "s1"),
                    accessory = headerAcc,
                ),
                SettingsRootDiff.UpdateAccessory(
                    target = AccessoryTarget.SectionFooter(sectionId = "s1"),
                    accessory = footerAcc,
                ),
            ),
            emitted,
        )

        job.cancel()
    }

    /**
     * Root 系 target は `SettingsRoot` 値型に state を持たないため sectionId 検証の対象外であり、
     * header / footer とも Diff を emit する。
     */
    @Test
    fun `updateAccessory_Root系targetはheaderもfooterもDiffをemitする`() = runTest {
        val store = SettingsRootStore(initialRoot = SettingsRoot())
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        val headerAcc = SettingsAccessory.Root(RootAccessory.Text("ROOT-H"))
        val footerAcc = SettingsAccessory.Root(RootAccessory.Text("ROOT-F"))
        store.updateAccessory(target = AccessoryTarget.RootHeader, accessory = headerAcc)
        store.updateAccessory(target = AccessoryTarget.RootFooter, accessory = footerAcc)
        advanceUntilIdle()

        assertEquals(
            listOf(
                SettingsRootDiff.UpdateAccessory(target = AccessoryTarget.RootHeader, accessory = headerAcc),
                SettingsRootDiff.UpdateAccessory(target = AccessoryTarget.RootFooter, accessory = footerAcc),
            ),
            emitted,
        )

        job.cancel()
    }

    // MARK: - applyTheme（purify-core-extract-style-to-ui-layer）

    @Test
    fun `applyTheme で theme StateFlow が更新され Diff は emit されない`() = runTest {
        val store = SettingsRootStore(initialRoot = SettingsRoot())
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        val newTheme = Theme(separatorColor = Color(0xFF1A334D))
        store.applyTheme(newTheme)
        advanceUntilIdle()

        // Theme StateFlow が更新されること
        assertEquals(newTheme, store.theme.value)
        // SettingsRootDiff には何も emit されないこと（Theme は独立 StateFlow で配信）
        assertTrue("applyTheme で Diff が emit されてはならない", emitted.isEmpty())

        job.cancel()
    }

    @Test
    fun `Store の initialTheme 引数で theme StateFlow が初期化される`() {
        val initialTheme = Theme(separatorColor = Color(0xFFE6DAB9))
        val store = SettingsRootStore(initialRoot = SettingsRoot(), initialTheme = initialTheme)
        assertEquals(initialTheme, store.theme.value)
    }

    @Test
    fun `Store の initialTheme 省略時は Theme 既定値で初期化される`() {
        val store = SettingsRootStore(initialRoot = SettingsRoot())
        assertEquals(Theme(), store.theme.value)
    }

    // MARK: - preview

    @Test
    fun `preview ファクトリで作った store は渡した root を保持する`() {
        val initial = SettingsRoot(
            sections = listOf(Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A")))),
        )
        val store = SettingsRootStore.preview(root = initial)
        assertEquals(initial, store.state.value)
    }

    // MARK: - 存在しない ID への操作（no-op 契約）

    // Store の各メソッドが「対象 ID が存在しない場合」に state を変更せず、Diff も emit しない
    // （no-op になる）ことを検証する。これにより `applyDiff` 側のエラー検出パスを誤発火させない
    // 「safe by default」契約を担保する。

    @Test
    fun `removeSection_存在しないIDではstate変更もDiff発行もされない`() = runTest {
        val sec = Section(id = "s1")
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(sec)))
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        store.removeSection(sectionId = "bogus")
        advanceUntilIdle()

        assertEquals(1, store.state.value.sections.size)
        assertEquals(0, emitted.size)

        job.cancel()
    }

    @Test
    fun `moveSection_範囲外fromではstate変更もDiff発行もされない`() = runTest {
        val s1 = Section(id = "s1")
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(s1)))
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        store.moveSection(from = 5, to = 0)
        advanceUntilIdle()

        assertEquals(listOf("s1"), store.state.value.sections.map { it.id })
        assertEquals(0, emitted.size)

        job.cancel()
    }

    @Test
    fun `replaceSection_存在しないIDではstate変更もDiff発行もされない`() = runTest {
        val sec = Section(id = "s1", header = SectionAccessory.Text("S"))
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(sec)))
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        val other = Section(id = "bogus", header = SectionAccessory.Text("other"))
        store.replaceSection(sectionId = "bogus", new = other)
        advanceUntilIdle()

        assertEquals(SectionAccessory.Text("S"), store.state.value.sections[0].header)
        assertEquals(0, emitted.size)

        job.cancel()
    }

    @Test
    fun `insertCell_存在しないsectionIDではstate変更もDiff発行もされない`() = runTest {
        val store = SettingsRootStore(initialRoot = SettingsRoot())
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        store.insertCell(cell = LabelCell(id = "c1", title = "X"), sectionId = "bogus", at = 0)
        advanceUntilIdle()

        assertEquals(0, store.state.value.sections.size)
        assertEquals(0, emitted.size)

        job.cancel()
    }

    @Test
    fun `removeCell_存在しないcellIDではstate変更もDiff発行もされない`() = runTest {
        val sec = Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A")))
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(sec)))
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        store.removeCell(cellId = "bogus")
        advanceUntilIdle()

        assertEquals(1, store.state.value.sections[0].cells.size)
        assertEquals(0, emitted.size)

        job.cancel()
    }

    @Test
    fun `replaceCell_存在しないcellIDではstate変更もDiff発行もされない`() = runTest {
        val sec = Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A")))
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(sec)))
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        store.replaceCell(cellId = "bogus", new = LabelCell(id = "bogus", title = "new"))
        advanceUntilIdle()

        val title = (store.state.value.sections[0].cells[0] as LabelCell).title
        assertEquals("A", title)
        assertEquals(0, emitted.size)

        job.cancel()
    }

    @Test
    fun `moveCell_存在しないcellIDではstate変更もDiff発行もされない`() = runTest {
        val sec = Section(
            id = "s1",
            cells = listOf(
                LabelCell(id = "c1", title = "A"),
                LabelCell(id = "c2", title = "B"),
            ),
        )
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(sec)))
        val emitted = mutableListOf<SettingsRootDiff>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }

        store.moveCell(cellId = "bogus", to = 0)
        advanceUntilIdle()

        assertEquals(listOf("c1", "c2"), store.state.value.sections[0].cells.map { it.id })
        assertEquals(0, emitted.size)

        job.cancel()
    }

    /**
     * 未知 sectionId の section header 更新は、現在状態も state Flow も diffs Flow も動かさない。
     *
     * 観測は Diff 件数だけでなく `state` の emit 件数まで含めて no-op を確認する
     * （初期値の 1 件を除いた追加 emit が 0 件であること）。
     */
    @Test
    fun `updateAccessory_未知sectionIdのSectionHeaderはstate変更もDiff発行もされない`() = runTest {
        val sec = Section(
            id = "s1",
            header = SectionAccessory.Text("H"),
            footer = SectionAccessory.Text("F"),
        )
        val initial = SettingsRoot(sections = listOf(sec))
        val store = SettingsRootStore(initialRoot = initial)
        val emitted = mutableListOf<SettingsRootDiff>()
        val states = mutableListOf<SettingsRoot>()
        val diffJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }
        val stateJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.state.collect { states.add(it) }
        }

        store.updateAccessory(
            target = AccessoryTarget.SectionHeader(sectionId = "bogus"),
            accessory = SettingsAccessory.Section(SectionAccessory.Text("X")),
        )
        advanceUntilIdle()

        assertEquals(initial, store.state.value)
        assertEquals("state Flow へ emit されない（初期値のみ）", listOf(initial), states)
        assertEquals(0, emitted.size)

        diffJob.cancel()
        stateJob.cancel()
    }

    /** 未知 sectionId の section footer 更新も同じく no-op になる。 */
    @Test
    fun `updateAccessory_未知sectionIdのSectionFooterはstate変更もDiff発行もされない`() = runTest {
        val sec = Section(
            id = "s1",
            header = SectionAccessory.Text("H"),
            footer = SectionAccessory.Text("F"),
        )
        val initial = SettingsRoot(sections = listOf(sec))
        val store = SettingsRootStore(initialRoot = initial)
        val emitted = mutableListOf<SettingsRootDiff>()
        val states = mutableListOf<SettingsRoot>()
        val diffJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.diffs.collect { emitted.add(it) }
        }
        val stateJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.state.collect { states.add(it) }
        }

        store.updateAccessory(
            target = AccessoryTarget.SectionFooter(sectionId = "bogus"),
            accessory = SettingsAccessory.Section(SectionAccessory.Text("X")),
        )
        advanceUntilIdle()

        assertEquals(initial, store.state.value)
        assertEquals("state Flow へ emit されない（初期値のみ）", listOf(initial), states)
        assertEquals(0, emitted.size)

        diffJob.cancel()
        stateJob.cancel()
    }

    // MARK: - replaceCells（複数 Cell 一括内容更新 / RadioCell グループ連動）

    /**
     * RadioCell グループ連動のリグレッション検証。
     *
     * 別項目をタップすると同一グループの全 RadioCell の `selectedValue` が同時に変化する。このとき
     * 個別 `replaceCell` を複数回呼ぶと UI 層で submitList が連続発行され、AsyncListDiffer が一部の
     * notifyItemChanged を破棄して旧選択セルの ✓ が消えない（複数 ✓）不具合が起きていた。
     * `replaceCells` は対象 cellId 群を **1 件のバッチ**として `contentUpdateBatches` に配信し、
     * UI 層が単一 submitList + 複数 notifyItemChanged で反映できることを保証する。
     */
    @Test
    fun `replaceCells はRadioグループのselectedValue変化を1バッチで配信する`() = runTest {
        // selectedValue=light（Light が選択中）の 3 つの RadioCell。
        val sec = Section(
            id = "g",
            cells = listOf(
                RadioCell(id = "r-light", title = "Light", groupId = "theme", value = "light", selectedValue = "light"),
                RadioCell(id = "r-dark", title = "Dark", groupId = "theme", value = "dark", selectedValue = "light"),
                RadioCell(id = "r-auto", title = "Auto", groupId = "theme", value = "auto", selectedValue = "light"),
            ),
        )
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(sec)))

        val batches = mutableListOf<List<String>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.contentUpdateBatches.collect { batches.add(it) }
        }

        // Dark をタップ → selectedValue が light→dark に変わる。全 3 セルの selectedValue が更新される。
        val updated = sec.cells.map { cell ->
            cell.id to (cell as RadioCell).copy(selectedValue = "dark")
        }
        store.replaceCells(updated)
        advanceUntilIdle()

        // state: 全セルの selectedValue が dark に更新されている。
        val cells = store.state.value.sections[0].cells.map { it as RadioCell }
        assertEquals(listOf("dark", "dark", "dark"), cells.map { it.selectedValue })
        // 選択状態（value == selectedValue）は dark のみ true（複数 ✓ にならない）。
        assertEquals(listOf(false, true, false), cells.map { it.value == it.selectedValue })

        // バッチは 1 件のみ、かつ全 3 cellId を含む（連続 submitList による取りこぼしが起きない）。
        assertEquals(1, batches.size)
        assertEquals(listOf("r-light", "r-dark", "r-auto"), batches[0])

        job.cancel()
    }

    /**
     * `replaceCells` は存在しない id を無視し、適用 0 件ならバッチを配信しない（no-op）。
     */
    @Test
    fun `replaceCells は存在しないidを無視し適用0件なら配信しない`() = runTest {
        val sec = Section(
            id = "s1",
            cells = listOf(LabelCell(id = "c1", title = "A")),
        )
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(sec)))

        val batches = mutableListOf<List<String>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.contentUpdateBatches.collect { batches.add(it) }
        }

        store.replaceCells(listOf("bogus" to LabelCell(id = "bogus", title = "X")))
        advanceUntilIdle()

        assertEquals(SettingsRoot(sections = listOf(sec)), store.state.value)
        assertEquals(0, batches.size)

        job.cancel()
    }
}
