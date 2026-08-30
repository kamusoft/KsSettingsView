package jp.kamusoft.kssettingsview.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.DSLStyleModifiableCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Handle ベースの DSL 拡張（[SectionHandle] / [CellHandle] chain、`forEach`、`unaryPlus`）のテスト。
 *
 * 検証範囲:
 *   - [SectionHandle.sectionHeader] / [SectionHandle.sectionFooter] が Section H/F を上書きすること
 *   - [SectionHandle.sectionID] が明示 ID ヒントを反映すること
 *   - [CellHandle.cellHeight] / [CellHandle.font] が Cell style を反映すること
 *   - [CellHandle.cellID] が明示 ID ヒントを反映すること
 *   - [KsIdentifiable] 版 forEach（RootScope / SectionScope）の自動 ID 採番
 *   - [DSLSectionScope.unaryPlus] で Cell が DSL に流れること
 */
class DSLHandleTest {

    private fun evaluate(content: DSLSettingsRootScope.() -> Unit): List<Section> {
        val scope = DSLSettingsRootScope().apply(content)
        val tree = DSLRootTree(sectionNodes = scope.build())
        return tree.resolvedSections()
    }

    // MARK: - 25.4.1 SectionHandle.sectionFooter

    @Test
    fun `SectionHandle sectionFooter が Section footer を上書きする`() {
        val sections = evaluate {
            Section(header = "S1") {
                cell(HandleTestCell(title = "X"))
            }.sectionFooter("動的に追加した footer")
        }
        assertEquals(1, sections.size)
        val footer = sections[0].footer
        assertTrue(footer is SectionAccessory.Text)
        assertEquals("動的に追加した footer", (footer as SectionAccessory.Text).value)
    }

    // MARK: - 25.4.2 SectionHandle.sectionHeader

    @Test
    fun `SectionHandle sectionHeader が Section header を上書きする`() {
        val sections = evaluate {
            // header 引数なしで Section を作成し、Handle 経由で文字列ヘッダを後付けする
            Section { cell(HandleTestCell(title = "X")) }
                .sectionHeader("後付け Header")
        }
        assertEquals(1, sections.size)
        val header = sections[0].header
        assertTrue(header is SectionAccessory.Text)
        assertEquals("後付け Header", (header as SectionAccessory.Text).value)
    }

    // MARK: - 25.4.3 SectionHandle.sectionID

    @Test
    fun `SectionHandle sectionID で Explicit hint が反映され ID が安定する`() {
        // 1 回目: Section A のみ
        val first = evaluate {
            Section { cell(HandleTestCell(title = "X")) }
                .sectionID("explicit-section-a")
        }
        // 2 回目: Section B（先頭挿入） + Section A
        val second = evaluate {
            Section { cell(HandleTestCell(title = "Y")) }
                .sectionID("explicit-section-b")
            Section { cell(HandleTestCell(title = "X")) }
                .sectionID("explicit-section-a")
        }
        assertEquals(1, first.size)
        assertEquals(2, second.size)
        assertEquals(
            "明示 sectionID は位置移動を跨いで安定すること",
            first[0].id,
            second[1].id,
        )
    }

    // MARK: - 25.4.4 CellHandle.cellHeight / font

    @Test
    fun `CellHandle cellHeight が style に反映される`() {
        val sections = evaluate {
            Section(header = "S1") {
                cell(HandleTestCell(title = "X")).cellHeight(120.dp)
            }
        }
        val cell = sections[0].cells[0] as DSLStyleModifiableCell
        assertEquals(120.dp, cell.style.cellHeight)
    }

    @Test
    fun `CellHandle font が style に反映される`() {
        val font = TextStyle(fontSize = 18.sp)
        val sections = evaluate {
            Section(header = "S1") {
                cell(HandleTestCell(title = "X")).font(font)
            }
        }
        val cell = sections[0].cells[0] as DSLStyleModifiableCell
        assertEquals(font, cell.style.titleFont)
    }

    @Test
    fun `CellHandle titleColor が style に反映される`() {
        val color = Color(red = 0.5f, green = 0.5f, blue = 0.5f, alpha = 1.0f)
        val sections = evaluate {
            Section(header = "S1") {
                cell(HandleTestCell(title = "X")).titleColor(color)
            }
        }
        val cell = sections[0].cells[0] as DSLStyleModifiableCell
        assertEquals(color, cell.style.titleColor)
    }

    // MARK: - 25.4.5 CellHandle.cellID

    @Test
    fun `CellHandle cellID で Explicit hint が反映され ID が安定する`() {
        // 1 回目: Cell A のみ
        val first = evaluate {
            Section(header = "S1") {
                cell(HandleTestCell(title = "X")).cellID("explicit-cell-a")
            }
        }
        // 2 回目: Cell B（先頭挿入） + Cell A
        val second = evaluate {
            Section(header = "S1") {
                cell(HandleTestCell(title = "Y")).cellID("explicit-cell-b")
                cell(HandleTestCell(title = "X")).cellID("explicit-cell-a")
            }
        }
        assertEquals(
            "CellHandle.cellID 明示指定は位置移動を跨いで安定すること",
            first[0].cells[0].id,
            second[0].cells[1].id,
        )
    }

