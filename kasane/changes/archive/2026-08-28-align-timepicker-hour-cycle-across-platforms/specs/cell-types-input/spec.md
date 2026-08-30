# Delta: cell-types-input (align-timepicker-hour-cycle-across-platforms)

## ADDED Requirements

### Requirement: TimePickerCell の時制指定 (is24Hour)

`TimePickerCell` は時制指定 `is24Hour` (Bool、既定 `true` = 24時間制) を持つ (SHALL — iOS / Android native と MAUI facade の3面共通)。選択面 (時刻 picker) の時制は `is24Hour` のみで決まる (SHALL)。`format` は行の valueText の文字列化 (表示責務) にのみ効き、選択面の時制には関与しない (SHALL NOT)。端末の地域・24時間表示設定も選択面の時制には関与しない (SHALL NOT)。`format` と `is24Hour` が食い違う指定 (例: AM/PM 表記の format と 24時間制の指定) は検証・補正せず、それぞれの責務でそのまま描画する (SHALL — core/ADR-0028)。`is24Hour` は各面の更新検知 (Cell の等価判定・snapshot 差分判定) に参加し、表示済み Cell の `is24Hour` だけを変更した更新も次回の選択面に反映される (SHALL)。

#### Scenario: 既定は 24時間制
- **GIVEN** `is24Hour` 未指定 (既定) の `TimePickerCell`
- **WHEN** 選択面を開く
- **THEN** 24時間制で提示される (現行既定と同じ挙動)

#### Scenario: is24Hour = false で 12時間制
- **GIVEN** `is24Hour = false` の `TimePickerCell`
- **WHEN** 選択面を開く
- **THEN** 12時間制 (午前/午後の区分を持つ形) で提示される

#### Scenario: format は時制に影響しない
- **GIVEN** `format` に AM/PM 表記 (`"h:mm a"` 相当) を指定し、`is24Hour` は既定のままの `TimePickerCell`
- **WHEN** 行を表示して選択面を開く
- **THEN** 行の valueText は format 通りの AM/PM 表記で表示され、選択面は 24時間制で提示される

#### Scenario: 表示済み Cell の is24Hour 変更が反映される
- **GIVEN** `is24Hour` 既定のまま表示済みの `TimePickerCell`
- **WHEN** 同一 ID のまま `is24Hour = false` だけを変えた内容で更新する
- **THEN** 更新が変化として検知され、次に開く選択面は 12時間制で提示される
