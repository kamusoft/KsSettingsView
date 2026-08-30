# Delta: maui-cells (add-entrycell-placeholder-color)

## ADDED Requirements

### Requirement: EntryCell.PlaceholderColor facade

MAUI の `EntryCell` は `PlaceholderColor` (BindableProperty、`Color?`、既定 null) を持つ (SHALL — AiForms 互換の per-cell 指定。core の Cell 固有値へ写す)。null は未指定を意味し、`SettingsView.CellPlaceholderColor` → platform default へ解決を委ねる (SHALL)。表示中の `PlaceholderColor` 変更は内容更新として配信され、表示へ反映される (SHALL)。MAUI facade は CellStyle 段の placeholder 色指定を持たない — per-cell の `PlaceholderColor` と重複するため (SHALL NOT)。

#### Scenario: PlaceholderColor が native の placeholder 色に反映される
- **GIVEN** `PlaceholderColor` を指定した text 空の `EntryCell`
- **WHEN** 行を表示する
- **THEN** placeholder が指定色で表示される (iOS / Android とも)

#### Scenario: null は Theme → platform default へ解決される
- **GIVEN** `PlaceholderColor` 未指定 (null) の `EntryCell`
- **WHEN** 行を表示する
- **THEN** `SettingsView.CellPlaceholderColor` があればその色、無ければ platform default で表示される

#### Scenario: 表示中の変更が反映される
- **GIVEN** 表示中の text 空の `EntryCell`
- **WHEN** `PlaceholderColor` を別の色に変更する
- **THEN** 表示中の行の placeholder 色が変わる
