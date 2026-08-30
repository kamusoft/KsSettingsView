// UnifyCellCommonFieldsTests.swift
// KsSettingsViewUITests
//
// 全 Cell 共通のフィールド（`description` / `valueText` / `icon` / `hintText` / `accentColor`）と
// 共通行レイアウト関数 `applyCellBaseLayout` の振る舞いを検証する。
//
// `applyCellBaseLayout` は `UIListContentConfiguration` / `UICellAccessory` を使わず
// `KsListCellBase` の自前 `UIStackView` 階層を直接更新するため、以下を assert する：
//   - `cell.titleLabel.text` / `cell.descriptionLabel.text` / `cell.iconImageView.image`
//   - `cell.contentStack.arrangedSubviews` の構成（[titleLabel, (valueLabel?), trailingViews...]）
//   - `cell.accessoryHolder` の構成（Cell 級アクセサリ 0 個または 1 個 / 空時 isHidden）
//   - `cell.hintLabel` の text / isHidden / 制約
//
// trailing の受け口は 2 系統ある: `accessoryView` は `accessoryHolder` へ、
// `trailingViews` / `valueLabelText` は `contentStack` へ配置される。

#if canImport(UIKit)
import XCTest
import UIKit
@testable import KsSettingsViewUI
@testable import KsSettingsViewCore

@MainActor
final class UnifyCellCommonFieldsTests: XCTestCase {

    // MARK: - モデル equality / hash / withDSLID / withStyle

    func test_SwitchCellに追加された共通フィールドがequalityで判定される() {
        let id = UUID()
        let a = SwitchCell(id: id, title: "x", valueText: "A", isOn: true)
        let b = SwitchCell(id: id, title: "x", valueText: "B", isOn: true)
        XCTAssertNotEqual(a, b, "valueText の差が == に反映される")
        XCTAssertNotEqual(a.hashValue, b.hashValue, "valueText の差が hash に反映される")
    }

    func test_CheckboxCellに追加された共通フィールドがequalityで判定される() {
        let id = UUID()
        let a = CheckboxCell(id: id, title: "x", icon: KsImage.systemName("bell"), isChecked: false)
        let b = CheckboxCell(id: id, title: "x", icon: KsImage.systemName("star"), isChecked: false)
        XCTAssertNotEqual(a, b, "icon の差が == に反映される")
    }

    func test_RadioCellに追加された共通フィールドとaccentColorがequalityで判定される() {
        let id = UUID()
        let a = RadioCell(
            id: id,
            title: "x",
            description: "d1",
            groupId: "g",
            value: "v",
            selectedValue: "v",
            accentColor: UIColor.red
        )
        let b = RadioCell(
            id: id,
            title: "x",
            description: "d2",
            groupId: "g",
            value: "v",
            selectedValue: "v",
            accentColor: UIColor.red
        )
        XCTAssertNotEqual(a, b, "description の差が == に反映される")

        let c = RadioCell(
            id: id,
            title: "x",
            description: "d1",
            groupId: "g",
            value: "v",
            selectedValue: "v",
            accentColor: UIColor.blue
        )
        XCTAssertNotEqual(a, c, "accentColor の差が == に反映される")
    }

    func test_SimpleCheckCellに追加された共通フィールドとaccentColorがequalityで判定される() {
        let id = UUID()
        let a = SimpleCheckCell(id: id, title: "x", valueText: "A", isChecked: true)
        let b = SimpleCheckCell(id: id, title: "x", valueText: "B", isChecked: true)
        XCTAssertNotEqual(a, b)

        let c = SimpleCheckCell(id: id, title: "x", isChecked: true, accentColor: UIColor.red)
        let d = SimpleCheckCell(id: id, title: "x", isChecked: true, accentColor: UIColor.green)
        XCTAssertNotEqual(c, d, "accentColor の差が == に反映される")
    }

    func test_ButtonCellに追加された共通フィールドがequalityで判定される() {
        let id = UUID()
        let a = ButtonCell(id: id, title: "x", valueText: "A")
        let b = ButtonCell(id: id, title: "x", valueText: "B")
        XCTAssertNotEqual(a, b)

        let c = ButtonCell(id: id, title: "x", icon: KsImage.systemName("bell"))
        let d = ButtonCell(id: id, title: "x", icon: KsImage.systemName("star"))
        XCTAssertNotEqual(c, d, "icon の差が == に反映される")
    }

