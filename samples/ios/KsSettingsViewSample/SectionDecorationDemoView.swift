// SectionDecorationDemoView.swift
// KsSettingsViewSample
//
// style（Classic / Modern）と Theme の Section 装飾 4 属性を実行時に切り替えて、
// 同じ設定内容が装飾だけ変わって描かれることを目視確認するデモ画面。
//
// Modern で確認できること:
//   - Section の Cell 行だけを角丸の箱が覆い、Header / Footer は箱の外側に置かれる
//   - separator は箱の中間だけに引かれ、箱の上下端には出ない（単一 Cell の Section には出ない）
//   - プリセット切替で箱の余白・角丸・ボーダーが変わる
//
// Classic では箱を描かないため、プリセットの差は Section 間の上下余白にだけ現れる。
//
// 装飾値は SettingsView 全体の Theme が持つため、ボーダーの有無は画面内の全 Section に
// 一括で効く（Section 単位の上書きは公開していない）。
//
// 対応する Android 側定義:
// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SectionDecorationDemoScreen.kt

import SwiftUI
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI

/// style 切替と Section 装飾プリセット切替を観察するデモ画面。
struct SectionDecorationDemoView: View {
    @State private var style: KsSettingsViewStyle = .modern
    @State private var preset: SectionDecorationPreset = .standard

    @State private var airplaneMode: Bool = false
    @State private var autoAppearance: Bool = true
    @State private var trueTone: Bool = true

    /// 実効外観。ダークのとき Theme の dark 側を渡す。
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(spacing: 0) {
            SectionDecorationDemoControls(style: $style, preset: $preset)

            KsSettingsView(style: style) {
                // 1. Header / Footer なし・icon 付き Cell の Section
                Section {
                    SwitchCell(
                        title: "機内モード",
                        icon: KsImage.uiImage(SampleIconBadge.airplane),
                        isOn: airplaneMode,
                        onValueChanged: { newValue in airplaneMode = newValue }
                    )
                    CommandCell(
                        title: "Wi-Fi",
                        valueText: "demoAP-0a1b2c-5",
                        icon: KsImage.uiImage(SampleIconBadge.wifi)
                    )
                    CommandCell(
                        title: "Bluetooth",
                        valueText: "オン",
                        icon: KsImage.uiImage(SampleIconBadge.bluetooth)
                    )
                    CommandCell(
                        title: "バッテリー",
                        icon: KsImage.uiImage(SampleIconBadge.battery)
                    )
                }

                // 2. Header / Footer 付きの Section（どちらも箱の外側に置かれる）
                Section(
                    "外観モード",
                    footer: "好みに応じて外観モードを選択できます。Header と Footer は箱の外側に配置されます。"
                ) {
                    SwitchCell(
                        title: "自動",
                        isOn: autoAppearance,
                        onValueChanged: { newValue in autoAppearance = newValue }
                    )
                    CommandCell(title: "テキストサイズを変更")
                }

                // 3. 単一 Cell の Section（separator は引かれない）
                Section {
                    SwitchCell(
                        title: "True Tone",
                        isOn: trueTone,
                        onValueChanged: { newValue in trueTone = newValue }
                    )
                }

                // 4. ボーダー指定の観察用 Section
                Section(
                    "ボーダー指定時の例",
                    footer: "既定はボーダーなし (width 0)。指定時のみ枠線が箱の輪郭に描かれます。"
                ) {
                    LabelCell(title: "sectionBorderWidth: 2")
                    LabelCell(title: "sectionBorderColor: gray")
                }
            }
            .theme(preset.theme(dark: colorScheme == .dark))
        }
        .navigationTitle(SampleScreen.sectionDecoration.title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

#if DEBUG
#Preview {
    NavigationStack {
        SectionDecorationDemoView()
    }
}
#endif
