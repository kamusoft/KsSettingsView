# Delta: maui-cells (align-timepicker-hour-cycle-across-platforms)

## ADDED Requirements

### Requirement: TimePickerCell の Is24Hour 指定

MAUI facade の `TimePickerCell` は `Is24Hour` (bool、既定 `true` = 24時間制) の bindable property を持つ (SHALL)。値は snapshot に透過し、native TimePickerCell の `is24Hour` として選択面の時制を決める (SHALL)。`Format` は表示専用のまま変更しない (時制には関与しない SHALL NOT — cell-types-input の3面共通契約に従う)。

#### Scenario: 既定 true が snapshot に透過する
- **GIVEN** `Is24Hour` 未指定の facade `TimePickerCell`
- **WHEN** snapshot を作る
- **THEN** snapshot の `Is24Hour` は `true` である

#### Scenario: false 指定が snapshot に透過する
- **GIVEN** `Is24Hour = false` を指定した facade `TimePickerCell`
- **WHEN** snapshot を作る
- **THEN** snapshot の `Is24Hour` は `false` である

#### Scenario: 表示済み Cell の Is24Hour 変更が再送出される
- **GIVEN** 表示済みの facade `TimePickerCell`
- **WHEN** `Is24Hour` プロパティだけを変更する
- **THEN** snapshot の変化として検知され、native 側へ更新が送出される (次に開く選択面は新しい時制になる)
