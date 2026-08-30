package jp.kamusoft.kssettingsview.ui

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.core.Cell

/**
 * Cell 描画用 ViewHolder の抽象基底。
 *
 * `[T : Cell]` を型パラメータに取り、`bind(cell, theme)` を型安全に呼べるようにする。
 *
 * # ライフサイクル契約
 *
 * - [bind] は ListAdapter の `onBindViewHolder` から呼ばれる。
 * - [reset] は ListAdapter の `onViewRecycled` から呼ばれ、ViewHolder 内に保持されている
 *   listener / Job / Disposable / 画像参照などを必ず解放する責務を持つ。
 *
 * # 内容更新の部分反映（refactor-display-state-sync）
 *
 * 「表示状態同期の三層分離」原則に従い、同一 id の Cell の内容変化はセルを再生成せず反映する。
 * 具体的には、`KsSettingsView.applyDiff` の `ReplaceCell`（同一 id の内容更新）が
 * `KsSettingsListAdapter.submitContentUpdate` 経由で該当 position に `notifyItemChanged` を発行し、
 * **同一 ViewHolder に対して再度 [bind] を呼ぶ**（破棄・再生成しない）。[bind] は最新 Cell の内容で
 * View を更新する純粋な再反映として実装すること（重い初期化を毎回行わない）。
 *
 * チェック系 Cell（Switch / Checkbox / Radio / SimpleCheck）の ViewHolder は、ユーザー操作時に
 * View 自身を直接トグル（TwoWay）して `onValueChanged` / `onSelected` でモデルへ書き戻す。この
 * TwoWay の表示更新は `submitList` / `DiffUtil` の再構築を経由しない。RadioCell のグループ連動
 * （同一 `groupId` の他セルの選択解除）は、該当セルへの `notifyItemChanged`（ReplaceCell 経路）で
 * 反映され、グループ全体の再生成は行わない。
 *
 * # 可視性
 *
 * 外部モジュール（Sample アプリや利用側アプリ）が独自 Cell 型用に派生 ViewHolder を
 * 定義できるよう `public` で公開する。利用側が独自 Cell 型 `XxxCell` に対して
 * `class XxxCellViewHolder(view: View) : CellViewHolder<XxxCell>(view)`
 * のような派生を書けることが、独自 Cell 型を [KsCellRegistry] に登録する前提となる。
 */
abstract class CellViewHolder<T : Cell>(itemView: View) : RecyclerView.ViewHolder(itemView) {

    /**
     * Cell の内容と Theme を ViewHolder に反映する。
     *
     * @param cell 描画対象の Cell
     * @param theme SettingsRoot に紐付く Theme（`CellStyle` の null フィールドを補完するために参照）
     */
    abstract fun bind(cell: T, theme: Theme)

    /**
     * ViewHolder 再利用時の内部状態リセット。
     *
     * 既定実装は何もしない。listener / 画像参照 / 進行中 Job などを保持する派生クラスは
     * オーバーライドして必ずクリアすること。
     */
    open fun reset() {
        // no-op（派生クラスでオーバーライド）
    }
}
