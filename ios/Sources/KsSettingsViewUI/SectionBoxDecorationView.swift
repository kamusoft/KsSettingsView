// SectionBoxDecorationView.swift
// KsSettingsViewUI
//
// Section の Cell 行だけを覆う箱（角丸背景 + ボーダー）を描く decoration view。

#if canImport(UIKit)
import UIKit

/// Section の箱を描く decoration view。
///
/// 塗りとボーダーを 1 つの `CALayer` で描く。`CALayer` はボーダーを背景色より前面に描くため、
/// 塗りと枠を別の decoration へ分けたときのような重ね順の取り決めが要らない。
///
/// Cell 側の背景・押下背景は箱の内側形状へ収める（`SectionBoxCellClip`）ため、Cell が不透明背景を
/// 持ってもボーダーは隠れない。
internal final class SectionBoxDecorationView: UICollectionReusableView {
    /// 最後に適用したボーダー色。`layer.borderColor` は解決済みの `CGColor` で外観を持たないため、
    /// 外観の変化に追随できるよう解決前の `UIColor` を保持する。
    private var appliedBorderColor: UIColor = .clear

    override init(frame: CGRect) {
        super.init(frame: frame)
        // 箱はタップを受けない。Cell のタップ判定を妨げないようにする。
        isUserInteractionEnabled = false
        layer.cornerCurve = .continuous

        // dark mode 等の color appearance 変化時に cgColor を再解決する。
        // iOS 17+ は deprecated な traitCollectionDidChange(_:) の代わりに
        // registerForTraitChanges(_:handler:) を使用する。
        if #available(iOS 17.0, *) {
            registerForTraitChanges([UITraitUserInterfaceStyle.self]) {
                (view: SectionBoxDecorationView, _: UITraitCollection) in
                view.applyBorderColor()
            }
        }
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    override func apply(_ layoutAttributes: UICollectionViewLayoutAttributes) {
        super.apply(layoutAttributes)
        guard let attributes = layoutAttributes as? SectionBoxAttributes else {
            backgroundColor = .clear
            appliedBorderColor = .clear
            applyBorderColor()
            layer.borderWidth = 0
            layer.cornerRadius = 0
            return
        }
        backgroundColor = attributes.boxBackgroundColor
        layer.borderWidth = attributes.borderWidth
        appliedBorderColor = attributes.borderColor
        applyBorderColor()
        // 半径の上限は Cell 側の clip と同じ規則で決める（実装を 1 か所に持つ）。
        layer.cornerRadius = SectionBoxMetrics.clampedCornerRadius(attributes.cornerRadius, for: bounds.size)
    }

    /// 保持しているボーダー色を現在の trait（dark mode 等）で解決して `layer` に反映する。
    ///
    /// iOS 16 では traitCollectionDidChange(_:) からのフォールバックで呼び出される。
    /// iOS 17+ では init(frame:) で登録した registerForTraitChanges(_:handler:) から呼び出される。
    private func applyBorderColor() {
        layer.borderColor = appliedBorderColor.resolvedColor(with: traitCollection).cgColor
    }

    /// iOS 16 向けフォールバック。iOS 17+ では registerForTraitChanges(_:handler:) を使用する。
    @available(iOS, deprecated: 17.0, message: "Use registerForTraitChanges(_:handler:) on iOS 17+")
    override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        guard #unavailable(iOS 17.0) else { return }
        if traitCollection.hasDifferentColorAppearance(comparedTo: previousTraitCollection) {
            applyBorderColor()
        }
    }
}
#endif
