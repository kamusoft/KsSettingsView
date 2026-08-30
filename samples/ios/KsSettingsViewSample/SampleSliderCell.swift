// SampleSliderCell.swift
// KsSettingsViewSample
//
// `CustomCell` を返すラップ関数の例（CustomCell デモ「再利用（SliderCell ラップ関数）」）。
//
// 独自の Cell 型を新設しなくても、CustomCell を返す関数を 1 つ用意すれば
// 「アプリ固有の Cell」を再利用単位として切り出せる、という利用パターンを示す。
// ラップ関数を別ファイルに置いてあるのは、その再利用性そのものを示すため。
//
// 対応する Android 側定義: samples/android/.../SampleSliderCell.kt

import SwiftUI
import UIKit
import KsSettingsViewUI

/// `SliderCell` の content 値。
///
/// 「表示に効く値はクロージャのキャプチャではなく content に含める」という CustomCell の
/// 利用者契約に従い、ラベルと値の両方を content に持たせる。
/// これにより値が変わった行だけが再バインドされる。
struct SampleSliderValue: Hashable {
    let label: String
    let value: Int
}

/// ラベル + スライダー + 数値の 1 行を `CustomCell` として組み立てる。
///
/// ```swift
/// Section("再利用（SliderCell ラップ関数）") {
///     SliderCell(label: "明るさ", value: brightness) { brightness = $0 }
/// }
/// ```
///
/// - Parameters:
///   - label: 行頭のラベル
///   - value: 0...100 の値。content に含まれるため、変化すると行が再バインドされる
///   - isEnabled: `false` で content 内部の操作（スライダーのドラッグ）が抑止される
///   - onValueChanged: ドラッグ確定時に呼ばれる。関数値は等価性に参加しない
/// - Returns: そのまま DSL に直書きできる `CustomCell`
func SliderCell(
    label: String,
    value: Int,
    isEnabled: Bool = true,
    onValueChanged: (@Sendable (Int) -> Void)? = nil
) -> CustomCell {
    CustomCell(
        content: SampleSliderValue(label: label, value: value),
        isEnabled: isEnabled
    ) { content in
        SampleSliderRow(content: content, onValueChanged: onValueChanged)
    }
}

/// `SliderCell` の行 View。
private struct SampleSliderRow: View {
    let content: SampleSliderValue
    let onValueChanged: (@Sendable (Int) -> Void)?

    /// ドラッグ追従用のローカル値。
    ///
    /// ドラッグのたびに content を差し替えると 1 フレームごとに再バインドが走るため、
    /// ドラッグ中はローカル state で追従し、確定時にだけ `onValueChanged` で外へ返す。
    @State private var draggingValue: Double

    init(content: SampleSliderValue, onValueChanged: (@Sendable (Int) -> Void)?) {
        self.content = content
        self.onValueChanged = onValueChanged
        self._draggingValue = State(initialValue: Double(content.value))
    }

    var body: some View {
        HStack(spacing: 12) {
            Text(content.label)
                .font(.system(size: 16))
                .foregroundStyle(Color(uiColor: SampleTheme.mauiDeepText))
                .frame(width: 64, alignment: .leading)

            Slider(value: $draggingValue, in: 0...100) { isEditing in
                if !isEditing {
                    onValueChanged?(Int(draggingValue))
                }
            }
            .tint(Color(uiColor: SampleTheme.mauiAccent))

            Text("\(Int(draggingValue))")
                .font(.system(size: 14))
                .foregroundStyle(Color(uiColor: SampleTheme.mauiFooterText))
                .frame(width: 40, alignment: .trailing)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
