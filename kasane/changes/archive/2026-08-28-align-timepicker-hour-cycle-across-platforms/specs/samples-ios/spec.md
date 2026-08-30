# Delta: samples-ios (align-timepicker-hour-cycle-across-platforms)

## ADDED Requirements

### Requirement: 12時間制デモの常設

iOS サンプルの入力 Cell デモに、`is24Hour = false` と 12時間表記の `format` を指定した TimePickerCell を1行常設する (SHALL — Android の既存デモ行 (title「就寝」・初期値 22:15・pickerTitle「就寝時刻」) を基準に文言・初期値・構成を一致させる。端末設定に依らずデモとして成立する)。

#### Scenario: デモ行で 12時間制の picker が提示される
- **GIVEN** サンプルアプリの入力 Cell デモ画面
- **WHEN** 12時間制デモ行の picker を表示する
- **THEN** 12時間制 (午前/午後の区分あり) の picker が提示され、行の valueText は 12時間表記で表示される
