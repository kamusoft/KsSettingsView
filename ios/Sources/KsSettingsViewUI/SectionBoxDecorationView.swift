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
    override init(frame: CGRect) {
        super.init(frame: frame)
        // 箱はタップを受けない。Cell のタップ判定を妨げないようにする。
        isUserInteractionEnabled = false
        layer.cornerCurve = .continuous
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    override func apply(_ layoutAttributes: UICollectionViewLayoutAttributes) {
        super.apply(layoutAttributes)
        guard let attributes = layoutAttributes as? SectionBoxAttributes else {
            backgroundColor = .clear
            layer.borderWidth = 0
            layer.cornerRadius = 0
            return
        }
        backgroundColor = attributes.boxBackgroundColor
        layer.borderWidth = attributes.borderWidth
        layer.borderColor = attributes.borderColor.cgColor
        // 半径の上限は Cell 側の clip と同じ規則で決める（実装を 1 か所に持つ）。
        layer.cornerRadius = SectionBoxMetrics.clampedCornerRadius(attributes.cornerRadius, for: bounds.size)
    }
}
#endif
