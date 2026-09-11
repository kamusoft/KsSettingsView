# レビュー結果: install-examples-and-release-notes (002 回目)

**日付**: 2026-09-11
**判定**: APPROVED

## サマリー

1 周目の指摘 6 点はいずれも解消を確認した。Major 2 件 (`overwrite: true` の欠落・ページ送りの自己テストが本番実装を通らない) は実測で裏を取っており、特に後者は同じミューテーション (`GitHubApi.paged` を 1 ページ打ち切りに退行) で今回 4 件が落ちる — 1 周目は全件緑だった検出力の穴が実際に塞がっている。

条件式を `Decide release notes scope` に一本化した分岐は、`main` 以外からの `dry-run` / `main` からの `dry-run` / 本番の 3 経路をいずれも意図どおりに通る。新しく持ち込まれた欠陥は見つからなかった。残る指摘は Suggestion 2 件のみで、いずれも今の成果物に実害は無い。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (コメント構文を持つ全ソース。本 change は `.py` 2 本と workflow 2 本・PR テンプレートを触る) |
| `kasane/handbook/cross/install-examples.md` | README 2 枚と `skills/` のインストール例を書き換えるとき (本 change が新設した規約そのもの) |
| `kasane/handbook/cross/release-procedure.md` | リリース手順・再実行 / リハーサルの記述を触るとき |
| `kasane/handbook/cross/user-skill-api-listing.md` | `skills/` を触るとき |
| `kasane/handbook/cross/test-execution.md` | テストを実行し結果を報告するとき (本 change は platform ソースに触れないため、対象は script の自己テストと lint) |
| `kasane/decisions/cross/0029` / `0030` | いずれも `proposed`。衝突は無く、所見として挙げる点も無い |

`kasane/lessons/code-review.md` の L-001 (ミューテーションによる検出力の実測) を今回も適用した。結果は下表。

## 1 周目の指摘の解消確認

| # | 1 周目の指摘 | 状態 | 根拠 |
|---|---|---|---|
| 1 | `Upload release notes` に `overwrite: true` が無い (Major) | 解消 | `.github/workflows/release.yml:215`。workflow 内の `upload-artifact` 7 箇所すべてが `overwrite: true` を持つことを YAML 解析で確認 (`:215` `:286` `:351` `:447` `:792` `:803` `:995`)。他 3 箇所と同じ趣旨のコメントも添えられている |
| 2 | ページ送りの自己テストが本番実装を通っていない (Major) | 解消 | `scripts/release/build-release-notes.py:342` の `PagedGitHubApi` が `GitHubApi` を継承し `_get` だけを差し替える形になり、`paged` は本物を通る。`PagingApi` は撤去済み。ミューテーション実測は下記 |
| 3 | 再実行条件が同一 commit の tag と別 commit の同名 tag を区別していない (Minor) | 解消 | `kasane/handbook/cross/release-procedure.md:209`。「別 commit を指す同名 tag は出し直せない / 起動した commit を指す場合は再実行として続行する」に分割され、`.github/workflows/release.yml:118-132` (`Verify monorepo tag`) の実装と一致する |
| 4 | 「`## Changes` がそのまま Release 本文になる」の説明が実装と食い違う (Minor) | 解消 | 同 handbook `:160`。「書いた順や字面がそのまま出るのではなく、workflow が項目を種別別に再編し、種別の見出し・pull request 番号・比較リンクを付けて整形する」に改められ、`render_notes` の挙動と一致する |
| 5 | `render` サブコマンドに案内が無い (Suggestion) | 解消 | 同 handbook `:211-217`。「publish に入る前 (validate) の失敗」節に、記載の不備を手元で確かめる用途として実行例つきで置かれた。`render` は API も git も使わないという性質も添えられている |
| 6 | リハーサル分岐の条件式が 3 箇所に散っている (Suggestion) | 解消 | `.github/workflows/release.yml:173-186` の `Decide release notes scope` で一度だけ評価し、後続 3 step (`:189` `:208` `:219`) は `steps.notes-scope.outputs.collect` だけを見る。条件式の字面の重複は無くなった |

