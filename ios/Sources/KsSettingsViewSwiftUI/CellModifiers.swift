// CellModifiers.swift
// KsSettingsViewSwiftUI
//
// `KsCell` プロトコル準拠の Cell に対する DSL 向け modifier 拡張。
//
// `CellStyle` / `KsImage` / `Theme` は UI 層 (`KsSettingsViewUI`) に属し、Native 型で表現する
// （core/ADR-0009）。したがって本ファイルは UI 層の `CellStyle` と `UIColor` / `UIFont` を
// 直接扱い、`titleColor(_:)` / `backgroundColor(_:)` / `font(_:)` は `UIColor` / `UIFont` を
// 引数に取る。

#if canImport(UIKit)
import Foundation
import SwiftUI
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI

extension KsCell {

    /// タイトル用フォントを上書きする。
    public func font(_ font: UIFont) -> Self {
        return mutateStyle { style in
            CellStyle(
                titleColor: style.titleColor,
                titleFont: font,
                descriptionColor: style.descriptionColor,
                descriptionFont: style.descriptionFont,
                valueTextColor: style.valueTextColor,
                valueTextFont: style.valueTextFont,
                iconSize: style.iconSize,
                iconRadius: style.iconRadius,
                cellHeight: style.cellHeight,
                hintTextColor: style.hintTextColor,
                hintTextFont: style.hintTextFont,
                backgroundColor: style.backgroundColor,
                accentColor: style.accentColor
            )
        }
    }

    /// 説明文用フォントを上書きする。
    public func descriptionFont(_ font: UIFont) -> Self {
        return mutateStyle { style in
            CellStyle(
                titleColor: style.titleColor,
                titleFont: style.titleFont,
                descriptionColor: style.descriptionColor,
                descriptionFont: font,
                valueTextColor: style.valueTextColor,
                valueTextFont: style.valueTextFont,
                iconSize: style.iconSize,
                iconRadius: style.iconRadius,
                cellHeight: style.cellHeight,
                hintTextColor: style.hintTextColor,
                hintTextFont: style.hintTextFont,
                backgroundColor: style.backgroundColor,
                accentColor: style.accentColor
            )
        }
    }

    /// アイコンの 1 辺サイズ（pt）を上書きする。
    public func iconSize(_ size: CGFloat) -> Self {
        return mutateStyle { style in
            CellStyle(
                titleColor: style.titleColor,
                titleFont: style.titleFont,
                descriptionColor: style.descriptionColor,
                descriptionFont: style.descriptionFont,
                valueTextColor: style.valueTextColor,
                valueTextFont: style.valueTextFont,
                iconSize: size,
                iconRadius: style.iconRadius,
                cellHeight: style.cellHeight,
                hintTextColor: style.hintTextColor,
                hintTextFont: style.hintTextFont,
                backgroundColor: style.backgroundColor,
                accentColor: style.accentColor
            )
        }
    }

    /// Cell 高さを上書きする。
    public func cellHeight(_ height: CGFloat) -> Self {
        return mutateStyle { style in
            CellStyle(
                titleColor: style.titleColor,
                titleFont: style.titleFont,
                descriptionColor: style.descriptionColor,
                descriptionFont: style.descriptionFont,
                valueTextColor: style.valueTextColor,
                valueTextFont: style.valueTextFont,
                iconSize: style.iconSize,
                iconRadius: style.iconRadius,
                cellHeight: height,
                hintTextColor: style.hintTextColor,
                hintTextFont: style.hintTextFont,
                backgroundColor: style.backgroundColor,
                accentColor: style.accentColor
            )
        }
    }

    /// タイトル色を上書きする。
    public func titleColor(_ color: UIColor) -> Self {
        return mutateStyle { style in
            CellStyle(
                titleColor: color,
                titleFont: style.titleFont,
                descriptionColor: style.descriptionColor,
                descriptionFont: style.descriptionFont,
                valueTextColor: style.valueTextColor,
                valueTextFont: style.valueTextFont,
                iconSize: style.iconSize,
                iconRadius: style.iconRadius,
                cellHeight: style.cellHeight,
                hintTextColor: style.hintTextColor,
                hintTextFont: style.hintTextFont,
                backgroundColor: style.backgroundColor,
                accentColor: style.accentColor
            )
        }
    }

