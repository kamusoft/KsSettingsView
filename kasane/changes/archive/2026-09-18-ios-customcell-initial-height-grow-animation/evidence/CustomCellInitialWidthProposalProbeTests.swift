// CustomCellInitialWidthProposalProbeTests.swift
// KsSettingsViewUITests
//
// 使い捨ての観測用テスト。
//
// CustomCell の内容を SwiftUI へ埋め込む representable が、行の初回表示 (self-sizing の最初の計測)
// の時点で有限幅の提案を受け取るかを実測する。受け取らないなら、幅付きで測り直す手当ては成り立たない。
// あわせて、埋め込んだ view の必要サイズ (intrinsicContentSize) が問われる順序と、その時点の
// bounds.width も記録する。
//
// 実測が済んだら削除する。

#if canImport(UIKit)
import XCTest
import SwiftUI
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

// MARK: - 観測ログ

/// 計測の問い合わせを起きた順に記録する入れ物。
private final class ProbeLog {
    private(set) var events: [String] = []

    func record(_ text: String) {
        events.append("\(events.count): \(text)")
    }

    /// 記録済みの件数を控える (段階ごとの切り出しに使う)。
    var count: Int { events.count }

    func slice(from start: Int) -> [String] {
        Array(events[min(start, events.count)...])
    }

    func dump(_ label: String) {
        print("[PROBE] ---- \(label) ----")
        for event in events {
            print("[PROBE] \(event)")
        }
    }
}

// MARK: - 自己計測 wrapper の代役

/// 幅が決まっていない間は無限幅で測り、幅が付いたら折り返し後の高さを答える view。
///
/// 折り返す Label を包んだ自己計測 wrapper と同じ計測契約を再現する
/// (幅 0 のとき 1 行ぶん、幅が付くと複数行ぶん)。
private final class ProbeWrapperView: UIView {

    /// 無限幅 (= 折り返さない) で測ったときの高さ。
    static let unwrappedHeight: CGFloat = 60

    /// 有限幅 (= 折り返す) で測ったときの高さ。
    static let wrappedHeight: CGFloat = 200

    private let log: ProbeLog
    private var measuredWidth: CGFloat = -1

    init(log: ProbeLog) {
        self.log = log
        super.init(frame: .zero)
        backgroundColor = .systemTeal
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) は使用しない")
    }

    override var intrinsicContentSize: CGSize {
        measuredWidth = bounds.width
        let widthConstraint = measuredWidth > 0 ? measuredWidth : CGFloat.infinity
        let height = Self.height(forWidth: widthConstraint)
        log.record("intrinsicContentSize bounds.width=\(bounds.width) 使った幅=\(widthConstraint) 返した高さ=\(height)")
        return CGSize(width: UIView.noIntrinsicMetric, height: height)
    }

    override func sizeThatFits(_ size: CGSize) -> CGSize {
        let height = Self.height(forWidth: size.width)
        log.record("sizeThatFits(幅=\(size.width)) bounds.width=\(bounds.width) 返した高さ=\(height)")
        return CGSize(width: size.width, height: height)
    }

    override func layoutSubviews() {
        if abs(measuredWidth - bounds.width) > 0.5 {
            measuredWidth = bounds.width
            log.record("layoutSubviews 幅の変化を検知 bounds.width=\(bounds.width) → 必要サイズを無効化")
            invalidateIntrinsicContentSize()
        }
        super.layoutSubviews()
    }

    /// 折り返す内容の代役。無限幅なら 1 行ぶん、有限幅なら折り返し後の高さ。
    static func height(forWidth width: CGFloat) -> CGFloat {
        (width.isFinite && width > 0) ? wrappedHeight : unwrappedHeight
    }
}

// MARK: - 埋め込みに使う representable

/// 現行の Bridge と同じ測り方 (包んだ view の `intrinsicContentSize` の高さを中継する) の representable。
private struct IntrinsicRelayRepresentable: UIViewRepresentable {
    let view: ProbeWrapperView
    let log: ProbeLog

