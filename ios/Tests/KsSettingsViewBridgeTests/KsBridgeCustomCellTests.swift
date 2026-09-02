// KsBridgeCustomCellTests.swift
// KsSettingsViewBridgeTests
//
// interop 境界を越えて渡した `UIView` が CustomCell の内容として表示されるまでと、
// その view インスタンスがトークンの変化にだけ追従することを実描画で確認する。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewBridge
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

/// 破棄回数を数える共有カウンタ。
private final class DisposeCounter: @unchecked Sendable {
    var value = 0
}

/// 行の内容として埋め込む観測用の view。
///
/// 自分で必要な高さを答え、親への取り付け・取り外しと自身の破棄を数える。Bridge が
/// 「同じインスタンスを返し、破棄には関与しない」ことを回数で測るための観測点。
private final class ProbeContentView: UIView {

    /// 取り付けられた回数 (新しい親へ入った回数)。
    private(set) var attachCount = 0

    /// 取り外された回数 (親から外れた回数)。
    private(set) var detachCount = 0

    /// 必要な高さ。
    var contentHeight: CGFloat

    /// 破棄回数の記録先。
    private let disposeCounter: DisposeCounter

    init(counter: DisposeCounter, height: CGFloat = 40) {
        self.disposeCounter = counter
        self.contentHeight = height
        super.init(frame: .zero)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) は使用しない")
    }

    deinit {
        disposeCounter.value += 1
    }

    override var intrinsicContentSize: CGSize {
        CGSize(width: UIView.noIntrinsicMetric, height: contentHeight)
    }

    override func willMove(toSuperview newSuperview: UIView?) {
        if newSuperview == nil && superview != nil {
            detachCount += 1
        }
        super.willMove(toSuperview: newSuperview)
    }

    override func didMoveToSuperview() {
        if superview != nil {
            attachCount += 1
        }
        super.didMoveToSuperview()
    }
}

@MainActor
final class KsBridgeCustomCellTests: XCTestCase {

    // MARK: - DTO の変換

    func test_CustomCellDTOがCustomCellへ変換される() {
        let counter = DisposeCounter()
        let dto = KsBridgeCustomCell(title: "無視される")
        dto.view = ProbeContentView(counter: counter)
        dto.contentToken = "token-1"
        dto.showArrowIndicator = true
        dto.hasTapHandler = true
        dto.isVisible = false
        dto.isEnabled = false
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: CustomCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertEqual(cell?.content, AnyHashable("token-1"), "content にはトークンが格納される")
        XCTAssertEqual(cell?.showArrow, true)
        XCTAssertEqual(cell?.isEnabled, false)
        XCTAssertEqual(cell?.isVisible, false)
        XCTAssertNotNil(cell?.onTap, "タップ購読ありでは行タップのコールバックが注入される")
    }

    func test_タップ購読なしのDTOは行タップ動作を持たない() {
        let dto = KsBridgeCustomCell(title: "")
        dto.contentToken = "token-1"
        dto.hasTapHandler = false
        let bridge = KsBridgeFixture.withCells([dto])

        let cell: CustomCell? = KsBridgeFixture.storedCell(bridge)
        XCTAssertNil(cell?.onTap, "購読なしの行は onTap を持たず、内容の中の操作を妨げない")
    }

    func test_同一トークンのCustomCellは等価になる() {
        let counter = DisposeCounter()
        let id = UUID()
        let left = KsBridgeCustomCell(title: "")
        left.view = ProbeContentView(counter: counter)
        left.contentToken = "token-1"
        let right = KsBridgeCustomCell(title: "")
        right.view = ProbeContentView(counter: counter)
        right.contentToken = "token-1"

        let relay = KsBridgeInteractionRelay()
        let leftCell = left.makeCell(id: id, relay: relay) as? CustomCell
        let rightCell = right.makeCell(id: id, relay: relay) as? CustomCell

        XCTAssertEqual(leftCell, rightCell, "等価性はトークンで決まり、view の違いは参加しない")

        right.contentToken = "token-2"
        let changed = right.makeCell(id: id, relay: relay) as? CustomCell
        XCTAssertNotEqual(leftCell, changed, "トークンが変われば等価ではなくなる")
    }

