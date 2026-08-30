// AccessoryViewLiveProbeTests.swift
// KsSettingsViewUITests
//
// accessory を差し替えずに view accessory の中身だけがサイズを変えたとき、
// supplementary の実高さがどこまで自動で追従するかを観測する。
//
// UICollectionView の self-sizing は「内側 view の内在サイズを無効化しただけ」では
// supplementary の再計測を起こさない。追従させるには layout 側の無効化が要る。
// 本ファイルは、どの段階の無効化で追従が始まるかを実測で固定する。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class AccessoryViewLiveProbeTests: XCTestCase {

    /// 内容高さを後から変えられる UIView。
    /// `contentHeight` の更新と `invalidateIntrinsicContentSize()` の呼び出しで、
    /// 中身が自分の計測結果の変化を native へ伝えた状態を作る。
    private final class MutableIntrinsicHeightView: UIView {
        var contentHeight: CGFloat

        init(height: CGFloat) {
            self.contentHeight = height
            super.init(frame: .zero)
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) {
            fatalError("init(coder:) は使用しない")
        }

        override var intrinsicContentSize: CGSize {
            CGSize(width: UIView.noIntrinsicMetric, height: contentHeight)
        }
    }

    private func hostControllerInWindow(
        root: SettingsRoot,
        theme: Theme = Theme()
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let controller = KsSettingsViewController(root: root, theme: theme)
        let size = CGSize(width: 375, height: 600)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        let rootView = controller.view!
        rootView.frame = CGRect(origin: .zero, size: size)
        rootView.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        pump(cv)
        return (controller, cv, window)
    }

    private func pump(_ view: UIView, seconds: TimeInterval = 0.05) {
        view.setNeedsLayout()
        view.layoutIfNeeded()
        RunLoop.current.run(until: Date().addingTimeInterval(seconds))
        view.setNeedsLayout()
        view.layoutIfNeeded()
    }

    private func headerSupplementary(_ cv: UICollectionView, section: Int) -> UICollectionReusableView? {
        cv.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: section)
        )
    }

    private func headerFrameHeight(_ cv: UICollectionView, section: Int) -> CGFloat? {
        headerSupplementary(cv, section: section)?.frame.height
    }

    private func footerFrameHeight(_ cv: UICollectionView, section: Int) -> CGFloat? {
        cv.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionFooter,
            at: IndexPath(item: 0, section: section)
        )?.frame.height
    }

    /// 対象の supplementary だけを無効化するコンテキストを layout へ渡す。
    private func invalidateSupplementary(
        _ cv: UICollectionView,
        kind: String,
        section: Int
    ) {
        let context = UICollectionViewLayoutInvalidationContext()
        context.invalidateSupplementaryElements(ofKind: kind, at: [IndexPath(item: 0, section: section)])
        cv.collectionViewLayout.invalidateLayout(with: context)
    }

    /// `sizeThatFits` でだけ希望サイズを返し、内在サイズを持たない UIView。
    private final class SizeThatFitsOnlyView: UIView {
        private let contentHeight: CGFloat

        init(height: CGFloat) {
            self.contentHeight = height
            super.init(frame: .zero)
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) {
            fatalError("init(coder:) は使用しない")
        }

        override func sizeThatFits(_ size: CGSize) -> CGSize {
            CGSize(width: size.width, height: contentHeight)
        }
    }

    // MARK: - 自己計測の伝え方

    /// 自動高さの accessory は Auto Layout 経由でしか計測されない。
    /// `sizeThatFits` だけを実装した view は希望高さが無視され、領域は推定値のままになる。
    func test_sizeThatFitsだけの中身は高さが反映されない() throws {
        let section = KsSettingsViewCore.Section(
            header: .view(KsAnyView.uiKit { SizeThatFitsOnlyView(height: 120) }),
            cells: [LabelCell(title: "X")]
        )
        let (controller, cv, window) = hostControllerInWindow(root: SettingsRoot(sections: [section]))
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        let height = try XCTUnwrap(headerFrameHeight(cv, section: 0))
        XCTAssertNotEqual(height, 120, accuracy: 0.5,
                          "sizeThatFits だけで自動高さが決まるようになった (中身の計測の伝え方が変わる)")
    }

    // MARK: - 追従に必要な無効化の段階

    /// 弱い通知から順に押し込み、どの段階で追従が始まるかを測る。
    ///   S1 内側 view の invalidateIntrinsicContentSize のみ
    ///   S2 + supplementary view 自身の setNeedsLayout / layoutIfNeeded
    ///   S3 + 当該 supplementary だけを対象にした layout 無効化
    func test_内容変化の追従はsupplementaryのlayout無効化が要る() throws {
        let inner = MutableIntrinsicHeightView(height: 70)
        let section = KsSettingsViewCore.Section(
            header: .view(KsAnyView.uiKit { inner }),
            cells: [LabelCell(title: "X")]
        )
        let (controller, cv, window) = hostControllerInWindow(root: SettingsRoot(sections: [section]))
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        XCTAssertEqual(try XCTUnwrap(headerFrameHeight(cv, section: 0)), 70, accuracy: 0.5,
                       "前提: 初期高さが内容高さ 70pt になっていない")

        // S1: 内側 view の内在サイズ無効化だけでは届かない
        inner.contentHeight = 140
        inner.invalidateIntrinsicContentSize()
        pump(cv)
        XCTAssertEqual(try XCTUnwrap(headerFrameHeight(cv, section: 0)), 70, accuracy: 0.5,
                       "内側 view の内在サイズ無効化だけで追従するようになった (再計算の口の要否が変わる)")

        // S2: supplementary view 自身のレイアウト要求まででも届かない
        headerSupplementary(cv, section: 0)?.setNeedsLayout()
        headerSupplementary(cv, section: 0)?.layoutIfNeeded()
        pump(cv)
        XCTAssertEqual(try XCTUnwrap(headerFrameHeight(cv, section: 0)), 70, accuracy: 0.5,
                       "supplementary view のレイアウト要求だけで追従するようになった")

        // S3: 対象 supplementary の layout 無効化で追従する
        invalidateSupplementary(cv, kind: UICollectionView.elementKindSectionHeader, section: 0)
        pump(cv)
        XCTAssertEqual(try XCTUnwrap(headerFrameHeight(cv, section: 0)), 140, accuracy: 0.5,
                       "対象 supplementary の layout 無効化でも追従しない")
    }

    /// 対象を限定した layout 無効化は、他の押し込みを挟まずとも単独で効く。
    func test_対象限定のlayout無効化だけでheaderの実高さが追従する() throws {
        let inner = MutableIntrinsicHeightView(height: 70)
        let section = KsSettingsViewCore.Section(
            header: .view(KsAnyView.uiKit { inner }),
            cells: [LabelCell(title: "X")]
        )
        let (controller, cv, window) = hostControllerInWindow(root: SettingsRoot(sections: [section]))
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }
        XCTAssertEqual(try XCTUnwrap(headerFrameHeight(cv, section: 0)), 70, accuracy: 0.5)

        inner.contentHeight = 140
        inner.invalidateIntrinsicContentSize()
        invalidateSupplementary(cv, kind: UICollectionView.elementKindSectionHeader, section: 0)
        pump(cv)

        XCTAssertEqual(try XCTUnwrap(headerFrameHeight(cv, section: 0)), 140, accuracy: 0.5,
                       "対象限定の layout 無効化だけでは追従しない")
    }

    /// footer 側も header と同じ条件 (layout 無効化が要る) で追従する。
    func test_footerも同じ条件で追従する() throws {
        let inner = MutableIntrinsicHeightView(height: 50)
        let section = KsSettingsViewCore.Section(
            header: .text("H"),
            footer: .view(KsAnyView.uiKit { inner }),
            cells: [LabelCell(title: "X")]
        )
        let (controller, cv, window) = hostControllerInWindow(root: SettingsRoot(sections: [section]))
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }
        XCTAssertEqual(try XCTUnwrap(footerFrameHeight(cv, section: 0)), 50, accuracy: 0.5)

        inner.contentHeight = 120
        inner.invalidateIntrinsicContentSize()
        pump(cv)
        XCTAssertEqual(try XCTUnwrap(footerFrameHeight(cv, section: 0)), 50, accuracy: 0.5,
                       "footer は内在サイズ無効化だけで追従するようになった")

        invalidateSupplementary(cv, kind: UICollectionView.elementKindSectionFooter, section: 0)
        pump(cv)
        XCTAssertEqual(try XCTUnwrap(footerFrameHeight(cv, section: 0)), 120, accuracy: 0.5,
                       "footer が layout 無効化でも追従しない")
    }

    /// 追従した高さは後続セクションの配置まで届く (領域だけが伸びて重ならない)。
    func test_追従した高さは後続セクションの原点まで届く() throws {
        let inner = MutableIntrinsicHeightView(height: 70)
        let s0 = KsSettingsViewCore.Section(
            header: .view(KsAnyView.uiKit { inner }),
            cells: [LabelCell(title: "X")]
        )
        let s1 = KsSettingsViewCore.Section(
            header: .text("次のセクション"),
            cells: [LabelCell(title: "Y")]
        )
        let (controller, cv, window) = hostControllerInWindow(root: SettingsRoot(sections: [s0, s1]))
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        func section1OriginY() -> CGFloat? {
            cv.collectionViewLayout.layoutAttributesForSupplementaryView(
                ofKind: UICollectionView.elementKindSectionHeader,
                at: IndexPath(item: 0, section: 1)
            )?.frame.origin.y
        }

        let before = try XCTUnwrap(section1OriginY())
        inner.contentHeight = 170
        inner.invalidateIntrinsicContentSize()
        invalidateSupplementary(cv, kind: UICollectionView.elementKindSectionHeader, section: 0)
        pump(cv)

        XCTAssertEqual(try XCTUnwrap(section1OriginY()) - before, 100, accuracy: 1.0,
                       "内容拡大 100pt が後続セクションの原点へ届いていない")
    }

    /// Root accessory も同じ条件で追従する (Root H/F は独自の elementKind を使う)。
    func test_Rootヘッダも同じ条件で追従する() throws {
        let inner = MutableIntrinsicHeightView(height: 60)
        let store = SettingsRootStore(
            initialRoot: SettingsRoot(
                sections: [KsSettingsViewCore.Section(cells: [LabelCell(title: "X")])]
            )
        )
        let controller = KsSettingsViewController(store: store)
        let size = CGSize(width: 375, height: 600)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        controller.view!.frame = CGRect(origin: .zero, size: size)
        controller.view!.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        store.updateAccessory(target: .rootHeader, accessory: .root(.view(KsAnyView.uiKit { inner })))
        pump(cv, seconds: 0.3)

        func rootHeaderHeight() -> CGFloat? {
            cv.visibleSupplementaryViews(
                ofKind: KsSettingsViewController.rootHeaderElementKind
            ).first?.frame.height
        }

        // Root accessory は Section 単位余白を自身の内側に持つため、領域の高さは
        // 内容高さ + Classic 既定 margin の top (22pt) になる。
        XCTAssertEqual(try XCTUnwrap(rootHeaderHeight()), 60 + 22, accuracy: 0.5,
                       "前提: Root header の初期高さが内容高さ 60pt + 既定 margin 22pt になっていない")

        inner.contentHeight = 130
        inner.invalidateIntrinsicContentSize()
        pump(cv)
        XCTAssertEqual(try XCTUnwrap(rootHeaderHeight()), 60 + 22, accuracy: 0.5,
                       "Root header は内在サイズ無効化だけで追従するようになった")

        // Root H/F は layout configuration 側の boundary supplementary であり、
        // 対象限定の無効化でも Section 側と同じ indexPath で指定できる。
        invalidateSupplementary(cv, kind: KsSettingsViewController.rootHeaderElementKind, section: 0)
        pump(cv)
        XCTAssertEqual(try XCTUnwrap(rootHeaderHeight()), 130 + 22, accuracy: 0.5,
                       "Root header が対象限定の layout 無効化で追従しない")
    }
}
#endif
