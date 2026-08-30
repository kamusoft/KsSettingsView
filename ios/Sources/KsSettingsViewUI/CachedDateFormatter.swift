// CachedDateFormatter.swift
// KsSettingsViewUI
//
// `DateFormatter` をフォーマット文字列キーで再利用するための内部ユーティリティ。
// `TimePickerCell` / `DatePickerCell` の `effectiveValueText()` で毎回 `DateFormatter()` を
// 新規生成して `dateFormat` を設定する実装は、ICU バインディング初期化を含むため微小ながら
// コストが累積する。同一の `dateFormat` であれば 1 度生成した `DateFormatter` を再利用する。
//
// スレッドセーフ性:
//   - キャッシュへの読み書きは `NSLock` で直列化する。
//   - 返却された `DateFormatter` の `string(from:)` は read-only として呼び、`dateFormat` の
//     書き換えは行わない（書き換えはキャッシュ生成時の 1 度のみ）。`DateFormatter` は
//     iOS 7+ で設定済みインスタンスの read-only 利用は thread-safe であることが
//     ドキュメント化されている。

#if canImport(UIKit)
import Foundation

/// `dateFormat` 文字列をキーとした `DateFormatter` のグローバルキャッシュ。
internal enum CachedDateFormatter {
    private static let lock = NSLock()
    nonisolated(unsafe) private static var cache: [String: DateFormatter] = [:]

    /// 指定 `format` 用の `DateFormatter` を返す（無ければ生成してキャッシュする）。
    /// 返却された `DateFormatter` の `dateFormat` を書き換えてはならない（read-only として扱う）。
    static func formatter(for format: String) -> DateFormatter {
        lock.lock()
        defer { lock.unlock() }
        if let cached = cache[format] {
            return cached
        }
        let f = DateFormatter()
        f.dateFormat = format
        cache[format] = f
        return f
    }

    /// 指定 `format` を適用した文字列を返す（キャッシュされた `DateFormatter` を read-only 利用）。
    static func string(from date: Date, format: String) -> String {
        return formatter(for: format).string(from: date)
    }
}
#endif
