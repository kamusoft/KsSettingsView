# セカンドオピニオン: install-examples-and-release-notes (spec-002)
**相方**: codex / **label**: so-spec-install-examples-and-release-notes-2 / **日付**: 2026-09-11 / **対象**: spec-001 の指摘を反映した改訂版の proposal.md・design.md・specs/ 2 本・tasks.md・exploration.md、kasane/decisions/cross/0029・0030
---
# レビュー結果: install-examples-and-release-notes

**日付**: 2026-09-11  
**判定**: **CHANGES_REQUESTED**

## サマリー

Critical 0件、Major 5件、Minor 1件です。

現行の14行のインストール例、release workflow、README／Skill、関連ADRとの照合結果は概ね正確です。一方、Releaseノートの重複生成、対象PRの境界、release Skillの受け入れ条件に実装前に塞ぐべき穴があります。

静的レビューのみ実施し、ビルド・テスト・ファイル変更は行っていません。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/user-skill-api-listing.md`（`skills/`変更）
- `kasane/handbook/cross/release-procedure.md`（release workflow／手順変更）
- `kasane/lessons/spec-review.md` L-001、L-002、L-004
- 関連ADR 0019、0020、0022、0024、0028
- `kasane/concepts/cross/architecture/release-pipeline.md`

## 指摘事項

### [🟠 Major] release Skillの下書き範囲により変更が二重掲載される

**該当箇所**: `specs/release-workflow/spec.md:33`、`specs/release-workflow/spec.md:62`、`specs/release-workflow/spec.md:115`、`kasane/decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md:23`、`kasane/decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md:39`

**問題点**: workflowは前回Release以降の全main PRを集めます。一方、release Skillも前回Release以降の全変更から、これから作るPRの`## Changes`下書きを生成します。

例えば前回tag後にPR Aがmainへ入り、その後release PR Bを作ると、Bの下書きにはAとBの変更が含まれます。workflowはPR AとPR Bを両方連結するため、Aの変更が二重掲載されます。これは「複数PR分が重複なく並ぶ」Scenarioと矛盾します。

**推奨修正**: release Skillの下書き対象を「今回作成するPRがmainへ新たに持ち込む差分」、具体的には`origin/main..origin/develop`相当へ変更してください。先行するmain PRが存在する場合でも重複しないScenarioとテストタスクを追加してください。

### [🟠 Major] 「直前のRelease」の選択規則が一意でなく、対象選定のテストもない

**該当箇所**: `design.md:39`、`design.md:43`、`specs/release-workflow/spec.md:33`、`tasks.md:35`、`tasks.md:36`、`tasks.md:59`

**問題点**: 「今回versionを除いた、直前に実在するGitHub Release」について、次が決まっていません。

- draftを除外するか
- 対象commitの祖先であるtagだけに限るか
- `createdAt`、`publishedAt`、semver、commit距離のどれで「直前」を決めるか
- 前回Releaseが存在しない場合にどうするか

