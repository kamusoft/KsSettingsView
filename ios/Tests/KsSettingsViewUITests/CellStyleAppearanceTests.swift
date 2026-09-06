// CellStyleAppearanceTests.swift
// KsSettingsViewUITests
//
// 利用者が `CellStyle` に明示した色と外観（ライト / ダーク）の関係を検証する。
//
// 観測点は表示中の行の実体に取る。title 色は行の `titleLabel.textColor` を、その label 自身の
// trait で解決した値で見る（label がその trait で描く値そのもの）。行の背景は
// `UICollectionViewListCell.backgroundConfiguration` に適用された色を、その Cell 自身の trait で
// 解決して見る。
//
// 期待値はテスト側に literal で持つ（実装側の定数と突き合わせると恒真になるため）。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class CellStyleAppearanceTests: XCTestCase {
    private static let viewSize = CGSize(width: 375, height: 700)

    // MARK: - ハーネス

    /// LabelCell 1 行だけの root を、指定した CellStyle で作る。
    private func makeRoot(style: CellStyle) -> SettingsRoot {
        let section = KsSettingsViewCore.Section(
            cells: [LabelCell(style: style, title: "A")]
        )
        return SettingsRoot(sections: [section])
    }

    /// controller を指定外観の window に載せて実レイアウトを走らせる。
    private func host(
        root: SettingsRoot,
        userInterfaceStyle: UIUserInterfaceStyle
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let controller = KsSettingsViewController(root: root, theme: Theme(), style: .classic)
        let window = UIWindow(frame: CGRect(origin: .zero, size: Self.viewSize))
        window.overrideUserInterfaceStyle = userInterfaceStyle
        window.rootViewController = controller
        window.makeKeyAndVisible()
        let rootView = controller.view!
        rootView.frame = CGRect(origin: .zero, size: Self.viewSize)
        rootView.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: Self.viewSize)
        awaitInitialRender(controller)
        return (controller, cv, window)
    }

    /// window の外観を切り替え、レイアウトを確定させる。
    private func switchAppearance(
        _ window: UIWindow,
        to style: UIUserInterfaceStyle,
        controller: KsSettingsViewController
    ) {
        window.overrideUserInterfaceStyle = style
        window.layoutIfNeeded()
        layoutNow(controller.internalCollectionView)
    }

    /// 先頭行の title label に適用された色を、その label 自身の trait で解決して返す。
    private func appliedTitleColor(_ cv: UICollectionView) -> UIColor? {
        guard let cell = cv.cellForItem(at: IndexPath(item: 0, section: 0)) as? KsListCellBase else {
            return nil
        }
        return cell.titleLabel.textColor.resolvedColor(with: cell.titleLabel.traitCollection)
    }

    /// 先頭行に適用された背景色を、その Cell 自身の trait で解決して返す。
    private func appliedCellBackgroundColor(_ cv: UICollectionView) -> UIColor? {
        guard let cell = cv.cellForItem(at: IndexPath(item: 0, section: 0)) as? UICollectionViewListCell,
              let color = cell.backgroundConfiguration?.backgroundColor else { return nil }
        return color.resolvedColor(with: cell.traitCollection)
    }

    private func components(hex: UInt32) -> (r: CGFloat, g: CGFloat, b: CGFloat) {
        return (
            CGFloat((hex >> 16) & 0xFF) / 255.0,
            CGFloat((hex >> 8) & 0xFF) / 255.0,
            CGFloat(hex & 0xFF) / 255.0
        )
    }

    private func assertColor(
        _ color: UIColor?,
        isCloseTo expected: (r: CGFloat, g: CGFloat, b: CGFloat),
        _ label: String,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        guard let color else {
            return XCTFail("\(label): 色が設定されていない", file: file, line: line)
        }
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        color.getRed(&r, green: &g, blue: &b, alpha: &a)
        let distance = abs(r - expected.r) + abs(g - expected.g) + abs(b - expected.b)
        XCTAssertLessThan(
            distance, 1.0 / 512.0,
            "\(label): 期待 \(expected) に対し実測 (\(r), \(g), \(b))",
            file: file, line: line
        )
    }

    /// 両外観で異なる値を持つ色。利用者が dynamic な `UIColor` を渡す形を模す。
    private func dynamicUserColor() -> UIColor {
        return UIColor { trait in
            trait.userInterfaceStyle == .dark
                ? UIColor(red: 0.2, green: 0.4, blue: 0.6, alpha: 1.0)
                : UIColor(red: 0.9, green: 0.8, blue: 0.7, alpha: 1.0)
        }
    }

    // MARK: - dynamic な色はその色自身の dark 値になる

    /// 利用者が `CellStyle.titleColor` に渡した dynamic 色は、ダーク外観でその色の dark 値へ
    /// 解決される。ライブラリの dark 既定（`UIColor.label` の dark 値）には置き換わらない。
    func test_CellStyleのdynamic色はダーク外観でその色のdark値になる() {
        let userColor = dynamicUserColor()
        let (_, cv, window) = host(
            root: makeRoot(style: CellStyle(titleColor: userColor)),
            userInterfaceStyle: .dark
        )
        defer { window.isHidden = true }

        assertColor(
            appliedTitleColor(cv),
            isCloseTo: (0.2, 0.4, 0.6),
            "ダーク外観での title 色"
        )

        // ライブラリの既定（未指定時の title 色）の dark 値とは異なる。
        let libraryDefault = UIColor.label.resolvedColor(
            with: UITraitCollection(userInterfaceStyle: .dark)
        )
        var lr: CGFloat = 0, lg: CGFloat = 0, lb: CGFloat = 0, la: CGFloat = 0
        libraryDefault.getRed(&lr, green: &lg, blue: &lb, alpha: &la)
        XCTAssertGreaterThan(
            abs(lr - 0.2) + abs(lg - 0.4) + abs(lb - 0.6), 0.05,
            "利用者の色はライブラリの dark 既定と別の値である"
        )
    }

    /// 表示中に外観を切り替えても、dynamic 色は同じ行のまま dark 値へ描き直される。
    func test_表示中に外観を切り替えるとdynamic色はdark値へ描き直される() {
        let userColor = dynamicUserColor()
        let (controller, cv, window) = host(
            root: makeRoot(style: CellStyle(titleColor: userColor)),
            userInterfaceStyle: .light
        )
        defer { window.isHidden = true }

        assertColor(appliedTitleColor(cv), isCloseTo: (0.9, 0.8, 0.7), "切替前の title 色")
        let cellBefore = cv.cellForItem(at: IndexPath(item: 0, section: 0))

        switchAppearance(window, to: .dark, controller: controller)

        assertColor(appliedTitleColor(cv), isCloseTo: (0.2, 0.4, 0.6), "切替後の title 色")
        XCTAssertTrue(
            cellBefore === cv.cellForItem(at: IndexPath(item: 0, section: 0)),
            "外観切替で行の identity は維持される"
        )
    }

    // MARK: - 固定色は外観で変わらない

    /// 固定色で明示した title 色は外観切替で変わらず、同じ行の未指定の背景だけが
    /// dark 既定の値で描き直される。
    func test_CellStyleの固定色は外観切替で変わらず未指定の背景だけがdark既定になる() {
        let fixed = UIColor(red: 0.9, green: 0.7, blue: 0.3, alpha: 1.0)
        let (controller, cv, window) = host(
            root: makeRoot(style: CellStyle(titleColor: fixed)),
            userInterfaceStyle: .light
        )
        defer { window.isHidden = true }

        assertColor(appliedTitleColor(cv), isCloseTo: (0.9, 0.7, 0.3), "切替前の title 色")
        assertColor(
            appliedCellBackgroundColor(cv),
            isCloseTo: (1.0, 1.0, 1.0),
            "切替前の未指定の行背景"
        )

        switchAppearance(window, to: .dark, controller: controller)

        assertColor(appliedTitleColor(cv), isCloseTo: (0.9, 0.7, 0.3), "切替後の title 色")
        assertColor(
            appliedCellBackgroundColor(cv),
            isCloseTo: components(hex: 0x1C1C1E),
            "切替後の未指定の行背景"
        )
    }
}
#endif
