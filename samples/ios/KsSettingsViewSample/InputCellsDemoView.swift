// InputCellsDemoView.swift
// KsSettingsViewSample
//
// 5 種の入力系 Cell（EntryCell / PickerCell / NumberPickerCell / TimePickerCell /
// DatePickerCell）を 1 画面に並べて目視確認できるデモ画面。
//
// 構成方針:
//   - 5 種すべての Cell を SwiftUI DSL の `KsSettingsView { ... }` で配置。
//   - 基本 Cell 7 種デモと同一様式（MAUI 互換 Theme + 直近イベント1行）に揃える。
//   - Section 見出しはプラットフォーム間で同一にできる中立な文言にする。
//   - 「TwoWay binding 経路」と「Store 経路（callback）」の両方が触れるように、
//     Section ごとに分けて両経路を併載する。
//   - 入力 Cell 5 種の Renderer は `KsSettingsViewController.init` のデフォルト
//     `registerInputCells()` で auto-register されているため、ここでは追加登録は不要。
//   - 末尾の「EntryCell（下部配置）」セクションは、画面下半分に置いた EntryCell の
//     キーボード回避 (フォーカス時のせり上がり) を確認するための検証用。

import SwiftUI
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI

/// 入力系 Cell 5 種デモ画面。
///
/// SwiftUI `@State` を `Binding` 経由で各 Cell に渡し（TwoWay binding 経路）、
/// 値が実際に変わったときだけ画面上部の直近イベント表示を更新する。
/// 一部 Cell は「Store 経路（callback 形式）」も併載し、両 API の挙動を比較できる。
struct InputCellsDemoView: View {

    // MARK: - EntryCell TwoWay binding 用

    /// 名前（テキスト、`maxLength = 20`）
    @State private var userName: String = "Tanaka Taro"
    /// メールアドレス（テキスト、Email キーボード）
    @State private var email: String = "tanaka.taro@example.com"
    /// 電話番号（Native `UIKeyboardType.phonePad` 直接渡し）
    @State private var phone: String = "090-0000-0000"
    /// パスワード（マスク表示）
    @State private var password: String = "secret123"

    // MARK: - EntryCell Store 経路（callback）用

    /// callback 経路の表示用ニックネーム
    @State private var nickname: String = ""

    // MARK: - EntryCell placeholder 色指定用

    /// 表示名（placeholder 色を Cell 個別に指定する行）
    @State private var displayName: String = ""

    // MARK: - PickerCell（単一）TwoWay binding 用

    /// 単一選択：テーマ index
    @State private var themeIndex: Int? = 0
    private let themes: [String] = ["ライト", "ダーク", "自動"]

    // MARK: - PickerCell（複数）TwoWay binding 用

    /// 複数選択：通知種別 index 集合
    @State private var notifSelection: Set<Int> = [0, 2]
    private let notifTypes: [String] = ["メール", "プッシュ", "SMS", "アプリ内", "電話"]

    // MARK: - PickerCell（object 候補）用

    /// object 候補：主表示に名前、副表示に役割を射影する架空のメンバー
    private let members: [SampleMember] = SampleMember.notificationTargets
    /// 単一選択：担当者（選択した元要素をそのまま受け取る経路）
    @State private var assignee: SampleMember? = SampleMember.notificationTargets.first
    /// 複数選択：通知先メンバーの index 集合（確定時に元要素の一覧を callback で受け取る）
    @State private var memberSelection: Set<Int> = [0, 2]

    // MARK: - NumberPickerCell TwoWay binding 用

    /// サイズ（10..30、step 1、unit "px"）
    @State private var volume: Int = 30

    // MARK: - TimePickerCell TwoWay binding 用

    /// アラーム時刻（`Date` の hour / minute 成分のみ使用）
    @State private var alarmDate: Date = Self.makeTime(hour: 7, minute: 30)
    /// 就寝時刻（`is24Hour = false`。選択面が午前／午後のホイールを持つ形になる）
    @State private var bedDate: Date = Self.makeTime(hour: 22, minute: 15)

    // MARK: - DatePickerCell TwoWay binding 用

    /// 誕生日（`Date` の year / month / day 成分のみ使用）
    @State private var birthdayDate: Date = Self.makeDate(year: 1990, month: 1, day: 1)
    /// 予約日（誕生日とは独立した状態）
    @State private var reservationDate: Date = Self.makeDate(year: 2026, month: 6, day: 1)

    // MARK: - EntryCell 下部配置（キーボード回避検証）用

    /// メモ（画面下半分での フォーカス→せり上がり 検証用）
    @State private var memo: String = ""
    /// 署名（最下部での フォーカス→せり上がり 検証用）
    @State private var signature: String = ""

    // MARK: - 直近イベント表示

    /// 「最後のイベント: <Cell の title> → <変更後の値>」形式の1行表示。
    @State private var lastEvent: String = "(none)"

