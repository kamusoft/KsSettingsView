// SwitchCellView.swift
// KsSettingsViewUI
//
// `SwitchCell` の Renderer 実装。共通行レイアウト関数 `applyCellBaseLayout(...)` 経由で描画し、
// `UISwitch` を Cell 級アクセサリとして `accessoryView` へ渡す（セル全体に対し垂直センターに置かれる）。
// `UIListContentConfiguration` / `UICellAccessory` 経路は使わない。

#if canImport(UIKit)
import UIKit
import KsSettingsViewCore

/// `SwitchCell` 描画用 Cell View。
@MainActor
internal final class SwitchCellView: KsListCellBase, @MainActor KsCellRenderer {
    /// 値変更時に呼ばれるクロージャ。`UISwitch` の `valueChanged` から間接的に呼ばれる。
    internal var valueChangedHandler: (@Sendable (Bool) -> Void)?

    /// 右側に配置する `UISwitch`。Cell 内に保持して再利用時に同じインスタンスを使う。
    private let toggle: UISwitch = UISwitch()

    override init(frame: CGRect) {
        super.init(frame: frame)
        toggle.addTarget(self, action: #selector(handleValueChanged(_:)), for: .valueChanged)
        // UISwitch は固定サイズで残り領域を吸わない
        toggle.setContentHuggingPriority(.required, for: .horizontal)
        toggle.setContentCompressionResistancePriority(.required, for: .horizontal)
    }

    func render(cell: any KsCell, theme: Theme) {
        guard let sw = cell as? SwitchCell else {
            assertionFailure("SwitchCellView received unexpected cell type: \(type(of: cell))")
            return
        }
        let effective = EffectiveStyle(theme: theme, cellStyle: sw.style)

        // UISwitch の値・色を bind 時に毎回設定。SwitchCell.accentColor 優先、それ以外は effective.accentColor。
        toggle.setOn(sw.isOn, animated: false)
        if let c = sw.accentColor {
            toggle.onTintColor = c
        } else {
            toggle.onTintColor = effective.accentColor
        }
        toggle.isEnabled = sw.isEnabled

        applyCellBaseLayout(
            self,
            title: sw.title,
            description: sw.description,
            icon: sw.icon,
            hintText: sw.hintText,
            effective: effective,
            theme: theme,
            isEnabled: sw.isEnabled,
            valueLabelText: sw.valueText,
            accessoryView: toggle
        )

        // listener を最新の cell.onValueChanged に差し替え（旧クロージャ参照を防ぐ）。
        self.valueChangedHandler = sw.onValueChanged
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        self.valueChangedHandler = nil
    }

    @objc private func handleValueChanged(_ sender: UISwitch) {
        valueChangedHandler?(sender.isOn)
    }

    // MARK: - テスト用

    /// テストから UISwitch の `setOn(_:animated:) + sendActions(.valueChanged)` を発火させるためのフック。
    internal func _simulateValueChange(to newValue: Bool) {
        toggle.setOn(newValue, animated: false)
        handleValueChanged(toggle)
    }

    /// テストから `UISwitch.isEnabled` を覗くためのアクセサ。
    internal var _isSwitchEnabled: Bool { toggle.isEnabled }
}
#endif
