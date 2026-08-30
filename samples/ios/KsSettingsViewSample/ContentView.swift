// ContentView.swift
// KsSettingsViewSample
//
// Sample アプリの起動直後に表示するルートメニュー。
//
// 「デモ」グループ（ライブラリの使い方を示す画面）と「検証」グループ
// （プラットフォーム固有の技術検証画面）を List の Section で分けて表示する。
// 項目文言は SampleScreen.title を参照する（遷移先のタイトルと同一定義）。

import SwiftUI

/// 起動直後のルートメニュー。デモ群と検証群をグループ分けして表示する。
struct ContentView: View {
    var body: some View {
        NavigationStack {
            List {
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
    }
}

#if DEBUG
#Preview {
    ContentView()
}
#endif
