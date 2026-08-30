---
scope: spec-review
kind: pain
severity: normal
count: 2
first-seen: 2026-08-05
last-seen: 2026-08-06
evidence:
  - fix-dsl-header-height-diff (spec の「headerHeight + 内容の同時変更で両方反映」が、合流先 `applyFullSnapshot` の既存契約「`.full` は同一 ID Cell を再構成しない」(既存テストが固定) と両立しないまま提案確定しかけた。ホスト側は「担保方法は実装判断」に逃がし、相方 second-opinion-001 が Critical で検出)
  - fix-ios-full-content-refresh (行 identity 保証が `reloadSections` の既知動作 (concepts 記載の全 Cell 再構成) と矛盾 [M1]、`KsCellID` の UUID のみ等価判定で具象型変更ケースが未定義 [M2]、既存 headerHeight preflight の発行列 (`.full` + `.replaceCell`) と新機構で同一 Cell の二重 reconfigure [M4]。ホスト自己レビュー 2 周は検出 0、相方 second-opinion-001 が Major 5 件を検出し全件採用)
---

## ルール文

ADDED / MODIFIED Requirement が保証 (表示反映・行 identity 維持・適用は一度だけ等) を宣言する提案をレビューするときは、その保証が通る**合流先の既存機構の契約** — concepts の記載・既存テストが固定している契約・既存経路が発行する diff 列 — を特定して突き合わせ、(1) 保証が既存契約と両立するか、(2) 既存経路との重複適用が生じないか、を確認する。Requirement 単体で自然に読めることは根拠にならない — 合流先の契約と矛盾する spec は実装不能か二重適用になる。

## 経緯

- 2026-08-05 fix-dsl-header-height-diff: iOS の同時変更 Scenario は `.full` のみの発行を前提としたが、当時の `applyFullSnapshot` は同一 ID Cell を reconfigure しない契約 (既存テスト `test_fullDiffでheader不変ならCellは再構成されない` が固定) で、表示反映の保証が成立しなかった。相方 Critical を受けて `.full` + `.replaceCell` 続発方式へ spec を確定。
- 2026-08-06 fix-ios-full-content-refresh: 内容再適用機構の追加提案で、reloadSections との identity 矛盾・具象型変更の未定義・既存 preflight 続発との二重適用の 3 点がいずれも「新 Requirement × 既存機構の契約」の突き合わせ漏れとして相方から指摘された。うち二重適用 (M4) はオーナー裁定で続発廃止が確定し、前 change で入れた続発方式は 1 日で廃止された — 突き合わせを提案段階で行っていれば、依存 change の実装順 (full 側を先に直す) の検討機会もあった。