    /// Cell 個別の背景色を上書きする。
    public func backgroundColor(_ color: UIColor) -> Self {
        return mutateStyle { style in
            CellStyle(
                titleColor: style.titleColor,
                titleFont: style.titleFont,
                descriptionColor: style.descriptionColor,
                descriptionFont: style.descriptionFont,
                valueTextColor: style.valueTextColor,
                valueTextFont: style.valueTextFont,
                iconSize: style.iconSize,
                iconRadius: style.iconRadius,
                cellHeight: style.cellHeight,
                hintTextColor: style.hintTextColor,
                hintTextFont: style.hintTextFont,
                backgroundColor: color,
                accentColor: style.accentColor
            )
        }
    }

    /// Cell を無効化（タップ抑制等）するためのフラグ。
    /// 注: `CellStyle` に `disabled` フィールドが存在しないため、本提案の範囲では暫定 no-op。
    public func disabled(_ flag: Bool) -> Self {
        _ = flag
        return self
    }

    /// Cell のアイコンを上書きする。
    ///
    /// 受け取り型は UI 層 `KsImage`。`DSLIconModifiable` 準拠 Cell の場合は
    /// `withIcon(_:)` 経由で copy が生成される。
    /// 非準拠 Cell（アイコン領域を持たない `CustomCell`）の場合は no-op とする。
    public func icon(_ icon: KsImage) -> Self {
        if let modifiable = self as? any DSLIconModifiable {
            if let rebuilt = applyIconIfMatching(modifiable, newIcon: icon) as? Self {
                // 既存ヒントを新インスタンスに引き継ぐ。
                if let existing = DSLHintRegistry.shared.cellHint(for: self.id) {
                    DSLHintRegistry.shared.recordCellHint(
                        cellInstanceID: rebuilt.id,
                        hint: existing
                    )
                }
                return rebuilt
            }
        }
        return self
    }

    /// Cell の明示 ID を指定する（動的構造での同一性安定化用）。
    /// - Parameter id: 任意の `AnyHashable`
    /// - Returns: 自身を返す（ヒントはサイドチャンネル レジストリに記録）
    public func cellID(_ id: AnyHashable) -> Self {
        DSLHintRegistry.shared.recordCellHint(
            cellInstanceID: self.id,
            hint: .explicit(id)
        )
        return self
    }

    // MARK: - 内部ヘルパ

    /// 各具象 Cell が UI 層 `DSLStyleModifiable` 準拠なら `withStyle(_:)` を呼ぶ。
    /// 未準拠なら自身を返す（DEBUG ビルドで警告のみで no-op）。
    ///
    /// `DSLStyleModifiable` プロトコルが `var style: CellStyle { get }` を要求するため、
    /// existential 経由で `style` / `withStyle(_:)` 両方を呼べる（具象型へのダウンキャストは不要）。
    fileprivate func mutateStyle(_ transform: (CellStyle) -> CellStyle) -> Self {
        if let modifiable = self as? any DSLStyleModifiable {
            let currentStyle = modifiable.style
            let newStyle = transform(currentStyle)
            if let rebuilt = applyStyleIfMatching(modifiable, newStyle: newStyle) as? Self {
                // 既存ヒントを新インスタンスに引き継ぐ。
                if let existing = DSLHintRegistry.shared.cellHint(for: self.id) {
                    DSLHintRegistry.shared.recordCellHint(
                        cellInstanceID: rebuilt.id,
                        hint: existing
                    )
                }
                return rebuilt
            }
        }
        return self
    }
}

/// `any DSLStyleModifiable` を具象 `Self` に解決して `withStyle(_:)` を呼ぶグローバルヘルパ。
fileprivate func applyStyleIfMatching<T: DSLStyleModifiable>(
    _ cell: T,
    newStyle: CellStyle
) -> any KsCell {
    return cell.withStyle(newStyle)
}

/// `any DSLIconModifiable` を具象 `Self` に解決して `withIcon(_:)` を呼ぶグローバルヘルパ。
fileprivate func applyIconIfMatching<T: DSLIconModifiable>(
    _ cell: T,
    newIcon: KsImage?
) -> any KsCell {
    return cell.withIcon(newIcon)
}
#endif
