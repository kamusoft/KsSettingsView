---
scope: impl
kind: pain
severity: normal
count: 2
first-seen: 2026-08-28
last-seen: 2026-09-01
evidence:
  - customcell-android-maui-perf (exploration.md で「〜と推定」だった iOS 側の機構説明が、concepts へ起こす過程で留保を失い断定形になった。review-001 Major-1 が指摘 — しかも断定内容は本プロジェクトの iOS 検証経路 (Simulator = JIT 実行) には当てはまらず、機構としても不正確だった)
  - add-spm-distribution (付随修正で handbook cross/test-execution.md に追記した「複数バンドル時の件数集計手順」が、書いた文言どおりに実行されないまま確定した。review-002 Major が実測と突き合わせて捕捉 — 旧文言に従うと 1000 件の実行を 2998 件と報告する。review-003 で新文言どおりの集計が実測 1000 と一致することを確認して解消)
---

## ルール文

実測に裏打ちされていない推定を concepts / ADR など長命層に書くときは、留保 (「推定」「未計測」) を明記し、実測に基づく記述と同じ断定の語調で並べない。特に足場アーティファクト (exploration / session) から長命層への転記は留保が脱落しやすい局面 — 転記時に各文の裏付け (実測 / ソース確認 / 推定) を確かめ直す。 推定の記述に限らず、長命層 (handbook / concepts) に書く手順・数え方も、書いた文言どおりに一度実行した実出力で検証してから確定する — 手順は従われて初めて誤りが露呈するため、腐り方が記述より実害的になる。

## 経緯

- 2026-09-01 add-spm-distribution: handbook test-execution.md への件数集計手順の追記 (規範層の改訂) が、実出力での検証なしに確定した。`Executed` 行の全合算という文言は中間サマリ行も拾って二重計上する。規範が governs する当の領域 (件数確認) で誤った手順を指示する形になり、CHANGES_REQUESTED の根拠となった。

- 2026-08-28 customcell-android-maui-perf: 「iOS は Debug でも AOT 混在で動くため乖離が小さい」を concepts に断定形で記載。実測表に iOS の行は無く、レビューが「実測と未計測の推定が同じ確度で並び、後続が iOS 側の調査をこの一文で打ち切るリスク」と指摘。さらに実機 Debug には当てはまるが Simulator (JIT・Mac の性能) 経路には成り立たない不正確さも判明し、未計測の明記 + 経路依存の注記へ書き直した。
- 類似パターン: [test-limitation-asserted-without-measurement](test-limitation-asserted-without-measurement.md) (未実測の断定を証跡に書きテストを弱める)。どちらも「未検証の断定の固定化」だが、こちらは長命層 (ksn-drift のディープ検証対象) に入る点で腐り方が長期化する。
