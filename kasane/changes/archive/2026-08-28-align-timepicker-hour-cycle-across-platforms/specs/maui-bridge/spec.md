# Delta: maui-bridge (align-timepicker-hour-cycle-across-platforms)

## ADDED Requirements

### Requirement: 時制フラグの輸送

時刻 Cell の輸送 (`KsTimePickerCellSnapshot` および両 OS の TimePicker bridge DTO) は `is24Hour` を運ぶ (SHALL)。bridge DTO (Compose / SwiftUI から直接使う宣言 Cell) の `is24Hour` は未指定 (null) を許し、null は native 既定 (24時間制) として写す (SHALL — `format` の null 透過と同じ規則)。指定値は native `TimePickerCell` の `is24Hour` へそのまま写る (SHALL)。iOS の binding assembly (`ApiDefinition` の `KsBridgeTimePickerCell`) は `is24Hour` を C# 側へ露出し、gateway から設定可能である (SHALL)。

#### Scenario: 指定値が native cell へ写る
- **GIVEN** `is24Hour = false` を設定した TimePicker bridge DTO
- **WHEN** native `TimePickerCell` へ resolve する
- **THEN** native cell の `is24Hour` は `false` である (両 OS)

#### Scenario: 未指定は native 既定に落ちる
- **GIVEN** `is24Hour` を null にした TimePicker bridge DTO
- **WHEN** native `TimePickerCell` へ resolve する
- **THEN** native cell の `is24Hour` は既定 `true` である (両 OS)

#### Scenario: MAUI facade の値が gateway を透過する
- **GIVEN** `Is24Hour = false` の facade `TimePickerCell` を含む構成
- **WHEN** gateway が snapshot を bridge DTO へ変換する
- **THEN** 両 OS の TimePicker bridge DTO の `is24Hour` は `false` である
