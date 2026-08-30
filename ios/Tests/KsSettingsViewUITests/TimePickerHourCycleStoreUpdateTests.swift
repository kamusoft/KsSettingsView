// TimePickerHourCycleStoreUpdateTests.swift
// KsSettingsViewUITests
//
// `TimePickerCell` の `is24Hour` を Store の公開操作で変更したとき、
// Store → Host → 行の再バインドを通って picker の時制へ届くことを検証する。
//
// `is24Hour` は生成後の変更が表示へ反映される動的反映プロパティであり、Store 経路と
// DSL 経路の双方に反映テストを置く契約になっている (core/ADR-0018)。本ファイルは Store 経路の担保で、
// DSL 経路は KsSettingsViewSwiftUITests の DSLTimePickerHourCycleTests が受け持つ。
//
// 検証は window に載せた実物の行から取り出した picker の時制で行う。Renderer の直呼びでは
// 更新経路のどこかで `is24Hour` の変化が取りこぼされる無音の失敗を検出できない。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class TimePickerHourCycleStoreUpdateTests: XCTestCase {

    /// Store に接続した Controller を window に載せ、行の実描画を確定させる。
    ///
    /// Store 購読は `[weak self]` で張られるため、window に controller を強参照させて
    /// 更新が届く所有関係を作る。
    private func hostController(
        store: SettingsRootStore
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let controller = KsSettingsViewController(store: store)
        let size = CGSize(width: 375, height: 600)
        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        let root = controller.view!
        root.frame = CGRect(origin: .zero, size: size)
        root.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        pump(cv)
        return (controller, cv, window)
    }

    /// レイアウトと再構成を確定させる。
    private func pump(_ view: UIView, seconds: TimeInterval = 0.05) {
        view.setNeedsLayout()
        view.layoutIfNeeded()
        RunLoop.current.run(until: Date().addingTimeInterval(seconds))
        view.setNeedsLayout()
        view.layoutIfNeeded()
    }

    /// 先頭 Section の先頭行が実際に提示する picker の時制を返す。
    ///
    /// `j` は「その Locale の時制に従う時」を表すテンプレートで、24時間制なら `H` / `k`、
    /// 12時間制なら `h` / `K` に解決される。
    private func renderedIs24Hour(_ cv: UICollectionView) -> Bool? {
        let row = cv.cellForItem(at: IndexPath(item: 0, section: 0))
        guard let view = row as? TimePickerCellView, let locale = view._pickerLocale else {
            return nil
        }
        guard let pattern = DateFormatter.dateFormat(fromTemplate: "j", options: 0, locale: locale) else {
            return nil
        }
        if pattern.contains("H") || pattern.contains("k") { return true }
        if pattern.contains("h") || pattern.contains("K") { return false }
        return nil
    }

    /// Store の `replaceCell` で `is24Hour` だけを変えた更新が、次に開く picker の時制へ届く。
    func test_Store経路のis24Hour変更がpickerの時制へ届く() {
        let cellID = UUID()
        let time = Calendar.current.date(bySettingHour: 22, minute: 15, second: 0, of: Date())!
        let sec = Section(
            header: .text("TimePickerCell"),
            cells: [TimePickerCell(id: cellID, title: "就寝", time: time)]
        )
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        let (controller, cv, window) = hostController(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        XCTAssertEqual(renderedIs24Hour(cv), true, "前提: 初期表示の picker が 24 時間制になっていない")

        store.replaceCell(
            cellID: KsCellID(id: cellID),
            new: TimePickerCell(id: cellID, title: "就寝", time: time, is24Hour: false)
        )
        pump(cv)

        XCTAssertEqual(renderedIs24Hour(cv), false, "Store 経由の is24Hour 変更が picker へ届いていない")
    }

    /// 24時間制へ戻す方向の更新も同じ経路で届く。
    func test_Store経路のis24Hour変更は24時間制へ戻す方向でも届く() {
        let cellID = UUID()
        let time = Calendar.current.date(bySettingHour: 22, minute: 15, second: 0, of: Date())!
        let sec = Section(
            header: .text("TimePickerCell"),
            cells: [TimePickerCell(id: cellID, title: "就寝", time: time, is24Hour: false)]
        )
        let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [sec]))
        let (controller, cv, window) = hostController(store: store)
        defer {
            window.isHidden = true
            withExtendedLifetime(controller) {}
        }

        XCTAssertEqual(renderedIs24Hour(cv), false, "前提: 初期表示の picker が 12 時間制になっていない")

        store.replaceCell(
            cellID: KsCellID(id: cellID),
            new: TimePickerCell(id: cellID, title: "就寝", time: time, is24Hour: true)
        )
        pump(cv)

        XCTAssertEqual(renderedIs24Hour(cv), true, "Store 経由の is24Hour 変更が picker へ届いていない")
    }
}
#endif
