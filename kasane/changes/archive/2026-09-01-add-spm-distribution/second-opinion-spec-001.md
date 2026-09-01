# セカンドオピニオン: add-spm-distribution (spec-001)
**相方**: codex / **label**: so-spec-add-spm-distribution / **日付**: 2026-09-01 / **対象**: 提案一式 (proposal.md / specs/spm-distribution/spec.md / tasks.md)
---
# レビュー結果: add-spm-distribution

**判定**: **NEEDS_DISCUSSION**  
**件数**: Critical 1 / Major 4 / Minor 2 / Suggestion 0

破壊的な同期処理の安全契約が未定義で、公開 prerelease tag は lockstep / tag-last の既存決定と衝突します。また、iOS テストが実質的に空振りできる受け入れ条件になっています。このままの実装着手は推奨できません。

## 指摘事項

### [🔴 Critical] 同期先を誤指定すると任意の作業ツリーを消去できる

**該当箇所**: `specs/spm-distribution/spec.md:35`

**問題点**: 引数で渡されたディレクトリから `.git` 以外をすべて削除しますが、同期先が本当に配信リポジトリのルートかを検証する要件も、検証失敗時に変更しない保証もありません。空文字・monorepo 本体・その親・symlink・別リポジトリなどの誤指定で、回復困難なデータ消失が起こり得ます。また、コピー元不足を削除後に検出すると作業コピーが半壊します。`git 非操作` Scenario も commit/tag しか観測せず、push・index・remote・hook・refs の変更を検出できません。

**推奨修正**: 次を SHALL と Scenario に追加してください。

- コピー元5点を削除前に全件検証する
- canonical path 化し、ルート・monorepo・その祖先/自身など危険な対象を拒否する
- 対象が git top-level であり、期待する配信リポジトリであることを確認する
- `.git` がディレクトリの場合と worktree のファイルの場合を明確化する
- 検証失敗時は同期先を一切変更しない
- Git metadata・HEAD・index・remote・refsを変更せず、ネットワーク操作もしない
- 誤指定、symlink、コピー元不足、別リポジトリをテストする

### [🟠 Major] iOS のテストとHTTPS消費者検証がmacOSで空振りできる

**該当箇所**: `specs/spm-distribution/spec.md:16`、`specs/spm-distribution/spec.md:63`、`tasks.md:6`、`tasks.md:23`

**問題点**: `swift test` は macOS 上で UIKit ガード内のテストを除外し、UI tests は0件、SwiftUI tests もほぼ実行されません。これは `kasane/handbook/cross/test-execution.md:47` および `cross/ADR-0026` に明確に反します。

HTTPS消費者側も platform/destination が未指定です。macOS buildで3 moduleを単にimportするだけでも成功でき、iOS用productとして成立していることを確認できません。

**推奨修正**:

- 完了判定を `xcodebuild test -scheme KsSettingsView-Package -destination <iOS Simulator>` に変更する
- 実行テスト件数が1件以上であることをTHENに含める
- HTTPS消費者はiOS Simulator向けにbuildする
- importだけでなく、各moduleの公開型を最低1つ参照して配線を確認する
- 既存の `.github/workflows/verify-ios.yml` と同じ検証経路を再利用する

### [🟠 Major] 配信リポジトリの初期設定に仕様と受け入れ基準がない

**該当箇所**: `proposal.md:12`、`tasks.md:21`、`specs/spm-distribution/spec.md:57`

**問題点**: proposal/tasks は visibility、default branch、Issues等の無効化、PR collaborators-only、workflow・branch protectionなし、description・Websiteを約束しています。しかしデルタスペックはHTTPS解決しか要求しておらず、すべての設定を誤ってもScenarioを満たします。READMEの誘導先も存在確認だけで内容を検証しません。

**推奨修正**: 「配信リポジトリの初期状態」を独立Requirementにし、GitHub API等で各設定、READMEの誘導URL、最初のpush後のdefault branchを検証するScenarioを追加してください。

### [🟠 Major] 検証用prerelease tagがlockstep/tag-lastと衝突する

**該当箇所**: `tasks.md:22`、`specs/spm-distribution/spec.md:62`、`kasane/decisions/cross/0019-lockstep-single-version.md:18`、`kasane/decisions/cross/0020-release-dispatch-tag-last-version-injection.md:19`

**問題点**: publicな配信リポジトリへsemver prerelease tagを打つと、iOSだけが外部から解決可能な公開版になります。これは「全platformを同一versionで一斉リリース」「publish全成功後にだけtagを作る」という決定と衝突します。「検証用」と呼ぶだけでは例外になりません。検証後にtagを残すか削除するかも未定義です。

