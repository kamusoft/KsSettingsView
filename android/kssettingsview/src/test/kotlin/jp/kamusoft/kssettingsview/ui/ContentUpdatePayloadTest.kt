package jp.kamusoft.kssettingsview.ui

import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Cell の内容更新が **同一 ViewHolder への再 bind** として届くことを検証する。
 *
 * payload なしの `notifyItemChanged` では `SimpleItemAnimator.canReuseUpdatedViewHolder` が
 * false を返し、RecyclerView が更新行の ViewHolder を新規生成して旧行とクロスフェードする。
 * EntryCell では EditText ごと差し替わって IME 接続が切れるため、
 * 「payload が付いていること」と「change アニメーションが無効であること」の双方が必要になる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ContentUpdatePayloadTest {

    private val ctx: android.content.Context
        get() = ApplicationProvider.getApplicationContext()

    private fun rowOf(text: String) = CellListItem.CellRow(
        sectionId = "s1",
        cell = EntryCell(id = "c1", title = "名前", text = text),
    )

    /** コミット済みの平坦リストを、失敗メッセージ用に行種別と内容へ要約する。 */
    private fun committedSummary(adapter: KsSettingsListAdapter): List<String> =
        adapter.currentList.map { item ->
            when (item) {
                is CellListItem.SectionHeader -> "header"
                is CellListItem.SectionFooter -> "footer"
                is CellListItem.CellRow -> "cell(text=${(item.cell as? EntryCell)?.text})"
            }
        }

    @Test
    fun `submitContentUpdate は payload 付きで notifyItemChanged を発行する`() {
        val adapter = KsSettingsListAdapter()
        adapter.submitList(listOf(rowOf("")))
        idle()

        val observer = ChangeRecordingObserver()
        adapter.registerAdapterDataObserver(observer)

        adapter.submitContentUpdate(listOf(rowOf("あ")), "c1")
        awaitDifferCommit({ committedSummary(adapter) }) { observer.changedPositions.isNotEmpty() }

        assertEquals(listOf(0), observer.changedPositions)
        // payload が非空であることが canReuseUpdatedViewHolder = true の条件。
        assertEquals(listOf<Any?>(KsSettingsListAdapter.PAYLOAD_CONTENT), observer.payloads)
    }

    @Test
    fun `複数 Cell の同時内容更新でも全てに payload が付く`() {
        val adapter = KsSettingsListAdapter()
        val initial = listOf(
            CellListItem.CellRow(sectionId = "s1", cell = EntryCell(id = "c1", title = "姓", text = "")),
            CellListItem.CellRow(sectionId = "s1", cell = EntryCell(id = "c2", title = "名", text = "")),
        )
        adapter.submitList(initial)
        idle()

        val observer = ChangeRecordingObserver()
        adapter.registerAdapterDataObserver(observer)

        val updated = listOf(
            CellListItem.CellRow(sectionId = "s1", cell = EntryCell(id = "c1", title = "姓", text = "山")),
            CellListItem.CellRow(sectionId = "s1", cell = EntryCell(id = "c2", title = "名", text = "太")),
        )
        adapter.submitContentUpdate(updated, listOf("c1", "c2"))
        awaitDifferCommit({ committedSummary(adapter) }) { observer.changedPositions.size >= 2 }

        assertEquals(listOf(0, 1), observer.changedPositions)
        assertEquals(
            listOf<Any?>(KsSettingsListAdapter.PAYLOAD_CONTENT, KsSettingsListAdapter.PAYLOAD_CONTENT),
            observer.payloads,
        )
    }

    @Test
    fun `payload 付き再 bind でも Cell の内容が反映される`() {
        val adapter = KsSettingsListAdapter()
        adapter.submitList(listOf(rowOf("")))
        idle()

        val holder = EntryCellViewHolder.create(FrameLayout(ctx))
        adapter.onBindViewHolder(holder, 0)
        assertEquals("", holder.editText.text?.toString())

        adapter.submitContentUpdate(listOf(rowOf("あ")), "c1")
        awaitDifferCommit({ committedSummary(adapter) }) {
            val row = adapter.currentList.firstOrNull() as? CellListItem.CellRow
            (row?.cell as? EntryCell)?.text == "あ"
        }

        // 内容 payload は 3 引数版 onBindViewHolder から 2 引数版へ委譲され、
        // フル bind となる（内容は完全に反映される）。
        adapter.onBindViewHolder(holder, 0, listOf(KsSettingsListAdapter.PAYLOAD_CONTENT))
        assertEquals("あ", holder.editText.text?.toString())
    }

    @Test
    fun `KsSettingsView の RecyclerView は change アニメーションを無効化している`() {
        val view = KsSettingsView(ctx)
        val animator = view.internalRecyclerView().itemAnimator
        assertNotNull(animator)
        assertFalse((animator as SimpleItemAnimator).supportsChangeAnimations)
    }

    /**
     * `onItemRangeChanged` の position と payload を記録する Observer。
     *
     * payload なしの `notifyItemChanged(position)` も 3 引数版へ payload = null で届くため、
     * 記録された payload が非 null であることが「payload 付き通知」の証拠になる。
     */
    private class ChangeRecordingObserver : RecyclerView.AdapterDataObserver() {
        val changedPositions = mutableListOf<Int>()
        val payloads = mutableListOf<Any?>()

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            changedPositions += positionStart
            payloads += payload
        }
    }
}
