# 一致検証結果: add-sample-dark-mode-toggle (001 回目)

**日付**: 2026-09-05
**判定**: VALID

デルタスペック 3 枚 (samples-ios / samples-android / samples-maui) の全 Requirement / Scenario に対応する実装と証跡が存在する。deviation.md 記録済みの縮小が 1 項目 (Theme を渡さない画面のダーク描画)、記録済みの本体修正が 1 項目あり、それ以外の未記録乖離・虚偽チェックは検出していない。

## 実行した客観確認

| 検査 | 結果 |
|---|---|
| iOS 全テスト (`xcodebuild test -scheme KsSettingsView`, iPhone 17 Pro) | **TEST SUCCEEDED**。バンドル集計 166 + 88 + 94 + 7 + 654 = **1009 tests / 0 failures** |
| iOS Swift 6 言語モード (`SWIFT_VERSION=6` 付きビルド、`-swift-version 6` が実際に渡ったことをログで確認、126 SwiftCompile) | **error 0 件** (warning は残存。`ios/Package.swift` は無変更) |
| Android Sample ビルド (`./gradlew :app:assembleDebug --offline`) | 成功 (exit 0) |
| MAUI Sample ビルド (`dotnet build -f net9.0-ios`) | 成功 (exit 0)。net9.0-android は未実行 (指揮側報告と `maui-android-*` 証跡で代替) |
| 標準 lint (`local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py`) | いずれも 0 件 (comment-policy は検査対象 770 ファイル) |

## 対応表

### specs/samples-ios/spec.md

| Requirement / Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| 外観の切り替え / ダークを選ぶとライブラリ既定色の画面が暗く描画される | `samples/ios/KsSettingsViewSample/ContentView.swift:18,23,61`、`samples/ios/KsSettingsViewSample/SampleAppearance.swift` | `ui/verification/ios-menu-dark.png` (選択中の識別)、`ui/verification/ios-visibility-dark.png` | ⚠️ deviation 記録済み (1 項目目 — 既定色は追随せず、chrome と Theme 明示画面までが達成範囲) |
| 外観の切り替え / システムに戻すと端末の外観に従う | `ContentView.swift:61` (`colorScheme` が `nil` = 上書きなし)、`SampleAppearance.swift` の `colorScheme` | `ui/verification/ios-menu-light.png` | ✅ 一致 |
| 外観の切り替え / 選択が再起動後も維持される | `ContentView.swift:18` (`@AppStorage`) | `ui/verification/ios-menu-dark-relaunch.png` | ✅ 一致 |
| 外観の切り替え / システム選択中に端末の外観が変わると追随する | `ContentView.swift:61` | `ui/verification/ios-menu-system-device-dark.png` | ✅ 一致 |
| Sample Theme の差し替え / ダーク時に dark プリセットで描画される | `samples/ios/KsSettingsViewSample/SampleTheme.swift:158-186`、`BasicCellsDemoView.swift:167,174`、`InputCellsDemoView.swift:321`、`CustomCellDemoView.swift:239`、`SectionDecorationDemoView.swift:98`、`SectionDecorationPreset.swift:39-55` | `ui/verification/ios-basic-cells-dark-1.png` / `-2.png`、`ios-input-cells-dark.png`、`ios-section-decoration-dark.png` | ✅ 一致 |
| Sample Theme の差し替え / ライト時は従来どおり | `SampleTheme.swift:138` (`mauiLight` は旧 `maui` と同一定義。旧 `mauiTitleText` = `#CC9900` は `mauiHeaderText` と同値で `mauiTitleText(dark:)` の light 側に移設) | `ui/verification/ios-basic-cells-light.png` | ✅ 一致 |
| Sample Theme の差し替え / dark 側は description と valueText も明示 | `SampleTheme.swift` `mauiDark` の `cellValueTextColor` / `cellDescriptionColor` | 承認モック色ロール対応表と RGBA 一致 (下記「色値の検算」) | ✅ 一致 |
| カレンダー範囲デモ / 範囲外の日付は選択できない | `InputCellsDemoView.swift:296-297` (`minDate` 2026/06/01・`maxDate` 2026/06/20) | `ui/verification/ios-calendar-dark-range.png` (21 日以降が無効表示) | ✅ 一致 |
| カレンダー範囲デモ / 今日が範囲外なら今日ジャンプは選択状態を変えない | 同上 (本体挙動) | 同上 + tasks 5.3 の 4 実行面確認 | ✅ 一致 |

