// KsCellViewSupportTests.swift
// KsSettingsViewUITests
//
// `KsCellViewSupport.adjustedLayoutAttributes` の下限保証ロジックを検証する。
//
//   - `Theme.hasUnevenRows` のデフォルト = true → `isFixedHeight = false` で
//     intrinsic >= effectiveCellHeight なら intrinsic を採用、未満なら effectiveCellHeight に
//     強制引き上げる（下限保証）。
//   - `Theme.hasUnevenRows = false` → `isFixedHeight = true` で intrinsic に関わらず
//     effectiveCellHeight を強制適用する（固定高さ）。
//
// 参照: `AiForms.Maui.SettingsView` の iOS 実装は `UITableView.AutomaticDimension` と
//       `MinRowHeight = 48` を採用している。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class KsCellViewSupportTests: XCTestCase {
    private static let viewSize = CGSize(width: 375, height: 700)

    private func host(
        cell: LabelCell,
        theme: Theme
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let root = SettingsRoot(sections: [Section(cells: [cell])])
        let controller = KsSettingsViewController(root: root, theme: theme, style: .classic)
        let window = UIWindow(frame: CGRect(origin: .zero, size: Self.viewSize))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        controller.loadViewIfNeeded()
        controller.view.frame = CGRect(origin: .zero, size: Self.viewSize)
        controller.view.layoutIfNeeded()
        let collectionView = controller.internalCollectionView
        collectionView.frame = CGRect(origin: .zero, size: Self.viewSize)
        collectionView.setNeedsLayout()
        collectionView.layoutIfNeeded()
        return (controller, collectionView, window)
    }

    private func waitForFirstCell(
        in collectionView: UICollectionView,
        timeout: TimeInterval = 1.0,
        file: StaticString = #filePath,
        line: UInt = #line
    ) -> UICollectionViewCell? {
        let deadline = Date.now.addingTimeInterval(timeout)
        while Date.now < deadline {
            collectionView.setNeedsLayout()
            collectionView.layoutIfNeeded()
            if let cell = collectionView.cellForItem(at: IndexPath(item: 0, section: 0)) {
                return cell
            }
            _ = RunLoop.current.run(mode: .default, before: Date.now.addingTimeInterval(0.001))
        }
        XCTFail(
            "期限までに先頭 Cell が生成されなかった。表示中 Cell 数: \(collectionView.visibleCells.count)",
            file: file,
            line: line
        )
        return nil
    }

    @discardableResult
    private func waitForBackgroundColor(
        _ expectedColor: UIColor,
        of cell: UICollectionViewCell,
        in collectionView: UICollectionView,
        timeout: TimeInterval = 1.0,
        file: StaticString = #filePath,
        line: UInt = #line
    ) -> Bool {
        let deadline = Date.now.addingTimeInterval(timeout)
        while Date.now < deadline {
            collectionView.setNeedsLayout()
            collectionView.layoutIfNeeded()
            if cell.backgroundConfiguration?.backgroundColor?.isEqual(expectedColor) == true {
                return true
            }
            _ = RunLoop.current.run(mode: .default, before: Date.now.addingTimeInterval(0.001))
        }
        XCTFail(
            "背景色が期限までに収束しなかった。期待値: \(expectedColor)、実測値: \(String(describing: cell.backgroundConfiguration?.backgroundColor))",
            file: file,
            line: line
        )
        return false
    }

    func test_押下中は選択色になり解除後は平常時の実効背景色へ戻る() {
        let selectedColor = UIColor.magenta
        let normalColor = UIColor.yellow
        let model = LabelCell(
            style: CellStyle(backgroundColor: normalColor),
            title: "A"
        )
        let (_, collectionView, window) = host(
            cell: model,
            theme: Theme(selectedColor: selectedColor)
        )
        defer { window.isHidden = true }
        guard let cell = waitForFirstCell(in: collectionView) else { return }

        cell.isHighlighted = true
        guard waitForBackgroundColor(selectedColor, of: cell, in: collectionView) else { return }

        cell.isHighlighted = false
        waitForBackgroundColor(normalColor, of: cell, in: collectionView)
    }

    func test_無効Cellは押下しても選択色を塗らない() {
        let selectedColor = UIColor.magenta
        let normalColor = UIColor.yellow
        let model = LabelCell(
            style: CellStyle(backgroundColor: normalColor),
            title: "A",
            isEnabled: false
        )
        let (_, collectionView, window) = host(
            cell: model,
            theme: Theme(selectedColor: selectedColor)
        )
        defer { window.isHidden = true }
        guard let cell = waitForFirstCell(in: collectionView) else { return }

        var sentinelBackground = cell.backgroundConfiguration ?? UIBackgroundConfiguration.clear()
        sentinelBackground.backgroundColor = .cyan
        cell.backgroundConfiguration = sentinelBackground
        cell.isHighlighted = true

        waitForBackgroundColor(normalColor, of: cell, in: collectionView)
    }

    /// `applyEffectiveHeight` を呼ぶと `lastHeight` / `lastIsFixedHeight` が記録され、
    /// 続く `adjustedLayoutAttributes` で intrinsic（proposed）が `effectiveCellHeight` 未満なら
    /// `effectiveCellHeight` に強制引き上げられる。
    func test_adjustedLayoutAttributes_可変高さでintrinsicが下限未満なら強制引き上げ() {
        let cell = LabelCellView()
        let theme = Theme(hasUnevenRows: true)  // 可変高さ（新デフォルト）
        let effective = EffectiveStyle(theme: theme, cellStyle: CellStyle(cellHeight: 80.0))
        KsCellViewSupport.applyEffectiveHeight(cell, effective: effective)

        // intrinsic を 40pt（下限 80pt 未満）として proposed を作る
        let proposed = UICollectionViewLayoutAttributes()
        proposed.size = CGSize(width: 320, height: 40)

        let adjusted = KsCellViewSupport.adjustedLayoutAttributes(cell, proposed: proposed)
        XCTAssertEqual(adjusted.size.height, 80.0, "intrinsic(40) < effectiveCellHeight(80) なら 80 に引き上げ")
    }

    /// 可変高さで intrinsic が下限以上なら intrinsic がそのまま採用される。
    func test_adjustedLayoutAttributes_可変高さでintrinsicが下限以上ならintrinsicを採用() {
        let cell = LabelCellView()
        let theme = Theme(hasUnevenRows: true)
        let effective = EffectiveStyle(theme: theme, cellStyle: CellStyle(cellHeight: 48.0))
        KsCellViewSupport.applyEffectiveHeight(cell, effective: effective)

        // intrinsic を 100pt（下限 48pt 以上）として proposed を作る
        let proposed = UICollectionViewLayoutAttributes()
        proposed.size = CGSize(width: 320, height: 100)

        let adjusted = KsCellViewSupport.adjustedLayoutAttributes(cell, proposed: proposed)
        XCTAssertEqual(adjusted.size.height, 100, "intrinsic(100) >= effectiveCellHeight(48) なら intrinsic を採用")
    }

    /// 固定高さ（`hasUnevenRows = false`）では intrinsic に関わらず effectiveCellHeight が強制適用される。
    func test_adjustedLayoutAttributes_固定高さでintrinsicに関わらずeffectiveCellHeightで上書き() {
        let cell = LabelCellView()
        let theme = Theme(hasUnevenRows: false)
        let effective = EffectiveStyle(theme: theme, cellStyle: CellStyle(cellHeight: 80.0))
        KsCellViewSupport.applyEffectiveHeight(cell, effective: effective)

        // intrinsic = 200pt（下限の遥か上）でも固定 80 に揃う
        let proposed = UICollectionViewLayoutAttributes()
        proposed.size = CGSize(width: 320, height: 200)

        let adjusted = KsCellViewSupport.adjustedLayoutAttributes(cell, proposed: proposed)
        XCTAssertEqual(adjusted.size.height, 80.0, "isFixedHeight = true なら intrinsic を無視して effectiveCellHeight に揃える")
    }

    /// `preferredLayoutAttributesFitting` 経由でも同じく下限保証が効くことを確認する
    /// （`KsListCellBase` の override 経路が KsCellViewSupport.adjustedLayoutAttributes を経由している）。
    func test_preferredLayoutAttributesFitting_下限保証が効く() {
        let cell = LabelCellView()
        // 新デフォルト Theme(): hasUnevenRows = true / minRowHeight = 48
        let theme = Theme()
        let effective = EffectiveStyle(theme: theme, cellStyle: CellStyle())
        KsCellViewSupport.applyEffectiveHeight(cell, effective: effective)

        // intrinsic を 20pt（下限 48pt 未満）として proposed を作る
        let proposed = UICollectionViewLayoutAttributes()
        proposed.size = CGSize(width: 320, height: 20)

        let adjusted = cell.preferredLayoutAttributesFitting(proposed)
        XCTAssertEqual(adjusted.size.height, 48.0, "preferredLayoutAttributesFitting でも下限 48 が強制適用される")
    }
}
#endif
