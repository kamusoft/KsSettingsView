# Delta: maui-core (add-entrycell-placeholder-color)

## ADDED Requirements

### Requirement: SettingsView.CellPlaceholderColor (Theme 段)

MAUI の `SettingsView` は Theme 段のプロパティ `CellPlaceholderColor` (BindableProperty、`Color?`、既定 null) を持つ (SHALL — 既存の `CellValueTextColor` / `CellHintTextColor` と同じ Theme プロパティ群の一員)。null は未指定を意味し platform default へ解決を委ねる (SHALL)。表示中の変更は Theme 更新として配信され、表示中の `EntryCell` の placeholder 色へ再適用される (SHALL)。

#### Scenario: Theme 段の色が全 EntryCell に適用される
- **GIVEN** `CellPlaceholderColor` を指定した `SettingsView` と、`PlaceholderColor` 未指定の複数の text 空 `EntryCell`
- **WHEN** 一覧を表示する
- **THEN** すべての `EntryCell` の placeholder が Theme 段の色で表示される

#### Scenario: per-cell 指定が Theme 段より優先される
- **GIVEN** `CellPlaceholderColor` 指定済みの `SettingsView` 内の、`PlaceholderColor` を個別指定した `EntryCell`
- **WHEN** 行を表示する
- **THEN** その行の placeholder は per-cell の色で表示される

#### Scenario: 表示中の Theme 変更が追従する
- **GIVEN** 表示中の `SettingsView`
- **WHEN** `CellPlaceholderColor` を別の色に変更する
- **THEN** 表示中の `EntryCell` の placeholder 色が変わる