### specs/samples-android/spec.md

| Requirement / Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| 外観の切り替え / ダークを選ぶとライブラリ既定色の画面が暗く描画される | `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt:53-55,99-104`、`SampleAppearanceStore.kt:48-57`、`MenuScreen.kt:56,62-77`、`SampleScreen.kt:54-82` | `ui/verification/android-menu-dark.png`、`android-visibility-dark.png` | ⚠️ deviation 記録済み (1 項目目) |
| 外観の切り替え / システムに戻すと端末の夜間モードに従う | `SampleAppearanceStore.kt:49` (`System` は `null` = 上書きなし) | `ui/verification/android-menu-light.png` | ✅ 一致 |
| 外観の切り替え / 選択が再起動後も維持される | `SampleAppearanceStore.kt:25-38` (SharedPreferences) + `MainActivity.kt:53-55` | tasks 5.1 の 4 実行面確認 | ✅ 一致 |
| 外観の切り替え / システム選択中に端末の外観が変わると追随する | 上書きなし + `AndroidManifest.xml` に `configChanges` 未指定 (uiMode 変更で Activity 再生成) | tasks 5.1 の 4 実行面確認 | ✅ 一致 |
| 外観の切り替え / Activity は `ComponentActivity` のまま (android/ADR-0020) | `MainActivity.kt:46` (`class MainActivity : ComponentActivity()`)、`AndroidManifest.xml:23` の style は `@android:style/Theme.Material.*.NoActionBar` 親 (AppCompat / MaterialComponents 不使用) | `samples/android/app/src/main/res/values/themes.xml`、`res/values-night/themes.xml` | ✅ 一致 |
| chrome の夜間モード追随 / ダーク時に chrome も暗くなる | `AndroidManifest.xml:23`、`res/values-night/themes.xml`、`MainActivity.kt:141-147` (`SampleAppTheme`) | `ui/verification/android-menu-dark.png` | ✅ 一致 |
| chrome の夜間モード追随 / 選択面のダイアログも実効外観に従う | `MainActivity.kt:53-55` (Activity の Configuration 上書き) | `ui/verification/android-calendar-dark-range.png` (ダーク配色のダイアログ) | ✅ 一致 |
| Sample Theme の差し替え (2 Scenario) | `SampleTheme.kt:202-232`、`BasicCellsDemoScreen.kt:46,65,184`、`InputCellsDemoScreen.kt:41,116`、`CustomCellDemoScreen.kt:50,75`、`SectionDecorationDemoScreen.kt:37,59`、`SectionDecorationPreset.kt:35-49` | `ui/verification/android-basic-cells-dark-1.png` / `-2.png` / `android-basic-cells-light.png` | ✅ 一致 |
| Sample Theme の差し替え / 色値は iOS 側と同一 RGBA | `SampleTheme.kt:57-84` と `samples/ios/KsSettingsViewSample/SampleTheme.swift:44-64` を突き合わせ | 下記「色値の検算」 | ✅ 一致 |
| カレンダー範囲デモ (2 Scenario) | `InputCellsDemoScreen.kt:322-323` | `ui/verification/android-calendar-dark-range.png` | ✅ 一致 |

### specs/samples-maui/spec.md

