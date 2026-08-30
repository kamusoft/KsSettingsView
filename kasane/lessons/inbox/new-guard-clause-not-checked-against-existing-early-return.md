---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-05
last-seen: 2026-08-05
evidence:
  - fix-dsl-header-height-diff (iOS `DSLDiffCalculator.compute` に headerHeight preflight を追加したが、先行する可視性 preflight の早期 return が優先されるため「可視性変化 + headerHeight 変化 + 同一 ID Cell 内容変化」の併発で spec 要求の `.replaceCell` が欠落。ホスト review-001 は APPROVED、相方 second-opinion-002 が Major として検出)
---

## ルール文

早期 return するガード節 (preflight・guard・先頭の `if ... return`) を既存のガード節の列に追加する変更をレビューするときは、**新規ガードと既存ガードが同時に成立する入力**を必ず1ケース以上構成して、どちらが先に return するか・その結果が新規ガードの要求を満たすかを確認する。単独成立時の挙動だけを見て承認しない。追加されたガードが「既存ガードと同じ位置・同じ段階」と説明されている場合ほど、実際には順序で挙動が変わるため確認する。

## 経緯

- 2026-08-05 fix-dsl-header-height-diff: iOS の `DSLDiffCalculator.compute` は可視性 preflight (`:62`) が `.full` のみを返して早期 return し、その後に追加された headerHeight preflight (`:75`) が `.full` + `.replaceCell` を返す構造だった。デルタスペックは「headerHeight が変化している場合、同一再評価内で内容が変わった同一 ID Cell があれば `.full` に続けて `.replaceCell` を発行する」を無条件の SHALL としていたため、可視性変化が同時に起きる再評価では spec 違反になる。追加テストは可視性変化を組み合わせていなかったため検出できず、ホスト側レビューも併発ケースを構成しないまま APPROVED を出した。相方 (codex) が該当行を特定して Major で検出し、突き合わせで採用。