**推奨修正**: オーナー判断で次のいずれかを明文化してください。

- 一時的な例外としてtagを作成し、検証直後に削除して証跡だけ残す
- production配信リポジトリとは別の検証用リポジトリで確認する
- 全platformのprereleaseとしてphase-8で実施する

少なくともtag名の予約規則、削除・保持、失敗時の後始末をRequirement化する必要があります。

### [🟠 Major] 変更級とdomainがKasane規約に一致しない

**該当箇所**: `proposal.md:31`、`proposal.md:35`

**問題点**: 外部GitHubリポジトリの作成・設定・実リモート統合を含むため、ksn-coreの「外部連携」に該当しL級です。また `ios/` に加えてリポジトリ横断スクリプト、`handbook/cross`、配信リポジトリ境界を扱うため、domain-axis上は `domain: cross` です。M/iosのままではdesign.mdが省略され、蒸留先とスキル解決も誤る可能性があります。

**推奨修正**: `級: L`、`domain: cross` に変更し、design.mdに少なくとも同期安全契約、外部状態の検証方法、検証tagのライフサイクルをDecision形式で記載してください。

### [🟡 Minor] 「変更しない」契約をScenarioが検証していない

**該当箇所**: `specs/spm-distribution/spec.md:7`、`specs/spm-distribution/spec.md:12`

**問題点**: Requirementはtarget構成・module名・platforms指定を不変としますが、THENはproductsしか確認しません。例えばiOS最小版を変更してもScenarioは成功できます。

**推奨修正**: `dump-package` のplatforms、target名・依存・pathも期待値と照合するか、products以外の正規化済みmanifest差分がないことを確認してください。

### [🟡 Minor] handbook更新タスクの位置づけと仕様対応が不明

**該当箇所**: `proposal.md:13`、`proposal.md:23`、`tasks.md:27`

**問題点**: `public-identifiers.md` は規範層ですが、更新が実装タスクとして置かれ、対応Requirement/Scenarioがありません。一方conceptsは蒸留送りとしており、長命層の扱いが統一されていません。

**推奨修正**: handbookの規範変更としてproposalで明示承認の対象にし、実装前の規範改訂または蒸留時更新のどちらで扱うか統一してください。tasksに残す場合は対応するRequirementを明記してください。

## アクションプラン

1. 同期スクリプトの安全境界と失敗時保証を仕様化する。
2. 検証用tagの扱いをADR-0019/0020との関係込みで決定する。
3. L級・`domain: cross`へ修正し、design.mdを追加する。
4. iOS Simulatorによるテスト・HTTPS消費者検証へ受け入れ条件を修正する。
5. 外部リポジトリ設定とmanifest不変条件のScenarioを追加する。

依頼どおり、ビルド・テストおよびレビュー結果ファイルの作成は行っていません。

## 突き合わせ結果 (2026-09-01)

| # | 指摘 | 採否 | 反映先 |
|---|---|---|---|
| 1 | Critical: 同期スクリプトの安全契約なし | 採用 (相方のみ + 根拠強) | spec R「スナップショット同期スクリプト」の検証 SHALL + 誤指定拒否 / コピー元不足 Scenario、design.md Decision 1 |
| 2 | Major: swift test が macOS で空振り | 採用 | spec の受け入れ経路を iOS Simulator + 実行件数 ≥ 1 へ変更、design.md Decision 4 |
| 3 | Major: 初期設定の受け入れ基準なし | 採用 | spec R「配信リポジトリの初期状態」新設 (gh api 検証)、design.md Decision 3 |
| 4 | Major: prerelease tag が lockstep / tag-last と衝突 | 採用 (オーナー裁定: 検証後削除) | spec R「https 解決」の tag 削除 SHALL + 後始末 Scenario、design.md Decision 2 |
| 5 | Major: 級 L / domain cross | 採用 (オーナー裁定) | proposal を L / cross へ改訂、design.md 新設 |
| 6 | Minor: 不変契約の検証なし | 採用 | spec Scenario「package 定義の確認」の THEN を拡張 |
| 7 | Minor: handbook 更新の位置づけ | 部分採用 | proposal に承認済み規範改訂 (phase-9 申し送り出典) と明記。Requirement 化は降格 (挙動契約でないため) |

未解決: なし。オーナーによる的外れ却下: なし (ksn-lesson 捕捉対象なし)
