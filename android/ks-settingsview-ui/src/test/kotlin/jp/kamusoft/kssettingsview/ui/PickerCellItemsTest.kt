package jp.kamusoft.kssettingsview.ui

import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/** 射影対象の要素型（主表示と副表示の材料を別プロパティに持つ）。 */
private data class Plan(val name: String, val detail: String)

/**
 * [PickerCell] の候補モデル（[PickerItem]）と、任意の要素型を受けるジェネリック factory の検証。
 * 射影・副表示の正規化・元要素の受け取り・value 自動表示・公開シグネチャの呼び出し形を扱う。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PickerCellItemsTest {

    private val plans = listOf(
        Plan(name = "無料", detail = "広告あり"),
        Plan(name = "標準", detail = "広告なし"),
        Plan(name = "上位", detail = "全機能"),
    )

    private val ctx: android.content.Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            MaterialR.style.Theme_Material3_Light_NoActionBar,
        )

    // MARK: - 選択面を通した確定操作のヘルパ

    /** ViewHolder の行タップ経路で選択面を開く（Cell から選択面への配線ごと検証する）。 */
    private fun openSheet(cell: PickerCell): PickerSelectionSheet {
        val vh = PickerCellViewHolder.create(FrameLayout(ctx))
        vh.bind(cell, Theme())
        vh.views.root.performClick()
        return ShadowDialog.getLatestDialog() as PickerSelectionSheet
    }

    /** 単一選択の選択面で1行を確定する。 */
    private fun confirmSingle(cell: PickerCell, row: Int) {
        openSheet(cell).bindRow(row).root.performClick()
    }

    /** 複数選択の選択面で任意の行をトグルしてから「完了」で確定する。 */
    private fun confirmMultiple(cell: PickerCell, toggling: List<Int>) {
        val sheet = openSheet(cell)
        toggling.forEach { sheet.bindRow(it).root.performClick() }
        sheet.confirmView.performClick()
    }

    // MARK: - 候補モデル

    @Test
    fun `生の PickerItem 列がそのまま候補になる`() {
        val items = listOf(
            PickerItem(text = "無料", subText = "広告あり"),
            PickerItem(text = "標準"),
        )
        val cell = PickerCell(title = "プラン", items = items)

        assertEquals(items, cell.items)
        assertEquals("広告あり", cell.items[0].subText)
        assertNull(cell.items[1].subText)
    }

    @Test
    fun `ジェネリック縁の射影が各要素に適用される`() {
        val cell = PickerCell(
            title = "プラン",
            items = plans,
            displayText = { it.name },
            subText = { it.detail },
        )

        assertEquals(listOf("無料", "標準", "上位"), cell.items.map { it.text })
        assertEquals(listOf("広告あり", "広告なし", "全機能"), cell.items.map { it.subText })
    }

    @Test
    fun `String 特殊化は射影なしで主表示だけの候補になる`() {
        val cell = PickerCell(title = "テーマ", items = listOf("ライト", "ダーク"))

        assertEquals(listOf("ライト", "ダーク"), cell.items.map { it.text })
        assertTrue(cell.items.all { it.subText == null })
    }

    @Test
    fun `空文字列の subText は副表示なしへ正規化される`() {
        val cell = PickerCell(
            title = "プラン",
            items = plans,
            displayText = { it.name },
            subText = { if (it.name == "標準") "" else it.detail },
        )

        assertEquals("広告あり", cell.items[0].subText)
        assertNull(cell.items[1].subText)
        assertEquals("全機能", cell.items[2].subText)
    }

    @Test
    fun `構築後の元コレクション変更は object callback に現れない`() {
        val source = plans.toMutableList()
        var received: Plan? = null
        val cell = PickerCell(
            title = "プラン",
            items = source,
            displayText = { it.name },
            onItemSelected = { received = it },
        )
        source[1] = Plan(name = "差し替え", detail = "後から変更")

        confirmSingle(cell, row = 1)

        assertEquals(plans[1], received)
    }

    // MARK: - value 自動表示

    @Test
    fun `単一選択の自動表示は主表示のみを使う`() {
        val cell = PickerCell(
            title = "プラン",
            items = plans,
            displayText = { it.name },
            subText = { it.detail },
            selectedIndex = 1,
        )

        assertEquals("標準", cell.autoValueText())
    }

    @Test
    fun `複数選択の自動表示は主表示のみを index 昇順でカンマ連結する`() {
        val cell = PickerCell(
            title = "プラン",
            items = plans,
            displayText = { it.name },
            subText = { it.detail },
            selectedIndices = setOf(2, 0),
        )

        assertEquals("無料, 上位", cell.autoValueText())
    }

    @Test
    fun `範囲外 index は自動表示から除外される`() {
        val cell = PickerCell(
            title = "プラン",
            items = plans,
            displayText = { it.name },
            selectedIndices = setOf(0, 9),
        )

        assertEquals("無料", cell.autoValueText())
    }

    // MARK: - 単一選択の object 書き戻し

    @Test
    fun `確定で onItemSelected に元要素が1回届く`() {
        val received = mutableListOf<Plan>()
        val cell = PickerCell(
            title = "プラン",
            items = plans,
            displayText = { it.name },
            onItemSelected = { received.add(it) },
        )

        confirmSingle(cell, row = 2)

        assertEquals(listOf(plans[2]), received)
    }

    @Test
    fun `確定では index callback が先に元要素 callback が後に走る`() {
        val order = mutableListOf<String>()
        val cell = PickerCell(
            title = "プラン",
            items = plans,
            displayText = { it.name },
            onSelectionChanged = { order.add("index") },
            onItemSelected = { order.add("item") },
        )

        confirmSingle(cell, row = 0)

        assertEquals(listOf("index", "item"), order)
    }

    // MARK: - 複数選択の object 受け取り

    @Test
    fun `確定で onItemsSelected に index 昇順の元要素列が届く`() {
        val received = mutableListOf<List<Plan>>()
        val cell = PickerCell(
            title = "プラン",
            items = plans,
            displayText = { it.name },
            selectedIndices = setOf(2),
            onItemsSelected = { received.add(it) },
        )

        confirmMultiple(cell, toggling = listOf(0))

        assertEquals(listOf(listOf(plans[0], plans[2])), received)
    }

    @Test
    fun `範囲外 index は onItemsSelected の列から除外され index 集合には残る`() {
        var indices: Set<Int>? = null
        var items: List<Plan>? = null
        val cell = PickerCell(
            title = "プラン",
            items = plans,
            displayText = { it.name },
            selectedIndices = setOf(1, 9),
            onMultiSelectionChanged = { indices = it },
            onItemsSelected = { items = it },
        )

        confirmMultiple(cell, toggling = emptyList())

        assertEquals(setOf(1, 9), indices)
        assertEquals(listOf(plans[1]), items)
    }

    // MARK: - 公開シグネチャの呼び出し形

    @Test
    fun `公開シグネチャの全呼び出し形が意図した overload へ解決される`() {
        // 生の経路（data class constructor）
        val raw = PickerCell(title = "生", items = listOf(PickerItem(text = "A", subText = "a")))
        assertEquals("a", raw.items[0].subText)

        // String 特殊化（単一 / 複数）
        val stringSingle = PickerCell(title = "文字列単一", items = listOf("A", "B"), selectedIndex = 1)
        assertEquals(PickerSelectionMode.Single, stringSingle.selectionMode)
        assertEquals(1, stringSingle.selectedIndex)

        val stringMultiple = PickerCell(
            title = "文字列複数",
            items = listOf("A", "B"),
            subText = { "$it の説明" },
            selectedIndices = setOf(0),
            maxSelectedNumber = 2,
        )
        assertEquals(PickerSelectionMode.Multiple, stringMultiple.selectionMode)
        assertEquals("A の説明", stringMultiple.items[0].subText)

        // ジェネリック縁（単一 / 複数）
        val genericSingle = PickerCell(
            title = "汎用単一",
            items = plans,
            displayText = { it.name },
            subText = { it.detail },
            selectedIndex = 0,
            onSelectionChanged = {},
            onItemSelected = {},
        )
        assertEquals(PickerSelectionMode.Single, genericSingle.selectionMode)

        val genericMultiple = PickerCell(
            title = "汎用複数",
            items = plans,
            displayText = { it.name },
            selectedIndices = setOf(0, 1),
            onMultiSelectionChanged = {},
            onItemsSelected = {},
        )
        assertEquals(PickerSelectionMode.Multiple, genericMultiple.selectionMode)
    }
}
