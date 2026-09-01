# counterpart の sandbox が届かない作業の委譲 (L-008 の経緯)

`workers.*: counterpart` の委譲で、counterpart (codex) の sandbox が作業に届かず停止した 3 件の記録。いずれもワーカーは正しく停止報告したが、指揮側が引き取るまで 1 往復ぶんの手戻りが出た。

## 届かない範囲の 2 段判定

**(1) 作業の一部が届かないだけ → 指揮側 (ホスト) の担当として切り分ける**

- **ファイルの削除** — リポジトリが外部ボリューム (`/Volumes/`) 上にあると `trash` が Trash 領域へ書けず権限エラーになる (`rm` への切り替えは削除コマンド規約に反するため、ワーカー側に回避策は無い)
- **dot ディレクトリ配下 (`.agents/` `.github/` 等) の既存ファイルの編集** — 実パスがリポジトリ内でも sandbox が project 外と判定して拒否することがある (新規作成は拒否されない)

タスクをグループ化するときに「削除」「dot ディレクトリの編集」を同じ委譲に混ぜない。

**(2) 作業の完了条件が届かない → その役割の backend を host にする (切り分けでは解決しない)**

- **Simulator / Emulator での実行を完了条件とするタスク** — codex sandbox は CoreSimulator サービスへ接続できず、`xcrun simctl` も `xcodebuild -destination 'platform=iOS Simulator,...'` も拒否される。検証だけをホストへ切り分けると、ワーカーが自分の成果を一度も検証できない委譲になって往復が増えるだけになる。config が `workers.impl: counterpart` でも、これに当たる change では host のワーカーへ切り替える

## 経緯

- 2026-08-29 consolidate-readmes-and-contribution: 同一 change・同一ワーカーセッションで 2 回発生。旧 README 5 枚の削除委譲が `trash` の `NSCocoaErrorDomain Code=513` / `afpAccessDenied` で停止 (ホストの shell では成功)。`.agents/skills/docs-refresh/SKILL.md` の改訂委譲が `patch rejected: writing outside of the project` で停止 (ホスト側で編集して完了)。どちらもホストなら数分で終わる作業で、切り分けておけば往復は発生しなかった。
- 2026-09-01 fix-ios-test-pump-condition-wait / Wave 1: 切り分けでは解決しない型。iOS の待機ヘルパ新設は「XCTest を共有ターゲットからリンクできるか」の実測が着手ゲートに置かれており、CoreSimulator アクセスが `Operation not permitted`、`xcodebuild build-for-testing` が `Connection refused` / exit 66 で拒否され、相方は 1 行も書けずに停止した。同じコマンドはホストで `** TEST BUILD SUCCEEDED **`。委譲前にホストで 1 回通しておけば (実測 2 分程度)、backend の選択を誤らずに済んだ。
