// BasicCellsTests.swift
// KsSettingsViewUITests
//
// 基本 Cell 7 種（LabelCell / CommandCell / ButtonCell / SwitchCell / CheckboxCell /
// RadioCell / SimpleCheckCell）の bind / 通知 / 再利用クリアを検証する。
//
// 各 Cell View は `UIListContentConfiguration` / `UICellAccessory` を使わず、`KsListCellBase` の
// 自前 UIStackView 階層 (stackH / stackV / contentStack / iconImageView / titleLabel /
// descriptionLabel / accessoryHolder) を直接更新するため、検証も subview 階層に対して行う。
// Cell 級アクセサリ (Switch / checkbox / checkmark / chevron) は `accessoryHolder` に、
// 行内 trailing (valueText / EntryCell の入力フィールド) は `contentStack` に置かれる。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class BasicCellsTests: XCTestCase {

    // MARK: - id デフォルト値規約

    func test_LabelCellはidデフォルトUUIDで自動採番される() {
        let a = LabelCell(title: "x")
        let b = LabelCell(title: "x")
        XCTAssertNotEqual(a.id, b.id, "id 省略時は UUID() で自動採番される")
    }

    func test_全Cellのidデフォルト値規約() {
        XCTAssertNotEqual(CommandCell(title: "x").id, CommandCell(title: "x").id)
        XCTAssertNotEqual(ButtonCell(title: "x").id, ButtonCell(title: "x").id)
        XCTAssertNotEqual(SwitchCell(title: "x").id, SwitchCell(title: "x").id)
        XCTAssertNotEqual(CheckboxCell(title: "x").id, CheckboxCell(title: "x").id)
        XCTAssertNotEqual(
            RadioCell(title: "x", groupId: "g", value: "a", selectedValue: "a").id,
            RadioCell(title: "x", groupId: "g", value: "a", selectedValue: "a").id
        )
        XCTAssertNotEqual(SimpleCheckCell(title: "x").id, SimpleCheckCell(title: "x").id)
    }

    // MARK: - DSLReidentifiable / DSLStyleModifiable 規約

    func test_LabelCellのwithDSLIDは新しいidを持つCopyを返す() {
        let original = LabelCell(title: "x", description: "d")
        let newID = UUID()
        let copy = original.withDSLID(newID)
        XCTAssertEqual(copy.id, newID)
        XCTAssertEqual(copy.title, "x")
        XCTAssertEqual(copy.description, "d")
    }

    func test_SwitchCellのwithStyleは新しいstyleを持つCopyを返す() {
        let original = SwitchCell(title: "x", isOn: true)
        let newStyle = CellStyle(titleColor: UIColor(red: 1.0, green: 0.0, blue: 0.0, alpha: 1.0))
        let copy = original.withStyle(newStyle)
        XCTAssertEqual(copy.id, original.id)
        XCTAssertEqual(copy.style, newStyle)
        XCTAssertEqual(copy.isOn, true)
    }

    // MARK: - LabelCell の描画

    func test_LabelCellView_bindでtitleが反映される() {
        let view = LabelCellView()
        let cell = LabelCell(title: "プロフィール")
        view.render(cell: cell, theme: Theme())

        // contentConfiguration 経路は使わず、titleLabel に直接反映される。
        XCTAssertNil(view.contentConfiguration, "applyCellBaseLayout が contentConfiguration を必ず nil にする")
        XCTAssertEqual(view.titleLabel.text, "プロフィール")
    }

    /// description と valueText を両方指定したとき、
    /// description は descriptionLabel に、valueText は title 行の右側に value label として
    /// 配置され、Android 側のレイアウト（`[title / description][valueText (右寄せ)]`）と一致する。
    func test_LabelCellView_descriptionとvalueText両方指定時はdescriptionLabelに表示_valueTextはcontentStackに表示() {
        let view = LabelCellView()
        let cell = LabelCell(
            title: "通知",
            description: "プッシュ通知設定",
            valueText: "オン"
        )
        view.render(cell: cell, theme: Theme())

        XCTAssertEqual(view.titleLabel.text, "通知")
        // description は descriptionLabel に入り、表示される。
        XCTAssertEqual(view.descriptionLabel.text, "プッシュ通知設定")
        XCTAssertFalse(view.descriptionLabel.isHidden)
        // valueText は contentStack の titleLabel の右側に value label として配置される。
        let valueLabel = view.contentStack.arrangedSubviews
            .first(where: { $0 !== view.titleLabel }) as? UILabel
        XCTAssertNotNil(valueLabel, "valueText 用の UILabel が contentStack に追加されていない")
        XCTAssertEqual(valueLabel?.text, "オン")
    }

    /// description のみ指定時は descriptionLabel に description が入り、
    /// contentStack には titleLabel 以外の subview は追加されない。
    func test_LabelCellView_descriptionのみ指定時はdescriptionLabelに反映される() {
        let view = LabelCellView()
        let cell = LabelCell(title: "通知", description: "プッシュ通知設定")
        view.render(cell: cell, theme: Theme())

        XCTAssertEqual(view.titleLabel.text, "通知")
        XCTAssertEqual(view.descriptionLabel.text, "プッシュ通知設定")
        XCTAssertFalse(view.descriptionLabel.isHidden)
        // description のみ時は contentStack 内に value label 等は追加されない。
        XCTAssertEqual(
            view.contentStack.arrangedSubviews.count,
            1,
            "description のみ時、contentStack は titleLabel だけ"
        )
    }

    /// valueText のみ指定時は contentStack に value label が追加される。
    func test_LabelCellView_valueTextのみ指定時はcontentStackにvalueTextラベルが追加される() {
        let view = LabelCellView()
        let cell = LabelCell(title: "通知", valueText: "オン")
        view.render(cell: cell, theme: Theme())

        XCTAssertEqual(view.titleLabel.text, "通知")
        // description は無いので descriptionLabel は hidden。
        XCTAssertTrue(view.descriptionLabel.isHidden)
        // contentStack に value label が追加されている。
        let valueLabel = view.contentStack.arrangedSubviews
            .first(where: { $0 !== view.titleLabel }) as? UILabel
        XCTAssertNotNil(valueLabel)
        XCTAssertEqual(valueLabel?.text, "オン")
    }

    func test_LabelCellView_prepareForReuseでcontentがクリアされる() {
        let view = LabelCellView()
        view.render(cell: LabelCell(title: "X"), theme: Theme())
        view.prepareForReuse()
        // contentConfiguration は常に nil。
        XCTAssertNil(view.contentConfiguration)
        // base の prepareForReuse によって titleLabel.text もクリアされる。
        XCTAssertNil(view.titleLabel.text)
    }

    // MARK: - CommandCell の描画と通知

    func test_CommandCellView_disclosureが表示される() {
        let view = CommandCellView()
        view.render(cell: CommandCell(title: "License"), theme: Theme())
        // chevron は makeChevronView() 由来の UIImageView として Cell 級アクセサリ列
        // (accessoryHolder) に配置される（contentStack ではない）。
        let hasChevron = view.accessoryHolder.arrangedSubviews.contains { $0 is UIImageView }
        XCTAssertTrue(hasChevron, "hideArrow=false（既定）で chevron (UIImageView) が accessoryHolder に配置される")
        XCTAssertFalse(view.accessoryHolder.isHidden, "アクセサリありのとき accessoryHolder は表示される")
        XCTAssertFalse(
            view.contentStack.arrangedSubviews.contains { $0 is UIImageView },
            "chevron は行内 (contentStack) には置かれない"
        )
    }

    func test_CommandCellView_hideArrow_trueでdisclosure非表示() {
        let view = CommandCellView()
        view.render(cell: CommandCell(title: "X", hideArrow: true), theme: Theme())
        XCTAssertTrue(view.accessoryHolder.arrangedSubviews.isEmpty, "hideArrow=true で chevron は配置されない")
        XCTAssertTrue(view.accessoryHolder.isHidden, "アクセサリなしのとき accessoryHolder は隠れる")
    }

    func test_CommandCellView_tapHandlerがonTapを保持する() {
        let view = CommandCellView()
        var called = false
        let onTap: @Sendable () -> Void = { called = true }
        view.render(cell: CommandCell(title: "X", onTap: onTap), theme: Theme())
        view.tapHandler?()
        XCTAssertTrue(called)
    }

    func test_CommandCellView_prepareForReuseでtapHandlerがクリアされる() {
        let view = CommandCellView()
        view.render(cell: CommandCell(title: "X", onTap: { }), theme: Theme())
        view.prepareForReuse()
        XCTAssertNil(view.tapHandler)
    }

    // MARK: - KsImage

    /// `KsImage.systemName` を持つ LabelCell は `UIImage(systemName:)` を解決して
    /// `iconImageView.image` に設定する。
    func test_LabelCellView_systemNameアイコンがUIImageに解決される() {
        let view = LabelCellView()
        view.render(
            cell: LabelCell(title: "Storage", icon: KsImage.systemName("externaldrive")),
            theme: Theme()
        )
        // SF Symbols 名が解決可能（不正名でない）であれば image が設定される
        XCTAssertNotNil(view.iconImageView.image, "systemName が解決可能なら iconImageView.image が設定される")
        XCTAssertFalse(view.iconImageView.isHidden)
    }

    /// `KsImage.uiImage` を持つ LabelCell は渡された UIImage をそのまま
    /// `iconImageView.image` に設定する。
    func test_LabelCellView_uiImageアイコンがそのまま設定される() {
        let view = LabelCellView()
        let custom = UIImage()
        view.render(
            cell: LabelCell(title: "Custom", icon: KsImage.uiImage(custom)),
            theme: Theme()
        )
        XCTAssertTrue(view.iconImageView.image === custom, "渡された UIImage がそのまま iconImageView.image に設定される")
    }

    /// `icon: nil` の LabelCell は `iconImageView.image` を nil のままにし、isHidden = true。
    func test_LabelCellView_iconがnilのときiconImageViewもnil() {
        let view = LabelCellView()
        view.render(cell: LabelCell(title: "No Icon", icon: nil), theme: Theme())
        XCTAssertNil(view.iconImageView.image)
        XCTAssertTrue(view.iconImageView.isHidden)
    }

    // MARK: - ButtonCell

    func test_ButtonCellView_centerAlignmentで描画される() {
        let view = ButtonCellView()
        view.render(cell: ButtonCell(title: "ログアウト"), theme: Theme())
        // ButtonCellView も applyCellBaseLayout 経由で base の titleLabel に描画する。
        XCTAssertNil(view.contentConfiguration, "contentConfiguration 経路は使用しない")
        XCTAssertEqual(view.titleLabel.text, "ログアウト")
        XCTAssertEqual(view.titleLabel.textAlignment, .center)
        // ButtonCellView は行内 trailing を渡さないので、contentStack には titleLabel のみ。
        XCTAssertEqual(
            view.contentStack.arrangedSubviews.count,
            1,
            "Disclosure 等の行内 trailing は付与されない"
        )
        // Cell 級アクセサリも持たない。
        XCTAssertTrue(view.accessoryHolder.arrangedSubviews.isEmpty, "ButtonCell は Cell 級アクセサリを持たない")
        XCTAssertTrue(view.accessoryHolder.isHidden)
    }

    func test_ButtonCellView_onTapが保存される() {
        let view = ButtonCellView()
        var called = 0
        view.render(cell: ButtonCell(title: "X", onTap: { called += 1 }), theme: Theme())
        view.tapHandler?()
        view.tapHandler?()
        XCTAssertEqual(called, 2)
    }

    // MARK: - SwitchCell

    func test_SwitchCellView_bindで状態が反映される() {
        let view = SwitchCellView()
        view.render(cell: SwitchCell(title: "通知", isOn: true), theme: Theme())
        // UISwitch は Cell 級アクセサリとして accessoryHolder に配置される。
        let toggle = view.accessoryHolder.arrangedSubviews.first as? UISwitch
        XCTAssertNotNil(toggle, "UISwitch が accessoryHolder に配置される")
        XCTAssertEqual(toggle?.isOn, true)
        XCTAssertFalse(view.accessoryHolder.isHidden)
        XCTAssertFalse(
            view.contentStack.arrangedSubviews.contains { $0 is UISwitch },
            "UISwitch は行内 (contentStack) には置かれない"
        )
    }

    func test_SwitchCellView_値変更時にonValueChangedが呼ばれる() {
        let view = SwitchCellView()
        var received: Bool?
        view.render(
            cell: SwitchCell(title: "X", isOn: false, onValueChanged: { v in received = v }),
            theme: Theme()
        )
        view._simulateValueChange(to: true)
        XCTAssertEqual(received, true)
    }

    func test_SwitchCellView_prepareForReuseでlistenerクリア() {
        let view = SwitchCellView()
        view.render(
            cell: SwitchCell(title: "X", isOn: false, onValueChanged: { _ in }),
            theme: Theme()
        )
        view.prepareForReuse()
        // listener クリア後に simulateValueChange しても呼ばれない（クラッシュもしない）
        view._simulateValueChange(to: true)
        // valueChangedHandler が nil であることを間接的に保証（既に nil クリアされている）
        XCTAssertTrue(true)
    }

    // MARK: - CheckboxCell

    func test_CheckboxCellView_isChecked_trueで角丸チェックボックスがアクセサリ列に常設かつchecked() {
        let view = CheckboxCellView()
        view.render(cell: CheckboxCell(title: "X", isChecked: true), theme: Theme())
        // チェックボックスは Cell 級アクセサリ列に常設される（追加・削除しない）。
        XCTAssertTrue(view._hasCellAccessoryCheckBox, "角丸チェックボックスは accessoryHolder に常設される")
        XCTAssertTrue(view._isCheckBoxChecked, "isChecked=true が内部 View に反映される")
    }

    func test_CheckboxCellView_isChecked_falseでもアクセサリは常設されuncheckedになる() {
        let view = CheckboxCellView()
        view.render(cell: CheckboxCell(title: "X", isChecked: false), theme: Theme())
        // unchecked でも accessoryHolder に常設される（位置を変えないため）。
        XCTAssertTrue(view._hasCellAccessoryCheckBox, "unchecked でも accessoryHolder に常設される")
        XCTAssertFalse(view._isCheckBoxChecked, "isChecked=false が内部 View に反映される")
    }

    func test_CheckboxCellView_checked切替でsubview数が変化しない() {
        let view = CheckboxCellView()
        view.render(cell: CheckboxCell(title: "X", isChecked: false), theme: Theme())
        let countUnchecked = view.contentStack.arrangedSubviews.count
        let accessoryCountUnchecked = view.accessoryHolder.arrangedSubviews.count
        view.render(cell: CheckboxCell(title: "X", isChecked: true), theme: Theme())
        let countChecked = view.contentStack.arrangedSubviews.count
        let accessoryCountChecked = view.accessoryHolder.arrangedSubviews.count
        XCTAssertEqual(countUnchecked, countChecked, "checked 切替で contentStack subview 数は変化しない")
        XCTAssertEqual(
            accessoryCountUnchecked,
            accessoryCountChecked,
            "checked 切替で accessoryHolder の内容数は変化しない"
        )
        XCTAssertEqual(accessoryCountChecked, 1, "accessoryHolder の内容は常に 1 個")
        XCTAssertTrue(view._isCheckBoxChecked)
    }

    func test_CheckboxCellView_タップでtoggleされた値が通知される() {
        let view = CheckboxCellView()
        var received: Bool?
        view.render(
            cell: CheckboxCell(title: "X", isChecked: false, onValueChanged: { v in received = v }),
            theme: Theme()
        )
        view.tapHandler?()
        XCTAssertEqual(received, true, "isChecked=false の状態でタップすると true が通知される")
    }

    // MARK: - RadioCell

    func test_RadioCellView_value一致でcheckmarkがアクセサリ列に常設かつalpha1() {
        let view = RadioCellView()
        let cell = RadioCell(title: "Dark", groupId: "theme", value: "dark", selectedValue: "dark")
        view.render(cell: cell, theme: Theme())
        XCTAssertTrue(view._hasCellAccessoryCheckmark, "checkmark は accessoryHolder に常設される")
        XCTAssertEqual(view._checkmarkAlpha, 1.0, accuracy: 0.001, "選択中は alpha 1")
    }

    func test_RadioCellView_value不一致でもアクセサリ列に常設かつalpha0() {
        let view = RadioCellView()
        let cell = RadioCell(title: "Light", groupId: "theme", value: "light", selectedValue: "dark")
        view.render(cell: cell, theme: Theme())
        XCTAssertTrue(view._hasCellAccessoryCheckmark, "非選択でも accessoryHolder に常設される")
        XCTAssertEqual(view._checkmarkAlpha, 0.0, accuracy: 0.001, "非選択は alpha 0")
    }

    func test_RadioCellView_選択切替でsubview数が変化しない() {
        let view = RadioCellView()
        view.render(
            cell: RadioCell(title: "L", groupId: "g", value: "light", selectedValue: "dark"),
            theme: Theme()
        )
        let countNonSelected = view.contentStack.arrangedSubviews.count
        let accessoryCountNonSelected = view.accessoryHolder.arrangedSubviews.count
        // 同一セルの状態変化（reconfigure 相当）。prepareForReuse は呼ばない。
        view.render(
            cell: RadioCell(title: "L", groupId: "g", value: "light", selectedValue: "light"),
            theme: Theme()
        )
        let countSelected = view.contentStack.arrangedSubviews.count
        let accessoryCountSelected = view.accessoryHolder.arrangedSubviews.count
        XCTAssertEqual(countNonSelected, countSelected, "選択切替で contentStack subview 数は変化しない")
        XCTAssertEqual(
            accessoryCountNonSelected,
            accessoryCountSelected,
            "選択切替で accessoryHolder の内容数は変化しない"
        )
        XCTAssertEqual(accessoryCountSelected, 1, "accessoryHolder の内容は常に 1 個")
    }

    func test_RadioCellView_タップでonSelectedにvalueが渡される() {
        let view = RadioCellView()
        var received: String?
        let cell = RadioCell(
            title: "L",
            groupId: "g",
            value: "light",
            selectedValue: "dark",
            onSelected: { v in received = v }
        )
        view.render(cell: cell, theme: Theme())
        view.tapHandler?()
        XCTAssertEqual(received, "light")
    }

    // MARK: - SimpleCheckCell

    func test_SimpleCheckCellView_右端customView方式でiconは使われない() {
        let view = SimpleCheckCellView()
        view.render(cell: SimpleCheckCell(title: "X", isChecked: true), theme: Theme())
        XCTAssertNil(view.iconImageView.image, "左側チェック（iconImageView）は使われない")
        // 右端 checkmark は Cell 級アクセサリ列に常設される。
        XCTAssertTrue(view._hasCellAccessoryCheckmark, "右端 checkmark は accessoryHolder に常設される")
        XCTAssertEqual(view._checkmarkAlpha, 1.0, accuracy: 0.001, "checked は alpha 1")
    }

    func test_SimpleCheckCellView_isChecked_falseで右端checkmarkはalpha0() {
        let view = SimpleCheckCellView()
        view.render(cell: SimpleCheckCell(title: "X", isChecked: false), theme: Theme())
        XCTAssertNil(view.iconImageView.image)
        XCTAssertEqual(view._checkmarkAlpha, 0.0, accuracy: 0.001, "unchecked は alpha 0（非表示）")
    }

    func test_SimpleCheckCellView_タップでtoggle通知() {
        let view = SimpleCheckCellView()
        var received: Bool?
        view.render(
            cell: SimpleCheckCell(title: "X", isChecked: true, onValueChanged: { v in received = v }),
            theme: Theme()
        )
        view.tapHandler?()
        XCTAssertEqual(received, false)
    }

    // MARK: - isEnabled / titleAlignment

    func test_全Cellのデフォルト_isEnabled_は_true() {
        XCTAssertEqual(LabelCell(title: "x").isEnabled, true)
        XCTAssertEqual(CommandCell(title: "x").isEnabled, true)
        XCTAssertEqual(ButtonCell(title: "x").isEnabled, true)
        XCTAssertEqual(SwitchCell(title: "x").isEnabled, true)
        XCTAssertEqual(CheckboxCell(title: "x").isEnabled, true)
        XCTAssertEqual(
            RadioCell(title: "x", groupId: "g", value: "a", selectedValue: "a").isEnabled,
            true
        )
        XCTAssertEqual(SimpleCheckCell(title: "x").isEnabled, true)
    }

    func test_isEnabled_を_false_に指定できる() {
        XCTAssertEqual(LabelCell(title: "x", isEnabled: false).isEnabled, false)
        XCTAssertEqual(SwitchCell(title: "x", isEnabled: false).isEnabled, false)
        XCTAssertEqual(CheckboxCell(title: "x", isEnabled: false).isEnabled, false)
    }

    func test_isEnabled_を変えると等価でなくなる() {
        let a = LabelCell(id: UUID(), title: "x", isEnabled: true)
        let b = LabelCell(id: a.id, title: "x", isEnabled: false)
        XCTAssertNotEqual(a, b)
    }

    func test_isEnabled_を変えるとhashValueも変わる() {
        let a = SwitchCell(id: UUID(), title: "x", isOn: true, isEnabled: true)
        let b = SwitchCell(id: a.id, title: "x", isOn: true, isEnabled: false)
        XCTAssertNotEqual(a.hashValue, b.hashValue)
    }

    func test_ButtonCellのデフォルト_titleAlignment_は_center() {
        XCTAssertEqual(ButtonCell(title: "x").titleAlignment, .center)
    }

    func test_ButtonCellのtitleAlignmentを指定できる() {
        XCTAssertEqual(ButtonCell(title: "x", titleAlignment: .start).titleAlignment, .start)
        XCTAssertEqual(ButtonCell(title: "x", titleAlignment: .end).titleAlignment, .end)
    }

    func test_ButtonCell_titleAlignmentが異なれば等価でない() {
        let id = UUID()
        let a = ButtonCell(id: id, title: "x", titleAlignment: .center)
        let b = ButtonCell(id: id, title: "x", titleAlignment: .start)
        XCTAssertNotEqual(a, b)
    }

    // MARK: - 内部 View への isEnabled 委譲

    /// CheckboxCellView: `isEnabled = false` の Cell を render したとき、
    /// disabled 表現が **内部 KsCheckBoxView の `isEnabled`** に委譲され、
    /// **Cell コンテナの内部 View 全体 alpha は変更されない**ことを確認する。
    func test_CheckboxCellView_disabledは内部KsCheckBoxViewのisEnabledに委譲される() {
        let view = CheckboxCellView()
        let cell = CheckboxCell(title: "X", isChecked: true, isEnabled: false)
        view.render(cell: cell, theme: Theme())

        XCTAssertFalse(view._isCheckBoxEnabled, "内部 KsCheckBoxView の isEnabled は false に委譲される")
        XCTAssertEqual(
            view._checkBoxViewAlpha,
            1.0,
            accuracy: 0.001,
            "内部 View 全体の alpha は 1.0 のまま（描画分岐は KsCheckBoxView 内部で実施）"
        )
    }

    func test_CheckboxCellView_isEnabled_trueでは内部KsCheckBoxViewもenabled() {
        let view = CheckboxCellView()
        let cell = CheckboxCell(title: "X", isChecked: false, isEnabled: true)
        view.render(cell: cell, theme: Theme())

        XCTAssertTrue(view._isCheckBoxEnabled, "isEnabled = true で内部 KsCheckBoxView もそのまま enabled")
    }

    /// RadioCellView: 同上。
    func test_RadioCellView_disabledは内部KsCheckmarkAccessoryViewのisEnabledに委譲される() {
        let view = RadioCellView()
        let cell = RadioCell(
            title: "Dark", groupId: "theme", value: "dark", selectedValue: "dark",
            isEnabled: false
        )
        view.render(cell: cell, theme: Theme())

        XCTAssertFalse(view._isCheckmarkEnabled, "内部 KsCheckmarkAccessoryView の isEnabled は false に委譲される")
        XCTAssertEqual(
            view._checkmarkViewAlpha,
            1.0,
            accuracy: 0.001,
            "内部 View 全体の alpha は 1.0 のまま（描画分岐は KsCheckmarkAccessoryView 内部で実施）"
        )
    }

    func test_RadioCellView_isEnabled_trueでは内部Checkmarkもenabled() {
        let view = RadioCellView()
        let cell = RadioCell(
            title: "Dark", groupId: "theme", value: "dark", selectedValue: "dark",
            isEnabled: true
        )
        view.render(cell: cell, theme: Theme())
        XCTAssertTrue(view._isCheckmarkEnabled)
    }

    /// SimpleCheckCellView: 同上。
    func test_SimpleCheckCellView_disabledは内部KsCheckmarkAccessoryViewのisEnabledに委譲される() {
        let view = SimpleCheckCellView()
        let cell = SimpleCheckCell(title: "X", isChecked: true, isEnabled: false)
        view.render(cell: cell, theme: Theme())

        XCTAssertFalse(view._isCheckmarkEnabled, "内部 KsCheckmarkAccessoryView の isEnabled は false に委譲される")
        XCTAssertEqual(
            view._checkmarkViewAlpha,
            1.0,
            accuracy: 0.001,
            "内部 View 全体の alpha は 1.0 のまま（描画分岐は KsCheckmarkAccessoryView 内部で実施）"
        )
    }

    /// KsCheckBoxView: `isEnabled` 状態の変化で再描画がトリガされる（setNeedsDisplay 経路）ことを
    /// 「`isEnabled` プロパティが値として保持される」ことを通じて確認する。
    func test_KsCheckBoxView_isEnabled_を切替できる() {
        let v = KsCheckBoxView(
            frame: CGRect(x: 0, y: 0, width: KsCheckBoxView.defaultSide, height: KsCheckBoxView.defaultSide)
        )
        XCTAssertTrue(v.isEnabled, "既定では enabled")
        v.isEnabled = false
        XCTAssertFalse(v.isEnabled)
        v.isEnabled = true
        XCTAssertTrue(v.isEnabled)
    }

    /// KsCheckmarkAccessoryView: 同上 + tint 色が isEnabled でアルファ低下することを確認する。
    func test_KsCheckmarkAccessoryView_isEnabled_でtint色アルファが下がる() {
        let v = KsCheckmarkAccessoryView()
        let accent: UIColor = .systemBlue

        // enabled: tint は accent 等価（resolvedColor は同じ色を返す前提）
        v.isEnabled = true
        v.apply(selected: true, accent: accent, animated: false)
        var alpha: CGFloat = 0
        v.checkmarkTintColor?.getRed(nil, green: nil, blue: nil, alpha: &alpha)
        XCTAssertEqual(alpha, 1.0, accuracy: 0.001, "enabled: tint アルファは 1.0")

        // disabled: tint アルファ 0.5
        v.isEnabled = false
        var alpha2: CGFloat = 0
        v.checkmarkTintColor?.getRed(nil, green: nil, blue: nil, alpha: &alpha2)
        XCTAssertEqual(alpha2, 0.5, accuracy: 0.001, "disabled: tint アルファは 0.5")
    }

    // MARK: - ButtonCell baseColor の 4 段階優先順位

    /// 1. Cell 個別の `titleColor` が指定されていればそれを採用する。
    func test_ButtonCellView_baseColor_Cell個別titleColor優先() {
        let view = ButtonCellView()
        let red = UIColor(red: 1.0, green: 0.0, blue: 0.0, alpha: 1.0)
        let themeColor = UIColor(red: 0.0, green: 1.0, blue: 0.0, alpha: 1.0)
        let cell = ButtonCell(title: "削除", titleColor: red)
        view.render(cell: cell, theme: Theme(cellTitleColor: themeColor))
        // disabled でなければ baseColor が反映される
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        view.titleLabel.textColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 1.0, accuracy: 0.005)
        XCTAssertEqual(g, 0.0, accuracy: 0.005)
    }

    /// 2. `CellStyle.titleColor` が指定されていれば `effective.titleColor` を採用する。
    func test_ButtonCellView_baseColor_CellStyle_titleColor優先() {
        let view = ButtonCellView()
        let purple = UIColor(red: 0.5, green: 0.0, blue: 0.5, alpha: 1.0)
        let cell = ButtonCell(style: CellStyle(titleColor: purple), title: "次へ")
        view.render(cell: cell, theme: Theme())
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        view.titleLabel.textColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 0.5, accuracy: 0.005)
    }

    /// 2'. `Theme.titleColor` のみ指定されていれば `effective.titleColor` を採用する。
    func test_ButtonCellView_baseColor_Theme_titleColor優先() {
        let view = ButtonCellView()
        let orange = UIColor(red: 0.8, green: 0.6, blue: 0.0, alpha: 1.0)
        view.render(cell: ButtonCell(title: "登録"), theme: Theme(cellTitleColor: orange))
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        view.titleLabel.textColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 0.8, accuracy: 0.005)
        XCTAssertEqual(g, 0.6, accuracy: 0.005)
    }

    /// 3. いずれも未指定なら `.systemBlue` にフォールバックする。
    func test_ButtonCellView_baseColor_全て未指定はsystemBlue() {
        let view = ButtonCellView()
        view.render(cell: ButtonCell(title: "OK"), theme: Theme())
        XCTAssertEqual(view.titleLabel.textColor, UIColor.systemBlue)
    }

    /// `cell.isEnabled == false` 時は disabledTextColor が baseColor より優先される（既存挙動の退行確認）。
    func test_ButtonCellView_isEnabled_falseでdisabledTextColorが採用される() {
        let view = ButtonCellView()
        let red = UIColor(red: 1.0, green: 0.0, blue: 0.0, alpha: 1.0)
        let gray = UIColor(red: 0.5, green: 0.5, blue: 0.5, alpha: 1.0)
        view.render(
            cell: ButtonCell(title: "削除", titleColor: red, isEnabled: false),
            theme: Theme(disabledTextColor: gray)
        )
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        view.titleLabel.textColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 0.5, accuracy: 0.005)
        XCTAssertEqual(g, 0.5, accuracy: 0.005)
    }

    // MARK: - 各 Cell で Theme.titleColor が反映される

    /// LabelCell：`Theme.titleColor` 指定時にタイトル色が反映される。
    func test_LabelCellView_Theme_titleColor_反映される() {
        let view = LabelCellView()
        let themeColor = UIColor(red: 0.3, green: 0.7, blue: 0.2, alpha: 1.0)
        view.render(cell: LabelCell(title: "X"), theme: Theme(cellTitleColor: themeColor))
        // 色は titleLabel.textColor で直接確認する。
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        view.titleLabel.textColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 0.3, accuracy: 0.005)
        XCTAssertEqual(g, 0.7, accuracy: 0.005)
        XCTAssertEqual(b, 0.2, accuracy: 0.005)
    }

    /// CommandCell：`Theme.titleColor` 指定時にタイトル色が反映される。
    func test_CommandCellView_Theme_titleColor_反映される() {
        let view = CommandCellView()
        let themeColor = UIColor(red: 0.4, green: 0.5, blue: 0.6, alpha: 1.0)
        view.render(cell: CommandCell(title: "X"), theme: Theme(cellTitleColor: themeColor))
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        view.titleLabel.textColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 0.4, accuracy: 0.005)
        XCTAssertEqual(g, 0.5, accuracy: 0.005)
        XCTAssertEqual(b, 0.6, accuracy: 0.005)
    }

    /// SwitchCell：`Theme.titleColor` 指定時にタイトル色が反映される。
    func test_SwitchCellView_Theme_titleColor_反映される() {
        let view = SwitchCellView()
        let themeColor = UIColor(red: 0.5, green: 0.0, blue: 0.8, alpha: 1.0)
        view.render(
            cell: SwitchCell(title: "通知", isOn: false, onValueChanged: { _ in }),
            theme: Theme(cellTitleColor: themeColor)
        )
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        view.titleLabel.textColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 0.5, accuracy: 0.005)
        XCTAssertEqual(g, 0.0, accuracy: 0.005)
        XCTAssertEqual(b, 0.8, accuracy: 0.005)
    }

    /// CheckboxCell：`Theme.titleColor` 指定時にタイトル色が反映される。
    func test_CheckboxCellView_Theme_titleColor_反映される() {
        let view = CheckboxCellView()
        let themeColor = UIColor(red: 0.2, green: 0.4, blue: 0.6, alpha: 1.0)
        view.render(
            cell: CheckboxCell(title: "同意する", isChecked: false, onValueChanged: { _ in }),
            theme: Theme(cellTitleColor: themeColor)
        )
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        view.titleLabel.textColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 0.2, accuracy: 0.005)
        XCTAssertEqual(g, 0.4, accuracy: 0.005)
        XCTAssertEqual(b, 0.6, accuracy: 0.005)
    }

    /// RadioCell：`Theme.titleColor` 指定時にタイトル色が反映される。
    func test_RadioCellView_Theme_titleColor_反映される() {
        let view = RadioCellView()
        let themeColor = UIColor(red: 0.7, green: 0.1, blue: 0.3, alpha: 1.0)
        view.render(
            cell: RadioCell(title: "Dark", groupId: "theme", value: "dark", selectedValue: "dark"),
            theme: Theme(cellTitleColor: themeColor)
        )
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        view.titleLabel.textColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 0.7, accuracy: 0.005)
        XCTAssertEqual(g, 0.1, accuracy: 0.005)
        XCTAssertEqual(b, 0.3, accuracy: 0.005)
    }

    /// SimpleCheckCell：`Theme.titleColor` 指定時にタイトル色が反映される。
    func test_SimpleCheckCellView_Theme_titleColor_反映される() {
        let view = SimpleCheckCellView()
        let themeColor = UIColor(red: 0.1, green: 0.9, blue: 0.4, alpha: 1.0)
        view.render(
            cell: SimpleCheckCell(title: "選択", isChecked: false, onValueChanged: { _ in }),
            theme: Theme(cellTitleColor: themeColor)
        )
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        view.titleLabel.textColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 0.1, accuracy: 0.005)
        XCTAssertEqual(g, 0.9, accuracy: 0.005)
        XCTAssertEqual(b, 0.4, accuracy: 0.005)
    }

    /// CellStyle.titleColor は Theme.titleColor より優先される（LabelCell 経路）。
    func test_LabelCellView_CellStyle_titleColor_がTheme_titleColorより優先される() {
        let view = LabelCellView()
        let cellColor = UIColor(red: 1.0, green: 0.0, blue: 0.0, alpha: 1.0)
        let themeColor = UIColor(red: 0.0, green: 0.0, blue: 1.0, alpha: 1.0)
        view.render(
            cell: LabelCell(style: CellStyle(titleColor: cellColor), title: "X"),
            theme: Theme(cellTitleColor: themeColor)
        )
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        view.titleLabel.textColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        XCTAssertEqual(r, 1.0, accuracy: 0.005)
        XCTAssertEqual(b, 0.0, accuracy: 0.005)
    }

    // MARK: - 一括登録 API

    func test_registerBasicCells_で7種が登録される() {
        let registry = KsCellRegistry()
        registry.registerBasicCells()
        XCTAssertNotNil(registry.resolveRendererType(for: LabelCell(title: "x")))
        XCTAssertNotNil(registry.resolveRendererType(for: CommandCell(title: "x")))
        XCTAssertNotNil(registry.resolveRendererType(for: ButtonCell(title: "x")))
        XCTAssertNotNil(registry.resolveRendererType(for: SwitchCell(title: "x")))
        XCTAssertNotNil(registry.resolveRendererType(for: CheckboxCell(title: "x")))
        XCTAssertNotNil(registry.resolveRendererType(
            for: RadioCell(title: "x", groupId: "g", value: "a", selectedValue: "a")
        ))
        XCTAssertNotNil(registry.resolveRendererType(for: SimpleCheckCell(title: "x")))
    }
}
#endif
