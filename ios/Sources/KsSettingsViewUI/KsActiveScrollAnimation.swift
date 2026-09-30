// KsActiveScrollAnimation.swift
// KsSettingsViewUI
//
// 命令によるアニメーション付きスクロールの進行状態。

#if canImport(UIKit)
import QuartzCore

/// 命令によるアニメーション付きスクロールの進行状態。
///
/// 画面外の行・見出しの位置と内容の高さは推定高さで求まり、表示範囲の寸法もスクロール中に変わる
/// (推定より低い行が実測で確定して内容が短くなる・大きいタイトルの縮小で表示範囲が伸びる)。
/// 命令の時点で求めた位置へ一度に送ると、行き先がスクロール可能範囲の端を越えて止まるため、
/// 毎フレーム行き先を求め直し、出発点からその時点の行き先までを進み具合で補間した位置へ送る
/// (`KsSettingsViewController.stepScrollAnimation(at:)`)。
internal struct KsActiveScrollAnimation {
    /// 行き先の要素。
    let target: KsScrollTarget
    /// 行き先の要素を表示範囲のどこへ合わせるか。
    let position: KsScrollPosition
    /// 出発点のスクロール位置 (contentOffset.y)。
    let startOffset: CGFloat
    /// 進んでいる向き (下向きが正)。命令の時点の行き先から決める。
    let direction: CGFloat
    /// アニメーションの長さ (秒)。
    let duration: CFTimeInterval
    /// 最初のフレームの時刻。最初のフレームが来るまでは `nil`。
    var startTime: CFTimeInterval?
    /// 直近に求めた行き先のスクロール位置 (contentOffset.y)。
    var destination: CGFloat

    /// 進み具合 (0...1) を、動き出しと止まり際を緩めた進み具合へ写す。
    static func eased(_ progress: Double) -> Double {
        let t = min(max(progress, 0), 1)
        // 3 次の ease-in-out。
        return t < 0.5 ? 4 * t * t * t : 1 - pow(-2 * t + 2, 3) / 2
    }
}
#endif
