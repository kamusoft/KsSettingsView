// PresentationAppearanceTests.swift
// KsSettingsViewUITests
//
// Cell から提示するモーダル（PickerCell の選択面・DatePickerCell のカレンダーシート）が、
// 提示元の実効外観を引き継ぐことを検証する。
//
// 問題になるホスト構成は「window は端末の外観のまま、root view controller 側にだけ
// `overrideUserInterfaceStyle` が掛かっている」形。提示物の土台になる container view は
// window 直下に置かれるため、引き継ぎが無いと提示物の中身と地色が食い違う。
//
// 提示そのもの（`present(_:animated:)`）はテスト環境で再現できない。`KeyWindowResolver` は
// `UIApplication.connectedScenes` から key window を引くが、テストランナーには window scene が
// 存在しないため提示先が解決されない。そのため検証は、提示経路とテストが共有する「提示する VC の
// 組み立て」seam に対して行う。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class PresentationAppearanceTests: XCTestCase {

    // MARK: - ヘルパ

    private var window: UIWindow?

    override func tearDown() {
        window?.isHidden = true
        window = nil
        super.tearDown()
    }

    /// window と root view controller に別々の外観を与えたホストを作り、root を返す。
    private func makeHost(
        windowStyle: UIUserInterfaceStyle,
        rootStyle: UIUserInterfaceStyle
    ) -> UIViewController {
        let w = UIWindow(frame: CGRect(x: 0, y: 0, width: 375, height: 600))
        w.overrideUserInterfaceStyle = windowStyle
        let root = UIViewController()
        root.overrideUserInterfaceStyle = rootStyle
        w.rootViewController = root
        w.isHidden = false
        root.view.layoutIfNeeded()
        self.window = w
        return root
    }

    /// Cell view を root の view 階層へ載せ、実効 trait を root から継承させる。
    private func attach(_ view: UIView, to root: UIViewController) {
        view.frame = CGRect(x: 0, y: 0, width: 375, height: 44)
        root.view.addSubview(view)
        root.view.layoutIfNeeded()
    }

    // MARK: - 引き継ぐ外観の決定

    func test_styleToInherit_windowが提示元と食い違うなら提示元の外観を返す() {
        XCTAssertEqual(
            PresentationAppearance.styleToInherit(source: .dark, window: .light),
            .dark
        )
        XCTAssertEqual(
            PresentationAppearance.styleToInherit(source: .light, window: .dark),
            .light
        )
    }

    func test_styleToInherit_windowが提示元と同じなら上書きしない() {
        XCTAssertNil(PresentationAppearance.styleToInherit(source: .dark, window: .dark))
        XCTAssertNil(PresentationAppearance.styleToInherit(source: .light, window: .light))
    }

    func test_styleToInherit_提示元がunspecifiedなら上書きしない() {
        XCTAssertNil(PresentationAppearance.styleToInherit(source: .unspecified, window: .light))
    }

    // MARK: - 引き継ぎの適用

    func test_inherit_提示元がwindowと食い違うときVCと提示コントローラの双方へ与える() {
        let root = makeHost(windowStyle: .light, rootStyle: .dark)
        let source = UIView()
        attach(source, to: root)
        let presented = UIViewController()

        PresentationAppearance.inherit(from: source, to: presented)

        XCTAssertEqual(presented.overrideUserInterfaceStyle, .dark)
        XCTAssertEqual(
            presented.presentationController?.overrideTraitCollection?.userInterfaceStyle,
            .dark,
            "sheet の地色は presentation controller 側の外観で描かれるため、そちらにも引き継ぐ"
        )
    }

    func test_inherit_window未接続の提示元では何もしない() {
        let source = UIView()
        source.overrideUserInterfaceStyle = .dark
        let presented = UIViewController()

        PresentationAppearance.inherit(from: source, to: presented)

        XCTAssertEqual(presented.overrideUserInterfaceStyle, .unspecified)
        XCTAssertNil(presented.presentationController?.overrideTraitCollection)
    }

    // MARK: - DatePickerCell のカレンダーシート

    func test_カレンダーシートはwindowと食い違う提示元の外観を引き継ぐ() throws {
        let sheet = try makeCalendarSheet(windowStyle: .light, rootStyle: .dark)

        XCTAssertEqual(sheet.overrideUserInterfaceStyle, .dark)
        XCTAssertEqual(
            sheet.presentationController?.overrideTraitCollection?.userInterfaceStyle,
            .dark
        )
    }

    func test_カレンダーシートはwindowと同じ外観なら上書きを付けない() throws {
        let sheet = try makeCalendarSheet(windowStyle: .dark, rootStyle: .dark)

        XCTAssertEqual(sheet.overrideUserInterfaceStyle, .unspecified)
        // presentationController 自体が無いと下の XCTAssertNil は無条件に通り、検出力を失う。
        XCTAssertNotNil(sheet.presentationController)
        XCTAssertNil(sheet.presentationController?.overrideTraitCollection)
    }

    private func makeCalendarSheet(
        windowStyle: UIUserInterfaceStyle,
        rootStyle: UIUserInterfaceStyle
    ) throws -> DatePickerCalendarSheetController {
        let root = makeHost(windowStyle: windowStyle, rootStyle: rootStyle)
        let view = DatePickerCellView()
        attach(view, to: root)
        view.render(
            cell: DatePickerCell(title: "予約日", date: Date(), uiStyle: .calendar),
            theme: Theme()
        )
        return try XCTUnwrap(view._makeCalendarSheetControllerForTesting())
    }

    // MARK: - PickerCell の選択面

    func test_選択面はwindowと食い違う提示元の外観を引き継ぐ() throws {
        let presented = try makePickerScreen(windowStyle: .light, rootStyle: .dark)

        XCTAssertEqual(presented.overrideUserInterfaceStyle, .dark)
        XCTAssertEqual(
            presented.presentationController?.overrideTraitCollection?.userInterfaceStyle,
            .dark
        )
    }

    func test_選択面はwindowと同じ外観なら上書きを付けない() throws {
        let presented = try makePickerScreen(windowStyle: .dark, rootStyle: .dark)

        XCTAssertEqual(presented.overrideUserInterfaceStyle, .unspecified)
        // presentationController 自体が無いと下の XCTAssertNil は無条件に通り、検出力を失う。
        XCTAssertNotNil(presented.presentationController)
        XCTAssertNil(presented.presentationController?.overrideTraitCollection)
    }

    private func makePickerScreen(
        windowStyle: UIUserInterfaceStyle,
        rootStyle: UIUserInterfaceStyle
    ) throws -> UINavigationController {
        let root = makeHost(windowStyle: windowStyle, rootStyle: rootStyle)
        let view = PickerCellView()
        attach(view, to: root)
        view.render(
            cell: PickerCell(title: "サイズ", items: ["A", "B"], selectedIndex: 0),
            theme: Theme()
        )
        return try XCTUnwrap(view._makePresentedViewControllerForTesting())
    }
}
#endif
