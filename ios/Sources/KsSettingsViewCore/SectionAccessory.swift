// SectionAccessory.swift
// KsSettingsViewCore
//
// Section の header / footer 位置に配置可能な内容を表す sum type。
// 文字列ヘッダ／フッタの簡潔な表現と、任意 View（`KsAnyView` ラップ）の柔軟な表現を
// 一つの型で扱う。ヘッダ／フッタは表示専用の装飾領域であり、Cell（タップ・選択・編集する行）の
// 概念は持ち込まない。任意の見た目が要る場合は `text` ではなく `view` に `KsAnyView` を包んで渡す。

import Foundation

/// Section のヘッダ／フッタ位置に配置可能な内容を表す sum type。
///
/// - `text(String)`: 文字列ヘッダ／フッタ（簡潔表現）。
///   `UICollectionLayoutListConfiguration` の supplementary header API（文字列）に最短で対応する。
/// - `view(KsAnyView)`: 任意 View ヘッダ／フッタ（`KsAnyView` ラップ）。
///   `UIHostingConfiguration` 等で任意 SwiftUI View / UIView を描画する根拠となる。
///
/// `Hashable` は手動実装で、`view` ケースの中身（`KsAnyView`）は hash 計算に含めず、
/// ケース判別のみで判定する。
public enum SectionAccessory: Hashable, Sendable {
    /// 文字列ヘッダ／フッタ。
    case text(String)
    /// 任意 View ヘッダ／フッタ（`KsAnyView` ラップ）。
    /// `KsAnyView` は `Hashable` を持たないため、本ケースの等価性は「ケース一致のみ」で判定する。
    case view(KsAnyView)

    // MARK: - Hashable 手動実装

    /// `KsAnyView` の中身は判定対象外とし、ケース判別のみで等価判定する。
    public static func == (lhs: SectionAccessory, rhs: SectionAccessory) -> Bool {
        switch (lhs, rhs) {
        case let (.text(a), .text(b)):
            return a == b
        case (.view, .view):
            // `KsAnyView` は等価性に参加しない。ケース一致のみで等価。
            return true
        default:
            return false
        }
    }

    /// `KsAnyView` の中身は hash 計算に含めず、ケース判別の discriminator のみを混ぜる。
    public func hash(into hasher: inout Hasher) {
        switch self {
        case .text(let value):
            // discriminator: 0 + 文字列 hash
            hasher.combine(0)
            hasher.combine(value)
        case .view:
            // discriminator: 1 のみ（`KsAnyView` 中身は hash に含めない）
            hasher.combine(1)
        }
    }
}
