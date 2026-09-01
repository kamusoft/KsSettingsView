// ConditionWait.swift
// KsSettingsViewTestSupport
//
// 非同期に反映される状態の収束を、完了条件の述語で待つヘルパ群。

#if canImport(UIKit)
import Foundation
import UIKit

/// 述語が成立するまで待つ。
///
/// `UICollectionView` の行の生成・再利用や、レイアウトに伴う内容の反映は `layoutIfNeeded()` を
/// 呼んだ時点では完了しない。待ちたい完了条件そのものを `until` に渡すことで、収束した時点で
/// 即座に後続へ進み、収束しないまま assert へ進むこともなくなる。
///
/// 述語には「操作前には成立せず、非同期反映後に初めて成立する遷移証拠」を渡す。更新前から真の
/// 不変条件を渡すと、待たずに抜けて固定時間待機と同じ「待ったつもり」になる。
///
/// - Parameters:
///   - description: 何の成立を待っているか。失敗メッセージに載る
///   - view: 毎回のループでレイアウトを走らせる view。不要なら `nil`
///   - deadline: 打ち切りまでの実時間 (秒)。既定は `KsTestWait.defaultDeadline`
///   - actual: 失敗時に「実際はどうだったか」を出すための観測値の文字列化
///   - until: 完了条件の述語
@MainActor
public func awaitCondition(
    _ description: String,
    in view: UIView? = nil,
    deadline: TimeInterval = KsTestWait.defaultDeadline,
    actual: () -> String,
    file: StaticString = #filePath,
    line: UInt = #line,
    until predicate: () -> Bool
) {
    let clock = KsTestMonotonicClock()
    var elapsed: TimeInterval = 0

    repeat {
        settleLayout(view)
        // レイアウト確定直後の判定で成立したら、その状態のまま戻る。ここで再度レイアウトを
        // 走らせると「消える / 回収される」型の条件が成立後に崩れうる。
        if predicate() { return }
        // 待機対象へ実行機会を譲る。RunLoop を回さないと main queue へ post された反映が進まない。
        RunLoop.current.run(until: Date().addingTimeInterval(KsTestWait.pollInterval))
        elapsed = clock.elapsed
    } while elapsed < deadline

    settleLayout(view)
    if predicate() { return }

    let observed = actual()
    let message = """
        条件が deadline 内に成立しなかった: \(description) / \
        経過 \(KsTestWait.formatSeconds(elapsed)) 秒 (deadline \(KsTestWait.formatSeconds(deadline)) 秒) / \
        実測: \(observed)
        """
    KsTestWait.failureReporter(message, file, line)
}

/// 観測値が期待値と等しくなるまで待つ。
///
/// `awaitCondition` の頻出形 (「表示が期待どおりになる」) を薄く包み、失敗時の実測値を自動で
/// メッセージへ載せる。
@MainActor
public func awaitEqual<Value: Equatable>(
    _ description: String,
    expected: Value,
    in view: UIView? = nil,
    deadline: TimeInterval = KsTestWait.defaultDeadline,
    file: StaticString = #filePath,
    line: UInt = #line,
    actual: () -> Value
) {
    awaitCondition(
        "\(description) (期待値: \(expected))",
        in: view,
        deadline: deadline,
        actual: { "\(actual())" },
        file: file,
        line: line,
        until: { actual() == expected }
    )
}

/// 観測対象が得られる (非 nil になる) まで待ち、得られた値を返す。
///
/// 実物の Cell / supplementary view / 埋め込み view が生成・再表示されるのを待つ形が頻出する
/// ため、待機と取り出しをまとめる。deadline を超えた場合は失敗を発火して `nil` を返す。
@discardableResult
@MainActor
public func awaitNonNil<Value>(
    _ description: String,
    in view: UIView? = nil,
    deadline: TimeInterval = KsTestWait.defaultDeadline,
    file: StaticString = #filePath,
    line: UInt = #line,
    produce: () -> Value?
) -> Value? {
    var result: Value?
    awaitCondition(
        description,
        in: view,
        deadline: deadline,
        actual: { "nil のまま" },
        file: file,
        line: line,
        until: {
            result = produce()
            return result != nil
        }
    )
    return result
}

/// 待機ループの各段でレイアウトを確定させる。
@MainActor
private func settleLayout(_ view: UIView?) {
    guard let view else { return }
    view.setNeedsLayout()
    view.layoutIfNeeded()
}
#endif
