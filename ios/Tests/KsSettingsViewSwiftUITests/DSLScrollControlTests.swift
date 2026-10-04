// DSLScrollControlTests.swift
// KsSettingsViewSwiftUITests
//
// SwiftUI の `KsSettingsView` に `.scrollController(_:)` で渡したハンドルの命令が、内部の Host へ
// 届くことを検証する。DSL 方式では、明示 ID (`.cellID(_:)` / `.sectionID(_:)`) と `ForEach` の key
// から宣言ツリーの最終 ID へ引き直して実行する。Store 方式では Store の ID をそのまま使う。
//
// ここで確かめるのは ID の引き直しと到達した位置で、アニメーションの有無によらない。
// アニメーションの経過を待たずに済むよう、アニメーションなしの命令で確かめる。

#if canImport(UIKit)
import XCTest
import UIKit
import SwiftUI
import KsSettingsViewTestSupport
@testable import KsSettingsViewSwiftUI
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class DSLScrollControlTests: XCTestCase {

    // MARK: - 宣言

    /// 明示 ID の Cell を下方に置いた DSL。
    private struct ExplicitIDView: SwiftUI.View {
        let controller: KsScrollController

        var body: some SwiftUI.View {
            KsSettingsView {
                ForEach(0..<8, id: \.self) { sectionIndex in
                    Section("Section \(sectionIndex)") {
                        LabelCell(title: "Row \(sectionIndex)-0")
                        LabelCell(title: "Row \(sectionIndex)-1")
                        LabelCell(title: "Row \(sectionIndex)-2")
                    }
                }
                Section("Network") {
                    LabelCell(title: "Wi-Fi").cellID("wifi")
                    LabelCell(title: "Bluetooth")
                }
                ForEach(100..<104, id: \.self) { sectionIndex in
                    Section("Tail \(sectionIndex)") {
                        LabelCell(title: "Tail \(sectionIndex)-0")
                        LabelCell(title: "Tail \(sectionIndex)-1")
                        LabelCell(title: "Tail \(sectionIndex)-2")
                    }
                }
            }
            .scrollController(controller)
        }
    }

    /// `ForEach` の key で Section を作る DSL。
    private struct ForEachKeyView: SwiftUI.View {
        let controller: KsScrollController

        var body: some SwiftUI.View {
            KsSettingsView {
                ForEach(30..<50, id: \.self) { key in
                    Section("Item \(key)") {
                        LabelCell(title: "Item \(key)-0")
                        LabelCell(title: "Item \(key)-1")
                        LabelCell(title: "Item \(key)-2")
                    }
                }
            }
            .scrollController(controller)
        }
    }

    /// 同じ値 "a" を、上方の `ForEach` の key (Cell Y) と下方の明示 ID (Cell X) の両方に使う DSL。
    private struct CollidingIDView: SwiftUI.View {
        let controller: KsScrollController

        var body: some SwiftUI.View {
            KsSettingsView {
                Section("Keyed") {
                    ForEach(["a", "b"], id: \.self) { key in
                        LabelCell(title: "Keyed \(key)")
                    }
                }
                ForEach(0..<8, id: \.self) { sectionIndex in
                    Section("Filler \(sectionIndex)") {
                        LabelCell(title: "Filler \(sectionIndex)-0")
                        LabelCell(title: "Filler \(sectionIndex)-1")
                        LabelCell(title: "Filler \(sectionIndex)-2")
                    }
                }
                Section("Explicit") {
                    LabelCell(title: "Explicit a").cellID("a")
                }
                ForEach(100..<104, id: \.self) { sectionIndex in
                    Section("Tail \(sectionIndex)") {
                        LabelCell(title: "Tail \(sectionIndex)-0")
                        LabelCell(title: "Tail \(sectionIndex)-1")
                        LabelCell(title: "Tail \(sectionIndex)-2")
                    }
                }
            }
            .scrollController(controller)
        }
    }

    /// 配列に項目を足し、同じ処理で追加した項目へ送る DSL。処理は `register` で外へ渡す。
    private struct AppendAndScrollView: SwiftUI.View {
        @State private var items: [String] = ["first", "second"]
        let controller: KsScrollController
        let register: (@escaping () -> Void) -> Void

        var body: some SwiftUI.View {
            KsSettingsView {
                ForEach(0..<8, id: \.self) { sectionIndex in
                    Section("Section \(sectionIndex)") {
                        LabelCell(title: "Row \(sectionIndex)-0")
                        LabelCell(title: "Row \(sectionIndex)-1")
                        LabelCell(title: "Row \(sectionIndex)-2")
                    }
                }
                Section("Items") {
                    ForEach(items, id: \.self) { item in
                        LabelCell(title: item)
                    }
                }
                ForEach(100..<104, id: \.self) { sectionIndex in
                    Section("Tail \(sectionIndex)") {
                        LabelCell(title: "Tail \(sectionIndex)-0")
                        LabelCell(title: "Tail \(sectionIndex)-1")
                        LabelCell(title: "Tail \(sectionIndex)-2")
                    }
                }
            }
            .scrollController(controller)
            .onAppear { register(appendAndScroll) }
        }

        private func appendAndScroll() {
            items.append("new")
            controller.scrollTo(id: "new", animated: false)
        }
    }

    /// 渡すハンドルを状態で差し替えられる DSL。差し替えの処理は `register` で外へ渡す。
    private struct SwappableControllerView: SwiftUI.View {
        @State private var controller: KsScrollController?
        let register: (@escaping (KsScrollController?) -> Void) -> Void

        init(initial: KsScrollController, register: @escaping (@escaping (KsScrollController?) -> Void) -> Void) {
            _controller = State(initialValue: initial)
            self.register = register
        }

        var body: some SwiftUI.View {
            settings.onAppear { register { controller = $0 } }
        }

        /// ハンドルが無いときは修飾子を付けない。どちらも同じ `KsSettingsView` の値なので、差し替えで
        /// 画面の同一性は変わらない。
        private var settings: KsSettingsView {
            let view = KsSettingsView {
                ForEach(0..<12, id: \.self) { sectionIndex in
                    Section("Section \(sectionIndex)") {
                        LabelCell(title: "Row \(sectionIndex)-0")
                        LabelCell(title: "Row \(sectionIndex)-1")
                        LabelCell(title: "Row \(sectionIndex)-2")
                    }
                }
            }
            guard let controller else { return view }
            return view.scrollController(controller)
        }
    }

    private var window: UIWindow?

    override func tearDown() {
        window?.isHidden = true
        window = nil
        super.tearDown()
    }

    // MARK: - ヘルパ

    /// SwiftUI の View を window に載せ、内部の Host の最初の反映を待って返す。
    private func host<Content: SwiftUI.View>(_ content: Content) throws -> (UIViewController, KsSettingsViewController) {
        let hosting = UIHostingController(rootView: content)
        let size = CGSize(width: 375, height: 600)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = hosting
        window.makeKeyAndVisible()
        self.window = window
        hosting.view.frame = CGRect(origin: .zero, size: size)
        layoutNow(hosting.view)
        let controller = try XCTUnwrap(
            awaitNonNil("SwiftUI 配下の KsSettingsViewController", in: hosting.view) {
                self.findSettingsController(in: hosting)
            }
        )
        awaitCondition(
            "最初の snapshot の反映",
            in: hosting.view,
            actual: { "applied=\(controller.hasAppliedInitialSnapshot) applying=\(controller.applyingSnapshotCount)" },
            until: { controller.hasAppliedInitialSnapshot && controller.applyingSnapshotCount == 0 }
        )
        return (hosting, controller)
    }

    private func awaitProcessed(
        _ controller: KsSettingsViewController,
        _ count: Int,
        in view: UIView,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        awaitCondition(
            "命令 \(count) 件の処理",
            in: view,
            actual: { scrollCommandState(controller) },
            file: file,
            line: line,
            until: { controller.processedScrollCommandCount >= count && controller.activeScrollAnimation == nil }
        )
    }

    // MARK: - 命令の処理を待つ待機の失敗報告

    /// 命令を出した時点の記録。
    @MainActor
    private final class IssuedCommand {
        /// 命令を出したハンドル。受け口の状態を読むために持つ。
        let handle: KsScrollController
        let issuedAt = DispatchTime.now()
        /// 命令の直後に積んだ `main.async` が回ったか。
        var mainQueueRan = false

        init(handle: KsScrollController) {
            self.handle = handle
        }
    }

    /// 直近に出した命令の記録。次の命令を記録するまで残る。
    private var lastIssuedCommand: IssuedCommand?

    /// 命令を出した直後に呼び、命令からの経過時間と main キューの消化を失敗報告に載せられるようにする。
    private func markIssued(_ handle: KsScrollController) {
        let issued = IssuedCommand(handle: handle)
        lastIssuedCommand = issued
        DispatchQueue.main.async { issued.mainQueueRan = true }
    }

    /// 命令の処理を待つ待機が deadline を超えたときに載せる観測値。
    ///
    /// 命令が止まった位置を切り分けるため、受け口 (ID の引き直し)・Host の待ち行列・実行の条件・
    /// main キューの消化をまとめて出す。
    private func scrollCommandState(_ controller: KsSettingsViewController) -> String {
        let cv: UICollectionView? = controller.isViewLoaded ? controller.collectionView : nil
        let queue = "待ち行列: 遅延前=\(controller.incomingScrollEntries.count)"
            + " 遅延後=\(controller.readyScrollEntries.count)"
            + " 遅延の予約中=\(controller.isScrollDeferralScheduled)"
        let conditions = "実行の条件: 最初の反映済み=\(controller.hasAppliedInitialSnapshot)"
            + " 適用中の snapshot=\(controller.applyingSnapshotCount)"
            + " window=\(cv?.window != nil)"
            + " 高さ=\(cv.map { "\($0.bounds.height)" } ?? "view 未読込")"
        let animation = "アニメーション中=\(controller.activeScrollAnimation != nil)"
        return [
            "processed=\(controller.processedScrollCommandCount)",
            receiverState(controller),
            queue,
            conditions,
            animation,
            issueState(),
        ].joined(separator: " / ")
    }

    /// ハンドルの接続先の状態。DSL 方式では引き直しの受け口、Store 方式では Host へ直接つながる。
    private func receiverState(_ controller: KsSettingsViewController) -> String {
        guard let handle = lastIssuedCommand?.handle else { return "受け口: 命令の記録なし" }
        guard let receiver = handle.currentReceiver else { return "受け口: 未接続" }
        if receiver === controller { return "受け口: Host へ直接接続" }
        guard let resolver = receiver as? DSLScrollCommandResolver else {
            return "受け口: 想定外の接続先 \(type(of: receiver))"
        }
        let resolved = resolver.lastResolvedGenerations.map { "世代 \($0.received) → \($0.resolved)" } ?? "未実施"
        return "受け口: 引き直し=\(resolved)"
            + " 宣言ツリーの世代=\(resolver.treeGeneration)"
            + " 受け口の Host が同一=\(resolver.host === controller)"
    }

    /// 命令を出してからの経過時間と、命令の直後に積んだ `main.async` が回ったか。
    private func issueState() -> String {
        guard let issued = lastIssuedCommand else { return "命令の記録なし" }
        let seconds = Double(DispatchTime.now().uptimeNanoseconds - issued.issuedAt.uptimeNanoseconds) / 1_000_000_000
        return "命令から \(String(format: "%.3f", seconds)) 秒"
            + " 命令の直後の main.async=\(issued.mainQueueRan ? "回った" : "未実行")"
    }

    private func findSettingsController(in parent: UIViewController) -> KsSettingsViewController? {
        if let found = parent as? KsSettingsViewController { return found }
        for child in parent.children {
            if let found = findSettingsController(in: child) { return found }
        }
        return nil
    }

    private func visibleTop(_ cv: UICollectionView) -> CGFloat {
        cv.contentOffset.y + cv.adjustedContentInset.top
    }

    private func visibleCenter(_ cv: UICollectionView) -> CGFloat {
        let inset = cv.adjustedContentInset
        return cv.contentOffset.y + inset.top + (cv.bounds.height - inset.top - inset.bottom) / 2
    }

    /// 表示中の Host から、指定タイトルの LabelCell の行の frame を引く。
    private func rowFrame(_ controller: KsSettingsViewController, title: String) -> CGRect? {
        for section in controller.visibleSections {
            for cell in section.cells {
                guard let label = cell as? LabelCell, label.title == title else { continue }
                guard let indexPath = controller.internalDataSource?.indexPath(for: KsCellID(cell: cell)) else {
                    return nil
                }
                return controller.internalCollectionView.layoutAttributesForItem(at: indexPath)?.frame
            }
        }
        return nil
    }

    /// 表示中の Host から、指定見出しの Section の見出しの frame を引く。
    private func headerFrame(_ controller: KsSettingsViewController, title: String) -> CGRect? {
        guard let index = controller.visibleSections.firstIndex(where: { $0.header == .text(title) }) else {
            return nil
        }
        return controller.internalCollectionView.layoutAttributesForSupplementaryElement(
            ofKind: UICollectionView.elementKindSectionHeader,
            at: IndexPath(item: 0, section: index)
        )?.frame
    }

    // MARK: - SwiftUI の修飾子と宣言 UI での指し方

    func test_明示IDでCellを指せる() throws {
        let scrollController = KsScrollController()
        let (hosting, controller) = try host(ExplicitIDView(controller: scrollController))
        let cv = controller.internalCollectionView

        scrollController.scrollTo(id: "wifi", position: .center, animated: false)
        markIssued(scrollController)

        awaitProcessed(controller, 1, in: hosting.view)
        XCTAssertEqual(rowFrame(controller, title: "Wi-Fi")?.midY ?? .nan, visibleCenter(cv), accuracy: 0.5,
                       "明示 ID の Cell の行の中央が表示範囲の中央に来る")
    }

    func test_ForEachのkeyでSectionを指せる() throws {
        let scrollController = KsScrollController()
        let (hosting, controller) = try host(ForEachKeyView(controller: scrollController))
        let cv = controller.internalCollectionView

        scrollController.scrollToSection(id: 42, animated: false)
        markIssued(scrollController)

        awaitProcessed(controller, 1, in: hosting.view)
        XCTAssertEqual(headerFrame(controller, title: "Item 42")?.minY ?? .nan, visibleTop(cv), accuracy: 0.5,
                       "key 42 の Section の見出しの上端が表示範囲の上端に来る")
    }

    func test_同じ値が明示IDとkeyの両方にあるときは明示IDの要素を採る() throws {
        let scrollController = KsScrollController()
        let (hosting, controller) = try host(CollidingIDView(controller: scrollController))
        let cv = controller.internalCollectionView

        scrollController.scrollTo(id: "a", animated: false)
        markIssued(scrollController)

        awaitProcessed(controller, 1, in: hosting.view)
        XCTAssertEqual(rowFrame(controller, title: "Explicit a")?.minY ?? .nan, visibleTop(cv), accuracy: 0.5,
                       "明示 ID の Cell X が表示範囲の上端に来る")
        XCTAssertLessThan(rowFrame(controller, title: "Keyed a")?.maxY ?? 0, visibleTop(cv),
                          "key の Cell Y は上方に外れている")
    }

    func test_どちらにも無い値への命令は何もしない() throws {
        let scrollController = KsScrollController()
        let (hosting, controller) = try host(ExplicitIDView(controller: scrollController))
        let cv = controller.internalCollectionView
        let initial = cv.contentOffset.y

        scrollController.scrollTo(id: "missing", animated: false)
        scrollController.scrollToSection(id: "missing", animated: false)
        // 解決できない命令は Host へ渡らない。後続の命令が処理されたことを完了の合図にする。
        scrollController.scrollToStart(animated: false)
        markIssued(scrollController)

        awaitProcessed(controller, 1, in: hosting.view)
        XCTAssertEqual(controller.processedScrollCommandCount, 1, "解決できない 2 件は Host へ届かない")
        XCTAssertEqual(cv.contentOffset.y, initial, accuracy: 0.5)
    }

    func test_状態の変更と同じ処理から出した命令は新しいツリーで解決される() throws {
        let scrollController = KsScrollController()
        var action: (() -> Void)?
        let (hosting, controller) = try host(AppendAndScrollView(
            controller: scrollController,
            register: { action = $0 }
        ))
        let cv = controller.internalCollectionView
        let appendAndScroll = try XCTUnwrap(
            awaitNonNil("Button の処理の登録", in: hosting.view) { action }
        )
        let resolver = try XCTUnwrap(
            scrollController.currentReceiver as? DSLScrollCommandResolver,
            "DSL 方式ではハンドルは引き直しの受け口に接続する"
        )
        XCTAssertNil(rowFrame(controller, title: "new"), "前提: 追加前は存在しない")

        appendAndScroll()
        markIssued(scrollController)

        awaitProcessed(controller, 1, in: hosting.view)
        let generations = try XCTUnwrap(resolver.lastResolvedGenerations)
        XCTAssertGreaterThan(generations.resolved, generations.received,
                             "引き直しは状態変更による宣言の更新の後に行われる")
        XCTAssertEqual(rowFrame(controller, title: "new")?.minY ?? .nan, visibleTop(cv), accuracy: 0.5,
                       "追加した項目の Cell が表示範囲の上端に来る")
    }

    func test_解決待ちの間にハンドルを差し替えるとその命令はHostを動かさない() throws {
        let first = KsScrollController()
        let second = KsScrollController()
        var swap: ((KsScrollController?) -> Void)?
        let (hosting, controller) = try host(SwappableControllerView(initial: first, register: { swap = $0 }))
        let cv = controller.internalCollectionView
        let setController = try XCTUnwrap(awaitNonNil("差し替えの処理の登録", in: hosting.view) { swap })
        let resolver = try XCTUnwrap(first.currentReceiver as? DSLScrollCommandResolver)
        let initial = cv.contentOffset.y

        // 命令を受けた処理の中でハンドルを差し替える。SwiftUI の更新と引き直し (main.async) の順序に
        // 依らず、命令がまだ受け口で引き直しを待っている間に差し替わるよう、更新で行われるのと同じ
        // 接続の差し替えをこの処理の中で行う。
        first.scrollToEnd(animated: false)
        setController(second)
        resolver.connect(second)

        awaitCondition(
            "差し替え後のハンドルの接続",
            in: hosting.view,
            actual: { "first=\(String(describing: first.currentReceiver)) second=\(String(describing: second.currentReceiver))" },
            until: { first.currentReceiver == nil && second.currentReceiver is DSLScrollCommandResolver }
        )
        waitForNegativeVerification(in: hosting.view)
        XCTAssertEqual(controller.processedScrollCommandCount, 0, "外したハンドルの解決待ちの命令は Host へ届かない")
        XCTAssertEqual(cv.contentOffset.y, initial, accuracy: 0.5)

        // 差し替え後のハンドルの命令は届く。
        second.scrollToSection(id: 5, animated: false)
        markIssued(second)

        awaitProcessed(controller, 1, in: hosting.view)
        XCTAssertEqual(controller.processedScrollCommandCount, 1, "差し替え後の命令だけが処理される")
        XCTAssertEqual(headerFrame(controller, title: "Section 5")?.minY ?? .nan, visibleTop(cv), accuracy: 0.5,
                       "差し替え後のハンドルの命令の位置に止まる")
    }

    func test_解決待ちの間にハンドルを外すとその命令はHostを動かさない() throws {
        let first = KsScrollController()
        var swap: ((KsScrollController?) -> Void)?
        let (hosting, controller) = try host(SwappableControllerView(initial: first, register: { swap = $0 }))
        let cv = controller.internalCollectionView
        let setController = try XCTUnwrap(awaitNonNil("差し替えの処理の登録", in: hosting.view) { swap })
        let resolver = try XCTUnwrap(first.currentReceiver as? DSLScrollCommandResolver)
        let initial = cv.contentOffset.y

        // 命令がまだ受け口で引き直しを待っている間に外す (上のテストと同じく、この処理の中で外す)。
        first.scrollToEnd(animated: false)
        setController(nil)
        resolver.connect(nil)

        awaitCondition(
            "ハンドルの切断",
            in: hosting.view,
            actual: { "first=\(String(describing: first.currentReceiver))" },
            until: { first.currentReceiver == nil }
        )
        waitForNegativeVerification(in: hosting.view)
        XCTAssertEqual(controller.processedScrollCommandCount, 0, "外したハンドルの解決待ちの命令は Host へ届かない")
        XCTAssertEqual(cv.contentOffset.y, initial, accuracy: 0.5)
    }

    /// 表示範囲を途中へ送って位置を控え、先頭へ戻す。
    private func captureMidAnchor(_ controller: KsSettingsViewController) throws -> KsScrollAnchor {
        let cv = controller.internalCollectionView
        cv.setContentOffset(CGPoint(x: 0, y: 500), animated: false)
        layoutNow(cv)
        let anchor = try XCTUnwrap(controller.captureScrollAnchor())
        cv.setContentOffset(CGPoint(x: 0, y: -cv.adjustedContentInset.top), animated: false)
        layoutNow(cv)
        return anchor
    }

    func test_Hostへ渡った後の未実行の命令もハンドルの差し替えで捨てられ位置の復元は残る() throws {
        let first = KsScrollController()
        let second = KsScrollController()
        var swap: ((KsScrollController?) -> Void)?
        let (hosting, controller) = try host(SwappableControllerView(initial: first, register: { swap = $0 }))
        let setController = try XCTUnwrap(awaitNonNil("差し替えの処理の登録", in: hosting.view) { swap })
        let resolver = try XCTUnwrap(first.currentReceiver as? DSLScrollCommandResolver)
        let anchor = try captureMidAnchor(controller)
        var handedToHost: Int?

        // 受け口の引き直し (main.async) → Host の遅延 (main.async) の間に割り込んで差し替える。
        first.scrollToEnd(animated: false)
        // 割り込みの処理が回らないまま待機が deadline を超えても、命令を出した時点の記録が残るようにする。
        markIssued(first)
        DispatchQueue.main.async {
            handedToHost = controller.incomingScrollEntries.count + controller.readyScrollEntries.count
            controller.restoreScrollAnchor(anchor)
            setController(second)
            // SwiftUI の更新を待たずに、更新で行われるのと同じ接続の差し替えをこの時点で行う。
            resolver.connect(second)
            self.markIssued(second)
        }

        awaitProcessed(controller, 1, in: hosting.view)
        waitForNegativeVerification(in: hosting.view)
        XCTAssertEqual(handedToHost, 1, "前提: 差し替えの時点で命令は Host の待ち行列にあり未実行")
        XCTAssertEqual(controller.processedScrollCommandCount, 1, "位置の復元だけが処理され、末尾への命令は捨てられる")
        XCTAssertEqual(controller.captureScrollAnchor(), anchor, "控えた位置に戻っている")
        XCTAssertTrue(second.currentReceiver === resolver, "差し替え後のハンドルは受け口に接続されている")
    }

    func test_Hostへ渡った後の未実行の命令もハンドルを外すと捨てられる() throws {
        let first = KsScrollController()
        var swap: ((KsScrollController?) -> Void)?
        let (hosting, controller) = try host(SwappableControllerView(initial: first, register: { swap = $0 }))
        let cv = controller.internalCollectionView
        let setController = try XCTUnwrap(awaitNonNil("差し替えの処理の登録", in: hosting.view) { swap })
        let resolver = try XCTUnwrap(first.currentReceiver as? DSLScrollCommandResolver)
        let initial = cv.contentOffset.y
        var handedToHost: Int?

        first.scrollToEnd(animated: false)
        DispatchQueue.main.async {
            handedToHost = controller.incomingScrollEntries.count + controller.readyScrollEntries.count
            setController(nil)
            resolver.connect(nil)
        }

        awaitCondition(
            "差し替えの処理の実行",
            in: hosting.view,
            actual: { "handed=\(String(describing: handedToHost))" },
            until: { handedToHost != nil }
        )
        waitForNegativeVerification(in: hosting.view)
        XCTAssertEqual(handedToHost, 1, "前提: 外した時点で命令は Host の待ち行列にあり未実行")
        XCTAssertEqual(controller.processedScrollCommandCount, 0, "Host へ渡った未実行の命令も捨てられる")
        XCTAssertEqual(cv.contentOffset.y, initial, accuracy: 0.5)
    }

    func test_Store方式ではStoreのCellのIDで指せる() throws {
        let sections: [KsSettingsViewCore.Section] = (0..<12).map { sectionIndex in
            KsSettingsViewCore.Section(
                header: .text("Section \(sectionIndex)"),
                cells: (0..<4).map { LabelCell(title: "Row \(sectionIndex)-\($0)") }
            )
        }
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: sections))
        let scrollController = KsScrollController()
        let (hosting, controller) = try host(
            KsSettingsView(store: store).scrollController(scrollController)
        )
        let cv = controller.internalCollectionView
        let target = KsCellID(cell: sections[7].cells[2])
        XCTAssertTrue(controller.scrollController === scrollController, "Store 方式では Host へ直接接続する")

        scrollController.scrollTo(id: target, animated: false)
        markIssued(scrollController)

        awaitProcessed(controller, 1, in: hosting.view)
        XCTAssertEqual(rowFrame(controller, title: "Row 7-2")?.minY ?? .nan, visibleTop(cv), accuracy: 0.5,
                       "Store の Cell の ID で指した Cell が表示範囲の上端に来る")
    }
}
#endif
