// CommandCellView.swift
// KsSettingsViewUI
//
// `CommandCell` の Renderer 実装。共通行レイアウト関数 `applyCellBaseLayout(...)` 経由で描画し、
// chevron を Cell 級アクセサリとして `accessoryView` へ渡す（`hideArrow == true` のとき nil）。
// `UIListContentConfiguration` / `UICellAccessory` 経路は使わない。

#if canImport(UIKit)
import UIKit
import KsSettingsViewCore

/// `CommandCell` 描画用 Cell View。
@MainActor
internal final class CommandCellView: KsListCellBase, @MainActor KsCellRenderer {
    /// 直近 bind 時の `onTap` クロージャ。
    internal var tapHandler: (@Sendable () -> Void)?

    func render(cell: any KsCell, theme: Theme) {
        guard let cmd = cell as? CommandCell else {
            assertionFailure("CommandCellView received unexpected cell type: \(type(of: cell))")
            return
        }
        let effective = EffectiveStyle(theme: theme, cellStyle: cmd.style)

        // chevron を Cell 級アクセサリとして渡す（hideArrow == true のとき nil）
        let accessoryView: UIView? = cmd.hideArrow ? nil : makeChevronView()

        applyCellBaseLayout(
            self,
            title: cmd.title,
            description: cmd.description,
            icon: cmd.icon,
            hintText: cmd.hintText,
            effective: effective,
            theme: theme,
            isEnabled: cmd.isEnabled,
            valueLabelText: cmd.valueText,
            accessoryView: accessoryView
        )

        // isEnabled = false の場合はタップを通さない
        self.tapHandler = cmd.isEnabled ? cmd.onTap : nil
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        self.tapHandler = nil
    }
}
#endif
