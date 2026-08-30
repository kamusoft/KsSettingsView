// InputCellsTests.swift
// KsSettingsViewUITests
//
// 入力系 Cell 5 種（EntryCell / PickerCell / NumberPickerCell / TimePickerCell / DatePickerCell）の
// bind / TwoWay / 共通フィールド / VisibilityAware / isEnabled / 再利用クリアを検証する。

#if canImport(UIKit)
import XCTest
import UIKit
import SwiftUI
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class InputCellsTests: XCTestCase {

    // MARK: - id デフォルト値規約

    func test_入力系Cell5種_idデフォルトUUID自動採番() {
        XCTAssertNotEqual(EntryCell(title: "x", text: "").id, EntryCell(title: "x", text: "").id)
        XCTAssertNotEqual(
            PickerCell(title: "x", items: [], selectedIndex: nil).id,
            PickerCell(title: "x", items: [], selectedIndex: nil).id
        )
        XCTAssertNotEqual(
            NumberPickerCell(title: "x", value: 0).id,
            NumberPickerCell(title: "x", value: 0).id
        )
        XCTAssertNotEqual(
            TimePickerCell(title: "x", time: Date()).id,
            TimePickerCell(title: "x", time: Date()).id
        )
        XCTAssertNotEqual(
            DatePickerCell(title: "x", date: Date()).id,
            DatePickerCell(title: "x", date: Date()).id
        )
    }

    // MARK: - PickerSelectionMode 列挙

    func test_PickerSelectionMode_2ケース定義() {
        XCTAssertNotEqual(PickerSelectionMode.single, PickerSelectionMode.multiple)
        XCTAssertEqual(PickerSelectionMode.single, PickerSelectionMode.single)
    }

    // MARK: - EntryCell の基本

    func test_EntryCell_既定値() {
        let cell = EntryCell(title: "メモ", text: "")
        XCTAssertEqual(cell.keyboardType, .default)
        XCTAssertEqual(cell.isPassword, false)
        // textAlignment の既定は `.end`（AiForms `EntryCell.TextAlignmentProperty` の `TextAlignment.End` 準拠）
        XCTAssertEqual(cell.textAlignment, .end)
        XCTAssertNil(cell.maxLength)
        XCTAssertTrue(cell.isEnabled)
        XCTAssertTrue(cell.isVisible)
    }

    func test_EntryCell_withDSLID_withStyle_withIcon() {
        let original = EntryCell(title: "メモ", text: "hello")
        let newID = UUID()
        let withID = original.withDSLID(newID)
        XCTAssertEqual(withID.id, newID)
        XCTAssertEqual(withID.text, "hello")

        let newStyle = CellStyle(titleColor: .red)
        let withStyle = original.withStyle(newStyle)
        XCTAssertEqual(withStyle.style, newStyle)
        XCTAssertEqual(withStyle.id, original.id)

        let withIcon = original.withIcon(.systemName("pencil"))
        XCTAssertEqual(withIcon.icon, .systemName("pencil"))
    }

    func test_EntryCellView_renderでテキストが反映される() {
        let view = EntryCellView()
        let cell = EntryCell(title: "メモ", text: "abc", placeholder: "入力してください")
        view.render(cell: cell, theme: Theme())
        XCTAssertEqual(view._textField.text, "abc")
        XCTAssertEqual(view._textField.placeholder, "入力してください")
    }

    func test_EntryCellView_keyboardTypeがNative型で直接反映される() {
        let view = EntryCellView()
        let cell = EntryCell(title: "電話", text: "", keyboardType: .phonePad)
        view.render(cell: cell, theme: Theme())
        XCTAssertEqual(view._textField.keyboardType, .phonePad)
    }

    func test_EntryCellView_isPasswordでisSecureTextEntryが切り替わる() {
        let view = EntryCellView()
        view.render(cell: EntryCell(title: "P", text: "", isPassword: true), theme: Theme())
        XCTAssertTrue(view._textField.isSecureTextEntry)
        view.render(cell: EntryCell(title: "P", text: "", isPassword: false), theme: Theme())
        XCTAssertFalse(view._textField.isSecureTextEntry)
    }

    func test_EntryCellView_accentColorがtintColorに反映される() {
        let view = EntryCellView()
        let cell = EntryCell(title: "メモ", text: "", accentColor: .systemPink)
        view.render(cell: cell, theme: Theme())
        XCTAssertEqual(view._textField.tintColor, UIColor.systemPink)
    }

    func test_EntryCellView_TwoWay入力でcallbackが呼ばれる() {
        let view = EntryCellView()
        var captured: String?
        let cell = EntryCell(title: "メモ", text: "", onTextChanged: { captured = $0 })
        view.render(cell: cell, theme: Theme())
        view._simulateTextInput("abc")
        XCTAssertEqual(captured, "abc")
    }

    func test_EntryCellView_maxLength超過時はshouldChangeで拒否() {
        let view = EntryCellView()
        let cell = EntryCell(title: "メモ", text: "abcde", maxLength: 5)
        view.render(cell: cell, theme: Theme())
        // delegate メソッド経由でテスト
        let tf = view._textField
        let result = view.textField(
            tf,
            shouldChangeCharactersIn: NSRange(location: 5, length: 0),
            replacementString: "f"
        )
        XCTAssertFalse(result, "5 文字到達後の追加入力は拒否される")
    }

    func test_EntryCellView_maxLengthがnilのときは無制限() {
        let view = EntryCellView()
        let cell = EntryCell(title: "メモ", text: "abcdefghij")
        view.render(cell: cell, theme: Theme())
        let tf = view._textField
        let result = view.textField(
            tf,
            shouldChangeCharactersIn: NSRange(location: 10, length: 0),
            replacementString: "x"
        )
        XCTAssertTrue(result, "maxLength = nil なら入力は常に許可される")
    }

    func test_EntryCellView_isEnabledFalseでUITextFieldがdisabled() {
        let view = EntryCellView()
        let cell = EntryCell(title: "メモ", text: "", isEnabled: false)
        view.render(cell: cell, theme: Theme())
        XCTAssertFalse(view._textField.isEnabled)
    }

    func test_EntryCellView_prepareForReuseで状態クリア() {
        let view = EntryCellView()
        view.render(cell: EntryCell(title: "メモ", text: "abc", onTextChanged: { _ in }), theme: Theme())
        view.prepareForReuse()
        XCTAssertNil(view.textChangedHandler)
        // UITextField.text は `nil` を設定しても空文字列を返す実装になっているため、
        // 「テキストが残っていない」ことを `isEmpty` で判定する。
        XCTAssertTrue((view._textField.text ?? "").isEmpty)
    }

    func test_EntryCell_VisibilityAware準拠() {
        let cell = EntryCell(title: "x", text: "", isVisible: false)
        let aware = cell as VisibilityAware
        XCTAssertFalse(aware.isVisible)
    }

    func test_EntryCell_共通フィールドのEquatable() {
        let id = UUID()
        let a = EntryCell(id: id, title: "x", text: "v")
        let b = EntryCell(id: id, title: "x", text: "v")
        XCTAssertEqual(a, b)
        let c = EntryCell(id: id, title: "x", text: "w")
        XCTAssertNotEqual(a, c)
    }

    // MARK: - EntryCell の placeholder 色

    func test_EntryCellView_placeholder色指定で指定色のattributed表示になる() {
        let view = EntryCellView()
        let cell = EntryCell(title: "メモ", text: "", placeholder: "入力してください", placeholderColor: .systemPink)
        view.render(cell: cell, theme: Theme())

        XCTAssertEqual(view._textField.attributedPlaceholder?.string, "入力してください")
        XCTAssertEqual(placeholderColor(of: view), UIColor.systemPink)
    }

    func test_EntryCellView_placeholder色未指定はプレーン表示のまま() {
        let view = EntryCellView()
        let cell = EntryCell(title: "メモ", text: "", placeholder: "入力してください")
        view.render(cell: cell, theme: Theme())

        XCTAssertEqual(view._textField.placeholder, "入力してください")
        assertSystemDefaultPlaceholderColor(view, "色を指定しないとシステム既定色で描画される")
    }

    func test_EntryCellView_placeholder色はCellStyleとThemeから解決される() {
        let styleColor = UIColor.systemGreen
        let themeColor = UIColor.systemBlue

        let fromCell = EntryCellView()
        fromCell.render(
            cell: EntryCell(
                style: CellStyle(placeholderColor: styleColor),
                title: "メモ",
                text: "",
                placeholder: "p",
                placeholderColor: .systemPink
            ),
            theme: Theme(cellPlaceholderColor: themeColor)
        )
        XCTAssertEqual(placeholderColor(of: fromCell), UIColor.systemPink, "Cell 固有値が最優先")

        let fromStyle = EntryCellView()
        fromStyle.render(
            cell: EntryCell(style: CellStyle(placeholderColor: styleColor), title: "メモ", text: "", placeholder: "p"),
            theme: Theme(cellPlaceholderColor: themeColor)
        )
        XCTAssertEqual(placeholderColor(of: fromStyle), styleColor, "Cell 固有値がなければ CellStyle")

        let fromTheme = EntryCellView()
        fromTheme.render(
            cell: EntryCell(title: "メモ", text: "", placeholder: "p"),
            theme: Theme(cellPlaceholderColor: themeColor)
        )
        XCTAssertEqual(placeholderColor(of: fromTheme), themeColor, "CellStyle もなければ Theme")
    }

    func test_EntryCellView_placeholder色指定時のfontは入力テキストと同じ実効fontになる() {
        let valueFont = UIFont.systemFont(ofSize: 21, weight: .bold)
        let view = EntryCellView()
        let cell = EntryCell(
            style: CellStyle(valueTextFont: valueFont),
            title: "メモ",
            text: "",
            placeholder: "入力してください",
            placeholderColor: .systemPink
        )
        view.render(cell: cell, theme: Theme())

        let attributedFont: UIFont? = placeholderAttribute(of: view, .font)
        XCTAssertEqual(attributedFont, valueFont)
        XCTAssertEqual(view._textField.font, valueFont, "入力テキストと同じ font を使う")
    }

    func test_EntryCellView_入力済みテキストの色にplaceholder色は影響しない() {
        let view = EntryCellView()
        let cell = EntryCell(
            style: CellStyle(valueTextColor: .systemGreen),
            title: "メモ",
            text: "abc",
            placeholder: "入力してください",
            placeholderColor: .systemPink
        )
        view.render(cell: cell, theme: Theme())

        XCTAssertEqual(view._textField.textColor, UIColor.systemGreen)
        XCTAssertEqual(placeholderColor(of: view), UIColor.systemPink)
    }

    func test_EntryCellView_明示したplaceholder色は無効状態でも変わらない() {
        let view = EntryCellView()
        let cell = EntryCell(
            title: "メモ",
            text: "abc",
            placeholder: "入力してください",
            placeholderColor: .systemPink,
            isEnabled: false
        )
        view.render(cell: cell, theme: Theme(disabledTextColor: .lightGray))

        XCTAssertEqual(placeholderColor(of: view), UIColor.systemPink)
        XCTAssertEqual(view._textField.textColor, UIColor.lightGray, "無効時の重ねは入力テキストにだけ効く")
    }

    func test_EntryCellView_再利用で前の行のplaceholder色が残らない() {
        let view = EntryCellView()
        view.render(
            cell: EntryCell(title: "メモ", text: "", placeholder: "色つき", placeholderColor: .systemPink),
            theme: Theme()
        )
        XCTAssertEqual(placeholderColor(of: view), UIColor.systemPink)

        view.prepareForReuse()
        XCTAssertNil(view._textField.attributedPlaceholder, "再利用直後に placeholder が残らない")

        view.render(cell: EntryCell(title: "メモ", text: "", placeholder: "色なし"), theme: Theme())
        XCTAssertEqual(view._textField.placeholder, "色なし")
        assertSystemDefaultPlaceholderColor(view, "色未指定の Cell はシステム既定色に戻る")
    }

    func test_EntryCellView_色指定から未指定への再bindでプレーン表示に戻る() {
        let view = EntryCellView()
        view.render(
            cell: EntryCell(title: "メモ", text: "", placeholder: "p", placeholderColor: .systemPink),
            theme: Theme()
        )
        view.render(cell: EntryCell(title: "メモ", text: "", placeholder: "p"), theme: Theme())

        assertSystemDefaultPlaceholderColor(view)
    }

    func test_EntryCellView_placeholder文字列nilなら色指定があってもplaceholderを持たない() {
        let view = EntryCellView()
        view.render(
            cell: EntryCell(title: "メモ", text: "", placeholder: nil, placeholderColor: .systemPink),
            theme: Theme()
        )

        XCTAssertNil(view._textField.attributedPlaceholder)
        XCTAssertNil(view._textField.placeholder)
        XCTAssertTrue(view._textField.isEnabled, "入力欄は通常どおり動作する")
    }

    func test_EntryCellView_placeholder空文字列は色指定があっても安全に描画される() {
        let view = EntryCellView()
        view.render(
            cell: EntryCell(title: "メモ", text: "", placeholder: "", placeholderColor: .systemPink),
            theme: Theme()
        )

        XCTAssertEqual(view._textField.attributedPlaceholder?.string ?? "", "")
    }

    func test_applyTheme経由のTheme変更で表示中のplaceholder色が追従する() {
        let root = SettingsRoot(sections: [
            Section(cells: [EntryCell(title: "メモ", text: "", placeholder: "入力してください")])
        ])
        let (controller, cv, window) = hostEntryController(
            root: root,
            theme: Theme(cellPlaceholderColor: .systemGreen)
        )
        defer { window.isHidden = true }

        XCTAssertEqual(placeholderColor(of: entryCell(cv)), UIColor.systemGreen)

        controller.applyTheme(Theme(cellPlaceholderColor: .systemPink))
        pumpEntry(cv)

        XCTAssertEqual(
            placeholderColor(of: entryCell(cv)), UIColor.systemPink,
            "表示中の行の placeholder 色が新しい Theme に追従する"
        )
    }

    func test_EntryCell_placeholderColorだけ異なると非同値() {
        let id = UUID()
        let base = EntryCell(id: id, title: "x", text: "v", placeholder: "p")
        let colored = EntryCell(id: id, title: "x", text: "v", placeholder: "p", placeholderColor: .systemPink)
        XCTAssertNotEqual(base, colored)

        let other = EntryCell(id: id, title: "x", text: "v", placeholder: "p", placeholderColor: .systemGreen)
        XCTAssertNotEqual(colored, other)
    }

    func test_CellStyleとThemeのplaceholderColorだけ異なると非同値() {
        XCTAssertNotEqual(CellStyle(), CellStyle(placeholderColor: .systemPink))
        XCTAssertNotEqual(CellStyle(placeholderColor: .systemPink), CellStyle(placeholderColor: .systemGreen))
        XCTAssertNotEqual(Theme(), Theme(cellPlaceholderColor: .systemPink))
        XCTAssertNotEqual(Theme(cellPlaceholderColor: .systemPink), Theme(cellPlaceholderColor: .systemGreen))
    }

    // MARK: - placeholder 検証ヘルパ

    /// 表示中の placeholder に載っている文字色。プレーン表示（色未指定）では `nil`。
    private func placeholderColor(of view: EntryCellView) -> UIColor? {
        return placeholderAttribute(of: view, .foregroundColor)
    }

    /// placeholder が「色を指定していない `UITextField` と同じ既定色」で描画されることを確かめる。
    ///
    /// プレーン表示の placeholder には UIKit が既定色（動的カラー）を載せるため、素の
    /// `UITextField` から取り出した同じ既定色と照合する。動的カラーは解決後の値で比較する。
    private func assertSystemDefaultPlaceholderColor(
        _ view: EntryCellView,
        _ message: String = "システム既定色で描画される",
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        let reference = UITextField()
        reference.placeholder = "x"
        let expected = reference.attributedPlaceholder?
            .attribute(.foregroundColor, at: 0, effectiveRange: nil) as? UIColor
        XCTAssertNotNil(expected, "素の UITextField から既定色を取得できる", file: file, line: line)
        let traits = UITraitCollection(userInterfaceStyle: .light)
        XCTAssertEqual(
            placeholderColor(of: view)?.resolvedColor(with: traits),
            expected?.resolvedColor(with: traits),
            message,
            file: file, line: line
        )
    }

    /// 表示中の placeholder の先頭文字に載っている属性を取り出す。
    private func placeholderAttribute<T>(of view: EntryCellView, _ key: NSAttributedString.Key) -> T? {
        guard let attributed = view._textField.attributedPlaceholder, attributed.length > 0 else {
            return nil
        }
        return attributed.attribute(key, at: 0, effectiveRange: nil) as? T
    }

    /// Controller を window に載せ、行の実描画を確定させる。
    private func hostEntryController(
        root: SettingsRoot,
        theme: Theme
    ) -> (KsSettingsViewController, UICollectionView, UIWindow) {
        let controller = KsSettingsViewController(root: root, theme: theme)
        let size = CGSize(width: 375, height: 600)
        let hostView = controller.view!
        hostView.frame = CGRect(origin: .zero, size: size)
        let window = UIWindow(frame: hostView.frame)
        window.addSubview(hostView)
        window.makeKeyAndVisible()
        hostView.layoutIfNeeded()
        let cv = controller.internalCollectionView
        cv.frame = CGRect(origin: .zero, size: size)
        pumpEntry(cv)
        return (controller, cv, window)
    }

    /// レイアウトと再構成を確定させる。
    private func pumpEntry(_ view: UIView, seconds: TimeInterval = 0.05) {
        view.setNeedsLayout()
        view.layoutIfNeeded()
        RunLoop.current.run(until: Date().addingTimeInterval(seconds))
        view.setNeedsLayout()
        view.layoutIfNeeded()
    }

    /// 先頭 Section の先頭行に表示されている `EntryCellView` を取り出す。
    private func entryCell(_ cv: UICollectionView) -> EntryCellView {
        guard let cell = cv.cellForItem(at: IndexPath(item: 0, section: 0)) as? EntryCellView else {
            fatalError("先頭行の EntryCellView が表示されていない")
        }
        return cell
    }

    // MARK: - PickerCell（単一）

    func test_PickerCell_既定値() {
        let cell = PickerCell(title: "x", items: ["A", "B"], selectedIndex: 0)
        XCTAssertEqual(cell.selectionMode, .single)
        XCTAssertEqual(cell.selectedIndex, 0)
        XCTAssertEqual(cell.selectedIndices, [])
        XCTAssertEqual(cell.maxSelectedNumber, 0)
    }

    func test_PickerCell_effectiveValueText_単一() {
        let cell = PickerCell(
            title: "テーマ",
            items: ["ライト", "ダーク"],
            selectedIndex: 1
        )
        XCTAssertEqual(cell.effectiveValueText(), "ダーク")
    }

    func test_PickerCell_effectiveValueText_明示指定が優先() {
        let cell = PickerCell(
            title: "テーマ",
            valueText: "カスタム",
            items: ["ライト", "ダーク"],
            selectedIndex: 0
        )
        XCTAssertEqual(cell.effectiveValueText(), "カスタム")
    }

    func test_PickerCell_effectiveValueText_複数_カンマ連結() {
        let cell = PickerCell(
            title: "通知種別",
            items: ["メール", "プッシュ", "SMS"],
            selectedIndices: Set([0, 2])
        )
        XCTAssertEqual(cell.effectiveValueText(), "メール, SMS")
    }

    func test_PickerCellView_renderでvalueText自動表示() {
        let view = PickerCellView()
        let cell = PickerCell(title: "テーマ", items: ["ライト", "ダーク", "自動"], selectedIndex: 1)
        view.render(cell: cell, theme: Theme())
        // valueText の配置検証は test_PickerCellView_valueTextは行内でchevronはアクセサリ列 が担う。
        // ここでは render 時に内部へ Cell が保持されることだけを確認する。
        XCTAssertEqual(view._lastCell?.title, "テーマ")
    }

    // MARK: - 2 系統配置（行内 trailing と Cell 級アクセサリ）

    /// Picker 系 renderer 共通の配線検証ヘルパ。
    /// value label が行内（`contentStack`）に 1 個だけ置かれ、chevron は Cell 級アクセサリ列
    /// （`accessoryHolder`）に置かれて行内には混ざらないことを確かめる。
    private func assertPickerValueInlineAndChevronInAccessory(
        _ view: KsListCellBase,
        expectedValueText: String,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        // 行内 trailing: titleLabel の後ろに value label が 1 個
        let valueLabels = view.contentStack.arrangedSubviews
            .compactMap { $0 as? UILabel }
            .filter { $0 !== view.titleLabel }
        XCTAssertEqual(valueLabels.count, 1, "value label は行内 (contentStack) に 1 個", file: file, line: line)
        XCTAssertEqual(valueLabels.first?.text, expectedValueText, file: file, line: line)
        // Cell 級アクセサリ: chevron は accessoryHolder に 1 個
        XCTAssertEqual(
            view.accessoryHolder.arrangedSubviews.count,
            1,
            "chevron は accessoryHolder に 1 個だけ置かれる",
            file: file,
            line: line
        )
        XCTAssertTrue(
            view.accessoryHolder.arrangedSubviews.first is UIImageView,
            "chevron は makeChevronView() 由来の UIImageView",
            file: file,
            line: line
        )
        XCTAssertFalse(view.accessoryHolder.isHidden, "アクセサリありなので holder は表示される", file: file, line: line)
        // chevron は行内には置かれない
        XCTAssertFalse(
            view.contentStack.arrangedSubviews.contains { $0 is UIImageView },
            "chevron は contentStack には置かれない",
            file: file,
            line: line
        )
    }

    func test_PickerCellView_valueTextは行内でchevronはアクセサリ列() {
        let view = PickerCellView()
        let cell = PickerCell(title: "テーマ", items: ["ライト", "ダーク", "自動"], selectedIndex: 1)
        view.render(cell: cell, theme: Theme())
        assertPickerValueInlineAndChevronInAccessory(view, expectedValueText: "ダーク")
    }

    func test_NumberPickerCellView_valueTextは行内でchevronはアクセサリ列() {
        let view = NumberPickerCellView()
        let cell = NumberPickerCell(title: "音量", value: 60)
        view.render(cell: cell, theme: Theme())
        assertPickerValueInlineAndChevronInAccessory(view, expectedValueText: "60")
    }

    func test_TimePickerCellView_valueTextは行内でchevronはアクセサリ列() {
        let view = TimePickerCellView()
        let time = Calendar.current.date(bySettingHour: 7, minute: 30, second: 0, of: Date())!
        let cell = TimePickerCell(title: "アラーム", time: time, format: "HH:mm")
        view.render(cell: cell, theme: Theme())
        assertPickerValueInlineAndChevronInAccessory(view, expectedValueText: "07:30")
    }

    func test_DatePickerCellView_valueTextは行内でchevronはアクセサリ列() {
        let view = DatePickerCellView()
        let date = Calendar.current.date(from: DateComponents(year: 2000, month: 12, day: 31))!
        let cell = DatePickerCell(title: "誕生日", date: date)
        view.render(cell: cell, theme: Theme())
        assertPickerValueInlineAndChevronInAccessory(view, expectedValueText: "2000/12/31")
    }

    /// EntryCell の入力フィールドは行内（`contentStack`）に置かれ、Cell 級アクセサリの領域は
    /// 確保されない（`accessoryHolder` は空かつ非表示）ことを検証する。
    func test_EntryCellView_入力フィールドは行内でアクセサリ列は空() {
        let view = EntryCellView()
        let cell = EntryCell(title: "名前", text: "abc")
        view.render(cell: cell, theme: Theme())
        XCTAssertTrue(
            view._textField.isDescendant(of: view.contentStack),
            "入力フィールドは contentStack 内（行内 trailing）に配置される"
        )
        XCTAssertTrue(
            view.accessoryHolder.arrangedSubviews.isEmpty,
            "EntryCell は Cell 級アクセサリを持たない"
        )
        XCTAssertTrue(view.accessoryHolder.isHidden, "アクセサリ用の空領域は残らない")
    }

    // MARK: - PickerListViewController（モーダル）

    func test_PickerListViewController_単一選択() {
        var picked: Int?
        let vc = PickerListViewController(
            items: ["A", "B", "C"].map { PickerItem(text: $0) },
            selectionMode: .single,
            selectedIndex: 0,
            selectedIndices: [],
            maxSelectedNumber: 0,
            navigationTitle: nil,
            theme: Theme(),
            cellStyle: CellStyle(),
            cellAccentColor: nil,
            onSingleDone: { picked = $0 },
            onMultiDone: nil
        )
        vc.loadViewIfNeeded()
        vc._simulateSelect(1)
        XCTAssertEqual(picked, 1)
    }

    func test_PickerListViewController_複数選択_完了で確定() {
        var picked: Set<Int>?
        let vc = PickerListViewController(
            items: ["A", "B", "C"].map { PickerItem(text: $0) },
            selectionMode: .multiple,
            selectedIndex: nil,
            selectedIndices: [0],
            maxSelectedNumber: 0,
            navigationTitle: nil,
            theme: Theme(),
            cellStyle: CellStyle(),
            cellAccentColor: nil,
            onSingleDone: nil,
            onMultiDone: { picked = $0 }
        )
        vc.loadViewIfNeeded()
        vc._simulateSelect(1)
        vc._simulateSelect(2)
        vc._simulateDone()
        XCTAssertEqual(picked, Set([0, 1, 2]))
    }

    func test_PickerListViewController_複数選択_maxSelectedNumber上限超過は無視() {
        var picked: Set<Int>?
        let vc = PickerListViewController(
            items: ["A", "B", "C", "D"].map { PickerItem(text: $0) },
            selectionMode: .multiple,
            selectedIndex: nil,
            selectedIndices: [0, 1],
            maxSelectedNumber: 2,
            navigationTitle: nil,
            theme: Theme(),
            cellStyle: CellStyle(),
            cellAccentColor: nil,
            onSingleDone: nil,
            onMultiDone: { picked = $0 }
        )
        vc.loadViewIfNeeded()
        vc._simulateSelect(2) // 上限超過 → 無視
        vc._simulateDone()
        XCTAssertEqual(picked, Set([0, 1]))
    }

    // MARK: - NumberPickerCell

    func test_NumberPickerCell_既定値() {
        let cell = NumberPickerCell(title: "音量", value: 50)
        XCTAssertEqual(cell.min, 0)
        XCTAssertEqual(cell.max, 100)
        XCTAssertEqual(cell.step, 1)
    }

    func test_NumberPickerCell_effectiveValueText_自動() {
        let cell = NumberPickerCell(title: "音量", value: 75)
        XCTAssertEqual(cell.effectiveValueText(), "75")
    }

    func test_NumberPickerCell_effectiveValueText_明示優先() {
        let cell = NumberPickerCell(title: "音量", valueText: "中", value: 50)
        XCTAssertEqual(cell.effectiveValueText(), "中")
    }

    func test_NumberPickerCellView_render_lastCell保持() {
        let view = NumberPickerCellView()
        let cell = NumberPickerCell(title: "音量", value: 60)
        view.render(cell: cell, theme: Theme())
        XCTAssertEqual(view._lastCell?.value, 60)
    }

    func test_NumberPickerCellView_doneで選択値が通知される() {
        var captured: Int?
        let view = NumberPickerCellView()
        let cell = NumberPickerCell(
            title: "音量",
            min: 0, max: 100, step: 5,
            value: 50,
            onValueChanged: { captured = $0 }
        )
        view.render(cell: cell, theme: Theme())
        view._simulateSelect(value: 75)
        view._simulateDone()
        XCTAssertEqual(captured, 75)
    }

    func test_NumberPickerCellView_unit_valueTextに反映() {
        let view = NumberPickerCellView()
        let cell = NumberPickerCell(title: "サイズ", value: 15, unit: "px")
        view.render(cell: cell, theme: Theme())
        XCTAssertEqual(cell.effectiveValueText(), "15 px")
    }

    func test_NumberPickerCellView_unit_PickerUI各行にsuffix() {
        let view = NumberPickerCellView()
        let cell = NumberPickerCell(title: "サイズ", min: 10, max: 20, step: 1, value: 15, unit: "px")
        view.render(cell: cell, theme: Theme())
        XCTAssertEqual(view._pickerTitle(forRow: 0), "10 px")
        XCTAssertEqual(view._pickerTitle(forRow: 5), "15 px")
    }

    func test_NumberPickerCellView_unit空のとき数字のみ() {
        let view = NumberPickerCellView()
        let cell = NumberPickerCell(title: "個数", min: 0, max: 5, value: 3)
        view.render(cell: cell, theme: Theme())
        XCTAssertEqual(cell.effectiveValueText(), "3")
        XCTAssertEqual(view._pickerTitle(forRow: 3), "3")
    }

    func test_NumberPickerCellView_cancelで元値に戻る() {
        var captured: Int?
        let view = NumberPickerCellView()
        let cell = NumberPickerCell(
            title: "音量",
            min: 0, max: 100, step: 5,
            value: 50,
            onValueChanged: { captured = $0 }
        )
        view.render(cell: cell, theme: Theme())
        view._simulateSelect(value: 75)
        view._simulateCancel()
        // Cancel 後は onValueChanged は呼ばれない
        XCTAssertNil(captured)
        // 内部選択は元値に戻る
        XCTAssertEqual(view._currentValue, 50)
    }

    func test_NumberPickerCellView_isEnabledFalseでtapHandlerがnil() {
        let view = NumberPickerCellView()
        view.render(cell: NumberPickerCell(title: "x", value: 0, isEnabled: false), theme: Theme())
        XCTAssertNil(view.tapHandler)
    }

    // MARK: - TimePickerCell

    func test_TimePickerCell_既定format() {
        let cell = TimePickerCell(title: "アラーム", time: Date())
        XCTAssertEqual(cell.format, "HH:mm")
    }

    func test_TimePickerCell_effectiveValueText_format反映() {
        let cal = Calendar.current
        let d = cal.date(bySettingHour: 7, minute: 30, second: 0, of: Date())!
        let cell = TimePickerCell(title: "アラーム", time: d, format: "HH:mm")
        XCTAssertEqual(cell.effectiveValueText(), "07:30")
    }

    func test_TimePickerCellView_lastCell保持() {
        let view = TimePickerCellView()
        let cell = TimePickerCell(title: "アラーム", time: Date())
        view.render(cell: cell, theme: Theme())
        XCTAssertNotNil(view._lastCell)
    }

    // MARK: - TimePickerCell の時制 (is24Hour)

    /// Locale が picker に与える時制を、実際の時刻書式パターンから判定する。
    /// `j` は「その Locale の時制に従う時」を表すテンプレートで、24時間制なら `H` / `k`、
    /// 12時間制なら `h` / `K` に解決される。
    private func resolvedIs24Hour(_ locale: Locale?) -> Bool? {
        guard let locale else { return nil }
        guard let pattern = DateFormatter.dateFormat(fromTemplate: "j", options: 0, locale: locale) else {
            return nil
        }
        if pattern.contains("H") || pattern.contains("k") { return true }
        if pattern.contains("h") || pattern.contains("K") { return false }
        return nil
    }

    func test_TimePickerCell_既定is24Hourは24時間制() {
        XCTAssertTrue(TimePickerCell(title: "アラーム", time: Date()).is24Hour)
    }

    func test_TimePickerCell_is24Hourは等価判定とhashに参加する() {
        let time = Calendar.current.date(bySettingHour: 14, minute: 30, second: 0, of: Date())!
        let id = UUID()
        let base = TimePickerCell(id: id, title: "就寝", time: time)
        let twelveHour = TimePickerCell(id: id, title: "就寝", time: time, is24Hour: false)
        XCTAssertNotEqual(base, twelveHour)
        XCTAssertNotEqual(base.hashValue, twelveHour.hashValue)
        XCTAssertEqual(base, TimePickerCell(id: id, title: "就寝", time: time, is24Hour: true))
    }

    func test_TimePickerCell_withDSLID_withStyle_withIconはis24Hourを保つ() {
        let cell = TimePickerCell(title: "就寝", time: Date(), is24Hour: false)
        XCTAssertFalse(cell.withDSLID(UUID()).is24Hour)
        XCTAssertFalse(cell.withStyle(CellStyle()).is24Hour)
        XCTAssertFalse(cell.withIcon(nil).is24Hour)
    }

    /// 端末 Locale が 12 時間制 (en_US) でも 24 時間制 (ja_JP) でも、時制は指定どおりに決まる。
    func test_HourCycleLocale_時制は基準Localeの既定時制に依存しない() {
        let twelveHourBase = Locale(identifier: "en_US")
        let twentyFourHourBase = Locale(identifier: "ja_JP")
        // 前提: 基準 Locale 自身の既定時制は互いに逆であること
        XCTAssertEqual(resolvedIs24Hour(twelveHourBase), false)
        XCTAssertEqual(resolvedIs24Hour(twentyFourHourBase), true)

        XCTAssertEqual(resolvedIs24Hour(HourCycleLocale.forcing(is24Hour: true, base: twelveHourBase)), true)
        XCTAssertEqual(resolvedIs24Hour(HourCycleLocale.forcing(is24Hour: false, base: twelveHourBase)), false)
        XCTAssertEqual(resolvedIs24Hour(HourCycleLocale.forcing(is24Hour: true, base: twentyFourHourBase)), true)
        XCTAssertEqual(resolvedIs24Hour(HourCycleLocale.forcing(is24Hour: false, base: twentyFourHourBase)), false)
    }

    /// 端末 Locale 基準 (キャッシュ経路) でも両方の時制が正しく得られ、繰り返し呼んでも変わらない。
    func test_HourCycleLocale_端末Locale基準でも両方の時制が得られる() {
        for _ in 0..<3 {
            XCTAssertEqual(resolvedIs24Hour(HourCycleLocale.forcing(is24Hour: true)), true)
            XCTAssertEqual(resolvedIs24Hour(HourCycleLocale.forcing(is24Hour: false)), false)
        }
        // 言語は端末 Locale のまま保たれる
        XCTAssertEqual(
            HourCycleLocale.forcing(is24Hour: false).language.languageCode,
            Locale.current.language.languageCode
        )
    }

    func test_TimePickerCellView_既定は24時間制のpicker() {
        let view = TimePickerCellView()
        view.render(cell: TimePickerCell(title: "アラーム", time: Date()), theme: Theme())
        XCTAssertEqual(resolvedIs24Hour(view._pickerLocale), true)
    }

    func test_TimePickerCellView_is24Hourfalseは12時間制のpickerで初期値はcellのtime() {
        let time = Calendar.current.date(bySettingHour: 14, minute: 30, second: 0, of: Date())!
        let view = TimePickerCellView()
        view.render(cell: TimePickerCell(title: "就寝", time: time, is24Hour: false), theme: Theme())
        XCTAssertEqual(resolvedIs24Hour(view._pickerLocale), false)
        XCTAssertEqual(view._currentPickerDate, time)
    }

    func test_TimePickerCellView_formatは時制に関与しない() {
        let time = Calendar.current.date(bySettingHour: 14, minute: 30, second: 0, of: Date())!
        let view = TimePickerCellView()
        let cell = TimePickerCell(title: "アラーム", time: time, format: "h:mm a")
        view.render(cell: cell, theme: Theme())
        // 行の valueText は format 通りの AM/PM 表記、picker は既定どおり 24時間制。
        XCTAssertEqual(cell.effectiveValueText(), CachedDateFormatter.string(from: time, format: "h:mm a"))
        XCTAssertEqual(resolvedIs24Hour(view._pickerLocale), true)
    }

    func test_HourCycleLocale_時制の強制でも表記の言語は基準Localeを保つ() {
        let japanese = Locale(identifier: "ja_JP")
        let forced = HourCycleLocale.forcing(is24Hour: false, base: japanese)
        XCTAssertEqual(resolvedIs24Hour(forced), false)
        XCTAssertEqual(forced.language.languageCode?.identifier, "ja")
        let formatter = DateFormatter()
        formatter.locale = forced
        XCTAssertEqual(formatter.amSymbol, "午前")
        XCTAssertEqual(formatter.pmSymbol, "午後")
    }

    func test_TimePickerCellView_表示済みCellのis24Hour変更が次のpickerに反映される() {
        let id = UUID()
        let time = Calendar.current.date(bySettingHour: 14, minute: 30, second: 0, of: Date())!
        let view = TimePickerCellView()
        view.render(cell: TimePickerCell(id: id, title: "就寝", time: time), theme: Theme())
        XCTAssertEqual(resolvedIs24Hour(view._pickerLocale), true)

        view.render(cell: TimePickerCell(id: id, title: "就寝", time: time, is24Hour: false), theme: Theme())
        XCTAssertEqual(resolvedIs24Hour(view._pickerLocale), false)
    }

    func test_TimePickerCellView_12時間制でも確定値の往復が保たれる() {
        var captured: Date?
        let calendar = Calendar.current
        let base = calendar.date(from: DateComponents(year: 2026, month: 8, day: 28, hour: 14, minute: 30))!
        let view = TimePickerCellView()
        let cell = TimePickerCell(
            title: "就寝",
            time: base,
            is24Hour: false,
            onValueChanged: { captured = $0 }
        )
        view.render(cell: cell, theme: Theme())
        view._simulateChange(to: calendar.date(bySettingHour: 14, minute: 45, second: 0, of: base)!)
        view._simulateDone()

        let components = calendar.dateComponents([.year, .month, .day, .hour, .minute], from: captured ?? Date())
        XCTAssertEqual(components.year, 2026)
        XCTAssertEqual(components.month, 8)
        XCTAssertEqual(components.day, 28)
        XCTAssertEqual(components.hour, 14)
        XCTAssertEqual(components.minute, 45)
    }

    // MARK: - DatePickerCell

    func test_DatePickerCell_既定format() {
        let cell = DatePickerCell(title: "誕生日", date: Date())
        XCTAssertEqual(cell.format, "yyyy/MM/dd")
        XCTAssertNil(cell.minDate)
        XCTAssertNil(cell.maxDate)
    }

    func test_DatePickerCell_effectiveValueText_format反映() {
        let d = Calendar.current.date(from: DateComponents(year: 2000, month: 12, day: 31))!
        let cell = DatePickerCell(title: "誕生日", date: d)
        XCTAssertEqual(cell.effectiveValueText(), "2000/12/31")
    }

    func test_DatePickerCellView_lastCell保持() {
        let view = DatePickerCellView()
        let cell = DatePickerCell(title: "誕生日", date: Date())
        view.render(cell: cell, theme: Theme())
        XCTAssertNotNil(view._lastCell)
    }

    func test_DatePickerCellView_wheelsモード_doneで日付通知() {
        var captured: Date?
        let d = Calendar.current.date(from: DateComponents(year: 2020, month: 1, day: 1))!
        let view = DatePickerCellView()
        let cell = DatePickerCell(
            title: "誕生日",
            date: d,
            onValueChanged: { captured = $0 }
        )
        view.render(cell: cell, theme: Theme())
        let new = Calendar.current.date(from: DateComponents(year: 2025, month: 6, day: 14))!
        view._simulateWheelsChange(to: new)
        view._simulateWheelsDone()
        // year/month/day のみ反映、hour/minute/second は元値（cell.date）から継承
        let ymd = Calendar.current.dateComponents([.year, .month, .day], from: captured ?? Date())
        XCTAssertEqual(ymd.year, 2025)
        XCTAssertEqual(ymd.month, 6)
        XCTAssertEqual(ymd.day, 14)
    }

    func test_DatePickerCellView_wheelsモード_todayボタンで本日にセット() {
        let d = Calendar.current.date(from: DateComponents(year: 2020, month: 1, day: 1))!
        let view = DatePickerCellView()
        let cell = DatePickerCell(
            title: "誕生日",
            date: d,
            todayText: "今日"
        )
        view.render(cell: cell, theme: Theme())
        view._simulateWheelsToday()
        let today = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        let picked = Calendar.current.dateComponents([.year, .month, .day], from: view._currentWheelsDate)
        XCTAssertEqual(today.year, picked.year)
        XCTAssertEqual(today.month, picked.month)
        XCTAssertEqual(today.day, picked.day)
    }

    func test_DatePickerCellView_wheelsモード_cancelで元値に戻る() {
        var captured: Date?
        let d = Calendar.current.date(from: DateComponents(year: 2020, month: 1, day: 1))!
        let view = DatePickerCellView()
        let cell = DatePickerCell(
            title: "誕生日",
            date: d,
            onValueChanged: { captured = $0 }
        )
        view.render(cell: cell, theme: Theme())
        let new = Calendar.current.date(from: DateComponents(year: 2025, month: 6, day: 14))!
        view._simulateWheelsChange(to: new)
        view._simulateWheelsCancel()
        XCTAssertNil(captured, "Cancel 時は onValueChanged が呼ばれてはいけない")
        // Picker 内部の値は元に戻る
        let ymd = Calendar.current.dateComponents([.year, .month, .day], from: view._currentWheelsDate)
        XCTAssertEqual(ymd.year, 2020)
        XCTAssertEqual(ymd.month, 1)
        XCTAssertEqual(ymd.day, 1)
    }

    func test_DatePickerCell_uiStyle_既定はwheels() {
        let cell = DatePickerCell(title: "誕生日", date: Date())
        XCTAssertEqual(cell.uiStyle, .wheels)
    }

    func test_DatePickerCell_uiStyle_calendar指定() {
        let cell = DatePickerCell(title: "誕生日", date: Date(), uiStyle: .calendar)
        XCTAssertEqual(cell.uiStyle, .calendar)
    }

    func test_DatePickerCell_todayText_既定はnil() {
        let cell = DatePickerCell(title: "誕生日", date: Date())
        XCTAssertNil(cell.todayText)
    }

    func test_DatePickerCalendarSheetController_doneで日付通知() {
        var captured: Date?
        let d = Calendar.current.date(from: DateComponents(year: 2020, month: 1, day: 1))!
        let vc = DatePickerCalendarSheetController(
            initial: d,
            minimumDate: nil,
            maximumDate: nil,
            pickerTitle: nil,
            todayText: nil,
            accentColor: nil,
            onDone: { captured = $0 }
        )
        vc.loadViewIfNeeded()
        let new = Calendar.current.date(from: DateComponents(year: 2025, month: 6, day: 14))!
        vc._simulateChange(to: new)
        vc._simulateDone()
        let ymd = Calendar.current.dateComponents([.year, .month, .day], from: captured ?? Date())
        XCTAssertEqual(ymd.year, 2025)
        XCTAssertEqual(ymd.month, 6)
        XCTAssertEqual(ymd.day, 14)
    }

    // MARK: - 登録 API

    func test_registerInputCells_5種が登録される() {
        let registry = KsCellRegistry()
        registry.registerInputCells()
        // 各 Cell の resolveRendererType が成立することを検証する。
        let entry = EntryCell(title: "x", text: "")
        XCTAssertNotNil(registry.resolveRendererType(for: entry))
        let picker = PickerCell(title: "x", items: [], selectedIndex: nil)
        XCTAssertNotNil(registry.resolveRendererType(for: picker))
        let num = NumberPickerCell(title: "x", value: 0)
        XCTAssertNotNil(registry.resolveRendererType(for: num))
        let time = TimePickerCell(title: "x", time: Date())
        XCTAssertNotNil(registry.resolveRendererType(for: time))
        let date = DatePickerCell(title: "x", date: Date())
        XCTAssertNotNil(registry.resolveRendererType(for: date))
    }

    // MARK: - VisibilityAware 一括検証

    func test_入力系Cell5種_VisibilityAware準拠() {
        XCTAssertTrue((EntryCell(title: "x", text: "") as Any) is VisibilityAware)
        XCTAssertTrue((PickerCell(title: "x", items: [], selectedIndex: nil) as Any) is VisibilityAware)
        XCTAssertTrue((NumberPickerCell(title: "x", value: 0) as Any) is VisibilityAware)
        XCTAssertTrue((TimePickerCell(title: "x", time: Date()) as Any) is VisibilityAware)
        XCTAssertTrue((DatePickerCell(title: "x", date: Date()) as Any) is VisibilityAware)
    }

    // MARK: - AiForms 互換編集体験

    /// EntryCell は Done ツールバーを常時表示する。
    /// `UITextField.inputAccessoryView` に `UIToolbar` が設定され、末尾の `UIBarButtonItem` の
    /// `systemItem == .done` であること。
    func test_EntryCellView_inputAccessoryViewがDoneボタン付きUIToolbar() {
        let view = EntryCellView()
        let toolbar = view._textField.inputAccessoryView as? UIToolbar
        XCTAssertNotNil(toolbar, "inputAccessoryView は UIToolbar")
        let items = toolbar?.items ?? []
        XCTAssertFalse(items.isEmpty, "items が空でない")
        // 末尾の UIBarButtonItem が Done システムアイテムであること（target/action 経由で実装、
        // システムアイテムは action から判別困難なので action selector の名前で識別）。
        // バーアイテムの type 判定は description ベース（systemItem の直接 getter は public でない）で行う。
        if let lastItem = items.last {
            // UIBarButtonItem の description は "<UIBarButtonItem ... systemItem=Done>" 形式
            // （`UIBarButtonItem.SystemItem.done` の case 名は内部的に "Done" で出力される）。
            let desc = String(describing: lastItem)
            XCTAssertTrue(
                desc.localizedCaseInsensitiveContains("systemItem=Done")
                    || desc.localizedCaseInsensitiveContains("systemItem: Done")
                    || desc.localizedCaseInsensitiveContains("systemItem: done"),
                "items の末尾が systemItem .done であるはず — 実際: \(desc)"
            )
        }
    }

    /// リストのドラッグでキーボードを閉じる。
    /// `KsSettingsViewController.loadView()` 後、`collectionView.keyboardDismissMode == .onDrag`。
    func test_KsSettingsViewController_collectionView_keyboardDismissModeOnDrag() {
        let controller = KsSettingsViewController(root: SettingsRoot(), theme: Theme())
        // loadView() を強制的に走らせる
        controller.loadViewIfNeeded()
        XCTAssertEqual(controller.internalCollectionView.keyboardDismissMode, .onDrag)
    }

    /// Cell タップでテキストフィールドにフォーカスが当たる。
    /// `UIWindow.makeKeyAndVisible()` 上で `view.tapHandler?()` を呼ぶと `view._textField.isFirstResponder == true`。
    func test_EntryCellView_tapHandler呼び出しでtextFieldがbecomeFirstResponder() {
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 320, height: 480))
        let view = EntryCellView(frame: CGRect(x: 0, y: 0, width: 320, height: 60))
        let cell = EntryCell(title: "名前", text: "")
        view.render(cell: cell, theme: Theme())
        // window に view を載せて key window にする（becomeFirstResponder の成立条件）
        window.addSubview(view)
        window.makeKeyAndVisible()
        // tapHandler を呼び、async Task で becomeFirstResponder() が走るのを 1 run loop 待つ
        let handler = view.tapHandler
        XCTAssertNotNil(handler, "tapHandler は nil でない")
        handler?()
        // Task は MainActor 上で 1 cycle 後に実行されるので run loop を回す
        RunLoop.current.run(until: Date(timeIntervalSinceNow: 0.1))
        XCTAssertTrue(
            view._textField.isFirstResponder,
            "tapHandler 呼び出しで UITextField が first responder になる"
        )
    }
}
#endif
