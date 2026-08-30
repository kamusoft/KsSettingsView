---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-08
last-seen: 2026-08-08
evidence:
  - add-maui-core (ホスト側 review-001 は APPROVED としたが、design.md Decision 4 が明記する iOS containment の手順順序「AddChild → view 追加 → DidMove」と実装の実順序「view 返却 → attach 後に AddChild + DidMove」の乖離を見逃した。相方 codex が Major として検出し修正サイクルが発生。spec の Scenario 照合は行われていたが、design の手順記述と実装の順序照合が抜けていた)
---

## ルール文

design.md の Decision に**手順・順序** (「A → B → C の順で行う」) が明記されている場合、レビューは spec の Scenario 照合とは別に、その順序と実装の実行順序を突き合わせる。順序乖離は E2E が通っていても潜在する (UIKit 等のフレームワーク契約は最終状態でなく遷移順序に依存する) ため、「動いている」ことは順序が正しいことの証明にならない。

## 経緯

- 2026-08-08 add-maui-core: review-001 (ホスト側) は spec 全 Scenario と実装の照合・interop 契約の遡及検証まで行い APPROVED としたが、design Decision 4 の containment 順序と実装の乖離を見逃した。相方セカンドオピニオン (second-opinion-002) が UIKit 契約違反として Major 指摘し、採用・修正 (IKsHostContainment 切り出しで順序を共通部のテスト可能な形に固定)。クロスモデルレビューが機能した例でもあるが、ホスト側チェックリストに「design 手順と実装順序の照合」が無かったことが根本。
