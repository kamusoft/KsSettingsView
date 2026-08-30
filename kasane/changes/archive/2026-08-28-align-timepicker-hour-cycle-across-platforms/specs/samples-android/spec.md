# Delta: samples-android (align-timepicker-hour-cycle-across-platforms)

## ADDED Requirements

### Requirement: 12時間制デモの時制明示

Android サンプルの 12時間制デモセル (就寝時刻) は `is24Hour = false` を明示する (SHALL — format の `a` による暗黙判定には依存しない)。行の表示 format は現行の 12時間表記を維持する (SHALL)。文言・構成は 3 platform でパリティを保つ (SHALL)。

#### Scenario: デモ行で 12時間制の選択面が提示される
- **GIVEN** サンプルアプリの入力 Cell デモ画面
- **WHEN** 12時間制デモ行をタップする
- **THEN** 12時間制 (午前/午後の系列あり) の選択面が提示される
