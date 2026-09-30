// AccessoryViewDetachDiagnosticTests.swift
// KsSettingsViewUITests
//
// view accessory を別インスタンスへ差し替えたときに、旧 view がどう退役するかを観測する。
//
// Section 側の差し替えは supplementary を新しい実体で作り直すため、旧 view は
// 「表示からは退役済み (非表示・不透明度 0・visible supplementary の外) だが
// view 階層には残ったまま」になる。所有側が明示的に剥がすまで superview は解けない。
// Root 側は表示中の supplementary を使い回すため、その場で剥がれる。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class AccessoryViewDetachDiagnosticTests: XCTestCase {

    /// 先頭 Section の header supplementary が表示しているテキストを返す。
    private func headerLabelText(_ cv: UICollectionView, section: Int = 0) -> String? {
        let view = cv.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: section)
        )
        guard let listCell = view as? UICollectionViewListCell else { return nil }
        return listCell.contentView.subviews.compactMap { $0 as? UILabel }.first?.text
    }

    /// 先頭 Section の header として UIKit が保持している supplementary を返す。
    private func headerSupplementary(_ cv: UICollectionView) -> UICollectionReusableView? {
        return cv.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: 0)
        )
    }

    /// 先頭 Section の header のレイアウト上の位置が可視矩形の外にあるかを返す。
    ///
    /// `supplementaryView(forElementKind:at:)` が nil を返すことを画面外の判定に使わない —
    /// UICollectionView は可視矩形の外へ出た supplementary をしばらく返し続け、手放す時期は
    /// 実行機の速さで変わる。位置で判定すれば、UIKit が supplementary を保持し続けても判定の
    /// 意味は変わらない。
    private func isHeaderOffscreen(_ cv: UICollectionView) -> Bool {
        guard let frame = headerLayoutFrame(cv) else { return false }
        return !frame.intersects(cv.bounds)
    }

    private func headerLayoutFrame(_ cv: UICollectionView) -> CGRect? {
        return cv.layoutAttributesForSupplementaryElement(
            ofKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: 0)
        )?.frame
    }

    /// view を失敗メッセージ用に、型と実体の識別子で表す。
    private func describeIdentity(_ view: UIView?) -> String {
        guard let view else { return "nil" }
        return "\(type(of: view))(\(UInt(bitPattern: ObjectIdentifier(view).hashValue)))"
    }

    /// 失敗時に止まった理由を切り分けられるよう、header の位置・可視矩形・UIKit が保持している
    /// supplementary の状態と、比較の基準にした supplementary の実体を返す。
    private func describeHeaderPlacement(
        _ cv: UICollectionView,
        before: UICollectionReusableView
    ) -> String {
        let retained = headerSupplementary(cv)
        return """
            先頭 header frame \(headerLayoutFrame(cv).map { "\($0)" } ?? "なし") / \
            可視矩形 \(cv.bounds) / \
            保持中の supplementary \(describeIdentity(retained)) \
            hidden \(retained.map { "\($0.isHidden)" } ?? "なし") / \
            スクロール前の supplementary \(describeIdentity(before))
            """
    }

    private func host(
        _ controller: KsSettingsViewController
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let size = CGSize(width: 375, height: 600)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        let rootView = controller.view!
        rootView.frame = CGRect(origin: .zero, size: size)
        rootView.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        awaitInitialRender(controller)
        return (controller, cv, window)
    }

    /// header に view accessory を 1 つ持つ Store 接続済み Host を組み立てる。
    private func startHost(
        sectionID: UUID,
        header: SectionAccessory
    ) -> (SettingsRootStore, KsSettingsViewController, UICollectionView, UIWindow) {
        let section = KsSettingsViewCore.Section(
            id: sectionID,
            header: header,
            cells: [LabelCell(title: "X")]
        )
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section]))
        let (controller, cv, window) = host(KsSettingsViewController(store: store))
        return (store, controller, cv, window)
    }

    /// 差し替え後の Section header で、新しい view が表示され旧 view は表示から退役する。
    func test_Sectionのview差し替えで新viewが表示され旧supplementaryは退役する() throws {
        let old = UIView()
        let sectionID = UUID()
        let (store, controller, cv, window) = startHost(
            sectionID: sectionID,
            header: .view(KsAnyView.uiKit { old })
        )
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }
        XCTAssertNotNil(old.window, "前提: 初期表示で旧 view が window に載っていない")

        let new = UIView()
        store.updateAccessory(
            target: .sectionHeader(sectionID: sectionID),
            accessory: .section(.view(KsAnyView.uiKit { new }))
        )
        awaitCondition(
            "差し替えた新しい view が window に載る",
            in: cv,
            actual: { "new.window = \(String(describing: new.window))" },
            until: { new.window != nil }
        )

        XCTAssertNotNil(new.window, "新しい view が表示されていない")

        let oldCell = try XCTUnwrap(old.superview?.superview as? UICollectionReusableView,
                                    "旧 view の supplementary が辿れない")
        let visible = cv.visibleSupplementaryViews(ofKind: UICollectionView.elementKindSectionHeader)
        XCTAssertTrue(oldCell.isHidden || oldCell.alpha == 0,
                      "旧 supplementary が可視のまま残っている (hidden=\(oldCell.isHidden) alpha=\(oldCell.alpha))")
        XCTAssertFalse(visible.contains(oldCell),
                       "旧 supplementary が visible supplementary に含まれている")
        XCTAssertTrue(
            cv.supplementaryView(
                forElementKind: UICollectionView.elementKindSectionHeader,
                at: IndexPath(item: 0, section: 0)
            ) === new.superview?.superview,
            "表示中の supplementary が新しい view を載せた実体になっていない"
        )
    }

    /// 退役した supplementary は view 階層に残り続ける。所有側が剥がすまで superview は解けない。
    func test_退役した旧viewは明示的に剥がすまでview階層に残る() throws {
        let old = UIView()
        let sectionID = UUID()
        let (store, controller, cv, window) = startHost(
            sectionID: sectionID,
            header: .view(KsAnyView.uiKit { old })
        )
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        let new = UIView()
        store.updateAccessory(
            target: .sectionHeader(sectionID: sectionID),
            accessory: .section(.view(KsAnyView.uiKit { new }))
        )
        awaitCondition(
            "差し替えた新しい view が window に載る",
            in: cv,
            actual: { "new.window = \(String(describing: new.window))" },
            until: { new.window != nil }
        )
        XCTAssertNotNil(old.superview, "旧 view が自動で剥がれるようになった (所有側の剥がし手順の要否が変わる)")

        // 画面外にある間に作り直された supplementary を経由して戻しても解消しない。
        // 画面外へ出た supplementary を UIKit がいつ手放すかは実行機の速さで決まるため、画面外に
        // ある間に同じ new を指定し直して header を作り直させ、戻したときにスクロール前と別の
        // 実体へ new が載ることを待つ。
        let supplementaryBeforeScroll = try XCTUnwrap(
            headerSupplementary(cv),
            "前提: 先頭 Section header の supplementary が表示されていない"
        )
        cv.setContentOffset(CGPoint(x: 0, y: 400), animated: false)
        awaitCondition(
            "画面外へ送った header が可視矩形から外れる",
            in: cv,
            actual: { describeHeaderPlacement(cv, before: supplementaryBeforeScroll) },
            until: { isHeaderOffscreen(cv) }
        )
        // Section が 1 つだけのため、作り直しより前に UIKit が header を手放していると、戻したときに
        // 再利用プールから同じ実体が取り出され「別の実体」を遷移の証拠に使えない。作り直しの時点で
        // UIKit がスクロール前の実体を保持していることを前提として確かめる。
        XCTAssertTrue(
            headerSupplementary(cv) === supplementaryBeforeScroll,
            "前提: 作り直しの前に UIKit が header を手放している (\(describeHeaderPlacement(cv, before: supplementaryBeforeScroll)))"
        )
        store.updateAccessory(
            target: .sectionHeader(sectionID: sectionID),
            accessory: .section(.view(KsAnyView.uiKit { new }))
        )
        cv.setContentOffset(.zero, animated: false)
        awaitCondition(
            "先頭へ戻した header が別の supplementary 実体で new を載せて再表示される",
            in: cv,
            actual: {
                "\(describeHeaderPlacement(cv, before: supplementaryBeforeScroll)) / " +
                    "new を載せた supplementary \(describeIdentity(new.superview?.superview))"
            },
            until: {
                guard let current = headerSupplementary(cv) else { return false }
                return current !== supplementaryBeforeScroll && new.superview?.superview === current
            }
        )
        XCTAssertNotNil(old.superview, "作り直された supplementary を経由した往復で旧 view が剥がれるようになった")

        // 所有側が明示的に剥がせば解消し、表示中の view は影響を受けない
        old.removeFromSuperview()
        awaitCondition(
            "明示的に剥がした旧 view が window から外れる",
            in: cv,
            actual: { "old.window = \(String(describing: old.window))" },
            until: { old.window == nil }
        )
        XCTAssertNil(old.window, "明示的に剥がした後も旧 view が残っている")
        XCTAssertNotNil(new.window, "明示的な剥がしで表示中の view まで外れている")
    }

    /// view から text へ落とす場合も、旧 view は同じく view 階層に残る。
    func test_viewからtextへの切替でも旧viewはview階層に残る() throws {
        let old = UIView()
        let sectionID = UUID()
        let (store, controller, cv, window) = startHost(
            sectionID: sectionID,
            header: .view(KsAnyView.uiKit { old })
        )
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        store.updateAccessory(
            target: .sectionHeader(sectionID: sectionID),
            accessory: .section(.text("テキスト"))
        )
        awaitEqual(
            "text へ切り替えた header の実描画テキスト",
            expected: "テキスト" as String?,
            in: cv,
            actual: { headerLabelText(cv) }
        )

        XCTAssertNotNil(old.superview, "text への切替で旧 view が自動で剥がれるようになった")
        old.removeFromSuperview()
        XCTAssertNil(old.window, "明示的に剥がした後も旧 view が残っている")
    }

    /// Root accessory の差し替えは表示中の supplementary を使い回すため、旧 view はその場で剥がれる。
    func test_Rootのview差し替えでは旧viewがその場で剥がれる() throws {
        let store = SettingsRootStore(
            initialRoot: SettingsRoot(
                sections: [KsSettingsViewCore.Section(cells: [LabelCell(title: "X")])]
            )
        )
        let (controller, cv, window) = host(KsSettingsViewController(store: store))
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        let old = UIView()
        store.updateAccessory(target: .rootHeader, accessory: .root(.view(KsAnyView.uiKit { old })))
        awaitCondition(
            "Root header の view が window に載る",
            in: cv,
            actual: { "old.window = \(String(describing: old.window))" },
            until: { old.window != nil }
        )
        XCTAssertNotNil(old.window, "前提: Root header の view が window に載っていない")

        let new = UIView()
        store.updateAccessory(target: .rootHeader, accessory: .root(.view(KsAnyView.uiKit { new })))
        awaitCondition(
            "差し替えた Root header の view が window に載る",
            in: cv,
            actual: { "new.window = \(String(describing: new.window))" },
            until: { new.window != nil }
        )

        XCTAssertNotNil(new.window, "新しい Root header view が表示されていない")
        XCTAssertNil(old.superview, "Root header の旧 view が剥がれていない")
    }

    /// 同一 view インスタンスは、一度 text へ落として再設定しても表示へ復帰できる。
    func test_同一viewインスタンスはtext経由で再設定しても表示へ復帰する() throws {
        let reused = UIView()
        let sectionID = UUID()
        let (store, controller, cv, window) = startHost(
            sectionID: sectionID,
            header: .view(KsAnyView.uiKit { reused })
        )
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        store.updateAccessory(
            target: .sectionHeader(sectionID: sectionID),
            accessory: .section(.text("テキスト"))
        )
        awaitEqual(
            "text へ切り替えた header の実描画テキスト",
            expected: "テキスト" as String?,
            in: cv,
            actual: { headerLabelText(cv) }
        )
        store.updateAccessory(
            target: .sectionHeader(sectionID: sectionID),
            accessory: .section(.view(KsAnyView.uiKit { reused }))
        )
        awaitCondition(
            "再設定した同一 view が window に載る",
            in: cv,
            actual: { "reused.window = \(String(describing: reused.window))" },
            until: { reused.window != nil }
        )

        XCTAssertNotNil(reused.window, "再設定した view が表示されていない")
    }

    /// スクロールで画面外に出た supplementary が再利用されても、同一 view インスタンスは復帰できる。
    func test_スクロール再利用をまたいでも同一viewインスタンスは復帰する() throws {
        let shared = UIView()
        var sections: [KsSettingsViewCore.Section] = [
            KsSettingsViewCore.Section(
                header: .view(KsAnyView.uiKit { shared }),
                cells: [LabelCell(title: "先頭")]
            )
        ]
        for i in 1...30 {
            sections.append(
                KsSettingsViewCore.Section(
                    header: .text("セクション \(i)"),
                    cells: [LabelCell(title: "行 \(i)")]
                )
            )
        }
        let controller = KsSettingsViewController(root: SettingsRoot(sections: sections))
        let (_, cv, window) = host(controller)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }
        XCTAssertNotNil(shared.window, "前提: 初期表示で view accessory が載っていない")

        cv.setContentOffset(CGPoint(x: 0, y: 2000), animated: false)
        awaitCondition(
            "画面外へ出た view accessory が supplementary から剥がれる",
            in: cv,
            actual: { "shared.superview = \(String(describing: shared.superview))" },
            until: { shared.superview == nil }
        )
        XCTAssertNil(shared.superview, "画面外へ出た view accessory が剥がれていない")

        cv.setContentOffset(.zero, animated: false)
        awaitCondition(
            "スクロール復帰で view accessory が再表示される",
            in: cv,
            actual: { "shared.window = \(String(describing: shared.window))" },
            until: { shared.window != nil }
        )
        XCTAssertNotNil(shared.window, "スクロール復帰後に view accessory が表示されていない")
    }
}
#endif
