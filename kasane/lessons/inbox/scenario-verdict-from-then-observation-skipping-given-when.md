---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-05
last-seen: 2026-08-05
evidence:
  - fix-dsl-header-height-diff (iOS Store Scenario の GIVEN「Store 接続で表示されている」WHEN「`replaceSection` する」に対し、テストは `controller.applyDiff(...)` 直呼びで Store→Publisher→Controller 経路を通っていなかった。ホスト review-001 は THEN の観測点が実 frame であることを根拠に「対称テスト義務を満たす」と APPROVED、相方 second-opinion-002 が Major として検出)
---

## ルール文

Scenario とテストの対応を判定するときは、THEN の観測点 (何を assert しているか) だけで充足と判断しない。**GIVEN の状態構築と WHEN の操作が、Scenario に書かれた経路そのものを通っているか**をテストコードで確認する。特に Scenario が経路を明示している場合 (「Store 接続で」「公開 API 経由で」等)、内部 API の直呼びでの代替は充足にならない — 経路上のどの層が壊れても検出できないため。「内部的には同じ diff を送るだけだから等価」という説明は、その等価性自体が回帰で壊れうるので根拠にしない。

## 経緯

- 2026-08-05 fix-dsl-header-height-diff: iOS の Store Requirement は core/ADR-0018 の経路対称性を閉じるためのもので、Scenario は GIVEN「Store 接続で表示されている」・WHEN「`replaceSection` する」と経路を明示していた。追加テストは観測点 (表示中 supplementary view の実 frame 高さ) は spec の要求どおりだったが、Controller を `root:` initializer で生成し `controller.applyDiff(...)` を直接呼ぶ構成で、Store→Publisher→Controller 経路は未検証だった。ホスト側レビューと verify はともに観測点の適切さを根拠に充足と判定 (verify は「注記」として事実は記録)。相方が「Store 経由テストが Store を通っていない」として Major で検出し、突き合わせで採用。
