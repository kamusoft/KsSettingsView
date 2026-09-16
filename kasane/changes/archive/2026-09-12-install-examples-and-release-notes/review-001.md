# レビュー結果: install-examples-and-release-notes (001 回目)

**日付**: 2026-09-11
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペックの Requirement / Scenario はおおむね実装されており、インストール例のプレースホルダ化・lint・置換機構の撤去・Release ノートの組み立て・リリース用スキル・handbook の追随はいずれも成果物として揃っている。lint と自己テストは全件成功し、commit と pull request の関連付けという中心の前提も実リポジトリへの読み取り照会で成立を確認できた。

一方で、先行 change が積み上げた「再実行で回復できる」性質を 1 箇所崩している (`Upload release notes` だけ `overwrite: true` を持たない) ことと、tasks 5.3e が名指しした「取りこぼしは静かに成功する」経路の回帰検出力が実測でゼロだったことの 2 件が Major。いずれも修正は局所的で、設計の作り直しは要らない。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (コメント構文を持つ全ソース。本 change は `.py` 2 本と workflow 2 本を触る) |
| `kasane/handbook/cross/install-examples.md` | README 2 枚と `skills/` のインストール例を書き換えるとき (本 change が新設した規約そのもの) |
| `kasane/handbook/cross/release-procedure.md` | リリース手順・再実行 / リハーサルの記述を触るとき |
| `kasane/handbook/cross/user-skill-api-listing.md` | `skills/` を触るとき |
| `kasane/handbook/cross/test-execution.md` | テストを実行し結果を報告するとき |
| `kasane/decisions/cross/0029` / `0030` | いずれも `proposed`。衝突は無く、所見としても挙げる点は無い |

`kasane/lessons/code-review.md` の L-001 (ミューテーションによる検出力の実測) を適用した。指摘 2 はその実測結果である。

## 実行した検証

| 実行 | 結果 |
|---|---|
| `python3 scripts/install-example-lint.py` | exit 0 (「10 ファイルのインストール例 14 行が契約を満たす」) |
| `python3 scripts/install-example-lint.py --selftest` | 19 件 OK / 0 件 NG |
| `python3 scripts/release/build-release-notes.py --selftest` | 35 件 OK / 0 件 NG |
| `python3 scripts/release/check-publish-step-order.py` (+ `--selftest`) | exit 0 / 7 件 OK |
| `python3 scripts/release/check-time-budget.py` (+ `--selftest`) | exit 0 |
| `python3 scripts/readme-example-lint.py` | exit 0 |
| `local-path-lint` / `identity-lint` / `comment-policy-lint --advisory` | exit 0 / 禁止 0 件 (要確認 164 件はすべて `samples/` の既存分) |
| commit → pull request の関連付け (読み取り照会) | `main` の集約 merge commit と範囲内の非 merge commit の双方が base `main` の pull request を返すことを実リポジトリで確認。既定ブランチが `main` であるため「既定ブランチに無い commit は open な pull request しか返らない」制約にも当たらない |
| 既発行 Release の印 (tasks 4.2 / 4.3) | `0.1.0-beta.1` / `0.1.0-beta.2` とも `prerelease: false` / `draft: false`、`releases/latest` は `0.1.0-beta.2` に解決 |
| 足場の凍結 | `specs/` `proposal.md` `design.md` に差分なし。変更は `tasks.md` のチェックと `deviation.md` の新規のみ |
| `deviation.md` の付随修正 4 件 | いずれも本務で触るファイル内・公開 API / スキーマ / accepted ADR に触れず・局所・既存または追加の自己テストで担保。同梱条件に収まる |

未チェックの 8.3b / 8.3c / 8.4 / 8.5 / 8.6 は指摘対象にしていない。なお「実地でしか確かめられない設計になっていないか」の観点では、解析・整形・対象選定・成果物の受け渡しがすべて `--selftest` と `main` からの `dry-run` で到達できる形になっており、本番リリースまで一度も通らない経路は残っていない (ただし指摘 2 の範囲を除く)。

