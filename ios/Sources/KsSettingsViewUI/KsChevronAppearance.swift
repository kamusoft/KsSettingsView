// KsChevronAppearance.swift
// KsSettingsViewUI
//
// Disclosure Indicator（chevron）の見た目を決める定数群。
//
// UIKit 経路（`makeChevronView()` → `KsListCellBase.accessoryHolder`）と
// 宣言 UI 経路（`CustomCellHostedContent` の `UIHostingConfiguration` 内合成）の
// 双方が本定数を参照することで、CustomCell の chevron が標準 Cell（CommandCell 等）の
// chevron とアセット・寸法・末端余白で一致することを構造的に担保する。
//
// 決定: core/ADR-0022
//   「既存 Cell と見た目を揃える装飾は hosted 宣言 UI 内で同一アセット・同一寸法定数を共有して合成する」。

#if canImport(UIKit)
import SwiftUI
import UIKit

/// chevron（Disclosure Indicator）の共有アピアランス定数。
internal enum KsChevronAppearance {
    /// SF Symbol 名。
    static let symbolName: String = "chevron.right"

    /// シンボルのベースとなるテキストスタイル（`UIImage.SymbolConfiguration(font:scale:)` の font 側）。
    static let textStyle: UIFont.TextStyle = .body

    /// シンボルスケール（UIKit 経路）。
    static let symbolScale: UIImage.SymbolScale = .small

    /// シンボルスケール（SwiftUI 経路）。`symbolScale` と対になる値を返す。
    static var swiftUIImageScale: Image.Scale {
        switch symbolScale {
        case .small: return .small
        case .medium: return .medium
        case .large: return .large
        default: return .medium
        }
    }

    /// 描画色。標準 Cell の `UIImageView.tintColor` と同値。
    static var tintColor: UIColor { .tertiaryLabel }

    /// 行の右端から chevron までの余白（pt）。
    ///
    /// UIKit 経路では `KsListCellBase.stackH.layoutMargins.right`（16pt）が同じ役割を果たす。
    /// 宣言 UI 内合成（full-bleed で行マージンを持たない CustomCell）では、この定数を
    /// chevron の trailing padding として明示的に適用することで同じ位置に揃える。
    static let trailingMargin: CGFloat = 16
}
#endif
