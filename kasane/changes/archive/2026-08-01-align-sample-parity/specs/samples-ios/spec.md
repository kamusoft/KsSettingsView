# Delta Spec: samples-ios (align-sample-parity)

## ADDED Requirements

### Requirement: ルートメニューのデモ/検証グループ分離
ルートメニューは、デモ画面の項目と platform 固有の技術検証画面の項目を別グループとして表示する SHALL。デモ群は「Store 方式デモ」「DSL 方式デモ」「基本 Cell 7 種デモ」「入力 Cell 5 種デモ」「共通フィールド統合デモ」「isVisible デモ（条件付き非表示）」の6件をこの順で、検証群は「Minimal Diffable 検証」のみを含む。

#### Scenario: メニューのグループ構成
- **GIVEN** Sample アプリを起動する
- **WHEN** ルートメニューが表示される
- **THEN** デモ6項目と「Minimal Diffable 検証」が別グループに分かれて表示される

### Requirement: メニュー文言と画面タイトルの同一性
全メニュー項目の遷移先画面は、メニュー項目と一字一句同一の画面タイトルを表示する SHALL (現在タイトルを持たない Store 方式デモ・DSL 方式デモを含む全7画面)。

#### Scenario: 遷移先タイトルの一致
- **GIVEN** ルートメニュー
- **WHEN** 任意の項目を選択して遷移する
- **THEN** 遷移先画面のタイトルは選択した項目の文言と一字一句一致する

### Requirement: 入力 Cell 5 種デモの様式統一
入力 Cell 5 種デモは、基本 Cell 7 種デモと同一様式の直近イベント表示を持つ SHALL。イベント文言は「最後のイベント: <対象 Cell の title> → <変更後の値>」形式 (初期表示は「最後のイベント: (none)」。値は各 Cell の表示形式に従う — 例: 「名前 → Tanaka」「テーマ → ダーク」「アラーム → 07:45」)。全入力 Cell の値変更で更新され、受け付けられなかった操作 (複数選択の上限超過等) では更新されない。従来の現在値プレビュー領域 (全 Cell の現在値一覧) は表示しない。EntryCell Section の footer には「ニックネーム (callback) は値変更コールバックで状態を更新する経路のデモ。他の入力欄は双方向バインディング経路。」を表示する SHALL。「予約日」DatePickerCell は「誕生日」と独立した状態を持ち SHALL、初期値は 2026/06/01 とする (現行の状態共有を解消)。上記の変更および DatePicker Section の文言中立化 (次 Requirement) を除き、Section 構成 (7 Section・Cell 数・表示文言・パラメータ) は現行を維持する。

#### Scenario: 直近イベント表示への置き換え
- **GIVEN** 入力 Cell 5 種デモを表示する
- **WHEN** いずれかの入力 Cell の値を変更する
- **THEN** 現在値の一覧は表示されず、直近イベント表示が「最後のイベント: <対象 Cell の title> → <変更後の値>」に更新される

#### Scenario: 受け付けられない操作では更新されない
- **GIVEN** PickerCell（複数選択）で既に3件選択済み
- **WHEN** 4件目を選択しようとする
- **THEN** 選択は受け付けられず、直近イベント表示は変化しない

#### Scenario: 日付 Cell の状態独立
- **GIVEN** 入力 Cell 5 種デモを表示する
- **WHEN** 「誕生日」の日付を変更する
- **THEN** 「予約日」の表示値は変化しない

#### Scenario: callback 経路の意図明示
- **GIVEN** 入力 Cell 5 種デモを表示する
- **WHEN** EntryCell Section を見る
- **THEN** footer に callback 経路のデモである旨の説明文言が表示されている

### Requirement: DatePicker Section の文言中立化
入力 Cell 5 種デモの2つの DatePicker Section は、platform API 名・platform 固有機能に依存しない文言とする SHALL。見出しは「DatePickerCell（ホイール）」「DatePickerCell（カレンダー）」、footer は「ホイール形式で日付を選択するデモ。」「カレンダー形式で日付を選択するデモ。」とする (現行の「.wheels」「.calendar」「Toolbar に「今日」ボタン」「iOS カレンダーアプリ風」等の表現を廃止)。

#### Scenario: 見出しと footer の表示
- **GIVEN** 入力 Cell 5 種デモを表示する
- **WHEN** DatePicker の2 Section を見る
- **THEN** 見出しと footer は上記の中立文言であり、platform API 名を含まない

### Requirement: 共通フィールド統合デモの hintText 追随
共通フィールド統合デモの RadioCell「ダーク」は hintText「推奨」を表示する SHALL。当該 Section の header は「RadioCell — accentColor / description / icon / hintText」とする (Android 実装と同一文言)。

#### Scenario: hintText の表示
- **GIVEN** 共通フィールド統合デモを表示する
- **WHEN** RadioCell Section を見る
- **THEN** 「ダーク」の行に hintText「推奨」が表示され、Section header は「RadioCell — accentColor / description / icon / hintText」である

### Requirement: DSL デモの動的 Section 見出しの中立化
DSL 方式デモの動的 Section の見出しは「動的 Section（繰り返し）」とする SHALL (platform API 名を含む見出しを廃止)。

#### Scenario: 見出しの表示
- **GIVEN** DSL 方式デモを表示する
- **WHEN** 動的 Section を見る
- **THEN** 見出しは「動的 Section（繰り返し）」である
