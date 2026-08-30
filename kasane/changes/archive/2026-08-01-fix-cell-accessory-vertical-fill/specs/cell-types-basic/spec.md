# cell-types-basic Specification 差分 (fix-cell-accessory-vertical-fill)

## ADDED Requirements

### Requirement: Cell 級アクセサリと行内 trailing の 2 系統配置

共通行の trailing 側は 2 系統に区別して配置しなければならない (SHALL):

- **Cell 級アクセサリ**: Cell 種別固有の操作・状態コントロール (SwitchCell の Switch、CheckboxCell の checkbox、RadioCell / SimpleCheckCell の checkmark、CommandCell / Picker 系 4 種の chevron)。セル全体 (title + description を含む) に対して垂直センターに配置する
- **行内 trailing**: valueText と、iOS における EntryCell の入力フィールド。title と同じ主行内に配置する (Android の EntryCell は入力フィールドを accessory 領域に置く既存配置を維持し、本 change の対象外)

description の表示幅は Cell 級アクセサリの領域と重なってはならない (MUST NOT) — description は Cell 級アクセサリより leading 側で折り返す。

本要件は AiForms.Maui.SettingsView オリジナルの配置 (アクセサリは `AccessoryView` / `Accessory` 相当としてセル縦センター、description はその左まで) を両 platform で踏襲するものである。Android は既存実装が本要件を満たしており、iOS が追随する。

#### Scenario: SwitchCell の description がアクセサリの下に回り込まない

- **GIVEN** `SwitchCell(title: "Notification", description: <折り返しが発生する長文>, isOn: true)`
- **WHEN** 描画される
- **THEN** Switch はセル全体に対して垂直センターに置かれ、description は Switch の領域と重ならない幅で折り返す

#### Scenario: Picker 系は valueText が行内・chevron が Cell 級

- **GIVEN** `PickerCell(title: "Favorites", ...)` で valueText 相当の選択値表示と description を持つ状態
- **WHEN** 描画される
- **THEN** 選択値テキストは title と同じ主行の trailing 側に表示され、chevron はセル全体に対して垂直センターに置かれ、description は chevron の領域と重ならない幅で折り返す

#### Scenario: EntryCell の入力フィールドは行内のまま (iOS)

- **GIVEN** iOS の `EntryCell(title: "Name", ...)`
- **WHEN** 描画される
- **THEN** 入力フィールドは title と同じ主行内で残り領域を使って配置される。Cell 級アクセサリの領域は確保されない

#### Scenario: Android は既存構造で本要件を満たす

- **GIVEN** Android の `SwitchCell(title = "Notification", description = <長文>, isOn = true)`
- **WHEN** 描画される
- **THEN** description の end は accessory 領域の start までに制限され、Switch は縦センターに置かれる (既存実装の挙動を本要件の契約として明文化する。実装変更は不要)
