---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-08-24
last-seen: 2026-08-24
evidence:
  - android-hinttext-position (review-001 が「次回撮影時は通知を消してから撮ること」と指摘したのに、review-001 対応の再撮影 android-after.png のステータスバーにも通知アイコンが残っていた。review-002 が Minor として再指摘)
---

## ルール文

実機で証跡を撮る前に、通知シェードを消化しステータスバーに通知アイコンが無い状態にしてから撮る (またはエミュレータ + デモデータで撮る)。kasane/ 配下の画像は commit 後に git 履歴から消せず、防波堤は撮影時にしか置けない (ksn-core references/ui-artifacts.md の撮影規律)。

## 経緯

- 2026-08-24 android-hinttext-position: 撮影規律 (通知の写り込み禁止) が review-001 で指摘された後、同 change 内の再撮影でも守られなかった (同一 change のため count は 1)。
