# settings-view-android-ui Specification 差分 (fix-android-cell-width-allocation)

## ADDED Requirements

### Requirement: 共通行の主行幅配分

Android の共通行は、title と行内 trailing (valueText、または EntryCell の入力フィールド) が同じ主行で行幅を分け合い、次の配分を保証しなければならない (SHALL):

- **既定 (valueText 系)**: valueText はコンテンツ幅 (行幅を上限とし、超過分は末尾省略)。title が主行の残り幅を占め、収まらない場合は末尾省略で切り詰める。
- **EntryCell**: title はコンテンツ幅。入力フィールドが主行の残り幅全体を占める。
- **行内 trailing がない場合**: title が主行の全幅を使える。

いずれの場合も、title と行内 trailing は互いの表示領域に重なってはならない (MUST NOT)。この配分は移植元 AiForms.SettingsView の Android 実装と同型である (android/ADR-0002)。

#### Scenario: EntryCell の入力フィールドが残り幅全体を占める

- **GIVEN** Android の `EntryCell(title = "名前", text = <入力フィールドの表示幅を超える長文>)`
- **WHEN** 描画される
- **THEN** 入力フィールドの表示領域は主行のうち title (コンテンツ幅) を除いた残り幅全体に広がり、title と重ならない

#### Scenario: パスワード入力でも同じ配分になる

- **GIVEN** Android の `EntryCell(title = "パスワード", text = <長い値>, isPassword = true)`
- **WHEN** 描画される
- **THEN** 入力フィールドの表示領域は title を除いた主行の残り幅全体に広がる

#### Scenario: 入力フィールドの幅が固定の最低幅に依存しない

- **GIVEN** Android の `EntryCell(title = <主行の大半を占める長い title>, text = "abc")`
- **WHEN** 描画される
- **THEN** 入力フィールドの表示幅は「主行幅 − title のコンテンツ幅」(下限 0) に一致し、固定最低幅による title の押し出しが発生しない。title が主行幅を使い切る場合、入力フィールドの表示幅が 0 になることは原典同型の挙動として許容する (承認済み mock どおり)

#### Scenario: 行内 trailing がない場合は title が全幅を使う

- **GIVEN** Android の valueText なし・入力フィールドなしの Cell (例: `CommandCell(title = <長い title>)`)
- **WHEN** 描画される
- **THEN** title は主行の全幅 (icon・Cell 級アクセサリを除く) を表示領域として使える

#### Scenario: valueText はコンテンツ幅で title が残り幅を占める

- **GIVEN** Android の `LabelCell(title = <折り返しが必要な長い title>, valueText = "Green")`
- **WHEN** 描画される
- **THEN** valueText はコンテンツ幅で全文表示され、title は valueText と重ならない残り幅内で末尾省略される

#### Scenario: 行幅を超える valueText は末尾省略される

- **GIVEN** Android の `LabelCell(title = "短", valueText = <主行幅を超える長文>)`
- **WHEN** 描画される
- **THEN** valueText は主行幅を上限として末尾省略され、行からはみ出さない
