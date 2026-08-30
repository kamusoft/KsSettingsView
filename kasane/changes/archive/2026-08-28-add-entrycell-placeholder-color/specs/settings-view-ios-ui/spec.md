# Delta: settings-view-ios-ui (add-entrycell-placeholder-color)

## ADDED Requirements

### Requirement: iOS の placeholder 色描画

iOS の `EntryCell` 描画は、解決済み placeholder 色が指定されているとき placeholder をその色で表示し、未指定のときはプレーンな placeholder 表示 (システム既定色) を維持する (SHALL)。色付き placeholder の font は入力テキストと同じ実効 font (valueText の解決 font) を用い、色指定の有無で placeholder の font が変わらない (SHALL)。`CellStyle.placeholderColor` と `Theme.cellPlaceholderColor` を新設し、実効値解決 (`EffectiveStyle`) は Cell 固有値 → `CellStyle` → `Theme` → platform default の順に従う (SHALL)。行の再利用時に前の行の placeholder 色を持ち越さない (SHALL)。Theme の変更は表示中の行の placeholder 色にも再適用される (SHALL — Theme 更新の既存契約に従う)。placeholder 文字列が nil のときは色指定の有無に関わらず placeholder を表示しない (attributed 表示も作らない) (SHALL)。空文字列は空の placeholder として扱い、色指定があってもクラッシュ・表示崩れを起こさない (SHALL)。

#### Scenario: 指定色で placeholder が表示される
- **GIVEN** placeholder 色を指定した text 空の `EntryCell`
- **WHEN** 行を表示する
- **THEN** placeholder が指定色で表示される

#### Scenario: font 指定と色指定が共存する
- **GIVEN** placeholder 色と valueText font の両方を指定した `EntryCell`
- **WHEN** 行を表示する
- **THEN** placeholder は指定色かつ入力テキストと同じ font で表示される

#### Scenario: 再利用行に色が残らない
- **GIVEN** placeholder 色付きの `EntryCell` と色未指定の `EntryCell` を含む長いリスト
- **WHEN** スクロールで行が再利用される
- **THEN** 色未指定の `EntryCell` の placeholder はシステム既定色で表示される

#### Scenario: Theme 変更が表示中の placeholder に追従する
- **GIVEN** `Theme.cellPlaceholderColor` 由来の色で placeholder を表示中の行
- **WHEN** 別の `cellPlaceholderColor` を持つ Theme を適用する
- **THEN** 表示中の行の placeholder 色が新しい Theme の色に変わる

#### Scenario: placeholder 文字列 nil + 色指定でも安全に描画される
- **GIVEN** placeholder が nil で placeholder 色だけを指定した `EntryCell`
- **WHEN** 行を表示する
- **THEN** placeholder は表示されず、入力欄は通常どおり動作する
