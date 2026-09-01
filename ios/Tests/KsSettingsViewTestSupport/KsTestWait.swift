// KsTestWait.swift
// KsSettingsViewTestSupport
//
// テストの待機ヘルパが共有する定数と、失敗の発火口を提供する。

#if canImport(UIKit)
import Foundation
import XCTest

/// テストの待機ヘルパが共有する設定値と失敗の発火口。
///
/// deadline やポーリング間隔をここ 1 箇所に集約し、呼び出し側は既定値をそのまま使う
/// (根拠がある場合だけ呼び出しごとに上書きする)。
public enum KsTestWait {

    /// 条件ベース待機の既定 deadline (秒)。
    ///
    /// 条件が成立した時点で待機は即座に抜けるため、この値を長めに取っても通常時の実行時間は
    /// 増えない。実行機が混雑して反映が遅れた場合にだけ余裕として効く。
    public static let defaultDeadline: TimeInterval = 3.0

    /// 条件ベース待機のループ 1 回あたりに RunLoop へ譲る時間 (秒)。
    ///
    /// 待機対象 (レイアウト・再構成・main queue へ post された処理) に実行機会を渡すために、
    /// 短い時間だけ RunLoop を回す。
    public static let pollInterval: TimeInterval = 0.01

    /// 負の検証のための固定待機の既定時間 (秒)。
    public static let negativeVerificationDuration: TimeInterval = 0.2

    /// 待機が deadline を超えたときに失敗を発火する口。
    ///
    /// 既定は `XCTFail` を呼ぶ。差し替えは `withFailureReporter(_:during:)` の内側だけで行い、
    /// 恒久的な差し替えはできない — 戻し忘れた差し替えは以後すべての待機の失敗を握り潰す。
    @MainActor
    static var failureReporter: (String, StaticString, UInt) -> Void = { message, file, line in
        XCTFail(message, file: file, line: line)
    }

    /// `body` の実行中だけ失敗の発火口を差し替える。
    ///
    /// ヘルパ自身のテストが deadline 超過経路の到達とメッセージ内容を検証するための入口である。
    /// 発火口は `body` を抜けた時点で必ず既定へ戻る。
    @MainActor
    public static func withFailureReporter(
        _ reporter: @escaping (String, StaticString, UInt) -> Void,
        during body: () -> Void
    ) {
        let original = failureReporter
        failureReporter = reporter
        defer { failureReporter = original }
        body()
    }

    /// 秒数を失敗メッセージ用に整形する。
    ///
    /// 失敗メッセージの表記を実行環境によらず一定に保つため、桁数と表記規則を固定する。
    static func formatSeconds(_ seconds: TimeInterval) -> String {
        seconds.formatted(
            .number.precision(.fractionLength(3)).locale(Locale(identifier: "en_US_POSIX"))
        )
    }
}

/// 単調増加時計で経過時間を測るストップウォッチ。
///
/// 壁時計 (`Date`) は時刻補正で伸縮するため、待機の打ち切り判定には使わない。
struct KsTestMonotonicClock {
    private let origin = DispatchTime.now()

    /// 生成時点からの経過秒数。
    var elapsed: TimeInterval {
        let nanoseconds = DispatchTime.now().uptimeNanoseconds - origin.uptimeNanoseconds
        return TimeInterval(nanoseconds) / 1_000_000_000
    }
}
#endif