    var body: some View {
        VStack(spacing: 0) {
            Text("最後のイベント: \(lastEvent)")
                .font(.caption)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()

            KsSettingsView {
                // 1. EntryCell セクション（TwoWay binding + Store 経路）
                Section(
                    "EntryCell",
                    footer: "ニックネーム (callback) は値変更コールバックで状態を更新する経路のデモ。他の入力欄は双方向バインディング経路。表示名は placeholder 色を Cell 個別に指定した行。"
                ) {
                    // TwoWay binding（@Binding<String>）
                    EntryCell(
                        title: "名前",
                        text: tracked($userName, title: "名前"),
                        placeholder: "山田 太郎",
                        keyboardType: .default,
                        maxLength: 20
                    )
                    EntryCell(
                        title: "メール",
                        text: tracked($email, title: "メール"),
                        placeholder: "example@example.com",
                        keyboardType: .emailAddress
                    )
                    // Native UIKeyboardType.phonePad 直接渡し
                    EntryCell(
                        title: "電話",
                        text: tracked($phone, title: "電話"),
                        placeholder: "090-0000-0000",
                        keyboardType: .phonePad
                    )
                    // パスワードマスク（イベント表示も Cell と同じマスク形式に合わせる）
                    EntryCell(
                        title: "パスワード",
                        text: tracked($password, title: "パスワード", display: Self.maskedText),
                        placeholder: "8 文字以上",
                        isPassword: true
                    )
                    // Store 経路（text: String + onTextChanged callback）
                    EntryCell(
                        title: "ニックネーム (callback)",
                        text: nickname,
                        placeholder: "callback 経路で更新",
                        onTextChanged: { newValue in
                            guard newValue != nickname else { return }
                            nickname = newValue
                            lastEvent = "ニックネーム (callback) → \(newValue)"
                        }
                    )
                    // placeholder 色を Cell 個別に指定する経路（未指定の行は OS 既定色）
                    EntryCell(
                        title: "表示名",
                        text: tracked($displayName, title: "表示名"),
                        placeholder: "placeholder 色の指定例",
                        placeholderColor: SampleTheme.demoPlaceholderOrange
                    )
                }

                // 2. PickerCell（単一）セクション
                Section("PickerCell（単一選択）") {
                    PickerCell(
                        title: "テーマ",
                        items: themes,
                        selectedIndex: tracked($themeIndex, title: "テーマ", display: { idx in
                            guard let idx, themes.indices.contains(idx) else { return "(未選択)" }
                            return themes[idx]
                        }),
                        pageTitle: "テーマを選択"
                    )
                }

                // 3. PickerCell（複数）セクション
                Section(
                    "PickerCell（複数選択 / 上限 3）",
                    footer: "3 つまで選択できます。4 つ目を選ぼうとすると触覚フィードバックが返ります。"
                ) {
                    PickerCell(
                        title: "通知種別",
                        items: notifTypes,
                        selectedIndices: tracked($notifSelection, title: "通知種別", display: { indices in
                            let labels = indices.sorted().compactMap { idx -> String? in
                                guard notifTypes.indices.contains(idx) else { return nil }
                                return notifTypes[idx]
                            }
                            return labels.isEmpty ? "(未選択)" : labels.joined(separator: ", ")
                        }),
                        maxSelectedNumber: 3,
                        pageTitle: "通知種別を選択"
                    )
                }

                // 4. PickerCell（object 候補）セクション
                Section(
                    "PickerCell（object 候補 / 副表示）",
                    footer: "任意の型の要素を候補にして、主表示と副表示に射影するデモ。担当者は選択した要素そのものを受け取る経路、通知先メンバーは確定時に選択要素の一覧を受け取る経路。副表示を持たない候補は 1 行で表示される。"
                ) {
                    PickerCell(
                        title: "担当者",
                        items: members,
                        displayText: { $0.name },
                        subText: { $0.role },
                        selectedItem: tracked(
                            $assignee,
                            title: "担当者",
                            display: { $0?.name ?? "(未選択)" }
                        ),
                        pageTitle: "担当者を選択"
                    )
                    PickerCell(
                        title: "通知先メンバー",
                        items: members,
                        displayText: { $0.name },
                        subText: { $0.role },
                        selectedIndices: $memberSelection,
                        pageTitle: "通知先メンバー",
                        onItemsSelected: { picked in
                            let labels = picked.map(\.name)
                            lastEvent = "通知先メンバー → "
                                + (labels.isEmpty ? "(未選択)" : labels.joined(separator: ", "))
                        }
                    )
                }

                // 5. NumberPickerCell セクション
                Section(
                    "NumberPickerCell",
                    footer: "Picker UI と Cell の valueText に \"px\" suffix が付く。"
                ) {
                    NumberPickerCell(
                        title: "サイズ",
                        min: 10,
                        max: 30,
                        step: 1,
                        value: tracked($volume, title: "サイズ", display: { "\($0) px" }),
                        unit: "px",
                        pickerTitle: "サイズを選択"
                    )
                }

                // 6. TimePickerCell セクション
                Section("TimePickerCell") {
                    TimePickerCell(
                        title: "アラーム",
                        time: tracked($alarmDate, title: "アラーム", display: { Self.timeFormatter.string(from: $0) }),
                        format: "HH:mm",
                        pickerTitle: "アラーム時刻"
                    )
                    // `is24Hour = false` を指定すると選択面は午前／午後を含む 3 系列になる (並び順は端末 Locale の時刻表記に従う)。
                    // `format` は行の表示にだけ効き、選択面の時制には関与しない。
                    TimePickerCell(
                        title: "就寝",
                        time: tracked($bedDate, title: "就寝", display: { Self.time12HFormatter.string(from: $0) }),
                        format: "h:mm a",
                        is24Hour: false,
                        pickerTitle: "就寝時刻"
                    )
                }

                // 7. DatePickerCell セクション（ホイール形式）
                Section(
                    "DatePickerCell（ホイール）",
                    footer: "ホイール形式で日付を選択するデモ。"
                ) {
                    DatePickerCell(
                        title: "誕生日",
                        date: tracked($birthdayDate, title: "誕生日", display: { Self.dateFormatter.string(from: $0) }),
                        format: "yyyy/MM/dd",
                        minDate: Self.makeDate(year: 1900, month: 1, day: 1),
                        maxDate: Date(),
                        pickerTitle: "誕生日",
                        uiStyle: .wheels,
                        todayText: "今日"
                    )
                }

                // 8. DatePickerCell セクション（カレンダー形式）
                Section(
                    "DatePickerCell（カレンダー）",
                    footer: "カレンダー形式で日付を選択するデモ。"
                ) {
                    DatePickerCell(
                        title: "予約日",
                        date: tracked($reservationDate, title: "予約日", display: { Self.dateFormatter.string(from: $0) }),
                        format: "yyyy/MM/dd",
                        pickerTitle: "予約日を選択",
                        uiStyle: .calendar,
                        todayText: "今日"
                    )
                }

                // 9. EntryCell（下部配置）セクション — キーボード回避の検証用
                Section(
                    "EntryCell（下部配置）",
                    footer: "画面下半分に配置した EntryCell をフォーカスしたとき、キーボードに合わせてコンテンツがせり上がるかを確認するための検証用セクション。"
                ) {
                    EntryCell(
                        title: "メモ",
                        text: tracked($memo, title: "メモ"),
                        placeholder: "下部配置の検証用"
                    )
                    EntryCell(
                        title: "署名",
                        text: tracked($signature, title: "署名"),
                        placeholder: "最下部の検証用"
                    )
                }
            }
            .theme(SampleTheme.maui)
            .ignoresSafeArea(.container, edges: .bottom)
        }
        .navigationTitle(SampleScreen.inputCells.title)
    }

