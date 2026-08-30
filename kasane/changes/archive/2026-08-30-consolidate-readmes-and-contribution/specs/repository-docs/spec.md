# repository-docs

## ADDED Requirements

### Requirement: README の所在

**公開ドキュメント面**に置かれる README は次の 5 枚に限られる SHALL: ルート直下の `README.md` (英語) と `README_ja.md` (日本語)、`skills/README.md` と `skills/README_ja.md` (Skill 索引)、`maui/spike/README.md` (完了済み検証の記録)。platform ディレクトリ (`android/` `maui/`) と Sample ディレクトリ (`samples/ios/` `samples/android/` `samples/maui/`) には README を置かない SHALL。

公開ドキュメント面とは、リポジトリルート直下と `skills/` `android/` `ios/` `maui/` `samples/` の配下を指す SHALL。`kasane/` (変更管理・決定記録・ロードマップとその検証証跡)、`openspec/` (凍結済みの歴史資料)、`.claude/` は本要件の対象外とする SHALL — これらは過去の記録であり、廃止した README への言及を含んでいてよい。

#### Scenario: platform / Sample README の不在

- **GIVEN** 本変更を適用した作業ツリー
- **WHEN** `android/` `maui/` `samples/ios/` `samples/android/` `samples/maui/` の各直下を調べる
- **THEN** いずれにも `README.md` が存在しない

#### Scenario: 公開ドキュメント面の README 集合

- **WHEN** 公開ドキュメント面 (ルート直下と `skills/` `android/` `ios/` `maui/` `samples/` 配下) から `README*.md` を列挙する
- **THEN** ルート 2 枚・`skills/` 索引 2 枚・`maui/spike/README.md` の 5 枚だけが得られる

#### Scenario: 現行文書からの参照の解消

- **GIVEN** 廃止前にルート README や `samples/maui/README.md` から張られていた platform / Sample README へのリンク
- **WHEN** 現行の公開文書 (ルート README 2 枚・`skills/`・`.github/`・`.agents/skills/docs-refresh/SKILL.md`) から `android/README.md` `maui/README.md` `samples/*/README.md` への Markdown リンクを検索する
- **THEN** 解決可能なリンクが存在しない

### Requirement: ルート README の節構成

ルート README は英日とも次の節をこの順で持つ SHALL: 概要と主な特徴 / スクリーンショット / 対応プラットフォーム / インストール / 最小コード例 / Skills / リポジトリ構成 / 貢献 / ライセンス (サードパーティ通知を含む)。開発者向けのビルド手順・環境セットアップ手順・モジュール一覧を持たない SHALL。「リポジトリ構成」節はディレクトリの表と `AGENTS.md` / `kasane/concepts/` へのリンクだけを持つ SHALL。「インストール」節は 3 platform の依存宣言 (配布座標) と prerelease の取得方法だけを持ち、詳細な導入手順 (IDE での追加操作・含まれる module の説明・要件表) は `skills/` に委ねる SHALL。「最小コード例」節は 3 platform ごとに 1 例を置き、各例は対応する platform Skill (`kssettingsview-ios` / `kssettingsview-android` / `kssettingsview-maui`) の `SKILL.md` の最小動作コードブロックと一致する SHALL (AiForms 移行 Skill は対象外)。「インストール」節の prerelease の記述は ecosystem ごとの指定方法を示す SHALL: SwiftPM は prerelease の semver tag を `from:` / `exact:` で指す形、Maven は `X.Y.Z-{alpha|beta|rc}.N` のバージョン指定、NuGet は同バージョン指定と prerelease を含める指定。

#### Scenario: 開発者向け手順の不在

- **GIVEN** ルート README
- **WHEN** 節の内容を調べる
- **THEN** Android SDK ロケーションの設定手順・検証ホストの起動手順・個別モジュールのビルドコマンドに相当する記述がない

#### Scenario: 導入手順の委譲

- **GIVEN** ルート README のインストール節
- **WHEN** 内容を読む
- **THEN** 依存宣言と prerelease の取得方法だけがあり、IDE での追加操作・module 構成の説明・要件表は `skills/` を参照する案内になっている