    // MARK: - 実描画

    func test_setRootで輸送したviewが行の内容として表示される() {
        let counter = DisposeCounter()
        let probe = ProbeContentView(counter: counter)
        let dto = KsBridgeCustomCell(title: "")
        dto.view = probe
        dto.contentToken = "token-1"
        let bridge = KsBridgeFixture.withCells([dto])

        let attachment = KsBridgeTestHost.attach(bridge)

        XCTAssertTrue(embeddedProbe(attachment) === probe, "輸送した view インスタンスがそのまま行に表示される")
        XCTAssertEqual(counter.value, 0, "Bridge は輸送された view を破棄しない")

        let row = attachment.collectionView.cellForItem(at: IndexPath(item: 0, section: 0))
        XCTAssertEqual(
            probe.frame.width,
            row?.contentView.bounds.width ?? .nan,
            accuracy: 0.5,
            "内容は行の幅いっぱいに描画される"
        )
    }

    /// 輸送した view は入れ物の中に置かれる。
    ///
    /// 描画側が作り直す単位を入れ物にしておかないと、行の再利用で内容が別の行へ移った後に
    /// 前の行の片付けが走ったとき、表示中の行から内容が外れて空行になる。
    func test_輸送したviewは入れ物の中に置かれる() {
        let counter = DisposeCounter()
        let probe = ProbeContentView(counter: counter)
        let dto = KsBridgeCustomCell(title: "")
        dto.view = probe
        dto.contentToken = "token-1"
        let bridge = KsBridgeFixture.withCells([dto])

        let attachment = KsBridgeTestHost.attach(bridge)

        XCTAssertTrue(embeddedProbe(attachment) === probe, "前提: 輸送した view が行に埋め込まれていない")
        XCTAssertTrue(
            probe.superview is KsBridgeCellContentHostView,
            "輸送した view が描画側へ直接渡されている"
        )
    }

    /// 既に別の親に付いている view を輸送しても、行の内容として取り付け直される。
    func test_既存の親を持つviewも行の内容として取り付けられる() {
        let counter = DisposeCounter()
        let probe = ProbeContentView(counter: counter)
        let previousParent = UIView()
        previousParent.addSubview(probe)
        XCTAssertTrue(probe.superview === previousParent, "前提: 別の親に付いていない")

        let dto = KsBridgeCustomCell(title: "")
        dto.view = probe
        dto.contentToken = "token-1"
        let bridge = KsBridgeFixture.withCells([dto])
        let attachment = KsBridgeTestHost.attach(bridge)

        XCTAssertTrue(embeddedProbe(attachment) === probe, "既存の親から切り離して行へ取り付けられる")
        XCTAssertTrue(previousParent.subviews.isEmpty, "元の親からは外れる")
    }

    func test_replaceCellsで輸送したviewが行の内容として表示される() {
        let counter = DisposeCounter()
        let first = ProbeContentView(counter: counter)
        let second = ProbeContentView(counter: counter)
        let dto = KsBridgeCustomCell(title: "")
        dto.view = first
        dto.contentToken = "token-1"
        let bridge = KsBridgeFixture.withCells([dto, KsBridgeLabelCell(title: "後続")])
        let attachment = KsBridgeTestHost.attach(bridge)

        let update = KsBridgeCustomCell(title: "")
        update.view = second
        update.contentToken = "token-2"
        bridge.replaceCells([
            KsBridgeCellUpdate(cellID: dto.cellID, cell: update),
            KsBridgeCellUpdate(cellID: KsBridgeFixture.unusedIdentifier(), cell: KsBridgeLabelCell(title: "X")),
        ])
        awaitEmbeddedProbe(attachment, is: second, "バッチ更新後の行の内容の入れ替え")

        XCTAssertTrue(embeddedProbe(attachment) === second, "バッチ更新でも輸送した view が行に表示される")
    }

