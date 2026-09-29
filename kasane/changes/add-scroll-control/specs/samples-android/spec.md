# Delta: samples-android (add-scroll-control)

## ADDED Requirements

### Requirement: スクロール制御デモ画面

Sample に「スクロール制御」デモ画面を追加し、ルートメニューから開けるようにする (SHALL)。画面は設定一覧だけで構成し、操作も一覧の中の Cell で行う (SHALL)。

- 先頭の操作の Section に 4 つの操作を置く: 末尾へ / Section へ / Cell を中央へ / 追加して末尾へ
- 続けて番号付きの Section を複数置く。各 Section に Cell と Footer を持たせる
- 最後の Section に「先頭へ」の操作を置き、Root Footer を付ける

画面タイトル・メニュー項目・操作の文言・Section の構成とデモデータは、samples-ios / samples-android / samples-maui で一字一句一致させる (SHALL、sample-parity)。開いたときに自動でスクロールはしない。ハンドルは画面の Composable が持つ (`rememberScrollController()`)。

#### Scenario: 各操作で対象の位置へ移る
- **GIVEN** スクロール制御デモ画面を開いた
- **WHEN** 「末尾へ」「Section へ」「Cell を中央へ」「先頭へ」をそれぞれ行う
- **THEN** 内容の末尾 (Root Footer)・対象 Section の見出し・対象 Cell (表示範囲の中央)・内容の先頭がそれぞれ表示される

#### Scenario: 追加して末尾へで追加した Cell が見える
- **GIVEN** スクロール制御デモ画面を開いた
- **WHEN** 「追加して末尾へ」を行う
- **THEN** 最後の番号付き Section に Cell が 1 つ増え、内容の末尾までスクロールして増えた Cell が表示される
