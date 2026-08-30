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
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class AccessoryViewDetachDiagnosticTests: XCTestCase {

    private func pump(_ view: UIView, seconds: TimeInterval = 0.05) {
        view.setNeedsLayout()
        view.layoutIfNeeded()
        RunLoop.current.run(until: Date().addingTimeInterval(seconds))
        view.setNeedsLayout()
        view.layoutIfNeeded()
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
        pump(cv)
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
        pump(cv, seconds: 1.0)

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
        pump(cv, seconds: 1.0)
        XCTAssertNotNil(old.superview, "旧 view が自動で剥がれるようになった (所有側の剥がし手順の要否が変わる)")

        // スクロールで再利用を経由しても解消しない
        cv.setContentOffset(CGPoint(x: 0, y: 400), animated: false)
        pump(cv, seconds: 0.3)
        cv.setContentOffset(.zero, animated: false)
        pump(cv, seconds: 0.3)
        XCTAssertNotNil(old.superview, "スクロール往復で旧 view が剥がれるようになった")

        // 所有側が明示的に剥がせば解消し、表示中の view は影響を受けない
        old.removeFromSuperview()
        pump(cv, seconds: 0.2)
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
        pump(cv, seconds: 1.0)

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
        pump(cv, seconds: 0.3)
        XCTAssertNotNil(old.window, "前提: Root header の view が window に載っていない")

        let new = UIView()
        store.updateAccessory(target: .rootHeader, accessory: .root(.view(KsAnyView.uiKit { new })))
        pump(cv, seconds: 0.5)

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
        pump(cv, seconds: 0.5)
        store.updateAccessory(
            target: .sectionHeader(sectionID: sectionID),
            accessory: .section(.view(KsAnyView.uiKit { reused }))
        )
        pump(cv, seconds: 0.5)

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
        pump(cv, seconds: 0.3)
        XCTAssertNil(shared.superview, "画面外へ出た view accessory が剥がれていない")

        cv.setContentOffset(.zero, animated: false)
        pump(cv, seconds: 0.3)
        XCTAssertNotNil(shared.window, "スクロール復帰後に view accessory が表示されていない")
    }
}
#endif
