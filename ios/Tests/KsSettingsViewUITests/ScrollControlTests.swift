// ScrollControlTests.swift
// KsSettingsViewUITests
//
// `KsScrollController` を UIKit Host (`KsSettingsViewController`) につないだときの、接続の規則・
// データの反映の後に実行される順序・Cell / Section / 両端への位置・一方向の着地・位置を控えて戻す
// 窓口を検証する。
//
// 位置は window に載せた実物の UICollectionView のスクロール位置とレイアウト属性から読む。
// 命令の処理済みは Host の処理済み件数 (`processedScrollCommandCount`) の変化で待つ。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class ScrollControlTests: XCTestCase {

    // MARK: - 表示内容

    /// 見出し・5 行・Footer を持つ Section を 12 個並べた、画面より十分に長い内容。
    @MainActor
    private struct Fixture {
        let store: SettingsRootStore
        let sections: [Section]

        init(sectionCount: Int = 12, extraSections: [Section] = []) {
            var sections: [Section] = (0..<sectionCount).map { sectionIndex in
                Section(
                    header: .text("Section \(sectionIndex)"),
                    footer: .text("Footer \(sectionIndex)"),
                    cells: (0..<5).map { LabelCell(title: "Cell \(sectionIndex)-\($0)") }
                )
            }
            sections.append(contentsOf: extraSections)
            self.sections = sections
            self.store = SettingsRootStore(initialRoot: SettingsRoot(sections: sections))
        }

        func cellID(section: Int, item: Int) -> KsCellID {
            KsCellID(cell: sections[section].cells[item])
        }
    }

    private var windows: [UIWindow] = []

    override func tearDown() {
        windows.forEach { $0.isHidden = true }
        windows.removeAll()
        super.tearDown()
    }

    // MARK: - ヘルパ

    /// Host を window に載せ、最初の snapshot の反映とレイアウトを確定させる。
    @discardableResult
    private func present(
        _ controller: KsSettingsViewController,
        size: CGSize = CGSize(width: 375, height: 600)
    ) -> UICollectionView {
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        windows.append(window)
        controller.view.frame = CGRect(origin: .zero, size: size)
        layoutNow(controller.view)
        awaitCondition(
            "最初の snapshot の反映",
            in: controller.view,
            actual: {
                "applied=\(controller.hasAppliedInitialSnapshot) applying=\(controller.applyingSnapshotCount)"
            },
            until: { controller.hasAppliedInitialSnapshot && controller.applyingSnapshotCount == 0 }
        )
        return controller.internalCollectionView
    }

    /// 命令が指定件数まで処理され、命令のアニメーション (補正を含む) が止まるまで待つ。
    private func awaitScrollSettled(
        _ controller: KsSettingsViewController,
        processed count: Int,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        awaitCondition(
            "命令 \(count) 件の処理とアニメーションの停止",
            in: controller.view,
            actual: { scrollCommandState(controller) },
            file: file,
            line: line,
            until: {
                controller.processedScrollCommandCount >= count && controller.activeScrollAnimation == nil
            }
        )
    }

    /// 命令を出した時点の記録。
    @MainActor
    private final class IssuedCommand {
        let issuedAt = DispatchTime.now()
        /// 命令の直後に積んだ `main.async` が回ったか。
        var mainQueueRan = false
    }

    /// 直近に出した命令の記録。次の命令を記録するまで残る。
    private var lastIssuedCommand: IssuedCommand?

    /// 命令 (位置の戻しを含む) を出した直後に呼び、命令からの経過時間と main キューの消化を
    /// 失敗報告に載せられるようにする。
    private func markIssued() {
        let issued = IssuedCommand()
        lastIssuedCommand = issued
        DispatchQueue.main.async { issued.mainQueueRan = true }
    }

    /// 命令の処理を待つ待機が deadline を超えたときに載せる観測値。
    ///
    /// 命令が止まった位置を切り分けるため、Host の待ち行列・実行の条件・main キューの消化をまとめて出す。
    private func scrollCommandState(_ host: KsSettingsViewController) -> String {
        let cv: UICollectionView? = host.isViewLoaded ? host.collectionView : nil
        let queue = "待ち行列: 遅延前=\(host.incomingScrollEntries.count)"
            + " 遅延後=\(host.readyScrollEntries.count)"
            + " 遅延の予約中=\(host.isScrollDeferralScheduled)"
        let conditions = "実行の条件: 最初の反映済み=\(host.hasAppliedInitialSnapshot)"
            + " 適用中の snapshot=\(host.applyingSnapshotCount)"
            + " window=\(cv?.window != nil)"
            + " 高さ=\(cv.map { "\($0.bounds.height)" } ?? "view 未読込")"
        let issue: String
        if let issued = lastIssuedCommand {
            let seconds = Double(DispatchTime.now().uptimeNanoseconds - issued.issuedAt.uptimeNanoseconds)
                / 1_000_000_000
            issue = "命令から \(String(format: "%.3f", seconds)) 秒"
                + " 命令の直後の main.async=\(issued.mainQueueRan ? "回った" : "未実行")"
        } else {
            issue = "命令の記録なし"
        }
        return [
            "processed=\(host.processedScrollCommandCount)",
            queue,
            conditions,
            "アニメーション中=\(host.activeScrollAnimation != nil)",
            issue,
        ].joined(separator: " / ")
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

    private func cellFrame(_ controller: KsSettingsViewController, _ cellID: KsCellID) -> CGRect? {
        guard let indexPath = controller.internalDataSource?.indexPath(for: cellID) else { return nil }
        return controller.internalCollectionView.layoutAttributesForItem(at: indexPath)?.frame
    }

    private func headerFrame(_ controller: KsSettingsViewController, sectionID: UUID) -> CGRect? {
        guard let index = controller.internalDataSource?.snapshot().indexOfSection(sectionID) else { return nil }
        return controller.internalCollectionView.layoutAttributesForSupplementaryElement(
            ofKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: index)
        )?.frame
    }

    private func footerFrame(_ controller: KsSettingsViewController, sectionID: UUID) -> CGRect? {
        guard let index = controller.internalDataSource?.snapshot().indexOfSection(sectionID) else { return nil }
        return controller.internalCollectionView.layoutAttributesForSupplementaryElement(
            ofKind: UICollectionView.elementKindSectionFooter,
            at: IndexPath(item: 0, section: index)
        )?.frame
    }

    // MARK: - スクロール命令ハンドルの公開

    func test_未接続のハンドルへの命令は何も起こさない() {
        let handle = KsScrollController()

        handle.scrollTo(id: KsCellID(id: UUID()))
        handle.scrollToSection(id: UUID())
        handle.scrollToStart()
        handle.scrollToEnd()

        XCTAssertNil(handle.currentReceiver, "未接続のまま命令を出しても接続先は生まれない")
    }

    func test_protocol型の変数からも末尾への命令を出せる() {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        let controlling: any KsScrollControlling = handle

        controlling.scrollToEnd()
        markIssued()

        awaitScrollSettled(host, processed: 1)
        XCTAssertEqual(cv.contentOffset.y, maxOffset(cv), accuracy: 0.5, "protocol 型からの命令で末尾へ届く")
    }

    /// 命令を記録するだけの `KsScrollControlling` 実装。protocol extension の既定値を観測する。
    @MainActor
    private final class RecordingScrollControlling: KsScrollControlling {
        var calls: [String] = []

        func scrollTo(id: some Hashable, position: KsScrollPosition, animated: Bool) {
            calls.append("cell \(id) \(position) \(animated)")
        }

        func scrollToSection(id: some Hashable, position: KsScrollPosition, animated: Bool) {
            calls.append("section \(id) \(position) \(animated)")
        }

        func scrollToStart(animated: Bool) {
            calls.append("start \(animated)")
        }

        func scrollToEnd(animated: Bool) {
            calls.append("end \(animated)")
        }
    }

    func test_protocolの既定値は位置startとアニメーションあり() {
        let recorder = RecordingScrollControlling()
        let controlling: any KsScrollControlling = recorder

        controlling.scrollTo(id: "a")
        controlling.scrollTo(id: "b", position: .center)
        controlling.scrollTo(id: "c", animated: false)
        controlling.scrollToSection(id: 1)
        controlling.scrollToSection(id: 2, position: .end)
        controlling.scrollToSection(id: 3, animated: false)
        controlling.scrollToStart()
        controlling.scrollToEnd()

        XCTAssertEqual(recorder.calls, [
            "cell a start true",
            "cell b center true",
            "cell c start false",
            "section 1 start true",
            "section 2 end true",
            "section 3 start false",
            "start true",
            "end true",
        ])
    }

    // MARK: - Host への接続と最後の接続

    func test_最後に接続したHostだけが命令を受ける() {
        let fixture = Fixture()
        let hostA = KsSettingsViewController(store: fixture.store)
        let hostB = KsSettingsViewController(store: fixture.store)
        let cvA = present(hostA)
        let cvB = present(hostB)
        let initialA = cvA.contentOffset.y
        let handle = KsScrollController()
        hostA.scrollController = handle
        hostB.scrollController = handle

        handle.scrollToEnd(animated: false)
        markIssued()

        awaitScrollSettled(hostB, processed: 1)
        XCTAssertEqual(cvB.contentOffset.y, maxOffset(cvB), accuracy: 0.5, "後から接続した Host B が末尾へ動く")
        XCTAssertEqual(hostA.processedScrollCommandCount, 0, "Host A には命令が届かない")
        XCTAssertEqual(cvA.contentOffset.y, initialA, accuracy: 0.5, "Host A の位置は変わらない")
    }

    func test_接続を外したハンドルの命令は何も起こさない() {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let initial = cv.contentOffset.y
        let handle = KsScrollController()
        host.scrollController = handle
        host.scrollController = nil

        handle.scrollToEnd(animated: false)

        waitForNegativeVerification(in: host.view)
        XCTAssertNil(handle.currentReceiver)
        XCTAssertEqual(host.processedScrollCommandCount, 0)
        XCTAssertEqual(cv.contentOffset.y, initial, accuracy: 0.5, "接続を外した後の命令で位置は変わらない")
    }

    func test_Storeから切断すると接続が外れる() {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let initial = cv.contentOffset.y
        let handle = KsScrollController()
        host.scrollController = handle

        host.disconnectStore()
        handle.scrollToEnd(animated: false)

        XCTAssertNil(host.scrollController, "disconnectStore で scrollController が nil に戻る")
        waitForNegativeVerification(in: host.view)
        XCTAssertEqual(host.processedScrollCommandCount, 0)
        XCTAssertEqual(cv.contentOffset.y, initial, accuracy: 0.5, "切断後の命令で位置は変わらない")
    }

    func test_ハンドルはHostの寿命を延ばさない() {
        let fixture = Fixture()
        let handle = KsScrollController()
        weak var weakHost: KsSettingsViewController?
        autoreleasepool {
            let host = KsSettingsViewController(store: fixture.store)
            host.loadViewIfNeeded()
            host.scrollController = handle
            weakHost = host
            XCTAssertTrue(handle.currentReceiver === host, "前提: 接続されている")
        }

        XCTAssertNil(weakHost, "Host への参照を手放すと、ハンドルが残っていても Host は解放される")
        XCTAssertNil(handle.currentReceiver, "Host の解放でハンドルは未接続に戻る")
        handle.scrollToEnd(animated: false)
        handle.scrollTo(id: fixture.cellID(section: 0, item: 0))
    }

    // MARK: - 命令はデータの反映の後に実行される

    func test_Cellの追加と同じ処理で出した末尾への命令が新しい末尾へ届く() {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        let lastSection = fixture.sections[fixture.sections.count - 1]
        let added = LabelCell(title: "Added")

        // 追加はアニメーション付きで反映される。命令はその反映の完了を待って実行される。
        fixture.store.insertCell(added, in: lastSection.id, at: lastSection.cells.count)
        handle.scrollToEnd(animated: false)
        markIssued()

        awaitScrollSettled(host, processed: 1)
        XCTAssertEqual(cv.contentOffset.y, maxOffset(cv), accuracy: 0.5, "末尾まで届く")
        let addedFrame = cellFrame(host, KsCellID(cell: added))
        XCTAssertNotNil(addedFrame, "追加した Cell が表示に載っている")
        if let addedFrame {
            XCTAssertLessThanOrEqual(addedFrame.maxY, visibleBottom(cv) + 0.5, "追加した Cell が表示範囲に入っている")
            XCTAssertGreaterThanOrEqual(addedFrame.minY, visibleTop(cv) - 0.5)
        }
    }

    func test_進行中の反映がある間は命令を実行せず完了を待つ() {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        let lastSection = fixture.sections[fixture.sections.count - 1]
        let added = LabelCell(title: "Added")
        var observedAtDeferral: (applying: Int, processed: Int)?

        // 命令を先に出し、同じ処理で Cell を追加する。命令の遅延 (main.async) の時点では追加の
        // 反映 (アニメーション付きの apply) がまだ完了していない。
        handle.scrollToEnd(animated: false)
        markIssued()
        DispatchQueue.main.async {
            observedAtDeferral = (host.applyingSnapshotCount, host.processedScrollCommandCount)
        }
        fixture.store.insertCell(added, in: lastSection.id, at: lastSection.cells.count)

        awaitScrollSettled(host, processed: 1)
        let observed = observedAtDeferral
        XCTAssertNotNil(observed)
        XCTAssertGreaterThan(observed?.applying ?? 0, 0, "前提: 命令の遅延の直後にはまだ反映が進行中")
        XCTAssertEqual(observed?.processed, 0, "進行中の反映がある間は命令を実行しない")
        XCTAssertEqual(cv.contentOffset.y, maxOffset(cv), accuracy: 0.5, "反映の完了後に新しい末尾へ届く")
        XCTAssertLessThanOrEqual(cellFrame(host, KsCellID(cell: added))?.maxY ?? .infinity, visibleBottom(cv) + 0.5)
    }

    func test_続けて出した命令は最後の命令の位置で止まる() {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        let targetSection = fixture.sections[8]

        // 先行の命令はアニメーション付き。後続の命令の実行で止められ、最終位置は後続の命令のものになる。
        handle.scrollTo(id: fixture.cellID(section: 2, item: 1), position: .start, animated: true)
        handle.scrollToSection(id: targetSection.id, animated: false)
        markIssued()

        awaitScrollSettled(host, processed: 2)
        let header = headerFrame(host, sectionID: targetSection.id)
        XCTAssertNotNil(header)
        XCTAssertEqual(header?.minY ?? .nan, visibleTop(cv), accuracy: 0.5, "最終位置は Section 8 の見出しの上端")
    }

    func test_実行前に対象が削除された命令は飛ばされ次の命令は実行される() {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        let removed = fixture.cellID(section: 5, item: 2)

        handle.scrollTo(id: removed, animated: false)
        fixture.store.removeCell(cellID: removed)
        handle.scrollToEnd(animated: false)
        markIssued()

        awaitScrollSettled(host, processed: 2)
        XCTAssertNil(cellFrame(host, removed), "前提: 対象は表示から消えている")
        XCTAssertEqual(cv.contentOffset.y, maxOffset(cv), accuracy: 0.5, "後続の末尾への命令は実行される")
    }

    func test_画面の読み込み前に出した命令は初回の表示の後に実行される() {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let handle = KsScrollController()
        host.scrollController = handle
        XCTAssertFalse(host.isViewLoaded, "前提: view は未読込")

        handle.scrollToEnd(animated: false)
        markIssued()
        let cv = present(host)

        awaitScrollSettled(host, processed: 1)
        XCTAssertEqual(cv.contentOffset.y, maxOffset(cv), accuracy: 0.5, "初回の表示の後に末尾へ届く")
    }

    func test_windowから外れている間に出した命令は取り付け直した後に実行される() throws {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        let window = try XCTUnwrap(host.view.window)
        let target = fixture.sections[6]

        host.view.removeFromSuperview()
        handle.scrollToSection(id: target.id, animated: false)
        markIssued()

        waitForNegativeVerification()
        XCTAssertNil(cv.window, "前提: window から外れている")
        XCTAssertEqual(host.processedScrollCommandCount, 0, "外れている間は実行しない")

        window.addSubview(host.view)
        host.view.frame = window.bounds
        layoutNow(host.view)

        awaitScrollSettled(host, processed: 1)
        XCTAssertEqual(headerFrame(host, sectionID: target.id)?.minY ?? .nan, visibleTop(cv), accuracy: 0.5,
                       "取り付け直した後のレイアウトで Section の見出しが上端に来る")
    }

    // MARK: - Cell への命令の位置

    func test_表示範囲の外のCellを中央に合わせる() {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        let target = fixture.cellID(section: 7, item: 2)
        XCTAssertGreaterThan(cellFrame(host, target)?.minY ?? 0, visibleBottom(cv), "前提: 対象は表示範囲の下方")

        handle.scrollTo(id: target, position: .center, animated: false)
        markIssued()

        awaitScrollSettled(host, processed: 1)
        let frame = cellFrame(host, target)
        let visibleCenter = (visibleTop(cv) + visibleBottom(cv)) / 2
        XCTAssertEqual(frame?.midY ?? .nan, visibleCenter, accuracy: 0.5, "行の中央が表示範囲の中央に来る")
    }

    func test_Cellをendに合わせると行の下端が表示範囲の下端に来る() {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        let target = fixture.cellID(section: 6, item: 4)

        handle.scrollTo(id: target, position: .end, animated: false)
        markIssued()

        awaitScrollSettled(host, processed: 1)
        XCTAssertEqual(cellFrame(host, target)?.maxY ?? .nan, visibleBottom(cv), accuracy: 0.5)
    }

    func test_内容の最後のCellを上端に合わせようとするとスクロール可能範囲の端で止まる() {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        let last = fixture.cellID(section: 11, item: 4)

        handle.scrollTo(id: last, position: .start, animated: false)
        markIssued()

        awaitScrollSettled(host, processed: 1)
        XCTAssertEqual(cv.contentOffset.y, maxOffset(cv), accuracy: 0.5, "末尾側の端で止まり、行き過ぎない")
        XCTAssertGreaterThan(cellFrame(host, last)?.minY ?? 0, visibleTop(cv), "前提: 行の上端は上端まで届いていない")
    }

    func test_非表示のCellと存在しないIDへの命令は位置を変えない() {
        let hidden = LabelCell(title: "Hidden", isVisible: false)
        let extra = Section(header: .text("Extra"), cells: [LabelCell(title: "Shown"), hidden])
        let fixture = Fixture(extraSections: [extra])
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        let initial = cv.contentOffset.y

        handle.scrollTo(id: KsCellID(cell: hidden), animated: false)
        handle.scrollTo(id: KsCellID(id: UUID()), animated: false)
        markIssued()

        awaitScrollSettled(host, processed: 2)
        XCTAssertEqual(cv.contentOffset.y, initial, accuracy: 0.5, "非表示と未知の ID への命令で位置は変わらない")
    }

    // MARK: - Section への命令は見出しごと見せる

    func test_Sectionをstartで指すと見出しの上端が表示範囲の上端に来る() {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        let target = fixture.sections[9]

        handle.scrollToSection(id: target.id, animated: false)
        markIssued()

        awaitScrollSettled(host, processed: 1)
        XCTAssertEqual(headerFrame(host, sectionID: target.id)?.minY ?? .nan, visibleTop(cv), accuracy: 0.5)
    }

    func test_見出しの無いSectionは最初のCellが上端に来る() {
        let headless = Section(cells: [LabelCell(title: "Headless 0"), LabelCell(title: "Headless 1")])
        let fixture = Fixture(extraSections: [headless] + (0..<3).map { index in
            Section(header: .text("Tail \(index)"), cells: (0..<5).map { LabelCell(title: "Tail \(index)-\($0)") })
        })
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle

        handle.scrollToSection(id: headless.id, animated: false)
        markIssued()

        awaitScrollSettled(host, processed: 1)
        let firstCell = cellFrame(host, KsCellID(cell: headless.cells[0]))
        XCTAssertEqual(firstCell?.minY ?? .nan, visibleTop(cv), accuracy: 0.5, "最初の表示 Cell の上端が上端に来る")
    }

    func test_Sectionの範囲を下端と中央に合わせる() throws {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        let endTarget = fixture.sections[7]
        XCTAssertGreaterThan(headerFrame(host, sectionID: endTarget.id)?.minY ?? 0, visibleBottom(cv),
                             "前提: 対象は表示範囲の下方")

        handle.scrollToSection(id: endTarget.id, position: .end, animated: false)
        markIssued()

        awaitScrollSettled(host, processed: 1)
        XCTAssertEqual(footerFrame(host, sectionID: endTarget.id)?.maxY ?? .nan, visibleBottom(cv), accuracy: 0.5,
                       "Footer の下端が表示範囲の下端に来る")

        let centerTarget = fixture.sections[2]
        handle.scrollToSection(id: centerTarget.id, position: .center, animated: false)
        markIssued()

        awaitScrollSettled(host, processed: 2)
        let header = try XCTUnwrap(headerFrame(host, sectionID: centerTarget.id))
        let footer = try XCTUnwrap(footerFrame(host, sectionID: centerTarget.id))
        XCTAssertEqual((header.minY + footer.maxY) / 2, (visibleTop(cv) + visibleBottom(cv)) / 2, accuracy: 0.5,
                       "見出しの上端から Footer の下端までの範囲の中央が表示範囲の中央に来る")
    }

    func test_表示範囲より高いSectionは下端を指しても上端に合わせる() throws {
        let tall = Section(
            header: .text("Tall"),
            footer: .text("Tall footer"),
            cells: (0..<30).map { LabelCell(title: "Tall-\($0)") }
        )
        let fixture = Fixture(sectionCount: 4, extraSections: [tall] + (0..<4).map { index in
            Section(header: .text("Tail \(index)"), cells: (0..<5).map { LabelCell(title: "Tail \(index)-\($0)") })
        })
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle

        handle.scrollToSection(id: tall.id, position: .end, animated: false)
        markIssued()

        awaitScrollSettled(host, processed: 1)
        let header = try XCTUnwrap(headerFrame(host, sectionID: tall.id))
        let footer = try XCTUnwrap(footerFrame(host, sectionID: tall.id))
        XCTAssertGreaterThan(footer.maxY - header.minY, visibleBottom(cv) - visibleTop(cv), "前提: Section は表示範囲より高い")
        XCTAssertEqual(header.minY, visibleTop(cv), accuracy: 0.5, "見出しの上端が表示範囲の上端に来る")
    }

    func test_非表示のSectionと表示される要素の無いSectionへの命令は位置を変えない() {
        let hiddenSection = Section(header: .text("Hidden"), cells: [LabelCell(title: "X")], isVisible: false)
        let emptySection = Section()
        let fixture = Fixture(extraSections: [hiddenSection, emptySection])
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        let initial = cv.contentOffset.y

        handle.scrollToSection(id: hiddenSection.id, animated: false)
        handle.scrollToSection(id: emptySection.id, animated: false)
        markIssued()

        awaitScrollSettled(host, processed: 2)
        XCTAssertEqual(cv.contentOffset.y, initial, accuracy: 0.5)
    }

    // MARK: - 先頭・末尾への命令は内容の両端へ送る

    func test_末尾への命令でRootFooterの下端まで見える() {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        host.rootFooter = .text("Root Footer")
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle

        handle.scrollToEnd(animated: false)
        markIssued()

        awaitScrollSettled(host, processed: 1)
        let footer = cv.layoutAttributesForSupplementaryElement(
            ofKind: KsSettingsViewController.rootFooterElementKind,
            at: IndexPath(index: 0)
        )
        XCTAssertNotNil(footer)
        XCTAssertEqual(footer?.frame.maxY ?? .nan, visibleBottom(cv), accuracy: 0.5, "Root Footer の下端が表示範囲の下端に来る")
    }

    func test_先頭への命令でRootHeaderの上端まで戻る() {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        host.rootHeader = .text("Root Header")
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        handle.scrollToEnd(animated: false)
        markIssued()
        awaitScrollSettled(host, processed: 1)

        handle.scrollToStart(animated: false)
        markIssued()

        awaitScrollSettled(host, processed: 2)
        let header = cv.layoutAttributesForSupplementaryElement(
            ofKind: KsSettingsViewController.rootHeaderElementKind,
            at: IndexPath(index: 0)
        )
        XCTAssertNotNil(header)
        XCTAssertEqual(header?.frame.minY ?? .nan, visibleTop(cv), accuracy: 0.5, "Root Header の上端が表示範囲の上端に来る")
    }

    // MARK: - アニメーションは一方向で着地する

    // アニメーション付きの命令は画面の更新ごとに 1 フレームずつ進む。テストでは画面の更新による
    // 駆動を止め、フレームの時刻を指定して 1 フレームずつ進めて、途中の位置と行き先の変化を観測する。

    /// アニメーション付きの命令を出し、処理されたところで画面の更新による駆動を止めて返す。
    private func beginManualAnimation(
        _ host: KsSettingsViewController,
        issue: () -> Void,
        file: StaticString = #filePath,
        line: UInt = #line
    ) throws -> KsActiveScrollAnimation {
        issue()
        markIssued()
        awaitCondition(
            "命令の処理",
            actual: { scrollCommandState(host) },
            file: file,
            line: line,
            until: { host.processedScrollCommandCount >= 1 }
        )
        host.scrollAnimationTicker?.stop()
        // 画面の更新で既に進んだフレームがあっても、以降はテストが渡す時刻を起点に数え直す。
        host.activeScrollAnimation?.startTime = nil
        return try XCTUnwrap(host.activeScrollAnimation, "アニメーション付きの命令は進行中として記録される",
                             file: file, line: line)
    }

    /// アニメーションの長さを `count` 等分したフレームを順に進め、各フレームの後のスクロール位置を返す。
    /// `through` を渡すとそのフレームで止める (アニメーションの途中で止めるため)。
    /// `beforeFrame` はフレームの番号を受け取り、そのフレームの前に表示を変えるために使う。
    private func stepFrames(
        _ host: KsSettingsViewController,
        count: Int,
        through: Int? = nil,
        beforeFrame: (Int) -> Void = { _ in }
    ) -> [CGFloat] {
        let cv = host.internalCollectionView
        let duration = KsSettingsViewController.scrollAnimationDuration
        var samples: [CGFloat] = [cv.contentOffset.y]
        let origin: CFTimeInterval = 1000
        for frame in 0...(through ?? count) {
            beforeFrame(frame)
            // 最後のフレームは長さちょうどの時刻に丸め誤差が乗っても終わるよう、わずかに後ろへずらす。
            let slack: CFTimeInterval = frame == count ? 0.001 : 0
            host.stepScrollAnimation(at: origin + duration * Double(frame) / Double(count) + slack)
            samples.append(cv.contentOffset.y)
        }
        return samples
    }

    private func assertOneWay(
        _ samples: [CGFloat],
        direction: CGFloat,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        for (previous, next) in zip(samples, samples.dropFirst()) {
            XCTAssertGreaterThanOrEqual((next - previous) * direction, -0.5,
                                        "進む向きにだけ動く (\(previous) → \(next))", file: file, line: line)
        }
    }

    func test_中央合わせのアニメーションは下方向にだけ進み中央に止まる() throws {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        let target = fixture.cellID(section: 8, item: 2)

        let active = try beginManualAnimation(host) {
            handle.scrollTo(id: target, position: .center, animated: true)
        }
        XCTAssertEqual(active.direction, 1, "対象は下方にあるので下向きに進む")
        let samples = stepFrames(host, count: 24)

        XCTAssertGreaterThan(Set(samples).count, 5, "前提: 途中の位置を経て進む")
        assertOneWay(samples, direction: 1)
        XCTAssertNil(host.activeScrollAnimation, "時間が尽きたら終える")
        let visibleCenter = (visibleTop(cv) + visibleBottom(cv)) / 2
        XCTAssertEqual(cellFrame(host, target)?.midY ?? .nan, visibleCenter, accuracy: 0.5, "行の中央が表示範囲の中央に止まる")
    }

    func test_スクロール中に表示範囲が伸びても末尾を越えずに一方向で止まる() throws {
        // 大きいタイトルの縮小で、スクロール中に一覧の表示範囲が伸びる (スクロール可能範囲が縮む) 状況。
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host, size: CGSize(width: 375, height: 548))
        let handle = KsScrollController()
        host.scrollController = handle
        let active = try beginManualAnimation(host) { handle.scrollToEnd(animated: true) }
        let issuedMax = maxOffset(cv)
        XCTAssertEqual(active.destination, issuedMax, accuracy: 0.5, "前提: 命令の時点の末尾へ向かう")

        let samples = stepFrames(host, count: 24) { frame in
            guard frame == 4 else { return }
            let window = try? XCTUnwrap(host.view.window)
            window?.frame = CGRect(x: 0, y: 0, width: 375, height: 600)
            host.view.frame = CGRect(x: 0, y: 0, width: 375, height: 600)
            layoutNow(host.view)
        }

        XCTAssertLessThan(maxOffset(cv), issuedMax - 40, "前提: スクロール中にスクロール可能範囲が縮んだ")
        assertOneWay(samples, direction: 1)
        XCTAssertEqual(cv.contentOffset.y, maxOffset(cv), accuracy: 0.5, "縮んだ後の末尾で止まり、越えない")
        XCTAssertLessThanOrEqual(samples.max() ?? 0, maxOffset(cv) + 0.5, "途中でも末尾を越えない")
    }

    func test_スクロール中に内容が短くなっても末尾を越えずに一方向で止まる() throws {
        // 推定より低い行が実測で確定して内容が短くなる状況を、末尾の Section の行を減らして作る。
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        _ = try beginManualAnimation(host) { handle.scrollToEnd(animated: true) }
        let issuedMax = maxOffset(cv)

        let samples = stepFrames(host, count: 24) { frame in
            guard frame == 6 else { return }
            for item in 1..<5 {
                fixture.store.removeCell(cellID: fixture.cellID(section: 11, item: item))
            }
            layoutNow(host.view)
        }

        XCTAssertLessThan(maxOffset(cv), issuedMax - 40, "前提: スクロール中に内容が短くなった")
        assertOneWay(samples, direction: 1)
        XCTAssertEqual(cv.contentOffset.y, maxOffset(cv), accuracy: 0.5, "短くなった後の末尾で止まり、越えない")
    }

    func test_アニメーション中の命令は先行のアニメーションを止める() throws {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        _ = try beginManualAnimation(host) { handle.scrollToEnd(animated: true) }
        _ = stepFrames(host, count: 24, through: 4)
        XCTAssertNotNil(host.activeScrollAnimation, "前提: 先行のアニメーションが進行中")
        let target = fixture.sections[3]

        handle.scrollToSection(id: target.id, animated: false)
        markIssued()

        awaitScrollSettled(host, processed: 2)
        XCTAssertNil(host.activeScrollAnimation, "先行のアニメーションは止まっている")
        XCTAssertEqual(headerFrame(host, sectionID: target.id)?.minY ?? .nan, visibleTop(cv), accuracy: 0.5)
    }

    func test_アニメーション中に出した対象の無い命令は先行のアニメーションを止めない() throws {
        let hidden = LabelCell(title: "Hidden", isVisible: false)
        let extra = Section(header: .text("Extra"), cells: [LabelCell(title: "Shown"), hidden])
        let hiddenSection = Section(header: .text("Hidden"), cells: [LabelCell(title: "X")], isVisible: false)
        let emptySection = Section()
        let fixture = Fixture(extraSections: [extra, hiddenSection, emptySection])
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        _ = try beginManualAnimation(host) { handle.scrollToEnd(animated: true) }
        _ = stepFrames(host, count: 24, through: 4)
        XCTAssertNotNil(host.activeScrollAnimation, "前提: 先行のアニメーションが進行中")
        XCTAssertLessThan(cv.contentOffset.y, maxOffset(cv) - 40, "前提: まだ末尾に着いていない")

        // 存在しない ID・非表示の Cell / Section・表示される要素の無い Section への命令。
        handle.scrollTo(id: KsCellID(id: UUID()), animated: false)
        handle.scrollTo(id: KsCellID(cell: hidden), animated: false)
        handle.scrollToSection(id: hiddenSection.id, animated: false)
        handle.scrollToSection(id: emptySection.id, animated: false)
        handle.scrollToSection(id: UUID(), animated: true)
        markIssued()

        awaitCondition(
            "対象の無い命令の処理",
            actual: { scrollCommandState(host) },
            until: { host.processedScrollCommandCount >= 6 }
        )
        XCTAssertNotNil(host.activeScrollAnimation, "対象の無い命令では先行のアニメーションは止まらない")

        host.stepScrollAnimation(at: 1000 + KsSettingsViewController.scrollAnimationDuration + 0.001)

        XCTAssertNil(host.activeScrollAnimation, "時間が尽きたら終える")
        XCTAssertEqual(cv.contentOffset.y, maxOffset(cv), accuracy: 0.5, "先行の命令の行き先 (末尾) に着く")
    }

    func test_ドラッグを始めるとアニメーションを止める() throws {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        _ = try beginManualAnimation(host) { handle.scrollToEnd(animated: true) }
        _ = stepFrames(host, count: 24, through: 4)
        XCTAssertNotNil(host.activeScrollAnimation, "前提: アニメーションが進行中")

        host.scrollViewWillBeginDragging(cv)

        XCTAssertNil(host.activeScrollAnimation, "利用者の操作を優先して止める")
    }

    func test_アニメーションなしの命令は即座に最終位置へ移る() {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        let target = fixture.sections[10]
        var samples: [CGFloat] = []
        let observation = cv.observe(\.contentOffset, options: [.new]) { _, change in
            if let value = change.newValue?.y {
                MainActor.assumeIsolated { samples.append(value) }
            }
        }
        defer { observation.invalidate() }

        handle.scrollToSection(id: target.id, position: .start, animated: false)
        markIssued()

        awaitCondition(
            "命令の処理",
            actual: { scrollCommandState(host) },
            until: { host.processedScrollCommandCount >= 1 }
        )
        // 処理した時点 (次の表示の更新より前) で、見出しの上端が表示範囲の上端にある。
        XCTAssertNil(host.activeScrollAnimation, "アニメーションは起こさない")
        XCTAssertEqual(headerFrame(host, sectionID: target.id)?.minY ?? .nan, visibleTop(cv), accuracy: 0.5)
        XCTAssertLessThanOrEqual(samples.count, 4, "途中の位置を経ずに最終位置へ移る (送り直しの回数まで)")
    }

    // MARK: - 位置を控える・戻す窓口

    /// Host A で Cell を上端から 10pt 上へずらした位置に置き、その位置を控える。
    private func captureAnchorAtCell(
        _ fixture: Fixture,
        cellID: KsCellID
    ) -> KsScrollAnchor? {
        let hostA = KsSettingsViewController(store: fixture.store)
        let cvA = present(hostA)
        let handle = KsScrollController()
        hostA.scrollController = handle
        handle.scrollTo(id: cellID, animated: false)
        markIssued()
        awaitScrollSettled(hostA, processed: 1)
        cvA.setContentOffset(CGPoint(x: 0, y: cvA.contentOffset.y + 10), animated: false)
        layoutNow(cvA)
        let anchor = hostA.captureScrollAnchor()
        XCTAssertEqual(anchor?.element, .cell(cellID.id), "前提: 上端にかかる Cell が控えられる")
        XCTAssertEqual(anchor?.offsetFromTop ?? .nan, 10, accuracy: 0.5, "前提: 上端からのずれが控えられる")
        hostA.disconnectStore()
        return anchor
    }

    func test_控えた位置を新しいHostで戻す() throws {
        let fixture = Fixture()
        let target = fixture.cellID(section: 6, item: 3)
        let anchor = try XCTUnwrap(captureAnchorAtCell(fixture, cellID: target))

        let hostB = KsSettingsViewController(store: fixture.store)
        hostB.restoreScrollAnchor(anchor)
        markIssued()
        let cvB = present(hostB)

        awaitScrollSettled(hostB, processed: 1)
        XCTAssertEqual(cellFrame(hostB, target)?.minY ?? .nan, visibleTop(cvB) - 10, accuracy: 0.5,
                       "Host A で上端にかかっていた Cell が同じずれで上端にかかる")
    }

    func test_控えた後に上に項目が増えても同じ要素へ戻る() throws {
        let fixture = Fixture()
        let target = fixture.cellID(section: 6, item: 3)
        let anchor = try XCTUnwrap(captureAnchorAtCell(fixture, cellID: target))
        fixture.store.insertCell(LabelCell(title: "Inserted A"), in: fixture.sections[0].id, at: 0)
        fixture.store.insertCell(LabelCell(title: "Inserted B"), in: fixture.sections[3].id, at: 0)

        let hostB = KsSettingsViewController(store: fixture.store)
        hostB.restoreScrollAnchor(anchor)
        markIssued()
        let cvB = present(hostB)

        awaitScrollSettled(hostB, processed: 1)
        XCTAssertEqual(cellFrame(hostB, target)?.minY ?? .nan, visibleTop(cvB) - 10, accuracy: 0.5)
    }

    func test_控えた要素が消えていたら何もしない() throws {
        let fixture = Fixture()
        let target = fixture.cellID(section: 6, item: 3)
        let anchor = try XCTUnwrap(captureAnchorAtCell(fixture, cellID: target))
        fixture.store.removeCell(cellID: target)

        let hostB = KsSettingsViewController(store: fixture.store)
        hostB.restoreScrollAnchor(anchor)
        markIssued()
        let cvB = present(hostB)

        awaitScrollSettled(hostB, processed: 1)
        XCTAssertEqual(cvB.contentOffset.y, minOffset(cvB), accuracy: 0.5, "位置は内容の先頭のまま")
    }

    func test_戻した後に出した命令が最終位置を決める() throws {
        let fixture = Fixture()
        let target = fixture.cellID(section: 4, item: 1)
        let anchor = try XCTUnwrap(captureAnchorAtCell(fixture, cellID: target))

        let hostB = KsSettingsViewController(store: fixture.store)
        let handle = KsScrollController()
        hostB.scrollController = handle
        hostB.restoreScrollAnchor(anchor)
        handle.scrollToEnd(animated: false)
        markIssued()
        let cvB = present(hostB)

        awaitScrollSettled(hostB, processed: 2)
        XCTAssertEqual(cvB.contentOffset.y, maxOffset(cvB), accuracy: 0.5, "最終位置は内容の末尾")
    }

    func test_先頭で控えるとRootHeaderが控えられ新しいHostで先頭に戻る() throws {
        let fixture = Fixture()
        let hostA = KsSettingsViewController(store: fixture.store)
        hostA.rootHeader = .text("Root Header")
        present(hostA)
        let anchor = try XCTUnwrap(hostA.captureScrollAnchor())
        XCTAssertEqual(anchor.element, .rootHeader, "先頭では Root Header が上端にかかる")
        hostA.disconnectStore()

        let hostB = KsSettingsViewController(store: fixture.store)
        hostB.rootHeader = .text("Root Header")
        let handle = KsScrollController()
        hostB.scrollController = handle
        let cvB = present(hostB)
        handle.scrollToEnd(animated: false)
        markIssued()
        awaitScrollSettled(hostB, processed: 1)

        hostB.restoreScrollAnchor(anchor)
        markIssued()

        awaitScrollSettled(hostB, processed: 2)
        XCTAssertEqual(cvB.contentOffset.y, minOffset(cvB), accuracy: 0.5, "Root Header の上端が上端に戻る")
    }

    func test_Sectionの見出しとFooterを控えて新しいHostで戻す() throws {
        let fixture = Fixture()
        let target = fixture.sections[5]
        let hostA = KsSettingsViewController(store: fixture.store)
        let cvA = present(hostA)
        let handleA = KsScrollController()
        hostA.scrollController = handleA

        // 見出しが上端から 5pt 上にはみ出す位置で控える。
        handleA.scrollToSection(id: target.id, animated: false)
        markIssued()
        awaitScrollSettled(hostA, processed: 1)
        cvA.setContentOffset(CGPoint(x: 0, y: cvA.contentOffset.y + 5), animated: false)
        layoutNow(cvA)
        let headerAnchor = try XCTUnwrap(hostA.captureScrollAnchor())
        XCTAssertEqual(headerAnchor.element, .sectionHeader(target.id), "前提: 上端にかかる Section の見出しが控えられる")
        XCTAssertEqual(headerAnchor.offsetFromTop, 5, accuracy: 0.5)

        // Footer が上端から 3pt 上にはみ出す位置で控える。
        let footerA = try XCTUnwrap(footerFrame(hostA, sectionID: target.id))
        cvA.setContentOffset(CGPoint(x: 0, y: footerA.minY + 3 - cvA.adjustedContentInset.top), animated: false)
        layoutNow(cvA)
        let footerAnchor = try XCTUnwrap(hostA.captureScrollAnchor())
        XCTAssertEqual(footerAnchor.element, .sectionFooter(target.id), "前提: 上端にかかる Section の Footer が控えられる")
        XCTAssertEqual(footerAnchor.offsetFromTop, 3, accuracy: 0.5)
        hostA.disconnectStore()

        let hostB = KsSettingsViewController(store: fixture.store)
        hostB.restoreScrollAnchor(headerAnchor)
        markIssued()
        let cvB = present(hostB)

        awaitScrollSettled(hostB, processed: 1)
        XCTAssertEqual(headerFrame(hostB, sectionID: target.id)?.minY ?? .nan, visibleTop(cvB) - 5, accuracy: 0.5,
                       "Section の見出しが同じずれで上端にかかる")

        hostB.restoreScrollAnchor(footerAnchor)
        markIssued()

        awaitScrollSettled(hostB, processed: 2)
        XCTAssertEqual(footerFrame(hostB, sectionID: target.id)?.minY ?? .nan, visibleTop(cvB) - 3, accuracy: 0.5,
                       "Section の Footer が同じずれで上端にかかる")
    }

    func test_戻しの実行前に控えると未実行の復元の控えが返る() throws {
        let fixture = Fixture()
        let target = fixture.cellID(section: 6, item: 3)
        let anchor = try XCTUnwrap(captureAnchorAtCell(fixture, cellID: target))

        // 表示前の Host: 戻しを積んだ直後に控える (表示前のすばやい切断・再接続)。
        let unloaded = KsSettingsViewController(store: fixture.store)
        unloaded.restoreScrollAnchor(anchor)
        XCTAssertEqual(unloaded.captureScrollAnchor(), anchor, "表示前でも未実行の復元の控えが返る")

        // 表示済みの Host: 戻しの遅延 (main.async) を経る前に控える。
        let shown = KsSettingsViewController(store: fixture.store)
        present(shown)
        shown.restoreScrollAnchor(anchor)
        markIssued()
        XCTAssertEqual(shown.processedScrollCommandCount, 0, "前提: 戻しはまだ実行されていない")
        XCTAssertEqual(shown.captureScrollAnchor(), anchor, "現在の先頭側の位置ではなく未実行の復元の控えが返る")

        // 戻しが実行された後は、現在の表示位置を控える。
        awaitScrollSettled(shown, processed: 1)
        let afterRestore = try XCTUnwrap(shown.captureScrollAnchor())
        XCTAssertEqual(afterRestore.element, .cell(target.id))
        XCTAssertEqual(afterRestore.offsetFromTop, 10, accuracy: 0.5)
    }

    func test_windowから外れた後は控えずnilを返す() throws {
        let fixture = Fixture()
        let target = fixture.cellID(section: 6, item: 3)
        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        let handle = KsScrollController()
        host.scrollController = handle
        handle.scrollTo(id: target, animated: false)
        markIssued()
        awaitScrollSettled(host, processed: 1)
        XCTAssertNotNil(host.captureScrollAnchor(), "前提: window 上では控えられる")

        host.view.removeFromSuperview()
        layoutNow(host.view)

        XCTAssertNil(cv.window, "前提: window から外れている")
        XCTAssertNil(host.captureScrollAnchor(), "外れた後のレイアウトからは控えない")
    }

    func test_windowから外れていても未実行の復元があればその控えを返す() throws {
        let fixture = Fixture()
        let target = fixture.cellID(section: 6, item: 3)
        let anchor = try XCTUnwrap(captureAnchorAtCell(fixture, cellID: target))

        let host = KsSettingsViewController(store: fixture.store)
        let cv = present(host)
        host.view.removeFromSuperview()
        host.restoreScrollAnchor(anchor)

        XCTAssertNil(cv.window, "前提: window から外れている")
        XCTAssertEqual(host.processedScrollCommandCount, 0, "前提: 戻しはまだ実行されていない")
        XCTAssertEqual(host.captureScrollAnchor(), anchor, "外れていても未実行の復元の控えが返る")
    }

    func test_控えられる内容が無いときはnilを返す() {
        let host = KsSettingsViewController(store: SettingsRootStore(initialRoot: SettingsRoot()))
        XCTAssertNil(host.captureScrollAnchor(), "view 未読込では控えられない")
        present(host)
        XCTAssertNil(host.captureScrollAnchor(), "表示する要素が無ければ控えられない")
    }

    // MARK: - 戻しに連動して祖先の寸法が変わる画面

    /// 子の view の寸法を、自分の view の `viewDidLayoutSubviews` で自分の bounds に合わせる親の画面。
    /// 取り付けた画面の view は `contentView` の中に置き、`contentView` の `layoutSubviews` が
    /// 自分の bounds に合わせる。
    private final class BoundsFollowingContainerController: UIViewController {
        let contentView = BoundsFollowingView()

        override func viewDidLoad() {
            super.viewDidLoad()
            view.addSubview(contentView)
        }

        override func viewDidLayoutSubviews() {
            super.viewDidLayoutSubviews()
            contentView.frame = view.bounds
        }
    }

    /// 一覧が先頭から動いた時点で、親の画面の表示領域を 1 回だけ伸ばす
    /// (大きいタイトルのナビゲーションバーが縮む画面と同じ変化)。
    @MainActor
    private final class ExpandOnScroll {
        private(set) var didExpand = false
        private var observation: NSKeyValueObservation?

        init(scrollView: UIScrollView, expanding view: UIView, to frame: CGRect) {
            observation = scrollView.observe(\.contentOffset, options: [.new]) { [weak self, weak view] _, change in
                guard let offset = change.newValue, offset.y > 1 else { return }
                MainActor.assumeIsolated {
                    guard let self, let view, !self.didExpand else { return }
                    self.didExpand = true
                    view.frame = frame
                }
            }
        }

        func invalidate() {
            observation?.invalidate()
            observation = nil
        }
    }

    /// 最初の子の frame を自分の bounds に合わせる view。
    private final class BoundsFollowingView: UIView {
        override func layoutSubviews() {
            super.layoutSubviews()
            subviews.first?.frame = bounds
        }
    }

    /// 表示領域が伸びる親の画面に Host を取り付けた構成。
    private struct ExpandingContainerSetup {
        let container: BoundsFollowingContainerController
        let trigger: ExpandOnScroll
        let collectionView: UICollectionView
    }

    /// 親の画面の表示領域の、伸びる前と伸びた後の高さ。
    private static let collapsedContainerHeight: CGFloat = 500
    private static let expandedContainerHeight: CGFloat = 552

    /// 積んだ命令 (戻しを含む) の最初の反映と 1 回の遅延を window の外で済ませてから、一覧が先頭から
    /// 動いた時点で表示領域が伸びる親の画面 (大きいタイトルのナビゲーションバーが縮む画面と同じ変化)
    /// に Host を取り付け、window を表示する。積んだ命令は、取り付けた後の最初のレイアウトの中で
    /// 実行される。
    private func attachToExpandingContainer(
        _ host: KsSettingsViewController,
        queuedEntries: Int = 1,
        file: StaticString = #filePath,
        line: UInt = #line
    ) -> ExpandingContainerSetup {
        awaitCondition(
            "window の外での最初の反映と命令の遅延",
            actual: {
                "applied=\(host.hasAppliedInitialSnapshot) incoming=\(host.incomingScrollEntries.count) ready=\(host.readyScrollEntries.count)"
            },
            file: file,
            line: line,
            until: {
                host.hasAppliedInitialSnapshot && host.incomingScrollEntries.isEmpty
                    && host.readyScrollEntries.count == queuedEntries
            }
        )

        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 375, height: 700))
        let root = UIViewController()
        window.rootViewController = root
        windows.append(window)
        let container = BoundsFollowingContainerController()
        root.addChild(container)
        root.view.addSubview(container.view)
        container.view.frame = CGRect(
            x: 0, y: 700 - Self.collapsedContainerHeight, width: 375, height: Self.collapsedContainerHeight
        )
        container.didMove(toParent: root)
        container.addChild(host)
        container.contentView.addSubview(host.view)
        host.didMove(toParent: container)

        let cv = host.internalCollectionView
        let trigger = ExpandOnScroll(
            scrollView: cv,
            expanding: container.view,
            to: CGRect(x: 0, y: 700 - Self.expandedContainerHeight, width: 375, height: Self.expandedContainerHeight)
        )

        window.makeKeyAndVisible()
        window.layoutIfNeeded()
        return ExpandingContainerSetup(container: container, trigger: trigger, collectionView: cv)
    }

    /// 命令が指定件数まで処理され、それに連動して伸びた親の表示領域に Host の領域が追従し、
    /// 祖先の再レイアウトと命令のアニメーションが済むまで待つ。
    ///
    /// 祖先のレイアウトは外から求めずに待つ (外から求めると、Host が求めなくても追従してしまう)。
    private func awaitExpandedAndSettled(
        _ host: KsSettingsViewController,
        _ setup: ExpandingContainerSetup,
        processed count: Int,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        awaitCondition(
            "命令 \(count) 件の実行と Host の領域の追従",
            actual: {
                "expanded=\(setup.trigger.didExpand) container=\(setup.container.view.bounds.height) content=\(setup.container.contentView.frame.height) host=\(host.view.frame.height) relayoutScheduled=\(host.isAncestorRelayoutScheduled) / "
                    + scrollCommandState(host)
            },
            file: file,
            line: line,
            until: {
                host.processedScrollCommandCount >= count && setup.trigger.didExpand
                    && host.view.frame.height == Self.expandedContainerHeight
                    && !host.isAncestorRelayoutScheduled && host.activeScrollAnimation == nil
            }
        )
    }

    /// 読み込んだ Host にハンドルをつなぎ、`issue` で命令を積んでから、表示領域が伸びる親の画面に
    /// 取り付けて、命令の実行と領域の追従を待つ。
    private func attachWithCommand(
        _ fixture: Fixture,
        file: StaticString = #filePath,
        line: UInt = #line,
        issue: (KsScrollController) -> Void
    ) -> (host: KsSettingsViewController, setup: ExpandingContainerSetup) {
        let host = KsSettingsViewController(store: fixture.store)
        let handle = KsScrollController()
        host.scrollController = handle
        host.loadViewIfNeeded()
        issue(handle)
        markIssued()
        let setup = attachToExpandingContainer(host, file: file, line: line)
        awaitExpandedAndSettled(host, setup, processed: 1, file: file, line: line)
        return (host, setup)
    }

    func test_戻しに連動して祖先の表示領域が伸びてもHostの領域が追従し戻した位置を保つ() throws {
        let fixture = Fixture()
        let target = fixture.cellID(section: 6, item: 3)
        let anchor = try XCTUnwrap(captureAnchorAtCell(fixture, cellID: target))

        let host = KsSettingsViewController(store: fixture.store)
        host.loadViewIfNeeded()
        host.restoreScrollAnchor(anchor)
        markIssued()
        let setup = attachToExpandingContainer(host)
        defer { setup.trigger.invalidate() }

        awaitExpandedAndSettled(host, setup, processed: 1)
        let cv = setup.collectionView
        XCTAssertEqual(setup.container.contentView.frame.height, Self.expandedContainerHeight,
                       "親の子の view が伸びた領域に合う")
        XCTAssertEqual(cellFrame(host, target)?.minY ?? .nan, visibleTop(cv) - 10, accuracy: 0.5,
                       "領域が伸びた後も、控えた Cell が同じずれで上端にかかる")
    }

    // 取り付け後の最初のレイアウトで実行した命令は、祖先の表示領域が伸びた後の新しい表示範囲で
    // 位置を解き直す (上端の要素を保つだけでは、中央・下端の合わせがずれる)。

    func test_中央に合わせたCellは祖先の表示領域が伸びた後も表示範囲の中央に来る() {
        let fixture = Fixture()
        let target = fixture.cellID(section: 6, item: 3)
        let (host, setup) = attachWithCommand(fixture) { $0.scrollTo(id: target, position: .center, animated: false) }
        defer { setup.trigger.invalidate() }

        let cv = setup.collectionView
        let visibleCenter = (visibleTop(cv) + visibleBottom(cv)) / 2
        XCTAssertEqual(cellFrame(host, target)?.midY ?? .nan, visibleCenter, accuracy: 0.5,
                       "伸びた後の表示範囲の中央に行の中央が来る")
    }

    func test_下端に合わせたCellは祖先の表示領域が伸びた後も表示範囲の下端に来る() {
        let fixture = Fixture()
        let target = fixture.cellID(section: 6, item: 3)
        let (host, setup) = attachWithCommand(fixture) { $0.scrollTo(id: target, position: .end, animated: false) }
        defer { setup.trigger.invalidate() }

        let cv = setup.collectionView
        XCTAssertEqual(cellFrame(host, target)?.maxY ?? .nan, visibleBottom(cv), accuracy: 0.5,
                       "伸びた後の表示範囲の下端に行の下端が来る")
    }

    func test_中央に合わせたSectionは祖先の表示領域が伸びた後も表示範囲の中央に来る() throws {
        let fixture = Fixture()
        let target = fixture.sections[6]
        let (host, setup) = attachWithCommand(fixture) {
            $0.scrollToSection(id: target.id, position: .center, animated: false)
        }
        defer { setup.trigger.invalidate() }

        let cv = setup.collectionView
        let header = try XCTUnwrap(headerFrame(host, sectionID: target.id))
        let footer = try XCTUnwrap(footerFrame(host, sectionID: target.id))
        XCTAssertEqual((header.minY + footer.maxY) / 2, (visibleTop(cv) + visibleBottom(cv)) / 2, accuracy: 0.5,
                       "伸びた後の表示範囲の中央に Section の範囲の中央が来る")
    }

    func test_下端に合わせたSectionは祖先の表示領域が伸びた後も表示範囲の下端に来る() {
        let fixture = Fixture()
        let target = fixture.sections[6]
        let (host, setup) = attachWithCommand(fixture) {
            $0.scrollToSection(id: target.id, position: .end, animated: false)
        }
        defer { setup.trigger.invalidate() }

        let cv = setup.collectionView
        XCTAssertEqual(footerFrame(host, sectionID: target.id)?.maxY ?? .nan, visibleBottom(cv), accuracy: 0.5,
                       "伸びた後の表示範囲の下端に Footer の下端が来る")
    }

    func test_末尾への命令は祖先の表示領域が伸びた後も末尾に留まる() {
        let fixture = Fixture()
        let (_, setup) = attachWithCommand(fixture) { $0.scrollToEnd(animated: false) }
        defer { setup.trigger.invalidate() }

        let cv = setup.collectionView
        XCTAssertEqual(cv.contentOffset.y, maxOffset(cv), accuracy: 0.5, "伸びた後のスクロール可能範囲の末尾に留まる")
    }

    // 祖先の再レイアウトの後に位置を解き直さない条件。

    func test_再レイアウトの前に届いた命令があるときは再レイアウトの後に位置を解き直さない() throws {
        let fixture = Fixture()
        let target = fixture.cellID(section: 6, item: 3)
        let host = KsSettingsViewController(store: fixture.store)
        let handle = KsScrollController()
        host.scrollController = handle
        host.loadViewIfNeeded()
        handle.scrollTo(id: target, position: .center, animated: false)
        let setup = attachToExpandingContainer(host)
        defer { setup.trigger.invalidate() }
        let cv = setup.collectionView
        XCTAssertEqual(host.processedScrollCommandCount, 1, "前提: 最初のレイアウトの中で命令を実行した")
        XCTAssertTrue(host.isAncestorRelayoutScheduled, "前提: 祖先の再レイアウトが予約されている")
        XCTAssertTrue(setup.trigger.didExpand, "前提: 命令のスクロールで親の表示領域が伸びた")

        // 再レイアウトより前に、アニメーション付きの次の命令を出す。次の命令のアニメーションの出発点は、
        // 再レイアウトの後に位置を解き直していれば、解き直した位置になる。
        let offsetAtIssue = cv.contentOffset.y
        let next = fixture.sections[2]
        handle.scrollToSection(id: next.id, animated: true)
        markIssued()

        awaitCondition(
            "次の命令の実行",
            actual: { scrollCommandState(host) },
            until: { host.processedScrollCommandCount >= 2 }
        )
        let active = try XCTUnwrap(host.activeScrollAnimation, "前提: 次の命令のアニメーションが進行中")
        XCTAssertEqual(active.startOffset, offsetAtIssue, accuracy: 0.5,
                       "次の命令は、再レイアウトの後に位置を解き直して動かしていない位置から始まる")

        awaitExpandedAndSettled(host, setup, processed: 2)
        XCTAssertEqual(headerFrame(host, sectionID: next.id)?.minY ?? .nan, visibleTop(cv), accuracy: 0.5,
                       "最終位置は次の命令の位置になる")
    }

    func test_最初のレイアウトで始めた命令のアニメーションは祖先の再レイアウトで打ち切られない() {
        let fixture = Fixture()
        let host = KsSettingsViewController(store: fixture.store)
        let handle = KsScrollController()
        host.scrollController = handle
        host.loadViewIfNeeded()
        handle.scrollToEnd(animated: true)
        markIssued()
        let cv = host.internalCollectionView
        var samples: [CGFloat] = []
        let observation = cv.observe(\.contentOffset, options: [.new]) { _, change in
            guard let value = change.newValue?.y else { return }
            MainActor.assumeIsolated { samples.append(value) }
        }
        defer { observation.invalidate() }

        let setup = attachToExpandingContainer(host)
        defer { setup.trigger.invalidate() }
        XCTAssertNotNil(host.activeScrollAnimation, "前提: 最初のレイアウトの中でアニメーションを始めた")
        XCTAssertTrue(host.isAncestorRelayoutScheduled, "前提: 祖先の再レイアウトが予約されている")

        awaitExpandedAndSettled(host, setup, processed: 1)
        let start = minOffset(cv)
        let end = maxOffset(cv)
        XCTAssertGreaterThan(end - start, 1000, "前提: 末尾は先頭から十分に離れている")
        // 行き先の手前 (残り 100pt より前) の途中の位置を、いくつも経ていること。
        let intermediate = samples.filter { $0 > start + 1 && $0 < end - 100 }
        XCTAssertGreaterThanOrEqual(intermediate.count, 3,
                                    "途中の位置を経て進む (再レイアウトで打ち切られて末尾へ飛んでいない): \(samples)")
        XCTAssertEqual(cv.contentOffset.y, end, accuracy: 0.5, "伸びた後の末尾に着く")
    }
}
#endif
