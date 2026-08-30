# Delta Spec: android-timepicker (時刻選択面のホイールシート統一)

対象能力: android-timepicker — TimePickerCell の選択面。本デルタは Material 時計ダイヤルダイアログを廃止し、全ホストでボトムシート + 時分ホイールに統一した後の契約を定義する (android/ADR-0018)。器・ヘッダー・確定/破棄・スナップ静止・アクセシビリティの共通契約は NumberPickerCell の選択面 (kasane/concepts/core/cells/number-picker-selection-surface.md) と同一とする。

## ADDED Requirements

### Requirement: ホスト前提に依存しない時刻選択面の提示

`isEnabled` な TimePickerCell の行タップで、ホスト Activity の型 (`ComponentActivity` を含む) と XML テーマに関わらず時刻選択面 (ボトムシート + ホイール) が提示される SHALL。`isEnabled = false` はタップ無効 SHALL。タイトルは `pickerTitle` があればそれ、なければ `title` で解決する SHALL。従来の Material 時計ダイヤルダイアログは提示しない SHALL NOT。

#### Scenario: ComponentActivity ホストでの提示

- **GIVEN** `ComponentActivity` (FragmentActivity でない) のホストに配置した有効な TimePickerCell
- **WHEN** 行をタップする
- **THEN** 時刻選択面が提示される (何も起きない・例外、のいずれも発生しない)

#### Scenario: 無効 Cell のタップ

- **GIVEN** `isEnabled = false` の TimePickerCell
- **WHEN** 行をタップする
- **THEN** 選択面は提示されない

### Requirement: 時制の決定と候補系列

選択面の時制は、`format` 文字列の**引用符 (`'`) 外**に AM/PM パターン文字 `a` (小文字。`DateTimeFormatter.ofPattern` のパターン文法に従う) を含むか否かで決まる SHALL: 含まない場合は 24 時間制 (時 0–23 / 分 0–59 の2系列)、含む場合は 12 時間制 (時 1–12 / 分 0–59 / 午前・午後 の3系列)。引用リテラル内の `a` と大文字 `A` は判定に影響しない SHALL NOT。午前/午後の表示は端末 Locale の表記から導出し、自前の翻訳文字列を同梱しない SHALL NOT。初期選択は開いた時点の `cell.time` である SHALL。

#### Scenario: 既定 format は 24 時間制

- **GIVEN** `format` 既定 (`"HH:mm"`)、`time = 14:30` の TimePickerCell
- **WHEN** 選択面を開く
- **THEN** 時系列 0–23・分系列 0–59 の2系列で、時 14・分 30 が選択中で提示される

#### Scenario: AM/PM format は 12 時間制

- **GIVEN** `format = "h:mm a"`、`time = 14:30` の TimePickerCell
- **WHEN** 選択面を開く
- **THEN** 時 1–12・分・午前/午後の3系列で、時 2・分 30・午後が選択中で提示される

#### Scenario: 引用リテラル内の a は判定に影響しない

- **GIVEN** `format = "HH:mm 'at'"` の TimePickerCell
- **WHEN** 選択面を開く
- **THEN** 24 時間制 (2系列) で提示される

#### Scenario: 12 時間制の深夜と正午の境界

- **GIVEN** `format = "h:mm a"` の TimePickerCell
- **WHEN** `time = 00:30` で開く / `time = 12:30` で開く
- **THEN** それぞれ「時 12・分 30・午前」/「時 12・分 30・午後」が選択中で提示され、確定するとそれぞれ `LocalTime.of(0, 30)` / `LocalTime.of(12, 30)` で発火する

### Requirement: 確定のみ反映

確定操作で、その時点の選択 (時・分・(12時間制では) 午前/午後) から作った `LocalTime` を引数に `onValueChanged` を1回発火して閉じる SHALL。非確定の閉じ方 (キャンセル・外側タップ・Back・下スワイプ等、器が提供するすべての経路) では発火せず、変更は破棄される SHALL。

#### Scenario: 確定で1回発火

- **GIVEN** 選択面で時 9・分 5 を選択した状態
- **WHEN** 確定操作を行う
- **THEN** `onValueChanged(LocalTime.of(9, 5))` が1回だけ発火し、選択面が閉じる

#### Scenario: 非確定 dismiss は無発火

- **GIVEN** 選択面で選択を変更した状態
- **WHEN** キャンセル (または外側タップ・Back) で閉じる
- **THEN** `onValueChanged` は発火しない

### Requirement: 構成変更で閉じる

時刻選択面は構成変更 (回転等) による Activity 再生成後に再提示されない SHALL (シート系選択面の既存契約と同じ)。このとき `onValueChanged` は発火しない SHALL。

#### Scenario: 回転で閉じて無発火

- **GIVEN** 表示中の時刻選択面
- **WHEN** 画面を回転して Activity が再生成される
- **THEN** 選択面は再提示されず、`onValueChanged` は発火しない

## REMOVED Requirements

### Requirement: Material 時計ダイヤルダイアログの配色契約

**Reason**: 対象 UI (`MaterialTimePicker`) の廃止に伴い、表示後の内部 View 走査による配色契約 (概念文書「入力 Cell」の Android TimePicker ダイアログ配色・android/ADR-0006) は対象を失う。新選択面の配色は既存シート系の styling 解決 (accent 3段解決等) に従う。
