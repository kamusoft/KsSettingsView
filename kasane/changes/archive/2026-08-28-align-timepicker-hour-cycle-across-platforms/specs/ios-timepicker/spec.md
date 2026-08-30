# Delta: ios-timepicker (align-timepicker-hour-cycle-across-platforms)

対象能力: ios-timepicker — TimePickerCell の埋め込み時刻 picker。本デルタは時制の決定源を端末設定から `is24Hour` フラグへ置き換える (core/ADR-0028)。確定値の反映 (元の `cell.time` の年月日を保持して hour/minute のみ差し替える) と valueText の解決は現行契約を変更しない。

## ADDED Requirements

### Requirement: 時制の決定 (端末設定非依存)

埋め込み時刻 picker の時制は cell の `is24Hour` (既定 `true`) で決まる SHALL: `true` は 24 時間制 (時 0–23)、`false` は 12 時間制 (時 1–12 と午前/午後の区分)。端末の地域・24時間表示設定は picker の時制に影響しない SHALL NOT。`format` は行の valueText の文字列化にのみ効き、picker の時制に関与しない SHALL NOT。時制の強制は hour cycle にのみ作用し、午前/午後などの表記の言語・地域は端末 Locale 由来を保つ SHALL (表記言語まで固定 Locale に変える実装は不合格)。午前/午後の表記は自前の翻訳文字列を同梱しない SHALL NOT。

#### Scenario: 既定は 24 時間制

- **GIVEN** `is24Hour` 既定の TimePickerCell (端末の 24 時間表示設定はオフ = 12時間表示の環境を含む)
- **WHEN** 行の picker を表示する
- **THEN** picker は 24 時間制で提示される

#### Scenario: is24Hour = false は 12 時間制

- **GIVEN** `is24Hour = false`、`time = 14:30` の TimePickerCell (端末の 24 時間表示設定はオンの環境を含む)
- **WHEN** 行の picker を表示する
- **THEN** picker は 12 時間制で提示され、午後 2:30 が選択中である

#### Scenario: 表記の言語は端末 Locale を保つ

- **GIVEN** `is24Hour = false` の TimePickerCell と、英語以外 (日本語等) の端末 Locale
- **WHEN** 行の picker を表示する
- **THEN** 午前/午後の表記は端末 Locale の言語の表記であり、時制の強制によって表記言語が変わらない

#### Scenario: 12 時間制でも確定値の往復が保たれる

- **GIVEN** `is24Hour = false`、`time = 14:30` の TimePickerCell
- **WHEN** picker で午後 2 時 45 分を選択して確定する
- **THEN** `onValueChanged` は 14:45 (元の `time` の年月日を保持した Date) で発火する
