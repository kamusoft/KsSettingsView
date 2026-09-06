// BasicCellsDemoView.swift
// KsSettingsViewSample
//
// 7 種の基本 Cell（LabelCell / CommandCell / ButtonCell / SwitchCell / CheckboxCell /
// RadioCell / SimpleCheckCell）を 1 画面に並べて目視確認できるデモ画面。
//
// `AiForms.Maui.SettingsView` の Sample（Sample/Views/MainPage.xaml）と限りなく一致する
// 見た目を目指し、MAUI 互換 Theme を明示渡しする。
//
// 色はスタイル層の Native 型である `UIColor` で直接構築する（core/ADR-0009）。中間の論理
// 色表現は挟まない。
//
// セクション構成は Cell タイプ別（CommandCell → LabelCell → SwitchCell → CheckboxCell →
// RadioCell → SimpleCheckCell → ButtonCell）。iOS / Android で一字一句揃える。

import SwiftUI
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI

/// 基本 Cell 7 種を Cell タイプ別構成で確認するデモ画面。
///
/// MAUI 原典 `Sample/Views/MainPage.xaml` に倣い、MAUI 互換 Theme を `.theme(...)` で明示渡しする。
/// セクション名は Cell タイプ名そのもの。
struct BasicCellsDemoView: View {

    // SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell の状態
    @State private var notifEnabled: Bool = true
    @State private var agreedTerms: Bool = true
    @State private var selectedType: String = "TypeA"
    @State private var simpleCheck1: Bool = true
    @State private var simpleCheck2: Bool = false
    @State private var simpleCheck3: Bool = false

    @State private var lastTappedTitle: String = "(none)"

    /// 実効外観。ダークのとき Theme の dark 側を渡す。
    @Environment(\.colorScheme) private var colorScheme

    // MARK: - MAUI 互換 Theme
    //
    // 色定数と Theme は入力 Cell 5 種デモと共用の `SampleTheme` に一元化してある。

    var body: some View {
        VStack(spacing: 0) {
            Text("最後にタップ: \(lastTappedTitle)")
                .font(.caption)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()

            KsSettingsView {
                // 1. CommandCell セクション（3 個: フル / シンプル / 中間）
                Section("CommandCell", headerHeight: 60) {
                    CommandCell(
                        style: CellStyle(cellHeight: 80),
                        title: "Tanaka Taro",
                        description: "tanaka.taro@example.com",
                        icon: KsImage.systemName("person.crop.circle"),
                        onTap: { lastTappedTitle = "Tanaka Taro" }
                    )
                    CommandCell(
                        title: "プロフィール",
                        onTap: { lastTappedTitle = "プロフィール" }
                    )
                    CommandCell(
                        title: "通知設定",
                        valueText: "オン",
                        onTap: { lastTappedTitle = "通知設定" }
                    )
                }

                // 2. LabelCell セクション（2 個: フル / シンプル）
                Section("LabelCell") {
                    LabelCell(
                        title: "Storage",
                        description: "This is description. you can write detail explanation of the item here. long text wrap automatically.",
                        valueText: "256 GB",
                        icon: KsImage.systemName("externaldrive")
                    )
                    LabelCell(
                        title: "バージョン",
                        valueText: "1.0.0"
                    )
                }

                // 3. SwitchCell セクション（1 個）
                Section("SwitchCell") {
                    SwitchCell(
                        title: "Notification",
                        description: "This is description. you can write detail explanation of the item here. long text wrap automatically.",
                        isOn: notifEnabled,
                        onValueChanged: { newValue in
                            notifEnabled = newValue
                            lastTappedTitle = "Notification → \(newValue)"
                        }
                    )
                }

                // 4. CheckboxCell セクション（1 個）
                Section("CheckboxCell") {
                    CheckboxCell(
                        title: "Agree to Terms",
                        isChecked: agreedTerms,
                        onValueChanged: { newValue in
                            agreedTerms = newValue
                            lastTappedTitle = "Agree → \(newValue)"
                        }
                    )
                }

                // 5. RadioCell セクション（2 個 + footer 説明文）
                Section("RadioCell", footer: "You can select either TypeA or TypeB.") {
                    RadioCell(
                        title: "TypeA",
                        groupId: "type",
                        value: "TypeA",
                        selectedValue: selectedType,
                        onSelected: { v in
                            selectedType = v
                            lastTappedTitle = "Type → \(v)"
                        }
                    )
                    RadioCell(
                        title: "TypeB",
                        groupId: "type",
                        value: "TypeB",
                        selectedValue: selectedType,
                        onSelected: { v in
                            selectedType = v
                            lastTappedTitle = "Type → \(v)"
                        }
                    )
                }

                // 6. SimpleCheckCell セクション（3 個: Item 1 / Item 2 / Item 3）
                Section("SimpleCheckCell") {
                    SimpleCheckCell(
                        title: "Item 1",
                        isChecked: simpleCheck1,
                        onValueChanged: { newValue in
                            simpleCheck1 = newValue
                            lastTappedTitle = "Item 1 → \(newValue)"
                        }
                    )
                    SimpleCheckCell(
                        title: "Item 2",
                        isChecked: simpleCheck2,
                        onValueChanged: { newValue in
                            simpleCheck2 = newValue
                            lastTappedTitle = "Item 2 → \(newValue)"
                        }
                    )
                    SimpleCheckCell(
                        title: "Item 3",
                        isChecked: simpleCheck3,
                        onValueChanged: { newValue in
                            simpleCheck3 = newValue
                            lastTappedTitle = "Item 3 → \(newValue)"
                        }
                    )
                }

                // 7. ButtonCell セクション（1 個: ログアウト、titleAlignment 既定 = .center）
                Section("ButtonCell") {
                    ButtonCell(
                        style: CellStyle(titleColor: SampleTheme.mauiTitleText(dark: colorScheme == .dark)),
                        title: "ログアウト",
                        onTap: { lastTappedTitle = "ログアウト" },
                        titleAlignment: .center
                    )
                }
            }
            .theme(SampleTheme.maui(dark: colorScheme == .dark))
            .ignoresSafeArea(.container, edges: .bottom)
        }
        .navigationTitle(SampleScreen.basicCells.title)
    }
}

#if DEBUG
#Preview {
    NavigationStack {
        BasicCellsDemoView()
    }
}
#endif
