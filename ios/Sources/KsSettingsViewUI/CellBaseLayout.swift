// CellBaseLayout.swift
// KsSettingsViewUI
//
// 全 Cell View が共通して使う **行レイアウト関数 `applyCellBaseLayout(...)`**。
//
// `KsListCellBase` が `init(frame:)` で install した自前 UIStackView 階層
// （`stackH` / `stackV` / `contentStack` / `iconImageView` / `titleLabel` / `descriptionLabel` /
// `accessoryHolder`）の各 subview を更新する形で Cell の見た目を構成する。
// `UIListContentConfiguration` / `UICellAccessory` 経路は使わない。
//
// trailing の受け口は 2 系統ある（ios/ADR-0001）。
//   - `accessoryView: UIView?` — Cell 級アクセサリ（`UISwitch` / checkbox / checkmark / chevron）。
//     `accessoryHolder`（stackH のアクセサリ列）へ配置され、セル全体に対して垂直センターに置かれる。
//     AiForms オリジナルの `UITableViewCell.AccessoryView` / `Accessory` 相当。
//   - `trailingViews: [UIView]` / `valueLabelText: String?` — 行内 trailing。
//     `contentStack` の titleLabel の後ろへ配置される。
//     AiForms オリジナルの `ContentStack.AddArrangedSubview` 相当。
// 例:
//   - EntryCellView → trailingViews: [fieldWrapper]（行内）
//   - CommandCellView → accessoryView: makeChevronView()
//   - SwitchCellView → accessoryView: uiSwitch
//   - PickerCellView → valueLabelText: "値"（行内）, accessoryView: makeChevronView()
//
// 参照:
//   - AiForms オリジナル `SettingsView/Native/iOS/Cells/CellBaseView.cs` SetUpContentView()（自前 stack 構造の根拠）

#if canImport(UIKit)
import UIKit
import KsSettingsViewCore

/// 全 Cell 共通の行レイアウト関数（theme 経由バリアント）。
///
/// `KsListCellBase` の自前 UIStackView 階層を更新する形で title / description / icon / hintText /
/// trailingViews を配置する。`UIListContentConfiguration` / `UICellAccessory` は使わない。
///
/// - Parameters:
///   - listCell: 対象 Cell（`KsListCellBase` 派生）
///   - title: タイトル（必須）
///   - description: 副題（任意、`nil` または空文字列で非表示）
///   - icon: アイコン（任意、`nil` で iconImageView を非表示）
///   - hintText: ヒントテキスト（任意、cell 右上に float 配置）
///   - effective: 実効スタイル（Theme × CellStyle）
///   - theme: Theme（`KsCellViewSupport.state(listCell).theme` に記録）
///   - isEnabled: 有効／無効（`false` のときテキスト色を `effective.disabledTextColor` で上書き）
///   - trailingViews: 行内 trailing。title の右に並べる UIView 群（順序通り `contentStack` に addArrangedSubview）
///   - valueLabelText: 行内 trailing の value 表示用ショートカット（non-nil のとき内部で `UILabel` を生成して trailingViews の先頭に詰む）
///   - accessoryView: Cell 級アクセサリ。non-nil のとき `accessoryHolder` へ配置し、`nil` のとき holder を空にして隠す
///   - titleColorOverride: `nil` 以外を指定すると、`isEnabled == true` 時の title 色をこの色で上書きする
@MainActor
internal func applyCellBaseLayout(
    _ listCell: KsListCellBase,
    title: String,
    description: String?,
    icon: KsImage?,
    hintText: String?,
    effective: EffectiveStyle,
    theme: Theme,
    isEnabled: Bool,
    trailingViews: [UIView] = [],
    valueLabelText: String? = nil,
    accessoryView: UIView? = nil,
    titleColorOverride: UIColor? = nil
) {
    // UIListContentConfiguration / UICellAccessory 経路を無効化し、
    // 自前 UIStackView 階層と描画が二重になるのを防ぐ
    listCell.contentConfiguration = nil
    listCell.accessories = []

    // 色解決（isEnabled = false のとき各テキスト色を disabledTextColor に置換）
    let titleColor: UIColor = isEnabled
        ? (titleColorOverride ?? effective.titleColor)
        : effective.disabledTextColor
    let descriptionColor: UIColor = isEnabled ? effective.descriptionColor : effective.disabledTextColor
    let valueTextColor: UIColor = isEnabled ? effective.valueTextColor : effective.disabledTextColor
    let hintTextColor: UIColor = isEnabled ? effective.hintTextColor : effective.disabledTextColor

    // title
    listCell.titleLabel.text = title
    listCell.titleLabel.font = effective.titleFont
    listCell.titleLabel.textColor = titleColor

    // description
    if let desc = description, !desc.isEmpty {
        listCell.descriptionLabel.text = desc
        listCell.descriptionLabel.font = effective.descriptionFont
        listCell.descriptionLabel.textColor = descriptionColor
        listCell.descriptionLabel.isHidden = false
    } else {
        listCell.descriptionLabel.text = nil
        listCell.descriptionLabel.isHidden = true
    }

    // icon
    if let icon = icon {
        switch icon {
        case .systemName(let name):
            listCell.iconImageView.image = normalizedIconImage(UIImage(systemName: name))
        case .uiImage(let image):
            listCell.iconImageView.image = normalizedIconImage(image)
        }
        if listCell.iconImageView.image != nil {
            // 角丸は解決済み icon size の正方形枠に対してかかり、aspect fit 後の描画矩形には
            // 追従しない（core/ADR-0025）。
            listCell.iconImageView.layer.cornerRadius = effective.iconRadius
            listCell.showIcon(size: effective.iconSize)
        } else {
            // 解決できない systemName を指定された場合は icon 列を持たない行として扱う。
            listCell.iconImageView.layer.cornerRadius = 0
            listCell.hideIcon()
        }
    } else {
        listCell.iconImageView.image = nil
        listCell.iconImageView.layer.cornerRadius = 0
        listCell.hideIcon()
    }

    // 背景色
    var bg = listCell.defaultBackgroundConfiguration()
    bg.backgroundColor = effective.cellBackgroundColor
    listCell.backgroundConfiguration = bg

    // 共通状態反映（タッチフィードバック / 高さ）
    KsCellViewSupport.state(listCell).theme = theme
    KsCellViewSupport.setRenderState(
        listCell,
        theme: theme,
        isEnabled: isEnabled,
        effectiveBackgroundColor: effective.cellBackgroundColor
    )
    KsCellViewSupport.applyEffectiveHeight(listCell, effective: effective)

    // contentStack の trailingViews を一旦クリアして再構築する
    listCell.clearContentStackTrailingViews()

    // valueLabelText が指定された場合、内部で UILabel を生成して trailingViews の先頭に詰む
    // （AiForms.Maui.SettingsView と同じく title の右に value label が来る）
    if let valueText = valueLabelText {
        let valueLabel = UILabel()
        valueLabel.text = valueText
        valueLabel.font = effective.valueTextFont
        valueLabel.textColor = valueTextColor
        valueLabel.textAlignment = .right
        valueLabel.numberOfLines = 1
        valueLabel.lineBreakMode = .byTruncatingTail
        // value label は行内の残り領域を吸って広がる。trailingViews が無ければ contentStack の右端
        // （= Cell 級アクセサリ列の手前）まで、あれば trailingViews との間で按分する。
        valueLabel.setContentHuggingPriority(.defaultLow, for: .horizontal)
        valueLabel.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        listCell.contentStack.addArrangedSubview(valueLabel)
    }

    // trailingViews を contentStack に順次 addArrangedSubview
    for view in trailingViews {
        listCell.contentStack.addArrangedSubview(view)
    }

    // Cell 級アクセサリを accessoryHolder に配置する（旧内容は setAccessoryView 内で必ず除去され、
    // 再 render で蓄積しない）。nil のとき holder は空・非表示になり空領域を残さない。
    listCell.setAccessoryView(accessoryView)

    // hintLabel（cell 直下右上 float）
    let label = listCell.ensureHintLabel()
    label.font = effective.hintTextFont
    label.textColor = hintTextColor
    if let hint = hintText, !hint.isEmpty {
        label.text = hint
        label.isHidden = false
    } else {
        label.text = nil
        label.isHidden = true
    }
    // contentStack 更新後にも hintLabel を最前面に保つ
    listCell.bringSubviewToFront(label)
}

