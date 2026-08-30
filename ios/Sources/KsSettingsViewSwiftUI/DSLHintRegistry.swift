// DSLHintRegistry.swift
// KsSettingsViewSwiftUI
//
// DSL（`KsSettingsView { ... }` / `Section { ... }`）内の Section / Cell に対する
// **ID 採番ヒント**（`.cellID(_:)` / `.sectionID(_:)` / `ForEach` 配下の item.id）を
// 一時保管するプロセスローカル レジストリ。
//
// なぜ必要か:
//   - `KsSettingsViewCore.Section` / `KsCell` には modifier 履歴を載せるフィールドが存在せず、
//     これらの型を本提案で変更することは禁止されている（proposal の影響範囲外）。
//   - DSL builder（`@SettingsRootBuilder` / `@SectionBuilder`）の戻り型は
//     `[KsSettingsViewCore.Section]` / `[any KsCell]` であり、ヒントを乗せた wrapper 型を
//     戻すと既存 `extension SettingsRoot { init(... sections: () -> [Section]) }` 等との
//     互換性が崩れる。
//   - そこで、Section / Cell の **インスタンス ID（生成時 UUID）** をキーにヒントを記録する
//     サイドチャンネル方式を採用する。`DSLDiffCalculator` 等の後段ロジックは
//     インスタンス ID から hint を逆引きする。
//
// ライフサイクル:
//   - レジストリは `body` 再評価の頭でクリアする（`KsSettingsView` 側で reset を呼ぶ）。
//   - 各 body 評価中に積まれたヒントは、その body 評価の終了時に DSL ツリーへ取り込まれ、
//     その後消費される。プロセスローカルなため、複数 `KsSettingsView` が同時に評価されても
//     インスタンス ID（UUID）が衝突しない限り正しく動作する（UUID 衝突確率は無視できる）。

import Foundation

/// プロセスローカル DSL ヒントレジストリ。
///
/// シングルトンとして提供し、`ForEach` 関数や `.cellID(_:)` / `.sectionID(_:)` modifier から
/// `section.id` / `cell.id` をキーにヒントを記録する。`DSLDiffCalculator` 等が後段で逆引きする。
internal final class DSLHintRegistry: @unchecked Sendable {

    /// シングルトンインスタンス。
    static let shared = DSLHintRegistry()

    /// Section インスタンス ID → ID 採番ヒント
    private var sectionHints: [UUID: DSLIdentityHint] = [:]
    /// Cell インスタンス ID → ID 採番ヒント
    private var cellHints: [UUID: DSLIdentityHint] = [:]

    /// スレッド安全のためのロック（軽量な NSLock）
    private let lock = NSLock()

    private init() {}

    /// Section に対する ID 採番ヒントを記録する。
    /// 既存ヒントが存在する場合、`.explicit > .forEach` の優先順位で上書き判定する。
    func recordSectionHint(sectionInstanceID: UUID, hint: DSLIdentityHint) {
        lock.lock()
        defer { lock.unlock() }
        let existing = sectionHints[sectionInstanceID]
        if shouldOverride(existing: existing, new: hint) {
            sectionHints[sectionInstanceID] = hint
        }
    }

    /// Cell に対する ID 採番ヒントを記録する。
    func recordCellHint(cellInstanceID: UUID, hint: DSLIdentityHint) {
        lock.lock()
        defer { lock.unlock() }
        let existing = cellHints[cellInstanceID]
        if shouldOverride(existing: existing, new: hint) {
            cellHints[cellInstanceID] = hint
        }
    }

    /// Section の登録ヒントを取り出す。
    func sectionHint(for sectionInstanceID: UUID) -> DSLIdentityHint? {
        lock.lock()
        defer { lock.unlock() }
        return sectionHints[sectionInstanceID]
    }

    /// Cell の登録ヒントを取り出す。
    func cellHint(for cellInstanceID: UUID) -> DSLIdentityHint? {
        lock.lock()
        defer { lock.unlock() }
        return cellHints[cellInstanceID]
    }

    /// レジストリをクリアする（body 再評価頭などで呼ぶ）。
    func reset() {
        lock.lock()
        defer { lock.unlock() }
        sectionHints.removeAll(keepingCapacity: true)
        cellHints.removeAll(keepingCapacity: true)
    }

    /// 優先順位判定：明示指定（`.explicit`）は常に勝つ。同種の場合は新しい方を採用。
    private func shouldOverride(existing: DSLIdentityHint?, new: DSLIdentityHint) -> Bool {
        guard let existing = existing else { return true }
        // 既存が explicit、新規が非 explicit なら上書きしない（明示指定を維持）。
        switch (existing, new) {
        case (.explicit, .forEach):
            return false
        case (.explicit, .explicit):
            return true // 後勝ち
        default:
            return true
        }
    }
}