    func test_view未指定のDTOは空の内容の行になる() {
        let dto = KsBridgeCustomCell(title: "")
        dto.contentToken = "token-1"
        let bridge = KsBridgeFixture.withCells([dto, KsBridgeLabelCell(title: "後続")])

        let attachment = KsBridgeTestHost.attach(bridge)

        XCTAssertNotNil(
            attachment.collectionView.cellForItem(at: IndexPath(item: 0, section: 0)),
            "内容なしでも行そのものは出力される"
        )
        XCTAssertEqual(
            attachment.collectionView.numberOfItems(inSection: 0),
            2,
            "後続の行も通常どおり並ぶ"
        )
    }

    // MARK: - view インスタンスの安定性

    /// 同一トークンで再発行しても、行に埋め込まれた view は同じインスタンスのまま維持される。
    ///
    /// 埋め込みの入れ替え (materialize) 0 回・破棄 (dispose) 0 回を測る。再バインドが実際に
    /// 起きたことは、同時に変えた有効状態が行へ届いていることで裏付ける。
    func test_同一トークンの再発行ではviewインスタンスが維持される() {
        let counter = DisposeCounter()
        let probe = ProbeContentView(counter: counter)
        let dto = KsBridgeCustomCell(title: "")
        dto.view = probe
        dto.contentToken = "token-1"
        dto.hasTapHandler = true
        let bridge = KsBridgeFixture.withCells([dto])
        let attachment = KsBridgeTestHost.attach(bridge)

        XCTAssertTrue(embeddedProbe(attachment) === probe, "前提: 輸送した view が行に埋め込まれていない")
        XCTAssertNotNil(customCellView(attachment)?.tapHandler, "前提: 行タップ動作が設定されていない")
        let attachBefore = probe.attachCount
        let detachBefore = probe.detachCount

        let update = KsBridgeCustomCell(title: "")
        update.view = probe
        update.contentToken = "token-1"
        update.hasTapHandler = true
        update.isEnabled = false
        bridge.replaceCell(cellID: dto.cellID, newCell: update)
        // 更新前は行タップ動作が設定されているため、それが外れることが再バインドの遷移証拠になる。
        awaitCondition(
            "無効化した CustomCell の再バインド (行タップ動作の解除)",
            in: attachment.collectionView,
            actual: { "tapHandler \(self.customCellView(attachment)?.tapHandler == nil ? "nil" : "設定あり")" },
            until: { self.customCellView(attachment)?.tapHandler == nil }
        )

        XCTAssertNil(
            customCellView(attachment)?.tapHandler,
            "前提: 有効状態の変更が行へ届いておらず、再バインドが起きたと言えない"
        )
        XCTAssertTrue(
            embeddedProbe(attachment) === probe,
            "同一トークンの再発行では同じインスタンスが表示され続ける"
        )
        XCTAssertEqual(probe.attachCount, attachBefore, "埋め込みが作り直されている")
        XCTAssertEqual(probe.detachCount, detachBefore, "埋め込みが外されている")
        XCTAssertEqual(counter.value, 0, "同一トークンの再発行で view は破棄されない")
    }

    /// トークンが変われば行の内容が新しい view へ入れ替わり、旧 view は行から外れる。
    ///
    /// 埋め込みの入れ替え (materialize) 1 回・破棄 (dispose) 0 回を測る。旧 view は行の描画から
    /// 外れるが、Bridge は破棄も superview からの取り外しも行わない (どちらも輸送元の責務)。
    func test_トークン変更で行の内容が新しいviewへ置き換わる() {
        let counter = DisposeCounter()
        let first = ProbeContentView(counter: counter)
        let second = ProbeContentView(counter: counter, height: 80)
        let dto = KsBridgeCustomCell(title: "")
        dto.view = first
        dto.contentToken = "token-1"
        let bridge = KsBridgeFixture.withCells([dto])
        let attachment = KsBridgeTestHost.attach(bridge)

        XCTAssertTrue(embeddedProbe(attachment) === first, "前提: 最初の view が行に埋め込まれていない")

        let update = KsBridgeCustomCell(title: "")
        update.view = second
        update.contentToken = "token-2"
        bridge.replaceCell(cellID: dto.cellID, newCell: update)
        awaitEmbeddedProbe(attachment, is: second, "トークン変更による行の内容の入れ替え")

        XCTAssertTrue(embeddedProbe(attachment) === second, "トークン変更で行の内容が新しい view になる")
        XCTAssertEqual(second.attachCount, 1, "新しい view の埋め込みは 1 回だけ起きる")
        XCTAssertFalse(isDisplayedInRow(first, attachment), "旧 view は行の描画から外れる")
        XCTAssertEqual(counter.value, 0, "旧 view の破棄は Bridge の責務ではない")
    }

