---
scope: code-review
kind: success
severity: normal
count: 1
first-seen: 2026-08-02
last-seen: 2026-08-02
evidence:
  - android-picker-selection-sheet (review-002〜005 が実ダイアログ階層の measure/layout + 実 MotionEvent ドラッグ列で実測表を作り、相方の静的レビューが見逃した Major を2回検出。収束の決め手にもなった)
---

## ルール文

Android の動的レイアウト・ジェスチャー挙動のレビューは、静的読解に留めず Robolectric で実ダイアログ階層 (CoordinatorLayout 配下) を measure/layout し、実 `MotionEvent` のイベント列を送って挙動を実測する。指摘は「主張」ではなく数値の実測表で提示する (オーナー裁定と修正方針の決定が速くなる)。計測用の一時テストは計測後に削除する。

## 経緯

- 2026-08-02 android-picker-selection-sheet: review-002 が初期スクロールの不成立を件数域ごとの実測表で提示、review-003 が nested scroll によるシート全画面化の誤発火を実イベント列で検出 (相方 second-opinion-004 は APPROVED で見逃し)、review-004 が「ハンドル 27dp 下ドラッグで末尾 7 候補が到達不能」を実測、review-005 が破綻3経路の解消を実 MotionEvent 7 経路で再検証して APPROVED へ収束させた。静的レビューでは到達できない検出力を4周連続で実証した形。
