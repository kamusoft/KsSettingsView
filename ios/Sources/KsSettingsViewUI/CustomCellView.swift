// CustomCellView.swift
// KsSettingsViewUI
//
// `CustomCell` の Renderer 実装。共通行レイアウト（`KsListCellBase` / `applyCellBaseLayout`）は
// 使わず、`UIHostingConfiguration` で builder 出力を行全体に描画する。
//
// 決定: core/ADR-0022（CustomCell は共通行レイアウトによる構成の適用除外）。

#if canImport(UIKit)
import SwiftUI
import UIKit
import KsSettingsViewCore

/// `CustomCell` 描画用 Cell View。
///
/// `KsListCellBase` は title / description の stack 階層を `contentView` に敷くため
/// full-bleed の宣言 UI ホスティングと噛み合わない。本 View は
/// `UICollectionViewListCell` を直接継承し、`contentConfiguration` に
/// `UIHostingConfiguration` を差し込む形で描画する。
///
/// `UICollectionViewListCell` を選ぶのは、罫線（`UIListSeparatorConfiguration`）と
/// `KsCellViewSupport`（背景色 / タッチフィードバック / 実効高さ）の共通機構を
/// 標準 Cell と同一の経路で共有するため。
@MainActor
internal final class CustomCellView: UICollectionViewListCell, @MainActor KsCellRenderer {
    /// 直近 bind 時の `onTap` クロージャ（`isEnabled == false` のときは nil）。
    internal var tapHandler: (@Sendable () -> Void)?

    override init(frame: CGRect) {
        super.init(frame: frame)
        // 標準 Cell と同じタッチフィードバック（Theme.selectedColor）を共有する。
        KsCellViewSupport.installSelectedColorHandler(self)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    // MARK: - KsCellRenderer

    func render(cell: any KsCell, theme: Theme) {
        guard let custom = cell as? CustomCell else {
            assertionFailure("CustomCellView received unexpected cell type: \(type(of: cell))")
            return
        }
        let effective = EffectiveStyle(theme: theme, cellStyle: custom.style)

        // --- 行レベル style の適用 ---
        // CustomCell が適用するのは行レベル項目（背景色 / cellHeight）のみ。
        // テキスト色・フォント等のコンテンツ内装項目は builder 出力に適用先が存在しないため
        // 参照しない（結果として no-op）。
        var background = defaultBackgroundConfiguration()
        background.backgroundColor = effective.cellBackgroundColor
        self.backgroundConfiguration = background

        KsCellViewSupport.state(self).theme = theme
        KsCellViewSupport.setRenderState(
            self,
            theme: theme,
            isEnabled: custom.isEnabled,
            effectiveBackgroundColor: effective.cellBackgroundColor
        )
        KsCellViewSupport.applyEffectiveHeight(self, effective: effective)

        // --- content の描画 ---
        // builder は init 時点で `(AnyHashable) -> AnyView` へ型消去済み。
        // 引数には常に自身の content を渡す。
        let hosted = CustomCellHostedContent(
            content: custom.builder(custom.content),
            showArrow: custom.showArrow,
            isEnabled: custom.isEnabled,
            effectiveCellHeight: effective.effectiveCellHeight
        )
        // full-bleed（行の内側マージンをライブラリ側で持たない）。chevron 側の末端余白のみ
        // `CustomCellHostedContent` が `KsChevronAppearance.trailingMargin` で表現する。
        self.contentConfiguration = UIHostingConfiguration { hosted }
            .margins(.all, 0)

        // --- 行タップ ---
        // `onTap == nil` または `isEnabled == false` のとき行タップは発火しない。
        // content 内の操作可能要素がジェスチャを消費した場合は
        // `UICollectionViewDelegate.didSelectItemAt` 自体が呼ばれないため、
        // 行タップと子の操作は二重発火しない（本 View は行タップ用の
        // gesture recognizer / target-action を独自に追加しない）。
        self.tapHandler = custom.isEnabled ? custom.onTap : nil
    }

    // MARK: - Section の箱への clip

    /// Modern の箱に収めるための mask を bounds の変化に追従させる（標準 Cell と同じ扱い）。
    override func layoutSubviews() {
        super.layoutSubviews()
        KsCellViewSupport.updateSectionBoxClipMask(self)
    }

    // MARK: - 高さ

    /// `KsCellViewSupport.applyEffectiveHeight` が記録した実効 cellHeight を
    /// self-sizing の結果に反映する（標準 Cell の `KsListCellBase` と同じ扱い）。
    override func preferredLayoutAttributesFitting(
        _ layoutAttributes: UICollectionViewLayoutAttributes
    ) -> UICollectionViewLayoutAttributes {
        let base = super.preferredLayoutAttributesFitting(layoutAttributes)
        return KsCellViewSupport.adjustedLayoutAttributes(self, proposed: base)
    }

    // MARK: - 再利用

    /// 再利用時に前の content 由来の表示・listener を残さない。
    ///
    /// `contentConfiguration = nil` で hosted View 階層（および SwiftUI の購読）を解放し、
    /// `tapHandler = nil` で前の `onTap` 参照を切る。
    ///
    /// hosting 階層はリサイクル毎の再生成を許容する（実測で再生成コストが無視できる
    /// 規模であることを確認済み。ios/ADR-0002 — Android の ReusableContent 方式に
    /// 相当する「中身のリサイクル」は iOS では行わない）。
    override func prepareForReuse() {
        super.prepareForReuse()
        self.tapHandler = nil
        self.contentConfiguration = nil
    }
}
#endif
