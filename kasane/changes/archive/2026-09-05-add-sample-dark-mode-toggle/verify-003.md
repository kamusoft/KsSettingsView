# 一致検証結果: add-sample-dark-mode-toggle (003 回目)

**日付**: 2026-09-05
**判定**: VALID

デルタスペック 3 枚 (samples-ios / samples-android / samples-maui) の全 Requirement / Scenario に対応する実装と証跡が存在する。deviation.md 記録済みの縮小が 1 項目 (Theme を渡さない画面のダーク描画)、記録済みの本体修正が 1 項目。❌ は 0 件で、未記録乖離・虚偽チェック・逆流はいずれも検出していない。

サイクル 3 で入った変更 (`App.cs` の Window 保持、`MainActivity.kt` の同値ガード、`PresentationAppearanceTests.swift` の負の検証補強、`ui/brief.md` の 2 箇所) は、いずれも既存 Scenario の対応先を置き換える修正で、新しい Scenario を要求しない。samples-maui の「外観が切り替わったとき表示中のページも追随する」の証跡が 1 枚増えている (`ui/verification/maui-android-input-cells-system-device-toggle-x3.png`)。

## 実行した客観確認

| 検査 | 結果 |
|---|---|
| iOS 全テスト (`cd ios && xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17 Pro'`) | **TEST SUCCEEDED**。バンドル集計 166 + 88 + 94 + 7 + 654 = **1009 tests / 0 failures** |
| iOS Swift 6 言語モード (`xcodebuild build ... SWIFT_VERSION=6`。swiftc への `-swift-version 6` 伝播を確認) | **BUILD SUCCEEDED / error 0 件**。`ios/Package.swift` は差分 0 |
| iOS Sample (`samples/ios` で `xcodebuild build -scheme KsSettingsViewSample`) | BUILD SUCCEEDED / 警告 0 |
| Android Sample (`samples/android` で `./gradlew :app:assembleDebug --offline --rerun-tasks`) | BUILD SUCCESSFUL (59 tasks executed)。警告 2 件は本 change 対象外の `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelView.kt` の既存 deprecation |
| MAUI Sample `net10.0-android` | ビルド成功 (警告 0 / エラー 0) |
| MAUI Sample `net10.0-ios` | ビルド成功 (警告 0 / エラー 0) |
| 標準 lint (`local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py`) | いずれも 0 件 (comment-policy は検査対象 771 ファイル) |

`samples/` にテストターゲットは無いため、Scenario の担保は tasks.md 冒頭の宣言どおり 4 実行面 (iOS Native / Android Native / MAUI iOS / MAUI Android) の実機・Simulator 確認と `ui/verification/` の画像 49 枚が担う。iOS 本体を触った 1 項目 (deviation 2 項目目) だけは単体テスト `ios/Tests/KsSettingsViewUITests/PresentationAppearanceTests.swift` (9 件) を持つ。

## 対応表

### specs/samples-ios/spec.md

