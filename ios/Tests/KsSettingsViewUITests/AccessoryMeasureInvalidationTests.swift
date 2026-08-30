// AccessoryMeasureInvalidationTests.swift
// KsSettingsViewUITests
//
// accessory の中身がサイズを変えたときに、領域の高さを測り直す口が Store から Host へ届くことを
// 実描画で確認する。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class AccessoryMeasureInvalidationTests: XCTestCase {

    /// 内容高さを後から変えられる accessory の中身。
    private final class ProbeView: UIView {
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

    @MainActor
    private struct Attachment {
        let store: SettingsRootStore
        let controller: KsSettingsViewController
        let window: UIWindow

        var collectionView: UICollectionView { controller.internalCollectionView }
    }

    private func attach(_ root: SettingsRoot) -> Attachment {
        let store = SettingsRootStore(initialRoot: root)
        let controller = KsSettingsViewController(store: store)
        let size = CGSize(width: 375, height: 600)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        controller.view!.frame = CGRect(origin: .zero, size: size)
        controller.view!.layoutIfNeeded()
        let attachment = Attachment(store: store, controller: controller, window: window)
        attachment.collectionView.frame = CGRect(origin: .zero, size: size)
        pump(attachment)
        return attachment
    }

    private func pump(_ attachment: Attachment, seconds: TimeInterval = 0.05) {
        let view = attachment.collectionView
        view.setNeedsLayout()
        view.layoutIfNeeded()
        RunLoop.current.run(until: Date().addingTimeInterval(seconds))
        view.setNeedsLayout()
        view.layoutIfNeeded()
    }

    private func headerHeight(_ attachment: Attachment, section: Int) -> CGFloat? {
        return attachment.collectionView.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: section)
        )?.frame.height
    }

    private func footerHeight(_ attachment: Attachment, section: Int) -> CGFloat? {
        return attachment.collectionView.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionFooter,
            at: IndexPath(item: 0, section: section)
        )?.frame.height
    }

    // MARK: - Section 対象

    /// Store の再計測要求で、対象 Section の header 高さが中身の新しい高さへ追従する。
    func test_Store経由の要求でsection_headerの高さが追従する() throws {
        let inner = ProbeView(height: 70)
        let sectionID = UUID()
        let attachment = attach(SettingsRoot(sections: [
            KsSettingsViewCore.Section(
                id: sectionID,
                header: .view(KsAnyView.uiKit { inner }),
                cells: [LabelCell(title: "X")]
            )
        ]))
        defer { attachment.window.isHidden = true }

        XCTAssertEqual(try XCTUnwrap(headerHeight(attachment, section: 0)), 70, accuracy: 0.5,
                       "前提: 初期高さが中身の高さになっていない")

        inner.contentHeight = 140
        inner.invalidateIntrinsicContentSize()
        attachment.store.invalidateAccessoryMeasurement(target: .sectionHeader(sectionID: sectionID))
        pump(attachment)

        XCTAssertEqual(try XCTUnwrap(headerHeight(attachment, section: 0)), 140, accuracy: 0.5,
                       "再計測要求が header 領域の高さへ届いていない")
    }

    /// footer も同じ経路で追従する。
    func test_Store経由の要求でsection_footerの高さが追従する() throws {
        let inner = ProbeView(height: 50)
        let sectionID = UUID()
        let attachment = attach(SettingsRoot(sections: [
            KsSettingsViewCore.Section(
                id: sectionID,
                header: .text("H"),
                footer: .view(KsAnyView.uiKit { inner }),
                cells: [LabelCell(title: "X")]
            )
        ]))
        defer { attachment.window.isHidden = true }

        XCTAssertEqual(try XCTUnwrap(footerHeight(attachment, section: 0)), 50, accuracy: 0.5)

        inner.contentHeight = 120
        inner.invalidateIntrinsicContentSize()
        attachment.store.invalidateAccessoryMeasurement(target: .sectionFooter(sectionID: sectionID))
        pump(attachment)

        XCTAssertEqual(try XCTUnwrap(footerHeight(attachment, section: 0)), 120, accuracy: 0.5,
                       "再計測要求が footer 領域の高さへ届いていない")
    }

    /// 固定高さの header では、再計測要求を出しても高さが変わらない。
    func test_固定高さのheaderは再計測要求で変化しない() throws {
        let inner = ProbeView(height: 40)
        let sectionID = UUID()
        let attachment = attach(SettingsRoot(sections: [
            KsSettingsViewCore.Section(
                id: sectionID,
                header: .view(KsAnyView.uiKit { inner }),
                cells: [LabelCell(title: "X")],
                headerHeight: 80
            )
        ]))
        defer { attachment.window.isHidden = true }

        XCTAssertEqual(try XCTUnwrap(headerHeight(attachment, section: 0)), 80, accuracy: 0.5,
                       "前提: 固定高さが適用されていない")

        inner.contentHeight = 200
        inner.invalidateIntrinsicContentSize()
        attachment.store.invalidateAccessoryMeasurement(target: .sectionHeader(sectionID: sectionID))
        pump(attachment)

        XCTAssertEqual(try XCTUnwrap(headerHeight(attachment, section: 0)), 80, accuracy: 0.5,
                       "固定高さの領域は中身の変化に追従しない")
    }

    /// 現在状態に存在しない sectionID への要求は、表示を変えない。
    func test_未知のsectionIDへの要求は表示を変えない() throws {
        let inner = ProbeView(height: 70)
        let attachment = attach(SettingsRoot(sections: [
            KsSettingsViewCore.Section(
                header: .view(KsAnyView.uiKit { inner }),
                cells: [LabelCell(title: "X")]
            )
        ]))
        defer { attachment.window.isHidden = true }

        inner.contentHeight = 140
        inner.invalidateIntrinsicContentSize()
        attachment.store.invalidateAccessoryMeasurement(target: .sectionHeader(sectionID: UUID()))
        pump(attachment)

        XCTAssertEqual(try XCTUnwrap(headerHeight(attachment, section: 0)), 70, accuracy: 0.5,
                       "別の対象への要求で高さが変わってはいけない")
    }

    /// Store 購読を切ったあとの要求は Host に届かない。
    func test_購読解除後の要求は届かない() throws {
        let inner = ProbeView(height: 70)
        let sectionID = UUID()
        let attachment = attach(SettingsRoot(sections: [
            KsSettingsViewCore.Section(
                id: sectionID,
                header: .view(KsAnyView.uiKit { inner }),
                cells: [LabelCell(title: "X")]
            )
        ]))
        defer { attachment.window.isHidden = true }

        attachment.controller.disconnectStore()
        inner.contentHeight = 140
        inner.invalidateIntrinsicContentSize()
        attachment.store.invalidateAccessoryMeasurement(target: .sectionHeader(sectionID: sectionID))
        pump(attachment)

        XCTAssertEqual(try XCTUnwrap(headerHeight(attachment, section: 0)), 70, accuracy: 0.5,
                       "購読解除後の要求が表示へ届いている")
    }

    // MARK: - Root 対象

    /// Root header も同じ経路で追従する。
    func test_Store経由の要求でroot_headerの高さが追従する() throws {
        let inner = ProbeView(height: 60)
        let attachment = attach(SettingsRoot(sections: [
            KsSettingsViewCore.Section(cells: [LabelCell(title: "X")])
        ]))
        defer { attachment.window.isHidden = true }

        attachment.store.updateAccessory(
            target: .rootHeader,
            accessory: .root(.view(KsAnyView.uiKit { inner }))
        )
        pump(attachment, seconds: 0.3)

        func rootHeaderHeight() -> CGFloat? {
            attachment.collectionView.visibleSupplementaryViews(
                ofKind: KsSettingsViewController.rootHeaderElementKind
            ).first?.frame.height
        }

        // Root accessory は Section 単位余白を自身の内側に持つため、領域の高さは
        // 中身の高さ + Classic 既定 margin の top (22pt) になる。
        XCTAssertEqual(try XCTUnwrap(rootHeaderHeight()), 60 + 22, accuracy: 0.5,
                       "前提: Root header の初期高さが中身の高さ + 既定 margin になっていない")

        inner.contentHeight = 130
        inner.invalidateIntrinsicContentSize()
        attachment.store.invalidateAccessoryMeasurement(target: .rootHeader)
        pump(attachment)

        XCTAssertEqual(try XCTUnwrap(rootHeaderHeight()), 130 + 22, accuracy: 0.5,
                       "Root header の再計測要求が領域の高さへ届いていない")
    }

    /// 未設定の Root footer への要求は表示を変えない。
    func test_未設定のroot_footerへの要求は表示を変えない() throws {
        let attachment = attach(SettingsRoot(sections: [
            KsSettingsViewCore.Section(header: .text("H"), cells: [LabelCell(title: "X")])
        ]))
        defer { attachment.window.isHidden = true }

        attachment.store.invalidateAccessoryMeasurement(target: .rootFooter)
        pump(attachment)

        XCTAssertTrue(
            attachment.collectionView.visibleSupplementaryViews(
                ofKind: KsSettingsViewController.rootFooterElementKind
            ).isEmpty,
            "未設定の Root footer に領域が生まれてはいけない"
        )
    }
}
#endif
