// ThemeDarkAppearanceRenderingTests.swift
// KsSettingsViewUITests
//
// Theme を渡さない SettingsView がダーク外観で dark セットの色で描かれること、表示中の外観切替に
// 追随すること、明示指定した固定色は追随しないことを検証する。
//
// list の下地は実描画した画素で観測する。Cell 背景・Header / Footer の文字色・separator の色は、
// 表示中の実体へ実際に適用された値（`UICollectionViewListCell.backgroundConfiguration`、
// supplementary の `UILabel.textColor`、`separatorConfiguration(for:base:)` が返す構成）を、
// その実体の trait で解決して観測する。Cell の背景は `UIBackgroundConfiguration` が描く領域で、
// `CALayer.render(in:)` による画像化には現れないため画素では観測しない。

#if canImport(UIKit)
import XCTest
import UIKit
import KsSettingsViewTestSupport
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class ThemeDarkAppearanceRenderingTests: XCTestCase {
    private static let viewSize = CGSize(width: 375, height: 700)

    // MARK: - ハーネス

    private func makeRoot() -> SettingsRoot {
        let section = KsSettingsViewCore.Section(
            header: .text("一般"),
            footer: .text("補足"),
            cells: [LabelCell(title: "A"), LabelCell(title: "B")]
        )
        return SettingsRoot(sections: [section])
    }

    /// controller を指定外観の window に載せて実レイアウトを走らせる。
    private func host(
        root: SettingsRoot,
        theme: Theme = Theme(),
        style: KsSettingsViewStyle = .classic,
        userInterfaceStyle: UIUserInterfaceStyle
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let controller = KsSettingsViewController(root: root, theme: theme, style: style)
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

    private func components(hex: UInt32) -> (r: CGFloat, g: CGFloat, b: CGFloat) {
        return (
            CGFloat((hex >> 16) & 0xFF) / 255.0,
            CGFloat((hex >> 8) & 0xFF) / 255.0,
            CGFloat(hex & 0xFF) / 255.0
        )
    }

    private func render(_ cv: UICollectionView) -> UIImage {
        let renderer = UIGraphicsImageRenderer(bounds: cv.bounds)
        return renderer.image { context in
            cv.layer.render(in: context.cgContext)
        }
    }

    private func pixelColor(_ image: UIImage, at point: CGPoint) -> (CGFloat, CGFloat, CGFloat)? {
        guard let cgImage = image.cgImage else { return nil }
        let scale = image.scale
        let x = Int(point.x * scale)
        let y = Int(point.y * scale)
        guard x >= 0, y >= 0, x < cgImage.width, y < cgImage.height else { return nil }
        var pixel: [UInt8] = [0, 0, 0, 0]
        guard let context = CGContext(
            data: &pixel,
            width: 1,
            height: 1,
            bitsPerComponent: 8,
            bytesPerRow: 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ) else { return nil }
        context.translateBy(x: -CGFloat(x), y: -CGFloat(y))
        context.draw(cgImage, in: CGRect(x: 0, y: 0, width: cgImage.width, height: cgImage.height))
        return (CGFloat(pixel[0]) / 255.0, CGFloat(pixel[1]) / 255.0, CGFloat(pixel[2]) / 255.0)
    }

    private func assertPixel(
        _ image: UIImage,
        at point: CGPoint,
        isCloseTo expected: (r: CGFloat, g: CGFloat, b: CGFloat),
        _ label: String,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        guard let actual = pixelColor(image, at: point) else {
            return XCTFail("\(label): 画素を読めない", file: file, line: line)
        }
        let distance = abs(actual.0 - expected.r) + abs(actual.1 - expected.g) + abs(actual.2 - expected.b)
        XCTAssertLessThan(
            distance, 0.05,
            "\(label): 期待 \(expected) に対し実測 \(actual)",
            file: file, line: line
        )
    }

    /// 解決済みの色が期待の生値と一致することを検査する。
    private func assertColor(
        _ color: UIColor?,
        equalsHex hex: UInt32,
        _ label: String,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        guard let color else {
            return XCTFail("\(label): 色が設定されていない", file: file, line: line)
        }
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        color.getRed(&r, green: &g, blue: &b, alpha: &a)
        let expected = components(hex: hex)
        let distance = abs(r - expected.r) + abs(g - expected.g) + abs(b - expected.b)
        XCTAssertLessThan(
            distance, 1.0 / 512.0,
            "\(label): 期待 \(expected) に対し実測 (\(r), \(g), \(b))",
            file: file, line: line
        )
    }

    /// 表示中の先頭 Cell に適用された背景色を、その Cell 自身の trait で解決して返す。
    private func appliedCellBackgroundColor(_ cv: UICollectionView) -> UIColor? {
        guard let cell = cv.cellForItem(at: IndexPath(item: 0, section: 0)) as? UICollectionViewListCell,
              let color = cell.backgroundConfiguration?.backgroundColor else { return nil }
        return color.resolvedColor(with: cell.traitCollection)
    }

    /// 表示中の supplementary の文字色を、その実体自身の trait で解決して返す。
    private func supplementaryTextColor(_ cv: UICollectionView, kind: String) -> UIColor? {
        guard let cell = cv.visibleSupplementaryViews(ofKind: kind).first as? UICollectionViewListCell,
              let label = cell.contentView.subviews.compactMap({ $0 as? UILabel }).first else { return nil }
        return label.textColor.resolvedColor(with: label.traitCollection)
    }

    /// どの Section にも覆われない下地の、描画画像上の観測 Y。
    private func backgroundProbeY(_ cv: UICollectionView) -> CGFloat {
        return cv.bounds.height - 10
    }

    // MARK: - ダーク外観での既定色

    func test_Themeを渡さないlistはダークでdarkセットの色で描かれる() {
        let (controller, cv, window) = host(root: makeRoot(), userInterfaceStyle: .dark)
        defer { window.isHidden = true }

        let image = render(cv)
        assertPixel(
            image, at: CGPoint(x: cv.bounds.midX, y: backgroundProbeY(cv)),
            isCloseTo: components(hex: 0x000000), "list 下地"
        )
        assertColor(
            appliedCellBackgroundColor(cv),
            equalsHex: 0x1C1C1E, "Cell 背景"
        )

        assertColor(
            supplementaryTextColor(cv, kind: UICollectionView.elementKindSectionHeader),
            equalsHex: 0x8E8E93, "Header 文字"
        )
        assertColor(
            supplementaryTextColor(cv, kind: UICollectionView.elementKindSectionFooter),
            equalsHex: 0x8E8E93, "Footer 文字"
        )

        let separator = controller.separatorConfiguration(
            for: IndexPath(item: 0, section: 0),
            base: UIListSeparatorConfiguration(listAppearance: .plain)
        )
        assertColor(
            separator.color.resolvedColor(with: cv.traitCollection),
            equalsHex: 0x38383A, "separator"
        )
    }

    // MARK: - 表示中の外観切替

    func test_表示中に外観をダークへ切り替えると既定色が描き直される() {
        let (controller, cv, window) = host(root: makeRoot(), userInterfaceStyle: .light)
        defer { window.isHidden = true }

        let before = render(cv)
        assertPixel(
            before, at: CGPoint(x: cv.bounds.midX, y: backgroundProbeY(cv)),
            isCloseTo: (1.0, 1.0, 1.0), "切替前の list 下地"
        )
        let cellBefore = cv.cellForItem(at: IndexPath(item: 0, section: 0))
        XCTAssertNotNil(cellBefore, "切替前に先頭 Cell が実体化している")

        switchAppearance(window, to: .dark, controller: controller)

        let after = render(cv)
        assertPixel(
            after, at: CGPoint(x: cv.bounds.midX, y: backgroundProbeY(cv)),
            isCloseTo: components(hex: 0x000000), "切替後の list 下地"
        )
        assertColor(
            appliedCellBackgroundColor(cv),
            equalsHex: 0x1C1C1E, "切替後の Cell 背景"
        )
        assertColor(
            supplementaryTextColor(cv, kind: UICollectionView.elementKindSectionHeader),
            equalsHex: 0x8E8E93, "切替後の Header 文字"
        )
        XCTAssertTrue(
            cellBefore === cv.cellForItem(at: IndexPath(item: 0, section: 0)),
            "外観切替で Cell の identity は維持される"
        )
    }

    /// 明示指定した固定色は外観が変わっても置き換えられない。
    func test_明示指定した固定色は外観切替で変わらない() {
        let fixed = UIColor(red: 0.9, green: 0.7, blue: 0.3, alpha: 1.0)
        let (controller, cv, window) = host(
            root: makeRoot(),
            theme: Theme(backgroundColor: fixed),
            userInterfaceStyle: .light
        )
        defer { window.isHidden = true }

        switchAppearance(window, to: .dark, controller: controller)

        let image = render(cv)
        assertPixel(
            image, at: CGPoint(x: cv.bounds.midX, y: backgroundProbeY(cv)),
            isCloseTo: (0.9, 0.7, 0.3), "明示指定した list 下地"
        )
    }

    // MARK: - Section 装飾の枠線

    func test_dynamicなsectionBorderColorが外観切替で再解決される() {
        let borderColor = UIColor { trait in
            trait.userInterfaceStyle == .dark
                ? UIColor(red: 0.2, green: 0.4, blue: 0.6, alpha: 1.0)
                : UIColor(red: 0.9, green: 0.1, blue: 0.1, alpha: 1.0)
        }
        let theme = Theme(sectionBorderWidth: 2.0, sectionBorderColor: borderColor)
        let (controller, cv, window) = host(
            root: makeRoot(),
            theme: theme,
            style: .modern,
            userInterfaceStyle: .light
        )
        defer { window.isHidden = true }

        guard let decoration = cv.subviews.compactMap({ $0 as? SectionBoxDecorationView }).first else {
            return XCTFail("Section の箱の decoration view が実体化していない")
        }
        assertBorder(decoration, isCloseTo: (0.9, 0.1, 0.1), "切替前の枠線")

        switchAppearance(window, to: .dark, controller: controller)

        assertBorder(decoration, isCloseTo: (0.2, 0.4, 0.6), "切替後の枠線")
    }

    private func assertBorder(
        _ view: SectionBoxDecorationView,
        isCloseTo expected: (r: CGFloat, g: CGFloat, b: CGFloat),
        _ label: String,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        guard let border = view.layer.borderColor,
              let converted = UIColor(cgColor: border).cgColor.components,
              converted.count >= 3 else {
            return XCTFail("\(label): 枠線色を読めない", file: file, line: line)
        }
        let distance = abs(converted[0] - expected.r) + abs(converted[1] - expected.g)
            + abs(converted[2] - expected.b)
        XCTAssertLessThan(
            distance, 1.0 / 512.0,
            "\(label): 期待 \(expected) に対し実測 (\(converted[0]), \(converted[1]), \(converted[2]))",
            file: file, line: line
        )
    }
}
#endif
