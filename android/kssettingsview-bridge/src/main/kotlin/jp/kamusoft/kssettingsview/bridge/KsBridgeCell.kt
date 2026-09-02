package jp.kamusoft.kssettingsview.bridge

import android.graphics.drawable.Drawable
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.KsImage

/**
 * interop 境界で Cell を輸送する DTO の共通基底。
 *
 * Cell 種ごとに派生 DTO を持ち（maui/ADR-0011）、[KsBridgeSection.cells] や
 * [KsBridgeCellUpdate] はこの基底型で異種 Cell を混載する。ID 採番と全 Cell 共通の
 * 行レイアウトフィールド（title / descriptionText / valueText / hintText / icon）・スタイル上書き・
 * 有効性・可視性はこの基底が持つ。
 *
 * インスタンス生成時に Bridge が canonical UUID 文字列の [cellID] を採番する。呼び出し側は
 * この [cellID]（Builder / insert 系 API の戻り値と同一）を更新 API へ渡す（maui/ADR-0005）。
 *
 * DTO は 1 インスタンスが 1 つの Cell identity を表す。同じインスタンスを複数箇所へ追加すると
 * 同じ [cellID] の Cell が重複するため、Cell ごとに新しいインスタンスを生成する。
 *
 * DTO の内容は Bridge の API を呼んだ時点で Store へ写し取られる。呼び出し後に DTO のプロパティを
 * 書き換えても表示は変化しない。
 *
 * @property title タイトル（必須）
 * @property descriptionText 説明文（未指定は `null`）
 * @property valueText 右側に表示する値文字列（未指定は `null`）
 * @property hintText ヒントテキスト（未指定は `null`）
 * @property isEnabled 有効／無効フラグ
 * @property isVisible 可視性フラグ。`false` の Cell は表示から除外される
 */
abstract class KsBridgeCell @JvmOverloads constructor(
    var title: String,
    var descriptionText: String? = null,
    var valueText: String? = null,
    var hintText: String? = null,
    var isEnabled: Boolean = true,
    var isVisible: Boolean = true,
) {

    /** Bridge が採番した canonical UUID 文字列の Cell ID。 */
    var cellID: String = KsBridgeIdentifier.make()
        private set

    /** アイコン画像（未指定は `null`）。上位層が解決した platform 画像をそのまま受け取る。 */
    var icon: Drawable? = null

    /** Cell 個別スタイルの上書き（未指定は `null` で Theme を継承） */
    var style: KsBridgeCellStyle? = null

    /**
     * 既存 Cell の cellID を引き継ぐ。
     *
     * Section の内容を差し替えるとき（`replaceSection`）に、配下 Cell の採番済み cellID を
     * 温存するために使う。採番済みの ID を載せた DTO で差し替えると、差し替え後のユーザー操作
     * 通知は従前と同じ cellID で届く。
     *
     * canonical UUID 文字列として解釈できない値は引き継がず、DTO 自身が採番した ID のままにする。
     *
     * @param cellID 引き継ぐ cellID
     * @return 引き継いだ場合は `true`、解釈できず無視した場合は `false`
     */
    fun adoptCellID(cellID: String): Boolean {
        val canonical = KsBridgeIdentifier.canonical(cellID) ?: return false
        this.cellID = canonical
        return true
    }

    /**
     * 指定 ID で Native の Cell を組み立てる。
     *
     * 内容更新（`replaceCell` / `replaceCells`）では更新対象の ID を渡し、DTO 自身の [cellID]
     * ではなく対象の identity を保つ。
     *
     * @param id 生成する Cell の ID
     * @param relay ユーザー操作の通知先へ転送する中継
     */
    @JvmSynthetic
    internal abstract fun makeCell(id: String, relay: KsBridgeInteractionRelay): Cell

    /**
     * DTO 自身が採番した ID で Native の Cell を組み立てる。
     *
     * @param relay ユーザー操作の通知先へ転送する中継
     */
    @JvmSynthetic
    internal fun makeCell(relay: KsBridgeInteractionRelay): Cell = makeCell(cellID, relay)

    /** スタイル上書きを Native の `CellStyle` へ解決する。未指定なら全項目未指定のスタイル。 */
    @get:JvmSynthetic
    internal val resolvedStyle: CellStyle
        get() = style?.resolve() ?: CellStyle()

    /** アイコンを Native の `KsImage` へ包む。未指定なら `null`。 */
    @get:JvmSynthetic
    internal val resolvedIcon: KsImage?
        get() = icon?.let { KsImage.Drawable(it) }
}
