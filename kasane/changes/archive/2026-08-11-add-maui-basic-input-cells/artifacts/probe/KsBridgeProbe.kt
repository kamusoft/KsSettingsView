package jp.kamusoft.kssettingsview.bridge

import android.graphics.drawable.Drawable

/**
 * probe 専用の最小例（add-maui-basic-input-cells / tasks 1.1〜1.5）。
 *
 * binding 生成の可否だけを確かめるための一時コードであり、Bridge の公開契約ではない。
 * 検証が済んだら削除する。
 */

/** probe 1.2: listener interface が C# の interface として binding されるかの検証。 */
interface KsBridgeProbeListener {

    /** 引数なし相当（cellId のみ）の通知。 */
    fun probeTapped(cellId: String)

    /** scalar 引数を伴う通知。 */
    fun probeSwitchChanged(cellId: String, isOn: Boolean)

    /** 配列引数を伴う通知（複数選択の wire 表現の検証）。 */
    fun probeIndicesChanged(cellId: String, indices: IntArray)

    /** 文字列引数を伴う通知（ISO 時刻の wire 表現の検証）。 */
    fun probeTimeChanged(cellId: String, time: String)
}

/** probe 1.4 / 1.3 / 1.5: 共通基底 DTO・platform 画像・nullable scalar の検証。 */
abstract class KsBridgeProbeCell {

    /** 基底が採番する Cell ID。 */
    val cellID: String = KsBridgeIdentifier.make()

    /** 共通フィールド。 */
    var title: String = ""

    /** probe 1.3: platform 画像（Drawable）を interop で受け取れるか。 */
    var icon: Drawable? = null

    /** probe 1.5: nullable scalar（Double?）の binding 表現。 */
    var iconSize: Double? = null

    /** probe 1.5: nullable scalar（Int? = enum の序数輸送）の binding 表現。 */
    var uiStyle: Int? = null
}

/** probe 1.4: 基底の派生 A。 */
class KsBridgeProbeLabelCell : KsBridgeProbeCell()

/** probe 1.4: 基底の派生 B（固有フィールドを持つ）。 */
class KsBridgeProbeSwitchCell : KsBridgeProbeCell() {

    /** 派生固有フィールド。 */
    var isOn: Boolean = false
}

/** probe の入口。listener 保持と異種 DTO 混載を C# から駆動する。 */
class KsBridgeProbe {

    /** probe 1.2: listener の保持と解除（null 設定）。 */
    var listener: KsBridgeProbeListener? = null

    private val mutableCells: MutableList<KsBridgeProbeCell> = mutableListOf()

    /** probe 1.4: 基底型のコレクションとして混載を公開する。 */
    val cells: List<KsBridgeProbeCell>
        get() = mutableCells.toList()

    /** probe 1.4: 基底型の引数で異種 DTO を受け取る。 */
    fun addCell(cell: KsBridgeProbeCell): String {
        mutableCells.add(cell)
        return cell.cellID
    }

    /** probe 1.4: 基底型のコレクション引数で異種 DTO をまとめて受け取る。 */
    fun setCells(cells: List<KsBridgeProbeCell>) {
        mutableCells.clear()
        mutableCells.addAll(cells)
    }

    /**
     * probe 1.3〜1.5: 受け取った DTO を Kotlin 側で判別・読み出しした結果を文字列で返す。
     *
     * C# から渡した派生型・Drawable・nullable scalar が Kotlin 側で正しく見えるかを、
     * この戻り値の目視で確認する。
     */
    fun describeCells(): String = mutableCells.joinToString(separator = " | ") { cell ->
        val kind = when (cell) {
            is KsBridgeProbeSwitchCell -> "Switch(on=${cell.isOn})"
            is KsBridgeProbeLabelCell -> "Label"
            else -> "Unknown"
        }
        val iconKind = cell.icon?.let { it::class.java.simpleName } ?: "null"
        "$kind title=${cell.title} icon=$iconKind iconSize=${cell.iconSize} uiStyle=${cell.uiStyle}"
    }

    /** probe 1.2: 保持中の listener へ 4 種の通知を発火する。 */
    fun fireAll() {
        val target = listener ?: return
        target.probeTapped("cell-1")
        target.probeSwitchChanged("cell-2", true)
        target.probeIndicesChanged("cell-3", intArrayOf(1, 2, 3))
        target.probeTimeChanged("cell-4", "09:30")
    }
}
