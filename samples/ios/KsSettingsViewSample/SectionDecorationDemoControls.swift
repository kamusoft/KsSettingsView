// SectionDecorationDemoControls.swift
// KsSettingsViewSample
//
// Section 装飾デモの操作部。style（Classic / Modern）と Section 装飾プリセットを選ぶ。
// SettingsView 自身の描画に影響を与えないよう、デモ本体の上に独立して置く。

import SwiftUI
import KsSettingsViewUI

/// style と Section 装飾プリセットの選択 UI。
struct SectionDecorationDemoControls: View {
    @Binding var style: KsSettingsViewStyle
    @Binding var preset: SectionDecorationPreset

    var body: some View {
        VStack(spacing: 8) {
            Picker("style", selection: $style) {
                Text("Classic").tag(KsSettingsViewStyle.classic)
                Text("Modern").tag(KsSettingsViewStyle.modern)
            }
            .pickerStyle(.segmented)

            HStack(spacing: 4) {
                Text("装飾プリセット")
                    .font(.subheadline)
                Picker("装飾プリセット", selection: $preset) {
                    ForEach(SectionDecorationPreset.allCases) { item in
                        Text(item.title).tag(item)
                    }
                }
                .pickerStyle(.menu)
                .labelsHidden()
                Spacer()
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
}
