// KsScrollPosition.swift
// KsSettingsViewUI
//
// スクロール命令で対象を表示範囲のどこへ合わせるかを表す位置。

import Foundation

/// スクロール命令で、対象を表示範囲のどこへ合わせるかを表す位置です。
///
/// 表示範囲は、安全領域と一覧の内容の余白を除いた、実際に見えている領域です。
public enum KsScrollPosition: Sendable {
    /// 対象の上端を表示範囲の上端へ合わせます。
    case start
    /// 対象の中央を表示範囲の中央へ合わせます。
    case center
    /// 対象の下端を表示範囲の下端へ合わせます。
    case end
}
