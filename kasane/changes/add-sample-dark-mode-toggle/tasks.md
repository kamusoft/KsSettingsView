# Tasks: add-sample-dark-mode-toggle

samples/ には単体テストの対象 (テストターゲット) が無いため、デルタスペックの Scenario は「5. 検証」の実機 / シミュレータ確認で担保する (前例: align-timepicker-hour-cycle-across-platforms の 5.4)。

## 1. 共通 (3 面の定義同期)

- [x] 1.1 dark プリセットの色値を承認モック (ui/mock/approved.png の色ロール対応表) から確定し、3 面の `SampleTheme` に同一 RGBA を定義する (`maui` の対、Section 装飾デモ下地の対。dark 側は description / valueText の色も含む) (→ Requirement: 外観に応じた Sample Theme の差し替え [samples-ios / samples-android / samples-maui])
- [x] 1.2 ルートメニューの「外観」見出しと「システム / ライト / ダーク」の文言を 3 面の画面定義 (`SampleScreen` 相当の一元定義) に置き、一字一句一致させる (→ Requirement: 外観の切り替え [3 面])

## 2. samples/ios

- [x] 2.1 ルートメニューに「外観」の項目群を追加し、選択をアプリ全体の外観へ反映する。選択は永続化し、初回は「システム」 (→ Requirement: 外観の切り替え [samples-ios] 全 Scenario)
- [x] 2.2 `SampleTheme.maui` / `sectionDecorationDemo` を渡す画面で、実効外観に応じて light / dark 側を選ぶ (→ Requirement: 外観に応じた Sample Theme の差し替え [samples-ios] 全 Scenario)
- [x] 2.3 「予約日」に固定の `minDate` / `maxDate` を指定する (→ Requirement: カレンダー型 DatePickerCell の範囲デモ [samples-ios])

## 3. samples/android

- [x] 3.0 spike: `ComponentActivity.attachBaseContext` で `applyOverrideConfiguration` に uiMode (night yes/no) を与え、`values-night/` リソース・Compose の `isSystemInDarkTheme()`・ライブラリ UI (Cell 行と予約日の選択面) がすべて上書き側で解決されることを Emulator で確認する。崩れる場合は実装を止めて報告する (→ Requirement: 外観の切り替え / サンプル chrome の夜間モード追随 の前提)
- [x] 3.1 ルートメニューに「外観」の項目群を追加し、選択を SharedPreferences に保存して `recreate()` で反映する。「システム」は上書きなし (→ Requirement: 外観の切り替え [samples-android] 全 Scenario)
- [x] 3.2 サンプル chrome を夜間モードに追随させる。Manifest テーマは `values/` と `values-night/` の同名 style で framework テーマ (`Theme.Material.Light.NoActionBar` / `Theme.Material.NoActionBar`) を切り替え、AppCompat / MaterialComponents の XML テーマは使わない (ADR-0020 の「素のテーマでも動く」検証条件を保つ)。Compose の `MaterialTheme` は実効外観で light / dark を分岐 (→ Requirement: サンプル chrome の夜間モード追随)
- [x] 3.3 `SampleTheme.maui` / `sectionDecorationDemo` を渡す画面で、実効外観に応じて light / dark 側を選ぶ (→ Requirement: 外観に応じた Sample Theme の差し替え [samples-android] 全 Scenario)
- [x] 3.4 「予約日」に固定の `minDate` / `maxDate` を指定する (→ Requirement: カレンダー型 DatePickerCell の範囲デモ [samples-android])

## 4. samples/maui

- [x] 4.1 ルートメニューに「外観」の項目群を追加し、`Application.UserAppTheme` で反映する。選択は Preferences に永続化し、初回は「システム」。ナビゲーションバーの固定色は変えない (spec の対象外宣言どおり) (→ Requirement: 外観の切り替え [samples-maui] 全 Scenario)
- [x] 4.2 `SampleTheme.Apply` / `ApplySectionDecorationDemo` を実効外観で light / dark に分岐させ、`RequestedThemeChanged` で表示中ページも追随させる (→ Requirement: 外観に応じた Sample Theme の差し替え [samples-maui] 全 Scenario)
- [x] 4.3 「予約日」に固定の `MinimumDate` / `MaximumDate` を指定する (→ Requirement: カレンダー型 DatePickerCell の範囲デモ [samples-maui])

## 5. 検証

実行面は 4 つ (iOS Native / Android Native / MAUI iOS / MAUI Android)。5.1〜5.3 は各実行面で行い、結果を実行面 × 確認項目の表で報告する。

- [x] 5.1 4 実行面で外観の切り替え・再起動後の維持・システムへの復帰・システム選択中の端末外観変更への追随を実機 / シミュレータで確認する (→ Requirement: 外観の切り替え 全 Scenario)
- [x] 5.2 4 実行面で基本 Cell 7 種デモ (dark プリセット) と isVisible デモ (ライブラリ既定色) のダーク描画を撮影し、approved.png と視覚照合する。結果を ui/brief.md に記録し、最終周の画像を ui/verification/ に置く (→ Requirement: 外観に応じた Sample Theme の差し替え / サンプル chrome の夜間モード追随)
- [x] 5.3 4 実行面で「予約日」の選択面を開き、範囲外 disabled と今日ジャンプの無変更を確認する (→ Requirement: カレンダー型 DatePickerCell の範囲デモ 全 Scenario)
- [x] 5.4 Android Native で「予約日」の選択面がダーク選択時にダーク配色で提示されることを確認する (→ Requirement: サンプル chrome の夜間モード追随 Scenario「選択面のダイアログも実効外観に従う」)
- [x] 5.5 iOS Sample / Android Sample / MAUI Sample (両 TFM) のビルドが通ること、標準 lint (`scripts/` の lint 群、comment-policy を含む) が 0 件であることを確認する (→ 全 Requirement の前提)
