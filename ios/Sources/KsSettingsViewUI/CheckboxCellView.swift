// CheckboxCellView.swift
// KsSettingsViewUI
//
// `CheckboxCell` の Renderer 実装。共通行レイアウト関数 `applyCellBaseLayout(...)` 経由で描画し、
// 角丸の四角いチェックボックス（`KsCheckBoxView`）を Cell 級アクセサリとして `accessoryView` へ渡す。
// `UIListContentConfiguration` / `UICellAccessory` 経路は使わない。

#if canImport(UIKit)
import UIKit
import KsSettingsViewCore

/// `CheckboxCell` 描画用 Cell View。
@MainActor
internal final class CheckboxCellView: KsListCellBase, @MainActor KsCellRenderer {
    /// Cell タップ時に呼ぶハンドラ（外部からは Cell 全体タップで toggle 通知を発火）。
    internal var tapHandler: (@Sendable () -> Void)?

    /// 右端に常設する角丸チェックボックス。reuse をまたいで同一インスタンスを使い回す。
    private let checkBoxView = KsCheckBoxView(
        frame: CGRect(x: 0, y: 0, width: KsCheckBoxView.defaultSide, height: KsCheckBoxView.defaultSide)
    )

    override init(frame: CGRect) {
        super.init(frame: frame)
        // checkBoxView は固定サイズで残り領域を吸わない
        checkBoxView.setContentHuggingPriority(.required, for: .horizontal)
        checkBoxView.setContentCompressionResistancePriority(.required, for: .horizontal)
    }

    func render(cell: any KsCell, theme: Theme) {
        guard let cb = cell as? CheckboxCell else {
            assertionFailure("CheckboxCellView received unexpected cell type: \(type(of: cell))")
            return
        }
        let effective = EffectiveStyle(theme: theme, cellStyle: cb.style)

        // accent カラー: CheckboxCell.accentColor 優先、無ければ effective.accentColor。
        if let c = cb.accentColor {
            checkBoxView.accentColor = c
        } else {
            checkBoxView.accentColor = effective.accentColor
        }
        checkBoxView.isChecked = cb.isChecked
        checkBoxView.isEnabled = cb.isEnabled

        applyCellBaseLayout(
            self,
            title: cb.title,
            description: cb.description,
            icon: cb.icon,
            hintText: cb.hintText,
            effective: effective,
            theme: theme,
            isEnabled: cb.isEnabled,
            valueLabelText: cb.valueText,
            accessoryView: checkBoxView
        )

        // タップで toggle 通知を発火する Handler を仕掛ける（isEnabled = false なら無効化）
        if cb.isEnabled {
            let current = cb.isChecked
            let userHandler = cb.onValueChanged
            self.tapHandler = {
                userHandler?(!current)
            }
        } else {
            self.tapHandler = nil
        }
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        self.tapHandler = nil
        // カスタム View の状態をリセットしておく（再 bind 時に render で上書きされる）。
        checkBoxView.isChecked = false
        checkBoxView.isEnabled = true
    }

    // MARK: - テスト用アクセサ

    /// テストから内部チェックボックスの `isChecked` を覗くためのアクセサ。
    internal var _isCheckBoxChecked: Bool { checkBoxView.isChecked }

    /// テストから内部チェックボックスの `isEnabled` を覗くためのアクセサ。
    internal var _isCheckBoxEnabled: Bool { checkBoxView.isEnabled }

    /// テストから内部チェックボックスの Cell コンテナ側 alpha を覗くためのアクセサ。
    internal var _checkBoxViewAlpha: CGFloat { checkBoxView.alpha }

    /// テスト用: 右端の角丸チェックボックスが Cell 級アクセサリ列（`accessoryHolder`）に配置されているか。
    internal var _hasCellAccessoryCheckBox: Bool {
        return accessoryHolder.arrangedSubviews.contains { $0 === checkBoxView }
    }
}
#endif
