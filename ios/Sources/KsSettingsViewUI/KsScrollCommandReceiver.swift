// KsScrollCommandReceiver.swift
// KsSettingsViewUI
//
// 命令ハンドルが命令を届ける受け口。

import Foundation

/// 命令ハンドル (`KsScrollController`) が命令を届ける受け口。
///
/// UIKit Host (`KsSettingsViewController`) と、宣言 UI の ID を引き直す SwiftUI 側の受け口が準拠する。
/// ハンドルは受け口を弱参照で持つため、受け口の寿命はハンドルに延ばされない。
@MainActor
package protocol KsScrollCommandReceiver: AnyObject {
    /// 命令を受け取る。受けた処理の中では実行せず、受け口の待ち行列に積む。
    func receive(_ command: KsScrollCommand)
}
