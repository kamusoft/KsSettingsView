---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-09-03
last-seen: 2026-09-03
evidence:
  - add-release-workflow (review-002 Major / Minor。検査 script の呼び出し 2 箇所が `if [ "$(script)" = match ]` の形で条件部に置かれ、script の exit 1 が握り潰されて「内容の異なる同名 tag があっても validate が成功する」状態になっていた)
---

## ルール文 (候補)

shell (`set -e` / `set -euo pipefail` を前提にした workflow の `run` や script) で、失敗を終了ステータスで伝える検査コマンドの出力を使うときは、**コマンド置換を `if` / `[ ]` / `&&` の条件部や `local` / `export` と同じ行に置かない**。条件部・宣言と同じ行ではコマンド置換の終了ステータスが捨てられる (errexit の対象外)。代わりに単独行の代入 (`result=$(cmd)`) で受けてから (失敗はここで errexit が拾う) 結果の値で分岐し、script 側は「失敗」と「該当なし」を終了ステータスと出力で区別できる契約にする。

事後判定: `git grep -nE 'if \[.*\$\(|(local|export) [A-Za-z_]+=\$\('` 相当の検索で、失敗し得る検査コマンドを条件部・宣言と同じ行で置換している箇所が無い。昇格時は lessons ではなく lint (shellcheck の SC2155 / SC2312 相当を CI の lint job に載せる) へ流す候補 — 機械検査で拾える型。

## 経緯

- 2026-09-03 add-release-workflow: 相方レビューの指摘で新設した配信リポジトリ tag の検査 script (`check-distribution-tag.sh`、`absent` / `match` を出力し、別内容なら exit 1) を validate と publish の 2 箇所で `if [ "$(...)" = match ]` の形で呼んでいた。exit 1 でも出力が空なので `match` ではない側の分岐へ進み、validate は成功、publish は tag 作成直前まで進んで `git tag` の重複エラーで止まる (意図した理由が出ない)。review-002 が合成リポジトリで再現して指摘し、単独行の代入 + `match` / `absent` / `*` の 3 分岐へ直した (review-003 で実測して解消を確認)。
