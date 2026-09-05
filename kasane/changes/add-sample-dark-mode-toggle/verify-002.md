# 一致検証結果: add-sample-dark-mode-toggle (002 回目)

**日付**: 2026-09-05
**判定**: VALID

デルタスペック 3 枚 (samples-ios / samples-android / samples-maui) の全 Requirement / Scenario に対応する実装と証跡が存在する。deviation.md 記録済みの縮小が 1 項目 (Theme を渡さない画面のダーク描画)、記録済みの本体修正が 1 項目。verify-001 で唯一の所見だった「PickerCell 選択面の証跡なし」は本サイクルで解消し、未記録乖離・虚偽チェック・逆流はいずれも検出していない。

## 実行した客観確認

| 検査 | 結果 |
|---|---|
| iOS 全テスト (`cd ios && xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17 Pro'`) | **TEST SUCCEEDED**。バンドル集計 166 + 88 + 94 + 7 + 654 = **1009 tests / 0 failures** |
| iOS Swift 6 言語モード (`xcodebuild build ... SWIFT_VERSION=6`。`-swift-version 6` の伝播と 126 SwiftCompile を確認) | **error 0 件**。`ios/Package.swift` は無変更 |
| Android Sample (`samples/android` で `./gradlew :app:assembleDebug --offline`) | BUILD SUCCESSFUL |
| MAUI Sample `net10.0-android` | ビルド成功 (警告 0 / エラー 0) |
| MAUI Sample `net10.0-ios` | ビルド成功 (警告 0 / エラー 0) |
| 標準 lint (`local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py`) | いずれも 0 件 (comment-policy は検査対象 771 ファイル) |

`samples/` にテストターゲットは無いため、Scenario の担保は tasks.md 冒頭の宣言どおり 4 実行面 (iOS Native / Android Native / MAUI iOS / MAUI Android) の実機・Simulator 確認と `ui/verification/` の画像 48 枚が担う。

## 対応表

### specs/samples-ios/spec.md

