package jp.kamusoft.kssettingsview.bridge

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import jp.kamusoft.kssettingsview.core.Cell
import java.util.UUID

/**
 * Bridge テストで共有する標準的な設定ツリーの組み立て。
 *
 * 構成は Section 2 個で、1 つ目（header "S1"）に Cell A / B、2 つ目（header "S2"）に Cell C。
 */
internal object KsBridgeFixture {

    /** 組み立て済みの Bridge と、後続操作で使う DTO 群。 */
    internal class Built(
        val bridge: KsSettingsBridge,
        val section1: KsBridgeSection,
        val section2: KsBridgeSection,
        val cellA: KsBridgeLabelCell,
        val cellB: KsBridgeLabelCell,
        val cellC: KsBridgeLabelCell,
    )

    /** 標準構成の Bridge を `setRoot` 済みの状態で返す。 */
    fun standard(): Built {
        val bridge = KsSettingsBridge()
        val builder = KsBridgeRootBuilder()

        val section1 = builder.addSection(headerText = "S1", footerText = null)
        val section2 = builder.addSection(headerText = "S2", footerText = null)

        val cellA = KsBridgeLabelCell(title = "A")
        val cellB = KsBridgeLabelCell(title = "B")
        val cellC = KsBridgeLabelCell(title = "C")
        builder.addLabelCell(cellA, section1.sectionID)
        builder.addLabelCell(cellB, section1.sectionID)
        builder.addLabelCell(cellC, section2.sectionID)

        bridge.setRoot(builder)
        return Built(
            bridge = bridge,
            section1 = section1,
            section2 = section2,
            cellA = cellA,
            cellB = cellB,
            cellC = cellC,
        )
    }

    /** 標準構成が実描画されたときの行テキスト（Section header と Cell title を並び順に並べたもの）。 */
    val standardRows: List<String> = listOf("S1", "A", "B", "S2", "C")

    /**
     * 指定した Cell 群を 1 つの Section に載せた Bridge を `setRoot` 済みで返す。
     *
     * @param cells Section へ載せる Cell DTO 群（Cell 種を問わない）
     */
    fun withCells(cells: List<KsBridgeCell>): KsSettingsBridge {
        val bridge = KsSettingsBridge()
        val builder = KsBridgeRootBuilder()
        val section = builder.addSection(headerText = "S", footerText = null)
        cells.forEach { section.addCell(it) }
        bridge.setRoot(builder)
        return bridge
    }

    /**
     * Store の先頭 Section に載っている Cell を指定型として取り出す。
     *
     * @param bridge 対象 Bridge
     * @param index 先頭 Section 内の位置
     */
    inline fun <reified T> storedCell(bridge: KsSettingsBridge, index: Int = 0): T? =
        bridge.store.state.value.sections.firstOrNull()?.cells?.getOrNull(index) as? T

    /** 先頭 Section に載っている Cell 群。 */
    fun storedCells(bridge: KsSettingsBridge): List<Cell> =
        bridge.store.state.value.sections.firstOrNull()?.cells ?: emptyList()

    /** 単色の `Drawable` を生成する。interop で受け渡す platform 画像として使う。 */
    fun drawable(): Drawable = ColorDrawable(android.graphics.Color.RED)

    /** Bridge が採番しない、canonical UUID として解釈できない ID。 */
    const val UNKNOWN_IDENTIFIER: String = "not-a-canonical-uuid"

    /** canonical UUID ではあるが Bridge が設定ツリーへ載せていない ID。 */
    fun unusedIdentifier(): String = UUID.randomUUID().toString()
}
