---
scope: test
kind: pain
severity: normal
count: 2
first-seen: 2026-08-08
last-seen: 2026-08-22
evidence:
  - fix-compose-dsl-double-update-flaky-test (待機の上限を反復回数 (50 回) で置き、ループ内に sleep も yield も無いため、バックグラウンド diff の完了前に上限を使い切って flaky 化)
  - clarify-host-attach-order-contract (`AttachOrderRestoreTest` の `idleUntilQuiet()` が `Thread.sleep(5)`×30 の固定 150ms 待ちで「quiet」を観測していなかった。相方 code-review が検出 (second-opinion-002 Minor、採用) し条件ベース待機へ置換)
---

## ルール文

テストの待機ヘルパは、待ちたい完了条件そのもの (例: `AsyncListDiffer` のコミット済み `currentList` と期待値の一致) を観測する条件ベース待機で書く。待機の上限は**実時間の deadline** で置く。固定時間 sleep の繰り返しで「静止」を代用しない (通常時は無駄待ち、低速環境では不足して flaky になる)。上限を反復回数で置くのも同じ誤りで、待機を伴わないループは対象がバックグラウンドスレッドにある間に回数を使い切る。タイムアウト時は黙って戻らず fail() で落とす ([async-wait-helper-times-out-silently](async-wait-helper-times-out-silently.md) と同族の別型)。

## 経緯

- 2026-08-08 clarify-host-attach-order-contract: kotlin-impl-skill references/testing.md が「テストで `Thread.sleep` (実時間待ち)」を禁止しているにもかかわらず、実装ワーカーが固定時間待機ヘルパを書き、ホスト review-001 も見逃した。相方 code-review の指摘採用で条件ベースの `awaitConvergence` (タイムアウトで fail・失敗時に実測値をメッセージへ載せる) に置換し、テスト実行時間は 2.249s → 0.141s へ短縮。既存テスト4ファイル (AdapterReattachTest 等) にも同パターンが残存していた。
- 2026-08-08 (追記): 残存分は同日の直接セッションで解消 — 実際は5ファイル (AdapterReattachTest / InitialThemeDecorationTest / ContentUpdatePayloadTest / FullUpdateContentSyncTest / StoreUnbindTest) にあり、すべて条件ベース待機へ置換済み (Android 2012 tests / 0 failures)。カウント規律により count は増やさない (同一 change 起因の同型)。
- 2026-08-22 fix-compose-dsl-double-update-flaky-test: 同型の別バリエーション。`waitForAdapterItemCount` の上限が反復回数 (50 回) で、ループ本体は `Looper.idle()` + `composeRule.waitForIdle()` のみ (sleep も yield も無し)。`AsyncListDiffer` の diff はバックグラウンドスレッドで走り結果を main へ post するため、post 前は main のキューが空で `idle()` が即戻り、50 回がマイクロ秒で燃え尽きる。全モジュール並列実行 (`org.gradle.parallel=true`) で CPU が競合したときだけ落ちる flaky として表面化した。deadline (5s) + 条件判定 + `Thread.sleep(1)` へ置換。`Thread.yield()` は OS へのヒントに留まり CPU 飽和時に譲れる保証がないため sleep を選択。なお docstring は「最大 50 回、1 回 1ms 待機」と書かれていたが待機はコードに存在せず、この乖離が不備を覆い隠していた。
