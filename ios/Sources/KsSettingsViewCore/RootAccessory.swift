// RootAccessory.swift
// KsSettingsViewCore
//
// `SettingsRoot` のヘッダ／フッタ位置に配置可能な内容を表す sum type。
// Root と Section で装飾の責務が異なるため（core/ADR-0005）、`SectionAccessory` とは別型とし、
// Root 固有の挙動分岐（ピン留め・テーマ継承等）を独立に持てるようにする。

import Foundation

/// `SettingsRoot` のヘッダ／フッタ位置に配置可能な内容を表す sum type。
///
/// - `text(String)`: 文字列ヘッダ／フッタ（簡潔表現）
/// - `view(KsAnyView)`: 任意 View ヘッダ／フッタ（`KsAnyView` ラップ）
///
/// `Hashable` は手動実装で、`view` ケースの中身（`KsAnyView`）は hash 計算に含めず、
/// ケース判別のみで判定する。
public enum RootAccessory: Hashable, Sendable {
    /// 文字列ヘッダ／フッタ。
    case text(String)
    /// 任意 View ヘッダ／フッタ（`KsAnyView` ラップ）。
    /// `KsAnyView` は `Hashable` を持たないため、本ケースの等価性は「ケース一致のみ」で判定する。
    case view(KsAnyView)

    // MARK: - Hashable 手動実装

    /// `KsAnyView` の中身は判定対象外とし、ケース判別のみで等価判定する。
    public static func == (lhs: RootAccessory, rhs: RootAccessory) -> Bool {
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
