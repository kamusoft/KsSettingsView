// NegativeVerificationWait.swift
// KsSettingsViewTestSupport
//
// 負の検証 (no-op・不達の確認) 専用の固定時間待機。

#if canImport(UIKit)
import Foundation
import UIKit

/// 「何も起きないこと」を確認するために、一定時間だけ反映の機会を与える。
///
/// 未知 ID・範囲外指定の更新が表示を変えないこと (no-op の確認) や、dispose・購読解除・Host 解放の
/// 後の更新が表示へ届かないこと (不達の確認) を検証するための待機である。これらには待つべき正の
/// 完了条件が存在しないため、条件ベース待機ではなく固定時間の待機で書く (cross/ADR-0027)。
///
/// 収束待ち — 正の完了条件が存在する待機 — にこのヘルパを使ってはいけない。その場合は
/// `awaitCondition` を使う。呼び出し名がこの区別を担っており、ここに現れる待機は
/// 「条件ベース化の直し漏れ」ではなく不変性検証のための意図的な固定待機である。
///
/// - Parameters:
///   - view: 反映の機会を与える view。不要なら `nil`
///   - seconds: 待機時間 (秒)。既定は `KsTestWait.negativeVerificationDuration`。
///     この時間より遅れて起きる誤反映は検出できない (cross/ADR-0027 が受け入れた検出力の上限)
@MainActor
public func waitForNegativeVerification(
    in view: UIView? = nil,
    seconds: TimeInterval = KsTestWait.negativeVerificationDuration
) {
    if let view {
        view.setNeedsLayout()
        view.layoutIfNeeded()
    }
    RunLoop.current.run(until: Date().addingTimeInterval(seconds))
    if let view {
        view.setNeedsLayout()
        view.layoutIfNeeded()
    }
}
#endif
