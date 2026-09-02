package jp.kamusoft.kssettingsview.ui

import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.SectionAccessory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `CellListItemDiffCallback` / `KsSettingsListAdapter.getItemId` の挙動テスト。
 *
 * 「表示状態同期の三層分離」: Cell の構造同期は id 同一性のみで行い、`CellRow` の
 * `areContentsTheSame` は同一 id なら常に true を返す。Section H/F だけは専用の部分更新経路を
 * 持たないため accessory の内容を比較し、内容差では payload 付き変更通知に落とす（android/ADR-0012）。
 * `getItemId` は内容非依存の id ベース安定 ID。
 */
class ListAdapterDiffTest {

    /**
     * Cell 同士: id が同じなら areItemsTheSame は true、areContentsTheSame も true（内容比較しない）。
     */
    @Test
    fun `同一 id 同一内容の Cell は areItems も areContents も等価`() {
        val a = CellListItem.CellRow(
            sectionId = "s1",
            cell = LabelCell(id = "c1", title = "Hello"),
        )
        val b = CellListItem.CellRow(
            sectionId = "s1",
            cell = LabelCell(id = "c1", title = "Hello"),
        )
        assertTrue(CellListItemDiffCallback.areItemsTheSame(a, b))
        assertTrue(CellListItemDiffCallback.areContentsTheSame(a, b))
    }

    /**
     * Cell の内容（title）のみ変えた場合でも、areItemsTheSame は true（id 同一）かつ
     * areContentsTheSame も true（構造同期は内容を見ない）。これにより内容変化が行のフル
     * リバインド（ちらつき）を起こさない。内容更新は notifyItemChanged による部分更新で反映される。
     */
    @Test
    fun `同一 id で内容が異なる Cell は areItems も areContents も等価`() {
        val a = CellListItem.CellRow(
            sectionId = "s1",
            cell = LabelCell(id = "c1", title = "Hello"),
        )
        val b = CellListItem.CellRow(
            sectionId = "s1",
            cell = LabelCell(id = "c1", title = "World"),
        )
        assertTrue(CellListItemDiffCallback.areItemsTheSame(a, b))
        assertTrue(CellListItemDiffCallback.areContentsTheSame(a, b))
        assertNull(
            "CellRow は areContents が常に true のため payload を必要としない",
            CellListItemDiffCallback.getChangePayload(a, b),
        )
    }

