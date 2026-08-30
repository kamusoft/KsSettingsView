// RadioCellView.swift
// KsSettingsViewUI
//
// `RadioCell` の Renderer 実装。共通行レイアウト関数 `applyCellBaseLayout(...)` 経由で描画し、
// checkmark を Cell 級アクセサリとして `accessoryView` へ渡す。
// `UIListContentConfiguration` / `UICellAccessory` 経路は使わない。

#if canImport(UIKit)
import UIKit
import KsSettingsViewCore

/// `RadioCell` 描画用 Cell View。
@MainActor
internal final class RadioCellView: KsListCellBase, @MainActor KsCellRenderer {
    internal var tapHandler: (@Sendable () -> Void)?

    /// 右端に常設する checkmark。reuse をまたいで同一インスタンスを使い回す。
    private let checkmarkView = KsCheckmarkAccessoryView()
    /// 初回 bind（reuse 直後）か否か。初回は即時 alpha、状態変化時のみ animate する（チラつき回避）。
    private var isInitialBind = true

    override init(frame: CGRect) {
        super.init(frame: frame)
        checkmarkView.setContentHuggingPriority(.required, for: .horizontal)
        checkmarkView.setContentCompressionResistancePriority(.required, for: .horizontal)
    }

    func render(cell: any KsCell, theme: Theme) {
        guard let rc = cell as? RadioCell else {
            assertionFailure("RadioCellView received unexpected cell type: \(type(of: cell))")
            return
        }
        let effective = EffectiveStyle(theme: theme, cellStyle: rc.style)

        // accent 色: RadioCell.accentColor → CellStyle.accentColor → Theme.cellAccentColor
        let resolvedAccent: UIColor = rc.accentColor ?? effective.accentColor

        // 視覚的 disabled は内部 View に委譲する。
        checkmarkView.isEnabled = rc.isEnabled

        // 選択状態を alpha フェードで反映。初回 bind は即時、状態変化時のみ animate。
        let isSelected = rc.value == rc.selectedValue
        checkmarkView.apply(selected: isSelected, accent: resolvedAccent, animated: !isInitialBind)
        isInitialBind = false

        applyCellBaseLayout(
            self,
            title: rc.title,
            description: rc.description,
            icon: rc.icon,
            hintText: rc.hintText,
            effective: effective,
            theme: theme,
            isEnabled: rc.isEnabled,
            valueLabelText: rc.valueText,
            accessoryView: checkmarkView
        )

        if rc.isEnabled {
            let userHandler = rc.onSelected
            let value = rc.value
            self.tapHandler = {
                userHandler?(value)
            }
        } else {
            self.tapHandler = nil
        }
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        self.tapHandler = nil
        isInitialBind = true
        checkmarkView.resetForReuse()
        checkmarkView.isEnabled = true
    }

    // MARK: - テスト用アクセサ

    /// テストから checkmark の alpha を覗くためのアクセサ。
    internal var _checkmarkAlpha: CGFloat { checkmarkView.checkmarkAlpha }

    /// テストから内部 checkmark View の `isEnabled` を覗くためのアクセサ。
    internal var _isCheckmarkEnabled: Bool { checkmarkView.isEnabled }

    /// テストから内部 checkmark View のコンテナ alpha を覗くためのアクセサ。
    internal var _checkmarkViewAlpha: CGFloat { checkmarkView.alpha }

    /// テスト用: 右端の checkmark が Cell 級アクセサリ列（`accessoryHolder`）に配置されているか。
    internal var _hasCellAccessoryCheckmark: Bool {
        return accessoryHolder.arrangedSubviews.contains { $0 === checkmarkView }
    }
}
#endif
