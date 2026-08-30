package jp.kamusoft.kssettingsview.bridge

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 全 12 操作を 1 つの表で駆動し、操作後の観察可能な結果（Host の実描画内容・Adapter 通知・
 * Store から Host への Diff 配信の生存）が対応する Store 操作の契約と一致することを検証する。
 *
 * 起点はいずれも標準構成（Section "S1" に Cell A / B、Section "S2" に Cell C）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsBridgeOperationContractTest {

    private var attachment: KsBridgeTestHost.Attachment? = null

    /**
     * Section 既定 margin を 0 に固定した Theme を返す。
     *
     * 本テストは全 adapter position が画面 1 枚分の高さに同時に実描画されている幾何を前提に
     * （[KsBridgeTestHost.renderedRows]）表示内容を観察する。ライブラリ既定の Section margin が
     * 変わるとその幾何が動いて観察できなくなるため、既定値から独立させて 0 に固定する。
     */
    private fun pinnedGeometryTheme(): KsBridgeTheme = KsBridgeTheme().apply {
        sectionMarginTop = 0.0
        sectionMarginLeading = 0.0
        sectionMarginBottom = 0.0
        sectionMarginTrailing = 0.0
    }

    @After
    fun tearDown() {
        attachment?.close()
        attachment = null
    }

    /** 1 操作分の検証仕様。 */
    private class OperationCase(
        /** 失敗時に操作を特定するためのラベル */
        val label: String,
        /** 検証対象の操作 */
        val act: (KsBridgeFixture.Built) -> Unit,
        /** 操作後に実描画される行テキスト（Section header と Cell title を並び順に並べたもの） */
        val rows: List<String>,
        /** 操作が構造変更として通知されるか */
        val structural: Boolean,
        /** 操作によって通知される内容更新の行数 */
        val contentChanges: Int = 0,
    )

    /**
     * 標準構成に対する全 12 操作の契約表。代表的な引数に加えて、未知 ID と index の
     * 丸め込みの境界も操作ごとに含める。
     */
    private fun cases(): List<OperationCase> = listOf(
        // 1. setRoot
        OperationCase(
            label = "setRoot: 別の root で全置換",
            act = { fixture ->
                val builder = KsBridgeRootBuilder()
                val section = builder.addSection(headerText = "N1", footerText = null)
                builder.addLabelCell(KsBridgeLabelCell(title = "X"), section.sectionID)
                fixture.bridge.setRoot(builder)
            },
            rows = listOf("N1", "X"),
            structural = true,
        ),

        // 2. insertSection
        OperationCase(
            label = "insertSection: 先頭へ挿入",
            act = { fixture ->
                val section = KsBridgeSection(headerText = "S0", footerText = null)
                section.addCell(KsBridgeLabelCell(title = "D"))
                fixture.bridge.insertSection(section, index = 0)
            },
            rows = listOf("S0", "D", "S1", "A", "B", "S2", "C"),
            structural = true,
        ),
        OperationCase(
            label = "insertSection: 範囲外 index は末尾へ丸められる",
            act = { fixture ->
                val section = KsBridgeSection(headerText = "S3", footerText = null)
                section.addCell(KsBridgeLabelCell(title = "D"))
                fixture.bridge.insertSection(section, index = 99)
            },
            rows = listOf("S1", "A", "B", "S2", "C", "S3", "D"),
            structural = true,
        ),

        // 3. removeSection
        OperationCase(
            label = "removeSection: 既知 ID",
            act = { fixture -> fixture.bridge.removeSection(fixture.section1.sectionID) },
            rows = listOf("S2", "C"),
            structural = true,
        ),
        OperationCase(
            label = "removeSection: 未知 ID は no-op",
            act = { fixture ->
                fixture.bridge.removeSection(KsBridgeFixture.unusedIdentifier())
                fixture.bridge.removeSection(KsBridgeFixture.UNKNOWN_IDENTIFIER)
            },
            rows = KsBridgeFixture.standardRows,
            structural = false,
        ),

        // 4. moveSection
        OperationCase(
            label = "moveSection: 順序入れ替え",
            act = { fixture -> fixture.bridge.moveSection(from = 0, to = 1) },
            rows = listOf("S2", "C", "S1", "A", "B"),
            structural = true,
        ),
        OperationCase(
            label = "moveSection: 範囲外の移動先は末尾へ丸められる",
            act = { fixture -> fixture.bridge.moveSection(from = 0, to = 99) },
            rows = listOf("S2", "C", "S1", "A", "B"),
            structural = true,
        ),
        OperationCase(
            label = "moveSection: 範囲外の移動元は no-op",
            act = { fixture -> fixture.bridge.moveSection(from = 99, to = 0) },
            rows = KsBridgeFixture.standardRows,
            structural = false,
        ),

        // 5. replaceSection
        // 置換後の header text は起点と同じ "S1" に固定する。header text を変える経路には
        // 未修正の再描画不具合があり、この表では Cell 側の置換だけを見る。
        OperationCase(
            label = "replaceSection: 既知 ID (header text は不変)",
            act = { fixture ->
                val replacement = KsBridgeSection(headerText = "S1", footerText = null)
                replacement.addCell(KsBridgeLabelCell(title = "Z"))
                fixture.bridge.replaceSection(fixture.section1.sectionID, replacement)
            },
            rows = listOf("S1", "Z", "S2", "C"),
            structural = true,
        ),
        OperationCase(
            label = "replaceSection: 未知 ID は no-op",
            act = { fixture ->
                fixture.bridge.replaceSection(
                    KsBridgeFixture.unusedIdentifier(),
                    KsBridgeSection(headerText = "X", footerText = null),
                )
                fixture.bridge.replaceSection(
                    KsBridgeFixture.UNKNOWN_IDENTIFIER,
                    KsBridgeSection(headerText = "X", footerText = null),
                )
            },
            rows = KsBridgeFixture.standardRows,
            structural = false,
        ),

        // 6. insertCell
        OperationCase(
            label = "insertCell: Section 先頭へ挿入",
            act = { fixture ->
                fixture.bridge.insertCell(KsBridgeLabelCell(title = "A0"), fixture.section1.sectionID, index = 0)
            },
            rows = listOf("S1", "A0", "A", "B", "S2", "C"),
            structural = true,
        ),
        OperationCase(
            label = "insertCell: 範囲外 index は末尾へ丸められる",
            act = { fixture ->
                fixture.bridge.insertCell(KsBridgeLabelCell(title = "A9"), fixture.section1.sectionID, index = 99)
            },
            rows = listOf("S1", "A", "B", "A9", "S2", "C"),
            structural = true,
        ),
        OperationCase(
            label = "insertCell: 未知 sectionID は no-op",
            act = { fixture ->
                fixture.bridge.insertCell(
                    KsBridgeLabelCell(title = "X"),
                    KsBridgeFixture.unusedIdentifier(),
                    index = 0,
                )
                fixture.bridge.insertCell(
                    KsBridgeLabelCell(title = "X"),
                    KsBridgeFixture.UNKNOWN_IDENTIFIER,
                    index = 0,
                )
            },
            rows = KsBridgeFixture.standardRows,
            structural = false,
        ),

        // 7. removeCell
        OperationCase(
            label = "removeCell: 既知 ID",
            act = { fixture -> fixture.bridge.removeCell(fixture.cellA.cellID) },
            rows = listOf("S1", "B", "S2", "C"),
            structural = true,
        ),
        OperationCase(
            label = "removeCell: 未知 ID は no-op",
            act = { fixture ->
                fixture.bridge.removeCell(KsBridgeFixture.unusedIdentifier())
                fixture.bridge.removeCell(KsBridgeFixture.UNKNOWN_IDENTIFIER)
            },
            rows = KsBridgeFixture.standardRows,
            structural = false,
        ),

        // 8. moveCell
        OperationCase(
            label = "moveCell: Section 内で移動",
            act = { fixture -> fixture.bridge.moveCell(fixture.cellA.cellID, index = 1) },
            rows = listOf("S1", "B", "A", "S2", "C"),
            structural = true,
        ),
        OperationCase(
            label = "moveCell: 範囲外 index は末尾へ丸められる",
            act = { fixture -> fixture.bridge.moveCell(fixture.cellA.cellID, index = 99) },
            rows = listOf("S1", "B", "A", "S2", "C"),
            structural = true,
        ),
        OperationCase(
            label = "moveCell: 未知 ID は no-op",
            act = { fixture ->
                fixture.bridge.moveCell(KsBridgeFixture.unusedIdentifier(), index = 0)
                fixture.bridge.moveCell(KsBridgeFixture.UNKNOWN_IDENTIFIER, index = 0)
            },
            rows = KsBridgeFixture.standardRows,
            structural = false,
        ),

        // 9. replaceCell
        OperationCase(
            label = "replaceCell: 既知 ID",
            act = { fixture ->
                fixture.bridge.replaceCell(fixture.cellB.cellID, KsBridgeLabelCell(title = "B2"))
            },
            rows = listOf("S1", "A", "B2", "S2", "C"),
            structural = false,
            contentChanges = 1,
        ),
        OperationCase(
            label = "replaceCell: 未知 ID は no-op",
            act = { fixture ->
                fixture.bridge.replaceCell(KsBridgeFixture.unusedIdentifier(), KsBridgeLabelCell(title = "X"))
                fixture.bridge.replaceCell(KsBridgeFixture.UNKNOWN_IDENTIFIER, KsBridgeLabelCell(title = "X"))
            },
            rows = KsBridgeFixture.standardRows,
            structural = false,
        ),

        // 10. updateAccessory
        OperationCase(
            label = "updateAccessory: Section footer の text 追加",
            act = { fixture ->
                fixture.bridge.updateAccessory(
                    target = KsBridgeAccessoryTarget.SectionFooter,
                    sectionID = fixture.section1.sectionID,
                    text = "F1",
                )
            },
            rows = listOf("S1", "A", "B", "F1", "S2", "C"),
            structural = true,
        ),
        OperationCase(
            label = "updateAccessory: canonical UUID でない sectionID は no-op",
            act = { fixture ->
                fixture.bridge.updateAccessory(
                    target = KsBridgeAccessoryTarget.SectionHeader,
                    sectionID = KsBridgeFixture.UNKNOWN_IDENTIFIER,
                    text = "X",
                )
            },
            rows = KsBridgeFixture.standardRows,
            structural = false,
        ),
        OperationCase(
            label = "updateAccessory: 未使用 sectionID の Section header は no-op",
            act = { fixture ->
                fixture.bridge.updateAccessory(
                    target = KsBridgeAccessoryTarget.SectionHeader,
                    sectionID = KsBridgeFixture.unusedIdentifier(),
                    text = "X",
                )
            },
            rows = KsBridgeFixture.standardRows,
            structural = false,
        ),
        OperationCase(
            label = "updateAccessory: 未使用 sectionID の Section footer は no-op",
            act = { fixture ->
                fixture.bridge.updateAccessory(
                    target = KsBridgeAccessoryTarget.SectionFooter,
                    sectionID = KsBridgeFixture.unusedIdentifier(),
                    text = "Y",
                )
            },
            rows = KsBridgeFixture.standardRows,
            structural = false,
        ),

        // 11. replaceCells
        OperationCase(
            label = "replaceCells: 複数 Cell を 1 バッチで更新",
            act = { fixture ->
                fixture.bridge.replaceCells(
                    listOf(
                        KsBridgeCellUpdate(fixture.cellA.cellID, KsBridgeLabelCell(title = "A2")),
                        KsBridgeCellUpdate(fixture.cellC.cellID, KsBridgeLabelCell(title = "C2")),
                    ),
                )
            },
            rows = listOf("S1", "A2", "B", "S2", "C2"),
            structural = false,
            contentChanges = 2,
        ),
        OperationCase(
            label = "replaceCells: 未知 ID を含んでも既知分だけが 1 バッチで適用される",
            act = { fixture ->
                fixture.bridge.replaceCells(
                    listOf(
                        KsBridgeCellUpdate(KsBridgeFixture.unusedIdentifier(), KsBridgeLabelCell(title = "X")),
                        KsBridgeCellUpdate(fixture.cellB.cellID, KsBridgeLabelCell(title = "B2")),
                        KsBridgeCellUpdate(KsBridgeFixture.UNKNOWN_IDENTIFIER, KsBridgeLabelCell(title = "Y")),
                    ),
                )
            },
            rows = listOf("S1", "A", "B2", "S2", "C"),
            structural = false,
            contentChanges = 1,
        ),
        OperationCase(
            label = "replaceCells: 未知 ID のみは適用 0 件で配信されない",
            act = { fixture ->
                fixture.bridge.replaceCells(
                    listOf(KsBridgeCellUpdate(KsBridgeFixture.unusedIdentifier(), KsBridgeLabelCell(title = "X"))),
                )
            },
            rows = KsBridgeFixture.standardRows,
            structural = false,
        ),
        OperationCase(
            label = "replaceCells: 空リストは no-op",
            act = { fixture -> fixture.bridge.replaceCells(emptyList()) },
            rows = KsBridgeFixture.standardRows,
            structural = false,
        ),

        // 12. setTheme
        OperationCase(
            label = "setTheme: 構造変更も内容更新も発行せず構造を変えない",
            act = { fixture ->
                // margin の固定は維持したまま色だけ差し替える（幾何を動かさないため）。
                fixture.bridge.setTheme(
                    pinnedGeometryTheme().apply { cellTitleColor = 0xFF00FF00.toInt() },
                )
            },
            rows = KsBridgeFixture.standardRows,
            structural = false,
        ),
    )

    /**
     * 全 12 操作が契約どおりに反映されることを、観察可能な結果（実描画内容・Adapter 通知・
     * Diff 配信の生存）で検証する。
     */
    @Test
    fun `全 12 操作が契約どおりに反映される`() {
        for (testCase in cases()) {
            val fixture = KsBridgeFixture.standard()
            fixture.bridge.setTheme(pinnedGeometryTheme())
            val host = KsBridgeTestHost.attach(fixture.bridge)
            attachment = host
            assertEquals(
                "起点の表示が標準構成である: ${testCase.label}",
                KsBridgeFixture.standardRows,
                KsBridgeTestHost.renderedRows(host),
            )

            val observer = KsBridgeAdapterRecorder.attach(host)
            testCase.act(fixture)
            KsBridgeTestHost.pump(host)

            assertEquals(
                "表示される行: ${testCase.label}",
                testCase.rows,
                KsBridgeTestHost.renderedRows(host),
            )
            assertEquals(
                "構造変更の有無: ${testCase.label}",
                testCase.structural,
                observer.structuralCount > 0,
            )
            assertEquals(
                "内容更新の行数: ${testCase.label}",
                testCase.contentChanges,
                observer.contentChangeCount,
            )

            observer.detach(host)
            assertDiffDeliveryAlive(fixture, host, testCase)
            host.close()
            attachment = null
        }
    }

    /**
     * 操作の後も Store の Diff が Host へ届き続けることを、後続操作の表示反映で確認する。
     *
     * Store が設定ツリーに存在しない ID の Diff を発行すると、Host の厳格検出
     * （`KsCellRegistry.strictMode`）が Diff 購読のコルーチンごと停止させる。停止しても直前の
     * 操作の表示と Adapter 通知は変化しないため、no-op を主張するケースは表示と通知だけでは
     * 空振りする。ここで後続操作の到達まで見ることで、Store が Diff を発行しなかったこと
     * （core/ADR-0020 の未知 ID no-op）を観察できる。
     *
     * 後続操作には、直前の操作でどの Section / Cell が残っていても成立する先頭への Section
     * 挿入を使う。
     */
    private fun assertDiffDeliveryAlive(
        fixture: KsBridgeFixture.Built,
        host: KsBridgeTestHost.Attachment,
        testCase: OperationCase,
    ) {
        val probe = KsBridgeSection(headerText = PROBE_HEADER_TEXT, footerText = null)
        probe.addCell(KsBridgeLabelCell(title = PROBE_CELL_TITLE))
        fixture.bridge.insertSection(probe, index = 0)
        KsBridgeTestHost.pump(host)

        assertEquals(
            "後続操作が表示へ届く（Host の Diff 購読が生きている）: ${testCase.label}",
            listOf(PROBE_HEADER_TEXT, PROBE_CELL_TITLE) + testCase.rows,
            KsBridgeTestHost.renderedRows(host),
        )
    }

    private companion object {
        /** Diff 配信の生存確認で挿入する Section の header テキスト。 */
        const val PROBE_HEADER_TEXT = "PROBE-S"

        /** Diff 配信の生存確認で挿入する Section が持つ Cell のタイトル。 */
        const val PROBE_CELL_TITLE = "PROBE-C"
    }
}
