// CellTouchFeedbackTimingTests.swift
// KsSettingsViewUITests
//
// 押下色 (Theme.selectedColor) が指を置いた瞬間に出るための条件を検証する。
//
//   - collection view がタッチ遅延を持たず、スライド操作のコントロールの上でだけ
//     スクロールへの横取りを避けること
//   - 押下色の表示中に render しても平常色で上書きされないこと
//   - 立ち上がりの待ちの間にキャンセルされたら押下色が出ないこと
//   - 再利用で押下色の予約・表示状態が持ち越されないこと
//   - タップ後の選択解除が、遷移が始まったかどうかで分かれること
//
// 待ち時間の秒数そのものは検証しない (調整値であり、契約は「遅延がある / 無い」の側にある)。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class CellTouchFeedbackTimingTests: XCTestCase {
    private static let viewSize = CGSize(width: 375, height: 700)

    private func host(
        cells: [any KsCell],
        theme: Theme = Theme()
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let root = SettingsRoot(sections: [Section(cells: cells)])
        let controller = KsSettingsViewController(root: root, theme: theme, style: .classic)
        let window = UIWindow(frame: CGRect(origin: .zero, size: Self.viewSize))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        controller.loadViewIfNeeded()
        controller.view.frame = CGRect(origin: .zero, size: Self.viewSize)
        controller.view.layoutIfNeeded()
        let collectionView = controller.internalCollectionView
        collectionView.frame = CGRect(origin: .zero, size: Self.viewSize)
        awaitInitialRender(controller)
        return (controller, collectionView, window)
    }

    // MARK: - タッチ遅延

    func test_collectionViewはタッチ遅延を持たない() {
        let (_, collectionView, window) = host(cells: [LabelCell(title: "A")])
        defer { window.isHidden = true }

        XCTAssertTrue(
            collectionView is ImmediateTouchCollectionView,
            "押下を遅延なく Cell へ渡す collection view を使う"
        )
        XCTAssertFalse(
            collectionView.delaysContentTouches,
            "スクロール判定のための押下の遅延を持たない"
        )
    }

    func test_スライド操作のコントロール上ではタッチをキャンセルしない() {
        let collectionView = ImmediateTouchCollectionView(
            frame: CGRect(origin: .zero, size: Self.viewSize),
            collectionViewLayout: UICollectionViewFlowLayout()
        )

        XCTAssertFalse(
            collectionView.touchesShouldCancel(in: UISwitch()),
            "Switch のつまみ操作をスクロールへ横取りしない"
        )
        XCTAssertFalse(
            collectionView.touchesShouldCancel(in: UISlider()),
            "Slider のつまみ操作をスクロールへ横取りしない"
        )

        // SwiftUI 等のラッパー越しでも同じ判定になるよう、superview 連鎖を辿る。
        let switchControl = UISwitch()
        let descendant = UIView()
        switchControl.addSubview(descendant)
        XCTAssertFalse(
            collectionView.touchesShouldCancel(in: descendant),
            "Switch の子孫から始まったタッチもキャンセルしない"
        )

        XCTAssertTrue(
            collectionView.touchesShouldCancel(in: UIView()),
            "通常の view 上から始まったドラッグはスクロールへ渡す"
        )
        XCTAssertTrue(
            collectionView.touchesShouldCancel(in: UIButton()),
            "スライド操作を持たないコントロール上でもスクロールへ渡す"
        )
    }

    // MARK: - render との競合

    func test_押下色の表示中はrenderで平常色に上書きされない() {
        let selectedColor = UIColor.magenta
        let normalColor = UIColor.yellow
        let (_, collectionView, window) = host(
            cells: [LabelCell(style: CellStyle(backgroundColor: normalColor), title: "A")],
            theme: Theme(selectedColor: selectedColor)
        )
        defer { window.isHidden = true }
        guard let cell = collectionView.cellForItem(at: IndexPath(item: 0, section: 0))
            as? UICollectionViewListCell else {
            return XCTFail("先頭 Cell を取得できない")
        }

        cell.isHighlighted = true
        awaitCondition(
            "押下中の背景が選択色になる",
            in: collectionView,
            actual: { "背景色 \(String(describing: cell.backgroundConfiguration?.backgroundColor))" },
            until: { cell.backgroundConfiguration?.backgroundColor?.isEqual(selectedColor) ?? false }
        )

        // タップで値が変わる Cell はタップ直後に再構成される。そこで平常色を入れると押下色が消える。
        KsCellViewSupport.applyRenderedBackgroundColor(cell)
        XCTAssertEqual(
            cell.backgroundConfiguration?.backgroundColor?.isEqual(selectedColor),
            true,
            "押下色の表示中は render が平常色で上書きしない"
        )

        cell.isHighlighted = false
        awaitCondition(
            "解除後の背景が平常時の実効背景色へ戻る",
            in: collectionView,
            actual: { "背景色 \(String(describing: cell.backgroundConfiguration?.backgroundColor))" },
            until: { cell.backgroundConfiguration?.backgroundColor?.isEqual(normalColor) ?? false }
        )

        KsCellViewSupport.applyRenderedBackgroundColor(cell)
        XCTAssertEqual(
            cell.backgroundConfiguration?.backgroundColor?.isEqual(normalColor),
            true,
            "押下していない Cell の render は平常時の実効背景色を入れる"
        )
    }

    func test_立ち上がりの待ちの間にキャンセルされたら押下色は出ない() {
        let selectedColor = UIColor.magenta
        let normalColor = UIColor.yellow
        let (_, collectionView, window) = host(
            cells: [LabelCell(style: CellStyle(backgroundColor: normalColor), title: "A")],
            theme: Theme(selectedColor: selectedColor)
        )
        defer { window.isHidden = true }
        guard let cell = collectionView.cellForItem(at: IndexPath(item: 0, section: 0))
            as? UICollectionViewListCell else {
            return XCTFail("先頭 Cell を取得できない")
        }

        // 指を置いた直後にスクロールが始まった場合に相当する。押下状態の変化は構成更新まで
        // 遅延するため、押下が Cell へ届いてから (= 塗り始めが予約されてから) 指を滑らせる。
        cell.isHighlighted = true
        cell.layoutIfNeeded()
        XCTAssertNotNil(
            KsCellViewSupport.state(cell).pendingSelectedColorTask,
            "押下が届いた時点で塗り始めが予約される"
        )
        cell.isHighlighted = false

        // 押下色が出ないことの確認なので、待つべき正の完了条件が存在しない。
        waitForNegativeVerification(in: collectionView)
        XCTAssertEqual(
            cell.backgroundConfiguration?.backgroundColor?.isEqual(normalColor),
            true,
            "立ち上がりの待ちの間にキャンセルされたら塗り始めを捨てる"
        )
        XCTAssertFalse(
            KsCellViewSupport.state(cell).isShowingSelectedColor,
            "押下色を表示中の扱いにしない"
        )
    }

    func test_再利用で押下色の予約と表示状態がリセットされる() {
        let selectedColor = UIColor.magenta
        let normalColor = UIColor.yellow
        let (_, collectionView, window) = host(
            cells: [LabelCell(style: CellStyle(backgroundColor: normalColor), title: "A")],
            theme: Theme(selectedColor: selectedColor)
        )
        defer { window.isHidden = true }
        guard let cell = collectionView.cellForItem(at: IndexPath(item: 0, section: 0))
            as? UICollectionViewListCell else {
            return XCTFail("先頭 Cell を取得できない")
        }

        // 押下色を表示している段階での再利用。
        cell.isHighlighted = true
        awaitCondition(
            "押下中の背景が選択色になる",
            in: collectionView,
            actual: { "背景色 \(String(describing: cell.backgroundConfiguration?.backgroundColor))" },
            until: { cell.backgroundConfiguration?.backgroundColor?.isEqual(selectedColor) ?? false }
        )

        cell.prepareForReuse()

        XCTAssertFalse(
            KsCellViewSupport.state(cell).isShowingSelectedColor,
            "再利用で押下色の表示状態を持ち越さない"
        )
        KsCellViewSupport.applyRenderedBackgroundColor(cell)
        XCTAssertEqual(
            cell.backgroundConfiguration?.backgroundColor?.isEqual(normalColor),
            true,
            "再利用後の render は平常時の実効背景色を入れる"
        )

        // 塗り始めを予約している段階での再利用。
        cell.isHighlighted = false
        cell.layoutIfNeeded()
        cell.isHighlighted = true
        cell.layoutIfNeeded()
        XCTAssertNotNil(
            KsCellViewSupport.state(cell).pendingSelectedColorTask,
            "押下が届いた時点で塗り始めが予約される"
        )

        cell.prepareForReuse()

        XCTAssertNil(
            KsCellViewSupport.state(cell).pendingSelectedColorTask,
            "再利用で塗り始めの予約を持ち越さない"
        )
        // 予約が生きていれば待ちの経過後に塗られる。塗られないことの確認なので固定待機で書く。
        waitForNegativeVerification(in: collectionView)
        KsCellViewSupport.applyRenderedBackgroundColor(cell)
        XCTAssertEqual(
            cell.backgroundConfiguration?.backgroundColor?.isEqual(normalColor),
            true,
            "再利用後は予約されていた押下色が塗られない"
        )
    }

    // MARK: - タップ後の選択解除

    func test_遷移が始まらないタップでは選択が解除される() {
        let (controller, collectionView, window) = host(
            cells: [CommandCell(title: "A", onTap: {})]
        )
        defer { window.isHidden = true }
        let indexPath = IndexPath(item: 0, section: 0)

        collectionView.selectItem(at: indexPath, animated: false, scrollPosition: [])
        controller.collectionView(collectionView, didSelectItemAt: indexPath)

        awaitCondition(
            "猶予の経過後に選択が解除される",
            in: collectionView,
            actual: { "選択 \(collectionView.indexPathsForSelectedItems ?? [])" },
            until: { (collectionView.indexPathsForSelectedItems ?? []).isEmpty }
        )
    }

    func test_tapHandlerの中で同期的に遷移が始まっても選択が残る() {
        // UIKit から直接使う場合、tapHandler の中で pushViewController が同期的に走り、
        // その場で viewWillDisappear が届く。
        var controllerRef: KsSettingsViewController?
        let cell = CommandCell(title: "A", onTap: { @Sendable in
            MainActor.assumeIsolated {
                controllerRef?.viewWillDisappear(false)
            }
        })
        let (controller, collectionView, window) = host(cells: [cell])
        defer { window.isHidden = true }
        controllerRef = controller
        let indexPath = IndexPath(item: 0, section: 0)

        collectionView.selectItem(at: indexPath, animated: false, scrollPosition: [])
        controller.collectionView(collectionView, didSelectItemAt: indexPath)

        // 解除されないことの確認なので、待つべき正の完了条件が存在しない。
        waitForNegativeVerification(in: collectionView)
        XCTAssertEqual(
            collectionView.indexPathsForSelectedItems ?? [],
            [indexPath],
            "tapHandler の中で届いた遷移の合図も猶予の判定に効く"
        )
    }

    func test_タップで遷移が始まった場合は選択が残る() {
        let (controller, collectionView, window) = host(
            cells: [CommandCell(title: "A", onTap: {})]
        )
        defer { window.isHidden = true }
        let indexPath = IndexPath(item: 0, section: 0)

        collectionView.selectItem(at: indexPath, animated: false, scrollPosition: [])
        controller.collectionView(collectionView, didSelectItemAt: indexPath)
        // タップで push 遷移のように viewWillDisappear が届く遷移が起きた場合に相当する
        // (pageSheet の present では届かないため、遷移しないタップと同じ扱いになる)。
        controller.viewWillDisappear(false)

        // 解除されないことの確認なので、待つべき正の完了条件が存在しない。
        waitForNegativeVerification(in: collectionView)
        XCTAssertEqual(
            collectionView.indexPathsForSelectedItems ?? [],
            [indexPath],
            "遷移のあいだは押下色を残すため選択を解除しない"
        )
    }
}
#endif
