// ShowcaseScreen.swift
// KsSettingsViewSample
//
// Web サイトのスライドや README に載せるスクリーンショットを撮るための画面一覧と、
// 起動引数からの直接表示の受け口。
//
// 撮影用の画面はライブラリの使い方を示すデモではなく「実アプリの設定画面に見える」ことを
// 優先した見本であり、プラットフォーム間のパリティ対象 (cross/ADR-0016) には数えない。
//
// 起動引数 `--showcase <rawValue>` を渡すと、ルートメニューを経由せずにその画面だけを表示する。
//   xcrun simctl launch <UDID> jp.kamusoft.kssettingsview.samples.ios --showcase settings-modern

import SwiftUI
import KsSettingsViewUI

/// 撮影用の画面。
enum ShowcaseScreen: String, CaseIterable, Identifiable {
    /// 設定画面 (Modern style)。
    case settingsModern = "settings-modern"
    /// 設定画面 (Classic style)。中身は Modern と同じで style だけが違う。
    case settingsClassic = "settings-classic"

    var id: String { rawValue }

    /// ルートメニューの「撮影用」グループに表示する名前。
    var title: String {
        switch self {
        case .settingsModern: "設定画面（Modern）"
        case .settingsClassic: "設定画面（Classic）"
        }
    }

    /// 画面が使う KsSettingsView の style。
    var style: KsSettingsViewStyle {
        switch self {
        case .settingsModern: .modern
        case .settingsClassic: .classic
        }
    }

    /// 撮影用の画面本体。ナビゲーションバー込みで返す。
    @MainActor
    var destination: some View {
        ShowcaseSettingsView(style: style)
    }

    /// 起動引数 `--showcase <rawValue>` で指定された画面。指定なし・定義外なら nil。
    static let launchTarget: ShowcaseScreen? = {
        let arguments = ProcessInfo.processInfo.arguments
        guard let keyIndex = arguments.firstIndex(of: "--showcase") else { return nil }
        let valueIndex = arguments.index(after: keyIndex)
        guard valueIndex < arguments.endIndex else { return nil }
        return ShowcaseScreen(rawValue: arguments[valueIndex])
    }()
}
