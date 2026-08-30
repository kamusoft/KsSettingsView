---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-08-24
last-seen: 2026-08-24
evidence:
  - entrycell-keyboard-avoidance-check (3 OS のサンプル変更で iOS のみ視覚確認し、Android / MAUI はビルド確認で済ませて完了報告に向かった。オーナーが「Android の実機確認してなくない？」と指摘、独立レビューも同点を Major (L-003 抵触) で差し戻し)
---

## ルール文

複数 OS に同一の UI 変更を入れたときの視覚確認は、全対象 OS で行う。ビルド成功・コンパイル成功は視覚確認の代替にならない。特に変更の目的自体が実行時挙動 (キーボード回避・フォーカス・レイアウト着地) の確認であるとき、「主要 1 OS で確認できたので他 OS も同様」と推定してはいけない — プラットフォームごとに機構が異なり、結果は独立に検証するまで不明。

## 経緯

- 2026-08-24 entrycell-keyboard-avoidance-check: 昇格済み L-003 (視覚確認と証跡保存を APPROVED 条件とする) が既にあるにもかかわらず、iOS のみ視覚確認して Android / MAUI をビルド確認で済ませようとした。オーナー指摘と review-001 Major で全 OS の視覚確認 + evidence/ 保存を実施して解消。
