# 切り出しスクリプトの出力一致検証

docs-refresh の SKILL.md にインラインで置かれていた Python を `.agents/skills/docs-refresh/scripts/` へ切り出すにあたり、切り出し前後で標準出力が一致することを確認した記録。

## 方法

- 切り出し前の版は `git show HEAD:.agents/skills/docs-refresh/SKILL.md` から該当行範囲を抜き出して一時ファイルへ保存し、そのまま実行した (行範囲は下表の「元の行範囲」)
- 入力はどちらも同一: カレントディレクトリはリポジトリルート、`skills/.manifest.json` と `skills/` の実態は作業ツリーの現状
- 予定 manifest を要求する検査には、現行 `skills/.manifest.json` をそのままコピーした一時ファイルを `DOCS_REFRESH_MANIFEST` で渡した
- `link-resolution-check` の入力となる `/tmp/docs-refresh-targets.txt` は、切り出し後の `targets-list.py` で生成したものを両版に与えた
- 比較は標準出力の `diff` と終了コードの一致で判定した

## 結果

全 12 実行 (8 スクリプト、うち 4 つは環境変数の分岐ごとに 2 通り) で標準出力・終了コードともに一致。

| スクリプト | 元の行範囲 (HEAD の SKILL.md) | 実行時の環境変数 | 出力行数 | 判定 |
|---|---|---|---|---|
| `concepts-coverage-check.py` | L152-L170 | `DOCS_REFRESH_MANIFEST` = 予定 manifest 代用 | 1 | 一致 |
| `concepts-coverage-check.py` | L152-L170 | なし (既定のディスク manifest) | 1 | 一致 |
| `api-coverage-check.py` | L196-L236 | なし | 67 | 一致 |
| `planned-manifest.py` | L427-L443 | なし (判断なし = 元の空 `ADD_TARGETS` 相当) | 226 | 一致 |
| `targets-list.py` | L455-L463 | `DOCS_REFRESH_MANIFEST` | 38 | 一致 |
| `targets-list.py` | L455-L463 | `DOCS_REFRESH_MANIFEST` + `DOCS_REFRESH_README_ONLY=1` | 4 | 一致 |
| `heading-parity-check.py` | L487-L519 | `DOCS_REFRESH_MANIFEST` | 1 | 一致 |
| `heading-parity-check.py` | L487-L519 | `DOCS_REFRESH_MANIFEST` + `DOCS_REFRESH_README_ONLY=1` | 1 | 一致 |
| `code-block-parity-check.py` | L529-L559 | `DOCS_REFRESH_MANIFEST` | 1 | 一致 |
| `code-block-parity-check.py` | L529-L559 | `DOCS_REFRESH_MANIFEST` + `DOCS_REFRESH_README_ONLY=1` | 1 | 一致 |
| `frontmatter-check.py` | L569-L620 | `DOCS_REFRESH_MANIFEST` | 1 | 一致 |
| `link-resolution-check.py` | L651-L671 | なし | 1 | 一致 |

## 唯一の意図的な差分: 予定 manifest の判断入力

切り出し前の予定 manifest 生成はスクリプト本文の `ADD_TARGETS` / `ADD_EXCLUDED` / `DROP_CONCEPTS` を実行のたびに書き換える形だった。共有ファイルへ切り出すとこの書き換えがスクリプト本体の改変になるため、判断は環境変数 `DOCS_REFRESH_DECISIONS` が指す JSON ファイル (`addTargets` / `addExcluded` / `dropConcepts`) から読む形に改めた。

- 判断なしの実行 (環境変数を渡さない) では出力が切り出し前と完全に一致する — 上表で確認済み
- 判断ありの経路も別途動作を確認した (`addTargets` で `targets` の該当キーへ concept が追加され、`addExcluded` が `excluded` に載ること)

## 併せて通した lint

- `python3 scripts/local-path-lint.py --paths <触ったファイル一式>` — exit 0
- `python3 scripts/identity-lint.py --paths <触ったファイル一式>` — exit 0
- `python3 scripts/comment-policy-lint.py --summary` — 禁止 0 件。ただし `.py` は現行の検査対象拡張子 (既定 + `lint.comment-policy.ext`) に含まれないため、新設スクリプト 8 本は機械検査の対象外 (検査対象 0 ファイル)。規約適合は手読みで確認した

## SKILL.md のバイト数

| | バイト数 |
|---|---|
| 切り出し前 (HEAD) | 56,285 |
| 切り出し後 | 44,001 |

目標としていた 20KB 前後には届いていない。切り出せる Python はすべて外に出しており (SKILL.md にインラインの Python は 1 本も残っていない)、残る量は散文の手順・注記・Guardrails が占める。散文は意味を変えずに削れないため、これ以上の圧縮は手順そのものの取捨選択 (別判断) になる。
