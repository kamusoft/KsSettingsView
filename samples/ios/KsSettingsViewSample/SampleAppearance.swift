// SampleAppearance.swift
// KsSettingsViewSample
//
// ルートメニューで選ぶアプリ全体の外観の一元定義。
//
// 見出しと項目の文言、選択中の印の読み上げ文言は全 platform で一致させる（cross/ADR-0016）。
// 文言を画面側に手書きすると表記ゆれが再発するため、定義はここ1箇所に閉じる。
//
// 対応する定義:
// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleAppearance.kt
// samples/maui/KsSettingsView.Sample.Maui/SampleAppearance.cs

import SwiftUI

/// ルートメニューで選ぶアプリ全体の外観。
enum SampleAppearance: String, CaseIterable, Identifiable {
    /// 端末の外観に従う。
    case system
    /// 端末の設定に関わらずライト。
    case light
    /// 端末の設定に関わらずダーク。
    case dark

    var id: String { rawValue }

    /// ルートメニューに表示する項目名。
    var title: String {
        switch self {
        case .system: "システム"
        case .light: "ライト"
        case .dark: "ダーク"
        }
    }

    /// アプリ全体へ適用する配色。`nil` は上書きなし（端末の外観がそのまま効く）。
    var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }

    /// ルートメニューで外観の項目群につける見出し。
    static let sectionTitle: String = "外観"

    /// 選択中の項目に付く印の読み上げ文言。行の文言と合わせて読まれる。
    static let selectedAccessibilityLabel: String = "選択中"

    /// 初回起動時の選択。
    static let initial: SampleAppearance = .system

    /// 選択の永続化に使う `UserDefaults` のキー。
    static let storageKey: String = "appearance"
}