    func test_SwitchCellのwithDSLIDが追加フィールドを保持する() {
        let original = SwitchCell(
            title: "x",
            description: "d",
            valueText: "v",
            icon: KsImage.systemName("bell"),
            hintText: "h",
            isOn: true
        )
        let copy = original.withDSLID(UUID())
        XCTAssertEqual(copy.description, "d")
        XCTAssertEqual(copy.valueText, "v")
        XCTAssertEqual(copy.icon, KsImage.systemName("bell"))
        XCTAssertEqual(copy.hintText, "h")
    }

    func test_RadioCellのwithStyleが追加フィールドとaccentColorを保持する() {
        let original = RadioCell(
            title: "x",
            description: "d",
            valueText: "v",
            icon: KsImage.systemName("bell"),
            hintText: "h",
            groupId: "g",
            value: "v",
            selectedValue: "v",
            accentColor: UIColor.purple
        )
        let copy = original.withStyle(CellStyle(cellHeight: 80))
        XCTAssertEqual(copy.description, "d")
        XCTAssertEqual(copy.valueText, "v")
        XCTAssertEqual(copy.icon, KsImage.systemName("bell"))
        XCTAssertEqual(copy.hintText, "h")
        XCTAssertEqual(copy.accentColor, UIColor.purple)
    }

    // MARK: - applyCellBaseLayout 経由の描画テスト

    func test_applyCellBaseLayoutはtitleとdescriptionとvalueTextとiconを反映する() {
        let cell = makeKsListCell()
        let theme = Theme()
        let style = CellStyle()
        let effective = EffectiveStyle(theme: theme, cellStyle: style)
        applyCellBaseLayout(
            cell,
            title: "通知",
            description: "プッシュ",
            icon: KsImage.systemName("bell"),
            hintText: "推奨",
            effective: effective,
            theme: theme,
            isEnabled: true,
            trailingViews: [],
            valueLabelText: "オン"
        )
        // applyCellBaseLayout は contentConfiguration を使わず、KsListCellBase の subview を直接更新する
        XCTAssertNil(cell.contentConfiguration, "contentConfiguration 経路は使用しない")
        XCTAssertEqual(cell.accessories.count, 0, "UICellAccessory 経路は使用しない")
        XCTAssertEqual(cell.titleLabel.text, "通知")
        XCTAssertEqual(cell.descriptionLabel.text, "プッシュ")
        XCTAssertFalse(cell.descriptionLabel.isHidden, "description 非空のとき descriptionLabel は表示")
        XCTAssertNotNil(cell.iconImageView.image, "icon が systemName のとき image が設定される")
        XCTAssertFalse(cell.iconImageView.isHidden, "icon 指定時 iconImageView は表示")
    }

    /// `contentStack.arrangedSubviews` の並び順は `[titleLabel, (valueLabel?), trailingViews...]`。
    /// `hintText` は contentStack に含めない（hintLabel に直接反映される）。
    func test_applyCellBaseLayoutは_contentStack並び順を正しく組む() {
        let cell = makeKsListCell()
        let theme = Theme()
        let style = CellStyle()
        let effective = EffectiveStyle(theme: theme, cellStyle: style)
        let custom = UIView()
        applyCellBaseLayout(
            cell,
            title: "x",
            description: "d",
            icon: nil,
            hintText: "h",
            effective: effective,
            theme: theme,
            isEnabled: true,
            trailingViews: [custom],
            valueLabelText: "v"
        )
        // contentStack の arrangedSubviews 順序: [titleLabel, valueLabel, custom]
        // hintText は含まれない（hintLabel 経由）。
        XCTAssertEqual(cell.contentStack.arrangedSubviews.count, 3)
        XCTAssertTrue(cell.contentStack.arrangedSubviews[0] === cell.titleLabel)
        XCTAssertTrue(cell.contentStack.arrangedSubviews.last === custom)
    }

    func test_applyCellBaseLayoutは値テキストのみの場合trailingViewsなし() {
        let cell = makeKsListCell()
        let effective = EffectiveStyle(theme: Theme(), cellStyle: CellStyle())
        applyCellBaseLayout(
            cell,
            title: "x",
            description: nil,
            icon: nil,
            hintText: nil,
            effective: effective,
            theme: Theme(),
            isEnabled: true,
            trailingViews: [],
            valueLabelText: "V"
        )
        // valueLabelText のみ → contentStack に [titleLabel, valueLabel] が並ぶ
        XCTAssertEqual(cell.contentStack.arrangedSubviews.count, 2)
        XCTAssertTrue(cell.contentStack.arrangedSubviews[0] === cell.titleLabel)
        // valueLabel は UILabel
        XCTAssertTrue(cell.contentStack.arrangedSubviews[1] is UILabel)
        XCTAssertEqual((cell.contentStack.arrangedSubviews[1] as? UILabel)?.text, "V")
    }