/// icon 領域へ渡す画像の整列矩形を、View の枠と一致させる。
///
/// SF Symbols は字形ごとに `alignmentRectInsets` を持ち、`UIImageView` はそれを自身の整列矩形へ
/// そのまま引き継ぐ。Auto Layout はこの整列矩形に対して制約を解くため、insets が残ったままでは
/// 同じ解決済み icon size を指定しても字形ごとに枠の実寸が変わってしまう。
/// icon 領域は指定サイズの正方形として確保する契約なので、insets を取り除いて枠と一致させる。
@MainActor
private func normalizedIconImage(_ image: UIImage?) -> UIImage? {
    guard let image = image else { return nil }
    guard image.alignmentRectInsets != .zero else { return image }
    return image.withAlignmentRectInsets(.zero)
}

/// chevron 表示用の共通ヘルパ。SF Symbol `chevron.right` を tintColor `.tertiaryLabel` で生成する。
///
/// `CommandCellView` / `PickerCellView` / `NumberPickerCellView` / `TimePickerCellView` / `DatePickerCellView`
/// が `accessoryView`（Cell 級アクセサリ）として渡す標準 chevron。
///
/// アセット・寸法・色は `KsChevronAppearance` に集約し、宣言 UI 経路
/// （`CustomCellHostedContent`）と同じ定数を共有する。
@MainActor
internal func makeChevronView() -> UIImageView {
    let config = UIImage.SymbolConfiguration(
        font: .preferredFont(forTextStyle: KsChevronAppearance.textStyle),
        scale: KsChevronAppearance.symbolScale
    )
    let image = UIImage(systemName: KsChevronAppearance.symbolName, withConfiguration: config)
    let iv = UIImageView(image: image)
    iv.tintColor = KsChevronAppearance.tintColor
    iv.contentMode = .center
    iv.setContentHuggingPriority(.required, for: .horizontal)
    iv.setContentCompressionResistancePriority(.required, for: .horizontal)
    return iv
}
#endif
