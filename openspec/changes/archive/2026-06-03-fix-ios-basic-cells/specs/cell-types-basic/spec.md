## MODIFIED Requirements

### Requirement: CheckboxCell

`CheckboxCell` は ON/OFF をチェックマークで表すセルでなければならない (SHALL)。`title`、`isChecked: Bool` を持ち、タップで toggle し `onValueChanged` を発火しなければならない (MUST)。チェック時のアイコン（accent 表示）は `Theme.cellAccentColor` または `CellStyle.accentColor` で着色されなければならない (MUST)。

iOS では、チェック表現はオリジナル `AiForms.Maui.SettingsView` の `CheckBox`（`UIButton` + `Draw`）相当の **角丸の四角いチェックボックス UI** でなければならない (MUST)。すなわち、角丸（CornerRadius 相当）の四角枠（BorderWidth 相当）を持ち、チェック時は accent カラーで塗りつぶしたうえに白いチェックマークを重ね、非チェック時は枠のみを表示する。このチェックボックスは右端に `UICellAccessory.customView`（`placement: .trailing`）として常設し、チェック状態の切り替えは accessory の追加・削除ではなくカスタム View 内部の再描画で行わなければならない (MUST)（追加・削除に伴うスライドアニメーションを避けるため）。

#### Scenario: チェック状態の表示

- **GIVEN** `CheckboxCell(title: "規約に同意する", isChecked: true)`
- **WHEN** 表示される
- **THEN** 右端に角丸の四角いチェックボックス（accent カラーで塗りつぶし＋白いチェックマーク）が表示される

#### Scenario: 非チェック状態の表示

- **GIVEN** `CheckboxCell(title: "規約に同意する", isChecked: false)`
- **WHEN** 表示される
- **THEN** 右端に角丸の四角い枠のみ（塗りつぶし・チェックマークなし）が表示され、accessory の位置はチェック時と同一である

#### Scenario: タップで toggle

- **GIVEN** `CheckboxCell(isChecked: false, onValueChanged: { value in ... })`
- **WHEN** ユーザーが Cell をタップする
- **THEN** `onValueChanged(true)` が呼ばれ、内部状態が更新されると次回レンダリング時にチェックマークが表示される

### Requirement: RadioCell

`RadioCell` は同一 `groupId` 内で単一選択を行うラジオボタン用セルでなければならない (SHALL)。`title`、`groupId: String`、`value: String`、`selectedValue: String` を持ち、`value == selectedValue` のときチェック表示する (MUST)。タップで `onSelected(value)` を発火し、利用者は `selectedValue` を更新する (MUST)。

iOS では、選択状態のチェックマーク（`checkmark`）は右端に **常設の `UICollectionViewCell` accessory**（`customView` ベース）として配置し、選択状態の切り替えは accessory の追加・削除ではなく `alpha` のフェードで行わなければならない (MUST)。すなわち、非選択 → 選択は位置を変えずにフェードイン、選択 → 非選択は位置を変えずにフェードアウトしなければならない (MUST)（accessory の追加・削除に伴う横スライドアニメーションを生じさせてはならない (MUST NOT)）。

#### Scenario: 選択状態の表示

- **GIVEN** 同じ `groupId = "theme"` を持つ 3 つの RadioCell（value = "light"/"dark"/"auto"、selectedValue = "dark"）
- **WHEN** 表示される
- **THEN** "dark" の RadioCell のみチェック表示される

#### Scenario: 選択切り替え

- **GIVEN** 上記の RadioCell 3 つ、selectedValue = "dark"
- **WHEN** ユーザーが "light" の Cell をタップする
- **THEN** `onSelected("light")` が呼ばれる（実際の selectedValue 更新は SettingsRoot 側の責務）

#### Scenario: 選択解除時のフェードアウト

- **GIVEN** チェック表示中の RadioCell が非選択状態へ更新される
- **WHEN** チェックマークが消える
- **THEN** チェックマークは位置を変えずにその場でフェードアウトする（右方向などへスライドして消えてはならない）

### Requirement: SimpleCheckCell

`SimpleCheckCell` はリスト中の任意項目の選択／非選択を表す単純チェックセルでなければならない (SHALL)。`title`、`isChecked: Bool` を持ち、タップで toggle し `onValueChanged` を発火しなければならない (MUST)。

iOS では、チェック表現はオリジナル `AiForms.Maui.SettingsView` の `SimpleCheckCellView`（`UITableViewCellAccessory.Checkmark`）相当の **右端の checkmark** でなければならない (MUST)。レイアウトは `RadioCell` と同形（タイトル左寄せ・チェック右端）であり、選択状態の切り替えは `RadioCell` と同様に位置を変えない `alpha` のフェードで行わなければならない (MUST)。`CheckboxCell` との違いは、`SimpleCheckCell` がシンプルな checkmark を用いるのに対し、`CheckboxCell` は角丸の四角いチェックボックス UI を用いる点である。

#### Scenario: 右端チェック表示

- **GIVEN** `SimpleCheckCell(title: "通知1", isChecked: true)`
- **WHEN** 表示される
- **THEN** タイトルが左寄せで表示され、右端に checkmark（accent カラー）が表示される（RadioCell と同じレイアウト）

#### Scenario: 非チェック時の表示

- **GIVEN** `SimpleCheckCell(title: "通知1", isChecked: false)`
- **WHEN** 表示される
- **THEN** 右端の checkmark は表示されず（フェードアウト済み）、タイトルのみが表示される

#### Scenario: 選択解除時のフェードアウト

- **GIVEN** チェック表示中の SimpleCheckCell が非選択状態へ更新される
- **WHEN** チェックマークが消える
- **THEN** チェックマークは位置を変えずにその場でフェードアウトする