`kasane/concepts/cross/architecture/release-pipeline.md` の削除済みスクリプト参照は、指揮側の判断で蒸留へ繰り延べ済み (`deviation.md` に記録) のため再指摘しない。未チェックの tasks 8.3b / 8.3c / 8.4 / 8.5 / 8.6 も指摘対象にしていない。

## 実行した検証

| 実行 | 結果 |
|---|---|
| `python3 scripts/release/build-release-notes.py --selftest` | 37 件 OK / 0 件 NG (1 周目 35 件から +2。ページ送りが 3 件 → 5 件) |
| `python3 scripts/install-example-lint.py` (+ `--selftest`) | exit 0 (「10 ファイルのインストール例 14 行が契約を満たす」) / 19 件 OK |
| `python3 scripts/release/check-publish-step-order.py` (+ `--selftest`) | exit 0 / 7 件 OK |
| `python3 scripts/release/check-time-budget.py` | exit 0 (余裕 8 分) |
| `python3 scripts/readme-example-lint.py` | exit 0 |
| `local-path-lint` / `identity-lint` / `doc-structure-lint` / `comment-policy-lint --advisory` | いずれも exit 0。comment-policy は禁止 0 件 (要確認 164 件はすべて `samples/` の既存分)。doc-structure の指摘に `kasane/handbook/cross/release-procedure.md` と `install-examples.md` は含まれない |
| **ミューテーション実測 1** (L-001): `GitHubApi.paged` の本文を `return self._get(path, 1)` (1 ページ打ち切り) に退行 | **4 件 NG** (「ページ送りで全件を取る」「最後のページまで辿る」「2 ページ目の項目がノートに載る」「上限ちょうどのページの後も次を要求する」)。1 周目は同じ退行で全 35 件が緑だった。実測後、backup との shasum 一致 (`6868db28…`) で原状復帰を確認 |
| **ミューテーション実測 2**: `Download release notes` を `Download previous deployment id` より前へ移した写しを `check-publish-step-order.py --release-yml` に掛ける | exit 1。新設した download step も引き継ぎ順序の検査対象に正しく入っている (写しは作業ツリー外に置き、`.github/workflows/release.yml` は無改変を shasum で確認) |
| 足場の凍結 | `specs/` `proposal.md` `design.md` `exploration.md` に差分なし。change 内の変更は `tasks.md` のチェックと `deviation.md` / `review-001.md` / `second-opinion-code-001.md` の新規追加のみ |
| `deviation.md` の 5 件 | 付随修正 4 件はいずれも本務で触るファイル内・局所・自己テストで担保され、ksn-core の同梱条件に収まる。5 件目 (tasks 9.6 の走査結果) は繰り延べの記録。無断の仕様逸脱は見つからなかった |
| `set-readme-version.py` / `.github/release.yml` への参照 | 現行の規範・実装・利用者向け文書には残っていない。残るのは archive / roadmap / ADR-0029・0030 の経緯記述と、繰り延べ済みの `kasane/concepts/cross/architecture/release-pipeline.md:65` のみ |

### 分岐の静的追跡 (3 経路)

`Decide release notes scope` は `[ "${KS_DRY_RUN}" != "true" ] || [ "${KS_REF}" = "refs/heads/main" ]` を 1 度だけ評価して `collect` を出す。`if:` を状態関数なしで書いた step には `success()` が暗黙に掛かるため、先行 step が失敗したときは 3 step とも実行されない。

