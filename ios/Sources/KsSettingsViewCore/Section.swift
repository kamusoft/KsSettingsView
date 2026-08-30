// Section.swift
// KsSettingsViewCore
//
// 単一セクションを表す値型。複数の Cell（`any KsCell`）と任意 header / footer を持つ。
// `header` / `footer` は文字列だけでなく任意の View（`KsAnyView` ラップ）を許容するため
// `SectionAccessory?` 型で表現する。

import Foundation

/// 単一の設定セクションを表す値型。
///
/// `id` は `UUID` で一意とし、`header` / `footer` は `nil` 可。
/// `cells` は空リストでもよい（空セクションは仕様上許容される）。
///
/// `cells` は `[any KsCell]`（プロトコル型のヘテロ配列）を型消去ラッパを挟まずに直接保持する
/// （core/ADR-0013）。
///
/// 等価性判定は手動実装。`SectionAccessory.view(KsAnyView)` の中身は判定対象外とする。
/// `cells` の各要素は `AnyHashable` 経由で具象 `KsCell` の `Hashable` 実装に委譲する。
public struct Section: Hashable, Identifiable, Sendable {
    /// 一意な ID
    public let id: UUID
    /// セクションヘッダ（`nil` でヘッダ非表示。文字列ヘッダなら `.text(...)`、
    /// 任意 View ヘッダなら `.view(...)`）
    public let header: SectionAccessory?
    /// セクションフッタ（`nil` でフッタ非表示。表現は `header` と同様）
    public let footer: SectionAccessory?
    /// セクション内の Cell 群（プロトコル型のヘテロ配列）
    public let cells: [any KsCell]
    /// セクションヘッダの高さ（AiForms.Maui.SettingsView の `Section.HeaderHeight` 相当）。
    ///
    /// - `-1`（既定値） → 自動高さ。内容の寸法に合わせて算出する。
    /// - 正値（> 0） → その値を固定 Header 高さとして用いる。
    ///
    /// 高さの解決は「Header を表示する」と判定された後にのみ適用される（core/ADR-0023）。
    /// Header の内容が無い（`nil` または空 text）場合や `isHeaderVisible` が `false` の場合、
    /// `headerHeight` が正値でも UI 層は Header supplementary 自体を生成しない。
    public let headerHeight: Double

    /// セクション可視性フラグ（AiForms.Maui.SettingsView の `Section.IsVisible` 相当）。
    ///
    /// - `true`（既定値） → 通常の表示。UI 層は当該 Section を visible projection に含める。
    /// - `false` → UI 層は当該 Section（header / footer / 全 cells）を visible projection から
    ///   除外する。`SettingsRoot.sections` 自体には保持され、`true` に戻すと元の位置に復活する。
    ///
    /// `isVisible` は値型としての等価性（`Hashable` / `==`）の判定対象に含まれる。
    /// 構造同期（diff / snapshot 構築）の同一性判定は `id` のみで行うため、
    /// `isVisible` の値は構造同期の同一性に影響しない（core/ADR-0010）。
    public let isVisible: Bool

    /// Section Header の表示トグル（core/ADR-0023）。
    ///
    /// Header の表示は「トグル && 内容あり」の AND で決まる。
    ///
    /// - `true`（既定値） → `header` に内容があれば Header 領域を表示する。
    /// - `false` → `header` に内容があっても Header 領域を生成しない。内容は Section の状態として
    ///   保持され、`true` に戻すとその時点の最新の内容で再表示される。
    ///
    /// トグルは「内容があっても隠す」専用であり、内容が無い（`nil` または空 text）Header を
    /// トグルで表示させることはできない。`isFooterVisible` および Cell の表示とは独立している。
    public let isHeaderVisible: Bool

    /// Section Footer の表示トグル（core/ADR-0023）。意味論は `isHeaderVisible` と対称。
    public let isFooterVisible: Bool

    /// 任意フィールドを指定して `Section` を生成する。
    /// - Parameters:
    ///   - id: 一意な ID（既定で新規 UUID 自動採番）
    ///   - header: ヘッダ（既定 `nil`）
    ///   - footer: フッタ（既定 `nil`）
    ///   - cells: Cell 群（既定で空配列）
    ///   - headerHeight: ヘッダ高さ（既定 `-1` = 自動）
    ///   - isVisible: 可視性フラグ（既定 `true`）
    ///   - isHeaderVisible: Header 表示トグル（既定 `true`）
    ///   - isFooterVisible: Footer 表示トグル（既定 `true`）
    public init(
        id: UUID = UUID(),
        header: SectionAccessory? = nil,
        footer: SectionAccessory? = nil,
        cells: [any KsCell] = [],
        headerHeight: Double = -1,
        isVisible: Bool = true,
        isHeaderVisible: Bool = true,
        isFooterVisible: Bool = true
    ) {
        self.id = id
        self.header = header
        self.footer = footer
        self.cells = cells
        self.headerHeight = headerHeight
        self.isVisible = isVisible
        self.isHeaderVisible = isHeaderVisible
        self.isFooterVisible = isFooterVisible
    }

    // MARK: - Hashable 手動実装
    //
    // `cells: [any KsCell]` はプロトコル型配列のため `Hashable` 自動合成が効かない。
    // 各要素を `AnyHashable` で包んで具象 `KsCell` の `Hashable` 実装に委譲する。

    public static func == (lhs: Section, rhs: Section) -> Bool {
        guard lhs.id == rhs.id else { return false }
        guard lhs.header == rhs.header else { return false }
        guard lhs.footer == rhs.footer else { return false }
        guard lhs.headerHeight == rhs.headerHeight else { return false }
        guard lhs.isVisible == rhs.isVisible else { return false }
        guard lhs.isHeaderVisible == rhs.isHeaderVisible else { return false }
        guard lhs.isFooterVisible == rhs.isFooterVisible else { return false }
        guard lhs.cells.count == rhs.cells.count else { return false }
        for (l, r) in zip(lhs.cells, rhs.cells) {
            if AnyHashable(l) != AnyHashable(r) { return false }
        }
        return true
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
        hasher.combine(header)
        hasher.combine(footer)
        hasher.combine(headerHeight)
        hasher.combine(isVisible)
        hasher.combine(isHeaderVisible)
        hasher.combine(isFooterVisible)
        for cell in cells {
            hasher.combine(AnyHashable(cell))
        }
    }
}
