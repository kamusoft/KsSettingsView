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
