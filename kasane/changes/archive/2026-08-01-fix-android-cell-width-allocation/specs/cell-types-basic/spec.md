# cell-types-basic Specification 差分 (fix-android-cell-width-allocation)

変更点: 旧要件文の「Android の EntryCell は入力フィールドを accessory 領域に置く既存配置を維持し、本 change の対象外」を撤回する。原典照合の結果、原典 Android は入力フィールドを行内に置いており、accessory 配置は移植時の乖離だったため、Android も行内配置へ揃える (android/ADR-0002)。

## MODIFIED Requirements

### Requirement: Cell 級アクセサリと行内 trailing の 2 系統配置

共通行の trailing 側は 2 系統に区別して配置しなければならない (SHALL):

- **Cell 級アクセサリ**: Cell 種別固有の操作・状態コントロール (SwitchCell の Switch、CheckboxCell の checkbox、RadioCell / SimpleCheckCell の checkmark、CommandCell / Picker 系 4 種の chevron)。セル全体 (title + description を含む) に対して垂直センターに配置する
- **行内 trailing**: valueText と、EntryCell の入力フィールド (両 platform)。title と同じ主行内に配置する

description の表示幅は Cell 級アクセサリの領域と重なってはならない (MUST NOT) — description は Cell 級アクセサリより leading 側で折り返す。

本要件は AiForms.Maui.SettingsView オリジナルの配置 (アクセサリは `AccessoryView` / `Accessory` 相当としてセル縦センター、description はその左まで、EntryCell の入力フィールドは行内) を両 platform で踏襲するものである。

#### Scenario: SwitchCell の description がアクセサリの下に回り込まない

- **GIVEN** `SwitchCell(title: "Notification", description: <折り返しが発生する長文>, isOn: true)`
- **WHEN** 描画される
- **THEN** Switch はセル全体に対して垂直センターに置かれ、description は Switch の領域と重ならない幅で折り返す

#### Scenario: Picker 系は valueText が行内・chevron が Cell 級

- **GIVEN** `PickerCell(title: "Favorites", ...)` で valueText 相当の選択値表示と description を持つ状態
- **WHEN** 描画される
- **THEN** 選択値テキストは title と同じ主行の trailing 側に表示され、chevron はセル全体に対して垂直センターに置かれ、description は chevron の領域と重ならない幅で折り返す

#### Scenario: EntryCell の入力フィールドは行内に置かれる (Android)

- **GIVEN** Android の `EntryCell(title = "Name", ...)`
- **WHEN** 描画される
- **THEN** 入力フィールドは title と同じ主行内で残り領域を使って配置される。Cell 級アクセサリの領域は確保されない

#### Scenario: EntryCell の入力フィールドは行内のまま (iOS — 既存挙動の確認のみ)

- **GIVEN** iOS の `EntryCell(title: "Name", ...)`
- **WHEN** 描画される
- **THEN** 入力フィールドは title と同じ主行内で残り領域を使って配置される (iOS は既存実装が本要件を満たしており、実装変更は行わない — proposal の Non-Goals と整合)
