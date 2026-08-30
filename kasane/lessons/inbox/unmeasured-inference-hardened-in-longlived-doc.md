---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-08-28
last-seen: 2026-08-28
evidence:
  - customcell-android-maui-perf (exploration.md で「〜と推定」だった iOS 側の機構説明が、concepts へ起こす過程で留保を失い断定形になった。review-001 Major-1 が指摘 — しかも断定内容は本プロジェクトの iOS 検証経路 (Simulator = JIT 実行) には当てはまらず、機構としても不正確だった)
---

## ルール文

実測に裏打ちされていない推定を concepts / ADR など長命層に書くときは、留保 (「推定」「未計測」) を明記し、実測に基づく記述と同じ断定の語調で並べない。特に足場アーティファクト (exploration / session) から長命層への転記は留保が脱落しやすい局面 — 転記時に各文の裏付け (実測 / ソース確認 / 推定) を確かめ直す。

## 経緯

- 2026-08-28 customcell-android-maui-perf: 「iOS は Debug でも AOT 混在で動くため乖離が小さい」を concepts に断定形で記載。実測表に iOS の行は無く、レビューが「実測と未計測の推定が同じ確度で並び、後続が iOS 側の調査をこの一文で打ち切るリスク」と指摘。さらに実機 Debug には当てはまるが Simulator (JIT・Mac の性能) 経路には成り立たない不正確さも判明し、未計測の明記 + 経路依存の注記へ書き直した。
- 類似パターン: [test-limitation-asserted-without-measurement](test-limitation-asserted-without-measurement.md) (未実測の断定を証跡に書きテストを弱める)。どちらも「未検証の断定の固定化」だが、こちらは長命層 (ksn-drift のディープ検証対象) に入る点で腐り方が長期化する。
