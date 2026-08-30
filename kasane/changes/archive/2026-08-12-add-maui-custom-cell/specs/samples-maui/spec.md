# Delta Spec: samples-maui — CustomCell デモ (パリティ + MAUI 固有)

## ADDED Requirements

### Requirement: パリティ画面 CustomCellDemo を native と同一構成で提供する

MAUI サンプルに CustomCellDemo ページを追加し、native (iOS / Android) の既存 CustomCellDemo と**画面タイトル・メニュー文言・Section 構成・Cell 構成・表示文言・デモデータを一致**させる (SHALL — sample-parity 規約に従う。5構成: インライン / ラップ再利用 / 挙動プロパティ2構成 / スクロール耐性の多数行)。ラップ再利用の構成は CustomCell 派生 (または CustomCell を返すヘルパー) で native のラップ関数に対応させる (SHALL)。

#### Scenario: メニューからパリティ画面を開ける

- **GIVEN** サンプルアプリのメニュー画面
- **WHEN** native と同一文言の CustomCellDemo 項目を選択する
- **THEN** native と同一の Section 構成・文言のデモ画面が表示される

#### Scenario: インライン構成の live 更新が動作する

- **GIVEN** CustomCellDemo のインライン構成
- **WHEN** content 内の操作でバインド値を変更する
- **THEN** 同じ行の表示値が即時に更新される

#### Scenario: スクロール耐性構成で表示が混線しない

- **GIVEN** CustomCellDemo のスクロール耐性構成 (同型の CustomCell 多数行)
- **WHEN** リストを末尾まで往復スクロールする
- **THEN** 各行の表示と操作が行ごとに独立したまま維持され、内容の混線が起きない

### Requirement: MAUI 固有の CustomCell デモを別画面で提供する

MAUI 固有の意味論を確認するデモページを、ルートメニューの「MAUI 固有」区分に追加する (SHALL — AccessoryViewsDemoPage と同じ扱い)。提示する内容は以下とする (SHALL): ①`Content` の差し替えと null 遷移 ②`ItemsSource` / `ItemTemplate` から生成された CustomCell 群の独立動作 ③Handler 切断・再接続 (ページ離脱→再訪問) の復元 ④content サイズ変化への行高さ追従。

#### Scenario: 差し替えデモが動作する

- **GIVEN** MAUI 固有デモページ
- **WHEN** 差し替え操作を実行する
- **THEN** 対象行の内容が別の View に置き換わる

#### Scenario: ItemTemplate 生成の行が独立して動作する

- **GIVEN** ItemTemplate から生成された複数の CustomCell 行
- **WHEN** 1つの行の content 内の値を操作する
- **THEN** 操作した行だけが更新され、他の行は変わらない

#### Scenario: 再訪問で復元される

- **GIVEN** MAUI 固有デモページを表示した後、離脱する
- **WHEN** 同じページへ再訪問する
- **THEN** CustomCell の行が Content ごと再表示される

#### Scenario: サイズ変化デモで行高さが追従する

- **GIVEN** 展開/折りたたみ操作を持つ CustomCell 行
- **WHEN** content 内の操作でサイズを変化させる
- **THEN** 行の高さが新しいサイズに追従する