    /**
     * 異なる sealed subtype は areItemsTheSame で false。
     */
    @Test
    fun `異なる sealed subtype は areItems で不等価`() {
        val a = CellListItem.CellRow(
            sectionId = "s1",
            cell = LabelCell(id = "x", title = "A"),
        )
        val b = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.Text("Header"),
        )
        assertFalse(CellListItemDiffCallback.areItemsTheSame(a, b))
    }

    /**
     * SectionHeader 同士で sectionId が同じなら areItemsTheSame は true。
     * 内容（文字列）が異なれば areContentsTheSame は false になり、payload 付きの変更通知に落ちる。
     */
    @Test
    fun `SectionHeader Text の内容差は areContents で不等価になり payload が付く`() {
        val a = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.Text("一般"),
        )
        val b = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.Text("詳細"),
        )
        assertTrue(CellListItemDiffCallback.areItemsTheSame(a, b))
        assertFalse(CellListItemDiffCallback.areContentsTheSame(a, b))
        assertEquals(
            KsSettingsListAdapter.PAYLOAD_CONTENT,
            CellListItemDiffCallback.getChangePayload(a, b),
        )
    }

    /**
     * SectionHeader Text の内容が同一なら areContentsTheSame は true（変更通知を発行しない）。
     */
    @Test
    fun `SectionHeader Text の内容同一は areContents で等価`() {
        val a = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.Text("一般"),
        )
        val b = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.Text("一般"),
        )
        assertTrue(CellListItemDiffCallback.areItemsTheSame(a, b))
        assertTrue(CellListItemDiffCallback.areContentsTheSame(a, b))
    }

    /**
     * Text accessory の Header は固定高さも表示に効くため、accessory が同一でも
     * headerHeight の差は areContentsTheSame へ反映する。
     */
    @Test
    fun `SectionHeader Text は headerHeight の差で areContents 不等価になり payload が付く`() {
        val a = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.Text("一般"),
        )
        val b = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.Text("一般"),
            headerHeight = 48.0,
        )
        assertTrue(CellListItemDiffCallback.areItemsTheSame(a, b))
        assertFalse(CellListItemDiffCallback.areContentsTheSame(a, b))
        assertEquals(
            KsSettingsListAdapter.PAYLOAD_CONTENT,
            CellListItemDiffCallback.getChangePayload(a, b),
        )
    }

    /**
     * View accessory の Header にも固定高さは効くため、headerHeight の差は内容差として扱う。
     * 中身が同一のまま高さだけが変わった場合は高さ専用の payload になり、中身を作り直さずに
     * 高さだけが更新される。
     */
    @Test
    fun `SectionHeader View は headerHeight の差で areContents 不等価になり高さ payload が付く`() {
        val anyView = KsAnyView.AndroidView { ctx -> android.widget.TextView(ctx) }
        val a = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.View(anyView),
        )
        val b = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.View(anyView),
            headerHeight = 48.0,
        )
        assertTrue(CellListItemDiffCallback.areItemsTheSame(a, b))
        assertFalse(CellListItemDiffCallback.areContentsTheSame(a, b))
        assertEquals(
            KsSettingsListAdapter.PAYLOAD_HEADER_HEIGHT,
            CellListItemDiffCallback.getChangePayload(a, b),
        )
    }

    /**
     * View accessory の中身と高さが同時に変わった場合は、中身の再構築が要るため内容 payload になる。
     */
    @Test
    fun `SectionHeader View は中身と headerHeight が同時に変わると内容 payload が付く`() {
        val a = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.View(KsAnyView.AndroidView { ctx -> android.widget.TextView(ctx) }),
        )
        val b = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.View(KsAnyView.AndroidView { ctx -> android.widget.TextView(ctx) }),
            headerHeight = 48.0,
        )
        assertFalse(CellListItemDiffCallback.areContentsTheSame(a, b))
        assertEquals(
            KsSettingsListAdapter.PAYLOAD_CONTENT,
            CellListItemDiffCallback.getChangePayload(a, b),
        )
    }

    /**
     * SectionFooter も Header と同じく accessory の内容差を areContentsTheSame へ反映する。
     */
    @Test
    fun `SectionFooter Text の内容差は areContents で不等価になり payload が付く`() {
        val a = CellListItem.SectionFooter(
            sectionId = "s1",
            accessory = SectionAccessory.Text("説明"),
        )
        val b = CellListItem.SectionFooter(
            sectionId = "s1",
            accessory = SectionAccessory.Text("別の説明"),
        )
        assertTrue(CellListItemDiffCallback.areItemsTheSame(a, b))
        assertFalse(CellListItemDiffCallback.areContentsTheSame(a, b))
        assertEquals(
            KsSettingsListAdapter.PAYLOAD_CONTENT,
            CellListItemDiffCallback.getChangePayload(a, b),
        )
    }

    /**
     * View accessory 同士は保持する `KsAnyView` の参照同一性で判定する。
     * 同一インスタンスを持ち回る限り内容変化なしと見なす。
     */
    @Test
    fun `SectionHeader View は同一 KsAnyView 参照なら areContents で等価`() {
        val shared = KsAnyView.AndroidView { ctx -> android.widget.TextView(ctx) }
        val a = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.View(shared),
        )
        val b = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.View(shared),
        )
        assertTrue(CellListItemDiffCallback.areItemsTheSame(a, b))
        assertTrue(CellListItemDiffCallback.areContentsTheSame(a, b))
    }

    /**
     * `SectionAccessory.View.equals` はクラス一致のみで等価とするため、equals では View の差し替えを
     * 検出できない。参照比較なら別インスタンスへの差し替えを内容変更として検出できる。
     */
    @Test
    fun `SectionHeader View は別の KsAnyView 参照なら areContents で不等価になり payload が付く`() {
        val a = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.View(KsAnyView.AndroidView { ctx -> android.widget.TextView(ctx) }),
        )
        val b = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.View(KsAnyView.AndroidView { ctx -> android.widget.TextView(ctx) }),
        )
        assertTrue(CellListItemDiffCallback.areItemsTheSame(a, b))
        assertFalse(CellListItemDiffCallback.areContentsTheSame(a, b))
        assertEquals(
            KsSettingsListAdapter.PAYLOAD_CONTENT,
            CellListItemDiffCallback.getChangePayload(a, b),
        )
    }

    /**
     * accessory の型が Text から View へ切り替わる場合も、行の identity は維持したまま内容変更として
     * 扱う（表示は新しい accessory の内容になる）。
     */
    @Test
    fun `SectionHeader の Text と View の切替は areItems 等価 areContents 不等価`() {
        val a = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.Text("一般"),
        )
        val b = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.View(KsAnyView.AndroidView { ctx -> android.widget.TextView(ctx) }),
        )
        assertTrue(CellListItemDiffCallback.areItemsTheSame(a, b))
        assertFalse(CellListItemDiffCallback.areContentsTheSame(a, b))
        assertEquals(
            KsSettingsListAdapter.PAYLOAD_CONTENT,
            CellListItemDiffCallback.getChangePayload(a, b),
        )
    }

    /**
     * `getItemId` の安定 ID 算出: 同一 id の Cell は内容（title）が変わっても同一の itemId を返す。
     * これにより内容変化が「別アイテムの差し替え」と誤認されない。
     */
    @Test
    fun `stableIdOf は内容に依存せず同一 id で安定`() {
        val a = CellListItem.CellRow(
            sectionId = "s1",
            cell = LabelCell(id = "c1", title = "Hello"),
        )
        val b = CellListItem.CellRow(
            sectionId = "s1",
            cell = LabelCell(id = "c1", title = "World"),
        )
        assertTrue(
            KsSettingsListAdapter.stableIdOf(a) == KsSettingsListAdapter.stableIdOf(b),
        )
    }

    /**
     * `getItemId` の安定 ID は、異なる id の Cell では異なる値を返す。
     */
    @Test
    fun `stableIdOf は異なる id で異なる値を返す`() {
        val a = CellListItem.CellRow(
            sectionId = "s1",
            cell = LabelCell(id = "c1", title = "Hello"),
        )
        val b = CellListItem.CellRow(
            sectionId = "s1",
            cell = LabelCell(id = "c2", title = "Hello"),
        )
        assertFalse(
            KsSettingsListAdapter.stableIdOf(a) == KsSettingsListAdapter.stableIdOf(b),
        )
    }

    /**
     * 同一 sectionId の Header / Footer は別アイテムとして区別される（安定 ID が異なる）。
     */
    @Test
    fun `stableIdOf は同一 sectionId の Header と Footer を区別する`() {
        val header = CellListItem.SectionHeader(
            sectionId = "s1",
            accessory = SectionAccessory.Text("H"),
        )
        val footer = CellListItem.SectionFooter(
            sectionId = "s1",
            accessory = SectionAccessory.Text("F"),
        )
        assertFalse(
            KsSettingsListAdapter.stableIdOf(header) == KsSettingsListAdapter.stableIdOf(footer),
        )
    }
}