## 指摘事項

### [🟠 Major] `Upload release notes` に `overwrite: true` が無く、`Re-run all jobs` が validate で止まる

**該当箇所**: `.github/workflows/release.yml:189-194`

**問題点**: この workflow の他の upload 5 箇所 (`:258-265` iOS / `:323-330` Android / `:419-426` MAUI / `:766-771` `:776-782` `:969-974` deployment id) はすべて `overwrite: true` を持ち、iOS / Android / MAUI の 3 箇所には「`Re-run all jobs` で同じ名前を作り直せるようにする」というコメントまで添えられている。新設した `Upload release notes` だけがこれを持たない。

artifact は同一 run の attempt をまたいで残り、`actions/upload-artifact` v4 以降は同じ run に同名 artifact が既にあるとアップロードが失敗する。したがって、

- validate が成功した run を `Re-run all jobs` で回し直すと、validate が再走して `release-notes` の作成で衝突し、**validate の段で run 全体が止まる**。
- これは publish の途中 (NuGet へ push 済み・Maven Central へ release 済みなど) で失敗した後に `Re-run all jobs` を選んだ場合にも起きる。片側だけ公開された状態からの回復手段が 1 つ塞がれる。

`kasane/handbook/cross/release-procedure.md` の「publish の途中での失敗」節は、まさにこの本文の中で「`Re-run all jobs` でも成立する (artifact は同名を上書きし、保留中の deployment ID は attempt をまたいで引き継がれる)」と書いており、本 change はその節を編集しながら記述を成立させない変更を同じ commit に入れている。先行 change `backport-release-workflow-hardening` が整えた再実行の回復性を、1 行の欠落で部分的に崩している。

**推奨修正**: `Upload release notes` に `overwrite: true` を足す (他の 3 箇所と同じ趣旨のコメントを添えると、次に upload を増やす人が同じ穴に落ちない)。あわせて、`upload-artifact` を使う step が `overwrite: true` を持つことを `scripts/release/check-publish-step-order.py` と同じ性質の機械検査で押さえることも検討に値する — この欠落は平時の実行では緑のままで、再実行したときにだけ露見する類型である。

### [🟠 Major] ページ送りの自己テストが実装を通っておらず、検出力が実測でゼロ

**該当箇所**: `scripts/release/build-release-notes.py:252-266` (`GitHubApi.paged`) と `:342-355` (`PagingApi`) / `:578-592` (`selftest_paging`)

**問題点**: `selftest_paging` が検査しているのは `PagingApi.paged` であり、これは `GitHubApi.paged` と同じループを自己テスト側に書き写した別実装である。実装本体の `GitHubApi.paged` はどの自己テストからも呼ばれていない。

L-001 に従い実測した。`GitHubApi.paged` の本文を 1 ページ目だけを返す形 (`return self._get(path, 1)`) に置き換えて `--selftest` を走らせたところ、**「ページ送りで全件を取る」「最後のページまで辿る」「2 ページ目の項目がノートに載る」の 3 件を含む全 35 件が緑のまま**だった (実測後、backup との shasum 一致で原状復帰を確認済み)。つまり実装のページ送りが完全に壊れても自己テストは検出しない。

tasks 5.3e がこの自己テストを要求した理由は「取りこぼしは失敗せず不完全なノートとして成功するため」であり、静かに項目が欠けることを防ぐのが目的である。現状はその防壁が名目だけあって効いていない。むしろ「検査済み」に見えるぶん、自己テストが無い状態より危うい。

**推奨修正**: `GitHubApi` を継承して `_get(path, page)` だけを差し替えた fake を自己テストに置き、本物の `paged` を駆動する。ページ境界 (最後のページがちょうど `PER_PAGE` 件で、次ページが空で返る場合を含む) と、要求した page 番号の並びを押さえると、上のミューテーションで確実に落ちる。`PagingApi` はその際に不要になる。

