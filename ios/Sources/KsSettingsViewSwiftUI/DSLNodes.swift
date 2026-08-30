// DSLNodes.swift
// KsSettingsViewSwiftUI
//
// DSL（`KsSettingsView { Section { Cell... } }`）の中間ノード型。
//
// 目的:
//   - DSL ビルド中に Section / Cell の **ID 採番ヒント** を保持する。
//   - 評価結果として `[KsSettingsViewCore.Section]` を取り出せるよう、
//     `resolvedSections(...)` で `Section.id` および Cell の id を確定する。
//
// ## Cell の ID 書き換え戦略
//
// `KsCell` プロトコルが要求する `id: UUID` は通常 `init(... id: UUID = UUID())` のように
// 値型ごとに自動採番される。DSL では body 再評価のたびに新規 `UUID` が生成されると
// Diff 同一性判定が破壊されるため、DSL ラッパが採番した「安定 UUID」で Cell の `id` を
// **書き換える** 必要がある（core/ADR-0008）。
//
// この書き換え規約である `DSLReidentifiable` プロトコルは `KsSettingsViewCore` モジュールに
// 配置する（`import KsSettingsViewCore` 経由で参照）。Core 配置の理由は、具象 Cell
// （`LabelCell` / `SwitchCell` 等）が `KsSettingsViewUI` 配置であるため、SwiftUI 層に置くと
// 具象 Cell を準拠させるために `KsSettingsViewUI → KsSettingsViewSwiftUI` の依存が必要になり、
// 既存の `KsSettingsViewSwiftUI → KsSettingsViewUI` と循環するからである。
// 共通祖先である Core に protocol を置くことで、`*-ui → *-core ← *-swiftui` のレイヤリングを
// 維持しつつ、具象 Cell が DSL の rebind 規約を満たせる。
//
// 具象 Cell（`LabelCell` / `SwitchCell` 等）は本プロトコルへの準拠を必須とする。
// opt-in しない Cell では、利用者責任で `id` の安定性を確保してもらう
// （`.cellID(_:)` 明示指定や ID 不変の自前 Cell 等）。

#if canImport(UIKit)
import Foundation
import SwiftUI
import KsSettingsViewCore
import KsSettingsViewUI

/// DSL における Cell ノード。
///
/// `any KsCell` 自体と、ID 採番に使うヒントを保持する。
public struct DSLCellNode: @unchecked Sendable {
    /// 利用者が DSL で記述した Cell 値（modifier 適用後の最終形）。
    public let cell: any KsCell
    /// `.cellID(_:)` 等で明示指定された ID（`nil` ならフォールバックを採用）。
    public let identityHint: DSLIdentityHint?

    public init(cell: any KsCell, identityHint: DSLIdentityHint? = nil) {
        self.cell = cell
        self.identityHint = identityHint
    }

    /// セクション位置・Cell 型から導出した安定 UUID を返す。
    /// - Parameters:
    ///   - sectionID: 親 Section の確定 ID
    ///   - indexInSection: Section 内 0 始まり位置
    /// - Returns: 採番済み UUID
    public func resolvedID(sectionID: UUID, indexInSection: Int) -> UUID {
        if let hint = identityHint {
            return DSLIdentityUUID.uuid(from: hint)
        }
        let cellTypeName = String(reflecting: type(of: cell))
        let fallback = DSLIdentityHint.positional(
            sectionID: sectionID,
            indexInSection: indexInSection,
            cellType: cellTypeName
        )
        return DSLIdentityUUID.uuid(from: fallback)
    }

    /// 採番済み UUID を反映した Cell を返す。
    ///
    /// Cell が `DSLReidentifiable` 準拠なら `withDSLID(_:)` を呼ぶ。
    /// 準拠していない場合は元 Cell をそのまま返す（利用者責任で id 安定性確保）。
    public func resolvedCell(withID id: UUID) -> any KsCell {
        if let reidentifiable = cell as? any DSLReidentifiable {
            // 既に同じ id なら新規生成を避ける。
            if reidentifiable.id == id {
                return reidentifiable
            }
            return rebind(reidentifiable, newID: id)
        }
        return cell
    }

