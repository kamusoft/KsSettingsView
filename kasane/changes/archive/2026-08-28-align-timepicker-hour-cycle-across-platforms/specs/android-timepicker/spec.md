# Delta: android-timepicker (align-timepicker-hour-cycle-across-platforms)

対象能力: android-timepicker — TimePickerCell の選択面。本デルタは時制の決定源を `format` の `a` 判定から `is24Hour` フラグへ置き換える (core/ADR-0028)。器・確定/破棄・構成変更の契約は relax-android-host-prerequisites で定義済みのものを変更しない。

## MODIFIED Requirements

### Requirement: 時制の決定と候補系列

選択面の時制は cell の `is24Hour` (既定 `true`) で決まる SHALL: `true` は 24 時間制 (時 0–23 / 分 0–59 の2系列)、`false` は 12 時間制 (時 1–12 / 分 0–59 / 午前・午後 の3系列)。`format` 文字列は時制の判定に関与しない SHALL NOT (AM/PM パターン文字 `a` を含む format でも `is24Hour` が `true` なら 24 時間制)。端末の 24 時間設定も参照しない SHALL NOT。午前/午後の表示は端末 Locale の表記から導出し、自前の翻訳文字列を同梱しない SHALL NOT。初期選択は開いた時点の `cell.time` である SHALL。

#### Scenario: 既定は 24 時間制

- **GIVEN** `is24Hour` 既定・`format` 既定 (`"HH:mm"`)、`time = 14:30` の TimePickerCell
- **WHEN** 選択面を開く
- **THEN** 時系列 0–23・分系列 0–59 の2系列で、時 14・分 30 が選択中で提示される

#### Scenario: is24Hour = false は 12 時間制

- **GIVEN** `is24Hour = false`、`time = 14:30` の TimePickerCell
- **WHEN** 選択面を開く
- **THEN** 時 1–12・分・午前/午後の3系列で、時 2・分 30・午後が選択中で提示される

#### Scenario: format の a は時制に影響しない

- **GIVEN** `format = "h:mm a"`、`is24Hour` 既定の TimePickerCell
- **WHEN** 選択面を開く
- **THEN** 24 時間制 (2系列) で提示される

#### Scenario: 12 時間制の深夜と正午の境界

- **GIVEN** `is24Hour = false` の TimePickerCell
- **WHEN** `time = 00:30` で開く / `time = 12:30` で開く
- **THEN** それぞれ「時 12・分 30・午前」/「時 12・分 30・午後」が選択中で提示され、確定するとそれぞれ `LocalTime.of(0, 30)` / `LocalTime.of(12, 30)` で発火する

## ADDED Requirements

### Requirement: Compose DSL の is24Hour 指定

Compose の TwoWay DSL 拡張関数 `TimePickerCell(...)` は `is24Hour` (既定 `true`) を引数に持ち、native cell の `is24Hour` へそのまま透過する (SHALL)。

#### Scenario: DSL の指定が native cell へ透過する

- **GIVEN** DSL で `is24Hour = false` を指定した TimePickerCell
- **WHEN** DSL 評価で native cell を生成する
- **THEN** native cell の `is24Hour` は `false` である
