## ADDED Requirements

### Requirement: 表示中の外観変更で未指定色が追随する (MAUI)

`SettingsView` を表示したまま OS の外観 (ライト / ダーク) を切り替えたとき、色プロパティを設定していない (`null` の) 項目のうち、表示中の行の背景・行の文字色・list 下地は、現在の外観の Native のライブラリ既定で描き直される (SHALL)。行の文字色だけが新しい外観へ解決されて行の背景が前の外観のまま残る状態にならない (SHALL NOT)。切替はライト → ダークとダーク → ライトの両方向で、iOS / Android の両 platform で、画面を離れずページ遷移も再生成もしない同じ画面のまま成立する (SHALL)。設定した色の項目は変わらない (SHALL — 本 change 前と同じ)。facade は外観を受け取る型やイベントを持たず、Theme を未指定のまま Native に渡す契約は変えない (SHALL — 本 change 前と同じ)。

#### Scenario: 色を指定しない行を表示中に外観を往復させる (iOS)
- **GIVEN** 行の背景色 (`CellBackgroundColor`) と title 色 (`CellTitleColor`) を設定していない `SettingsView` を MAUI iOS で表示中 (ライト外観で描画済み)
- **WHEN** OS の外観をダークへ切り替え、続けてライトへ戻す
- **THEN** 同じ画面のまま、ダーク時は表示中の行の背景と文字がともに dark 既定の値で描き直されて文字が判読でき、ライトへ戻した後は light 既定の値に戻る

#### Scenario: 色を指定しない行を表示中に外観を往復させる (Android)
- **GIVEN** 行の背景色と title 色を設定していない `SettingsView` を MAUI Android で表示中 (ライト外観で描画済み、Activity は uiMode 変更で再生成されない構成)
- **WHEN** 端末の夜間モードを on にし、続けて off に戻す
- **THEN** 同じ画面のまま、行の背景と文字がともに現在の外観の既定の値で描き直される (本 change 前と同じ挙動の回帰確認)

### Requirement: SettingsView の Theme プロパティに書いた AppThemeBinding は外観変更で Theme の再適用まで届く

`SettingsView` の Theme プロパティ (`CellTitleColor` / `CellValueTextColor` / `CellBackgroundColor` / `HeaderTextColor` 等、画面全体の既定を表す色プロパティ) に XAML の `AppThemeBinding` を書いたとき、アプリの外観変更でそのプロパティは新しい外観側の値へ再評価され、facade のプロパティ変更 → snapshot → bridge DTO → Native の Theme 再適用まで届く (SHALL)。表示中の画面では、同じ画面のまま該当する項目が新しい外観側の値で描き直される (SHALL)。この経路は検証ホスト (`KsSettingsView.MauiHost`) の既定シナリオに常設され、両 platform で表示中の外観切替により確認できる (SHALL)。Cell の色プロパティに書いた `AppThemeBinding` が外観変更で再評価されない現行契約は変えない (SHALL — 本 change 前と同じ。手段は外観変更の購読と再代入)。

#### Scenario: Theme プロパティの AppThemeBinding が表示中の外観切替で描き直される (検証ホスト)
- **GIVEN** `SettingsView` の Theme プロパティ 1 つに light / dark で異なる色の `AppThemeBinding` を書いた検証ホストの設定画面を表示中 (ライト側の色で描画済み)
- **WHEN** OS の外観をダークへ切り替え、続けてライトへ戻す
- **THEN** 同じ画面のまま、そのプロパティが描く項目がダーク時は dark 側、戻した後は light 側の色で描き直される (iOS / Android 両方)。同じ画面の「外観追随ボタン」の title 色 (購読 + 再代入) も従来どおり切り替わる
