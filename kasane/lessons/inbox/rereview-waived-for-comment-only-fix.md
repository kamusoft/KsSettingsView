---
scope: process
kind: success
severity: normal
count: 2
first-seen: 2026-08-22
last-seen: 2026-09-19
evidence:
  - fix-compose-dsl-double-update-flaky-test (review-002 APPROVED 後、残 Minor への対応が KDoc 1 文の書き換えのみだったため、オーナー判断で独立文脈の再レビューを省略して完了)
  - picker-selection-after-dismiss (review-003 APPROVED 後、Suggestion の付随修正 [既存テストヘルパの `idle()` を条件ベース待機へ置換、テストコードのみ] をオーナー判断で同梱し「修正してテストが 1 回通れば OK」として独立文脈の再レビューを省略)
---

## ルール文

レビュー判定が APPROVED になった後、残指摘への対応が**実行コードを変えないコメント・docstring のみの修正**である場合は、独立文脈の再レビューを省略してよい。省略の条件は3つすべてを満たすこと: (1) `git diff` で実行コードに変更がないと確認済み、(2) 直前の判定が APPROVED、(3) 完了報告に省略の事実とオーナー判断である旨を明記。CHANGES_REQUESTED の解消や実行コードに触れる修正には適用しない。

## 経緯

- 2026-08-22 fix-compose-dsl-double-update-flaky-test: review-002 は APPROVED (Critical 0 / Major 0 / Minor 1 は任意)。残 Minor は「新 docstring が `Thread.yield()` の不十分さを一般命題として書いた結果、yield のままの共有ヘルパ 5 箇所の説明とリポジトリ規模で食い違う」というもので、対応は当該ヘルパ固有の要件記述へ書き換える KDoc 1 文のみ。オーナーが「文言修正するが再レビューは不要」と明示的に判断し、3 周目のレビューサイクルを省略した。ksn-orchestrator の「修正後の確認は独立文脈で行う (ここは譲らない)」に対する明示的な免除であり、完了報告にオーナー判断として記録した。
- 2026-09-19 picker-selection-after-dismiss: review-003 APPROVED 後の Suggestion (既存テストヘルパの `idle()` 4 か所の置き換え、実行コードは不変) をオーナーが同梱と決め、完了条件を「Android テスト 1 回の通過」と明示して再レビューを免除した。前例のコメントのみより一歩広く、**テストコードのみの修正**にも省略が適用された例。完了報告に省略の事実とオーナー判断を記録。
