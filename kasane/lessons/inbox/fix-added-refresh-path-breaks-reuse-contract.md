---
scope: impl
kind: pain
severity: normal
count: 2
first-seen: 2026-08-20
last-seen: 2026-09-18
evidence:
  - implement-modern-style (review-002 Major: レビュー指摘 [可視 Section 0 件時の余白] の修正として導入した `refreshSectionUnitPresentation()` が、Root Header / Footer の accessory を全 Diff で factory から作り直す副作用を持ち込んだ。Root accessory と無関係な replaceCell 1 件で KsAnyView.uiKit の factory が再実行されることを一時プローブで実測。concepts が明示的に避けるべきとする類型 [view accessory の factory 再生成による内部状態喪失] に該当。修正は直近適用値 appliedRootAccessoryMargin の保持と変化時のみの作り直し + 双方向の回帰テスト 2 本)
  - android-accessory-view-late-insert-animation (レビュー指摘 [接続失敗時の後片付けが最初の例外で中断する] の修正として `ReleaseHost` の後片付けを全件試行化した結果、書き戻し・内容なし配信に失敗した実体まで Native Host の解放より前に破棄するようになり、既存の退役順序契約 [Store 更新 → native 配信 → 破棄、maui/ADR-0016] を破った。指摘箇所のテストは通っており、相方レビュー second-opinion-code-005 が Major で検出。修正は知らせの届かなかった実体だけを Host 解放後に破棄する形 + ミューテーション実測付きの回帰テスト 2 本)
---

## ルール文

レビュー指摘の修正で全体 refresh・一括再適用の経路を新設・拡張するときは、その経路が既存の再生成回避契約 (view accessory の factory を作り直さない・同値は再適用しない等) を破っていないかを、修正対象と**無関係な操作** (単発の replaceCell 等) で実測してから提出する。指摘箇所のテストが通ることは副作用の不在を意味しない。

## 経緯

- 2026-08-20 implement-modern-style: 可視 Section 0 件時の余白修正で足した全体 refresh が、無関係な Diff のたびに Root accessory を factory から再生成 (review-002 Major、一時プローブで実測)。直近適用値の保持と変化時のみの再構築 + 「作り直さない / 作り直す」双方向の回帰テストで解消 (review-003 で確認)。
- 2026-09-18 android-accessory-view-late-insert-animation: 失敗時の後片付けを「全件試行」へ広げる修正が、成功時には守れていた退役順序 (maui/ADR-0016) を失敗経路で破った。修正対象 (例外の集約) のテストは緑のまま、独立レビューが順序違反を検出した。同型 2 件目 (前回は全体 refresh の新設、今回は失敗経路の網羅化 — どちらも「レビュー指摘の修正で経路を広げたとき、既存の順序・再生成回避契約を修正と無関係な観点で確かめていない」)
