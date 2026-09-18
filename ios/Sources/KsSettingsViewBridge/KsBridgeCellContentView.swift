// KsBridgeCellContentView.swift
// KsSettingsViewBridge
//
// interop 境界で受け取った `UIView` を Cell の内容として SwiftUI へ埋め込む包み。

#if canImport(UIKit)
import Foundation
import SwiftUI
import UIKit

/// interop 境界の `UIView` を行の内容として描画する representable。
///
/// Native の `CustomCell` は描画のたびに builder を呼んで View を得る契約だが、interop 境界を
/// 越えて渡されるのは生成済みのインスタンス 1 つである。そのため常に同じインスタンスを表示する
/// (maui/ADR-0017)。
///
/// 描画側へ渡すのは内容そのものではなく、内容を抱える入れ物である。行の再利用で内容が別の行へ
/// 移った後に前の行の描画が片付けられても、片付けの対象は入れ物だけになり、内容が表示中の行から
/// 外れて空行になることがない。
internal struct KsBridgeCellContentView: UIViewRepresentable {

    /// 行の内容として表示する view。
    internal let view: UIView

    internal func makeUIView(context: Context) -> KsBridgeCellContentHostView {
        let host = KsBridgeCellContentHostView()
        host.hold(view)
        return host
    }

    internal func updateUIView(_ uiView: KsBridgeCellContentHostView, context: Context) {
        uiView.refresh(view)
    }

    /// 提示された幅で包んだ view に高さを問い、その高さを SwiftUI の配置系へ中継する。
    ///
    /// 上位層が渡してくるのは自分で計測する view であり、その高さが行の高さになる。行の幅は最初の
    /// 問い合わせから提示されるので、view が自分の幅を知る前に答えを出してしまわないよう、提示された
    /// 幅を渡して測らせる (maui/ADR-0028)。これで折り返す内容も最初の答えが折り返し後の高さになる。
    ///
    /// 高さを答えない view や幅が提示されない問い合わせでは `nil` を返し、SwiftUI の既定の測り方に
    /// 任せる。
    internal func sizeThatFits(
        _ proposal: ProposedViewSize,
        uiView: KsBridgeCellContentHostView,
        context: Context
    ) -> CGSize? {
        guard let width = proposal.width, width.isFinite else {
            return nil
        }

        // 高さは上限なしとして渡し、幅に対する折り返し後の高さを答えさせる。上限なしは有限の最大値で
        // 表し、包む側 (自己計測 wrapper) がそれを上限なしとして解釈する (maui/ADR-0028)。
        let height = view.sizeThatFits(
            CGSize(width: width, height: .greatestFiniteMagnitude)
        ).height
        guard height > 0 else {
            return nil
        }

        return CGSize(width: width, height: height)
    }
}
#endif
