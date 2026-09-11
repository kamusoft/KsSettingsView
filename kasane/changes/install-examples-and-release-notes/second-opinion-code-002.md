# セカンドオピニオン: install-examples-and-release-notes (code-002)

**相方**: codex / **label**: so-code-install-examples-and-release-notes / **日付**: 2026-09-11 / **対象**: 修正サイクル 1 周目の後の作業ツリー (`release.yml` / `build-release-notes.py` / `release-procedure.md` の 3 ファイルが更新)

---

# レビュー結果: install-examples-and-release-notes（再レビュー）

**日付**: 2026-09-11  
**判定**: **APPROVED**

## サマリー

前回の3件はいずれも解消されています。更新された `release.yml` の3経路も静的に追跡し、Releaseノートの収集・artifact化・publishの分岐に新たな問題は確認できませんでした。

conceptsの繰り延べは `deviation.md` の合意済み差分として除外しています。

## 照合した規約

- `comment-policy.md`（always）
- `test-execution.md`（テスト結果の報告）
- `release-procedure.md`（release workflow・再実行・リハーサル）
- cross/ADR-0019、0020
- proposed ADR-0029、0030は変更仕様の背景として参照
- cross domainの追加レビュー用スキル指定なし

## 前回指摘の確認

1. **ページ送りselftest: 解消済み**

   `scripts/release/build-release-notes.py:342` の `PagedGitHubApi` は本番の `GitHubApi` を継承し、HTTP取得に相当する `_get()` だけを差し替えています。`selftest_paging():581` は `collect_pulls()` 経由で本番の `GitHubApi.paged()` を通ります。

   1ページ打ち切りのミューテーションで4件NGになるという提示結果も、退行検出力の根拠として十分です。

2. **同名tagの再実行説明: 解消済み**

   `release-procedure.md:209` で、別commitを指すtagは衝突、起動commitを指すtagは再実行として続行、と明確に区別されています。

3. **`## Changes` の変換説明: 解消済み**

   `release-procedure.md:160` は、項目が材料となり、workflowが種別別の再編・PR番号・比較リンクを追加することを正しく説明しています。

## リハーサル分岐の静的照合

| 経路 | Releaseノートstep | 後続job |
|---|---|---|
| 非main・`dry-run=true` | `collect=false`。Build／Uploadはskip、Skipだけ実行 | 本体検証・package・consumer検証は実行。publish／wait／smokeはskip |
| main・`dry-run=true` | `collect=true`。Build／Uploadを実行、Skipはskip | 本体検証・package・consumer検証は実行。publish／wait／smokeはskip |
| main・本番 | `collect=true`。Build／Uploadを実行 | 全事前検証後にpublishがartifactをdownloadし、その本文でReleaseを作成。続いてwait／smokeを実行 |

判定条件は `release.yml:173` で一度だけ評価され、Build・Upload・Skipが同じoutputを参照しています。本番の非main起動は、それ以前の入力検査で失敗します。

## 指摘事項

なし。

## アクションプラン

コードレビュー上の追加修正は不要です。予定どおり、未完了のremote `dry-run` 検証を完了条件として実施してください。

重要度別件数: **Critical 0 / Major 0 / Minor 0 / Suggestion 0**

