// SettingsRootStore.swift
// KsSettingsViewUI
//
// `SettingsRoot` の状態管理と部分更新 Diff 発行を担う `ObservableObject`。
//
// 構造の更新は Core の `SettingsRootDiff` を Publisher で流す経路（core/ADR-0006）で行う。
// 一方 Theme は Core モデルではなく UI 層に属するため（core/ADR-0009）、Store が直接保持し
// （`@Published var theme: Theme`）、Diff Publisher を介さない独立 API `applyTheme(_:)` で
// 更新する。

#if canImport(UIKit)
import Foundation
import Combine
import UIKit
import KsSettingsViewCore

/// `SettingsRoot` の状態管理と部分更新 Diff 発行を担う Store。
///
/// SwiftUI から `@StateObject` / `@ObservedObject` で監視可能。
/// `root` プロパティは `private(set)` であり、外部からの直接代入を防ぐ。状態の更新は
/// `replaceAll(_:)` / `insertCell(_:in:at:)` 等のメソッド経由のみで行う。
///
/// 内部で `PassthroughSubject<SettingsRootDiff, Never>` を保持し、各メソッド呼び出し時に
/// 対応する `SettingsRootDiff` を発行する。UI 層の `KsSettingsViewController` はこの
/// Publisher を購読し、`applyDiff(_:)` 経由で内部 snapshot を部分更新する。
///
/// Theme は `@Published var theme: Theme` として保持し、`applyTheme(_:)` で更新する。
/// Theme 更新は Diff Publisher を介さず（構造差分ではないため）、`$theme` の値変化として
/// Controller / SwiftUI に伝播する。
@MainActor
public final class SettingsRootStore: ObservableObject {

    /// 現在の `SettingsRoot` 状態。
    ///
    /// 外部からの直接代入を防ぐため `private(set)`。更新は Store のメソッド経由でのみ行う。
    @Published public private(set) var root: SettingsRoot

    /// 現在の Theme。`SettingsRoot` から分離した独立フィールド。
    /// `applyTheme(_:)` で更新する。SwiftUI から `@Published` 経由で監視可能。
    @Published public private(set) var theme: Theme

    /// Diff 発行用の内部 Subject。
    internal let diffSubject = PassthroughSubject<SettingsRootDiff, Never>()

    /// Diff Publisher。
    internal var diffPublisher: AnyPublisher<SettingsRootDiff, Never> {
        return diffSubject.eraseToAnyPublisher()
    }

    /// 内容更新バッチ発行用の内部 Subject。
    internal let contentUpdateBatchSubject = PassthroughSubject<[KsCellID], Never>()

    /// 内容更新バッチ Publisher。UI 層が購読し、対象 Cell 群を1回の部分更新で反映する。
    internal var contentUpdateBatchPublisher: AnyPublisher<[KsCellID], Never> {
        return contentUpdateBatchSubject.eraseToAnyPublisher()
    }

    /// accessory の再計測要求発行用の内部 Subject。
    internal let accessoryMeasureInvalidationSubject = PassthroughSubject<AccessoryTarget, Never>()

    /// accessory の再計測要求 Publisher。UI 層が購読し、対象領域だけを測り直す。
    internal var accessoryMeasureInvalidationPublisher: AnyPublisher<AccessoryTarget, Never> {
        return accessoryMeasureInvalidationSubject.eraseToAnyPublisher()
    }

    /// 初期 `SettingsRoot` と `Theme` を指定して Store を構築する。
    ///
    /// - Parameters:
    ///   - initialRoot: 初期 `SettingsRoot`
    ///   - initialTheme: 初期 `Theme`（既定 `Theme()`）
    public init(initialRoot: SettingsRoot, initialTheme: Theme = Theme()) {
        self.root = initialRoot
        self.theme = initialTheme
    }

    // MARK: - Root 全体操作

    /// `SettingsRoot` 全体を差し替える。
    ///
    /// - Parameter root: 新しい `SettingsRoot`
    public func replaceAll(_ root: SettingsRoot) {
        self.root = root
        diffSubject.send(.full(root))
    }

