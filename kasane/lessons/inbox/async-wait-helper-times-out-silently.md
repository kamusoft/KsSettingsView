---
scope: test
kind: pain
severity: normal
count: 2
first-seen: 2026-08-05
last-seen: 2026-08-22
evidence:
  - fix-android-accessory-header-refresh (テストヘルパ `awaitDifferCommit` がタイムアウト後に黙って戻るため、負のアサーションが「待機の空振り」でも通過し、待機条件の値誤りによる毎回 5 秒空転も検出されなかった。ホスト review-001 Minor と相方 second-opinion-002 Minor が独立に同一指摘)
  - fix-compose-dsl-double-update-flaky-test (compose 側の `waitForAdapterItemCount` が上限到達後に黙って戻り、後続が offset を読む呼び出しでは収束前の状態を検証していた。flaky として表面化)
---

## ルール文

条件成立を待つテストヘルパは、タイムアウト時に必ず `fail()` で落とす。黙って戻る待機は「待った後に何も起きていないこと」を確かめる負のアサーションを空振りさせ、待機条件の誤り (誤った期待値・成立し得ない条件) も隠す。

## 経緯

- 2026-08-05 fix-android-accessory-header-refresh: `awaitDifferCommit` は `timeoutMillis` を過ぎても `idle()` を 1 回呼んで黙って戻る設計だった。(1) positive テスト 1 件が待機空振り → トートロジーなアサーションで PASS (Major の増幅要因)、(2) 待機条件 `itemCount == 5` の誤り (正: 6) で 1 件が毎回 5 秒空転しても green のまま、(3) 負のアサーション (「通知を発行しない」) がコミット未完了でも通過し得る構造。タイムアウト時 `fail()` の 1 箇所修正でファイル内 19 箇所の待機すべてが「コミット完了後に評価されたこと」を保証できた。ホスト・相方の双方が独立に検出した確度の高いパターン。
- 2026-08-22 fix-compose-dsl-double-update-flaky-test: `ks-settingsview-compose` の `waitForAdapterItemCount` も同型で、上限到達後に「`assertEquals` 側が詳細メッセージ付きで落とすため即時失敗はしない」というコメント付きで黙って return する設計だった。itemCount を直接 assert する呼び出しは落ちるが、`cellRowOffsets` のように itemCount 到達後の派生値を読む呼び出しでは収束前の状態を検証してしまう。タイムアウト時 `fail()` へ変更し、現在値をメッセージに載せて「実装が壊れた」と「待機が足りない」を区別できるようにした。fail 経路はミューテーション実測 (`InsertCell` を index 0 限定へ一時改変) で到達を確認。
