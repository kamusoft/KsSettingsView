// DSLDiffCalculator.swift
// KsSettingsViewSwiftUI
//
// DSL の旧宣言ツリーと新宣言ツリーから `SettingsRootDiff` 列を算出する。
//
// アルゴリズム概要:
//   1. Section レベル：旧/新で Section ID 集合を比較し
//      insertSection / removeSection / moveSection / updateAccessory（Section H/F）を発行。
//   2. 各 Section 内 Cell レベル：Cell ID 集合を比較し
//      insertCell / removeCell / moveCell を発行（構造同期＝id 同一性のみ）。同一 id で内容のみが
//      異なる場合は replaceCell を発行する。
//   3. Root H/F：`.rootHeader(...)` / `.rootFooter(...)` の値変化で
//      updateAccessory（Root H/F）を発行。
//   4. Theme：Diff 列には載せず、呼び出し側が `Store.applyTheme(_:)` を別途呼ぶ
//      （Theme は構造モデルから分離され UI 層の独立した更新経路で扱う。core/ADR-0009）。
//   5. `KsAnyView` を含む `.view(...)` ケースは差分検出に参加しない（同ケース同士は等価扱い）。
//
// # 表示状態同期の三層分離（core/ADR-0010）
//
//   `replaceCell` は「**同一 id の内容更新（reconfigure 経路）**」を表し、構造同期（snapshot の item
//   集合・順序）を変更しない。`KsSettingsViewController.applyReplaceCell` が `reconfigureItems`
//   （iOS 15+、同一セルを破棄せず再構成）で反映するため、セルの破棄・再生成（reload）やちらつきは
//   生じない。構造同期（insertCell / removeCell / moveCell）は id 同一性のみで判定し、Cell の内容
//   プロパティを構造同期の判定には用いない。
//
//   注: Android（Compose）は内容変化で ReplaceCell を構造 Diff として発行せず、アダプタが
//   ViewHolder を直接部分更新する。経路の差は実装都合であり、上位原則「構造同期は id 同一性のみ・
//   内容更新はセルを再生成しない」は両プラットフォーム共通。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore
import KsSettingsViewUI

/// DSL 旧ツリーと新ツリーを比較し、`SettingsRootDiff` 列を算出するユーティリティ。
public enum DSLDiffCalculator {

