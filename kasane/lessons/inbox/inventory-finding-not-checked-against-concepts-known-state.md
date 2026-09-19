---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-17
last-seen: 2026-09-17
evidence:
  - scroll-indicator-visible-not-applied (探索の棚卸しで iOS の Header / Footer 背景色の未適用を「単なる未適用」と扱い、併せて直すと決めた。実際は concepts `core/styling/list-appearance.md` に「既知の platform 非対称」と明記された現状で、反映すると既定色のままでは iOS に新しい帯が出る。実装フェーズ中に発覚し、既定値の変更 (core/ADR-0030 の一部改訂) と S → M への級の引き上げ・提案の作り直しになった)
---

## ルール文 (候補)

探索でコードの棚卸しから「未適用・未実装」を見つけて修正対象に加える前に、対象のプロパティ名・能力名で `kasane/concepts/` と `kasane/decisions/` を検索し、その現状が既知として記録されていないか、直すと既定の見た目や既存の決定に波及しないかを確認する。

## 経緯

- 2026-09-17 scroll-indicator-visible-not-applied: 棚卸しは grep と View 側の消費箇所の追跡だけで判定し、長命層との照合をしなかった。exploration.md に「探索時の見落としの訂正」として記録されている。
