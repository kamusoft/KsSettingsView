// DSLDemoView.swift
// KsSettingsViewSample
//
// 宣言的 DSL（`KsSettingsView { Section { Cell... } }`）の動作確認画面。

import SwiftUI
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI

/// 宣言的 DSL のデモ画面。
///
/// - 静的構成（Section / Cell）
/// - `ForEach` による動的構成
/// - Cell modifier の適用
/// - Root H/F の `.rootHeader(...)` / `.rootFooter(...)` modifier
struct DSLDemoView: View {

    @State private var items: [DemoItem] = [
        DemoItem(id: 1, name: "Item A"),
        DemoItem(id: 2, name: "Item B"),
        DemoItem(id: 3, name: "Item C"),
    ]
    @State private var nextID: Int = 4

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button("項目追加") {
                    items.append(DemoItem(id: nextID, name: "Item \(nextID)"))
                    nextID += 1
                }
                .buttonStyle(.borderedProminent)

                Button("末尾削除") {
                    if !items.isEmpty {
                        items.removeLast()
                    }
                }
                .buttonStyle(.bordered)
            }
            .padding()

            KsSettingsView {
                // 静的 Section
                Section("静的 Section") {
                    LabelCell(title: "固定 Cell A")
                    LabelCell(title: "固定 Cell B")
                }
                .sectionFooter("Section H/F は modifier で指定")

                // 動的 Section（ForEach で繰り返し生成）
                // 見出しは platform API 名に依存しない中立文言にする（iOS / Android 共通）。
                Section("動的 Section（繰り返し）") {
                    ForEach(items) { item in
                        LabelCell(title: item.name)
                    }
                }

                // Cell modifier の例
                Section("Cell Modifier") {
                    LabelCell(title: "Cellは Modifier で装飾できる")
                        .cellHeight(80)
                }
            }
            .rootHeader("DSL 方式のデモ画面")
            .rootFooter { Text("© 2026 KsSettingsView Sample").font(.caption) }
            .ignoresSafeArea(.container, edges: .bottom)
        }
        .navigationTitle(SampleScreen.dsl.title)
    }
}

/// デモ用のサンプル ITEM。
struct DemoItem: Identifiable {
    let id: Int
    let name: String
}

#if DEBUG
#Preview {
    NavigationStack {
        DSLDemoView()
    }
}
#endif
