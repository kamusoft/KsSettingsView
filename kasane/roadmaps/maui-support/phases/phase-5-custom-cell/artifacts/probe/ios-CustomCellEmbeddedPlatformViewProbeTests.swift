// CustomCellEmbeddedPlatformViewProbeTests.swift
// KsSettingsViewUITests
//
// 使い捨ての観測用テスト。
//
// CustomCell の builder に「自己計測する UIView を包んだ UIViewRepresentable」を埋め込んだとき、
// その UIView が自分の計測を無効化しただけで行の高さが追従するかを実測する。追従しなければ、
// 行を対象にした一過性の再計測通知が別途要ることになる。
//
// 実測が済んだら削除する。

#if canImport(UIKit)
import XCTest
import SwiftUI
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

// MARK: - 自己計測する UIView

/// 必要な高さを `intrinsicContentSize` で答え、内容が変わったら計測無効化を自分から出す UIView。
///
/// 任意の外部 UI を包んで自分で計測・配置する host view (MAUI facade 側の wrapper 等) と
/// 同じ計測契約を再現する。
private final class ProbeHostView: UIView {
    private(set) var contentHeight: CGFloat

    /// 計測無効化を出した回数。
    private(set) var invalidateCount = 0

    /// `intrinsicContentSize` が問い合わされた回数。上位が測り直したかの観測点。
    private(set) var intrinsicQueryCount = 0

    private var measuredWidth: CGFloat = -1

    init(height: CGFloat) {
        self.contentHeight = height
        super.init(frame: .zero)
        backgroundColor = .systemTeal
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) は使用しない")
    }

    override var intrinsicContentSize: CGSize {
        intrinsicQueryCount += 1
        measuredWidth = bounds.width
        return CGSize(width: UIView.noIntrinsicMetric, height: contentHeight)
    }

    override func layoutSubviews() {
        if abs(measuredWidth - bounds.width) > 0.5 {
            measuredWidth = bounds.width
            invalidateIntrinsicContentSize()
        }
        super.layoutSubviews()
    }

    /// UIKit の計測要求にも同じ高さで答える (自分で計測する host view の典型形)。
    override func sizeThatFits(_ size: CGSize) -> CGSize {
        CGSize(width: size.width, height: contentHeight)
    }

    /// 内容のサイズが変わった状況を作る。
    func setContentHeight(_ height: CGFloat) {
        contentHeight = height
        invalidateCount += 1
        invalidateIntrinsicContentSize()
        setNeedsLayout()
    }

    /// 計測無効化を出さずに必要サイズだけを変える (負の対照用)。
    func setContentHeightSilently(_ height: CGFloat) {
        contentHeight = height
    }
}

// MARK: - 埋め込みに使う representable の候補

/// サイズ解決を SwiftUI の既定に任せる representable。
private struct PlainHostRepresentable: UIViewRepresentable {
    let view: ProbeHostView

    func makeUIView(context: Context) -> UIView {
        view.removeFromSuperview()
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}
}

/// 包んだ view のサイズを `sizeThatFits` で SwiftUI へ中継する representable。
private struct RelayingHostRepresentable: UIViewRepresentable {
    let view: ProbeHostView

    func makeUIView(context: Context) -> UIView {
        view.removeFromSuperview()
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UIView, context: Context) -> CGSize? {
        let intrinsic = uiView.intrinsicContentSize
        let width = proposal.width ?? intrinsic.width
        return CGSize(
            width: width.isFinite ? width : 0,
            height: intrinsic.height
        )
    }
}

// MARK: - 観測本体

@MainActor
final class CustomCellEmbeddedPlatformViewProbeTests: XCTestCase {

    private func hostController(
        cells: [any KsCell],
        theme: Theme = Theme(hasUnevenRows: true)
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let controller = KsSettingsViewController(
            root: SettingsRoot(sections: [Section(cells: cells)]),
            theme: theme
        )
        let size = CGSize(width: 375, height: 600)
        let root = controller.view!
        root.frame = CGRect(origin: .zero, size: size)
        let window = UIWindow(frame: root.frame)
        window.addSubview(root)
        window.makeKeyAndVisible()
        root.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        pump(cv)
        return (controller, cv, window)
    }

    private func pump(_ view: UIView, seconds: TimeInterval = 0.3) {
        view.setNeedsLayout()
        view.layoutIfNeeded()
        RunLoop.current.run(until: Date().addingTimeInterval(seconds))
        view.setNeedsLayout()
        view.layoutIfNeeded()
    }

    private func rowHeight(_ cv: UICollectionView) -> CGFloat? {
        cv.cellForItem(at: IndexPath(item: 0, section: 0))?.frame.height
    }

