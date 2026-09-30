// KsScrollQueueEntry.swift
// KsSettingsViewUI
//
// Host のスクロール命令の待ち行列に積む要素。

#if canImport(UIKit)
import Foundation

/// Host (`KsSettingsViewController`) のスクロール命令の待ち行列に積む要素。
///
/// ハンドルからの命令と、控えた位置の復元を同じ待ち行列で扱う。復元も命令と同じくデータの反映と
/// レイアウトの後に実行され、後から積まれた命令より先に処理される。
internal enum KsScrollQueueEntry {
    /// ハンドルから届いた命令。
    case command(KsScrollCommand)
    /// `restoreScrollAnchor(_:)` で要求された位置の復元。
    case restore(KsScrollAnchor)
}
#endif
