// KsScrollController.swift
// KsSettingsViewUI
//
// 設定画面へスクロール命令を送るハンドル。

import Foundation
import os

/// 設定画面へスクロール命令を送るハンドルです。
///
/// 画面のライフサイクルに依存しない値として作り、表示側へ接続して使います。UIKit では
/// `KsSettingsViewController.scrollController` へ代入し、SwiftUI では `KsSettingsView` の
/// `.scrollController(_:)` 修飾子で渡します。
///
/// - 命令は、同じ処理の中で行ったデータの変更が表示に反映された後に実行されます。
///   項目を追加した直後に `scrollToEnd()` を呼べば、追加した項目を含む末尾へ届きます。
/// - 続けて出した命令は呼んだ順に処理され、最終位置は最後の命令のものになります。
/// - どこにも接続していないハンドルへの命令は何もしません。
/// - 1 つのハンドルが命令を届ける先は、最後に接続した画面だけです。
/// - ハンドルは接続先の画面を保持しません。画面が破棄されると未接続に戻ります。
@MainActor
public final class KsScrollController: KsScrollControlling {
    /// 命令を届ける受け口。受け口の寿命を延ばさないよう弱参照で持つ (core/ADR-0037)。
    private weak var receiver: (any KsScrollCommandReceiver)?

    private static let logger = Logger(subsystem: "jp.kamusoft.kssettingsview", category: "KsScrollController")

    /// 未接続のハンドルを作ります。
    public init() {}

    public func scrollTo(id: some Hashable, position: KsScrollPosition, animated: Bool) {
        receiver?.receive(.cell(id: AnyHashable(id), position: position, animated: animated))
    }

    public func scrollToSection(id: some Hashable, position: KsScrollPosition, animated: Bool) {
        receiver?.receive(.section(id: AnyHashable(id), position: position, animated: animated))
    }

    public func scrollToStart(animated: Bool) {
        receiver?.receive(.start(animated: animated))
    }

    public func scrollToEnd(animated: Bool) {
        receiver?.receive(.end(animated: animated))
    }

    /// 受け口へ接続する。既に別の受け口へ接続していれば置き換え、debug ビルドで警告ログを出す。
    package func attach(_ receiver: any KsScrollCommandReceiver) {
        if let current = self.receiver, current !== receiver {
            #if DEBUG
            Self.logger.warning(
                "KsScrollController: the controller was attached to another settings view; the last attachment is used"
            )
            #endif
        }
        self.receiver = receiver
    }

    /// 受け口から切り離す。現在の受け口と同一のときだけ外す (別の受け口へ移った後の切断で、
    /// 移った先との接続を壊さないため)。
    package func detach(_ receiver: any KsScrollCommandReceiver) {
        guard self.receiver === receiver else { return }
        self.receiver = nil
    }

    /// 現在の受け口 (テストでの接続状態の確認用)。
    internal var currentReceiver: (any KsScrollCommandReceiver)? {
        receiver
    }
}
