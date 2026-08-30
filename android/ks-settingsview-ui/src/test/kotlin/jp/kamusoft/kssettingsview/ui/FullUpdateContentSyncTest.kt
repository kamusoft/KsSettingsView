package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `submitList` を通る全経路で内容の取りこぼしが起きないことを検証する（android/ADR-0012）。
 *
 * 検証対象は 2 系統ある。
 *
 * - Section H/F の accessory の内容更新（`updateAccessory` / `replaceSection`）が
 *   payload 付き変更通知として表示に届くこと。accessory の null ↔ 非 null は行の挿入・削除になること
 * - full 更新経路（`replaceSection` / `SettingsRootDiff.Full` / root の再設定）で、更新をまたいで残る
 *   同一 id の Cell の内容が表示に届くこと。新規・削除される Cell へは内容通知を発行しないこと
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FullUpdateContentSyncTest {

    private val ctx: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun makeView(sections: List<Section>): KsSettingsView {
        val view = KsSettingsView(ctx)
        view.setRootDirect(SettingsRoot(sections = sections))
        idle()
        return view
    }

    /** コミット済みの平坦リストを、失敗メッセージ用に行種別と内容へ要約する。 */
    private fun committedSummary(adapter: KsSettingsListAdapter): List<String> =
        adapter.currentList.map { item ->
            when (item) {
                is CellListItem.SectionHeader -> "header"
                is CellListItem.SectionFooter -> "footer"
                is CellListItem.CellRow -> "cell(${(item.cell as? LabelCell)?.title})"
            }
        }

    /** 平坦リストの [position] を新しい ViewHolder へ bind し、Section Text の表示文字列を返す。 */
    private fun bindSectionText(adapter: KsSettingsListAdapter, position: Int): String {
        val holder = SectionTextAccessoryViewHolder.create(RecyclerView(ctx))
        adapter.onBindViewHolder(holder, position)
        return (holder.itemView as TextView).text.toString()
    }

    /** 平坦リストの [position] を新しい ViewHolder へ bind し、Section View の子 View を返す。 */
    private fun bindSectionAnyView(adapter: KsSettingsListAdapter, position: Int): android.view.View? {
        val holder = SectionAnyViewAccessoryViewHolder.create(RecyclerView(ctx))
        adapter.onBindViewHolder(holder, position)
        return (holder.itemView as FrameLayout).getChildAt(0)
    }

    /** 平坦リストの [position] を新しい ViewHolder へ bind し、Cell の title 表示を返す。 */
    private fun bindCellTitle(adapter: KsSettingsListAdapter, position: Int): String {
        val holder = LabelCellViewHolder.create(FrameLayout(ctx))
        adapter.onBindViewHolder(holder, position)
        return holder.views.titleView.text.toString()
    }

    private fun sectionAccessory(accessory: SectionAccessory): SettingsAccessory =
        SettingsAccessory.Section(accessory)

    /** dp 指定の Header 高さを、bind が使うのと同じ換算で px へ直す。 */
    private fun dpToPx(dp: Double): Int = (dp * ctx.resources.displayMetrics.density).toInt()

    // MARK: - Section accessory の内容更新

    @Test
    fun `updateAccessory による header text 変更が表示と payload 付き通知に反映される`() {
        val view = makeView(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.Text("一般"),
                    cells = listOf(LabelCell(id = "c1", title = "A")),
                ),
            ),
        )
        val adapter = view.internalMainListAdapter()
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        view.applyDiff(
            SettingsRootDiff.UpdateAccessory(
                target = AccessoryTarget.SectionHeader("s1"),
                accessory = sectionAccessory(SectionAccessory.Text("詳細")),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { recorder.changed.isNotEmpty() }

        assertEquals("詳細", bindSectionText(adapter, 0))
        assertEquals(
            "header 行へ payload 付きの変更通知が発行される",
            listOf(ChangeRecord(0, 1, KsSettingsListAdapter.PAYLOAD_CONTENT)),
            recorder.changed,
        )
    }

    @Test
    fun `updateAccessory による footer text 変更が表示と payload 付き通知に反映される`() {
        val view = makeView(
            listOf(
                Section(
                    id = "s1",
                    cells = listOf(LabelCell(id = "c1", title = "A")),
                    footer = SectionAccessory.Text("説明"),
                ),
            ),
        )
        val adapter = view.internalMainListAdapter()
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        view.applyDiff(
            SettingsRootDiff.UpdateAccessory(
                target = AccessoryTarget.SectionFooter("s1"),
                accessory = sectionAccessory(SectionAccessory.Text("別の説明")),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { recorder.changed.isNotEmpty() }

        // 平坦リストは [CellRow, SectionFooter] の 2 件。
        assertEquals("別の説明", bindSectionText(adapter, 1))
        assertEquals(
            listOf(ChangeRecord(1, 1, KsSettingsListAdapter.PAYLOAD_CONTENT)),
            recorder.changed,
        )
    }

    @Test
    fun `replaceSection による header text 変更が表示へ反映される`() {
        val view = makeView(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.Text("一般"),
                    cells = listOf(LabelCell(id = "c1", title = "A")),
                ),
            ),
        )
        val adapter = view.internalMainListAdapter()
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        view.applyDiff(
            SettingsRootDiff.ReplaceSection(
                sectionId = "s1",
                newSection = Section(
                    id = "s1",
                    header = SectionAccessory.Text("詳細"),
                    cells = listOf(LabelCell(id = "c1", title = "A")),
                ),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { recorder.changed.isNotEmpty() }

        assertEquals("詳細", bindSectionText(adapter, 0))
        // 新規 ViewHolder への bind は通知の有無と無関係に currentList の新しい値を返すため、
        // 検出力を持つのは payload 付き変更通知の照合のほう。
        assertEquals(
            listOf(ChangeRecord(0, 1, KsSettingsListAdapter.PAYLOAD_CONTENT)),
            recorder.changed,
        )
    }

    @Test
    fun `replaceSection による headerHeight 変更が表示と payload 付き通知に反映される`() {
        val view = makeView(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.Text("一般"),
                    cells = listOf(LabelCell(id = "c1", title = "A")),
                ),
            ),
        )
        val adapter = view.internalMainListAdapter()
        // 同一 ViewHolder を使い回して bind し直す。payload 付き通知が落ちる先と同じ経路で
        // 高さが更新されることを見るため、新規 ViewHolder への bind では代用しない。
        val holder = SectionTextAccessoryViewHolder.create(RecyclerView(ctx))
        adapter.onBindViewHolder(holder, 0)
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, holder.itemView.layoutParams.height)

        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        // header text は据え置き、固定高さだけを変える。
        view.applyDiff(
            SettingsRootDiff.ReplaceSection(
                sectionId = "s1",
                newSection = Section(
                    id = "s1",
                    header = SectionAccessory.Text("一般"),
                    headerHeight = 48.0,
                    cells = listOf(LabelCell(id = "c1", title = "A")),
                ),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { recorder.changed.isNotEmpty() }

        adapter.onBindViewHolder(holder, 0)
        assertEquals(dpToPx(48.0), holder.itemView.layoutParams.height)
        assertEquals(
            listOf(ChangeRecord(0, 1, KsSettingsListAdapter.PAYLOAD_CONTENT)),
            recorder.changed,
        )
    }

    @Test
    fun `Full diff による headerHeight 変更が表示と payload 付き通知に反映される`() {
        val view = makeView(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.Text("一般"),
                    headerHeight = 48.0,
                    cells = listOf(LabelCell(id = "c1", title = "A")),
                ),
            ),
        )
        val adapter = view.internalMainListAdapter()
        val holder = SectionTextAccessoryViewHolder.create(RecyclerView(ctx))
        adapter.onBindViewHolder(holder, 0)
        assertEquals(dpToPx(48.0), holder.itemView.layoutParams.height)

        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        view.applyDiff(
            SettingsRootDiff.Full(
                SettingsRoot(
                    sections = listOf(
                        Section(
                            id = "s1",
                            header = SectionAccessory.Text("一般"),
                            headerHeight = 96.0,
                            cells = listOf(LabelCell(id = "c1", title = "A")),
                        ),
                    ),
                ),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { recorder.changed.isNotEmpty() }

        adapter.onBindViewHolder(holder, 0)
        assertEquals(dpToPx(96.0), holder.itemView.layoutParams.height)
        assertEquals(
            listOf(ChangeRecord(0, 1, KsSettingsListAdapter.PAYLOAD_CONTENT)),
            recorder.changed,
        )
    }

    @Test
    fun `View accessory の Header では headerHeight の差が高さ payload 付きの通知になる`() {
        val anyView = KsAnyView.AndroidView { c -> TextView(c).apply { text = "V" } }
        val view = makeView(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.View(anyView),
                    cells = listOf(LabelCell(id = "c1", title = "A")),
                ),
            ),
        )
        val adapter = view.internalMainListAdapter()
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        // accessory の中身は据え置き、固定高さだけを変える。
        view.applyDiff(
            SettingsRootDiff.Full(
                SettingsRoot(
                    sections = listOf(
                        Section(
                            id = "s1",
                            header = SectionAccessory.View(anyView),
                            headerHeight = 48.0,
                            cells = listOf(LabelCell(id = "c1", title = "A")),
                        ),
                    ),
                ),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { recorder.changed.isNotEmpty() }

        assertEquals(
            "高さだけの変更は中身を作り直さない高さ payload で通知される",
            listOf(ChangeRecord(0, 1, KsSettingsListAdapter.PAYLOAD_HEADER_HEIGHT)),
            recorder.changed,
        )
    }

    @Test
    fun `View accessory の Header は高さ payload で中身を作り直さずに固定高さだけを更新する`() {
        val anyView = KsAnyView.AndroidView { c -> TextView(c).apply { text = "V" } }
        val view = makeView(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.View(anyView),
                    cells = listOf(LabelCell(id = "c1", title = "A")),
                ),
            ),
        )
        val adapter = view.internalMainListAdapter()
        val holder = SectionAnyViewAccessoryViewHolder.create(RecyclerView(ctx))
        adapter.onBindViewHolder(holder, 0)
        val boundChild = (holder.itemView as FrameLayout).getChildAt(0)
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, holder.itemView.layoutParams.height)

        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        view.applyDiff(
            SettingsRootDiff.Full(
                SettingsRoot(
                    sections = listOf(
                        Section(
                            id = "s1",
                            header = SectionAccessory.View(anyView),
                            headerHeight = 48.0,
                            cells = listOf(LabelCell(id = "c1", title = "A")),
                        ),
                    ),
                ),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { recorder.changed.isNotEmpty() }

        // 実際に発行された payload をそのまま渡し、通知が落ちる先と同じ経路で反映させる。
        adapter.onBindViewHolder(holder, 0, recorder.changed.mapNotNull { it.payload })

        assertEquals(dpToPx(48.0), holder.itemView.layoutParams.height)
        assertEquals(
            "高さだけの変更では accessory の View インスタンスが維持される",
            boundChild,
            (holder.itemView as FrameLayout).getChildAt(0),
        )
    }

    @Test
    fun `accessory の型の切替が表示へ反映される`() {
        val view = makeView(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.Text("一般"),
                    cells = listOf(LabelCell(id = "c1", title = "A")),
                ),
            ),
        )
        val adapter = view.internalMainListAdapter()
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        view.applyDiff(
            SettingsRootDiff.UpdateAccessory(
                target = AccessoryTarget.SectionHeader("s1"),
                accessory = sectionAccessory(
                    SectionAccessory.View(KsAnyView.AndroidView { c -> TextView(c).apply { text = "V" } }),
                ),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { recorder.changed.isNotEmpty() }

        // 行の安定 ID は維持したまま view type だけが View 用に切り替わる。
        assertEquals(
            KsCellRegistry.VIEW_TYPE_SECTION_HEADER_VIEW,
            adapter.getItemViewType(0),
        )
        val child = bindSectionAnyView(adapter, 0)
        assertEquals("V", (child as TextView).text.toString())
        assertEquals(listOf(0), recorder.changed.map { it.positionStart })
    }

    @Test
    fun `View accessory の差し替えが表示と payload 付き通知に反映される`() {
        val view = makeView(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.View(
                        KsAnyView.AndroidView { c -> TextView(c).apply { text = "旧" } },
                    ),
                    cells = listOf(LabelCell(id = "c1", title = "A")),
                ),
            ),
        )
        val adapter = view.internalMainListAdapter()
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        view.applyDiff(
            SettingsRootDiff.UpdateAccessory(
                target = AccessoryTarget.SectionHeader("s1"),
                accessory = sectionAccessory(
                    SectionAccessory.View(KsAnyView.AndroidView { c -> TextView(c).apply { text = "新" } }),
                ),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { recorder.changed.isNotEmpty() }

        val child = bindSectionAnyView(adapter, 0)
        assertEquals("新", (child as TextView).text.toString())
        assertEquals(
            listOf(ChangeRecord(0, 1, KsSettingsListAdapter.PAYLOAD_CONTENT)),
            recorder.changed,
        )
    }

    @Test
    fun `内容が同一の Section H_F へは変更通知を発行しない`() {
        val view = makeView(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.Text("一般"),
                    cells = listOf(LabelCell(id = "c1", title = "A")),
                    footer = SectionAccessory.Text("説明"),
                ),
                Section(
                    id = "s2",
                    header = SectionAccessory.Text("その他"),
                    cells = listOf(LabelCell(id = "c2", title = "B")),
                ),
            ),
        )
        val adapter = view.internalMainListAdapter()
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        // 別 Section への Cell 追加。Section H/F の内容は一切変わらない。
        view.applyDiff(
            SettingsRootDiff.InsertCell(
                sectionId = "s2",
                index = 1,
                cell = LabelCell(id = "c3", title = "C"),
            ),
        )
        // 初期 5 行 (s1: H + c1 + F / s2: H + c2) に c3 が加わり 6 行になる。
        awaitDifferCommit({ committedSummary(adapter) }) { adapter.itemCount == 6 }

        assertTrue("挿入の構造通知だけが発行される", recorder.inserted.isNotEmpty())
        assertEquals("内容が同一の Section H/F へは変更通知を発行しない", emptyList<ChangeRecord>(), recorder.changed)
    }

    @Test
    fun `header の追加が行の挿入として反映される`() {
        val view = makeView(
            listOf(Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A")))),
        )
        val adapter = view.internalMainListAdapter()
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        view.applyDiff(
            SettingsRootDiff.UpdateAccessory(
                target = AccessoryTarget.SectionHeader("s1"),
                accessory = sectionAccessory(SectionAccessory.Text("一般")),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { adapter.itemCount == 2 }

        assertTrue(adapter.currentList[0] is CellListItem.SectionHeader)
        assertEquals("一般", bindSectionText(adapter, 0))
        assertTrue("header 行は挿入として通知される", recorder.inserted.isNotEmpty())
    }

    @Test
    fun `footer の解除が行の削除として反映される`() {
        val view = makeView(
            listOf(
                Section(
                    id = "s1",
                    cells = listOf(LabelCell(id = "c1", title = "A")),
                    footer = SectionAccessory.Text("説明"),
                ),
            ),
        )
        val adapter = view.internalMainListAdapter()
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        view.applyDiff(
            SettingsRootDiff.UpdateAccessory(
                target = AccessoryTarget.SectionFooter("s1"),
                accessory = null,
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { adapter.itemCount == 1 }

        assertTrue(adapter.currentList.none { it is CellListItem.SectionFooter })
        assertTrue("footer 行は削除として通知される", recorder.removed.isNotEmpty())
    }

    // MARK: - full 更新経路での同一 id の Cell 内容反映

    @Test
    fun `replaceSection で同一 id の Cell 内容変更が表示と payload 付き通知に反映される`() {
        val view = makeView(
            listOf(Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "旧")))),
        )
        val adapter = view.internalMainListAdapter()
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        view.applyDiff(
            SettingsRootDiff.ReplaceSection(
                sectionId = "s1",
                newSection = Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "新"))),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { recorder.changed.isNotEmpty() }

        assertEquals("新", bindCellTitle(adapter, 0))
        assertEquals(
            listOf(ChangeRecord(0, 1, KsSettingsListAdapter.PAYLOAD_CONTENT)),
            recorder.changed,
        )
    }

    @Test
    fun `Full diff で同一 id の Cell 内容変更が表示へ反映される`() {
        val view = makeView(
            listOf(Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "旧")))),
        )
        val adapter = view.internalMainListAdapter()
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        view.applyDiff(
            SettingsRootDiff.Full(
                SettingsRoot(
                    sections = listOf(Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "新")))),
                ),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { recorder.changed.isNotEmpty() }

        assertEquals("新", bindCellTitle(adapter, 0))
        // 新規 ViewHolder への bind は通知の有無と無関係に currentList の新しい値を返すため、
        // 検出力を持つのは payload 付き変更通知の照合のほう。
        assertEquals(
            listOf(ChangeRecord(0, 1, KsSettingsListAdapter.PAYLOAD_CONTENT)),
            recorder.changed,
        )
    }

    @Test
    fun `root の再設定でも同一 id の Cell 内容変更が反映される`() {
        val view = makeView(
            listOf(Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "旧")))),
        )
        val adapter = view.internalMainListAdapter()
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        view.setRootDirect(
            SettingsRoot(
                sections = listOf(Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "新")))),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { recorder.changed.isNotEmpty() }

        assertEquals("新", bindCellTitle(adapter, 0))
        assertEquals(
            listOf(ChangeRecord(0, 1, KsSettingsListAdapter.PAYLOAD_CONTENT)),
            recorder.changed,
        )
    }

    @Test
    fun `空 root への full 更新で表示が空になる`() {
        val view = makeView(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.Text("一般"),
                    cells = listOf(LabelCell(id = "c1", title = "A")),
                ),
            ),
        )
        val adapter = view.internalMainListAdapter()
        assertEquals(2, adapter.itemCount)

        view.applyDiff(SettingsRootDiff.Full(SettingsRoot()))
        awaitDifferCommit({ committedSummary(adapter) }) { adapter.itemCount == 0 }

        assertEquals("古い行が残らない", 0, adapter.itemCount)
    }

    @Test
    fun `Section header footer だけを持つ root への full 更新が反映される`() {
        val view = makeView(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.Text("一般"),
                    cells = listOf(LabelCell(id = "c1", title = "A")),
                ),
            ),
        )
        val adapter = view.internalMainListAdapter()

        view.applyDiff(
            SettingsRootDiff.Full(
                SettingsRoot(
                    sections = listOf(
                        Section(
                            id = "s1",
                            header = SectionAccessory.Text("一般"),
                            cells = emptyList(),
                            footer = SectionAccessory.Text("説明"),
                        ),
                    ),
                ),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) {
            adapter.itemCount == 2 && adapter.currentList.none { it is CellListItem.CellRow }
        }

        assertEquals(2, adapter.itemCount)
        assertTrue("古い Cell 行が残らない", adapter.currentList.none { it is CellListItem.CellRow })
        assertEquals("説明", bindSectionText(adapter, 1))
    }

    @Test
    fun `新規に挿入される Cell へは内容通知を重ねない`() {
        val view = makeView(
            listOf(Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "旧")))),
        )
        val adapter = view.internalMainListAdapter()
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        // 既存 c1 の内容も同時に変える。c1 へは内容通知が出るが、新規の c2 へは出ない。
        view.applyDiff(
            SettingsRootDiff.Full(
                SettingsRoot(
                    sections = listOf(
                        Section(
                            id = "s1",
                            cells = listOf(
                                LabelCell(id = "c1", title = "新"),
                                LabelCell(id = "c2", title = "B"),
                            ),
                        ),
                    ),
                ),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { adapter.itemCount == 2 && recorder.changed.isNotEmpty() }

        assertTrue("追加された Cell は挿入の構造通知で表示される", recorder.inserted.isNotEmpty())
        assertEquals(
            "内容通知は元から表示されていた行だけに発行される",
            listOf(0),
            recorder.changed.map { it.positionStart },
        )
        assertEquals("B", bindCellTitle(adapter, 1))
    }

    @Test
    fun `削除された Cell へは内容通知を発行しない`() {
        val view = makeView(
            listOf(
                Section(
                    id = "s1",
                    cells = listOf(
                        LabelCell(id = "c1", title = "旧"),
                        LabelCell(id = "c2", title = "B"),
                    ),
                ),
            ),
        )
        val adapter = view.internalMainListAdapter()
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        view.applyDiff(
            SettingsRootDiff.ReplaceSection(
                sectionId = "s1",
                newSection = Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "新"))),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { adapter.itemCount == 1 && recorder.changed.isNotEmpty() }

        assertTrue("削除は構造変更として反映される", recorder.removed.isNotEmpty())
        assertEquals(
            "残った行だけが内容通知の対象になる",
            listOf(0),
            recorder.changed.map { it.positionStart },
        )
    }

    @Test
    fun `hidden から表示へ復帰する Cell へは内容通知を重ねない`() {
        val view = makeView(
            listOf(
                Section(
                    id = "s1",
                    cells = listOf(
                        LabelCell(id = "c1", title = "旧"),
                        LabelCell(id = "c2", title = "B", isVisible = false),
                    ),
                ),
            ),
        )
        val adapter = view.internalMainListAdapter()
        assertEquals("hidden の Cell は表示されない", 1, adapter.itemCount)
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        view.applyDiff(
            SettingsRootDiff.Full(
                SettingsRoot(
                    sections = listOf(
                        Section(
                            id = "s1",
                            cells = listOf(
                                LabelCell(id = "c1", title = "新"),
                                LabelCell(id = "c2", title = "B"),
                            ),
                        ),
                    ),
                ),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { adapter.itemCount == 2 && recorder.changed.isNotEmpty() }

        assertTrue("復帰した Cell は挿入の構造通知で表示される", recorder.inserted.isNotEmpty())
        assertEquals(
            "hidden から復帰した行へは内容通知を重ねない",
            listOf(0),
            recorder.changed.map { it.positionStart },
        )
        assertEquals("B", bindCellTitle(adapter, 1))
    }

    @Test
    fun `内容が変わらない Cell へは内容通知を発行しない`() {
        val sections = listOf(
            Section(
                id = "s1",
                header = SectionAccessory.Text("一般"),
                cells = listOf(LabelCell(id = "c1", title = "A"), LabelCell(id = "c2", title = "B")),
            ),
        )
        val view = makeView(sections)
        val adapter = view.internalMainListAdapter()
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)
        val committedBefore = adapter.currentList

        // 同内容の root を再設定する。構造も内容も変化がないため通知は一切出ない。
        view.setRootDirect(SettingsRoot(sections = sections))
        // 差分計算のコミット完了（内部リスト参照の差し替え）まで待ってから通知の不在を確かめる。
        awaitDifferCommit({ committedSummary(adapter) }) { adapter.currentList !== committedBefore }

        assertEquals(emptyList<ChangeRecord>(), recorder.changed)
    }

    @Test
    fun `初回の root 反映では内容変更通知を発行しない`() {
        val view = KsSettingsView(ctx)
        val adapter = view.internalMainListAdapter()
        val recorder = NotificationRecorder()
        adapter.registerAdapterDataObserver(recorder)

        view.setRootDirect(
            SettingsRoot(
                sections = listOf(
                    Section(
                        id = "s1",
                        header = SectionAccessory.Text("一般"),
                        cells = listOf(LabelCell(id = "c1", title = "A")),
                    ),
                ),
            ),
        )
        awaitDifferCommit({ committedSummary(adapter) }) { adapter.itemCount == 2 }

        assertTrue("全行が挿入の構造通知で表示される", recorder.inserted.isNotEmpty())
        assertEquals(emptyList<ChangeRecord>(), recorder.changed)
    }

    // MARK: - 内容通知対象の算出

    @Test
    fun `contentChangedCellIds は旧新双方に存在し内容が変わった Cell id だけを返す`() {
        val oldList = KsSettingsView.flatten(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.Text("一般"),
                    cells = listOf(
                        LabelCell(id = "c1", title = "A"),
                        LabelCell(id = "c2", title = "B"),
                        LabelCell(id = "c4", title = "D"),
                    ),
                ),
            ),
        )
        val newList = KsSettingsView.flatten(
            listOf(
                Section(
                    id = "s1",
                    header = SectionAccessory.Text("一般"),
                    cells = listOf(
                        // c1: 内容が変わったので対象。c4: 内容が同じなので対象外。
                        LabelCell(id = "c1", title = "A'"),
                        LabelCell(id = "c4", title = "D"),
                        // c3: 新規なので対象外。c2: 削除されたので対象外。
                        LabelCell(id = "c3", title = "C"),
                    ),
                ),
            ),
        )
        assertEquals(listOf("c1"), KsSettingsView.contentChangedCellIds(oldList, newList))
    }

    @Test
    fun `contentChangedCellIds は旧リストが空なら空を返す`() {
        val newList = KsSettingsView.flatten(
            listOf(Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A")))),
        )
        assertEquals(emptyList<String>(), KsSettingsView.contentChangedCellIds(emptyList(), newList))
    }

    @Test
    fun `contentChangedCellIds は新リストに Cell が無ければ空を返す`() {
        val oldList = KsSettingsView.flatten(
            listOf(Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A")))),
        )
        val newList = KsSettingsView.flatten(
            listOf(Section(id = "s1", header = SectionAccessory.Text("一般"))),
        )
        assertEquals(emptyList<String>(), KsSettingsView.contentChangedCellIds(oldList, newList))
    }

    /** `onItemRangeChanged` の記録単位。position・件数・payload をまとめて突き合わせる。 */
    private data class ChangeRecord(val positionStart: Int, val itemCount: Int, val payload: Any?)

    /**
     * Adapter が発行した構造通知・変更通知を記録する Observer。
     *
     * payload なしの `notifyItemChanged(position)` も 3 引数版へ payload = null で届くため、
     * 記録された payload が非 null であることが「payload 付き通知」の証拠になる。
     */
    private class NotificationRecorder : RecyclerView.AdapterDataObserver() {
        val changed = mutableListOf<ChangeRecord>()
        val inserted = mutableListOf<Pair<Int, Int>>()
        val removed = mutableListOf<Pair<Int, Int>>()

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            changed += ChangeRecord(positionStart, itemCount, payload)
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            inserted += positionStart to itemCount
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            removed += positionStart to itemCount
        }
    }
}
