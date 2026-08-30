// UnifyCellCommonFieldsDemoView.swift
// KsSettingsViewSample
//
// Cell 共通行レイアウトが備える共通フィールド (`description` / `valueText` / `icon` /
// `hintText`、core/ADR-0011) と Radio/SimpleCheck の `accentColor` を視覚的に確認する
// ためのデモ画面。

import SwiftUI
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI

/// 7 種 Cell の共通フィールド（description / valueText / icon / hintText）と
/// Radio / SimpleCheck の accentColor を、組み合わせを変えながら一覧できるデモ画面。
struct UnifyCellCommonFieldsDemoView: View {

    @State private var switchOn1: Bool = true
    @State private var switchOn2: Bool = false
    @State private var checkbox1: Bool = false
    @State private var simpleCheck1: Bool = true
    @State private var simpleCheck2: Bool = false
    @State private var selectedTheme: String = "dark"

    var body: some View {
        KsSettingsView {
            // SwitchCell with icon + description + hintText
            Section("SwitchCell — 共通フィールド") {
                SwitchCell(
                    title: "通知",
                    description: "プッシュ通知を受信",
                    valueText: switchOn1 ? "オン" : "オフ",
                    icon: KsImage.systemName("bell"),
                    hintText: "推奨",
                    isOn: switchOn1,
                    onValueChanged: { switchOn1 = $0 }
                )
                SwitchCell(
                    title: "Wi-Fi のみ同期",
                    description: "従量回線では同期を停止",
                    icon: KsImage.systemName("wifi"),
                    hintText: "省データ",
                    isOn: switchOn2,
                    onValueChanged: { switchOn2 = $0 }
                )
            }

            // CheckboxCell with icon + description
            Section("CheckboxCell — 共通フィールド") {
                CheckboxCell(
                    title: "規約に同意",
                    description: "全文を読みました",
                    icon: KsImage.systemName("doc.text"),
                    isChecked: checkbox1,
                    onValueChanged: { checkbox1 = $0 }
                )
            }

            // RadioCell with accentColor
            Section("RadioCell — accentColor / description / icon / hintText") {
                RadioCell(
                    title: "ライト",
                    description: "明るい背景",
                    icon: KsImage.systemName("sun.max"),
                    groupId: "theme",
                    value: "light",
                    selectedValue: selectedTheme,
                    accentColor: SampleTheme.demoAccentOrange,
                    onSelected: { selectedTheme = $0 }
                )
                RadioCell(
                    title: "ダーク",
                    description: "暗い背景",
                    icon: KsImage.systemName("moon"),
                    // Radio + hintText の組み合わせをデモするため、ダークセルに「推奨」hintText を付ける（Android 実装に追随）
                    hintText: "推奨",
                    groupId: "theme",
                    value: "dark",
                    selectedValue: selectedTheme,
                    accentColor: SampleTheme.demoAccentPurple,
                    onSelected: { selectedTheme = $0 }
                )
                RadioCell(
                    title: "自動",
                    description: "システム設定に従う",
                    icon: KsImage.systemName("circle.lefthalf.fill"),
                    groupId: "theme",
                    value: "auto",
                    selectedValue: selectedTheme,
                    accentColor: SampleTheme.demoAccentTeal,
                    onSelected: { selectedTheme = $0 }
                )
            }

            // SimpleCheckCell with all common fields + accentColor
            Section("SimpleCheckCell — 共通フィールド + accentColor") {
                SimpleCheckCell(
                    title: "通知 1",
                    description: "週次レポート",
                    icon: KsImage.systemName("envelope"),
                    hintText: "新規",
                    isChecked: simpleCheck1,
                    accentColor: SampleTheme.demoAccentPink,
                    onValueChanged: { simpleCheck1 = $0 }
                )
                SimpleCheckCell(
                    title: "通知 2",
                    description: "月次サマリ",
                    icon: KsImage.systemName("calendar"),
                    isChecked: simpleCheck2,
                    accentColor: SampleTheme.demoAccentGreen,
                    onValueChanged: { simpleCheck2 = $0 }
                )
            }

            // ButtonCell with icon + valueText + hintText (normal layout)
            Section("ButtonCell — icon / valueText / hintText 指定時") {
                ButtonCell(
                    title: "登録",
                    valueText: "送信",
                    icon: KsImage.systemName("paperplane"),
                    hintText: "推奨",
                    titleColor: SampleTheme.demoTitleBlue,
                    titleAlignment: .start
                )
                ButtonCell(
                    title: "ログアウト",
                    titleAlignment: .center
                )
            }
        }
        .navigationTitle(SampleScreen.unifyCommonFields.title)
    }
}

#if DEBUG
#Preview {
    NavigationStack {
        UnifyCellCommonFieldsDemoView()
    }
}
#endif
