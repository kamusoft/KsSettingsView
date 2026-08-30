package jp.kamusoft.kssettingsview.compose

import androidx.compose.ui.unit.dp
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.DSLStyleModifiableCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compose DSL Integration テスト。
 *
 * `KsSettingsView { ... }` DSL の receiver scope を **2 回評価** したときに、
 * Section / Cell の安定 ID が Recomposition をまたいで一致することを検証する。
 *
 * 安定 ID 経路を構造的に検証できる粒度を保ち、iOS 側 `KsSettingsViewDSLIntegrationTests.swift`
 * と同等の観点を Compose 側でも揃える。
 *
 * 検証範囲:
 *   - 静的 DSL の 2 回評価で空 Diff
 *   - Cell 内容変更で対象 Cell の ReplaceCell のみ発行
 *   - `forEach(items, key=...)` で items に append → 既存 Cell ID 不変・新規のみ InsertCell
 *   - `Section` の sectionID（HeaderText 安定化）
 *   - Root H/F 変更 → UpdateAccessory
 *
 * アプローチ:
 *   `DSLSettingsRootScope` の DSL を直接評価して `DSLRootTree.resolvedSections()` で
 *   安定 ID 解決済みの `List<Section>` を取得し、`DSLDiffCalculator.compute(from, to)` で
 *   Diff が期待通りであることを確認する。Compose Composition / Robolectric は経由せず、
 *   純粋関数経路のみで検証する（より厳密かつ高速）。
 */
class DSLIntegrationTest {

    /** DSL ビルダー（receiver lambda）を評価し、resolved な `List<Section>` を返すヘルパ。 */
    private fun evaluate(content: DSLSettingsRootScope.() -> Unit): List<Section> {
        val scope = DSLSettingsRootScope().apply(content)
        val tree = DSLRootTree(sectionNodes = scope.build())
        return tree.resolvedSections()
    }

    private fun makeResolvedTree(
        sections: List<Section>,
        rootHeader: RootAccessory? = null,
    ): DSLDiffCalculator.ResolvedTree {
        return DSLDiffCalculator.ResolvedTree(
            sections = sections,
            rootHeader = rootHeader,
            rootFooter = null,
        )
    }

    // MARK: - 静的構造の Recomposition 耐性

    @Test
    fun `静的 DSL を 2 回評価しても Section ID と Cell ID が一致する`() {
        val first = evaluate {
            Section(header = "一般") {
                cell(TestCell(id = "noop1", title = "A"))
                cell(TestCell(id = "noop2", title = "B"))
            }
            Section(header = "高度") {
                cell(TestCell(id = "noop3", title = "C"))
            }
        }
        val second = evaluate {
            Section(header = "一般") {
                cell(TestCell(id = "noop1", title = "A"))
                cell(TestCell(id = "noop2", title = "B"))
            }
            Section(header = "高度") {
                cell(TestCell(id = "noop3", title = "C"))
            }
        }

        assertEquals(2, first.size)
        assertEquals(2, second.size)
        assertEquals(
            "Section ID はヘッダ文字列ベースで安定すること",
            first.map { it.id },
            second.map { it.id },
        )
        for ((s1, s2) in first.zip(second)) {
            assertEquals(
                "Cell ID は (SectionID, 位置, 型) ベースで安定すること",
                s1.cells.map { it.id },
                s2.cells.map { it.id },
            )
        }
    }

    @Test
    fun `静的 DSL を 2 回評価すると Diff が空になる`() {
        val first = evaluate {
            Section(header = "一般") {
                cell(TestCell(id = "noop", title = "A"))
            }
        }
        val second = evaluate {
            Section(header = "一般") {
                cell(TestCell(id = "noop", title = "A"))
            }
        }
        val diffs = DSLDiffCalculator.compute(
            from = makeResolvedTree(first),
            to = makeResolvedTree(second),
        )
        assertEquals(emptyList<SettingsRootDiff>(), diffs)
    }

    // MARK: - Cell 内容変更は構造 Diff を発行せず contentUpdates で検出

