// ShowcaseSettingsView.swift
// KsSettingsViewSample
//
// スクリーンショット撮影用の「実アプリの設定画面」風の見本 (ShowcaseScreen.swift 参照)。
//
// 架空のノートアプリの設定画面として、アカウント行・アイコン付きの Switch / Picker /
// TimePicker・Header / Footer・ButtonCell を 1 画面に収める。スライドでは画面の上半分ほどが
// 見えるため、見せたい Cell 種別ほど上に置く。
//
// 色は Theme をほぼ既定のまま使い、OS のライト / ダークにそのまま追随させる
// (撮影時は simctl の appearance で切り替える)。

import SwiftUI
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI

/// 撮影用の設定画面。style だけを外から受け取り、Modern / Classic で同じ中身を描く。
///
/// Cell の通知は `@Sendable` の閉包で受け取るが、呼ばれるのは常にメインスレッドなので、
/// `MainActor.assumeIsolated` の中で `@State` を書き換える。
struct ShowcaseSettingsView: View {
    let style: KsSettingsViewStyle

    @State private var notificationOn: Bool = true
    @State private var reminderTime: Date = Self.makeTime(hour: 21, minute: 30)
    @State private var soundIndex: Int? = 1
    @State private var themeIndex: Int? = 2
    @State private var largeText: Bool = false
    @State private var syncOn: Bool = true

    private let sounds: [String] = ["ベル", "チャイム", "そよ風", "なし"]
    private let themes: [String] = ["ライト", "ダーク", "自動"]

    var body: some View {
        KsSettingsView(style: style) {
            // 1. アカウント (大きめの丸アバター + 2 行)
            Section {
                CommandCell(
                    style: CellStyle(
                        iconSize: ShowcaseIcon.avatarSize,
                        iconRadius: ShowcaseIcon.avatarSize / 2,
                        cellHeight: 84
                    ),
                    title: "山田 花子",
                    description: "プレミアムプラン・3 台の端末で同期中",
                    icon: KsImage.uiImage(ShowcaseIcon.avatar)
                )
            }

            // 2. 通知 (Switch / TimePicker / Picker)
            Section("通知") {
                SwitchCell(
                    title: "通知",
                    icon: KsImage.uiImage(ShowcaseIcon.bell),
                    isOn: notificationOn,
                    onValueChanged: { newValue in MainActor.assumeIsolated { notificationOn = newValue } }
                )
                TimePickerCell(
                    title: "毎日のリマインダー",
                    icon: KsImage.uiImage(ShowcaseIcon.alarm),
                    time: $reminderTime,
                    pickerTitle: "リマインダーの時刻"
                )
                PickerCell(
                    title: "サウンド",
                    icon: KsImage.uiImage(ShowcaseIcon.sound),
                    items: sounds,
                    selectedIndex: $soundIndex,
                    pageTitle: "サウンド"
                )
            }

            // 3. 表示
            Section("表示") {
                PickerCell(
                    title: "テーマ",
                    icon: KsImage.uiImage(ShowcaseIcon.palette),
                    items: themes,
                    selectedIndex: $themeIndex,
                    pageTitle: "テーマ"
                )
                SwitchCell(
                    title: "大きな文字",
                    icon: KsImage.uiImage(ShowcaseIcon.textSize),
                    isOn: largeText,
                    onValueChanged: { newValue in MainActor.assumeIsolated { largeText = newValue } }
                )
            }

            // 4. データ (Footer 付き)
            Section("データ", footer: "最終バックアップ: 今日 9:30") {
                SwitchCell(
                    title: "クラウド同期",
                    icon: KsImage.uiImage(ShowcaseIcon.cloud),
                    isOn: syncOn,
                    onValueChanged: { newValue in MainActor.assumeIsolated { syncOn = newValue } }
                )
                LabelCell(
                    title: "使用容量",
                    valueText: "1.2 GB",
                    icon: KsImage.uiImage(ShowcaseIcon.storage)
                )
                CommandCell(
                    title: "ノートを書き出す",
                    icon: KsImage.uiImage(ShowcaseIcon.export)
                )
            }

            // 5. ログアウト
            Section {
                ButtonCell(
                    title: "ログアウト",
                    titleColor: .systemRed
                )
            }
        }
        .theme(ShowcaseIcon.theme)
        .navigationTitle("設定")
        .navigationBarTitleDisplayMode(.large)
    }

    /// 時と分だけを持つ `Date` を作る。
    private static func makeTime(hour: Int, minute: Int) -> Date {
        Calendar.current.date(bySettingHour: hour, minute: minute, second: 0, of: Date()) ?? Date()
    }
}

/// 撮影用画面のアイコンと Theme。
///
/// `KsImage.uiImage` の等価判定は参照同一性のため、画像は static let で 1 度だけ作る
/// (SampleIconBadge.swift と同じ理由)。
@MainActor
enum ShowcaseIcon {
    /// アカウント行のアバターの直径 (pt)。
    nonisolated static let avatarSize: CGFloat = 56

    static let bell = SampleIconBadge.make(systemName: "bell.fill", background: .systemRed)
    static let alarm = SampleIconBadge.make(systemName: "alarm.fill", background: .systemOrange)
    static let sound = SampleIconBadge.make(systemName: "speaker.wave.2.fill", background: .systemPink)
    static let palette = SampleIconBadge.make(systemName: "paintpalette.fill", background: .systemPurple)
    static let textSize = SampleIconBadge.make(systemName: "textformat.size", background: .systemBlue)
    static let cloud = SampleIconBadge.make(systemName: "icloud.fill", background: .systemTeal)
    static let storage = SampleIconBadge.make(systemName: "internaldrive.fill", background: .systemGray)
    static let export = SampleIconBadge.make(systemName: "square.and.arrow.up.fill", background: .systemGreen)

    /// アカウント行のアバター。斜めのグラデーションの円に頭文字を白で載せる。
    static let avatar: UIImage = {
        let canvas = CGSize(width: avatarSize, height: avatarSize)
        return UIGraphicsImageRenderer(size: canvas).image { context in
            let colors = [
                UIColor(red: 0xF3/255.0, green: 0x9A/255.0, blue: 0x2E/255.0, alpha: 1).cgColor,
                UIColor(red: 0xD0/255.0, green: 0x61/255.0, blue: 0x0F/255.0, alpha: 1).cgColor
            ] as CFArray
            if let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors, locations: [0, 1]) {
                context.cgContext.drawLinearGradient(
                    gradient,
                    start: .zero,
                    end: CGPoint(x: canvas.width, y: canvas.height),
                    options: []
                )
            }
            let text = "花" as NSString
            let attributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 24, weight: .semibold),
                .foregroundColor: UIColor.white
            ]
            let size = text.size(withAttributes: attributes)
            text.draw(
                at: CGPoint(x: (canvas.width - size.width) / 2, y: (canvas.height - size.height) / 2),
                withAttributes: attributes
            )
        }
    }()

    /// 撮影用 Theme。色は既定 (OS の外観に追随) のまま、アクセントとアイコン寸法だけを決める。
    static let theme = Theme(
        cellAccentColor: .systemGreen,
        cellIconSize: SampleIconBadge.size,
        cellIconRadius: SampleIconBadge.cornerRadius
    )
}

#if DEBUG
#Preview("Modern") {
    NavigationStack {
        ShowcaseSettingsView(style: .modern)
    }
}

#Preview("Classic") {
    NavigationStack {
        ShowcaseSettingsView(style: .classic)
    }
}
#endif