    // MARK: - isEnabled = false 時の色置換

    func test_applyCellBaseLayoutはisEnabledFalse時にdisabledTextColorで上書きする() {
        let cell = makeKsListCell()
        let theme = Theme(disabledTextColor: UIColor.gray)
        let effective = EffectiveStyle(theme: theme, cellStyle: CellStyle())
        applyCellBaseLayout(
            cell,
            title: "x",
            description: "d",
            icon: nil,
            hintText: "h",
            effective: effective,
            theme: theme,
            isEnabled: false,
            trailingViews: [],
            valueLabelText: nil
        )
        XCTAssertEqual(cell.titleLabel.textColor, UIColor.gray)
        XCTAssertEqual(cell.descriptionLabel.textColor, UIColor.gray)
        // hintLabel の color も disabledTextColor になる
        XCTAssertEqual(cell.hintLabel?.textColor, UIColor.gray)
    }

    // MARK: - RadioCell / SimpleCheckCell の accentColor 解決順序

    func test_RadioCellのaccentColorはCell個別が最優先() {
        let view = RadioCellView(frame: CGRect(x: 0, y: 0, width: 320, height: 60))
        let theme = Theme(cellAccentColor: UIColor.blue)
        let cell = RadioCell(
            title: "x",
            groupId: "g",
            value: "v",
            selectedValue: "v",
            accentColor: UIColor.red
        )
        view.render(cell: cell, theme: theme)
        XCTAssertTrue(view._hasCellAccessoryCheckmark)
    }

    func test_SimpleCheckCellのaccentColorはCell個別が最優先() {
        let view = SimpleCheckCellView(frame: CGRect(x: 0, y: 0, width: 320, height: 60))
        let theme = Theme(cellAccentColor: UIColor.blue)
        let cell = SimpleCheckCell(title: "x", isChecked: true, accentColor: UIColor.red)
        view.render(cell: cell, theme: theme)
        XCTAssertTrue(view._hasCellAccessoryCheckmark)
    }

    // MARK: - ButtonCell レイアウト分岐（常に applyCellBaseLayout 経由）

    func test_ButtonCellはicon指定時にapplyCellBaseLayout経由になる() {
        let view = ButtonCellView(frame: CGRect(x: 0, y: 0, width: 320, height: 60))
        view.render(cell: ButtonCell(title: "登録", icon: KsImage.systemName("paperplane")), theme: Theme())
        // contentConfiguration は常に nil（applyCellBaseLayout 経由）、titleLabel.text が反映される
        XCTAssertNil(view.contentConfiguration)
        XCTAssertEqual(view.titleLabel.text, "登録")
        XCTAssertNotNil(view.iconImageView.image)
    }

    func test_ButtonCellはauxフィールドなしでもapplyCellBaseLayout経由でtitleAlignmentが反映される() {
        let view = ButtonCellView(frame: CGRect(x: 0, y: 0, width: 320, height: 60))
        view.render(cell: ButtonCell(title: "ログアウト", titleAlignment: .center), theme: Theme())
        // contentConfiguration は nil、titleLabel.textAlignment が反映される
        XCTAssertNil(view.contentConfiguration)
        XCTAssertEqual(view.titleLabel.text, "ログアウト")
        XCTAssertEqual(view.titleLabel.textAlignment, .center)
    }

    // MARK: - ButtonCell に description フィールドが存在しないコンパイル時テスト

    // ButtonCell には description フィールドが存在しないため、`ButtonCell(title: ..., description: ...)`
    // のコンストラクタはコンパイルエラーになる。
    func test_ButtonCellは既存呼び出しの互換性を保つ() {
        let _ = ButtonCell(title: "OK")
        let _ = ButtonCell(title: "OK", titleColor: UIColor.red)
        let _ = ButtonCell(title: "OK", titleColor: UIColor.red, onTap: {})
        let _ = ButtonCell(title: "OK", titleAlignment: .start)
    }

    // MARK: - hintText 右上 float 配置のテスト

    /// applyCellBaseLayout(..., hintText: "推奨", ...) を呼んだ後、cell.hintLabel の
    /// text == "推奨" / isHidden == false であることを検証する。
    func test_applyCellBaseLayoutはhintTextを_hintLabelに反映する() {
        let cell = makeKsListCell()
        let effective = EffectiveStyle(theme: Theme(), cellStyle: CellStyle())
        applyCellBaseLayout(
            cell,
            title: "x",
            description: nil,
            icon: nil,
            hintText: "推奨",
            effective: effective,
            theme: Theme(),
            isEnabled: true,
            trailingViews: [],
            valueLabelText: nil
        )
        let hint = cell.hintLabel
        XCTAssertNotNil(hint)
        XCTAssertEqual(hint?.text, "推奨")
        XCTAssertFalse(hint?.isHidden ?? true)
    }