GitHub CLIはdraftを明示的に除外する`--exclude-drafts`を持ち、`createdAt`と`publishedAt`も別々に公開しています。この選択を実装任せにすると、draftや対象commitの祖先でないtagを起点にする余地があります。[GitHub CLI `gh release list`](https://cli.github.com/manual/gh_release_list)

また、`tasks.md:36`の自己テストは本文パーサーだけが対象で、PR選定ロジックを検査しません。dry-runは収集自体を省略するため、このままでは最初の結合確認が本番publish直前になります。

**推奨修正**:

- 起点を「今回versionを除いた、公開済み・非draftで、対象commitの祖先にあるReleaseのうち最も近いtag」など、一意に定義する。
- 前回Releaseなしの扱いを明記する。
- mocked Release一覧と一時git履歴を使い、draft、非祖先Release、Releaseなしtag、今回tag、同じPRが複数commitへ関連付く場合、main以外のPRを検査するタスクを追加する。

### [🟠 Major] release Skillに約束した責務の大半がScenarioとtasksに落ちていない

**該当箇所**: `kasane/decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md:39`、`exploration.md:37`、`specs/release-workflow/spec.md:109`、`tasks.md:44`

**問題点**: ADRとexplorationはrelease Skillの射程を次まで含むとしています。

- develop CIの事前確認
- docs-refresh依頼要否の確認
- release PRの作成
- workflowの起動
- 実行の監視
- 失敗時のhandbook案内
- 公開後の確認

しかしデルタスペックのScenarioは「下書き生成」と「handbookのコマンド変更追随」だけで、tasksもルーター作成・下書き・判断境界までです。PR作成やworkflow起動、監視、公開確認を欠いたSkillでも、現行のspecとtasksを満たしたことにできます。

**推奨修正**: 約束した各段について、観察可能な到達状態と対応タスクを追加してください。広い自動化を意図していないなら、ADR／exploration側の射程を「handbookへの案内と下書き生成」に縮める必要があります。

### [🟠 Major] ADR-0022の外部URL規則を「Releasesだけの例外」としても現行Skillと整合しない

**該当箇所**: `kasane/decisions/cross/0022-user-docs-as-agent-skills.md:23`、`kasane/decisions/cross/0029-install-examples-without-pinned-version.md:29`、`kasane/decisions/cross/0029-install-examples-without-pinned-version.md:30`、`skills/en/kssettingsview-ios/SKILL.md:30`、`skills/en/kssettingsview-aiforms-migration/references/api-mapping.md:350`

**問題点**: ADR-0022はSkill外のURLを禁止しています。ADR-0029は「Releasesへの案内に限って」緩和するとしていますが、現行Skillにはすでに次の外部URLがあります。

- SwiftPMの配布URL
- frontmatterの`metadata.source`
- Issue起票先URL

ADR-0029自身も「導入節には既に配布座標としてURLがある」と認めています。そのため「Releasesだけの例外」を追加しても、改訂後のADRと現行Skillの整合は回復しません。

**推奨修正**: URL文字列の種類ではなく目的で境界を定めてください。例えば「Skill外の文書へ知識を委ねる参照は禁止するが、正規の配布座標、source metadata、Release確認先、Issue窓口などの操作対象URLは許可する」と置き換えるのが一貫します。amends ADRとして、置き換える範囲と「ADR-0022の他の決定は維持する」ことも明記してください。

### [🟠 Major] validate jobの権限追加をそのまま実装するとcontents権限を失う余地がある

**該当箇所**: `design.md:128`、`tasks.md:38`、`.github/workflows/release.yml:35`、`.github/workflows/release.yml:103`

**問題点**: 現行はworkflowトップレベルに`contents: read`がありますが、taskはvalidate jobへ`pull-requests: read`を追加するとだけ指示しています。

GitHub Actionsではjobレベルに`permissions`を指定すると、列挙していない権限は`none`になります。したがって次のような素直な実装では`contents: read`が失われ、checkoutやcommit／Release情報の取得が壊れます。[GitHub Actions workflow syntax](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax#jobsjob_idpermissions)

**推奨修正**: taskまたはdesignに次の完全なjob権限を明記してください。

```yaml
permissions:
  contents: read
  pull-requests: read
```

`gh api`を使う設計なら、`GH_TOKEN: ${{ github.token }}`の受け渡しも実装タスクに含めてください。

### [🟡 Minor] 「英語版と日本語版が同一構成」の判定範囲が曖昧

**該当箇所**: `specs/install-examples/spec.md:35`、`specs/install-examples/spec.md:38`、`design.md:105`

**問題点**: 「同一構成」が、Skillディレクトリ名だけなのか、インストール宣言の種類・本数なのか、見出し構造まで含むのかが決まっていません。designの実装案は「Skill名の集合」と「対象表の宣言数」を検査するため、Requirementの字面より狭い保証になります。

**推奨修正**: 「en/jaで同じSkill名の集合を持ち、対応する各Skillが同じ種類・本数のインストール宣言を持つ」など、lintが保証する範囲へRequirementを限定してください。

## アクションプラン

1. release Skillの下書き範囲を今回PRの差分へ変更する。
2. 前回Releaseの選択規則を一意にし、PR選定ロジックの自己テストを追加する。
3. release Skillの約束した射程をScenarioとtasksへ展開する。
4. ADR-0029の外部URL例外を、現行Skillも包含する目的ベースの規則へ直す。
5. validate権限と英日構成の受け入れ基準を明文化する。
6. 改訂後に独立提案レビューを再実施する。

## 突き合わせ結果

ホスト側の自己レビューでは新たな指摘なし。相方の指摘 6 件は**全件を採用**した (降格・未解決なし)。前回 (spec-001) で指摘された 9 件は再指摘されておらず、反映が効いていると判断する。

| # | 指摘 | 採否 | 検証 |
|---|---|---|---|
| Major 1 | release Skill の下書き範囲により変更が二重掲載される | 採用 | workflow は前回 Release 以降の全 `main` PR を集め、スキルの下書きも「前回リリース以降の全変更」から作るため、先行 PR がある場合に同じ変更が 2 度載る。「重複なく並ぶ」Scenario と矛盾する。下書きの範囲は「今回の PR が `main` へ持ち込む差分」でなければならない |
| Major 2 | 「直前の Release」の選択規則が一意でなく、対象選定のテストもない | 採用 | draft の扱い・対象 commit の祖先かどうか・「直前」の判定基準・前回 Release 不在の扱いがいずれも未定であることを確認。自己テストも本文パーサーだけを対象にしていた |
| Major 3 | release Skill に約束した責務の大半が Scenario と tasks に落ちていない | 採用 | ADR と exploration は 7 段を射程としているが、Scenario は 2 本、tasks は 4 件しかない。射程は縮めずに Scenario と tasks を拡張する (オーナーが広い射程を承認済みのため) |
| Major 4 | ADR-0022 の外部 URL 規則を「Releases だけの例外」としても現行 Skill と整合しない | 採用 | `skills/en/` に既に 7 本の外部 URL が存在することを確認 (配布リポジトリ 2 本、本体リポジトリ 5 本。`metadata.source` と Issue 窓口を含む)。条項の字面は現時点で既に実態と合っておらず、種類ではなく目的で境界を定める必要がある |
| Major 5 | validate job の権限追加をそのまま実装すると contents 権限を失う余地がある | 採用 | job レベルの `permissions` は列挙しないものを `none` にするため、`pull-requests: read` だけ足すと checkout が壊れる |
| Minor 1 | 「英語版と日本語版が同一構成」の判定範囲が曖昧 | 採用 | Requirement の字面が design の実装案より広い |
