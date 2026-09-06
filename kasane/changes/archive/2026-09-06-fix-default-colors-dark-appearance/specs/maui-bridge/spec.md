## ADDED Requirements

### Requirement: 未指定色の外観既定への解決

Theme と CellStyle の輸送 DTO で未指定 (null) の色は、native 側の未指定表現 (iOS は既定値、Android は `Color.Unspecified`) へ写され、native の外観既定 (ライト / ダーク) へ解決される (SHALL)。Bridge は未指定の色を固定の既定値で埋めない (SHALL NOT)。明示された色は ARGB のまま native へ渡り、外観で変換されない (SHALL NOT)。DTO の wire 形式 (項目・型・null の意味) は本 change 前と同じである (SHALL)。

#### Scenario: MAUI で色を設定しない SettingsView をダーク外観で表示する
- **GIVEN** 色プロパティを設定しない MAUI の SettingsView と、ダーク外観のアプリ
- **WHEN** 表示する
- **THEN** list 下地・Cell 背景・separator・Header / Footer の文字は native の dark セットの値で描画され、判読できる (iOS / Android 両方)

#### Scenario: 設定した色は外観で変わらない
- **GIVEN** list 下地の色を設定した MAUI の SettingsView
- **WHEN** アプリの外観をダークへ切り替える
- **THEN** list 下地は設定した色のまま描画され、未設定の色だけが dark セットへ変わる

#### Scenario: AppThemeBinding の値が native まで届く
- **GIVEN** list 下地に `AppThemeBinding` (light / dark の値) を設定した MAUI の SettingsView を表示中
- **WHEN** アプリの外観をダークへ切り替える
- **THEN** list 下地は binding の dark 側の値で描き直される (facade → snapshot → bridge → native の Theme 適用まで届く)

#### Scenario: Android bridge は未指定を Unspecified として渡す
- **GIVEN** 色項目がすべて null の Theme DTO
- **WHEN** native の Theme へ変換する
- **THEN** 変換結果の色フィールドはすべて `Color.Unspecified` である

#### Scenario: Android bridge の CellStyle 変換は明示 ARGB を保ち未指定だけ Unspecified にする
- **GIVEN** title 色だけ ARGB を持ち他が null の CellStyle DTO
- **WHEN** native の CellStyle へ変換する
- **THEN** title 色は同じ ARGB の `Color`、他の色フィールドは `Color.Unspecified` である
