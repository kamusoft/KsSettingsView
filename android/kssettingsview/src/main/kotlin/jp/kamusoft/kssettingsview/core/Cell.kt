package jp.kamusoft.kssettingsview.core

/**
 * 全 Cell が満たすべき共通契約。
 *
 * `sealed interface` ではなく通常の `interface` として定義し、外部モジュール（Sample アプリ等）
 * からも `Cell` を実装する具象 Cell 型を新規定義できるようにする（core/ADR-0013）。
 * Kotlin の sealed 制約では別 Gradle モジュールから実装できず、Sample アプリ独自の Cell を
 * 定義できないためである。`when` の網羅性チェックの代わりに、
 * UI 層の `KsCellRegistry`（実行時レジストリ + `strictMode` フラグ）で未登録 Cell を検出する。
 *
 * 具象 Cell 型（`LabelCell`, `SwitchCell` 等）は UI 層（`jp.kamusoft.kssettingsview.ui`
 * パッケージ）が `Cell` を実装する形で定義する。
 * Sample アプリ等の利用側でも、本 `Cell` インターフェースを実装することで独自 Cell を定義できる。
 *
 * # スタイルを契約に含めない理由
 *
 * 本契約は `val style: CellStyle` 抽象プロパティを要求しない。`CellStyle` は UI 層
 * （`jp.kamusoft.kssettingsview.ui` パッケージ）に属し、Core からは参照できないためである
 * （core/ADR-0009）。
 * 各具象 Cell が個別に `style: CellStyle` プロパティを持ち、UI 層の
 * `DSLStyleModifiableCell` インターフェース経由でスタイル合成経路にアクセスする。
 *
 * # 表示状態同期における等価性の扱い
 *
 * 具象 Cell（`data class`）の `equals` / `hashCode` は **値型としての等価性**（id を含む全フィールド比較）
 * を表す。これは一般的な値比較・テスト・コレクション操作のための性質である。ただし、**差分検出
 * （`DiffUtil` / `UICollectionViewDiffableDataSource` の構造同期）はこの内容等価性を構造同期の同一性
 * 判定に用いてはならない**。構造同期は [id] の同一性のみで Cell の追加・削除・移動・差し替えを検出する。
 *
 * 同一 [id] を持つ Cell の内容（プロパティ）変化は、セルを破棄・再生成せず **同一セル（ViewHolder / Cell）
 * の部分更新（reconfigure）** で反映する。構造同期・内容同期・可視性の三経路を分離する原則は
 * core/ADR-0010 を参照。Android UI 層での具体的な判定は
 * `KsSettingsListAdapter.areContentsTheSame` / `getItemId` の実装コメントを参照。
 *
 * @property id 一意な ID（Bridge 境界では String として扱われる）。
 *   **一意性は呼び出し側の責務**であり、Core 層では値域チェックを行わない。
 */
interface Cell {
    val id: String
}
