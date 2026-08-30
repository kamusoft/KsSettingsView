# repository-docs — デルタスペック

## MODIFIED Requirements

### Requirement: Issue テンプレートの必須項目

`.github/ISSUE_TEMPLATE/` に用途別 3 本の Issue Forms を置く SHALL。バグ報告はバージョン・platform・再現手順・実際の挙動・期待した挙動を必須項目とする SHALL。提案は解決したい課題・現状どう困っているか・考えた選択肢を必須項目とする SHALL。質問はバージョン・platform・試したこと・参照した Skill / README の箇所を必須項目とする SHALL。項目のラベルは英語とし、本文を英語・日本語のいずれで書いてもよいことを案内する SHALL。各フォームには対応する GitHub 既定ラベル (`bug` / `enhancement` / `question`) を付与する SHALL。`.github/ISSUE_TEMPLATE/config.yml` で `blank_issues_enabled: false` とし、テンプレートを迂回した Issue 作成をできなくする SHALL。

#### Scenario: 質問の受け口の存在

- **GIVEN** 公開リポジトリの Issue 作成画面
- **WHEN** 提示されるテンプレートの選択肢を読む
- **THEN** バグ報告・提案・質問の 3 本が並び、質問の投稿先がある

#### Scenario: 証拠なしの質問の抑止

- **GIVEN** 質問のフォーム
- **WHEN** 「試したこと」を空のまま送信しようとする
- **THEN** 必須項目が未入力として送信できない

#### Scenario: 証拠なしのバグ報告の抑止

- **GIVEN** バグ報告のフォーム
- **WHEN** 再現手順を空のまま送信しようとする
- **THEN** 必須項目が未入力として送信できない

#### Scenario: テンプレートの迂回不可

- **GIVEN** 公開リポジトリの Issue 作成画面
- **WHEN** テンプレートを使わない空の Issue を作ろうとする
- **THEN** 選択肢として提示されない

#### Scenario: exploration への写像

- **GIVEN** 提案フォームで投稿された Issue
- **WHEN** オーナーが Kasane の change を起こす
- **THEN** 「解決したい課題」と「考えた選択肢」が `exploration.md` の「課題 / 動機」「検討した選択肢」へそのまま移せる

### Requirement: 貢献方針の表明

ルート README は英日とも貢献の節を持ち、外部からの Pull Request を受け付けず Issue で受けること、および Issue テンプレートの利用を案内する SHALL。`.github/CONTRIBUTING.md` (英語) と対応する日本語版を持ち、方針の理由と Issue の書き方を示す SHALL。CONTRIBUTING は用意されている Issue テンプレートの各種別について、何を書けばよいかを示す SHALL。英語版と日本語版は同一の種別を同一の粒度で扱う SHALL。

#### Scenario: 投稿前の到達経路

- **GIVEN** 公開リポジトリで Issue を作成しようとする外部の人
- **WHEN** GitHub が Issue 作成画面に表示する contributing guidelines のリンクをたどる
- **THEN** Pull Request を受け付けない方針と、Issue で受ける旨が読める

#### Scenario: README だけを読む人への到達

- **GIVEN** ルート README しか読まない訪問者
- **WHEN** 貢献の節を読む
- **THEN** PR を受け付けないこと・Issue で受けること・テンプレートを使ってほしいことが分かる

#### Scenario: テンプレート種別の網羅

- **GIVEN** `.github/CONTRIBUTING.md` と `.github/CONTRIBUTING_ja.md`
- **WHEN** Issue の書き方を説明している箇所を読む
- **THEN** バグ報告・提案・質問の 3 種別すべてについて何を書けばよいかが、英日で同じ粒度で示されている

### Requirement: スクリーンショットの提示

ルート README は、iOS と Android のそれぞれについて Modern と Classic の 2 style を示すスクリーンショット計 4 枚を主な特徴の直後に持つ SHALL。英日 README は同一の画像ファイルを参照し、キャプションのみ言語別とする SHALL。画像内の Sample アプリの表示文字列は英語である SHALL。画像はシミュレータ / エミュレータで撮影し、実機の時刻・実際のバッテリー残量・キャリア名・端末名・通知を含まない SHALL。撮影用に固定したデモ表示 (時刻 9:41・満充電を示す電池アイコン等) は許容する SHALL。画像内の表示文字列は切れ・重なり・不自然な折り返しがない SHALL。MAUI については画像を置かず、Native と同じ画面になる旨を文で示す SHALL。4 枚は同一のデモ画面・同一のスクロール位置で撮り、platform と style 以外の差を持たない SHALL。撮影のために `samples/` へ加えた改変は commit しない SHALL。

#### Scenario: 英日での画像共有

- **GIVEN** 英語 README と日本語 README
- **WHEN** 参照している画像ファイルのパスを比較する
- **THEN** 同一のファイルを指しており、異なるのはキャプションの言語だけである

#### Scenario: 4 枚の組み合わせの網羅

- **GIVEN** ルート README が参照するスクリーンショット
- **WHEN** platform (iOS / Android) と style (Modern / Classic) の組み合わせを数える
- **THEN** 4 通りすべてが 1 枚ずつ存在し、いずれも同一のデモ画面・同一のスクロール位置で撮られている

#### Scenario: 英語表示

- **GIVEN** 差し替え後のスクリーンショット 4 枚
- **WHEN** 画面内の Sample アプリの表示文字列を読む
- **THEN** 画面タイトル・Section の header / footer・Cell の表示文字列がすべて英語である

#### Scenario: 端末固有情報の不在

- **GIVEN** 採用したスクリーンショット 4 枚
- **WHEN** ステータスバー領域を目視で確認する
- **THEN** 実機の時刻・実際のバッテリー残量・キャリア名・端末名・通知がなく、写っているのは撮影用に固定したデモ表示だけである

#### Scenario: 表示文字列の可読性

- **GIVEN** 採用したスクリーンショット 4 枚
- **WHEN** 画面タイトル・Section の header / footer・Cell の表示文字列を読む
- **THEN** いずれも切れ・重なり・不自然な折り返しがなく最後まで読める

#### Scenario: Sample の無改変

- **GIVEN** 本変更を適用したあとの作業ツリー
- **WHEN** `samples/` 配下の差分を確認する
- **THEN** 撮影のための英訳を含む差分が 1 件も残っていない
