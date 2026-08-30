---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-29
last-seen: 2026-08-29
evidence:
  - restore-maui-picker-selected-command (既存 Cell の組み合わせだけで構成する MAUI 固有 Sample デモ画面の新設に対し、相方レビューが「新規画面の追加は UI 変更であり ui/brief.md と承認済み mock が必要」と 2 周連続で Major 指摘 [spec-001 と code-002]。ホスト側 review-002 は同じ論点を明示的に検討し「新規の視覚デザイン判断が無く既存 Cell / Section / SampleTheme の組み合わせに閉じるため ui/ 不在は妥当」と判定して両者が割れた。オーナー裁定は「実経路検証が両 OS・単一/複数・確定/再確定/非確定を網羅できているため今回は ui/ を求めない」)
---

## ルール文

Sample のデモ画面の新設でも、既存 Cell / Section / 共有テーマの組み合わせだけで構成され、新しい視覚デザインの判断 (レイアウト・寸法・色の決定) を含まないなら、`ui/` 一式 (brief / mock / 承認) を要求しない。この場合の「見た目の正」は既存 Cell の描画そのものであって、mock が固定すべき新しい対象が存在しない。要求すべきなのは実物での動作確認と証跡であり、そちらが揃っているかを見る。

## 経緯

- 2026-08-29 restore-maui-picker-selected-command: `ksn-propose` の「UI に触れる変更は級に関わらず ui/ を追加する」という文面と、既存の先例 (`kasane/changes/archive/2026-08-12-add-maui-accessory-views/` は MAUI 固有デモ画面を新設したが `ui/` を持たず、動作スクリーンショットと検証記録のみ) が食い違っており、レビュアーごとに判断が分かれた。規約文面を字義どおり読むと合成のみの画面にも mock 承認ゲートが要ることになるが、mock が固定する対象 (新しい視覚デザイン) が存在しないため、ゲートが空回りする。
- 割れた論点はオーナーへ上げて決着した。規約側の文面を「新規の視覚デザイン判断を伴う変更」に狭める改訂が要るかは [[ui-artifacts-scope-wording]] として別途検討する余地がある。
