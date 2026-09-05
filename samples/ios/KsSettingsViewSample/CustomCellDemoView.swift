// CustomCellDemoView.swift
// KsSettingsViewSample
//
// `CustomCell`（任意の SwiftUI View を行全体に描画する Cell）を 5 構成で目視確認できるデモ画面。
//
//   ① インライン        — content + builder を DSL に直書き / content 省略の静的糖衣
//   ② 再利用            — CustomCell を返すラップ関数（SampleSliderCell.swift）
//   ③ 動的高さ          — content 内の操作で展開/折りたたみし、行高さが追従する
//   ④ showArrow / onTap — Disclosure Indicator と行タップ（子要素タップとの二重発火なし）
//   ⑤ スクロール耐性    — 同型の CustomCell 40 行。行の再利用で表示・listener が混線しないこと
//
// 対応する Android 側定義: samples/android/.../CustomCellDemoScreen.kt
//   （文言・セクション構成・パラメータは Android と一字一句揃える）

import SwiftUI
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI

// MARK: - content 値

/// ①「同期ステータス」行の content。
struct SampleSyncState: Hashable {
    let isOk: Bool
}

/// ③「動的高さ」行の content。
///
/// 展開状態を content に含めることで、トグル時に等価性が崩れて行が再バインドされ、
/// 新しい高さで再計測される（これが「高さの自動追従」の実体）。
struct SampleExpanderState: Hashable {
    let title: String
    let body: String
    let isExpanded: Bool
}

/// ④「詳細設定」行の content。
struct SampleTitledContent: Hashable {
    let title: String
    let subtitle: String
}

/// ④「行タップカウンタ」行の content。
struct SampleTapCounter: Hashable {
    let count: Int
}

/// ⑤「スクロール耐性」ダミー行の content。
struct SampleDummyItem: Hashable, Identifiable {
    let index: Int
    let isTapped: Bool

    var id: Int { index }
}

// MARK: - デモ画面

/// `CustomCell` の 5 構成を 1 画面に並べたデモ。
struct CustomCellDemoView: View {

    // ② SliderCell ラップ関数に渡す値
    @State private var brightness: Int = 70
    @State private var volume: Int = 40
    @State private var disabledValue: Int = 60

    // ③ 動的高さ
    @State private var isTermsExpanded: Bool = false
    @State private var isPrivacyExpanded: Bool = true

    // ④ 行タップ
    @State private var rowTapCount: Int = 0

    // ⑤ スクロール耐性（タップ済みのダミー行）
    @State private var tappedDummyIndices: Set<Int> = []

    /// 実効外観。ダークのとき Theme の dark 側を渡す。
    @Environment(\.colorScheme) private var colorScheme

    /// ⑤ のダミー行データ。アクセント 6 色を循環させる。
    private var dummyItems: [SampleDummyItem] {
        (1...40).map { index in
            SampleDummyItem(index: index, isTapped: tappedDummyIndices.contains(index))
        }
    }

