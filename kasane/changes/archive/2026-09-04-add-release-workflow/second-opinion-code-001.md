# セカンドオピニオン: add-release-workflow (code-001)
**相方**: codex / **label**: so-code-add-release-workflow / **日付**: 2026-09-03 / **対象**: 作業ツリーの未コミット変更 (develop 4c04878 からの差分: .github/ scripts/release/ maui/ verification/maui/ kasane/handbook/cross/ AGENTS.md)
---
# レビュー結果: add-release-workflow

**日付**: 2026-09-03  
**判定**: **NEEDS_DISCUSSION**  
**件数**: Critical 0 / Major 4 / Minor 1 / Suggestion 0

## サマリー

提供されたテスト・pack・静的検査結果を前提としても、リリースの再開性、`main` の取り込み制限、不可逆 publish 前の競合検査に Major が残っています。特に `main` 制限の確実な実装には、必須 status check 7件という現設計を含めた判断が必要なため、NEEDS_DISCUSSION とします。

指定どおりビルド・テストは再実行せず、ファイルも作成していません。deviation.md の3件は合意済み差分として扱い、指摘していません。

## 照合した規約

- `comment-policy.md` — always
- `test-execution.md` — 提供済みテスト結果の扱い
- `local-development-setup.md` — 消費者検証の経路
- `release-procedure.md` — 新規リリース手順
- `kasane/lessons/code-review.md`
- 関連する cross/maui ADR・concepts
- MAUI handbook の integration-host / performance 規約は今回の静的なpack・workflow変更には非該当

## 指摘事項

### [🟠 Major] drop 済み deployment ID により再実行が停止する

**該当箇所**: `.github/workflows/release.yml:558`、`.github/workflows/release.yml:773`、`scripts/release/central-portal.sh:173`

**問題点**: Maven upload 後に NuGet push等が失敗すると、失敗処理が deployment をDELETEします。一方、`central-deployment-id` artifact は削除・無効化されないため、「失敗したjobから再実行」すると同じIDを再取得し、直ちに `status` を呼びます。`deployment_state` は200以外をすべて致命的エラーにしており、drop済み／not foundを「IDなしとして再upload」へ戻す分岐がありません。