    // MARK: - 直近イベント記録

    /// 値が実際に変化したときだけ直近イベント表示を更新する `Binding` ラッパー。
    ///
    /// 受け付けられなかった操作（複数選択の上限超過など）では元の値がそのまま
    /// 書き戻されるため、`newValue == 現在値` の場合は何も更新しない。
    private func tracked<Value: Equatable>(
        _ source: Binding<Value>,
        title: String,
        display: @escaping (Value) -> String
    ) -> Binding<Value> {
        Binding(
            get: { source.wrappedValue },
            set: { newValue in
                guard newValue != source.wrappedValue else { return }
                source.wrappedValue = newValue
                lastEvent = "\(title) → \(display(newValue))"
            }
        )
    }

    /// 値をそのまま文字列化する `tracked` の簡易版（`String` 値の Cell 用）。
    private func tracked(_ source: Binding<String>, title: String) -> Binding<String> {
        tracked(source, title: title, display: { $0 })
    }

    /// パスワードの表示形式（Cell と同じマスク表現）。
    private static func maskedText(_ value: String) -> String {
        String(repeating: "•", count: value.count)
    }

    // MARK: - 補助：固定時刻・固定日付の生成

    private static func makeTime(hour: Int, minute: Int) -> Date {
        var comps = DateComponents()
        comps.hour = hour
        comps.minute = minute
        return Calendar.current.date(from: comps) ?? Date()
    }

    private static func makeDate(year: Int, month: Int, day: Int) -> Date {
        var comps = DateComponents()
        comps.year = year
        comps.month = month
        comps.day = day
        return Calendar.current.date(from: comps) ?? Date()
    }

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "HH:mm"
        return f
    }()

    private static let time12HFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "h:mm a"
        return f
    }()

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy/MM/dd"
        return f
    }()
}

#if DEBUG
#Preview {
    NavigationStack {
        InputCellsDemoView()
    }
}
#endif
