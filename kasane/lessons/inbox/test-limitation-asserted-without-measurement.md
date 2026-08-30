---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-08-01
last-seen: 2026-08-01
evidence:
  - fix-android-cell-width-allocation (「Robolectric は TextView の水平オフセット補正を再現しない」という未実測の断定を KDoc / brief.md に記録し、その前提でテストを弱い形に留めた。review-002 が実測で反証)
---

## ルール文

「テスト環境では X を検証できない」という制約をコメント・証跡に書き、それを理由に検証を弱める (アサーションを間接化する・実機に委ねる) ときは、その制約を最小の実験コードで実測してから書く。実測せずに書いた「できない」は検証範囲の穴として固定化される。

## 経緯

- 2026-08-01 fix-android-cell-width-allocation: 実装ワーカーが「`isSingleLine` の TextView は `Layout` 幅が `VERY_WIDE` になるため Robolectric では実描画位置を検証できない」と KDoc / ui/brief.md に断定で記録し、オーナー要求「aux あり + CENTER の title 位置検証」を前提チェックのみの弱いテストで実装した。review-002 が調査テストで実測した結果、`dispatchOnPreDraw()` を 1 行呼べば `scrollX` 補正が再現され px 単位で検証可能と判明 (前半の `VERY_WIDE` は正しく、後半の「補正を再現しない」だけが誤り)。修正はレビュー 2 周を要し、確立した検証手法は `cross/conventions/test-execution.md` へ蒸留された。
- 類似パターン: [deviation-cause-written-without-reading-source](deviation-cause-written-without-reading-source.md) (未読の推測を原因として記録)。どちらも「未検証の断定を証跡に書く」だが、こちらは断定がテストの検証範囲を直接狭める点で影響が異なる。
