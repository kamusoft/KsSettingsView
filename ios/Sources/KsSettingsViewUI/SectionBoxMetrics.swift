// SectionBoxMetrics.swift
// KsSettingsViewUI
//
// `Theme` の Section 装飾 4 属性（`sectionMargin` / `sectionCornerRadius` /
// `sectionBorderWidth` / `sectionBorderColor`）を style ごとの実効値へ解決した結果。
//
// `nil` は「未指定」を表し、style ごとの既定へ解決する。負の寸法と非有限（NaN・±∞）の寸法は
// 0 に正規化し、描画側が不正な geometry を受け取らないようにする。

#if canImport(UIKit)
import UIKit

/// Section 装飾 4 属性の実効値。
///
/// `.modern` では Section の Cell 行を覆う箱の余白・角丸・ボーダーを表し、
/// `.classic` では `margin` の上下成分のみが意味を持つ（箱を描かないため角丸・ボーダーは常に 0）。
internal struct SectionBoxMetrics {
    /// Section 単位（Header・箱・Footer 一体）の外側余白。負・非有限の成分は 0 へ正規化済み。
    let margin: NSDirectionalEdgeInsets
    /// 箱の角丸半径。`.classic` では常に 0。
    let cornerRadius: CGFloat
    /// 箱のボーダー幅。`.classic` では常に 0。
    let borderWidth: CGFloat
    /// 箱のボーダー色。`.classic` では常に透明。
    let borderColor: UIColor

    // MARK: - style ごとの既定値

    /// `.modern` の既定余白。生値は両 platform で統一する（core/ADR-0027。見え方の所有は ios/ADR-0003）。
    static let modernDefaultMargin = NSDirectionalEdgeInsets(top: 22, leading: 16, bottom: 0, trailing: 16)
    /// `.modern` の既定角丸半径（両 platform で生値 26 に統一。core/ADR-0024）。
    static let modernDefaultCornerRadius: CGFloat = 26
    /// `.classic` の既定余白（`modernDefaultMargin` と同値。core/ADR-0027）。
    ///
    /// 上下成分は Classic でもそのまま効き、水平成分は `resolve` が `.classic` で常に 0 に
    /// 落とす（Section 境界を全幅に保つ）ため無視される。既定値を対称に保つことで
    /// 「水平は Classic では無視される」という仕様を値の側でも示す。
    /// Android 側の `CLASSIC_DEFAULT_MARGIN = MODERN_DEFAULT_MARGIN` と同じ別名方式に揃える。
    static let classicDefaultMargin = modernDefaultMargin
    /// ボーダー幅の既定（既定の Modern にボーダーは描かない）。
    static let defaultBorderWidth: CGFloat = 0
    /// ボーダー色の既定（透明）。
    static let defaultBorderColor: UIColor = .clear

    /// 寸法値を描画へ渡せる形に正規化する。
    ///
    /// 非有限（NaN・±∞）の値は 0 として扱い、負の値も 0 へ引き上げる。
    /// `Theme` は構築時に値を検査しないため、描画へ渡る直前のここが唯一の防波堤になる。
    private static func normalized(_ value: CGFloat) -> CGFloat {
        guard value.isFinite else { return 0 }
        return max(0, value)
    }

    /// `Theme` と style から実効値を解決する。
    ///
    /// - `nil` の属性は style ごとの既定へ解決する。
    /// - 負および非有限（NaN・±∞）の余白成分・ボーダー幅・角丸半径は 0 として扱う。
    /// - `.classic` は箱を描かないため角丸・ボーダーを 0 / 透明に落とし、
    ///   余白の leading / trailing 成分も 0 にする（Section 境界を全幅に保つ）。
    static func resolve(theme: Theme, style: KsSettingsViewStyle) -> SectionBoxMetrics {
        let isModern = (style == .modern)
        let rawMargin = theme.sectionMargin ?? (isModern ? modernDefaultMargin : classicDefaultMargin)
        let margin = NSDirectionalEdgeInsets(
            top: normalized(rawMargin.top),
            leading: isModern ? normalized(rawMargin.leading) : 0,
            bottom: normalized(rawMargin.bottom),
            trailing: isModern ? normalized(rawMargin.trailing) : 0
        )
        guard isModern else {
            return SectionBoxMetrics(
                margin: margin,
                cornerRadius: 0,
                borderWidth: 0,
                borderColor: defaultBorderColor
            )
        }
        return SectionBoxMetrics(
            margin: margin,
            cornerRadius: normalized(theme.sectionCornerRadius ?? modernDefaultCornerRadius),
            borderWidth: normalized(theme.sectionBorderWidth ?? defaultBorderWidth),
            borderColor: theme.sectionBorderColor ?? defaultBorderColor
        )
    }

    /// 箱の寸法に対して幾何的に許される角丸半径へ clamp する。
    ///
    /// 半径が箱の短辺の半分を超えると描画結果が破綻するため、描画の直前にここで抑える。
    /// 箱の decoration も Cell 側の clip も、角丸半径の上限はこの 1 か所で決める。
    static func clampedCornerRadius(_ cornerRadius: CGFloat, for size: CGSize) -> CGFloat {
        let limit = min(size.width, size.height) / 2
        guard limit > 0 else { return 0 }
        return min(max(0, cornerRadius), limit)
    }

    /// 解決済みの角丸半径を箱の寸法へ clamp する。
    func clampedCornerRadius(for size: CGSize) -> CGFloat {
        return Self.clampedCornerRadius(cornerRadius, for: size)
    }
}

// MARK: - Equatable

extension SectionBoxMetrics: Equatable {
    /// `UIColor` は Swift の `Equatable` に準拠しないため、色だけ `isEqual(_:)` で判定する。
    static func == (lhs: SectionBoxMetrics, rhs: SectionBoxMetrics) -> Bool {
        return lhs.margin == rhs.margin
            && lhs.cornerRadius == rhs.cornerRadius
            && lhs.borderWidth == rhs.borderWidth
            && uiColorEqual(lhs.borderColor, rhs.borderColor)
    }
}
#endif
