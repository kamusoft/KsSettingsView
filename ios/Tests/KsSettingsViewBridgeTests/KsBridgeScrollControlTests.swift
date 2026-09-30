// KsBridgeScrollControlTests.swift
// KsSettingsViewBridgeTests
//
// Bridge のスクロール命令 API と、Host の作り直しをまたぐスクロール位置の保持を検証する。
//
// 位置は window に載せた実物の Host の UICollectionView のスクロール位置とレイアウト属性から読む。
// 命令の処理済みは Host の処理済み件数 (`processedScrollCommandCount`) の変化で待つ。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class KsBridgeScrollControlTests: XCTestCase {

    // MARK: - 表示内容

    /// 見出し・5 行を持つ Section を 12 個並べた、画面より十分に長い内容。
    @MainActor
    private struct Fixture {
        let bridge = KsSettingsBridge()
        let sectionIDs: [String]
        let cellIDs: [[String]]

        init() {
            let builder = KsBridgeRootBuilder()
            var sectionIDs: [String] = []
            var cellIDs: [[String]] = []
            for sectionIndex in 0..<12 {
                let section = builder.addSection(headerText: "Section \(sectionIndex)", footerText: nil)
                sectionIDs.append(section.sectionID)
                cellIDs.append((0..<5).map { item in
                    let cell = KsBridgeLabelCell(title: "Cell \(sectionIndex)-\(item)")
                    builder.addLabelCell(cell, sectionID: section.sectionID)
                    return cell.cellID
                })
            }
            bridge.setRoot(builder)
            self.sectionIDs = sectionIDs
            self.cellIDs = cellIDs
        }

        func cellID(section: Int, item: Int) -> String {
            cellIDs[section][item]
        }
    }

    private var windows: [UIWindow] = []

    override func tearDown() {
        windows.forEach { $0.isHidden = true }
        windows.removeAll()
        super.tearDown()
    }

    // MARK: - ヘルパ

    /// Bridge から Host を作る。取り付けは `present(_:)` で行う。
    private func makeHost(_ bridge: KsSettingsBridge) -> KsSettingsViewController {
        guard let host = bridge.makeHostViewController() as? KsSettingsViewController else {
            fatalError("Bridge が Native Host を返さなかった")
        }
        return host
    }

    /// Host を window に載せ、最初の snapshot の反映とレイアウトを確定させる。
    @discardableResult
    private func present(_ host: KsSettingsViewController) -> UICollectionView {
        let size = CGSize(width: 375, height: 600)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = host
        window.makeKeyAndVisible()
        windows.append(window)
        host.view.frame = CGRect(origin: .zero, size: size)
        layoutNow(host.view)
        awaitCondition(
            "最初の snapshot の反映",
            in: host.view,
            actual: { "applied=\(host.hasAppliedInitialSnapshot) applying=\(host.applyingSnapshotCount)" },
            until: { host.hasAppliedInitialSnapshot && host.applyingSnapshotCount == 0 }
        )
        return host.internalCollectionView
    }

    /// Host を window から外す (MAUI の Handler 切断で Host の view が外れるのと同じ状態)。
    private func dismiss(_ host: KsSettingsViewController) {
        host.view.window?.isHidden = true
        host.view.removeFromSuperview()
    }

    /// 命令が指定件数まで処理され、命令のアニメーション (補正を含む) が止まるまで待つ。
    private func awaitScrollSettled(
        _ host: KsSettingsViewController,
        processed count: Int,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        awaitCondition(
            "命令 \(count) 件の処理とアニメーションの停止",
            in: host.view,
            actual: { "processed=\(host.processedScrollCommandCount) animating=\(host.activeScrollAnimation != nil)" },
            file: file,
            line: line,
            until: { host.processedScrollCommandCount >= count && host.activeScrollAnimation == nil }
        )
    }

    private func visibleTop(_ cv: UICollectionView) -> CGFloat {
        cv.contentOffset.y + cv.adjustedContentInset.top
    }

    private func visibleBottom(_ cv: UICollectionView) -> CGFloat {
        cv.contentOffset.y + cv.bounds.height - cv.adjustedContentInset.bottom
    }

    private func minOffset(_ cv: UICollectionView) -> CGFloat {
        -cv.adjustedContentInset.top
    }

    private func maxOffset(_ cv: UICollectionView) -> CGFloat {
        max(minOffset(cv), cv.contentSize.height + cv.adjustedContentInset.bottom - cv.bounds.height)
    }

    private func cellFrame(_ host: KsSettingsViewController, _ cellID: String) -> CGRect? {
        guard let uuid = UUID(uuidString: cellID),
              let indexPath = host.internalDataSource?.indexPath(for: KsCellID(id: uuid)) else { return nil }
        return host.internalCollectionView.layoutAttributesForItem(at: indexPath)?.frame
    }

    private func headerFrame(_ host: KsSettingsViewController, sectionID: String) -> CGRect? {
        guard let uuid = UUID(uuidString: sectionID),
              let index = host.internalDataSource?.snapshot().indexOfSection(uuid) else { return nil }
        return host.internalCollectionView.layoutAttributesForSupplementaryElement(
            ofKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: index)
        )?.frame
    }

    /// 表示中の Host を、`cellID` の行が上端から `offset` だけ上にはみ出す位置へ送る。
    private func scrollHost(
        _ bridge: KsSettingsBridge,
        _ host: KsSettingsViewController,
        cellID: String,
        offset: CGFloat
    ) {
        let cv = host.internalCollectionView
        let processed = host.processedScrollCommandCount
        bridge.scrollToCell(cellID: cellID, position: 0, animated: false)
        awaitScrollSettled(host, processed: processed + 1)
        cv.setContentOffset(CGPoint(x: 0, y: cv.contentOffset.y + offset), animated: false)
        layoutNow(cv)
        XCTAssertEqual(cellFrame(host, cellID)?.minY ?? .nan, visibleTop(cv) - offset, accuracy: 0.5,
                       "前提: 行が上端からはみ出している")
    }

    // MARK: - スクロール命令の Bridge API

    func test_Host生成後の命令で表示範囲の外のCellの中央が表示範囲の中央に来る() {
        let fixture = Fixture()
        let host = makeHost(fixture.bridge)
        let cv = present(host)
        let target = fixture.cellID(section: 7, item: 2)
        XCTAssertGreaterThan(cellFrame(host, target)?.minY ?? 0, visibleBottom(cv), "前提: 対象は表示範囲の下方")

        fixture.bridge.scrollToCell(cellID: target, position: 1, animated: false)

        awaitScrollSettled(host, processed: 1)
        let visibleCenter = (visibleTop(cv) + visibleBottom(cv)) / 2
        XCTAssertEqual(cellFrame(host, target)?.midY ?? .nan, visibleCenter, accuracy: 0.5,
                       "行の中央が表示範囲の中央に来る")
    }

    func test_Sectionへの命令は見出しを上端に合わせる() {
        let fixture = Fixture()
        let host = makeHost(fixture.bridge)
        let cv = present(host)

        fixture.bridge.scrollToSection(sectionID: fixture.sectionIDs[6], position: 0, animated: false)

        awaitScrollSettled(host, processed: 1)
        XCTAssertEqual(headerFrame(host, sectionID: fixture.sectionIDs[6])?.minY ?? .nan, visibleTop(cv),
                       accuracy: 0.5, "見出しの上端が表示範囲の上端に来る")
    }

    func test_末尾と先頭への命令が内容の両端へ届く() {
        let fixture = Fixture()
        let host = makeHost(fixture.bridge)
        let cv = present(host)

        fixture.bridge.scrollToEnd(animated: false)
        awaitScrollSettled(host, processed: 1)
        XCTAssertEqual(cv.contentOffset.y, maxOffset(cv), accuracy: 0.5, "末尾へ届く")

        fixture.bridge.scrollToStart(animated: false)
        awaitScrollSettled(host, processed: 2)
        XCTAssertEqual(cv.contentOffset.y, minOffset(cv), accuracy: 0.5, "先頭へ戻る")
    }

    func test_位置の整数は012をstartcenterendに対応させ範囲外はstartとする() {
        XCTAssertEqual(KsBridgeScrollPosition.position(from: 0), .start)
        XCTAssertEqual(KsBridgeScrollPosition.position(from: 1), .center)
        XCTAssertEqual(KsBridgeScrollPosition.position(from: 2), .end)
        XCTAssertEqual(KsBridgeScrollPosition.position(from: 3), .start)
        XCTAssertEqual(KsBridgeScrollPosition.position(from: -1), .start)
        XCTAssertEqual(KsBridgeScrollPosition.position(from: Int.max), .start)
    }

    func test_範囲外の位置はstartとして扱う() {
        let fixture = Fixture()
        let host = makeHost(fixture.bridge)
        let cv = present(host)
        let target = fixture.cellID(section: 5, item: 1)

        fixture.bridge.scrollToCell(cellID: target, position: 7, animated: false)

        awaitScrollSettled(host, processed: 1)
        XCTAssertEqual(cellFrame(host, target)?.minY ?? .nan, visibleTop(cv), accuracy: 0.5,
                       "行の上端が表示範囲の上端に来る")
    }

    func test_Hostの生成前に出した命令は何も起こさず後から生成したHostの位置にも影響しない() {
        let fixture = Fixture()

        fixture.bridge.scrollToCell(cellID: fixture.cellID(section: 7, item: 2), position: 1, animated: false)
        fixture.bridge.scrollToSection(sectionID: fixture.sectionIDs[6], position: 2, animated: false)
        fixture.bridge.scrollToStart(animated: false)
        fixture.bridge.scrollToEnd(animated: false)
        let host = makeHost(fixture.bridge)
        let cv = present(host)
        waitForNegativeVerification(in: host.view)

        XCTAssertEqual(host.processedScrollCommandCount, 0, "Host の生成前の命令は Host に届かない")
        XCTAssertEqual(cv.contentOffset.y, minOffset(cv), accuracy: 0.5, "内容の先頭のまま")
    }

    func test_releaseHostの後に出した命令は何も起こさず後から生成したHostの位置にも影響しない() {
        let fixture = Fixture()
        let hostA = makeHost(fixture.bridge)
        present(hostA)
        fixture.bridge.releaseHost()
        dismiss(hostA)

        fixture.bridge.scrollToCell(cellID: fixture.cellID(section: 7, item: 2), position: 1, animated: false)
        fixture.bridge.scrollToSection(sectionID: fixture.sectionIDs[6], position: 2, animated: false)
        fixture.bridge.scrollToStart(animated: false)
        fixture.bridge.scrollToEnd(animated: false)
        let hostB = makeHost(fixture.bridge)
        let cvB = present(hostB)
        waitForNegativeVerification(in: hostB.view)

        XCTAssertEqual(hostA.processedScrollCommandCount, 0, "解放した Host には命令が届かない")
        XCTAssertLessThanOrEqual(hostB.processedScrollCommandCount, 1, "新しい Host が処理するのは控えた位置の復元だけ")
        XCTAssertEqual(cvB.contentOffset.y, minOffset(cvB), accuracy: 0.5, "内容の先頭のまま")
    }

    func test_破棄後の命令は何も起こさない() {
        let fixture = Fixture()
        let host = makeHost(fixture.bridge)
        let cv = present(host)
        let before = cv.contentOffset.y

        fixture.bridge.dispose()
        fixture.bridge.scrollToEnd(animated: true)
        fixture.bridge.scrollToCell(cellID: fixture.cellID(section: 7, item: 2), position: 1, animated: false)
        waitForNegativeVerification(in: host.view)

        XCTAssertEqual(host.processedScrollCommandCount, 0, "保持し続けている Host にも命令は届かない")
        XCTAssertNil(host.scrollController, "破棄で Host からハンドルを外す")
        XCTAssertEqual(cv.contentOffset.y, before, accuracy: 0.5, "位置は変わらない")
        XCTAssertNil(fixture.bridge.scrollAnchor, "破棄で控えを捨てる")
    }

    func test_canonicalUUIDでないIDと未知のIDへの命令は位置を変えない() {
        let fixture = Fixture()
        let host = makeHost(fixture.bridge)
        let cv = present(host)

        fixture.bridge.scrollToCell(cellID: "not-a-uuid", position: 1, animated: false)
        fixture.bridge.scrollToSection(sectionID: "not-a-uuid", position: 1, animated: false)
        fixture.bridge.scrollToCell(cellID: UUID().uuidString, position: 1, animated: false)
        fixture.bridge.scrollToSection(sectionID: UUID().uuidString, position: 1, animated: false)
        // 未知の ID は Host まで届いて何もしない。処理済みになった時点で位置を確かめる。
        awaitScrollSettled(host, processed: 2)

        XCTAssertEqual(host.processedScrollCommandCount, 2, "canonical UUID でない ID は Host へ届かない")
        XCTAssertEqual(cv.contentOffset.y, minOffset(cv), accuracy: 0.5, "位置は変わらない")
    }

    // MARK: - Host の作り直しをまたぐスクロール位置の保持

    func test_Hostを作り直しても同じ要素が同じずれで上端にかかる() {
        let fixture = Fixture()
        let hostA = makeHost(fixture.bridge)
        present(hostA)
        let target = fixture.cellID(section: 6, item: 3)
        scrollHost(fixture.bridge, hostA, cellID: target, offset: 10)

        fixture.bridge.releaseHost()
        dismiss(hostA)
        let hostB = makeHost(fixture.bridge)
        let cvB = present(hostB)

        awaitScrollSettled(hostB, processed: 1)
        XCTAssertEqual(cellFrame(hostB, target)?.minY ?? .nan, visibleTop(cvB) - 10, accuracy: 0.5,
                       "手放す前に上端にかかっていた要素が同じずれで上端にかかる")
        XCTAssertNil(fixture.bridge.scrollAnchor, "戻した時点で控えを使い切る")
    }

    func test_windowから外れた後に寸法を失ってから手放しても外れる直前の要素が同じずれで上端にかかる() {
        // MAUI の Handler の切断と同じ順序。ページの view が先に window から外れ、その後に解放が届く。
        // 外れた後の Host のレイアウトは利用者が見ていたものと一致しない (MAUI では View を置いた見出しの
        // 高さも変わる) ため、ここでは外れた後に Host の寸法を失わせる。
        let fixture = Fixture()
        let hostA = makeHost(fixture.bridge)
        present(hostA)
        let target = fixture.cellID(section: 6, item: 3)
        scrollHost(fixture.bridge, hostA, cellID: target, offset: 10)

        dismiss(hostA)
        hostA.view.frame = CGRect(x: 0, y: 0, width: 375, height: 0)
        layoutNow(hostA.view)
        fixture.bridge.releaseHost()
        let hostB = makeHost(fixture.bridge)
        let cvB = present(hostB)

        awaitScrollSettled(hostB, processed: 1)
        XCTAssertEqual(cellFrame(hostB, target)?.minY ?? .nan, visibleTop(cvB) - 10, accuracy: 0.5,
                       "外れる直前に上端にかかっていた要素が同じずれで上端にかかる")
    }

    func test_windowに戻してから手放すと戻した後の位置を控える() {
        let fixture = Fixture()
        let hostA = makeHost(fixture.bridge)
        let cvA = present(hostA)
        scrollHost(fixture.bridge, hostA, cellID: fixture.cellID(section: 6, item: 3), offset: 10)
        dismiss(hostA)

        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 375, height: 600))
        window.addSubview(hostA.view)
        window.makeKeyAndVisible()
        windows.append(window)
        let target = fixture.cellID(section: 2, item: 1)
        scrollHost(fixture.bridge, hostA, cellID: target, offset: 4)
        XCTAssertNotNil(cvA.window, "前提: window に戻っている")
        fixture.bridge.releaseHost()
        dismiss(hostA)
        let hostB = makeHost(fixture.bridge)
        let cvB = present(hostB)

        awaitScrollSettled(hostB, processed: 1)
        XCTAssertEqual(cellFrame(hostB, target)?.minY ?? .nan, visibleTop(cvB) - 4, accuracy: 0.5,
                       "window に戻した後の位置が上端にかかる")
    }

    func test_Hostが無い間に上に項目が増えても同じ要素へ戻る() {
        let fixture = Fixture()
        let hostA = makeHost(fixture.bridge)
        present(hostA)
        let target = fixture.cellID(section: 6, item: 3)
        scrollHost(fixture.bridge, hostA, cellID: target, offset: 10)
        fixture.bridge.releaseHost()
        dismiss(hostA)

        fixture.bridge.insertCell(KsBridgeLabelCell(title: "Inserted A"), sectionID: fixture.sectionIDs[0], at: 0)
        fixture.bridge.insertCell(KsBridgeLabelCell(title: "Inserted B"), sectionID: fixture.sectionIDs[3], at: 0)
        let hostB = makeHost(fixture.bridge)
        let cvB = present(hostB)

        awaitScrollSettled(hostB, processed: 1)
        XCTAssertEqual(cellFrame(hostB, target)?.minY ?? .nan, visibleTop(cvB) - 10, accuracy: 0.5,
                       "控えた要素が同じずれで上端にかかる")
    }

    func test_作り直しの直後に出した命令が戻した位置より優先される() {
        let fixture = Fixture()
        let hostA = makeHost(fixture.bridge)
        present(hostA)
        scrollHost(fixture.bridge, hostA, cellID: fixture.cellID(section: 6, item: 3), offset: 10)
        fixture.bridge.releaseHost()
        dismiss(hostA)

        let hostB = makeHost(fixture.bridge)
        fixture.bridge.scrollToStart(animated: false)
        let cvB = present(hostB)

        awaitScrollSettled(hostB, processed: 2)
        XCTAssertEqual(cvB.contentOffset.y, minOffset(cvB), accuracy: 0.5, "最終位置は内容の先頭")
    }

    func test_控えは一度だけ使われる() {
        let fixture = Fixture()
        let hostA = makeHost(fixture.bridge)
        present(hostA)
        let target = fixture.cellID(section: 6, item: 3)
        scrollHost(fixture.bridge, hostA, cellID: target, offset: 10)
        fixture.bridge.releaseHost()
        dismiss(hostA)

        let hostB = makeHost(fixture.bridge)
        let cvB = present(hostB)
        awaitScrollSettled(hostB, processed: 1)
        XCTAssertEqual(cellFrame(hostB, target)?.minY ?? .nan, visibleTop(cvB) - 10, accuracy: 0.5,
                       "前提: 1 回目の控えへ戻る")
        fixture.bridge.scrollToStart(animated: false)
        awaitScrollSettled(hostB, processed: 2)
        fixture.bridge.releaseHost()
        dismiss(hostB)

        let hostC = makeHost(fixture.bridge)
        let cvC = present(hostC)
        awaitScrollSettled(hostC, processed: 1)

        XCTAssertEqual(cvC.contentOffset.y, minOffset(cvC), accuracy: 0.5,
                       "2 回目に手放したときの位置 (内容の先頭) で表示される")
    }

    func test_控えられる内容が無いHostを解放すると控えを持たない() {
        let fixture = Fixture()
        _ = makeHost(fixture.bridge)

        fixture.bridge.releaseHost()

        XCTAssertNil(fixture.bridge.scrollAnchor, "表示する前の Host からは控えない")
    }
}
#endif