    /// 同じ view を掴んだまま token だけを変えても、行の内容として取り付け直される。
    func test_同一viewのままトークンだけ変えても表示が続く() {
        let counter = DisposeCounter()
        let probe = ProbeContentView(counter: counter)
        let dto = KsBridgeCustomCell(title: "")
        dto.view = probe
        dto.contentToken = "token-1"
        let bridge = KsBridgeFixture.withCells([dto])
        let attachment = KsBridgeTestHost.attach(bridge)

        let attachBefore = probe.attachCount

        let update = KsBridgeCustomCell(title: "")
        update.view = probe
        update.contentToken = "token-2"
        bridge.replaceCell(cellID: dto.cellID, newCell: update)
        // 同一 view のため埋め込み先の変化は見えない。トークン変更で内容が作り直され、
        // 同じ view が取り付け直されることを遷移証拠にする。
        awaitCondition(
            "トークン変更による同一 view の取り付け直し",
            in: attachment.collectionView,
            actual: { "取り付け回数 \(probe.attachCount) (更新前 \(attachBefore))" },
            until: { probe.attachCount > attachBefore }
        )

        XCTAssertTrue(embeddedProbe(attachment) === probe)
        XCTAssertEqual(counter.value, 0)
    }

    // MARK: - リサイクル

    /// 画面外へ出て戻る (行の再利用が起きる) 間、同一 view が例外なく再表示される。
    func test_リサイクルを挟んだ再表示で内容が壊れない() {
        let counter = DisposeCounter()
        let probe = ProbeContentView(counter: counter)
        let bridge = KsSettingsBridge()
        let builder = KsBridgeRootBuilder()
        var customCellID = ""
        for sectionIndex in 0..<12 {
            let section = builder.addSection(headerText: "S\(sectionIndex)", footerText: nil)
            for cellIndex in 0..<5 {
                if sectionIndex == 0 && cellIndex == 0 {
                    let dto = KsBridgeCustomCell(title: "")
                    dto.view = probe
                    dto.contentToken = "token-1"
                    customCellID = dto.cellID
                    builder.addCell(dto, sectionID: section.sectionID)
                } else {
                    builder.addCell(
                        KsBridgeLabelCell(title: "C\(sectionIndex)-\(cellIndex)"),
                        sectionID: section.sectionID
                    )
                }
            }
        }
        bridge.setRoot(builder)

        let attachment = KsBridgeTestHost.attach(bridge)
        XCTAssertTrue(embeddedProbe(attachment) === probe, "前提: 先頭行に view が埋め込まれていない")
        XCTAssertFalse(customCellID.isEmpty)

        let collectionView = attachment.collectionView
        let maxOffset = max(0, collectionView.contentSize.height - collectionView.bounds.height)
        XCTAssertGreaterThan(maxOffset, 0, "前提: 画面外へスクロールできる長さの list になっていない")

        collectionView.contentOffset = CGPoint(x: 0, y: maxOffset)
        // 画面外へ出た行の回収と、その内容の取り外しは次のレイアウト周回で確定するため、
        // 回収が済んで内容がどの表示中の行にも残っていない状態そのものを待つ。
        let isRecycled = {
            collectionView.cellForItem(at: IndexPath(item: 0, section: 0)) == nil
                && !collectionView.indexPathsForVisibleItems.contains { indexPath in
                    collectionView.cellForItem(at: indexPath).map { probe.isDescendant(of: $0) } ?? false
                }
        }
        awaitCondition(
            "先頭行が画面外へ出て再利用され、内容が表示中の行から外れる",
            in: collectionView,
            actual: {
                "先頭行 \(KsBridgeTestHost.describe(collectionView.cellForItem(at: IndexPath(item: 0, section: 0))))"
            },
            until: isRecycled
        )
        XCTAssertNil(
            collectionView.cellForItem(at: IndexPath(item: 0, section: 0)),
            "前提: 先頭行が画面外へ出ていない"
        )
        for indexPath in collectionView.indexPathsForVisibleItems {
            let cell = collectionView.cellForItem(at: indexPath)
            XCTAssertFalse(
                cell.map { probe.isDescendant(of: $0) } ?? false,
                "画面外へ出た内容が別の行に残っている (\(indexPath))"
            )
        }

        collectionView.contentOffset = .zero
        awaitEmbeddedProbe(attachment, is: probe, "スクロール復帰後の先頭行への再表示")

        XCTAssertTrue(embeddedProbe(attachment) === probe, "再利用後の行に同一 view が再表示される")
        XCTAssertEqual(counter.value, 0, "リサイクルで view が破棄されてはいけない")
    }

