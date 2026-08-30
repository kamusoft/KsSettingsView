// KsCellID.swift
// KsSettingsViewCore
//
// Cell の識別子として使う Hashable な値型。Core 層に配置し、Diff 型
// (`SettingsRootDiff.removeCell` / `.replaceCell` / `.moveCell`) や
// UI 層の `UICollectionViewDiffableDataSource` のアイテム識別子として共通利用する。
//
// UI 層ではなく Core 層に配置するのは、Core 層の `SettingsRootDiff` が `KsCellID` を
// 参照するためである。
//
// 構造同期における identity 規約（core/ADR-0010）:
//   構造同期（`UICollectionViewDiffableDataSource` の item 集合・順序の差分計算）は
//   **id（識別子）の同一性のみ**で行い、Cell の内容プロパティを判定に用いない。
//   したがって `KsCellID` の同一性は `id`（UUID）のみに限定し、内容ハッシュは含めない。
//   内容を含む識別子にすると、reconfigure が snapshot の識別子を変えないため、同一 id セルへの
//   内容更新が 2 回連続で起きたときに 2 回目で snapshot とドリフトして破綻する。
//
//   内容変化の検出（`.replaceCell` を発行すべきかの判定）は、構造同期とは別レイヤである
//   `DSLDiffCalculator` 等が Cell 値全体の `Hashable` 比較で行う。`KsCellID` は内容比較には
//   一切関与しない（構造同期の identity 専用）。
//
// iOS / Android の Diff identity 非対称性:
//   - iOS 側は `KsCellID`（`id: UUID` のみ）という独立した値型を導入し、
//     `SettingsRootDiff.removeCell` / `.replaceCell` / `.moveCell` の identity として使用する。
//     `UICollectionViewDiffableDataSource` の itemIdentifier に直接渡せるよう、
//     `KsCell.id` をラップした値型としている。
//   - Android 側は対応する独立型を持たず、`SettingsRootDiff.RemoveCell` / `ReplaceCell` /
//     `MoveCell` は `cellId: String`（= `Cell.id` を直接利用）のみで identity を表現する。
//   両プラットフォームで identity の型シグネチャが異なる点に注意（クロスプラットフォームの
//   ブリッジ層を実装する際は、本非対称性を前提にマッピングを設計すること）。

import Foundation

/// Cell の構造同期に使う識別子の値型。
///
/// `KsCell.id`（UUID）のみで同一性（`==` / `hash(into:)`）を判定する。これにより
/// `UICollectionViewDiffableDataSource` の item 識別子が **内容変化では変わらず**、
/// `reconfigureItems` による同一セルの内容更新（破棄・再生成なし）が安定して機能する。
///
/// - Important: Cell の内容（title 等）が変わっても、同じ `id` を持つ Cell の `KsCellID` は
///   等価（`==` が `true`）になる。これは「構造同期は id 同一性のみを用い、内容を用いない」
///   という原則（core/ADR-0010）によるもの。内容変化の反映は
///   `.replaceCell`（reconfigure 経路）が担い、`KsCellID` は構造同期の identity 専用とする。
public struct KsCellID: Hashable, Sendable {
    /// Cell の一意 ID（同じ Cell スロットを跨いで識別可能）
    public let id: UUID

    /// `any KsCell` から識別子を生成する。
    /// - Parameter cell: 対象 Cell
    public init(cell: any KsCell) {
        self.id = cell.id
    }

    /// `KsCell.id`（UUID）を直接指定して識別子を生成する。
    /// - Parameter id: Cell の一意 ID
    public init(id: UUID) {
        self.id = id
    }

    // MARK: - Hashable（id 同一性のみ）
    //
    // 構造同期は id 同一性のみを用い、内容比較を行わないため、`==` / `hash(into:)` は
    // `id` のみを対象とする。`contentHash` 等の内容由来の値は一切含めない。

    public static func == (lhs: KsCellID, rhs: KsCellID) -> Bool {
        return lhs.id == rhs.id
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}