#### Scenario: 最小コード例の platform 対応

- **GIVEN** ルート README の最小コード例 3 つと、`kssettingsview-{ios,android,maui}` の `SKILL.md` の最小動作コードブロック
- **WHEN** platform ごとに対応する組を比較する
- **THEN** それぞれ一致し、AiForms 移行 Skill のコードは比較対象に含まれない

#### Scenario: prerelease の取得方法

- **GIVEN** インストール節の prerelease の記述
- **WHEN** 3 ecosystem それぞれの記載を読む
- **THEN** SwiftPM / Maven / NuGet それぞれで prerelease を取得する具体的な指定方法が示されている

#### Scenario: サードパーティ通知の所在と誤読防止

- **GIVEN** ルート README のライセンス節
- **WHEN** Material Symbols (Apache 2.0) の通知を読む
- **THEN** サンプルアプリで使用しているアイコンに関する通知であることが明示され、ライブラリ本体の依存と読めない

### Requirement: スクリーンショットの提示

ルート README は、iOS と Android のそれぞれについて Modern と Classic の 2 style を示すスクリーンショット計 4 枚を主な特徴の直後に持つ SHALL。英日 README は同一の画像ファイルを参照し、キャプションのみ言語別とする SHALL。画像は端末を特定できる表示 (キャリア名・実機の時刻・バッテリー残量等) を含まない SHALL。MAUI については画像を置かず、Native と同じ画面になる旨を文で示す SHALL。4 枚は同一のデモ画面・同一のスクロール位置で撮り、platform と style 以外の差を持たない SHALL。

#### Scenario: 英日での画像共有

- **GIVEN** 英語 README と日本語 README
- **WHEN** 参照している画像ファイルのパスを比較する
- **THEN** 同一のファイルを指しており、異なるのはキャプションの言語だけである

#### Scenario: 4 枚の組み合わせの網羅

- **GIVEN** ルート README が参照するスクリーンショット
- **WHEN** platform (iOS / Android) と style (Modern / Classic) の組み合わせを数える
- **THEN** 4 通りすべてが 1 枚ずつ存在し、いずれも同一のデモ画面・同一のスクロール位置で撮られている

#### Scenario: 端末固有情報の不在

- **GIVEN** 採用したスクリーンショット 4 枚
- **WHEN** ステータスバー領域を目視で確認する
- **THEN** キャリア名・実機の時刻・バッテリー残量など端末を特定できる表示がない

### Requirement: 配信準備中の状態表記

ルート README は冒頭に配信準備中である旨の表記を 1 箇所だけ持つ SHALL。インストール節の本文は公開レジストリに存在する前提の配布座標で書き、未配信の注記を持たない SHALL。API 安定性の表記 (0.x の間は破壊的変更があり得る旨) は状態表記と区別し、公開後も残る常設の記述として持つ SHALL。

#### Scenario: 解除箇所の単一性

- **GIVEN** 初回リリース時に状態表記を解除する作業
- **WHEN** ルート README から未配信・配信準備中を示す記述を探す
- **THEN** 冒頭の 1 箇所だけが該当し、インストール節には存在しない

#### Scenario: 配布座標の書き方

- **GIVEN** インストール節
- **WHEN** SwiftPM / Maven / NuGet の依存宣言を読む
- **THEN** いずれも公開レジストリに存在する前提の座標で書かれており、未配信を理由とした代替手順 (ソース参照等) を持たない

### Requirement: 英日 README の翻訳ロックステップ

ルート README 2 枚は同一の節構成を持つ SHALL。一方だけを更新した状態をコミットしない SHALL。

#### Scenario: 節構成の一致

- **GIVEN** 英語 README と日本語 README
- **WHEN** 見出しの階層と並びを比較する
- **THEN** 順序と階層が一致する

### Requirement: 開発者向け知識の所在

廃止した README が持っていた開発者向けの契約と手順は `kasane/concepts/` に置かれる SHALL。concepts から README を知識の正として指す参照を持たない SHALL。

#### Scenario: 知識の正の逆転の解消

