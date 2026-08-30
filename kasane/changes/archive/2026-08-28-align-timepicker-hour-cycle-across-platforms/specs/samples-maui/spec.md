# Delta: samples-maui (align-timepicker-hour-cycle-across-platforms)

## ADDED Requirements

### Requirement: 12時間制デモの常設

MAUI サンプルの入力 Cell デモに、`Is24Hour = false` と 12時間表記の `Format` を指定した TimePickerCell を1行常設する (SHALL — Android の既存デモ行 (title「就寝」・初期値 22:15・pickerTitle「就寝時刻」) を基準に文言・初期値・構成を一致させる。端末設定に依らずデモとして成立する)。

#### Scenario: デモ行で 12時間制の選択面が提示される
- **GIVEN** サンプルアプリの入力 Cell デモページ
- **WHEN** 12時間制デモ行の選択面を開く
- **THEN** 12時間制 (午前/午後の区分あり) で提示され、行の表示は 12時間表記である