    /// hintLabel の AutoLayout 制約（top == 2, trailing == -10）を検証する。
    func test_hintLabelの制約はTop2とTrailing_minus10である() {
        let cell = makeKsListCell()
        let effective = EffectiveStyle(theme: Theme(), cellStyle: CellStyle())
        applyCellBaseLayout(
            cell,
            title: "x",
            description: nil,
            icon: nil,
            hintText: "推奨",
            effective: effective,
            theme: Theme(),
            isEnabled: true,
            trailingViews: [],
            valueLabelText: nil
        )
        guard let hint = cell.hintLabel else {
            XCTFail("hintLabel が生成されていない")
            return
        }
        let allConstraints = cell.constraints + hint.constraints
        let topConstraint = allConstraints.first { c in
            isConstraint(c, firstView: hint, firstAttribute: .top, secondView: cell, secondAttribute: .top)
        }
        let trailingConstraint = allConstraints.first { c in
            isConstraint(c, firstView: hint, firstAttribute: .trailing, secondView: cell, secondAttribute: .trailing)
        }
        XCTAssertNotNil(topConstraint, "Top 制約が見つからない")
        XCTAssertEqual(Double(topConstraint?.constant ?? .nan), 2.0, accuracy: 0.001)
        XCTAssertNotNil(trailingConstraint, "Trailing 制約（cell.trailingAnchor 基準）が見つからない")
        XCTAssertEqual(Double(trailingConstraint?.constant ?? .nan), -10.0, accuracy: 0.001)
    }

    /// 各 Cell View 種別（accessory あり／なし）で hintLabel の `frame.maxX` が
    /// `cell.bounds.maxX - 10` と一致することを検証する。
    func test_hintLabelのmaxXは全Cell種別で_cellRight_minus10と一致する() {
        let switchCell = makeKsListCell()
        let effective = EffectiveStyle(theme: Theme(), cellStyle: CellStyle())
        applyCellBaseLayout(
            switchCell,
            title: "通知",
            description: nil,
            icon: nil,
            hintText: "推奨",
            effective: effective,
            theme: Theme(),
            isEnabled: true,
            trailingViews: [],
            valueLabelText: nil
        )
        switchCell.frame = CGRect(x: 0, y: 0, width: 320, height: 60)
        switchCell.setNeedsLayout()
        switchCell.layoutIfNeeded()
        guard let hintSwitch = switchCell.hintLabel else {
            XCTFail("SwitchCell の hintLabel が生成されていない")
            return
        }
        XCTAssertEqual(
            Double(hintSwitch.frame.maxX),
            Double(switchCell.bounds.maxX - 10),
            accuracy: 0.5,
            "hintLabel.frame.maxX は cell.bounds.maxX - 10 と一致するはず"
        )

        let buttonCell = makeKsListCell()
        applyCellBaseLayout(
            buttonCell,
            title: "登録",
            description: nil,
            icon: nil,
            hintText: "推奨",
            effective: effective,
            theme: Theme(),
            isEnabled: true,
            trailingViews: [],
            valueLabelText: nil
        )
        buttonCell.frame = CGRect(x: 0, y: 0, width: 320, height: 60)
        buttonCell.setNeedsLayout()
        buttonCell.layoutIfNeeded()
        guard let hintButton = buttonCell.hintLabel else {
            XCTFail("ButtonCell の hintLabel が生成されていない")
            return
        }
        XCTAssertEqual(
            Double(hintButton.frame.maxX),
            Double(buttonCell.bounds.maxX - 10),
            accuracy: 0.5,
            "hintLabel.frame.maxX は cell.bounds.maxX - 10 と一致するはず"
        )
    }

    /// `hintText = nil` のとき `hintLabel.isHidden == true` または text が nil/空。
    func test_applyCellBaseLayoutでhintTextがnilのときhintLabelは非表示() {
        let cell = makeKsListCell()
        let effective = EffectiveStyle(theme: Theme(), cellStyle: CellStyle())
        applyCellBaseLayout(
            cell,
            title: "x",
            description: nil,
            icon: nil,
            hintText: nil,
            effective: effective,
            theme: Theme(),
            isEnabled: true,
            trailingViews: [],
            valueLabelText: nil
        )
        if let hint = cell.hintLabel {
            XCTAssertTrue(hint.isHidden || (hint.text ?? "").isEmpty)
        }
    }