    // MARK: - タップ通知

    func test_行タップがcustomCellTappedで通知される() {
        let counter = DisposeCounter()
        let dto = KsBridgeCustomCell(title: "")
        dto.view = ProbeContentView(counter: counter)
        dto.contentToken = "token-1"
        dto.hasTapHandler = true
        let bridge = KsBridgeFixture.withCells([dto])
        let recorder = CustomCellTapRecorder()
        bridge.interactionDelegate = recorder
        let attachment = KsBridgeTestHost.attach(bridge)

        let before: CustomCell? = KsBridgeFixture.storedCell(bridge)
        attachment.controller.collectionView(
            attachment.collectionView,
            didSelectItemAt: IndexPath(item: 0, section: 0)
        )

        XCTAssertEqual(recorder.tappedCellIDs, [dto.cellID])
        XCTAssertEqual(
            KsBridgeFixture.storedCell(bridge) as CustomCell?,
            before,
            "タップに書き戻しは伴わない"
        )
    }

    func test_タップ購読なしの行はタップしても通知されない() {
        let counter = DisposeCounter()
        let dto = KsBridgeCustomCell(title: "")
        dto.view = ProbeContentView(counter: counter)
        dto.contentToken = "token-1"
        dto.hasTapHandler = false
        let bridge = KsBridgeFixture.withCells([dto])
        let recorder = CustomCellTapRecorder()
        bridge.interactionDelegate = recorder
        let attachment = KsBridgeTestHost.attach(bridge)

        attachment.controller.collectionView(
            attachment.collectionView,
            didSelectItemAt: IndexPath(item: 0, section: 0)
        )

        XCTAssertEqual(recorder.tappedCellIDs, [])
    }

    /// 購読の有無は同一トークンのままの再発行で切り替わり、view は維持される。
    func test_タップ購読の有無は再発行で切り替わる() {
        let counter = DisposeCounter()
        let probe = ProbeContentView(counter: counter)
        let dto = KsBridgeCustomCell(title: "")
        dto.view = probe
        dto.contentToken = "token-1"
        dto.hasTapHandler = false
        let bridge = KsBridgeFixture.withCells([dto])
        let recorder = CustomCellTapRecorder()
        bridge.interactionDelegate = recorder
        let attachment = KsBridgeTestHost.attach(bridge)

        let update = KsBridgeCustomCell(title: "")
        update.view = probe
        update.contentToken = "token-1"
        update.hasTapHandler = true
        bridge.replaceCell(cellID: dto.cellID, newCell: update)
        // 更新前は購読なしで行タップ動作を持たないため、その設定が再バインドの遷移証拠になる。
        awaitCondition(
            "タップ購読ありへの再発行が行へ届く",
            in: attachment.collectionView,
            actual: { "tapHandler \(self.customCellView(attachment)?.tapHandler == nil ? "nil" : "設定あり")" },
            until: { self.customCellView(attachment)?.tapHandler != nil }
        )

        attachment.controller.collectionView(
            attachment.collectionView,
            didSelectItemAt: IndexPath(item: 0, section: 0)
        )

        XCTAssertEqual(recorder.tappedCellIDs, [dto.cellID])
        XCTAssertTrue(embeddedProbe(attachment) === probe, "購読の切り替えで view は入れ替わらない")
        XCTAssertEqual(counter.value, 0)
    }