    /// 埋め込んだ view の計測無効化だけで行が追従するかを測る共通手順。
    private func measureFollow(
        label: String,
        makeContent: @escaping (ProbeHostView) -> AnyView,
        explicitInvalidation: ((KsSettingsViewController, UICollectionView) -> Void)? = nil
    ) {
        let host = ProbeHostView(height: 60)
        let cell = CustomCell(content: "不変") { _ in makeContent(host) }
        let (controller, cv, window) = hostController(cells: [cell])
        defer { window.isHidden = true }

        let before = rowHeight(cv)
        let queriesBefore = host.intrinsicQueryCount

        host.setContentHeight(240)
        pump(cv, seconds: 0.5)
        explicitInvalidation?(controller, cv)
        if explicitInvalidation != nil { pump(cv, seconds: 0.5) }

        let after = rowHeight(cv)
        let queriesAfter = host.intrinsicQueryCount

        // 縮小方向も測る (成長だけ追従する失敗形を検出するため)。
        host.setContentHeight(80)
        pump(cv, seconds: 0.5)
        explicitInvalidation?(controller, cv)
        if explicitInvalidation != nil { pump(cv, seconds: 0.5) }
        let shrunk = rowHeight(cv)

        print("""
            [PROBE] \(label): before=\(String(describing: before)) \
            after=\(String(describing: after)) \
            shrunk=\(String(describing: shrunk)) \
            intrinsicQuery=\(queriesBefore)->\(queriesAfter) \
            invalidateCount=\(host.invalidateCount)
            """)

        XCTAssertNotNil(before, "[PROBE] \(label): 行が取得できない")
        guard let before, let after, let shrunk else { return }
        XCTAssertGreaterThan(
            after, before + 100,
            "[PROBE] \(label): 行高さが伸びる方向へ追従しなかった (before=\(before) after=\(after))"
        )
        XCTAssertLessThan(
            shrunk, after - 100,
            "[PROBE] \(label): 行高さが縮む方向へ追従しなかった (after=\(after) shrunk=\(shrunk))"
        )
    }

    /// 既定の representable + 通知なし。
    func test_probe_既定representable_通知なしで行高さが追従するか() {
        measureFollow(label: "plain/no-notify") { host in
            AnyView(PlainHostRepresentable(view: host))
        }
    }

    /// サイズ中継つき representable + 通知なし。
    func test_probe_中継representable_通知なしで行高さが追従するか() {
        measureFollow(label: "relay/no-notify") { host in
            AnyView(RelayingHostRepresentable(view: host))
        }
    }

    /// サイズ中継つき representable + 行を対象にした一過性の再計測通知。
    func test_probe_中継representable_item無効化つきで行高さが追従するか() {
        measureFollow(
            label: "relay/invalidateItems",
            makeContent: { host in AnyView(RelayingHostRepresentable(view: host)) },
            explicitInvalidation: { _, cv in
                let context = UICollectionViewLayoutInvalidationContext()
                context.invalidateItems(at: [IndexPath(item: 0, section: 0)])
                cv.collectionViewLayout.invalidateLayout(with: context)
            }
        )
    }

    /// 既定の representable + 行を対象にした一過性の再計測通知。
    func test_probe_既定representable_item無効化つきで行高さが追従するか() {
        measureFollow(
            label: "plain/invalidateItems",
            makeContent: { host in AnyView(PlainHostRepresentable(view: host)) },
            explicitInvalidation: { _, cv in
                let context = UICollectionViewLayoutInvalidationContext()
                context.invalidateItems(at: [IndexPath(item: 0, section: 0)])
                cv.collectionViewLayout.invalidateLayout(with: context)
            }
        )
    }

    /// 負の対照: 計測無効化を出さずに必要サイズだけ変えても行は追従しないこと。
    ///
    /// これが追従してしまうなら、上の観測は計測無効化の効果ではなくレイアウト駆動の副作用になる。
    func test_probe_負の対照_計測無効化なしでは行高さが追従しない() {
        let host = ProbeHostView(height: 60)
        let cell = CustomCell(content: "不変") { _ in RelayingHostRepresentable(view: host) }
        let (_, cv, window) = hostController(cells: [cell])
        defer { window.isHidden = true }

        let before = rowHeight(cv)
        host.setContentHeightSilently(240)
        pump(cv, seconds: 0.5)
        let after = rowHeight(cv)
        print("[PROBE] relay/silent: before=\(String(describing: before)) after=\(String(describing: after))")

        guard let before, let after else {
            XCTFail("[PROBE] relay/silent: 行が取得できない")
            return
        }
        XCTAssertEqual(
            after, before, accuracy: 0.5,
            "[PROBE] 計測無効化なしでも追従している = 追従の原因が計測無効化とは言えない"
        )
    }

    /// 対照群: 純 SwiftUI の content が自分でサイズを変える経路。測定系が生きていることの確認。
    func test_probe_対照群_純SwiftUIのサイズ変化で行高さが追従する() {
        final class Toggle: ObservableObject {
            @Published var expanded = false
        }
        struct Content: View {
            @ObservedObject var toggle: Toggle
            var body: some View {
                Color.gray.frame(height: toggle.expanded ? 240 : 60)
            }
        }
        let toggle = Toggle()
        let cell = CustomCell(content: "不変") { _ in Content(toggle: toggle) }
        let (_, cv, window) = hostController(cells: [cell])
        defer { window.isHidden = true }

        let before = rowHeight(cv)
        toggle.expanded = true
        pump(cv, seconds: 0.5)
        let after = rowHeight(cv)
        print("[PROBE] swiftui-control: before=\(String(describing: before)) after=\(String(describing: after))")
        guard let before, let after else {
            XCTFail("[PROBE] swiftui-control: 行が取得できない")
            return
        }
        XCTAssertGreaterThan(after, before + 100, "[PROBE] 対照群が追従しない = 測定系が壊れている")
    }
}
#endif
