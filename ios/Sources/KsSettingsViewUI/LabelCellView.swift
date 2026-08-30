// LabelCellView.swift
// KsSettingsViewUI
//
// `LabelCell` の Renderer 実装。共通行レイアウト関数 `applyCellBaseLayout(...)` 経由で描画する。
//
// `UIListContentConfiguration` は使わず、`KsListCellBase` の自前 UIStackView 階層を更新する
// （core/ADR-0011）。

#if canImport(UIKit)
import UIKit
import KsSettingsViewCore

/// `LabelCell` を `KsListCellBase` ベースで描画する Cell View。
@MainActor
internal final class LabelCellView: KsListCellBase, @MainActor KsCellRenderer {

    func render(cell: any KsCell, theme: Theme) {
        guard let labelCell = cell as? LabelCell else {
            assertionFailure("LabelCellView received unexpected cell type: \(type(of: cell))")
            return
        }
        let effective = EffectiveStyle(theme: theme, cellStyle: labelCell.style)
        // 共通行レイアウト関数経由で描画（trailingViews 無し、valueText を valueLabelText で渡す）
        applyCellBaseLayout(
            self,
            title: labelCell.title,
            description: labelCell.description,
            icon: labelCell.icon,
            hintText: labelCell.hintText,
            effective: effective,
            theme: theme,
            isEnabled: labelCell.isEnabled,
            trailingViews: [],
            valueLabelText: labelCell.valueText
        )
    }
}
#endif