    @Test
    fun `Cell 内容変更で構造 Diff を発行せず contentUpdates で該当 Cell が検出される`() {
        val first = evaluate {
            Section(header = "一般") {
                cell(TestCell(id = "noop", title = "旧"))
                cell(TestCell(id = "noop", title = "B"))
            }
        }
        val second = evaluate {
            Section(header = "一般") {
                cell(TestCell(id = "noop", title = "新"))
                cell(TestCell(id = "noop", title = "B"))
            }
        }
        // Cell ID は位置 + 型ベースで一致する
        assertEquals(first[0].cells[0].id, second[0].cells[0].id)
        assertEquals(first[0].cells[1].id, second[0].cells[1].id)

        // 「表示状態同期の三層分離」: 内容変化では構造 Diff（ReplaceCell 含む）を発行しない。
        val diffs = DSLDiffCalculator.compute(
            from = makeResolvedTree(first),
            to = makeResolvedTree(second),
        )
        assertEquals(emptyList<SettingsRootDiff>(), diffs)

        // 内容更新は contentUpdates が該当 Cell（title が変わった先頭 Cell）のみ列挙する。
        val updates = DSLDiffCalculator.contentUpdates(
            from = makeResolvedTree(first),
            to = makeResolvedTree(second),
        )
        assertEquals(1, updates.size)
        assertEquals(first[0].cells[0].id, updates[0].id)
        assertEquals("新", (updates[0] as TestCell).title)
    }

    // MARK: - forEach 配下の動的追加

    @Test
    fun `forEach items 追加で既存 Cell ID 不変 新規のみ InsertCell が発行される`() {
        val items1 = listOf(1, 2)
        val items2 = listOf(1, 2, 3)

        val first = evaluate {
            Section(header = "Items") {
                forEach(items1, key = { it }) { item ->
                    cell(TestCell(id = "noop-$item", title = "Item $item"))
                }
            }
        }
        val second = evaluate {
            Section(header = "Items") {
                forEach(items2, key = { it }) { item ->
                    cell(TestCell(id = "noop-$item", title = "Item $item"))
                }
            }
        }

        assertEquals(2, first[0].cells.size)
        assertEquals(3, second[0].cells.size)
        assertEquals(
            "forEach key=1 の Cell ID が再評価をまたいで一致すること",
            first[0].cells[0].id,
            second[0].cells[0].id,
        )
        assertEquals(
            "forEach key=2 の Cell ID が再評価をまたいで一致すること",
            first[0].cells[1].id,
            second[0].cells[1].id,
        )

        val diffs = DSLDiffCalculator.compute(
            from = makeResolvedTree(first),
            to = makeResolvedTree(second),
        )
        assertEquals(1, diffs.size)
        val diff = diffs[0]
        assertTrue("Expected InsertCell, got $diff", diff is SettingsRootDiff.InsertCell)
        val insert = diff as SettingsRootDiff.InsertCell
        assertEquals(2, insert.index)
        assertEquals(first[0].id, insert.sectionId)
    }

    // MARK: - .sectionID(_:) で動的追加の Section ID 安定化（中置記法）

    @Test
    fun `sectionID 明示指定で動的追加時に Section ID が安定する`() {
        // 1 回目：Section A のみ
        val first = evaluate {
            Section { cell(TestCell(id = "noop", title = "X")) }
            // 直前の Section に明示 ID を付与
            overrideLastSectionId(DSLIdentityHint.Explicit("section-a"))
        }
        // 2 回目：Section A + Section B（先頭に新規追加）
        val second = evaluate {
            Section { cell(TestCell(id = "noop", title = "Y")) }
            overrideLastSectionId(DSLIdentityHint.Explicit("section-b"))
            Section { cell(TestCell(id = "noop", title = "X")) }
            overrideLastSectionId(DSLIdentityHint.Explicit("section-a"))
        }

        assertEquals(1, first.size)
        assertEquals(2, second.size)
        assertEquals(
            "明示 .sectionID(\"section-a\") は ForEach なしでも安定 ID を提供する",
            first[0].id,
            second[1].id,
        )

        val diffs = DSLDiffCalculator.compute(
            from = makeResolvedTree(first),
            to = makeResolvedTree(second),
        )
        // section-b の InsertSection が発行されるべき
        assertTrue(
            "section-b の InsertSection が発行されるべき",
            diffs.any { it is SettingsRootDiff.InsertSection && it.section.id == second[0].id },
        )
    }

    // MARK: - Root Header 変更で UpdateAccessory

