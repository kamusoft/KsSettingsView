# Delta Spec: settings-view-android-ui — view accessory への Header 固定高さ適用

## ADDED Requirements

### Requirement: Section Header の固定高さは accessory 種別に依らず適用される

Android の Section Header 領域の高さは、accessory が `SectionAccessory.Text` か `SectionAccessory.View` かに依らず、同一の優先順位で解決される (SHALL): `Section.headerHeight` が正値ならその固定高さ (dp)、`-1` (自動) かつ `Theme.headerHeight` が正値なら Theme の固定高さ、いずれも正値でなければ内容に応じた自動高さ。これは iOS の現行挙動 (accessory 種別を見ずに固定高さを適用) との OS 対称化である。

#### Scenario: view accessory + Section.headerHeight 正値で固定高さになる

- **GIVEN** `SectionAccessory.View` を header に持つ Section が `headerHeight` に正値を指定している
- **WHEN** その Section Header が表示される
- **THEN** Header 領域の高さは指定値 (dp) の固定になり、内容がそれより大きい場合ははみ出し分が表示されない (clip)

#### Scenario: view accessory + Theme.headerHeight フォールバック

- **GIVEN** `SectionAccessory.View` を header に持つ Section の `headerHeight` が `-1` (自動) で、`Theme.headerHeight` に正値が設定されている
- **WHEN** その Section Header が表示される
- **THEN** Header 領域の高さは `Theme.headerHeight` の固定になる

#### Scenario: view accessory + Section と Theme が両方正値なら Section が勝つ

- **GIVEN** `SectionAccessory.View` を header に持つ Section の `headerHeight` と `Theme.headerHeight` が互いに異なる正値で設定されている
- **WHEN** その Section Header が表示される
- **THEN** Header 領域の高さは `Section.headerHeight` の値になる (`Theme.headerHeight` ではない)

#### Scenario: view accessory + 高さ未指定は自動高さのまま

- **GIVEN** `SectionAccessory.View` を header に持つ Section の `headerHeight` が `-1` で、`Theme.headerHeight` も未指定 (正値でない)
- **WHEN** その Section Header が表示される
- **THEN** Header 領域は view の内容に応じた自動高さになる (従来挙動の維持)

#### Scenario: 固定高さの Header と自動高さの Header が混在しても互いに影響しない

- **GIVEN** `headerHeight` 正値の Section と高さ未指定 (`headerHeight = -1`、Theme も未指定) の Section が、どちらも view accessory の header を持って同一 list に存在する
- **WHEN** スクロール等で両方の Section Header が (任意の順序・回数で) 表示される
- **THEN** 正値側は常に固定高さ、未指定側は常に自動高さで表示される (表示の順序や再表示によって高さ解決が入れ替わらない)

#### Scenario: Footer の view accessory は高さ指定の対象外

- **GIVEN** `SectionAccessory.View` を footer に持つ Section が `headerHeight` に正値を指定している
- **WHEN** その Section Footer が表示される
- **THEN** Footer 領域は内容に応じた自動高さのまま変わらない (`headerHeight` は Header 専用 — 現行契約の維持)

#### Scenario: text accessory の高さ解決は変更されない

- **GIVEN** `SectionAccessory.Text` を header に持つ Section
- **WHEN** `headerHeight` 正値 / Theme フォールバック / 未指定のそれぞれで表示される
- **THEN** 従来と同一の高さ解決結果になる (本変更による回帰なし)

### Requirement: 表示済み Header の headerHeight 変更は hosted view を維持したまま反映される

view accessory の Section Header が表示された後に `Section.headerHeight` が変更された場合 (`replaceSection` / `Full` 等の更新経由)、新しい高さ解決結果が表示に反映される (SHALL)。このとき accessory が保持する view は再生成されず、view の内部状態は維持される (SHALL) — 高さのみの変更が内容の再バインドを引き起こしてはならない。

#### Scenario: 自動高さ → 固定高さへの動的変更

- **GIVEN** `headerHeight = -1` (自動高さ) で表示中の view accessory の Section Header
- **WHEN** 同じ Section の `headerHeight` を正値に変更する更新が適用される
- **THEN** Header 領域は指定の固定高さへ更新される

#### Scenario: 固定高さ → 自動高さへの動的変更

- **GIVEN** `headerHeight` 正値 (固定高さ) で表示中の view accessory の Section Header
- **WHEN** 同じ Section の `headerHeight` を `-1` に変更する更新が適用される
- **THEN** Header 領域は内容に応じた自動高さへ更新される

#### Scenario: 固定高さの値変更

- **GIVEN** `headerHeight` 正値 A で表示中の view accessory の Section Header
- **WHEN** 同じ Section の `headerHeight` を別の正値 B に変更する更新が適用される
- **THEN** Header 領域は B の固定高さへ更新される

#### Scenario: 高さのみの変更で view の内部状態が維持される

- **GIVEN** 内部状態を持つ view (例: 入力中のテキストを保持する view) を accessory とする Section Header が表示されている
- **WHEN** 同じ Section の `headerHeight` だけを変更する更新が適用される
- **THEN** 高さは更新されるが、accessory の view インスタンスは同一のまま維持され、内部状態 (入力中のテキスト等) は失われない