    var body: some View {
        KsSettingsView {
            // ① インライン CustomCell
            Section(
                "インライン CustomCell",
                footer: "content + builder を DSL に直書きした例と、content 省略の静的糖衣の例。"
            ) {
                // content + builder を直書きする形。
                CustomCell(content: SampleSyncState(isOk: true)) { state in
                    SampleAccentRow(
                        dotColor: SampleTheme.demoAccentGreen,
                        title: "同期ステータス",
                        subtitle: "content: SyncState(ok: \(state.isOk))"
                    ) {
                        SampleTagLabel(
                            text: "同期済み",
                            background: SampleTheme.mauiAccent,
                            foreground: SampleTheme.mauiCellBackground
                        )
                    }
                }

                // content を持たない静的糖衣の形（builder だけ）。
                CustomCell {
                    Text("content なしの静的 CustomCell（キャプション行）")
                        .font(.system(size: 13))
                        .foregroundStyle(Color(uiColor: SampleTheme.mauiFooterText))
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .frame(maxWidth: .infinity, alignment: .center)
                }
            }

            // ② 再利用（SliderCell ラップ関数）
            Section(
                "再利用（SliderCell ラップ関数）",
                footer: "SliderCell(label:value:) 関数が CustomCell を返す再利用例。"
            ) {
                SliderCell(label: "明るさ", value: brightness) { brightness = $0 }
                SliderCell(label: "音量", value: volume) { volume = $0 }
                // isEnabled = false の行。無効 Cell では content 内のスライダーのドラッグも抑止される。
                SliderCell(label: "無効", value: disabledValue, isEnabled: false) { disabledValue = $0 }
            }

            // ③ 動的高さ
            Section(
                "動的高さ",
                footer: "content 内の状態で展開/折りたたみ。行高さは self-sizing で追従する。"
            ) {
                CustomCell(
                    content: SampleExpanderState(
                        title: "利用規約（タップで展開）",
                        body: Self.policyBody,
                        isExpanded: isTermsExpanded
                    )
                ) { state in
                    SampleExpanderRow(state: state) { isTermsExpanded.toggle() }
                }

                CustomCell(
                    content: SampleExpanderState(
                        title: "プライバシーポリシー（展開中）",
                        body: Self.policyBody,
                        isExpanded: isPrivacyExpanded
                    )
                ) { state in
                    SampleExpanderRow(state: state) { isPrivacyExpanded.toggle() }
                }
            }

            // ④ showArrow / onTap
            Section(
                "showArrow / onTap",
                footer: "chevron は既存 CommandCell と同一素材・同一位置で表示される。"
            ) {
                // showArrow = true。行タップは下の行タップカウンタと同じカウンタを進める。
                CustomCell(
                    content: SampleTitledContent(
                        title: "詳細設定",
                        subtitle: "showArrow: true / onTap で遷移"
                    ),
                    showArrow: true,
                    onTap: { rowTapCount += 1 }
                ) { content in
                    SampleAccentRow(
                        dotColor: nil,
                        title: content.title,
                        subtitle: content.subtitle,
                        // chevron 側の余白は CustomCell が持つため、content 側は詰める。
                        trailingPadding: 8
                    ) {
                        EmptyView()
                    }
                }

                // chevron の見た目・位置を隣接比較するための既存 Cell（検証用の基準行）。
                CommandCell(title: "詳細設定（CommandCell）")

                // onTap のみ（矢印なし）。ピルは content 内の Button で、
                // タップしてもカウンタは進まず 0 に戻る（子要素の操作で行 onTap は発火しない）。
                CustomCell(
                    content: SampleTapCounter(count: rowTapCount),
                    onTap: { rowTapCount += 1 }
                ) { content in
                    SampleAccentRow(
                        dotColor: nil,
                        title: "行タップカウンタ",
                        subtitle: "onTap のみ（矢印なし）"
                    ) {
                        Button {
                            rowTapCount = 0
                        } label: {
                            SampleTagLabel(
                                text: "\(content.count) 回",
                                background: SampleTheme.demoPillBackground,
                                foreground: SampleTheme.mauiHeaderText
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            // ⑤ スクロール耐性（ダミー #01–#40）
            Section(
                "スクロール耐性（ダミー #01–#40）",
                footer: "十分なスクロール量を確保し、行の再利用（リサイクル）で表示・listener が混線しないことを確認する。"
            ) {
                ForEach(dummyItems) { item in
                    CustomCell(
                        content: item,
                        onTap: { toggleDummy(item.index) }
                    ) { content in
                        SampleAccentRow(
                            dotColor: SampleTheme.demoAccentPalette[
                                (content.index - 1) % SampleTheme.demoAccentPalette.count
                            ],
                            title: "ダミー行 #\(Self.paddedNumber(content.index))",
                            subtitle: "content: DummyItem(\(content.index))"
                        ) {
                            SampleTagLabel(
                                text: content.isTapped
                                    ? "#\(Self.paddedNumber(content.index)) ✓"
                                    : "#\(Self.paddedNumber(content.index))",
                                background: SampleTheme.demoPillBackground,
                                foreground: SampleTheme.mauiHeaderText
                            )
                        }
                    }
                }
            }
        }
        .theme(SampleTheme.maui(dark: colorScheme == .dark))
        .ignoresSafeArea(.container, edges: .bottom)
        .navigationTitle(SampleScreen.customCell.title)
    }

    /// ⑤ ダミー行のタップ状態をトグルする（listener の混線を目視するための印）。
    private func toggleDummy(_ index: Int) {
        if tappedDummyIndices.contains(index) {
            tappedDummyIndices.remove(index)
        } else {
            tappedDummyIndices.insert(index)
        }
    }

    /// 2 桁ゼロ埋めの連番文字列。
    private static func paddedNumber(_ value: Int) -> String {
        String(format: "%02d", value)
    }

    /// ③ の展開本文（2 行とも同じ本文を使う）。
    private static let policyBody: String =
        "本アプリはお客様の設定情報を端末内にのみ保存します。収集した情報を第三者に提供することはありません。"
        + "設定のバックアップを有効にした場合のみ、暗号化した上でクラウドに保存します。"
}

// MARK: - 行 View

/// 「ドット + タイトル/サブタイトル + trailing スロット」の共通行 View。
///
/// `CustomCell` は full-bleed（ライブラリ側が行の内側マージンを持たない）ため、
/// 標準 Cell と横位置を揃える 16pt の余白は content 側で明示的に持たせる。
struct SampleAccentRow<Trailing: View>: View {
    let dotColor: UIColor?
    let title: String
    let subtitle: String
    var trailingPadding: CGFloat = 16
    @ViewBuilder let trailing: () -> Trailing

    var body: some View {
        HStack(spacing: 0) {
            if let dotColor {
                Circle()
                    .fill(Color(uiColor: dotColor))
                    .frame(width: 12, height: 12)
                    .padding(.trailing, 12)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 16))
                    .foregroundStyle(Color(uiColor: SampleTheme.mauiDeepText))
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(uiColor: SampleTheme.mauiFooterText))
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            trailing()
        }
        .padding(.leading, 16)
        .padding(.trailing, trailingPadding)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// バッジ / ピル用の角丸ラベル。
struct SampleTagLabel: View {
    let text: String
    let background: UIColor
    let foreground: UIColor

    var body: some View {
        Text(text)
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(Color(uiColor: foreground))
            .padding(.horizontal, 10)
            .padding(.vertical, 3)
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(Color(uiColor: background))
            )
    }
}

/// ③「動的高さ」の展開/折りたたみ行。
///
/// 見出し部が content 内の Button であり、行の `onTap` は指定していない
/// （`onTap` 未指定の CustomCell は行タップ動作を持たない）。
struct SampleExpanderRow: View {
    let state: SampleExpanderState
    let onToggle: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button(action: onToggle) {
                HStack(spacing: 0) {
                    Text(state.isExpanded ? "▼" : "▶")
                        .font(.system(size: 13))
                        .foregroundStyle(Color(uiColor: SampleTheme.mauiAccent))
                        .padding(.trailing, 8)
                    Text(state.title)
                        .font(.system(size: 16))
                        .foregroundStyle(Color(uiColor: SampleTheme.mauiDeepText))
                    Spacer(minLength: 0)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if state.isExpanded {
                Text(state.body)
                    .font(.system(size: 13))
                    .foregroundStyle(Color(uiColor: SampleTheme.demoExpandText))
                    .lineSpacing(5)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(Color(uiColor: SampleTheme.demoExpandBackground))
                    )
                    .padding(.top, 8)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

#if DEBUG
#Preview {
    NavigationStack {
        CustomCellDemoView()
    }
}
#endif