    // MARK: - 25.4.6 KsIdentifiable 版 forEach

    @Test
    fun `KsIdentifiable 版 forEach（SectionScope）の自動 ID 採番`() {
        data class Item(override val id: Int, val name: String) : KsIdentifiable
        val items = listOf(Item(1, "A"), Item(2, "B"))
        val sections = evaluate {
            Section(header = "S") {
                forEach(items) { item -> cell(HandleTestCell(title = item.name)) }
            }
        }
        assertEquals(2, sections[0].cells.size)
        // 2 回目評価しても ID が一致することを確認（順序入れ替え）
        val secondItems = listOf(Item(2, "B"), Item(1, "A"))
        val second = evaluate {
            Section(header = "S") {
                forEach(secondItems) { item -> cell(HandleTestCell(title = item.name)) }
            }
        }
        // item.id = 1 と item.id = 2 由来の Cell ID は順序入れ替え後も
        // それぞれ前回と同じ ID を保持する必要がある
        val idA1 = sections[0].cells[0].id
        val idA2 = second[0].cells[1].id // A は 2 回目では index=1
        assertEquals("KsIdentifiable.id 由来の Cell ID は順序入れ替えを跨いで安定すること", idA1, idA2)
    }

    @Test
    fun `KsIdentifiable 版 forEach（RootScope）の自動 ID 採番`() {
        data class SectionItem(override val id: String, val title: String) : KsIdentifiable
        val items = listOf(
            SectionItem("alpha", "Section Alpha"),
            SectionItem("beta", "Section Beta"),
        )
        val sections = evaluate {
            forEach(items) { item ->
                Section(header = item.title) {
                    cell(HandleTestCell(title = "X"))
                }
            }
        }
        assertEquals(2, sections.size)
        // 順序を入れ替えた 2 回目評価で Section ID が安定することを確認
        val swapped = listOf(items[1], items[0])
        val second = evaluate {
            forEach(swapped) { item ->
                Section(header = item.title) {
                    cell(HandleTestCell(title = "X"))
                }
            }
        }
        assertEquals(
            "RootScope KsIdentifiable forEach: alpha Section ID は順序入れ替えを跨いで安定",
            sections[0].id,
            second[1].id,
        )
    }

    // MARK: - 25.4.7 unaryPlus

    @Test
    fun `unaryPlus 演算子で Cell が DSL に流れる`() {
        val sections = evaluate {
            Section(header = "S") {
                +HandleTestCell(title = "Plus 演算子で流す")
            }
        }
        assertEquals(1, sections.size)
        assertEquals(1, sections[0].cells.size)
        val resolvedCell = sections[0].cells[0] as HandleTestCell
        assertEquals("Plus 演算子で流す", resolvedCell.title)
    }

    // MARK: - 25.4.8 デフォルト id 値による DSL rebind 検証

    @Test
    fun `デフォルト id 値の Cell でも DSL 経路で id が rebind される`() {
        // 同一 title・デフォルト id（毎回 UUID）の Cell を 2 回構築しても、
        // DSL 経路では Section 内位置 + Cell 型のフォールバック ID が採番されるため、
        // 2 回評価で Cell ID が一致する。
        val first = evaluate {
            Section(header = "S") {
                cell(HandleTestCell(title = "A"))
                cell(HandleTestCell(title = "B"))
            }
        }
        val second = evaluate {
            Section(header = "S") {
                cell(HandleTestCell(title = "A"))
                cell(HandleTestCell(title = "B"))
            }
        }
        assertEquals(
            "デフォルト id 値の Cell でも DSL Section 内位置ベースで ID が安定すること",
            first[0].cells.map { it.id },
            second[0].cells.map { it.id },
        )
        // ただし元の Cell インスタンスの id は毎回 UUID で異なる
        val rawA1 = HandleTestCell(title = "A").id
        val rawA2 = HandleTestCell(title = "A").id
        assertNotEquals(rawA1, rawA2)
    }

    // MARK: - 25.4.9 後方互換性

    @Test
    fun `既存 引数版 Section と cell ラップ形式が引き続き動作する`() {
        val sections = evaluate {
            Section(header = "S1", footer = "F1") {
                cell(HandleTestCell(title = "X").cellID("legacy-id"))
            }
        }
        assertEquals(1, sections.size)
        assertNotNull(sections[0].header)
        assertNotNull(sections[0].footer)
        assertEquals(1, sections[0].cells.size)
    }
}

/**
 * テスト用 Cell。`id` はデフォルト値で毎回 UUID 採番する。
 *
 * 25.3 で確認されたとおり、デフォルト id を持つ Cell でも DSL Node 経路で
 * 安定 ID に rebind されるため、本テストは「デフォルト id 規約」を直接検証する役割も担う。
 */
internal data class HandleTestCell(
    override val id: String = "handle-test-${java.util.UUID.randomUUID()}",
    override val style: CellStyle = CellStyle(),
    val title: String,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell {
    override fun withDSLId(newId: String): Cell = copy(id = newId)
    override fun withDSLStyle(newStyle: CellStyle): Cell = copy(style = newStyle)
}
