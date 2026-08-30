// CustomCellRowPlacement.swift
// KsSettingsViewUI
//
// `CustomCellHostedContent` が builder 出力を行の中に置くためのレイアウト。
//
// 決定: core/ADR-0022（共通行レイアウトの適用除外。CustomCell は行の中の配置を自前で決める）。

#if canImport(UIKit)
import SwiftUI

/// content を行の中に配置するレイアウト。定常状態の行の高さ（`max(content の自然高, 実効行高さ)`）
/// を基準に縦位置を決め、行の高さがそれと異なる間（高さ遷移中）も縦位置を動かさない。
/// content が行に収まらないときは上端揃えになる。
///
/// # なぜ独自 Layout が要るか
///
/// `UIHostingConfiguration` のホスト View は、まず行の高さを提案して content を測り、返ってきた
/// 高さが行より大きいと**その高さで組み直して行の中央に置く**。つまり content が行に収まらない間、
/// content は上下へ均等にはみ出す。行の高さは content のサイズ変化より 1 レイアウトパス遅れて
/// 追いつくため、この中央揃えのままだと展開操作が「content が一度上へ飛び出してから落ちてくる」
/// 動きに見えてしまう。
///
/// `frame(maxHeight: .infinity)` では防げない。`frame` の高さは子の高さより小さくならないため、
/// 提案された行の高さへ切り詰めることができず、ホスト View から見た content の高さは
/// 変わらないからである。提案された高さをそのまま自分の高さとして返せるのは `Layout` だけなので、
/// ここだけ独自 Layout を置く。
///
/// # 配置規則
///
/// 縦中央揃えの基準は現在の行の高さではなく、**定常状態の行の高さ**
/// （`max(content の自然高, 実効行高さ)`）に取る。
///
/// - content が定常高さより低い（`cellHeight` で行を高くした場合など）: 定常高さの中で縦中央。
///   標準 Cell と同じ見え方
/// - content が行に収まらない: 上端揃え。はみ出しは下方向だけになり、行が伸びても content の
///   上端は動かない
/// - 行が定常高さより高い（折りたたみ操作の直後、行の縮小が追いつくまでの間）: 定常高さ基準の
///   位置を維持する。現在の行の高さを基準に中央へ置くと、content が「行の中央から上端へ
///   すり上がってくる」動きに見えるため、bounds には追従させない
internal struct CustomCellRowPlacement: Layout {

    /// 実効行高さ（pt）。`CellStyle.cellHeight ?? Theme.rowHeight` を最低行高さで下限ガード
    /// した解決値で、固定高行では行そのものの高さ、self-sizing 行では行高さの下限に一致する。
    let effectiveCellHeight: CGFloat

    func sizeThatFits(
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) -> CGSize {
        guard let subview = subviews.first else {
            return CGSize(
                width: Self.finite(proposal.width) ?? 0,
                height: Self.finite(proposal.height) ?? 0
            )
        }
        let natural = subview.sizeThatFits(Self.contentProposal(for: proposal))
        // 提案された高さをそのまま返す（＝行の高さを超えて自己申告しない）。提案がない
        // （intrinsic size の問い合わせ）ときだけ content の自然高を返し、行の self-sizing に使わせる。
        return CGSize(
            width: Self.finite(proposal.width) ?? natural.width,
            height: Self.finite(proposal.height) ?? natural.height
        )
    }

    func placeSubviews(
        in bounds: CGRect,
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) {
        guard let subview = subviews.first else { return }
        let contentProposal = ProposedViewSize(width: bounds.width, height: nil)
        let natural = subview.sizeThatFits(contentProposal)
        // 定常状態の行の高さ（自然高と実効行高さの大きい方）を基準に、余白があれば等分して
        // 中央に、無ければ上端に置く。基準は bounds を超えない（bounds が定常高さより低い間に
        // content を下へ押し出さないため）。
        let restingHeight = min(bounds.height, max(natural.height, effectiveCellHeight))
        let offsetY = max(0, (restingHeight - natural.height) / 2)
        subview.place(
            at: CGPoint(x: bounds.minX, y: bounds.minY + offsetY),
            anchor: .topLeading,
            proposal: contentProposal
        )
    }

    /// content には幅だけを提案し、高さは自然高に任せる。
    private static func contentProposal(for proposal: ProposedViewSize) -> ProposedViewSize {
        return ProposedViewSize(width: Self.finite(proposal.width), height: nil)
    }

    /// 有限値の提案だけを採用する（`nil` / 無限大は「提案なし」として扱う）。
    private static func finite(_ value: CGFloat?) -> CGFloat? {
        guard let value, value.isFinite else { return nil }
        return value
    }
}
#endif
