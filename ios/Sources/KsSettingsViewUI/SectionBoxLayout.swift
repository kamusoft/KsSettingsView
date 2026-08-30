// SectionBoxLayout.swift
// KsSettingsViewUI
//
// Section 背景の decoration を「その Section の Cell 行だけを覆う frame」へ補正する
// compositional layout。Modern の箱（角丸背景 + ボーダー）の描画基盤。
//
// 素の decoration item は Section Header / Footer を含む Section 全域を覆うため、
// 「箱は Cell 群のみを覆い、Header / Footer は箱の外」という契約には frame 補正が要る
// （ios/ADR-0003）。

#if canImport(UIKit)
import UIKit

/// Section の箱の decoration を Cell 行の範囲へ補正する compositional layout。
///
/// 装飾値（角丸・ボーダー・箱の塗り色）は `SectionBoxAttributes` で decoration view へ運ぶ。
/// Theme 変更時は `updateBoxAppearance(metrics:backgroundColor:)` で値を差し替え、
/// `invalidateLayout()` で再評価させる。
internal final class SectionBoxLayout: UICollectionViewCompositionalLayout {
    /// Section の箱を表す decoration の elementKind。
    static let decorationKind = "ks-section-box"

    /// 箱の装飾値。`updateBoxAppearance(metrics:backgroundColor:)` が来るまでの初期値は
    /// 「Theme 未指定の Classic」を `resolve` に通して作る（手組みだと `.classic` で
    /// 水平成分を落とす規則を素通りして、実効値にならない margin を持ってしまう）。
    private(set) var metrics = SectionBoxMetrics.resolve(theme: Theme(), style: .classic)
    /// 箱の塗り色（`Theme.cellBackgroundColor` から解決する）。
    private(set) var boxBackgroundColor: UIColor = .clear

    /// Section ごとの可視 Cell 数を供給する。layout は data source を直接参照しない。
    var cellCountInSection: (Int) -> Int = { _ in 0 }

    /// 箱の装飾値を差し替える。反映には `invalidateLayout()` の呼び出しが要る。
    func updateBoxAppearance(metrics: SectionBoxMetrics, backgroundColor: UIColor) {
        self.metrics = metrics
        self.boxBackgroundColor = backgroundColor
    }

    // MARK: - 属性の供給

    override func layoutAttributesForElements(in rect: CGRect) -> [UICollectionViewLayoutAttributes]? {
        guard let base = super.layoutAttributesForElements(in: rect) else { return nil }
        var result: [UICollectionViewLayoutAttributes] = []
        result.reserveCapacity(base.count)
        for attributes in base {
            guard attributes.representedElementCategory == .decorationView,
                  attributes.representedElementKind == Self.decorationKind else {
                result.append(attributes)
                continue
            }
            // 交差判定は補正前の frame（Section 全域）で済んでいる。補正は必ず縮小方向なので、
            // 補正後の frame は補正前に含まれ、rect との交差判定で取りこぼしは起きない。
            if let corrected = boxAttributes(at: attributes.indexPath, base: attributes) {
                result.append(corrected)
            }
        }
        return result
    }

    override func layoutAttributesForDecorationView(
        ofKind elementKind: String,
        at indexPath: IndexPath
    ) -> UICollectionViewLayoutAttributes? {
        guard let base = super.layoutAttributesForDecorationView(ofKind: elementKind, at: indexPath) else {
            return nil
        }
        guard elementKind == Self.decorationKind else { return base }
        return boxAttributes(at: indexPath, base: base)
    }

    /// 挿入アニメーション中の出現属性。
    ///
    /// 既定実装は補正前の frame を持つ属性を返すため、これを上書きしないと挿入直後の 1 フレームで
    /// 箱が Section 全域へ広がって見える。補正済みの属性を返して破綻を防ぐ。
    override func initialLayoutAttributesForAppearingDecorationElement(
        ofKind elementKind: String,
        at decorationIndexPath: IndexPath
    ) -> UICollectionViewLayoutAttributes? {
        guard elementKind == Self.decorationKind else {
            return super.initialLayoutAttributesForAppearingDecorationElement(
                ofKind: elementKind, at: decorationIndexPath
            )
        }
        return layoutAttributesForDecorationView(ofKind: elementKind, at: decorationIndexPath)
    }

    /// 削除アニメーション中の消滅属性。理由は出現属性と同じ。
    override func finalLayoutAttributesForDisappearingDecorationElement(
        ofKind elementKind: String,
        at decorationIndexPath: IndexPath
    ) -> UICollectionViewLayoutAttributes? {
        guard elementKind == Self.decorationKind else {
            return super.finalLayoutAttributesForDisappearingDecorationElement(
                ofKind: elementKind, at: decorationIndexPath
            )
        }
        return layoutAttributesForDecorationView(ofKind: elementKind, at: decorationIndexPath)
    }

    // MARK: - 箱の geometry

    /// Section の Cell 行だけを覆う箱の矩形。Cell を 1 つも持たない Section では `nil`。
    ///
    /// Cell 側の clip は箱と同じ弧を共有する必要があるため、箱の実寸をここから解決する。
    func sectionBoxFrame(inSection section: Int) -> CGRect? {
        return cellRowsFrame(inSection: section)
    }

    /// 指定 item の矩形。補正を伴わない素の layout 値を返す。
    func itemFrame(at indexPath: IndexPath) -> CGRect? {
        return super.layoutAttributesForItem(at: indexPath)?.frame
    }

    // MARK: - 補正

    /// 補正済みの箱属性を作る。Cell を 1 つも持たない Section では `nil` を返し、箱を生成しない。
    private func boxAttributes(
        at indexPath: IndexPath,
        base: UICollectionViewLayoutAttributes
    ) -> SectionBoxAttributes? {
        guard let frame = cellRowsFrame(inSection: indexPath.section) else { return nil }
        let attributes = SectionBoxAttributes(forDecorationViewOfKind: Self.decorationKind, with: indexPath)
        attributes.frame = frame
        attributes.zIndex = base.zIndex
        attributes.cornerRadius = metrics.cornerRadius
        attributes.borderWidth = metrics.borderWidth
        attributes.borderColor = metrics.borderColor
        attributes.boxBackgroundColor = boxBackgroundColor
        return attributes
    }

    /// Section 内の Cell 行だけを覆う矩形。先頭 Cell と末尾 Cell の frame の和で求める。
    ///
    /// `layoutAttributesForItem(at:)` は画面外の item にも属性を返すため、viewport より長い Section
    /// でも箱の両端は実際の Section 端に一致する。
    private func cellRowsFrame(inSection section: Int) -> CGRect? {
        let count = cellCountInSection(section)
        guard count > 0 else { return nil }
        guard let first = super.layoutAttributesForItem(at: IndexPath(item: 0, section: section)) else {
            return nil
        }
        guard count > 1 else { return first.frame }
        guard let last = super.layoutAttributesForItem(at: IndexPath(item: count - 1, section: section)) else {
            return first.frame
        }
        return first.frame.union(last.frame)
    }
}
#endif
