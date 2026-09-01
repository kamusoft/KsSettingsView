// ConditionWaitTests.swift
// KsSettingsViewTestSupportTests
//
// 条件ベース待機の契約を検証する: 成立時の早期 return、遅延して成立する述語の成功、
// deadline 超過時の実測値付き失敗。

#if canImport(UIKit)
import XCTest
import Foundation
import UIKit
import KsSettingsViewTestSupport

/// 発火した失敗メッセージを集める入れ物。
@MainActor
private final class FailureBox {
    var messages: [String] = []
}

/// 待機中に別の Task から更新される観測対象を保持する入れ物。
///
/// `Task` のクロージャからローカル変数を書き換えることはできないため、更新対象は参照型に持たせる。
@MainActor
private final class ObservedValue<Value> {
    var value: Value

    init(_ value: Value) {
        self.value = value
    }
}

/// `body` の実行中に発火した失敗メッセージを集めて返す。
///
/// 既定の発火口は `XCTFail` なので、超過経路を通ったことを検証するにはこの差し替えが要る。
@MainActor
private func captureFailures(_ body: () -> Void) -> [String] {
    let box = FailureBox()
    KsTestWait.withFailureReporter({ message, _, _ in box.messages.append(message) }, during: body)
    return box.messages
}

/// 単調増加時計で `body` の経過秒数を測る。
private func measureElapsed(_ body: () -> Void) -> TimeInterval {
    let start = DispatchTime.now()
    body()
    return TimeInterval(DispatchTime.now().uptimeNanoseconds - start.uptimeNanoseconds) / 1_000_000_000
}

/// `seconds` 秒後に MainActor 上で `update` を実行する Task を起こす。
@MainActor
private func scheduleUpdate(after seconds: TimeInterval, _ update: @escaping @MainActor () -> Void) {
    Task { @MainActor in
        try? await Task.sleep(for: .seconds(seconds))
        update()
    }
}

@MainActor
final class ConditionWaitTests: XCTestCase {

    /// 成立済みの条件では deadline まで待ち切らずに戻る。
    func testAwaitConditionReturnsImmediatelyWhenPredicateIsAlreadyTrue() {
        var elapsed: TimeInterval = 0
        let messages = captureFailures {
            elapsed = measureElapsed {
                awaitCondition("成立済みの条件", deadline: 5.0, actual: { "常に成立" }) { true }
            }
        }

        XCTAssertLessThan(elapsed, 0.5, "成立済みの条件で deadline まで待ち切っている")
        XCTAssertTrue(messages.isEmpty)
    }

    /// 呼び出し時点では偽で、待機中に真へ遷移する述語が deadline 内で成功する。
    /// 固定 1 回の評価で諦める待機や、成立を待たずに戻る待機ではここで落ちる。
    func testAwaitConditionSucceedsWithPredicateThatBecomesTrueDuringWait() {
        let flag = ObservedValue(false)
        let delay: TimeInterval = 0.3
        scheduleUpdate(after: delay) { flag.value = true }

        XCTAssertFalse(flag.value, "待機開始前に述語が成立していては遅延成立の検証にならない")

        var elapsed: TimeInterval = 0
        let messages = captureFailures {
            elapsed = measureElapsed {
                awaitCondition("遅延して成立する条件", actual: { "flag=\(flag.value)" }) { flag.value }
            }
        }

        XCTAssertTrue(flag.value)
        XCTAssertTrue(messages.isEmpty, "deadline 内に成立したのに失敗が発火している")
        XCTAssertGreaterThanOrEqual(elapsed, delay, "成立前に戻っている")
        XCTAssertLessThan(elapsed, KsTestWait.defaultDeadline, "deadline を超えている")
    }

    /// deadline 超過は黙って戻らず、経過時間と観測値を載せて失敗する。
    func testAwaitConditionFailsWithMeasuredValuesWhenDeadlineExceeded() {
        let deadline: TimeInterval = 0.1
        var elapsed: TimeInterval = 0
        let messages = captureFailures {
            elapsed = measureElapsed {
                awaitCondition("成立しない条件", deadline: deadline, actual: { "観測値 3 件" }) { false }
            }
        }

        XCTAssertGreaterThanOrEqual(elapsed, deadline)
        XCTAssertEqual(messages.count, 1)
        let message = messages.first ?? ""
        XCTAssertTrue(message.contains("成立しない条件"), message)
        XCTAssertTrue(message.contains("実測: 観測値 3 件"), message)
        XCTAssertTrue(message.contains("deadline 0.100 秒"), message)
        XCTAssertTrue(message.contains("経過 0."), message)
    }

    /// 等価待機は失敗時に期待値と実測値の両方をメッセージへ載せる。
    func testAwaitEqualFailureMessageCarriesExpectedAndActual() {
        let messages = captureFailures {
            awaitEqual("行タイトル数", expected: 3, deadline: 0.05) { 1 }
        }

        XCTAssertEqual(messages.count, 1)
        let message = messages.first ?? ""
        XCTAssertTrue(message.contains("期待値: 3"), message)
        XCTAssertTrue(message.contains("実測: 1"), message)
    }

    /// 非 nil 待機は、待機中に得られるようになった値を返す。
    func testAwaitNonNilReturnsValueThatAppearsDuringWait() {
        let produced = ObservedValue<String?>(nil)
        let delay: TimeInterval = 0.2
        scheduleUpdate(after: delay) { produced.value = "生成された view" }

        XCTAssertNil(produced.value, "待機開始前に値が得られていては遅延成立の検証にならない")

        var result: String?
        var elapsed: TimeInterval = 0
        let messages = captureFailures {
            elapsed = measureElapsed {
                result = awaitNonNil("値が得られる") { produced.value }
            }
        }

        XCTAssertEqual(result, "生成された view")
        XCTAssertTrue(messages.isEmpty)
        XCTAssertGreaterThanOrEqual(elapsed, delay, "値が得られる前に戻っている")
        XCTAssertLessThan(elapsed, KsTestWait.defaultDeadline, "deadline を超えている")
    }

    /// 負の検証用の固定待機は、指定した時間だけ反映の機会を与える。
    func testNegativeVerificationWaitSpendsRequestedDuration() {
        let seconds: TimeInterval = 0.1
        let elapsed = measureElapsed {
            waitForNegativeVerification(seconds: seconds)
        }

        XCTAssertGreaterThanOrEqual(elapsed, seconds)
    }
}
#endif
