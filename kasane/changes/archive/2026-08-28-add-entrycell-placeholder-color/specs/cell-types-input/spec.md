# Delta: cell-types-input (add-entrycell-placeholder-color)

## ADDED Requirements

### Requirement: EntryCell の placeholder 文字色指定

`EntryCell` は placeholder 文字色の任意指定 `placeholderColor` を持つ (SHALL — iOS は `UIColor?`、Android は Compose `Color?`、platform の色型を直接公開する既存方針に従う)。実効色は標準解決順 — Cell 固有値 (`placeholderColor`) → `CellStyle.placeholderColor` → `Theme.cellPlaceholderColor` → platform default — で解決する (SHALL)。どの段にも指定が無ければ platform default (iOS はシステムの placeholder 色、Android はホストテーマの hint 色) で描画し、ライブラリ独自の既定色を持ち込まない (SHALL)。指定色は placeholder の表示にのみ適用し、入力済みテキストの色 (valueText 解決) には影響しない (SHALL)。無効状態の文字色の重ね (disabledTextColor) は入力済みテキストにのみ適用し、明示指定された placeholder 色は有効・無効の状態で変化しない (SHALL)。全段未指定時の platform default は各 OS の既定の見え方 (状態別変化を含む) にそのまま従う (SHALL)。

#### Scenario: Cell 固有値が最優先で適用される
- **GIVEN** `Theme.cellPlaceholderColor` と `CellStyle.placeholderColor` を指定した構成の中の、`placeholderColor` を個別指定した `EntryCell`
- **WHEN** text が空の行を表示する
- **THEN** placeholder は Cell 固有値の色で表示される

#### Scenario: CellStyle → Theme の順で fallback する
- **GIVEN** `placeholderColor` 未指定の `EntryCell` と、`placeholderColor` を指定した `CellStyle`、別の色を指定した `Theme.cellPlaceholderColor`
- **WHEN** text が空の行を表示する
- **THEN** placeholder は `CellStyle.placeholderColor` の色で表示され、`CellStyle` 側の指定を外すと `Theme.cellPlaceholderColor` の色になる

#### Scenario: 全段未指定なら platform default になる
- **GIVEN** どの段にも placeholder 色を指定していない `EntryCell`
- **WHEN** text が空の行を表示する
- **THEN** placeholder は platform default の色で表示される (現行と同じ見た目)

#### Scenario: 入力済みテキストの色には影響しない
- **GIVEN** `placeholderColor` を指定した `EntryCell`
- **WHEN** 文字を入力する
- **THEN** 入力テキストは valueText の解決色で表示され、placeholder 色は適用されない