| Requirement / Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| 外観の切り替え / ダークを選ぶとライブラリ既定色のページが暗く描画される | `samples/maui/KsSettingsView.Sample.Maui/SampleAppearanceStore.cs:44` (`UserAppTheme`)、`MenuPage.cs:32,95,123-126`、`SampleAppearance.cs` | `ui/verification/maui-ios-menu-dark.png` / `maui-android-menu-dark.png`、`maui-ios-visibility-dark.png` / `maui-android-visibility-dark.png` | ⚠️ deviation 記録済み (1 項目目) |
| 外観の切り替え / システムに戻すと端末の外観に従う | `SampleAppearance.cs` `ToAppTheme()` の `AppTheme.Unspecified` | `ui/verification/maui-ios-menu-light.png` / `maui-android-menu-light.png` | ✅ 一致 |
| 外観の切り替え / 選択が再起動後も維持される | `SampleAppearanceStore.cs:29-40` (Preferences) + `App.cs:12` | `ui/verification/maui-ios-menu-dark-relaunch.png` / `maui-android-menu-dark-relaunch.png` | ✅ 一致 |
| 外観の切り替え / システム選択中に端末の外観が変わると追随する | `UserAppTheme = Unspecified` + `Platforms/Android/MainActivity.cs` から `ConfigChanges.UiMode` を外して再生成に委ねる | `ui/verification/maui-ios-menu-system-device-dark.png` (再開時反映 — spec の「再開時を含む」に合致) / `maui-android-menu-system-device-dark.png` | ✅ 一致 |
| 外観の切り替え / ナビゲーションバーは対象外 | `SampleTheme.cs` の `Apply` はバーの色に触れない | `ui/verification/maui-ios-menu-dark.png` (バーは固定色のまま) | ✅ 一致 |
| Sample Theme の差し替え / ダーク時に dark プリセットで描画される | `SampleTheme.cs:136,150-172,174,189-200`、各 `Pages/*.xaml.cs` の `SampleThemeFollower.Attach` (7 ページ) | `ui/verification/maui-ios-basic-cells-dark-1.png` / `-2.png`、`maui-android-basic-cells-dark-1.png` / `-2.png` | ✅ 一致 |
| Sample Theme の差し替え / 表示中のページも追随する | `SampleThemeFollower.cs:47-72` (`RequestedThemeChanged` 購読) | tasks 5.1 の 4 実行面確認 | ✅ 一致 |
| Sample Theme の差し替え / ライト時は従来どおり | `SampleTheme.cs:150-172` の light 分岐。`CellValueTextColor` / `CellDescriptionColor` に渡す `null` は `BindableProperty` 既定 `default(Color)` と同値 (`maui/KsSettingsView.Maui/SettingsView.cs:328-333,360-365`) のため light の描画は不変 | `ui/verification/maui-ios-basic-cells-light.png` / `maui-android-basic-cells-light.png` | ✅ 一致 |
| カレンダー範囲デモ (2 Scenario) | `ViewModels/InputCellsDemoViewModel.cs:291,294`、`Pages/InputCellsDemoPage.xaml:132-133` | `ui/verification/maui-ios-calendar-dark-range.png`、`maui-android-calendar-dark-range.png` | ✅ 一致 |

### deviation.md 2 項目目 (iOS 本体の提示外観引き継ぎ)

| 記録内容 | 実装 | 証跡 | 状態 |
|---|---|---|---|
| 提示元と window の外観が食い違うとき提示物へ提示元の外観を与える | `ios/Sources/KsSettingsViewUI/PresentationAppearance.swift` | `ios/Tests/KsSettingsViewUITests/PresentationAppearanceTests.swift` (8 テスト、全て pass) | ✅ 一致 |
| 修正範囲は「同型の提示を持つ箇所すべて」 | `ios/Sources/` 内の `present(` 呼び出しは 2 箇所のみ (`PickerCellView.swift:109` / `DatePickerCellView.swift:118`) で、双方が `PresentationAppearance.inherit` を通る (`PickerCellView.swift:101` / `DatePickerCellView.swift:139`) | 同上 | ✅ 一致 |
| MAUI iOS のカレンダーシート地色の解消 | 同上 | `ui/verification/maui-ios-calendar-dark-range-before.png` → `maui-ios-calendar-dark-range.png` の A/B、非回帰 `maui-ios-calendar-system-dark-range.png` / `ios-calendar-dark-range.png` | ✅ 一致 |
| PickerCell 選択面 (もう一方の提示経路) の実機描画 | 同上 | **証跡なし** (ユニットテストのみ) | ⚠️ 所見 — 下記「所見」参照。Scenario ではないため ❌ にはしない |

## 色値の検算 (承認モック対応表 ↔ 3 面の定義)