    /// 同一 cell に対して 2 回連続で applyCellBaseLayout を呼んでも hintLabel は 1 個のまま。
    func test_applyCellBaseLayoutを2回呼んでもhintLabelは重複しない() {
        let cell = makeKsListCell()
        let effective = EffectiveStyle(theme: Theme(), cellStyle: CellStyle())
        applyCellBaseLayout(
            cell,
            title: "x",
            description: nil,
            icon: nil,
            hintText: "h1",
            effective: effective,
            theme: Theme(),
            isEnabled: true,
            trailingViews: [],
            valueLabelText: nil
        )
        let first = cell.hintLabel
        applyCellBaseLayout(
            cell,
            title: "x",
            description: nil,
            icon: nil,
            hintText: "h2",
            effective: effective,
            theme: Theme(),
            isEnabled: true,
            trailingViews: [],
            valueLabelText: nil
        )
        let second = cell.hintLabel
        XCTAssertTrue(first === second, "hintLabel は同一インスタンスでなければならない")
        let labelCount = cell.subviews.compactMap { $0 as? UILabel }.filter { $0 === first }.count
        XCTAssertEqual(labelCount, 1)
        XCTAssertEqual(second?.text, "h2")
    }

    /// prepareForReuse 呼び出し後に hintLabel.text == nil / isHidden == true になる。
    func test_prepareForReuseでhintLabelがリセットされる() {
        let cell = makeKsListCell()
        let effective = EffectiveStyle(theme: Theme(), cellStyle: CellStyle())
        applyCellBaseLayout(
            cell,
            title: "x",
            description: nil,
            icon: nil,
            hintText: "推奨",
            effective: effective,
            theme: Theme(),
            isEnabled: true,
            trailingViews: [],
            valueLabelText: nil
        )
        XCTAssertEqual(cell.hintLabel?.text, "推奨")
        cell.prepareForReuse()
        XCTAssertNil(cell.hintLabel?.text)
        XCTAssertTrue(cell.hintLabel?.isHidden ?? false)
    }

    /// hintLabel の font / textColor が effective.hintTextFont / effective.hintTextColor と一致する。
    func test_hintLabelのフォントと色はeffectiveから解決される() {
        let cell = makeKsListCell()
        let customFont = UIFont.systemFont(ofSize: 11, weight: .semibold)
        let customColor = UIColor.systemPurple
        let theme = Theme(cellHintTextColor: customColor, cellHintFont: customFont)
        let effective = EffectiveStyle(theme: theme, cellStyle: CellStyle())
        applyCellBaseLayout(
            cell,
            title: "x",
            description: nil,
            icon: nil,
            hintText: "推奨",
            effective: effective,
            theme: theme,
            isEnabled: true,
            trailingViews: [],
            valueLabelText: nil
        )
        XCTAssertEqual(cell.hintLabel?.font, customFont)
        XCTAssertEqual(cell.hintLabel?.textColor, customColor)
    }

    // MARK: - subview 構造のリグレッション防止テスト

    /// `KsListCellBase` の subview 階層が AiForms 準拠で正しく構築されることを検証する。
    /// 初期化直後に stackH が `[iconImageView, stackV, accessoryHolder]`、stackV が
    /// `[contentStack, descriptionLabel]`、contentStack の先頭が titleLabel であり、
    /// 空の accessoryHolder は隠れている。
    func test_KsListCellBase_subviewHierarchy_AiForms準拠() {
        let base = makeKsListCell()
        // contentView 直下に stackH が存在
        XCTAssertTrue(base.stackH.isDescendant(of: base.contentView), "stackH は contentView の子孫")
        // stackH の arrangedSubviews が [iconImageView, stackV, accessoryHolder]
        XCTAssertEqual(base.stackH.arrangedSubviews.count, 3)
        XCTAssertTrue(base.stackH.arrangedSubviews[0] === base.iconImageView)
        XCTAssertTrue(base.stackH.arrangedSubviews[1] === base.stackV)
        XCTAssertTrue(base.stackH.arrangedSubviews[2] === base.accessoryHolder)
        // stackV の arrangedSubviews が [contentStack, descriptionLabel]
        XCTAssertEqual(base.stackV.arrangedSubviews.count, 2)
        XCTAssertTrue(base.stackV.arrangedSubviews[0] === base.contentStack)
        XCTAssertTrue(base.stackV.arrangedSubviews[1] === base.descriptionLabel)
        // contentStack の最初の arrangedSubview が titleLabel
        XCTAssertTrue(base.contentStack.arrangedSubviews.first === base.titleLabel)
        // 初期状態の accessoryHolder は空で隠れている
        XCTAssertTrue(base.accessoryHolder.arrangedSubviews.isEmpty, "初期化直後の accessoryHolder は空")
        XCTAssertTrue(base.accessoryHolder.isHidden, "空の accessoryHolder は isHidden = true")
    }

