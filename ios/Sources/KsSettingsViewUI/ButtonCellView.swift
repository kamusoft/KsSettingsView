// ButtonCellView.swift
// KsSettingsViewUI
//
// `ButtonCell` の Renderer 実装。共通行レイアウト関数 `applyCellBaseLayout(...)` 経由で描画し、
// 描画後に `titleLabel.textAlignment` を `titleAlignment` で上書きする。
//
// `ButtonCell` は `valueText` / `icon` / `hintText` を共通フィールドとして持ち、`description` は持たない。
//
// 設計メモ:
// `UIListContentConfiguration` / `UICellAccessory` は使わず、すべての描画分岐を
// `applyCellBaseLayout` 経由に統一する（core/ADR-0011）。ボタンスタイル専用の独自 `UILabel` は
// 持たず、`KsListCellBase` が提供する `titleLabel` を使い、`titleAlignment` は render 後に
// `titleLabel.textAlignment` を直接設定して反映する。

#if canImport(UIKit)
import UIKit
import KsSettingsViewCore

/// `ButtonCell` 描画用 Cell View。
@MainActor
internal final class ButtonCellView: KsListCellBase, @MainActor KsCellRenderer {
    /// 直近 bind 時の `onTap` クロージャ。
    internal var tapHandler: (@Sendable () -> Void)?

    func render(cell: any KsCell, theme: Theme) {
        guard let btn = cell as? ButtonCell else {
            assertionFailure("ButtonCellView received unexpected cell type: \(type(of: cell))")
            return
        }
        let effective = EffectiveStyle(theme: theme, cellStyle: btn.style)

        // 平常時のテキスト色決定（4 段優先順位）。SoT は `EffectiveStyle.effectiveButtonTitleColor`。
        let baseColor = EffectiveStyle.effectiveButtonTitleColor(
            buttonCellTitleColor: btn.titleColor,
            cellStyle: btn.style,
            theme: theme
        )

        applyCellBaseLayout(
            self,
            title: btn.title,
            description: nil,
            icon: btn.icon,
            hintText: btn.hintText,
            effective: effective,
            theme: theme,
            isEnabled: btn.isEnabled,
            trailingViews: [],
            valueLabelText: btn.valueText,
            titleColorOverride: baseColor
        )

        // `titleAlignment` を `titleLabel.textAlignment` に反映する。
        // `icon` / `valueText` / `hintText` のいずれかが指定された場合でも、title 列内の揃え位置のみを制御する
        // （`titleLabel` 自体は title 列で残り領域を吸って広がるため、内側で alignment が効く）。
        self.titleLabel.textAlignment = Self.textAlignment(for: btn.titleAlignment)

        self.tapHandler = btn.isEnabled ? btn.onTap : nil
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        self.tapHandler = nil
        // 次回 render 時に textAlignment は再設定されるので明示的に戻す必要はないが、
        // 安全のため既定 (.natural) に戻す。
        self.titleLabel.textAlignment = .natural
    }

    /// `CellTitleAlignment` を `UILabel.textAlignment` (`NSTextAlignment`) に変換する。
    internal static func textAlignment(for alignment: CellTitleAlignment) -> NSTextAlignment {
        switch alignment {
        case .start: return .left
        case .center: return .center
        case .end: return .right
        }
    }
}
#endif