    /// `any DSLReidentifiable` を具象 `Self` に解決して `withDSLID(_:)` を呼ぶヘルパ。
    private func rebind<T: DSLReidentifiable>(_ cell: T, newID: UUID) -> any KsCell {
        return cell.withDSLID(newID)
    }
}

/// DSL における Section ノード。
public struct DSLSectionNode: @unchecked Sendable {
    /// 利用者が DSL で記述した Section 値（modifier 適用後の最終形）。
    /// 注: `id` フィールドは未解決のため使用しない（resolvedID(rootIdx:) で確定）。
    public let section: KsSettingsViewCore.Section
    /// 明示 ID または ForEach 由来のヒント（`nil` ならフォールバックを採用）。
    public let identityHint: DSLIdentityHint?
    /// Section 内 Cell の DSL ノード列。`section.cells` と 1:1 で対応する。
    public let cellNodes: [DSLCellNode]

    public init(
        section: KsSettingsViewCore.Section,
        identityHint: DSLIdentityHint? = nil,
        cellNodes: [DSLCellNode]
    ) {
        self.section = section
        self.identityHint = identityHint
        self.cellNodes = cellNodes
    }

    /// ルート位置から導出した安定 UUID を返す。
    /// - Parameter rootIdx: ルート内 0 始まり位置
    public func resolvedID(rootIdx: Int) -> UUID {
        if let hint = identityHint {
            return DSLIdentityUUID.uuid(from: hint)
        }
        if case .text(let text) = section.header {
            return DSLIdentityUUID.uuid(from: .headerText(rootIdx: rootIdx, text: text))
        }
        return DSLIdentityUUID.uuid(from: .rootPosition(rootIdx: rootIdx))
    }
}

/// DSL 評価結果のルート。Section ノード列 + Root H/F（modifier 由来）+ Theme を保持する。
public struct DSLRootTree: @unchecked Sendable {
    /// Section ノード列（ルート位置順）。
    public let sectionNodes: [DSLSectionNode]
    /// `.rootHeader(...)` modifier で指定された Root Header（`nil` で非表示）。
    public let rootHeader: RootAccessory?
    /// `.rootFooter(...)` modifier で指定された Root Footer（`nil` で非表示）。
    public let rootFooter: RootAccessory?
    /// `.theme(...)` modifier で指定された Theme。
    public let theme: Theme

    public init(
        sectionNodes: [DSLSectionNode],
        rootHeader: RootAccessory? = nil,
        rootFooter: RootAccessory? = nil,
        theme: Theme = Theme()
    ) {
        self.sectionNodes = sectionNodes
        self.rootHeader = rootHeader
        self.rootFooter = rootFooter
        self.theme = theme
    }

    /// Section / Cell の resolved UUID を反映した `[KsSettingsViewCore.Section]` を返す。
    public func resolvedSections() -> [KsSettingsViewCore.Section] {
        return sectionNodes.enumerated().map { (idx, node) in
            let sectionID = node.resolvedID(rootIdx: idx)
            let resolvedCells: [any KsCell] = node.cellNodes.enumerated().map { (cellIdx, cellNode) in
                let cellID = cellNode.resolvedID(sectionID: sectionID, indexInSection: cellIdx)
                return cellNode.resolvedCell(withID: cellID)
            }
            return KsSettingsViewCore.Section(
                id: sectionID,
                header: node.section.header,
                footer: node.section.footer,
                cells: resolvedCells,
                headerHeight: node.section.headerHeight,
                isVisible: node.section.isVisible,
                isHeaderVisible: node.section.isHeaderVisible,
                isFooterVisible: node.section.isFooterVisible
            )
        }
    }

    /// DSL ツリーから `SettingsRoot` を構築する。
    ///
    /// `Theme` は `SettingsRoot` から分離されたため、本メソッドは `sections` のみを保持する
    /// `SettingsRoot` を返す。Theme は別経路（Store.applyTheme / View modifier）で適用される。
    public func toSettingsRoot() -> SettingsRoot {
        return SettingsRoot(sections: resolvedSections())
    }
}
#endif