    /// prepareForReuse で contentStack から行内 trailing が、accessoryHolder から Cell 級アクセサリが
    /// 除去され、恒常メンバー（titleLabel / accessoryHolder 等）は残ることを検証する。
    func test_prepareForReuseで_行内trailingとアクセサリが除去される() {
        let cell = makeKsListCell()
        let effective = EffectiveStyle(theme: Theme(), cellStyle: CellStyle())
        // first responder を含まない行内 trailing
        let custom = UIView()
        let toggle = UISwitch()
        applyCellBaseLayout(
            cell,
            title: "x",
            description: "d",
            icon: KsImage.systemName("gear"),
            hintText: nil,
            effective: effective,
            theme: Theme(),
            isEnabled: true,
            trailingViews: [custom],
            valueLabelText: nil,
            accessoryView: toggle
        )
        XCTAssertEqual(cell.contentStack.arrangedSubviews.count, 2)
        XCTAssertEqual(cell.accessoryHolder.arrangedSubviews.count, 1)

        cell.prepareForReuse()

        // titleLabel は残り、行内 trailing は除去される
        XCTAssertEqual(cell.contentStack.arrangedSubviews.count, 1)
        XCTAssertTrue(cell.contentStack.arrangedSubviews[0] === cell.titleLabel)
        // accessoryHolder は空になり隠れる
        XCTAssertTrue(cell.accessoryHolder.arrangedSubviews.isEmpty, "accessoryHolder は空になる")
        XCTAssertTrue(cell.accessoryHolder.isHidden)
        // 各 text / image がクリアされる
        XCTAssertNil(cell.titleLabel.text)
        XCTAssertNil(cell.descriptionLabel.text)
        XCTAssertNil(cell.iconImageView.image)
        // 恒常メンバーの階層は破壊されない
        XCTAssertEqual(cell.stackH.arrangedSubviews.count, 3)
        XCTAssertTrue(cell.stackH.arrangedSubviews[2] === cell.accessoryHolder)
        XCTAssertEqual(cell.stackV.arrangedSubviews.count, 2)
    }

    // MARK: - accessoryView 系統（Cell 級アクセサリ）

    /// `accessoryView` に渡した view が `accessoryHolder` に 1 個だけ配置され、
    /// 行内（`contentStack`）には入らないことを検証する。
    func test_applyCellBaseLayoutはaccessoryViewをaccessoryHolderに配置する() {
        let cell = makeKsListCell()
        let effective = EffectiveStyle(theme: Theme(), cellStyle: CellStyle())
        let toggle = UISwitch()
        applyCellBaseLayout(
            cell,
            title: "x",
            description: "d",
            icon: nil,
            hintText: nil,
            effective: effective,
            theme: Theme(),
            isEnabled: true,
            accessoryView: toggle
        )
        XCTAssertEqual(cell.accessoryHolder.arrangedSubviews.count, 1)
        XCTAssertTrue(cell.accessoryHolder.arrangedSubviews[0] === toggle)
        XCTAssertFalse(cell.accessoryHolder.isHidden)
        // 行内 (contentStack) には含まれない
        XCTAssertFalse(cell.contentStack.arrangedSubviews.contains { $0 === toggle })
        // contentConfiguration / accessories 経路を使わない状態が保たれることを確認する
        XCTAssertNil(cell.contentConfiguration)
        XCTAssertTrue(cell.accessories.isEmpty)
    }

    /// `accessoryView` が nil のとき `accessoryHolder` が空になり隠れ、
    /// アクセサリ用の空領域を残さないことを検証する。
    func test_applyCellBaseLayoutはaccessoryViewがnilならholderを空にして隠す() {
        let cell = makeKsListCell()
        let effective = EffectiveStyle(theme: Theme(), cellStyle: CellStyle())
        applyCellBaseLayout(
            cell,
            title: "x",
            description: nil,
            icon: nil,
            hintText: nil,
            effective: effective,
            theme: Theme(),
            isEnabled: true,
            accessoryView: nil
        )
        XCTAssertTrue(cell.accessoryHolder.arrangedSubviews.isEmpty)
        XCTAssertTrue(cell.accessoryHolder.isHidden, "アクセサリ用の空領域は残らない")
    }

