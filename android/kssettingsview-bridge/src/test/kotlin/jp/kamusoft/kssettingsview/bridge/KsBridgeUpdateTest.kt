package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.ui.KsSettingsView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Store 公開操作と 1:1 対応する更新 API が表示へ反映されることを検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsBridgeUpdateTest {

    private var attachment: KsBridgeTestHost.Attachment? = null

    @After
    fun tearDown() {
        attachment?.close()
        attachment = null
    }

    /** Cell の挿入と削除が表示へ反映される。 */
    @Test
    fun `insertCell と removeCell が表示へ反映される`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        val inserted = KsBridgeLabelCell(title = "A2")
        val insertedID = fixture.bridge.insertCell(inserted, fixture.section1.sectionID, index = 1)
        fixture.bridge.removeCell(fixture.cellC.cellID)
        KsBridgeTestHost.pump(host)

        assertEquals(inserted.cellID, insertedID)
        assertEquals(listOf("S1", "A", "A2", "B", "S2"), KsBridgeTestHost.renderedRows(host))
    }

    /** Section の挿入・並べ替え・削除が表示へ反映される。 */
    @Test
    fun `insertSection と moveSection と removeSection が表示へ反映される`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        val newSection = KsBridgeSection(headerText = "S3", footerText = null)
        newSection.addCell(KsBridgeLabelCell(title = "D"))
        val insertedID = fixture.bridge.insertSection(newSection, index = 0)
        KsBridgeTestHost.pump(host)
        assertEquals(newSection.sectionID, insertedID)
        assertEquals(listOf("S3", "D", "S1", "A", "B", "S2", "C"), KsBridgeTestHost.renderedRows(host))

        fixture.bridge.moveSection(from = 0, to = 2)
        KsBridgeTestHost.pump(host)
        assertEquals(listOf("S1", "A", "B", "S2", "C", "S3", "D"), KsBridgeTestHost.renderedRows(host))

        fixture.bridge.removeSection(fixture.section2.sectionID)
        KsBridgeTestHost.pump(host)
        assertEquals(listOf("S1", "A", "B", "S3", "D"), KsBridgeTestHost.renderedRows(host))
    }

    /** Section の内容置換は sectionID の identity を保ったまま反映される。 */
    @Test
    fun `replaceSection は sectionID の identity を保つ`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        val replacement = KsBridgeSection(headerText = "S1", footerText = null)
        replacement.addCell(KsBridgeLabelCell(title = "Z"))
        fixture.bridge.replaceSection(fixture.section1.sectionID, replacement)
        KsBridgeTestHost.pump(host)

        assertEquals(listOf("S1", "Z", "S2", "C"), KsBridgeTestHost.renderedRows(host))
        assertEquals(
            "置換後も Section の identity は sectionID のまま保たれる",
            fixture.section1.sectionID,
            fixture.bridge.store.state.value.sections.first().id,
        )
        assertEquals(
            SectionAccessory.Text("S1"),
            fixture.bridge.store.state.value.sections.first().header,
        )
    }

    /** replace 系が返す ID で後続操作が通り、渡した DTO 自身の ID は破棄される。 */
    @Test
    fun `replace 系が返す ID で後続操作ができる`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        val replacement = KsBridgeSection(headerText = "S1", footerText = null)
        replacement.addCell(KsBridgeLabelCell(title = "Z"))
        val replacedSectionID = fixture.bridge.replaceSection(fixture.section1.sectionID, replacement)
        assertEquals("戻り値は対象の sectionID", fixture.section1.sectionID, replacedSectionID)
        assertNotEquals(
            "渡した DTO 自身の sectionID は破棄される",
            replacement.sectionID,
            replacedSectionID,
        )

        val appended = KsBridgeLabelCell(title = "Z2")
        val appendedID = fixture.bridge.insertCell(appended, requireNotNull(replacedSectionID), index = 99)
        KsBridgeTestHost.pump(host)
        assertEquals(
            "replaceSection の戻り値 ID を挿入先に指定できる",
            listOf("S1", "Z", "Z2", "S2", "C"),
            KsBridgeTestHost.renderedRows(host),
        )

        val replacedCellID = fixture.bridge.replaceCell(
            requireNotNull(appendedID),
            KsBridgeLabelCell(title = "Z3"),
        )
        assertEquals("戻り値は対象の cellID", appendedID, replacedCellID)
        KsBridgeTestHost.pump(host)
        assertEquals(
            "replaceCell の戻り値 ID でさらに置換できる",
            listOf("S1", "Z", "Z3", "S2", "C"),
            KsBridgeTestHost.renderedRows(host),
        )

        assertEquals(
            "戻り値の ID は何度でも使える",
            replacedCellID,
            fixture.bridge.replaceCell(requireNotNull(replacedCellID), KsBridgeLabelCell(title = "Z4")),
        )
    }

    /** 対象が存在しない replace 系は `null` を返し、状態も表示も変えない。 */
    @Test
    fun `replace 系は対象が存在しないとき null を返す`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        assertNull(
            fixture.bridge.replaceSection(
                KsBridgeFixture.unusedIdentifier(),
                KsBridgeSection(headerText = "X", footerText = null),
            ),
        )
        assertNull(
            fixture.bridge.replaceSection(
                KsBridgeFixture.UNKNOWN_IDENTIFIER,
                KsBridgeSection(headerText = "X", footerText = null),
            ),
        )
        assertNull(
            fixture.bridge.replaceCell(
                KsBridgeFixture.unusedIdentifier(),
                KsBridgeLabelCell(title = "X"),
            ),
        )
        assertNull(
            fixture.bridge.replaceCell(
                KsBridgeFixture.UNKNOWN_IDENTIFIER,
                KsBridgeLabelCell(title = "X"),
            ),
        )
        KsBridgeTestHost.pump(host)

        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(host))
    }

    /** 同じ cellID への内容置換は行の identity を維持し、削除+挿入として扱われない。 */
    @Test
    fun `replaceCell は行の identity を維持する`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        val rowBefore = host.recyclerView.findViewHolderForAdapterPosition(1)?.itemView
        val observer = KsBridgeAdapterRecorder.attach(host)

        fixture.bridge.replaceCell(fixture.cellA.cellID, KsBridgeLabelCell(title = "A2"))
        KsBridgeTestHost.pump(host)

        assertEquals(listOf("S1", "A2", "B", "S2", "C"), KsBridgeTestHost.renderedRows(host))
        assertEquals("構造変更は発生しない", 0, observer.structuralCount)
        assertEquals("内容更新として 1 行だけ通知される", 1, observer.contentChangeCount)
        assertEquals(
            "同一の行が再構成される",
            rowBefore,
            host.recyclerView.findViewHolderForAdapterPosition(1)?.itemView,
        )
        observer.detach(host)
    }

    /** 複数 Cell の内容更新が 1 回のバッチ内容更新として反映される。 */
    @Test
    fun `replaceCells が 1 バッチで反映される`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val observer = KsBridgeAdapterRecorder.attach(host)

        fixture.bridge.replaceCells(
            listOf(
                KsBridgeCellUpdate(fixture.cellA.cellID, KsBridgeLabelCell(title = "A2")),
                KsBridgeCellUpdate(fixture.cellC.cellID, KsBridgeLabelCell(title = "C2")),
            ),
        )
        KsBridgeTestHost.pump(host)

        assertEquals(listOf("S1", "A2", "B", "S2", "C2"), KsBridgeTestHost.renderedRows(host))
        assertEquals("構造変更は発生しない", 0, observer.structuralCount)
        assertEquals("対象 2 行がまとめて内容更新される", 2, observer.contentChangeCount)
        observer.detach(host)
    }

    /** 未知 ID だけの `replaceCells` は適用 0 件となり表示も通知も変化しない。 */
    @Test
    fun `replaceCells は未知 ID のみでは配信されず表示も変わらない`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }
        val observer = KsBridgeAdapterRecorder.attach(host)

        fixture.bridge.replaceCells(
            listOf(
                KsBridgeCellUpdate(KsBridgeFixture.unusedIdentifier(), KsBridgeLabelCell(title = "X")),
                KsBridgeCellUpdate(KsBridgeFixture.UNKNOWN_IDENTIFIER, KsBridgeLabelCell(title = "Y")),
            ),
        )
        KsBridgeTestHost.pump(host)

        assertEquals(0, observer.totalCount)
        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(host))
        observer.detach(host)
    }

    /** Cell の移動が表示へ反映される。 */
    @Test
    fun `moveCell が表示へ反映される`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        fixture.bridge.moveCell(fixture.cellA.cellID, index = 1)
        KsBridgeTestHost.pump(host)

        assertEquals(listOf("S1", "B", "A", "S2", "C"), KsBridgeTestHost.renderedRows(host))
    }

    /** Section header / footer の text 追加と解除が表示へ反映される。 */
    @Test
    fun `updateAccessory の text 追加と解除が表示へ反映される`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        fixture.bridge.updateAccessory(
            target = KsBridgeAccessoryTarget.SectionFooter,
            sectionID = fixture.section1.sectionID,
            text = "F1",
        )
        KsBridgeTestHost.pump(host)
        assertEquals(listOf("S1", "A", "B", "F1", "S2", "C"), KsBridgeTestHost.renderedRows(host))

        fixture.bridge.updateAccessory(
            target = KsBridgeAccessoryTarget.SectionFooter,
            sectionID = fixture.section1.sectionID,
            text = null,
        )
        KsBridgeTestHost.pump(host)
        assertEquals(
            "clear 後は accessory が指定されていない場合と同じ表示になる",
            KsBridgeFixture.standardRows,
            KsBridgeTestHost.renderedRows(host),
        )
    }

    /**
     * canonical UUID として解釈できない sectionID の `updateAccessory` は Store へ渡らない。
     *
     * Store の `updateAccessory` は対象 Section が不在でも Diff を発行するため、Bridge が検証を
     * 省くと Host が未知 ID の Diff を受け取り、厳格検出（`KsCellRegistry.strictMode`）で例外を
     * 投げて Diff 購読ごと停止する。表示が変化しないことに加えて、後続操作が表示へ届くことまで
     * 確認することで「Store へ渡っていない」ことを観察できる。
     */
    @Test
    fun `updateAccessory の非 canonical sectionID は Store へ渡らない`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        fixture.bridge.updateAccessory(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = KsBridgeFixture.UNKNOWN_IDENTIFIER,
            text = "X",
        )
        KsBridgeTestHost.pump(host)
        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(host))

        fixture.bridge.replaceCell(fixture.cellA.cellID, KsBridgeLabelCell(title = "A2"))
        KsBridgeTestHost.pump(host)

        assertEquals(
            "後続操作が表示へ届く（Host の Diff 購読が生きている）",
            listOf("S1", "A2", "B", "S2", "C"),
            KsBridgeTestHost.renderedRows(host),
        )
    }

    /**
     * Bridge が採番していない canonical UUID の `updateAccessory` は、header / footer とも
     * 状態も表示も変えない。
     *
     * Bridge は `updateAccessory` を Store へ素通しするため、この no-op は Store 側の契約
     * （core/ADR-0020）がそのまま interop 表面に現れたものである。表示が変化しないことに加えて
     * 後続操作が表示へ届くことまで確認し、Host の Diff 購読が生きていることも観察する。
     */
    @Test
    fun `updateAccessory の未使用 sectionID は no-op になる`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        val unusedID = KsBridgeFixture.unusedIdentifier()
        fixture.bridge.updateAccessory(
            target = KsBridgeAccessoryTarget.SectionHeader,
            sectionID = unusedID,
            text = "X",
        )
        fixture.bridge.updateAccessory(
            target = KsBridgeAccessoryTarget.SectionFooter,
            sectionID = unusedID,
            text = "Y",
        )
        KsBridgeTestHost.pump(host)

        assertEquals(KsBridgeFixture.standardRows, KsBridgeTestHost.renderedRows(host))
        assertEquals(
            "Store の現在状態も変化しない",
            listOf(SectionAccessory.Text("S1"), SectionAccessory.Text("S2")),
            fixture.bridge.store.state.value.sections.map { it.header },
        )

        fixture.bridge.replaceCell(fixture.cellA.cellID, KsBridgeLabelCell(title = "A2"))
        KsBridgeTestHost.pump(host)

        assertEquals(
            "後続操作が表示へ届く（Host の Diff 購読が生きている）",
            listOf("S1", "A2", "B", "S2", "C"),
            KsBridgeTestHost.renderedRows(host),
        )
    }

    /** Root header / footer の text 更新が表示へ反映される。 */
    @Test
    fun `updateAccessory の root 対象が表示へ反映される`() {
        val fixture = KsBridgeFixture.standard()
        val host = KsBridgeTestHost.attach(fixture.bridge).also { attachment = it }

        fixture.bridge.updateAccessory(KsBridgeAccessoryTarget.RootHeader, sectionID = null, text = "ROOT-H")
        fixture.bridge.updateAccessory(KsBridgeAccessoryTarget.RootFooter, sectionID = null, text = "ROOT-F")
        KsBridgeTestHost.pump(host)

        val view = host.hostView as KsSettingsView
        assertEquals(RootAccessory.Text("ROOT-H"), view.rootHeader)
        assertEquals(RootAccessory.Text("ROOT-F"), view.rootFooter)
        assertEquals(
            listOf("ROOT-H") + KsBridgeFixture.standardRows + listOf("ROOT-F"),
            KsBridgeTestHost.renderedRows(host),
        )
    }
}