    // MARK: - 観測ヘルパ

    /// 先頭 Section の指定行を描画している CustomCell 用の Cell View を返す。
    private func customCellView(
        _ attachment: KsBridgeTestHost.Attachment,
        section: Int = 0,
        item: Int = 0
    ) -> CustomCellView? {
        return attachment.collectionView.cellForItem(
            at: IndexPath(item: item, section: section)
        ) as? CustomCellView
    }

    /// 指定行の内容が期待の観測用 view になるまで待つ。
    private func awaitEmbeddedProbe(
        _ attachment: KsBridgeTestHost.Attachment,
        is expected: ProbeContentView,
        _ description: String,
        section: Int = 0,
        item: Int = 0,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        awaitCondition(
            description,
            in: attachment.collectionView,
            actual: { KsBridgeTestHost.describe(self.embeddedProbe(attachment, section: section, item: item)) },
            file: file,
            line: line,
            until: { self.embeddedProbe(attachment, section: section, item: item) === expected }
        )
    }

    /// 指定行に実描画されている観測用 view を返す。
    private func embeddedProbe(
        _ attachment: KsBridgeTestHost.Attachment,
        section: Int = 0,
        item: Int = 0
    ) -> ProbeContentView? {
        guard let cell = attachment.collectionView.cellForItem(
            at: IndexPath(item: item, section: section)
        ) else {
            return nil
        }
        return firstDescendant(of: cell)
    }

    /// 指定行の描画に view が含まれているかを返す。
    private func isDisplayedInRow(
        _ view: UIView,
        _ attachment: KsBridgeTestHost.Attachment,
        section: Int = 0,
        item: Int = 0
    ) -> Bool {
        guard let cell = attachment.collectionView.cellForItem(
            at: IndexPath(item: item, section: section)
        ) else {
            return false
        }
        return view.isDescendant(of: cell)
    }

    /// 子孫を深さ優先でたどり、最初に見つかった観測用 view を返す。
    private func firstDescendant(of view: UIView) -> ProbeContentView? {
        for subview in view.subviews {
            if let probe = subview as? ProbeContentView {
                return probe
            }
            if let probe = firstDescendant(of: subview) {
                return probe
            }
        }
        return nil
    }
}

/// CustomCell のタップ通知だけを記録する delegate 実装。
private final class CustomCellTapRecorder: NSObject, KsBridgeInteractionDelegate {

    /// 通知された cellID の並び。
    private(set) var tappedCellIDs: [String] = []

    func customCellTapped(cellID: String) {
        tappedCellIDs.append(cellID)
    }

    func commandCellTapped(cellID: String) {}
    func buttonCellTapped(cellID: String) {}
    func switchCellChanged(cellID: String, isOn: Bool) {}
    func checkboxCellChanged(cellID: String, isChecked: Bool) {}
    func simpleCheckCellChanged(cellID: String, isChecked: Bool) {}
    func radioCellSelected(cellID: String, value: String) {}
    func entryCellTextChanged(cellID: String, text: String) {}
    func pickerCellSelectionChanged(cellID: String, index: Int) {}
    func pickerCellMultiSelectionChanged(cellID: String, indices: [Int]) {}
    func numberPickerCellChanged(cellID: String, value: Int) {}
    func timePickerCellChanged(cellID: String, time: String) {}
    func datePickerCellChanged(cellID: String, date: String) {}
}
#endif
