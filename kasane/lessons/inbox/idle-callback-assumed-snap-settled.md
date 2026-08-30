---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-08-02
last-seen: 2026-08-02
evidence:
  - android-numberpicker-modern-ui (SCROLL_STATE_IDLE を「スナップ静止」と同一視して選択確定し、SnapHelper の補正スクロール開始時点の IDLE で移動中候補が確定される競合。相方が Major 検出、ホストレビューは見逃し)
---

## ルール文

スクロール・アニメーション系の状態 callback (RecyclerView の `SCROLL_STATE_IDLE` 等) を「最終位置へ整列済み」と同一視しない。フレームワークの補助機構 (SnapHelper の補正スクロール等) が同じ callback を契機に動くため、整列を前提とする確定処理は整列条件そのもの (`calculateDistanceToFinalSnap` の残距離 0 等) を検証してから行う。

## 経緯

- 2026-08-02 android-numberpicker-modern-ui: `KsWheelView` が IDLE で `findSnapView()` を無条件に選択確定し、行間停止時は同じ IDLE が SnapHelper の補正スクロール開始契機でもあるため、補正移動中に確定すると spec「選択中候補の更新はスナップ静止時のみ」に違反する競合が成立 (second-opinion-002 で相方 codex が Major 検出。ホスト review-001 は実測系レビューだったがこの経路は見逃し)。修正は残距離 0 条件の追加 + 行間停止→補正完了の遷移を分けて検証する実 MotionEvent テスト。