    /// 旧/新の DSL ルートツリー（resolved 済み）を比較し、Diff 列を返す。
    /// - Parameters:
    ///   - old: 前回 body 評価結果（resolved 済み `[KsSettingsViewCore.Section]`）と
    ///          Root H/F / Theme
    ///   - new: 今回 body 評価結果
    /// - Returns: 順序付き Diff 列。空の場合は何も適用する必要なし。
    public static func compute(
        from old: ResolvedTree,
        to new: ResolvedTree
    ) -> [SettingsRootDiff] {
        var diffs: [SettingsRootDiff] = []

        // 完全一致なら早期 return。
        if old.sections == new.sections
            && old.rootHeader == new.rootHeader
            && old.rootFooter == new.rootFooter
            && old.theme == new.theme {
            return []
        }

        // preflight は「headerHeight 変化」「可視性変化」「Header / Footer 表示トグル変化」の 3 つを
        // 検出し、いずれも `.full(newRoot)` のみを発行して layout と snapshot を作り直す。
        //
        // - headerHeight の差は Section H/F の accessory 比較にも構造同期（id 同一性）にも現れず、
        //   通常の Diff 経路では表示へ届かない（core/ADR-0018）。
        // - 可視性の差を通常の `.replaceCell`（reconfigure 経路）に乗せると、表示行の追加・削除を
        //   取りこぼす（core/ADR-0010）。
        // - Header / Footer 表示トグルの差も accessory の値そのものは変えないため、
        //   accessory 比較には現れず通常の Diff 経路では表示へ届かない（core/ADR-0023）。
        //
        // 同一再評価内で同一 ID の Cell の内容も変わっている場合、その内容変化は `.full` の適用が
        // 内包する内容再適用で表示へ届くため、`.full` に `.replaceCell` を続けて発行しない
        // （同一 Cell への内容再適用が同一適用内で二重に走らないようにする）。
        if containsHeaderHeightChange(from: old.sections, to: new.sections)
            || containsVisibilityChange(from: old.sections, to: new.sections)
            || containsAccessoryVisibilityChange(from: old.sections, to: new.sections) {
            return [.full(SettingsRoot(sections: new.sections))]
        }

        // 1. Section レベル
        diffs.append(contentsOf: sectionLevelDiffs(old: old.sections, new: new.sections))

        // 2. 各 Section 内 Cell レベル（両方に存在する Section に対してのみ）
        let oldByID = Dictionary(uniqueKeysWithValues: old.sections.map { ($0.id, $0) })
        for newSection in new.sections {
            guard let oldSection = oldByID[newSection.id] else { continue }
            diffs.append(contentsOf: cellLevelDiffs(
                sectionID: newSection.id,
                old: oldSection.cells,
                new: newSection.cells
            ))
        }

        // 1.5 Section H/F 変化（同 SectionID）
        for newSection in new.sections {
            guard let oldSection = oldByID[newSection.id] else { continue }
            if oldSection.header != newSection.header {
                diffs.append(.updateAccessory(
                    target: .sectionHeader(sectionID: newSection.id),
                    accessory: newSection.header.map { .section($0) }
                ))
            }
            if oldSection.footer != newSection.footer {
                diffs.append(.updateAccessory(
                    target: .sectionFooter(sectionID: newSection.id),
                    accessory: newSection.footer.map { .section($0) }
                ))
            }
        }

        // 3. Root H/F
        if old.rootHeader != new.rootHeader {
            diffs.append(.updateAccessory(
                target: .rootHeader,
                accessory: new.rootHeader.map { .root($0) }
            ))
        }
        if old.rootFooter != new.rootFooter {
            diffs.append(.updateAccessory(
                target: .rootFooter,
                accessory: new.rootFooter.map { .root($0) }
            ))
        }

        // 4. Theme は構造 Diff の対象外（core/ADR-0009）。
        //    Theme 変化は呼び出し側で `Store.applyTheme(_:)` を別途呼ぶ責務とする。
        //    ResolvedTree.theme の値は呼び出し側（`evaluateAndApplyDiff` 等）が参照する。

        return diffs
    }

    // MARK: - 可視性変化の preflight 検出

    /// 旧/新ツリーの間で、同一 ID の Section について `isVisible` の値が変化している、または
    /// 同一 Cell ID で `(cell as? VisibilityAware)?.isVisible ?? true` の値が変化しているかを判定する。
    ///
    /// 可視性差分は通常の `.replaceCell`（reconfigure 経路）には乗せられないため、検出した場合は
    /// `.full(newRoot)` のみを発行する（core/ADR-0010）。
    internal static func containsVisibilityChange(
        from old: [KsSettingsViewCore.Section],
        to new: [KsSettingsViewCore.Section]
    ) -> Bool {
        // 旧 Section / Cell の id → isVisible マップを構築する。
        var oldSectionVisible: [UUID: Bool] = [:]
        var oldCellVisible: [UUID: Bool] = [:]
        for section in old {
            oldSectionVisible[section.id] = section.isVisible
            for cell in section.cells {
                oldCellVisible[cell.id] = (cell as? VisibilityAware)?.isVisible ?? true
            }
        }
        // 新側を走査し、同一 ID で `isVisible` が変化しているものがあれば true を返す。
        for section in new {
            if let oldVis = oldSectionVisible[section.id], oldVis != section.isVisible {
                return true
            }
            for cell in section.cells {
                let newVis = (cell as? VisibilityAware)?.isVisible ?? true
                if let oldVis = oldCellVisible[cell.id], oldVis != newVis {
                    return true
                }
            }
        }
        return false
    }

    // MARK: - Header / Footer 表示トグル変化の preflight 検出

