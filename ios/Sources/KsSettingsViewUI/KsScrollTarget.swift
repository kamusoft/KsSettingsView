// KsScrollTarget.swift
// KsSettingsViewUI
//
// スクロールの行き先。位置は実行のたびに最新のレイアウトから求め直す。

#if canImport(UIKit)
import Foundation
import KsSettingsViewCore

/// スクロールの行き先。
///
/// 行き先を座標ではなく要素で持つのは、推定高さで求めた位置を、スクロール中の各フレームやレイアウトの確定後に
/// 同じ要素について求め直して補正するため。
internal enum KsScrollTarget {
    /// Cell の行。
    case cell(KsCellID)
    /// Section の範囲 (表示されている見出し・Cell・Footer をつないだもの)。
    case section(UUID)
    /// 内容の最上端 (Root Header を含む)。
    case contentStart
    /// 内容の最下端 (Root Footer を含む)。
    case contentEnd
    /// 控えた要素を、控えたずれで表示範囲の上端にかける位置。
    case anchor(KsScrollAnchor)

    /// 控えた位置の戻しの行き先か。
    var isAnchor: Bool {
        if case .anchor = self { return true }
        return false
    }
}
#endif
