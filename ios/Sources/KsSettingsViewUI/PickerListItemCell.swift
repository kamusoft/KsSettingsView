// PickerListItemCell.swift
// KsSettingsViewUI
//
// `PickerCell` 選択面の候補行セル（内部用）。
//
// 主表示の下に副表示を置ける `.subtitle` 構成に固定する。副表示テキストが無い行は
// 主表示のみの 1 行構成として描画され、副表示は 1 行に収めて末尾を省略する。

#if canImport(UIKit)
import UIKit

/// 選択面の候補行セル（内部用）。主表示 + 任意の副表示の 2 行構成を持つ。
@MainActor
internal final class PickerListItemCell: UITableViewCell {

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        // 呼び出し元（`dequeueReusableCell` 経由の生成）が渡す style に関わらず、
        // 副表示を置ける構成へ固定する。
        super.init(style: .subtitle, reuseIdentifier: reuseIdentifier)
        detailTextLabel?.numberOfLines = 1
        detailTextLabel?.lineBreakMode = .byTruncatingTail
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }
}
#endif