    // MARK: - Section 操作

    /// Section を追加する。
    public func insertSection(_ section: KsSettingsViewCore.Section, at index: Int) {
        var sections = root.sections
        let clamped = min(max(0, index), sections.count)
        sections.insert(section, at: clamped)
        root = SettingsRoot(sections: sections)
        diffSubject.send(.insertSection(at: clamped, section: section))
    }

    /// 指定 ID の Section を削除する。
    public func removeSection(sectionID: UUID) {
        var sections = root.sections
        guard let index = sections.firstIndex(where: { $0.id == sectionID }) else {
            return
        }
        sections.remove(at: index)
        root = SettingsRoot(sections: sections)
        diffSubject.send(.removeSection(sectionID: sectionID))
    }

    /// Section の順序を変更する。
    public func moveSection(from: Int, to: Int) {
        var sections = root.sections
        guard sections.indices.contains(from) else {
            return
        }
        let moved = sections.remove(at: from)
        let clamped = min(max(0, to), sections.count)
        sections.insert(moved, at: clamped)
        root = SettingsRoot(sections: sections)
        diffSubject.send(.moveSection(from: from, to: to))
    }

    /// 指定 ID の Section を新しい Section で置換する。
    public func replaceSection(sectionID: UUID, new: KsSettingsViewCore.Section) {
        var sections = root.sections
        guard let index = sections.firstIndex(where: { $0.id == sectionID }) else {
            return
        }
        sections[index] = new
        root = SettingsRoot(sections: sections)
        diffSubject.send(.replaceSection(sectionID: sectionID, new: new))
    }

    // MARK: - Cell 操作

    /// Cell を指定 Section に追加する。
    public func insertCell(_ cell: any KsCell, in sectionID: UUID, at index: Int) {
        var sections = root.sections
        guard let sectionIndex = sections.firstIndex(where: { $0.id == sectionID }) else {
            return
        }
        let target = sections[sectionIndex]
        var cells = target.cells
        let clamped = min(max(0, index), cells.count)
        cells.insert(cell, at: clamped)
        sections[sectionIndex] = KsSettingsViewCore.Section(
            id: target.id,
            header: target.header,
            footer: target.footer,
            cells: cells,
            headerHeight: target.headerHeight,
            isVisible: target.isVisible,
            isHeaderVisible: target.isHeaderVisible,
            isFooterVisible: target.isFooterVisible
        )
        root = SettingsRoot(sections: sections)
        diffSubject.send(.insertCell(sectionID: sectionID, at: clamped, cell: cell))
    }

    /// 指定 Cell ID の Cell を削除する。
    public func removeCell(cellID: KsCellID) {
        let updated = mutateCellList { sections in
            for sectionIndex in sections.indices {
                let target = sections[sectionIndex]
                if let cellIndex = target.cells.firstIndex(where: { $0.id == cellID.id }) {
                    var cells = target.cells
                    cells.remove(at: cellIndex)
                    sections[sectionIndex] = KsSettingsViewCore.Section(
                        id: target.id,
                        header: target.header,
                        footer: target.footer,
                        cells: cells,
                        headerHeight: target.headerHeight,
                        isVisible: target.isVisible,
                        isHeaderVisible: target.isHeaderVisible,
                        isFooterVisible: target.isFooterVisible
                    )
                    return true
                }
            }
            return false
        }
        guard updated else { return }
        diffSubject.send(.removeCell(cellID: cellID))
    }

    /// 指定 Cell ID の Cell を新しい Cell で置換する。
    public func replaceCell(cellID: KsCellID, new: any KsCell) {
        let updated = mutateCellList { sections in
            for sectionIndex in sections.indices {
                let target = sections[sectionIndex]
                if let cellIndex = target.cells.firstIndex(where: { $0.id == cellID.id }) {
                    var cells = target.cells
                    cells[cellIndex] = new
                    sections[sectionIndex] = KsSettingsViewCore.Section(
                        id: target.id,
                        header: target.header,
                        footer: target.footer,
                        cells: cells,
                        headerHeight: target.headerHeight,
                        isVisible: target.isVisible,
                        isHeaderVisible: target.isHeaderVisible,
                        isFooterVisible: target.isFooterVisible
                    )
                    return true
                }
            }
            return false
        }
        guard updated else { return }
        diffSubject.send(.replaceCell(cellID: cellID, new: new))
    }

