## ADDED Requirements

### Requirement: スクロールバー表示と Header / Footer 背景色は native の契約に追随する

`SettingsView.ScrollIndicatorVisible` と `HeaderBackgroundColor` / `FooterBackgroundColor` は、未指定 (null) なら未指定のまま、指定されていればその値のまま native の Theme へ渡る (SHALL)。facade は未指定の値を自前の既定値で埋めない (SHALL NOT)。その結果、MAUI の画面は両 platform の native の契約にそのまま追随する (SHALL):

- `ScrollIndicatorVisible` が未指定または `true` なら、iOS / Android とも設定リストの縦スクロールバーが表示され、`false` ならどちらでも表示されない。表示中にプロパティを変えたときも追従する。PickerCell の選択面の候補リストも同じ値に従い、回転ホイールには表示されない
- `HeaderBackgroundColor` / `FooterBackgroundColor` が未指定なら、iOS / Android とも text の Header / Footer の領域に list 下地がそのまま見える (native の既定が透明であるため — core/ADR-0032)。指定されていれば、iOS / Android とも同じ範囲 (text 形式の Section / Root Header・Footer) がその色で描画される

facade の公開面と bridge の wire 形式は変えない (SHALL)。

#### Scenario: 未指定の値は未指定のまま native へ渡る
- **GIVEN** `ScrollIndicatorVisible` / `HeaderBackgroundColor` / `FooterBackgroundColor` をいずれも設定していない SettingsView
- **WHEN** Theme を native へ渡す
- **THEN** 3 つとも未指定として渡り、facade の既定値で埋められていない

#### Scenario: 指定した値はそのまま native へ渡る
- **GIVEN** `ScrollIndicatorVisible` に `false`、`HeaderBackgroundColor` と `FooterBackgroundColor` にそれぞれ別の色を設定した SettingsView
- **WHEN** Theme を native へ渡す
- **THEN** スクロールバーの非表示と 2 つの色が、設定した値のまま渡る

#### Scenario: 表示中のプロパティ変更が native へ届く
- **GIVEN** 表示中の SettingsView
- **WHEN** `ScrollIndicatorVisible` を `false` へ変更する
- **THEN** 変更後の値を持つ Theme が native へ再適用される

#### Scenario: MAUI の実行面で native と同じ見た目になる
- **GIVEN** iOS と Android の実機またはシミュレータで動く MAUI サンプルの、Theme プロパティを設定していない設定画面
- **WHEN** 表示してスクロールする
- **THEN** どちらの platform でも縦スクロールバーが表示され、text の Header / Footer の領域には list 下地が見える
