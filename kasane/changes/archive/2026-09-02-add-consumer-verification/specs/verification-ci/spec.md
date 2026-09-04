# verification-ci デルタスペック

## ADDED Requirements

### Requirement: 消費者検証 workflow の再利用契約
消費者検証の workflow は platform ごと (ios / android / maui) の 3 本を `workflow_call` で呼び出し可能 SHALL とし、入力としてモード (`dry-run` / `smoke`、必須)、version (任意。`smoke` では必須)、外部で準備した配布物の artifact 名 (任意) を受け取る。artifact 名が与えられればその配布物を取得して消費者ビルドだけを行い、無ければ job 内でフィード準備から行う。他の workflow (release workflow を含む) から CI 入口を経由せず呼び出せる。status check の名前は呼び出し側の job 名と呼ばれる側の job 名の双方で固定する (`consumer-ios / verify` / `consumer-android / verify` / `consumer-maui / verify`)。

#### Scenario: モードと version を与えた呼び出し
- **GIVEN** 消費者検証 workflow を `uses:` で参照し `mode` と `version` を渡す別の workflow
- **WHEN** その workflow が実行される
- **THEN** 当該 platform の消費者検証 job が指定のモードと version で実行される

#### Scenario: artifact を与えた呼び出し
- **GIVEN** 別 job が upload した配布物の artifact 名を入力に渡す呼び出し
- **WHEN** 消費者検証 job が実行される
- **THEN** job はその artifact を download して参照先とし、フィード準備 (pack / 発行 / スナップショット配置) を行わない

#### Scenario: 不正な入力で失敗する
- **GIVEN** `mode` に許可値以外を渡す、または `smoke` で `version` を省いた呼び出し
- **WHEN** 消費者検証 job が実行される
- **THEN** job はフィード準備の前に失敗として報告される

## MODIFIED Requirements

### Requirement: CI の起動条件
`develop` / `main` を対象とする pull_request と、`develop` への push で、検証 job 一式 (ios / android / maui / lint の 4 job と、`dry-run` モード + version 未指定 (platform ごとの開発用 version) で呼ぶ消費者検証 3 job) SHALL 起動する。paths による絞り込みは行わず、変更内容に関わらず常に全 job を実行する。

#### Scenario: PR で全 job が起動する
- **GIVEN** `develop` を base とする pull_request
- **WHEN** PR が作成または更新される
- **THEN** ios / android / maui / lint の 4 job と消費者検証 3 job がすべて実行される

#### Scenario: main への PR でも起動する
- **GIVEN** `main` を base とする pull_request (develop → main)
- **WHEN** PR が作成または更新される
- **THEN** 7 job がすべて実行される

#### Scenario: develop へのマージ後にも検証される
- **GIVEN** PR が `develop` にマージされた
- **WHEN** マージ commit が `develop` に push される
- **THEN** 7 job がマージ結果に対して実行される

#### Scenario: 消費者検証は dry-run で動く
- **GIVEN** CI 入口からの消費者検証 job
- **WHEN** job が実行される
- **THEN** 配布物はローカル参照先から解決され、job は配信先への書き込み権限・認証情報を持たず、公開レジストリ・配信リポジトリへの書き込みは発生しない

### Requirement: lint の検証
lint job は secret scan (gitleaks)・ローカル絶対パス検査 (local-path-lint)・個体/個人/秘密情報検査 (identity-lint)・コメント規約検査 (comment-policy-lint)・README 最小例と消費者ソースの一致検査を実行 SHALL し、いずれかの違反で job が失敗する。identity-lint の検査範囲は `samples` と `verification` を含む。

#### Scenario: 違反の検出
- **GIVEN** 検査対象範囲に違反 (秘密情報・ローカル絶対パス・個体識別子・コメント規約違反・README 最小例の不一致のいずれか) を含む変更
- **WHEN** lint job が実行される
- **THEN** job は失敗として報告され、違反箇所が出力で特定できる

#### Scenario: samples 配下の識別子検出
- **GIVEN** `samples/` または `verification/` 配下に開発チーム識別子等の個体情報が書き込まれた変更 (Xcode の実機ビルドによる書き戻しを含む)
- **WHEN** lint job が実行される
- **THEN** identity-lint が検出し job は失敗として報告される

### Requirement: マージ保護
`develop` / `main` への変更の取り込みは、7 job (ios / android / maui / lint と消費者検証 3 job) すべての成功を必須 status check とする pull_request 経由 SHALL とする (管理者のバイパスは許容する)。`main` が存在しない間は `develop` にのみ適用し、`main` の作成時に同じ設定を行う。

#### Scenario: 検査未通過のマージ拒否
- **GIVEN** 7 job のいずれかが失敗している PR
- **WHEN** マージしようとする
- **THEN** マージはブロックされる

#### Scenario: 直 push の拒否
- **GIVEN** `develop` または `main` への直接 push
- **WHEN** push を試みる
- **THEN** push は拒否される (admin バイパスを除く)
