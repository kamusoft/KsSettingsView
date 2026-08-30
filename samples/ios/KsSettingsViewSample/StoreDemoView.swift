// StoreDemoView.swift
// KsSettingsViewSample
//
// Store 方式（`SettingsRootStore` による部分更新）のデモ画面。
//
// 表示文言は Android `StoreDemoScreen.kt` と一字一句一致させる（cross/ADR-0016）。

import SwiftUI
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI

/// Store 方式のデモ画面。
///
/// `@StateObject var store: SettingsRootStore` で Store を保持し、
/// 「項目追加」「項目削除」ボタンで `store.insertCell(...)` / `store.removeCell(...)` を呼び、
/// 部分更新アニメーションを目視確認する。
struct StoreDemoView: View {
    private static let firstSectionID = UUID()

    @StateObject private var store: SettingsRootStore = SettingsRootStore(
        initialRoot: SettingsRoot {
            KsSettingsViewCore.Section(
                id: firstSectionID,
                header: SectionAccessory.text("PoC Section"),
                footer: SectionAccessory.text("This is a footer"),
                cells: [
                    LabelCell(title: "Sample Row 1"),
                    LabelCell(title: "Sample Row 2"),
                    LabelCell(title: "Sample Row 3"),
                ]
            )
        }
    )

    @State private var nextIndex: Int = 4

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button("項目追加") { addCell() }
                    .buttonStyle(.borderedProminent)

                Button("項目削除") { removeLastCell() }
                    .buttonStyle(.bordered)
            }
            .padding()

            KsSettingsView(store: store, style: .classic)
                .ignoresSafeArea(.container, edges: .bottom)
        }
        .navigationTitle(SampleScreen.store.title)
    }

    private func addCell() {
        guard let firstSection = store.root.sections.first else { return }
        let cell = LabelCell(title: "新規 \(nextIndex)")
        let endIndex = firstSection.cells.count
        store.insertCell(cell, in: firstSection.id, at: endIndex)
        nextIndex += 1
    }

    private func removeLastCell() {
        guard let firstSection = store.root.sections.first,
              let lastCell = firstSection.cells.last else { return }
        let cellID = KsCellID(cell: lastCell)
        store.removeCell(cellID: cellID)
    }
}

#if DEBUG
#Preview {
    NavigationStack {
        StoreDemoView()
    }
}
#endif