### [🟡 Minor] `kasane/concepts/` が削除済みスクリプトを手順として指したまま残る

**該当箇所**: `kasane/concepts/cross/architecture/release-pipeline.md:29`、`:65`

**問題点**: `:29` は validate の役割に「インストール例が入力 version と一致することを検査する」を挙げ、`:65` は「リリース PR の中で `scripts/release/set-readme-version.py` が置換し、validate が一致を検査する」と書いている。どちらも本 change で成立しなくなり、`:65` は削除済みのファイルを名指ししている。

これは `tasks.md` の「蒸留への申し送り」に明記された意図的な繰り延べなので、無断の逸脱ではない。ただし `proposal.md` の What Changes 6 が handbook と `AGENTS.md` を蒸留送りにしなかった理由 —「削除するスクリプトを指す記述が残ると、実装完了から蒸留までの間、エージェントが存在しないスクリプトを指示される」— は、`AGENTS.md` が「知識の正は kasane/handbook/ と kasane/concepts/」と宣言している以上、`:65` にもそのまま当てはまる。

**推奨修正**: 判断は指揮側に委ねる。蒸留を待たずに直すなら `:29` の一致検査の記述を落とし、`:65` を「インストール例は具体 version を持たず、リリースは書き換えない」に置き換える 2 箇所で済む。繰り延べるなら、蒸留までの間に release 周りを触るエージェントがこの 2 行を読む可能性を織り込んでおく。

### [🔵 Suggestion] `render` サブコマンドに呼び出し元が無い

**該当箇所**: `scripts/release/build-release-notes.py:628-646`

**問題点**: `render` は workflow・handbook・リリース用スキルのいずれからも呼ばれていない。Scenario「ノートの組み立てが単独で検査できる」は `--selftest` が満たしており、`render` はそれとは別に入力 JSON を受け取る経路を 1 つ増やしている。手で組み立てを試す口として有用ではあるが、どこにも案内が無いため使われないまま残る可能性が高い。

**推奨修正**: 残すなら `kasane/handbook/cross/release-procedure.md` の失敗時の節に「記載の不備を手元で確かめる」用途として 1 行案内を置き、残さないなら削る。どちらでもよい (実害は無い)。

### [🔵 Suggestion] リハーサル分岐の条件式が 3 箇所に散っている

**該当箇所**: `.github/workflows/release.yml:170`、`:189`、`:198`

**問題点**: `!inputs['dry-run'] || github.ref == 'refs/heads/main'` が `Build release notes` と `Upload release notes` に同じ字面で 2 度書かれ、`Skip release notes` にその否定が書かれている。将来どちらか片方だけを触ると、「収集は飛ばしたのに upload は走り、`if-no-files-found: error` で落ちる」という、条件の意味とは無関係な失敗になる。

**推奨修正**: 条件を一度だけ評価して `steps.<id>.outputs` か job 出力に落とし、3 箇所はそれを見る形にする。あるいは `Upload release notes` を `if: steps.<build の id>.outcome == 'success'` にして、build に従属させる。

## アクションプラン

1. `Upload release notes` に `overwrite: true` を足す (Major 1)。`Re-run all jobs` は handbook が明示的に成立すると書いている経路であり、publish 途中失敗からの回復手段に関わる
2. ページ送りの自己テストを `GitHubApi.paged` を通る形に書き直し、同じミューテーション (1 ページ打ち切り) で落ちることを実測で確かめる (Major 2)
3. `kasane/concepts/cross/architecture/release-pipeline.md` の 2 行を今直すか蒸留まで繰り延べるかを指揮側が決める (Minor 1)
4. Suggestion 2 件は任意。条件式の一本化 (Suggestion 2) は、Major 1 を直すついでに触る箇所と近いので同時に済ませやすい
