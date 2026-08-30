# Delta Spec: android-theming (ホストテーマ前提の撤廃)

対象能力: android-theming — Android ライブラリ UI のテーマ解決。本デルタは「ホストの XML テーマが `Theme.Material3.*` 派生であること」という利用前提の撤廃後の契約を定義する (android/ADR-0020)。

## ADDED Requirements

### Requirement: ホストテーマ前提の撤廃

ライブラリの全 Cell と選択面は、ホストの XML テーマが Material3 派生であることを要求しない SHALL。最小構成のテーマ・AppCompat 系テーマ・MAUI テンプレート既定テーマ (`Maui.SplashTheme` 系) のいずれのホストでも、例外を出さずに表示され、ライブラリ既定の配色で描画される SHALL。

#### Scenario: 非 Material3 テーマでの表示

- **GIVEN** XML テーマが Material3 派生でないホスト (AppCompat 系または最小テーマ)
- **WHEN** SwitchCell / CheckboxCell を含む全 Cell 種の root を表示する
- **THEN** 例外なく表示され、各 Cell はライブラリ既定の配色で描画される

#### Scenario: MAUI テンプレート既定テーマでの動作

- **GIVEN** MAUI テンプレート既定の MainActivity (`Maui.SplashTheme`、書き換えなし)
- **WHEN** 全 Cell 種を含む設定画面を表示し操作する
- **THEN** 全 Cell が正常に表示・動作する

### Requirement: ホストテーマからの視覚隔離

ホストテーマの属性値は**ライブラリ所有の UI** (標準 Cell の行・chrome・選択面) の配色に影響しない SHALL。ライブラリ UI の見た目の指定は `Theme` / `CellStyle` (ライブラリの公開 styling API) が正である SHALL。ButtonCell タイトル色の既定として従来行っていたホストテーマ `colorPrimary` の動的解決は廃止し、固定の既定色に統一する SHALL (利用者可視の挙動変更)。**利用者所有コンテンツ** (CustomCell の content・`KsAnyView` 経由の利用者 View) は隔離の対象外であり、従来どおりホストの Context (ホストテーマ) で解決される SHALL。

#### Scenario: 利用者所有コンテンツはホストテーマのまま

- **GIVEN** テーマ属性をカスタムしたホストと、テーマ属性を参照する利用者 View を持つ CustomCell
- **WHEN** CustomCell を表示する
- **THEN** 利用者 View の属性はホストテーマの値で解決される (ライブラリの同梱テーマに書き換えられない)

#### Scenario: ホストテーマ色からの隔離

- **GIVEN** Material3 派生テーマでプライマリ色等をカスタムしたホスト
- **WHEN** styling API 未指定のまま Cell を表示する
- **THEN** SwitchCell / シート等の配色はライブラリ既定のまま変わらない (ホストのカスタム色が漏れない)

#### Scenario: ButtonCell タイトル既定色の固定化

- **GIVEN** `colorPrimary` をカスタム定義したホストテーマと、title 色未指定の ButtonCell
- **WHEN** ButtonCell を表示する
- **THEN** タイトル色はライブラリ固定の既定色であり、ホストの `colorPrimary` に追従しない

### Requirement: 選択面のホストテーマ非依存

ボトムシート系選択面 (PickerCell / NumberPickerCell / DatePickerCell (Spinner) / TimePickerCell) およびカレンダーダイアログは、ホストテーマに関わらず提示・操作できる SHALL。

#### Scenario: 非 Material3 テーマでの選択面提示

- **GIVEN** 非 Material3 テーマのホストに配置した NumberPickerCell
- **WHEN** 行をタップする
- **THEN** 選択面が例外なく提示され、確定・キャンセル操作が機能する
