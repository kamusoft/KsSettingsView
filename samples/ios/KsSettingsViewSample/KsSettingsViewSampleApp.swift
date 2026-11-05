// KsSettingsViewSampleApp.swift
// KsSettingsViewSample
//
// SwiftUI App ライフサイクルのエントリポイント。
// `@main` で起動し、`WindowGroup` から `ContentView` を表示する。

import SwiftUI

/// KsSettingsView Sample アプリのエントリポイント。
///
/// `KsSettingsViewController.init` のデフォルトで `registerBasicCells()` が
/// 自動呼び出しされるため、Sample 側で明示的な Cell 登録は不要。
@main
struct KsSettingsViewSampleApp: App {
    var body: some Scene {
        WindowGroup {
            // 起動引数 `--showcase <画面>` があれば撮影用の画面だけを表示する (ShowcaseScreen.swift)。
            if let showcase = ShowcaseScreen.launchTarget {
                NavigationStack {
                    showcase.destination
                }
            } else {
                ContentView()
            }
        }
    }
}
