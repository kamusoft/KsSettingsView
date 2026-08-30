---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-09
last-seen: 2026-08-09
evidence:
  - add-maui-samples-foundation (相方 spec-review Major が「README 群は docs-refresh 経由のみ」規約を samples/maui/README.md の新設置換タスクに適用し、実装タスクとして完了不能と指摘。オーナー裁定: サンプル付属 README はサンプル成果物の一部で規約対象外 — 実装タスクで書く)
---

## ルール文

AGENTS.md の「docs/ と README 群の書き換えは docs-refresh 経由のみ」規約は、**concepts からの派生物である利用者向けドキュメントの追従更新**を縛るもの。新設するサンプル・検証ホストに付属する README (そのサンプルの一部として初めて書かれるもの) は規約の対象外で、変更の実装タスクとして書いてよい (オーナー裁定 2026-08-09)。spec-review でこの規約を根拠に指摘する前に、対象 README が「既存 docs の追従更新」か「新設成果物の付属物」かを区別する。

## 経緯

- 2026-08-09 add-maui-samples-foundation: 相方 spec-review が task 3.1 (placeholder README → クイックスタート置換) を AGENTS.md 規約との衝突として Major 指摘。オーナーは「規約の趣旨は concepts 派生 docs の自動書き換え禁止であり、新設サンプルの付属 README は含まない」と裁定し、実装タスクのまま維持した。
