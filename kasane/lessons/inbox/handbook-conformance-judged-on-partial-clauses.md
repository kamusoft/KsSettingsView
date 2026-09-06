---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-06
last-seen: 2026-09-06
evidence:
  - cell-style-dark-appearance-parity (ホスト側 review-001 は `cross/test-execution.md` を「適合。新規テストの待機は `awaitConvergence` / `composeRule.waitForIdle` / `layoutIfNeeded` を使い、固定秒数の収束待ちは持ち込んでいない」と判定したが、同 handbook の Android 節は「idle 系では `AsyncListDiffer` の差分計算の完了を待てない」と明記しており、`DSLCellStyleUpdateTest` は `waitForIdle()` 直後に行の色を読んでいた。相方コードレビュー (second-opinion-code-001 Major) が検出し、ホスト側の見逃しとして採用。条件ベース待機へ修正)
---

## ルール文

レビューで handbook の `kind: rule` 文書を「適合」と判定するときは、その文書が定める条項を 1 つずつ列挙し、条項ごとに diff 内の該当箇所と判定 (適合 / 該当なし) を対応付けてから結論を書く。禁止形 (固定秒数の待機等) が無いことだけを確認して「適合」としない — 規約は禁止形の不在に加えて要求形 (述語 + deadline + 実行機会の譲渡 + 実測値付き fail、idle 系を差分計算の完了待ちに使わない等) を求めており、要求形の欠落は禁止形の検索では見えない。事後判定: レビューの「照合した規約」表の判定欄に、適用した条項名 (または節名) と該当箇所が書かれている。

## 経緯

- 2026-09-06 cell-style-dark-appearance-parity: ホスト側は「固定秒数の待機が無い」という禁止形の不在で適合と判定した。相方は同じ handbook の Android 節の要求形 (`AsyncListDiffer` は idle では待てない) を diff に当てて Major を出した。同じ規約文書を両者が「照合した」と報告していたが、当てた条項が違った。
