// UserInterfaceLocale.swift
// KsSettingsViewUI
//
// 日付・時刻の自動表示と選択面で共有する、利用者が OS で選んだ Locale の解決。

#if canImport(UIKit)
import Foundation

/// OS のアプリ単位言語を優先し、端末の地域と組み合わせた表示用 Locale を返す。
internal enum UserInterfaceLocale {
    /// 現在の表示用 Locale。
    ///
    /// `Locale.preferredLanguages` は OS 管理のアプリ単位言語があればその値を先頭に返す。
    /// 言語側に地域が無い場合は、端末の現在 Locale の地域を補う。Bundle の対応 localization は
    /// 参照しないため、ライブラリの表示言語はアプリが提供する翻訳資源だけには制限されない。
    internal static var current: Locale {
        resolve(preferredLanguages: Locale.preferredLanguages, regionalLocale: .current)
    }

    /// テスト可能な Locale 解決本体。
    internal static func resolve(preferredLanguages: [String], regionalLocale: Locale) -> Locale {
        guard let preferredIdentifier = preferredLanguages.first, !preferredIdentifier.isEmpty else {
            return regionalLocale
        }
        let preferred = Locale(identifier: preferredIdentifier)
        var components = Locale.Components(locale: preferred)
        components.region = regionalLocale.region
        return Locale(components: components)
    }
}
#endif