| Requirement / Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 外観の切り替え / ダークを選ぶとライブラリ既定色の画面が暗く描画される | `samples/ios/KsSettingsViewSample/ContentView.swift:18,23-34,60`、`samples/ios/KsSettingsViewSample/SampleAppearance.swift:36-42` | `ui/verification/ios-menu-dark.png`、`ios-visibility-dark.png` | ⚠️ deviation 記録済み (1 項目目 — 既定色は追随せず、chrome と Theme 明示画面までが達成範囲) |
| 外観の切り替え / システムに戻すと端末の外観に従う | `SampleAppearance.swift:38` (`.system` → `nil` = 上書きなし) + `ContentView.swift:60` | `ui/verification/ios-menu-light.png` | ✅ 一致 |
| 外観の切り替え / 選択が再起動後も維持される | `ContentView.swift:18` (`@AppStorage(SampleAppearance.storageKey)`)、`SampleAppearance.swift:51,54` | `ui/verification/ios-menu-dark-relaunch.png` | ✅ 一致 |
| 外観の切り替え / システム選択中に端末の外観が変わると追随する | `ContentView.swift:60` (`preferredColorScheme(nil)`) | `ui/verification/ios-menu-system-device-dark.png` | ✅ 一致 |
| 外観の切り替え / 見出し・文言・選択中の識別 (Requirement 本文の SHALL) | `ContentView.swift:23,24,32-34`、`SampleAppearance.swift:27-33,45,48` (見出し「外観」/ 3 項目 / 読み上げ「選択中」を定義 1 箇所へ集約) | `ui/verification/ios-menu-dark.png`、`ui/brief.md:47` | ✅ 一致 |
| Sample Theme の差し替え / ダーク時に dark プリセットで描画される | `samples/ios/KsSettingsViewSample/SampleTheme.swift:158-174,179`、`BasicCellsDemoView.swift:167,174`、`InputCellsDemoView.swift:321`、`CustomCellDemoView.swift:239`、`SectionDecorationDemoView.swift:98`、`SectionDecorationPreset.swift:39-56` | `ui/verification/ios-basic-cells-dark-1.png` / `-2.png`、`ios-input-cells-dark.png`、`ios-section-decoration-dark.png` | ✅ 一致 |
| Sample Theme の差し替え / ライト時は従来どおり light プリセットで描画される | `SampleTheme.swift:138-151` (`mauiLight` は旧 `maui` と同一定義)、`:184` (`mauiTitleText(dark:false)` = `mauiHeaderText` #CC9900) | `ui/verification/ios-basic-cells-light.png` | ✅ 一致 |
| Sample Theme の差し替え / dark 側は description と valueText も明示 (Requirement 本文の SHALL) | `SampleTheme.swift:172-173` (`cellValueTextColor` / `cellDescriptionColor`) | 承認モック対応表と RGBA 一致 (下記「色値の検算」) | ✅ 一致 |
| カレンダー範囲デモ / 範囲外の日付は選択できない | `InputCellsDemoView.swift:294-297` (2026/06/01–06/20、初期値 `:90` は 2026/06/01 のまま) | `ui/verification/ios-calendar-dark-range.png` | ✅ 一致 |
| カレンダー範囲デモ / 今日が範囲外なら今日ジャンプは選択状態を変えない | 同上 (本体挙動) | `ui/verification/ios-calendar-dark-range.png` + `ui/brief.md:50` (「今日」は選択も月表示も変えない) | ✅ 一致 |

### specs/samples-android/spec.md

| Requirement / Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 外観の切り替え / ダークを選ぶとライブラリ既定色の画面が暗く描画される | `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt:54-57,100-110`、`SampleAppearanceStore.kt:49-56`、`MenuScreen.kt:53-76`、`SampleAppearance.kt:14-36` | `ui/verification/android-menu-dark.png`、`android-visibility-dark.png` | ⚠️ deviation 記録済み (1 項目目) |
| 外観の切り替え / システムに戻すと端末の夜間モードに従う | `SampleAppearanceStore.kt:51` (`System` → `return null` = 上書きなし) | `ui/verification/android-menu-light.png` | ✅ 一致 |
| 外観の切り替え / 選択が再起動後も維持される | `SampleAppearanceStore.kt:23-38` (SharedPreferences) + `MainActivity.kt:54-57` | tasks 5.1 の 4 実行面確認 (画像なし — review-003 の Minor 1 で証跡範囲の明記を推奨。読み出し経路自体は `android-menu-dark.png` が示す `recreate()` → `attachBaseContext` → `load` と同一) | ✅ 一致 |
| 外観の切り替え / システム選択中に端末の外観が変わると追随する | 上書きなし + `samples/android/app/src/main/AndroidManifest.xml` に `configChanges` 未指定 (uiMode 変更で OS が Activity を再生成し `attachBaseContext` から解決し直す) | tasks 5.1 の 4 実行面確認 (画像なし — 同上) | ✅ 一致 |
| 外観の切り替え / Activity は `ComponentActivity` のまま (Requirement 本文の SHALL、android/ADR-0020) | `MainActivity.kt:47`、`AndroidManifest.xml:23` の style は `@android:style/Theme.Material*.NoActionBar` 親 (AppCompat / MaterialComponents 不使用) | `samples/android/app/src/main/res/values/themes.xml`、`res/values-night/themes.xml` | ✅ 一致 |
| 外観の切り替え / OS のアプリ単位夜間モード設定は使わない (Requirement 本文の SHALL) | `UiModeManager` / `setApplicationNightMode` は `samples/android/` に出現しない (grep 0 件)。反映は `MainActivity.kt:54-57` の `applyOverrideConfiguration` + `:109` の `recreate()` | `SampleAppearanceStore.kt:9-11` (使わない理由を記述) | ✅ 一致 |
| chrome の夜間モード追随 / ダーク時に chrome も暗くなる | `AndroidManifest.xml:23`、`res/values-night/themes.xml`、`MainActivity.kt:148-153` (`SampleAppTheme` の light / dark 分岐) | `ui/verification/android-menu-dark.png` | ✅ 一致 |
| chrome の夜間モード追随 / 選択面のダイアログも実効外観に従う | `MainActivity.kt:54-57` (Activity の Configuration 上書き) | `ui/verification/android-calendar-dark-range.png` (ダーク配色のダイアログ)、tasks 5.4 | ✅ 一致 |
| Sample Theme の差し替え / ダーク時に dark プリセットで描画される | `SampleTheme.kt:202-218,225`、`BasicCellsDemoScreen.kt:46,65,184`、`InputCellsDemoScreen.kt:41,116`、`CustomCellDemoScreen.kt:50,75`、`SectionDecorationDemoScreen.kt:37,59`、`SectionDecorationPreset.kt:35-47` | `ui/verification/android-basic-cells-dark-1.png` / `-2.png`、`android-input-cells-dark.png`、`android-section-decoration-dark.png` | ✅ 一致 |
| Sample Theme の差し替え / ライト時は従来どおり light プリセットで描画される | `SampleTheme.kt:180-195` (`mauiLight`)、`:232` (`mauiTitleText(false)` = `mauiHeaderText`) | `ui/verification/android-basic-cells-light.png` | ✅ 一致 |
| Sample Theme の差し替え / dark 側は description と valueText も明示、色値は iOS と同一 RGBA (Requirement 本文の SHALL) | `SampleTheme.kt:62-86,216-217` ↔ `samples/ios/KsSettingsViewSample/SampleTheme.swift:47-63,172-173` | 下記「色値の検算」 | ✅ 一致 |
| カレンダー範囲デモ / 範囲外の日付は選択できない | `InputCellsDemoScreen.kt:322-325` (初期値 `:91` は 2026/06/01 のまま) | `ui/verification/android-calendar-dark-range.png` | ✅ 一致 |
| カレンダー範囲デモ / 今日が範囲外なら今日ジャンプは選択状態を変えない | 同上 (本体挙動) | `ui/verification/android-calendar-dark-range.png` + `ui/brief.md:39` | ✅ 一致 |

### specs/samples-maui/spec.md

| Requirement / Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 外観の切り替え / ダークを選ぶとライブラリ既定色のページが暗く描画される | `samples/maui/KsSettingsView.Sample.Maui/SampleAppearanceStore.cs:40-46` (`UserAppTheme`)、`MenuPage.cs:28-32,60-91,121-127`、`SampleAppearance.cs:52-75`、`App.cs:23` | `ui/verification/maui-ios-menu-dark.png` / `maui-android-menu-dark.png`、`maui-ios-visibility-dark.png` / `maui-android-visibility-dark.png` | ⚠️ deviation 記録済み (1 項目目) |
| 外観の切り替え / システムに戻すと端末の外観に従う | `SampleAppearance.cs:71` (`AppTheme.Unspecified`) | `ui/verification/maui-ios-menu-light.png` / `maui-android-menu-light.png` | ✅ 一致 |
| 外観の切り替え / 選択が再起動後も維持される | `SampleAppearanceStore.cs:25-37` (Preferences) + `App.cs:23` (起動時に保存値を適用) | `ui/verification/maui-ios-menu-dark-relaunch.png` / `maui-android-menu-dark-relaunch.png`、`ui/brief.md:97` (サイクル 3 の Window 保持変更後に再確認) | ✅ 一致 |
| 外観の切り替え / システム選択中に端末の外観が変わると追随する | `UserAppTheme = Unspecified` + `Platforms/Android/MainActivity.cs:26-28,41-56` (`ConfigChanges.UiMode` を受け取り night 変化時に `Recreate()`) | `ui/verification/maui-ios-menu-system-device-dark.png` (再開時反映 — spec の「再開時を含む」に合致) / `maui-android-menu-system-device-dark.png` | ✅ 一致 |
| 外観の切り替え / ナビゲーションバーは対象外 (Requirement 本文の SHALL) | `SampleTheme.cs:150-169` の `Apply` はバー色に触れず、`App.cs:31-37` が固定値を与える | `ui/verification/maui-ios-menu-dark.png` (バーは固定色のまま) | ✅ 一致 |
| Sample Theme の差し替え / ダーク時に dark プリセットで描画される | `SampleTheme.cs:136,150-169,174,189-200`、7 ページの `SampleThemeFollower.Attach` (`Pages/BasicCellsDemoPage.xaml.cs:20,23-27` ほか)、`Pages/BasicCellsDemoPage.xaml:76-77` (ButtonCell の色はコードビハインド経路へ移動) | `ui/verification/maui-ios-basic-cells-dark-1.png` / `-2.png`、`maui-android-basic-cells-dark-1.png` / `-2.png`、`maui-ios-input-cells-dark.png` / `maui-android-input-cells-dark.png`、`maui-ios-section-decoration-dark.png` / `maui-android-section-decoration-dark.png` | ✅ 一致 |
| Sample Theme の差し替え / 外観が切り替わったとき表示中のページも追随する (Requirement 本文の SHALL) | `SampleThemeFollower.cs:48-73` (`RequestedThemeChanged` 購読)、MAUI Android は `Platforms/Android/MainActivity.cs:41-53` と `App.cs:20,26-27` (Window ごと再利用して作り直しをまたぐ) | `ui/verification/maui-android-basic-cells-system-device-dark.png` (同じデモに留まり dark へ切替、行タップ状態も保持)、`maui-android-input-cells-system-device-toggle-x3.png` (3 往復でページ・入力状態・プリセットが維持)、`ui/brief.md:84-97` | ✅ 一致 (サイクル 3 で実装を差し替え、証跡 1 枚追加) |
| Sample Theme の差し替え / ライト時は従来どおり light プリセットで描画される | `SampleTheme.cs:150-169` の `dark == false` 経路 (`CellValueTextColor` / `CellDescriptionColor` へ `null` = 未指定)、`:174` (`MauiTitleText(false)` = `MauiHeaderText`) | `ui/verification/maui-ios-basic-cells-light.png` / `maui-android-basic-cells-light.png` | ✅ 一致 |
| Sample Theme の差し替え / dark 側は description と valueText も明示、色値は iOS / Android と同一 RGBA (Requirement 本文の SHALL) | `SampleTheme.cs:56-80,167-168` | 下記「色値の検算」 | ✅ 一致 |
| カレンダー範囲デモ / 範囲外の日付は選択できない | `ViewModels/InputCellsDemoViewModel.cs:286-294`、`Pages/InputCellsDemoPage.xaml:132-133` (`ViewModels/InputCellsDemoViewModel.cs:37` の初期値 は 2026/06/01 のまま) | `ui/verification/maui-ios-calendar-dark-range.png` / `maui-android-calendar-dark-range.png` | ✅ 一致 |
| カレンダー範囲デモ / 今日が範囲外なら今日ジャンプは選択状態を変えない | 同上 (本体挙動) | 同上 + `ui/brief.md:64` (「今日」は選択も月表示も変えない)、tasks 5.3 | ✅ 一致 |

## 色値の検算 (3 面の dark プリセット)

| 色ロール | iOS `SampleTheme.swift` | Android `SampleTheme.kt` | MAUI `SampleTheme.cs` | 一致 |
|---|---|---|---|---|
| 下地 (view / header / footer 背景) | `:47` `#1B1915` | `:62` `0xFF1B1915` | `:56` `#1B1915` | ✅ |
| Cell 背景 | `:49` `#2A2620` | `:65` `0xFF2A2620` | `:59` `#2A2620` | ✅ |
| separator | `:51` `#4A3F28` | `:68` `0xFF4A3F28` | `:62` `#4A3F28` | ✅ |
| header 文字 / ButtonCell title | `:53` `#E0B040` | `:71` `0xFFE0B040` | `:65` `#E0B040` | ✅ |
| footer 文字 | `:55` `#9A948A` | `:74` `0xFF9A948A` | `:68` `#9A948A` | ✅ |
| disabled 文字 | `:57` `#7A756C` | `:77` `0xFF7A756C` | `:71` `#7A756C` | ✅ |
| Cell title | `:59` `#E6E1D6` | `:80` `0xFFE6E1D6` | `:74` `#E6E1D6` | ✅ |
| valueText | `:61` `#B8B2A6` | `:83` `0xFFB8B2A6` | `:77` `#B8B2A6` | ✅ |
| description | `:63` `#9A948A` | `:86` `0xFF9A948A` | `:80` `#9A948A` | ✅ |
| accent / selected (light と共有) | `:28,30` `#FFBF00` / `#50FFBF00` | light 側と同一定数を `mauiDark` へ渡す (`:206-207`) | `:150-169` の `Apply` が light / dark 共通で `MauiSelected` / `MauiAccent` を渡す | ✅ |

`ui/brief.md:28` が宣言するとおり、色値の正は `ui/mock/approved.png` 内の色ロール対応表であり、`ui/brief.md:37`・`:48` に写した RGBA と上表は一致する。

## 追加検査

| 検査項目 | 結果 |
|---|---|
| tasks.md の全タスク完了 | 15 タスクすべて `[x]`。対応表と突き合わせて未実装の虚偽チェックは無し。5.5 が宣言する 4 ビルド + lint 0 件は本検証で全件再実行して成立を確認 |
| 逆流検査 (足場の書き換え) | `proposal.md` (09-05 10:26) / `specs/samples-ios/spec.md` (10:22) / `specs/samples-android/spec.md` (10:26) / `specs/samples-maui/spec.md` (10:22) / `ui/mock/approved.png` (10:22) / `ui/mock/plan-a.html` (10:22) は実装着手 (12:03 以降) より前で止まっている。更新されているのは記録側の `tasks.md` (12:23) / `deviation.md` (11:53) / `ui/brief.md` (13:59) と証跡ファイルのみ |
| 未記録乖離 | ❌ 0 件のため無し。diff にあって Scenario に対応しない変更は `ios/Sources/KsSettingsViewUI/PresentationAppearance.swift` (新規) と `PickerCellView.swift:96-110,130-133` / `DatePickerCellView.swift:113-142,238-242` の 3 ファイルだけで、いずれも deviation.md 2 項目目が宣言する範囲。`ios/Sources` 内の `present(` 呼び出しは `PickerCellView.swift:109` と `DatePickerCellView.swift:118` の 2 箇所のみで、どちらも `PresentationAppearance.inherit` を通っており「同型の提示を持つ箇所すべて」を満たす |
| 付随修正 | deviation.md に `[付随修正]` 行は無い。記録されている 2 項目はいずれもオーナー裁定の合意差分で、対応表では ⚠️ / 範囲宣言として扱った |
| UI 変更の記録 | `ui/brief.md:24-30` に承認モックの記録 (plan-a.html 採用、approved.png、2026-09-05 オーナー承認、改訂版の再承認) がある。`ui/brief.md:103-105` の「合意済み妥協」は「現時点でなし」と明記済み。照合結果は 4 実行面すべてに節がある |
| テスト全件成功 | iOS 1009 tests / 0 failures (実行して確認)。`samples/` にテストターゲットは無く、Android / MAUI とも本 change はテスト対象コードを触っていないためビルド成立で代替 |

## 所見 (判定に影響しないもの)

- samples-android の Scenario「選択が再起動後も維持される」「システム選択中に端末の外観が変わると追随する」は、4 実行面のうち Android Native だけ `ui/verification/` に対応画像が無く、記録が tasks 5.1 のチェックに留まる。挙動そのものは他の証跡から成立が導けるため ✅ としたが、証跡の形は他 3 面と揃っていない。詳細と推奨は review-003.md の Minor 1 に書いた
- deviation.md 2 項目目で iOS 本体に入った「提示物が提示元の実効外観を引き継ぐ」保証は、どのデルタスペックの Requirement にも対応しない (サンプル 3 能力の外にある本体挙動)。合意済み差分として扱ったが、蒸留で concepts に拾う対象になる
