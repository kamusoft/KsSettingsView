# セカンドオピニオン: backport-release-workflow-hardening (spec-001)
**相方**: codex / **label**: so-spec-backport-release-workflow-hardening / **日付**: 2026-09-11 / **対象**: kasane/changes/backport-release-workflow-hardening/ の proposal.md・specs/release-workflow/spec.md・tasks.md・exploration.md、kasane/decisions/cross/0029-install-example-version-written-by-release-workflow.md
---
# レビュー結果: backport-release-workflow-hardening

**判定**: NEEDS_DISCUSSION  
**件数**: Critical 0 / Major 4 / Minor 5 / Suggestion 0

仕様と実装タスクの間に、実装方針を決めないと解消できない矛盾があります。特に `develop` 書き戻しの成功契約・再実行安全性・検証方法を確定するまでは実装へ進めません。

静的レビューのみ実施し、ビルド・テスト・ファイル変更は行っていません。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/release-procedure.md`（release workflow の変更・再実行）
- `kasane/lessons/spec-review.md`（現行実体との突合、条項ペアの矛盾検算）
- 現行 `.github/workflows/release.yml`
- 現行 `scripts/release/`
- `kasane/concepts/cross/architecture/release-pipeline.md`
- cross/ADR-0020・0028・0029
- KsDialogs の知らせと対応実装

## 指摘事項

### [🟠 Major] `develop` が追従する SHALL と、追従しなくても成功する Scenario が矛盾している

**該当箇所**: `specs/release-workflow/spec.md:12`、`specs/release-workflow/spec.md:32`、`kasane/decisions/cross/0029-install-example-version-written-by-release-workflow.md:35`

**問題点**:  
Requirement は、publish 全成功後に `develop` が入力 version を示すことを無条件の SHALL としています。一方、Scenario は競合・検査失敗などで反映されなくても release が成功すると定めています。この場合、正常終了した release が同じ Requirement の SHALL を満たさない状態になります。

加えて、追従失敗後の回復主体がありません。通常の `Re-run failed jobs` では、警告に倒して成功した書き戻し step を再実行できません。

**推奨修正**:  
契約を次のように分離してください。

- workflow は `develop` への反映を SHALL attempt とする
- 反映成功時の postcondition
- 反映失敗時の postcondition（ブランチは変更しない・警告と summary を残す）
- 失敗後の再試行手段（同 version の再実行、専用 script、または明示的な事後手順）

「追従失敗を release 成功としてよいか」は設計判断なので、ここを確定してから Requirement を書き直す必要があります。

### [🟠 Major] post-publish 書き戻し経路を検証するタスクが存在しない

**該当箇所**: `proposal.md:37`、`tasks.md:14`、`tasks.md:39`、`tasks.md:40`

**問題点**:  
取得・worktree 作成・置換・lint・commit・再取得・rebase・push・summary という多数の分岐を追加しますが、検証タスクはすべて dry-run または既存スクリプトの自己テストです。proposal 自身が認めるとおり、dry-run では publish job と `develop` 書き戻しが走りません。

そのため、次の Scenario に実装前から検証方法がありません。

- 書き戻し成功
- 競合・lint 失敗・push 拒否を警告に変換
- release の成功状態を維持
- summary への結果出力
- 再実行時の冪等性

不可逆な公開の後で初めて不具合が判明する設計になります。

**推奨修正**:  
書き戻しロジックを自己テスト可能なスクリプトへ分離し、ローカル bare repository を使って成功・no-op・競合・push 拒否・古い run の各ケースを検査してください。少なくとも全 Scenario と tasks の対応を明記してください。

### [🟠 Major] 「公開レジストリへの反映待ち」の契約は Maven にも掛かるが、タスクは NuGet しか直さない

**該当箇所**: `specs/release-workflow/spec.md:65`、`specs/release-workflow/spec.md:67`、`tasks.md:27`、`scripts/release/wait-for-registries.sh:45`

**問題点**:  
Requirement はレジストリ一般について「未反映」と「照会不能」を区別すると規定しています。しかし実装タスクは `nuget_ready()` だけを対象にしています。

現行 `maven_central_ready()` も、200 以外をすべて同じ偽値に畳み込んでおり、404・5xx・通信失敗を区別しません。探索で「Maven 側は区別済み」とした判断は、`central-portal.sh published` には当てはまりますが、対象の `wait-for-registries.sh` には当てはまりません。

**推奨修正**:  
どちらかを選んで整合させてください。

- Requirement を「nuget.org の反映待ち」に限定する
- Maven と NuGet の両方に状態分類を実装し、双方の自己テストを追加する

### [🟠 Major] publish job の時間予算に検証可能な下限がない

**該当箇所**: `specs/release-workflow/spec.md:48`、`specs/release-workflow/spec.md:60`、`tasks.md:23`、`.github/workflows/release.yml:429`

**問題点**:  
「最悪ケースでも job 上限に達しない」としていますが、必要な `timeout-minutes` の値または算出式が決まっていません。

待ちだけで検証 30 分 × 2 + 公開 90 分 = 150 分です。さらに現行 HTTP 要求は1回最大300秒で、deadline は要求終了後に判定されます（`scripts/release/central-portal.sh:127`、`:220`、`:262`）。「本体11分」も実測値であり上限ではありません。

このままでは、実装者が任意の余裕を選べ、Scenario の合否を客観的に判定できません。

**推奨修正**:  
次を仕様または task に固定してください。

- job timeout の具体値または最低値
- 2回の検証待ち、公開待ち、HTTP timeout 超過分、本体処理、余裕時間を含む算出式
- workflow の設定値を静的に検査する受け入れ条件

### [🟡 Minor] 完了済みの古い release run が `develop` を旧 version へ戻せる

**該当箇所**: `tasks.md:11`、`.github/workflows/release.yml:109`、`.github/workflows/release.yml:796`

**問題点**:  
同じ commit を指す既存 tag は再実行として許可されます。そのため、過去の完了済み run を後日 `Re-run all jobs` すると、最新の `develop` を取得した後、無条件に過去の入力 version へ置換して push できます。

KsDialogs 側の独立レビューでも同じ問題が既知です（`../KsDialogs/kasane/changes/archive/2026-09-10-add-release-workflow/review-002.md:101`）。

**推奨修正**:  
「現在の `develop` が新しい version を示している場合は変更しない」Scenario と実装ガードを追加してください。手順で回避するなら、完了済みの古い run を再実行しない旨を release procedure に明記してください。

### [🟡 Minor] `readme-example-lint.py` の責務を誤認している

**該当箇所**: `kasane/decisions/cross/0029-install-example-version-written-by-release-workflow.md:34`、`scripts/readme-example-lint.py:2`

**問題点**:  
ADR は `readme-example-lint.py` を「インストール例の形」の検査として挙げていますが、現行スクリプトは英語 README の最小コード例と consumer source の一致を調べるだけです。日本語 README と Skills のインストール宣言は検査しません。

実際にインストール例の形と version を検査するのは `scripts/release/set-readme-version.py --check` です。

**推奨修正**:  
置換後に `set-readme-version.py --check "${KS_VERSION}"` を実行することを明記し、`readme-example-lint.py` は「最小コード例の一致検査」と正確に記述してください。

### [🟡 Minor] `set-readme-version.py` の説明が実装後に古くなる

**該当箇所**: `scripts/release/set-readme-version.py:6`、`tasks.md:16`

**問題点**:  
スクリプト冒頭には「release workflow の validate が検査モードを呼ぶ」とあります。task 2.6 でその step を削除しても、このコメントを更新するタスクがありません。

**推奨修正**:  
実装タスクにコメント更新を追加し、pack 前の置換と `develop` 書き戻しで利用される現在形の説明へ変更してください。

### [🟡 Minor] deployment ID の移動が exploration の確定スコープと食い違う

**該当箇所**: `exploration.md:51`、`proposal.md:21`、`specs/release-workflow/spec.md:87`、`tasks.md:34`

**問題点**:  
exploration は知らせ #6 を「対応済みのため扱わない」と確定していますが、proposal は同じ読み込み位置の移動を付随修正として追加し、spec と tasks に正式な Requirement として含めています。

既知の変更を Requirement 化しているため、実装中に発見した「付随修正」ではなく、通常の5件目のスコープです。

**推奨修正**:  
正式スコープに含めるなら What Changes と Impact に5件目として記載し、exploration から判断が変わった理由を示してください。不要なら spec/task から外してください。

### [🟡 Minor] ADR-0029 昇格時の改訂関係更新がタスクにない

**該当箇所**: `tasks.md:51`、`kasane/decisions/cross/0029-install-example-version-written-by-release-workflow.md:6`、`kasane/decisions/cross/0020-release-dispatch-tag-last-version-injection.md:1`

**問題点**:  
ADR-0029 の accepted 昇格だけが申し送られています。`amends: 0020` が accepted になった時点では、ADR-0020 の `amended-by: 0029` と cross index の前方参照も必要です。

**推奨修正**:  
蒸留への申し送りに、旧 ADR frontmatter と index の改訂関係更新を明記してください。

## アクションプラン

1. `develop` 書き戻しを強い保証にするか best-effort にするか決め、Requirement と失敗時の回復手段を整合させる。
2. 古い run による version 巻き戻し防止と、書き戻し経路の自己テストを追加する。
3. レジストリ状態分類を NuGet 限定にするか、Maven まで拡張するか決める。
4. publish job の時間予算を数値または算出式で固定する。
5. lint の責務、スコープ5件目、ADR 改訂関係、ソースコメントを整理する。

## 突き合わせ結果

ホスト側の自己レビュー (2 周) では 1 件のみ検出 (spec / tasks の「既定の開発ブランチ」がリポジトリの実体と食い違う — 修正済み)。相方の指摘 9 件はいずれもホスト側の見逃しであり、**全件を採用**した (降格・未解決なし)。

| # | 指摘 | 採否 | 検証 |
|---|---|---|---|
| Major 1 | `develop` 追従の SHALL と失敗許容 Scenario の矛盾・回復主体の不在 | 採用 | spec の条項を読み直し、postcondition を無条件 SHALL で書いていたことを確認 |
| Major 2 | post-publish 書き戻し経路を検証するタスクが無い | 採用 | tasks 6.x が dry-run と既存自己テストのみであることを確認 |
| Major 3 | Requirement はレジストリ一般だが tasks は NuGet のみ。`maven_central_ready()` も未区別 | 採用 | `wait-for-registries.sh:46-53` を確認。status を取得しているが 200 以外を一様に偽へ畳み込んでおり、404 / 5xx / 通信失敗を区別しない。proposal の「Maven 側は分けており nuget 側だけが非対称」は `central-portal.sh published` には当たるが `wait-for-registries.sh` には当たらない (事実誤り) |
| Major 4 | job の時間予算に検証可能な下限が無い | 採用 | spec・tasks に具体値も算出式も書いていないことを確認 |
| Minor 1 | 完了済みの古い run の再実行が `develop` を旧 version へ戻せる | 採用 | tag の commit 照合は同一 commit の再実行を許すため、後日の `Re-run all jobs` で成立する |
| Minor 2 | `readme-example-lint.py` の責務の誤認 | 採用 | `scripts/readme-example-lint.py:2-13` を確認。英語 README の最小コード例と `verification/` のソースの一致検査であり、インストール例の version は検査しない |
| Minor 3 | `set-readme-version.py` の docstring が実装後に古くなる | 採用 | 冒頭に validate が検査モードを呼ぶ旨の記述があることを確認 |
| Minor 4 | deployment ID の移動は付随修正ではなくスコープ 5 件目 | 採用 | Requirement と Scenario を与えた時点で正式スコープであることに同意 |
| Minor 5 | ADR 昇格時の改訂関係 (`amended-by` と index) の更新が申し送りに無い | 採用 | `core/0030` が `amended-by: 0031` を持つ慣行を確認 |

設計判断を要するもの (オーナーへ提示):

- Major 1 の「`develop` 追従の失敗を release 成功のままにしてよいか」と、その場合の回復手段
