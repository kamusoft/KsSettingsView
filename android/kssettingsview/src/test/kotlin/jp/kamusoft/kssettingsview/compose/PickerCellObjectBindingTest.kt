package jp.kamusoft.kssettingsview.compose

import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.ui.PickerCell
import jp.kamusoft.kssettingsview.ui.PickerCellViewHolder
import jp.kamusoft.kssettingsview.ui.PickerSelectionMode
import jp.kamusoft.kssettingsview.ui.PickerSelectionSheet
import jp.kamusoft.kssettingsview.ui.Theme
import jp.kamusoft.kssettingsview.ui.awaitMainLooperCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/** 射影対象の要素型（主表示と副表示の材料を別プロパティに持つ）。 */
private data class Plan(val name: String, val detail: String)

/**
 * PickerCell の DSL 拡張関数のうち、任意の要素型を受けるジェネリック overload と
 * 元要素の TwoWay overload（`selectedItem`）の逆引き・書き戻しを検証する。
 *
 * 閉じ切り通知だけは選択面の dismiss を経て届くため、DSL で組み立てた Cell を行タップ経路で
 * 開いて検証する。そのため Robolectric 上で実行する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PickerCellObjectBindingTest {

    private val plans = listOf(
        Plan(name = "無料", detail = "広告あり"),
        Plan(name = "標準", detail = "広告なし"),
        Plan(name = "上位", detail = "全機能"),
    )

    /** DSL で 1 Cell だけ組み立てて取り出す。 */
    private fun buildCell(block: DSLSectionScope.() -> Unit): PickerCell {
        val scope = DSLSettingsRootScope()
        scope.Section(block = block)
        return scope.build()[0].cellNodes[0].cell as PickerCell
    }

    /** 同梱テーマ解決の起点になる Context。 */
    private val ctx: android.content.Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            MaterialR.style.Theme_Material3_Light_NoActionBar,
        )

    /** Cell の行タップで選択面を開き、表示された選択面を返す。 */
    private fun openSheet(cell: PickerCell): PickerSelectionSheet {
        val vh = PickerCellViewHolder.create(FrameLayout(ctx))
        vh.bind(cell, Theme())
        vh.views.root.performClick()
        return ShadowDialog.getLatestDialog() as PickerSelectionSheet
    }

    // MARK: - 閉じ切り通知

    @Test
    fun `DSL の単一 overload で指定した閉じ切り callback が状態更新の後に届く`() {
        val sel = mutableStateOf<Int?>(null)
        val order = mutableListOf<String>()
        val cell = buildCell {
            PickerCell(
                title = "プラン",
                items = plans,
                displayText = { it.name },
                selectedIndex = sel,
                onItemSelected = { order.add("item:${it.name}") },
                onSelectionCompleted = { order.add("completed:$it (state=${sel.value})") },
            )
        }

        val sheet = openSheet(cell)
        sheet.bindRow(2).root.performClick()

        assertEquals(listOf("item:上位"), order)
        assertEquals(2, sel.value)

        awaitMainLooperCondition(diagnostics = { "受け取った通知: $order" }) { order.size == 2 }
        assertEquals(listOf("item:上位", "completed:2 (state=2)"), order)
    }

    @Test
    fun `DSL の複数 overload で指定した閉じ切り callback が状態更新の後に届く`() {
        val sel = mutableStateOf(emptySet<Int>())
        val order = mutableListOf<String>()
        val cell = buildCell {
            PickerCell(
                title = "プラン",
                items = plans,
                displayText = { it.name },
                selectedIndices = sel,
                onMultiSelectionCompleted = { order.add("completed:${it.sorted()} (state=${sel.value.sorted()})") },
            )
        }

        val sheet = openSheet(cell)
        sheet.bindRow(0).root.performClick()
        sheet.bindRow(2).root.performClick()
        sheet.confirmView.performClick()

        assertEquals(setOf(0, 2), sel.value)
        awaitMainLooperCondition(diagnostics = { "受け取った通知: $order" }) { order.size == 1 }
        assertEquals(listOf("completed:[0, 2] (state=[0, 2])"), order)
    }

    @Test
    fun `DSL の selectedItem overload で指定した閉じ切り callback が書き戻しの後に届く`() {
        val selected = mutableStateOf<Plan?>(plans[0])
        val order = mutableListOf<String>()
        val cell = buildCell {
            PickerCell(
                title = "プラン",
                items = plans,
                displayText = { it.name },
                selectedItem = selected,
                onSelectionCompleted = { order.add("completed:$it (state=${selected.value?.name})") },
            )
        }

        val sheet = openSheet(cell)
        sheet.bindRow(1).root.performClick()

        assertEquals(plans[1], selected.value)
        awaitMainLooperCondition(diagnostics = { "受け取った通知: $order" }) { order.size == 1 }
        assertEquals(listOf("completed:1 (state=標準)"), order)
    }

    // MARK: - ジェネリック overload（index 経路）

    @Test
    fun `ジェネリック単一 overload は射影を適用し元要素を通知する`() {
        val sel = mutableStateOf<Int?>(null)
        var received: Plan? = null
        val cell = buildCell {
            PickerCell(
                title = "プラン",
                items = plans,
                displayText = { it.name },
                selectedIndex = sel,
                subText = { it.detail },
                onItemSelected = { received = it },
            )
        }

        assertEquals(PickerSelectionMode.Single, cell.selectionMode)
        assertEquals(listOf("無料", "標準", "上位"), cell.items.map { it.text })
        assertEquals("広告なし", cell.items[1].subText)

        cell.onSelectionChanged?.invoke(2)
        assertEquals(2, sel.value)
        assertEquals(plans[2], received)
    }

    @Test
    fun `ジェネリック複数 overload は index 昇順の元要素列を通知する`() {
        val sel = mutableStateOf(setOf(1))
        var received: List<Plan>? = null
        val cell = buildCell {
            PickerCell(
                title = "プラン",
                items = plans,
                displayText = { it.name },
                selectedIndices = sel,
                onItemsSelected = { received = it },
            )
        }

        assertEquals(PickerSelectionMode.Multiple, cell.selectionMode)

        cell.onMultiSelectionChanged?.invoke(setOf(2, 0))
        assertEquals(setOf(2, 0), sel.value)
        assertEquals(listOf(plans[0], plans[2]), received)
    }

    @Test
    fun `構築後の元コレクション変更は元要素の通知に現れない`() {
        val source = plans.toMutableList()
        val sel = mutableStateOf<Int?>(null)
        var received: Plan? = null
        val cell = buildCell {
            PickerCell(
                title = "プラン",
                items = source,
                displayText = { it.name },
                selectedIndex = sel,
                onItemSelected = { received = it },
            )
        }
        source[1] = Plan(name = "差し替え", detail = "後から変更")

        cell.onSelectionChanged?.invoke(1)

        assertEquals(plans[1], received)
    }

    // MARK: - selectedItem TwoWay overload

    @Test
    fun `selectedItem は構築時に候補列から逆引きされる`() {
        val selected = mutableStateOf<Plan?>(plans[2])
        val cell = buildCell {
            PickerCell(
                title = "プラン",
                items = plans,
                displayText = { it.name },
                selectedItem = selected,
            )
        }

        assertEquals(2, cell.selectedIndex)
    }

    @Test
    fun `同値の重複候補は最初の index へ解決される`() {
        val duplicated = listOf(plans[0], plans[1], plans[0])
        val selected = mutableStateOf<Plan?>(plans[0])
        val cell = buildCell {
            PickerCell(
                title = "プラン",
                items = duplicated,
                displayText = { it.name },
                selectedItem = selected,
            )
        }

        assertEquals(0, cell.selectedIndex)
    }

    @Test
    fun `候補列に無い selectedItem は未選択になる`() {
        val selected = mutableStateOf<Plan?>(Plan(name = "特別", detail = "候補外"))
        val cell = buildCell {
            PickerCell(
                title = "プラン",
                items = plans,
                displayText = { it.name },
                selectedItem = selected,
            )
        }

        assertNull(cell.selectedIndex)
    }

    @Test
    fun `確定で selectedItem が対応する元要素へ書き戻される`() {
        val selected = mutableStateOf<Plan?>(plans[0])
        val cell = buildCell {
            PickerCell(
                title = "プラン",
                items = plans,
                displayText = { it.name },
                selectedItem = selected,
            )
        }

        cell.onSelectionChanged?.invoke(1)

        assertEquals(plans[1], selected.value)
    }

    @Test
    fun `候補が無い index の確定では selectedItem が未選択へ戻る`() {
        val selected = mutableStateOf<Plan?>(plans[0])
        val cell = buildCell {
            PickerCell(
                title = "プラン",
                items = plans,
                displayText = { it.name },
                selectedItem = selected,
            )
        }

        cell.onSelectionChanged?.invoke(9)

        assertNull(selected.value)
    }

    // MARK: - String 特殊化の呼び出し形

    @Test
    fun `文字列の全 overload が意図した経路へ解決される`() {
        val index = mutableStateOf<Int?>(0)
        val item = mutableStateOf<String?>("B")
        val indices = mutableStateOf(setOf(1))

        val byIndex = buildCell {
            PickerCell(
                title = "index 経路",
                items = listOf("A", "B"),
                selectedIndex = index,
                subText = { "$it の説明" },
                onItemSelected = {},
            )
        }
        assertEquals(PickerSelectionMode.Single, byIndex.selectionMode)
        assertEquals("A の説明", byIndex.items[0].subText)

        val byItem = buildCell {
            PickerCell(
                title = "item 経路",
                items = listOf("A", "B"),
                selectedItem = item,
            )
        }
        assertEquals(1, byItem.selectedIndex)

        val byIndices = buildCell {
            PickerCell(
                title = "複数経路",
                items = listOf("A", "B"),
                selectedIndices = indices,
                onItemsSelected = {},
            )
        }
        assertEquals(PickerSelectionMode.Multiple, byIndices.selectionMode)
    }
}
