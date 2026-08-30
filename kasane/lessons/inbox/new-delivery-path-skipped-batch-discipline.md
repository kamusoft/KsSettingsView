---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-08-12
last-seen: 2026-08-12
evidence:
  - add-maui-custom-cell (facade の cell content 配信 (ApplyHostViews / ReleaseCellContentViews) が既存の使い分け規律「同一操作の複数 Cell 更新は ReplaceCells で1バッチ」(PublishPending は適用済み) から漏れ、1件ずつ ReplaceCell を連発。Android の AsyncListDiffer が連続 submitList で先行世代の notifyItemChanged を破棄する既知制約を踏み、全 CustomCell の Content が空描画される Blocker に。ユニットテストは 1 セルずつ更新するため非検出、E2E で発覚)
---

## ルール文

gateway への配信を行う経路を新設・拡張するときは、既存経路の配信規律 (同一操作で複数 Cell を更新するなら `ReplaceCells` 1 バッチ、`updates.Count == 1` のみ単発 `ReplaceCell`) を新経路にも適用したかを完了条件で確認する。連続単発配信は Android の AsyncListDiffer 世代破棄 (既知制約) で通知が黙って失われ、ユニットテストでは 1 件更新しか通らないため検出できない。

## 経緯

- 2026-08-12 add-maui-custom-cell: Host 取り付け時の CustomCell content 一斉配信が単発 `ReplaceCell` の連発で実装され、Android 実機で全行空描画 (Blocker)。既知制約への対策 API (`ReplaceCells`) と使い分けの先例 (`PublishPending`) は存在しており、新経路がそれに合流しなかったことが原因。修正は配信元でのバッチ化 + 「修正を戻すと落ちる」再発防止テスト。
