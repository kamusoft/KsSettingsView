---
scope: process
kind: pain
severity: normal
count: 2
first-seen: 2026-08-29
last-seen: 2026-08-29
evidence:
  - consolidate-readmes-and-contribution / グループ4 (workers.impl: counterpart で codex ワーカーに旧 README 5 枚の削除を委譲したところ、`trash` が `NSCocoaErrorDomain Code=513` / `afpAccessDenied` で失敗し停止。リポジトリが外部ボリューム `/Volumes/` 上にあり sandbox から Trash 領域へ書けないため。同じコマンドをホストの shell から実行すると成功した)
  - consolidate-readmes-and-contribution / グループ6 (同じワーカーに `.agents/skills/docs-refresh/SKILL.md` の改訂を委譲したところ、`patch rejected: writing outside of the project; rejected by user approval settings` で停止。実パスはリポジトリ内だが、sandbox が dot ディレクトリ配下を project 外と判定した。ホスト側で編集して完了させた)
---

## ルール文 (候補)

`workers.*: counterpart` のプロジェクトでは、委譲パッケージを書く時点で **counterpart の sandbox が触れない操作を切り分け、指揮側 (ホスト) の担当として残す**。切り分ける対象は少なくとも次の 2 つ:

- **ファイルの削除** — リポジトリが外部ボリューム (`/Volumes/`) 上にあると `trash` が Trash 領域へ書けず権限エラーになる (`rm` への切り替えは削除コマンド規約に反するため、ワーカー側に回避策は無い)
- **dot ディレクトリ配下 (`.agents/` `.github/` 等) の既存ファイルの編集** — 実パスがリポジトリ内でも sandbox が project 外と判定して拒否することがある

いずれもワーカーは正しく停止報告するが、指揮側が引き取るまで 1 往復ぶんの手戻りが出る。タスクをグループ化するときに「削除」「dot ディレクトリの編集」を同じ委譲に混ぜないこと。

## 経緯

- 2026-08-29 consolidate-readmes-and-contribution: 同一 change・同一ワーカーセッションで 2 回発生した。グループ 3+4 をまとめた委譲はグループ 3 完了・グループ 4 冒頭で停止 (`trash`)、グループ 5+6+7 をまとめた委譲はグループ 5・7・6.4 完了・6.1〜6.3 で停止 (`.agents/` 書き込み)。どちらもホスト側で数分で完了する作業であり、切り分けておけば往復は発生しなかった。なお `.github/` は**新規作成**だったため拒否されておらず、拒否は既存ファイルの patch に対して起きている。