| 起動 | `collect` | 走る step | publish (`if: !inputs['dry-run']`) | spec との対応 |
|---|---|---|---|---|
| `dry-run` / `main` 以外 | `false` | `Skip release notes` のみ | skip | Scenario「開発ブランチからのリハーサルでは対象を集めない」— 記載が無いことを理由に失敗しない |
| `dry-run` / `main` | `true` | `Build release notes` → `Upload release notes` | skip | Scenario「main からのリハーサルでは実際の経路を通る」— 収集・検査・整形・成果物への受け渡しまで本番と同じ。Release だけ作られない |
| 本番 (`dry-run` false) | `true` | `Build release notes` → `Upload release notes` | 実行。`Download release notes` で受け取り、`Create GitHub Release` が `--notes-file` に使う | Scenario「検査の後に本文が編集されても公開内容が変わらない」— publish は pull request 本文を読み直さない |

本番の起動 ref は `Validate inputs` が `main` に限っているため、「本番かつ `main` 以外」の経路は到達しない。`Re-run all jobs` での再走も、`Verify monorepo tag` が起動 commit を指す tag を再実行として受理し、`select_base` が `tag == version` の Release を除くため、起点と対象は初回実行と同じになる (Scenario「今回の tag がある再実行でも対象が変わらない」)。

## 指摘事項

### [🔵 Suggestion] `overwrite: true` の有無を機械検査で押さえていない

**該当箇所**: `.github/workflows/release.yml:215` ほか `upload-artifact` 7 箇所、`scripts/release/check-publish-step-order.py`

**問題点**: 今回 7 箇所すべてが `overwrite: true` を持つことは確認できたが、それを守らせているのはコメントと慣例だけである。1 周目の欠落がそうであったように、この属性は平時の実行では緑のままで、`Re-run all jobs` を選んだときにだけ露見する。次に upload を増やす人が同じ穴に落ちる余地は残っている。

なお `check-publish-step-order.py` と `check-time-budget.py` は、まさに「崩れても平時は緑のまま」という同じ性質の不変条件を機械検査に落としている前例である。

**推奨修正**: 任意。`release.yml` の全 `upload-artifact` step が `overwrite: true` を持つことを検査する数行を、既存の検査スクリプトのいずれかに足す (自己テストは「1 箇所落とすと失敗する」1 ケースで足りる)。実害が出ていないので、見送っても構わない。

### [🔵 Suggestion] 対象の収集が範囲内 commit 数に比例して API を呼ぶ

**該当箇所**: `scripts/release/build-release-notes.py:268-284` (`collect_pulls`)

**問題点**: `collect_pulls` は範囲内の commit 1 つにつき `/repos/{repo}/commits/{sha}/pulls` を 1 回以上呼ぶ。`commits_in_range` は `--first-parent` を使わないため、集約 pull request が持ち込んだ develop 側の commit もすべて範囲に入る (実測: `0.1.0-beta.1..0.1.0-beta.2` は first-parent で 1 件、全体で 50 件)。1 回のリリースあたりの照会は「範囲の全 commit 数 + Release 一覧 1 回」になり、リリース間隔が延びるほど増える。

`GITHUB_TOKEN` のレート上限 (リポジトリあたり 1,000 requests/hour) に対して現状は十分余裕があり、超過しても `urllib.error.HTTPError` が `URLError` として捕まって publish の前に止まる (`:671-673`) ため、静かに不完全なノートが出る経路にはならない。現時点で実害は無い。

**推奨修正**: 任意。ただし範囲の取り方はデルタスペック (「起点と今回のリリース対象 commit の間に入った commit に紐づく pull request」) が定めているため、`--first-parent` へ狭めるなどの変更は実装側では決められない。将来リリース間隔が大きく延びたときに、spec 側の論点として扱う。

## アクションプラン

1. なし (Critical / Major / Minor いずれも無し)
2. Suggestion 2 件は任意。採るなら 1 件目 (`overwrite: true` の機械検査) のほうが、同じ型の欠落を 1 周目に実際に出している分だけ費用対効果が高い
3. 残る前提は `tasks.md` の 8.3b / 8.3c / 8.4 / 8.5 / 8.6 (remote への push 後に指揮側が実施予定)。8.3b と 8.3c は上表の分岐追跡を実地で裏取りする位置づけで、静的追跡の結論と食い違った場合はここへ戻る