| 色ロール | モック (dark) | iOS `SampleTheme.swift` | Android `SampleTheme.kt` | MAUI `SampleTheme.cs` |
|---|---|---|---|---|
| backgroundColor / header・footerBackground | #1B1915 | `mauiDarkViewBackground` #1B1915 | `mauiDarkViewBackground` 0xFF1B1915 | `MauiDarkViewBackground` #1B1915 |
| cellBackgroundColor | #2A2620 | #2A2620 | 0xFF2A2620 | #2A2620 |
| separatorColor | #4A3F28 | #4A3F28 | 0xFF4A3F28 | #4A3F28 |
| cellAccentColor (selectedColor は同色 α 0x50) | #FFBF00 | light と共有 (`mauiAccent` / `mauiSelected`) | 同左 | 同左 |
| headerTextColor / ButtonCell titleColor | #E0B040 | #E0B040 | 0xFFE0B040 | #E0B040 |
| footerTextColor | #9A948A | #9A948A | 0xFF9A948A | #9A948A |
| disabledTextColor | #7A756C | #7A756C | 0xFF7A756C | #7A756C |
| cellTitleColor | #E6E1D6 | #E6E1D6 | 0xFFE6E1D6 | #E6E1D6 |
| cellValueTextColor | #B8B2A6 | #B8B2A6 | 0xFFB8B2A6 | #B8B2A6 |
| cellDescriptionColor | #9A948A | #9A948A | 0xFF9A948A | #9A948A |

3 面すべて承認モックの対応表と一致。light 側は 3 面とも定数値の変更なし (iOS の `mauiTitleText` #CC9900 は `mauiHeaderText` と同値のため、関数化による light の実効色の変化はない)。

## 3 面の文言・パラメータ一致 (sample-parity)

| 項目 | iOS | Android | MAUI |
|---|---|---|---|
| 見出し | `SampleAppearance.sectionTitle` = "外観" | `SampleAppearance.SECTION_TITLE` = "外観" | `SampleAppearances.SectionTitle` = "外観" |
| 項目文言 | システム / ライト / ダーク | システム / ライト / ダーク | システム / ライト / ダーク |
| 並び順 | system → light → dark | System → Light → Dark | System → Light → Dark |
| 初期値 | `.system` | `System` | `AppTheme` `System` |
| 予約日の範囲 | 2026/06/01–2026/06/20 | 同左 | 同左 |
| 初期値 (予約日) | 変更なし | 変更なし | 変更なし |

証跡画像 (`ios-menu-dark.png` / `android-menu-dark.png` / `maui-ios-menu-dark.png`) でも見出しと 3 項目・並び順が一致していることを目視確認した。

## 追加検査

- **tasks.md の虚偽チェック**: なし。1.1〜5.5 の全項目に対応する実装または実行結果を確認した (5.5 のビルド・lint は本検証で再実行して追認、MAUI net9.0-android のみ未再実行)
- **逆流検査**: `kasane/changes/add-sample-dark-mode-toggle/` の proposal.md / specs/ / tasks.md / deviation.md / ui/ は未追跡 (未コミット) のため `git diff` による逆流検査ができない。追跡済みの `exploration.md` の変更は本検証開始時点の `git status` スナップショットにも既に含まれており、探索フェーズ (探索日 2026-09-05) の内容更新であって実装期間中の書き換えではない。**構造的にコミット前のため、逆流なしは git では証明できない**点を明記する
- **未記録乖離**: 対応表に ❌ なし
- **UI 変更の記録**: `ui/brief.md` に承認モック (plan-a / approved.png、2026-09-05 オーナー承認、および改訂後の再承認) と 4 実行面の照合結果、モックとの既知の差分 (「無効なボタン」行はモック側の創作で照合対象外) が記録済み。「合意済み妥協」「未合意の乖離」も明示的に空と記録されている
- **テスト**: iOS 1009 件 / 0 失敗を本検証で実行して確認。`samples/` にはテストターゲットがなく、Scenario は 4 実行面の確認と `ui/verification/` の 44 枚で担保 (tasks.md 冒頭の宣言どおり)

## 所見 (判定には影響しないが記録する)

- deviation.md 2 項目目の修正は iOS 本体の提示経路 2 箇所に入っているが、`ui/verification/` の証跡は DatePickerCell のカレンダーシートのみで、**PickerCell の選択面 (MAUI ダークでの解消・iOS Native での非回帰) の画像がない**。Scenario に対応しないため本検証では ❌ にしないが、レビュー側 (review-001.md) では process/L-003 に照らして指摘対象とした
- MAUI net9.0-android TFM のビルドのみ本検証で再実行していない (`maui-android-*` の証跡 11 枚が実行済みアプリの撮影であることから間接的に成立と判断)
