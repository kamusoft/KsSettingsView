---
scope: process
kind: pain
severity: normal
count: 3
first-seen: 2026-08-29
last-seen: 2026-09-01
evidence:
  - consolidate-readmes-and-contribution / グループ4 (workers.impl: counterpart で codex ワーカーに旧 README 5 枚の削除を委譲したところ、`trash` が `NSCocoaErrorDomain Code=513` / `afpAccessDenied` で失敗し停止。リポジトリが外部ボリューム `/Volumes/` 上にあり sandbox から Trash 領域へ書けないため。同じコマンドをホストの shell から実行すると成功した)
  - consolidate-readmes-and-contribution / グループ6 (同じワーカーに `.agents/skills/docs-refresh/SKILL.md` の改訂を委譲したところ、`patch rejected: writing outside of the project; rejected by user approval settings` で停止。実パスはリポジトリ内だが、sandbox が dot ディレクトリ配下を project 外と判定した。ホスト側で編集して完了させた)
  - fix-ios-test-pump-condition-wait / Wave 1 (workers.impl: counterpart で iOS テストの待機ヘルパ新設を委譲したところ、CoreSimulator へのアクセスが `Operation not permitted`、`xcodebuild build-for-testing -destination 'platform=iOS Simulator'` が `Connection refused` / exit 66 で拒否され、タスクの確定ゲート「XCTest リンク可否の実測」に到達できず 1 行も実装せずに停止。同じコマンドをホストで実行すると `** TEST BUILD SUCCEEDED **`。ホストの ksn-implementer へ切り替えて再委譲した)
---

## ルール文 (候補)

`workers.*: counterpart` のプロジェクトでは、委譲パッケージを書く時点で **counterpart の sandbox が届かない範囲を確認し、届かないなら切り分けるか委譲先そのものを host にする**。判断は次の 2 段:

**(1) 作業の一部が届かないだけ → 指揮側 (ホスト) の担当として切り分ける**

- **ファイルの削除** — リポジトリが外部ボリューム (`/Volumes/`) 上にあると `trash` が Trash 領域へ書けず権限エラーになる (`rm` への切り替えは削除コマンド規約に反するため、ワーカー側に回避策は無い)
- **dot ディレクトリ配下 (`.agents/` `.github/` 等) の既存ファイルの編集** — 実パスがリポジトリ内でも sandbox が project 外と判定して拒否することがある

タスクをグループ化するときに「削除」「dot ディレクトリの編集」を同じ委譲に混ぜない。

**(2) 作業の完了条件が届かない → その役割の backend を host にする (切り分けでは解決しない)**

- **Simulator / Emulator での実行を完了条件とするタスク** — codex sandbox は CoreSimulator サービスへ接続できず、`xcrun simctl` も `xcodebuild -destination 'platform=iOS Simulator,...'` も拒否される。iOS / Android の実装タスクは「テストが Simulator で通ること」が完了条件であり、検証だけをホストへ切り分けると**ワーカーが自分の成果を一度も検証できない委譲**になって往復が増えるだけになる。config が `workers.impl: counterpart` でも、これに当たる change では host のワーカーへ切り替える

いずれもワーカーは正しく停止報告するが、指揮側が引き取るまで 1 往復ぶんの手戻りが出る。

## 経緯

- 2026-08-29 consolidate-readmes-and-contribution: 同一 change・同一ワーカーセッションで 2 回発生した。グループ 3+4 をまとめた委譲はグループ 3 完了・グループ 4 冒頭で停止 (`trash`)、グループ 5+6+7 をまとめた委譲はグループ 5・7・6.4 完了・6.1〜6.3 で停止 (`.agents/` 書き込み)。どちらもホスト側で数分で完了する作業であり、切り分けておけば往復は発生しなかった。なお `.github/` は**新規作成**だったため拒否されておらず、拒否は既存ファイルの patch に対して起きている。
- 2026-09-01 fix-ios-test-pump-condition-wait: 前 2 件と違い、切り分けでは解決しない型として現れた。iOS の待機ヘルパ新設は「XCTest を共有ターゲットからリンクできるか」の実測が着手ゲートに置かれており、ビルドを走らせられない相方は 1 行も書けずに停止した。委譲前にホストで `xcodebuild build-for-testing` を 1 回通しておけば、backend の選択を誤らずに済んだ (実測は 2 分程度)。