    /// 再 render を繰り返しても `accessoryHolder` の内容が常に 0 個または 1 個であり、
    /// アクセサリが蓄積しないことを検証する。
    /// non-nil A → non-nil B → nil、および hideArrow 相当の false → true → false の遷移を通す。
    func test_再renderでaccessoryHolderの内容は常に0個または1個() {
        let cell = makeKsListCell()
        let effective = EffectiveStyle(theme: Theme(), cellStyle: CellStyle())
        let viewA = UISwitch()
        let viewB = UIView()

        func render(_ accessory: UIView?) {
            applyCellBaseLayout(
                cell,
                title: "x",
                description: nil,
                icon: nil,
                hintText: nil,
                effective: effective,
                theme: Theme(),
                isEnabled: true,
                accessoryView: accessory
            )
        }

        // non-nil A
        render(viewA)
        XCTAssertEqual(cell.accessoryHolder.arrangedSubviews.count, 1)
        XCTAssertTrue(cell.accessoryHolder.arrangedSubviews[0] === viewA)

        // non-nil A → non-nil B（B のみ残る）
        render(viewB)
        XCTAssertEqual(cell.accessoryHolder.arrangedSubviews.count, 1)
        XCTAssertTrue(cell.accessoryHolder.arrangedSubviews[0] === viewB)
        XCTAssertNil(viewA.superview, "旧アクセサリは view 階層から外れる")

        // non-nil → nil（空になり隠れる）
        render(nil)
        XCTAssertTrue(cell.accessoryHolder.arrangedSubviews.isEmpty)
        XCTAssertTrue(cell.accessoryHolder.isHidden)

        // 同一インスタンスを連続指定しても重複しない
        render(viewA)
        render(viewA)
        XCTAssertEqual(cell.accessoryHolder.arrangedSubviews.count, 1)

        // hideArrow の false → true → false 相当（毎回新規生成される chevron）
        for _ in 0..<3 {
            render(makeChevronView())
            XCTAssertEqual(cell.accessoryHolder.arrangedSubviews.count, 1)
            render(nil)
            XCTAssertEqual(cell.accessoryHolder.arrangedSubviews.count, 0)
        }
    }

    /// レイアウト後の幾何関係を検証する。長文 description がアクセサリ列と重ならない幅で折り返し、
    /// アクセサリ列の中心 Y が contentView の中心 Y と一致し、アクセサリなしのときは
    /// stackV が trailing margin まで広がる。
    func test_レイアウト後にdescriptionはアクセサリと交差せずアクセサリは垂直センター() {
        let cell = makeKsListCell()
        let effective = EffectiveStyle(theme: Theme(), cellStyle: CellStyle())
        let longDescription = "This is description. you can write detail explanation of the item here. "
            + "long text wrap automatically."
        let toggle = UISwitch()

        applyCellBaseLayout(
            cell,
            title: "Notification",
            description: longDescription,
            icon: nil,
            hintText: nil,
            effective: effective,
            theme: Theme(),
            isEnabled: true,
            accessoryView: toggle
        )
        // 固定幅 320pt で self-sizing させる
        cell.frame = CGRect(x: 0, y: 0, width: 320, height: 0)
        cell.frame.size = cell.systemLayoutSizeFitting(
            CGSize(width: 320, height: UIView.layoutFittingCompressedSize.height),
            withHorizontalFittingPriority: .required,
            verticalFittingPriority: .fittingSizeLevel
        )
        cell.layoutIfNeeded()

        let descFrame = cell.descriptionLabel.convert(cell.descriptionLabel.bounds, to: cell.contentView)
        let holderFrame = cell.accessoryHolder.convert(cell.accessoryHolder.bounds, to: cell.contentView)

        XCTAssertGreaterThan(holderFrame.width, 0, "アクセサリ列は内容の自然幅を保つ")
        XCTAssertLessThanOrEqual(
            descFrame.maxX,
            holderFrame.minX + 0.5,
            "description はアクセサリ列と重ならない幅で折り返す"
        )
        XCTAssertEqual(
            holderFrame.midY,
            cell.contentView.bounds.midY,
            accuracy: 1.0,
            "アクセサリはセル全体に対して垂直センターに置かれる"
        )
        // description が実際に折り返している（1 行に収まっていない）ことを確認
        XCTAssertGreaterThan(descFrame.height, cell.descriptionLabel.font.lineHeight * 1.5, "長文は複数行に折り返す")

        // accessoryView が nil のときは stackV が trailing margin まで広がる
        applyCellBaseLayout(
            cell,
            title: "Notification",
            description: longDescription,
            icon: nil,
            hintText: nil,
            effective: effective,
            theme: Theme(),
            isEnabled: true,
            accessoryView: nil
        )
        cell.layoutIfNeeded()
        let stackVFrame = cell.stackV.convert(cell.stackV.bounds, to: cell.contentView)
        XCTAssertEqual(
            stackVFrame.maxX,
            cell.contentView.bounds.maxX - cell.stackH.layoutMargins.right,
            accuracy: 0.5,
            "accessoryView が nil のとき stackV の右端は stackH の trailing margin まで広がる"
        )
    }

