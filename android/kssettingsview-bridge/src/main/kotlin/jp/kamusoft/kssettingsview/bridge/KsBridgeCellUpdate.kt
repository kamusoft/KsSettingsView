package jp.kamusoft.kssettingsview.bridge

/**
 * 複数 Cell の内容をまとめて置換するときの 1 件分の指定。
 *
 * [cellID] は Bridge が採番して返した既存 Cell の ID で、[cell] はその位置へ写し取る新しい内容。
 * [cell] 自身が持つ `cellID` は使われず、更新後も対象 Cell の identity は [cellID] のまま保たれる。
 *
 * [cell] は共通基底型で受けるため、1 回のバッチに Cell 種の異なる更新を混載できる。
 *
 * @property cellID 更新対象の cellID
 * @property cell 更新後の内容
 */
class KsBridgeCellUpdate(
    val cellID: String,
    val cell: KsBridgeCell,
)
