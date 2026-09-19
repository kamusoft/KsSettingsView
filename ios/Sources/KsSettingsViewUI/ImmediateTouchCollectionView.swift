// ImmediateTouchCollectionView.swift
// KsSettingsViewUI
//
// 押下を遅延なく Cell へ届ける collection view。
//   - スクロール判定のためのタッチ遅延を自分で切る
//   - `UIControl` 上から始めたドラッグでもスクロールをキャンセルできるようにする

#if canImport(UIKit)
import UIKit

/// 押下をすぐ Cell へ渡す `UICollectionView`。
///
/// 押下の伝わり方をライブラリ側で決める方針にもとづく (ios/ADR-0005)。
///
/// `UIScrollView` の既定は「スクロールかタップかを判定するあいだ押下を content へ渡さない」
/// (`delaysContentTouches = true`) で、速いタップでは押下色が出る前に指が離れてしまう。
/// 遅延を切ると押下は即座に届くが、既定の `touchesShouldCancel(in:)` は `UIControl` の上で
/// 始まったドラッグを取り消さないため、TextField などを掴んだままの指ではスクロールできなく
/// なる。この型は遅延を自分で切ったうえで、その判定を「取り消す」に寄せ、ドラッグでスクロール
/// できる状態を保つ。
///
/// ただしスライド操作そのものを入力に使うコントロール (`UISwitch` / `UISlider`) は例外で、
/// 取り消すとつまみのドラッグが scroll に横取りされて操作が不安定になるため、そこから始まった
/// タッチは取り消さない。
internal final class ImmediateTouchCollectionView: UICollectionView {
    override init(frame: CGRect, collectionViewLayout layout: UICollectionViewLayout) {
        super.init(frame: frame, collectionViewLayout: layout)
        // 押下色を指を置いた瞬間に出すため、スクロール判定のためのタッチ遅延を持たない。
        delaysContentTouches = false
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    override func touchesShouldCancel(in view: UIView) -> Bool {
        !hasSlidingControl(from: view)
    }

    /// `view` 自身か、その superview 連鎖のいずれかがスライド操作を持つコントロールかを返す。
    ///
    /// タッチが届く先は SwiftUI 等のラッパー View であることがあるため、自身だけでなく
    /// 上位も辿って判定する。
    private func hasSlidingControl(from view: UIView) -> Bool {
        var current: UIView? = view
        while let target = current, target !== self {
            if target is UISwitch || target is UISlider {
                return true
            }
            current = target.superview
        }
        return false
    }
}
#endif
