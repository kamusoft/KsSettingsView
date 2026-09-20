// CachedDateFormatter.swift
// KsSettingsViewUI
//
// `DateFormatter` をフォーマット文字列と Locale のキーで再利用する内部ユーティリティ。
// `TimePickerCell` / `DatePickerCell` の `effectiveValueText()` で毎回 `DateFormatter()` を
// 新規生成して `dateFormat` を設定する実装は、ICU バインディング初期化を含むため微小ながら
// コストが累積する。同一の `dateFormat` と Locale であれば生成済みの formatter を再利用する。
//
// スレッドセーフ性:
//   - キャッシュへの読み書きは `NSLock` で直列化する。
//   - 返却された `DateFormatter` の `string(from:)` は read-only として呼び、`dateFormat` の
//     書き換えは行わない（書き換えはキャッシュ生成時の 1 度のみ）。`DateFormatter` は
//     iOS 7+ で設定済みインスタンスの read-only 利用は thread-safe であることが
//     ドキュメント化されている。

#if canImport(UIKit)
import Foundation

/// `dateFormat` と Locale をキーとした `DateFormatter` のグローバルキャッシュ。
internal enum CachedDateFormatter {
    private struct CacheKey: Hashable {
        let format: String
        let localeIdentifier: String
    }

    private static let lock = NSLock()
    nonisolated(unsafe) private static var cache: [CacheKey: DateFormatter] = [:]

    /// 指定 `format` 用の `DateFormatter` を返す（無ければ生成してキャッシュする）。
    /// 返却された `DateFormatter` の `dateFormat` を書き換えてはならない（read-only として扱う）。
    static func formatter(for format: String, locale: Locale) -> DateFormatter {
        let key = CacheKey(format: format, localeIdentifier: locale.identifier)
        lock.lock()
        defer { lock.unlock() }
        if let cached = cache[key] {
            return cached
        }
        let f = DateFormatter()
        f.locale = locale
        f.dateFormat = format
        cache[key] = f
        return f
    }

    /// 指定 `format` を適用した文字列を返す（キャッシュされた `DateFormatter` を read-only 利用）。
    static func string(from date: Date, format: String, locale: Locale = UserInterfaceLocale.current) -> String {
        return formatter(for: format, locale: locale).string(from: date)
    }
}
#endif