    func makeUIView(context: Context) -> UIView {
        view.removeFromSuperview()
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}

    func sizeThatFits(
        _ proposal: ProposedViewSize,
        uiView: UIView,
        context: Context
    ) -> CGSize? {
        let proposedWidth = proposal.width.map { "\($0)" } ?? "nil"
        log.record("representable(intrinsic) 提案幅=\(proposedWidth) uiView.bounds.width=\(uiView.bounds.width)")

        let height = view.intrinsicContentSize.height
        guard height != UIView.noIntrinsicMetric,
              let width = proposal.width,
              width.isFinite else {
            log.record("representable(intrinsic) → nil を返す")
            return nil
        }
        log.record("representable(intrinsic) → 高さ \(height) を返す")
        return CGSize(width: width, height: height)
    }
}

/// 幅付きで包んだ view に問い直す representable (幅付き計測の案の形)。
private struct WidthAwareRepresentable: UIViewRepresentable {
    let view: ProbeWrapperView
    let log: ProbeLog

    func makeUIView(context: Context) -> UIView {
        view.removeFromSuperview()
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}

    func sizeThatFits(
        _ proposal: ProposedViewSize,
        uiView: UIView,
        context: Context
    ) -> CGSize? {
        let proposedWidth = proposal.width.map { "\($0)" } ?? "nil"
        log.record("representable(幅付き) 提案幅=\(proposedWidth) uiView.bounds.width=\(uiView.bounds.width)")

        guard let width = proposal.width, width.isFinite else {
            log.record("representable(幅付き) → nil を返す")
            return nil
        }
        let height = view.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude)).height
        log.record("representable(幅付き) → 高さ \(height) を返す")
        return CGSize(width: width, height: height)
    }
}

// MARK: - 観測本体

@MainActor
final class CustomCellInitialWidthProposalProbeTests: XCTestCase {

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

    /// 現行の測り方: 初回の問い合わせで有限幅が提案されるか、行の高さがどう決まるか。
    func test_probe_現行の中継_初回の提案幅と行高さ() {
        let log = ProbeLog()
        let wrapper = ProbeWrapperView(log: log)
        let cell = CustomCell(content: "不変") { _ in
            IntrinsicRelayRepresentable(view: wrapper, log: log)
        }
        let (_, cv, window) = hostController(cells: [cell])
        defer { window.isHidden = true }

        // 最初のレイアウトパスだけを回して、初回計測の記録を切り出す。
        cv.setNeedsLayout()
        cv.layoutIfNeeded()
        let firstPassCount = log.count
        let firstHeight = rowHeight(cv)

        pump(cv, seconds: 0.6)
        let settledHeight = rowHeight(cv)

        log.dump("intrinsic-relay")
        print("[PROBE] intrinsic-relay: 初回パスの記録件数=\(firstPassCount) 初回行高さ=\(String(describing: firstHeight)) 落ち着いた行高さ=\(String(describing: settledHeight))")
    }

    /// 幅付きで問い直す測り方: 初回から折り返し後の高さで行が作られるか。
    func test_probe_幅付きの問い合わせ_初回の提案幅と行高さ() {
        let log = ProbeLog()
        let wrapper = ProbeWrapperView(log: log)
        let cell = CustomCell(content: "不変") { _ in
            WidthAwareRepresentable(view: wrapper, log: log)
        }
        let (_, cv, window) = hostController(cells: [cell])
        defer { window.isHidden = true }

        cv.setNeedsLayout()
        cv.layoutIfNeeded()
        let firstPassCount = log.count
        let firstHeight = rowHeight(cv)

        pump(cv, seconds: 0.6)
        let settledHeight = rowHeight(cv)

        log.dump("width-aware")
        print("[PROBE] width-aware: 初回パスの記録件数=\(firstPassCount) 初回行高さ=\(String(describing: firstHeight)) 落ち着いた行高さ=\(String(describing: settledHeight))")
    }
}
#endif
