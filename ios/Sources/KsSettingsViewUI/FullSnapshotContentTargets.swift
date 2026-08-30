// FullSnapshotContentTargets.swift
// KsSettingsViewUI
//
// full snapshot 適用時に「同一 ID のまま内容が変わった表示中 Cell」を洗い出す純粋ロジック。
//
// snapshot の item 識別子は `KsCellID`（UUID のみ）であり内容を含まないため、同一 ID の Cell の
// 内容変化は snapshot の差分に現れない。full 経路はこの型が返す対象へ内容再適用を重ねることで、
// 部分更新経路（`replaceCell` / `replaceCells`）と同じ内容反映の出口を持つ。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore

/// full snapshot 適用時に内容を再適用する Cell の集合。
///
/// `reconfigure` は同一 Native cell を破棄せず再構成できる対象、`reload` は Native cell の
/// 交換が必要な対象を表す。いずれも旧・新 visible projection の**双方に存在する** Cell に限る。
/// 新規挿入・削除・hidden の Cell は構造反映の通常 bind で最新内容になるため対象に含めない。
internal struct FullSnapshotContentTargets: Equatable {

    /// 内容が変わり、Native cell を維持したまま再構成できる Cell の ID 群（新 projection の順序）。
    internal let reconfigure: [KsCellID]

    /// 同一 ID のまま具象型が変わり、Native cell の交換が必要な Cell の ID 群（新 projection の順序）。
    internal let reload: [KsCellID]

    /// 再適用の対象が 1 件も無いか。
    internal var isEmpty: Bool {
        return reconfigure.isEmpty && reload.isEmpty
    }

    internal init(reconfigure: [KsCellID], reload: [KsCellID]) {
        self.reconfigure = reconfigure
        self.reload = reload
    }

    /// 旧・新の visible projection を突き合わせて内容再適用の対象を求める。
    ///
    /// 判定は新 projection の並び順で行い、同じ結果に対して常に同じ順序を返す。
    ///
    /// - Parameters:
    ///   - oldVisible: 適用前の visible projection
    ///   - newVisible: 適用後の visible projection
    ///   - reloadSectionIDs: supplementary の再構成を行う Section の ID 群。これらの Section は
    ///     Section 全体が作り直されて Cell も内容ごと再構成されるため、`reconfigure` の対象から除く
    /// - Returns: 内容再適用の対象集合。該当が無ければ空
    internal static func compute(
        oldVisible: [KsSettingsViewCore.Section],
        newVisible: [KsSettingsViewCore.Section],
        reloadSectionIDs: Set<UUID>
    ) -> FullSnapshotContentTargets {
        // 旧 projection に載っていた Cell だけを引ける表を作る（hidden な Cell はここに現れない）。
        var oldCellsByID: [UUID: any KsCell] = [:]
        for section in oldVisible {
            for cell in section.cells {
                oldCellsByID[cell.id] = cell
            }
        }

        var reconfigure: [KsCellID] = []
        var reload: [KsCellID] = []
        for section in newVisible {
            let sectionIsReloaded = reloadSectionIDs.contains(section.id)
            for cell in section.cells {
                guard let oldCell = oldCellsByID[cell.id] else { continue }
                if type(of: oldCell) != type(of: cell) {
                    // 具象型が変われば Renderer も変わるため、同一 Native cell の再構成では反映できない。
                    // 判定を Section 再構成の除外より前に置くのは意図的で、Section 再構成側でも
                    // 内容は最新になるため実質冗長だが、Native cell を交換する判断を Section の
                    // 再構成の有無に依存させない。
                    reload.append(KsCellID(cell: cell))
                    continue
                }
                guard !sectionIsReloaded else { continue }
                // 内容比較は Cell 値全体の等価判定で行う（構造同期の identity とは別レイヤ）。
                if AnyHashable(oldCell) != AnyHashable(cell) {
                    reconfigure.append(KsCellID(cell: cell))
                }
            }
        }
        return FullSnapshotContentTargets(reconfigure: reconfigure, reload: reload)
    }
}
#endif
