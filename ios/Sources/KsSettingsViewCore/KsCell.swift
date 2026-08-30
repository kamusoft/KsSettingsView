// KsCell.swift
// KsSettingsViewCore
//
// 全 Cell 種類が満たすべき共通契約（プロトコル）。

import Foundation

/// 全 Cell が満たすべき共通契約。
///
/// `Identifiable` の `id` は `UUID` に固定する（Bridge 境界では `String` 化される）。
/// プロトコル定義側で `where ID == UUID` を明示することで、
/// `[any KsCell]` の利用側（`Section.cells` など）で改めて where 句を書く必要がなくなる。
/// `Hashable` 要件は `UICollectionViewDiffableDataSource` の前提条件。
/// 具象 Cell 型（`LabelCell`, `SwitchCell` 等）は `KsSettingsViewUI` 層に定義される。
///
/// # スタイルを契約に含めない理由
///
/// 本プロトコルは `var style: CellStyle { get }` を要求しない。`CellStyle` は UI 層
/// （`KsSettingsViewUI`）に属し、Core からは参照できないためである（core/ADR-0009）。
/// 各具象 Cell（UI 層配置）が任意で `style: CellStyle` プロパティを持つ。
///
/// # 表示状態同期における等価性の扱い
///
/// `Hashable`（`Equatable`）は **値型としての等価性**（`id` を含む全フィールド比較）を表す。これは
/// 一般的な値比較・テストのための性質である。ただし、**差分検出（snapshot の構造同期 = item 集合・
/// 順序の再構築）はこの内容等価性を構造同期の同一性判定に用いてはならない**。構造同期は item 識別子
/// `KsCellID`（`id` ベース）の同一性のみで Cell の追加・削除・移動・差し替えを検出する。
///
/// 同一 `id`（同一 `KsCellID`）の Cell の内容変化は、`reloadItems`（セル破棄・再生成）ではなく
/// `reconfigureItems`（iOS 15+、同一セルを破棄せず再構成）で反映する。構造同期・内容同期・
/// 可視性の三経路を分離する原則は core/ADR-0010 を参照。iOS UI 層での具体的な適用は
/// `KsSettingsViewController.applyReplaceCell` を参照。
public protocol KsCell: Hashable, Identifiable, Sendable where ID == UUID {
    /// 一意な ID
    var id: UUID { get }
}
