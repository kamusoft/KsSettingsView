## ADDED Requirements

### Requirement: MAUI Sample は XAML の AppThemeBinding だけで外観に追随する
MAUI Sample のデモ画面のうち Sample 共通配色 (`SampleTheme`) を SettingsView に適用する 7 画面 (基本 Cell 7 種 / 入力 Cell 5 種 / MAUI 固有 Cell 機能 / accessory View / Section 装飾 / CustomCell / CustomCell の MAUI 固有) は、`SettingsView` の Theme プロパティと Cell の色プロパティの外観 (light / dark) 追随を XAML の `AppThemeBinding` (共有 Style を含む) で表現し、`Application.RequestedThemeChanged` を購読して値を入れ直すコードを持たないこと。light / dark の色の実値は `SampleTheme` の定数のまま (3 platform 同一 RGBA)。

#### Scenario: 表示中のデモ画面で外観を切り替えると SettingsView と Cell の色が追随する
- **GIVEN** 基本 Cell 7 種デモを表示中 (外観 light)
- **WHEN** ルートメニューの外観選択で dark にする
- **THEN** 同じ画面のまま SettingsView の背景・separator・header 文字色と ButtonCell「ログアウト」のタイトル色が dark の値へ描き直される

#### Scenario: Sample に外観変更の購読コードが無い
- **GIVEN** `samples/maui` のソース
- **WHEN** `RequestedThemeChanged` を検索する
- **THEN** 該当がない
