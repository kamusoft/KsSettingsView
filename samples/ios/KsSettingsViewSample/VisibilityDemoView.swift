// VisibilityDemoView.swift
// KsSettingsViewSample
//
// `Section.isVisible` / `Cell.isVisible` の挙動を確認するサンプル。
// 末尾 / 中間それぞれの位置で Cell・Section を出し入れし、アニメーションを比較する。
// あわせて `Section.isHeaderVisible` / `Section.isFooterVisible` による
// Header / Footer 単位の出し入れも観察できる。

import SwiftUI
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI

/// 末尾 / 中間 × Cell / Section の 4 パターンと、Header / Footer 単位の visibility 切替を観察するデモ。
struct VisibilityDemoView: View {
    @State private var showTailCell: Bool = true
    @State private var showMiddleCell: Bool = true
    @State private var showTailSection: Bool = true
    @State private var showMiddleSection: Bool = true
    @State private var showHeader: Bool = true
    @State private var showFooter: Bool = true

    var body: some View {
        KsSettingsView {
            // Section 1: 制御用トグル群（常に表示）
            Section("基本設定") {
                SwitchCell(
                    title: "末尾セル表示",
                    description: "「観察対象 Section A」の末尾 Cell を出し入れ",
                    isOn: showTailCell,
                    onValueChanged: { v in showTailCell = v }
                )
                SwitchCell(
                    title: "中間セル表示",
                    description: "「観察対象 Section A」の中間 Cell を出し入れ",
                    isOn: showMiddleCell,
                    onValueChanged: { v in showMiddleCell = v }
                )
                SwitchCell(
                    title: "末尾セクション表示",
                    description: "末尾の Section C をまるごと出し入れ",
                    isOn: showTailSection,
                    onValueChanged: { v in showTailSection = v }
                )
                SwitchCell(
                    title: "中間セクション表示",
                    description: "中間の Section B をまるごと出し入れ",
                    isOn: showMiddleSection,
                    onValueChanged: { v in showMiddleSection = v }
                )
                SwitchCell(
                    title: "ヘッダー表示",
                    description: "「観察対象 Section D」の Header だけを出し入れ",
                    isOn: showHeader,
                    onValueChanged: { v in showHeader = v }
                )
                SwitchCell(
                    title: "フッター表示",
                    description: "「観察対象 Section D」の Footer だけを出し入れ",
                    isOn: showFooter,
                    onValueChanged: { v in showFooter = v }
                )
            }

            // Section 2 (A): Cell visibility 観察用
            //   - 1 行目は常に表示（基準）
            //   - 2 行目は「中間セル表示」トグルで visibility 切替
            //   - 3 行目は常に表示（中間 Cell の前後を可視化するためのアンカー）
            //   - 4 行目は「末尾セル表示」トグルで visibility 切替
            Section("観察対象 Section A（Cell 単位）") {
                LabelCell(title: "A-1: 常時表示")
                LabelCell(
                    title: "A-2: 中間セル",
                    description: "「中間セル表示」トグルで出し入れ",
                    isVisible: showMiddleCell
                )
                LabelCell(title: "A-3: 常時表示（中間 Cell のアンカー）")
                LabelCell(
                    title: "A-4: 末尾セル",
                    description: "「末尾セル表示」トグルで出し入れ",
                    isVisible: showTailCell
                )
            }

            // Section 3 (B): Section 単位（中間）の visibility 観察用
            //   「中間セクション表示」トグルで Section ごと出し入れ。
            //   前後 Section が押し下げ・押し上げされるスライドアニメを観察できる。
            Section(
                "観察対象 Section B（中間 Section）",
                footer: "Section 全体が非表示になります",
                isVisible: showMiddleSection
            ) {
                LabelCell(title: "B-1")
                LabelCell(title: "B-2")
                ButtonCell(title: "B-3 （Button）")
            }

            // Section 4 (Pivot): 中間 Section の出し入れを観察するためのアンカー。
            //   Section B の上下挙動を見るための「動かない隣」。
            Section("アンカー Section（中間 Section の隣）") {
                LabelCell(title: "P-1: 常時表示")
                LabelCell(title: "P-2: 常時表示")
            }

            // Section 5 (D): Header / Footer 単位の visibility 観察用
            //   「ヘッダー表示」「フッター表示」トグルで Header / Footer を独立に出し入れする。
            //   Cell の内容と accessory の文言はそのまま保持され、表示だけが切り替わる。
            Section(
                "観察対象 Section D（Header / Footer）",
                footer: "Header / Footer は内容を保持したまま隠れます",
                isHeaderVisible: showHeader,
                isFooterVisible: showFooter
            ) {
                LabelCell(title: "D-1: 常時表示")
                LabelCell(title: "D-2: 常時表示")
            }

            // Section 6 (C): Section 単位（末尾）の visibility 観察用
            //   「末尾セクション表示」トグルで Section ごと出し入れ。
            //   末尾なので fade のみ（前後の押し下げは発生しない）。
            Section(
                "観察対象 Section C（末尾 Section）",
                footer: "末尾なので fade アニメのみ",
                isVisible: showTailSection
            ) {
                LabelCell(title: "C-1")
                LabelCell(title: "C-2")
            }
        }
        .navigationTitle(SampleScreen.visibility.title)
    }
}

#if DEBUG
#Preview {
    NavigationStack {
        VisibilityDemoView()
    }
}
#endif
