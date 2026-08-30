---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-08
last-seen: 2026-08-08
evidence:
  - release-host-without-bridge-dispose (review-001 Suggestion「deinit の先頭で disconnectStore() を呼ぶと1箇所になる」を適用したところ、deinit は nonisolated な文脈のため MainActor-isolated メソッドを呼べずビルド失敗。元の複製へ復帰した)
---

## ルール文

Swift の `deinit` に対して「isolated メソッド (MainActor 等) への委譲でティアダウンを一本化する」修正を提案しない。`deinit` は nonisolated な文脈であり isolated インスタンスメソッドを呼べない (stored property への直接アクセスだけが deinit の特例で合法)。ティアダウン重複を指摘するときは、提案する修正が actor isolation 境界を越えないか — つまりコンパイルが通る形か — を確認してから出す。重複がコンパイラ制約由来なら、修正提案ではなく「両方に追記する」旨の注意コメントを促す指摘に切り替える。

## 経緯

- 2026-08-08 release-host-without-bridge-dispose: `KsSettingsViewController` の `deinit` と新設 `disconnectStore()` が購読3本の cancel + nil 代入を複製しており、review-001 が Suggestion で「`deinit` の先頭で `disconnectStore()` を呼ぶ形に寄せると1箇所になる」と提案。オーナー承認を得て適用したが、`Call to main actor-isolated instance method 'disconnectStore()' in a synchronous nonisolated context` でビルド失敗。複製を復元し、一本化できない理由と「購読を増やすときは両方に追記」の注意コメントを deinit 側へ残す形で収束した。
