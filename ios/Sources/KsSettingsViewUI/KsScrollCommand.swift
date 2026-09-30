// KsScrollCommand.swift
// KsSettingsViewUI
//
// 命令ハンドルから受け口へ渡すスクロール命令。

import Foundation

/// 命令ハンドル (`KsScrollController`) から受け口へ渡すスクロール命令。
///
/// ID は利用者が渡した値をそのまま運ぶ。何の ID として解釈するか (Store の Cell / Section の ID か、
/// 宣言 UI の明示 ID / `ForEach` の key か) は受け口が決める。
package enum KsScrollCommand {
    /// Cell の行へ送る。
    case cell(id: AnyHashable, position: KsScrollPosition, animated: Bool)
    /// Section の範囲 (見出し・Cell・Footer) へ送る。
    case section(id: AnyHashable, position: KsScrollPosition, animated: Bool)
    /// 内容の最上端 (Root Header を含む) へ送る。
    case start(animated: Bool)
    /// 内容の最下端 (Root Footer を含む) へ送る。
    case end(animated: Bool)
}
