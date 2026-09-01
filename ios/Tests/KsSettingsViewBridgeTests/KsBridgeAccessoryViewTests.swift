// KsBridgeAccessoryViewTests.swift
// KsSettingsViewBridgeTests
//
// interop 境界を越えて渡した `UIView` が accessory として表示されるまでを、実描画で確認する。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class KsBridgeAccessoryViewTests: XCTestCase {

    /// 内容高さを後から変えられる accessory の中身。
    ///
    /// `contentHeight` の更新と `invalidateIntrinsicContentSize()` の呼び出しで、中身が自分の
    /// 計測結果の変化を Native へ伝えた状態を作る。
    private final class ProbeView: UIView {
        var contentHeight: CGFloat

        init(height: CGFloat = 40) {
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

    // MARK: - updateAccessoryView

    /// Section header へ渡した view が実描画される。
    func test_updateAccessoryView_のsection_headerが表示される() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)
        let probe = ProbeView()

        fixture.bridge.updateAccessoryView(
            target: .sectionHeader,
            sectionID: fixture.section1.sectionID,
            view: probe
        )
        KsBridgeTestHost.awaitSameView(attachment, "section 0 header へ渡した view の実描画", is: probe) {
            KsBridgeTestHost.headerAccessoryView(attachment, section: 0)
        }

        XCTAssertTrue(KsBridgeTestHost.headerAccessoryView(attachment, section: 0) === probe,
                      "渡した view インスタンスがそのまま header に表示される")
        XCTAssertNil(KsBridgeTestHost.headerText(attachment, section: 0),
                     "view を設定した header に text は残らない")
    }

    /// Section footer へ渡した view が実描画される。
    func test_updateAccessoryView_のsection_footerが表示される() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)
        let probe = ProbeView()

        fixture.bridge.updateAccessoryView(
            target: .sectionFooter,
            sectionID: fixture.section1.sectionID,
            view: probe
        )
        KsBridgeTestHost.awaitSameView(attachment, "section 0 footer へ渡した view の実描画", is: probe) {
            KsBridgeTestHost.footerAccessoryView(attachment, section: 0)
        }

        XCTAssertTrue(KsBridgeTestHost.footerAccessoryView(attachment, section: 0) === probe)
    }

    /// Root header / footer へ渡した view が Host のプロパティに載る。
    func test_updateAccessoryView_のroot対象が表示へ反映される() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)
        let headerProbe = ProbeView()
        let footerProbe = ProbeView()

        fixture.bridge.updateAccessoryView(target: .rootHeader, sectionID: nil, view: headerProbe)
        fixture.bridge.updateAccessoryView(target: .rootFooter, sectionID: nil, view: footerProbe)
        KsBridgeTestHost.awaitSameView(attachment, "Root header へ渡した view の実描画", is: headerProbe) {
            KsBridgeTestHost.rootHeaderAccessoryView(attachment)
        }
        KsBridgeTestHost.awaitSameView(attachment, "Root footer へ渡した view の実描画", is: footerProbe) {
            KsBridgeTestHost.rootFooterAccessoryView(attachment)
        }

        XCTAssertTrue(KsBridgeTestHost.rootHeaderAccessoryView(attachment) === headerProbe)
        XCTAssertTrue(KsBridgeTestHost.rootFooterAccessoryView(attachment) === footerProbe)
    }

    /// `nil` を渡すと view accessory が解除され、accessory 未指定と同じ表示に戻る。
    func test_updateAccessoryView_のnilで解除される() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)
        let probe = ProbeView()

        fixture.bridge.updateAccessoryView(
            target: .sectionHeader,
            sectionID: fixture.section1.sectionID,
            view: probe
        )
        KsBridgeTestHost.awaitSameView(attachment, "section 0 header へ渡した view の実描画", is: probe) {
            KsBridgeTestHost.headerAccessoryView(attachment, section: 0)
        }
        XCTAssertTrue(KsBridgeTestHost.headerAccessoryView(attachment, section: 0) === probe)

        fixture.bridge.updateAccessoryView(
            target: .sectionHeader,
            sectionID: fixture.section1.sectionID,
            view: nil
        )
        // clear 前は probe が表示されているため、accessory が消えることが解除完了の遷移証拠になる。
        awaitCondition(
            "section 0 header の view accessory 解除",
            in: attachment.collectionView,
            actual: { KsBridgeTestHost.describe(KsBridgeTestHost.headerAccessoryView(attachment, section: 0)) },
            until: { KsBridgeTestHost.headerAccessoryView(attachment, section: 0) == nil }
        )

        XCTAssertNil(KsBridgeTestHost.headerAccessoryView(attachment, section: 0),
                     "clear 後は accessory が指定されていない場合と同じ表示になる")
        XCTAssertNil(fixture.bridge.store.root.sections.first?.header,
                     "Store の現在状態からも accessory が消える")
    }

    /// Bridge が採番していない canonical UUID への `updateAccessoryView` は、状態も表示も変えない。
    func test_updateAccessoryView_の未使用sectionIDはno_opになる() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        let unusedID = KsBridgeFixture.unusedIdentifier()
        fixture.bridge.updateAccessoryView(target: .sectionHeader, sectionID: unusedID, view: ProbeView())
        fixture.bridge.updateAccessoryView(target: .sectionFooter, sectionID: unusedID, view: ProbeView())
        waitForNegativeVerification(in: attachment.collectionView)

        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A", "B"], ["C"]])
        XCTAssertEqual(KsBridgeTestHost.headerText(attachment, section: 0), "S1")
        XCTAssertEqual(KsBridgeTestHost.headerText(attachment, section: 1), "S2")
        XCTAssertEqual(
            fixture.bridge.store.root.sections.map { $0.header },
            [.text("S1"), .text("S2")],
            "Store の現在状態も変化しない"
        )

        fixture.bridge.replaceCell(cellID: fixture.cellA.cellID, newCell: KsBridgeLabelCell(title: "A2"))
        KsBridgeTestHost.awaitRenderedTitles(attachment, equals: [["A2", "B"], ["C"]])

        XCTAssertEqual(
            KsBridgeTestHost.renderedTitles(attachment),
            [["A2", "B"], ["C"]],
            "後続操作が表示へ届く (Host の Diff 購読が生きている)"
        )
    }

    /// canonical UUID として解釈できない sectionID は Bridge の入口で弾かれる。
    func test_updateAccessoryView_の不正sectionIDはno_opになる() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        fixture.bridge.updateAccessoryView(
            target: .sectionHeader,
            sectionID: KsBridgeFixture.unknownIdentifier,
            view: ProbeView()
        )
        waitForNegativeVerification(in: attachment.collectionView)

        XCTAssertEqual(KsBridgeTestHost.headerText(attachment, section: 0), "S1")
    }

    /// 破棄済みの Bridge では `updateAccessoryView` が表示を変えない。
    func test_updateAccessoryView_は破棄後にno_opになる() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        fixture.bridge.dispose()
        fixture.bridge.updateAccessoryView(
            target: .sectionHeader,
            sectionID: fixture.section1.sectionID,
            view: ProbeView()
        )
        waitForNegativeVerification(in: attachment.collectionView)

        XCTAssertEqual(KsBridgeTestHost.headerText(attachment, section: 0), "S1")
    }

    // MARK: - KsBridgeSection の headerView / footerView

    /// `setRoot` の構築経路で `headerView` / `footerView` が輸送される。
    func test_setRoot_でview_accessory付きSectionが表示される() {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        let section = KsBridgeSection(headerText: nil, footerText: nil)
        let headerProbe = ProbeView()
        let footerProbe = ProbeView()
        section.headerView = headerProbe
        section.footerView = footerProbe
        section.addCell(KsBridgeLabelCell(title: "A"))
        builder.addSection(section)
        bridge.setRoot(builder)

        let attachment = KsBridgeTestHost.attach(bridge)

        XCTAssertTrue(KsBridgeTestHost.headerAccessoryView(attachment, section: 0) === headerProbe)
        XCTAssertTrue(KsBridgeTestHost.footerAccessoryView(attachment, section: 0) === footerProbe)
    }

    /// `replaceSection` の構築経路でも `headerView` が輸送される。
    func test_replaceSection_でview_accessoryが輸送される() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)

        let replacement = KsBridgeSection(headerText: nil, footerText: nil)
        let probe = ProbeView()
        replacement.headerView = probe
        replacement.addCell(KsBridgeLabelCell(title: "A"))
        fixture.bridge.replaceSection(sectionID: fixture.section1.sectionID, newSection: replacement)
        KsBridgeTestHost.awaitSameView(attachment, "置換 Section の header view の実描画", is: probe) {
            KsBridgeTestHost.headerAccessoryView(attachment, section: 0)
        }
        KsBridgeTestHost.awaitRenderedTitles(attachment, equals: [["A"], ["C"]])

        XCTAssertTrue(KsBridgeTestHost.headerAccessoryView(attachment, section: 0) === probe)
        XCTAssertEqual(KsBridgeTestHost.renderedTitles(attachment), [["A"], ["C"]])
    }

    /// text と view の両方を指定した Section では view が表示される。
    func test_textとviewの両指定はviewが優先される() {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        let section = KsBridgeSection(headerText: "TEXT-H", footerText: "TEXT-F")
        let headerProbe = ProbeView()
        section.headerView = headerProbe
        section.addCell(KsBridgeLabelCell(title: "A"))
        builder.addSection(section)
        bridge.setRoot(builder)

        let attachment = KsBridgeTestHost.attach(bridge)

        XCTAssertTrue(KsBridgeTestHost.headerAccessoryView(attachment, section: 0) === headerProbe)
        XCTAssertNil(KsBridgeTestHost.headerText(attachment, section: 0),
                     "view を指定した header に text は表示されない")
        XCTAssertEqual(KsBridgeTestHost.footerText(attachment, section: 0), "TEXT-F",
                       "view 未指定の footer は text がそのまま表示される")
    }

    // MARK: - 再バインド安全性

    /// 画面外へ出て戻る (accessory の再バインドが起きる) 間、同一 view が例外なく再表示される。
    func test_リサイクルを挟んだ再表示が失敗しない() {
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        var sectionIDs: [String] = []
        for sectionIndex in 0..<12 {
            let section = builder.addSection(headerText: "S\(sectionIndex)", footerText: nil)
            for cellIndex in 0..<5 {
                builder.addLabelCell(
                    KsBridgeLabelCell(title: "C\(sectionIndex)-\(cellIndex)"),
                    sectionID: section.sectionID
                )
            }
            sectionIDs.append(section.sectionID)
        }
        bridge.setRoot(builder)

        let attachment = KsBridgeTestHost.attach(bridge)
        let probe = ProbeView()
        bridge.updateAccessoryView(target: .sectionHeader, sectionID: sectionIDs[0], view: probe)
        KsBridgeTestHost.awaitSameView(attachment, "先頭 Section header への view 適用", is: probe) {
            KsBridgeTestHost.headerAccessoryView(attachment, section: 0)
        }
        XCTAssertTrue(KsBridgeTestHost.headerAccessoryView(attachment, section: 0) === probe)

        let collectionView = attachment.collectionView
        let maxOffset = max(0, collectionView.contentSize.height - collectionView.bounds.height)
        XCTAssertGreaterThan(maxOffset, 0, "前提: 画面外へスクロールできる長さの list になっていない")

        // 画面外へ出た supplementary の回収は次のレイアウト周回で確定するため、回収そのものを待つ。
        collectionView.contentOffset = CGPoint(x: 0, y: maxOffset)
        awaitCondition(
            "先頭 Section header が画面外へ出て回収される",
            in: collectionView,
            actual: { KsBridgeTestHost.describe(KsBridgeTestHost.headerAccessoryView(attachment, section: 0)) },
            until: { KsBridgeTestHost.headerAccessoryView(attachment, section: 0) == nil }
        )
        XCTAssertNil(KsBridgeTestHost.headerAccessoryView(attachment, section: 0),
                     "前提: 先頭 header が画面外へ出ていない")

        collectionView.contentOffset = .zero
        KsBridgeTestHost.awaitSameView(attachment, "先頭 Section header の再バインド", is: probe) {
            KsBridgeTestHost.headerAccessoryView(attachment, section: 0)
        }

        XCTAssertTrue(KsBridgeTestHost.headerAccessoryView(attachment, section: 0) === probe,
                      "同一 view が再バインドで再表示される")
    }

    // MARK: - 再計測要求

    /// 中身がサイズを変えたあとの再計測要求で、accessory 領域の高さが追従する。
    func test_invalidateAccessoryMeasurement_でsection_headerの高さが追従する() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)
        let probe = ProbeView(height: 40)

        fixture.bridge.updateAccessoryView(
            target: .sectionHeader,
            sectionID: fixture.section1.sectionID,
            view: probe
        )
        awaitCondition(
            "section 0 header への view 適用と初期計測 (期待高さ: 40)",
            in: attachment.collectionView,
            actual: { "\(KsBridgeTestHost.describe(KsBridgeTestHost.headerAccessoryView(attachment, section: 0))) / 高さ \(self.headerHeight(attachment, section: 0))" },
            until: {
                KsBridgeTestHost.headerAccessoryView(attachment, section: 0) === probe
                    && abs(self.headerHeight(attachment, section: 0) - 40) <= 0.5
            }
        )
        XCTAssertEqual(headerHeight(attachment, section: 0), 40, accuracy: 0.5,
                       "前提: 初期高さが中身の高さになっていない")

        probe.contentHeight = 100
        probe.invalidateIntrinsicContentSize()
        fixture.bridge.invalidateAccessoryMeasurement(
            target: .sectionHeader,
            sectionID: fixture.section1.sectionID
        )
        layoutNow(attachment.collectionView)

        XCTAssertEqual(headerHeight(attachment, section: 0), 100, accuracy: 0.5,
                       "再計測要求が領域の高さへ届いていない")
    }

    /// Root header も同じ経路で高さが追従する。
    func test_invalidateAccessoryMeasurement_でroot_headerの高さが追従する() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)
        let probe = ProbeView(height: 50)

        fixture.bridge.updateAccessoryView(target: .rootHeader, sectionID: nil, view: probe)
        KsBridgeTestHost.awaitSameView(attachment, "Root header への view 適用", is: probe) {
            KsBridgeTestHost.rootHeaderAccessoryView(attachment)
        }
        awaitCondition(
            "Root header の初期計測高さ (期待値: \(50 + 22))",
            in: attachment.collectionView,
            actual: { "高さ \(self.rootHeaderHeight(attachment))" },
            until: { abs(self.rootHeaderHeight(attachment) - (50 + 22)) <= 0.5 }
        )
        // Root accessory は Section 単位余白を自身の内側に持つため、領域の高さは
        // 中身の高さ + Classic 既定 margin の top (22pt) になる。
        XCTAssertEqual(rootHeaderHeight(attachment), 50 + 22, accuracy: 0.5,
                       "前提: Root header の初期高さが中身の高さ + 既定 margin になっていない")

        probe.contentHeight = 120
        probe.invalidateIntrinsicContentSize()
        fixture.bridge.invalidateAccessoryMeasurement(target: .rootHeader, sectionID: nil)
        layoutNow(attachment.collectionView)

        XCTAssertEqual(rootHeaderHeight(attachment), 120 + 22, accuracy: 0.5,
                       "Root header の再計測要求が領域の高さへ届いていない")
    }

    /// 未知の sectionID への再計測要求は、表示を変えずに no-op になる。
    func test_invalidateAccessoryMeasurement_の未使用sectionIDはno_opになる() {
        let fixture = KsBridgeFixture.standard()
        let attachment = KsBridgeTestHost.attach(fixture.bridge)
        let probe = ProbeView(height: 40)

        fixture.bridge.updateAccessoryView(
            target: .sectionHeader,
            sectionID: fixture.section1.sectionID,
            view: probe
        )
        awaitCondition(
            "section 0 header への view 適用と初期計測 (期待高さ: 40)",
            in: attachment.collectionView,
            actual: { "\(KsBridgeTestHost.describe(KsBridgeTestHost.headerAccessoryView(attachment, section: 0))) / 高さ \(self.headerHeight(attachment, section: 0))" },
            until: {
                KsBridgeTestHost.headerAccessoryView(attachment, section: 0) === probe
                    && abs(self.headerHeight(attachment, section: 0) - 40) <= 0.5
            }
        )

        probe.contentHeight = 100
        probe.invalidateIntrinsicContentSize()
        fixture.bridge.invalidateAccessoryMeasurement(
            target: .sectionHeader,
            sectionID: KsBridgeFixture.unusedIdentifier()
        )
        waitForNegativeVerification(in: attachment.collectionView)

        XCTAssertEqual(headerHeight(attachment, section: 0), 40, accuracy: 0.5,
                       "別の対象への要求で高さが変わってはいけない")
    }

    // MARK: - 観測ヘルパ

    private func headerHeight(_ attachment: KsBridgeTestHost.Attachment, section: Int) -> CGFloat {
        return attachment.collectionView.supplementaryView(
            forElementKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: section)
        )?.frame.height ?? .nan
    }

    private func rootHeaderHeight(_ attachment: KsBridgeTestHost.Attachment) -> CGFloat {
        return attachment.collectionView.visibleSupplementaryViews(
            ofKind: KsSettingsViewController.rootHeaderElementKind
        ).first?.frame.height ?? .nan
    }
}
#endif
