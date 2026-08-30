// SettingsRootDiff.swift
// KsSettingsViewCore
//
// `SettingsRoot` に対する部分更新を表現する sum type。
//
// Theme 更新のケースは持たない。Theme は構造差分の責務ではなく UI 層の責務であり
// （core/ADR-0009）、UI 層独立 API（`SettingsRootStore.applyTheme(_:)` 相当）が受け持つ。

import Foundation

/// `SettingsRoot` への部分更新を表現する sum type。
///
/// Native UI 層・MAUI Handler 層の `applyDiff` 実装は本型の全ケースを網羅して処理する。
/// `Hashable` プロトコルへ準拠するが、`any KsCell` を含むケース（`insertCell` / `replaceCell`）
/// は existential type の制約により手動 `Hashable` 実装が必要であるため、内部で `AnyHashable`
/// 経由で Cell の hash を取り込む。
public enum SettingsRootDiff: Hashable, Sendable {
    /// 全体差し替え
    case full(SettingsRoot)
    /// Section 追加
    case insertSection(at: Int, section: Section)
    /// Section 削除
    case removeSection(sectionID: UUID)
    /// Section 順序変更
    case moveSection(from: Int, to: Int)
    /// Section 全体置換
    case replaceSection(sectionID: UUID, new: Section)
    /// Section 内 Cell 追加
    case insertCell(sectionID: UUID, at: Int, cell: any KsCell)
    /// Cell 削除
    case removeCell(cellID: KsCellID)
    /// Cell 置換（= **同一 id を持つ Cell の内容更新 / reconfigure**）。
    ///
    /// - Important: `cellID.id`（置換対象 Cell の identity）と `new.id`（新しい Cell の identity）が
    ///   一致していることは **呼び出し側の責務** とする。
    ///   Cell の identity を変更したい場合は本ケースではなく `removeCell` + `insertCell` で表現すること。
    case replaceCell(cellID: KsCellID, new: any KsCell)
    /// Cell 順序変更（Section 内のみ、Section 間移動は別途 `removeCell` + `insertCell` で表現）
    case moveCell(cellID: KsCellID, to: Int)
    /// Root H/F / Section H/F の追加・更新・削除（`nil` は削除を意味する）
    case updateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?)

    // MARK: - Hashable 手動実装

    public static func == (lhs: SettingsRootDiff, rhs: SettingsRootDiff) -> Bool {
        switch (lhs, rhs) {
        case let (.full(a), .full(b)):
            return a == b
        case let (.insertSection(aIndex, aSection), .insertSection(bIndex, bSection)):
            return aIndex == bIndex && aSection == bSection
        case let (.removeSection(a), .removeSection(b)):
            return a == b
        case let (.moveSection(aFrom, aTo), .moveSection(bFrom, bTo)):
            return aFrom == bFrom && aTo == bTo
        case let (.replaceSection(aID, aNew), .replaceSection(bID, bNew)):
            return aID == bID && aNew == bNew
        case let (.insertCell(aSectionID, aIndex, aCell), .insertCell(bSectionID, bIndex, bCell)):
            return aSectionID == bSectionID
                && aIndex == bIndex
                && AnyHashable(aCell) == AnyHashable(bCell)
        case let (.removeCell(a), .removeCell(b)):
            return a == b
        case let (.replaceCell(aID, aNew), .replaceCell(bID, bNew)):
            return aID == bID && AnyHashable(aNew) == AnyHashable(bNew)
        case let (.moveCell(aID, aTo), .moveCell(bID, bTo)):
            return aID == bID && aTo == bTo
        case let (.updateAccessory(aTarget, aAccessory), .updateAccessory(bTarget, bAccessory)):
            return aTarget == bTarget && aAccessory == bAccessory
        default:
            return false
        }
    }

    public func hash(into hasher: inout Hasher) {
        switch self {
        case .full(let root):
            hasher.combine(0)
            hasher.combine(root)
        case let .insertSection(index, section):
            hasher.combine(1)
            hasher.combine(index)
            hasher.combine(section)
        case .removeSection(let sectionID):
            hasher.combine(2)
            hasher.combine(sectionID)
        case let .moveSection(from, to):
            hasher.combine(3)
            hasher.combine(from)
            hasher.combine(to)
        case let .replaceSection(sectionID, new):
            hasher.combine(4)
            hasher.combine(sectionID)
            hasher.combine(new)
        case let .insertCell(sectionID, index, cell):
            hasher.combine(5)
            hasher.combine(sectionID)
            hasher.combine(index)
            hasher.combine(AnyHashable(cell))
        case .removeCell(let cellID):
            hasher.combine(6)
            hasher.combine(cellID)
        case let .replaceCell(cellID, new):
            hasher.combine(7)
            hasher.combine(cellID)
            hasher.combine(AnyHashable(new))
        case let .moveCell(cellID, to):
            hasher.combine(8)
            hasher.combine(cellID)
            hasher.combine(to)
        case let .updateAccessory(target, accessory):
            hasher.combine(9)
            hasher.combine(target)
            hasher.combine(accessory)
        }
    }
}
