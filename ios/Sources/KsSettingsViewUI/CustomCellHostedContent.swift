// CustomCellHostedContent.swift
// KsSettingsViewUI
//
// `CustomCell` の builder 出力を `UIHostingConfiguration` に載せるための合成ルート View。
//   - builder 出力を行全体（accessory 領域を除く full-bleed）に広げる
//   - `showArrow == true` のとき trailing に chevron を合成する
//   - `isEnabled == false` のとき content 全体の操作を抑止し、content を淡色化する
//   - 行の中での縦位置を `CustomCellRowPlacement` に委ねる
//
// 決定: core/ADR-0022（共通行レイアウトの適用除外）。

#if canImport(UIKit)
import SwiftUI
import UIKit

/// `CustomCell` の hosted content ルート。
///
/// `UIHostingConfiguration` は `contentView` 全体を占有するため、chevron は
/// UIKit の accessory 経路ではなく本 View の内側で合成する。
internal struct CustomCellHostedContent: View {
    /// 型消去済みの builder 出力。
    let content: AnyView
    /// Disclosure Indicator を表示するか。
    let showArrow: Bool
    /// 有効フラグ。`false` のとき content 内の操作を抑止する。
    let isEnabled: Bool
    /// 実効行高さ（pt）。行の中での縦位置の基準として `CustomCellRowPlacement` に渡す。
    let effectiveCellHeight: CGFloat

    var body: some View {
        CustomCellRowPlacement(effectiveCellHeight: effectiveCellHeight) {
            HStack(spacing: 0) {
                // builder 出力は残り幅いっぱいを占有する（先頭揃え）。
                // 淡色化の対象は content だけであり、行の背景と chevron には掛けない。
                content
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .opacity(isEnabled ? 1 : Self.disabledContentOpacity)

                if showArrow {
                    chevron
                }
            }
        }
        // 無効時は content 内の操作可能要素（Button / Slider 等）を操作不能にする。
        // 視覚状態契約「無効 Cell は操作 callback と内包 control の操作を抑止する」に従う。
        // VoiceOver の読み上げは残る（Android の TalkBack とは意図的な非対称 — core/ADR-0017）。
        .disabled(!isEnabled)
    }

    /// `isEnabled == false` のときに content へ掛ける不透明度。
    ///
    /// 共通行レイアウトの Cell はテキスト色を `Theme.disabledTextColor`（既定 `#999999`）へ
    /// 置換して無効を表すが、任意の View である content には色の置換を適用できない。白背景上で
    /// `#999999` 相当の濃度になる値を選び、標準 Cell と並んだときの見え方を揃える。
    ///
    /// `.disabled(true)` は環境値 `isEnabled` を読む標準コントロールしか淡色化しないため、
    /// `Text` / `Image` を含む content 全体を薄くするにはこの不透明度を別途掛ける必要がある。
    /// 標準コントロールは両者が重なって二重に薄くなるが、content 全体が一様に「無効に見える」
    /// ことを優先する。
    private static let disabledContentOpacity: Double = 0.38

    /// 標準 Cell（`makeChevronView()`）と同一アセット・寸法・末端余白の chevron。
    private var chevron: some View {
        Image(systemName: KsChevronAppearance.symbolName)
            .font(Font(UIFont.preferredFont(forTextStyle: KsChevronAppearance.textStyle)))
            .imageScale(KsChevronAppearance.swiftUIImageScale)
            .foregroundStyle(Color(uiColor: KsChevronAppearance.tintColor))
            .padding(.trailing, KsChevronAppearance.trailingMargin)
    }
}
#endif
