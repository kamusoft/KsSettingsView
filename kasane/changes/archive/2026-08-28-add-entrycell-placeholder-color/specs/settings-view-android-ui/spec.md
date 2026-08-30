# Delta: settings-view-android-ui (add-entrycell-placeholder-color)

## ADDED Requirements

### Requirement: Android の placeholder (hint) 色描画

Android の `EntryCell` 描画は、解決済み placeholder 色が指定されているとき入力欄の hint (placeholder 相当) をその色で表示し、未指定のときはホストテーマの hint 色 (`android:textColorHint`) を**状態別表現 (`ColorStateList`) を含めてそのまま**維持する (SHALL — ライブラリは触らない)。明示色は単色として適用し、有効・無効の状態で変化しない (SHALL)。明示色から未指定へ戻ったとき (再利用行への再バインドを含む) は、明示色適用前のホスト既定の hint 色 (`ColorStateList`) を復元する (SHALL)。`CellStyle.placeholderColor` と `Theme.cellPlaceholderColor` を新設し、実効値解決 (`EffectiveStyle`) は Cell 固有値 → `CellStyle` → `Theme` → platform default の順に従う (SHALL)。同一 Cell への再バインドで変化の無い placeholder 色を再適用しない (SHALL — フォーカス中の再バインドが多い `EntryCell` の既存の差分判定の作法に従う)。Theme の変更は表示中の行の placeholder 色にも再適用される (SHALL)。

#### Scenario: 指定色で placeholder が表示される
- **GIVEN** placeholder 色を指定した text 空の `EntryCell`
- **WHEN** 行を表示する
- **THEN** 入力欄の hint が指定色で表示される

#### Scenario: 再利用行に色が残らない
- **GIVEN** placeholder 色付きの `EntryCell` と色未指定の `EntryCell` を含む長いリスト
- **WHEN** スクロールで行が再利用される
- **THEN** 色未指定の `EntryCell` の hint はホストテーマの hint 色で表示される

#### Scenario: Theme 変更が表示中の placeholder に追従する
- **GIVEN** `Theme.cellPlaceholderColor` 由来の色で hint を表示中の行
- **WHEN** 別の `cellPlaceholderColor` を持つ Theme を適用する
- **THEN** 表示中の行の hint 色が新しい Theme の色に変わる

#### Scenario: 明示色は無効状態でも変わらない
- **GIVEN** placeholder 色を指定した text 空の `EntryCell`
- **WHEN** `isEnabled = false` の行を表示する
- **THEN** hint は指定色のまま表示される (無効の状態重ねを適用しない)

#### Scenario: 明示色から未指定へ戻すとホスト既定の状態別色へ復帰する
- **GIVEN** placeholder 色を指定して表示中の行
- **WHEN** 内容更新で placeholder 色の指定を外す (全段未指定になる)
- **THEN** hint はホストテーマの hint 色 (`ColorStateList` の状態別表現を含む) で表示される

### Requirement: 入力文字色の valueText 解決 (規約乖離の是正)

Android の `EntryCell` の入力済みテキスト色は、valueText の解決順 — `CellStyle.valueTextColor` → `Theme.cellValueTextColor` → `Theme.cellTitleColor` → platform default — で解決する (SHALL — style-resolution の共通規則と iOS 実装に一致させる。現状の `titleColor` 直参照を是正する)。無効状態では従来どおり disabled 文字色を優先する (SHALL)。

#### Scenario: valueText 色の明示指定が入力文字へ適用される
- **GIVEN** `Theme.cellValueTextColor` に title 色と異なる色を指定した構成の `EntryCell`
- **WHEN** 文字を入力した行を表示する
- **THEN** 入力テキストは `cellValueTextColor` の色で表示される (従来は title 色だった)

#### Scenario: valueText 未指定なら従来と同じ見た目になる
- **GIVEN** valueText 色をどの段にも指定していない構成の `EntryCell`
- **WHEN** 文字を入力した行を表示する
- **THEN** 入力テキストは title 色の解決値で表示される (fallback により従来の見た目と一致)

#### Scenario: 無効状態は disabled 文字色が優先される
- **GIVEN** `isEnabled = false` かつ valueText 色を明示指定した `EntryCell`
- **WHEN** 行を表示する
- **THEN** 入力テキストは disabled 文字色で表示される
