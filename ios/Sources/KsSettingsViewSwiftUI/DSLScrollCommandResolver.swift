// DSLScrollCommandResolver.swift
// KsSettingsViewSwiftUI
//
// DSL 方式の `KsSettingsView` で、スクロール命令の ID (明示 ID / `ForEach` の key) を
// 宣言ツリーの最終 ID へ引き直して Host へ渡す受け口。

#if canImport(UIKit)
import Foundation
import os
import KsSettingsViewCore
import KsSettingsViewUI

/// DSL 方式の命令ハンドルの受け口。ハンドルは Host ではなくこの受け口に接続する。
///
/// 利用者が知っているのは自分で書いた明示 ID (`.cellID(_:)` / `.sectionID(_:)`) か `ForEach` の key
/// だけなので、それを最終 ID (`DSLIdentityUUID.uuid(from:)` で決まる UUID) へ引き直してから Host の
/// 待ち行列へ渡す。引き直しは、命令を受けた処理の中で起きた状態変更による宣言の更新が内部 Store へ
/// 届いてから行う。SwiftUI は状態変更を現在の実行ループの終わりで反映するため、`main.async` で
/// 1 回遅らせた時点の宣言ツリーで解決する。宣言の更新を伴わない命令は、遅らせた時点の (変わって
/// いない) 宣言ツリーで解決する。
@MainActor
internal final class DSLScrollCommandResolver: KsScrollCommandReceiver {

    /// 引き直した命令を渡す Host。Host の寿命は SwiftUI が管理するため弱参照で持つ。
    weak var host: KsSettingsViewController?

    /// 接続中のハンドル。Host が UIKit の接続口でハンドルを強参照するのと同じく、ここでも強参照で持つ。
    private(set) var connectedController: KsScrollController?

    /// 宣言ツリーの世代。宣言の更新が内部 Store へ流れるたびに進む。
    private(set) var treeGeneration = 0

    /// 最新の宣言ツリーの Section 列 (最終 ID で解決済み)。
    private var sections: [KsSettingsViewCore.Section]

    /// 受け取って、まだ引き直していない命令と、受けた時点の宣言ツリーの世代。
    private var pending: [(command: KsScrollCommand, receivedGeneration: Int)] = []

    /// 引き直しの `main.async` を予約済みか。
    private var isResolutionScheduled = false

    /// 直近に引き直した命令が受けた時点と引き直した時点の世代 (テストでの観測用)。
    private(set) var lastResolvedGenerations: (received: Int, resolved: Int)?

    private static let logger = Logger(subsystem: "jp.kamusoft.kssettingsview", category: "KsScrollControl")

    init(sections: [KsSettingsViewCore.Section]) {
        self.sections = sections
    }

    /// 命令ハンドルをこの受け口へ接続する。別のハンドルへ差し替えるときは、前のハンドルを切り離す。
    func connect(_ controller: KsScrollController?) {
        guard connectedController !== controller else { return }
        connectedController?.detach(self)
        connectedController = controller
        // 外した接続から届いていた未実行の命令は、まだ引き直していないものも、引き直して Host の
        // 待ち行列へ渡したものも捨てる (Host の接続口と同じ扱い。控えた位置の復元は Host が残す)。
        pending.removeAll()
        host?.dropPendingScrollCommands()
        controller?.attach(self)
    }

    /// 宣言の更新を内部 Store へ流したことを知らせ、引き直しに使う宣言ツリーを差し替える。
    func treeDidUpdate(_ sections: [KsSettingsViewCore.Section]) {
        self.sections = sections
        treeGeneration += 1
    }

    func receive(_ command: KsScrollCommand) {
        pending.append((command, treeGeneration))
        guard !isResolutionScheduled else { return }
        isResolutionScheduled = true
        DispatchQueue.main.async { [weak self] in
            self?.resolvePending()
        }
    }

    /// 保留中の命令を現在の宣言ツリーで引き直し、呼ばれた順に Host へ渡す。
    private func resolvePending() {
        isResolutionScheduled = false
        let commands = pending
        pending.removeAll()
        for entry in commands {
            lastResolvedGenerations = (entry.receivedGeneration, treeGeneration)
            guard let resolved = resolve(entry.command) else { continue }
            host?.receive(resolved)
        }
    }

    /// 命令の ID を最終 ID へ引き直す。明示 ID と `ForEach` の key の両方の形で計算し、宣言ツリーに
    /// 在るほうを採る。両方あれば明示 ID を採る (core/ADR-0036)。どちらも無ければ `nil`。
    private func resolve(_ command: KsScrollCommand) -> KsScrollCommand? {
        switch command {
        case let .cell(id, position, animated):
            let cellIDs = Set(sections.flatMap { $0.cells.map(\.id) })
            guard let finalID = finalID(for: id, existingIn: cellIDs) else {
                warnUnresolved(kind: "cell", id: id)
                return nil
            }
            return .cell(id: AnyHashable(KsCellID(id: finalID)), position: position, animated: animated)
        case let .section(id, position, animated):
            let sectionIDs = Set(sections.map(\.id))
            guard let finalID = finalID(for: id, existingIn: sectionIDs) else {
                warnUnresolved(kind: "section", id: id)
                return nil
            }
            return .section(id: AnyHashable(finalID), position: position, animated: animated)
        case .start, .end:
            return command
        }
    }

    private func finalID(for id: AnyHashable, existingIn ids: Set<UUID>) -> UUID? {
        let explicit = DSLIdentityUUID.uuid(from: .explicit(id))
        if ids.contains(explicit) { return explicit }
        let forEachKey = DSLIdentityUUID.uuid(from: .forEach(id))
        if ids.contains(forEachKey) { return forEachKey }
        return nil
    }

    private func warnUnresolved(kind: String, id: AnyHashable) {
        #if DEBUG
        let description = String(describing: id.base)
        Self.logger.warning(
            "KsSettingsView: ignored a scroll command to an unknown \(kind, privacy: .public) id \(description, privacy: .public)"
        )
        #endif
    }
}
#endif