    @Test
    fun `rootHeader 変更で UpdateAccessory が発行される`() {
        val first = evaluate {
            Section { cell(TestCell(id = "noop", title = "X")) }
        }
        val second = evaluate {
            Section { cell(TestCell(id = "noop", title = "X")) }
        }
        val diffs = DSLDiffCalculator.compute(
            from = makeResolvedTree(first, rootHeader = RootAccessory.Text("旧")),
            to = makeResolvedTree(second, rootHeader = RootAccessory.Text("新")),
        )
        assertEquals(1, diffs.size)
        val diff = diffs[0]
        assertTrue("Expected UpdateAccessory, got $diff", diff is SettingsRootDiff.UpdateAccessory)
        val update = diff as SettingsRootDiff.UpdateAccessory
        val rootAcc = update.accessory as SettingsAccessory.Root
        assertEquals(RootAccessory.Text("新"), rootAcc.accessory)
    }

    // MARK: - .cellID(_:) 明示指定で位置移動を跨ぐ Cell ID 安定性
    //
    // `Cell.cellID(id)` 拡張関数で明示指定された Cell ID が、Section 内位置の入れ替え
    // （別 Cell の先頭追加など）を跨いでも安定であることを検証する。
    // `Cell.cellID()` は Cell.id を直接書き換えず `DSLExplicitIdCell` でラップし、
    // `DSLSectionScope.cell()` が unwrap して `DSLIdentityHint.Explicit` に転写する。
    // この経路により明示 ID が Positional フォールバックより優先され、位置が変わっても
    // Cell ID が再上書きされない。

    @Test
    fun `cellID 明示指定で位置移動を跨いでも Cell ID が安定する`() {
        val first = evaluate {
            Section(header = "S") {
                cell(TestCell(id = "noop", title = "X").cellID("cell-x"))
            }
        }
        val second = evaluate {
            Section(header = "S") {
                cell(TestCell(id = "noop", title = "Y").cellID("cell-y"))
                cell(TestCell(id = "noop", title = "X").cellID("cell-x"))
            }
        }
        // cell-x は 1 回目で index=0、2 回目で index=1 と位置が変わるが、明示 ID が
        // 採用されるため Cell ID は一致しなければならない。
        assertEquals(
            "明示 cellID(\"cell-x\") は位置移動を跨いで安定すること",
            first[0].cells[0].id,
            second[0].cells[1].id,
        )

        val diffs = DSLDiffCalculator.compute(
            from = makeResolvedTree(first),
            to = makeResolvedTree(second),
        )
        // cell-y が先頭に挿入される InsertCell が発行され、
        // cell-x については移動のみで内容変化はないため Move のみ（または何も発行されない）。
        assertTrue(
            "cell-y の InsertCell が発行されるべき",
            diffs.any { it is SettingsRootDiff.InsertCell },
        )
    }

    // MARK: - Cell modifier 適用でも Cell ID が維持される

    @Test
    fun `Cell modifier 適用でも Cell ID が維持される`() {
        // 1 回目：modifier なし
        val first = evaluate {
            Section(header = "S") {
                cell(TestCell(id = "noop", title = "A"))
            }
        }
        // 2 回目：cellHeight を追加適用
        val second = evaluate {
            Section(header = "S") {
                cell(TestCell(id = "noop", title = "A").cellHeight(80.dp))
            }
        }
        assertEquals(
            "Cell modifier 適用後も Cell ID は維持される",
            first[0].cells[0].id,
            second[0].cells[0].id,
        )

        // style 変更は内容変化として扱う。「表示状態同期の三層分離」により構造 Diff（ReplaceCell）は
        // 発行されず、contentUpdates が該当 Cell を列挙する（ViewHolder の部分更新で反映）。
        val diffs = DSLDiffCalculator.compute(
            from = makeResolvedTree(first),
            to = makeResolvedTree(second),
        )
        assertTrue(
            "style 変更で ReplaceCell（構造 Diff）は発行されないこと",
            diffs.none { it is SettingsRootDiff.ReplaceCell },
        )
        val updates = DSLDiffCalculator.contentUpdates(
            from = makeResolvedTree(first),
            to = makeResolvedTree(second),
        )
        assertTrue(
            "style 変更は contentUpdates で検出されること",
            updates.any { it.id == second[0].cells[0].id },
        )
    }
}

/**
 * 統合テスト用の最小 Cell。`DSLReidentifiableCell` / `DSLStyleModifiableCell` 両方を実装する。
 */
internal data class TestCell(
    override val id: String,
    override val style: CellStyle = CellStyle(),
    val title: String,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell {
    override fun withDSLId(newId: String): Cell = copy(id = newId)
    override fun withDSLStyle(newStyle: CellStyle): Cell = copy(style = newStyle)
}
