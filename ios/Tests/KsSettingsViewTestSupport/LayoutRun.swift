// LayoutRun.swift
// KsSettingsViewTestSupport
//
// 時間待機を伴わずにレイアウトだけを同期実行するヘルパ。

#if canImport(UIKit)
import UIKit

/// 対象 view のレイアウトを同期的に確定させる。
///
/// frame や Auto Layout の解決結果のように、レイアウト実行だけで確定する状態を検証するときに
/// 使う。時間待機は伴わないため、非同期に反映される状態を待つ用途には使わない
/// (その場合は `awaitCondition` を使う)。
@MainActor
public func layoutNow(_ view: UIView) {
    view.setNeedsLayout()
    view.layoutIfNeeded()
}
#endif
