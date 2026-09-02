package jp.kamusoft.kssettingsview.core

/**
 * 単一の設定セクションを表す値型。
 *
 * [id] は文字列で一意、[header] / [footer] は `null` 可、
 * [cells] は空リストでもよい（空セクションは仕様上許容される）。
 *
 * [header] / [footer] は文字列だけでなく任意の View（[KsAnyView] ラップ）も
 * 配置できるよう [SectionAccessory] 型で表現する。Cell（タップ・選択・編集する行）の概念は
 * [header] / [footer] には持ち込まない。
 *
 * # 等価性契約
 *
 * `data class` の自動 `equals` / `hashCode` を採用する。
 * [SectionAccessory.View] の中身（`KsAnyView`）は等価性判定対象から除外される。
 * これは [SectionAccessory.View] が `equals` / `hashCode` を「クラス一致のみで等価」と
 * 手動実装しているため、本クラスが各プロパティに対して素直に `equals` を呼ぶだけで
 * 自動的にそうなる。
 *
 * @property id 一意な ID
 * @property header セクションヘッダ（`null` でヘッダ非表示。文字列ヘッダなら [SectionAccessory.Text]、
 *   任意 View ヘッダなら [SectionAccessory.View]）
 * @property footer セクションフッタ（`null` でフッタ非表示。表現は [header] と同様）
 * @property cells セクション内の Cell 群
 * @property headerHeight セクションヘッダの高さ（AiForms.Maui.SettingsView の `Section.HeaderHeight` 相当）。
 *   既定値 `-1.0` は「自動高さ」を意味し、正値（> 0）は固定高さとして用いる。
 *   高さの解決は「Header を表示する」と判定された後にのみ適用されるため、内容が無い
 *   （`null` または空文字列）Header や [isHeaderVisible] が `false` の Header では、正値を指定しても
 *   UI 層は Header 領域を生成しない（core/ADR-0023）。
 * @property isVisible セクション可視性フラグ（AiForms.Maui.SettingsView の `Section.IsVisible` 相当）。
 *   既定値 `true` で通常表示、`false` の場合は UI 層が当該 Section（header / footer / 全 cells）を
 *   visible projection（描画対象）から除外する。model（`SettingsRoot.sections`）には保持され、
 *   `true` に戻すと元の位置に復活する。`data class` の自動 `equals` / `hashCode` 判定対象に含まれる
 *   （構造同期は id 同一性のみを用いるため、`isVisible` 変化は構造同期の判定に影響しない）。
 * @property isHeaderVisible セクションヘッダの表示トグル（core/ADR-0023）。Header の表示は
 *   「トグル && 内容あり」の AND で決まる。既定値 `true` では [header] に内容があれば Header 領域を
 *   表示し、`false` では内容があっても Header 領域を生成しない。非表示中も内容は Section の状態として
 *   保持され、`true` に戻すとその時点の最新の内容で再表示される。トグルは「内容があっても隠す」専用で、
 *   内容が無い（`null` または空文字列）Header をトグルで表示させることはできない。
 *   [isFooterVisible] および Cell の表示とは独立している。
 * @property isFooterVisible セクションフッタの表示トグル（core/ADR-0023）。意味論は [isHeaderVisible] と対称。
 */
public data class Section(
    val id: String,
    val header: SectionAccessory? = null,
    val footer: SectionAccessory? = null,
    val cells: List<Cell> = emptyList(),
    val headerHeight: Double = -1.0,
    val isVisible: Boolean = true,
    val isHeaderVisible: Boolean = true,
    val isFooterVisible: Boolean = true,
)
