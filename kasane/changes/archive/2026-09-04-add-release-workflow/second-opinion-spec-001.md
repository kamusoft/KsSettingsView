# セカンドオピニオン: add-release-workflow (spec-001)
**相方**: codex / **label**: so-spec-add-release-workflow / **日付**: 2026-09-03 / **対象**: proposal.md / design.md / specs/ (4 能力) / tasks.md (提案一式)
---
NEEDS_DISCUSSION

## サマリー

公開後に修復困難となる Critical 1 件と、再実行性・並行実行・バージョン同一性などに関する Major 7 件があります。このままでは「全 platform の lockstep」「同じ version で再実行可能」という中核要件を保証できません。

指摘件数: Critical 1 / Major 7 / Minor 2 / Suggestion 0

静的レビューのみ実施し、ビルド・テスト・ファイル書き込みは行っていません。

## 照合した規約

- `ksn-review` の判定基準・指摘形式
- `ksn-propose` の spec-review 整合性チェック
- `kasane/lessons/spec-review.md` L-001 / L-002
- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/public-identifiers.md`（配布座標）
- cross/ADR-0018 / 0019 / 0020 / 0025 / 0026

## 指摘事項

### [🔴 Critical] 配信リポジトリの tag 衝突を不可逆 publish 後に検出している

**該当箇所**: `specs/release-workflow/spec.md:23`、`specs/release-workflow/spec.md:51`、`design.md:67`、`tasks.md:29`

**問題点**: 配信リポジトリの同名 tag が異なるスナップショットを指す場合、検出は Maven release と NuGet push の後です。この時点では Maven/NuGet の version は取り消せず、SPM tag も既存 tag を改変しない限り作れないため、同じ version の lockstep を回復できません。

さらに、フェーズ決定では validate が「monorepo と配信リポジトリの tag 重複検査」を行うことになっており、提案への転記時に配信リポジトリ側が抜けています（`kasane/roadmaps/package-distribution/phases/phase-8-release-workflow/agenda.md:24`）。

**推奨修正**: 期待するスナップショットの tree を生成した後、Maven upload / NuGet push より前に既存 SPM tag の有無と tree 一致を検査する Requirement / Scenario を追加してください。tag 作成直前にも競合を再確認します。

### [🟠 Major] release run の並行実行が未定義

**該当箇所**: `design.md:30`、`tasks.md:28`

**問題点**: workflow/job の `concurrency` が仕様にもタスクにもありません。GitHub Actions は既定で複数 run を並行実行し、Environment 自体は排他制御になりません。同じ version の二重 dispatch が SPM push、Central deployment、NuGet push を競合させると、単一 run 内の冪等性だけでは防げません。[GitHub の公式仕様](https://docs.github.com/en/actions/how-tos/deploy/configure-and-manage-deployments/control-deployments)も Environment と concurrency は独立だとしています。

**推奨修正**: 少なくとも `publish` job にリポジトリ固定の concurrency group と `cancel-in-progress: false` を要求し、ロック取得後に外部状態を再検査してください。「2 run が同時に publish へ進もうとしても外部書き込みは直列になる」Scenario も必要です。

### [🟠 Major] Central release の応答不明時に同じ version の再実行が成立しない

**該当箇所**: `design.md:16`、`design.md:56`、`specs/release-workflow/spec.md:64`、`specs/release-workflow/spec.md:116`、`tasks.md:13`、`tasks.md:29`

**問題点**: release API がサーバー側では受理されたものの、通信切断などで step が失敗した場合、deployment は `PUBLISHING` なのに cleanup が `DELETE` を試みます。Sonatype の公式仕様では drop 可能なのは `VALIDATED` / `FAILED` だけで、`PUBLISHING` / `PUBLISHED` は削除できません。[Portal API の状態仕様](https://central.sonatype.org/publish/publish-portal-api/)

再実行時には前回 deployment ID を保持しておらず、repo1 への反映前なら「未公開」と誤判定して同じ GAV を再 upload します。したがって「失敗した job から再実行するだけで完了」は保証されません。

**推奨修正**:

- deployment ID を run をまたいで復元可能な形で保存する
- 再実行時は ID の状態を先に照会し、`VALIDATED` は release、`PUBLISHING` は待機、`PUBLISHED` は skip、`FAILED` は整理して再 upload とする
- `DELETE` は状態確認後、drop 可能な状態だけで実行する
- cancel・runner 消失・release 応答不明の Scenario と手動復旧手順を追加する

### [🟠 Major] `main = 最新リリース` の契約が満たせず、取り込み元も制限されていない

**該当箇所**: `specs/verification-ci/spec.md:6`、`design.md:86`、`design.md:91`、`tasks.md:44`、`tasks.md:51`

**問題点**: リリース PR は README を次の version に書き換えて `main` へマージした後で dispatch します。そのためマージから publish 成功まで、または publish が失敗している間、`main` は「最新リリース」ではなく未公開のリリース候補を表します。

また、指定されている branch protection は PR・7 checks・force-push/削除禁止だけです。現在の CI は任意の `main` 向け PRで7 checksを起動するため、feature branch → main も全検査に通ればマージ可能です。「誤って main に向けても protection と CI で止まる」という `proposal.md:34` の説明は成立しません。

**推奨修正**: 次の設計判断を明文化してください。

- `main` を「最新公開版」ではなく「次回リリース候補を含み得る」と定義する、または失敗時の復元手順を設ける
- `main` 向け PR の head を `develop` に限定する CI 検査または ruleset を追加する
- リリース準備中・publish 失敗中・成功後それぞれの `main` / README / tag の期待状態を Scenario 化する

### [🟠 Major] semver 検査が先頭ゼロを受理し、NuGet と tag の version がずれる

**該当箇所**: `specs/release-workflow/spec.md:6`、`tasks.md:28`、`tasks.md:36`

**問題点**: `X / Y / Z / N は数字` および予定正規表現は `01.0.0` や `1.0.0-beta.01` を受理します。SemVer 2.0.0 の数値識別子は先頭ゼロを禁止しています。[SemVer 2.0.0](https://semver.org/)

さらに NuGet は version の先頭ゼロを正規化するため、dispatch/tag が `01.0.0`、NuGet が `1.0.0` となり、文字列同一という ADR-0020 の前提が壊れます。[NuGet version normalization](https://learn.microsoft.com/en-us/nuget/concepts/package-versioning)

**推奨修正**: 各数値部分を `0|[1-9][0-9]*` とし、`01.0.0`、`1.00.0`、`1.0.0-beta.01` の負ケースを追加してください。

### [🟠 Major] NuGet 3 パッケージの公開順と反映待ちが不完全

**該当箇所**: `specs/release-workflow/spec.md:90`、`design.md:51`、`tasks.md:15`、`tasks.md:29`

**問題点**: facade と binding 2 件をどの順で push するかが未規定です。facade が先に公開されると、binding の push 失敗中に解決不能な公開 package が残ります。

また反映待ちは `KsSettingsView.Maui` の index だけを確認し、binding 2 件を確認しません。facade が見えて binding がまだ取得不能な状態で smoke が始まり、仕様上不要な失敗になります。

**推奨修正**:

- `Binding.Android` / `Binding.iOS` を先、facade を最後に push する
- 各 main nupkg の push に対応する snupkg の扱いを明記する
- flat-container で3 Package IDすべての当該 versionまたはnupkg取得可能性を待つ
- binding 1件のpush後に失敗した再実行と、facadeだけ先に見える状態のScenarioを追加する

### [🟠 Major] Trusted Publishing の positive control は指定された Environment policy では実行できない

**該当箇所**: `tasks.md:7`、`tasks.md:45`

**問題点**: task 1.3 は draft PR 上の一時 workflow で `environment: release` を使いますが、Environment の作成と secrets 登録は task 6.2 です。また branch policy を `main` のみにすると、PR run の `GITHUB_REF` は `refs/pull/.../merge` なので Environment を参照できません。[GitHub Environment の branch/tag rule](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments)

NuGet 側の policy は workflow filename `release.yml` と Environment 名まで照合するため、「一時 workflow」が別名なら OIDC 交換の正ケースにもなりません。[NuGet Trusted Publishing](https://learn.microsoft.com/en-us/nuget/nuget-org/trusted-publishing)

**推奨修正**: positive control の実行時期と正確な workflow file/ref を決めてください。候補は、Environment 設定後に実際の `release.yml` を `main` から login-only で実行する方法、または一時的に PR ref を許可し、検証後に `main` 限定へ戻したことまで証跡化する方法です。

### [🟠 Major] Android dry-run と実際の publish 成果物の同一性を保証していない

**該当箇所**: `design.md:38`、`design.md:40`、`design.md:44`、`design.md:134`

**問題点**: dry-run 対象は Ubuntu の package job が作った未署名 Maven 成果物ですが、公開物は macOS の publish job が再ビルドします。「同一 commit・同一 JDK」でも OS が異なり、差が署名だけであることを仕様・検査のどちらも保証していません。`design.md:9` の「配布物そのものを dry-run」とも条件付きでしか整合しません。

**推奨修正**: Android は同一物検証の例外であることを Requirement 本文に明記し、package job と publish job の aar / pom / module / sources / javadoc を署名ファイルと許容メタデータを除いて比較する検査を追加してください。比較できないなら、dry-run が保証する範囲を明示的に縮める必要があります。

### [🟡 Minor] `Directory.Build.targets` を「新設」としているが既に存在する

**該当箇所**: `tasks.md:22`、`maui/Directory.Build.targets:1`

**問題点**: 既存ファイルは buildTransitive import と package icon の同梱を担っています。「新設」のまま実装すると既存内容を置き換える、または責務を見落とす危険があります。

**推奨修正**: 「既存 `maui/Directory.Build.targets` に追記し、既存 Import / icon ItemGroup を維持する」と訂正し、既存機能の回帰確認を task 3.4 に加えてください。

### [🟡 Minor] 実装コメントが change 内の Decision 番号へ依存する書き方になっている

**該当箇所**: `tasks.md:22`

**問題点**: 「design Decision 10 の代替 B をコメントに残す」と読める指示は、作業文書やローカル Decision 番号をソースコメントから参照してはいけない `kasane/handbook/cross/comment-policy.md:26` に抵触します。

**推奨修正**: コメントには Decision 番号を書かず、「現在の除外方式が成立しなくなった場合は、`_CreateAar` 前に native item を除外する方式を再検討する」のように、そのファイルだけで意味が閉じる現在形の説明を要求してください。

## アクションプラン

1. SPM tag の事前衝突検査と publish 排他制御を仕様化する。
2. Central deployment ID の永続化・状態別再開・cleanup の限界を設計し直す。
3. `main` の状態モデルと `develop → main` 以外を拒否する仕組みを決める。
4. semver、NuGet 3 package、Android成果物同一性の受け入れ基準を追加する。
5. Trusted Publishing positive control の実行順序を修正する。
6. `Directory.Build.targets` とコメント規約の局所的不整合を直す。



---

## 突き合わせ結果 (2026-09-03)

ホスト側の自己レビュー (2 周、検査軸: Requirement 同士の交差 / 既存 workflow・SDK の実体との突合 / 決定が作る責務の閉路) は指摘 3 件 (手順書の置き場所・配信リポジトリ tag の判定場所・能力名) を自己修正して完了していた。相方の指摘 10 件はいずれも該当箇所と実害シナリオを伴い、ホスト側の見逃しとして扱う。

| # | 指摘 | 採否 | 反映先 |
|---|---|---|---|
| 1 | [Critical] 配信リポジトリの tag 衝突を不可逆 publish 後に検出 | **採用** | validate で https clone + スナップショット生成 + ツリー比較 (spec「手動起動と入力の検証」、design Decision 5 / 6、tasks 4.1 / 5.1) |
| 2 | [Major] 並行実行が未定義 | **採用** | `concurrency: release` (cancel-in-progress false) + ロック後の再検査 (spec「段の構成と順序」、design Decision 3、tasks 4.1 / 5.3) |
| 3 | [Major] Central release の応答不明時に再実行が成立しない | **採用** | deployment ID を run artifact に保存し状態で分岐、drop は VALIDATED / FAILED のみ (spec「Maven Central の 2 段操作」、design Decision 1 / 5、tasks 1.1 / 1.4 / 2.1 / 4.2 / 5.6) |
| 4 | [Major] `main` = 最新リリースの契約が満たせず、取り込み元も制限されていない | **採用** | `main` の定義を「最新リリースまたはリリース進行中の候補」に、`main` 向け PR の head を `develop` に限定する lint 検査 (spec verification-ci「マージ保護」、design Decision 6、tasks 4.6 / 5.7、proposal Impact) |
| 5 | [Major] semver 検査が先頭ゼロを受理 | **採用** | 数値部を `0\|[1-9][0-9]*` に、負ケース 3 種を追加 (spec「手動起動と入力の検証」、tasks 4.1 / 5.1) |
| 6 | [Major] NuGet 3 パッケージの公開順と反映待ちが不完全 | **採用** | binding 2 件 → facade、nupkg と snupkg を対で、反映待ちは 3 Package ID (spec「NuGet.org への push」「反映待ちと smoke」、design Decision 5、tasks 2.3 / 4.2) |
| 7 | [Major] Trusted Publishing の positive control が指定の Environment policy では実行できない | **採用** | 事前検証は不可と認め spike を外す。login は不可逆操作前の位置にあり失敗しても戻れることを design Risks / proposal Impact に明記 (tasks 1.3 を差し替え) |
| 8 | [Major] Android dry-run と publish 成果物の同一性を保証していない | **採用** | publish job を ubuntu (package-android と同じ OS・JDK) に移し、署名を除く比較で差異があれば upload しない Requirement を追加。再現性は spike で先に実測 (spec「Android 成果物の同一性」、design Decision 3、tasks 1.3 / 2.6 / 4.2 / 5.4) |
| 9 | [Minor] `Directory.Build.targets` は既存 | **採用** | 「既存へ追記、既存機能を維持」に訂正、回帰確認を 3.4 に追加 (design Decision 10、tasks 3.2 / 3.4) |
| 10 | [Minor] コメントが Decision 番号に依存 | **採用** | コメントはファイル内で閉じる現在形に (tasks 3.2) |

採用 10 / 降格 0 / 未解決 0。相方の判定 NEEDS_DISCUSSION は、上記の反映により設計判断がすべて提案内で閉じたため、ホスト側は提案の完成として扱う (オーナー確認はハンドオフで行う)。
