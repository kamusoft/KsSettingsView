# Delta Spec: samples-android (align-sample-parity)

前提: 本デルタの一致対象は Phase 1 完了後の iOS 実装 (samples-ios デルタ適用済み) とする。

## ADDED Requirements

### Requirement: ルートメニューの一覧表示と文言一致
ルートメニューはデモ6項目を一覧形式で表示する SHALL。項目の文言・並び順は iOS ルートメニューのデモ群 (「Store 方式デモ」〜「isVisible デモ（条件付き非表示）」) と一字一句一致させ、デモ群の見出し文言「デモ」も iOS と同一とする。検証グループは持たない (iOS 固有画面のみのための、規約が許容するメニュー構造差)。

#### Scenario: メニューの表示
- **GIVEN** Sample アプリを起動する
- **WHEN** ルートメニューが表示される
- **THEN** デモ6項目が iOS と同一の文言・並び順で一覧表示される

### Requirement: Store 方式デモの表示文言一致
Store 方式デモの全表示文言 (Section の header / footer・Cell の title・操作ボタンの文言・「項目追加」で追加される Cell の文言) を iOS と一字一句一致させる SHALL (現状は iOS「Sample Row 1/2/3」に対し Android「Sample Label 1/2/3」等の不一致がある)。

#### Scenario: 表示文言の一致
- **GIVEN** Store 方式デモを表示する
- **WHEN** iOS の同画面と並置比較する
- **THEN** Section header / footer・Cell title・ボタン文言・追加操作後の Cell 文言が一字一句一致する

### Requirement: 入力 Cell 5 種デモの iOS 一致
入力 Cell 5 種デモは iOS の同画面と以下を一致させる SHALL: Section の数と並び順・各 Section の header / footer 文言・Cell の種類・数・並び順・表示文言 (title / placeholder / hintText)・パラメータとデモデータ (選択肢の文言と数、maxSelectedNumber、min / max / step / unit、初期値・初期選択)・直近イベント表示の文言様式。固有の rootHeader は表示しない。

#### Scenario: 画面構成の一致
- **GIVEN** 入力 Cell 5 種デモを表示する
- **WHEN** iOS の同画面と並置比較する
- **THEN** Section 数・header / footer 文言・Cell の文言・デモデータが一字一句/同値で一致する

#### Scenario: 複数選択の上限
- **GIVEN** PickerCell（複数選択）の選択画面で既に3件選択済み
- **WHEN** 4件目を選択しようとする
- **THEN** 選択は受け付けられない (iOS と同じ上限挙動)

### Requirement: DatePickerCell の表示形式対応
入力 Cell 5 種デモの2つの DatePickerCell は、iOS のホイール形式 / カレンダー形式に視覚的に対応する uiStyle で表示する SHALL (Section の見出し・footer 文言は iOS の中立文言と一字一句一致)。picker 内部の文言・機能の platform 差 (iOS のみの「今日」ボタン等、本体公開 API 差に由来するもの) は deviation.md に記録し本体側の統一課題とする (規約 sample-parity.md の定める手順)。

#### Scenario: 表示形式の対応
- **GIVEN** 入力 Cell 5 種デモを表示する
- **WHEN** 「誕生日」「予約日」の各 DatePickerCell を開く
- **THEN** それぞれ iOS のホイール形式 / カレンダー形式に対応する picker が表示され、対応不能な差分は deviation.md に記録されている

### Requirement: DSL デモの動的 Section 見出しの中立化
DSL 方式デモの動的 Section の見出しは「動的 Section（繰り返し）」とする SHALL (iOS と同一文言)。

#### Scenario: 見出しの表示
- **GIVEN** DSL 方式デモを表示する
- **WHEN** 動的 Section を見る
- **THEN** 見出しは「動的 Section（繰り返し）」である
