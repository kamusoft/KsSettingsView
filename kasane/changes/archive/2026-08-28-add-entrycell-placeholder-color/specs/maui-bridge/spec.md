# Delta: maui-bridge (add-entrycell-placeholder-color)

## ADDED Requirements

### Requirement: placeholder 色の輸送

Entry の輸送 (`KsEntryCellSnapshot` および両 OS の Entry DTO) は per-cell の placeholder 色を ARGB int の nullable フィールドで運び、Bridge は既存の色変換規則 (`KsBridgeColor` 相当) で native `EntryCell` の Cell 固有値へ写す (SHALL)。Theme の輸送 (`KsThemeSnapshot` および両 OS の `KsBridgeTheme`) は `CellPlaceholderColor` を同形式で運び、native `Theme.cellPlaceholderColor` へ写す (SHALL)。null フィールドは native 側の未指定として写す (SHALL)。CellStyle の輸送には placeholder 色フィールドを追加しない (SHALL NOT — facade が CellStyle 段を持たないため)。

#### Scenario: per-cell 値が native cell へ写る
- **GIVEN** placeholder 色の ARGB int を設定した Entry DTO
- **WHEN** native `EntryCell` へ resolve する
- **THEN** native cell の `placeholderColor` に platform の色型で写る (両 OS)

#### Scenario: Theme 値が native Theme へ写る
- **GIVEN** `CellPlaceholderColor` を設定した `KsBridgeTheme`
- **WHEN** native `Theme` へ resolve する
- **THEN** `Theme.cellPlaceholderColor` に platform の色型で写る (両 OS)

#### Scenario: null は未指定として写る
- **GIVEN** placeholder 色フィールドを null にした Entry DTO と `KsBridgeTheme`
- **WHEN** native へ resolve する
- **THEN** native の `placeholderColor` / `cellPlaceholderColor` は未指定 (nil / null) である