Sonatypeの仕様でもdropはdeployment IDに対するDELETE操作です。[Central Portal Publisher API](https://central.sonatype.org/publish/publish-portal-api/) 現在の自己テストはdrop単体までで、「drop後のartifactを使った次attempt」を検査していません。

**推奨修正**: drop成功後にartifactを空のtombstoneで上書きするか、statusのnot foundを明示的に「deploymentなし」と解釈して公開済み確認→再uploadへ進めてください。`VALIDATED → NuGet失敗 → drop → 再実行` の一連の回帰テストも追加します。

### [🟠 Major] `main` への取り込み制限をPR自身が無効化できる

**該当箇所**: `.github/workflows/ci.yml:87`

**問題点**: head制限は通常の `pull_request` workflow内にあります。GitHubではこのイベントのworkflow自体がPRのmerge commitから実行されるため、`main` 向けPRが同時に `ci.yml` の制限ステップを削除・改変できます。これは「develop以外からmainへのPRを必須checkで拒否する」という保証になりません。[GitHub公式ドキュメント](https://docs.github.com/en/actions/reference/security/securely-using-pull_request_target)

加えて `github.head_ref == "develop"` は短いブランチ名しか見ないため、fork側の `develop` も通ります。

**推奨修正**: PRコードをcheckout・実行しないbase-trustedな `pull_request_target` workflow、またはGitHub Rulesetのrequired workflow等で検査してください。少なくとも `head.ref == develop` と `head.repo.full_name == github.repository` の両方が必要です。独立checkを追加すると必須checkが8件になるため、現行の「7件」契約をどう扱うかオーナー判断が必要です。

### [🟠 Major] 配信リポジトリのtag競合を不可逆publish直前に再検査していない

**該当箇所**: `.github/workflows/release.yml:444`

**問題点**: publish開始時の再検査はmonorepo tagとMaven公開状態だけです。配信リポジトリのtagはvalidate時に一度確認されますが、その後のtest/package中に人や別automationが同名tagを作った場合、NuGet・Maven公開後の `Push distribution repository tag` で初めて競合を検出します。結果としてiOS tagだけ存在しない、取り消せない部分公開になります。

これはspecの「publish jobはロック取得後に外部状態（tag・公開済みversion）を再検査」と整合しません。

**推奨修正**: `Push snapshot commit` で同期後のtreeを作った時点、かつMaven uploadより前に、配信リポジトリの同名tagをfetchしてtree一致を再検査してください。

### [🟠 Major] README専用scriptが許可された変更範囲を越えている

**該当箇所**: `scripts/release/set-readme-version.py:102`、`AGENTS.md:17`、`README.md:44`

**問題点**: AGENTS.mdが許可する例外は「2枚×3行のversion文字列だけ」で、文面には触れない契約です。しかしscriptはSwiftPM宣言が `from:` なら `exact:` へ書き換えます。現行README 2枚は実際に `from:` なので、初回実行時にversion以外も変更されます。自己テストもこの追加変更を成功条件にしています。

**推奨修正**: `from:` → `exact:` は明示依頼されたdocs-refresh側で一度だけ行い、専用scriptは `exact:` のversion部分だけを置換してください。置換モードで `from:` を検出した場合は、文面を直さずエラーにするのが境界を保てます。

### [🟡 Minor] 新規scriptが削除コマンド規約に反している

**該当箇所**: `scripts/release/central-portal.sh:291`、`scripts/release/compare-maven-artifacts.sh:94`

**問題点**: 新規コードのcleanupが `rm -rf` を使用しており、AGENTS.mdの「削除コマンドはrmではなくtrash」に反します。対象は `mktemp` のディレクトリに限定されているため実害リスクは低いものの、明文規約への不適合です。

**推奨修正**: ローカルでは `trash` を使い、ephemeralなGitHub runnerでは `RUNNER_TEMP` の終了時破棄に委ねるcleanup helperにするか、一時ディレクトリだけを対象とする例外を明文化してください。

## アクションプラン

1. `main` のbase-trustedな制限方式と必須check数を決定する。
2. drop後のdeployment artifactを無効化し、再実行テストを追加する。
3. 配信リポジトリtagを不可逆操作の直前にも再検査する。
4. READMEの `exact:` 化をdocs-refreshへ分離する。
5. 新規scriptの削除処理を規約へ適合させる。


## 突き合わせ結果 (review-001 との照合、2026-09-03)

| 相方の指摘 | 採否 | 根拠 |
|---|---|---|
| drop 済み deployment ID により再実行が停止する (Major) | **確定** | review-001 の Major と一致 |
| 配信リポジトリの tag 競合を不可逆 publish 直前に再検査していない (Major) | **採用** (Major) | 相方のみだが、spec「publish job はロック取得後に外部状態 (tag・公開済み version) を再検査」の tag に配信リポジトリ側が含まれておらず、実装の再検査は monorepo tag と Maven 公開状態のみ (release.yml「Re-check external state」)。実害シナリオ (iOS だけ欠けた部分公開) が具体 |
| `main` への取り込み制限を PR 自身が無効化できる / fork の `develop` を通す (Major) | **分割**: fork 対策 (head.repo が自リポジトリであること) は採用 (Minor 相当の数行修正)。`pull_request_target` / Ruleset 化は design Decision 6 (CI の lint job で検査) の設計変更にあたるため **NEEDS_DISCUSSION** としてオーナーへ | 相方のみ。前者は根拠強・数行。後者は設計判断 |
| README 専用 script が許可された変更範囲を越えている (Major) | **降格** (Minor: AGENTS.md の例外文の文言修正) | `from:` → `exact:` の正規化は phase-8 agenda の決定 (「script による version 置換 … SwiftPM は `from:` が prerelease を解決しないため `exact:` へ」) どおり。食い違っているのは AGENTS.md に足した例外文の「文面には触れない」の側なので、そちらを実態に合わせる |
| 新規 script が削除コマンド規約に反している (Minor) | **降格** (対応しない) | `lessons/inbox/trash-rule-scope-is-agent-tools-not-scripts.md` (オーナー却下済みの型: スクリプト内の rm は指摘しない) |

確定 1 / 採用 2 (うち 1 は分割) / 降格 2 / 未解決 (オーナー判断待ち) 1
