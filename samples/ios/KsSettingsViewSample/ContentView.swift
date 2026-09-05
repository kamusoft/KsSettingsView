// ContentView.swift
// KsSettingsViewSample
//
// Sample アプリの起動直後に表示するルートメニュー。
//
// 先頭の「外観」グループでアプリ全体の外観を選び、続いて「デモ」グループ（ライブラリの
// 使い方を示す画面）と「検証」グループ（プラットフォーム固有の技術検証画面）を
// List の Section で分けて表示する。
// 項目文言は SampleAppearance.title / SampleScreen.title を参照する
// （遷移先のタイトルと同一定義）。

import SwiftUI

/// 起動直後のルートメニュー。外観の選択・デモ群・検証群をグループ分けして表示する。
struct ContentView: View {

    /// 外観の選択。`UserDefaults` に保存され、再起動後も維持される。
    @AppStorage(SampleAppearance.storageKey) private var appearance: SampleAppearance = .initial

    var body: some View {
        NavigationStack {
            List {
                SwiftUI.Section(SampleAppearance.sectionTitle) {
                    ForEach(SampleAppearance.allCases) { entry in
                        Button {
                            appearance = entry
                        } label: {
                            HStack {
                                Text(entry.title)
                                Spacer()
                                if entry == appearance {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(.tint)
                                        .accessibilityLabel(SampleAppearance.selectedAccessibilityLabel)
                                }
                            }
                            .contentShape(.rect)
                        }
                        // 項目名を List 行の既定色のまま残し、選択中の印だけを accent 色にする
                        // （既定の Button は label 全体を accent 色で描く）。
                        .buttonStyle(.plain)
                    }
                }

                SwiftUI.Section("デモ") {
                    ForEach(SampleScreen.demos) { screen in
                        NavigationLink(screen.title) { screen.destination }
                    }
                }

                SwiftUI.Section("検証") {
                    ForEach(SampleScreen.verifications) { screen in
                        NavigationLink(screen.title) { screen.destination }
                    }
                }
            }
            .navigationTitle("KsSettingsView Sample")
        }
        // 「システム」のときは上書きなし（端末の外観がそのまま効く）。
        .preferredColorScheme(appearance.colorScheme)
    }
}

#if DEBUG
#Preview {
    ContentView()
}
#endif