    /// 複数 Cell の内容をまとめて置換し、適用した Cell ID 群を1回のバッチ内容更新として配信する。
    ///
    /// 更新は入力順に適用し、状態は1回だけ更新する。配信は状態更新の後に行うため、購読者は
    /// 配信時点で更新後の現在状態を参照できる。存在しない cellID は無視し、適用が0件のときは
    /// 状態を変えず配信もしない。空配列は何もしない。同一 cellID を複数回指定した場合は
    /// 入力順に適用され、最後の値が状態に残る（配信される ID 群には適用ごとに含まれる）。
    ///
    /// 個々の更新は同じ ID の内容更新であり、Cell の identity を変えない。呼び出し側は対象
    /// cellID と新しい Cell の ID を一致させる（不一致を渡した場合の挙動は保証しない）。
    /// また `isVisible` を変える Cell を渡してはいけない。可視性の変化は内容更新ではなく
    /// `replaceCell(cellID:new:)` / `replaceAll(_:)` の経路で行う。
    ///
    /// - Parameter updates: (対象 cellID, 新しい Cell) の並び。入力順に適用する
    public func replaceCells(_ updates: [(cellID: KsCellID, new: any KsCell)]) {
        if updates.isEmpty { return }
        var sections = root.sections
        var appliedIDs: [KsCellID] = []
        for update in updates {
            for sectionIndex in sections.indices {
                let target = sections[sectionIndex]
                guard let cellIndex = target.cells.firstIndex(where: { $0.id == update.cellID.id }) else {
                    continue
                }
                var cells = target.cells
                cells[cellIndex] = update.new
                sections[sectionIndex] = KsSettingsViewCore.Section(
                    id: target.id,
                    header: target.header,
                    footer: target.footer,
                    cells: cells,
                    headerHeight: target.headerHeight,
                    isVisible: target.isVisible,
                    isHeaderVisible: target.isHeaderVisible,
                    isFooterVisible: target.isFooterVisible
                )
                appliedIDs.append(update.cellID)
                break
            }
        }
        if appliedIDs.isEmpty { return }
        root = SettingsRoot(sections: sections)
        contentUpdateBatchSubject.send(appliedIDs)
    }

    /// 指定 Cell ID の Cell を同一 Section 内で移動する。
    public func moveCell(cellID: KsCellID, to index: Int) {
        let updated = mutateCellList { sections in
            for sectionIndex in sections.indices {
                let target = sections[sectionIndex]
                if let cellIndex = target.cells.firstIndex(where: { $0.id == cellID.id }) {
                    var cells = target.cells
                    let moved = cells.remove(at: cellIndex)
                    let clamped = min(max(0, index), cells.count)
                    cells.insert(moved, at: clamped)
                    sections[sectionIndex] = KsSettingsViewCore.Section(
                        id: target.id,
                        header: target.header,
                        footer: target.footer,
                        cells: cells,
                        headerHeight: target.headerHeight,
                        isVisible: target.isVisible,
                        isHeaderVisible: target.isHeaderVisible,
                        isFooterVisible: target.isFooterVisible
                    )
                    return true
                }
            }
            return false
        }
        guard updated else { return }
        diffSubject.send(.moveCell(cellID: cellID, to: index))
    }

    // MARK: - Accessory / Theme 操作

