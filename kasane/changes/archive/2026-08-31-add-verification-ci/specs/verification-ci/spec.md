# verification-ci デルタスペック

## ADDED Requirements

### Requirement: CI の起動条件
`develop` / `main` を対象とする pull_request と、`develop` への push で、検証 job 一式 (ios / android / maui / lint の 4 job) SHALL 起動する。paths による絞り込みは行わず、変更内容に関わらず常に全 job を実行する。

#### Scenario: PR で全 job が起動する
- **GIVEN** `develop` を base とする pull_request
- **WHEN** PR が作成または更新される
- **THEN** ios / android / maui / lint の 4 job がすべて実行される

#### Scenario: main への PR でも起動する
- **GIVEN** `main` を base とする pull_request (develop → main)
- **WHEN** PR が作成または更新される
- **THEN** 4 job がすべて実行される

#### Scenario: develop へのマージ後にも検証される
- **GIVEN** PR が `develop` にマージされた
- **WHEN** マージ commit が `develop` に push される
- **THEN** 4 job がマージ結果に対して実行される

### Requirement: platform workflow の再利用契約
ios / android / maui の各検証 workflow は `workflow_call` で呼び出し可能 SHALL である。他の workflow (release workflow を含む) から、CI 入口を経由せず単独で呼び出せる。

#### Scenario: 別 workflow からの呼び出し
- **GIVEN** platform 検証 workflow を `uses:` で参照する別の workflow
- **WHEN** その workflow が実行される
- **THEN** platform 検証 job が CI 入口経由と同じ内容で実行される

### Requirement: iOS の検証
ios job は iOS Simulator 上でパッケージ全体のテストを実行 SHALL し、テストの失敗で job が失敗する。macOS ホスト上の `swift test` を成否判定に用いてはならない。

#### Scenario: Simulator 全件実行
- **GIVEN** ios job の実行
- **WHEN** テストが実行される
- **THEN** iOS Simulator destination で全テストターゲット (`#if canImport(UIKit)` ガード内を含む) が実行され、実行件数がログで確認できる

### Requirement: Android の検証と実行件数の担保
android job は全 module のユニットテスト (debug / release 両 variant) を毎回実行 SHALL し、実行件数を検査 SHALL する。検査は、期待する module×variant の集合を Gradle 構成 (`android/settings.gradle.kts` の include) から導出し、各組についてテスト結果 XML の欠落または `tests` 属性合計 0 のいずれでも job を失敗とする (存在する XML だけを数えると、タスクごと実行されなかった組を見逃すため)。実行件数の合計は job summary に表示する。

#### Scenario: 全件実行と件数表示
- **GIVEN** android job の実行
- **WHEN** テストが完了する
- **THEN** 全モジュール×variant のテストが実行され、合計実行件数が job summary に表示される

#### Scenario: 0 件実行の検出
- **GIVEN** 期待する module×variant のいずれかで、テストが 1 件も実行されなかった (結果 XML の欠落を含む) 状態
- **WHEN** 件数検査が走る
- **THEN** テスト自体が緑でも job は失敗として報告される

### Requirement: MAUI の検証
maui job は facade のユニットテストを実行 SHALL し、実行件数を検査して合計 0 件なら job を失敗 SHALL とする (件数は job summary に表示する)。あわせて facade の platform TFM (net10.0-ios / net10.0-android) と binding 2 プロジェクトのビルドを成功 SHALL させる。検証ホストの実行 (E2E) は行わない。

#### Scenario: facade テストと配線のコンパイル検証
- **GIVEN** maui job の実行
- **WHEN** テストとビルドが完了する
- **THEN** facade のユニットテストが全件実行されて実行件数が job summary で確認でき、platform TFM と binding のビルドがすべて成功している (テスト・ビルドの失敗、実行 0 件はいずれも job の失敗になる)

### Requirement: lint の検証
lint job は secret scan (gitleaks)・ローカル絶対パス検査 (local-path-lint)・個体/個人/秘密情報検査 (identity-lint)・コメント規約検査 (comment-policy-lint) を実行 SHALL し、いずれかの違反で job が失敗する。identity-lint の検査範囲は `samples` を含む。

#### Scenario: 違反の検出
- **GIVEN** 検査対象範囲に違反 (秘密情報・ローカル絶対パス・個体識別子・コメント規約違反のいずれか) を含む変更
- **WHEN** lint job が実行される
- **THEN** job は失敗として報告され、違反箇所が出力で特定できる

#### Scenario: samples 配下の識別子検出
- **GIVEN** `samples/` 配下に開発チーム識別子等の個体情報が書き込まれた変更 (Xcode の実機ビルドによる書き戻しを含む)
- **WHEN** lint job が実行される
- **THEN** identity-lint が検出し job は失敗として報告される

### Requirement: ツールチェーンの再現性
検証に用いるツールチェーンの版はリポジトリ内 (workflow 定義・`global.json`) で明示 SHALL され、ランナーイメージの既定値に依存しない。固定境界は次のとおり: ランナーイメージは版指定 (`macos-26` 等)、Xcode はメジャー.マイナー (パッチはイメージ同梱内の変動を許容)、JDK はディストリビューション + メジャー (Temurin 17)、.NET SDK と workload set は `global.json` の完全指定。Xcode の選択は iOS を扱う全 job (ios / maui) に適用する。

#### Scenario: 版の変更が diff に現れる
- **GIVEN** ツールチェーンの版を固定境界の粒度で上げる必要
- **WHEN** 版を変更する
- **THEN** 変更は workflow 定義または `global.json` の diff として PR に現れ、固定境界の粒度で版が diff なしに変わることはない

### Requirement: マージ保護
`develop` / `main` への変更の取り込みは、4 job すべての成功を必須 status check とする pull_request 経由 SHALL とする (管理者のバイパスは許容する)。

#### Scenario: 検査未通過のマージ拒否
- **GIVEN** 4 job のいずれかが失敗している PR
- **WHEN** マージしようとする
- **THEN** マージはブロックされる

#### Scenario: 直 push の拒否
- **GIVEN** `develop` または `main` への直接 push
- **WHEN** push を試みる
- **THEN** push は拒否される (admin バイパスを除く)
