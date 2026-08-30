// HourCycleLocale.swift
// KsSettingsViewUI
//
// 時刻 picker の時制（12/24時間制）を端末設定に依らず決めるための Locale 組み立て。

#if canImport(UIKit)
import Foundation

/// hour cycle だけを差し替えた `Locale` を組み立てる名前空間。
internal enum HourCycleLocale {

    /// `base` の言語・地域を保ったまま hour cycle だけを差し替えた `Locale` を返す。
    ///
    /// `UIDatePicker` の時制は `locale` の hour cycle で決まるため、時制を固定するには
    /// Locale の差し替えが必要になる。ただし `en_GB` のような固定 Locale への置き換えは
    /// 午前/午後の表記の言語まで英語に変えてしまう。ここで差し替えるのは hour cycle のみで、
    /// 表記の言語・地域は `base`（既定は端末の Locale）由来のまま保たれる。
    ///
    /// - Parameters:
    ///   - is24Hour: `true` で 24時間制（時 0–23）、`false` で 12時間制（時 1–12 と午前/午後）
    ///   - base: 言語・地域の由来となる Locale（既定は端末の `Locale.current`）
    internal static func forcing(is24Hour: Bool, base: Locale = .current) -> Locale {
        if base == .current {
            return currentBased(is24Hour: is24Hour)
        }
        return build(is24Hour: is24Hour, base: base)
    }

    private static func build(is24Hour: Bool, base: Locale) -> Locale {
        var components = Locale.Components(locale: base)
        components.hourCycle = is24Hour ? .zeroToTwentyThree : .oneToTwelve
        return Locale(components: components)
    }

    /// 端末 Locale 由来の 2 値のキャッシュ。
    ///
    /// 行の再バインドのたびに `Locale.Components` の分解と `Locale` の再構築が走るのを避ける。
    /// 端末 Locale が変わればキャッシュした基準と一致しなくなるため、作り直して入れ替える。
    private struct CurrentBasedCache {
        let base: Locale
        let twentyFourHour: Locale
        let twelveHour: Locale

        init(base: Locale) {
            self.base = base
            self.twentyFourHour = build(is24Hour: true, base: base)
            self.twelveHour = build(is24Hour: false, base: base)
        }
    }

    private static let lock = NSLock()
    nonisolated(unsafe) private static var cache: CurrentBasedCache?

    private static func currentBased(is24Hour: Bool) -> Locale {
        let current = Locale.current
        lock.lock()
        defer { lock.unlock() }
        if cache?.base != current {
            cache = CurrentBasedCache(base: current)
        }
        guard let cache else { return build(is24Hour: is24Hour, base: current) }
        return is24Hour ? cache.twentyFourHour : cache.twelveHour
    }
}
#endif
