// KsScrollAnimationTicker.swift
// KsSettingsViewUI
//
// 命令によるアニメーション付きスクロールを画面の更新ごとに進める CADisplayLink の受け手。

#if canImport(UIKit)
import UIKit

/// 命令によるアニメーション付きスクロールを、画面の更新ごとに 1 フレームずつ進める。
///
/// `CADisplayLink` は受け手を強参照するため、Host を直接の受け手にせず、Host を弱参照で持つ
/// この型を挟む。Host が解放されたら次のフレームで自分から止まる。
@MainActor
internal final class KsScrollAnimationTicker: NSObject {
    private weak var owner: KsSettingsViewController?
    private var displayLink: CADisplayLink?

    init(owner: KsSettingsViewController) {
        self.owner = owner
        super.init()
    }

    /// 画面の更新ごとの呼び出しを始める。既に動いていれば何もしない。
    func start() {
        guard displayLink == nil else { return }
        let link = CADisplayLink(target: self, selector: #selector(tick(_:)))
        link.add(to: .main, forMode: .common)
        displayLink = link
    }

    /// 画面の更新ごとの呼び出しを止める。
    func stop() {
        displayLink?.invalidate()
        displayLink = nil
    }

    @objc private func tick(_ link: CADisplayLink) {
        guard let owner else {
            stop()
            return
        }
        owner.stepScrollAnimation(at: link.targetTimestamp)
    }
}
#endif