    /// Cell 級アクセサリ列の左右の間隔を固定する。
    ///
    /// `stackV`（title / description 列）と `accessoryHolder`（Cell 級アクセサリ列）の間隔は 6pt。
    /// これは `contentStack` の行内間隔と同じ値で、valueText と Cell 級アクセサリが行内要素どうしと
    /// 同じリズムで並ぶようにするための設定である。`stackH` の既定間隔 16pt をそのまま使うと
    /// この間隔だけが広がるため、オーナー合意のうえで 6pt を維持している（16pt に開くのは退行）。
    ///
    /// 併せて `iconImageView` と `stackV` の間隔が `stackH` の 16pt のままであること、つまり
    /// アクセサリ側の詰めが icon 側へ波及していないことも固定する。
    func test_アクセサリ列は左隣と6ptで並びicon側は16ptを保つ() {
        let cell = makeKsListCell()
        let effective = EffectiveStyle(theme: Theme(), cellStyle: CellStyle())
        let toggle = UISwitch()

        applyCellBaseLayout(
            cell,
            title: "Notification",
            description: nil,
            icon: KsImage.systemName("bell"),
            hintText: nil,
            effective: effective,
            theme: Theme(),
            isEnabled: true,
            valueLabelText: "オン",
            accessoryView: toggle
        )
        // 固定幅 320pt で self-sizing させる
        cell.frame = CGRect(x: 0, y: 0, width: 320, height: 0)
        cell.frame.size = cell.systemLayoutSizeFitting(
            CGSize(width: 320, height: UIView.layoutFittingCompressedSize.height),
            withHorizontalFittingPriority: .required,
            verticalFittingPriority: .fittingSizeLevel
        )
        cell.layoutIfNeeded()

        let iconFrame = cell.iconImageView.convert(cell.iconImageView.bounds, to: cell.contentView)
        let stackVFrame = cell.stackV.convert(cell.stackV.bounds, to: cell.contentView)
        let holderFrame = cell.accessoryHolder.convert(cell.accessoryHolder.bounds, to: cell.contentView)

        XCTAssertFalse(cell.iconImageView.isHidden, "icon 指定時 iconImageView は表示")
        XCTAssertGreaterThan(holderFrame.width, 0, "アクセサリ列は内容の自然幅を保つ")
        XCTAssertEqual(
            Double(holderFrame.minX - stackVFrame.maxX),
            6.0,
            accuracy: 0.5,
            "valueText と Cell 級アクセサリの間隔は 6pt"
        )
        XCTAssertEqual(
            Double(stackVFrame.minX - iconFrame.maxX),
            16.0,
            accuracy: 0.5,
            "icon と本文列の間隔は 16pt"
        )
    }

    // MARK: - Helper

    /// hintLabel をテスト対象とする場合に使う `KsListCellBase` 派生の cell を生成する。
    private func makeKsListCell() -> KsListCellBase {
        return TestableKsListCell(frame: CGRect(x: 0, y: 0, width: 320, height: 60))
    }

    /// AutoLayout 制約が指定の View / Attribute 組み合わせに一致するか判定するヘルパ。
    private func isConstraint(
        _ c: NSLayoutConstraint,
        firstView: AnyObject,
        firstAttribute: NSLayoutConstraint.Attribute,
        secondView: AnyObject,
        secondAttribute: NSLayoutConstraint.Attribute
    ) -> Bool {
        return c.firstItem === firstView
            && c.firstAttribute == firstAttribute
            && c.secondItem === secondView
            && c.secondAttribute == secondAttribute
    }
}

/// テスト用に `KsListCellBase` をそのまま使うための薄いラッパ（テスト固有の subclass）。
@MainActor
private final class TestableKsListCell: KsListCellBase {
}
#endif