| Requirement / Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| 外観の切り替え / ダークを選ぶとライブラリ既定色の画面が暗く描画される | `samples/ios/KsSettingsViewSample/ContentView.swift:18,24,60`、`samples/ios/KsSettingsViewSample/SampleAppearance.swift:36-42` | `ui/verification/ios-menu-dark.png`、`ios-visibility-dark.png` | ⚠️ deviation 記録済み (1 項目目 — 既定色は追随せず、chrome と Theme 明示画面までが達成範囲) |
| 外観の切り替え / システムに戻すと端末の外観に従う | `SampleAppearance.swift:38` (`.system` → `nil` = 上書きなし) + `ContentView.swift:60` | `ui/verification/ios-menu-light.png` | ✅ 一致 |
| 外観の切り替え / 選択が再起動後も維持される | `ContentView.swift:18` (`@AppStorage(SampleAppearance.storageKey)`) | `ui/verification/ios-menu-dark-relaunch.png` | ✅ 一致 |
| 外観の切り替え / システム選択中に端末の外観が変わると追随する | `ContentView.swift:60` (`preferredColorScheme(nil)`) | `ui/verification/ios-menu-system-device-dark.png` | ✅ 一致 |
| 外観の切り替え / 選択中の項目が識別できる | `ContentView.swift:31-34` (checkmark + `selectedAccessibilityLabel`) | `ui/verification/ios-menu-dark.png` | ✅ 一致 |
| Sample Theme の差し替え / ダーク時に dark プリセットで描画される | `SampleTheme.swift:158-174,179`、`BasicCellsDemoView.swift:167,174`、`InputCellsDemoView.swift:321`、`CustomCellDemoView.swift:239`、`SectionDecorationDemoView.swift:98`、`SectionDecorationPreset.swift:40-55` | `ui/verification/ios-basic-cells-dark-1.png` / `-2.png`、`ios-input-cells-dark.png`、`ios-section-decoration-dark.png` | ✅ 一致 |
| Sample Theme の差し替え / ライト時は従来どおり | `SampleTheme.swift:138`(`mauiLight` は旧 `maui` と同一定義)、`:184`(`mauiTitleText(dark:false)` = `mauiHeaderText` = 旧 `mauiTitleText` #CC9900 と同値) | `ui/verification/ios-basic-cells-light.png` | ✅ 一致 |
| Sample Theme の差し替え / dark 側は description と valueText も明示 | `SampleTheme.swift:172-173` (`cellValueTextColor` / `cellDescriptionColor`) | 承認モック対応表と RGBA 一致 (下記「色値の検算」) | ✅ 一致 |
| カレンダー範囲デモ / 範囲外の日付は選択できない | `InputCellsDemoView.swift:296-297` (2026/06/01–06/20、初期値 `:90` は 2026/06/01 のまま) | `ui/verification/ios-calendar-dark-range.png` | ✅ 一致 |
| カレンダー範囲デモ / 今日が範囲外なら今日ジャンプは選択状態を変えない | 同上 (本体挙動) | 同上 + tasks 5.3 の 4 実行面確認 | ✅ 一致 |

### specs/samples-android/spec.md

| Requirement / Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| 外観の切り替え / ダークを選ぶとライブラリ既定色の画面が暗く描画される | `samples/android/.../MainActivity.kt:54-56,101-106`、`SampleAppearanceStore.kt:49-56`、`MenuScreen.kt:53-76`、`SampleAppearance.kt:14-36` | `ui/verification/android-menu-dark.png`、`android-visibility-dark.png` | ⚠️ deviation 記録済み (1 項目目) |
| 外観の切り替え / システムに戻すと端末の夜間モードに従う | `SampleAppearanceStore.kt:51` (`System` → `null` = 上書きなし) | `ui/verification/android-menu-light.png` | ✅ 一致 |
| 外観の切り替え / 選択が再起動後も維持される | `SampleAppearanceStore.kt:23-38` (SharedPreferences) + `MainActivity.kt:54-56` | tasks 5.1 の 4 実行面確認 | ✅ 一致 |
| 外観の切り替え / システム選択中に端末の外観が変わると追随する | 上書きなし + `AndroidManifest.xml` に `configChanges` 未指定 (uiMode 変更で Activity 再生成 → `attachBaseContext` から解決し直す) | tasks 5.1 の 4 実行面確認 | ✅ 一致 |
| 外観の切り替え / Activity は `ComponentActivity` のまま (android/ADR-0020) | `MainActivity.kt:47`、`AndroidManifest.xml:23` の style は `@android:style/Theme.Material*.NoActionBar` 親 (AppCompat / MaterialComponents 不使用) | `res/values/themes.xml`、`res/values-night/themes.xml` | ✅ 一致 |
| chrome の夜間モード追随 / ダーク時に chrome も暗くなる | `AndroidManifest.xml:23`、`res/values-night/themes.xml`、`MainActivity.kt:143-147` (`SampleAppTheme` の light / dark 分岐) | `ui/verification/android-menu-dark.png` | ✅ 一致 |
| chrome の夜間モード追随 / 選択面のダイアログも実効外観に従う | `MainActivity.kt:54-56` (Activity の Configuration 上書き) | `ui/verification/android-calendar-dark-range.png` (ダーク配色のダイアログ) | ✅ 一致 |
| Sample Theme の差し替え (2 Scenario) | `SampleTheme.kt:202-223,225`、`BasicCellsDemoScreen.kt:46,65,184`、`InputCellsDemoScreen.kt:41,116`、`CustomCellDemoScreen.kt:50,75`、`SectionDecorationDemoScreen.kt:37,59`、`SectionDecorationPreset.kt:36-49` | `ui/verification/android-basic-cells-dark-1.png` / `-2.png` / `android-basic-cells-light.png` | ✅ 一致 |
| Sample Theme の差し替え / 色値は iOS 側と同一 RGBA | `SampleTheme.kt:62-84` ↔ `samples/ios/KsSettingsViewSample/SampleTheme.swift:47-63` | 下記「色値の検算」 | ✅ 一致 |
| カレンダー範囲デモ (2 Scenario) | `InputCellsDemoScreen.kt:324-325` | `ui/verification/android-calendar-dark-range.png` | ✅ 一致 |

### specs/samples-maui/spec.md

| Requirement / Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| 外観の切り替え / ダークを選ぶとライブラリ既定色のページが暗く描画される | `samples/maui/KsSettingsView.Sample.Maui/SampleAppearanceStore.cs:40-46` (`UserAppTheme`)、`MenuPage.cs:32,93-95,123-125`、`SampleAppearance.cs:52-75` | `ui/verification/maui-ios-menu-dark.png` / `maui-android-menu-dark.png`、`maui-ios-visibility-dark.png` / `maui-android-visibility-dark.png` | ⚠️ deviation 記録済み (1 項目目) |
| 外観の切り替え / システムに戻すと端末の外観に従う | `SampleAppearance.cs:71` (`AppTheme.Unspecified`) | `ui/verification/maui-ios-menu-light.png` / `maui-android-menu-light.png` | ✅ 一致 |
| 外観の切り替え / 選択が再起動後も維持される | `SampleAppearanceStore.cs:25-36` (Preferences) + `App.cs:17` | `ui/verification/maui-ios-menu-dark-relaunch.png` / `maui-android-menu-dark-relaunch.png` | ✅ 一致 |
| 外観の切り替え / システム選択中に端末の外観が変わると追随する | `UserAppTheme = Unspecified` + `Platforms/Android/MainActivity.cs:26-28,41-56` (`ConfigChanges.UiMode` を受け取り night 変化時に `Recreate()`) | `ui/verification/maui-ios-menu-system-device-dark.png` (再開時反映 — spec の「再開時を含む」に合致) / `maui-android-menu-system-device-dark.png` | ✅ 一致 |
| 外観の切り替え / ナビゲーションバーは対象外 | `SampleTheme.cs:150-172` の `Apply` はバー色に触れず、`App.cs:32-33` が固定値を与える | `ui/verification/maui-ios-menu-dark.png` (バーは固定色のまま) | ✅ 一致 |
| Sample Theme の差し替え / ダーク時に dark プリセットで描画される | `SampleTheme.cs:136,150-172,174,189-200`、7 ページの `SampleThemeFollower.Attach` (`Pages/BasicCellsDemoPage.xaml.cs:20` ほか) | `ui/verification/maui-ios-basic-cells-dark-1.png` / `-2.png`、`maui-android-basic-cells-dark-1.png` / `-2.png` | ✅ 一致 |
| Sample Theme の差し替え / 外観が切り替わったとき表示中のページも追随する | `SampleThemeFollower.cs:48-73` (`RequestedThemeChanged` 購読) + MAUI Android は `Platforms/Android/MainActivity.cs:41-53` と `App.cs:14,22-23` (同じ `NavigationPage` を作り直した Window へ載せ直す) | `ui/verification/maui-android-basic-cells-system-device-dark.png` (同じ「基本 Cell 7 種デモ」に留まり dark へ切り替わる。行タップ状態「最後にタップ: Tanaka Taro」も保持) | ✅ 一致 (verify-001 以降に実装追加・証跡追加) |
| Sample Theme の差し替え / ライト時は従来どおり | `SampleTheme.cs:150-172` の light 分岐。`CellValueTextColor` / `CellDescriptionColor` へ渡す `null` は `BindableProperty` 既定 `default(Color)` と同値 | `ui/verification/maui-ios-basic-cells-light.png` / `maui-android-basic-cells-light.png` | ✅ 一致 |
| カレンダー範囲デモ (2 Scenario) | `ViewModels/InputCellsDemoViewModel.cs:291,294`、`Pages/InputCellsDemoPage.xaml:132-133` | `ui/verification/maui-ios-calendar-dark-range.png`、`maui-android-calendar-dark-range.png` | ✅ 一致 |

### deviation.md 2 項目目 (iOS 本体の提示外観引き継ぎ)

| 記録内容 | 実装 | 証跡 | 状態 |
|---|---|---|---|
| 提示元と window の外観が食い違うとき提示物へ提示元の外観を与える | `ios/Sources/KsSettingsViewUI/PresentationAppearance.swift:24-47` | `ios/Tests/KsSettingsViewUITests/PresentationAppearanceTests.swift` (8 テスト、本検証で全 pass を確認) | ✅ 一致 |
| 修正範囲は「同型の提示を持つ箇所すべて」 | `ios/Sources/` 内の `present(` 呼び出しは 2 箇所のみで、双方が `PresentationAppearance.inherit` を通る (`PickerCellView.swift:101`、`DatePickerCellView.swift:139`) | 同上 | ✅ 一致 |
| MAUI iOS のカレンダーシート地色の解消 | 同上 | A/B `maui-ios-calendar-dark-range-before.png` → `maui-ios-calendar-dark-range.png`、非回帰 `maui-ios-calendar-system-dark-range.png` / `ios-calendar-dark-range.png` | ✅ 一致 |
| PickerCell 選択面 (もう一方の提示経路) の実機描画 | 同上 | `maui-ios-picker-dark.png` (MAUI ダークで提示物・地色ともダーク) / `ios-picker-dark.png` (iOS Native 非回帰) | ✅ 一致 (**verify-001 の所見は解消**) |
| inputView 経路 (同型かどうかの確認) | 変更なし (症状が出ないため対象外) | `maui-ios-time-picker-dark.png` (ホイール・アクセサリバー・地色ともダーク) | ✅ 一致 (範囲の曖昧さが解消) |

## 色値の検算 (承認モック対応表 ↔ 3 面の定義)

| 色ロール | モック (dark) | iOS `SampleTheme.swift` | Android `SampleTheme.kt` | MAUI `SampleTheme.cs` |
|---|---|---|---|---|
| backgroundColor / header・footerBackground | #1B1915 | `:47` #1B1915 | `:62` 0xFF1B1915 | `MauiDarkViewBackground` #1B1915 |
| cellBackgroundColor | #2A2620 | `:49` #2A2620 | 0xFF2A2620 | #2A2620 |
| separatorColor | #4A3F28 | `:51` #4A3F28 | 0xFF4A3F28 | #4A3F28 |
| cellAccentColor (selectedColor は同色 α 0x50) | #FFBF00 | light と共有 (`mauiAccent` / `mauiSelected`) | 同左 | 同左 |
| headerTextColor / ButtonCell titleColor | #E0B040 | `:53` #E0B040 | 0xFFE0B040 | #E0B040 |
| footerTextColor | #9A948A | `:55` #9A948A | 0xFF9A948A | #9A948A |
| disabledTextColor | #7A756C | `:57` #7A756C | 0xFF7A756C | #7A756C |
| cellTitleColor | #E6E1D6 | `:59` #E6E1D6 | 0xFFE6E1D6 | #E6E1D6 |
| cellValueTextColor | #B8B2A6 | `:61` #B8B2A6 | 0xFFB8B2A6 | #B8B2A6 |
| cellDescriptionColor | #9A948A | `:63` #9A948A | 0xFF9A948A | #9A948A |

3 面すべて承認モックの対応表と一致。light 側は 3 面とも定数値の変更なし。

## 3 面の文言・パラメータ一致 (sample-parity)

| 項目 | iOS | Android | MAUI |
|---|---|---|---|
| 見出し | `SampleAppearance.sectionTitle` = "外観" | `SampleAppearance.SECTION_TITLE` = "外観" | `SampleAppearances.SectionTitle` = "外観" |
| 項目文言 | システム / ライト / ダーク | システム / ライト / ダーク | システム / ライト / ダーク |
| 並び順 | system → light → dark | System → Light → Dark | System → Light → Dark |
| 初期値 | `.system` (`initial`) | `System` (`DEFAULT`) | `SampleAppearance.System` (`Default`) |
| 選択中の読み上げ | `selectedAccessibilityLabel` = "選択中" | `SELECTED_LABEL` = "選択中" | `SelectedLabel` = "選択中" |
| 予約日の範囲 | 2026/06/01–06/20 | 同左 | 同左 |
| 予約日の初期値 | 変更なし (2026/06/01) | 変更なし | 変更なし |

証跡画像 (`ios-menu-dark.png` / `android-menu-dark.png` / `maui-ios-menu-dark.png`) でも見出しと 3 項目・並び順の一致を目視確認した。

## 追加検査

- **tasks.md の虚偽チェック**: なし。1.1〜5.5 の全項目に対応する実装または実行結果を確認した (5.5 のビルド・lint は本検証で 5 種すべて再実行して追認。verify-001 で未実行だった MAUI `net10.0-android` も本検証で成功を確認)
- **逆流検査**: change 配下の成果物はまだ未追跡 (未コミット) のため `git diff` による証明ができない。代わりに更新時刻で確認した — `proposal.md` 10:26 / `specs/samples-{ios,maui}/spec.md` 10:22 / `specs/samples-android/spec.md` 10:26 / `ui/mock/approved.png` 10:22 に対し、実装ファイルの最終更新は 12:03〜13:15、review-001 / verify-001 は 12:39。**実装着手以降に足場 (proposal / specs / 承認モック) が触られていない**ことは示せる。`tasks.md` (12:23) はチェック更新、`deviation.md` (11:53) と `ui/brief.md` (13:25) は記録の追加であり、いずれも足場の書き換えではない
- **未記録乖離**: 対応表に ❌ なし。diff にあって Scenario に対応しない変更は (a) iOS 本体 2 ファイル + `PresentationAppearance.swift` / 同テスト = deviation 2 項目目、(b) `samples/ios/KsSettingsViewSample.xcodeproj/project.pbxproj` の 1 ファイル追加 = 2.1 の付随、(c) `Pages/BasicCellsDemoPage.xaml` の `xmlns:local` 除去と `x:Name` 付与 = 4.2 の付随、(d) `MenuPage.cs` の行モデル導入 = 4.1 の付随。いずれも Requirement の実装に必要な範囲で、独立した機能追加ではない
- **change 外の作業ツリー変更**: `kasane/lessons/inbox/mock-shows-param-not-matching-current-impl.md` の count 加算と `kasane/lessons/inbox/longlived-mechanism-claim-taken-as-scenario-premise.md` の新規、`kasane/changes/fix-default-colors-dark-appearance/exploration.md` の新規 (deviation.md 1 項目目が参照する切り出し先スタブ) がある。いずれも Kasane の教訓捕捉・簡易起票の正規手順であり、本 change のコード成果物ではない
- **UI 変更の記録**: `ui/brief.md` に承認モック (plan-a / approved.png、2026-09-05 承認と改訂後の再承認)、4 実行面の照合結果、「合意済み妥協: なし」「未合意の乖離: なし」、モックとの既知の差分 (「無効なボタン」行はモック側の創作で照合対象外) が記録済み。個人要素の非混入も各面で明記され、Emulator シリアルは `<android-serial>` に置換済み
- **テスト**: iOS 1009 件 / 0 失敗を本検証で実行して確認。`samples/` にテストターゲットは無く、Scenario は 4 実行面の確認と `ui/verification/` の 48 枚で担保

## 所見 (判定には影響しない)

- Android Sample のビルドは `59 actionable tasks: 59 up-to-date` で完了した。現行ソースがそのままの入力ハッシュでコンパイル済みであることは示せるが、本検証で改めてコンパイルが走ったわけではない
- MAUI Android の表示中ページ追随は、`App` が `static` フィールドで `NavigationPage` を保持し Window をまたいで載せ直す形で実現している。Scenario は満たすが、この保持はプロセス寿命であり外観切り替え以外の Activity 再生成経路にも効く (詳細と扱いは review-002.md の Suggestion)