- **GIVEN** `kasane/concepts/maui/api/native-bridge.md`
- **WHEN** binding 構成・ビルド手順・既知の制約・SDK 更新時の再検証箇所についての記述を読む
- **THEN** 「正は `maui/README.md`」に相当する参照がなく、内容が concepts 側にある

#### Scenario: 移送した手順の到達可能性

- **GIVEN** 廃止前の `android/README.md` にあった Android SDK ロケーションの設定手順
- **WHEN** `kasane/concepts/cross/conventions/` を調べる
- **THEN** `ANDROID_HOME` の設定と 2 つの `local.properties` が必要である旨が読める

#### Scenario: Sample の実行手順の到達可能性

- **GIVEN** 廃止前の `samples/*/README.md` にあった Sample の実行手順・デモ画面一覧・本体ライブラリへステップインする手順
- **WHEN** `kasane/concepts/` を調べる
- **THEN** 3 platform それぞれの実行手順とステップイン手順が読め、デモ画面一覧は Sample の実ソース (`SampleScreen`) を正とする旨が示されている

#### Scenario: 検証ホストの起動手順の到達可能性

- **GIVEN** 廃止前の `maui/README.md` にあった検証ホストの起動手順と期待される表示
- **WHEN** `kasane/concepts/maui/` を調べる
- **THEN** 起動手順 (`DEVELOPER_DIR` の指定を含む) と期待される表示が読める

### Requirement: 貢献方針の表明

ルート README は英日とも貢献の節を持ち、外部からの Pull Request を受け付けず Issue で受けること、および Issue テンプレートの利用を案内する SHALL。`.github/CONTRIBUTING.md` (英語) と対応する日本語版を持ち、方針の理由と Issue の書き方を示す SHALL。

#### Scenario: 投稿前の到達経路

- **GIVEN** 公開リポジトリで Issue を作成しようとする外部の人
- **WHEN** GitHub が Issue 作成画面に表示する contributing guidelines のリンクをたどる
- **THEN** Pull Request を受け付けない方針と、Issue で受ける旨が読める

#### Scenario: README だけを読む人への到達

- **GIVEN** ルート README しか読まない訪問者
- **WHEN** 貢献の節を読む
- **THEN** PR を受け付けないこと・Issue で受けること・テンプレートを使ってほしいことが分かる

### Requirement: Issue テンプレートの必須項目

`.github/ISSUE_TEMPLATE/` に用途別 2 本の Issue Forms を置く SHALL。バグ報告はバージョン・platform・再現手順・実際の挙動・期待した挙動を必須項目とする SHALL。提案は解決したい課題・現状どう困っているか・考えた選択肢を必須項目とする SHALL。項目のラベルは英語とし、本文を英語・日本語のいずれで書いてもよいことを案内する SHALL。`.github/ISSUE_TEMPLATE/config.yml` で `blank_issues_enabled: false` とし、テンプレートを迂回した Issue 作成をできなくする SHALL。

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

### Requirement: 配布座標の文書間の一致

配布座標は初回リリースまで**暫定値**として扱う SHALL。ルート README のインストール節と `skills/` 各 `SKILL.md` の導入節は同一の値を示す SHALL — 文書間で食い違わせないことが本要件の目的であり、値そのものの確定は配信フェーズの責務とする SHALL。iOS の SwiftPM Package URL は `KsSettingsView-SPM` とする SHALL (配信リポジトリ名は本フェーズで確定したため)。Maven / NuGet の座標は現時点の値を書くにとどめ、本変更では確定させない SHALL。

#### Scenario: 仮名の解消

- **GIVEN** `skills/{en,ja}/kssettingsview-ios/SKILL.md`
- **WHEN** Package URL と `.product(package:)` の値を読む
- **THEN** いずれも `KsSettingsView-SPM` を指し、仮名 `KsSettingsView-Swift` が残っていない

#### Scenario: README と Skills の一致

- **GIVEN** ルート README のインストール節と各 `SKILL.md` の導入節
- **WHEN** 3 platform の配布座標を比較する
- **THEN** 同じ値である
