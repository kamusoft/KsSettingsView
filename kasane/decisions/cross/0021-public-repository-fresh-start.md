---
id: 0021
title: public リポジトリは新規に作り、既存の private リポジトリの履歴は引き継がない
status: proposed
date: 2026-08-21
---

## Context

配布は公開レジストリの標準チャネルのみで行い、SwiftPM が git を直接解決する都合上リポジトリを public に切り替える (cross/ADR-0018)。切り替え前に作業ツリーと git 全履歴 (235 commit) の機密情報・個人情報を点検した結果、秘密情報 (API key・トークン・鍵・keystore・`local.properties` 類) は作業ツリー・履歴とも 0 件だった (grep 点検 + gitleaks)。

一方で履歴には次が残っている:

- コミット author の大半が noreply でない個人メールアドレス
- 削除済みファイルを含む多数のローカル絶対パス (`/Volumes/...` `/Users/...`)。非公開ローカルクローン (AiForms / KsDialogs) の存在を示すものを含む

どちらも機密ではないが、public 化後は履歴から恒久的に閲覧できる。既存リポジトリには tag が無く、Issue 1 件・PR 11 件・worktree は本体のみで、履歴を捨てることで失う運用資産は小さい。

## Decision

**public リポジトリは新規に作成し、現在の作業ツリーを単一の initial commit として公開する。開発の本籍はその新リポジトリへ移し、既存の private リポジトリは履歴保管用として凍結する (archive)。**

- initial commit は noreply のメールアドレスで作る (それ以降のコミットも同様)
- 新リポジトリは既存の名前 (`kamusoft/KsSettingsView`) を引き継ぎ、既存 private リポジトリは別名へ rename した上で GitHub の Archive で読み取り専用にする
- 既存 private リポジトリの履歴は書き換えず、移転後は push しない
- **公開ツリーには開発ハーネスの記録 (`kasane/` `openspec/` `.claude/` `.codex/`) を含める。外すのは、容量と画面内の個人情報リスクが大きい `kasane/changes/archive/**` の媒体 (画像・動画) と、本番が採らなかった方式を実装していて追従の仕組みがない `maui/spike/` だけ** (2026-08-30 追記)
- 新リポジトリは配布・Issue 窓口・CI の唯一の場であり、旧リポジトリとの同期は行わない (cross/ADR-0018 が却下した「配布用ミラー」とは異なる: ミラーは 2 つのリポジトリを並走させるが、本決定は本籍を移すだけで並走しない)

## Alternatives Considered

- **既存リポジトリを `git filter-repo` で書き換えてから public 化する**: 却下。author の mailmap 置換だけなら容易だが、履歴中の約 1000 行のローカルパスまで消すには `--replace-text` の正規表現書き換えと漏れの検証が必要で、得られる結果は新規リポジトリと同じ。
- **既存リポジトリをそのまま public 化する**: 却下。個人メールアドレスとローカルパスが全履歴に残り、後から消すには結局 force push が要る。

## Consequences

- 正: 履歴の個人情報・ローカル環境情報が公開されない。履歴書き換えの失敗リスクがない。
- 正: 公開リポジトリの初期状態を意図的に選べる (公開ツリーに含めるものの取捨選択が initial commit の時点でできる)。
- 負: 公開リポジトリからは初回リリースまでの開発履歴 (blame・コミット単位の経緯) が辿れない。経緯は旧 private リポジトリと `kasane/changes/archive/`・`kasane/decisions/` のドキュメントに残る。
- 負: 既存の Issue / PR は引き継がれない。
- 負: 今後の change 記録・議論の経緯もすべて公開される。書く時点で公開を前提とした規律 (ローカル絶対パスを書かない、個体・個人・秘密を特定する値を残さない) が必要になり、その担保として lint (`scripts/local-path-lint.py` / `scripts/identity-lint.py`) と 書き込み hook を置いた (2026-08-30 追記)
- 負: ローカルクローン・リモート設定・GitHub 上の名前 (旧リポジトリの rename) の切り替え作業が発生する。同名のリポジトリを作り直すと旧名へのリダイレクトが新リポジトリを指すため、旧リポジトリを参照するクローンは remote を明示的に付け替える必要がある。

出典: kasane/roadmaps/package-distribution/phases/phase-2-public-readiness/history.md (2026-08-21: 履歴の扱い) / kasane/roadmaps/package-distribution/phases/phase-2-public-readiness/artifacts/scan-2026-08-21.md / (2026-08-30 追記分) 同 agenda.md の決定「体裁と公開対象の範囲」「公開対象の範囲 — evidence 媒体」「`maui/spike/` は公開リポジトリに載せない」および artifacts/publish-procedure.md の実施記録
