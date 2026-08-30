---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-08-04
last-seen: 2026-08-04
evidence:
  - add-cell-types-custom (オーケストレーターがレビュアーへのコンテキストパッケージに「`git diff develop` で全体を把握できる」と書いたが、新規19ファイルが untracked のため diff に現れず、実装本体が丸ごと欠落していた。起動直後に `git status` を見て気づき SendMessage で訂正。相方 codex への依頼文にも同じ注記を入れた)
---

## ルール文

レビュー・verify・セカンドオピニオンへ diff の取得方法を渡す前に `git status --short` を実行し、**未コミットの新規ファイル (`??`) があれば「`git diff <base>` には現れないので直接 Read すること」と対象ファイル名を列挙して渡す**。Kasane は commit をユーザー/CI の責務としており実装完了時点で新規ファイルが untracked のまま残るため、diff だけを指示すると新規実装が丸ごとレビュー対象から抜ける。変更ファイルの差分だけを見て「レビュー済み」になる事故は、レビュー結果からは判別できない。

## 経緯

- 2026-08-04 add-cell-types-custom: L 級変更で新規19ファイル (CustomCell 本体・テスト・Sample) がすべて untracked、tracked な変更は既存11ファイル 122 行のみだった。ksn-reviewer へのパッケージに「`git diff develop` で全体を把握できる」と書いてしまい、起動直後に `git status --short` を確認して発覚。SendMessage で訂正し、新規ファイル一覧を明示して送り直した。相方 codex への instructions には最初から注記を入れたため同じ穴は生じなかった。以後の再レビュー・verify のパッケージにも同注記を入れて再発を防いだ。
