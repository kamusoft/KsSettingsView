// KsScrollAnchor.swift
// KsSettingsViewUI
//
// 表示位置を控えて別の Host で戻すための、中身を公開しない値。

import Foundation
import CoreGraphics

/// 設定画面の表示位置を控えた値です。
///
/// `KsSettingsViewController.captureScrollAnchor()` で控え、同じ内容を表示する画面の
/// `restoreScrollAnchor(_:)` へ渡すと、控えたときに表示範囲の上端にかかっていた要素が同じずれで
/// 上端にかかる位置へ戻ります。要素の ID で控えるため、控えた後に項目が増減しても同じ要素の場所へ
/// 戻ります。中身 (座標や要素) は公開しません。
public struct KsScrollAnchor: Sendable, Equatable {
    /// 表示範囲の上端にかかっていた要素。
    internal enum Element: Sendable, Equatable {
        /// Cell の行。値は Cell の ID (`KsCellID.id`)。
        case cell(UUID)
        case sectionHeader(UUID)
        case sectionFooter(UUID)
        case rootHeader
        case rootFooter
    }

    /// 控えた要素。
    internal let element: Element

    /// 表示範囲の上端から見た、要素の上端までのずれ (要素の上端が表示範囲の上端より上にあれば正)。
    internal let offsetFromTop: CGFloat

    internal init(element: Element, offsetFromTop: CGFloat) {
        self.element = element
        self.offsetFromTop = offsetFromTop
    }
}