    /// Accessory（Root H/F / Section H/F）を更新する。
    ///
    /// Section H/F の `sectionID` が現在状態に存在しない場合は、state 更新も Diff 発行も行わない
    /// no-op とする（core/ADR-0020）。Root H/F は `SettingsRoot` 値型に state を持たないため
    /// sectionID 検証の対象外であり、常に Diff を発行する。
    public func updateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?) {
        switch target {
        case .rootHeader, .rootFooter:
            // Root H/F は SettingsRoot 値型に含まれない（UI 層プロパティ）。
            // root state は更新せず、Diff のみ発行して UI 層に反映を委ねる。
            break

        case .sectionHeader(let sectionID):
            guard updateSectionAccessory(sectionID: sectionID, accessory: accessory, isHeader: true) else {
                return
            }

        case .sectionFooter(let sectionID):
            guard updateSectionAccessory(sectionID: sectionID, accessory: accessory, isHeader: false) else {
                return
            }
        }
        diffSubject.send(.updateAccessory(target: target, accessory: accessory))
    }

    /// 表示中の accessory 領域の高さを測り直すよう Host へ要求する。
    ///
    /// view accessory の中身が自分の計測結果を変えても、Host は領域の高さを自動では測り直さない。
    /// 中身の所有者が変化を知った時点で本 API を呼ぶと、対象の accessory 領域だけが再計測される。
    ///
    /// 要求は一過性の通知であり、Store の復元可能な現在状態は変化しない — 購読者がいない間に
    /// 呼んだ要求は誰にも届かないまま捨てられる。固定高さの領域では再計測しても高さが変わらず、
    /// 実質的に何も起きない。
    ///
    /// - Parameter target: 再計測する accessory
    public func invalidateAccessoryMeasurement(target: AccessoryTarget) {
        accessoryMeasureInvalidationSubject.send(target)
    }

    /// `Theme` を適用する。
    ///
    /// 構造差分 (`SettingsRootDiff`) は発行せず、`@Published var theme` の値変化として
    /// Controller / SwiftUI に伝播する。Theme 更新は構造差分の責務ではなく、UI 層独立 API
    /// の責務とする（core/ADR-0009）。
    ///
    /// - Parameter theme: 新しい Theme
    public func applyTheme(_ theme: Theme) {
        // 同値なら通知を抑制（@Published は同値判定を行わないため明示的にチェック）。
        if self.theme == theme { return }
        self.theme = theme
    }

    // MARK: - Preview / Test 用ファクトリ

    /// Preview / Test 用ファクトリ。
    public static func preview(
        root: SettingsRoot,
        theme: Theme = Theme()
    ) -> SettingsRootStore {
        return SettingsRootStore(initialRoot: root, initialTheme: theme)
    }

    // MARK: - 内部ヘルパ

    /// `root.sections` を mutate するための内部ヘルパ。
    /// 変更があった場合のみ `root` を更新する。
    @discardableResult
    private func mutateCellList(_ mutation: (inout [KsSettingsViewCore.Section]) -> Bool) -> Bool {
        var sections = root.sections
        let didMutate = mutation(&sections)
        if didMutate {
            root = SettingsRoot(sections: sections)
        }
        return didMutate
    }

    /// Section H/F を更新する。`accessory` が `.section(...)` 以外の場合は `nil` 扱いとする。
    ///
    /// - Returns: state を更新した場合 `true`、`sectionID` が現在状態に存在せず何もしなかった場合 `false`
    private func updateSectionAccessory(
        sectionID: UUID,
        accessory: SettingsAccessory?,
        isHeader: Bool
    ) -> Bool {
        var sections = root.sections
        guard let sectionIndex = sections.firstIndex(where: { $0.id == sectionID }) else {
            return false
        }
        let target = sections[sectionIndex]

        // SettingsAccessory.section ケースのみ Section H/F に反映。
        let newAccessory: SectionAccessory?
        switch accessory {
        case .section(let s):
            newAccessory = s
        case .root, .none:
            newAccessory = nil
        }

        sections[sectionIndex] = KsSettingsViewCore.Section(
            id: target.id,
            header: isHeader ? newAccessory : target.header,
            footer: isHeader ? target.footer : newAccessory,
            cells: target.cells,
            headerHeight: target.headerHeight,
            isVisible: target.isVisible,
            isHeaderVisible: target.isHeaderVisible,
            isFooterVisible: target.isFooterVisible
        )
        root = SettingsRoot(sections: sections)
        return true
    }
}
#endif