    /// 旧/新ツリーの間で、同一 ID の Section の `isHeaderVisible` / `isFooterVisible` が
    /// 変化しているかを判定する。
    ///
    /// トグルは accessory の値を変えないため Section H/F の accessory 比較には現れず、
    /// 構造同期（id 同一性）にも現れない。検出した場合は `.full(newRoot)` のみを発行して
    /// layout を作り直す（core/ADR-0023）。
    internal static func containsAccessoryVisibilityChange(
        from old: [KsSettingsViewCore.Section],
        to new: [KsSettingsViewCore.Section]
    ) -> Bool {
        var oldToggles: [UUID: (header: Bool, footer: Bool)] = [:]
        for section in old {
            oldToggles[section.id] = (section.isHeaderVisible, section.isFooterVisible)
        }
        for section in new {
            guard let oldToggle = oldToggles[section.id] else { continue }
            if oldToggle.header != section.isHeaderVisible
                || oldToggle.footer != section.isFooterVisible {
                return true
            }
        }
        return false
    }

    // MARK: - headerHeight 変化の preflight 検出

    /// 旧/新ツリーの間で、同一 ID の Section の `headerHeight` が変化しているかを判定する。
    ///
    /// 検出対象は固定高さ間の変更（正値 → 別の正値）、自動から固定（`-1` → 正値）、
    /// 固定から自動（正値 → `-1`）のいずれも含む。
    internal static func containsHeaderHeightChange(
        from old: [KsSettingsViewCore.Section],
        to new: [KsSettingsViewCore.Section]
    ) -> Bool {
        var oldHeaderHeights: [UUID: Double] = [:]
        for section in old {
            oldHeaderHeights[section.id] = section.headerHeight
        }
        for section in new {
            guard let oldHeight = oldHeaderHeights[section.id] else { continue }
            if oldHeight != section.headerHeight {
                return true
            }
        }
        return false
    }

    // MARK: - Section レベル突合

    private static func sectionLevelDiffs(
        old: [KsSettingsViewCore.Section],
        new: [KsSettingsViewCore.Section]
    ) -> [SettingsRootDiff] {
        var diffs: [SettingsRootDiff] = []

        let oldIDs = Set(old.map { $0.id })
        let newIDs = Set(new.map { $0.id })

        // 1) 削除（旧にあって新にない）
        for section in old where !newIDs.contains(section.id) {
            diffs.append(.removeSection(sectionID: section.id))
        }

        // 2) 追加（新にあって旧にない）
        for (newIdx, section) in new.enumerated() where !oldIDs.contains(section.id) {
            diffs.append(.insertSection(at: newIdx, section: section))
        }

        // 3) 移動（両方にあって位置が違う）
        // 削除済 Section / 追加済 Section を除いた両者で位置比較する。
        let remainingNew = new.enumerated().filter { oldIDs.contains($0.element.id) }
        for (newIdx, section) in remainingNew {
            // 旧の中での位置と新の中での位置が違うか
            if let oldIdx = old.firstIndex(where: { $0.id == section.id }), oldIdx != newIdx {
                diffs.append(.moveSection(from: oldIdx, to: newIdx))
            }
        }

        return diffs
    }

    // MARK: - Cell レベル突合
    //
    // ## 三層分離における Cell 内容比較の位置づけ（core/ADR-0010）
    //
    // 「表示状態同期の三層分離」では、構造同期（item 集合・順序の差分計算）と内容更新
    // （同一 id セルの部分更新 = reconfigure）を別レイヤとして扱う。
    //
    // - 構造同期の identity は `KsCellID`（= `KsCell.id` のみ）で判定し、内容を判定材料にしない。
    //   したがって `KsCellID(cell:)` は内容比較には一切関与しない。
    // - 内容変化の検出（`.replaceCell` を発行すべきかの判定）は、**本算出ロジックがここで**
    //   `AnyHashable(oldCell) != AnyHashable(cell)` という Cell 値全体の比較で行う。
    //   発行された `.replaceCell` は構造同期 snapshot を変えず、`applyReplaceCell` が
    //   `reconfigureItems` で同一セルを破棄せず再構成して反映する。
    //
    // この内容比較（`.replaceCell` 発行判定）は `KsCell` プロトコルが `Hashable` を要求して
    // いることに依存するため、具象 Cell 実装は **以下の規約** を満たすことを要求する：
    //
    // 1. Cell 値の `==` は **`id` を含むすべての保持フィールド** を対象とする
    //    （Swift の自動合成 `Hashable` は struct の全フィールドを対象にするためデフォルトで OK）。
    // 2. `id` のみ同じで内容が異なる Cell（例: title が変わった同 id Cell）に対して
    //    `==` は `false` を返すこと。これが守られない場合、本 Diff 算出は `replaceCell` を
    //    発行できず Cell の表示更新が走らない。
    //    （注: この `==` は **内容更新の発行判定** にのみ使う。構造同期の identity =
    //    `KsCellID` は別レイヤであり id のみで等価判定するため、内容変化で snapshot は変わらない。）
    // 3. `KsAnyView` を含むフィールド（任意 View 等）は `==` で「常に true」または「常に false」
    //    のどちらかに振る舞いを統一する。`KsSettingsViewCore.KsAnyView` は常に等価扱いで
    //    実装されているため、利用者は Cell に `KsAnyView` フィールドを保持する場合の差分検出
    //    は別途 `id` 変更 or `forceReload` 専用 modifier 等を検討する必要がある。
    // 4. `@Binding` / `MutableState` を保持する Cell は、`wrappedValue` を `Hashable` の対象に
    //    含めること（同一バインド先・同一値なら等価判定される）。
    //
    // 規約 1 / 2 を満たさない Cell 実装に対しては、本 Diff 算出は「内容変更されたが replaceCell が
    // 発行されない」未定義動作となる。本ライブラリが提供する具象 Cell 群は、すべて自動合成
    // `Hashable` で規約を満たす設計とする。

