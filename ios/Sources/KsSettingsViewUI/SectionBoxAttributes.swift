// SectionBoxAttributes.swift
// KsSettingsViewUI
//
// Section の箱（角丸背景 + ボーダー）の装飾値を layout から decoration view へ運ぶ
// `UICollectionViewLayoutAttributes` サブクラス。

#if canImport(UIKit)
import UIKit

/// Section の箱を描く decoration view へ装飾値を輸送する layoutAttributes。
///
/// `UICollectionViewLayoutAttributes` の既定の `isEqual(_:)` は frame / indexPath / zIndex などの
/// 幾何情報しか見ないため、装飾値を等価判定へ含めないと「frame は同じで角丸だけ変えた」更新が
/// decoration view へ届かない。`copy(with:)` も装飾値を引き継ぐよう自前で拡張する。
internal final class SectionBoxAttributes: UICollectionViewLayoutAttributes {
    /// 箱の角丸半径（clamp 前の指定値。clamp は描画時に行う）。
    var cornerRadius: CGFloat = 0
    /// 箱のボーダー幅。
    var borderWidth: CGFloat = 0
    /// 箱のボーダー色。
    var borderColor: UIColor = .clear
    /// 箱の塗り色。
    var boxBackgroundColor: UIColor = .clear

    override func copy(with zone: NSZone? = nil) -> Any {
        // `UICollectionViewLayoutAttributes.copy` はレシーバのクラスで複製を作るため、
        // このキャストは常に成功する。
        guard let copied = super.copy(with: zone) as? SectionBoxAttributes else {
            return super.copy(with: zone)
        }
        copied.cornerRadius = cornerRadius
        copied.borderWidth = borderWidth
        copied.borderColor = borderColor
        copied.boxBackgroundColor = boxBackgroundColor
        return copied
    }

    override func isEqual(_ object: Any?) -> Bool {
        guard let other = object as? SectionBoxAttributes else { return false }
        return super.isEqual(object)
            && cornerRadius == other.cornerRadius
            && borderWidth == other.borderWidth
            && borderColor.isEqual(other.borderColor)
            && boxBackgroundColor.isEqual(other.boxBackgroundColor)
    }

    override var hash: Int {
        var hasher = Hasher()
        hasher.combine(super.hash)
        hasher.combine(cornerRadius)
        hasher.combine(borderWidth)
        hasher.combine(borderColor)
        hasher.combine(boxBackgroundColor)
        return hasher.finalize()
    }
}
#endif