    private static func cellLevelDiffs(
        sectionID: UUID,
        old: [any KsCell],
        new: [any KsCell]
    ) -> [SettingsRootDiff] {
        var diffs: [SettingsRootDiff] = []

        let oldIDs = Set(old.map { $0.id })
        let newIDs = Set(new.map { $0.id })

        // 1) 削除（旧にあって新にない）
        for cell in old where !newIDs.contains(cell.id) {
            diffs.append(.removeCell(cellID: KsCellID(cell: cell)))
        }

        // 2) 追加（新にあって旧にない）
        for (newIdx, cell) in new.enumerated() where !oldIDs.contains(cell.id) {
            diffs.append(.insertCell(sectionID: sectionID, at: newIdx, cell: cell))
        }

        // 3) 移動 / 置換（両方に id 存在）
        for (newIdx, cell) in new.enumerated() {
            guard oldIDs.contains(cell.id) else { continue }
            guard let oldIdx = old.firstIndex(where: { $0.id == cell.id }) else { continue }
            let oldCell = old[oldIdx]
            // 内容比較：上記規約に基づく `AnyHashable` 経由の Cell 値全体比較。
            // `id` は両者一致が前提（`oldIDs.contains(cell.id)` で絞り込んでいるため）なので、
            // ここでの差分は「id 以外のフィールドの変化」を意味する。
            // 内容変化時は `.replaceCell`（= 同一 id の内容更新 / reconfigure 経路）を発行する。
            // この `.replaceCell` は構造同期（item 集合・順序）を変えず、`applyReplaceCell` が
            // `reconfigureItems` で同一セルを破棄せず再構成して反映する（セル破棄・ちらつきなし）。
            let contentsChanged = AnyHashable(oldCell) != AnyHashable(cell)
            if oldIdx != newIdx {
                diffs.append(.moveCell(cellID: KsCellID(cell: oldCell), to: newIdx))
                if contentsChanged {
                    // 移動（構造同期）とは別に内容更新（reconfigure 経路）も発行
                    diffs.append(.replaceCell(cellID: KsCellID(cell: oldCell), new: cell))
                }
            } else if contentsChanged {
                diffs.append(.replaceCell(cellID: KsCellID(cell: oldCell), new: cell))
            }
        }

        return diffs
    }

    /// Diff 算出器に渡す resolved 済み DSL ツリー。
    public struct ResolvedTree: Sendable {
        public let sections: [KsSettingsViewCore.Section]
        public let rootHeader: RootAccessory?
        public let rootFooter: RootAccessory?
        public let theme: Theme

        public init(
            sections: [KsSettingsViewCore.Section],
            rootHeader: RootAccessory? = nil,
            rootFooter: RootAccessory? = nil,
            theme: Theme = Theme()
        ) {
            self.sections = sections
            self.rootHeader = rootHeader
            self.rootFooter = rootFooter
            self.theme = theme
        }
    }
}
#endif
